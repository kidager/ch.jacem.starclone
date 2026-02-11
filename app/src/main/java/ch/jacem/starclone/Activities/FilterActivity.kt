package ch.jacem.starclone.Activities

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import ch.jacem.starclone.Database.DatabaseHandler
import ch.jacem.starclone.Items.Filter
import ch.jacem.starclone.Items.FilterEntry
import ch.jacem.starclone.R
import ch.jacem.starclone.RuntimeConfiguration
import ch.jacem.starclone.ui.screens.filter.FilterEntryState
import ch.jacem.starclone.ui.screens.filter.FilterScreen
import ch.jacem.starclone.ui.theme.StarCloneTheme
import ch.jacem.starclone.util.ActivityHelper
import es.dmoral.toasty.Toasty

class FilterActivity : AppCompatActivity() {

    companion object {
        const val ID_EXTRA = "FILTER_EDIT_ID"
        const val SAVED_FILTER_ID_EXTRA = "filterId"
    }

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(RuntimeConfiguration.attach(this, newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ActivityHelper.applyTheme(this)
        setContentView(R.layout.activity_compose_shell)
        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val dbHandler = DatabaseHandler(this)
        val filterId = intent.extras?.getLong(ID_EXTRA) ?: 0L
        var existingFilter: Filter? = null

        if (filterId != 0L) {
            existingFilter = dbHandler.getFilter(filterId)
            if (existingFilter == null) {
                Toasty.error(this, getString(R.string.filteractivity_filter_not_found)).show()
                finish()
                return
            }
        }

        val initialTitle = existingFilter?.title ?: ""
        val initialEntries = existingFilter?.getFilters()?.map {
            FilterEntryState(it.filterType, it.filter)
        } ?: emptyList()

        findViewById<androidx.compose.ui.platform.ComposeView>(R.id.compose_view).setContent {
            StarCloneTheme {
                FilterScreen(
                    title = initialTitle,
                    entries = initialEntries,
                    onSave = { title, entries ->
                        val filter = if (existingFilter != null) {
                            Filter(existingFilter.id)
                        } else {
                            Filter(0)
                        }
                        filter.title = title
                        filter.setFilters(entries.map { FilterEntry(it.filterType, it.filter) })

                        val savedFilter = if (existingFilter != null) {
                            dbHandler.updateFilter(filter)
                            filter
                        } else {
                            dbHandler.createFilter(filter)
                        }
                        val resultIntent = Intent().putExtra(SAVED_FILTER_ID_EXTRA, savedFilter.id)
                        setResult(Activity.RESULT_OK, resultIntent)
                        finish()
                    },
                )
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}
