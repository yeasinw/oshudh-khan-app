package com.yeasinx.oshudhkhan.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

enum class FrequencyType {
    DAILY,
    EVERY_N_DAYS
}

// `id` is local only (alarm request codes, DoseLog.medicineId); `uuid` identifies the medicine in the cloud backup.
@Entity(tableName = "medicines", indices = [Index(value = ["uuid"], unique = true)])
data class Medicine(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    @ColumnInfo(defaultValue = "")
    val uuid: String = UUID.randomUUID().toString(),
    val name: String,
    val dosage: String,
    val frequencyType: FrequencyType,
    val timesPerDay: Int = 1,
    val intervalDays: Int = 1,
    val reminderTimes: String,
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    // Last local change; the newer side wins when merging with the cloud backup
    @ColumnInfo(defaultValue = "0")
    val updatedAt: Long = System.currentTimeMillis()
)