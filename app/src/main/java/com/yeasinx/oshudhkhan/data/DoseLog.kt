package com.yeasinx.oshudhkhan.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

// One row per dose actually taken. No foreign key: history survives medicine deletion.
@Entity(
    tableName = "dose_logs",
    indices = [Index(value = ["medicineId", "date", "time"], unique = true)]
)
data class DoseLog(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val medicineId: Int,
    val date: String,      // yyyy-MM-dd
    val time: String,      // HH:mm, as stored in Medicine.reminderTimes
    val takenAt: Long = System.currentTimeMillis()
)
