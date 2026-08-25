package com.repsrox.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.repsrox.app.data.Session
import com.repsrox.app.data.detail
import com.repsrox.app.data.formatVolume
import com.repsrox.app.data.topSet
import com.repsrox.app.data.totalSets
import com.repsrox.app.data.volumeKg
import com.repsrox.app.ui.RepsRoxViewModel
import com.repsrox.app.ui.Screen
import com.repsrox.app.ui.components.AccentAction
import com.repsrox.app.ui.components.Panel
import com.repsrox.app.ui.components.QuietAction
import com.repsrox.app.ui.components.RuledRow
import com.repsrox.app.ui.components.SectionLabel
import com.repsrox.app.ui.components.StatBlock
import com.repsrox.app.ui.formatMinutes
import com.repsrox.app.ui.theme.Accent
import com.repsrox.app.ui.theme.AccentLineFaint
import com.repsrox.app.ui.theme.AccentLineSoft
import com.repsrox.app.ui.theme.AccentWash
import com.repsrox.app.ui.theme.TextMuted
import com.repsrox.app.ui.theme.TextPrimary
import com.repsrox.app.ui.theme.TextSecondary
import com.repsrox.app.ui.theme.TextSubtle
import com.repsrox.app.ui.theme.inter
import com.repsrox.app.ui.theme.mono
import com.repsrox.app.ui.theme.oswald

@Composable
fun SummaryScreen(viewModel: RepsRoxViewModel) {
    val sessions by viewModel.bankedSessions.collectAsState()

    Column(
        Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        when {
            // Still reading the log off disk — hold the screen blank for the one
            // frame it takes rather than flashing the empty state.
            sessions == null -> Unit
            sessions!!.isEmpty() -> NothingBanked(onStart = { viewModel.go(Screen.Live) })
            // A session picked from a list, or the newest when nothing was picked —
            // which is what finishing one leaves behind.
            else -> Banked(
                session = viewModel.viewedSession
                    ?.let { picked -> sessions!!.firstOrNull { it.finishedAt == picked } }
                    ?: sessions!!.first(),
                onDone = { viewModel.go(Screen.Today) },
            )
        }
    }
}

@Composable
private fun ColumnScope.NothingBanked(onStart: () -> Unit) {
    Panel {
        Text(
            "No session banked yet.",
            color = TextPrimary,
            style = oswald(20f, FontWeight.W500),
        )
        Text(
            "Work through a session and finish it, and what you actually lifted " +
                "lands here — duration, tonnage and every set you banked.",
            color = TextSubtle,
            style = inter(11.5f, lineHeight = 1.5f),
            modifier = Modifier.padding(top = 8.dp),
        )
    }
    AccentAction(
        "Start today's session",
        icon = Icons.Filled.PlayArrow,
        modifier = Modifier.fillMaxWidth(),
        onClick = onStart,
    )
}

@Composable
private fun ColumnScope.Banked(session: Session, onDone: () -> Unit) {
    Panel(
        borderColor = AccentLineFaint,
        contentPadding = PaddingValues(16.dp),
    ) {
        SectionLabel("Session banked", color = Accent)
        Text(
            session.name,
            color = TextPrimary,
            style = oswald(22f, FontWeight.W500, lineHeight = 1.15f),
            modifier = Modifier.padding(top = 8.dp),
        )
        Row(
            Modifier.padding(top = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            StatBlock(formatMinutes(session.seconds), "duration", valueSize = 20f)
            StatBlock(formatVolume(session.volumeKg), "volume", valueSize = 20f)
            StatBlock("${session.totalSets}", "sets", valueSize = 20f)
        }
    }

    // The design calls out a personal best here. Beating one needs history the
    // app doesn't keep yet, so this reports the session's own heaviest set —
    // true of every session, and no claim the log can't back.
    session.topSet?.let { top ->
        Panel(
            background = AccentWash,
            borderColor = AccentLineSoft,
            contentPadding = PaddingValues(13.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(
                    Icons.Filled.EmojiEvents,
                    contentDescription = null,
                    tint = Accent,
                    modifier = Modifier.size(22.dp),
                )
                Column {
                    Text(
                        "${top.exercise} · ${top.set.reps} × ${top.set.kg} kg",
                        color = TextPrimary,
                        style = inter(13f, FontWeight.W500, lineHeight = 1.3f),
                    )
                    Text(
                        "Heaviest set of the session.",
                        color = TextSecondary,
                        style = inter(11f),
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
            }
        }
    }

    Column {
        SectionLabel("What you did", modifier = Modifier.padding(bottom = 4.dp))
        session.exercises.forEach { exercise ->
            RuledRow(verticalPadding = 10.dp) {
                Text(
                    exercise.name,
                    color = TextPrimary,
                    style = inter(12.5f, FontWeight.W500, lineHeight = 1.3f),
                    modifier = Modifier.weight(1f),
                )
                Text(exercise.detail(), color = TextSecondary, style = mono(11.5f))
            }
        }
    }

    Panel(contentPadding = PaddingValues(13.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                Icons.Outlined.Restaurant,
                contentDescription = null,
                tint = TextMuted,
                modifier = Modifier.size(19.dp),
            )
            Text(
                "Today's target went up 320 kcal and protein to 165 g.",
                color = TextSubtle,
                style = inter(11.5f, lineHeight = 1.5f),
            )
        }
    }

    QuietAction(
        "Done",
        modifier = Modifier.fillMaxWidth(),
        onClick = onDone,
    )
}
