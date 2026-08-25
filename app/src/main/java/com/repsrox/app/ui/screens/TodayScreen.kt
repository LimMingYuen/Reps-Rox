package com.repsrox.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.repsrox.app.data.SESSIONS_DONE
import com.repsrox.app.data.SESSIONS_PLANNED
import com.repsrox.app.data.SessionKind
import com.repsrox.app.data.dayLabel
import com.repsrox.app.data.formatKilos
import com.repsrox.app.data.formatSigned
import com.repsrox.app.data.formatVolume
import com.repsrox.app.data.summarise
import com.repsrox.app.data.totalSets
import com.repsrox.app.data.volumeKg
import com.repsrox.app.ui.BodyViewModel
import com.repsrox.app.ui.RepsRoxViewModel
import com.repsrox.app.ui.Screen
import com.repsrox.app.ui.components.AccentAction
import com.repsrox.app.ui.components.Meter
import com.repsrox.app.ui.components.Panel
import com.repsrox.app.ui.components.RuledRow
import com.repsrox.app.ui.components.SectionLabel
import com.repsrox.app.ui.components.SegmentRing
import com.repsrox.app.ui.components.StatBlock
import com.repsrox.app.ui.components.icon
import com.repsrox.app.ui.formatMinutes
import com.repsrox.app.ui.theme.Accent
import com.repsrox.app.ui.theme.TextFaint
import com.repsrox.app.ui.theme.TextMeta
import com.repsrox.app.ui.theme.TextMuted
import com.repsrox.app.ui.theme.TextPrimary
import com.repsrox.app.ui.theme.TextSecondary
import com.repsrox.app.ui.theme.Track
import com.repsrox.app.ui.theme.inter
import com.repsrox.app.ui.theme.mono
import com.repsrox.app.ui.theme.oswald

@Composable
fun TodayScreen(viewModel: RepsRoxViewModel, bodyViewModel: BodyViewModel = viewModel()) {
    val weight = bodyViewModel.weighIns.collectAsState().value
        ?.let { entries -> remember(entries) { summarise(entries) } }

    Column(
        Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Column {
            SectionLabel("Saturday 15 August")
            Text(
                "Week 3 · Build",
                color = TextPrimary,
                style = oswald(26f, FontWeight.W500, lineHeight = 1.15f, tracking = 0.02f),
                modifier = Modifier.padding(top = 6.dp),
            )
        }

        TodaySessionCard(onStart = { viewModel.go(Screen.Live) })

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Tile(
                label = "Bodyweight",
                modifier = Modifier.weight(1f),
                onClick = { viewModel.go(Screen.Body) },
            ) {
                Text(
                    buildAnnotatedString {
                        append(weight?.let { formatKilos(it.latest.kg) } ?: "—")
                        withStyle(SpanStyle(fontSize = 12.sp, color = TextMeta)) { append(" kg") }
                    },
                    color = TextPrimary,
                    style = oswald(22f, lineHeight = 1.2f),
                )
                Text(
                    weight?.fortnightChange?.let { "${formatSigned(it)} in 14d" } ?: "log a weigh-in",
                    color = if (weight?.fortnightChange != null) Accent else TextMeta,
                    style = inter(11f, lineHeight = 1f),
                    modifier = Modifier.padding(top = 3.dp),
                )
            }
            Tile(label = "7-day load", modifier = Modifier.weight(1f)) {
                Text("612", color = TextPrimary, style = oswald(22f, lineHeight = 1.2f))
                Text(
                    "on plan",
                    color = TextMeta,
                    style = inter(11f, lineHeight = 1f),
                    modifier = Modifier.padding(top = 3.dp),
                )
            }
        }

        WeekRingCard(onSeeWeek = { viewModel.go(Screen.Plan) })

        FuelStrip(onClick = { viewModel.go(Screen.Fuel) })

        Column(Modifier.padding(top = 2.dp)) {
            SectionLabel("Last sessions", modifier = Modifier.padding(bottom = 9.dp))
            val banked = viewModel.bankedSessions.collectAsState().value
            when {
                banked == null -> Unit
                banked.isEmpty() -> Text(
                    "Nothing banked yet. Finish a session and it lands here.",
                    color = TextMeta,
                    style = inter(11.5f, lineHeight = 1.5f),
                )
                // Only strength sessions are banked, so they all read as one kind.
                else -> banked.take(3).forEach { session ->
                    RuledRow(onClick = { viewModel.openSession(session) }) {
                        Icon(
                            SessionKind.STRENGTH.icon(),
                            contentDescription = null,
                            tint = Accent,
                            modifier = Modifier.size(16.dp),
                        )
                        Column(Modifier.weight(1f)) {
                            Text(
                                session.name,
                                color = TextPrimary,
                                style = inter(12.5f, FontWeight.W500, lineHeight = 1.3f),
                            )
                            Text(
                                "${formatMinutes(session.seconds)} · " +
                                    "${formatVolume(session.volumeKg)} · ${session.totalSets} sets",
                                color = TextMeta,
                                style = inter(10.5f),
                            )
                        }
                        Text(session.dayLabel(), color = TextFaint, style = mono(11f))
                    }
                }
            }
        }
    }
}

@Composable
private fun TodaySessionCard(onStart: () -> Unit) {
    Panel(
        onClick = onStart,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Today · strength".uppercase(),
                color = Accent,
                style = inter(10f, FontWeight.W500, lineHeight = 1f, tracking = 0.12f),
            )
            Spacer(Modifier.weight(1f))
            Text("≈ 58 min", color = TextMeta, style = mono(11f))
        }
        Text(
            "Lower push + sled finisher",
            color = TextPrimary,
            style = oswald(21f, FontWeight.W500, lineHeight = 1.15f, tracking = 0.02f),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            StatBlock("5", "exercises")
            StatBlock("18", "sets")
            StatBlock("4.2t", "planned volume")
        }
        AccentAction(
            "Start session",
            icon = Icons.Filled.PlayArrow,
            verticalPadding = 10.dp,
            modifier = Modifier.fillMaxWidth(),
            onClick = onStart,
        )
    }
}

@Composable
private fun Tile(
    label: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    Panel(modifier = modifier, onClick = onClick, contentPadding = PaddingValues(12.dp)) {
        SectionLabel(label, tracking = 0.12f, modifier = Modifier.padding(bottom = 7.dp))
        content()
    }
}

@Composable
private fun WeekRingCard(onSeeWeek: () -> Unit) {
    Panel {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            SegmentRing(size = 74.dp, done = SESSIONS_DONE, strokeWidth = 9f) {
                Text("$SESSIONS_DONE/$SESSIONS_PLANNED", color = TextPrimary, style = oswald(17f))
            }
            Column(Modifier.weight(1f)) {
                Text(
                    "Week 3 sessions",
                    color = TextPrimary,
                    style = inter(14f, FontWeight.W500, lineHeight = 1.3f),
                )
                Text(
                    "Two strength, one run banked. Sled work and the long Z2 left.",
                    color = TextSecondary,
                    style = inter(11.5f, lineHeight = 1.5f),
                    modifier = Modifier.padding(top = 3.dp),
                )
                Text(
                    "See the week →".uppercase(),
                    color = Accent,
                    style = inter(11f, FontWeight.W500, lineHeight = 1f, tracking = 0.06f),
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .clickable(onClick = onSeeWeek),
                )
            }
        }
    }
}

@Composable
private fun FuelStrip(onClick: () -> Unit) {
    Panel(onClick = onClick, contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp)) {
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
            Column(Modifier.weight(1f)) {
                Text(
                    "Protein 148 / 165 g",
                    color = TextPrimary,
                    style = inter(12.5f, FontWeight.W500, lineHeight = 1.3f),
                )
                Meter(
                    fraction = 0.90f,
                    height = 4.dp,
                    track = Track,
                    modifier = Modifier.padding(top = 7.dp),
                )
            }
            Text("+320 kcal", color = TextMeta, style = mono(11f))
        }
    }
}
