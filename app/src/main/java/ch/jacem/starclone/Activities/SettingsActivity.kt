package ch.jacem.starclone.Activities

import android.content.Context
import android.os.Bundle
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.addCallback
import androidx.appcompat.app.AppCompatActivity
import ch.jacem.starclone.RuntimeConfiguration
import ch.jacem.starclone.ui.screens.settings.OnSettingCategorySelectedListener
import ch.jacem.starclone.ui.screens.settings.SettingsContainerFragment
import ch.jacem.starclone.util.ActivityHelper

class SettingsActivity : AppCompatActivity(), OnSettingCategorySelectedListener {

    private lateinit var container: SettingsContainerFragment

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(RuntimeConfiguration.attach(this, newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Back gestures don't call onBackPressed from API 36 on
        onBackPressedDispatcher.addCallback(this) {
            if (!container.onBackPressed()) {
                isEnabled = false
                onBackPressedDispatcher.onBackPressed()
                isEnabled = true
            }
        }
        ActivityHelper.applyTheme(this)

        val frameLayout = FrameLayout(this).apply {
            id = android.R.id.content
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            )
        }
        setContentView(frameLayout)

        if (savedInstanceState == null) {
            container = SettingsContainerFragment.newInstance()
            supportFragmentManager.beginTransaction()
                .replace(android.R.id.content, container, TAG_CONTAINER)
                .commit()
        } else {
            container = supportFragmentManager
                .findFragmentByTag(TAG_CONTAINER) as SettingsContainerFragment
        }
    }

    override fun onSettingCategoryClicked(category: Int) {
        container.onSettingCategoryClicked(category)
    }

    companion object {
        private const val TAG_CONTAINER = "settings_container"
    }
}
