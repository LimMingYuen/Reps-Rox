package com.repsrox.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.repsrox.app.data.Exercise
import com.repsrox.app.data.NAME_MAX_CHARS
import com.repsrox.app.data.PlannedSession
import com.repsrox.app.data.SessionKind
import com.repsrox.app.data.formatTonnes
import com.repsrox.app.data.plural
import com.repsrox.app.data.sanitise
import com.repsrox.app.ui.components.AccentAction
import com.repsrox.app.ui.components.ChoiceChip
import com.repsrox.app.ui.components.DateStepper
import com.repsrox.app.ui.components.Panel
import com.repsrox.app.ui.components.PlainTextField
import com.repsrox.app.ui.components.QuietAction
import com.repsrox.app.ui.components.RuledRow
import com.repsrox.app.ui.components.SectionLabel
import com.repsrox.app.ui.components.StatBlock
import com.repsrox.app.ui.components.icon
import com.repsrox.app.ui.theme.Accent
import com.repsrox.app.ui.theme.BorderAction
import com.repsrox.app.ui.theme.TextDim
import com.repsrox.app.ui.theme.TextFaint
import com.repsrox.app.ui.theme.TextMeta
import com.repsrox.app.ui.theme.TextPrimary
import com.repsrox.app.ui.theme.inter
import com.repsrox.app.ui.theme.mono
import com.repsrox.app.ui.theme.oswald
import java.time.LocalDate
import java.util.UUID

/**
 * Where a session is written. A strength session is built exercise by exercise
 * and is not worth saving until it holds one; everything else is a name, a day
 * and the line it is described by.
 */
@Composable
fun BuildSessionScreen(
    date: LocalDate,
    onSave: (PlannedSession) -> Unit,
    onCancel: () -> Unit,
) {
    val today = remember { LocalDate.now() }

    var name by remember { mutableStateOf("") }
    var kind by remember { mutableStateOf(SessionKind.STRENGTH) }
    var day by remember { mutableStateOf(date) }
    var note by remember { mutableStateOf("") }
    val exercises = remember { mutableStateListOf<Exercise>() }
    var adding by remember { mutableStateOf(false) }

    val cleanName = sanitise(name)
    // A strength session with nothing in it would open a live tracker with nothing
    // to bank, so it stays unsaveable until it prescribes something.
    val canSave = cleanName.isNotEmpty() &&
        (kind != SessionKind.STRENGTH || exercises.isNotEmpty())

    Column(
        Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Panel(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Field("Name") {
                PlainTextField(
                    value = name,
                    onValueChange = { name = it },
                    placeholder = "Lower push + sled finisher",
                    maxChars = NAME_MAX_CHARS,
                )
            }

            Field("Kind") {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    SessionKind.entries.forEach { option ->
                        ChoiceChip(
                            label = option.label(),
                            selected = option == kind,
                            icon = option.icon(),
                            modifier = Modifier.weight(1f),
                            onClick = { kind = option },
                        )
                    }
                }
            }

            // The plan runs forward from today; a session logged after the fact is
            // back-filled, so the stepper is left free in both directions.
            Field("Day") {
                DateStepper(date = day, onChange = { day = it }, today = today)
            }

            Field(if (kind == SessionKind.STRENGTH) "Note" else "Target") {
                PlainTextField(
                    value = note,
                    onValueChange = { note = it },
                    placeholder = when (kind) {
                        SessionKind.RUN -> "8 km Zone 2 · 75 min"
                        SessionKind.RACE -> "8 stations"
                        SessionKind.REST -> "Walk 6 km"
                        SessionKind.STRENGTH -> "Optional"
                    },
                    maxChars = NAME_MAX_CHARS,
                )
            }
        }

        if (kind == SessionKind.STRENGTH) {
            ExerciseList(exercises, onRemove = { exercises.removeAt(it) })
            AccentAction(
                "Add exercise",
                icon = Icons.Filled.Add,
                modifier = Modifier.fillMaxWidth(),
                onClick = { adding = true },
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            QuietAction("Cancel", modifier = Modifier.weight(1f), onClick = onCancel)
            AccentAction(
                "Save session",
                modifier = Modifier.weight(1f),
                borderColor = if (canSave) Accent else BorderAction,
                onClick = {
                    if (!canSave) return@AccentAction
                    onSave(
                        PlannedSession(
                            id = UUID.randomUUID().toString(),
                            date = day,
                            name = cleanName,
                            kind = kind,
                            note = sanitise(note),
                            // Only a strength session carries exercises; switching
                            // kind after building some leaves them behind.
                            exercises = if (kind == SessionKind.STRENGTH) exercises.toList() else emptyList(),
                        ),
                    )
                },
            )
        }
    }

    if (adding) {
        AddExerciseDialog(
            onDismiss = { adding = false },
            onAdd = { exercise ->
                exercises.add(exercise)
                adding = false
            },
        )
    }
}

@Composable
private fun ColumnScope.Field(label: String, content: @Composable () -> Unit) {
    SectionLabel(label, tracking = 0.10f, modifier = Modifier.padding(bottom = 6.dp))
    content()
}

@Composable
private fun ExerciseList(exercises: List<Exercise>, onRemove: (Int) -> Unit) {
    Column {
        Row(
            Modifier.fillMaxWidth().padding(bottom = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            SectionLabel("Exercises")
            if (exercises.isNotEmpty()) {
                val sets = exercises.sumOf { it.sets.size }
                SectionLabel("$sets ${plural(sets, "set")}", tracking = 0.10f)
            }
        }

        if (exercises.isEmpty()) {
            Text(
                "Nothing prescribed yet. Add the movements you'll work through, " +
                    "in the order you'll take them.",
                color = TextFaint,
                style = inter(11.5f, lineHeight = 1.5f),
                modifier = Modifier.padding(top = 4.dp, bottom = 6.dp),
            )
            return@Column
        }

        exercises.forEachIndexed { index, exercise ->
            RuledRow(verticalPadding = 10.dp) {
                Text(
                    "${index + 1}",
                    color = TextDim,
                    style = oswald(11f),
                    modifier = Modifier.width(14.dp),
                )
                Column(Modifier.weight(1f)) {
                    Text(
                        exercise.name,
                        color = TextPrimary,
                        style = inter(12.5f, FontWeight.W500, lineHeight = 1.3f),
                    )
                    Text(exercise.target, color = TextMeta, style = mono(10.5f))
                }
                // Drawn small to sit quietly in the row, with a finger-sized target.
                Box(
                    Modifier
                        .size(32.dp)
                        .clickable { onRemove(index) },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Outlined.Close,
                        contentDescription = "Remove ${exercise.name}",
                        tint = TextDim,
                        modifier = Modifier.size(14.dp),
                    )
                }
            }
        }

        val volume = exercises.fold(0f) { total, exercise ->
            total + exercise.sets.fold(0f) { sum, set ->
                sum + set.reps * (set.kg.toFloatOrNull() ?: 0f)
            }
        }
        if (volume > 0f) {
            Row(
                Modifier.padding(top = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                StatBlock("${exercises.size}", plural(exercises.size, "exercise"))
                StatBlock(formatTonnes(volume), "planned volume")
            }
        }
    }
}

private fun SessionKind.label(): String = when (this) {
    SessionKind.STRENGTH -> "Lift"
    SessionKind.RUN -> "Run"
    SessionKind.RACE -> "Race"
    SessionKind.REST -> "Rest"
}
