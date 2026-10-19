package com.yeasinx.oshudhkhan

import android.app.Application
import android.os.Build
import com.yeasinx.oshudhkhan.data.MedicineDatabase
import com.yeasinx.oshudhkhan.data.MedicineRepository
import com.yeasinx.oshudhkhan.data.UserRepository
import com.yeasinx.oshudhkhan.notification.NotificationHelper

class MedicineApp : Application() {
    val database by lazy { MedicineDatabase.getDatabase(this) }
    val repository by lazy { MedicineRepository(database.medicineDao(), database.doseLogDao()) }
    val userRepository by lazy { UserRepository(this) }

    override fun onCreate() {
        super.onCreate()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationHelper.createNotificationChannel(this)
        }
    }
}