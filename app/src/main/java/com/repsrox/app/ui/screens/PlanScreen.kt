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
import androidx.compose.material.icons.filled.Edit
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
import com.repsrox.app.data.DayStatus
import com.repsrox.app.data.PlannedSession
import com.repsrox.app.data.exportPlan
import com.repsrox.app.data.plural
import com.repsrox.app.data.rollingWeek
import com.repsrox.app.data.weekDates
import com.repsrox.app.data.weekStart
import com.repsrox.app.ui.BodyViewModel
import com.repsrox.app.ui.FuelViewModel
import com.repsrox.app.ui.PlanViewModel
import com.repsrox.app.ui.RepsRoxViewModel
import com.repsrox.app.ui.components.AccentAction
import com.repsrox.app.ui.components.Panel
import com.repsrox.app.ui.components.QuietAction
import com.repsrox.app.ui.components.Step
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
fun PlanScreen(
    viewModel: RepsRoxViewModel,
    planViewModel: PlanViewModel = viewModel(),
    bodyViewModel: BodyViewModel = viewModel(),
    fuelViewModel: FuelViewModel = viewModel(),
) {
    val sessions by planViewModel.sessions.collectAsState()
    val today = remember { LocalDate.now() }
    val weekStart = viewModel.weekStart
    val rollingPlan by planViewModel.rollingPlan.collectAsState()
    val rollingMeals by fuelViewModel.repeating.collectAsState()
    var exporting by remember { mutableStateOf(false) }
    var importing by remember { mutableStateOf(false) }
    var stopping by remember { mutableStateOf(false) }

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
        // Rest days count, so the header agrees with the rows under it and Today's ring.
        val training = thisWeek.size
        // A week nothing has been written into yet is the plan being printed. Say so,
        // so a week that fills itself does not look like one someone else planned.
        val fromPlan = thisWeek.isNotEmpty() && thisWeek.all { rollingWeek(it.id) != null }

        WeekHeader(
            weekStart = weekStart,
            today = today,
            // Week one is the week the plan starts in, so the number means something
            // rather than being a label carried over from the design.
            number = plan.minOfOrNull { it.date }?.let {
                ChronoUnit.WEEKS.between(it.weekStart(), weekStart) + 1
            },
            detail = when {
                thisWeek.isEmpty() -> "empty"
                fromPlan -> "$training ${plural(training, "session")} · from your plan"
                else -> "$training ${plural(training, "session")}"
            },
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
                        onEdit = { viewModel.goEdit(session) },
                        onDelete = { planViewModel.delete(session.id) },
                    )
                }
            }
        }

        // A week with something on every day has no room for another session.
        if (week.any { day -> thisWeek.none { it.date == day } }) {
            AccentAction(
                "New session",
                icon = Icons.Filled.Add,
                modifier = Modifier.fillMaxWidth(),
                // A session added from a week lands in that week, not on today.
                onClick = { viewModel.goBuild(if (today in week) today else weekStart) },
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            QuietAction("Export", modifier = Modifier.weight(1f), onClick = { exporting = true })
            QuietAction("Import", modifier = Modifier.weight(1f), onClick = { importing = true })
        }

        // A plan that repeats has to be stoppable, or a week can never be empty again.
        if (rollingPlan != null || rollingMeals) {
            QuietAction(
                "Stop following plan",
                modifier = Modifier.fillMaxWidth(),
                onClick = { stopping = true },
            )
        }
    }

    if (stopping) {
        ConfirmDialog(
            title = "Stop following plan",
            body = "Weeks ahead go back to empty, meals included. Weeks you have already " +
                "edited or finished keep what they hold.",
            confirm = "Stop",
            onDismiss = { stopping = false },
            onConfirm = {
                planViewModel.clearRollingPlan()
                fuelViewModel.clearRollingMeals()
                stopping = false
            },
        )
    }

    if (exporting) {
        val banked = viewModel.bankedSessions.collectAsState().value.orEmpty()
        val weighIns = bodyViewModel.weighIns.collectAsState().value.orEmpty()
        // The rolling week prints meals a year ahead; the document is this week's.
        val meals = fuelViewModel.meals.collectAsState().value.orEmpty()
            .filter { it.date.weekStart() == weekStart }
        ExportPlanDialog(
            markdown = exportPlan(
                plan = sessions.orEmpty(),
                weekStart = weekStart,
                meals = meals,
                banked = banked,
                weighIns = weighIns,
            ),
            onDismiss = { exporting = false },
        )
    }

    if (importing) {
        ImportPlanDialog(
            onDismiss = { importing = false },
            onApply = { parsed ->
                // Sessions and meals are stored apart, so a document lands in two places.
                planViewModel.applyImport(parsed)
                fuelViewModel.applyImport(parsed)
                importing = false
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

@Composable
private fun EmptyWeek() {
    Panel {
        Text("Nothing planned this week.", color = TextPrimary, style = oswald(20f, FontWeight.W500))
        Text(
            "Build a session, or import a plan.",
            color = TextSubtle,
            style = inter(11.5f, lineHeight = 1.5f),
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

/**
 * One row of the week. [status]/[meta] read off [session] itself now — there is
 * no separate log to correlate against, since a finished session's own figures
 * are what [PlannedSession.done] and its exercises already carry.
 */
@Composable
private fun SessionRow(
    session: PlannedSession,
    today: LocalDate,
    onOpen: () -> Unit,
    onEdit: () -> Unit,
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
        onClick = onOpen.takeUnless { status == DayStatus.REST },
        contentPadding = PaddingValues(12.dp),
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
                    session.date.dayOfWeek.getDisplayName(DateTextStyle.SHORT, Locale.US).uppercase(),
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
            if (status != DayStatus.REST) {
                // Drawn small to sit quietly in the row, with a finger-sized target.
                Box(
                    Modifier
                        .size(32.dp)
                        .clickable(onClick = onEdit),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Filled.Edit,
                        contentDescription = "Edit ${session.name}",
                        tint = TextDim,
                        modifier = Modifier.size(13.dp),
                    )
                }
            }
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
