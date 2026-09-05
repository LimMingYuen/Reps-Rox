package com.repsrox.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.repsrox.app.data.SessionKind
import com.repsrox.app.data.WeekTemplate
import com.repsrox.app.ui.PlanViewModel
import com.repsrox.app.ui.RepsRoxViewModel
import com.repsrox.app.ui.components.Panel
import com.repsrox.app.ui.components.QuietAction
import com.repsrox.app.ui.components.RuledRow
import com.repsrox.app.ui.components.icon
import com.repsrox.app.ui.theme.Accent
import com.repsrox.app.ui.theme.TextDim
import com.repsrox.app.ui.theme.TextMeta
import com.repsrox.app.ui.theme.TextPrimary
import com.repsrox.app.ui.theme.TextSubtle
import com.repsrox.app.ui.theme.inter
import com.repsrox.app.ui.theme.mono
import com.repsrox.app.ui.theme.oswald
import java.util.Locale
import java.time.format.TextStyle as DateTextStyle

/**
 * The saved weeks. A plan here is a week worth repeating; laying one down writes
 * real sessions onto the weeks you choose, which you are then free to change
 * without touching the plan they came from.
 */
@Composable
fun PlansScreen(viewModel: RepsRoxViewModel, planViewModel: PlanViewModel = viewModel()) {
    val plans by planViewModel.plans.collectAsState()
    var applying by remember { mutableStateOf<WeekTemplate?>(null) }

    Column(
        Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        // The shelf is a detour off the week, so the way back is on the screen too.
        QuietAction(
            "Back to the week",
            icon = Icons.Filled.ChevronLeft,
            onClick = { viewModel.back() },
        )

        val saved = plans ?: return@Column

        if (saved.isEmpty()) {
            EmptyShelf()
            return@Column
        }

        saved.forEach { plan ->
            PlanCard(
                plan = plan,
                onApply = { applying = plan },
                onDelete = { planViewModel.deletePlan(plan.id) },
            )
        }
    }

    applying?.let { plan ->
        ApplyPlanDialog(
            plan = plan,
            weekStart = viewModel.weekStart,
            onDismiss = { applying = null },
            onApply = { weekStart, weeks ->
                planViewModel.applyPlan(plan.id, weekStart, weeks)
                applying = null
                // Land on the first week written, so the result is in front of you.
                viewModel.goToWeek(weekStart)
                viewModel.back()
            },
        )
    }
}

@Composable
private fun EmptyShelf() {
    Panel {
        Text("No plans saved.", color = TextPrimary, style = oswald(20f, FontWeight.W500))
        Text(
            "Build a week you'd train again, then save it from the week screen. " +
                "It can be laid down over as many weeks as you like from here.",
            color = TextSubtle,
            style = inter(11.5f, lineHeight = 1.5f),
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

@Composable
private fun PlanCard(plan: WeekTemplate, onApply: () -> Unit, onDelete: () -> Unit) {
    Panel(contentPadding = PaddingValues(start = 14.dp, top = 14.dp, bottom = 14.dp, end = 6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    plan.name,
                    color = TextPrimary,
                    style = oswald(19f, FontWeight.W500, lineHeight = 1.15f, tracking = 0.02f),
                )
                Text(
                    plan.summary(),
                    color = TextMeta,
                    style = mono(11f),
                    modifier = Modifier.padding(top = 3.dp),
                )
            }
            // Drawn small to sit quietly in the card, with a finger-sized target.
            Box(
                Modifier
                    .size(32.dp)
                    .clickable(onClick = onDelete),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Outlined.Close,
                    contentDescription = "Delete ${plan.name}",
                    tint = TextDim,
                    modifier = Modifier.size(14.dp),
                )
            }
        }

        Column(Modifier.padding(top = 6.dp, end = 8.dp)) {
            plan.sessions.forEach { session ->
                RuledRow(verticalPadding = 9.dp) {
                    Text(
                        session.dayOfWeek
                            .getDisplayName(DateTextStyle.SHORT, Locale.US)
                            .uppercase(),
                        color = TextDim,
                        style = oswald(11f, tracking = 0.06f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.width(30.dp),
                    )
                    Icon(
                        session.kind.icon(),
                        contentDescription = null,
                        tint = if (session.kind == SessionKind.REST) TextDim else Accent,
                        modifier = Modifier.size(14.dp),
                    )
                    Text(
                        session.name,
                        color = if (session.kind == SessionKind.REST) TextDim else TextPrimary,
                        style = inter(12f, FontWeight.W500, lineHeight = 1.3f),
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        when {
                            session.exercises.isNotEmpty() ->
                                "${session.exercises.sumOf { it.sets.size }} sets"
                            session.note.isNotBlank() -> session.note
                            else -> ""
                        },
                        color = TextMeta,
                        style = mono(10.5f),
                    )
                }
            }
        }

        QuietAction(
            "Apply this plan",
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 14.dp, end = 8.dp),
            onClick = onApply,
        )
    }
}
