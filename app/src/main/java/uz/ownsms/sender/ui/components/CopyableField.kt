package uz.ownsms.sender.ui.components

import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import uz.ownsms.sender.R
import uz.ownsms.sender.ui.theme.Mono

/**
 * A labelled read-only value the user copies with one tap. Secrets (API key) are shown [masked]
 * as `osk_•••••••1234` with an eye toggle to reveal; the copy button always copies the full value.
 */
@Composable
fun CopyableField(label: String, value: String, modifier: Modifier = Modifier, masked: Boolean = false) {
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current
    var revealed by remember { mutableStateOf(!masked) }
    val blank = value.isBlank()
    val shown = when {
        blank -> "—"
        masked && !revealed -> maskSecret(value)
        else -> value
    }

    Column(modifier) {
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    shown,
                    style = MaterialTheme.typography.bodyMedium,
                    fontFamily = Mono,
                    modifier = Modifier.weight(1f).padding(start = 12.dp, top = 10.dp, bottom = 10.dp),
                )
                if (masked && !blank) {
                    IconButton(onClick = { revealed = !revealed }) {
                        Icon(
                            if (revealed) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                            contentDescription = stringResource(
                                if (revealed) R.string.action_hide else R.string.action_reveal,
                            ),
                        )
                    }
                }
                IconButton(
                    enabled = !blank,
                    onClick = {
                        clipboard.setText(AnnotatedString(value))
                        Toast.makeText(context, context.getString(R.string.copied), Toast.LENGTH_SHORT).show()
                    },
                ) {
                    Icon(Icons.Filled.ContentCopy, contentDescription = stringResource(R.string.action_copy))
                }
            }
        }
    }
}

/** `osk_S8BUL2H1Bde…` → `osk_•••••••Bde1` (keep a recognizable head + tail). */
private fun maskSecret(value: String): String {
    if (value.length <= 8) return "•".repeat(value.length)
    return value.take(4) + "•".repeat(7) + value.takeLast(4)
}
