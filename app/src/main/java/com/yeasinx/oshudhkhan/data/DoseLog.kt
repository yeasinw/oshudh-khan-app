package com.yeasinx.oshudhkhan.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

// One row per dose actually taken. No foreign key: history survives medicine deletion.
// medicineUuid identifies the dose across installs; medicineId is 0 for history restored
// from the cloud whose medicine no longer exists.
@Entity(
    tableName = "dose_logs",
    indices = [Index(value = ["medicineUuid", "date", "time"], unique = true)]
)
data class DoseLog(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val medicineId: Int,
    @ColumnInfo(defaultValue = "")
    val medicineUuid: String,
    val date: String,      // yyyy-MM-dd
    val time: String,      // HH:mm, as stored in Medicine.reminderTimes
    val takenAt: Long = System.currentTimeMillis()
)
