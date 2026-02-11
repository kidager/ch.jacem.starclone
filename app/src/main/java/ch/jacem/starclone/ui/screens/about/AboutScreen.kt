package ch.jacem.starclone.ui.screens.about

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Book
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ch.jacem.starclone.Activities.AboutLibsActivity
import ch.jacem.starclone.Activities.ChangelogActivity
import ch.jacem.starclone.Activities.ContributorActivity
import ch.jacem.starclone.BuildConfig
import ch.jacem.starclone.R
import ch.jacem.starclone.Rclone
import ch.jacem.starclone.ui.components.StarCloneListItem
import ch.jacem.starclone.util.ActivityHelper

@Composable
fun AboutScreen() {
    val context = LocalContext.current
    val rcloneVersion = remember { Rclone(context).rcloneVersion }

    LazyColumn(
        contentPadding = PaddingValues(vertical = 8.dp),
    ) {
        item {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)) {
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.headlineMedium,
                )
                Text(
                    text = "v${BuildConfig.VERSION_NAME}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = "rclone $rcloneVersion",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        item { SectionHeader("Info") }

        item {
            StarCloneListItem(
                headlineText = stringResource(R.string.changelog),
                leadingIcon = Icons.Outlined.Book,
                onClick = {
                    context.startActivity(Intent(context, ChangelogActivity::class.java))
                },
            )
        }
        item {
            StarCloneListItem(
                headlineText = stringResource(R.string.license_and_contributors),
                leadingIcon = Icons.Outlined.People,
                onClick = {
                    context.startActivity(Intent(context, ContributorActivity::class.java))
                },
            )
        }
        item {
            StarCloneListItem(
                headlineText = "Open Source Libraries",
                leadingIcon = Icons.Outlined.Code,
                onClick = {
                    context.startActivity(Intent(context, AboutLibsActivity::class.java))
                },
            )
        }

        item { SectionHeader("Links") }

        item {
            StarCloneListItem(
                headlineText = stringResource(R.string.star_on_github),
                leadingIcon = Icons.Outlined.Star,
                onClick = {
                    ActivityHelper.tryStartActivity(
                        context,
                        Intent(Intent.ACTION_VIEW, Uri.parse(context.getString(R.string.github_app_url))),
                    )
                },
            )
        }
        item {
            StarCloneListItem(
                headlineText = stringResource(R.string.report_a_bug),
                leadingIcon = Icons.Outlined.BugReport,
                onClick = {
                    ActivityHelper.tryStartActivity(
                        context,
                        Intent(Intent.ACTION_VIEW, Uri.parse(context.getString(R.string.github_issue_url))),
                    )
                },
            )
        }

        item { SectionHeader("Authors") }

        item {
            StarCloneListItem(
                headlineText = stringResource(R.string.maintainer),
                supportingText = stringResource(R.string.maintainer_name),
                leadingIcon = Icons.Outlined.Info,
                onClick = {
                    ActivityHelper.tryStartActivity(
                        context,
                        Intent(Intent.ACTION_VIEW, Uri.parse(context.getString(R.string.github_maintainer_url))),
                    )
                },
            )
        }
        item {
            StarCloneListItem(
                headlineText = stringResource(R.string.old_maintainer),
                supportingText = stringResource(R.string.old_maintainer_name),
                leadingIcon = Icons.Outlined.Info,
                onClick = {
                    ActivityHelper.tryStartActivity(
                        context,
                        Intent(Intent.ACTION_VIEW, Uri.parse(context.getString(R.string.github_maintainer_url))),
                    )
                },
            )
        }
        item {
            StarCloneListItem(
                headlineText = stringResource(R.string.author),
                supportingText = "patrykcoding",
                leadingIcon = Icons.Outlined.Info,
                onClick = {
                    ActivityHelper.tryStartActivity(
                        context,
                        Intent(Intent.ACTION_VIEW, Uri.parse(context.getString(R.string.github_author_url))),
                    )
                },
            )
        }

        item { Spacer(Modifier.height(80.dp)) }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
    )
}
