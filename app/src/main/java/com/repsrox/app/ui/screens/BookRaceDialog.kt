package com.repsrox.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.repsrox.app.data.RACE_NAME_MAX_CHARS
import com.repsrox.app.data.Race
import com.repsrox.app.data.countdownTo
import com.repsrox.app.data.raceDateLabel
import com.repsrox.app.ui.components.AccentAction
import com.repsrox.app.ui.components.MonthCalendar
import com.repsrox.app.ui.components.Panel
import com.repsrox.app.ui.components.PlainTextField
import com.repsrox.app.ui.components.QuietAction
import com.repsrox.app.ui.components.SectionLabel
import com.repsrox.app.ui.theme.Accent
import com.repsrox.app.ui.theme.BorderAction
import com.repsrox.app.ui.theme.TextMeta
import com.repsrox.app.ui.theme.inter
import java.time.LocalDate

/**
 * The race being trained for, and the day it is run. Booking over a race that is
 * already on the calendar replaces it — there is only ever a next race.
 */
@Composable
fun BookRaceDialog(
    booked: Race?,
    onDismiss: () -> Unit,
    onSave: (Race) -> Unit,
    onClear: () -> Unit,
) {
    val today = remember { LocalDate.now() }
    var name by remember { mutableStateOf(booked?.name.orEmpty()) }
    // A race already booked opens on its own day, unless that day has been and
    // gone — the calendar should open where the picking starts.
    var date by remember {
        mutableStateOf(booked?.date?.takeIf { !it.isBefore(today) } ?: today)
    }

    // A race with no name is a date, and a date says nothing on the profile line.
    val canSave = name.isNotBlank()

    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }

    Dialog(
        onDismissRequest = onDismiss,
        // Same reason as the weigh-in dialog: the platform's own width is
        // narrower than the design's gutter and crowds the calendar.
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Panel(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            contentPadding = PaddingValues(16.dp),
        ) {
            SectionLabel(if (booked == null) "Book a race" else "Race day")

            PlainTextField(
                value = name,
                onValueChange = { name = it },
                placeholder = "HYROX London",
                maxChars = RACE_NAME_MAX_CHARS,
                modifier = Modifier
                    .padding(top = 14.dp)
                    .focusRequester(focus),
            )

            MonthCalendar(
                selected = date,
                onSelect = { date = it },
                today = today,
                // There is nothing to train for behind you.
                earliest = today,
                modifier = Modifier.padding(top = 10.dp),
            )

            Text(
                "${raceDateLabel(date, today)} · ${countdownTo(date, today)}",
                color = TextMeta,
                style = inter(11f),
                modifier = Modifier.padding(top = 10.dp),
            )

            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                QuietAction("Cancel", modifier = Modifier.weight(1f), onClick = onDismiss)
                AccentAction(
                    "Save",
                    modifier = Modifier.weight(1f),
                    // Nothing to save until the race has a name.
                    borderColor = if (canSave) Accent else BorderAction,
                    onClick = { if (canSave) onSave(Race(name.trim(), date)) },
                )
            }

            if (booked != null) {
                QuietAction(
                    "Remove race",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    onClick = onClear,
                )
            }
        }
    }
}
