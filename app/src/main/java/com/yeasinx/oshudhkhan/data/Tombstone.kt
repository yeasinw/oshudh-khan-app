package com.yeasinx.oshudhkhan.data

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

// Records a local delete (medicine or un-taken dose) so the merge can tell "deleted here"
// from "never had it" and propagate the delete instead of restoring the item.
@Entity(tableName = "tombstones", primaryKeys = ["kind", "docId"])
data class Tombstone(
    val kind: String,      // cloud collection name, see CloudBackup.MEDICINES / DOSE_LOGS
    val docId: String,
    val deletedAt: Long
)

@Dao
interface TombstoneDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(tombstone: Tombstone)

    @Query("DELETE FROM tombstones WHERE kind = :kind AND docId = :docId")
    suspend fun delete(kind: String, docId: String)

    @Query("SELECT * FROM tombstones WHERE kind = :kind")
    suspend fun getAll(kind: String): List<Tombstone>

    @Query("DELETE FROM tombstones")
    suspend fun clear()
}
