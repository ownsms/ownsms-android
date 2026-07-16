package uz.ownsms.sender.ui.screens

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
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import uz.ownsms.sender.R
import uz.ownsms.sender.ui.MainViewModel

@Composable
fun OnboardingScreen(vm: MainViewModel, onRequestPermissions: () -> Unit) {
    val message by vm.message.collectAsState()
    val loading by vm.loading.collectAsState()
    val permsGranted by vm.permsGranted.collectAsState()
    val sims by vm.sims.collectAsState()
    val simNumbers by vm.simNumbers.collectAsState()
    val defaultSubId by vm.defaultSubId.collectAsState()
    var url by remember { mutableStateOf(vm.baseUrl.value) }
    var email by remember { mutableStateOf("") }
    var pairCode by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Spacer(Modifier.height(24.dp))
        Text(stringResource(R.string.app_name), style = MaterialTheme.typography.headlineLarge)
        Text(
            stringResource(R.string.onboarding_subtitle),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = url,
            onValueChange = { url = it },
            label = { Text(stringResource(R.string.onboarding_api_url_label)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text(stringResource(R.string.onboarding_email_label)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        // Grant permissions before registering — that's when the device's SIMs are sent to the server.
        FilledTonalButton(
            onClick = onRequestPermissions,
            enabled = !permsGranted && !loading,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(if (permsGranted) R.string.onboarding_perms_ok else R.string.onboarding_request_perms_btn))
        }

        SimNumberStep(sims, simNumbers, defaultSubId, permsGranted, vm)

        Button(
            onClick = { vm.register(url, email) },
            enabled = url.isNotBlank() && permsGranted && !loading,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
        ) {
            Text(stringResource(R.string.onboarding_register_btn), style = MaterialTheme.typography.titleMedium)
        }

        if (loading) {
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
            }
        }

        Text(
            stringResource(R.string.onboarding_or_join),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )

        OutlinedTextField(
            value = pairCode,
            onValueChange = { pairCode = it },
            label = { Text(stringResource(R.string.onboarding_pair_code_label)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        FilledTonalButton(
            onClick = { vm.pairWithCode(url, pairCode) },
            enabled = url.isNotBlank() && pairCode.isNotBlank() && permsGranted && !loading,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.onboarding_pair_btn))
        }

        message?.let {
            Text(it, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

/** After permissions are granted, show each detected SIM with an editable number + a default picker. */
@Composable
private fun SimNumberStep(
    sims: List<uz.ownsms.sender.sms.SimInfo>,
    simNumbers: Map<Int, String>,
    defaultSubId: Int,
    permsGranted: Boolean,
    vm: MainViewModel,
) {
    if (!permsGranted) {
        Text(
            stringResource(R.string.onboarding_perms_first),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        return
    }
    if (sims.isEmpty()) return

    Text(stringResource(R.string.onboarding_sims_title), style = MaterialTheme.typography.titleMedium)
    Text(
        stringResource(R.string.onboarding_sims_hint),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    sims.forEach { sim ->
        OutlinedTextField(
            value = simNumbers[sim.subscriptionId].orEmpty(),
            onValueChange = { vm.setSimNumber(sim.subscriptionId, it) },
            label = { Text(sim.displayName) },
            placeholder = { Text(stringResource(R.string.onboarding_sim_number_hint)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        if (sims.size > 1) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(
                    selected = sim.subscriptionId == defaultSubId,
                    onClick = { vm.setDefaultSub(sim.subscriptionId) },
                )
                Text(stringResource(R.string.onboarding_sim_default), style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
