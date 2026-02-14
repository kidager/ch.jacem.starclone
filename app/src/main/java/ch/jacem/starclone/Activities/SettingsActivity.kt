package ch.jacem.starclone.Activities

import android.content.Context
import android.os.Bundle
import android.view.ViewGroup
import android.widget.FrameLayout
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

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (!container.onBackPressed()) {
            super.onBackPressed()
        }
    }

    companion object {
        private const val TAG_CONTAINER = "settings_container"
    }
}
