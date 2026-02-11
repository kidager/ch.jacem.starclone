package ca.pkay.rcloneexplorer.backup

import android.content.Context
import android.net.Uri
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.zip.ZipInputStream

data class BackupContents(
    val manifest: BackupManifest?,
    val isLegacyFormat: Boolean,
    val isEncrypted: Boolean,
    val remoteNames: List<String>,
    val taskCount: Int,
    val triggerCount: Int,
    val filterCount: Int,
    val hasPreferences: Boolean,
    val configBytes: ByteArray?,
    val databaseJson: String?,
    val preferencesJson: String?,
)

object BackupReader {

    /**
     * Reads a backup file. Supports:
     * - New format ZIP (with manifest.json)
     * - Legacy ZIP (rclone.conf + rcx.json + rcx.prefs, no manifest)
     * - Bare rclone.conf file
     *
     * @throws BadPasswordException if the password is wrong
     */
    fun readBackup(context: Context, sourceUri: Uri, password: String?): BackupContents {
        val inputStream = context.contentResolver.openInputStream(sourceUri)
            ?: throw IllegalStateException("Cannot open input stream")

        val rawBytes = inputStream.use { it.readBytes() }

        // Try as ZIP first
        val zipEntries = tryReadZip(rawBytes)
        if (zipEntries != null) {
            return parseZip(context, zipEntries, password)
        }

        // Not a ZIP — treat as bare rclone.conf
        val configText = rawBytes.toString(Charsets.UTF_8)
        val remoteNames = parseRemoteNames(configText)
        return BackupContents(
            manifest = null,
            isLegacyFormat = false,
            isEncrypted = false,
            remoteNames = remoteNames,
            taskCount = 0,
            triggerCount = 0,
            filterCount = 0,
            hasPreferences = false,
            configBytes = rawBytes,
            databaseJson = null,
            preferencesJson = null,
        )
    }

    private fun tryReadZip(rawBytes: ByteArray): Map<String, ByteArray>? {
        return try {
            val entries = mutableMapOf<String, ByteArray>()
            ZipInputStream(rawBytes.inputStream()).use { zis ->
                var entry = zis.nextEntry
                while (entry != null) {
                    val baos = ByteArrayOutputStream()
                    zis.copyTo(baos)
                    entries[entry.name] = baos.toByteArray()
                    zis.closeEntry()
                    entry = zis.nextEntry
                }
            }
            if (entries.isEmpty()) null else entries
        } catch (_: Exception) {
            null
        }
    }

    private fun parseZip(
        context: Context,
        zipEntries: Map<String, ByteArray>,
        password: String?,
    ): BackupContents {
        val manifestBytes = zipEntries["manifest.json"]

        if (manifestBytes != null) {
            return parseNewFormat(context, zipEntries, BackupManifest.fromJson(String(manifestBytes)), password)
        }

        // Legacy format — no manifest, all unencrypted
        return parseLegacyFormat(zipEntries)
    }

    private fun parseNewFormat(
        context: Context,
        zipEntries: Map<String, ByteArray>,
        manifest: BackupManifest,
        password: String?,
    ): BackupContents {
        // Check manifest flags AND detect rclone-encrypted config files
        val isEncrypted = manifest.entries.any { it.encrypted } ||
            manifest.entries.any { entry ->
                entry.category == BackupCategory.REMOTES &&
                    zipEntries[entry.filename]?.let { isRcloneEncrypted(it) } == true
            }

        // If encrypted entries exist but no password given, return partial info
        if (isEncrypted && password == null) {
            return BackupContents(
                manifest = manifest,
                isLegacyFormat = false,
                isEncrypted = true,
                remoteNames = emptyList(),
                taskCount = 0,
                triggerCount = 0,
                filterCount = 0,
                hasPreferences = manifest.entries.any { it.category == BackupCategory.PREFERENCES },
                configBytes = null,
                databaseJson = null,
                preferencesJson = null,
            )
        }

        var configBytes: ByteArray? = null
        var databaseJson: String? = null
        var preferencesJson: String? = null

        for (entry in manifest.entries) {
            val raw = zipEntries[entry.filename] ?: continue
            val data = if (entry.encrypted) {
                if (entry.category == BackupCategory.REMOTES && isRcloneEncrypted(raw)) {
                    // Rclone-native encryption — decrypt using rclone binary
                    BackupCrypto.decryptConfigWithRclone(context, raw, password!!)
                } else {
                    BackupCrypto.decrypt(password!!, raw)
                }
            } else {
                raw
            }

            when (entry.category) {
                BackupCategory.REMOTES -> configBytes = data
                BackupCategory.DATABASE -> databaseJson = String(data)
                BackupCategory.PREFERENCES -> preferencesJson = String(data)
            }
        }

        val remoteNames = if (configBytes != null) parseRemoteNames(String(configBytes)) else emptyList()
        val (tasks, triggers, filters) = countDatabaseItems(databaseJson)

        return BackupContents(
            manifest = manifest,
            isLegacyFormat = false,
            isEncrypted = isEncrypted,
            remoteNames = remoteNames,
            taskCount = tasks,
            triggerCount = triggers,
            filterCount = filters,
            hasPreferences = preferencesJson != null,
            configBytes = configBytes,
            databaseJson = databaseJson,
            preferencesJson = preferencesJson,
        )
    }

    private fun parseLegacyFormat(zipEntries: Map<String, ByteArray>): BackupContents {
        val configBytes = zipEntries["rclone.conf"]
        val databaseJson = zipEntries["rcx.json"]?.let { String(it) }
        val prefsJson = zipEntries["rcx.prefs"]?.let { String(it) }

        val remoteNames = if (configBytes != null) parseRemoteNames(String(configBytes)) else emptyList()
        val (tasks, triggers, filters) = countDatabaseItems(databaseJson)

        return BackupContents(
            manifest = null,
            isLegacyFormat = true,
            isEncrypted = false,
            remoteNames = remoteNames,
            taskCount = tasks,
            triggerCount = triggers,
            filterCount = filters,
            hasPreferences = prefsJson != null,
            configBytes = configBytes,
            databaseJson = databaseJson,
            preferencesJson = prefsJson,
        )
    }

    private val SECTION_REGEX = Regex("""^\[(.+)]$""", RegexOption.MULTILINE)

    internal fun parseRemoteNames(configText: String): List<String> {
        return SECTION_REGEX.findAll(configText).map { it.groupValues[1] }.toList()
    }

    /**
     * Filters an rclone.conf to only include the given remote sections.
     * Returns the filtered config text.
     */
    internal fun filterConfigByRemotes(configText: String, remotes: Set<String>): String {
        val sections = parseConfigSections(configText)
        return remotes.mapNotNull { sections[it] }.joinToString("\n")
    }

    /**
     * Parses rclone.conf into a map of remoteName -> full section text (including [header]).
     */
    private fun parseConfigSections(configText: String): Map<String, String> {
        val sections = mutableMapOf<String, String>()
        var currentName: String? = null
        val currentLines = mutableListOf<String>()

        for (line in configText.lines()) {
            val match = SECTION_REGEX.find(line)
            if (match != null) {
                if (currentName != null) {
                    sections[currentName] = currentLines.joinToString("\n").trimEnd()
                }
                currentName = match.groupValues[1]
                currentLines.clear()
                currentLines.add(line)
            } else if (currentName != null) {
                currentLines.add(line)
            }
        }
        if (currentName != null) {
            sections[currentName] = currentLines.joinToString("\n").trimEnd()
        }
        return sections
    }

    private fun countDatabaseItems(json: String?): Triple<Int, Int, Int> {
        if (json == null) return Triple(0, 0, 0)
        return try {
            val obj = JSONObject(json)
            Triple(
                obj.optJSONArray("tasks")?.length() ?: 0,
                obj.optJSONArray("trigger")?.length() ?: 0,
                obj.optJSONArray("filters")?.length() ?: 0,
            )
        } catch (_: Exception) {
            Triple(0, 0, 0)
        }
    }
}
