package ch.jacem.starclone.ui.screens.processes

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import ch.jacem.starclone.R
import ch.jacem.starclone.ui.theme.StarCloneTheme

class ProcessesFragment : Fragment() {

    private val viewModel: ProcessesViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ) = ComposeView(requireContext()).apply {
        setContent {
            StarCloneTheme {
                ProcessesScreen(viewModel = viewModel)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        activity?.title = getString(R.string.processes)
        viewModel.loadHistory()
    }

    fun highlightProcess(workerId: String) {
        viewModel.highlightProcess(workerId)
    }

    companion object {
        @JvmStatic
        fun newInstance() = ProcessesFragment()
    }
}
