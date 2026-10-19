package com.yeasinx.oshudhkhan.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.yeasinx.oshudhkhan.MedicineApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime

// Handles the "Taken" and "Remind in 15 min" actions on a dose reminder.
class DoseActionReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_TAKEN = "com.yeasinx.oshudhkhan.action.DOSE_TAKEN"
        const val ACTION_SNOOZE = "com.yeasinx.oshudhkhan.action.DOSE_SNOOZE"
        const val EXTRA_NOTIFICATION_ID = "notification_id"
        const val EXTRA_MEDICINE_ID = "medicine_id"
        const val EXTRA_MEDICINE_NAME = "medicine_name"
        const val EXTRA_MEDICINE_DOSAGE = "medicine_dosage"
        const val EXTRA_DATE = "date"
        const val EXTRA_TIME = "time"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, -1)
        val medicineId = intent.getIntExtra(EXTRA_MEDICINE_ID, -1)
        val date = intent.getStringExtra(EXTRA_DATE)?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
        val time = intent.getStringExtra(EXTRA_TIME)?.let { runCatching { LocalTime.parse(it) }.getOrNull() }
        if (medicineId < 0 || notificationId < 0 || date == null || time == null) return

        when (intent.action) {
            ACTION_TAKEN -> {
                val repository = (context.applicationContext as MedicineApp).repository
                val pending = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        repository.markTaken(medicineId, date, time)
                        NotificationHelper.cancel(context, notificationId)
                    } finally {
                        pending.finish()
                    }
                }
            }
            ACTION_SNOOZE -> {
                AlarmScheduler.scheduleSnooze(
                    context = context,
                    requestCode = notificationId,
                    medicineId = medicineId,
                    medicineName = intent.getStringExtra(EXTRA_MEDICINE_NAME) ?: return,
                    dosage = intent.getStringExtra(EXTRA_MEDICINE_DOSAGE) ?: "",
                    date = date,
                    time = time
                )
                NotificationHelper.cancel(context, notificationId)
            }
        }
    }
}
