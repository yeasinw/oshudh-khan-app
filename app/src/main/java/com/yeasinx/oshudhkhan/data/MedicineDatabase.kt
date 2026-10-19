package com.yeasinx.oshudhkhan.data

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
    entities = [Medicine::class, DoseLog::class],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class MedicineDatabase : RoomDatabase() {

    abstract fun medicineDao(): MedicineDao
    abstract fun doseLogDao(): DoseLogDao

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

        fun getDatabase(context: Context): MedicineDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    MedicineDatabase::class.java,
                    "medicine_database"
                ).addMigrations(MIGRATION_1_2).build()
                INSTANCE = instance
                instance
            }
        }
    }
}