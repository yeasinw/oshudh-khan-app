package com.yeasinx.oshudhkhan.data

import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.LocalTime

class MedicineRepository(
    private val dao: MedicineDao,
    private val doseLogDao: DoseLogDao
) {
    val allMedicine: Flow<List<Medicine>> = dao.getAllMedicines()

    suspend fun getById(id: Int): Medicine? = dao.getMedicineById(id)

    suspend fun insert(medicine: Medicine): Long = dao.insertMedicine(medicine)

    suspend fun update(medicine: Medicine) = dao.updateMedicine(medicine)

    suspend fun delete(medicine: Medicine) = dao.deleteMedicine(medicine)

    suspend fun getActiveMedicines(): List<Medicine> = dao.getActiveMedicines()

    fun doseLogsBetween(from: LocalDate, to: LocalDate): Flow<List<DoseLog>> =
        doseLogDao.logsBetween(from.toString(), to.toString())

    suspend fun markTaken(medicineId: Int, date: LocalDate, time: LocalTime) =
        doseLogDao.insert(DoseLog(medicineId = medicineId, date = date.toString(), time = time.toString()))

    suspend fun unmarkTaken(medicineId: Int, date: LocalDate, time: LocalTime) =
        doseLogDao.delete(medicineId, date.toString(), time.toString())
}
