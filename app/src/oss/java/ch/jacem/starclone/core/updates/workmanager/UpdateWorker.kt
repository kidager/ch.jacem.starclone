package ch.jacem.starclone.core.updates.workmanager

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters

/**
 * OSS build: no update check. The rs flavor's checker uses AppUpdateChecker, flagged NonFreeNet for F-Droid style
 * repositories, which handle the updates themselves.
 */
class UpdateWorker(context: Context, workerParams: WorkerParameters) : Worker(context, workerParams) {
    override fun doWork(): Result = Result.success()
}
