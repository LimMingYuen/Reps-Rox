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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.repsrox.app.data.DayStatus
import com.repsrox.app.data.PlannedSession
import com.repsrox.app.data.SessionKind
import com.repsrox.app.data.plural
import com.repsrox.app.data.weekDates
import com.repsrox.app.data.weekStart
import com.repsrox.app.ui.PlanViewModel
import com.repsrox.app.ui.RepsRoxViewModel
import com.repsrox.app.ui.Screen
import com.repsrox.app.ui.components.AccentAction
import com.repsrox.app.ui.components.Panel
import com.repsrox.app.ui.components.QuietAction
import com.repsrox.app.ui.components.VerticalRule
import com.repsrox.app.ui.components.icon
import com.repsrox.app.ui.theme.Accent
import com.repsrox.app.ui.theme.TextDim
import com.repsrox.app.ui.theme.TextMeta
import com.repsrox.app.ui.theme.TextMuted
import com.repsrox.app.ui.theme.TextPrimary
import com.repsrox.app.ui.theme.TextSubtle
import com.repsrox.app.ui.theme.inter
import com.repsrox.app.ui.theme.mono
import com.repsrox.app.ui.theme.oswald
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.Locale
import java.time.format.TextStyle as DateTextStyle

@Composable
fun PlanScreen(viewModel: RepsRoxViewModel, planViewModel: PlanViewModel = viewModel()) {
    val sessions by planViewModel.sessions.collectAsState()
    val today = remember { LocalDate.now() }
    val weekStart = viewModel.weekStart
    var saving by remember { mutableStateOf(false) }

    Column(
        Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Still reading the plan off disk — hold the screen blank for the one frame
        // it takes rather than flashing the empty state.
        val plan = sessions ?: return@Column

        val week = weekStart.weekDates().toSet()
        val thisWeek = plan.filter { it.date in week }
        val training = thisWeek.count { it.kind != SessionKind.REST }

        WeekHeader(
            weekStart = weekStart,
            today = today,
            // Week one is the week the plan starts in, so the number means something
            // rather than being a label carried over from the design.
            number = plan.minOfOrNull { it.date }?.let {
                ChronoUnit.WEEKS.between(it.weekStart(), weekStart) + 1
            },
            detail = if (thisWeek.isEmpty()) "empty" else "$training ${plural(training, "session")}",
            onStep = { viewModel.goToWeek(weekStart.plusWeeks(it)) },
        )

        if (thisWeek.isEmpty()) {
            EmptyWeek()
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                thisWeek.forEach { session ->
                    SessionRow(
                        session = session,
                        today = today,
                        onOpen = { viewModel.open(session) },
                        onDelete = { planViewModel.delete(session.id) },
                    )
                }
            }
        }

        AccentAction(
            "New session",
            icon = Icons.Filled.Add,
            modifier = Modifier.fillMaxWidth(),
            // A session added from a week lands in that week, not on today.
            onClick = { viewModel.goBuild(if (today in week) today else weekStart) },
        )

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            QuietAction(
                "Save as plan",
                modifier = Modifier.weight(1f),
                onClick = { if (thisWeek.isNotEmpty()) saving = true },
            )
            QuietAction(
                "My plans",
                modifier = Modifier.weight(1f),
                onClick = { viewModel.go(Screen.Plans) },
            )
        }
    }

    if (saving) {
        SavePlanDialog(
            onDismiss = { saving = false },
            onSave = { name ->
                planViewModel.saveWeekAsPlan(weekStart, name)
                saving = false
            },
        )
    }
}

@Composable
private fun WeekHeader(
    weekStart: LocalDate,
    today: LocalDate,
    number: Long?,
    detail: String,
    onStep: (Long) -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Step(Icons.Filled.ChevronLeft, "Previous week") { onStep(-1) }
        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                weekStart.weekLabel(today),
                color = TextPrimary,
                style = oswald(21f, FontWeight.W500, lineHeight = 1.15f, tracking = 0.02f),
                textAlign = TextAlign.Center,
            )
            Text(
                listOfNotNull(number?.takeIf { it >= 1 }?.let { "Week $it" }, detail)
                    .joinToString(" · "),
                color = TextMeta,
                style = mono(11f),
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        Step(Icons.Filled.ChevronRight, "Next week") { onStep(1) }
    }
}

/** The week's own name where it has one, and the Monday it runs from otherwise. */
private fun LocalDate.weekLabel(today: LocalDate): String = when (this) {
    today.weekStart() -> "This week"
    today.weekStart().plusWeeks(1) -> "Next week"
    today.weekStart().minusWeeks(1) -> "Last week"
    else -> "$dayOfMonth ${month.getDisplayName(DateTextStyle.SHORT, Locale.US)}"
}

@Composable
private fun Step(icon: ImageVector, description: String, onClick: () -> Unit) {
    Icon(
        icon,
        contentDescription = description,
        tint = TextSubtle,
        modifier = Modifier
            .size(34.dp)
            .clickable(onClick = onClick)
            .padding(6.dp),
    )
}

@Composable
private fun EmptyWeek() {
    Panel {
        Text("Nothing planned.", color = TextPrimary, style = oswald(20f, FontWeight.W500))
        Text(
            "Add sessions one at a time, or lay a saved plan down over this week " +
                "and the weeks after it.",
            color = TextSubtle,
            style = inter(11.5f, lineHeight = 1.5f),
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

@Composable
private fun SessionRow(
    session: PlannedSession,
    today: LocalDate,
    onOpen: () -> Unit,
    onDelete: () -> Unit,
) {
    val status = session.status(today)

    // Completed and rest days recede; today and what's still ahead stay at full ink.
    val dayColor = when (status) {
        DayStatus.DONE -> TextMuted
        DayStatus.REST -> TextDim
        else -> TextPrimary
    }
    val nameColor = if (status == DayStatus.REST) TextDim else dayColor
    val iconColor = when (status) {
        DayStatus.DONE, DayStatus.TODAY -> Accent
        else -> TextDim
    }

    Panel(
        onClick = if (status == DayStatus.REST) null else onOpen,
        contentPadding = PaddingValues(start = 12.dp, top = 12.dp, bottom = 12.dp, end = 4.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(
                Modifier.width(34.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    session.date.dayOfWeek
                        .getDisplayName(DateTextStyle.SHORT, Locale.US)
                        .uppercase(),
                    color = dayColor,
                    style = oswald(13f, tracking = 0.06f),
                    textAlign = TextAlign.Center,
                )
                Text("${session.date.dayOfMonth}", color = TextDim, style = inter(10f))
            }
            VerticalRule()
            Column(Modifier.weight(1f)) {
                Text(
                    session.name,
                    color = nameColor,
                    style = inter(13f, FontWeight.W500, lineHeight = 1.3f),
                )
                Text(
                    session.meta(today),
                    color = TextMeta,
                    style = inter(10.5f),
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
            Icon(
                status.icon(session.kind),
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(16.dp),
            )
            // Drawn small to sit quietly in the row, with a finger-sized target.
            Box(
                Modifier
                    .size(32.dp)
                    .clickable(onClick = onDelete),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Outlined.Close,
                    contentDescription = "Delete ${session.name}",
                    tint = TextDim,
                    modifier = Modifier.size(13.dp),
                )
            }
        }
    }
}
