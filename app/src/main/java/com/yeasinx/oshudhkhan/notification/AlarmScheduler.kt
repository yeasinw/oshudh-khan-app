package com.yeasinx.oshudhkhan.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.yeasinx.oshudhkhan.data.FrequencyType
import com.yeasinx.oshudhkhan.data.DoseSchedule
import com.yeasinx.oshudhkhan.data.Medicine
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

object AlarmScheduler {

    fun scheduleMedicineAlarms(context: Context, medicine: Medicine) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val times = medicine.reminderTimes.split(",").map { it.trim() }.filter { it.isNotEmpty() }

        times.forEachIndexed { index, time ->
            val parts = time.split(":")
            if (parts.size < 2) return@forEachIndexed
            val hour = parts[0].toIntOrNull() ?: return@forEachIndexed
            val minute = parts[1].toIntOrNull() ?: return@forEachIndexed

            val requestCode = medicine.id * 100 + index

            val intervalDays = when (medicine.frequencyType) {
                FrequencyType.DAILY -> 1
                FrequencyType.EVERY_N_DAYS -> medicine.intervalDays
            }

            val intent = Intent(context, AlarmReceiver::class.java).apply {
                putExtra(AlarmReceiver.EXTRA_MEDICINE_ID, medicine.id)
                putExtra(AlarmReceiver.EXTRA_MEDICINE_NAME, medicine.name)
                putExtra(AlarmReceiver.EXTRA_MEDICINE_DOSAGE, medicine.dosage)
                putExtra(AlarmReceiver.EXTRA_HOUR, hour)
                putExtra(AlarmReceiver.EXTRA_MINUTE, minute)
                putExtra(AlarmReceiver.EXTRA_INTERVAL_DAYS, intervalDays)
                putExtra(AlarmReceiver.EXTRA_REQUEST_CODE, requestCode)
            }

            val pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // First trigger on a day the medicine is actually due (keeps EVERY_N_DAYS in phase with DoseSchedule)
            val zone = ZoneId.systemDefault()
            val now = LocalDateTime.now(zone)
            val earliest = if (now.toLocalTime() < LocalTime.of(hour, minute)) now.toLocalDate()
                           else now.toLocalDate().plusDays(1)
            val triggerDate: LocalDate = DoseSchedule.nextDueDate(medicine, earliest, zone)
            val triggerAtMillis = triggerDate.atTime(hour, minute).atZone(zone).toInstant().toEpochMilli()

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerAtMillis,
                        pendingIntent
                    )
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            }
        }
    }

    fun cancelMedicineAlarms(context: Context, medicine: Medicine) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val times = medicine.reminderTimes.split(",")

        times.forEachIndexed { index, _ ->
            val requestCode = medicine.id * 100 + index
            val intent = Intent(context, AlarmReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.cancel(pendingIntent)
        }
    }
}