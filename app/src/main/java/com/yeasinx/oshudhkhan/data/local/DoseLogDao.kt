package com.yeasinx.oshudhkhan.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface DoseLogDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(log: DoseLog)

    @Query("DELETE FROM dose_logs WHERE medicineUuid = :medicineUuid AND date = :date AND time = :time")
    suspend fun delete(medicineUuid: String, date: String, time: String)

    @Query("SELECT EXISTS(SELECT 1 FROM dose_logs WHERE medicineId = :medicineId AND date = :date AND time = :time)")
    suspend fun exists(medicineId: Int, date: String, time: String): Boolean

    @Query("SELECT * FROM dose_logs WHERE date BETWEEN :from AND :to")
    fun logsBetween(from: String, to: String): Flow<List<DoseLog>>

    @Query("SELECT * FROM dose_logs")
    suspend fun getAll(): List<DoseLog>

    // Restored logs whose medicine arrived later get linked to its local id
    @Query(
        "UPDATE dose_logs SET medicineId = (SELECT id FROM medicines WHERE medicines.uuid = dose_logs.medicineUuid) " +
            "WHERE medicineId = 0 AND medicineUuid IN (SELECT uuid FROM medicines)"
    )
    suspend fun relinkToMedicines()

    @Query("DELETE FROM dose_logs")
    suspend fun deleteAll()
}
