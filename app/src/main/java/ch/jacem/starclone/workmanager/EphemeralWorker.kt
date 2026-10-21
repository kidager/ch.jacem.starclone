package ch.jacem.starclone.workmanager

import android.app.Notification
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
import android.net.wifi.WifiManager
import android.os.Parcel
import androidx.annotation.StringRes
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.preference.PreferenceManager
import androidx.work.ForegroundInfo
import androidx.work.Worker
import androidx.work.WorkerParameters
import ch.jacem.starclone.Items.FileItem
import ch.jacem.starclone.Items.RemoteItem
import ch.jacem.starclone.Log2File
import ch.jacem.starclone.R
import ch.jacem.starclone.Database.DatabaseHandler
import ch.jacem.starclone.Items.TransferRecord
import ch.jacem.starclone.Rclone
import ch.jacem.starclone.notifications.prototypes.WorkerNotification
import ch.jacem.starclone.notifications.support.ErrorObject
import ch.jacem.starclone.notifications.support.StatusObject
import ch.jacem.starclone.util.FLog
import ch.jacem.starclone.util.SyncLog
import ch.jacem.starclone.util.WifiConnectivitiyUtil
import ch.jacem.starclone.core.extensions.tag
import ch.jacem.starclone.core.notifications.implementations.DownloadWorkerNotification
import org.json.JSONException
import org.json.JSONObject
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.io.InterruptedIOException
import kotlin.random.Random
import android.util.Log
import androidx.work.Data
import ch.jacem.starclone.core.notifications.implementations.DeleteWorkerNotification
import ch.jacem.starclone.core.notifications.implementations.MoveWorkerNotification
import ch.jacem.starclone.core.notifications.implementations.UploadWorkerNotification


class EphemeralWorker (private var mContext: Context, workerParams: WorkerParameters): Worker(mContext, workerParams) {


    companion object {
        const val EPHEMERAL_TYPE = "TASK_EPHEMERAL_TYPE"
        const val REMOTE = "REMOTE"

        const val DOWNLOAD_TARGETPATH = "DOWNLOAD_TARGETPATH"
        const val DOWNLOAD_SOURCE = "DOWNLOAD_SOURCE"

        const val UPLOAD_FILE = "UPLOAD_FILE"
        const val UPLOAD_TARGETPATH = "UPLOAD_TARGETPATH"

        const val MOVE_FILE = "MOVE_FILE"
        const val MOVE_TARGETPATH = "MOVE_TARGETPATH"

        const val DELETE_FILE = "DELETE_FILE"

        const val PROGRESS_PERCENT = "progress_percent"
        const val PROGRESS_CONTENT = "progress_content"
        const val PROGRESS_SPEED = "progress_speed"
        const val PROGRESS_FILENAME = "progress_filename"
        const val PROGRESS_TYPE = "progress_type"
        const val PROGRESS_REMOTE = "progress_remote"
        const val PROGRESS_SOURCE = "progress_source"
        const val PROGRESS_DEST = "progress_dest"
    }

    internal enum class FAILURE_REASON {
        NO_FAILURE, NO_UNMETERED, NO_CONNECTION, RCLONE_ERROR, CONNECTIVITY_CHANGED, CANCELLED, NO_TASK
    }

    // Objects
    private var mNotificationManager: WorkerNotification? = null
    private val mPreferences = PreferenceManager.getDefaultSharedPreferences(mContext)


    private var log2File: Log2File? = null


    // States
    private val sIsLoggingEnabled = mPreferences.getBoolean(getString(R.string.pref_key_logs), false)
    private var sConnectivityChanged = false

    private var sRcloneProcess: Process? = null
    private val statusObject = StatusObject(mContext)
    private var failureReason = FAILURE_REASON.NO_FAILURE
    private var endNotificationAlreadyPosted = false
    private var silentRun = false
    private val ongoingNotificationID = Random.nextInt()


    private var mTitle: String = mNotificationManager?.initialTitle ?: ""
    private var startedAt: Long = 0L
    private var mRemoteName: String = ""
    private var mSource: String = ""
    private var mDest: String = ""

    override fun doWork(): Result {
        startedAt = System.currentTimeMillis()

        registerBroadcastReceivers()

        updateForegroundNotification(mNotificationManager?.updateNotification(
            mTitle,
            mTitle,
            ArrayList(),
            0,
            ongoingNotificationID
        ))

        if (inputData.keyValueMap.containsKey(EPHEMERAL_TYPE)){
            val type = Type.valueOf(inputData.getString(EPHEMERAL_TYPE) ?: "")
            mNotificationManager = prepareNotificationManager(type)

            val remoteItem = getRemoteitemFromParcel(REMOTE)
            if(remoteItem == null){
                log("$REMOTE: No valid remote was passed!")
                return Result.failure()
            }

            mNotificationManager?.setCancelId(id)
            mNotificationManager?.setWorkerId(id.toString())
            mRemoteName = remoteItem.name
            if(preconditionsMet()) {
                // do not instantiate rclone when you dont want it to run.
                // It will immediately run!
                when(type){
                    Type.DOWNLOAD -> {
                        val target = inputData.getString(DOWNLOAD_TARGETPATH)
                        val fileItem = getFileitemFromParcel(DOWNLOAD_SOURCE)

                        if(fileItem == null){
                            log("$DOWNLOAD_SOURCE: No valid target was passed!")
                            return Result.failure()
                        }

                        mSource = "${remoteItem.name}:${fileItem.path}${fileItem.name}"
                        mDest = target ?: ""

                        sRcloneProcess = Rclone(mContext).downloadFile(
                            remoteItem,
                            fileItem,
                            target
                        )
                    }
                    Type.UPLOAD -> {
                        val target = inputData.getString(UPLOAD_TARGETPATH)
                        val file = inputData.getString(UPLOAD_FILE)

                        mSource = file ?: ""
                        mDest = "${remoteItem.name}:${target ?: ""}"

                        sRcloneProcess = Rclone(mContext).uploadFile(
                            remoteItem,
                            target,
                            file
                        )
                    }
                    Type.MOVE -> {
                        val target = inputData.getString(MOVE_TARGETPATH)
                        val fileItem = getFileitemFromParcel(MOVE_FILE)

                        if(fileItem == null){
                            log("$MOVE_FILE: No valid target was passed!")
                            return Result.failure()
                        }

                        mSource = "${remoteItem.name}:${fileItem.path}${fileItem.name}"
                        mDest = "${remoteItem.name}:${target ?: ""}"

                        sRcloneProcess = Rclone(mContext).moveTo(
                            remoteItem,
                            fileItem,
                            target
                        )
                    }
                    Type.DELETE -> {
                        val fileItem = getFileitemFromParcel(DELETE_FILE)

                        if(fileItem == null){
                            log("$DELETE_FILE: No valid target was passed!")
                            return Result.failure()
                        }

                        mSource = "${remoteItem.name}:${fileItem.path}${fileItem.name}"
                        mDest = ""

                        sRcloneProcess = Rclone(mContext).deleteItems(
                            remoteItem,
                            fileItem
                        )
                    }
                }
                handleSync(mTitle)
            } else {
                log("Preconditions are not met!")
                postSync()
                return Result.failure()
            }

            postSync()
            return if (failureReason == FAILURE_REASON.NO_FAILURE) Result.success() else Result.failure()
        }
        log("Critical: No valid ephemeral type passed!")
        return Result.failure()
    }

    override fun onStopped() {
        super.onStopped()
        // Only log/notify cancellation if work actually started (process was launched)
        if (sRcloneProcess != null) {
            SyncLog.info(mContext, mTitle, mContext.getString(R.string.operation_sync_cancelled))
            SyncLog.info(mContext, mTitle, statusObject.toString())
            failureReason = FAILURE_REASON.CANCELLED
        }
        finishWork()
    }

    private fun finishWork() {
        sRcloneProcess?.destroy()
        try {
            mContext.unregisterReceiver(connectivityChangeBroadcastReceiver)
        } catch (_: IllegalArgumentException) {
            // Receiver was never registered (worker stopped before doWork ran)
        }
        postSync()
    }

    fun prepareNotificationManager(type: Type): WorkerNotification {
        return when(type){
            Type.DOWNLOAD -> DownloadWorkerNotification(mContext)
            Type.UPLOAD -> UploadWorkerNotification(mContext)
            Type.DELETE -> DeleteWorkerNotification(mContext)
            Type.MOVE -> MoveWorkerNotification(mContext)
        }
    }

    private fun handleSync(title: String) {
        if (sRcloneProcess != null) {
            val localProcessReference = sRcloneProcess!!
            try {
                val reader = BufferedReader(InputStreamReader(localProcessReference.errorStream))
                val iterator = reader.lineSequence().iterator()
                var lastNotificationUpdate = 0L
                while(iterator.hasNext()) {
                    val line = iterator.next()
                    try {
                        val logline = JSONObject(line)
                        //todo: migrate this to StatusObject, so that we can handle everything properly.
                        val level = logline.getString("level")
                        if (level == "error") {
                            if (sIsLoggingEnabled) {
                                log2File?.log(line)
                            }
                        }
                        if (level == "error" || level == "warning" || level == "notice") {
                            statusObject.parseLoglineToStatusObject(logline)
                        }

                        val now = System.currentTimeMillis()
                        if (statusObject.notificationContent.isNotBlank() && now - lastNotificationUpdate >= 1000) {
                            lastNotificationUpdate = now
                            updateForegroundNotification(mNotificationManager?.updateNotification(
                                title,
                                statusObject.notificationContent,
                                statusObject.notificationBigText,
                                statusObject.notificationPercent,
                                ongoingNotificationID
                            ))
                            val currentFileName = statusObject.mStats.optJSONArray("transferring")
                                ?.optJSONObject(0)?.optString("name", "") ?: ""
                            setProgressAsync(Data.Builder()
                                .putInt(PROGRESS_PERCENT, statusObject.notificationPercent)
                                .putString(PROGRESS_CONTENT, statusObject.notificationContent)
                                .putString(PROGRESS_SPEED, statusObject.getSpeed())
                                .putString(PROGRESS_FILENAME, currentFileName)
                                .putString(PROGRESS_TYPE, inputData.getString(EPHEMERAL_TYPE) ?: "")
                                .putString(PROGRESS_REMOTE, mRemoteName)
                                .putString(PROGRESS_SOURCE, mSource)
                                .putString(PROGRESS_DEST, mDest)
                                .build())
                        }
                    } catch (e: JSONException) {
                        val trimmed = line.trim()
                        if (trimmed.isNotBlank() && !trimmed.contains("DEBUG")) {
                            Log.e(tag(), "Error: the offending line: $line")
                            statusObject.mErrorList.add(ErrorObject("rclone", trimmed))
                        }
                    }
                }
            } catch (e: InterruptedIOException) {
                FLog.e(tag(), "onHandleIntent: I/O interrupted, stream closed", e)
            } catch (e: IOException) {
                FLog.e(tag(), "onHandleIntent: error reading stdout", e)
            }
            try {
                localProcessReference.waitFor()
                if (localProcessReference.exitValue() != 0) {
                    FLog.e(tag(), "rclone exited with code ${localProcessReference.exitValue()}")
                    failureReason = FAILURE_REASON.RCLONE_ERROR
                }
            } catch (e: InterruptedException) {
                FLog.e(tag(), "onHandleIntent: error waiting for process", e)
            }
        } else {
            log("Sync: No Rclone Process!")
        }
        mNotificationManager?.cancelSyncNotification(ongoingNotificationID)
    }

    private fun saveTransferRecord() {
        if (sRcloneProcess == null && failureReason == FAILURE_REASON.NO_FAILURE) return
        val status = when (failureReason) {
            FAILURE_REASON.NO_FAILURE -> TransferRecord.STATUS_SUCCEEDED
            FAILURE_REASON.CANCELLED -> TransferRecord.STATUS_CANCELLED
            else -> TransferRecord.STATUS_FAILED
        }
        val typeStr = inputData.getString(EPHEMERAL_TYPE) ?: ""
        val record = TransferRecord(
            workerId = id.toString(),
            type = typeStr,
            title = mTitle.ifEmpty { typeStr },
            remoteName = mRemoteName,
            source = mSource,
            destination = mDest,
            status = status,
            bytesTransferred = statusObject.mStats.optLong("bytes", 0),
            errorMessage = statusObject.getAllErrorMessages().ifEmpty { null },
            startedAt = startedAt,
            finishedAt = System.currentTimeMillis(),
        )
        try {
            DatabaseHandler(mContext).insertTransferRecord(record)
        } catch (e: Exception) {
            FLog.e("EphemeralWorker", "Failed to save transfer record", e)
        }
    }

    private fun postSync() {
        if (endNotificationAlreadyPosted) {
            return
        }
        if (silentRun) {
            return
        }
        // Don't post notifications if no work was actually started
        if (sRcloneProcess == null && failureReason == FAILURE_REASON.NO_FAILURE) {
            return
        }

        saveTransferRecord()

        val notificationId = System.currentTimeMillis().toInt()

        var content = mContext.getString(R.string.operation_failed_unknown, mTitle)
        when (failureReason) {
            FAILURE_REASON.NO_FAILURE -> {
                showSuccessNotification(notificationId)
                return
            }
            FAILURE_REASON.CANCELLED -> {
                showCancelledNotification(notificationId)
                endNotificationAlreadyPosted = true
                return
            }
            FAILURE_REASON.NO_TASK -> {
                content = getString(R.string.operation_failed_notask)
            }
            FAILURE_REASON.CONNECTIVITY_CHANGED -> {
                content = mContext.getString(R.string.operation_failed_data_change, mTitle)
            }
            FAILURE_REASON.NO_UNMETERED -> {
                content = mContext.getString(R.string.operation_failed_no_unmetered, mTitle)
            }
            FAILURE_REASON.NO_CONNECTION -> {
                content = mContext.getString(R.string.operation_failed_no_connection, mTitle)
            }
            FAILURE_REASON.RCLONE_ERROR -> {
                content = mContext.getString(R.string.operation_failed_unknown_rclone_error, mTitle)
            }
        }
        showFailNotification(notificationId, content)
        endNotificationAlreadyPosted = true
        finishWork()
    }

    private fun showCancelledNotification(notificationId: Int) {
        val content = mContext.getString(R.string.operation_failed_cancelled)
        SyncLog.info(mContext, mTitle, content)
        mNotificationManager?.showCancelledNotification(
            mTitle,
            content,
            notificationId,
            0
        )
    }

    private fun showSuccessNotification(notificationId: Int) {
        var message = mNotificationManager?.generateSuccessMessage(statusObject, getCurrentFile()) ?: "error"

        mNotificationManager?.showSuccessNotification(
            mTitle,
            message,
            notificationId
        )

        message += """

        Est. Speed: ${statusObject.getEstimatedAverageSpeed()}
        Avg. Speed: ${statusObject.getLastItemAverageSpeed()}
                        """.trimIndent()

        if (statusObject.mErrorList.isNotEmpty()) {
            message += "\n\n${statusObject.mErrorList.size} error(s) — see individual entries below"
        }
        SyncLog.info(mContext, mContext.getString(R.string.operation_success, mTitle), message)
        logIndividualErrors()
    }

    private fun showFailNotification(notificationId: Int, content: String, wasCancelled: Boolean = false) {
        var text = content
        if (statusObject.mErrorList.isNotEmpty()) {
            text += "\n\n${statusObject.mErrorList.size} error(s) — see individual entries below"
        }

        var notifyTitle = mContext.getString(R.string.operation_failed)
        if (wasCancelled) {
            notifyTitle = mContext.getString(R.string.operation_failed_cancelled)
        }
        SyncLog.error(mContext, notifyTitle, "$mTitle: $text")
        logIndividualErrors()
        mNotificationManager?.showFailedNotification(
            mTitle,
            text,
            notificationId,
           0
        )
    }

    private fun logIndividualErrors() {
        val deduplicated = statusObject.mErrorList
            .map { it.mErrorMessage to it.mErrorObject }
            .distinct()
        for ((message, file) in deduplicated) {
            val entry = if (file.isNotBlank()) "$message\nFile: $file" else message
            SyncLog.error(mContext, "$mTitle — error", entry)
        }
    }

    private fun preconditionsMet(): Boolean {
        val wifiOnly = mPreferences.getBoolean(mContext.getString(R.string.pref_key_wifi_only_transfers), false)
        val connection = WifiConnectivitiyUtil.dataConnection(this.applicationContext)
        if (wifiOnly && connection === WifiConnectivitiyUtil.Connection.METERED) {
            failureReason = FAILURE_REASON.NO_UNMETERED
            return false
        } else if (connection === WifiConnectivitiyUtil.Connection.DISCONNECTED || connection === WifiConnectivitiyUtil.Connection.NOT_AVAILABLE) {
            failureReason = FAILURE_REASON.NO_CONNECTION
            return false
        }

        return true
    }

    private fun sendUploadFinishedBroadcast(remote: String, path: String?) {
        val intent = Intent()
        intent.action = getString(R.string.background_service_broadcast)
        intent.putExtra(getString(R.string.background_service_broadcast_data_remote), remote)
        intent.putExtra(getString(R.string.background_service_broadcast_data_path), path)
        LocalBroadcastManager.getInstance(mContext).sendBroadcast(intent)
    }

    // Creates an instance of ForegroundInfo which can be used to update the
    // ongoing notification.
    private fun updateForegroundNotification(notification: Notification?) {

        notification?.let {
            setForegroundAsync(ForegroundInfo(ongoingNotificationID, it, FOREGROUND_SERVICE_TYPE_DATA_SYNC))
        }
    }


    private fun log(message: String) {
        FLog.e(tag(), "EphemeralWorker: $message")
    }

    private fun getString(@StringRes resId: Int?): String {
        return if (resId == null) {
            "Error"
        } else {
            mContext.getString(resId)
        }
    }

    private fun registerBroadcastReceivers() {
        val intentFilter = IntentFilter()
        intentFilter.addAction(WifiManager.SUPPLICANT_CONNECTION_CHANGE_ACTION)
        mContext.registerReceiver(connectivityChangeBroadcastReceiver, intentFilter)
    }

    private val connectivityChangeBroadcastReceiver: BroadcastReceiver =
        object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if(endNotificationAlreadyPosted){
                    return
                }
                sConnectivityChanged = true
                failureReason = FAILURE_REASON.CONNECTIVITY_CHANGED
            }
        }

    private fun getFileitemFromParcel(key: String): FileItem {

        val sourceParcelByteArray = inputData.getByteArray(key)
        if(sourceParcelByteArray == null){
            log("No valid target was passed!")
            throw NullPointerException("The passed FileItem was null. We cannot continue!")
        }

        val parcel = Parcel.obtain()
        parcel.unmarshall(sourceParcelByteArray, 0, sourceParcelByteArray.size)
        parcel.setDataPosition(0)
        return FileItem.CREATOR.createFromParcel(parcel)
    }

    private fun getRemoteitemFromParcel(key: String): RemoteItem? {

        val sourceParcelByteArray = inputData.getByteArray(key)
        if(sourceParcelByteArray == null){
            log("No valid target was passed!")
            return null
        }

        val parcel = Parcel.obtain()
        parcel.unmarshall(sourceParcelByteArray, 0, sourceParcelByteArray.size)
        parcel.setDataPosition(0)
        return RemoteItem.CREATOR.createFromParcel(parcel)
    }

    private fun getCurrentFile(): FileItem {
        return when(Type.valueOf(inputData.getString(EPHEMERAL_TYPE)?:Type.DOWNLOAD.name)){
            Type.DOWNLOAD -> {
                getFileitemFromParcel(DOWNLOAD_SOURCE)
            }
            Type.UPLOAD -> {
                val pathAndName = inputData.getString(UPLOAD_FILE) ?: ""
                val name = pathAndName.substring(pathAndName.lastIndexOf("/")+1, pathAndName.length)
                val path = pathAndName.substring(0, pathAndName.lastIndexOf("/")+1)
                // TODO: Make this work properly! All the params are guessed!
                FileItem(
                        RemoteItem("", ""),
                        path,
                        name,
                        0L,
                        "modTime",
                        "mimeType",
                        false,
                        false)
            }
            Type.MOVE -> {
                getFileitemFromParcel(MOVE_FILE)
            }
            Type.DELETE -> {
                getFileitemFromParcel(DELETE_FILE)
            }
        }
    }
}
