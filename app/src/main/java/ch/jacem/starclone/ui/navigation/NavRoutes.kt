package ch.jacem.starclone.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.SyncAlt
import androidx.compose.ui.graphics.vector.ImageVector

enum class BottomNavTab(
    val route: String,
    val label: String,
    val icon: ImageVector,
) {
    REMOTES("remotes", "Remotes", Icons.Outlined.Cloud),
    TASKS("tasks", "Tasks", Icons.Outlined.SyncAlt),
    PROCESSES("processes", "Processes", Icons.Outlined.Speed),
    LOGS("logs", "Logs", Icons.Outlined.History),
    SETTINGS("settings", "Settings", Icons.Outlined.Settings),
}
