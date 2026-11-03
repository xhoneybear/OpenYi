package dev.lisek.openyi

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform

expect fun getLocalIp(): String?
