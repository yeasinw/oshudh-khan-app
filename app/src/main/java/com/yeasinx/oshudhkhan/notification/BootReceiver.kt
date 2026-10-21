package com.yeasinx.oshudhkhan.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.yeasinx.oshudhkhan.data.local.MedicineDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        CoroutineScope(Dispatchers.IO).launch {
            val db = MedicineDatabase.getDatabase(context)
            val activeMedicines = db.medicineDao().getActiveMedicines()
            activeMedicines.forEach { medicine ->
                AlarmScheduler.scheduleMedicineAlarms(context, medicine)
            }
        }
    }
}