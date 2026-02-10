package ca.pkay.rcloneexplorer.ui.screens.settings.theming

import android.content.SharedPreferences
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.preference.PreferenceManager
import ca.pkay.rcloneexplorer.R
import ca.pkay.rcloneexplorer.ui.components.StarCloneListItem

@Composable
fun ThemingScreen(onThemeChanged: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { PreferenceManager.getDefaultSharedPreferences(context) }
    val themeKey = stringResource(R.string.pref_key_theme)
    val wrapKey = stringResource(R.string.pref_key_wrap_filenames)

    var currentTheme by remember {
        mutableStateOf(prefs.getString(themeKey, "0") ?: "0")
    }
    var wrapFilenames by remember {
        mutableStateOf(prefs.getBoolean(wrapKey, true))
    }
    var showThemeDialog by remember { mutableStateOf(false) }

    val themeLabels = listOf("Follow System Theme", "Dark theme", "Light Mode")
    val themeValues = listOf("0", "1", "2")
    val currentThemeLabel = themeLabels.getOrElse(themeValues.indexOf(currentTheme)) { themeLabels[0] }

    LazyColumn(
        contentPadding = PaddingValues(vertical = 8.dp),
    ) {
        item {
            StarCloneListItem(
                headlineText = stringResource(R.string.theme_title),
                supportingText = currentThemeLabel,
                onClick = { showThemeDialog = true },
            )
        }
        item {
            StarCloneListItem(
                headlineText = stringResource(R.string.wrap_filenames),
                supportingText = stringResource(R.string.wrap_filenames_summary),
                trailingContent = {
                    Switch(
                        checked = wrapFilenames,
                        onCheckedChange = null,
                    )
                },
                onClick = {
                    wrapFilenames = !wrapFilenames
                    prefs.edit().putBoolean(wrapKey, wrapFilenames).apply()
                },
            )
        }
        item { Spacer(Modifier.height(80.dp)) }
    }

    if (showThemeDialog) {
        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            title = { Text(stringResource(R.string.theme_title)) },
            text = {
                Column {
                    themeLabels.forEachIndexed { index, label ->
                        StarCloneListItem(
                            headlineText = label,
                            trailingContent = {
                                RadioButton(
                                    selected = currentTheme == themeValues[index],
                                    onClick = null,
                                )
                            },
                            onClick = {
                                currentTheme = themeValues[index]
                                prefs.edit().putString(themeKey, themeValues[index]).apply()
                                showThemeDialog = false
                                onThemeChanged()
                            },
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showThemeDialog = false }) { Text("Cancel") }
            },
        )
    }
}
