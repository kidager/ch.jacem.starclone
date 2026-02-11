package ch.jacem.starclone.backup

import android.content.Context
import android.net.Uri
import ch.jacem.starclone.Database.json.Exporter
import ch.jacem.starclone.Database.json.SharedPreferencesBackup
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object BackupWriter {

    fun writeBackup(
        context: Context,
        targetUri: Uri,
        categories: Set<BackupCategory>,
        encryptionScope: EncryptionScope,
        password: String?,
        selectedRemotes: Set<String>? = null,
    ) {
        val entries = mutableListOf<BackupEntry>()

        val configBytes = if (BackupCategory.REMOTES in categories) {
            val raw = File(context.filesDir, "rclone.conf").takeIf { it.exists() }?.readText()
            if (raw != null && selectedRemotes != null) {
                BackupReader.filterConfigByRemotes(raw, selectedRemotes).toByteArray()
            } else {
                raw?.toByteArray()
            }
        } else null

        val databaseBytes = if (BackupCategory.DATABASE in categories) {
            Exporter.create(context).toByteArray()
        } else null

        val prefsBytes = if (BackupCategory.PREFERENCES in categories) {
            SharedPreferencesBackup.export(context).toByteArray()
        } else null

        val outputStream = context.contentResolver.openOutputStream(targetUri)
            ?: throw IllegalStateException("Cannot open output stream")

        ZipOutputStream(outputStream).use { zos ->
            if (configBytes != null) {
                val encrypted = shouldEncrypt(BackupCategory.REMOTES, encryptionScope)
                // Use rclone's native encryption for the config (portable format)
                val data = if (encrypted) {
                    BackupCrypto.encryptConfigWithRclone(context, configBytes, password!!)
                } else {
                    configBytes
                }
                // Always store as rclone.conf (rclone encryption is internal to the file)
                writeEntry(zos, "rclone.conf", data)
                entries.add(BackupEntry("rclone.conf", BackupCategory.REMOTES, encrypted))
            }

            if (databaseBytes != null) {
                val encrypted = shouldEncrypt(BackupCategory.DATABASE, encryptionScope)
                val filename = if (encrypted) "rcx.json.enc" else "rcx.json"
                val data = if (encrypted) BackupCrypto.encrypt(password!!, databaseBytes) else databaseBytes
                writeEntry(zos, filename, data)
                entries.add(BackupEntry(filename, BackupCategory.DATABASE, encrypted))
            }

            if (prefsBytes != null) {
                val encrypted = shouldEncrypt(BackupCategory.PREFERENCES, encryptionScope)
                val filename = if (encrypted) "rcx.prefs.enc" else "rcx.prefs"
                val data = if (encrypted) BackupCrypto.encrypt(password!!, prefsBytes) else prefsBytes
                writeEntry(zos, filename, data)
                entries.add(BackupEntry(filename, BackupCategory.PREFERENCES, encrypted))
            }

            val manifest = BackupManifest(
                version = 1,
                encryptionScope = encryptionScope,
                entries = entries,
            )
            writeEntry(zos, "manifest.json", manifest.toJson().toByteArray())
        }
    }

    private fun shouldEncrypt(category: BackupCategory, scope: EncryptionScope): Boolean {
        return when (scope) {
            EncryptionScope.NONE -> false
            EncryptionScope.CONFIG_ONLY -> category == BackupCategory.REMOTES
            EncryptionScope.EVERYTHING -> true
        }
    }

    private fun writeEntry(zos: ZipOutputStream, filename: String, data: ByteArray) {
        zos.putNextEntry(ZipEntry(filename))
        zos.write(data)
        zos.closeEntry()
    }
}
