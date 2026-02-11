package ca.pkay.rcloneexplorer.ui.screens.about

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ca.pkay.rcloneexplorer.R
import ca.pkay.rcloneexplorer.util.ActivityHelper

private data class Library(
    val name: String,
    val url: String,
    val licence: String,
    val licenceUrl: String,
)

@Composable
fun LibrariesScreen() {
    val context = LocalContext.current
    val libraries = remember { createLibraries() }

    LazyColumn(
        contentPadding = PaddingValues(vertical = 8.dp),
    ) {
        items(libraries, key = { it.name }) { lib ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        ActivityHelper.tryStartActivity(
                            context,
                            Intent(Intent.ACTION_VIEW, Uri.parse(lib.url)),
                        )
                    }
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            ) {
                Text(
                    text = lib.name,
                    style = MaterialTheme.typography.bodyLarge,
                )
                Text(
                    text = lib.licence,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable {
                        ActivityHelper.tryStartActivity(
                            context,
                            Intent(Intent.ACTION_VIEW, Uri.parse(lib.licenceUrl)),
                        )
                    },
                )
            }
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 48.dp),
                horizontalArrangement = Arrangement.Center,
            ) {
                Text(text = stringResource(R.string.app_icon_made_by))
                Text(
                    text = stringResource(R.string.smashicons),
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .clickable {
                            ActivityHelper.tryStartActivity(
                                context,
                                Intent(Intent.ACTION_VIEW, Uri.parse("https://www.flaticon.com/authors/smashicons")),
                            )
                        },
                )
                Text(text = stringResource(R.string.from))
                Text(
                    text = stringResource(R.string.flaticon),
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .clickable {
                            ActivityHelper.tryStartActivity(
                                context,
                                Intent(Intent.ACTION_VIEW, Uri.parse("https://www.flaticon.com/")),
                            )
                        },
                )
            }
        }

        item { Spacer(Modifier.height(80.dp)) }
    }
}

private fun createLibraries(): List<Library> = listOf(
    Library(
        "Android Support Libraries",
        "https://developer.android.com/topic/libraries/support-library/",
        "Licensed under Apache-2.0",
        "http://www.apache.org/licenses/LICENSE-2.0",
    ),
    Library(
        "Floating Action Button Speed Dial",
        "https://github.com/leinardi/FloatingActionButtonSpeedDial",
        "Licensed under Apache-2.0",
        "https://github.com/leinardi/FloatingActionButtonSpeedDial/blob/master/LICENSE",
    ),
    Library(
        "Glide",
        "https://github.com/bumptech/glide",
        "BSD, part MIT and Apache 2.0",
        "https://github.com/bumptech/glide/blob/master/LICENSE",
    ),
    Library(
        "MarkDown View",
        "https://github.com/falnatsheh/MarkdownView",
        "Licensed under Apache-2.0",
        "https://github.com/falnatsheh/MarkdownView/blob/master/license.txt",
    ),
    Library(
        "Material Design",
        "https://github.com/Templarian/MaterialDesign",
        "Licensed under SIL Open Font 1.1",
        "http://scripts.sil.org/cms/scripts/page.php?item_id=OFL_web",
    ),
    Library(
        "Rclone",
        "https://github.com/ncw/rclone",
        "Licensed under MIT",
        "https://github.com/ncw/rclone/blob/master/COPYING",
    ),
    Library(
        "Recyclerview Animators",
        "https://github.com/wasabeef/recyclerview-animators",
        "Licensed under Apache 2.0",
        "https://github.com/wasabeef/recyclerview-animators/blob/master/LICENSE",
    ),
    Library(
        "Toasty",
        "https://github.com/GrenderG/Toasty",
        "Licensed under LGPL-3.0",
        "https://github.com/GrenderG/Toasty/blob/master/LICENSE",
    ),
    Library(
        "rcloneExplorer",
        "https://github.com/kaczmarkiewiczp/rcloneExplorer",
        "Licensed under MIT",
        "https://github.com/kaczmarkiewiczp/rcloneExplorer/blob/e5afbfda50c747bc088543dab9576d80bf99ec63/LICENSE",
    ),
    Library(
        "OkHttp",
        "https://square.github.io/okhttp",
        "Licensed under Apache 2.0",
        "https://square.github.io/okhttp/#license",
    ),
    Library(
        "RFC 3339 Date Parser",
        "https://github.com/x0b/rfc3339parser",
        "Licensed under MIT",
        "https://github.com/x0b/rfc3339parser/blob/master/LICENSE",
    ),
    Library(
        "unDraw - Open Source Illustrations",
        "https://undraw.co/",
        "Licensed under Custom License",
        "https://undraw.co/license",
    ),
)
