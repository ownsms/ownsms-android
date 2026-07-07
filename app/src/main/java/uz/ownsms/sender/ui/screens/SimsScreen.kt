package uz.ownsms.sender.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import uz.ownsms.sender.R
import uz.ownsms.sender.ui.MainViewModel
import uz.ownsms.sender.ui.theme.Mono

@Composable
fun SimsScreen(vm: MainViewModel) {
    val sims by vm.sims.collectAsState()
    val defaultSubId by vm.defaultSubId.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = { vm.refreshSims() }) { Text(stringResource(R.string.common_refresh)) }
        }

        if (sims.isEmpty()) {
            Text(
                stringResource(R.string.sims_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            sims.forEach { sim ->
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 1.dp,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(end = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = defaultSubId == sim.subscriptionId,
                            onClick = { vm.setDefaultSub(sim.subscriptionId) },
                        )
                        Column(Modifier.weight(1f)) {
                            Text(sim.displayName, style = MaterialTheme.typography.bodyLarge)
                            val detail = if (sim.number.isNotBlank()) {
                                sim.number
                            } else {
                                stringResource(R.string.sims_slot_sub_format, sim.slotIndex, sim.subscriptionId)
                            }
                            Text(
                                detail,
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = Mono,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
            Text(
                stringResource(R.string.sims_default_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
