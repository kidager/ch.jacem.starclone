package ch.jacem.starclone.ui.screens.tasks

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import ch.jacem.starclone.Activities.TaskActivity
import ch.jacem.starclone.Activities.TriggerActivity
import ch.jacem.starclone.R
import ch.jacem.starclone.ui.theme.StarCloneTheme

class TasksFragment : Fragment() {

    private val viewModel: TasksViewModel by viewModels()

    private val taskLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) {
        viewModel.loadData()
    }

    private val triggerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) {
        viewModel.loadData()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ) = ComposeView(requireContext()).apply {
        setContent {
            StarCloneTheme {
                TasksScreen(
                    viewModel = viewModel,
                    onAddTask = {
                        taskLauncher.launch(Intent(context, TaskActivity::class.java))
                    },
                    onEditTask = { task ->
                        taskLauncher.launch(
                            Intent(context, TaskActivity::class.java).apply {
                                putExtra(TaskActivity.ID_EXTRA, task.id)
                            },
                        )
                    },
                    onAddTrigger = {
                        triggerLauncher.launch(Intent(context, TriggerActivity::class.java))
                    },
                    onEditTrigger = { trigger ->
                        triggerLauncher.launch(
                            Intent(context, TriggerActivity::class.java).apply {
                                putExtra(TriggerActivity.ID_EXTRA, trigger.id)
                            },
                        )
                    },
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        activity?.title = getString(R.string.tasks)
        viewModel.loadData()
    }

    companion object {
        @JvmStatic
        fun newInstance() = TasksFragment()
    }
}
