package uz.ownsms.sender.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity

/**
 * Required component for default-SMS-app eligibility (SENDTO/compose). The gateway has no compose
 * UI — redirect to the main screen. ponytail: minimal stub; a real compose screen isn't needed for
 * a send-only gateway.
 */
class ComposeSmsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        startActivity(Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        finish()
    }
}
