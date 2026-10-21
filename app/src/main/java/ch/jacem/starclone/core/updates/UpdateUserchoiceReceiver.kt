package ch.jacem.starclone.core.updates

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Build.VERSION_CODES
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.content.FileProvider
import androidx.preference.PreferenceManager
import ch.jacem.starclone.BuildConfig
import ch.jacem.starclone.R
import ch.jacem.starclone.core.extensions.tag
import ch.jacem.starclone.core.notifications.AppUpdateNotification
import java.io.File
import java.io.FileOutputStream
import java.net.URL


class UpdateUserchoiceReceiver : BroadcastReceiver() {

    companion object {
        var ACTION_IGNORE = "ACTION_IGNORE"
        var ACTION_DOWNLOAD = "ACTION_DOWNLOAD"
        var IGNORE_VERSION_EXTRA = "IGNORE_VERSION_EXTRA"
    }
    override fun onReceive(context: Context, intent: Intent) {

        val preferenceManager = PreferenceManager.getDefaultSharedPreferences(context)
        if(intent.action == ACTION_IGNORE) {
            Log.e(tag(), "Ignore current update!")
            val key = context.getString(R.string.pref_key_app_update_dismiss_current_update)
            preferenceManager.edit().putString(key, intent.getStringExtra(IGNORE_VERSION_EXTRA)).apply()
            AppUpdateNotification(context).cancelNotification()
        }

        if(intent.action == ACTION_DOWNLOAD) {
            val versionKey = context.getString(R.string.pref_key_app_updates_found_update_for_version)
            val version = preferenceManager.getString(versionKey,"")?: ""

            if(version.isNotEmpty()) {
                // Releases are tagged vX.Y.Z and carry the OSS universal APK as starclone-vX.Y.Z-oss.apk (release.yml)
                val tag = if (version.startsWith("v")) version else "v$version"
                downloadAndInstall(
                    URL("https://github.com/kidager/ch.jacem.starclone/releases/download/$tag/starclone-$tag-oss.apk"),
                    context,
                    tag
                )
            }
            AppUpdateNotification(context).cancelNotification()
        }
    }

    private fun downloadAndInstall(url: URL, context: Context, tag: String) {
        Log.e(tag(), "Download url: $url")
        Thread {
            val dir = context.externalCacheDir?.absolutePath ?: ""
            Log.e(tag(), "Download dir: $dir")
            val target = File(dir, "starclone-$tag.apk")
            url.openStream()
                .use { input ->
                FileOutputStream(target).use {
                    input.copyTo(it)
                }
            }

            val fileUri = FileProvider.getUriForFile(context,BuildConfig.APPLICATION_ID + ".fileprovider", target)

            val intent = Intent(Intent.ACTION_VIEW)
            intent.setDataAndType(fileUri, "application/vnd.android.package-archive")
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            context.startActivity(intent)
        }.start()
    }
}