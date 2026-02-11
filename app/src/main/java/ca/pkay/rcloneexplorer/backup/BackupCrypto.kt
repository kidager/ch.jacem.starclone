package ca.pkay.rcloneexplorer.backup

import android.content.Context
import ca.pkay.rcloneexplorer.util.FLog
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

class BadPasswordException : Exception("Incorrect password or corrupted data")

private const val RCLONE_ENCRYPTED_HEADER = "# Encrypted rclone configuration File"

fun isRcloneEncrypted(data: ByteArray): Boolean {
    val header = data.toString(Charsets.UTF_8).lineSequence().firstOrNull() ?: return false
    return header.trim() == RCLONE_ENCRYPTED_HEADER
}

object BackupCrypto {

    private const val PBKDF2_ITERATIONS = 210_000
    private const val KEY_LENGTH_BITS = 256
    private const val SALT_LENGTH = 16
    private const val IV_LENGTH = 12
    private const val GCM_TAG_BITS = 128
    private const val ALGORITHM = "AES/GCM/NoPadding"

    /**
     * Encrypts data with AES-256-GCM using a PBKDF2-derived key.
     * Output format: [16-byte salt][12-byte IV][AES-GCM ciphertext+tag]
     */
    fun encrypt(password: String, data: ByteArray): ByteArray {
        val random = SecureRandom()
        val salt = ByteArray(SALT_LENGTH).also { random.nextBytes(it) }
        val iv = ByteArray(IV_LENGTH).also { random.nextBytes(it) }
        val key = deriveKey(password, salt)

        val cipher = Cipher.getInstance(ALGORITHM)
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(GCM_TAG_BITS, iv))
        val encrypted = cipher.doFinal(data)

        return salt + iv + encrypted
    }

    /**
     * Decrypts data encrypted by [encrypt].
     * @throws BadPasswordException if the password is wrong or data is corrupted
     */
    fun decrypt(password: String, encrypted: ByteArray): ByteArray {
        if (encrypted.size < SALT_LENGTH + IV_LENGTH + GCM_TAG_BITS / 8) {
            throw BadPasswordException()
        }

        val salt = encrypted.copyOfRange(0, SALT_LENGTH)
        val iv = encrypted.copyOfRange(SALT_LENGTH, SALT_LENGTH + IV_LENGTH)
        val ciphertext = encrypted.copyOfRange(SALT_LENGTH + IV_LENGTH, encrypted.size)
        val key = deriveKey(password, salt)

        return try {
            val cipher = Cipher.getInstance(ALGORITHM)
            cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_BITS, iv))
            cipher.doFinal(ciphertext)
        } catch (e: Exception) {
            throw BadPasswordException()
        }
    }

    private fun deriveKey(password: String, salt: ByteArray): SecretKeySpec {
        val spec = PBEKeySpec(password.toCharArray(), salt, PBKDF2_ITERATIONS, KEY_LENGTH_BITS)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val keyBytes = factory.generateSecret(spec).encoded
        return SecretKeySpec(keyBytes, "AES")
    }

    // --- Rclone-native config encryption ---

    private const val TAG = "BackupCrypto"

    /**
     * Encrypts a plain rclone.conf using rclone's native encryption format.
     * Uses `rclone config encryption set` with `--password-command` for non-interactive operation.
     * The result is portable to any rclone client.
     */
    fun encryptConfigWithRclone(context: Context, plainConfig: ByteArray, password: String): ByteArray {
        val rcloneBin = context.applicationInfo.nativeLibraryDir + "/librclone.so"
        val tempFile = File(context.filesDir, "rclone.conf-encrypt-tmp")
        val passFile = File(context.filesDir, "rclone-pass-tmp")

        try {
            tempFile.writeBytes(plainConfig)
            passFile.writeText(password)
            passFile.setReadable(true, true)

            val command = arrayOf(
                rcloneBin,
                "--config", tempFile.absolutePath,
                "--password-command", "cat ${passFile.absolutePath}",
                "config", "encryption", "set",
            )

            val process = Runtime.getRuntime().exec(command)
            process.waitFor()

            if (process.exitValue() != 0) {
                val error = process.errorStream.bufferedReader().readText()
                FLog.e(TAG, "rclone config encryption set failed: $error")
                throw IllegalStateException("Failed to encrypt config: $error")
            }

            val result = tempFile.readBytes()
            if (!isRcloneEncrypted(result)) {
                throw IllegalStateException("Config was not encrypted by rclone")
            }

            return result
        } finally {
            passFile.delete()
            tempFile.delete()
        }
    }

    /**
     * Decrypts a rclone-encrypted config using rclone's native decryption.
     * @throws BadPasswordException if the password is wrong
     */
    fun decryptConfigWithRclone(context: Context, encryptedConfig: ByteArray, password: String): ByteArray {
        val rcloneBin = context.applicationInfo.nativeLibraryDir + "/librclone.so"
        val tempFile = File(context.filesDir, "rclone.conf-decrypt-tmp")

        try {
            tempFile.writeBytes(encryptedConfig)

            val command = arrayOf(
                rcloneBin, "--config", tempFile.absolutePath,
                "--ask-password=false", "config", "show",
            )
            val env = arrayOf("RCLONE_CONFIG_PASS=$password")

            val process = Runtime.getRuntime().exec(command, env)
            val output = StringBuilder()
            BufferedReader(InputStreamReader(process.inputStream)).use { reader ->
                var line = reader.readLine()
                while (line != null) {
                    output.appendLine(line)
                    line = reader.readLine()
                }
            }
            process.waitFor()

            if (process.exitValue() != 0) {
                throw BadPasswordException()
            }

            return output.toString().toByteArray()
        } finally {
            tempFile.delete()
        }
    }

}
