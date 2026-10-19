package com.yeasinx.oshudhkhan.notification

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.yeasinx.oshudhkhan.MedicineApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

class AlarmReceiver : BroadcastReceiver() {

    companion object {
        const val EXTRA_MEDICINE_ID = "medicine_id"
        const val EXTRA_MEDICINE_NAME = "medicine_name"
        const val EXTRA_MEDICINE_DOSAGE = "medicine_dosage"
        const val EXTRA_HOUR = "hour"
        const val EXTRA_MINUTE = "minute"
        const val EXTRA_INTERVAL_DAYS = "interval_days"
        const val EXTRA_REQUEST_CODE = "request_code"
        // Present only on snoozed reminders: the date the dose originally belonged to
        const val EXTRA_SNOOZE_DATE = "snooze_date"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val medicineId = intent.getIntExtra(EXTRA_MEDICINE_ID, -1)
        val medicineName = intent.getStringExtra(EXTRA_MEDICINE_NAME) ?: return
        val medicineDosage = intent.getStringExtra(EXTRA_MEDICINE_DOSAGE) ?: ""
        val doseTime = LocalTime.of(intent.getIntExtra(EXTRA_HOUR, 8), intent.getIntExtra(EXTRA_MINUTE, 0))
        val requestCode = intent.getIntExtra(EXTRA_REQUEST_CODE, medicineId)
        val snoozeDate = intent.getStringExtra(EXTRA_SNOOZE_DATE)
            ?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
        val doseDate = snoozeDate ?: LocalDate.now()

        // Snoozes are one-off; only the recurring alarm schedules its next occurrence
        if (snoozeDate == null) {
            scheduleNext(context, intent, doseTime, requestCode)
        }

        val repository = (context.applicationContext as MedicineApp).repository
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Skip the reminder if the dose was already taken (e.g. from the Home screen)
                if (!repository.isTaken(medicineId, doseDate, doseTime)) {
                    NotificationHelper.showMedicineNotification(
                        context = context,
                        notificationId = requestCode,
                        medicineId = medicineId,
                        medicineName = medicineName,
                        dosage = medicineDosage,
                        doseDate = doseDate,
                        doseTime = doseTime
                    )
                }
            } finally {
                pending.finish()
            }
        }
    }

    private fun scheduleNext(context: Context, intent: Intent, doseTime: LocalTime, requestCode: Int) {
        val intervalDays = intent.getIntExtra(EXTRA_INTERVAL_DAYS, 1)
        val zone = ZoneId.systemDefault()
        val triggerAtMillis = LocalDate.now(zone).plusDays(intervalDays.toLong())
            .atTime(doseTime).atZone(zone).toInstant().toEpochMilli()

        // Same extras as the alarm that just fired, so the chain keeps repeating
        val nextIntent = Intent(context, AlarmReceiver::class.java).putExtras(intent)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            nextIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        AlarmScheduler.setExact(context, triggerAtMillis, pendingIntent)
    }
}
