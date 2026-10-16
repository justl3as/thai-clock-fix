package com.thaiclockfix.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Source
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.thaiclockfix.R

internal object AboutLinks {
    const val SOURCE = "https://github.com/justl3as/thai-clock-fix"
    const val RELEASES = "https://github.com/justl3as/thai-clock-fix/releases/latest"
    const val ISSUES = "https://github.com/justl3as/thai-clock-fix/issues"
    const val SECURITY = "https://github.com/justl3as/thai-clock-fix/security/policy"
    const val SHIZUKU = "https://github.com/RikkaApps/Shizuku"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    versionName: String,
    versionCode: Long,
    applicationId: String,
    diagnosticInfo: String,
    onBack: () -> Unit,
    onOpenUrl: (String) -> Unit,
    onCopyDiagnostic: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    BackHandler(onBack = onBack)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.about_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.about_back_cd),
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
        modifier = modifier.fillMaxSize(),
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            AppIdentity(
                versionName = versionName,
                versionCode = versionCode,
            )

            AboutSection(title = stringResource(R.string.about_privacy_title)) {
                AboutBullet(stringResource(R.string.about_privacy_no_internet))
                AboutBullet(stringResource(R.string.about_privacy_no_ads_analytics))
                AboutBullet(stringResource(R.string.about_privacy_local_only))
                AboutBullet(stringResource(R.string.about_privacy_shizuku))
            }

            AboutSection(title = stringResource(R.string.about_project_title)) {
                AboutLinkRow(
                    title = stringResource(R.string.about_source),
                    icon = Icons.Default.Source,
                    testTag = "about_link_source",
                    onClick = { onOpenUrl(AboutLinks.SOURCE) },
                )
                HorizontalDivider()
                AboutLinkRow(
                    title = stringResource(R.string.about_releases),
                    icon = Icons.Default.Download,
                    testTag = "about_link_releases",
                    onClick = { onOpenUrl(AboutLinks.RELEASES) },
                )
                HorizontalDivider()
                AboutLinkRow(
                    title = stringResource(R.string.about_issues),
                    icon = Icons.Default.BugReport,
                    testTag = "about_link_issues",
                    onClick = { onOpenUrl(AboutLinks.ISSUES) },
                )
                HorizontalDivider()
                AboutLinkRow(
                    title = stringResource(R.string.about_security),
                    icon = Icons.Default.Security,
                    testTag = "about_link_security",
                    onClick = { onOpenUrl(AboutLinks.SECURITY) },
                )
            }

            AboutSection(title = stringResource(R.string.about_shizuku_title)) {
                Text(
                    text = stringResource(R.string.about_shizuku_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(4.dp))
                AboutLinkRow(
                    title = stringResource(R.string.about_shizuku_link),
                    icon = Icons.Default.Code,
                    testTag = "about_link_shizuku",
                    onClick = { onOpenUrl(AboutLinks.SHIZUKU) },
                )
            }

            AboutSection(title = stringResource(R.string.about_support_title)) {
                Text(
                    text = stringResource(R.string.about_support_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = { onCopyDiagnostic(diagnosticInfo) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("about_copy_diagnostics"),
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                    Text(stringResource(R.string.about_copy_diagnostics))
                }
            }

            Text(
                text = stringResource(R.string.about_application_id),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = applicationId,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )

            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
private fun AppIdentity(
    versionName: String,
    versionCode: Long,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.size(88.dp),
        ) {
            Image(
                painter = painterResource(R.mipmap.ic_launcher_foreground),
                contentDescription = null,
                modifier = Modifier.padding(8.dp),
            )
        }

        Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = stringResource(R.string.about_version, versionName, versionCode),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = stringResource(R.string.about_description),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun AboutSection(
    title: String,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        content()
    }
}

@Composable
private fun AboutBullet(text: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            text = "•",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun AboutLinkRow(
    title: String,
    icon: ImageVector,
    testTag: String,
    onClick: () -> Unit,
) {
    ListItem(
        leadingContent = {
            Icon(
                imageVector = icon,
                contentDescription = null,
            )
        },
        trailingContent = {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
        },
        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .testTag(testTag)
            .clickable(onClick = onClick),
    ) {
        Text(title)
    }
}
