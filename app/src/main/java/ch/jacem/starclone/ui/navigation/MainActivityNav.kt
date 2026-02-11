package ch.jacem.starclone.ui.navigation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import ch.jacem.starclone.ui.theme.StarCloneTheme

private var selectedTab by mutableStateOf(BottomNavTab.REMOTES)

fun syncSelectedTab(tab: BottomNavTab) {
    selectedTab = tab
}

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
