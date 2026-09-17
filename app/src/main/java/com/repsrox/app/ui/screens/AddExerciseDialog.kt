package com.repsrox.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.repsrox.app.data.Exercise
import com.repsrox.app.data.LOAD_RANGE_KG
import com.repsrox.app.data.NAME_MAX_CHARS
import com.repsrox.app.data.REPS_RANGE
import com.repsrox.app.data.SETS_RANGE
import com.repsrox.app.data.SetUnit
import com.repsrox.app.data.WorkSet
import com.repsrox.app.data.buildExercise
import com.repsrox.app.data.formatLoad
import com.repsrox.app.data.sanitise
import com.repsrox.app.ui.components.AccentAction
import com.repsrox.app.ui.components.ChoiceChip
import com.repsrox.app.ui.components.NumberField
import com.repsrox.app.ui.components.Panel
import com.repsrox.app.ui.components.PlainTextField
import com.repsrox.app.ui.components.QuietAction
import com.repsrox.app.ui.components.SectionLabel
import com.repsrox.app.ui.components.toDecimal
import com.repsrox.app.ui.theme.Accent
import com.repsrox.app.ui.theme.BorderAction
import com.repsrox.app.ui.theme.TextFaint
import com.repsrox.app.ui.theme.TextMeta
import com.repsrox.app.ui.theme.inter
import com.repsrox.app.ui.theme.oswald

/**
 * A movement and the work prescribed against it. Sets are laid out uniformly —
 * five of five at 120 kg — which is what a strength session is written as; a set
 * that runs heavy or light is changed on the day, in the live tracker. [initial]
 * opens the dialog pre-filled to edit an exercise already on the board rather
 * than add a new one; [onDelete] is offered only alongside one.
 */
@Composable
fun AddExerciseDialog(
    initial: Exercise? = null,
    onDismiss: () -> Unit,
    onSave: (Exercise) -> Unit,
    onDelete: (() -> Unit)? = null,
) {
    val initialSet = initial?.sets?.firstOrNull()
    var name by remember { mutableStateOf(initial?.name.orEmpty()) }
    var sets by remember { mutableStateOf(initial?.sets?.size?.toString().orEmpty()) }
    var reps by remember { mutableStateOf(initialSet?.reps?.toString().orEmpty()) }
    var load by remember { mutableStateOf(initialSet?.kg.orEmpty()) }
    var unit by remember { mutableStateOf(initialSet?.unit ?: SetUnit.REPS) }

    val setCount = sets.toIntOrNull()
    val repCount = reps.toIntOrNull()
    // An empty load is bodyweight, which is a real answer; a typed one must read.
    val kg = if (load.isBlank()) 0f else load.toDecimal()

    val cleanName = sanitise(name)
    val canSave = cleanName.isNotEmpty() &&
        setCount in SETS_RANGE &&
        repCount in REPS_RANGE &&
        kg != null && kg in LOAD_RANGE_KG

    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { if (initial == null) focus.requestFocus() }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Panel(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            contentPadding = PaddingValues(16.dp),
        ) {
            SectionLabel(if (initial == null) "Add exercise" else "Edit exercise")

            PlainTextField(
                value = name,
                onValueChange = { name = it },
                placeholder = "Back squat",
                maxChars = NAME_MAX_CHARS,
                modifier = Modifier
                    .padding(top = 14.dp)
                    .focusRequester(focus),
            )

            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ChoiceChip(
                    label = "Reps",
                    selected = unit == SetUnit.REPS,
                    modifier = Modifier.weight(1f),
                    onClick = { unit = SetUnit.REPS },
                )
                ChoiceChip(
                    label = "Metres",
                    selected = unit == SetUnit.METRES,
                    modifier = Modifier.weight(1f),
                    onClick = { unit = SetUnit.METRES },
                )
            }

            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Count("Sets", sets, { sets = it }, "5", Modifier.weight(1f))
                Count(if (unit == SetUnit.METRES) "Metres" else "Reps", reps, { reps = it }, "5", Modifier.weight(1f))
                Column(Modifier.weight(1.2f)) {
                    SectionLabel("Load", tracking = 0.10f, modifier = Modifier.padding(bottom = 5.dp))
                    NumberField(
                        value = load,
                        onValueChange = { load = it },
                        suffix = "kg",
                        placeholder = "—",
                        textStyle = oswald(18f, FontWeight.W500),
                    )
                }
            }

            Text(
                "Leave the load empty for bodyweight work.",
                color = TextFaint,
                style = inter(10.5f),
                modifier = Modifier.padding(top = 8.dp),
            )
            if (sets.isNotBlank() && setCount !in SETS_RANGE) {
                Hint("Between ${SETS_RANGE.first} and ${SETS_RANGE.last} sets")
            }
            if (reps.isNotBlank() && repCount !in REPS_RANGE) {
                Hint("Between ${REPS_RANGE.first} and ${REPS_RANGE.last} reps")
            }
            if (load.isNotBlank() && (kg == null || kg !in LOAD_RANGE_KG)) {
                Hint("Up to ${LOAD_RANGE_KG.endInclusive.toInt()} kg")
            }

            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                QuietAction("Cancel", modifier = Modifier.weight(1f), onClick = onDismiss)
                AccentAction(
                    if (initial == null) "Add" else "Save",
                    modifier = Modifier.weight(1f),
                    borderColor = if (canSave) Accent else BorderAction,
                    onClick = {
                        if (canSave) onSave(buildExercise(cleanName, setCount!!, repCount!!, kg!!, unit))
                    },
                )
            }

            if (onDelete != null) {
                QuietAction(
                    "Remove from board",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    onClick = onDelete,
                )
            }
        }
    }
}

/**
 * One set, changed on the day: the bar went up, or the last set came in two reps
 * short. Only this set moves — the rest of the exercise stays as it was written.
 */
@Composable
fun EditSetDialog(
    number: Int,
    initial: WorkSet,
    onDismiss: () -> Unit,
    onSave: (WorkSet) -> Unit,
) {
    var reps by remember { mutableStateOf(initial.reps.toString()) }
    var load by remember { mutableStateOf(initial.kg) }

    val repCount = reps.toIntOrNull()
    val kg = if (load.isBlank()) 0f else load.toDecimal()
    val canSave = repCount in REPS_RANGE && kg != null && kg in LOAD_RANGE_KG

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Panel(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            contentPadding = PaddingValues(16.dp),
        ) {
            SectionLabel("Set $number")

            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Count(
                    if (initial.unit == SetUnit.METRES) "Metres" else "Reps",
                    reps,
                    { reps = it },
                    "5",
                    Modifier.weight(1f),
                )
                Column(Modifier.weight(1.2f)) {
                    SectionLabel("Load", tracking = 0.10f, modifier = Modifier.padding(bottom = 5.dp))
                    NumberField(
                        value = load,
                        onValueChange = { load = it },
                        suffix = "kg",
                        placeholder = "—",
                        textStyle = oswald(18f, FontWeight.W500),
                    )
                }
            }

            if (reps.isNotBlank() && repCount !in REPS_RANGE) {
                Hint("Between ${REPS_RANGE.first} and ${REPS_RANGE.last} reps")
            }
            if (load.isNotBlank() && (kg == null || kg !in LOAD_RANGE_KG)) {
                Hint("Up to ${LOAD_RANGE_KG.endInclusive.toInt()} kg")
            }

            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                QuietAction("Cancel", modifier = Modifier.weight(1f), onClick = onDismiss)
                AccentAction(
                    "Save",
                    modifier = Modifier.weight(1f),
                    borderColor = if (canSave) Accent else BorderAction,
                    onClick = {
                        if (canSave) {
                            onSave(initial.copy(reps = repCount!!, kg = if (kg!! > 0f) formatLoad(kg) else ""))
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun Count(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        SectionLabel(label, tracking = 0.10f, modifier = Modifier.padding(bottom = 5.dp))
        NumberField(
            value = value,
            onValueChange = onValueChange,
            suffix = "",
            placeholder = placeholder,
            textStyle = oswald(18f, FontWeight.W500),
            decimal = false,
        )
    }
}

@Composable
private fun Hint(text: String) {
    Text(text, color = TextMeta, style = inter(10.5f), modifier = Modifier.padding(top = 5.dp))
}
