package ch.jacem.starclone.Activities

import android.content.Context
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import ch.jacem.starclone.R
import ch.jacem.starclone.RuntimeConfiguration
import ch.jacem.starclone.ui.screens.about.AboutScreen
import ch.jacem.starclone.ui.theme.StarCloneTheme
import ch.jacem.starclone.util.ActivityHelper

class AboutActivity : AppCompatActivity() {

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(RuntimeConfiguration.attach(this, newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ActivityHelper.applyTheme(this)
        setContentView(R.layout.activity_compose_shell)
        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        findViewById<androidx.compose.ui.platform.ComposeView>(R.id.compose_view).setContent {
            StarCloneTheme { AboutScreen() }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}
