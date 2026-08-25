package com.repsrox.app.ui.screens

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.repsrox.app.data.EXERCISES
import com.repsrox.app.data.PLANNED_SETS
import com.repsrox.app.ui.RepsRoxViewModel
import com.repsrox.app.ui.components.Panel
import com.repsrox.app.ui.components.QuietAction
import com.repsrox.app.ui.components.RuledRow
import com.repsrox.app.ui.components.SectionLabel
import com.repsrox.app.ui.formatMinutes
import com.repsrox.app.ui.theme.Accent
import com.repsrox.app.ui.theme.AccentLineSoft
import com.repsrox.app.ui.theme.AccentSet
import com.repsrox.app.ui.theme.AccentWash
import com.repsrox.app.ui.theme.AccentLine
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
            RecordingPulse()
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
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                exercise.sets.forEachIndexed { index, set ->
                    SetChip(
                        reps = set.reps,
                        kg = set.kg,
                        banked = index < viewModel.setsDone[viewModel.currentExercise],
                        onClick = { viewModel.logSet(viewModel.currentExercise, index) },
                    )
                }
            }
            Text(
                "Tap a set to bank it at target. Hold to change weight or reps.",
                color = TextFaint,
                style = inter(10.5f),
            )
        }

        RestCard(
            seconds = viewModel.restSeconds,
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

        QuietAction(
            "Finish session",
            modifier = Modifier.fillMaxWidth(),
            onClick = viewModel::finishSession,
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

@Composable
private fun RowScope.SetChip(reps: Int, kg: String, banked: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(6.dp)
    Column(
        Modifier
            .weight(1f)
            .height(56.dp)
            .clip(shape)
            .background(if (banked) AccentSet else SurfaceRaised, shape)
            .border(1.dp, if (banked) Accent else BorderChip, shape)
            .clickable(onClick = onClick),
        verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("$reps", color = if (banked) Accent else TextPrimary, style = oswald(15f))
        Text(
            // Bodyweight work carries no load, so the chip says reps and nothing else.
            if (kg.isBlank()) "reps" else "$kg kg",
            color = if (banked) Accent else TextPrimary,
            style = inter(9.5f, lineHeight = 1f, tracking = 0.04f),
        )
    }
}

@Composable
private fun RestCard(seconds: Int, onSkip: () -> Unit) {
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
                SectionLabel("Rest", tracking = 0.12f)
                Text(
                    if (resting) formatMinutes(seconds) else "ready",
                    color = ink,
                    style = oswald(24f, lineHeight = 1.1f),
                    modifier = Modifier.padding(top = 3.dp),
                )
            }
            Text(
                "Skip".uppercase(),
                color = TextSubtle,
                style = inter(11f, FontWeight.W500, lineHeight = 1f, tracking = 0.06f),
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .border(1.dp, BorderAction, RoundedCornerShape(6.dp))
                    .clickable(onClick = onSkip)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            )
        }
    }
}
