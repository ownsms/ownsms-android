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
data class MessagesPage(val data: List<DevMessage> = emptyList(), val next_before: Long? = null)

data class DeviceSim(val number: String? = null, val operator: String? = null, val is_default: Boolean = false)

/** GET `/api/v1/device` response — account-wide device status. */
data class DeviceStatus(
    val online: Boolean,
    val status: String,
    val last_seen: String? = null,
    val sims: List<DeviceSim> = emptyList(),
)
