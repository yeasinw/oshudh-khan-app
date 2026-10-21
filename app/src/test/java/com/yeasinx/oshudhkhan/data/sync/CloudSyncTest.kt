package com.yeasinx.oshudhkhan.data.sync

import com.yeasinx.oshudhkhan.data.local.DoseLog
import com.yeasinx.oshudhkhan.data.local.FrequencyType
import com.yeasinx.oshudhkhan.data.local.Medicine
import com.yeasinx.oshudhkhan.data.remote.SyncRecord
import com.yeasinx.oshudhkhan.data.UserProfile
import com.yeasinx.oshudhkhan.data.remote.doseFromRecord
import com.yeasinx.oshudhkhan.data.remote.medicineFromRecord
import com.yeasinx.oshudhkhan.data.remote.profileFromRecord
import com.yeasinx.oshudhkhan.data.remote.syncRecordFromDoc
import com.yeasinx.oshudhkhan.data.remote.toDoc
import com.yeasinx.oshudhkhan.data.remote.toRecord
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CloudSyncTest {

    private fun live(id: String, at: Long) = SyncRecord(id, at, data = mapOf("v" to at))
    private fun gone(id: String, at: Long) = SyncRecord(id, at, deleted = true)

    // Simulates a Firestore write + read: whole numbers come back as Long
    private fun viaFirestore(record: SyncRecord) =
        syncRecordFromDoc(record.id, record.toDoc().mapValues { (_, v) -> if (v is Int) v.toLong() else v })

    @Test
    fun oneSidedItemsMoveToTheOtherSide() {
        val result = reconcile(listOf(live("local", 1)), listOf(live("remote", 1)))
        assertEquals(listOf(live("local", 1)), result.push)
        assertEquals(listOf(live("remote", 1)), result.applyLocally)
    }

    @Test
    fun remoteDeleteOfUnknownItemIsIgnored() {
        val result = reconcile(emptyList(), listOf(gone("x", 1)))
        assertEquals(emptyList<SyncRecord>(), result.applyLocally + result.push)
    }

    @Test
    fun newerEditWins() {
        assertEquals(listOf(live("x", 2)), reconcile(listOf(live("x", 2)), listOf(live("x", 1))).push)
        assertEquals(listOf(live("x", 2)), reconcile(listOf(live("x", 1)), listOf(live("x", 2))).applyLocally)
    }

    @Test
    fun deleteOnOneDeviceReachesTheOther() {
        // Deleted here after the cloud copy was written: push the delete
        assertEquals(listOf(gone("x", 5)), reconcile(listOf(gone("x", 5)), listOf(live("x", 1))).push)
        // Deleted on another device: delete locally
        assertEquals(listOf(gone("x", 5)), reconcile(listOf(live("x", 1)), listOf(gone("x", 5))).applyLocally)
    }

    @Test
    fun editAfterRemoteDeleteRestoresTheItem() {
        assertEquals(listOf(live("x", 9)), reconcile(listOf(live("x", 9)), listOf(gone("x", 5))).push)
    }

    @Test
    fun deleteWinsATimestampTie() {
        assertEquals(listOf(gone("x", 3)), reconcile(listOf(gone("x", 3)), listOf(live("x", 3))).push)
        assertEquals(listOf(gone("x", 3)), reconcile(listOf(live("x", 3)), listOf(gone("x", 3))).applyLocally)
    }

    @Test
    fun identicalSidesProduceNoWork() {
        val result = reconcile(listOf(live("a", 1), gone("b", 2)), listOf(live("a", 1), gone("b", 2)))
        assertEquals(emptyList<SyncRecord>(), result.applyLocally + result.push)
    }

    @Test
    fun medicineRoundTripsThroughFirestore() {
        val original = Medicine(
            id = 7, uuid = "m1", name = "Napa", dosage = "1 tablet",
            frequencyType = FrequencyType.EVERY_N_DAYS, intervalDays = 3, reminderTimes = "08:00,20:00",
            isActive = false, createdAt = 1L, updatedAt = 2L
        )
        // Local id is not part of the backup
        assertEquals(original.copy(id = 0), medicineFromRecord(viaFirestore(original.toRecord())))
    }

    @Test
    fun malformedMedicineDocIsIgnored() {
        assertNull(medicineFromRecord(SyncRecord("x", 1, data = mapOf("name" to "Only a name"))))
    }

    @Test
    fun doseRoundTripsWithItsMedicineUuid() {
        val log = DoseLog(medicineId = 3, medicineUuid = "m1", date = "2026-10-21", time = "08:00", takenAt = 5L)
        val record = viaFirestore(log.toRecord())
        assertEquals("m1_2026-10-21_08:00", record.id)
        assertEquals(5L, record.updatedAt)
        assertEquals(log.copy(medicineId = 0), doseFromRecord(record))
    }

    @Test
    fun profileRoundTrips() {
        val profile = UserProfile("Yeasin", 30, "a@b.com")
        val record = viaFirestore(profile.toRecord(updatedAt = 4L))
        assertEquals(profile, profileFromRecord(record))
        assertEquals(4L, record.updatedAt)
    }
}
