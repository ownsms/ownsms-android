package uz.ownsms.sender.ui

/** One recipient plus the 1-based source line it came from (for mapping server bad_rows back). */
data class ParsedRecipient(val line: Int, val number: String)

/**
 * Split the bulk textarea into recipients: one number per line, trimmed, blanks dropped.
 * Each keeps its original 1-based line number so a 422 `bad_rows[i].index` (position in the
 * submitted array) can be shown against the line the user actually typed.
 */
fun parseRecipients(raw: String): List<ParsedRecipient> = raw.split("\n").mapIndexedNotNull { i, s ->
    val t = s.trim()
    if (t.isEmpty()) null else ParsedRecipient(i + 1, t)
}
