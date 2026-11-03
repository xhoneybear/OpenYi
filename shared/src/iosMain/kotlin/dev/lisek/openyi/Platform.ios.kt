package dev.lisek.openyi

import kotlinx.cinterop.CPointer
import kotlinx.cinterop.CPointerVar
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.pointed
import kotlinx.cinterop.ptr
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.toKString
import kotlinx.cinterop.value
import platform.UIKit.UIDevice
import platform.darwin.freeifaddrs
import platform.darwin.getifaddrs
import platform.darwin.ifaddrs
import platform.posix.AF_INET
import platform.posix.sockaddr_in

class IOSPlatform: Platform {
    override val name: String = UIDevice.currentDevice.systemName() + " " + UIDevice.currentDevice.systemVersion
}

actual fun getPlatform(): Platform = IOSPlatform()

@OptIn(ExperimentalForeignApi::class)
actual fun getLocalIp(): String? {
    return memScoped {
        val ifap = alloc<CPointerVar<ifaddrs>>()
        if (getifaddrs(ifap.ptr) != 0) return null
        try {
            var curr: CPointer<ifaddrs>? = ifap.value
            while (curr != null) {
                val ifa = curr.pointed
                val addr = ifa.ifa_addr
                if (addr != null && addr.pointed.sa_family.toInt() == AF_INET) {
                    val name = ifa.ifa_name?.toKString()
                    if (name != null && (name.startsWith("en") || name.startsWith("ap") || name.startsWith("bridge"))) {
                        val sin = addr.reinterpret<sockaddr_in>()
                        val sAddr = sin.pointed.sin_addr.s_addr
                        val b0 = sAddr and 0xFFu
                        val b1 = (sAddr shr 8) and 0xFFu
                        val b2 = (sAddr shr 16) and 0xFFu
                        val b3 = (sAddr shr 24) and 0xFFu
                        val ip = "$b0.$b1.$b2.$b3"
                        if (!ip.startsWith("127.") && !ip.startsWith("169.254.")) {
                            return@memScoped ip
                        }
                    }
                }
                curr = ifa.ifa_next
            }
            null
        } finally {
            freeifaddrs(ifap.value)
        }
    }
}

