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
                        val parent = parentFragment
                        if (parent is RemotesContainerFragment) {
                            parent.openRemote(remote)
                        } else {
                            (requireActivity() as MainActivity).onRemoteClick(remote)
                        }
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
                    onReconnectRemote = { remote ->
                        reconnectRemote(remote)
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
        viewModel.loadRemotes()
    }

    private fun reconnectRemote(remote: RemoteItem) {
        val rclone = ch.jacem.starclone.Rclone(requireContext())
        val context = requireContext()
        Thread {
            val process = rclone.reconnectRemote(remote)
            if (process != null) {
                val start = ch.jacem.starclone.InteractiveRunner.Step("y/n> ", ch.jacem.starclone.InteractiveRunner.StringAction("y"))
                val postOauth = start.addFollowing("y/n> ", "y")
                    .addFollowing(ch.jacem.starclone.RemoteConfig.OauthHelper.InitOauthStep(context))
                    .addFollowing(ch.jacem.starclone.RemoteConfig.OauthHelper.OauthFinishStep())

                if (RemoteItem.ONEDRIVE == remote.type) {
                    postOauth.addFollowing("OneDrive Personal or Business", "onedrive")
                        .addFollowing("Chose drive to use:> ", "0")
                        .addFollowing("y/n> ", "y")
                }

                val errorHandler = ch.jacem.starclone.InteractiveRunner.ErrorHandler { e ->
                    ch.jacem.starclone.util.FLog.e("RemotesFragment", "onError: recipe for %s is probably bad", e, remote.typeReadable)
                    process.destroy()
                }

                val runner = ch.jacem.starclone.InteractiveRunner(start, errorHandler, process)
                ch.jacem.starclone.RemoteConfig.OauthHelper.registerRunner(runner)
                runner.runSteps()

                try { process.waitFor() } catch (_: InterruptedException) {}
            }
        }.start()
    }

    companion object {
        @JvmStatic
        fun newInstance() = RemotesFragment()
    }
}
