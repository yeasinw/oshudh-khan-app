package com.yeasinx.oshudhkhan.data.sync

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.yeasinx.oshudhkhan.data.remote.CloudBackup.Companion.DOSE_LOGS
import com.yeasinx.oshudhkhan.data.remote.CloudBackup.Companion.MEDICINES
import com.yeasinx.oshudhkhan.notification.AlarmScheduler
import kotlinx.coroutines.flow.first
import com.yeasinx.oshudhkhan.data.UserRepository
import com.yeasinx.oshudhkhan.data.local.DoseLogDao
import com.yeasinx.oshudhkhan.data.local.MedicineDao
import com.yeasinx.oshudhkhan.data.local.TombstoneDao
import com.yeasinx.oshudhkhan.data.remote.CloudBackup
import com.yeasinx.oshudhkhan.data.remote.SyncRecord
import com.yeasinx.oshudhkhan.data.remote.doseFromRecord
import com.yeasinx.oshudhkhan.data.remote.medicineFromRecord
import com.yeasinx.oshudhkhan.data.remote.toRecord

private val Context.syncDataStore by preferencesDataStore(name = "sync_state")

data class Reconciliation(val applyLocally: List<SyncRecord>, val push: List<SyncRecord>)

// Last-writer-wins per item. Local and remote lists include delete markers, so a delete made on
// either side beats an older copy on the other. On a timestamp tie the delete wins.
fun reconcile(local: List<SyncRecord>, remote: List<SyncRecord>): Reconciliation {
    val localById = local.associateBy { it.id }
    val remoteById = remote.associateBy { it.id }
    val applyLocally = mutableListOf<SyncRecord>()
    val push = mutableListOf<SyncRecord>()
    for (id in localById.keys + remoteById.keys) {
        val l = localById[id]
        val r = remoteById[id]
        when {
            r == null -> push += l!!
            l == null -> if (!r.deleted) applyLocally += r
            l.updatedAt > r.updatedAt || (l.updatedAt == r.updatedAt && l.deleted && !r.deleted) -> push += l
            r.updatedAt > l.updatedAt || (r.updatedAt == l.updatedAt && r.deleted && !l.deleted) -> applyLocally += r
        }
    }
    return Reconciliation(applyLocally, push)
}

// Runs whenever a signed-in user is detected (sign-in and each launch): merges Room with the cloud.
class CloudSync(
    private val context: Context,
    private val medicineDao: MedicineDao,
    private val doseLogDao: DoseLogDao,
    private val tombstoneDao: TombstoneDao,
    private val userRepository: UserRepository,
    private val backup: CloudBackup
) {
    private val syncState = context.applicationContext.syncDataStore

    suspend fun sync(uid: String, email: String?) {
        // Local data belongs to the account it was last synced with; a different account replaces it
        val owner = syncState.data.first()[OWNER_UID]
        if (owner != null && owner != uid) clearLocalData()

        val remote = backup.fetch(uid)

        val medicines = reconcile(
            medicineDao.getAll().map { it.toRecord() } + tombstoneDao.getAll(MEDICINES).map { it.toRecord() },
            remote.medicines
        )
        medicines.applyLocally.forEach { applyMedicine(it) }
        medicines.push.forEach { backup.push(MEDICINES, it) }

        val doses = reconcile(
            doseLogDao.getAll().map { it.toRecord() } + tombstoneDao.getAll(DOSE_LOGS).map { it.toRecord() },
            remote.doses
        )
        doses.applyLocally.forEach { applyDose(it) }
        doses.push.forEach { backup.push(DOSE_LOGS, it) }
        doseLogDao.relinkToMedicines()

        // A never-saved local profile (updatedAt 0) has nothing to contribute
        val profile = reconcile(listOf(userRepository.record()).filter { it.updatedAt > 0 }, listOfNotNull(remote.profile))
        profile.applyLocally.forEach { userRepository.saveFromCloud(it) }
        profile.push.forEach(backup::pushProfile)

        // Done after the merge so a fresh install doesn't overwrite the backed-up profile
        val current = userRepository.profile.first()
        if (current.email.isBlank() && email != null) userRepository.save(current.copy(email = email))

        syncState.edit { it[OWNER_UID] = uid }
    }

    // Writes go straight to the DAOs: these changes come from the cloud and must not be re-stamped or pushed
    private suspend fun applyMedicine(record: SyncRecord) {
        val existing = medicineDao.getByUuid(record.id)
        existing?.let { AlarmScheduler.cancelMedicineAlarms(context, it) }
        if (record.deleted) {
            existing?.let { medicineDao.deleteMedicine(it) }
            return
        }
        val incoming = medicineFromRecord(record) ?: return
        val saved = if (existing != null) {
            incoming.copy(id = existing.id).also { medicineDao.updateMedicine(it) }
        } else {
            incoming.copy(id = medicineDao.insertMedicine(incoming).toInt())
        }
        tombstoneDao.delete(MEDICINES, record.id)
        if (saved.isActive) AlarmScheduler.scheduleMedicineAlarms(context, saved)
    }

    private suspend fun applyDose(record: SyncRecord) {
        if (record.deleted) {
            // Dose ids are "<medicineUuid>_<date>_<time>"; uuids never contain '_'
            val (uuid, date, time) = record.id.split('_').takeIf { it.size == 3 } ?: return
            doseLogDao.delete(uuid, date, time)
            return
        }
        val dose = doseFromRecord(record) ?: return
        val medicineId = medicineDao.getByUuid(dose.medicineUuid)?.id ?: 0
        doseLogDao.insert(dose.copy(medicineId = medicineId))
        tombstoneDao.delete(DOSE_LOGS, record.id)
    }

    private suspend fun clearLocalData() {
        medicineDao.getAll().forEach { AlarmScheduler.cancelMedicineAlarms(context, it) }
        medicineDao.deleteAll()
        doseLogDao.deleteAll()
        tombstoneDao.clear()
        userRepository.clear()
    }

    private companion object {
        val OWNER_UID = stringPreferencesKey("owner_uid")
    }
}
