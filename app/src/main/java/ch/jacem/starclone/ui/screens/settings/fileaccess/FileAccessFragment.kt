package ch.jacem.starclone.ui.screens.settings.fileaccess

import android.annotation.SuppressLint
import android.content.Intent
import android.content.UriPermission
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import ch.jacem.starclone.BuildConfig
import ch.jacem.starclone.R
import ch.jacem.starclone.ui.theme.StarCloneTheme

class FileAccessFragment : Fragment() {

    private val permissions = mutableStateListOf<UriPermission>()

    private val documentTreeLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val uri = result.data?.data ?: return@registerForActivityResult
            if (uri.authority == BuildConfig.VCP_AUTHORITY) {
                Toast.makeText(requireContext(), getString(R.string.saf_uri_permission_error), Toast.LENGTH_LONG).show()
                return@registerForActivityResult
            }
            val contentResolver = requireContext().contentResolver
            for (perm in contentResolver.persistedUriPermissions) {
                if (uri.toString().startsWith(perm.uri.toString())) {
                    Toast.makeText(requireContext(), getString(R.string.saf_uri_permission_already_added), Toast.LENGTH_LONG).show()
                    return@registerForActivityResult
                }
            }
            if ("com.android.externalstorage.documents" != uri.authority) {
                Toast.makeText(requireContext(), "Third party providers are not tested well, proceed with caution.", Toast.LENGTH_LONG).show()
            }
            val takeFlags = result.data?.flags ?: 0
            contentResolver.takePersistableUriPermission(
                uri,
                (Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION) and takeFlags,
            )
            refreshPermissions()
            Toast.makeText(requireContext(), getString(R.string.saf_uri_permission_added), Toast.LENGTH_SHORT).show()
        }
    }

    private val allFilesLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && Environment.isExternalStorageManager()) {
            Toast.makeText(requireContext(), "You can now access all local storage", Toast.LENGTH_LONG).show()
        } else {
            Toast.makeText(requireContext(), "Manage files not granted", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ) = ComposeView(requireContext()).apply {
        refreshPermissions()
        setContent {
            StarCloneTheme {
                FileAccessScreen(
                    permissions = permissions,
                    onAddStorage = { addRoot() },
                    onRemovePermission = { perm -> removePermission(perm) },
                    onOpenAllFilesSettings = { openAllFilesSettings() },
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        activity?.title = getString(R.string.pref_header_file_access)
        refreshPermissions()
    }

    private fun refreshPermissions() {
        val current = requireContext().contentResolver.persistedUriPermissions
        permissions.clear()
        permissions.addAll(current)
    }

    private fun addRoot() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
            addFlags(Intent.FLAG_GRANT_PREFIX_URI_PERMISSION or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
        }
        documentTreeLauncher.launch(intent)
    }

    private fun removePermission(permission: UriPermission) {
        requireContext().contentResolver.releasePersistableUriPermission(
            permission.uri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
        )
        refreshPermissions()
    }

    @SuppressLint("InlinedApi")
    private fun openAllFilesSettings() {
        val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
            data = Uri.fromParts("package", BuildConfig.APPLICATION_ID, null)
        }
        allFilesLauncher.launch(intent)
    }
}
