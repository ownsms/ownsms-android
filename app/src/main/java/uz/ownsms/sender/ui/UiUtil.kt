package uz.ownsms.sender.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import uz.ownsms.sender.R

/** Opens [url] in the user's browser; silently no-ops if no browser can handle it. */
fun Context.openUrl(url: String) {
    try {
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    } catch (e: Exception) {
        Toast.makeText(this, getString(R.string.no_browser), Toast.LENGTH_SHORT).show()
    }
}
