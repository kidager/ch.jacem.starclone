package ch.jacem.starclone.ui.screens.remotes

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import ch.jacem.starclone.Activities.MainActivity
import ch.jacem.starclone.R
import ch.jacem.starclone.util.ActivityHelper
import ch.jacem.starclone.Dialogs.RemotePropertiesDialog
import ch.jacem.starclone.Items.RemoteItem
import ch.jacem.starclone.RemoteConfig.RemoteConfig
import ch.jacem.starclone.ui.theme.StarCloneTheme
import ch.jacem.starclone.AppShortcutsHelper

class RemotesFragment : Fragment() {

    private val viewModel: RemotesViewModel by viewModels()

    private val configLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) {
        viewModel.loadRemotes()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ) = ComposeView(requireContext()).apply {
        setContent {
            StarCloneTheme {
                RemotesScreen(
                    viewModel = viewModel,
                    onRemoteClick = { remote ->
                        (requireActivity() as MainActivity).onRemoteClick(remote)
                    },
                    onAddRemote = {
                        configLauncher.launch(Intent(context, RemoteConfig::class.java))
                    },
                    onEditRemote = { remote ->
                        configLauncher.launch(
                            Intent(context, RemoteConfig::class.java).apply {
                                putExtra(RemoteConfig.CONFIG_EDIT_TARGET, remote.name)
                            },
                        )
                    },
                    onShowProperties = { remote ->
                        showRemotePropertiesDialog(remote)
                    },
                    onAddToHomeScreen = if (AppShortcutsHelper.isRequestPinShortcutSupported(requireContext())) {
                        { remote -> AppShortcutsHelper.addRemoteToHomeScreen(requireContext(), remote) }
                    } else {
                        null
                    },
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        activity?.title = getString(R.string.remotes)
    }

    private fun showRemotePropertiesDialog(remote: RemoteItem) {
        val isDark = ActivityHelper.isDarkTheme(requireActivity())
        val dialog = RemotePropertiesDialog.newInstance(remote, isDark)
        dialog.show(childFragmentManager, "remote_properties")
    }

    companion object {
        @JvmStatic
        fun newInstance() = RemotesFragment()
    }
}
