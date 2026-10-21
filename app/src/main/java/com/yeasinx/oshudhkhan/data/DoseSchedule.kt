package com.yeasinx.oshudhkhan.data

import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import com.yeasinx.oshudhkhan.data.local.FrequencyType
import com.yeasinx.oshudhkhan.data.local.Medicine

// Single source of truth for which doses of a medicine are due on a given date.
object DoseSchedule {

    fun reminderTimes(medicine: Medicine): List<LocalTime> =
        medicine.reminderTimes.split(",")
            .map { it.trim() }
            .mapNotNull { runCatching { LocalTime.parse(it) }.getOrNull() }

    fun isDueOn(medicine: Medicine, date: LocalDate, zone: ZoneId = ZoneId.systemDefault()): Boolean {
        val start = Instant.ofEpochMilli(medicine.createdAt).atZone(zone).toLocalDate()
        if (date.isBefore(start)) return false
        return when (medicine.frequencyType) {
            FrequencyType.DAILY -> true
            FrequencyType.EVERY_N_DAYS ->
                ChronoUnit.DAYS.between(start, date) % medicine.intervalDays.coerceAtLeast(1) == 0L
        }
    }

    fun dosesFor(medicine: Medicine, date: LocalDate, zone: ZoneId = ZoneId.systemDefault()): List<LocalTime> =
        if (medicine.isActive && isDueOn(medicine, date, zone)) reminderTimes(medicine) else emptyList()

    // First date on or after `from` when the medicine is due.
    fun nextDueDate(medicine: Medicine, from: LocalDate, zone: ZoneId = ZoneId.systemDefault()): LocalDate {
        var date = from
        while (!isDueOn(medicine, date, zone)) date = date.plusDays(1)
        return date
    }
}
