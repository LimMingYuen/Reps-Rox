package com.repsrox.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.repsrox.app.data.DayStatus
import com.repsrox.app.data.PlannedDay
import com.repsrox.app.data.SessionKind
import com.repsrox.app.data.WEEK
import com.repsrox.app.data.day
import com.repsrox.app.data.formatVolume
import com.repsrox.app.data.totalSets
import com.repsrox.app.data.volumeKg
import com.repsrox.app.ui.RepsRoxViewModel
import com.repsrox.app.ui.Screen
import com.repsrox.app.ui.components.Panel
import com.repsrox.app.ui.components.VerticalRule
import com.repsrox.app.ui.components.icon
import com.repsrox.app.ui.formatMinutes
import com.repsrox.app.ui.theme.Accent
import com.repsrox.app.ui.theme.TextDim
import com.repsrox.app.ui.theme.TextMeta
import com.repsrox.app.ui.theme.TextMuted
import com.repsrox.app.ui.theme.TextPrimary
import com.repsrox.app.ui.theme.inter
import com.repsrox.app.ui.theme.mono
import com.repsrox.app.ui.theme.oswald
import java.time.LocalDate

@Composable
fun PlanScreen(viewModel: RepsRoxViewModel) {
    val banked = viewModel.bankedSessions.collectAsState().value
    // Recomputed on every pass rather than remembered, so the day it compares
    // against is still right after the app has been open across midnight.
    val todaysSession = banked?.firstOrNull { it.day() == LocalDate.now() }

    Column(
        Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                "Week 3",
                color = TextPrimary,
                style = oswald(24f, FontWeight.W500, lineHeight = 1.15f, tracking = 0.02f),
            )
            Text("build · 5 sessions", color = TextMeta, style = mono(11f))
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            WEEK.forEach { day ->
                // Today is the only row the log can speak to: the week itself is
                // still fixed sample content, with no real dates to match against.
                val session = if (day.status == DayStatus.TODAY) todaysSession else null
                DayRow(
                    day = day,
                    status = if (session != null) DayStatus.DONE else day.status,
                    meta = session?.let {
                        "Done · ${formatMinutes(it.seconds)} · " +
                            "${formatVolume(it.volumeKg)} · ${it.totalSets} sets"
                    } ?: day.meta,
                    onClick = when {
                        session != null -> ({ viewModel.openSession(session) })
                        // Nothing in the log stands behind these, so there is no
                        // summary to open — a rest day has never been tappable either.
                        day.status == DayStatus.DONE || day.status == DayStatus.REST -> null
                        else -> ({ viewModel.go(day.destination()) })
                    },
                )
            }
        }
    }
}

/**
 * [status] and [meta] are passed in rather than read off [day], because a day the
 * log has a session for reads as done on that session's real figures.
 */
@Composable
private fun DayRow(
    day: PlannedDay,
    status: DayStatus,
    meta: String,
    onClick: (() -> Unit)?,
) {
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
        onClick = onClick,
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
                    day.day,
                    color = dayColor,
                    style = oswald(13f, tracking = 0.06f),
                    textAlign = TextAlign.Center,
                )
                Text(day.date, color = TextDim, style = inter(10f))
            }
            VerticalRule()
            Column(Modifier.weight(1f)) {
                Text(
                    day.name,
                    color = nameColor,
                    style = inter(13f, FontWeight.W500, lineHeight = 1.3f),
                )
                Text(
                    meta,
                    color = TextMeta,
                    style = inter(10.5f),
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
            Icon(
                status.icon(day.kind),
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

/**
 * The design reaches the run and race screens from its canvas tab strip, which a
 * real app doesn't have — so the week is where a session is opened from, each row
 * going to the tracker its kind needs. Rows already banked open their summary
 * instead, which [PlanScreen] handles because only it knows the log.
 */
private fun PlannedDay.destination(): Screen = when (kind) {
    SessionKind.RUN -> Screen.Run
    SessionKind.RACE -> Screen.Race
    else -> Screen.Live
}
