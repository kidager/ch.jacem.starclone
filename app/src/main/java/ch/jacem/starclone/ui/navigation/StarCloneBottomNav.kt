package ch.jacem.starclone.ui.navigation

import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun StarCloneBottomNav(
    selectedTab: BottomNavTab,
    onTabSelected: (BottomNavTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    NavigationBar(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        BottomNavTab.entries.forEach { tab ->
            NavigationBarItem(
                icon = { Icon(imageVector = tab.icon, contentDescription = tab.label) },
                label = { Text(tab.label) },
                selected = selectedTab == tab,
                onClick = { onTabSelected(tab) },
            )
        }
    }
}
