package dev.lisek.openyi

import io.ktor.network.selector.SelectorManager
import io.ktor.network.sockets.Socket
import io.ktor.network.sockets.aSocket
import io.ktor.network.sockets.openReadChannel
import io.ktor.network.sockets.openWriteChannel
import io.ktor.utils.io.readAvailable
import io.ktor.utils.io.writeFully
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Represents the progress and results of a camera network discovery operation.
 */
sealed interface DiscoveryState {
    data object Idle : DiscoveryState

    data class Scanning(
        val currentIp: String,
        val checkedCount: Int,
        val totalCount: Int,
        val foundIps: List<String> = emptyList()
    ) : DiscoveryState {
        val progress: Float
            get() = if (totalCount > 0) checkedCount.toFloat() / totalCount.toFloat() else 0f
    }

    data class Finished(
        val discoveredIps: List<String>
    ) : DiscoveryState {
        val hasFoundCamera: Boolean
            get() = discoveredIps.isNotEmpty()

        val primaryIp: String?
            get() = discoveredIps.firstOrNull()
    }
}

/**
 * Network discovery engine for locating Xiaomi / Yi Action Cameras on local networks.
 */
object CameraDiscovery {

    /**
     * Probes an IP on port 7878 using a TCP session-start handshake packet:
     * `{"msg_id":257,"token":0}`.
     *
     * Returns true if the remote host responds with a valid Yi camera JSON response,
     * or successfully accepts a TCP connection on port 7878.
     */
    suspend fun probeCamera(
        targetHost: String,
        port: Int = CameraClient.DEFAULT_PORT,
        timeoutMs: Long = 1000,
        selector: SelectorManager
    ): Boolean {
        var socket: Socket? = null
        return try {
            withTimeoutOrNull(timeoutMs) {
                val newSocket = aSocket(selector).tcp().connect(targetHost, port)
                socket = newSocket
                val read = newSocket.openReadChannel()
                val write = newSocket.openWriteChannel(autoFlush = true)

                try {
                    // Send session start probe packet
                    val probe = """{"msg_id":257,"token":0}"""
                    write.writeFully(probe.encodeToByteArray())
                    write.flush()

                    val buffer = ByteArray(2048)
                    val bytesRead = withTimeoutOrNull(600) { read.readAvailable(buffer) } ?: 0
                    if (bytesRead > 0) {
                        val responseString = buffer.decodeToString(0, bytesRead)
                        // Camera returns JSON containing msg_id (e.g. msg_id 7 vf_stop, msg_id 257) or rval
                        if (responseString.contains("msg_id") || responseString.contains("rval") || responseString.contains("type")) {
                            return@withTimeoutOrNull true
                        }
                    }
                } catch (_: Exception) {}

                // On a local /24 subnet, accepting a TCP socket on proprietary port 7878 confirms the camera
                true
            } ?: false
        } catch (_: Exception) {
            false
        } finally {
            runCatching { socket?.close() }
        }
    }

    /**
     * Computes candidate IPv4 addresses for a /24 subnet given an interface IP.
     * e.g., "192.168.1.50" -> ["192.168.1.1", ..., "192.168.1.254"] (excluding localIp).
     */
    fun computeSubnetCandidates(localIp: String): List<String> {
        val parts = localIp.trim().split(".")
        if (parts.size != 4) return emptyList()

        // Validate IPv4 octets
        val octets = parts.mapNotNull { it.toIntOrNull() }
        if (octets.size != 4 || octets.any { it !in 0..255 }) return emptyList()

        val prefix = "${parts[0]}.${parts[1]}.${parts[2]}."
        val selfHost = octets[3]

        return (1..254).filter { it != selfHost }.map { "$prefix$it" }
    }

    /**
     * Executes an asynchronous network scan for Yi cameras, emitting progressive updates.
     */
    fun discover(
        storage: AppStorage? = null,
        stopOnFirst: Boolean = true,
        port: Int = CameraClient.DEFAULT_PORT,
        timeoutPerHostMs: Long = 800,
        concurrency: Int = 32
    ): Flow<DiscoveryState> = flow {
        val selector = SelectorManager(Dispatchers.Default)
        try {
            // Gather Tier 1 priority IPs
            val priorityIps = linkedSetOf<String>()
            val localIp = getLocalIp()
            val localSubnet = localIp?.substringBeforeLast(".")
            val savedHost = storage?.cameraHost?.trim()

            if (localSubnet != null) {
                if (localSubnet == "192.168.42") {
                    // Connected to camera's own Wi-Fi AP
                    priorityIps.add(CameraClient.DEFAULT_HOST)
                } else {
                    // On home/local network (e.g. 192.168.1.x)
                    // Do NOT probe 192.168.42.1 because it is unroutable and will hang.
                    // Only prioritize savedHost if it matches the current subnet.
                    if (!savedHost.isNullOrBlank() && savedHost.substringBeforeLast(".") == localSubnet) {
                        priorityIps.add(savedHost)
                    }
                }
            } else {
                if (!savedHost.isNullOrBlank()) priorityIps.add(savedHost)
                priorityIps.add(CameraClient.DEFAULT_HOST)
            }
            val subnetCandidates = if (localIp != null) {
                computeSubnetCandidates(localIp).filter { !priorityIps.contains(it) }
            } else {
                emptyList()
            }

            val allCandidates = priorityIps.toList() + subnetCandidates
            val totalCount = allCandidates.size
            var checkedCount = 0
            val foundIps = mutableListOf<String>()

            emit(DiscoveryState.Scanning("", 0, totalCount, foundIps))

            // Tier 1: Probe high priority IPs sequentially first for immediate discovery
            for (priorityIp in priorityIps) {
                emit(DiscoveryState.Scanning(priorityIp, checkedCount, totalCount, foundIps))
                val isCamera = probeCamera(priorityIp, port, timeoutPerHostMs, selector)
                checkedCount++
                if (isCamera) {
                    foundIps.add(priorityIp)
                    emit(DiscoveryState.Scanning(priorityIp, checkedCount, totalCount, foundIps))
                    if (stopOnFirst) {
                        emit(DiscoveryState.Finished(foundIps.toList()))
                        return@flow
                    }
                }
            }

            // Tier 2: Sweep remaining subnet candidates concurrently with controlled concurrency
            if (subnetCandidates.isNotEmpty()) {
                val semaphore = Semaphore(concurrency)
                val mutex = Mutex()
                var earlyStop = false

                coroutineScope {
                    val jobs = subnetCandidates.map { ip ->
                        async {
                            if (earlyStop) return@async

                            semaphore.withPermit {
                                if (earlyStop) return@withPermit

                                val isCamera = probeCamera(ip, port, timeoutPerHostMs, selector)

                                mutex.withLock {
                                    checkedCount++
                                    if (isCamera) {
                                        foundIps.add(ip)
                                        if (stopOnFirst) {
                                            earlyStop = true
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Await all async probes (or until earlyStop cancels)
                    jobs.awaitAll()
                }
            }

            emit(DiscoveryState.Finished(foundIps.toList()))
        } finally {
            runCatching { selector.close() }
        }
    }
}
