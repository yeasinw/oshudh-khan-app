package com.yeasinx.oshudhkhan.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface DoseLogDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(log: DoseLog)

    @Query("DELETE FROM dose_logs WHERE medicineId = :medicineId AND date = :date AND time = :time")
    suspend fun delete(medicineId: Int, date: String, time: String)

    @Query("SELECT EXISTS(SELECT 1 FROM dose_logs WHERE medicineId = :medicineId AND date = :date AND time = :time)")
    suspend fun exists(medicineId: Int, date: String, time: String): Boolean

    @Query("SELECT * FROM dose_logs WHERE date BETWEEN :from AND :to")
    fun logsBetween(from: String, to: String): Flow<List<DoseLog>>
}
