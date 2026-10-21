package ch.jacem.starclone.core.updates.workmanager

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters

/**
 * Play Store build: no update check, Google Play updates the app (and its policy forbids self-updates).
 */
class UpdateWorker(context: Context, workerParams: WorkerParameters) : Worker(context, workerParams) {
    override fun doWork(): Result = Result.success()
}
