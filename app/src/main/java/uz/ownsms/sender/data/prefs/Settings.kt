package uz.ownsms.sender.data.prefs

import android.content.Context
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

/** A locally-remembered bulk campaign (server has no list endpoint, so we mirror our own sends). */
data class CampaignSummary(val id: String, val preview: String, val total: Int, val createdAt: Long)

/** Simple synchronous settings store (SharedPreferences). */
class Settings(context: Context) {
    private val sp = context.applicationContext.getSharedPreferences("ownsms", Context.MODE_PRIVATE)

    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    private val campaignsAdapter =
        moshi.adapter<List<CampaignSummary>>(Types.newParameterizedType(List::class.java, CampaignSummary::class.java))

    var baseUrl: String
        get() = sp.getString(KEY_BASE_URL, "").orEmpty()
        set(v) = sp.edit().putString(KEY_BASE_URL, v.trim()).apply()

    var deviceToken: String
        get() = sp.getString(KEY_TOKEN, "").orEmpty()
        set(v) = sp.edit().putString(KEY_TOKEN, v.trim()).apply()

    /** API key for the developer's server (shown once after registration so it can be copied). */
    var apiKey: String
        get() = sp.getString(KEY_API_KEY, "").orEmpty()
        set(v) = sp.edit().putString(KEY_API_KEY, v).apply()

    /** Default SIM subscriptionId; -1 = use system default. */
    var defaultSubId: Int
        get() = sp.getInt(KEY_DEFAULT_SUB, -1)
        set(v) = sp.edit().putInt(KEY_DEFAULT_SUB, v).apply()

    var enabled: Boolean
        get() = sp.getBoolean(KEY_ENABLED, false)
        set(v) = sp.edit().putBoolean(KEY_ENABLED, v).apply()

    /** SIM numbers entered at registration — the `from` options for a campaign (phone numbers, no commas). */
    var registeredNumbers: List<String>
        get() = sp.getString(KEY_NUMBERS, "").orEmpty().split(",").map { it.trim() }.filter { it.isNotEmpty() }
        set(v) = sp.edit().putString(KEY_NUMBERS, v.joinToString(",")).apply()

    /** Recent bulk campaigns sent from this phone, newest first (capped at [CAMPAIGN_HISTORY_MAX]). */
    var campaignHistory: List<CampaignSummary>
        get() = sp.getString(KEY_CAMPAIGNS, null)?.let {
            runCatching { campaignsAdapter.fromJson(it) }.getOrNull()
        }.orEmpty()
        set(v) = sp.edit().putString(KEY_CAMPAIGNS, campaignsAdapter.toJson(v.take(CAMPAIGN_HISTORY_MAX))).apply()

    fun addCampaign(c: CampaignSummary) {
        campaignHistory = listOf(c) + campaignHistory.filter { it.id != c.id }
    }

    // --- send-rate overrides (blank/absent/0 = use the server's per-SIM config) ---
    // 0 is treated as unset: a real 0 rate would brick sending (RateGate blocks everything).
    private fun optInt(key: String): Int? = sp.getInt(key, 0).takeIf { it >= 1 }
    private fun setOptInt(key: String, v: Int?) = sp.edit().apply { if (v == null) remove(key) else putInt(key, v) }.apply()

    /** Inter-message pause floor in seconds; never below [PAUSE_FLOOR_SEC] (carrier anti-spam). */
    var pauseSeconds: Int?
        get() = optInt(KEY_PAUSE)
        set(v) = setOptInt(KEY_PAUSE, v?.coerceAtLeast(PAUSE_FLOOR_SEC))
    var ratePerMin: Int?
        get() = optInt(KEY_RATE_MIN)
        set(v) = setOptInt(KEY_RATE_MIN, v)
    var ratePerHour: Int?
        get() = optInt(KEY_RATE_HOUR)
        set(v) = setOptInt(KEY_RATE_HOUR, v)
    var ratePerDay: Int?
        get() = optInt(KEY_RATE_DAY)
        set(v) = setOptInt(KEY_RATE_DAY, v)
    var dailyQuota: Int?
        get() = optInt(KEY_QUOTA)
        set(v) = setOptInt(KEY_QUOTA, v)

    val isConfigured: Boolean get() = baseUrl.isNotBlank() && deviceToken.isNotBlank()

    companion object {
        const val PAUSE_FLOOR_SEC = 2
        private const val CAMPAIGN_HISTORY_MAX = 50
        private const val KEY_BASE_URL = "base_url"
        private const val KEY_TOKEN = "device_token"
        private const val KEY_API_KEY = "api_key"
        private const val KEY_DEFAULT_SUB = "default_sub_id"
        private const val KEY_ENABLED = "enabled"
        private const val KEY_NUMBERS = "registered_numbers"
        private const val KEY_CAMPAIGNS = "campaign_history"
        private const val KEY_PAUSE = "ovr_pause_sec"
        private const val KEY_RATE_MIN = "ovr_rate_min"
        private const val KEY_RATE_HOUR = "ovr_rate_hour"
        private const val KEY_RATE_DAY = "ovr_rate_day"
        private const val KEY_QUOTA = "ovr_quota"
    }
}
