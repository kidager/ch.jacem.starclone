package ca.pkay.rcloneexplorer.RemoteConfig

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.SearchView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import ca.pkay.rcloneexplorer.Activities.MainActivity
import ca.pkay.rcloneexplorer.R
import ca.pkay.rcloneexplorer.Rclone
import ca.pkay.rcloneexplorer.RuntimeConfiguration
import ca.pkay.rcloneexplorer.rclone.Provider
import ca.pkay.rcloneexplorer.ui.screens.remoteconfig.AuthScreen
import ca.pkay.rcloneexplorer.ui.screens.remoteconfig.ProviderListScreen
import ca.pkay.rcloneexplorer.ui.screens.remoteconfig.RemoteConfigFormScreen
import ca.pkay.rcloneexplorer.ui.theme.StarCloneTheme
import ca.pkay.rcloneexplorer.util.ActivityHelper
import es.dmoral.toasty.Toasty
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class RemoteConfig : AppCompatActivity() {

    companion object {
        const val CONFIG_EDIT_CODE = 139
        const val CONFIG_EDIT_TARGET = "CONFIG_EDIT_TARGET"
    }

    private enum class Screen { ProviderList, ConfigForm, Auth }

    private var currentScreen by mutableStateOf(Screen.ProviderList)
    private var searchQuery by mutableStateOf("")
    private var showAdvanced by mutableStateOf(false)
    private var selectedProvider by mutableStateOf<Provider?>(null)
    private var isEdit by mutableStateOf(false)
    private var existingValues by mutableStateOf<Map<String, String>>(emptyMap())
    private var useOauth by mutableStateOf(false)
    private var providers by mutableStateOf<List<Provider>>(emptyList())
    private var isAuthenticating by mutableStateOf(false)

    private lateinit var mSearchBar: SearchView
    private var mSearchIcon: MenuItem? = null
    private var mAdvancedIcon: MenuItem? = null

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(RuntimeConfiguration.attach(this, newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ActivityHelper.applyTheme(this)
        setContentView(R.layout.activity_remote_config)
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        mSearchBar = findViewById(R.id.search)

        val rclone = Rclone(this)
        providers = rclone.providers ?: emptyList()

        val editTarget = intent.getStringExtra(CONFIG_EDIT_TARGET)
        if (!editTarget.isNullOrEmpty()) {
            val config = rclone.getConfig(editTarget)
            if (config != null) {
                val provider = rclone.getProvider(config["type"])
                if (provider != null) {
                    selectedProvider = provider
                    isEdit = true
                    existingValues = config.toMap()
                    useOauth = isOauthProvider(provider.name)
                    currentScreen = Screen.ConfigForm
                    supportActionBar?.title = provider.getNameCapitalized()
                }
            }
        }

        mSearchBar.setOnCloseListener {
            toggleSearch(false)
            true
        }
        mSearchBar.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextChange(newText: String): Boolean {
                searchQuery = newText
                return true
            }
            override fun onQueryTextSubmit(query: String): Boolean = true
        })

        findViewById<ComposeView>(R.id.compose_view).setContent {
            StarCloneTheme {
                when {
                    isAuthenticating -> AuthScreen(
                        onCancel = {
                            // Auth cancellation handled by the coroutine
                            finish()
                        },
                    )
                    currentScreen == Screen.ProviderList -> ProviderListScreen(
                        providers = providers,
                        searchQuery = searchQuery,
                        onProviderSelected = ::onProviderSelected,
                        preselection = selectedProvider?.name,
                    )
                    currentScreen == Screen.ConfigForm -> {
                        selectedProvider?.let { provider ->
                            RemoteConfigFormScreen(
                                provider = provider,
                                isEdit = isEdit,
                                existingValues = existingValues,
                                useOauth = useOauth,
                                searchQuery = searchQuery,
                                showAdvanced = showAdvanced,
                                onSave = ::onSaveRemote,
                            )
                        }
                    }
                }
            }
        }
    }

    private fun onProviderSelected(provider: Provider) {
        toggleSearch(false)
        selectedProvider = provider
        useOauth = isOauthProvider(provider.name)
        currentScreen = Screen.ConfigForm
        supportActionBar?.title = provider.getNameCapitalized()
        invalidateOptionsMenu()
    }

    private fun onSaveRemote(remoteName: String, options: Map<String, String>) {
        val optionList = ArrayList<String>()
        optionList.add(remoteName)
        optionList.add(selectedProvider!!.name)

        if (isEdit && useOauth) {
            optionList.add("config_refresh_token")
            optionList.add("false")
        }
        for ((key, value) in options) {
            optionList.add(key)
            optionList.add(value)
        }

        if (isEdit) {
            RemoteConfigHelper.updateAndWait(this, optionList)
            finish()
            return
        }

        if (useOauth) {
            isAuthenticating = true
            val context = this
            val rclone = Rclone(this)
            CoroutineScope(Dispatchers.IO).launch {
                val success = OauthHelper.createOptionsWithOauth(optionList, rclone, context)
                withContext(Dispatchers.Main) {
                    if (!success) {
                        Toasty.error(context, getString(R.string.error_creating_remote), Toast.LENGTH_SHORT, true).show()
                    } else {
                        Toasty.success(context, getString(R.string.remote_creation_success), Toast.LENGTH_SHORT, true).show()
                    }
                    val intent = Intent(context, MainActivity::class.java)
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    startActivity(intent)
                }
            }
        } else {
            RemoteConfigHelper.setupAndWait(this, optionList)
            finish()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.remote_config_menu, menu)
        mSearchIcon = menu?.findItem(R.id.app_bar_search)

        if (currentScreen == Screen.ConfigForm) {
            menuInflater.inflate(R.menu.fragment_config_menu, menu)
            mAdvancedIcon = menu?.findItem(R.id.action_toggle_advanced)
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.app_bar_search -> {
                toggleSearch(true)
                true
            }
            R.id.action_toggle_advanced -> {
                showAdvanced = !showAdvanced
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun toggleSearch(show: Boolean) {
        if (show) {
            mSearchBar.visibility = View.VISIBLE
            mSearchBar.requestFocus()
            mSearchBar.isFocusable = true
            mSearchBar.isIconified = false
            mSearchBar.requestFocusFromTouch()
            mSearchIcon?.isVisible = false
        } else {
            mSearchBar.visibility = View.GONE
            mSearchBar.setQuery("", false)
            searchQuery = ""
            mSearchIcon?.isVisible = true
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        handleBackAction()
        return true
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        handleBackAction()
    }

    private fun handleBackAction() {
        if (mSearchBar.visibility == View.VISIBLE) {
            toggleSearch(false)
            return
        }
        when {
            currentScreen == Screen.ProviderList -> finish()
            currentScreen == Screen.ConfigForm && isEdit -> finish()
            currentScreen == Screen.ConfigForm -> {
                currentScreen = Screen.ProviderList
                supportActionBar?.setTitle(R.string.title_activity_remote_config)
                showAdvanced = false
                searchQuery = ""
                invalidateOptionsMenu()
            }
            else -> finish()
        }
    }

    private fun isOauthProvider(name: String): Boolean {
        return name in listOf("box", "dropbox", "pcloud", "yandex", "drive", "google photos", "onedrive")
    }
}
