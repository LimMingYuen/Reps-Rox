package com.repsrox.app.data

import android.content.Context
import com.repsrox.app.data.db.AppDatabase
import com.repsrox.app.data.db.WeighInEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate

/** The weigh-in log, in the database: a row a day, keyed by its date. */
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

/**
 * A first-run log, so the screen opens with the shape the design shows instead
 * of an empty chart. It is the design's own series, hung off the install date
 * as twelve weekly weigh-ins, written into the database when that is first set up.
 * Delete this and its use in [LegacyImport] to ship an empty tracker.
 */
internal val WEIGHT_SEED: List<WeighIn> by lazy {
    val today = LocalDate.now()
    val last = WEIGHT_SERIES.lastIndex
    WEIGHT_SERIES.mapIndexed { index, kg ->
        WeighIn(date = today.minusWeeks((last - index).toLong()), kg = kg)
    }
}
