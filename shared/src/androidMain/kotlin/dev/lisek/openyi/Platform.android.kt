package dev.lisek.openyi

import android.os.Build
import java.net.Inet4Address
import java.net.NetworkInterface

class AndroidPlatform : Platform {
    override val name: String = "Android ${Build.VERSION.SDK_INT}"
}

actual fun getPlatform(): Platform = AndroidPlatform()

actual fun getLocalIp(): String? {
    try {
        val interfaces = NetworkInterface.getNetworkInterfaces() ?: return null
        val list = interfaces.toList()

        // Priority 1: explicitly named Wi-Fi or AP interfaces
        for (interf in list) {
            if (!interf.isUp || interf.isLoopback) continue
            val name = interf.name.lowercase()
            val isWifi = name.startsWith("wlan") || name.startsWith("wifi") || name.startsWith("ap") || name.startsWith("softap")
            if (isWifi) {
                for (addr in interf.inetAddresses) {
                    if (!addr.isLoopbackAddress && addr is Inet4Address) {
                        val host = addr.hostAddress
                        if (host != null && !host.startsWith("127.")) {
                            return host
                        }
                    }
                }
            }
        }

        // Priority 2: any active non-loopback, non-cellular IPv4 interface (e.g. eth0 in emulator)
        for (interf in list) {
            if (!interf.isUp || interf.isLoopback) continue
            val name = interf.name.lowercase()
            if (name.startsWith("rmnet") || name.startsWith("pdp") || name.startsWith("ccmni")) continue
            for (addr in interf.inetAddresses) {
                if (!addr.isLoopbackAddress && addr is Inet4Address) {
                    val host = addr.hostAddress
                    if (host != null && !host.startsWith("127.")) {
                        return host
                    }
                }
            }
        }
    } catch (_: Throwable) {}
    return null
}
