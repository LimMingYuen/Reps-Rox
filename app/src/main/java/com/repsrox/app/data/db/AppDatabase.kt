package com.repsrox.app.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
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
    version = 1,
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
                .build()
                .also { instance = it }
        }
    }
}

class Converters {
    /** ISO dates, so they sort as text the way they sort as dates. */
    @TypeConverter
    fun fromDate(date: LocalDate): String = date.toString()

    @TypeConverter
    fun toDate(raw: String): LocalDate = LocalDate.parse(raw)
}
