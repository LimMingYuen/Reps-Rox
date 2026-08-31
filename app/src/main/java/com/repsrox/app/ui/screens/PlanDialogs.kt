package com.repsrox.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.repsrox.app.data.NAME_MAX_CHARS
import com.repsrox.app.data.REPEAT_RANGE
import com.repsrox.app.data.WeekTemplate
import com.repsrox.app.data.plural
import com.repsrox.app.data.sanitise
import com.repsrox.app.data.weekStart
import com.repsrox.app.ui.components.AccentAction
import com.repsrox.app.ui.components.Panel
import com.repsrox.app.ui.components.PlainTextField
import com.repsrox.app.ui.components.QuietAction
import com.repsrox.app.ui.components.SectionLabel
import com.repsrox.app.ui.theme.Accent
import com.repsrox.app.ui.theme.BorderAction
import com.repsrox.app.ui.theme.TextDim
import com.repsrox.app.ui.theme.TextFaint
import com.repsrox.app.ui.theme.TextPrimary
import com.repsrox.app.ui.theme.TextSubtle
import com.repsrox.app.ui.theme.inter
import com.repsrox.app.ui.theme.oswald
import java.time.LocalDate
import java.util.Locale
import java.time.format.TextStyle as DateTextStyle

/** Names the week being kept, so it can be laid down again later. */
@Composable
fun SavePlanDialog(onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var name by remember { mutableStateOf("") }
    val clean = sanitise(name)

    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }

    DialogPanel(onDismiss) {
        SectionLabel("Save this week as a plan")

        PlainTextField(
            value = name,
            onValueChange = { name = it },
            placeholder = "Hyrox build week",
            maxChars = NAME_MAX_CHARS,
            modifier = Modifier
                .padding(top = 14.dp)
                .focusRequester(focus),
        )
        Text(
            "The week's sessions are kept by the day they fall on, with their " +
                "exercises. What you've already finished is not part of the shape.",
            color = TextFaint,
            style = inter(10.5f, lineHeight = 1.5f),
            modifier = Modifier.padding(top = 8.dp),
        )

        Actions(
            confirm = "Save",
            enabled = clean.isNotEmpty(),
            onDismiss = onDismiss,
            onConfirm = { onSave(clean) },
        )
    }
}

/** Chooses the week a saved plan lands on, and how many weeks it runs for. */
@Composable
fun ApplyPlanDialog(
    plan: WeekTemplate,
    weekStart: LocalDate,
    onDismiss: () -> Unit,
    onApply: (weekStart: LocalDate, weeks: Int) -> Unit,
) {
    val today = remember { LocalDate.now() }
    var start by remember { mutableStateOf(weekStart) }
    var weeks by remember { mutableIntStateOf(1) }

    DialogPanel(onDismiss) {
        SectionLabel("Apply ${plan.name}")

        Stepper(
            label = start.weekLabel(today),
            onBack = { start = start.minusWeeks(1) },
            onForward = { start = start.plusWeeks(1) },
            backEnabled = true,
            forwardEnabled = true,
            modifier = Modifier.padding(top = 14.dp),
        )

        Stepper(
            label = "$weeks ${plural(weeks, "week")}",
            onBack = { weeks = (weeks - 1).coerceAtLeast(REPEAT_RANGE.first) },
            onForward = { weeks = (weeks + 1).coerceAtMost(REPEAT_RANGE.last) },
            backEnabled = weeks > REPEAT_RANGE.first,
            forwardEnabled = weeks < REPEAT_RANGE.last,
            modifier = Modifier.padding(top = 8.dp),
        )

        Text(
            "Replaces what's planned on those weeks. Sessions you've already " +
                "finished are left alone.",
            color = TextFaint,
            style = inter(10.5f, lineHeight = 1.5f),
            modifier = Modifier.padding(top = 10.dp),
        )

        Actions(
            confirm = "Apply",
            enabled = true,
            onDismiss = onDismiss,
            onConfirm = { onApply(start, weeks) },
        )
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
private fun Stepper(
    label: String,
    onBack: () -> Unit,
    onForward: () -> Unit,
    backEnabled: Boolean,
    forwardEnabled: Boolean,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Arrow(Icons.Filled.ChevronLeft, "Back", backEnabled, onBack)
        Text(
            label,
            color = TextPrimary,
            style = oswald(18f, FontWeight.W500, lineHeight = 1.1f),
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f),
        )
        Arrow(Icons.Filled.ChevronRight, "Forward", forwardEnabled, onForward)
    }
}

@Composable
private fun Arrow(icon: ImageVector, description: String, enabled: Boolean, onClick: () -> Unit) {
    Icon(
        icon,
        contentDescription = description,
        tint = if (enabled) TextSubtle else TextDim,
        modifier = Modifier
            .size(34.dp)
            .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(6.dp),
    )
}

@Composable
private fun Actions(
    confirm: String,
    enabled: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(top = 18.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        QuietAction("Cancel", modifier = Modifier.weight(1f), onClick = onDismiss)
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
