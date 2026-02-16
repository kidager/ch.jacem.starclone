package ch.jacem.starclone.workmanager

import android.app.Notification
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
import android.net.wifi.WifiManager
import androidx.annotation.StringRes
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.preference.PreferenceManager
import androidx.work.ForegroundInfo
import androidx.work.Worker
import androidx.work.WorkerParameters
import ch.jacem.starclone.Database.DatabaseHandler
import ch.jacem.starclone.Items.RemoteItem
import ch.jacem.starclone.Items.SyncDirectionObject
import ch.jacem.starclone.Items.Task
import ch.jacem.starclone.Items.TransferRecord
import ch.jacem.starclone.Log2File
import ch.jacem.starclone.R
import ch.jacem.starclone.Rclone
import ch.jacem.starclone.notifications.GenericSyncNotification
import ch.jacem.starclone.notifications.ReportNotifications
import ch.jacem.starclone.notifications.SyncServiceNotifications
import ch.jacem.starclone.notifications.SyncServiceNotifications.Companion.GROUP_ID
import ch.jacem.starclone.notifications.support.ErrorObject
import ch.jacem.starclone.notifications.support.StatusObject
import ch.jacem.starclone.util.FLog
import ch.jacem.starclone.util.SyncLog
import ch.jacem.starclone.util.WifiConnectivitiyUtil
import kotlinx.serialization.json.Json
import org.json.JSONException
import org.json.JSONObject
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.io.InterruptedIOException
import java.util.Random
import androidx.work.Data

class SyncWorker (private var mContext: Context, workerParams: WorkerParameters): Worker(mContext, workerParams) {

    companion object {
        const val TASK_ID = "TASK_ID"
        const val TASK_EPHEMERAL = "TASK_EPHEMERAL"
        private const val TAG = "SyncWorker"

        //those Extras do not follow the above schema, because they are exposed to external applications
        //That means shorter values make it easier to use. There is no other technical reason
        const val TASK_SYNC_ACTION = "START_TASK"
        const val TASK_CANCEL_ACTION = "CANCEL_TASK"
        const val EXTRA_TASK_ID = "task"

        // Todo: Allow SyncWorker to run in silent mode, or remove this!
        const val EXTRA_TASK_SILENT = "notification"

        const val PROGRESS_PERCENT = "progress_percent"
        const val PROGRESS_CONTENT = "progress_content"
        const val PROGRESS_SPEED = "progress_speed"
        const val PROGRESS_FILENAME = "progress_filename"
        const val PROGRESS_TITLE = "progress_title"
        const val PROGRESS_SOURCE = "progress_source"
        const val PROGRESS_DEST = "progress_dest"
    }



    internal enum class FAILURE_REASON {
        NO_FAILURE, NO_UNMETERED, NO_CONNECTION, RCLONE_ERROR, CONNECTIVITY_CHANGED, CANCELLED, NO_TASK
    }

    // Objects
    private var mRclone = Rclone(mContext)
    private var mDatabase = DatabaseHandler(mContext)
    private var mNotificationManager = SyncServiceNotifications(mContext)
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
    private val ongoingNotificationID = Random().nextInt()


    // Task
    private lateinit var mTask: Task
    private var mTitle: String = mContext.getString(R.string.sync_service_notification_startingsync)
    private var startedAt: Long = 0L
    private var mSource: String = ""
    private var mDest: String = ""



    override fun doWork(): Result {
        startedAt = System.currentTimeMillis()

        prepareNotifications()
        registerBroadcastReceivers()

        updateForegroundNotification(mNotificationManager.updateSyncNotification(
            mTitle,
            mTitle,
            ArrayList(),
            0,
            ongoingNotificationID
        ))


        var ephemeralTask: Task? = null

        if(inputData.keyValueMap.containsKey(TASK_ID)){
            val id = inputData.getLong(TASK_ID, -1)
            ephemeralTask = mDatabase.getTask(id)
        }

        if(inputData.keyValueMap.containsKey(TASK_EPHEMERAL)){
            val taskString = inputData.getString(TASK_EPHEMERAL) ?: ""
            if(taskString.isNotEmpty()) {
                try {
                    ephemeralTask = Json.decodeFromString<Task>(taskString)
                } catch (e: Exception) {
                    log("Could not deserialize")
                }
            }
        }

        if (ephemeralTask != null) {
            mTask = ephemeralTask
            handleTask()
            postSync()
        } else {
            postSync()
            return Result.failure()
        }

        return if (failureReason == FAILURE_REASON.NO_FAILURE) Result.success() else Result.failure()
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

    private fun handleTask() {
        mTitle = mTask.title
        mNotificationManager.setCancelId(id)
        mNotificationManager.setWorkerId(id.toString())
        val remoteItem = RemoteItem(mTask.remoteId, mTask.remoteType, "")

        if (mTask.title == "") {
            mTitle = mTask.remotePath
        }

        val remotePath = "${mTask.remoteId}:${mTask.remotePath}"
        val isR2R = SyncDirectionObject.isRemoteToRemote(mTask.direction)
        val remotePath2 = "${mTask.remoteId2}:${mTask.remotePath2}"
        when (mTask.direction) {
            SyncDirectionObject.SYNC_LOCAL_TO_REMOTE,
            SyncDirectionObject.COPY_LOCAL_TO_REMOTE -> {
                mSource = mTask.localPath
                mDest = remotePath
            }
            SyncDirectionObject.SYNC_REMOTE_TO_LOCAL,
            SyncDirectionObject.COPY_REMOTE_TO_LOCAL -> {
                mSource = remotePath
                mDest = mTask.localPath
            }
            SyncDirectionObject.SYNC_REMOTE_TO_REMOTE,
            SyncDirectionObject.COPY_REMOTE_TO_REMOTE -> {
                mSource = remotePath
                mDest = remotePath2
            }
            else -> {
                mSource = mTask.localPath
                mDest = remotePath
            }
        }

        if(arePreconditionsMet()) {
            val taskFilter = if(mTask.filterId != null ) mDatabase.getFilter(mTask.filterId!!) else null;
            val taskFilterList = taskFilter?.getFilters() ?: ArrayList()
            val localPathArg = if (isR2R) remotePath2 else mTask.localPath
            sRcloneProcess = mRclone.sync(
                remoteItem,
                localPathArg,
                mTask.remotePath,
                mTask.direction,
                mTask.md5sum,
                taskFilterList,
                mTask.deleteExcluded
            )
            handleSync(mTitle)
            sendUploadFinishedBroadcast(remoteItem.name, mTask.remotePath)
        }
    }

    private fun handleSync(title: String) {
        SyncLog.info(mContext, mTitle, mContext.getString(R.string.operation_start_sync))
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
                            updateForegroundNotification(mNotificationManager.updateSyncNotification(
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
                                .putString(PROGRESS_TITLE, mTitle)
                                .putString(PROGRESS_SOURCE, mSource)
                                .putString(PROGRESS_DEST, mDest)
                                .build())
                        }
                    } catch (e: JSONException) {
                        val trimmed = line.trim()
                        if (trimmed.isNotBlank() && !trimmed.contains("DEBUG")) {
                            FLog.e(TAG, "SyncService-Error: the offending line: $line")
                            statusObject.mErrorList.add(ErrorObject("rclone", trimmed))
                        }
                    }
                }
            } catch (e: InterruptedIOException) {
                FLog.e(TAG, "onHandleIntent: I/O interrupted, stream closed", e)
            } catch (e: IOException) {
                FLog.e(TAG, "onHandleIntent: error reading stdout", e)
            }
            try {
                localProcessReference.waitFor()
                if (localProcessReference.exitValue() != 0) {
                    FLog.e(TAG, "rclone exited with code ${localProcessReference.exitValue()}")
                    failureReason = FAILURE_REASON.RCLONE_ERROR
                }
            } catch (e: InterruptedException) {
                FLog.e(TAG, "onHandleIntent: error waiting for process", e)
            }
        } else {
            log("Sync: No Rclone Process!")
        }
        mNotificationManager.cancelSyncNotification(ongoingNotificationID)
    }

    private fun saveTransferRecord() {
        if (sRcloneProcess == null && failureReason == FAILURE_REASON.NO_FAILURE) return
        val status = when (failureReason) {
            FAILURE_REASON.NO_FAILURE -> TransferRecord.STATUS_SUCCEEDED
            FAILURE_REASON.CANCELLED -> TransferRecord.STATUS_CANCELLED
            else -> TransferRecord.STATUS_FAILED
        }
        val remoteName = if (::mTask.isInitialized) mTask.remoteId else ""
        val record = TransferRecord(
            workerId = id.toString(),
            type = "SYNC",
            title = mTitle,
            remoteName = remoteName,
            source = mSource,
            destination = mDest,
            status = status,
            bytesTransferred = statusObject.mStats.optLong("bytes", 0),
            errorMessage = statusObject.getAllErrorMessages().ifEmpty { null },
            startedAt = startedAt,
            finishedAt = System.currentTimeMillis(),
        )
        try {
            mDatabase.insertTransferRecord(record)
        } catch (e: Exception) {
            FLog.e(TAG, "Failed to save transfer record", e)
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
                followupTask(mTask.onSuccessFollowup)
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
        followupTask(mTask.onFailFollowup)
        showFailNotification(notificationId, content)
        endNotificationAlreadyPosted = true
        finishWork()
    }

    private fun showCancelledNotification(notificationId: Int) {
        SyncLog.info(mContext, mTitle, mContext.getString(R.string.operation_failed_cancelled))
        mNotificationManager.showCancelledNotificationOrReport(
            mTitle,
            notificationId,
            mTask.id
        )
    }

    private fun showSuccessNotification(notificationId: Int) {
        var message = generateSuccessMessage(statusObject)
        mNotificationManager.showSuccessNotificationOrReport(
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

    // this is currently only a useless mapper. It is supposed to keep this worker in sync with the ephemeral one.
    // when they are merged eventually, this can be easily extracted.
    private fun generateSuccessMessage(statusObject: StatusObject): String {
        var message = mContext.resources.getQuantityString(
                R.plurals.operation_success_description,
                statusObject.getTotalTransfers(),
                mTitle,
                statusObject.getTotalSize(),
                statusObject.getTotalTransfers()
        )
        if (statusObject.getTotalTransfers() == 0) {
            message = mContext.resources.getString(R.string.operation_success_description_zero)
        }
        if (statusObject.getDeletions() > 0) {
            message += """
                        
                        ${
                mContext.getString(
                        R.string.operation_success_description_deletions_prefix,
                        statusObject.getDeletions()
                )
            }
                        """.trimIndent()
        }
        return message
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
        mNotificationManager.showFailedNotificationOrReport(
            mTitle,
            text,
            notificationId,
            mTask.id
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

    private fun arePreconditionsMet(): Boolean {
        val connection = WifiConnectivitiyUtil.dataConnection(this.applicationContext)
        if (mTask.wifionly && connection === WifiConnectivitiyUtil.Connection.METERED) {
            failureReason = FAILURE_REASON.NO_UNMETERED
            return false
        } else if (connection === WifiConnectivitiyUtil.Connection.DISCONNECTED || connection === WifiConnectivitiyUtil.Connection.NOT_AVAILABLE) {
            failureReason = FAILURE_REASON.NO_CONNECTION
            return false
        }

        return true
    }

    private fun prepareNotifications() {

        GenericSyncNotification(mContext).setNotificationChannel(
                SyncServiceNotifications.CHANNEL_ID,
                getString(R.string.sync_service_notification_channel_title),
                getString(R.string.sync_service_notification_channel_description),
                GROUP_ID,
                getString(R.string.sync_service_notification_group)
        )
        GenericSyncNotification(mContext).setNotificationChannel(
            SyncServiceNotifications.CHANNEL_SUCCESS_ID,
            getString(R.string.sync_service_notification_channel_success_title),
            getString(R.string.sync_service_notification_channel_success_description),
                GROUP_ID,
                getString(R.string.sync_service_notification_group)
        )
        GenericSyncNotification(mContext).setNotificationChannel(
            SyncServiceNotifications.CHANNEL_FAIL_ID,
            getString(R.string.sync_service_notification_channel_fail_title),
            getString(R.string.sync_service_notification_channel_fail_description),
                GROUP_ID,
                getString(R.string.sync_service_notification_group)
        )
        GenericSyncNotification(mContext).setNotificationChannel(
            ReportNotifications.CHANNEL_REPORT_ID,
            getString(R.string.sync_service_notification_channel_report_title),
            getString(R.string.sync_service_notification_channel_report_description),
                GROUP_ID,
                getString(R.string.sync_service_notification_group)
        )

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
        FLog.e(TAG, "SyncWorker: $message")
    }

    private fun getString(@StringRes resId: Int): String {
        return mContext.getString(resId)
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

    private fun followupTask(followUpTaskID: Long?) {
        if (followUpTaskID == null || followUpTaskID == -1L) {
            return
        }
        Thread.sleep(1000)
        SyncManager(mContext).queue(followUpTaskID)
    }
}
