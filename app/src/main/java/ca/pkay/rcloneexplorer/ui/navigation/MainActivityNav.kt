package ca.pkay.rcloneexplorer.ui.navigation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import ca.pkay.rcloneexplorer.ui.theme.StarCloneTheme

private var selectedTab by mutableStateOf(BottomNavTab.REMOTES)

fun setupBottomNav(
    composeView: ComposeView,
    owner: LifecycleOwner,
    onTabSelected: (BottomNavTab) -> Unit,
) {
    composeView.setViewTreeLifecycleOwner(owner)
    if (owner is SavedStateRegistryOwner) {
        composeView.setViewTreeSavedStateRegistryOwner(owner)
    }
    composeView.setContent {
        StarCloneTheme {
            StarCloneBottomNav(
                selectedTab = selectedTab,
                onTabSelected = { tab ->
                    selectedTab = tab
                    onTabSelected(tab)
                },
            )
        }
    }
}
