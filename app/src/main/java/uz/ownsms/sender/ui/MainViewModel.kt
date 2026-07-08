package uz.ownsms.sender.ui

import android.app.Application
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import uz.ownsms.sender.ServiceLocator
import uz.ownsms.sender.data.db.JobEntity
import uz.ownsms.sender.data.remote.ApiClient
import uz.ownsms.sender.data.remote.DevMessage
import uz.ownsms.sender.data.remote.DeviceStatus
import uz.ownsms.sender.data.remote.PairRequest
import uz.ownsms.sender.data.remote.Provisioned
import uz.ownsms.sender.data.remote.SendMessageReq
import uz.ownsms.sender.data.remote.SignupRequest
import uz.ownsms.sender.data.remote.SimReg
import uz.ownsms.sender.reliability.Check
import uz.ownsms.sender.reliability.ReliabilityChecker
import uz.ownsms.sender.service.OemAutostart
import uz.ownsms.sender.service.SenderService
import uz.ownsms.sender.service.WatchdogWorker
import uz.ownsms.sender.sms.SimInfo
import uz.ownsms.sender.sms.SimRepository

class MainViewModel(app: Application) : AndroidViewModel(app) {

    private val settings = ServiceLocator.settings
    private val simRepo = SimRepository(app)
    private val dao = ServiceLocator.db.jobDao()
    private val reliabilityChecker = ReliabilityChecker(app)

    val baseUrl = MutableStateFlow(settings.baseUrl)
    val token = MutableStateFlow(settings.deviceToken)
    val enabled = MutableStateFlow(settings.enabled)
    val sims = MutableStateFlow<List<SimInfo>>(emptyList())
    val defaultSubId = MutableStateFlow(settings.defaultSubId)
    val reliability = MutableStateFlow<List<Check>>(emptyList())
    val ready = MutableStateFlow(false)

    /** May the sender be started? Hard-gated on registration + mandatory permissions + a chosen SIM. */
    val canStart = MutableStateFlow(false)

    /** Mandatory runtime permissions granted — required before registering so SIMs are captured. */
    val permsGranted = MutableStateFlow(false)
    val oemAggressive: Boolean = OemAutostart.isLikelyAggressive()
    val apiKey = MutableStateFlow(settings.apiKey)
    val message = MutableStateFlow<String?>(null)
    val loading = MutableStateFlow(false)

    val recent: StateFlow<List<JobEntity>> =
        dao.recentFlow(50).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // --- server-backed dashboard (account-wide, via api_key) ---
    val serverMessages = MutableStateFlow<List<DevMessage>>(emptyList())
    val serverStatus = MutableStateFlow<DeviceStatus?>(null)
    val serverError = MutableStateFlow<String?>(null)
    val hasMoreServer = MutableStateFlow(false)
    private var nextBefore: Long? = null
    private var serverLoadedOnce = false

    fun loadServer() {
        if (settings.apiKey.isBlank()) return
        viewModelScope.launch {
            loading.value = true
            serverError.value = null
            try {
                val api = ServiceLocator.devApi()
                serverStatus.value = api.deviceStatus()
                val page = api.listMessages()
                serverMessages.value = page.data
                nextBefore = page.next_before
                hasMoreServer.value = page.next_before != null
                serverLoadedOnce = true
            } catch (e: Exception) {
                serverError.value = e.message ?: "Server bilan bog'lanib bo'lmadi"
            } finally {
                loading.value = false
            }
        }
    }

    /** Load once (e.g. on Home's first composition) — no-op on subsequent calls. */
    fun loadServerOnce() {
        if (!serverLoadedOnce) loadServer()
    }

    fun loadMoreServer() {
        val before = nextBefore ?: return
        if (settings.apiKey.isBlank()) return
        viewModelScope.launch {
            loading.value = true
            serverError.value = null
            try {
                val page = ServiceLocator.devApi().listMessages(before = before)
                serverMessages.value = serverMessages.value + page.data
                nextBefore = page.next_before
                hasMoreServer.value = page.next_before != null
            } catch (e: Exception) {
                serverError.value = e.message ?: "Server bilan bog'lanib bo'lmadi"
            } finally {
                loading.value = false
            }
        }
    }

    fun refreshSims() {
        val list = simRepo.list()
        sims.value = list
        // Auto-select a valid SIM so single-SIM phones aren't blocked; the user can still change it.
        if (list.isNotEmpty() && list.none { it.subscriptionId == defaultSubId.value }) {
            setDefaultSub(list.first().subscriptionId)
        }
        recomputeCanStart()
    }

    fun refreshReliability() {
        reliability.value = reliabilityChecker.checks()
        ready.value = reliabilityChecker.isReady()
        permsGranted.value = reliabilityChecker.permissionsGranted()
        recomputeCanStart()
    }

    private fun recomputeCanStart() {
        canStart.value = token.value.isNotBlank() &&
            reliabilityChecker.permissionsGranted() &&
            sims.value.any { it.subscriptionId == defaultSubId.value }
    }

    fun openOemAutostart() {
        OemAutostart.open(getApplication())
    }

    fun register(url: String) {
        viewModelScope.launch {
            loading.value = true
            try {
                val res = ApiClient.create(url, "")
                    .signup(SignupRequest(device_name = Build.MODEL, app_version = "0.1.0", sims = simRegs()))
                applyProvisioned(url, res)
                message.value = "Ro'yxatdan o'tdingiz — API KEY saqlandi."
            } catch (e: Exception) {
                message.value = "Ro'yxat xatosi: ${e.message ?: "tarmoq"}"
            } finally {
                loading.value = false
            }
        }
    }

    fun pairWithCode(url: String, code: String) {
        viewModelScope.launch {
            loading.value = true
            try {
                val res = ApiClient.create(url, "")
                    .pair(PairRequest(code = code.trim(), device_name = Build.MODEL, app_version = "0.1.0", sims = simRegs()))
                applyProvisioned(url, res)
                message.value = "Pairing muvaffaqiyatli — API KEY saqlandi."
            } catch (e: Exception) {
                message.value = "Pairing xatosi: ${e.message ?: "kod noto'g'ri"}"
            } finally {
                loading.value = false
            }
        }
    }

    fun generatePairingCode() {
        viewModelScope.launch {
            loading.value = true
            try {
                val res = ServiceLocator.api().pairingCode()
                message.value = "Pairing kod: ${res.code} (10 daqiqa)"
            } catch (e: Exception) {
                message.value = "Kod xatosi: ${e.message ?: "tarmoq"}"
            } finally {
                loading.value = false
            }
        }
    }

    fun sendTest(to: String, text: String) {
        if (to.isBlank()) {
            message.value = "Raqam kiriting"
            return
        }
        viewModelScope.launch {
            loading.value = true
            try {
                val res = ServiceLocator.devApi().sendMessage(SendMessageReq(to = to, text = text.ifBlank { "ownsms test" }))
                message.value = "Test yuborildi: ${res.id} (${res.status})"
            } catch (e: Exception) {
                message.value = "Test xatosi: ${e.message ?: "tarmoq"}"
            } finally {
                loading.value = false
            }
        }
    }

    private fun simRegs(): List<SimReg> = simRepo.list().map {
        SimReg(it.subscriptionId, it.number, "", it.subscriptionId == defaultSubId.value)
    }

    private fun applyProvisioned(url: String, res: Provisioned) {
        settings.baseUrl = url
        settings.deviceToken = res.device_token
        settings.apiKey = res.api_key
        baseUrl.value = settings.baseUrl
        token.value = settings.deviceToken
        apiKey.value = settings.apiKey
        recomputeCanStart()
    }

    fun setDefaultSub(subId: Int) {
        defaultSubId.value = subId
        settings.defaultSubId = subId
        recomputeCanStart()
    }

    fun saveUrl(url: String) {
        settings.baseUrl = url
        baseUrl.value = settings.baseUrl
    }

    fun start() {
        recomputeCanStart()
        if (!canStart.value) {
            message.value = "Ishga tushirishdan oldin barcha ruxsatlarni bering va SIM tanlang."
            return
        }
        settings.enabled = true
        enabled.value = true
        SenderService.start(getApplication())
        WatchdogWorker.schedule(getApplication())
    }

    fun stop() {
        settings.enabled = false
        enabled.value = false
        SenderService.stop(getApplication())
        WatchdogWorker.cancel(getApplication())
    }
}
