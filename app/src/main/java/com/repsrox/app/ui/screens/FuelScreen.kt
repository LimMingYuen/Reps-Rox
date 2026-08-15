package com.repsrox.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.repsrox.app.data.MACROS
import com.repsrox.app.data.MEALS
import com.repsrox.app.ui.RepsRoxViewModel
import com.repsrox.app.ui.components.Meter
import com.repsrox.app.ui.components.Panel
import com.repsrox.app.ui.components.RuledRow
import com.repsrox.app.ui.components.SectionLabel
import com.repsrox.app.ui.theme.Accent
import com.repsrox.app.ui.theme.TextDim
import com.repsrox.app.ui.theme.TextFaint
import com.repsrox.app.ui.theme.TextMeta
import com.repsrox.app.ui.theme.TextMuted
import com.repsrox.app.ui.theme.TextPrimary
import com.repsrox.app.ui.theme.TextSecondary
import com.repsrox.app.ui.theme.TrackSoft
import com.repsrox.app.ui.theme.inter
import com.repsrox.app.ui.theme.mono
import com.repsrox.app.ui.theme.oswald

@Composable
fun FuelScreen(viewModel: RepsRoxViewModel) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Column {
            SectionLabel("Target for today")
            Row(
                Modifier.padding(top = 6.dp),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    "2,540 kcal",
                    color = TextPrimary,
                    style = oswald(26f, FontWeight.W500, lineHeight = 1.15f),
                )
                Text(
                    "+320 for the sled work",
                    color = Accent,
                    style = inter(11f, FontWeight.W500, lineHeight = 1f),
                    modifier = Modifier.padding(bottom = 3.dp),
                )
            }
        }

        Panel(verticalArrangement = Arrangement.spacedBy(13.dp)) {
            MACROS.forEach { macro ->
                Column {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            macro.label,
                            color = TextPrimary,
                            style = inter(12f, FontWeight.W500, lineHeight = 1f),
                        )
                        Text(macro.value, color = TextSecondary, style = mono(12f))
                    }
                    Meter(
                        fraction = macro.percent / 100f,
                        color = if (macro.accented) Accent else TextSecondary,
                        track = TrackSoft,
                        height = 6.dp,
                        modifier = Modifier.padding(top = 7.dp),
                    )
                }
            }
        }

        Column {
            SectionLabel("Check-ins", modifier = Modifier.padding(bottom = 6.dp))
            MEALS.forEach { meal ->
                val logged = viewModel.isMealLogged(meal.key)
                RuledRow(
                    onClick = { viewModel.toggleMeal(meal.key) },
                    verticalPadding = 12.dp,
                ) {
                    Icon(
                        if (logged) Icons.Filled.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
                        contentDescription = if (logged) "Logged" else "Not logged",
                        tint = if (logged) Accent else TextDim,
                        modifier = Modifier.size(19.dp),
                    )
                    Column(Modifier.weight(1f)) {
                        Text(
                            meal.name,
                            // Once it's banked the row recedes.
                            color = if (logged) TextMuted else TextPrimary,
                            style = inter(12.5f, FontWeight.W500, lineHeight = 1.3f),
                        )
                        Text(meal.meta, color = TextMeta, style = inter(10.5f))
                    }
                    Text(meal.kcal, color = TextSecondary, style = mono(11.5f, FontWeight.W500))
                }
            }
        }

        Text(
            "No food search here — the target moves with training load and you confirm " +
                "you hit it. Protein is the only macro with a hard floor.",
            color = TextFaint,
            style = inter(11f, lineHeight = 1.55f),
        )
    }
}
