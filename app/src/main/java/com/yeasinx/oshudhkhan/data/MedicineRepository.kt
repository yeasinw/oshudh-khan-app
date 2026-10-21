package com.yeasinx.oshudhkhan.data

import com.yeasinx.oshudhkhan.data.remote.CloudBackup.Companion.DOSE_LOGS
import com.yeasinx.oshudhkhan.data.remote.CloudBackup.Companion.MEDICINES
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.LocalTime
import com.yeasinx.oshudhkhan.data.local.DoseLog
import com.yeasinx.oshudhkhan.data.local.DoseLogDao
import com.yeasinx.oshudhkhan.data.local.Medicine
import com.yeasinx.oshudhkhan.data.local.MedicineDao
import com.yeasinx.oshudhkhan.data.local.Tombstone
import com.yeasinx.oshudhkhan.data.local.TombstoneDao
import com.yeasinx.oshudhkhan.data.remote.CloudBackup
import com.yeasinx.oshudhkhan.data.remote.doseDocId
import com.yeasinx.oshudhkhan.data.remote.toRecord

// Room is the source of truth; each write is stamped for merging and mirrored to the cloud backup
// while signed in. Deletes leave a tombstone so they also reach the cloud after an offline/signed-out delete.
class MedicineRepository(
    private val dao: MedicineDao,
    private val doseLogDao: DoseLogDao,
    private val tombstoneDao: TombstoneDao,
    private val backup: CloudBackup
) {
    val allMedicine: Flow<List<Medicine>> = dao.getAllMedicines()

    suspend fun getById(id: Int): Medicine? = dao.getMedicineById(id)

    suspend fun insert(medicine: Medicine): Long {
        val stamped = medicine.copy(updatedAt = System.currentTimeMillis())
        return dao.insertMedicine(stamped).also { backup.push(MEDICINES, stamped.toRecord()) }
    }

    suspend fun update(medicine: Medicine) {
        val stamped = medicine.copy(updatedAt = System.currentTimeMillis())
        dao.updateMedicine(stamped)
        backup.push(MEDICINES, stamped.toRecord())
    }

    // Dose history is kept (locally and in the cloud) after a medicine is deleted
    suspend fun delete(medicine: Medicine) {
        dao.deleteMedicine(medicine)
        deleted(MEDICINES, medicine.uuid)
    }

    suspend fun getActiveMedicines(): List<Medicine> = dao.getActiveMedicines()

    fun doseLogsBetween(from: LocalDate, to: LocalDate): Flow<List<DoseLog>> =
        doseLogDao.logsBetween(from.toString(), to.toString())

    suspend fun isTaken(medicineId: Int, date: LocalDate, time: LocalTime): Boolean =
        doseLogDao.exists(medicineId, date.toString(), time.toString())

    suspend fun markTaken(medicineId: Int, date: LocalDate, time: LocalTime) {
        val medicine = dao.getMedicineById(medicineId) ?: return
        val log = DoseLog(
            medicineId = medicineId,
            medicineUuid = medicine.uuid,
            date = date.toString(),
            time = time.toString()
        )
        doseLogDao.insert(log)
        val record = log.toRecord()
        tombstoneDao.delete(DOSE_LOGS, record.id)
        backup.push(DOSE_LOGS, record)
    }

    suspend fun unmarkTaken(medicineId: Int, date: LocalDate, time: LocalTime) {
        val medicine = dao.getMedicineById(medicineId) ?: return
        doseLogDao.delete(medicine.uuid, date.toString(), time.toString())
        deleted(DOSE_LOGS, doseDocId(medicine.uuid, date.toString(), time.toString()))
    }

    private suspend fun deleted(kind: String, docId: String) {
        val tombstone = Tombstone(kind, docId, System.currentTimeMillis())
        tombstoneDao.upsert(tombstone)
        backup.push(kind, tombstone.toRecord())
    }
}
