package ca.pkay.rcloneexplorer.ui.screens.settings

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import ca.pkay.rcloneexplorer.Activities.MainActivity
import ca.pkay.rcloneexplorer.R
import ca.pkay.rcloneexplorer.Settings.SettingsFragment as OldSettingsFragment
import ca.pkay.rcloneexplorer.ui.theme.StarCloneTheme

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
                    onImport = {
                        (activity as? MainActivity)?.startConfigImportFlow()
                    },
                    onExport = {
                        (activity as? MainActivity)?.startConfigExportFlow()
                    },
                )
            }
        }
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
