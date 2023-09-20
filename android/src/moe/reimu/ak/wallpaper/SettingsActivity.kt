package moe.reimu.ak.wallpaper

import android.app.WallpaperManager
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.google.android.gms.oss.licenses.OssLicensesMenuActivity


class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.settings_activity)
        if (savedInstanceState == null) {
            supportFragmentManager
                .beginTransaction()
                .replace(R.id.settings, SettingsFragment())
                .commit()
        }
    }

    fun openLwpSetter() {
        val intent = Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER)
        intent.putExtra(
            WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT, ComponentName(
                this,
                LiveWallpaper::class.java
            )
        )

        try {
            this.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(this, R.string.error_wpsetter, Toast.LENGTH_LONG).show()
        }
    }

    fun openLicensesActivity() {
        startActivity(Intent(this, OssLicensesMenuActivity::class.java))
    }

    fun notifyChangeCharacter() {
        Intent().also { intent ->
            intent.action = LiveWallpaper.ACTION_CHARACTER_UPDATED
            sendBroadcast(intent)
        }
    }

    class SettingsFragment : PreferenceFragmentCompat() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey)

            findPreference<Preference>("openSet")!!.setOnPreferenceClickListener {
                (requireActivity() as SettingsActivity).openLwpSetter()
                true
            }

            findPreference<Preference>("ossLicenses")!!.setOnPreferenceClickListener {
                (requireActivity() as SettingsActivity).openLicensesActivity()
                true
            }

            findPreference<ListPreference>(getString(R.string.pref_key_char))!!.onPreferenceChangeListener =
                Preference.OnPreferenceChangeListener { _, _ ->
                    (requireActivity() as SettingsActivity).notifyChangeCharacter()
                    true
                }
        }
    }
}