package uz.ownsms.sender.ui

import android.content.Context
import retrofit2.HttpException
import uz.ownsms.sender.R

/**
 * Human-readable message for an API failure. A 401 means the stored device_token / api_key is no
 * longer valid (blank, revoked, or the account was cleared server-side) — tell the user to
 * re-register rather than showing a raw "401".
 */
fun friendlyError(context: Context, e: Throwable, fallback: String): String = when {
    e is HttpException && e.code() == 401 -> context.getString(R.string.err_unauthorized)
    else -> e.message ?: fallback
}

/** True when the failure is an auth failure (401) the user should recover from by re-registering. */
fun isUnauthorized(e: Throwable): Boolean = e is HttpException && e.code() == 401
