package com.repsrox.app.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.repsrox.app.data.WEIGHT_CHART_TOP
import com.repsrox.app.data.WEIGHT_MONTHS
import com.repsrox.app.data.WEIGHT_SERIES
import com.repsrox.app.ui.components.AccentAction
import com.repsrox.app.ui.components.Panel
import com.repsrox.app.ui.components.SectionLabel
import com.repsrox.app.ui.theme.Accent
import com.repsrox.app.ui.theme.AccentArea
import com.repsrox.app.ui.theme.TextDim
import com.repsrox.app.ui.theme.TextMeta
import com.repsrox.app.ui.theme.TextPrimary
import com.repsrox.app.ui.theme.inter
import com.repsrox.app.ui.theme.mono
import com.repsrox.app.ui.theme.oswald

@Composable
fun BodyScreen() {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text("81.4", color = TextPrimary, style = oswald(34f, FontWeight.W500))
            Text(
                "kg · this morning",
                color = TextMeta,
                style = inter(12f, lineHeight = 1f),
                modifier = Modifier.padding(bottom = 3.dp),
            )
        }

        Panel {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                SectionLabel("12 weeks", tracking = 0.10f)
                SectionLabel("−2.8 kg", tracking = 0.10f)
            }
            WeightChart(
                Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .padding(top = 10.dp),
            )
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, start = 2.dp, end = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                WEIGHT_MONTHS.forEach { month ->
                    Text(month, color = TextDim, style = mono(9.5f))
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MetricTile("7d avg", "81.7", Modifier.weight(1f))
            MetricTile("Waist", "83 cm", Modifier.weight(1f))
            MetricTile("Rel. sled", "1.9×", Modifier.weight(1f))
        }

        AccentAction(
            "Log weigh-in",
            icon = Icons.Filled.Add,
            modifier = Modifier.fillMaxWidth(),
            onClick = { /* Logging a weigh-in needs a store; the shell stops here. */ },
        )
    }
}

@Composable
private fun MetricTile(label: String, value: String, modifier: Modifier = Modifier) {
    Panel(modifier = modifier, contentPadding = PaddingValues(12.dp)) {
        SectionLabel(label, tracking = 0.10f)
        Text(
            value,
            color = TextPrimary,
            style = oswald(20f, lineHeight = 1.2f),
            modifier = Modifier.padding(top = 6.dp),
        )
    }
}

/**
 * Twelve weigh-ins as a filled line. The design pins the top of the plot to
 * [WEIGHT_CHART_TOP] and stretches horizontally, so this does the same rather
 * than fitting the series' own range.
 */
@Composable
private fun WeightChart(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val width = size.width
        val height = size.height
        val stepX = width / (WEIGHT_SERIES.size - 1)
        val scaleY = height / CHART_VIEW_HEIGHT

        val points = WEIGHT_SERIES.mapIndexed { index, kg ->
            index * stepX to (WEIGHT_CHART_TOP - kg) * CHART_UNITS_PER_KG * scaleY
        }

        val line = Path().apply {
            points.forEachIndexed { i, (x, y) -> if (i == 0) moveTo(x, y) else lineTo(x, y) }
        }

        val area = Path().apply {
            moveTo(0f, height)
            points.forEach { (x, y) -> lineTo(x, y) }
            lineTo(width, height)
            close()
        }

        drawPath(area, AccentArea)
        drawPath(line, Accent, style = Stroke(width = 2.5f * scaleY, join = StrokeJoin.Round))
    }
}

/** The design's SVG viewBox height, and the px it gives each kilogram. */
private const val CHART_VIEW_HEIGHT = 120f
private const val CHART_UNITS_PER_KG = 34f
