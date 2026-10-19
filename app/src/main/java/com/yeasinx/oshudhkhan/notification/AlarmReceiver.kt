package com.yeasinx.oshudhkhan.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.icu.util.Calendar
import android.os.Build
import java.time.LocalTime

class AlarmReceiver : BroadcastReceiver() {

    companion object {
        const val EXTRA_MEDICINE_ID = "medicine_id"
        const val EXTRA_MEDICINE_NAME = "medicine_name"
        const val EXTRA_MEDICINE_DOSAGE = "medicine_dosage"
        const val EXTRA_HOUR = "hour"
        const val EXTRA_MINUTE = "minute"
        const val EXTRA_INTERVAL_DAYS = "interval_days"
        const val EXTRA_REQUEST_CODE = "request_code"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val medicineId = intent.getIntExtra(EXTRA_MEDICINE_ID, -1)
        val medicineName = intent.getStringExtra(EXTRA_MEDICINE_NAME) ?: return
        val medicineDosage = intent.getStringExtra(EXTRA_MEDICINE_DOSAGE) ?: ""
        val hour = intent.getIntExtra(EXTRA_HOUR, 8)
        val minute = intent.getIntExtra(EXTRA_MINUTE, 0)
        val intervalDays = intent.getIntExtra(EXTRA_INTERVAL_DAYS, 1)
        val requestCode = intent.getIntExtra(EXTRA_REQUEST_CODE, medicineId)

        NotificationHelper.showMedicineNotification(
            context = context,
            notificationId = requestCode,
            medicineId = medicineId,
            medicineName = medicineName,
            dosage = medicineDosage,
            doseTime = LocalTime.of(hour, minute)
        )

        // Schedule the next alarm
        val nextCalender = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            add(Calendar.DAY_OF_MONTH, intervalDays)
        }

        val nextIntent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra(EXTRA_MEDICINE_ID, medicineId)
            putExtra(EXTRA_MEDICINE_NAME, medicineName)
            putExtra(EXTRA_MEDICINE_DOSAGE, medicineDosage)
            putExtra(EXTRA_HOUR, hour)
            putExtra(EXTRA_MINUTE, minute)
            putExtra(EXTRA_INTERVAL_DAYS, intervalDays)
            putExtra(EXTRA_REQUEST_CODE, requestCode)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            nextIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    nextCalender.timeInMillis,
                    pendingIntent
                )
            }
        } else {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                nextCalender.timeInMillis,
                pendingIntent
            )
        }
    }
}