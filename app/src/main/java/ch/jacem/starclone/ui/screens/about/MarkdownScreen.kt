package ch.jacem.starclone.ui.screens.about

import android.widget.Toast
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import ch.jacem.starclone.util.MarkdownView

@Composable
fun MarkdownScreen(assetPath: String) {
    val context = LocalContext.current
    AndroidView(
        factory = {
            try {
                MarkdownView(it).apply { loadAsset(assetPath) }
            } catch (e: Exception) {
                if (e.message?.contains("Failed to load WebView provider") == true) {
                    Toast.makeText(it, "Install WebView and try again", Toast.LENGTH_LONG).show()
                    (it as? android.app.Activity)?.finish()
                }
                throw e
            }
        },
        modifier = Modifier.fillMaxSize(),
    )
}
