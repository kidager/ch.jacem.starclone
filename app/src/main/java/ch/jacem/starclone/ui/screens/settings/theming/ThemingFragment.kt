package ch.jacem.starclone.ui.screens.settings.theming

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import ch.jacem.starclone.R
import ch.jacem.starclone.ui.theme.StarCloneTheme

class ThemingFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ) = ComposeView(requireContext()).apply {
        setContent {
            StarCloneTheme {
                ThemingScreen(
                    onThemeChanged = { activity?.recreate() },
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        activity?.title = getString(R.string.look_and_feel)
    }
}
