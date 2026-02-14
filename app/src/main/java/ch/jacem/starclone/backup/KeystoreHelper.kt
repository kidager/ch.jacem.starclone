package ch.jacem.starclone.backup

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

private const val KEYSTORE_PROVIDER = "AndroidKeyStore"
private const val KEY_ALIAS = "starclone_backup_key"
private const val PREFS_FILE = "starclone_secure_backup"
private const val ALGORITHM = "AES/GCM/NoPadding"
private const val GCM_TAG_BITS = 128

object KeystoreHelper {

    fun storeSecret(context: Context, key: String, value: String) {
        val secretKey = getOrCreateKey()
        val cipher = Cipher.getInstance(ALGORITHM)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)

        val encrypted = cipher.doFinal(value.toByteArray())
        val iv = cipher.iv

        val prefs = context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)
        prefs.edit()
            .putString("${key}_data", android.util.Base64.encodeToString(encrypted, android.util.Base64.NO_WRAP))
            .putString("${key}_iv", android.util.Base64.encodeToString(iv, android.util.Base64.NO_WRAP))
            .apply()
    }

    fun getSecret(context: Context, key: String): String? {
        val prefs = context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)
        val dataB64 = prefs.getString("${key}_data", null) ?: return null
        val ivB64 = prefs.getString("${key}_iv", null) ?: return null

        val encrypted = android.util.Base64.decode(dataB64, android.util.Base64.NO_WRAP)
        val iv = android.util.Base64.decode(ivB64, android.util.Base64.NO_WRAP)

        val secretKey = getOrCreateKey()
        val cipher = Cipher.getInstance(ALGORITHM)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(GCM_TAG_BITS, iv))

        return String(cipher.doFinal(encrypted))
    }

    fun deleteSecret(context: Context, key: String) {
        val prefs = context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)
        prefs.edit()
            .remove("${key}_data")
            .remove("${key}_iv")
            .apply()
    }

    fun hasSecret(context: Context, key: String): Boolean {
        val prefs = context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)
        return prefs.contains("${key}_data")
    }

    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER)
        keyStore.load(null)

        keyStore.getKey(KEY_ALIAS, null)?.let { return it as SecretKey }

        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE_PROVIDER)
        keyGenerator.init(
            KeyGenParameterSpec.Builder(KEY_ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()
        )
        return keyGenerator.generateKey()
    }
}
