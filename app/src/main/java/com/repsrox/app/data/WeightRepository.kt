package com.repsrox.app.data

import android.content.Context
import com.repsrox.app.data.db.AppDatabase
import com.repsrox.app.data.db.WeighInEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate

/**
 * The weigh-in log, in the database: a row a day, keyed by its date.
 *
 * Nothing is seeded. An install that has weighed in nowhere reads as an empty
 * log, which the Body screen shows as its empty state rather than as a trend
 * drawn through numbers nobody stood on a scale for.
 */
class WeightRepository(context: Context) {

    private val context = context.applicationContext
    private val db = AppDatabase.get(context)
    private val dao = db.weightDao()

    /** Oldest first — the order both the chart and the trend want. */
    val weighIns: Flow<List<WeighIn>> = flow {
        LegacyImport.ensure(this@WeightRepository.context, db)
        emitAll(dao.observe().map { rows -> rows.map { WeighIn(it.date, it.kg) } })
    }

    /** Records a weigh-in, replacing whatever was already logged for that date. */
    suspend fun add(entry: WeighIn) {
        LegacyImport.ensure(context, db)
        dao.put(WeighInEntity(entry.date, entry.kg))
    }

    suspend fun remove(date: LocalDate) {
        LegacyImport.ensure(context, db)
        dao.remove(date)
    }
}
