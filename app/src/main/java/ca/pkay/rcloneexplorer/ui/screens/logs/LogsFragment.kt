package ca.pkay.rcloneexplorer.ui.screens.logs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import ca.pkay.rcloneexplorer.R
import ca.pkay.rcloneexplorer.ui.theme.StarCloneTheme

class LogsFragment : Fragment() {

    private val viewModel: LogsViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ) = ComposeView(requireContext()).apply {
        setContent {
            StarCloneTheme {
                LogsScreen(viewModel = viewModel)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        activity?.title = getString(R.string.logFragment)
        viewModel.loadData()
    }

    companion object {
        @JvmStatic
        fun newInstance() = LogsFragment()
    }
}
