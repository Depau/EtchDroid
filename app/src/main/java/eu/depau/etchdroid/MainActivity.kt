package eu.depau.etchdroid

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import dagger.android.AndroidInjection
import dagger.android.DispatchingAndroidInjector
import dagger.android.HasAndroidInjector
import eu.depau.etchdroid.ui.activities.ActivityBase
import eu.depau.etchdroid.ui.misc.DoNotShowAgainDialogFragment
import eu.depau.etchdroid.ui.misc.NightModeHelper
import eu.depau.etchdroid.utils.ktexts.toast
import me.jfenn.attribouter.Attribouter
import javax.inject.Inject

class MainActivity : AppCompatActivity(), HasAndroidInjector {

    @Inject
    lateinit var dispatchingAndroidInjector: DispatchingAndroidInjector<Any>
    private var nightModeHelper: NightModeHelper? = null

    companion object {
        const val DISMISSED_DIALOGS_PREFS = "dismissed_dialogs"
        const val READ_REQUEST_CODE = 42
        const val READ_EXTERNAL_STORAGE_PERMISSION = 29
    }

    var shouldShowAndroidPieAlertDialog: Boolean
        get() {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P)
                return false
            val settings = getSharedPreferences(ActivityBase.DISMISSED_DIALOGS_PREFS, 0)
            return !settings.getBoolean("Android_Pie_alert", false)
        }
        set(value) {
            val settings = getSharedPreferences(ActivityBase.DISMISSED_DIALOGS_PREFS, 0)
            val editor = settings.edit()
            editor.putBoolean("Android_Pie_alert", !value)
            editor.apply()
        }

    internal val isNightMode: Boolean
        get() = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_YES != 0

    override fun onCreate(savedInstanceState: Bundle?) {
        AndroidInjection.inject(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            nightModeHelper = NightModeHelper(this, R.style.AppTheme)
        }
    }

    override fun androidInjector() = dispatchingAndroidInjector

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)

        // Hide night mode menu on Android 10 as it causes weird issues
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            menu!!.findItem(R.id.action_nightmode).isVisible = false
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_about -> {
                Attribouter
                        .from(this)
                        .withFile(R.xml.about)
                        .show()
                return true
            }
            R.id.action_donate -> {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://etchdroid.depau.eu/donate/"))
                startActivity(intent)
                return true
            }
            R.id.action_reset_warnings -> {
                getSharedPreferences(ActivityBase.DISMISSED_DIALOGS_PREFS, 0)
                        .edit().clear().apply()
                toast(getString(R.string.warnings_reset))
                return true
            }
            R.id.action_nightmode -> {
                nightModeHelper?.toggle()
                return true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    fun showAndroidPieAlertDialog(callback: () -> Unit) {
        val dialogFragment = DoNotShowAgainDialogFragment(isNightMode)
        dialogFragment.title = getString(R.string.android_pie_bug)
        dialogFragment.message = getString(R.string.android_pie_bug_dialog_text)
        dialogFragment.positiveButton = getString(R.string.i_understand)
        dialogFragment.listener = object : DoNotShowAgainDialogFragment.DialogListener {
            override fun onDialogNegative(dialog: DoNotShowAgainDialogFragment, showAgain: Boolean) {}
            override fun onDialogPositive(dialog: DoNotShowAgainDialogFragment, showAgain: Boolean) {
                shouldShowAndroidPieAlertDialog = showAgain
                callback()
            }
        }
        dialogFragment.show(supportFragmentManager, "DMGBetaAlertDialogFragment")
    }

    fun checkAndRequestStorageReadPerm(): Boolean {
        if ((ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                            Manifest.permission.READ_EXTERNAL_STORAGE)) {
                toast(getString(R.string.storage_permission_required))
            } else {
                ActivityCompat.requestPermissions(this,
                        arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                        ActivityBase.READ_EXTERNAL_STORAGE_PERMISSION)
            }
        } else {
            // Permission granted
            return true
        }
        return false
    }

}