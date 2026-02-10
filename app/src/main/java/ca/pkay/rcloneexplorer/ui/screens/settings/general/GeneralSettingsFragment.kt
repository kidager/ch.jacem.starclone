package ca.pkay.rcloneexplorer.ui.screens.settings.general

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import ca.pkay.rcloneexplorer.R
import ca.pkay.rcloneexplorer.ui.theme.StarCloneTheme

class GeneralSettingsFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ) = ComposeView(requireContext()).apply {
        setContent {
            StarCloneTheme {
                GeneralSettingsScreen()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        activity?.title = getString(R.string.pref_header_general)
    }
}
