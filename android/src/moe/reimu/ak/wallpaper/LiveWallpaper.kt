package moe.reimu.ak.wallpaper

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import androidx.preference.PreferenceManager
import com.badlogic.gdx.backends.android.*

class LiveWallpaper : AndroidLiveWallpaperService() {
    private lateinit var wallpaper: AkWallpaperListener
    private lateinit var bcastReceiver: BroadcastReceiver

    override fun onCreateApplication() {
        super.onCreateApplication()

        val config = AndroidApplicationConfiguration().apply {
            useAccelerometer = false
            useCompass = false
            useGyroscope = false
            numSamples = 2
        }

        wallpaper = AkWallpaperListener(this,)

        PreferenceManager.getDefaultSharedPreferences(this).apply {
            val savedZoom = getFloat(getString(R.string.pref_key_zoom), 1f)
            val savedTranslateX = getFloat(getString(R.string.pref_key_translate_x), 0f)
            val savedTranslateY = getFloat(getString(R.string.pref_key_translate_y), 0f)
            wallpaper.applySavedCamera(savedZoom, savedTranslateX, savedTranslateY)
        }

        val filter = IntentFilter(ACTION_CHARACTER_UPDATED)
        bcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                loadCharacter()
            }
        }
        registerReceiver(bcastReceiver, filter)

        initialize(wallpaper, config)
    }

    fun loadCharacter() {
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        val character = prefs.getString(
            getString(R.string.pref_key_char),
            getString(R.string.pref_default_char)
        )!!
        Log.d(TAG, "Loading character $character from preferences")
        wallpaper.loadCharacter(character)
    }

    override fun onDestroy() {
        unregisterReceiver(bcastReceiver)
        super.onDestroy()
    }

    override fun onCreateEngine(): Engine {
        Log.d(TAG, "Engine created")
        return AkEgine()
    }

    // Hack to communicate pause and resume to the app
    inner class AkEgine: AndroidWallpaperEngine() {
        override fun onPause() {
            super.onPause()
            wallpaper.pause()
        }

        override fun onResume() {
            super.onResume()
            wallpaper.resume()
        }
    }


    class AkWallpaperListener(private val context: LiveWallpaper) :
        AkWallpaper(),
        AndroidWallpaperListener {

        override fun create() {
            super.create()
            // Load character after initialization
            context.loadCharacter()
        }

        override fun offsetChange(
            xOffset: Float,
            yOffset: Float,
            xOffsetStep: Float,
            yOffsetStep: Float,
            xPixelOffset: Int,
            yPixelOffset: Int
        ) {
            Log.i(
                TAG,
                "offsetChange(xOffset:$xOffset yOffset:$yOffset xOffsetSteep:$xOffsetStep yOffsetStep:$yOffsetStep xPixelOffset:$xPixelOffset yPixelOffset:$yPixelOffset)"
            );
        }

        override fun previewStateChange(isPreview: Boolean) {
            saveSettings()
            Log.i(TAG, "previewStateChange(isPreview:$isPreview)")
            this.isPreview = isPreview
        }

        override fun iconDropped(x: Int, y: Int) {
            Log.i(TAG, "iconDropped ($x, $y)")
        }

        private fun saveSettings() {
            // Only save in preview mode
            if (!isPreview) {
                return
            }

            Log.i(TAG, "Saving settings")
            PreferenceManager.getDefaultSharedPreferences(context).edit().apply {
                putFloat(context.getString(R.string.pref_key_zoom), currentZoom)
                putFloat(context.getString(R.string.pref_key_translate_x), camera.position.x)
                putFloat(context.getString(R.string.pref_key_translate_y), camera.position.y)
                apply()
            }
        }

        override fun pause() {
            saveSettings()
            super.pause()
        }
    }

    companion object {
        const val TAG = "AkLiveWallpaper"
        const val ACTION_CHARACTER_UPDATED = "moe.reimu.ak.wallpaper.CHAR_UPDATED"
    }
}