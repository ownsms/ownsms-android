package uz.ownsms.sender.data.remote

/**
 * DTOs for the ownsms device protocol (`/api/v1/device/...`). Field names are snake_case to match
 * the JSON contract exactly (parsed by Moshi's reflective Kotlin adapter — no annotations needed).
 */

data class RegisterRequest(val app_version: String)
data class RegisterResponse(val device_id: Long)

data class Job(
    val id: Long,
    val to: String,
    val text: String,
    val subscription_id: Int?,
    val segments: Int,
)

data class PollResponse(val jobs: List<Job> = emptyList())

data class StatusReport(val status: String, val error_code: String? = null)
data class StatusResponse(val id: Long, val status: String)

data class HeartbeatResponse(val ok: Boolean)

data class SimConfig(
    val subscription_id: Int,
    val rate_per_min: Int,
    val rate_per_hour: Int,
    val rate_per_day: Int,
    val jitter_min: Int,
    val jitter_max: Int,
    val work_hours_start: String?,
    val work_hours_end: String?,
    val daily_quota: Int?,
)

data class DeviceConfig(
    val default_ttl: Int,
    val sims: List<SimConfig> = emptyList(),
)

// --- registration / pairing ---

data class SimReg(
    val subscription_id: Int,
    val number: String = "",
    val operator: String = "",
    val is_default: Boolean = false,
)

data class SignupRequest(
    val email: String = "",
    val device_name: String = "",
    val app_version: String = "",
    val sims: List<SimReg> = emptyList(),
)

data class PairRequest(
    val code: String,
    val device_name: String = "",
    val app_version: String = "",
    val sims: List<SimReg> = emptyList(),
)

/** Response for both /register and /register/pair. */
data class Provisioned(
    val account_id: Long,
    val device_id: Long,
    val device_token: String,
    val api_key: String,
)

data class PairingCodeResult(val code: String, val expires_at: String)

// --- developer API (`/api/v1/messages`, `/api/v1/device`, api_key auth) ---

data class SendMessageReq(val to: String, val text: String, val from: String? = null, val queued: Boolean = false)

/**
 * Shape of `_serialize(m)` in `views/messages.py` — used for both the POST response and each item
 * in the GET list. `id` is the string form `"msg_<pk>"`, NOT a raw integer (see [MessagesPage]).
 */
data class DevMessage(
    val id: String,
    val status: String,
    val to: String,
    val from: String? = null,
    val segments: Int = 1,
    val error_code: String? = null,
    val created_at: String? = null,
)

/** The POST `/api/v1/messages` response has the exact same shape as a list item. */
typealias SentMessage = DevMessage

/**
 * GET `/api/v1/messages` response. `next_before` is the raw integer pk of the last row (NOT the
 * "msg_"-prefixed `id` string) — pass it straight back as the `before` query param to page further.
 */
data class MessagesPage(
    val data: List<DevMessage> = emptyList(),
    val next_before: Long? = null,
    // Per-status totals for the whole account (server >= 0.3.8), so the activity tiles report the
    // real numbers instead of counting the page that happens to be loaded. Empty on older servers.
    val counts: Map<String, Int> = emptyMap(),
)

data class DeviceSim(val number: String? = null, val operator: String? = null, val is_default: Boolean = false)

/** GET `/api/v1/device` response — account-wide device status. */
data class DeviceStatus(
    val online: Boolean,
    val status: String,
    val last_seen: String? = null,
    val sims: List<DeviceSim> = emptyList(),
)

// --- campaigns (bulk send, api_key auth) ---

data class Recipient(val to: String)

data class CreateCampaignReq(
    val text: String,
    val recipients: List<Recipient>,
    val from: String? = null,
    // ISO-8601; null → send now
    val send_at: String? = null,
    val queued: Boolean = true,
)

/** POST `/api/v1/campaigns` 202 response. */
data class CampaignCreated(val id: String, val status: String, val total: Int)

/** Always carries all seven keys (server remaps internal "dispatched" → "sending"). */
data class CampaignProgress(
    val queued: Int = 0,
    val sending: Int = 0,
    val sent: Int = 0,
    val delivered: Int = 0,
    val failed: Int = 0,
    val expired: Int = 0,
    val canceled: Int = 0,
)

/**
 * GET `/api/v1/campaigns/{id}` returns all fields; the pause/resume/cancel action endpoint returns
 * only `{id, status}`, so `total`/`progress` default here to keep Moshi parsing both.
 */
data class CampaignDetail(
    val id: String,
    val status: String,
    val total: Int = 0,
    val progress: CampaignProgress = CampaignProgress(),
)

/**
 * 422 body from create when any recipient is invalid — the whole batch is rejected (fail-fast atomic).
 * `index` is the position in the submitted `recipients` array, mapped back to the source line by the UI.
 */
data class CampaignErrorBody(val error: CampaignError? = null)
data class CampaignError(val code: String, val message: String = "", val bad_rows: List<BadRow> = emptyList())
data class BadRow(val index: Int, val error: String, val to: String? = null, val missing: List<String>? = null)

// --- account management (api_key auth) ---

/** GET/POST `/api/v1/keys` item. On create, [api_key] carries the one-time raw `osk_...` token. */
data class ApiKeyInfo(
    val id: Long,
    val prefix: String,
    val name: String = "",
    val scopes: List<String> = emptyList(),
    val device_id: Long? = null,
    val ip_allowlist: List<String> = emptyList(),
    val is_test: Boolean = false,
    val revoked: Boolean = false,
    val created_at: String? = null,
    val last_used_at: String? = null,
    // present only in the 201 create response
    val api_key: String? = null,
)
data class KeysList(val data: List<ApiKeyInfo> = emptyList())
data class CreateKeyReq(
    val name: String = "",
    val scopes: List<String> = listOf("send", "read"),
    val ip_allowlist: List<String> = emptyList(),
)

/** GET/PUT `/api/v1/webhook`. `secret` is read-only (server-generated); PUT ignores it. */
data class WebhookConfig(
    val url: String = "",
    val events: List<String> = emptyList(),
    val enabled: Boolean = false,
    val secret: String = "",
)
data class WebhookPut(val url: String, val events: List<String>, val enabled: Boolean)

data class DeviceInfo(
    val id: Long,
    val name: String = "",
    val status: String = "",
    val online: Boolean = false,
    val last_seen: String? = null,
    val app_version: String = "",
    val sims: List<DeviceSim> = emptyList(),
)
data class DevicesList(val data: List<DeviceInfo> = emptyList())

data class AuditEntry(
    val actor: String = "",
    val action: String = "",
    val target: String = "",
    val ip: String = "",
    val at: String = "",
)
data class AuditList(val data: List<AuditEntry> = emptyList())
