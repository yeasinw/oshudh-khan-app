package com.yeasinx.oshudhkhan.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface MedicineDao {
    @Query("SELECT * FROM medicines ORDER BY createdAt DESC")
    fun getAllMedicines(): Flow<List<Medicine>>

    @Query("SELECT * FROM medicines WHERE id = :id")
    suspend fun getMedicineById(id: Int): Medicine?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMedicine(medicine: Medicine): Long

    @Update
    suspend fun updateMedicine(medicine: Medicine)

    @Delete
    suspend fun deleteMedicine(medicine: Medicine)

    @Query("SELECT * FROM medicines")
    suspend fun getAll(): List<Medicine>

    @Query("SELECT * FROM medicines WHERE uuid = :uuid")
    suspend fun getByUuid(uuid: String): Medicine?

    @Query("DELETE FROM medicines")
    suspend fun deleteAll()

    @Query("SELECT * FROM medicines WHERE isActive == 1")
    suspend fun getActiveMedicines(): List<Medicine>
}