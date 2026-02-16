package ch.jacem.starclone.backup

import android.app.backup.BackupAgent
import android.app.backup.FullBackupDataOutput
import android.os.ParcelFileDescriptor
import androidx.preference.PreferenceManager
import ch.jacem.starclone.R
import ch.jacem.starclone.util.FLog
import java.io.File

class EncryptedBackupAgent : BackupAgent() {

    override fun onFullBackup(data: FullBackupDataOutput) {
        val configFile = File(filesDir, "rclone.conf")
        if (configFile.exists()) {
            val prefs = PreferenceManager.getDefaultSharedPreferences(this)
            val encryptionKey = getString(R.string.pref_key_cloud_backup_encryption)
            val encryption = prefs.getString(encryptionKey, "none") ?: "none"

            when (encryption) {
                "password" -> {
                    val password = KeystoreHelper.getSecret(this, "cloud_backup_password")
                    if (password != null) {
                        val encrypted = BackupCrypto.encrypt(password, configFile.readBytes())
                        File(filesDir, "rclone.conf.enc").writeBytes(encrypted)
                    }
                }
                "age" -> {
                    val ageKeyPref = getString(R.string.pref_key_cloud_backup_age_public_key)
                    val publicKey = prefs.getString(ageKeyPref, "")
                    if (!publicKey.isNullOrBlank()) {
                        val encrypted = AgeEncryption.encrypt(publicKey, configFile.readBytes())
                        File(filesDir, "rclone.conf.enc").writeBytes(encrypted)
                    }
                }
            }
        }
        super.onFullBackup(data)
        File(filesDir, "rclone.conf.enc").delete()
    }

    override fun onRestoreFinished() {
        val encFile = File(filesDir, "rclone.conf.enc")
        if (encFile.exists()) {
            val prefs = PreferenceManager.getDefaultSharedPreferences(this)
            prefs.edit().putBoolean(PREF_PENDING_BACKUP_RESTORE, true).apply()
            FLog.i(TAG, "Encrypted backup restored, pending decryption on next launch")
        }
    }

    override fun onBackup(
        oldState: ParcelFileDescriptor?,
        data: android.app.backup.BackupDataOutput?,
        newState: ParcelFileDescriptor?,
    ) {
        // Not used — we use full backup via onFullBackup
    }

    override fun onRestore(
        data: android.app.backup.BackupDataInput?,
        appVersionCode: Int,
        newState: ParcelFileDescriptor?,
    ) {
        // Not used — we use full backup via onRestoreFinished
    }

    companion object {
        private const val TAG = "EncryptedBackupAgent"
        const val PREF_PENDING_BACKUP_RESTORE = "pending_backup_restore"
    }
}
