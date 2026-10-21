package ch.jacem.starclone.core.updates.workmanager

import android.content.Context
import android.util.Log
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import ch.jacem.starclone.BuildConfig
import ch.jacem.starclone.R
import ch.jacem.starclone.core.extensions.tag
import ch.jacem.starclone.core.notifications.AppUpdateNotification
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.IOException

/**
 * OSS build: checks the latest GitHub release of this repo and notifies when it is newer than the installed version.
 * No third-party update library (F-Droid style repositories flag them), only OkHttp and org.json.
 */
class UpdateWorker(private val mContext: Context, workerParams: WorkerParameters) : Worker(mContext, workerParams) {

    private val preferenceManager = PreferenceManager.getDefaultSharedPreferences(mContext)

    private val checkForUpdates = preferenceManager.getBoolean(mContext.getString(R.string.pref_key_app_updates), false)
    private val ignoredVersion = preferenceManager.getString(mContext.getString(R.string.pref_key_app_update_dismiss_current_update), "")
    private val lastFoundVersion = preferenceManager.getString(mContext.getString(R.string.pref_key_app_updates_found_update_for_version), BuildConfig.VERSION_NAME) ?: BuildConfig.VERSION_NAME

    override fun doWork(): Result {
        if (!checkForUpdates) {
            return Result.success()
        }

        // A newer version was already found: only remind, unless the user skipped it
        if (BuildConfig.VERSION_NAME != lastFoundVersion && ignoredVersion != lastFoundVersion) {
            notifyIfRequired(lastFoundVersion)
            return Result.success()
        }

        val release = try {
            latestRelease()
        } catch (e: IOException) {
            Log.e(tag(), "Update check failed", e)
            return Result.retry()
        } ?: return Result.success()

        val latestVersion = release.getString("tag_name").removePrefix("v")
        if (isNewer(latestVersion, BuildConfig.VERSION_NAME)) {
            setFoundVersion(latestVersion)
            setChangelog(release.optString("body"))
            notifyIfRequired(latestVersion)
        } else {
            setFoundVersion(BuildConfig.VERSION_NAME)
        }
        return Result.success()
    }

    private fun latestRelease(): JSONObject? {
        val request = Request.Builder()
            .url("https://api.github.com/repos/kidager/ch.jacem.starclone/releases/latest")
            .header("Accept", "application/vnd.github+json")
            .build()
        OkHttpClient().newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                Log.e(tag(), "Update check: HTTP ${response.code}")
                return null
            }
            return JSONObject(response.body.string())
        }
    }

    /** Compares X.Y.Z versions, ignoring anything after a "-" (e.g. "-DEBUG") */
    private fun isNewer(candidate: String, current: String): Boolean {
        fun parts(version: String) = version.substringBefore('-').split('.').map { it.toIntOrNull() ?: 0 }
        val a = parts(candidate)
        val b = parts(current)
        for (i in 0 until maxOf(a.size, b.size)) {
            val diff = a.getOrElse(i) { 0 } - b.getOrElse(i) { 0 }
            if (diff != 0) return diff > 0
        }
        return false
    }

    /**
     * Does not notify the user when the user skipped this update.
     */
    private fun notifyIfRequired(version: String) {
        if (ignoredVersion != version) {
            AppUpdateNotification(mContext).showNotification(version)
        }
    }

    private fun setChangelog(changelog: String) {
        preferenceManager.edit { putString(mContext.getString(R.string.pref_key_app_updates_changelog), changelog) }
    }

    private fun setFoundVersion(version: String) {
        preferenceManager.edit { putString(mContext.getString(R.string.pref_key_app_updates_found_update_for_version), version) }
    }
}
