package uz.ownsms.sender.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import uz.ownsms.sender.R
import uz.ownsms.sender.ui.theme.StatusDelivered
import uz.ownsms.sender.ui.theme.StatusFailed
import uz.ownsms.sender.ui.theme.StatusMuted
import uz.ownsms.sender.ui.theme.StatusQueued
import uz.ownsms.sender.ui.theme.statusSent

data class StatusVisual(val label: String, val glyph: String, val color: Color)

/** Maps a local job state to a label + tick glyph + color (delivery-receipt motif). */
@Composable
fun statusVisual(state: String): StatusVisual = when (state) {
    "queued" -> StatusVisual(stringResource(R.string.status_queued), "•", StatusQueued)
    "sending", "dispatched" -> StatusVisual(stringResource(R.string.status_sending), "↑", StatusQueued)
    "sent" -> StatusVisual(stringResource(R.string.status_sent), "✓", statusSent())
    "delivered" -> StatusVisual(stringResource(R.string.status_delivered), "✓✓", StatusDelivered)
    "failed" -> StatusVisual(stringResource(R.string.status_failed), "✕", StatusFailed)
    "expired" -> StatusVisual(stringResource(R.string.status_expired), "⌛", StatusMuted)
    else -> StatusVisual(state, "•", StatusMuted)
}

@Composable
fun StatusChip(state: String) {
    val v = statusVisual(state)
    Text(
        text = "${v.glyph} ${v.label}",
        color = v.color,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(v.color.copy(alpha = 0.12f))
            .padding(horizontal = 8.dp, vertical = 4.dp),
    )
}
