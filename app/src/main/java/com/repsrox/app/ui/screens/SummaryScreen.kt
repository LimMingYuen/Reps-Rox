package com.repsrox.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.repsrox.app.data.LIVE_ELAPSED
import com.repsrox.app.data.formatTonnes
import com.repsrox.app.data.logLine
import com.repsrox.app.ui.RepsRoxViewModel
import com.repsrox.app.ui.Screen
import com.repsrox.app.ui.formatMinutes
import com.repsrox.app.ui.components.Panel
import com.repsrox.app.ui.components.QuietAction
import com.repsrox.app.ui.components.RuledRow
import com.repsrox.app.ui.components.SectionLabel
import com.repsrox.app.ui.components.StatBlock
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
    val session = viewModel.activeSession
    val exercises = viewModel.activeExercises
    // Nothing times a session yet, so the duration stays the design's fixed clock;
    // everything countable is read off what was actually worked through.
    val volume = exercises.fold(0f) { total, exercise ->
        total + exercise.sets.fold(0f) { sum, set -> sum + set.reps * (set.kg.toFloatOrNull() ?: 0f) }
    }

    Column(
        Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Panel(
            borderColor = AccentLineFaint,
            contentPadding = PaddingValues(16.dp),
        ) {
            SectionLabel("Session banked", color = Accent)
            Text(
                session?.name ?: "Lower push + sled finisher",
                color = TextPrimary,
                style = oswald(22f, FontWeight.W500, lineHeight = 1.15f),
                modifier = Modifier.padding(top = 8.dp),
            )
            Row(
                Modifier.padding(top = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                StatBlock(formatMinutes(LIVE_ELAPSED), "duration", valueSize = 20f)
                if (volume > 0f) {
                    StatBlock(formatTonnes(volume), "volume", valueSize = 20f)
                }
                StatBlock("${viewModel.totalSetsDone}", "sets", valueSize = 20f)
            }
        }

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
                        "Back squat · 5 × 120 kg",
                        color = TextPrimary,
                        style = inter(13f, FontWeight.W500, lineHeight = 1.3f),
                    )
                    Text(
                        "Best five at this weight. Old best 5 × 115.",
                        color = TextSecondary,
                        style = inter(11f),
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
            }
        }

        Column {
            SectionLabel("What you did", modifier = Modifier.padding(bottom = 4.dp))
            exercises.forEach { exercise ->
                RuledRow(verticalPadding = 10.dp) {
                    Text(
                        exercise.name,
                        color = TextPrimary,
                        style = inter(12.5f, FontWeight.W500, lineHeight = 1.3f),
                        modifier = Modifier.weight(1f),
                    )
                    Text(exercise.logLine(), color = TextSecondary, style = mono(11.5f))
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
            onClick = { viewModel.go(Screen.Today) },
        )
    }
}
