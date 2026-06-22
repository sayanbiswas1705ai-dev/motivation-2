package com.example.data.model

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object DateUtils {
    const val DATE_FORMAT = "yyyy-MM-dd"

    fun getFormatter(): SimpleDateFormat {
        return SimpleDateFormat(DATE_FORMAT, Locale.US)
    }

    fun getTodayString(): String {
        return getFormatter().format(Date())
    }

    fun parseDate(dateStr: String): Date? {
        return try {
            getFormatter().parse(dateStr)
        } catch (e: Exception) {
            null
        }
    }

    fun isSameDay(date1: Date, date2: Date): Boolean {
        val cal1 = Calendar.getInstance().apply { time = date1 }
        val cal2 = Calendar.getInstance().apply { time = date2 }
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
               cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }

    fun isSameDay(dateStr1: String, dateStr2: String): Boolean {
        val d1 = parseDate(dateStr1) ?: return false
        val d2 = parseDate(dateStr2) ?: return false
        return isSameDay(d1, d2)
    }

    fun isToday(dateStr: String): Boolean {
        val d = parseDate(dateStr) ?: return false
        return isSameDay(d, Date())
    }
}
