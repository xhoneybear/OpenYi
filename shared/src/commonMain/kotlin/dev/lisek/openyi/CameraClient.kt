package dev.lisek.openyi

import io.ktor.network.selector.SelectorManager
import io.ktor.network.sockets.Socket
import io.ktor.network.sockets.aSocket
import io.ktor.network.sockets.openReadChannel
import io.ktor.network.sockets.openWriteChannel
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.ByteWriteChannel
import io.ktor.utils.io.readAvailable
import io.ktor.utils.io.writeFully
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonPrimitive

/**
 * Representation of a JSON response received from the Yi Action Camera on TCP port 7878.
 */
@Serializable
data class CameraPacket(
    @SerialName("msg_id")
    val msgId: Int? = null,
    val rval: Int? = null,
    val param: JsonElement? = null,
    val token: Int? = null,
    val type: String? = null
) {
    val isSuccess: Boolean
        get() = rval == 0

    val intParam: Int?
        get() = param?.jsonPrimitive?.content?.toIntOrNull()

    val strParam: String?
        get() = param?.jsonPrimitive?.content

    override fun toString(): String {
        return Json.encodeToString(this)
    }

    companion object {
        fun parse(json: String): CameraPacket {
            return Json.decodeFromString<CameraPacket>(json)
        }
    }
}

enum class CameraLogLevel {
    TX,
    RX,
    INFO,
    WARN,
    ERROR
}

data class CameraLogEntry(
    val level: CameraLogLevel,
    val message: String
)

/**
 * A classified signal / notification event received from the camera.
 */
data class CameraSignal(
    val type: String,
    val param: String?,
    val rawResponse: CameraPacket
)

/**
 * Dispatcher that classifies incoming signals by `type` and invokes
 * corresponding functions with the parameter from the `param` block.
 */
open class CameraSignalDispatcher {
    open fun onSignal(type: String, param: String?, rawResponse: CameraPacket) {
        when (type) {
            "battery" -> onBattery(param)
            "adapter" -> onAdapter(param)
            "adapter_status" -> onAdapterStatus(param)
            "sd_card_status" -> onSdCardStatus(param)
            "photo_taken" -> onPhotoTaken(param)
            "video_record_complete" -> onVideoRecordComplete(param)
            "start_video_record" -> onStartVideoRecord(param)
            "vf_start" -> onVfStart(param)
            "vf_stop" -> onVfStop(param)
            else -> onCustomSignal(type, param)
        }
    }

    open fun onBattery(param: String?) {}
    open fun onAdapter(param: String?) {}
    open fun onAdapterStatus(param: String?) {}
    open fun onSdCardStatus(param: String?) {}
    open fun onPhotoTaken(param: String?) {}
    open fun onVideoRecordComplete(param: String?) {}
    open fun onStartVideoRecord(param: String?) {}
    open fun onVfStart(param: String?) {}
    open fun onVfStop(param: String?) {}
    open fun onCustomSignal(type: String, param: String?) {}
}

/**
 * Frames JSON objects from a byte stream using balanced braces `{ ... }`.
 * Handles string escapes, whitespace, and multi-packet chunking.
 */
class JsonStreamFramer(
    private val onPacketReceived: (String) -> Unit
) {
    private val byteBuffer = mutableListOf<Byte>()
    private var braceDepth = 0
    private var inString = false
    private var escape = false
    private var started = false

    fun feed(buffer: ByteArray, count: Int = buffer.size) {
        for (i in 0 until count) {
            val b = buffer[i]
            if (!started) {
                if (b == 0x7B.toByte()) { // '{'
                    started = true
                    braceDepth = 1
                    inString = false
                    escape = false
                    byteBuffer.clear()
                    byteBuffer.add(b)
                }
                continue
            }

            byteBuffer.add(b)
            if (escape) {
                escape = false
            } else if (b == 0x5C.toByte()) { // '\\'
                escape = true
            } else if (b == 0x22.toByte()) { // '"'
                inString = !inString
            } else if (!inString) {
                if (b == 0x7B.toByte()) { // '{'
                    braceDepth++
                } else if (b == 0x7D.toByte()) { // '}'
                    braceDepth--
                    if (braceDepth == 0) {
                        val jsonBytes = byteBuffer.toByteArray()
                        byteBuffer.clear()
                        started = false
                        val jsonString = jsonBytes.decodeToString()
                        onPacketReceived(jsonString)
                    }
                }
            }
        }
    }
}

/**
 * TCP socket client for controlling the Xiaomi / Yi Action Camera.
 *
 * Communicates with the camera via JSON commands on TCP port 7878.
 */
class CameraClient(
    var host: String = DEFAULT_HOST,
    val port: Int = DEFAULT_PORT,
    private val selector: SelectorManager = SelectorManager(Dispatchers.Default),
    private val storage: AppStorage? = null
) {
    init {
        storage?.cameraHost?.let { savedHost ->
            host = savedHost
        }
    }

    companion object {
        const val DEFAULT_HOST = "192.168.42.1"
        const val DEFAULT_PORT = 7878

        const val MSG_GET_SETTING = 1
        const val MSG_SET_SETTING = 2
        const val MSG_GET_ALL_SETTINGS = 3
        const val MSG_FORMAT_SD = 4
        const val MSG_GET_SD_STATUS = 5
        const val MSG_GET_SETTING_CHOICES = 9
        const val MSG_GET_BATTERY = 13
        const val MSG_START_SESSION = 257
        const val MSG_STOP_SESSION = 258
        const val MSG_START_RECORD = 513
        const val MSG_STOP_RECORD = 514
        const val MSG_TAKE_PHOTO = 769

        val shared: CameraClient by lazy {
            val s = AppStorage()
            CameraClient(host = s.cameraHost, storage = s)
        }
    }

    fun updateHost(newHost: String) {
        if (host != newHost) {
            host = newHost
            storage?.cameraHost = newHost
            log(CameraLogLevel.INFO, "Camera target host set to: $newHost")
        }
    }

    private val _logs = MutableSharedFlow<CameraLogEntry>(replay = 50, extraBufferCapacity = 64)
    val logs: SharedFlow<CameraLogEntry> = _logs.asSharedFlow()

    fun log(level: CameraLogLevel, message: String) {
        _logs.tryEmit(CameraLogEntry(level, message))
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private var socket: Socket? = null
    private var readChannel: ByteReadChannel? = null
    private var writeChannel: ByteWriteChannel? = null
    private var readJob: Job? = null

    private val connectionMutex = Mutex()
    private val sendMutex = Mutex()

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val _currentToken = MutableStateFlow<Int?>(storage?.authToken?.toIntOrNull())
    val currentToken: StateFlow<Int?> = _currentToken.asStateFlow()

    private val _responses = MutableSharedFlow<CameraPacket>(extraBufferCapacity = 64)
    val responses: SharedFlow<CameraPacket> = _responses.asSharedFlow()

    private val _rawResponses = MutableSharedFlow<String>(extraBufferCapacity = 64)
    val rawResponses: SharedFlow<String> = _rawResponses.asSharedFlow()

    private val _signals = MutableSharedFlow<CameraSignal>(extraBufferCapacity = 64)
    val signals: SharedFlow<CameraSignal> = _signals.asSharedFlow()

    var signalDispatcher: CameraSignalDispatcher? = null

    private val typeHandlers = mutableMapOf<String, (String?) -> Unit>()
    private val anySignalHandlers = mutableListOf<(String, String?) -> Unit>()

    private val pendingRequests = mutableMapOf<Int, CompletableDeferred<CameraPacket>>()
    private var genericPendingRequest: CompletableDeferred<CameraPacket>? = null

    fun onSignal(type: String, handler: (param: String?) -> Unit) {
        typeHandlers[type] = handler
    }

    fun onAnySignal(handler: (type: String, param: String?) -> Unit) {
        anySignalHandlers.add(handler)
    }

    suspend fun connect(targetHost: String = host, targetPort: Int = port) {
        connectionMutex.withLock {
            if (_isConnected.value) {
                log(CameraLogLevel.WARN, "Already connected to $host:$port")
                return
            }

            log(CameraLogLevel.INFO, "Connecting to $targetHost:$targetPort...")
            try {
                val newSocket = aSocket(selector).tcp().connect(targetHost, targetPort)
                if (targetHost != host) {
                    updateHost(targetHost)
                }
                socket = newSocket
                val read = newSocket.openReadChannel()
                val write = newSocket.openWriteChannel(autoFlush = true)
                readChannel = read
                writeChannel = write
                _isConnected.value = true
                log(CameraLogLevel.INFO, "Connected to $targetHost:$targetPort")

                startReadLoop(read)
            } catch (e: Exception) {
                val errorDetails = e.message ?: e::class.simpleName ?: e.toString()
                log(CameraLogLevel.ERROR, "Connect to $targetHost:$targetPort failed: $errorDetails")
                disconnectInternal()
                throw e
            }
        }
    }

    suspend fun disconnect() {
        connectionMutex.withLock {
            log(CameraLogLevel.INFO, "Disconnecting from $host:$port")
            disconnectInternal()
        }
    }

    private fun disconnectInternal() {
        val wasConnected = _isConnected.value
        _isConnected.value = false
        readJob?.cancel()
        readJob = null

        val pendingToCancel = pendingRequests.values.toList()
        pendingRequests.clear()
        pendingToCancel.forEach {
            it.completeExceptionally(IllegalStateException("Camera socket disconnected"))
        }
        genericPendingRequest?.completeExceptionally(IllegalStateException("Camera socket disconnected"))
        genericPendingRequest = null

        runCatching { socket?.close() }
        socket = null
        readChannel = null
        writeChannel = null
        if (wasConnected) {
            log(CameraLogLevel.WARN, "Disconnected from camera")
        }
    }

    private fun startReadLoop(read: ByteReadChannel) {
        readJob = scope.launch {
            val framer = JsonStreamFramer { jsonString ->
                handleIncomingPacket(jsonString)
            }
            val buffer = ByteArray(4096)
            try {
                while (!read.isClosedForRead) {
                    val count = read.readAvailable(buffer)
                    if (count == -1) break
                    framer.feed(buffer, count)
                }
            } catch (e: CancellationException) {
                // Expected when canceling readJob
            } catch (e: Exception) {
                // Connection error
            } finally {
                disconnectInternal()
            }
        }
    }

    private fun handleIncomingPacket(jsonString: String) {
        log(CameraLogLevel.RX, jsonString)
        val response = CameraPacket.parse(jsonString)

        if (response.msgId == MSG_START_SESSION && response.isSuccess) {
            response.intParam?.let { token ->
                _currentToken.value = token
                storage?.authToken = token.toString()
            }
        } else if (response.token != null && _currentToken.value == null) {
            _currentToken.value = response.token
            storage?.authToken = response.token.toString()
        }

        _rawResponses.tryEmit(jsonString)
        _responses.tryEmit(response)

        // Type classification dispatch for signals
        val type = response.type
        if (type != null) {
            val param = response.strParam
            val signal = CameraSignal(type, param, response)
            _signals.tryEmit(signal)

            signalDispatcher?.onSignal(type, param, response)
            typeHandlers[type]?.invoke(param)
            anySignalHandlers.forEach { it.invoke(type, param) }
        }

        val msgId = response.msgId
        if (msgId != null && pendingRequests.containsKey(msgId)) {
            pendingRequests.remove(msgId)?.complete(response)
        } else if (genericPendingRequest != null) {
            genericPendingRequest?.complete(response)
            genericPendingRequest = null
        }
    }

    suspend fun ensureConnected() {
        if (!_isConnected.value) {
            connect()
        }
    }

    /**
     * Sends a raw JSON packet string over the socket and awaits the corresponding response.
     */
    suspend fun sendPacket(json: CameraPacket, timeoutMs: Long = 5000): CameraPacket {
        ensureConnected()
        return sendMutex.withLock {
            val write = writeChannel ?: error("Not connected to camera")

            val deferred = CompletableDeferred<CameraPacket>()
            if (json.msgId != null) {
                pendingRequests[json.msgId] = deferred
            } else {
                genericPendingRequest = deferred
            }

            try {
                log(CameraLogLevel.TX, json.toString())
                write.writeFully(json.toString().encodeToByteArray())
                write.flush()
                withTimeout(timeoutMs) {
                    deferred.await()
                }
            } catch (e: Exception) {
                val errorDetails = e.message ?: e::class.simpleName ?: e.toString()
                log(CameraLogLevel.ERROR, "Send failed (msg_id ${json.msgId}): $errorDetails")
                throw e
            } finally {
                if (json.msgId != null) {
                    pendingRequests.remove(json.msgId)
                } else if (genericPendingRequest === deferred) {
                    genericPendingRequest = null
                }
            }
        }
    }

    /**
     * Sends a command with the given msg_id, optional parameter, and token.
     */
    suspend fun send(
        msgId: Int,
        param: String? = null,
        token: Int? = currentToken.value,
        timeoutMs: Long = 5000
    ): CameraPacket {
        val effectiveToken = token ?: 0
        val packet = CameraPacket(msgId = msgId, token = effectiveToken, param = JsonPrimitive(param))
        return sendPacket(packet, timeoutMs)
    }

    /**
     * Sends `{"msg_id":257,"token":0}` to start a session and acquire a session token.
     */
    suspend fun startSession(timeoutMs: Long = 5000): CameraPacket {
        log(CameraLogLevel.INFO, "Starting session (msg_id: 257)...")
        val packet = CameraPacket(msgId = MSG_START_SESSION, token = 0)
        return sendPacket(packet, timeoutMs).also { resp ->
            if (resp.isSuccess) {
                log(CameraLogLevel.INFO, "Session started successfully! Token: ${resp.intParam}")
            } else {
                log(CameraLogLevel.WARN, "Session start returned rval: ${resp.rval}")
            }
        }
    }

    /**
     * Sends `{"msg_id":258,"token":<token>}` to end the active session.
     */
    suspend fun stopSession(timeoutMs: Long = 5000): CameraPacket {
        val token = currentToken.value ?: 0
        val packet = CameraPacket(msgId = MSG_STOP_SESSION, token = token)
        return sendPacket(packet, timeoutMs).also {
            _currentToken.value = null
            storage?.authToken = null
        }
    }

    /**
     * Sends `{"msg_id":3,"token":<token>}` to retrieve all camera settings.
     */
    suspend fun getAllSettings(timeoutMs: Long = 10000): CameraPacket {
        val token = currentToken.value ?: 0
        val packet = CameraPacket(msgId = MSG_GET_ALL_SETTINGS, token = token)
        return sendPacket(packet, timeoutMs)
    }
}
