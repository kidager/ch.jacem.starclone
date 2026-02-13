package ch.jacem.starclone.ui.screens.settings.logging

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import ch.jacem.starclone.R
import ch.jacem.starclone.ui.theme.StarCloneTheme

class LoggingSettingsFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ) = ComposeView(requireContext()).apply {
        setContent {
            StarCloneTheme {
                LoggingSettingsScreen(
                    onNavigateBack = { parentFragmentManager.popBackStack() },
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        activity?.title = getString(R.string.logging_settings_header)
    }
}
