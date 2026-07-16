package uz.ownsms.sender.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException
import uz.ownsms.sender.ServiceLocator
import uz.ownsms.sender.data.prefs.CampaignSummary
import uz.ownsms.sender.data.remote.CampaignDetail
import uz.ownsms.sender.data.remote.CampaignErrorBody
import uz.ownsms.sender.data.remote.CreateCampaignReq
import uz.ownsms.sender.data.remote.Recipient
import uz.ownsms.sender.sms.SimRepository

/** One recipient the server rejected, mapped back to the line the user typed. */
data class RecipientError(val line: Int, val number: String, val reason: String)

class BulkViewModel(app: Application) : AndroidViewModel(app) {

    private val settings = ServiceLocator.settings
    private val simRepo = SimRepository(app)
    private val errorAdapter =
        Moshi.Builder().add(KotlinJsonAdapterFactory()).build().adapter(CampaignErrorBody::class.java)

    // form
    val numbersText = MutableStateFlow("")
    val messageText = MutableStateFlow("")
    val queued = MutableStateFlow(true)
    val sendAt = MutableStateFlow<Long?>(null) // epoch millis; null = send now
    val fromNumber = MutableStateFlow<String?>(null) // null = default SIM

    val loading = MutableStateFlow(false)
    val message = MutableStateFlow<String?>(null)
    val badRows = MutableStateFlow<List<RecipientError>>(emptyList())

    // active campaign (survives navigating away and back)
    val active = MutableStateFlow<CampaignDetail?>(null)
    val history = MutableStateFlow(settings.campaignHistory)
    private var pollJob: Job? = null

    /**
     * Numbers offered as a campaign `from`: the ones entered at registration (persisted), falling
     * back to live SIM numbers. UZ carriers often leave the SIM number blank, so the registration
     * values are the reliable source.
     */
    fun fromNumbers(): List<String> {
        val registered = settings.registeredNumbers
        return registered.ifEmpty { simRepo.list().map { it.number }.filter { it.isNotBlank() } }
    }

    val recipientCount: Int get() = parseRecipients(numbersText.value).size

    fun send() {
        val parsed = parseRecipients(numbersText.value)
        if (parsed.isEmpty()) {
            message.value = getApplication<Application>().getString(uz.ownsms.sender.R.string.bulk_no_recipients)
            return
        }
        if (settings.apiKey.isBlank()) {
            message.value = getApplication<Application>().getString(uz.ownsms.sender.R.string.activity_no_api_key)
            return
        }
        viewModelScope.launch {
            loading.value = true
            message.value = null
            badRows.value = emptyList()
            try {
                val created = ServiceLocator.devApi().createCampaign(
                    CreateCampaignReq(
                        text = messageText.value,
                        recipients = parsed.map { Recipient(it.number) },
                        from = fromNumber.value,
                        send_at = sendAt.value?.let { isoUtc(it) },
                        queued = queued.value,
                    ),
                )
                val preview = messageText.value.take(60)
                settings.addCampaign(CampaignSummary(created.id, preview, created.total, nowMillis()))
                history.value = settings.campaignHistory
                numbersText.value = ""
                messageText.value = ""
                open(created.id)
            } catch (e: HttpException) {
                if (e.code() == 422) mapBadRows(e, parsed) else message.value = "Xato: HTTP ${e.code()}"
            } catch (e: Exception) {
                message.value = e.message ?: "Tarmoq xatosi"
            } finally {
                loading.value = false
            }
        }
    }

    private fun mapBadRows(e: HttpException, parsed: List<ParsedRecipient>) {
        val body = runCatching { errorAdapter.fromJson(e.response()?.errorBody()?.string().orEmpty()) }.getOrNull()
        badRows.value = body?.error?.bad_rows.orEmpty().mapNotNull { br ->
            parsed.getOrNull(br.index)?.let { RecipientError(it.line, it.number, br.error) }
        }
        message.value = getApplication<Application>().getString(uz.ownsms.sender.R.string.bulk_has_bad_rows)
    }

    /** Open a campaign's live progress and start polling until it reaches a terminal status. */
    fun open(id: String) {
        pollJob?.cancel()
        pollJob = viewModelScope.launch {
            while (true) {
                try {
                    val d = ServiceLocator.devApi().campaign(id)
                    active.value = d
                    if (d.status in TERMINAL) break
                } catch (e: Exception) {
                    message.value = e.message ?: "Tarmoq xatosi"
                    break
                }
                delay(5_000L)
            }
        }
    }

    fun closeActive() {
        pollJob?.cancel()
        active.value = null
    }

    fun action(act: String) {
        val id = active.value?.id ?: return
        viewModelScope.launch {
            loading.value = true
            try {
                // The action response omits progress/total; re-poll for the full detail (also covers cancel).
                ServiceLocator.devApi().campaignAction(id, act)
                open(id)
            } catch (e: Exception) {
                message.value = e.message ?: "Tarmoq xatosi"
            } finally {
                loading.value = false
            }
        }
    }

    override fun onCleared() {
        pollJob?.cancel()
    }

    private companion object {
        val TERMINAL = setOf("completed", "canceled")
    }
}

private fun nowMillis(): Long = System.currentTimeMillis()

/** Epoch millis → "2026-07-20T09:00:00Z" (truncated to the second, as the API expects). */
private fun isoUtc(millis: Long): String =
    java.time.Instant.ofEpochMilli(millis).truncatedTo(java.time.temporal.ChronoUnit.SECONDS).toString()
