package com.repsrox.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.repsrox.app.ui.theme.Accent
import com.repsrox.app.ui.theme.AccentSet
import com.repsrox.app.ui.theme.BorderChip
import com.repsrox.app.ui.theme.SurfaceRaised
import com.repsrox.app.ui.theme.TextDim
import com.repsrox.app.ui.theme.TextFaint
import com.repsrox.app.ui.theme.TextPrimary
import com.repsrox.app.ui.theme.TextSubtle
import com.repsrox.app.ui.theme.inter
import com.repsrox.app.ui.theme.oswald
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.util.Locale
import java.time.format.TextStyle as DateTextStyle

/** Monday first, as a training week is read here. */
private val WEEK: List<DayOfWeek> = DayOfWeek.values().toList()

/** Always six, so paging a five-week month into a six-week one doesn't resize the dialog. */
private const val WEEKS_SHOWN = 6

/**
 * A month at a time, for picking a day that may be a long way off. [DateStepper]
 * covers a weigh-in you missed yesterday; a race is months out, and stepping to
 * it a day at a time is not picking a date, it's counting to one.
 */
@Composable
fun MonthCalendar(
    selected: LocalDate,
    onSelect: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
    today: LocalDate = LocalDate.now(),
    earliest: LocalDate? = null,
) {
    // The month on show is the calendar's own state: paging through it looks
    // ahead without moving the day that is picked.
    var month by remember { mutableStateOf(YearMonth.from(selected)) }
    val shape = RoundedCornerShape(6.dp)

    Column(
        modifier
            .fillMaxWidth()
            .background(SurfaceRaised, shape)
            .border(1.dp, BorderChip, shape)
            .padding(6.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Step(
                Icons.Filled.ChevronLeft,
                "Previous month",
                enabled = earliest == null || month > YearMonth.from(earliest),
            ) { month = month.minusMonths(1) }
            Text(
                "${month.month.getDisplayName(DateTextStyle.SHORT, Locale.US)} ${month.year}",
                color = TextPrimary,
                style = oswald(14f, FontWeight.W500),
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f),
            )
            Step(Icons.Filled.ChevronRight, "Next month") { month = month.plusMonths(1) }
        }

        Row(Modifier.fillMaxWidth().padding(top = 4.dp)) {
            WEEK.forEach { day ->
                Text(
                    day.getDisplayName(DateTextStyle.NARROW, Locale.US),
                    color = TextFaint,
                    style = inter(10f, lineHeight = 1f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
            }
        }

        // Monday is 1, so the first of the month sits that many cells in.
        val lead = month.atDay(1).dayOfWeek.value - 1
        repeat(WEEKS_SHOWN) { week ->
            Row(Modifier.fillMaxWidth().padding(top = 2.dp)) {
                repeat(WEEK.size) { column ->
                    val dayOfMonth = week * WEEK.size + column - lead + 1
                    val date = if (dayOfMonth in 1..month.lengthOfMonth()) {
                        month.atDay(dayOfMonth)
                    } else {
                        null
                    }
                    DayCell(
                        date = date,
                        selected = date != null && date == selected,
                        isToday = date != null && date == today,
                        enabled = date != null && (earliest == null || !date.isBefore(earliest)),
                        modifier = Modifier.weight(1f),
                        onClick = { date?.let(onSelect) },
                    )
                }
            }
        }
    }
}

/** One day, or the blank that stands in for a cell either side of the month. */
@Composable
private fun DayCell(
    date: LocalDate?,
    selected: Boolean,
    isToday: Boolean,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(5.dp)
    Box(
        modifier
            .height(30.dp)
            .then(
                if (selected) {
                    Modifier.background(AccentSet, shape).border(1.dp, Accent, shape)
                } else {
                    Modifier
                },
            )
            .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        if (date != null) {
            Text(
                date.dayOfMonth.toString(),
                color = when {
                    selected -> Accent
                    !enabled -> TextDim
                    isToday -> TextPrimary
                    else -> TextSubtle
                },
                style = inter(
                    12f,
                    if (selected || isToday) FontWeight.W600 else FontWeight.W400,
                    lineHeight = 1f,
                ),
            )
        }
    }
}
