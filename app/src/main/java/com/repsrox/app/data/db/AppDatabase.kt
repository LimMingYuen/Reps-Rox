package com.repsrox.app.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import java.time.LocalDate

/** Everything the app keeps: the week, the saved weeks, weigh-ins, and sessions part-way through. */
@Database(
    entities = [
        SessionEntity::class,
        TemplateEntity::class,
        TemplateSessionEntity::class,
        ExerciseEntity::class,
        SetEntity::class,
        WeighInEntity::class,
        WorkoutProgressEntity::class,
        MetaEntity::class,
    ],
    version = 2,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun planDao(): PlanDao
    abstract fun weightDao(): WeightDao
    abstract fun workoutDao(): WorkoutDao
    abstract fun metaDao(): MetaDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        /** One database for the process — the repositories all share it. */
        fun get(context: Context): AppDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "repsrox.db")
                .addMigrations(MIGRATION_1_2)
                .build()
                .also { instance = it }
        }
    }
}

/**
 * Sets gain their unit. Version 1 dropped it on the way in, so every distance came
 * back as reps; the target line still says "4 × 1000 m", which is enough to put the
 * metres back on the rows already stored.
 */
internal val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE sets ADD COLUMN unit TEXT NOT NULL DEFAULT 'REPS'")
        db.execSQL(
            "UPDATE sets SET unit = 'METRES' WHERE exerciseId IN " +
                "(SELECT id FROM exercises WHERE target LIKE '% m' OR target LIKE '% m ·%')",
        )
    }
}

class Converters {
    /** ISO dates, so they sort as text the way they sort as dates. */
    @TypeConverter
    fun fromDate(date: LocalDate): String = date.toString()

    @TypeConverter
    fun toDate(raw: String): LocalDate = LocalDate.parse(raw)
}
