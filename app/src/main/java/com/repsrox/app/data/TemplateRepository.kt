package com.repsrox.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.DayOfWeek

private val Context.templateStore: DataStore<Preferences> by preferencesDataStore(name = "templates")

private val TEMPLATE_KEY = stringPreferencesKey("weeks")

/**
 * The saved weeks, on disk. Written flat — one line per session, repeating the
 * plan it belongs to — so a template session carries the same seven fields a
 * planned session does and can share its exercise codec:
 *
 *     p1|Hyrox build|MONDAY|STRENGTH|Lower push||Back squat~5 × 5 · 120 kg~5x120
 */
class TemplateRepository(context: Context) {

    private val store = context.applicationContext.templateStore

    val templates: Flow<List<WeekTemplate>> = store.data.map { it.templates() }

    /** Writes a plan, replacing whatever was held under the same id. */
    suspend fun save(template: WeekTemplate) = update { saved ->
        saved.filterNot { it.id == template.id } + template
    }

    suspend fun remove(id: String) = update { saved ->
        saved.filterNot { it.id == id }
    }

    private suspend fun update(transform: (List<WeekTemplate>) -> List<WeekTemplate>) {
        store.edit { prefs -> prefs[TEMPLATE_KEY] = encodeTemplates(transform(prefs.templates())) }
    }

    /** Nothing has ever been saved is an empty shelf, not a seeded one. */
    private fun Preferences.templates(): List<WeekTemplate> =
        this[TEMPLATE_KEY]?.let(::decodeTemplates) ?: emptyList()
}

internal fun encodeTemplates(templates: List<WeekTemplate>): String = templates
    .flatMap { template ->
        template.sessions.map { session ->
            listOf(
                template.id,
                template.name,
                session.dayOfWeek.name,
                session.kind.name,
                session.name,
                session.note,
                encodeExercises(session.exercises),
            ).joinToString(FIELD.toString())
        }
    }
    .joinToString("\n")

/**
 * Rebuilds the plans from their sessions, in the order the lines were written.
 * A line that cannot be read is dropped; a plan left with no readable session is
 * dropped with it, since an empty week is not a plan.
 */
internal fun decodeTemplates(raw: String): List<WeekTemplate> {
    val names = LinkedHashMap<String, String>()
    val sessions = LinkedHashMap<String, MutableList<TemplateSession>>()

    raw.lineSequence().forEach { line ->
        val fields = line.split(FIELD)
        if (fields.size < 7) return@forEach
        val id = fields[0].takeIf { it.isNotBlank() } ?: return@forEach
        val day = DayOfWeek.entries.firstOrNull { it.name == fields[2] } ?: return@forEach
        val kind = SessionKind.entries.firstOrNull { it.name == fields[3] } ?: return@forEach

        names.putIfAbsent(id, fields[1])
        sessions.getOrPut(id) { mutableListOf() }.add(
            TemplateSession(
                dayOfWeek = day,
                name = fields[4],
                kind = kind,
                note = fields[5],
                exercises = decodeExercises(fields[6]),
            ),
        )
    }

    return sessions.map { (id, list) -> WeekTemplate(id, names.getValue(id), list) }
}
