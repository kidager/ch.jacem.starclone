package ch.jacem.starclone.extract.notifications.implementations

import android.content.Context
import ch.jacem.starclone.Items.FileItem
import ch.jacem.starclone.R
import ch.jacem.starclone.notifications.prototypes.WorkerNotification
import ch.jacem.starclone.notifications.support.StatusObject

class DownloadWorkerNotification(var context: Context) : WorkerNotification(context) {

    override val CHANNEL_ID = "ch.jacem.starclone.extract.download_service"


    override val initialTitle = string(R.string.worker_download_initialtitle)
    override val serviceOngoingTitle = initialTitle
    override val serviceFailed = string(R.string.worker_download_failed)
    override val serviceCancelled = string(R.string.worker_download_cancelled)
    override val serviceSuccess = string(R.string.worker_download_complete)



    override val channel_ongoing_title = string(R.string.download_service_ongoing_notification_title)
    override val channel_ongoing_description = string(R.string.download_service_ongoing_notification_description)
    override val channel_success_title = string(R.string.download_service_success_notification_title)
    override val channel_success_description = string(R.string.download_service_success_notification_description)
    override val channel_failed_title = string(R.string.download_service_failed_notification_title)
    override val channel_failed_description = string(R.string.download_service_failed_notification_description)

    override val PERSISTENT_NOTIFICATION_ID = 389

    override val SUMMARY_ID = 390

    override fun generateSuccessMessage(statusObject: StatusObject, fileItem: FileItem): String {
        val transfers = maxOf(statusObject.getTotalTransfers(), 1)
        return mContext.resources.getQuantityString(
                R.plurals.worker_download_success_message,
                transfers,
                fileItem.name
        )
    }

}