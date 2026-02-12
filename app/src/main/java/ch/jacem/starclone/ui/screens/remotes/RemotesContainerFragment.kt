package ch.jacem.starclone.ui.screens.remotes

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.fragment.app.Fragment
import ch.jacem.starclone.Fragments.FileExplorerFragment
import ch.jacem.starclone.Items.RemoteItem

class RemotesContainerFragment : Fragment() {

    private var containerId = View.NO_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        containerId = savedInstanceState?.getInt(KEY_CONTAINER_ID)
            ?: View.generateViewId()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        return FrameLayout(requireContext()).apply {
            id = containerId
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            )
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (savedInstanceState == null) {
            childFragmentManager.beginTransaction()
                .replace(containerId, RemotesFragment.newInstance())
                .commit()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(KEY_CONTAINER_ID, containerId)
    }

    fun openRemote(remote: RemoteItem) {
        childFragmentManager.beginTransaction()
            .replace(containerId, FileExplorerFragment.newInstance(remote), TAG_FILE_EXPLORER)
            .addToBackStack(null)
            .commit()
    }

    fun resetToRemotesList() {
        val fm = childFragmentManager
        for (i in 0 until fm.backStackEntryCount) {
            fm.popBackStack()
        }
    }

    fun onBackPressed(): Boolean {
        val currentFragment = childFragmentManager.findFragmentByTag(TAG_FILE_EXPLORER)
        if (currentFragment is FileExplorerFragment) {
            if (currentFragment.onBackButtonPressed()) {
                return true
            }
            if (childFragmentManager.backStackEntryCount > 0) {
                childFragmentManager.popBackStack()
                return true
            }
        }
        return childFragmentManager.backStackEntryCount > 0
    }

    companion object {
        private const val TAG_FILE_EXPLORER = "file_explorer"
        private const val KEY_CONTAINER_ID = "container_id"

        @JvmStatic
        fun newInstance() = RemotesContainerFragment()
    }
}
