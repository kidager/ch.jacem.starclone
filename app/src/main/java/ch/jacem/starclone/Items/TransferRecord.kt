package ch.jacem.starclone.Items

import androidx.compose.runtime.Immutable

@Immutable
data class TransferRecord(
    val id: Long = 0,
    val workerId: String = "",
    val type: String = "",
    val title: String = "",
    val remoteName: String = "",
    val source: String = "",
    val destination: String = "",
    val status: String = "",
    val bytesTransferred: Long = 0,
    val errorMessage: String? = null,
    val startedAt: Long = 0,
    val finishedAt: Long = 0,
) {
    companion object {
        const val TABLE_NAME = "transfer_history"
        const val COLUMN_ID = "id"
        const val COLUMN_WORKER_ID = "worker_id"
        const val COLUMN_TYPE = "type"
        const val COLUMN_TITLE = "title"
        const val COLUMN_REMOTE_NAME = "remote_name"
        const val COLUMN_SOURCE = "source"
        const val COLUMN_DESTINATION = "destination"
        const val COLUMN_STATUS = "status"
        const val COLUMN_BYTES_TRANSFERRED = "bytes_transferred"
        const val COLUMN_ERROR_MESSAGE = "error_message"
        const val COLUMN_STARTED_AT = "started_at"
        const val COLUMN_FINISHED_AT = "finished_at"

        const val STATUS_SUCCEEDED = "SUCCEEDED"
        const val STATUS_FAILED = "FAILED"
        const val STATUS_CANCELLED = "CANCELLED"
    }
}
