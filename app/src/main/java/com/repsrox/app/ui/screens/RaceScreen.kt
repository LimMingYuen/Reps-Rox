package com.repsrox.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardDoubleArrowRight
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.repsrox.app.data.LEGS
import com.repsrox.app.data.formatDelta
import com.repsrox.app.data.formatHours
import com.repsrox.app.data.formatMinutes
import com.repsrox.app.data.legDelta
import com.repsrox.app.data.projectedFinish
import com.repsrox.app.data.runAverage
import com.repsrox.app.data.stationAverage
import com.repsrox.app.ui.RepsRoxViewModel
import com.repsrox.app.ui.components.AccentAction
import com.repsrox.app.ui.components.Panel
import com.repsrox.app.ui.components.RuledRow
import com.repsrox.app.ui.components.SectionLabel
import com.repsrox.app.ui.components.SegmentRing
import com.repsrox.app.ui.components.VerticalRule
import com.repsrox.app.ui.theme.Accent
import com.repsrox.app.ui.theme.AccentLine
import com.repsrox.app.ui.theme.AccentTint
import com.repsrox.app.ui.theme.TextDim
import com.repsrox.app.ui.theme.TextMeta
import com.repsrox.app.ui.theme.TextMuted
import com.repsrox.app.ui.theme.TextPrimary
import com.repsrox.app.ui.theme.TextSecondary
import com.repsrox.app.ui.theme.inter
import com.repsrox.app.ui.theme.mono
import com.repsrox.app.ui.theme.oswald

/**
 * The simulation, timed leg by leg. Advancing closes the leg being raced at
 * whatever the clock says it took, so every split and delta on this board is
 * measured rather than written in.
 *
 * The design's Roxzone total is gone: the roxzone is the walk between a station
 * and the next run, and the course here is a contiguous list of legs with no
 * transition to time. Station average takes its place — a figure the clock can
 * actually stand behind.
 */
@Composable
fun RaceScreen(viewModel: RepsRoxViewModel) {
    val closed = viewModel.legSeconds.toList()
    val leg = viewModel.leg
    val onStation = leg.isStation
    val stationNumber = viewModel.stationNumber

    Column(
        Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // On a station the ring shows it as in progress, not yet banked.
            SegmentRing(
                size = 112.dp,
                done = stationNumber - if (onStation) 1 else 0,
                strokeWidth = 11f,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "STN",
                        color = TextMeta,
                        style = oswald(12f, tracking = 0.08f),
                        textAlign = TextAlign.Center,
                    )
                    Text("$stationNumber", color = TextPrimary, style = oswald(26f))
                }
            }
            Column(Modifier.weight(1f)) {
                SectionLabel(
                    if (onStation) "On station" else "On the run",
                    color = Accent,
                )
                Text(
                    leg.name,
                    color = TextPrimary,
                    style = oswald(19f, FontWeight.W500, lineHeight = 1.15f, tracking = 0.02f),
                    modifier = Modifier.padding(top = 6.dp),
                )
                Text(
                    leg.target,
                    color = TextSecondary,
                    style = inter(11.5f),
                    modifier = Modifier.padding(top = 4.dp),
                )
                Text(
                    formatHours(viewModel.raceSeconds),
                    color = TextPrimary,
                    style = oswald(34f, lineHeight = 1f),
                    modifier = Modifier.padding(top = 10.dp),
                )
                Text(
                    "This leg ${formatMinutes(viewModel.currentLegSeconds)} " +
                        "· target ${formatMinutes(leg.targetSeconds)}",
                    color = TextMeta,
                    style = mono(11f),
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            AccentAction(
                // A sim yet to be started reads Start, not Resume: there is
                // nothing to resume until the clock has run.
                when {
                    viewModel.raceOn -> "Pause"
                    viewModel.raceSeconds == 0 -> "Start"
                    else -> "Resume"
                },
                modifier = Modifier.weight(1f),
                verticalPadding = 13.dp,
                onClick = viewModel::toggleRace,
            )
            AccentAction(
                if (onStation) "Next run" else "On station",
                icon = Icons.Filled.KeyboardDoubleArrowRight,
                modifier = Modifier.weight(1f),
                background = AccentTint,
                borderColor = AccentLine,
                verticalPadding = 13.dp,
                onClick = viewModel::nextLeg,
            )
        }

        Panel(contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                RaceStat("Station avg", stationAverage(closed)?.let(::formatMinutes) ?: "—")
                VerticalRule(height = 38.dp)
                RaceStat("Run avg", runAverage(closed)?.let(::formatMinutes) ?: "—")
                VerticalRule(height = 38.dp)
                RaceStat("Projected", formatHours(projectedFinish(viewModel.raceSeconds, closed)))
            }
        }

        Column {
            SectionLabel("Splits", modifier = Modifier.padding(bottom = 4.dp))
            LEGS.forEachIndexed { index, item ->
                val split = closed.getOrNull(index)
                val past = split != null
                val now = index == viewModel.legIndex && split == null
                val delta = split?.let { legDelta(index, it) }
                val ink = when {
                    now -> Accent
                    past -> TextPrimary
                    else -> TextDim
                }
                RuledRow(verticalPadding = 9.dp) {
                    Text(
                        item.tag,
                        color = when {
                            now -> Accent
                            past -> TextMuted
                            else -> TextDim
                        },
                        style = oswald(10f, tracking = 0.06f),
                        modifier = Modifier.width(38.dp),
                    )
                    Text(
                        item.name,
                        color = ink,
                        style = inter(12f, FontWeight.W500, lineHeight = 1.3f),
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        when {
                            split != null -> formatMinutes(split)
                            now && viewModel.raceSeconds > 0 -> "running"
                            else -> "—"
                        },
                        color = ink,
                        style = mono(12f, FontWeight.W500),
                    )
                    Text(
                        delta?.let(::formatDelta).orEmpty(),
                        // Under target reads as a win, so it takes the accent.
                        color = if (delta != null && delta < 0) Accent else TextMuted,
                        style = mono(10.5f),
                        textAlign = TextAlign.End,
                        modifier = Modifier.width(38.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun RaceStat(label: String, value: String) {
    Column {
        SectionLabel(label, tracking = 0.10f)
        Text(
            value,
            color = TextPrimary,
            style = oswald(18f, lineHeight = 1.2f),
            modifier = Modifier.padding(top = 5.dp),
        )
    }
}
