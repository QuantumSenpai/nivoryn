package dev.kyzenn.nivoryn.documents

import dev.kyzenn.nivoryn.domain.Evidence
import java.time.LocalDate
import java.util.Locale

/** Consistency of explicit labels only. Neither a signature nor an issuer/booking verifier. */
object DocumentInspector {
    private val fields = Regex("(?im)^\\s*(PNR|Booking ID|Ticket ID|Amount|Total|Date|Travel date|Issue date)\\s*[:=]\\s*([^\\r\\n]{1,80})")
    fun inspect(text: String, qr: String): List<Evidence> {
        require(text.length <= 12000 && qr.length <= 12000)
        fun values(source: String) = fields.findAll(source).take(32).groupBy(
            { it.groupValues[1].lowercase(Locale.ROOT) }, { it.groupValues[2].trim() })
        val visible = values(text); val decoded = values(qr)
        val evidence = mutableListOf<Evidence>()
        fun normalise(key: String, value: String): String {
            if (key in listOf("amount", "total")) {
                val amount = Regex("(?i)^(?:(?:INR|Rs\\.?|₹)\\s*)?([0-9,]+(?:\\.[0-9]{1,2})?)$").matchEntire(value)
                if (amount != null) return amount.groupValues[1].replace(",", "").toBigDecimalOrNull()?.stripTrailingZeros()?.toPlainString() ?: value
            }
            return value.uppercase(Locale.ROOT)
        }
        visible.forEach { (key, values) ->
            if (values.map { normalise(key, it) }.distinct().size > 1)
                evidence += Evidence("Repeated document field differs", "$key: ${values.joinToString(" / ")}", "The same labelled field has multiple values. Multiple passengers/items or OCR errors may explain it; this does not prove forgery.")
            val other = decoded[key]
            if (other != null && values.map { normalise(key, it) }.toSet() != other.map { normalise(key, it) }.toSet())
                evidence += Evidence("Document / QR field mismatch", "Text $key: ${values.joinToString()}\nQR $key: ${other.joinToString()}", "Reviewed text and decoded QR disagree on the same labelled field. Correct extraction errors and check with the issuer; a mismatch does not prove fraud.")
        }
        (visible.entries.map { "Reviewed text" to it } + decoded.entries.map { "Decoded QR" to it }).forEach { (source, entry) ->
            if (entry.key.contains("date")) entry.value.forEach { date ->
                if (date.matches(Regex("[0-9]{4}-[0-9]{2}-[0-9]{2}")) && runCatching { LocalDate.parse(date) }.isFailure)
                    evidence += Evidence("Invalid calendar date", "$source ${entry.key}: $date", "A labelled ISO date is not a valid calendar day. OCR or data-entry errors are possible; this does not prove forgery.")
            }
        }
        return evidence.take(32)
    }
}
