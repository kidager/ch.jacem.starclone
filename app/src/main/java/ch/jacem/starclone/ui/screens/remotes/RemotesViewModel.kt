package ch.jacem.starclone.ui.screens.remotes

import android.app.Application
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.preference.PreferenceManager
import ch.jacem.starclone.Items.RemoteItem
import ch.jacem.starclone.R
import ch.jacem.starclone.Rclone
import ch.jacem.starclone.AppShortcutsHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class RemotesViewModel(application: Application) : AndroidViewModel(application) {

    private val context: Context get() = getApplication()
    private val rclone = Rclone(application)
    private val prefs = PreferenceManager.getDefaultSharedPreferences(application)

    var remotes by mutableStateOf<List<RemoteItem>>(emptyList())
        private set
    var isLoading by mutableStateOf(true)
        private set
    var isCompatible by mutableStateOf(true)
        private set
    var hasConfig by mutableStateOf(true)
        private set

    init {
        loadRemotes()
    }

    fun loadRemotes() {
        viewModelScope.launch(Dispatchers.IO) {
            val compatible = rclone.isCompatible

            if (!compatible) {
                withContext(Dispatchers.Main) {
                    isCompatible = false
                    isLoading = false
                }
                return@launch
            }

            val configCreated = rclone.isConfigFileCreated
            val allRemotes = rclone.getRemotes()

            val hiddenKey = context.getString(R.string.shared_preferences_hidden_remotes)
            val hiddenSet = prefs.getStringSet(hiddenKey, emptySet()) ?: emptySet()
            val filtered = allRemotes.filter { it.name !in hiddenSet }.toMutableList()
            RemoteItem.prepareDisplay(context, filtered)
            filtered.sort()

            withContext(Dispatchers.Main) {
                hasConfig = configCreated && allRemotes.isNotEmpty()
                remotes = filtered
                isLoading = false
            }
        }
    }

    fun pinRemote(remote: RemoteItem) {
        val key = context.getString(R.string.shared_preferences_pinned_remotes)
        val pinnedSet = prefs.getStringSet(key, emptySet())?.toMutableSet() ?: mutableSetOf()
        pinnedSet.add(remote.name)
        prefs.edit().putStringSet(key, pinnedSet).apply()
        remote.pin(true)
        remotes = remotes.sorted()
    }

    fun unpinRemote(remote: RemoteItem) {
        val key = context.getString(R.string.shared_preferences_pinned_remotes)
        val pinnedSet = prefs.getStringSet(key, emptySet())?.toMutableSet() ?: mutableSetOf()
        pinnedSet.remove(remote.name)
        prefs.edit().putStringSet(key, pinnedSet).apply()
        remote.pin(false)
        remotes = remotes.sorted()
    }

    fun deleteRemote(remote: RemoteItem) {
        viewModelScope.launch(Dispatchers.IO) {
            rclone.deleteRemote(remote.name)

            val editor = prefs.edit()

            val pinnedKey = context.getString(R.string.shared_preferences_pinned_remotes)
            val pinnedSet = prefs.getStringSet(pinnedKey, emptySet())?.toMutableSet()
            if (pinnedSet != null) {
                pinnedSet.remove(remote.name)
                editor.putStringSet(pinnedKey, HashSet(pinnedSet))
            }

            val hiddenKey = context.getString(R.string.shared_preferences_hidden_remotes)
            val hiddenSet = prefs.getStringSet(hiddenKey, emptySet())?.toMutableSet()
            if (hiddenSet != null) {
                hiddenSet.remove(remote.name)
                editor.putStringSet(hiddenKey, HashSet(hiddenSet))
            }

            val drawerKey = context.getString(R.string.shared_preferences_drawer_pinned_remotes)
            val drawerSet = prefs.getStringSet(drawerKey, emptySet())?.toMutableSet()
            if (drawerSet != null) {
                drawerSet.remove(remote.name)
                editor.putStringSet(drawerKey, HashSet(drawerSet))
            }

            val renamedKey = context.getString(R.string.pref_key_renamed_remotes)
            val renamedSet = prefs.getStringSet(renamedKey, emptySet())?.toMutableSet()
            if (renamedSet != null) {
                renamedSet.remove(remote.name)
                editor.putStringSet(renamedKey, HashSet(renamedSet))
            }
            editor.remove(context.getString(R.string.pref_key_renamed_remote_prefix, remote.name))
            editor.apply()

            AppShortcutsHelper.removeAppShortcut(context, remote.name)

            withContext(Dispatchers.Main) {
                remotes = remotes.filter { it.name != remote.name }
                if (remotes.isEmpty()) hasConfig = false
            }
        }
    }

    fun renameRemote(remote: RemoteItem, newName: String) {
        val editor = prefs.edit()
        val renamedKey = context.getString(R.string.pref_key_renamed_remotes)
        val renamedSet = prefs.getStringSet(renamedKey, emptySet())?.toMutableSet() ?: mutableSetOf()

        if (newName.isBlank()) {
            renamedSet.remove(remote.name)
            editor.remove(context.getString(R.string.pref_key_renamed_remote_prefix, remote.name))
            remote.setDisplayName(null)
        } else {
            renamedSet.add(remote.name)
            editor.putString(
                context.getString(R.string.pref_key_renamed_remote_prefix, remote.name),
                newName,
            )
            remote.setDisplayName(newName)
        }
        editor.putStringSet(renamedKey, HashSet(renamedSet)).apply()
        remotes = remotes.toList()
    }
}
