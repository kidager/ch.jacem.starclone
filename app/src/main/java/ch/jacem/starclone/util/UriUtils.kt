package ch.jacem.starclone.util

import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.DocumentsContract
import ch.jacem.starclone.BuildConfig

object UriUtils {

    private const val VCP_ROOT_PREFIX = "rclone/remotes/"

    /**
     * Resolve a tree URI (from OpenDocumentTree) to a filesystem path.
     * e.g. content://com.android.externalstorage.documents/tree/primary%3ADownload
     *      → /storage/emulated/0/Download
     */
    fun getPathFromTreeUri(context: Context, treeUri: Uri): String? {
        val docId = DocumentsContract.getTreeDocumentId(treeUri)
        return resolveExternalStorageDocId(docId)
    }

    /**
     * Resolve a document URI (from OpenDocument/OpenMultipleDocuments) to a filesystem path.
     * e.g. content://com.android.externalstorage.documents/document/primary%3ADCIM%2Fphoto.jpg
     *      → /storage/emulated/0/DCIM/photo.jpg
     */
    fun getPathFromDocumentUri(context: Context, documentUri: Uri): String? {
        if (!DocumentsContract.isDocumentUri(context, documentUri)) return null
        val authority = documentUri.authority ?: return null

        return when (authority) {
            "com.android.externalstorage.documents" -> {
                val docId = DocumentsContract.getDocumentId(documentUri)
                resolveExternalStorageDocId(docId)
            }
            "com.android.providers.downloads.documents" -> {
                val docId = DocumentsContract.getDocumentId(documentUri)
                if (docId.startsWith("raw:")) docId.removePrefix("raw:") else null
            }
            else -> null
        }
    }

    /**
     * Check if a URI comes from our VirtualContentProvider.
     */
    fun isVcpUri(uri: Uri): Boolean {
        return uri.authority == BuildConfig.VCP_AUTHORITY
    }

    /**
     * Parse a VCP document URI into an rclone source path for remote-to-remote copy.
     * e.g. content://<vcp>/document/rclone%2Fremotes%2Fgdrive%3A%2Ffolder%2Ffile.txt
     *      → gdrive:folder/file.txt
     */
    fun getRclonePathFromVcpUri(context: Context, uri: Uri): String? {
        if (!DocumentsContract.isDocumentUri(context, uri)) return null
        if (uri.authority != BuildConfig.VCP_AUTHORITY) return null

        var docId = DocumentsContract.getDocumentId(uri)
        // Strip root prefix (may be prepended multiple times by buggy SAF clients)
        while (docId.startsWith(VCP_ROOT_PREFIX)) {
            docId = docId.substring(VCP_ROOT_PREFIX.length)
        }
        // docId is now like "gdrive:/folder/file.txt"
        val colonIdx = docId.indexOf(':')
        if (colonIdx < 0) return null

        val remoteName = docId.substring(0, colonIdx)
        var path = docId.substring(colonIdx + 1)
        // Strip leading slash — rclone paths don't use it
        if (path.startsWith("/")) {
            path = path.substring(1)
        }

        return "$remoteName:$path"
    }

    /**
     * Parse an external storage document ID like "primary:DCIM/photo.jpg"
     * or "1234-5678:folder/file.txt" into a real filesystem path.
     */
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
}
