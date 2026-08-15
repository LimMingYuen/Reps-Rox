package com.repsrox.app.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.repsrox.app.data.RACE_SLED_KG
import com.repsrox.app.data.WeighIn
import com.repsrox.app.data.WeightSummary
import com.repsrox.app.data.formatChange
import com.repsrox.app.data.formatKilos
import com.repsrox.app.data.shortName
import com.repsrox.app.data.summarise
import com.repsrox.app.ui.BodyViewModel
import com.repsrox.app.ui.components.AccentAction
import com.repsrox.app.ui.components.Panel
import com.repsrox.app.ui.components.RuledRow
import com.repsrox.app.ui.components.SectionLabel
import com.repsrox.app.ui.theme.Accent
import com.repsrox.app.ui.theme.AccentArea
import com.repsrox.app.ui.theme.TextDim
import com.repsrox.app.ui.theme.TextMeta
import com.repsrox.app.ui.theme.TextPrimary
import com.repsrox.app.ui.theme.TextSubtle
import com.repsrox.app.ui.theme.inter
import com.repsrox.app.ui.theme.mono
import com.repsrox.app.ui.theme.oswald
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.Locale

@Composable
fun BodyScreen(viewModel: BodyViewModel = viewModel()) {
    val entries by viewModel.weighIns.collectAsState()
    var logging by remember { mutableStateOf(false) }

    Column(
        Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        when {
            // Still reading the log off disk — hold the screen blank for the one
            // frame it takes rather than flashing the empty state.
            entries == null -> Unit
            entries!!.isEmpty() -> EmptyLog(onLog = { logging = true })
            else -> WeightLog(
                entries = entries!!,
                onLog = { logging = true },
                onDelete = viewModel::delete,
            )
        }
    }

    if (logging) {
        LogWeighInDialog(
            onDismiss = { logging = false },
            onSave = { date, kg ->
                viewModel.log(date, kg)
                logging = false
            },
        )
    }
}

@Composable
private fun ColumnScope.EmptyLog(onLog: () -> Unit) {
    Panel {
        Text(
            "No weigh-ins yet.",
            color = TextPrimary,
            style = oswald(20f, FontWeight.W500),
        )
        Text(
            "Log one each morning, before breakfast. The trend needs a fortnight " +
                "before it says anything the scales don't.",
            color = TextSubtle,
            style = inter(11.5f, lineHeight = 1.5f),
            modifier = Modifier.padding(top = 8.dp),
        )
    }
    AccentAction(
        "Log weigh-in",
        icon = Icons.Filled.Add,
        modifier = Modifier.fillMaxWidth(),
        onClick = onLog,
    )
}

@Composable
private fun ColumnScope.WeightLog(
    entries: List<WeighIn>,
    onLog: () -> Unit,
    onDelete: (LocalDate) -> Unit,
) {
    val summary = remember(entries) { summarise(entries) } ?: return

    Row(
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(formatKilos(summary.latest.kg), color = TextPrimary, style = oswald(34f, FontWeight.W500))
        Text(
            "kg · ${summary.latestLabel}",
            color = TextMeta,
            style = inter(12f, lineHeight = 1f),
            modifier = Modifier.padding(bottom = 3.dp),
        )
    }

    TrendPanel(entries, summary)

    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        MetricTile(
            "7d avg",
            summary.sevenDayAverage?.let(::formatKilos) ?: "—",
            Modifier.weight(1f),
        )
        // Sled work is carried by bodyweight, so the ratio moves with the log.
        MetricTile(
            "Rel. sled",
            String.format(Locale.US, "%.1f×", RACE_SLED_KG / summary.latest.kg),
            Modifier.weight(1f),
        )
    }

    AccentAction(
        "Log weigh-in",
        icon = Icons.Filled.Add,
        modifier = Modifier.fillMaxWidth(),
        onClick = onLog,
    )

    History(entries, onDelete)
}

@Composable
private fun TrendPanel(entries: List<WeighIn>, summary: WeightSummary) {
    Panel {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            SectionLabel(
                when (summary.trendWeeks) {
                    0L -> "This week"
                    1L -> "1 week"
                    else -> "${summary.trendWeeks} weeks"
                },
                tracking = 0.10f,
            )
            SectionLabel(summary.trendChange?.let(::formatChange) ?: "—", tracking = 0.10f)
        }

        if (entries.size < 2) {
            Text(
                "One more weigh-in and the trend starts drawing.",
                color = TextMeta,
                style = inter(11.5f, lineHeight = 1.5f),
                modifier = Modifier.padding(top = 10.dp),
            )
            return@Panel
        }

        WeightChart(
            entries,
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
            // The months the log actually spans, in order, as the design's axis.
            entries.map { it.date.month }.distinct().forEach { month ->
                Text(month.shortName(), color = TextDim, style = mono(9.5f))
            }
        }
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

/** The last handful of weigh-ins, newest first, each removable if it was fat-fingered. */
@Composable
private fun History(entries: List<WeighIn>, onDelete: (LocalDate) -> Unit) {
    val newestFirst = entries.asReversed()
    Column {
        SectionLabel("Recent", modifier = Modifier.padding(bottom = 4.dp))
        newestFirst.take(HISTORY_ROWS).forEachIndexed { index, entry ->
            // Each row is read against the weigh-in before it, which is the next
            // one along in this reversed view.
            val previous = newestFirst.getOrNull(index + 1)
            RuledRow(verticalPadding = 10.dp) {
                Text(
                    "${entry.date.dayOfMonth} ${entry.date.month.shortName()}",
                    color = TextPrimary,
                    style = inter(12.5f, FontWeight.W500, lineHeight = 1.3f),
                    modifier = Modifier.weight(1f),
                )
                Text(formatKilos(entry.kg), color = TextPrimary, style = oswald(13f))
                Text(
                    previous?.let { formatChange(entry.kg - it.kg) } ?: "—",
                    color = TextMeta,
                    style = mono(10.5f),
                    textAlign = TextAlign.End,
                    modifier = Modifier.width(58.dp),
                )
                // The cross is drawn small to sit quietly in the row, but it takes
                // a finger-sized target around it.
                Box(
                    Modifier
                        .size(32.dp)
                        .clickable { onDelete(entry.date) },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Outlined.Close,
                        contentDescription = "Delete weigh-in",
                        tint = TextDim,
                        modifier = Modifier.size(14.dp),
                    )
                }
            }
        }
    }
}

private const val HISTORY_ROWS = 8

/**
 * The log as a filled line. Unlike the design's fixed plot this fits the weights
 * it is given, and spaces them by date, so a gap in the log reads as a gap.
 */
@Composable
private fun WeightChart(entries: List<WeighIn>, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val width = size.width
        val height = size.height

        val weights = entries.map { it.kg }
        // Breathing room above and below, so the line never rides an edge.
        val padding = ((weights.max() - weights.min()) * 0.15f).coerceAtLeast(0.3f)
        val top = weights.max() + padding
        val span = (weights.max() - weights.min()) + padding * 2f

        val start = entries.first().date
        val days = ChronoUnit.DAYS.between(start, entries.last().date).toFloat()

        val points = entries.map { entry ->
            val x = if (days <= 0f) 0f else width * ChronoUnit.DAYS.between(start, entry.date) / days
            x to height * (top - entry.kg) / span
        }

        val line = Path().apply {
            points.forEachIndexed { i, (x, y) -> if (i == 0) moveTo(x, y) else lineTo(x, y) }
        }

        val area = Path().apply {
            moveTo(points.first().first, height)
            points.forEach { (x, y) -> lineTo(x, y) }
            lineTo(points.last().first, height)
            close()
        }

        drawPath(area, AccentArea)
        drawPath(line, Accent, style = Stroke(width = 2.5.dp.toPx(), join = StrokeJoin.Round))
    }
}
