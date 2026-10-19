package com.yeasinx.oshudhkhan

import com.yeasinx.oshudhkhan.data.DoseSchedule
import com.yeasinx.oshudhkhan.data.FrequencyType
import com.yeasinx.oshudhkhan.data.Medicine
import com.yeasinx.oshudhkhan.ui.home.greetingFor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset

class DoseScheduleTest {
    private val zone = ZoneOffset.UTC
    private val start = LocalDate.of(2026, 10, 1)

    private fun medicine(type: FrequencyType, interval: Int = 1, active: Boolean = true) = Medicine(
        name = "A", dosage = "1", frequencyType = type, intervalDays = interval,
        reminderTimes = "08:00, 20:30", isActive = active,
        createdAt = start.atTime(12, 0).toInstant(zone).toEpochMilli()
    )

    @Test fun dailyReturnsAllTimes() {
        assertEquals(
            listOf(LocalTime.of(8, 0), LocalTime.of(20, 30)),
            DoseSchedule.dosesFor(medicine(FrequencyType.DAILY), start.plusDays(5), zone)
        )
    }

    @Test fun everyNDaysOnlyOnDueDays() {
        val m = medicine(FrequencyType.EVERY_N_DAYS, interval = 3)
        assertEquals(2, DoseSchedule.dosesFor(m, start.plusDays(3), zone).size)
        assertTrue(DoseSchedule.dosesFor(m, start.plusDays(4), zone).isEmpty())
        assertEquals(start.plusDays(6), DoseSchedule.nextDueDate(m, start.plusDays(4), zone))
    }

    @Test fun inactiveAndBeforeCreationHaveNoDoses() {
        assertTrue(DoseSchedule.dosesFor(medicine(FrequencyType.DAILY, active = false), start, zone).isEmpty())
        assertTrue(DoseSchedule.dosesFor(medicine(FrequencyType.DAILY), start.minusDays(1), zone).isEmpty())
    }

    @Test fun greetingByHour() {
        assertEquals("Good morning", greetingFor(5))
        assertEquals("Good afternoon", greetingFor(12))
        assertEquals("Good evening", greetingFor(20))
        assertEquals("Good night", greetingFor(2))
    }
}
