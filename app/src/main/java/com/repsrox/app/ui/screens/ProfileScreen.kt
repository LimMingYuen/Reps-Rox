package com.repsrox.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.SportsScore
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.repsrox.app.data.PR_LIFTS
import com.repsrox.app.data.PR_STATIONS
import com.repsrox.app.data.PersonalRecord
import com.repsrox.app.ui.components.Panel
import com.repsrox.app.ui.components.RuledRow
import com.repsrox.app.ui.components.SectionLabel
import com.repsrox.app.ui.theme.Accent
import com.repsrox.app.ui.theme.BorderChip
import com.repsrox.app.ui.theme.SurfaceRaised
import com.repsrox.app.ui.theme.TextFaint
import com.repsrox.app.ui.theme.TextMeta
import com.repsrox.app.ui.theme.TextPrimary
import com.repsrox.app.ui.theme.TextSecondary
import com.repsrox.app.ui.theme.TextSubtle
import com.repsrox.app.ui.theme.inter
import com.repsrox.app.ui.theme.mono
import com.repsrox.app.ui.theme.oswald

@Composable
fun ProfileScreen() {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(13.dp),
        ) {
            Box(
                Modifier
                    .size(52.dp)
                    .background(SurfaceRaised, CircleShape)
                    .border(1.dp, BorderChip, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text("ML", color = Accent, style = oswald(17f), textAlign = TextAlign.Center)
            }
            Column {
                Text(
                    "MING LIM",
                    color = TextPrimary,
                    style = oswald(17f, FontWeight.W500, lineHeight = 1.2f, tracking = 0.03f),
                )
                Text(
                    "Hybrid · 81.4 kg · no race booked",
                    color = TextMeta,
                    style = inter(11f),
                    modifier = Modifier.padding(top = 3.dp),
                )
            }
        }

        Panel(contentPadding = PaddingValues(13.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(
                    Icons.Outlined.SportsScore,
                    contentDescription = null,
                    tint = TextSecondary,
                    modifier = Modifier.size(20.dp),
                )
                Text(
                    "No race on the calendar. Sim results still feed your projected finish.",
                    color = TextSubtle,
                    style = inter(11.5f, lineHeight = 1.5f),
                    modifier = Modifier.weight(1f),
                )
                Text(
                    "Add".uppercase(),
                    color = Accent,
                    style = inter(11f, FontWeight.W500, lineHeight = 1f, tracking = 0.06f),
                )
            }
        }

        RecordList("Lifts", PR_LIFTS)
        RecordList("Station bests", PR_STATIONS)
    }
}

@Composable
private fun RecordList(title: String, records: List<PersonalRecord>) {
    Column {
        SectionLabel(title, modifier = Modifier.padding(bottom = 4.dp))
        records.forEach { record ->
            RuledRow(verticalPadding = 10.dp) {
                Text(
                    record.name,
                    color = TextPrimary,
                    style = inter(12.5f, FontWeight.W500, lineHeight = 1.3f),
                    modifier = Modifier.weight(1f),
                )
                Text(record.value, color = TextPrimary, style = oswald(13f))
                if (record.whenLabel != null) {
                    Text(
                        record.whenLabel,
                        color = TextFaint,
                        style = mono(10.5f),
                        textAlign = TextAlign.End,
                        modifier = Modifier.width(52.dp),
                    )
                }
            }
        }
    }
}
