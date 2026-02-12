package ch.jacem.starclone.ui.navigation

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import ch.jacem.starclone.ui.screens.logs.LogsFragment
import ch.jacem.starclone.ui.screens.processes.ProcessesFragment
import ch.jacem.starclone.ui.screens.remotes.RemotesContainerFragment
import ch.jacem.starclone.ui.screens.settings.SettingsContainerFragment
import ch.jacem.starclone.ui.screens.tasks.TasksFragment

class MainPagerAdapter(activity: FragmentActivity) : FragmentStateAdapter(activity) {

    override fun getItemCount(): Int = BottomNavTab.entries.size

    override fun createFragment(position: Int): Fragment = when (position) {
        0 -> RemotesContainerFragment.newInstance()
        1 -> TasksFragment.newInstance()
        2 -> ProcessesFragment.newInstance()
        3 -> LogsFragment.newInstance()
        4 -> SettingsContainerFragment.newInstance()
        else -> throw IllegalArgumentException("Invalid page position: $position")
    }
}
