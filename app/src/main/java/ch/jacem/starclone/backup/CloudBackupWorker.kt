package ch.jacem.starclone.backup

import android.content.Context
import androidx.preference.PreferenceManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import ch.jacem.starclone.Database.json.Exporter
import ch.jacem.starclone.Database.json.SharedPreferencesBackup
import ch.jacem.starclone.R
import ch.jacem.starclone.Rclone
import ch.jacem.starclone.util.FLog
import org.json.JSONArray
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class CloudBackupWorker(
    context: Context,
    params: WorkerParameters,
) : Worker(context, params) {

    companion object {
        private const val TAG = "CloudBackupWorker"
        private const val ENCRYPTION_NONE = "none"
        private const val ENCRYPTION_PASSWORD = "password"
        private const val ENCRYPTION_AGE = "age"
        private const val SECRET_KEY_PASSWORD = "cloud_backup_password"
        private const val SECRET_KEY_AGE_SECRET = "cloud_backup_age_secret"
    }

    override fun doWork(): Result {
        val context = applicationContext
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        val rclone = Rclone(context)

        val remoteName = prefs.getString(context.getString(R.string.pref_key_cloud_backup_remote), "") ?: ""
        val backupPath = prefs.getString(context.getString(R.string.pref_key_cloud_backup_path), "starclone-backups") ?: "starclone-backups"
        val encryption = prefs.getString(context.getString(R.string.pref_key_cloud_backup_encryption), ENCRYPTION_NONE) ?: ENCRYPTION_NONE
        val retention = prefs.getInt(context.getString(R.string.pref_key_cloud_backup_retention), 5)

        if (remoteName.isBlank()) {
            FLog.e(TAG, "No remote configured for cloud backup")
            saveStatus(context, false)
            return Result.failure()
        }

        var tempFile: File? = null
        try {
            // 1. Create backup ZIP
            val backupData = createBackupZip(context, rclone)

            // 2. Encrypt if needed
            val finalData = encryptData(context, backupData, encryption)

            // 3. Write to temp file
            val timestamp = SimpleDateFormat("yyyy-MM-dd-HHmmss", Locale.US).format(Date())
            val extension = if (encryption != ENCRYPTION_NONE) "zip.enc" else "zip"
            val filename = "starclone-backup-$timestamp.$extension"
            tempFile = File(context.cacheDir, filename)
            tempFile.writeBytes(finalData)

            // 4. Upload via rclone copy
            val destPath = "$remoteName:$backupPath"
            val uploaded = runRcloneCommand(rclone, "copy", tempFile.absolutePath, destPath)
            if (!uploaded) {
                FLog.e(TAG, "Failed to upload backup")
                saveStatus(context, false)
                return Result.failure()
            }

            // 5. Enforce retention
            enforceRetention(rclone, destPath, retention)

            saveStatus(context, true)
            return Result.success()
        } catch (e: Exception) {
            FLog.e(TAG, "Cloud backup failed", e)
            saveStatus(context, false)
            return Result.failure()
        } finally {
            tempFile?.delete()
        }
    }

    private fun createBackupZip(context: Context, rclone: Rclone): ByteArray {
        val configFile = File(context.filesDir, "rclone.conf")
        val configBytes = if (configFile.exists()) configFile.readBytes() else ByteArray(0)
        val databaseBytes = Exporter.create(context).toByteArray()
        val prefsBytes = SharedPreferencesBackup.export(context).toByteArray()

        val output = java.io.ByteArrayOutputStream()
        ZipOutputStream(output).use { zos ->
            writeZipEntry(zos, "rclone.conf", configBytes)
            writeZipEntry(zos, "rcx.json", databaseBytes)
            writeZipEntry(zos, "rcx.prefs", prefsBytes)

            val manifest = BackupManifest(
                version = 1,
                encryptionScope = EncryptionScope.NONE,
                entries = listOf(
                    BackupEntry("rclone.conf", BackupCategory.REMOTES, false),
                    BackupEntry("rcx.json", BackupCategory.DATABASE, false),
                    BackupEntry("rcx.prefs", BackupCategory.PREFERENCES, false),
                ),
            )
            writeZipEntry(zos, "manifest.json", manifest.toJson().toByteArray())
        }
        return output.toByteArray()
    }

    private fun writeZipEntry(zos: ZipOutputStream, name: String, data: ByteArray) {
        zos.putNextEntry(ZipEntry(name))
        zos.write(data)
        zos.closeEntry()
    }

    private fun encryptData(context: Context, data: ByteArray, encryption: String): ByteArray {
        return when (encryption) {
            ENCRYPTION_PASSWORD -> {
                val password = KeystoreHelper.getSecret(context, SECRET_KEY_PASSWORD)
                    ?: throw IllegalStateException("No backup password configured")
                BackupCrypto.encrypt(password, data)
            }
            ENCRYPTION_AGE -> {
                val prefs = PreferenceManager.getDefaultSharedPreferences(context)
                val publicKey = prefs.getString(context.getString(R.string.pref_key_cloud_backup_age_public_key), "")
                    ?: throw IllegalStateException("No age public key configured")
                AgeEncryption.encrypt(publicKey, data)
            }
            else -> data
        }
    }

    private fun runRcloneCommand(rclone: Rclone, vararg args: String): Boolean {
        val context = applicationContext
        val rcloneBin = context.applicationInfo.nativeLibraryDir + "/librclone.so"
        val rcloneConf = context.filesDir.path + "/rclone.conf"

        val command = mutableListOf(rcloneBin, "--config", rcloneConf)
        command.addAll(args)

        val env = rclone.getRcloneEnv()
        val process = Runtime.getRuntime().exec(command.toTypedArray(), env)
        val exitCode = process.waitFor()

        if (exitCode != 0) {
            val error = process.errorStream.bufferedReader().readText()
            FLog.e(TAG, "rclone ${args.firstOrNull()} failed: $error")
        }
        return exitCode == 0
    }

    private fun enforceRetention(rclone: Rclone, destPath: String, retention: Int) {
        val context = applicationContext
        val rcloneBin = context.applicationInfo.nativeLibraryDir + "/librclone.so"
        val rcloneConf = context.filesDir.path + "/rclone.conf"

        // List files in the backup directory
        val command = arrayOf(rcloneBin, "--config", rcloneConf, "lsjson", destPath)
        val env = rclone.getRcloneEnv()
        val process = Runtime.getRuntime().exec(command, env)

        val output = StringBuilder()
        BufferedReader(InputStreamReader(process.inputStream)).use { reader ->
            var line = reader.readLine()
            while (line != null) {
                output.append(line)
                line = reader.readLine()
            }
        }
        process.waitFor()

        if (process.exitValue() != 0) {
            FLog.e(TAG, "Failed to list backups for retention")
            return
        }

        try {
            val files = JSONArray(output.toString())
            val backupFiles = mutableListOf<Pair<String, String>>() // name, modTime

            for (i in 0 until files.length()) {
                val file = files.getJSONObject(i)
                val name = file.getString("Name")
                val isDir = file.getBoolean("IsDir")
                if (!isDir && name.startsWith("starclone-backup-")) {
                    backupFiles.add(name to file.getString("ModTime"))
                }
            }

            // Sort by name (which includes timestamp) ascending
            backupFiles.sortBy { it.first }

            // Delete oldest files beyond retention
            val toDelete = backupFiles.size - retention
            if (toDelete > 0) {
                for (i in 0 until toDelete) {
                    val fileName = backupFiles[i].first
                    runRcloneCommand(rclone, "deletefile", "$destPath/$fileName")
                }
            }
        } catch (e: Exception) {
            FLog.e(TAG, "Error enforcing retention", e)
        }
    }

    private fun saveStatus(context: Context, success: Boolean) {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        prefs.edit()
            .putLong(context.getString(R.string.pref_key_cloud_backup_last_time), System.currentTimeMillis())
            .putBoolean(context.getString(R.string.pref_key_cloud_backup_last_status), success)
            .apply()
    }
}
