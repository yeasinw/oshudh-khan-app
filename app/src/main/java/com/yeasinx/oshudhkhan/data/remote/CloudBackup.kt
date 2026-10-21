package com.yeasinx.oshudhkhan.data.remote

import android.util.Log
import com.google.android.gms.tasks.Task
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.tasks.await
import com.yeasinx.oshudhkhan.data.UserProfile
import com.yeasinx.oshudhkhan.data.local.DoseLog
import com.yeasinx.oshudhkhan.data.local.FrequencyType
import com.yeasinx.oshudhkhan.data.local.Medicine
import com.yeasinx.oshudhkhan.data.local.Tombstone

// One synced item as stored in the cloud. Deletes are kept as `deleted` markers so they
// reach other devices; `updatedAt` decides which side wins in a merge.
data class SyncRecord(
    val id: String,
    val updatedAt: Long,
    val deleted: Boolean = false,
    val data: Map<String, Any?> = emptyMap()
)

data class RemoteSnapshot(val medicines: List<SyncRecord>, val doses: List<SyncRecord>, val profile: SyncRecord?)

// Firestore I/O for the backup at users/{uid}. Writes are no-ops while signed out.
// Writes are not awaited: Firestore queues them offline and sends them when the network returns.
class CloudBackup(
    private val auth: FirebaseAuth = Firebase.auth,
    private val firestore: FirebaseFirestore = Firebase.firestore
) {
    private fun userDoc(uid: String) = firestore.collection(USERS).document(uid)

    private fun write(block: (uid: String) -> Task<Void>) {
        val uid = auth.currentUser?.uid ?: return
        block(uid).addOnFailureListener { Log.w(TAG, "Backup write failed", it) }
    }

    // kind is MEDICINES or DOSE_LOGS
    fun push(kind: String, record: SyncRecord) = write { uid ->
        userDoc(uid).collection(kind).document(record.id).set(record.toDoc())
    }

    fun pushProfile(record: SyncRecord) = write { uid ->
        userDoc(uid).set(record.toDoc(), SetOptions.merge())
    }

    suspend fun fetch(uid: String): RemoteSnapshot {
        val user = userDoc(uid)
        suspend fun records(kind: String) = user.collection(kind).get().await().documents
            .mapNotNull { doc -> doc.data?.let { syncRecordFromDoc(doc.id, it) } }
        val profile = user.get().await().data?.let { syncRecordFromDoc(PROFILE_ID, it) }
        return RemoteSnapshot(records(MEDICINES), records(DOSE_LOGS), profile)
    }

    companion object {
        const val MEDICINES = "medicines"
        const val DOSE_LOGS = "doseLogs"
        const val PROFILE_ID = "profile"
        private const val USERS = "users"
        private const val TAG = "CloudBackup"
    }
}

fun SyncRecord.toDoc(): Map<String, Any?> = data + mapOf("updatedAt" to updatedAt, "deleted" to deleted)

fun syncRecordFromDoc(id: String, doc: Map<String, Any?>) = SyncRecord(
    id = id,
    updatedAt = (doc["updatedAt"] as? Number)?.toLong() ?: 0L,
    deleted = doc["deleted"] == true,
    data = doc - setOf("updatedAt", "deleted")
)

fun doseDocId(medicineUuid: String, date: String, time: String) = "${medicineUuid}_${date}_$time"

fun Tombstone.toRecord() = SyncRecord(docId, deletedAt, deleted = true)

fun Medicine.toRecord() = SyncRecord(
    id = uuid,
    updatedAt = updatedAt,
    data = mapOf(
        "name" to name,
        "dosage" to dosage,
        "frequencyType" to frequencyType.name,
        "timesPerDay" to timesPerDay,
        "intervalDays" to intervalDays,
        "reminderTimes" to reminderTimes,
        "isActive" to isActive,
        "createdAt" to createdAt
    )
)

// Firestore returns whole numbers as Long; returns null for malformed docs so one bad doc can't break a restore.
fun medicineFromRecord(record: SyncRecord): Medicine? = runCatching {
    val map = record.data
    Medicine(
        uuid = record.id,
        name = map["name"] as String,
        dosage = map["dosage"] as String,
        frequencyType = FrequencyType.valueOf(map["frequencyType"] as String),
        timesPerDay = (map["timesPerDay"] as Number).toInt(),
        intervalDays = (map["intervalDays"] as Number).toInt(),
        reminderTimes = map["reminderTimes"] as String,
        isActive = map["isActive"] as Boolean,
        createdAt = (map["createdAt"] as Number).toLong(),
        updatedAt = record.updatedAt
    )
}.getOrNull()

// A dose's change time is when it was taken
fun DoseLog.toRecord() = SyncRecord(
    id = doseDocId(medicineUuid, date, time),
    updatedAt = takenAt,
    data = mapOf("medicineUuid" to medicineUuid, "date" to date, "time" to time, "takenAt" to takenAt)
)

// medicineId is local; the caller links it to the matching medicine (0 if it no longer exists)
fun doseFromRecord(record: SyncRecord): DoseLog? = runCatching {
    val map = record.data
    DoseLog(
        medicineId = 0,
        medicineUuid = map["medicineUuid"] as String,
        date = map["date"] as String,
        time = map["time"] as String,
        takenAt = (map["takenAt"] as Number).toLong()
    )
}.getOrNull()

fun UserProfile.toRecord(updatedAt: Long) = SyncRecord(
    id = CloudBackup.PROFILE_ID,
    updatedAt = updatedAt,
    data = mapOf("name" to name, "age" to age, "email" to email)
)

fun profileFromRecord(record: SyncRecord) = UserProfile(
    name = record.data["name"] as? String ?: "",
    age = (record.data["age"] as? Number)?.toInt(),
    email = record.data["email"] as? String ?: ""
)
