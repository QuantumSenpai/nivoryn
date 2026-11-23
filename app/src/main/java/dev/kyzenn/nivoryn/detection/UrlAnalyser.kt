package dev.kyzenn.nivoryn.detection

import dev.kyzenn.nivoryn.domain.Evidence
import java.net.IDN
import java.net.URI
import java.util.Locale

data class UrlFindings(val evidence: List<Evidence>, val invalid: Boolean, val examined: Boolean, val destinations: List<String> = emptyList())

/** Structure only: deliberately has no DNS resolver, HTTP client or redirect follower. */
class UrlAnalyser(private val pack: IndicatorPack? = null, private val now: Long = System.currentTimeMillis()) {
    fun inspect(text: String, requireUrl: Boolean = false): UrlFindings {
        val matches = Regex("(?i)(?:https?://|upi://|www\\.)[^\\s<>\\\"']+").findAll(text).take(16).map { it.value.trimEnd('.', ',', ')', ']') }.toList()
        val candidates = if (requireUrl && text.trim().none { it.isWhitespace() }) listOf(text.trim()) else if (matches.isEmpty() && requireUrl) listOf(text.trim()) else matches
        val destinations = mutableListOf<String>()
        val evidence = mutableListOf<Evidence>(); var invalid = false
        for (raw in candidates) {
            try {
                require(raw.length <= 2048 && raw.none { it.code < 32 || it in "\u200b\u200c\u200d\ufeff\u202e\u202d" })
                val uri = URI(if (raw.startsWith("www.", true) || !raw.contains(':')) "https://$raw" else raw)
                val scheme = uri.scheme?.lowercase(Locale.ROOT)
                require(scheme in listOf("https", "http", "upi"))
                fun add(title: String, explanation: String) { evidence.add(Evidence(title, raw, explanation)) }
                if (scheme == "upi") {
                    require(uri.host.equals("pay", true) && !uri.rawQuery.isNullOrBlank())
                    require(Regex("(?:^|&)pa=[^&]+").containsMatchIn(uri.rawQuery))
                    add("Outgoing payment request", "This payload requests a payment. Recipient ownership and the reason for payment remain unverified. Nothing was opened or paid.")
                    continue
                }
                val authority = requireNotNull(uri.rawAuthority)
                require('%' !in authority && '\\' !in authority)
                val host = (uri.host ?: authority.substringAfterLast('@').substringBefore(':')).trim('[', ']')
                require(host.isNotBlank())
                val ascii = (if (':' in host) host else IDN.toASCII(host, IDN.USE_STD3_ASCII_RULES)).lowercase(Locale.ROOT)
                require(ascii.length <= 253 && ('.' in ascii || ':' in ascii))
                if (':' !in ascii) {
                    require(ascii.trimEnd('.').split('.').all { it.isNotEmpty() && it.length <= 63 })
                    val hostPort = authority.substringAfterLast('@')
                    if (':' in hostPort) require(hostPort.substringAfterLast(':').toIntOrNull() in 1..65535)
                }
                destinations += ascii
                if (pack?.matchesDomain(ascii.trimEnd('.'), now) == true)
                    add("Local threat-list match", "The destination matches a current signed local indicator pack (version ${pack.version}). List membership is evidence; ownership and current site content remain unverified.")
                val labels = ascii.trimEnd('.').split('.')
                val canonicalHost = ascii.trimEnd('.')
                if (labels.any { it.split('-').contains("paypal") } && !(canonicalHost == "paypal.com" || canonicalHost.endsWith(".paypal.com") || canonicalHost == "paypalobjects.com" || canonicalHost.endsWith(".paypalobjects.com")))
                    add("Brand / domain mismatch", "The hostname contains a supported brand name but is outside its configured domains. This is a limited heuristic, not proof of phishing. Actual destination is shown separately.")
                if (uri.rawQuery?.let { Regex("(?i)(?:https?%3a|https?://|%253a)").containsMatchIn(it) } == true)
                    add("Nested destination", "The query contains an encoded or nested URL. The visible host does not establish the eventual destination; redirects were not followed.")
                if (uri.rawUserInfo != null || '@' in authority) add("Misleading URL authority", "Text before @ is user information, not the destination. Actual host: $ascii")
                if (scheme == "http") add("Unencrypted destination", "HTTP does not encrypt the connection. HTTPS alone would not establish trust either.")
                if (Regex("^[0-9.]+$").matches(ascii) || ':' in ascii) add("Numeric destination", "The URL uses a numeric address ($ascii). Local checks do not contact this address or establish its owner.")
                if (ascii.split('.').any { it.startsWith("xn--") }) add("Internationalised host", "The host uses internationalised characters. Read the actual destination carefully: $ascii. This alone does not prove fraud.")
                if (ascii.split('.').size > 4) add("Layered hostname", "Several subdomains can conceal the destination. Host: $ascii. No live ownership check ran.")
            } catch (_: IllegalArgumentException) { invalid = true }
              catch (_: java.net.URISyntaxException) { invalid = true }
        }
        return UrlFindings(evidence, invalid, candidates.isNotEmpty(), destinations.distinct())
    }
}
