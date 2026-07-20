package uz.ownsms.sender.ui.screens

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import uz.ownsms.sender.R
import uz.ownsms.sender.data.remote.CampaignDetail
import uz.ownsms.sender.data.remote.CampaignProgress
import uz.ownsms.sender.ui.BulkViewModel
import uz.ownsms.sender.ui.components.SectionCard
import uz.ownsms.sender.ui.theme.StatusFailed
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@Composable
fun BulkScreen(vm: BulkViewModel) {
    val numbers by vm.numbersText.collectAsState()
    val text by vm.messageText.collectAsState()
    val queued by vm.queued.collectAsState()
    val sendAt by vm.sendAt.collectAsState()
    val fromNumber by vm.fromNumber.collectAsState()
    val loading by vm.loading.collectAsState()
    val message by vm.message.collectAsState()
    val badRows by vm.badRows.collectAsState()
    val active by vm.active.collectAsState()
    val history by vm.history.collectAsState()

    val count = remember(numbers) { vm.recipientCount }
    val fromOptions = remember(numbers) { vm.fromNumbers() }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        active?.let { ActiveCampaignCard(it, loading, onAction = vm::action, onClose = vm::closeActive) }

        SectionCard(stringResource(R.string.bulk_compose_title)) {
            OutlinedTextField(
                value = numbers,
                onValueChange = { vm.numbersText.value = it },
                label = { Text(stringResource(R.string.bulk_numbers_label)) },
                placeholder = { Text(stringResource(R.string.bulk_numbers_hint)) },
                modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp),
            )
            Text(
                stringResource(R.string.bulk_count, count),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedTextField(
                value = text,
                onValueChange = { vm.messageText.value = it },
                label = { Text(stringResource(R.string.bulk_text_label)) },
                modifier = Modifier.fillMaxWidth().heightIn(min = 96.dp),
            )
        }

        SectionCard(stringResource(R.string.bulk_options_title)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(stringResource(R.string.bulk_queued_label), style = MaterialTheme.typography.bodyMedium)
                Switch(checked = queued, onCheckedChange = { vm.queued.value = it })
            }
            if (fromOptions.isNotEmpty()) FromPicker(fromOptions, fromNumber) { vm.fromNumber.value = it }
            SendAtRow(sendAt, onPick = { vm.sendAt.value = it }, onClear = { vm.sendAt.value = null })
        }

        if (badRows.isNotEmpty()) {
            SectionCard(stringResource(R.string.bulk_bad_rows_title)) {
                badRows.forEach {
                    Text(
                        stringResource(R.string.bulk_bad_row, it.line, it.number, stringResource(reasonRes(it.reason))),
                        style = MaterialTheme.typography.bodySmall,
                        color = StatusFailed,
                    )
                }
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = { vm.send() }, enabled = !loading && count > 0) {
                Text(stringResource(R.string.bulk_send_btn))
            }
            if (loading) CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
        }

        message?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }

        if (history.isNotEmpty()) {
            SectionCard(stringResource(R.string.bulk_history_title)) {
                history.forEach { c ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            c.preview.ifBlank { "—" },
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                        TextButton(onClick = { vm.open(c.id) }) {
                            Text(stringResource(R.string.bulk_history_total, c.total))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ActiveCampaignCard(d: CampaignDetail, loading: Boolean, onAction: (String) -> Unit, onClose: () -> Unit) {
    var confirmCancel by remember { mutableStateOf(false) }
    val done = d.progress.sent + d.progress.delivered
    val terminal = d.status in setOf("completed", "canceled")

    SectionCard(stringResource(R.string.bulk_progress_title)) {
        val frac = if (d.total > 0) done.toFloat() / d.total else 0f
        LinearProgressIndicator(progress = { frac }, modifier = Modifier.fillMaxWidth())
        Text("$done / ${d.total}", style = MaterialTheme.typography.titleMedium)
        Text(progressLine(d.progress, d.total), style = MaterialTheme.typography.bodySmall)
        Text(
            stringResource(R.string.bulk_progress_status, d.status),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        // Non-destructive controls: pause/resume + hide (does NOT stop the campaign).
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            when (d.status) {
                "running", "scheduled" -> OutlinedButton(onClick = { onAction("pause") }, enabled = !loading) {
                    Text(stringResource(R.string.bulk_pause))
                }
                "paused" -> OutlinedButton(onClick = { onAction("resume") }, enabled = !loading) {
                    Text(stringResource(R.string.bulk_resume))
                }
            }
            TextButton(onClick = onClose) { Text(stringResource(R.string.bulk_hide)) }
        }
        // Destructive cancel is separated and confirmed so it can't be tapped by accident.
        if (!terminal) {
            TextButton(
                onClick = { confirmCancel = true },
                enabled = !loading,
                colors = ButtonDefaults.textButtonColors(contentColor = StatusFailed),
            ) {
                Text(stringResource(R.string.bulk_cancel))
            }
        }
    }

    if (confirmCancel) {
        AlertDialog(
            onDismissRequest = { confirmCancel = false },
            title = { Text(stringResource(R.string.bulk_cancel_confirm_title)) },
            text = { Text(stringResource(R.string.bulk_cancel_confirm_text)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmCancel = false
                    onAction("cancel")
                }) { Text(stringResource(R.string.bulk_cancel)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmCancel = false }) { Text(stringResource(R.string.bulk_cancel_dismiss)) }
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FromPicker(numbers: List<String>, selected: String?, onSelect: (String?) -> Unit) {
    var open by remember { mutableStateOf(false) }
    val label = selected ?: stringResource(R.string.bulk_from_default)
    ExposedDropdownMenuBox(expanded = open, onExpandedChange = { open = it }) {
        OutlinedTextField(
            value = label,
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(R.string.bulk_from_label)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = open) },
            modifier = Modifier.fillMaxWidth().menuAnchor(),
        )
        ExposedDropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.bulk_from_default)) },
                onClick = {
                    onSelect(null)
                    open = false
                },
            )
            numbers.forEach { n ->
                DropdownMenuItem(text = { Text(n) }, onClick = {
                    onSelect(n)
                    open = false
                })
            }
        }
    }
}

@Composable
private fun SendAtRow(sendAt: Long?, onPick: (Long) -> Unit, onClear: () -> Unit) {
    val context = LocalContext.current
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(onClick = {
            val c = Calendar.getInstance()
            DatePickerDialog(
                context,
                { _, y, mo, day ->
                    TimePickerDialog(
                        context,
                        { _, h, min ->
                            val picked = Calendar.getInstance().apply {
                                set(y, mo, day, h, min, 0)
                                set(Calendar.MILLISECOND, 0)
                            }
                            onPick(picked.timeInMillis)
                        },
                        c.get(Calendar.HOUR_OF_DAY),
                        c.get(Calendar.MINUTE),
                        true,
                    ).show()
                },
                c.get(Calendar.YEAR),
                c.get(Calendar.MONTH),
                c.get(Calendar.DAY_OF_MONTH),
            ).show()
        }) {
            Text(
                sendAt?.let { fmtDateTime(it) } ?: stringResource(R.string.bulk_schedule_btn),
            )
        }
        if (sendAt != null) TextButton(onClick = onClear) { Text(stringResource(R.string.bulk_schedule_clear)) }
    }
}

private fun reasonRes(code: String): Int = when (code) {
    "invalid_phone" -> R.string.bulk_reason_invalid_phone
    "missing_to" -> R.string.bulk_reason_missing_to
    else -> R.string.bulk_reason_other
}

private fun progressLine(p: CampaignProgress, total: Int): String {
    val done = p.sent + p.delivered
    return "$done / $total  ·  ⏳${p.queued + p.sending}  ✕${p.failed}"
}

private fun fmtDateTime(millis: Long): String = SimpleDateFormat("dd.MM HH:mm", Locale.getDefault()).format(millis)
