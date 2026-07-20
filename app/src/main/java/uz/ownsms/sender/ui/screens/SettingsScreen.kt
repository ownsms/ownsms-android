package uz.ownsms.sender.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import uz.ownsms.sender.R
import uz.ownsms.sender.ui.AccountViewModel
import uz.ownsms.sender.ui.MainViewModel
import uz.ownsms.sender.ui.components.CopyableField
import uz.ownsms.sender.ui.components.ExpandableCard
import uz.ownsms.sender.ui.components.SectionCard
import uz.ownsms.sender.ui.theme.StatusDelivered
import uz.ownsms.sender.ui.theme.StatusFailed

@Composable
fun SettingsScreen(vm: MainViewModel, accountVm: AccountViewModel, onRequestPermissions: () -> Unit, onIgnoreBattery: () -> Unit) {
    val reliability by vm.reliability.collectAsState()
    val apiKey by vm.apiKey.collectAsState()
    val message by vm.message.collectAsState()
    val savedToken by vm.token.collectAsState()
    val loading by vm.loading.collectAsState()
    var url by remember { mutableStateOf(vm.baseUrl.value) }
    var testTo by remember { mutableStateOf("") }
    var testText by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        SectionCard(stringResource(R.string.settings_readiness_title)) {
            reliability.forEach { c ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        if (c.ok) "✓" else "✗",
                        color = if (c.ok) StatusDelivered else StatusFailed,
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    Text(c.label, style = MaterialTheme.typography.bodyMedium)
                }
            }
            FilledTonalButton(onClick = onRequestPermissions, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.settings_request_perms_btn))
            }
            FilledTonalButton(onClick = onIgnoreBattery, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.settings_disable_battery_btn))
            }
            if (vm.oemAggressive) {
                FilledTonalButton(onClick = { vm.openOemAutostart() }, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.settings_oem_autostart_btn))
                }
            }
        }

        SectionCard(stringResource(R.string.settings_server_title)) {
            OutlinedTextField(
                value = url,
                onValueChange = { url = it },
                label = { Text(stringResource(R.string.settings_api_url_label)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Button(onClick = { vm.saveUrl(url) }) { Text(stringResource(R.string.settings_save_url_btn)) }
        }

        if (savedToken.isNotBlank()) {
            SectionCard(stringResource(R.string.settings_account_title)) {
                CopyableField(
                    label = stringResource(R.string.settings_apikey_label),
                    value = apiKey,
                    masked = true,
                )
                Button(
                    onClick = { vm.generatePairingCode() },
                    enabled = !loading,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.settings_generate_pair_btn))
                }
                FilledTonalButton(
                    onClick = { vm.reRegister() },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.settings_reregister_btn))
                }
            }

            RateOverridesCard(vm)

            AccountSettings(accountVm)

            SectionCard(stringResource(R.string.settings_test_sms_title)) {
                Text(
                    stringResource(R.string.settings_test_sms_notice),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedTextField(
                    value = testTo,
                    onValueChange = { testTo = it },
                    label = { Text(stringResource(R.string.settings_phone_label)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = testText,
                    onValueChange = { testText = it },
                    label = { Text(stringResource(R.string.settings_test_text_label)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Button(
                        onClick = { vm.sendTest(testTo, testText) },
                        enabled = !loading,
                    ) {
                        Text(stringResource(R.string.settings_send_test_btn))
                    }
                    if (loading) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    }
                }
            }
        }

        message?.let {
            Text(it, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun RateOverridesCard(vm: MainViewModel) {
    val pause by vm.pauseSeconds.collectAsState()
    val min by vm.ratePerMin.collectAsState()
    val hour by vm.ratePerHour.collectAsState()
    val day by vm.ratePerDay.collectAsState()
    val quota by vm.dailyQuota.collectAsState()

    var pauseS by remember(pause) { mutableStateOf(pause?.toString() ?: "") }
    var minS by remember(min) { mutableStateOf(min?.toString() ?: "") }
    var hourS by remember(hour) { mutableStateOf(hour?.toString() ?: "") }
    var dayS by remember(day) { mutableStateOf(day?.toString() ?: "") }
    var quotaS by remember(quota) { mutableStateOf(quota?.toString() ?: "") }

    ExpandableCard(stringResource(R.string.settings_rate_title)) {
        Text(
            stringResource(R.string.settings_rate_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        IntField(stringResource(R.string.settings_rate_pause), pauseS) { pauseS = it }
        IntField(stringResource(R.string.settings_rate_min), minS) { minS = it }
        IntField(stringResource(R.string.settings_rate_hour), hourS) { hourS = it }
        IntField(stringResource(R.string.settings_rate_day), dayS) { dayS = it }
        IntField(stringResource(R.string.settings_rate_quota), quotaS) { quotaS = it }
        Button(onClick = {
            vm.saveOverrides(pauseS.toIntOrNull(), minS.toIntOrNull(), hourS.toIntOrNull(), dayS.toIntOrNull(), quotaS.toIntOrNull())
        }) { Text(stringResource(R.string.settings_save)) }
    }
}

@Composable
private fun IntField(label: String, value: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = { s -> onChange(s.filter { it.isDigit() }) },
        label = { Text(label) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
}
