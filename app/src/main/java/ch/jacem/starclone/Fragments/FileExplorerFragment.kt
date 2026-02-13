package ch.jacem.starclone.Fragments

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.ComposeView
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.preference.PreferenceManager
import ch.jacem.starclone.Activities.MainActivity
import ch.jacem.starclone.BuildConfig
import ch.jacem.starclone.Dialogs.Dialogs
import ch.jacem.starclone.Dialogs.GoToDialog
import ch.jacem.starclone.Dialogs.LoadingDialog
import ch.jacem.starclone.Items.FileItem
import ch.jacem.starclone.Items.RemoteItem
import ch.jacem.starclone.Items.Task
import ch.jacem.starclone.R
import ch.jacem.starclone.Rclone
import ch.jacem.starclone.Services.StreamingService
import ch.jacem.starclone.Services.ThumbnailsLoadingService
import ch.jacem.starclone.ui.screens.fileexplorer.FileExplorerScreen
import ch.jacem.starclone.ui.screens.fileexplorer.FileExplorerViewModel
import ch.jacem.starclone.ui.theme.StarCloneTheme
import ch.jacem.starclone.util.ActivityHelper.tryStartActivity
import ch.jacem.starclone.util.ActivityHelper.tryStartService
import ch.jacem.starclone.util.FLog
import ch.jacem.starclone.util.UriUtils
import ch.jacem.starclone.workmanager.EphemeralTaskManager
import ch.jacem.starclone.workmanager.SyncManager
import es.dmoral.toasty.Toasty
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.IOException
import java.net.ServerSocket
import java.security.SecureRandom

class FileExplorerFragment : Fragment(),
    GoToDialog.Callbacks {

    private val viewModel: FileExplorerViewModel by viewModels()
    private lateinit var remote: RemoteItem
    private lateinit var rclone: Rclone

    private var showThumbnails = false
    private var isThumbnailsServiceRunning = false
    private var thumbnailServerAuth = ""
    private var thumbnailServerPort = 0
    private var goToDefaultSet = false
    private var syncDirection = -1
    private var pendingDownloadFiles: List<FileItem> = emptyList()
    private var pendingCloudUris: List<Uri> = emptyList()

    // --- SAF pickers ---

    private val uploadPicker = registerForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        val ctx = context ?: return@registerForActivityResult
        val resolved = mutableListOf<String>()
        val unresolved = mutableListOf<Uri>()

        for (uri in uris) {
            val path = if (UriUtils.isVcpUri(uri)) {
                UriUtils.getRclonePathFromVcpUri(ctx, uri)
            } else {
                UriUtils.getPathFromDocumentUri(ctx, uri)
            }
            if (path != null) {
                resolved.add(path)
            } else {
                unresolved.add(uri)
            }
        }

        // Queue resolved files immediately
        for (path in resolved) {
            EphemeralTaskManager.queueUpload(ctx, remote, path, viewModel.currentPath)
        }

        // Ask confirmation for unresolved (cloud) files
        if (unresolved.isNotEmpty()) {
            pendingCloudUris = unresolved
            viewModel.showCloudFilesDialog = unresolved.size
        }
    }

    private val downloadDestPicker = registerForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri == null) return@registerForActivityResult
        val ctx = context ?: return@registerForActivityResult
        val path = UriUtils.getPathFromTreeUri(ctx, uri) ?: return@registerForActivityResult
        viewModel.clearSelection()
        for (item in pendingDownloadFiles) {
            EphemeralTaskManager.queueDownload(ctx, remote, item, path)
        }
        pendingDownloadFiles = emptyList()
    }

    private val syncDestPicker = registerForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri == null) return@registerForActivityResult
        val ctx = context ?: return@registerForActivityResult
        val path = UriUtils.getPathFromTreeUri(ctx, uri) ?: return@registerForActivityResult
        val task = Task(-1).apply {
            remoteId = remote.name
            localPath = path
            direction = syncDirection
            remotePath = viewModel.syncRemotePath ?: ""
        }
        SyncManager(ctx).queueEphemeral(task)
    }

    private val streamingLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        context?.stopService(Intent(context, StreamingService::class.java))
    }

    private val backgroundTaskReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val broadcastRemote = intent.getStringExtra(getString(R.string.background_service_broadcast_data_remote))
            val broadcastPath = intent.getStringExtra(getString(R.string.background_service_broadcast_data_path))
            val broadcastPath2 = intent.getStringExtra(getString(R.string.background_service_broadcast_data_path2))

            if (broadcastRemote != viewModel.remoteName) return

            if (broadcastPath != null) {
                viewModel.invalidateCachePath(broadcastPath)
            }
            if (broadcastPath2 != null) {
                viewModel.invalidateCachePath(broadcastPath2)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        remote = arguments?.getParcelable(ARG_REMOTE) ?: return
        rclone = Rclone(requireContext())

        val prefs = PreferenceManager.getDefaultSharedPreferences(requireContext())
        showThumbnails = prefs.getBoolean(getString(R.string.pref_key_show_thumbnails), false)
        goToDefaultSet = prefs.getBoolean(getString(R.string.pref_key_go_to_default_set), false)

        val startAtRoot = if (goToDefaultSet) {
            prefs.getBoolean(getString(R.string.pref_key_start_at_root), false)
        } else false

        viewModel.startAtRoot = startAtRoot
        viewModel.initialize(remote)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        if (showThumbnails) {
            initializeThumbnailParams()
            startThumbnailService()
        }

        // Show SFTP go-to dialog on first launch if no default set
        if (remote.isRemoteType(RemoteItem.SFTP) && !goToDefaultSet && savedInstanceState == null) {
            showSFTPgoToDialog()
        }

        return ComposeView(requireContext()).apply {
            setContent {
                StarCloneTheme {
                    FileExplorerScreen(
                        viewModel = viewModel,
                        onFileClick = { fileItem -> handleFileClick(fileItem) },
                        onUploadFiles = { uploadPicker.launch(arrayOf("*/*")) },
                        onDownloadFiles = { files -> onDownloadFiles(files) },
                        onSyncDirectionSelected = { direction -> onSyncDirectionSelected(direction) },
                        onShowGoToDialog = { showSFTPgoToDialog() },
                        onNavigateBack = { parentFragmentManager.popBackStack() },
                        onCloudFilesConfirmed = { onCloudFilesConfirmed() },
                        onServeConfirmed = { protocol, allowRemoteAccess, user, password ->
                            onServeOptionsSelected(protocol, allowRemoteAccess, user, password)
                        },
                        serveAllowRemoteAccess = getServeAllowRemoteAccess(),
                        onTransferClick = { workerId -> navigateToProcess(workerId) },
                    )
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        registerReceivers()
        if (showThumbnails) startThumbnailService()
    }

    override fun onResume() {
        super.onResume()
    }

    override fun onStop() {
        super.onStop()
        if (isThumbnailsServiceRunning) {
            requireContext().stopService(Intent(requireContext(), ThumbnailsLoadingService::class.java))
            isThumbnailsServiceRunning = false
        }
        LocalBroadcastManager.getInstance(requireContext()).unregisterReceiver(backgroundTaskReceiver)
    }

    override fun onDetach() {
        super.onDetach()
    }

    // --- Back press ---

    fun onBackButtonPressed(): Boolean {
        return viewModel.navigateUp()
    }

    // --- File handling ---

    private fun handleFileClick(fileItem: FileItem) {
        val type = fileItem.mimeType ?: ""
        if (type.startsWith("video/") || type.startsWith("audio/")) {
            streamFile(fileItem)
        } else {
            downloadAndOpen(fileItem)
        }
    }

    private fun downloadAndOpen(fileItem: FileItem, openAs: Int = -1) {
        val loadingDialog = LoadingDialog()
            .setCanCancel(false)
            .setTitle(getString(R.string.loading_file))
            .setNegativeButton(getString(R.string.cancel))

        loadingDialog.show(childFragmentManager, "loading dialog")

        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) {
                val extCacheDirs = ContextCompat.getExternalCacheDirs(requireContext())
                if (extCacheDirs.isEmpty()) return@withContext null

                val saveLocation = extCacheDirs[0].absolutePath
                val fileLocation = "$saveLocation/${fileItem.name}"
                val process = rclone.downloadFile(remote, fileItem, saveLocation) ?: return@withContext null

                try {
                    process.waitFor()
                } catch (e: InterruptedException) {
                    FLog.e(TAG, "downloadAndOpen: error waiting for process", e)
                    return@withContext null
                }

                if (process.exitValue() == 0) {
                    File(fileLocation).setReadOnly()
                    fileLocation
                } else {
                    rclone.logErrorOutput(process)
                    null
                }
            }

            Dialogs.dismissSilently(loadingDialog)

            if (result == null || !isAdded) return@launch

            val ctx = context ?: return@launch
            val sharedFileUri = FileProvider.getUriForFile(
                ctx,
                BuildConfig.APPLICATION_ID + ".fileprovider",
                File(result),
            )
            val intent = Intent(Intent.ACTION_VIEW, sharedFileUri).apply {
                when (openAs) {
                    OPEN_AS_TEXT -> setDataAndType(sharedFileUri, "text/*")
                    OPEN_AS_IMAGE -> setDataAndType(sharedFileUri, "image/*")
                    else -> {
                        val mimeType = fileItem.mimeType
                        if (mimeType != null && mimeType != "application/octet-stream") {
                            setDataAndTypeAndNormalize(sharedFileUri, mimeType)
                        } else {
                            setDataAndType(sharedFileUri, "*/*")
                        }
                    }
                }
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            tryStartActivity(ctx, intent)
        }
    }

    private fun streamFile(fileItem: FileItem, openAs: Int = -1) {
        val loadingDialog = LoadingDialog()
            .setCanCancel(false)
            .setTitle(R.string.loading)

        loadingDialog.show(childFragmentManager, "loading dialog")

        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) {
                val ctx = context ?: return@withContext null
                val port = allocatePort(8080, true)
                val serveIntent = Intent(ctx, StreamingService::class.java).apply {
                    putExtra(StreamingService.SERVE_PATH_ARG, fileItem.path)
                    putExtra(StreamingService.REMOTE_ARG, remote)
                    putExtra(StreamingService.SHOW_NOTIFICATION_TEXT, false)
                    putExtra(StreamingService.SERVE_PORT, port)
                }
                ctx.stopService(Intent(ctx, StreamingService::class.java))
                tryStartService(ctx, serveIntent)

                val uri = Uri.parse("http://127.0.0.1:$port")
                    .buildUpon()
                    .appendPath(fileItem.name)
                    .build()

                val intent = Intent(Intent.ACTION_VIEW).apply {
                    when {
                        openAs == OPEN_AS_VIDEO -> setDataAndType(uri, "video/*")
                        openAs == OPEN_AS_AUDIO -> setDataAndType(uri, "audio/*")
                        fileItem.mimeType?.startsWith("audio/") == true -> setDataAndType(uri, "audio/*")
                        fileItem.mimeType?.startsWith("video/") == true -> setDataAndType(uri, "video/*")
                        else -> data = uri
                    }
                }

                // Wait for server to be ready
                val client = OkHttpClient.Builder().build()
                val request = Request.Builder().url(uri.toString()).head().build()
                var waitTime = 30_000L
                var available = false
                while (waitTime > 0) {
                    val start = System.nanoTime()
                    try {
                        val response = client.newCall(request).execute()
                        if (response.code == 200) {
                            available = true
                            break
                        }
                    } catch (_: IOException) {
                        // Server not yet online
                    }
                    try { Thread.sleep(250) } catch (_: InterruptedException) {}
                    waitTime -= (System.nanoTime() - start) / 1_000_000
                }

                if (available) intent else null
            }

            Dialogs.dismissSilently(loadingDialog)

            if (!isAdded) return@launch

            if (result != null) {
                streamingLauncher.launch(result)
            } else {
                context?.let {
                    Toasty.error(it, getString(R.string.streaming_task_failed), Toast.LENGTH_LONG, true).show()
                }
            }
        }
    }

    // --- Callbacks ---

    private fun onDownloadFiles(files: List<FileItem>) {
        pendingDownloadFiles = files
        viewModel.clearSelection()
        downloadDestPicker.launch(null)
    }

    private fun onSyncDirectionSelected(direction: Int) {
        syncDirection = direction
        syncDestPicker.launch(null)
    }

    private fun onCloudFilesConfirmed() {
        val ctx = context ?: return
        val uris = pendingCloudUris
        pendingCloudUris = emptyList()
        lifecycleScope.launch(Dispatchers.IO) {
            for (uri in uris) {
                val cachedPath = UriUtils.copyToCache(ctx, uri)
                if (cachedPath != null) {
                    EphemeralTaskManager.queueUpload(ctx, remote, cachedPath, viewModel.currentPath)
                }
            }
        }
    }

    private fun onServeOptionsSelected(protocol: Int, allowRemoteAccess: Boolean, user: String?, password: String?) {
        val ctx = context ?: return

        PreferenceManager.getDefaultSharedPreferences(ctx)
            .edit()
            .putBoolean(getString(R.string.pref_choice_serve_dialog_allow_ext), allowRemoteAccess)
            .apply()

        ctx.stopService(Intent(ctx, StreamingService::class.java))

        val intent = Intent(ctx, StreamingService::class.java).apply {
            putExtra(StreamingService.SERVE_PATH_ARG, viewModel.currentPath)
            putExtra(StreamingService.REMOTE_ARG, remote)
            putExtra(StreamingService.SHOW_NOTIFICATION_TEXT, true)
            putExtra(StreamingService.ALLOW_REMOTE_ACCESS, allowRemoteAccess)
            putExtra(StreamingService.AUTHENTICATION_USERNAME, user)
            putExtra(StreamingService.AUTHENTICATION_PASSWORD, password)
            putExtra(
                StreamingService.SERVE_PROTOCOL,
                when (protocol) {
                    Rclone.SERVE_PROTOCOL_HTTP -> StreamingService.SERVE_HTTP
                    Rclone.SERVE_PROTOCOL_FTP -> StreamingService.SERVE_FTP
                    Rclone.SERVE_PROTOCOL_DLNA -> StreamingService.SERVE_DLNA
                    Rclone.SERVE_PROTOCOL_WEBDAV -> StreamingService.SERVE_WEBDAV
                    else -> return
                },
            )
        }
        tryStartService(ctx, intent)
    }

    private fun getServeAllowRemoteAccess(): Boolean {
        val ctx = context ?: return false
        val prefs = PreferenceManager.getDefaultSharedPreferences(ctx)
        return prefs.getBoolean(getString(R.string.pref_choice_serve_dialog_allow_ext), false)
    }

    private fun showSFTPgoToDialog() {
        GoToDialog().show(childFragmentManager, "go to dialog")
    }

    override fun onRootClicked(isSetAsDefault: Boolean) {
        viewModel.goToRoot(isSetAsDefault)
    }

    override fun onHomeClicked(isSetAsDefault: Boolean) {
        viewModel.goToHome(isSetAsDefault)
    }

    private fun navigateToProcess(workerId: String) {
        (activity as? MainActivity)?.navigateToProcesses(workerId)
    }

    // --- Helpers ---

    private fun registerReceivers() {
        val filter = IntentFilter().apply {
            addAction(getString(R.string.background_service_broadcast))
        }
        LocalBroadcastManager.getInstance(requireContext())
            .registerReceiver(backgroundTaskReceiver, filter)
    }

    private fun initializeThumbnailParams() {
        val random = SecureRandom()
        val values = ByteArray(16)
        random.nextBytes(values)
        thumbnailServerAuth = Base64.encodeToString(values, Base64.NO_PADDING or Base64.NO_WRAP or Base64.URL_SAFE)
        thumbnailServerPort = allocatePort(29179, true)
    }

    private fun startThumbnailService() {
        if (remote.type == RemoteItem.SAFW) return
        val intent = Intent(context, ThumbnailsLoadingService::class.java).apply {
            putExtra(ThumbnailsLoadingService.REMOTE_ARG, remote)
            putExtra(ThumbnailsLoadingService.HIDDEN_PATH, thumbnailServerAuth)
            putExtra(ThumbnailsLoadingService.SERVER_PORT, thumbnailServerPort)
        }
        context?.let { tryStartService(it, intent) }
        isThumbnailsServiceRunning = true
    }

    companion object {
        private const val TAG = "FileExplorerFragment"
        private const val ARG_REMOTE = "remote_param"
        private const val OPEN_AS_TEXT = 1
        private const val OPEN_AS_IMAGE = 2
        private const val OPEN_AS_VIDEO = 0
        private const val OPEN_AS_AUDIO = 1

        @JvmStatic
        fun newInstance(remoteItem: RemoteItem): FileExplorerFragment {
            return FileExplorerFragment().apply {
                arguments = Bundle().apply {
                    putParcelable(ARG_REMOTE, remoteItem)
                }
            }
        }

        private fun allocatePort(port: Int, allocateFallback: Boolean): Int {
            return try {
                ServerSocket(port).use { socket ->
                    socket.reuseAddress = true
                    socket.localPort
                }
            } catch (e: IOException) {
                if (allocateFallback) allocatePort(0, false)
                else throw IllegalStateException("No port available")
            }
        }
    }
}
