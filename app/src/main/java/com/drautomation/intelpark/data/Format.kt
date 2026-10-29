package com.drautomation.intelpark.data

import java.text.NumberFormat
import java.time.Duration
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Currency
import java.util.Locale

/** Display helpers: money in the server's currency, dates in the phone's time zone. */
object Format {
    fun money(m: Money?): String {
        if (m == null) return "—"
        val nf = NumberFormat.getCurrencyInstance(Locale.getDefault())
        runCatching { nf.currency = Currency.getInstance(m.currency.trim()) }
        return nf.format(m.amountCents / 100.0)
    }

    fun parse(iso: String?): OffsetDateTime? = iso?.let { runCatching { OffsetDateTime.parse(it) }.getOrNull() }

    private val local: ZoneId get() = ZoneId.systemDefault()

    fun time(iso: String?): String =
        parse(iso)?.atZoneSameInstant(local)?.format(DateTimeFormatter.ofPattern("HH:mm")) ?: "—"

    fun clock(epochMillis: Long): String =
        java.time.Instant.ofEpochMilli(epochMillis).atZone(local).format(DateTimeFormatter.ofPattern("HH:mm:ss"))

    fun dateTime(iso: String?): String =
        parse(iso)?.atZoneSameInstant(local)?.format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT)) ?: "—"

    fun date(iso: String?): String = runCatching {
        java.time.LocalDate.parse(iso).format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))
    }.getOrDefault(iso ?: "—")

    fun duration(minutes: Long): String {
        val h = minutes / 60
        val m = minutes % 60
        return when {
            h == 0L -> "$m min"
            m == 0L -> "$h h"
            else -> "$h h ${m.toString().padStart(2, '0')}"
        }
    }

    fun minutesLeft(iso: String?): Long? = parse(iso)?.let { Duration.between(OffsetDateTime.now(), it).toMinutes() }

    fun ago(iso: String?): String {
        val minutes = parse(iso)?.let { Duration.between(it, OffsetDateTime.now()).toMinutes() } ?: return "—"
        return duration(maxOf(0, minutes))
    }

    fun distance(meters: Int?): String = when {
        meters == null -> ""
        meters < 1000 -> "$meters m"
        else -> String.format(Locale.getDefault(), "%.1f km", meters / 1000.0)
    }

    fun plate(text: String): String = text.uppercase(Locale.ROOT).filter { it.isLetterOrDigit() || it == ' ' || it == '-' }.trim()
}
