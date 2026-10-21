package com.yeasinx.oshudhkhan.data.local

import android.content.Context
import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

class Converters {
    @TypeConverter
    fun fromFrequencyType(value: FrequencyType): String = value.name

    @TypeConverter
    fun toFrequencyType(value: String): FrequencyType = FrequencyType.valueOf(value)
}

@Database(
    entities = [Medicine::class, DoseLog::class, Tombstone::class],
    version = 3,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class MedicineDatabase : RoomDatabase() {

    abstract fun medicineDao(): MedicineDao
    abstract fun doseLogDao(): DoseLogDao
    abstract fun tombstoneDao(): TombstoneDao

    companion object {
        @Volatile
        private var INSTANCE: MedicineDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `dose_logs` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`medicineId` INTEGER NOT NULL, `date` TEXT NOT NULL, " +
                        "`time` TEXT NOT NULL, `takenAt` INTEGER NOT NULL)"
                )
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS `index_dose_logs_medicineId_date_time` " +
                        "ON `dose_logs` (`medicineId`, `date`, `time`)"
                )
            }
        }

        // Cloud backup support: stable ids, change times and delete markers
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Random hex is enough for the uuid since it is only an opaque key
                db.execSQL("ALTER TABLE `medicines` ADD COLUMN `uuid` TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE `medicines` ADD COLUMN `updatedAt` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("UPDATE `medicines` SET `uuid` = lower(hex(randomblob(16))), `updatedAt` = `createdAt`")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_medicines_uuid` ON `medicines` (`uuid`)")

                // Dose logs are now unique per medicine uuid; history of already-deleted medicines gets a placeholder
                db.execSQL("DROP INDEX IF EXISTS `index_dose_logs_medicineId_date_time`")
                db.execSQL("ALTER TABLE `dose_logs` ADD COLUMN `medicineUuid` TEXT NOT NULL DEFAULT ''")
                db.execSQL(
                    "UPDATE `dose_logs` SET `medicineUuid` = COALESCE(" +
                        "(SELECT `uuid` FROM `medicines` WHERE `medicines`.`id` = `dose_logs`.`medicineId`), " +
                        "'orphan-' || `medicineId`)"
                )
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS `index_dose_logs_medicineUuid_date_time` " +
                        "ON `dose_logs` (`medicineUuid`, `date`, `time`)"
                )

                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `tombstones` (`kind` TEXT NOT NULL, `docId` TEXT NOT NULL, " +
                        "`deletedAt` INTEGER NOT NULL, PRIMARY KEY(`kind`, `docId`))"
                )
            }
        }

        fun getDatabase(context: Context): MedicineDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    MedicineDatabase::class.java,
                    "medicine_database"
                ).addMigrations(MIGRATION_1_2, MIGRATION_2_3).build()
                INSTANCE = instance
                instance
            }
        }
    }
}