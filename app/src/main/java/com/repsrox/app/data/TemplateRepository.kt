package com.repsrox.app.data

import android.content.Context
import com.repsrox.app.data.db.AppDatabase
import com.repsrox.app.data.db.toModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

/** The saved weeks, in the database alongside the plan they are laid down into. */
class TemplateRepository(context: Context) {

    private val context = context.applicationContext
    private val db = AppDatabase.get(context)
    private val dao = db.planDao()

    val templates: Flow<List<WeekTemplate>> = flow {
        LegacyImport.ensure(this@TemplateRepository.context, db)
        emitAll(dao.observeTemplates().map { rows -> rows.map { it.toModel() } })
    }

    /** Writes a plan, replacing whatever was held under the same id. */
    suspend fun save(template: WeekTemplate) {
        LegacyImport.ensure(context, db)
        dao.save(template)
    }

    suspend fun remove(id: String) {
        LegacyImport.ensure(context, db)
        dao.deleteTemplate(id)
    }
}
