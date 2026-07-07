package uz.ownsms.sender.data.prefs

import android.content.Context

/** Simple synchronous settings store (SharedPreferences). */
class Settings(context: Context) {
    private val sp = context.applicationContext.getSharedPreferences("ownsms", Context.MODE_PRIVATE)

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

    val isConfigured: Boolean get() = baseUrl.isNotBlank() && deviceToken.isNotBlank()

    private companion object {
        const val KEY_BASE_URL = "base_url"
        const val KEY_TOKEN = "device_token"
        const val KEY_API_KEY = "api_key"
        const val KEY_DEFAULT_SUB = "default_sub_id"
        const val KEY_ENABLED = "enabled"
    }
}
