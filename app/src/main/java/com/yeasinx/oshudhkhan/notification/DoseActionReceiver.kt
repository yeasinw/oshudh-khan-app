package com.yeasinx.oshudhkhan.notification

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.yeasinx.oshudhkhan.MedicineApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime

// Handles the "Taken" action on a medicine reminder notification.
class DoseActionReceiver : BroadcastReceiver() {

    companion object {
        const val EXTRA_NOTIFICATION_ID = "notification_id"
        const val EXTRA_MEDICINE_ID = "medicine_id"
        const val EXTRA_DATE = "date"
        const val EXTRA_TIME = "time"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val medicineId = intent.getIntExtra(EXTRA_MEDICINE_ID, -1)
        val date = intent.getStringExtra(EXTRA_DATE)?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
        val time = intent.getStringExtra(EXTRA_TIME)?.let { runCatching { LocalTime.parse(it) }.getOrNull() }
        if (medicineId < 0 || date == null || time == null) return

        val repository = (context.applicationContext as MedicineApp).repository
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                repository.markTaken(medicineId, date, time)
                val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                manager.cancel(intent.getIntExtra(EXTRA_NOTIFICATION_ID, medicineId))
            } finally {
                pending.finish()
            }
        }
    }
}
