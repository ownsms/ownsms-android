package uz.ownsms.sender.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import uz.ownsms.sender.ServiceLocator
import uz.ownsms.sender.data.remote.ApiKeyInfo
import uz.ownsms.sender.data.remote.AuditEntry
import uz.ownsms.sender.data.remote.CreateKeyReq
import uz.ownsms.sender.data.remote.DeviceInfo
import uz.ownsms.sender.data.remote.WebhookConfig
import uz.ownsms.sender.data.remote.WebhookPut

/** Account-scoped admin surface (api_key): API keys, webhook, devices, audit. Fetch-on-open, no polling. */
class AccountViewModel(app: Application) : AndroidViewModel(app) {

    val keys = MutableStateFlow<List<ApiKeyInfo>>(emptyList())
    val webhook = MutableStateFlow<WebhookConfig?>(null)
    val devices = MutableStateFlow<List<DeviceInfo>>(emptyList())
    val audit = MutableStateFlow<List<AuditEntry>>(emptyList())

    /** The one-time full `osk_...` shown right after creating a key (server never returns it again). */
    val newKey = MutableStateFlow<String?>(null)
    val loading = MutableStateFlow(false)
    val error = MutableStateFlow<String?>(null)

    private val app get() = getApplication<Application>()

    private fun run(block: suspend () -> Unit) {
        if (ServiceLocator.settings.apiKey.isBlank()) {
            error.value = app.getString(uz.ownsms.sender.R.string.err_no_api_key)
            return
        }
        viewModelScope.launch {
            loading.value = true
            error.value = null
            try {
                block()
            } catch (e: Exception) {
                error.value = friendlyError(app, e, app.getString(uz.ownsms.sender.R.string.msg_server_unreachable))
            } finally {
                loading.value = false
            }
        }
    }

    fun loadKeys() = run { keys.value = ServiceLocator.devApi().keys().data }

    fun createKey(name: String, scopes: List<String>, ipAllowlist: List<String>) = run {
        val created = ServiceLocator.devApi().createKey(CreateKeyReq(name = name, scopes = scopes, ip_allowlist = ipAllowlist))
        newKey.value = created.api_key
        keys.value = ServiceLocator.devApi().keys().data
    }

    fun clearNewKey() {
        newKey.value = null
    }

    fun revokeKey(id: Long) = run {
        ServiceLocator.devApi().revokeKey(id)
        keys.value = ServiceLocator.devApi().keys().data
    }

    fun loadWebhook() = run { webhook.value = ServiceLocator.devApi().webhook() }

    fun saveWebhook(url: String, events: List<String>, enabled: Boolean) = run {
        webhook.value = ServiceLocator.devApi().putWebhook(WebhookPut(url, events, enabled))
    }

    fun loadDevices() = run { devices.value = ServiceLocator.devApi().devices().data }

    fun deviceAction(id: Long, action: String) = run {
        ServiceLocator.devApi().deviceAction(id, action)
        devices.value = ServiceLocator.devApi().devices().data
    }

    fun loadAudit() = run { audit.value = ServiceLocator.devApi().audit().data }
}
