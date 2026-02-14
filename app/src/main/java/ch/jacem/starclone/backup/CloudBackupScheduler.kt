package ch.jacem.starclone.backup

import android.content.Context
import androidx.preference.PreferenceManager
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import ch.jacem.starclone.R
import java.util.Calendar
import java.util.concurrent.TimeUnit

object CloudBackupScheduler {

    private const val WORK_NAME = "cloud_backup_periodic"
    private const val WORK_TAG = "cloud_backup"

    const val FREQUENCY_DAILY = 0
    const val FREQUENCY_WEEKLY = 1
    const val FREQUENCY_MONTHLY = 2

    fun schedule(context: Context) {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        val enabled = prefs.getBoolean(context.getString(R.string.pref_key_cloud_backup_enabled), false)
        val destination = prefs.getString(context.getString(R.string.pref_key_cloud_backup_destination), "")

        if (!enabled || destination != "custom_remote") {
            cancel(context)
            return
        }

        val frequency = prefs.getInt(context.getString(R.string.pref_key_cloud_backup_frequency), FREQUENCY_DAILY)
        val timeMinutes = prefs.getInt(context.getString(R.string.pref_key_cloud_backup_time), 3 * 60) // default 3:00 AM
        val wifiOnly = prefs.getBoolean(context.getString(R.string.pref_key_cloud_backup_wifi_only), true)

        val intervalHours = when (frequency) {
            FREQUENCY_DAILY -> 24L
            FREQUENCY_WEEKLY -> 24L * 7
            FREQUENCY_MONTHLY -> 24L * 30
            else -> 24L
        }

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(if (wifiOnly) NetworkType.UNMETERED else NetworkType.CONNECTED)
            .build()

        val initialDelay = calculateInitialDelay(timeMinutes)

        val workRequest = PeriodicWorkRequestBuilder<CloudBackupWorker>(intervalHours, TimeUnit.HOURS)
            .setConstraints(constraints)
            .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
            .addTag(WORK_TAG)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            workRequest,
        )
    }

    fun cancel(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
    }

    fun runNow(context: Context) {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        val wifiOnly = prefs.getBoolean(context.getString(R.string.pref_key_cloud_backup_wifi_only), true)

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(if (wifiOnly) NetworkType.UNMETERED else NetworkType.CONNECTED)
            .build()

        val workRequest = OneTimeWorkRequestBuilder<CloudBackupWorker>()
            .setConstraints(constraints)
            .addTag(WORK_TAG)
            .build()

        WorkManager.getInstance(context).enqueue(workRequest)
    }

    private fun calculateInitialDelay(targetTimeMinutes: Int): Long {
        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, targetTimeMinutes / 60)
            set(Calendar.MINUTE, targetTimeMinutes % 60)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        if (target.before(now)) {
            target.add(Calendar.DAY_OF_MONTH, 1)
        }

        return target.timeInMillis - now.timeInMillis
    }
}
