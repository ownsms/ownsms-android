package uz.ownsms.sender.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/** A small dot that pulses an outer ring while [active]; quiet otherwise. The "live signal" cue. */
@Composable
fun SignalDot(active: Boolean, color: Color, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "signal")
    val scale by transition.animateFloat(
        initialValue = 0.55f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "scale",
    )
    Canvas(modifier = modifier.size(16.dp)) {
        val radius = size.minDimension / 2f
        if (active) {
            drawCircle(color = color.copy(alpha = 0.25f), radius = radius * scale)
        }
        drawCircle(
            color = if (active) color else color.copy(alpha = 0.35f),
            radius = radius * 0.5f,
        )
    }
}
