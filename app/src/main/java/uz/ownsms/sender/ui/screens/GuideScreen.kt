package uz.ownsms.sender.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import uz.ownsms.sender.R
import uz.ownsms.sender.ui.MainViewModel
import uz.ownsms.sender.ui.components.CopyableField
import uz.ownsms.sender.ui.theme.Ultramarine

@Composable
fun GuideScreen(vm: MainViewModel) {
    val baseUrl by vm.baseUrl.collectAsState()
    val apiKey by vm.apiKey.collectAsState()

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(stringResource(R.string.guide_intro), style = MaterialTheme.typography.bodyMedium)

        Step(1, stringResource(R.string.guide_step1_title), stringResource(R.string.guide_step1_body))
        Step(2, stringResource(R.string.guide_step2_title), stringResource(R.string.guide_step2_body))
        Step(3, stringResource(R.string.guide_step3_title), stringResource(R.string.guide_step3_body))
        Step(4, stringResource(R.string.guide_step4_title), stringResource(R.string.guide_step4_body))
        Step(5, stringResource(R.string.guide_step5_title), stringResource(R.string.guide_step5_body))

        val url = baseUrl.ifBlank { "https://sms.omadli.uz" }.trimEnd('/')
        val key = apiKey.ifBlank { "osk_..." }
        val curl = "curl -X POST $url/api/v1/messages \\\n" +
            "  -H \"Authorization: Bearer $key\" \\\n" +
            "  -H \"Content-Type: application/json\" \\\n" +
            "  -d '{\"to\":\"+998901234567\",\"text\":\"Salom ownsms!\"}'"
        CopyableField(label = stringResource(R.string.guide_curl_label), value = curl)
    }
}

@Composable
private fun Step(n: Int, title: String, body: String) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Surface(color = Ultramarine, shape = CircleShape, modifier = Modifier.size(30.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        n.toString(),
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
