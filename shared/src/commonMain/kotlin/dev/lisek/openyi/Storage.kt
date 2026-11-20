package dev.lisek.openyi

import com.russhwolf.settings.Settings

class AppStorage(
    private val settings: Settings = Settings() // zero parameters required
) {
    var cameraHost: String
        get() = settings.getString("camera_host", defaultValue = CameraClient.DEFAULT_HOST)
        set(value) = settings.putString("camera_host", value)

    var showIntro: Boolean
        get() = settings.getBoolean("showIntro", defaultValue = true)
        set(value) = settings.putBoolean("showIntro", value)

    fun clearAll() {
        settings.clear()
    }
}
