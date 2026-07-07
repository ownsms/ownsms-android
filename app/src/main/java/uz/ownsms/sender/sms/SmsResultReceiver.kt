package uz.ownsms.sender.sms

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import uz.ownsms.sender.ServiceLocator
import uz.ownsms.sender.data.db.JobState

/**
 * Receives per-part sent/delivered results and updates the local job state.
 * Reporting these outcomes to the server is done by [uz.ownsms.sender.service.SenderService].
 */
class SmsResultReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val jobId = intent.getLongExtra(EXTRA_JOB_ID, -1L)
        if (jobId < 0L) return
        val action = intent.action ?: return
        val ok = resultCode == Activity.RESULT_OK
        val code = resultCode

        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val dao = ServiceLocator.db.jobDao()
                val job = dao.getById(jobId) ?: return@launch
                val live = job.state == JobState.SENDING || job.state == JobState.SENT
                when (action) {
                    ACTION_SENT ->
                        if (ok) {
                            if (job.state == JobState.SENDING) dao.setState(jobId, JobState.SENT)
                        } else if (live) {
                            dao.setState(jobId, JobState.FAILED, "send_failed_$code")
                        }

                    ACTION_DELIVERED ->
                        if (ok) {
                            if (live) dao.setState(jobId, JobState.DELIVERED)
                        } else if (live) {
                            dao.setState(jobId, JobState.FAILED, "not_delivered")
                        }
                }
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        const val ACTION_SENT = "uz.ownsms.sender.SMS_SENT"
        const val ACTION_DELIVERED = "uz.ownsms.sender.SMS_DELIVERED"
        const val EXTRA_JOB_ID = "job_id"
        const val EXTRA_PART = "part"
    }
}
