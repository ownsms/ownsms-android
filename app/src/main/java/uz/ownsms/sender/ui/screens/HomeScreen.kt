package uz.ownsms.sender.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import uz.ownsms.sender.R
import uz.ownsms.sender.data.remote.DevMessage
import uz.ownsms.sender.data.remote.DeviceStatus
import uz.ownsms.sender.ui.MainViewModel
import uz.ownsms.sender.ui.components.SignalDot
import uz.ownsms.sender.ui.theme.LiveGreen
import uz.ownsms.sender.ui.theme.StatusDelivered
import uz.ownsms.sender.ui.theme.StatusFailed
import uz.ownsms.sender.ui.theme.StatusQueued
import uz.ownsms.sender.ui.theme.statusSent
import java.time.LocalDate
import java.time.OffsetDateTime

@Composable
fun HomeScreen(vm: MainViewModel, onOpenSettings: () -> Unit) {
    val enabled by vm.enabled.collectAsState()
    val canStart by vm.canStart.collectAsState()
    val isDefaultSms by vm.isDefaultSms.collectAsState()
    val recent by vm.recent.collectAsState()
    val apiKey by vm.apiKey.collectAsState()
    val serverStatus by vm.serverStatus.collectAsState()
    val serverMessages by vm.serverMessages.collectAsState()

    LaunchedEffect(apiKey) {
        if (apiKey.isNotBlank()) vm.loadServerOnce()
    }

    val sent = recent.count { it.state == "sent" }
    val delivered = recent.count { it.state == "delivered" }
    val failed = recent.count { it.state == "failed" }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = if (enabled) LiveGreen.copy(alpha = 0.10f) else MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    SignalDot(active = enabled, color = LiveGreen)
                    Text(
                        stringResource(if (enabled) R.string.home_status_active else R.string.home_status_off),
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
                Button(
                    onClick = { if (enabled) vm.stop() else vm.start() },
                    enabled = enabled || canStart,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                ) {
                    Text(
                        stringResource(if (enabled) R.string.home_stop_btn else R.string.home_start_btn),
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
            }
        }

        if (!enabled && !canStart) {
            Surface(
                onClick = onOpenSettings,
                shape = RoundedCornerShape(14.dp),
                color = StatusQueued.copy(alpha = 0.12f),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    stringResource(R.string.home_missing_perms_notice),
                    color = StatusQueued,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(14.dp),
                )
            }
        }

        if (!isDefaultSms) {
            Surface(
                onClick = onOpenSettings,
                shape = RoundedCornerShape(14.dp),
                color = StatusFailed.copy(alpha = 0.10f),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    stringResource(R.string.home_defsms_warning),
                    color = StatusFailed,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(14.dp),
                )
            }
        }

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            StatCard(stringResource(R.string.status_sent), sent, statusSent(), Modifier.weight(1f))
            StatCard(stringResource(R.string.status_delivered), delivered, StatusDelivered, Modifier.weight(1f))
            StatCard(stringResource(R.string.status_failed), failed, StatusFailed, Modifier.weight(1f))
        }
        Text(
            stringResource(R.string.home_recent_count, recent.size),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        serverStatus?.let { status -> ServerStatusCard(status, serverMessages) }
    }
}

/** Compact account-wide status card — device online state + today's SMS count + SIM count. */
@Composable
private fun ServerStatusCard(status: DeviceStatus, messages: List<DevMessage>) {
    val todayCount = todaysCount(messages)
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                SignalDot(active = status.online, color = if (status.online) LiveGreen else StatusFailed)
                Text(
                    stringResource(if (status.online) R.string.home_server_online else R.string.home_server_offline),
                    style = MaterialTheme.typography.titleSmall,
                )
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    stringResource(R.string.home_today_sms),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text("$todayCount", style = MaterialTheme.typography.bodySmall)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    stringResource(R.string.tab_sims_title),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text("${status.sims.size}", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

/** Counts messages (from the most recent server page) created today — the device-status endpoint
 * has no dedicated usage counter, so this is derived client-side from the loaded page. */
private fun todaysCount(messages: List<DevMessage>): Int {
    val today = LocalDate.now()
    return messages.count { m ->
        val iso = m.created_at ?: return@count false
        try {
            OffsetDateTime.parse(iso).toLocalDate() == today
        } catch (e: Exception) {
            false
        }
    }
}

@Composable
private fun StatCard(label: String, value: Int, color: Color, modifier: Modifier) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        modifier = modifier,
    ) {
        Column(Modifier.padding(14.dp)) {
            Text("$value", style = MaterialTheme.typography.headlineSmall, color = color)
            Text(
                label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
