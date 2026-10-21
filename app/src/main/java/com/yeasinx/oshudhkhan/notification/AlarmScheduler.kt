package com.yeasinx.oshudhkhan.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.yeasinx.oshudhkhan.data.local.FrequencyType
import com.yeasinx.oshudhkhan.data.DoseSchedule
import com.yeasinx.oshudhkhan.data.local.Medicine
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

object AlarmScheduler {

    private const val SNOOZE_MINUTES = 15L

    // Also the notification id for that dose slot (index = position in reminderTimes, max 4).
    fun requestCode(medicineId: Int, index: Int) = medicineId * 100 + index

    // Snooze alarms use their own slot so they never replace the recurring alarm.
    private fun snoozeRequestCode(requestCode: Int) = requestCode + 50

    fun scheduleMedicineAlarms(context: Context, medicine: Medicine) {
        val times = medicine.reminderTimes.split(",").map { it.trim() }.filter { it.isNotEmpty() }

        times.forEachIndexed { index, time ->
            val parts = time.split(":")
            if (parts.size < 2) return@forEachIndexed
            val hour = parts[0].toIntOrNull() ?: return@forEachIndexed
            val minute = parts[1].toIntOrNull() ?: return@forEachIndexed

            val requestCode = requestCode(medicine.id, index)

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

            setExact(context, triggerAtMillis, pendingIntent)
        }
    }

    fun cancelMedicineAlarms(context: Context, medicine: Medicine) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val times = medicine.reminderTimes.split(",")

        times.forEachIndexed { index, _ ->
            val requestCode = requestCode(medicine.id, index)
            listOf(requestCode, snoozeRequestCode(requestCode)).forEach { code ->
                val pendingIntent = PendingIntent.getBroadcast(
                    context,
                    code,
                    Intent(context, AlarmReceiver::class.java),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                alarmManager.cancel(pendingIntent)
            }
        }
    }

    // One-off repeat of a dose reminder; does not touch the recurring schedule.
    fun scheduleSnooze(
        context: Context,
        requestCode: Int,
        medicineId: Int,
        medicineName: String,
        dosage: String,
        date: LocalDate,
        time: LocalTime
    ) {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra(AlarmReceiver.EXTRA_MEDICINE_ID, medicineId)
            putExtra(AlarmReceiver.EXTRA_MEDICINE_NAME, medicineName)
            putExtra(AlarmReceiver.EXTRA_MEDICINE_DOSAGE, dosage)
            putExtra(AlarmReceiver.EXTRA_HOUR, time.hour)
            putExtra(AlarmReceiver.EXTRA_MINUTE, time.minute)
            putExtra(AlarmReceiver.EXTRA_REQUEST_CODE, requestCode)
            putExtra(AlarmReceiver.EXTRA_SNOOZE_DATE, date.toString())
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            snoozeRequestCode(requestCode),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        setExact(context, System.currentTimeMillis() + SNOOZE_MINUTES * 60_000, pendingIntent)
    }

    fun setExact(context: Context, triggerAtMillis: Long, pendingIntent: PendingIntent) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) return
        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
    }
}
