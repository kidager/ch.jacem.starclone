package ch.jacem.starclone.ui.screens.settings

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import ch.jacem.starclone.Activities.BackupActivity
import ch.jacem.starclone.R
import ch.jacem.starclone.Settings.SettingsFragment as OldSettingsFragment
import ch.jacem.starclone.ui.theme.StarCloneTheme

class SettingsFragment : Fragment() {

    private var clickListener: OldSettingsFragment.OnSettingCategorySelectedListener? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is OldSettingsFragment.OnSettingCategorySelectedListener) {
            clickListener = context
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ) = ComposeView(requireContext()).apply {
        setContent {
            StarCloneTheme {
                SettingsScreen(
                    onCategoryClick = { category ->
                        clickListener?.onSettingCategoryClicked(category)
                    },
                    onImport = { launchBackup("import") },
                    onExport = { launchBackup("export") },
                )
            }
        }
    }

    private fun launchBackup(mode: String) {
        startActivity(Intent(requireContext(), BackupActivity::class.java).apply {
            putExtra(BackupActivity.EXTRA_MODE, mode)
        })
    }

    override fun onResume() {
        super.onResume()
        activity?.title = getString(R.string.settings)
    }

    companion object {
        @JvmStatic
        fun newInstance() = SettingsFragment()
    }
}
