package uz.ownsms.sender.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import uz.ownsms.sender.R
import uz.ownsms.sender.ui.openUrl
import uz.ownsms.sender.ui.theme.Ultramarine

@Composable
fun AboutScreen() {
    val context = LocalContext.current
    val version = remember(context) {
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }
            .getOrNull() ?: "—"
    }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Surface(color = Ultramarine, shape = RoundedCornerShape(20.dp), modifier = Modifier.size(88.dp)) {
            Image(painter = painterResource(R.drawable.ic_launcher_foreground), contentDescription = null)
        }
        Text(stringResource(R.string.app_name), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
        Text(
            stringResource(R.string.app_tagline),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        InfoCard(stringResource(R.string.about_what_title)) {
            Text(stringResource(R.string.about_what_body), style = MaterialTheme.typography.bodyMedium)
        }
        InfoCard(stringResource(R.string.about_how_title)) {
            Text(stringResource(R.string.about_how_body), style = MaterialTheme.typography.bodyMedium)
        }

        OutlinedButton(
            onClick = { context.openUrl("https://ownsms.uz") },
            modifier = Modifier.fillMaxWidth(),
        ) { Text(stringResource(R.string.about_website_btn)) }
        OutlinedButton(
            onClick = { context.openUrl("https://sms.omadli.uz/api/v1/docs") },
            modifier = Modifier.fillMaxWidth(),
        ) { Text(stringResource(R.string.drawer_api_docs)) }

        Spacer(Modifier.height(4.dp))
        Text(
            stringResource(R.string.drawer_version, version),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun InfoCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            content()
        }
    }
}
