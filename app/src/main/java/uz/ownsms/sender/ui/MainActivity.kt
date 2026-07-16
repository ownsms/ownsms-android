package uz.ownsms.sender.ui

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import uz.ownsms.sender.ui.theme.OwnSmsTheme

class MainActivity : ComponentActivity() {

    private val vm: MainViewModel by viewModels()
    private val bulkVm: BulkViewModel by viewModels()
    private val accountVm: AccountViewModel by viewModels()

    private val permLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) {
        vm.refreshSims()
        vm.refreshReliability()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            OwnSmsTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    AppRoot(
                        vm = vm,
                        bulkVm = bulkVm,
                        accountVm = accountVm,
                        onRequestPermissions = ::requestPerms,
                        onIgnoreBattery = ::requestIgnoreBattery,
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Re-evaluate permissions/battery/SIMs whenever the user returns from a settings screen.
        vm.refreshSims()
        vm.refreshReliability()
    }

    private fun requestPerms() {
        val perms = mutableListOf(
            Manifest.permission.SEND_SMS,
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.READ_PHONE_NUMBERS,
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            perms.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        permLauncher.launch(perms.toTypedArray())
    }

    private fun requestIgnoreBattery() {
        try {
            startActivity(
                Intent(
                    Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                    Uri.parse("package:$packageName"),
                ),
            )
        } catch (e: Exception) {
            startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
        }
    }
}
