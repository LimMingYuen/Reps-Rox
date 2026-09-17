package com.repsrox.app.ui.screens

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.repsrox.app.data.Exercise
import com.repsrox.app.data.SetUnit
import com.repsrox.app.data.formatMinutes
import com.repsrox.app.ui.PlanViewModel
import com.repsrox.app.ui.RepsRoxViewModel
import com.repsrox.app.ui.components.AccentAction
import com.repsrox.app.ui.components.Panel
import com.repsrox.app.ui.components.QuietAction
import com.repsrox.app.ui.components.RuledRow
import com.repsrox.app.ui.components.SectionLabel
import com.repsrox.app.ui.theme.Accent
import com.repsrox.app.ui.theme.AccentLine
import com.repsrox.app.ui.theme.AccentLineSoft
import com.repsrox.app.ui.theme.AccentSet
import com.repsrox.app.ui.theme.AccentWash
import com.repsrox.app.ui.theme.BorderAction
import com.repsrox.app.ui.theme.BorderChip
import com.repsrox.app.ui.theme.BorderSoft
import com.repsrox.app.ui.theme.SurfaceBg
import com.repsrox.app.ui.theme.SurfaceRaised
import com.repsrox.app.ui.theme.TextDim
import com.repsrox.app.ui.theme.TextFaint
import com.repsrox.app.ui.theme.TextMeta
import com.repsrox.app.ui.theme.TextMuted
import com.repsrox.app.ui.theme.TextPrimary
import com.repsrox.app.ui.theme.TextSecondary
import com.repsrox.app.ui.theme.TextSubtle
import com.repsrox.app.ui.theme.inter
import com.repsrox.app.ui.theme.mono
import com.repsrox.app.ui.theme.oswald

@Composable
fun LiveScreen(viewModel: RepsRoxViewModel, planViewModel: PlanViewModel = viewModel()) {
    val exercises = viewModel.activeExercises
    val exercise = exercises.getOrNull(viewModel.currentExercise) ?: return
    val session = viewModel.activeSession
    var editingSet by remember { mutableStateOf<Int?>(null) }
    var editingExercise by remember { mutableStateOf(false) }

    // The board and the plan are the same session, so a change here is written back.
    fun rewrite(changed: Exercise) {
        viewModel.updateExercise(viewModel.currentExercise, changed)?.let(planViewModel::save)
    }

    Column(
        Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                formatMinutes(viewModel.sessionSeconds),
                color = TextPrimary,
                style = oswald(30f, tracking = 0.02f),
            )
            if (viewModel.sessionOn) RecordingPulse() else PausedLabel(viewModel.sessionSeconds)
            Spacer(Modifier.weight(1f))
            Text(
                "${viewModel.totalSetsDone}/${viewModel.plannedSets} sets",
                color = TextMeta,
                style = mono(11f),
            )
        }

        if (session != null) {
            Text(
                session.name,
                color = TextSecondary,
                style = inter(12.5f, FontWeight.W500, lineHeight = 1.3f),
                modifier = Modifier.padding(top = 2.dp),
            )
        }

        Panel(
            borderColor = AccentLineSoft,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    exercise.name,
                    color = TextPrimary,
                    style = oswald(19f, FontWeight.W500, lineHeight = 1.15f, tracking = 0.02f),
                )
                Spacer(Modifier.weight(1f))
                Text(exercise.target, color = TextSecondary, style = mono(11.5f))
                // Drawn small to sit quietly in the card, with a finger-sized target.
                Box(
                    Modifier
                        .size(32.dp)
                        .clickable { editingExercise = true },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Filled.Edit,
                        contentDescription = "Edit ${exercise.name}",
                        tint = TextDim,
                        modifier = Modifier.size(13.dp),
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                exercise.sets.forEachIndexed { index, set ->
                    SetChip(
                        reps = set.reps,
                        kg = set.kg,
                        unit = set.unit,
                        banked = index < viewModel.setsDone[viewModel.currentExercise],
                        onClick = { viewModel.logSet(viewModel.currentExercise, index) },
                        onHold = { editingSet = index },
                    )
                }
            }
            Text(
                "Tap a set to bank it. Hold one to change its weight or reps; " +
                    "the pencil changes how many sets.",
                color = TextFaint,
                style = inter(10.5f),
            )
        }

        RestCard(
            seconds = viewModel.restSeconds,
            target = viewModel.restTarget,
            onAdjust = viewModel::adjustRest,
            onSkip = viewModel::skipRest,
        )

        Column {
            SectionLabel("Up next", modifier = Modifier.padding(bottom = 6.dp))
            exercises.forEachIndexed { index, item ->
                val isCurrent = index == viewModel.currentExercise
                val complete = viewModel.setsDone[index] == item.sets.size
                RuledRow(onClick = { viewModel.selectExercise(index) }) {
                    Text(
                        "${index + 1}",
                        color = if (isCurrent) Accent else TextDim,
                        style = oswald(11f),
                        modifier = Modifier.width(14.dp),
                    )
                    Text(
                        item.name,
                        color = when {
                            isCurrent -> Accent
                            complete -> TextMuted
                            else -> TextPrimary
                        },
                        style = inter(12.5f, FontWeight.W500, lineHeight = 1.3f),
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        "${viewModel.setsDone[index]}/${item.sets.size}",
                        color = TextFaint,
                        style = mono(11f),
                    )
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            AccentAction(
                // A session yet to be started reads Start, not Resume: there is
                // nothing to resume until the clock has run.
                when {
                    viewModel.sessionOn -> "Pause"
                    viewModel.sessionSeconds == 0 -> "Start"
                    else -> "Resume"
                },
                modifier = Modifier.weight(1f),
                onClick = viewModel::toggleSession,
            )
            QuietAction(
                "Finish session",
                modifier = Modifier.weight(1f),
                onClick = viewModel::finishSession,
            )
        }
    }

    editingSet?.let { index ->
        val set = exercise.sets.getOrNull(index)
        if (set == null) {
            editingSet = null
        } else {
            EditSetDialog(
                number = index + 1,
                initial = set,
                onDismiss = { editingSet = null },
                onSave = { changed ->
                    rewrite(exercise.copy(sets = exercise.sets.toMutableList().also { it[index] = changed }))
                    editingSet = null
                },
            )
        }
    }

    if (editingExercise) {
        AddExerciseDialog(
            initial = exercise,
            onDismiss = { editingExercise = false },
            onSave = { changed ->
                rewrite(changed)
                editingExercise = false
            },
            // The last exercise stays: a board with nothing on it has nothing to bank.
            onDelete = if (exercises.size > 1) {
                {
                    viewModel.removeExercise(viewModel.currentExercise)?.let(planViewModel::save)
                    editingExercise = false
                }
            } else {
                null
            },
        )
    }
}

/** The design's `rrpulse`: full to 35% and back, over two seconds. */
@Composable
private fun RecordingPulse() {
    val transition = rememberInfiniteTransition(label = "recording")
    val alpha by transition.animateFloat(
        initialValue = 1f,
        targetValue = 0.35f,
        animationSpec = infiniteRepeatable(tween(1000), RepeatMode.Reverse),
        label = "alpha",
    )
    Text(
        "recording".uppercase(),
        color = Accent,
        style = inter(10f, lineHeight = 1f, tracking = 0.12f),
        modifier = Modifier.alpha(alpha),
    )
}

/** What stands where the pulse does while the clock is stopped. */
@Composable
private fun PausedLabel(seconds: Int) {
    Text(
        (if (seconds == 0) "not started" else "paused").uppercase(),
        color = TextMeta,
        style = inter(10f, lineHeight = 1f, tracking = 0.12f),
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun RowScope.SetChip(
    reps: Int,
    kg: String,
    unit: SetUnit,
    banked: Boolean,
    onClick: () -> Unit,
    onHold: () -> Unit,
) {
    val shape = RoundedCornerShape(6.dp)
    Column(
        Modifier
            .weight(1f)
            .height(56.dp)
            .clip(shape)
            .background(if (banked) AccentSet else SurfaceRaised, shape)
            .border(1.dp, if (banked) Accent else BorderChip, shape)
            .combinedClickable(onClick = onClick, onLongClick = onHold),
        verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        val metres = unit == SetUnit.METRES
        // A loaded carry has its second line taken by the load, so the distance says so itself.
        Text(
            if (metres && kg.isNotBlank()) "$reps m" else "$reps",
            color = if (banked) Accent else TextPrimary,
            style = oswald(15f),
        )
        Text(
            // Unloaded work carries no load, so the chip names what is being counted instead.
            if (kg.isNotBlank()) "$kg kg" else if (metres) "metres" else "reps",
            color = if (banked) Accent else TextPrimary,
            style = inter(9.5f, lineHeight = 1f, tracking = 0.04f),
        )
    }
}

@Composable
private fun RestCard(seconds: Int, target: Int, onAdjust: (Int) -> Unit, onSkip: () -> Unit) {
    // When the clock runs out the whole card lifts to the accent.
    val resting = seconds > 0
    val edge = if (resting) BorderSoft else AccentLine
    val ink = if (resting) TextPrimary else Accent

    Panel(
        background = if (resting) SurfaceBg else AccentWash,
        borderColor = edge,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(
                Modifier
                    .size(46.dp)
                    .border(2.dp, edge, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Outlined.Timer,
                    contentDescription = null,
                    tint = ink,
                    modifier = Modifier.size(20.dp),
                )
            }
            Column(Modifier.weight(1f)) {
                SectionLabel("Rest · ${formatMinutes(target)}", tracking = 0.12f)
                Text(
                    if (resting) formatMinutes(seconds) else "ready",
                    color = ink,
                    style = oswald(24f, lineHeight = 1.1f),
                    modifier = Modifier.padding(top = 3.dp),
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                RestButton("−15", "Shorten rest by 15 seconds") { onAdjust(-1) }
                RestButton("+15", "Lengthen rest by 15 seconds") { onAdjust(1) }
                RestButton("Skip", "Skip rest", onSkip)
            }
        }
    }
}

@Composable
private fun RestButton(label: String, description: String, onClick: () -> Unit) {
    Text(
        label.uppercase(),
        color = TextSubtle,
        style = inter(11f, FontWeight.W500, lineHeight = 1f, tracking = 0.06f),
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .border(1.dp, BorderAction, RoundedCornerShape(6.dp))
            .clickable(onClickLabel = description, onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp),
    )
}
