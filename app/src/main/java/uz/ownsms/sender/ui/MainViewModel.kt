package uz.ownsms.sender.ui

import android.app.Application
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import uz.ownsms.sender.BuildConfig
import uz.ownsms.sender.R
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

    private val app get() = getApplication<Application>()

    val baseUrl = MutableStateFlow(settings.baseUrl)
    val token = MutableStateFlow(settings.deviceToken)
    val enabled = MutableStateFlow(settings.enabled)
    val sims = MutableStateFlow<List<SimInfo>>(emptyList())
    val defaultSubId = MutableStateFlow(settings.defaultSubId)

    /** User-editable phone numbers per SIM (carriers often leave the SIM number blank in UZ). */
    val simNumbers = MutableStateFlow<Map<Int, String>>(emptyMap())
    val reliability = MutableStateFlow<List<Check>>(emptyList())
    val ready = MutableStateFlow(false)

    /** May the sender be started? Hard-gated on registration + mandatory permissions + a chosen SIM. */
    val canStart = MutableStateFlow(false)

    /** Mandatory runtime permissions granted — required before registering so SIMs are captured. */
    val permsGranted = MutableStateFlow(false)
    val oemAggressive: Boolean = OemAutostart.isLikelyAggressive()

    /** Whether this app is the phone's default SMS app (exempts bulk sends from the OS rate limit). */
    val isDefaultSms = MutableStateFlow(false)
    val apiKey = MutableStateFlow(settings.apiKey)
    val message = MutableStateFlow<String?>(null)
    val loading = MutableStateFlow(false)

    val recent: StateFlow<List<JobEntity>> =
        dao.recentFlow(50).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Local job totals per state — counted over the whole table, not just the rows the list shows. */
    val localCounts: StateFlow<Map<String, Int>> =
        dao.stateCountsFlow()
            .map { rows -> rows.associate { it.state to it.n } }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    // --- server-backed dashboard (account-wide, via api_key) ---
    val serverMessages = MutableStateFlow<List<DevMessage>>(emptyList())
    val serverCounts = MutableStateFlow<Map<String, Int>>(emptyMap())
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
                serverCounts.value = page.counts
                nextBefore = page.next_before
                hasMoreServer.value = page.next_before != null
                serverLoadedOnce = true
            } catch (e: Exception) {
                serverError.value = friendlyError(app, e, app.getString(R.string.msg_server_unreachable))
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
                serverError.value = friendlyError(app, e, app.getString(R.string.msg_server_unreachable))
            } finally {
                loading.value = false
            }
        }
    }

    fun refreshSims() {
        val list = simRepo.list()
        sims.value = list
        // Seed editable numbers from the detected value, but never clobber a number the user typed.
        simNumbers.value = list.associate { sim ->
            sim.subscriptionId to (simNumbers.value[sim.subscriptionId] ?: sim.number)
        }
        // Auto-select a valid SIM so single-SIM phones aren't blocked; the user can still change it.
        if (list.isNotEmpty() && list.none { it.subscriptionId == defaultSubId.value }) {
            setDefaultSub(list.first().subscriptionId)
        }
        recomputeCanStart()
    }

    fun setSimNumber(subId: Int, number: String) {
        simNumbers.value = simNumbers.value + (subId to number)
    }

    fun refreshReliability() {
        reliability.value = reliabilityChecker.checks()
        ready.value = reliabilityChecker.isReady()
        permsGranted.value = reliabilityChecker.permissionsGranted()
        // Re-read enabled so the Home toggle reflects a Stop done from the notification.
        enabled.value = settings.enabled
        isDefaultSms.value = uz.ownsms.sender.sms.DefaultSmsApp.isDefault(app)
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

    fun register(url: String, email: String) {
        viewModelScope.launch {
            loading.value = true
            try {
                val res = ApiClient.create(url, "")
                    .signup(SignupRequest(email = email.trim(), device_name = Build.MODEL, app_version = APP_VERSION, sims = simRegs()))
                applyProvisioned(url, res)
                message.value = app.getString(R.string.msg_register_ok)
            } catch (e: Exception) {
                message.value = app.getString(R.string.msg_register_err, e.message ?: "")
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
                    .pair(PairRequest(code = code.trim(), device_name = Build.MODEL, app_version = APP_VERSION, sims = simRegs()))
                applyProvisioned(url, res)
                message.value = app.getString(R.string.msg_pair_ok)
            } catch (e: Exception) {
                message.value = app.getString(R.string.msg_pair_err, e.message ?: "")
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
                message.value = app.getString(R.string.msg_pairing_code, res.code)
            } catch (e: Exception) {
                message.value = app.getString(R.string.msg_pairing_code_err, e.message ?: "")
            } finally {
                loading.value = false
            }
        }
    }

    fun sendTest(to: String, text: String) {
        if (to.isBlank()) {
            message.value = app.getString(R.string.msg_need_number)
            return
        }
        if (settings.apiKey.isBlank()) {
            message.value = app.getString(R.string.err_no_api_key)
            return
        }
        viewModelScope.launch {
            loading.value = true
            try {
                val res = ServiceLocator.devApi().sendMessage(SendMessageReq(to = to, text = text.ifBlank { "ownsms test" }))
                message.value = app.getString(R.string.msg_test_ok, res.id, res.status)
            } catch (e: Exception) {
                message.value = friendlyError(app, e, app.getString(R.string.msg_test_err, ""))
            } finally {
                loading.value = false
            }
        }
    }

    /** Clear the stored credentials and return to onboarding — recovery when the server rejects them (401). */
    fun reRegister() {
        stop()
        settings.deviceToken = ""
        settings.apiKey = ""
        token.value = ""
        apiKey.value = ""
        recomputeCanStart()
        message.value = app.getString(R.string.msg_reregister_prompt)
    }

    private fun simRegs(): List<SimReg> = sims.value.map {
        SimReg(
            subscription_id = it.subscriptionId,
            number = (simNumbers.value[it.subscriptionId] ?: it.number).trim(),
            operator = it.operator,
            is_default = it.subscriptionId == defaultSubId.value,
        )
    }.also { regs ->
        // Remember the numbers so the bulk "from" picker can offer them (carrier numbers are often blank).
        settings.registeredNumbers = regs.map { it.number }.filter { it.isNotBlank() }
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

    // --- send-rate overrides (persisted; SenderService reads them via Settings) ---
    val pauseSeconds = MutableStateFlow(settings.pauseSeconds)
    val ratePerMin = MutableStateFlow(settings.ratePerMin)
    val ratePerHour = MutableStateFlow(settings.ratePerHour)
    val ratePerDay = MutableStateFlow(settings.ratePerDay)
    val dailyQuota = MutableStateFlow(settings.dailyQuota)

    fun saveOverrides(pause: Int?, min: Int?, hour: Int?, day: Int?, quota: Int?) {
        settings.pauseSeconds = pause
        settings.ratePerMin = min
        settings.ratePerHour = hour
        settings.ratePerDay = day
        settings.dailyQuota = quota
        // Re-read: pauseSeconds may have been floored to PAUSE_FLOOR_SEC on write.
        pauseSeconds.value = settings.pauseSeconds
        ratePerMin.value = settings.ratePerMin
        ratePerHour.value = settings.ratePerHour
        ratePerDay.value = settings.ratePerDay
        dailyQuota.value = settings.dailyQuota
    }

    fun start() {
        recomputeCanStart()
        if (!canStart.value) {
            message.value = app.getString(R.string.msg_start_blocked)
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

    private companion object {
        val APP_VERSION: String = BuildConfig.VERSION_NAME
    }
}
