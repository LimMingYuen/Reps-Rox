package com.repsrox.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.repsrox.app.data.weekStart
import com.repsrox.app.ui.components.AccentAction
import com.repsrox.app.ui.components.Panel
import com.repsrox.app.ui.components.QuietAction
import com.repsrox.app.ui.components.SectionLabel
import com.repsrox.app.ui.theme.Accent
import com.repsrox.app.ui.theme.BorderAction
import com.repsrox.app.ui.theme.TextFaint
import com.repsrox.app.ui.theme.inter
import java.time.LocalDate
import java.util.Locale
import java.time.format.TextStyle as DateTextStyle

/** A plain yes or no, for a change worth pausing over that asks for nothing else. */
@Composable
fun ConfirmDialog(
    title: String,
    body: String,
    confirm: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    dismiss: String = "Cancel",
) {
    DialogPanel(onDismiss) {
        SectionLabel(title)
        Text(
            body,
            color = TextFaint,
            style = inter(10.5f, lineHeight = 1.5f),
            modifier = Modifier.padding(top = 10.dp),
        )
        Actions(confirm = confirm, enabled = true, onDismiss = onDismiss, onConfirm = onConfirm, dismiss = dismiss)
    }
}

@Composable
private fun DialogPanel(onDismiss: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    Dialog(
        onDismissRequest = onDismiss,
        // The design's cards run full-bleed to a 16dp gutter; the platform's own
        // dialog width is narrower than that and crowds the fields.
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Panel(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            contentPadding = PaddingValues(16.dp),
            content = content,
        )
    }
}

@Composable
private fun Actions(
    confirm: String,
    enabled: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    dismiss: String = "Cancel",
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(top = 18.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        QuietAction(dismiss, modifier = Modifier.weight(1f), onClick = onDismiss)
        AccentAction(
            confirm,
            modifier = Modifier.weight(1f),
            borderColor = if (enabled) Accent else BorderAction,
            onClick = { if (enabled) onConfirm() },
        )
    }
}

internal fun LocalDate.weekLabel(today: LocalDate): String = when (this) {
    today.weekStart() -> "This week"
    today.weekStart().plusWeeks(1) -> "Next week"
    today.weekStart().minusWeeks(1) -> "Last week"
    else -> "Week of $dayOfMonth ${month.getDisplayName(DateTextStyle.SHORT, Locale.US)}"
}
