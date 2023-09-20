package moe.reimu.ak.wallpaper

import kotlin.jvm.JvmStatic
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application
import moe.reimu.ak.wallpaper.AkWallpaper

// Please note that on macOS your application needs to be started with the -XstartOnFirstThread JVM argument
object DesktopLauncher {
    @JvmStatic
    fun main(arg: Array<String>) {
        val config = Lwjgl3ApplicationConfiguration()
        config.setForegroundFPS(60)
        config.setTitle("Arknights Wallpaper")
        config.setBackBufferConfig(8, 8, 8, 8, 16, 0, 8)
        val akwp = AkWpDesktop().apply {
            isPreview = true
        }
        Lwjgl3Application(akwp, config)
    }

    class AkWpDesktop: AkWallpaper() {
        override fun create() {
            super.create()
            loadCharacter("mizuki_summer_feast")
            loadBackground("bg_anniversary_1")
        }
    }
}