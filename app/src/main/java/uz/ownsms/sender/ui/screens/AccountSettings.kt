package uz.ownsms.sender.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import uz.ownsms.sender.R
import uz.ownsms.sender.data.remote.ApiKeyInfo
import uz.ownsms.sender.data.remote.DeviceInfo
import uz.ownsms.sender.ui.AccountViewModel
import uz.ownsms.sender.ui.components.CopyableField
import uz.ownsms.sender.ui.components.ExpandableCard

/** API-key-writable account surface: keys, webhook, devices, audit. Each card loads on first expand. */
@Composable
fun AccountSettings(vm: AccountViewModel) {
    val error by vm.error.collectAsState()
    KeysCard(vm)
    WebhookCard(vm)
    DevicesCard(vm)
    AuditCard(vm)
    error?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error) }
}

@Composable
private fun KeysCard(vm: AccountViewModel) {
    val keys by vm.keys.collectAsState()
    val newKey by vm.newKey.collectAsState()
    var name by remember { mutableStateOf("") }
    var send by remember { mutableStateOf(true) }
    var read by remember { mutableStateOf(true) }
    var ips by remember { mutableStateOf("") }

    ExpandableCard(stringResource(R.string.settings_keys_title)) {
        LoadRow { vm.loadKeys() }
        keys.forEach { KeyRow(it) { vm.revokeKey(it.id) } }

        Text(stringResource(R.string.settings_keys_new), style = MaterialTheme.typography.titleSmall)
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text(stringResource(R.string.settings_keys_name)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Row {
            ScopeCheck("send", send) { send = it }
            ScopeCheck("read", read) { read = it }
        }
        OutlinedTextField(
            value = ips,
            onValueChange = { ips = it },
            label = { Text(stringResource(R.string.settings_keys_ip)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            stringResource(R.string.settings_keys_ip_note),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedButton(onClick = {
            val scopes = listOfNotNull("send".takeIf { send }, "read".takeIf { read })
            val ipList = ips.split(",").map { it.trim() }.filter { it.isNotEmpty() }
            vm.createKey(name, scopes, ipList)
            name = ""
            ips = ""
        }) { Text(stringResource(R.string.settings_keys_create)) }

        newKey?.let {
            Text(
                stringResource(R.string.settings_keys_new_notice),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
            )
            CopyableField(label = "", value = it)
            TextButton(onClick = { vm.clearNewKey() }) { Text(stringResource(R.string.common_close)) }
        }
    }
}

@Composable
private fun ScopeCheck(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Checkbox(checked = checked, onCheckedChange = onChange)
        Text(label)
    }
}

@Composable
private fun KeyRow(k: ApiKeyInfo, onRevoke: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                k.name.ifBlank { k.prefix },
                style = MaterialTheme.typography.bodyMedium,
                fontFamily = FontFamily.Monospace,
            )
            val meta = k.scopes.joinToString(",") +
                (if (k.ip_allowlist.isNotEmpty()) "  ·  ${k.ip_allowlist.joinToString(",")}" else "") +
                (if (k.revoked) "  ·  " + stringResource(R.string.settings_keys_revoked) else "")
            Text(meta, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (!k.revoked) TextButton(onClick = onRevoke) { Text(stringResource(R.string.settings_keys_revoke)) }
    }
}

@Composable
private fun WebhookCard(vm: AccountViewModel) {
    val wh by vm.webhook.collectAsState()
    var url by remember(wh) { mutableStateOf(wh?.url ?: "") }
    var enabled by remember(wh) { mutableStateOf(wh?.enabled ?: false) }
    var sent by remember(wh) { mutableStateOf(wh?.events?.contains("message.sent") ?: false) }
    var delivered by remember(wh) { mutableStateOf(wh?.events?.contains("message.delivered") ?: false) }
    var failed by remember(wh) { mutableStateOf(wh?.events?.contains("message.failed") ?: false) }

    ExpandableCard(stringResource(R.string.settings_webhook_title)) {
        LoadRow { vm.loadWebhook() }
        OutlinedTextField(
            value = url,
            onValueChange = { url = it },
            label = { Text(stringResource(R.string.settings_webhook_url)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        EventCheck(stringResource(R.string.settings_webhook_event_sent), sent) { sent = it }
        EventCheck(stringResource(R.string.settings_webhook_event_delivered), delivered) { delivered = it }
        EventCheck(stringResource(R.string.settings_webhook_event_failed), failed) { failed = it }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(stringResource(R.string.settings_webhook_enabled))
            Switch(checked = enabled, onCheckedChange = { enabled = it })
        }
        OutlinedButton(onClick = {
            val events = listOfNotNull(
                "message.sent".takeIf { sent },
                "message.delivered".takeIf { delivered },
                "message.failed".takeIf { failed },
            )
            vm.saveWebhook(url.trim(), events, enabled)
        }) { Text(stringResource(R.string.settings_save)) }

        wh?.let { CopyableField(label = stringResource(R.string.settings_webhook_secret), value = it.secret, masked = true) }
    }
}

@Composable
private fun EventCheck(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Checkbox(checked = checked, onCheckedChange = onChange)
        Text(label, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun DevicesCard(vm: AccountViewModel) {
    val devices by vm.devices.collectAsState()
    ExpandableCard(stringResource(R.string.settings_devices_title)) {
        LoadRow { vm.loadDevices() }
        devices.forEach { DeviceRow(it) { act -> vm.deviceAction(it.id, act) } }
    }
}

@Composable
private fun DeviceRow(d: DeviceInfo, onAction: (String) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(d.name.ifBlank { "#${d.id}" }, style = MaterialTheme.typography.bodyMedium)
            val on = stringResource(if (d.online) R.string.settings_device_online else R.string.settings_device_offline)
            Text(
                "${d.status} · $on",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (d.status == "active") {
            TextButton(onClick = { onAction("deactivate") }) { Text(stringResource(R.string.settings_device_deactivate)) }
        } else {
            TextButton(onClick = { onAction("activate") }) { Text(stringResource(R.string.settings_device_activate)) }
        }
    }
}

@Composable
private fun AuditCard(vm: AccountViewModel) {
    val audit by vm.audit.collectAsState()
    ExpandableCard(stringResource(R.string.settings_audit_title)) {
        LoadRow { vm.loadAudit() }
        if (audit.isEmpty()) {
            Text(
                stringResource(R.string.settings_audit_empty),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        audit.forEach {
            Text(
                "${it.action}  ·  ${it.actor}  ·  ${it.ip}",
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
            )
        }
    }
}

@Composable
private fun LoadRow(onLoad: () -> Unit) {
    OutlinedButton(onClick = onLoad) { Text(stringResource(R.string.settings_load)) }
}
