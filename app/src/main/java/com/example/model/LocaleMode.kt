package com.example.model

import androidx.annotation.StringRes
import com.example.R
import java.time.LocalDateTime
import java.time.chrono.ThaiBuddhistChronology
import java.time.format.DateTimeFormatter
import java.util.Locale

enum class LocaleMode(
    val tag: String,
    @StringRes val titleRes: Int,
    @StringRes val shortLabelRes: Int
) {
    THAI_BUDDHIST(
        tag = "th-TH-u-ca-buddhist",
        titleRes = R.string.mode_thai_title,
        shortLabelRes = R.string.mode_thai_short
    ),
    ENGLISH(
        tag = "en-US",
        titleRes = R.string.mode_english_title,
        shortLabelRes = R.string.mode_english_short
    );

    fun formatTime24h(dateTime: LocalDateTime): String {
        return dateTime.format(DateTimeFormatter.ofPattern("HH:mm", Locale.US))
    }

    fun formatDatePreview(dateTime: LocalDateTime): String {
        return when (this) {
            THAI_BUDDHIST -> {
                val thaiLocale = Locale.forLanguageTag("th-TH")
                val formatter = DateTimeFormatter.ofPattern("EEE d MMM yyyy", thaiLocale)
                    .withChronology(ThaiBuddhistChronology.INSTANCE)
                dateTime.format(formatter)
            }
            ENGLISH -> {
                val formatter = DateTimeFormatter.ofPattern("EEE, d MMM yyyy", Locale.US)
                dateTime.format(formatter)
            }
        }
    }

    companion object {
        fun fromTag(tag: String?): LocaleMode? {
            if (tag.isNullOrBlank()) return null
            val clean = tag.trim().lowercase()
            return entries.find { it.tag.lowercase() == clean || clean.contains(it.tag.lowercase()) }
        }

        val DEFAULT: LocaleMode = THAI_BUDDHIST
    }
}
