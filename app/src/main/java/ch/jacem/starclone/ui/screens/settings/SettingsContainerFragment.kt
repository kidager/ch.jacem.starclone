package ch.jacem.starclone.ui.screens.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.fragment.app.Fragment
import ch.jacem.starclone.Settings.SettingsFragment as OldSettingsFragment
import ch.jacem.starclone.ui.screens.settings.fileaccess.FileAccessFragment
import ch.jacem.starclone.ui.screens.settings.general.GeneralSettingsFragment
import ch.jacem.starclone.ui.screens.settings.logging.LoggingSettingsFragment
import ch.jacem.starclone.ui.screens.settings.notifications.NotificationSettingsFragment
import ch.jacem.starclone.ui.screens.settings.theming.ThemingFragment

class SettingsContainerFragment : Fragment(),
    OldSettingsFragment.OnSettingCategorySelectedListener {

    private var containerId = View.NO_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        containerId = savedInstanceState?.getInt(KEY_CONTAINER_ID)
            ?: View.generateViewId()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        return FrameLayout(requireContext()).apply {
            id = containerId
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            )
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (savedInstanceState == null) {
            childFragmentManager.beginTransaction()
                .replace(containerId, SettingsFragment.newInstance())
                .commit()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(KEY_CONTAINER_ID, containerId)
    }

    override fun onSettingCategoryClicked(category: Int) {
        val subFragment: Fragment = when (category) {
            OldSettingsFragment.GENERAL_SETTINGS -> GeneralSettingsFragment()
            OldSettingsFragment.FILE_ACCESS_SETTINGS -> FileAccessFragment()
            OldSettingsFragment.LOOK_AND_FEEL_SETTINGS -> ThemingFragment()
            OldSettingsFragment.LOGGING_SETTINGS -> LoggingSettingsFragment()
            OldSettingsFragment.NOTIFICATION_SETTINGS -> NotificationSettingsFragment()
            else -> return
        }
        childFragmentManager.beginTransaction()
            .replace(containerId, subFragment)
            .addToBackStack(null)
            .commit()
    }

    fun resetToSettingsList() {
        val fm = childFragmentManager
        for (i in 0 until fm.backStackEntryCount) {
            fm.popBackStack()
        }
    }

    fun onBackPressed(): Boolean {
        if (childFragmentManager.backStackEntryCount > 0) {
            childFragmentManager.popBackStack()
            return true
        }
        return false
    }

    companion object {
        private const val KEY_CONTAINER_ID = "container_id"

        @JvmStatic
        fun newInstance() = SettingsContainerFragment()
    }
}
