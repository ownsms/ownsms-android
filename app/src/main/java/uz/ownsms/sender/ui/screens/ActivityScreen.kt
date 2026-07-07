package uz.ownsms.sender.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import uz.ownsms.sender.R
import uz.ownsms.sender.data.db.JobEntity
import uz.ownsms.sender.data.remote.DevMessage
import uz.ownsms.sender.ui.MainViewModel
import uz.ownsms.sender.ui.components.StatusChip
import uz.ownsms.sender.ui.components.statusVisual
import uz.ownsms.sender.ui.theme.Mono
import uz.ownsms.sender.ui.theme.StatusDelivered
import uz.ownsms.sender.ui.theme.StatusFailed
import uz.ownsms.sender.ui.theme.StatusSent
import java.text.SimpleDateFormat
import java.time.OffsetDateTime
import java.util.Date
import java.util.Locale

private data class Filter(val key: String, val labelRes: Int)

private val FILTERS = listOf(
    Filter("all", R.string.status_filter_all),
    Filter("sent", R.string.status_sent),
    Filter("delivered", R.string.status_delivered),
    Filter("failed", R.string.status_failed),
)

private enum class Source { LOCAL, SERVER }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivityScreen(vm: MainViewModel) {
    val recent by vm.recent.collectAsState()
    val apiKey by vm.apiKey.collectAsState()
    val serverMessages by vm.serverMessages.collectAsState()
    val serverError by vm.serverError.collectAsState()
    val loading by vm.loading.collectAsState()
    val hasMoreServer by vm.hasMoreServer.collectAsState()

    var source by remember { mutableStateOf(Source.LOCAL) }
    var filter by remember { mutableStateOf("all") }
    var selectedLocal by remember { mutableStateOf<JobEntity?>(null) }
    var selectedServer by remember { mutableStateOf<DevMessage?>(null) }

    val effectiveSource = if (apiKey.isBlank()) Source.LOCAL else source

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (apiKey.isNotBlank()) {
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = effectiveSource == Source.LOCAL,
                    onClick = { source = Source.LOCAL },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                ) { Text(stringResource(R.string.activity_source_local)) }
                SegmentedButton(
                    selected = effectiveSource == Source.SERVER,
                    onClick = {
                        source = Source.SERVER
                        vm.loadServer()
                    },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                ) { Text(stringResource(R.string.activity_source_server)) }
            }
        } else {
            Text(
                stringResource(R.string.activity_no_api_key),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        if (effectiveSource == Source.SERVER) {
            ServerActivity(
                messages = serverMessages,
                error = serverError,
                loading = loading,
                hasMore = hasMoreServer,
                onRefresh = vm::loadServer,
                onLoadMore = vm::loadMoreServer,
                onSelect = { selectedServer = it },
            )
        } else {
            val sent = recent.count { it.state == "sent" }
            val delivered = recent.count { it.state == "delivered" }
            val failed = recent.count { it.state == "failed" }
            val shown = if (filter == "all") recent else recent.filter { it.state == filter }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatTile(stringResource(R.string.status_sent), sent, StatusSent, Modifier.weight(1f))
                StatTile(stringResource(R.string.status_delivered), delivered, StatusDelivered, Modifier.weight(1f))
                StatTile(stringResource(R.string.status_failed), failed, StatusFailed, Modifier.weight(1f))
            }

            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FILTERS.forEach { f ->
                    FilterChip(
                        selected = filter == f.key,
                        onClick = { filter = f.key },
                        label = { Text(stringResource(f.labelRes)) },
                    )
                }
            }

            if (shown.isEmpty()) {
                Text(
                    stringResource(R.string.activity_empty_local),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(shown, key = { it.id }) { job -> MessageRow(job, onClick = { selectedLocal = job }) }
                }
            }
        }
    }

    selectedLocal?.let { job ->
        MessageDetailDialog(job, onDismiss = { selectedLocal = null })
    }
    selectedServer?.let { msg ->
        ServerMessageDetailDialog(msg, onDismiss = { selectedServer = null })
    }
}

/** Account-wide messages fetched from the server (`GET /api/v1/messages` + `/api/v1/device`). */
@Composable
private fun ServerActivity(
    messages: List<DevMessage>,
    error: String?,
    loading: Boolean,
    hasMore: Boolean,
    onRefresh: () -> Unit,
    onLoadMore: () -> Unit,
    onSelect: (DevMessage) -> Unit,
) {
    val sent = messages.count { it.status == "sent" }
    val delivered = messages.count { it.status == "delivered" }
    val failed = messages.count { it.status == "failed" }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            stringResource(R.string.activity_server_all_messages),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        IconButton(onClick = onRefresh) {
            Icon(Icons.Filled.Refresh, contentDescription = stringResource(R.string.common_refresh))
        }
    }

    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        StatTile(stringResource(R.string.status_sent), sent, StatusSent, Modifier.weight(1f))
        StatTile(stringResource(R.string.status_delivered), delivered, StatusDelivered, Modifier.weight(1f))
        StatTile(stringResource(R.string.status_failed), failed, StatusFailed, Modifier.weight(1f))
    }

    error?.let {
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.errorContainer,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                it,
                color = MaterialTheme.colorScheme.onErrorContainer,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(14.dp),
            )
        }
    }

    when {
        loading && messages.isEmpty() -> {
            Box(
                modifier = Modifier.fillMaxWidth().padding(24.dp),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator() }
        }
        messages.isEmpty() && error == null -> {
            Text(
                stringResource(R.string.activity_server_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        else -> {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(messages, key = { it.id }) { msg -> MessageRow(msg, onClick = { onSelect(msg) }) }
                if (hasMore) {
                    item {
                        TextButton(
                            onClick = onLoadMore,
                            enabled = !loading,
                            modifier = Modifier.fillMaxWidth(),
                        ) { Text(stringResource(R.string.activity_load_more)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun MessageDetailDialog(job: JobEntity, onDismiss: () -> Unit) {
    val visual = statusVisual(job.state)
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_close)) }
        },
        title = {
            Text(
                job.to,
                fontFamily = Mono,
                style = MaterialTheme.typography.titleMedium,
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SelectionContainer {
                    Text(job.text, style = MaterialTheme.typography.bodyMedium)
                }
                DetailRow(stringResource(R.string.detail_status_label), "${visual.glyph} ${visual.label}", visual.color)
                job.errorCode?.let {
                    DetailRow(stringResource(R.string.detail_error_code_label), it, MaterialTheme.colorScheme.error)
                }
                DetailRow(stringResource(R.string.detail_segments_label), job.segments.toString())
                DetailRow(stringResource(R.string.detail_time_label), formatTime(job.createdAt))
            }
        },
    )
}

/** Detail dialog for a server-fetched message. The list endpoint doesn't return the SMS body text. */
@Composable
private fun ServerMessageDetailDialog(msg: DevMessage, onDismiss: () -> Unit) {
    val visual = statusVisual(msg.status)
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_close)) }
        },
        title = {
            Text(
                msg.to,
                fontFamily = Mono,
                style = MaterialTheme.typography.titleMedium,
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                DetailRow(stringResource(R.string.detail_status_label), "${visual.glyph} ${visual.label}", visual.color)
                DetailRow(stringResource(R.string.detail_sender_sim_label), msg.from ?: "—")
                msg.error_code?.let {
                    DetailRow(stringResource(R.string.detail_error_code_label), it, MaterialTheme.colorScheme.error)
                }
                DetailRow(stringResource(R.string.detail_segments_label), msg.segments.toString())
                DetailRow(stringResource(R.string.detail_time_label), formatIsoTime(msg.created_at))
            }
        },
    )
}

@Composable
private fun DetailRow(label: String, value: String, valueColor: Color = Color.Unspecified) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(value, style = MaterialTheme.typography.bodyMedium, color = valueColor)
    }
}

@Composable
private fun StatTile(label: String, value: Int, color: Color, modifier: Modifier) {
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
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun MessageRow(job: JobEntity, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    job.to,
                    style = MaterialTheme.typography.bodyLarge,
                    fontFamily = Mono,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    job.text,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    formatTime(job.createdAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            StatusChip(job.state)
        }
    }
}

/** Row for a server-fetched message. No SMS body text is available from the list endpoint, so the
 * secondary line shows the originating SIM number instead. */
@Composable
private fun MessageRow(msg: DevMessage, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    msg.to,
                    style = MaterialTheme.typography.bodyLarge,
                    fontFamily = Mono,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    msg.from?.let { stringResource(R.string.activity_row_sim, it) }
                        ?: stringResource(R.string.activity_row_segments, msg.segments),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    formatIsoTime(msg.created_at),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            StatusChip(msg.status)
        }
    }
}

private val timeFmt = SimpleDateFormat("HH:mm · dd MMM", Locale.getDefault())

private fun formatTime(millis: Long): String = timeFmt.format(Date(millis))

/** Parses the ISO-8601 timestamp the backend sends (`created_at`, `last_seen`) into the same display format. */
private fun formatIsoTime(iso: String?): String {
    if (iso.isNullOrBlank()) return "—"
    return try {
        timeFmt.format(Date.from(OffsetDateTime.parse(iso).toInstant()))
    } catch (e: Exception) {
        iso
    }
}
