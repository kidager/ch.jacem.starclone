package ch.jacem.starclone.backup

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupCryptoTest {

    @Test
    fun `encrypt then decrypt returns original data`() {
        val password = "test-password-123"
        val original = "Hello, World! This is a test config.".toByteArray()

        val encrypted = BackupCrypto.encrypt(password, original)
        val decrypted = BackupCrypto.decrypt(password, encrypted)

        assertArrayEquals(original, decrypted)
    }

    @Test
    fun `encrypt produces different output than input`() {
        val password = "test-password-123"
        val original = "some plain text config data".toByteArray()

        val encrypted = BackupCrypto.encrypt(password, original)

        assertFalse(original.contentEquals(encrypted))
    }

    @Test
    fun `encrypt produces different ciphertext each time due to random salt and IV`() {
        val password = "test-password-123"
        val original = "same input data".toByteArray()

        val encrypted1 = BackupCrypto.encrypt(password, original)
        val encrypted2 = BackupCrypto.encrypt(password, original)

        assertFalse(encrypted1.contentEquals(encrypted2))
    }

    @Test(expected = BadPasswordException::class)
    fun `decrypt with wrong password throws BadPasswordException`() {
        val original = "secret data".toByteArray()
        val encrypted = BackupCrypto.encrypt("correct-password", original)

        BackupCrypto.decrypt("wrong-password", encrypted)
    }

    @Test(expected = BadPasswordException::class)
    fun `decrypt with corrupted data throws BadPasswordException`() {
        val garbage = ByteArray(100) { it.toByte() }

        BackupCrypto.decrypt("any-password", garbage)
    }

    @Test(expected = BadPasswordException::class)
    fun `decrypt with too-short data throws BadPasswordException`() {
        val tooShort = ByteArray(10)

        BackupCrypto.decrypt("any-password", tooShort)
    }

    @Test
    fun `encrypt and decrypt large data`() {
        val password = "test-password-123"
        val original = ByteArray(1_000_000) { (it % 256).toByte() }

        val encrypted = BackupCrypto.encrypt(password, original)
        val decrypted = BackupCrypto.decrypt(password, encrypted)

        assertArrayEquals(original, decrypted)
    }

    @Test
    fun `encrypt and decrypt empty data`() {
        val password = "test-password-123"
        val original = ByteArray(0)

        val encrypted = BackupCrypto.encrypt(password, original)
        val decrypted = BackupCrypto.decrypt(password, encrypted)

        assertArrayEquals(original, decrypted)
    }

    @Test
    fun `isRcloneEncrypted detects encrypted header`() {
        val encrypted = "# Encrypted rclone configuration File\n\nSOME_ENCRYPTED_DATA".toByteArray()
        assertTrue(isRcloneEncrypted(encrypted))
    }

    @Test
    fun `isRcloneEncrypted returns false for plain config`() {
        val plain = "[myremote]\ntype = drive\nclient_id = xxx".toByteArray()
        assertFalse(isRcloneEncrypted(plain))
    }

    @Test
    fun `isRcloneEncrypted returns false for empty data`() {
        assertFalse(isRcloneEncrypted(ByteArray(0)))
    }
}
