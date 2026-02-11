package ch.jacem.starclone.backup

import org.json.JSONArray
import org.json.JSONObject

enum class BackupCategory { REMOTES, DATABASE, PREFERENCES }

enum class EncryptionScope { NONE, CONFIG_ONLY, EVERYTHING }

data class BackupEntry(
    val filename: String,
    val category: BackupCategory,
    val encrypted: Boolean,
)

data class BackupManifest(
    val version: Int = 1,
    val encryptionScope: EncryptionScope = EncryptionScope.NONE,
    val entries: List<BackupEntry> = emptyList(),
) {
    fun toJson(): String {
        val json = JSONObject()
        json.put("version", version)
        json.put("encryptionScope", encryptionScope.name)
        val entriesArray = JSONArray()
        for (entry in entries) {
            entriesArray.put(JSONObject().apply {
                put("filename", entry.filename)
                put("category", entry.category.name)
                put("encrypted", entry.encrypted)
            })
        }
        json.put("entries", entriesArray)
        return json.toString(2)
    }

    companion object {
        fun fromJson(json: String): BackupManifest {
            val obj = JSONObject(json)
            val version = obj.getInt("version")
            val scope = EncryptionScope.valueOf(obj.getString("encryptionScope"))
            val entriesArray = obj.getJSONArray("entries")
            val entries = (0 until entriesArray.length()).map { i ->
                val e = entriesArray.getJSONObject(i)
                BackupEntry(
                    filename = e.getString("filename"),
                    category = BackupCategory.valueOf(e.getString("category")),
                    encrypted = e.getBoolean("encrypted"),
                )
            }
            return BackupManifest(version = version, encryptionScope = scope, entries = entries)
        }
    }
}
