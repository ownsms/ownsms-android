package uz.ownsms.sender.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Local mirror of a server job, used for the at-most-once send/reconcile cycle. */
@Entity(tableName = "jobs")
data class JobEntity(
    // server message id
    @PrimaryKey val id: Long,
    val to: String,
    val text: String,
    // SIM slot (subscriptionId); null → use default
    val subscriptionId: Int?,
    val segments: Int,
    // see JobState
    val state: String,
    // already reported "sent" to the server (awaiting delivery)
    val sentReported: Boolean = false,
    // terminal outcome reported; row kept for history
    val reported: Boolean = false,
    val errorCode: String? = null,
    val createdAt: Long,
)

/** Local job lifecycle. Server transitions are derived from these. */
object JobState {
    const val CLAIMED = "claimed" // received from server, not yet handed to SmsManager
    const val SENDING = "sending" // handed to SmsManager; outcome unknown (in-flight)
    const val SENT = "sent" // sentIntent OK
    const val DELIVERED = "delivered" // deliveryIntent OK (terminal)
    const val FAILED = "failed" // failure or restart-uncertain (terminal)
}
