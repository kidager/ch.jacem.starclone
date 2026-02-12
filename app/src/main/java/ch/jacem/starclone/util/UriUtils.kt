package ch.jacem.starclone.util

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.provider.OpenableColumns
import ch.jacem.starclone.BuildConfig
import ch.jacem.starclone.util.FLog
import java.io.File

object UriUtils {

    private const val VCP_ROOT_PREFIX = "rclone/remotes/"

    /**
     * Resolve a tree URI (from OpenDocumentTree) to a filesystem path.
     */
    @Suppress("UNUSED_PARAMETER")
    fun getPathFromTreeUri(context: Context, treeUri: Uri): String? {
        val docId = DocumentsContract.getTreeDocumentId(treeUri)
        return resolveExternalStorageDocId(docId)
    }

    /**
     * Resolve a document URI (from OpenDocument/OpenMultipleDocuments) to a filesystem path.
     * Handles external storage, downloads, media providers, and content URIs.
     * Returns null if the path cannot be resolved (e.g., cloud-only SAF provider).
     */
    fun getPathFromDocumentUri(context: Context, documentUri: Uri): String? {
        // Try document URI resolution first
        if (DocumentsContract.isDocumentUri(context, documentUri)) {
            val authority = documentUri.authority ?: return null
            val resolved = resolveDocumentAuthority(context, authority, documentUri)
            if (resolved != null) return resolved
        }

        // Fallback: try querying DATA column directly for any content:// URI
        if (documentUri.scheme == "content") {
            val dataPath = queryDataColumn(context, documentUri)
            if (dataPath != null) return dataPath
        }

        // file:// URI
        if (documentUri.scheme == "file") {
            return documentUri.path
        }

        return null
    }

    private fun resolveDocumentAuthority(context: Context, authority: String, documentUri: Uri): String? {
        return when (authority) {
            "com.android.externalstorage.documents" -> {
                val docId = DocumentsContract.getDocumentId(documentUri)
                resolveExternalStorageDocId(docId)
            }
            "com.android.providers.downloads.documents" -> {
                val docId = DocumentsContract.getDocumentId(documentUri)
                when {
                    docId.startsWith("raw:") -> docId.removePrefix("raw:")
                    docId.startsWith("msf:") -> {
                        val mediaId = docId.removePrefix("msf:").toLongOrNull()
                        if (mediaId != null) {
                            val mediaUri = ContentUris.withAppendedId(
                                MediaStore.Files.getContentUri("external"),
                                mediaId,
                            )
                            queryDataColumn(context, mediaUri)
                        } else {
                            queryDataColumn(context, documentUri)
                        }
                    }
                    docId.toLongOrNull() != null -> {
                        val contentUri = ContentUris.withAppendedId(
                            Uri.parse("content://downloads/public_downloads"),
                            docId.toLong(),
                        )
                        queryDataColumn(context, contentUri)
                    }
                    else -> null
                }
            }
            "com.android.providers.media.documents" -> {
                val docId = DocumentsContract.getDocumentId(documentUri)
                resolveMediaDocId(context, docId)
            }
            else -> null
        }
    }

    private fun resolveMediaDocId(context: Context, docId: String): String? {
        val split = docId.split(":", limit = 2)
        if (split.size != 2) return null
        val type = split[0]
        val id = split[1].toLongOrNull() ?: return null

        val contentUri = when (type) {
            "image" -> MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            "video" -> MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            "audio" -> MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
            "document" -> MediaStore.Files.getContentUri("external")
            else -> return null
        }
        return queryDataColumn(context, ContentUris.withAppendedId(contentUri, id))
    }

    fun isVcpUri(uri: Uri): Boolean {
        return uri.authority == BuildConfig.VCP_AUTHORITY
    }

    /**
     * Parse a VCP document URI into an rclone source path for remote-to-remote copy.
     */
    fun getRclonePathFromVcpUri(context: Context, uri: Uri): String? {
        if (!DocumentsContract.isDocumentUri(context, uri)) return null
        if (uri.authority != BuildConfig.VCP_AUTHORITY) return null

        var docId = DocumentsContract.getDocumentId(uri)
        while (docId.startsWith(VCP_ROOT_PREFIX)) {
            docId = docId.substring(VCP_ROOT_PREFIX.length)
        }
        val colonIdx = docId.indexOf(':')
        if (colonIdx < 0) return null

        val remoteName = docId.substring(0, colonIdx)
        var path = docId.substring(colonIdx + 1)
        if (path.startsWith("/")) {
            path = path.substring(1)
        }

        return "$remoteName:$path"
    }

    /**
     * Copy a content URI to the app cache and return the cached file path.
     * Used as a last resort for cloud-only SAF providers.
     */
    fun copyToCache(context: Context, uri: Uri): String? {
        val fileName = getFileName(context, uri) ?: "upload_${System.currentTimeMillis()}"
        val cacheDir = File(context.cacheDir, "uploads").apply { mkdirs() }
        val destFile = File(cacheDir, fileName)

        return try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                destFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            destFile.absolutePath
        } catch (e: Exception) {
            FLog.e("UriUtils", "copyToCache: failed", e)
            null
        }
    }

    private fun getFileName(context: Context, uri: Uri): String? {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val name = cursor.getString(0)
                if (!name.isNullOrBlank()) return name
            }
        }
        return uri.lastPathSegment
    }

    private fun resolveExternalStorageDocId(docId: String): String? {
        val parts = docId.split(":", limit = 2)
        if (parts.size != 2) return null

        val volume = parts[0]
        val relativePath = parts[1]

        val basePath = if (volume.equals("primary", ignoreCase = true)) {
            Environment.getExternalStorageDirectory().absolutePath
        } else {
            "/storage/$volume"
        }

        return if (relativePath.isEmpty()) basePath else "$basePath/$relativePath"
    }

    @Suppress("DEPRECATION")
    private fun queryDataColumn(context: Context, uri: Uri): String? {
        try {
            context.contentResolver.query(
                uri,
                arrayOf(MediaStore.MediaColumns.DATA),
                null,
                null,
                null,
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val idx = cursor.getColumnIndex(MediaStore.MediaColumns.DATA)
                    if (idx >= 0) {
                        val path = cursor.getString(idx)
                        if (!path.isNullOrBlank()) return path
                    }
                }
            }
        } catch (_: Exception) {
            // SecurityException, IllegalArgumentException, etc.
        }
        return null
    }
}
