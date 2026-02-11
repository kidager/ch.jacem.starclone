package ch.jacem.starclone.backup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupReaderTest {

    @Test
    fun `parseRemoteNames extracts remote names from config`() {
        val config = """
            [gdrive]
            type = drive
            client_id = xxx

            [s3bucket]
            type = s3
            provider = AWS
        """.trimIndent()

        val names = BackupReader.parseRemoteNames(config)

        assertEquals(listOf("gdrive", "s3bucket"), names)
    }

    @Test
    fun `parseRemoteNames returns empty for empty config`() {
        assertEquals(emptyList<String>(), BackupReader.parseRemoteNames(""))
    }

    @Test
    fun `parseRemoteNames handles single remote`() {
        val config = "[onedrive]\ntype = onedrive\n"
        assertEquals(listOf("onedrive"), BackupReader.parseRemoteNames(config))
    }

    @Test
    fun `filterConfigByRemotes keeps only selected remotes`() {
        val config = """
            [gdrive]
            type = drive
            client_id = xxx

            [s3bucket]
            type = s3
            provider = AWS

            [local]
            type = local
        """.trimIndent()

        val filtered = BackupReader.filterConfigByRemotes(config, setOf("gdrive", "local"))
        val names = BackupReader.parseRemoteNames(filtered)

        assertEquals(listOf("gdrive", "local"), names)
        assertTrue(filtered.contains("[gdrive]"))
        assertTrue(filtered.contains("[local]"))
        assertTrue(!filtered.contains("[s3bucket]"))
    }

    @Test
    fun `filterConfigByRemotes returns empty for no matches`() {
        val config = "[gdrive]\ntype = drive\n"
        val filtered = BackupReader.filterConfigByRemotes(config, setOf("nonexistent"))

        assertEquals("", filtered)
    }
}
