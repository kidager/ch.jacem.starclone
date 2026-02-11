package ch.jacem.starclone.backup

import android.content.Context
import ch.jacem.starclone.Database.DatabaseHandler
import ch.jacem.starclone.Database.json.Importer
import ch.jacem.starclone.Database.json.SharedPreferencesBackup
import java.io.File

data class ImportResult(
    val remotesImported: Int,
    val tasksImported: Int,
    val triggersImported: Int,
    val filtersImported: Int,
    val preferencesImported: Boolean,
)

object BackupImporter {

    fun importBackup(
        context: Context,
        contents: BackupContents,
        categories: Set<BackupCategory>,
        selectedRemotes: Set<String>? = null,
    ): ImportResult {
        var remotesImported = 0
        var tasksImported = 0
        var triggersImported = 0
        var filtersImported = 0
        var preferencesImported = false

        if (BackupCategory.REMOTES in categories && contents.configBytes != null) {
            val configToImport = if (selectedRemotes != null) {
                val filtered = BackupReader.filterConfigByRemotes(
                    String(contents.configBytes), selectedRemotes,
                )
                filtered.toByteArray()
            } else {
                contents.configBytes
            }
            remotesImported = importConfig(context, configToImport)
        }

        if (BackupCategory.DATABASE in categories && contents.databaseJson != null) {
            val dbHandler = DatabaseHandler(context)
            val tasks = Importer.createTasklist(contents.databaseJson)
            for (task in tasks) {
                dbHandler.upsertTask(task)
            }
            tasksImported = tasks.size

            val triggers = Importer.createTriggerlist(contents.databaseJson)
            for (trigger in triggers) {
                dbHandler.upsertTrigger(trigger)
            }
            triggersImported = triggers.size

            val filters = Importer.createFilterList(contents.databaseJson)
            for (filter in filters) {
                dbHandler.upsertFilter(filter)
            }
            filtersImported = filters.size
        }

        if (BackupCategory.PREFERENCES in categories && contents.preferencesJson != null) {
            SharedPreferencesBackup.importJson(contents.preferencesJson, context)
            preferencesImported = true
        }

        return ImportResult(
            remotesImported = remotesImported,
            tasksImported = tasksImported,
            triggersImported = triggersImported,
            filtersImported = filtersImported,
            preferencesImported = preferencesImported,
        )
    }

    /**
     * Imports config by writing to a temp file, then replacing the current config.
     * Returns the number of remotes in the imported config.
     */
    private fun importConfig(context: Context, configBytes: ByteArray): Int {
        val filesDir = context.filesDir
        val configFile = File(filesDir, "rclone.conf")
        val tempFile = File(filesDir, "rclone.conf-import-tmp")

        // Write imported config to temp file
        tempFile.writeBytes(configBytes)

        // Parse remote names from the imported config
        val importedRemotes = BackupReader.parseRemoteNames(String(configBytes))

        if (importedRemotes.isEmpty()) {
            tempFile.delete()
            throw IllegalStateException("Imported rclone config contains no remotes")
        }

        // Replace current config with imported config
        // (merge by appending non-duplicate sections would be complex and error-prone;
        // rclone.conf format has provider-specific keys, so full replacement is safer)
        if (configFile.exists()) {
            val backupFile = File(filesDir, "rclone.conf-backup")
            configFile.copyTo(backupFile, overwrite = true)
        }
        tempFile.renameTo(configFile)

        return importedRemotes.size
    }
}
