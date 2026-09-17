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
import com.repsrox.app.data.LEG_DELTA
import com.repsrox.app.data.LEG_TIMES
import com.repsrox.app.data.RACE_PROJECTED
import com.repsrox.app.data.RACE_RUN_AVG
import com.repsrox.app.data.ROXZONE_TOTAL
import com.repsrox.app.ui.RepsRoxViewModel
import com.repsrox.app.ui.components.AccentAction
import com.repsrox.app.ui.components.Panel
import com.repsrox.app.ui.components.RuledRow
import com.repsrox.app.ui.components.SectionLabel
import com.repsrox.app.ui.components.SegmentRing
import com.repsrox.app.ui.components.VerticalRule
import com.repsrox.app.ui.formatHours
import com.repsrox.app.ui.formatMinutes
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

@Composable
fun RaceScreen(viewModel: RepsRoxViewModel) {
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
                    if (onStation) "On station" else "Roxzone → run",
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
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            AccentAction(
                when {
                    viewModel.raceOn -> "Pause"
                    viewModel.raceSeconds > 0 -> "Resume"
                    else -> "Start race"
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
                RaceStat("Roxzone", ROXZONE_TOTAL)
                VerticalRule(height = 38.dp)
                RaceStat("Run avg", RACE_RUN_AVG)
                VerticalRule(height = 38.dp)
                RaceStat("Projected", RACE_PROJECTED)
            }
        }

        Column {
            SectionLabel("Splits", modifier = Modifier.padding(bottom = 4.dp))
            LEGS.forEachIndexed { index, item ->
                val past = index < viewModel.legIndex
                val now = index == viewModel.legIndex
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
                            past -> formatMinutes(LEG_TIMES[index])
                            now -> if (viewModel.raceOn) "running" else "—"
                            else -> "—"
                        },
                        color = ink,
                        style = mono(12f, FontWeight.W500),
                    )
                    Text(
                        if (past) LEG_DELTA[index] else "",
                        // Under target reads as a win, so it takes the accent.
                        color = if (past && LEG_DELTA[index].startsWith("−")) Accent else TextMuted,
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
