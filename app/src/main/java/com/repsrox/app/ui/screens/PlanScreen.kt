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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.repsrox.app.data.DayStatus
import com.repsrox.app.data.PlannedDay
import com.repsrox.app.data.SessionKind
import com.repsrox.app.data.WEEK
import com.repsrox.app.ui.RepsRoxViewModel
import com.repsrox.app.ui.Screen
import com.repsrox.app.ui.components.Panel
import com.repsrox.app.ui.components.VerticalRule
import com.repsrox.app.ui.components.icon
import com.repsrox.app.ui.theme.Accent
import com.repsrox.app.ui.theme.TextDim
import com.repsrox.app.ui.theme.TextMeta
import com.repsrox.app.ui.theme.TextMuted
import com.repsrox.app.ui.theme.TextPrimary
import com.repsrox.app.ui.theme.inter
import com.repsrox.app.ui.theme.mono
import com.repsrox.app.ui.theme.oswald

@Composable
fun PlanScreen(viewModel: RepsRoxViewModel) {
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
                DayRow(day, onClick = { viewModel.go(day.destination()) })
            }
        }
    }
}

@Composable
private fun DayRow(day: PlannedDay, onClick: () -> Unit) {
    // Completed and rest days recede; today and what's still ahead stay at full ink.
    val dayColor = when (day.status) {
        DayStatus.DONE -> TextMuted
        DayStatus.REST -> TextDim
        else -> TextPrimary
    }
    val nameColor = if (day.status == DayStatus.REST) TextDim else dayColor
    val iconColor = when (day.status) {
        DayStatus.DONE, DayStatus.TODAY -> Accent
        else -> TextDim
    }

    Panel(
        onClick = if (day.status == DayStatus.REST) null else onClick,
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
                    day.meta,
                    color = TextMeta,
                    style = inter(10.5f),
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
            Icon(
                day.status.icon(day.kind),
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

/**
 * The design reaches the run and race screens from its canvas tab strip, which
 * a real app doesn't have — so the week is where you open a session from: what
 * you've done opens its summary, what's ahead opens the tracker it needs.
 */
private fun PlannedDay.destination(): Screen = when {
    status == DayStatus.DONE -> Screen.Summary
    kind == SessionKind.RUN -> Screen.Run
    kind == SessionKind.RACE -> Screen.Race
    else -> Screen.Live
}
