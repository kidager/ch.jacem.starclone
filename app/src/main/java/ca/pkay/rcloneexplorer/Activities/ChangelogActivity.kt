package ca.pkay.rcloneexplorer.Activities

import android.content.Context
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import ca.pkay.rcloneexplorer.R
import ca.pkay.rcloneexplorer.RuntimeConfiguration
import ca.pkay.rcloneexplorer.ui.screens.about.MarkdownScreen
import ca.pkay.rcloneexplorer.ui.theme.StarCloneTheme
import ca.pkay.rcloneexplorer.util.ActivityHelper

class ChangelogActivity : AppCompatActivity() {

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
            StarCloneTheme { MarkdownScreen(assetPath = "changelog.md") }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}
