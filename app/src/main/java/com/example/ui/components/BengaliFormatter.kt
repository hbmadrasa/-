package com.example.ui.components

import java.util.Calendar

object BengaliFormatter {

    private val bengaliDigits = charArrayOf('০', '১', '২', '৩', '৪', '৫', '৬', '৭', '৮', '৯')

    private val bengaliDays = arrayOf(
        "রবিবার",
        "সোমবার",
        "মঙ্গলবার",
        "বুধবার",
        "বৃহস্পতিবার",
        "শুক্রবার",
        "শনিবার"
    )

    private val bengaliMonths = arrayOf(
        "জানুয়ারি",
        "ফেব্রুয়ারি",
        "মার্চ",
        "এপ্রিল",
        "মে",
        "জুন",
        "জুলাই",
        "আগস্ট",
        "সেপ্টেম্বর",
        "অক্টোবর",
        "নভেম্বর",
        "ডিসেম্বর"
    )

    fun toBengaliDigits(input: String): String {
        val sb = StringBuilder(input.length)
        for (ch in input) {
            if (ch in '0'..'9') {
                sb.append(bengaliDigits[ch - '0'])
            } else {
                sb.append(ch)
            }
        }
        return sb.toString()
    }

    fun toBengaliNumber(value: Int): String = toBengaliDigits(value.toString())

    fun formatBengaliTime(calendar: Calendar): String {
        val hour24 = calendar.get(Calendar.HOUR_OF_DAY)
        val hour12 = when (val h = calendar.get(Calendar.HOUR)) {
            0 -> 12
            else -> h
        }
        val minute = calendar.get(Calendar.MINUTE)
        val formatted = String.format("%02d:%02d", hour12, minute)
        return toBengaliDigits(formatted)
    }

    fun formatDayPeriod(calendar: Calendar): String {
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        return when (hour) {
            in 4..11 -> "সকাল"
            in 12..15 -> "দুপুর"
            in 16..18 -> "বিকাল"
            in 19..20 -> "সন্ধ্যা"
            else -> "রাত"
        }
    }

    fun formatBengaliDate(calendar: Calendar): String {
        val dayOfWeek = bengaliDays.getOrElse(calendar.get(Calendar.DAY_OF_WEEK) - 1) { "" }
        val dayOfMonth = toBengaliNumber(calendar.get(Calendar.DAY_OF_MONTH))
        val month = bengaliMonths.getOrElse(calendar.get(Calendar.MONTH)) { "" }
        return "$dayOfWeek, $dayOfMonth $month"
    }
}
