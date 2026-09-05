package com.repsrox.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.repsrox.app.data.ZONES
import com.repsrox.app.data.ZONE_COLUMN_HEIGHT
import com.repsrox.app.data.formatMinutes
import com.repsrox.app.ui.RepsRoxViewModel
import com.repsrox.app.ui.Screen
import com.repsrox.app.ui.components.AccentAction
import com.repsrox.app.ui.components.Meter
import com.repsrox.app.ui.components.Panel
import com.repsrox.app.ui.components.QuietAction
import com.repsrox.app.ui.components.RuledRow
import com.repsrox.app.ui.components.SectionLabel
import com.repsrox.app.ui.components.StatBlock
import com.repsrox.app.ui.theme.Accent
import com.repsrox.app.ui.theme.AccentLine
import com.repsrox.app.ui.theme.AccentTint
import com.repsrox.app.ui.theme.AccentZ3
import com.repsrox.app.ui.theme.AccentZ4
import com.repsrox.app.ui.theme.AccentZ5
import com.repsrox.app.ui.theme.TextDim
import com.repsrox.app.ui.theme.TextPrimary
import com.repsrox.app.ui.theme.TextSecondary
import com.repsrox.app.ui.theme.TrackFaint
import com.repsrox.app.ui.theme.ZoneIdle
import com.repsrox.app.ui.theme.mono
import com.repsrox.app.ui.theme.oswald

/**
 * The run, timed kilometre by kilometre. Lap closes the one in progress at
 * whatever the clock says it took, so the splits and the average pace are
 * measured rather than written in.
 */
@Composable
fun RunScreen(viewModel: RepsRoxViewModel) {
    val splits = viewModel.runSplits.toList()
    // Bars read against the slowest kilometre, so the slowest one fills the row.
    val slowest = splits.maxOrNull() ?: 0
    Column(
        Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(top = 10.dp, bottom = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            SectionLabel("Zone 2 · easy 8 km")
            Text(
                formatMinutes(viewModel.runSeconds),
                color = TextPrimary,
                style = oswald(58f, tracking = -0.01f),
                modifier = Modifier.padding(top = 8.dp),
            )
            Row(
                Modifier.padding(top = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(28.dp),
            ) {
                StatBlock(viewModel.runKilometres, "km", valueSize = 20f)
                StatBlock(viewModel.runPace ?: "—", "/km avg", valueSize = 20f)
                StatBlock("148", "bpm", valueSize = 20f)
            }
        }

        HeartRateCard()

        Column {
            SectionLabel("Splits", modifier = Modifier.padding(bottom = 4.dp))
            splits.forEachIndexed { index, split ->
                RuledRow(verticalPadding = 10.dp) {
                    Text(
                        "${index + 1}",
                        color = TextSecondary,
                        style = oswald(11f),
                        modifier = Modifier.width(22.dp),
                    )
                    Meter(
                        fraction = if (slowest == 0) 0f else split.toFloat() / slowest,
                        color = Accent.copy(alpha = 0.8f),
                        track = TrackFaint,
                        height = 5.dp,
                        modifier = Modifier.weight(1f),
                    )
                    Text(formatMinutes(split), color = TextPrimary, style = mono(12f, FontWeight.W500))
                }
            }
            // The kilometre being run, shown as it fills rather than only once closed.
            if (viewModel.runSeconds > 0) {
                RuledRow(verticalPadding = 10.dp) {
                    Text(
                        "${splits.size + 1}",
                        color = TextDim,
                        style = oswald(11f),
                        modifier = Modifier.width(22.dp),
                    )
                    Meter(
                        fraction = 1f,
                        color = TrackFaint,
                        track = TrackFaint,
                        height = 5.dp,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        formatMinutes(viewModel.currentLapSeconds),
                        color = TextDim,
                        style = mono(12f, FontWeight.W500),
                    )
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            AccentAction(
                "Lap",
                icon = Icons.Filled.Flag,
                modifier = Modifier.weight(1f),
                background = AccentTint,
                borderColor = AccentLine,
                onClick = viewModel::lap,
            )
            AccentAction(
                // A run yet to be started reads Start, not Resume: there is
                // nothing to resume until the clock has run.
                when {
                    viewModel.runOn -> "Pause"
                    viewModel.runSeconds == 0 -> "Start"
                    else -> "Resume"
                },
                modifier = Modifier.weight(1f),
                onClick = viewModel::toggleRun,
            )
            QuietAction(
                "End",
                horizontalPadding = 18.dp,
                onClick = { viewModel.go(Screen.Summary) },
            )
        }
    }
}

@Composable
private fun HeartRateCard() {
    Panel(contentPadding = PaddingValues(13.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            SectionLabel("Heart-rate zones", tracking = 0.10f)
            SectionLabel("Z2 · 71%", tracking = 0.10f)
        }
        Row(
            Modifier
                .fillMaxWidth()
                .padding(top = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            ZONES.forEachIndexed { index, zone ->
                Column(
                    Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    // Bars are bottom-aligned in a fixed-height column.
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(ZONE_COLUMN_HEIGHT.dp),
                        contentAlignment = Alignment.BottomCenter,
                    ) {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .height(zone.height.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(zoneColor(index)),
                        )
                    }
                    Text(
                        zone.label,
                        color = TextDim,
                        style = mono(9f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 5.dp),
                    )
                }
            }
        }
    }
}

/** Time in zone reads as accent strength: Z2 solid, the rest falling away. */
private fun zoneColor(index: Int): Color = when (index) {
    0 -> ZoneIdle
    1 -> Accent
    2 -> AccentZ3
    3 -> AccentZ4
    else -> AccentZ5
}
