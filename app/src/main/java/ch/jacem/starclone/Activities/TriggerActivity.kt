package ch.jacem.starclone.Activities

import android.content.Context
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.ui.platform.ComposeView
import ch.jacem.starclone.R
import ch.jacem.starclone.RuntimeConfiguration
import ch.jacem.starclone.ui.screens.tasks.edit.TriggerEditScreen
import ch.jacem.starclone.ui.screens.tasks.edit.TriggerEditViewModel
import ch.jacem.starclone.ui.theme.StarCloneTheme
import ch.jacem.starclone.util.ActivityHelper

class TriggerActivity : AppCompatActivity() {

    companion object {
        const val ID_EXTRA = "TRIGGER_EDIT_ID"
    }

    private val viewModel by viewModels<TriggerEditViewModel>()

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(RuntimeConfiguration.attach(this, newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ActivityHelper.applyTheme(this)
        setContentView(R.layout.activity_compose_shell)
        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val triggerId = intent.getLongExtra(ID_EXTRA, 0)
        supportActionBar?.title = getString(
            if (triggerId != 0L) R.string.trigger_edit_title_edit else R.string.trigger_edit_title_new,
        )

        if (savedInstanceState == null) {
            viewModel.loadTrigger(triggerId)
        }

        findViewById<ComposeView>(R.id.compose_view).setContent {
            StarCloneTheme {
                TriggerEditScreen(
                    viewModel = viewModel,
                    onSave = {
                        if (viewModel.save()) {
                            finish()
                        }
                    },
                )
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
