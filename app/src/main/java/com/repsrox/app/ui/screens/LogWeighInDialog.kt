package com.repsrox.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.repsrox.app.data.WEIGHT_RANGE_KG
import com.repsrox.app.ui.components.AccentAction
import com.repsrox.app.ui.components.Panel
import com.repsrox.app.ui.components.QuietAction
import com.repsrox.app.ui.components.SectionLabel
import com.repsrox.app.ui.theme.Accent
import com.repsrox.app.ui.theme.BorderAction
import com.repsrox.app.ui.theme.BorderChip
import com.repsrox.app.ui.theme.SurfaceRaised
import com.repsrox.app.ui.theme.TextDim
import com.repsrox.app.ui.theme.TextMeta
import com.repsrox.app.ui.theme.TextPrimary
import com.repsrox.app.ui.theme.inter
import com.repsrox.app.ui.theme.oswald
import java.time.LocalDate
import java.util.Locale
import java.time.format.TextStyle as DateTextStyle

/**
 * A weight and the morning it was taken. Logging against a date already in the
 * log overwrites it, which is how a mistyped entry is fixed.
 */
@Composable
fun LogWeighInDialog(
    onDismiss: () -> Unit,
    onSave: (date: LocalDate, kg: Float) -> Unit,
) {
    val today = remember { LocalDate.now() }
    var date by remember { mutableStateOf(today) }
    var weight by remember { mutableStateOf("") }

    val kg = weight.toDecimal()
    val canSave = kg != null && kg in WEIGHT_RANGE_KG

    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }

    Dialog(
        onDismissRequest = onDismiss,
        // The design's cards run full-bleed to a 16dp gutter; the platform's own
        // dialog width is narrower than that and crowds the number field.
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Panel(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            contentPadding = PaddingValues(16.dp),
        ) {
            SectionLabel("Log weigh-in")

            DateStepper(
                date = date,
                today = today,
                onChange = { date = it },
                modifier = Modifier.padding(top = 14.dp),
            )

            NumberField(
                value = weight,
                onValueChange = { weight = it },
                suffix = "kg",
                placeholder = "0.0",
                textStyle = oswald(28f, FontWeight.W500),
                modifier = Modifier
                    .padding(top = 12.dp)
                    .focusRequester(focus),
            )
            if (weight.isNotBlank() && !canSave) {
                Hint("Between ${WEIGHT_RANGE_KG.start.toInt()} and ${WEIGHT_RANGE_KG.endInclusive.toInt()} kg")
            }

            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 18.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                QuietAction("Cancel", modifier = Modifier.weight(1f), onClick = onDismiss)
                AccentAction(
                    "Save",
                    modifier = Modifier.weight(1f),
                    // Nothing to save until the weight reads as a real one.
                    borderColor = if (canSave) Accent else BorderAction,
                    onClick = { if (canSave) onSave(date, kg!!) },
                )
            }
        }
    }
}

@Composable
private fun Hint(text: String) {
    Text(text, color = TextMeta, style = inter(10.5f), modifier = Modifier.padding(top = 6.dp))
}

/** Back-fills a missed morning. Never steps past today — there is nothing to log there. */
@Composable
private fun DateStepper(
    date: LocalDate,
    today: LocalDate,
    onChange: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(6.dp)
    Row(
        modifier
            .fillMaxWidth()
            .background(SurfaceRaised, shape)
            .border(1.dp, BorderChip, shape)
            .padding(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Step(Icons.Filled.ChevronLeft, "Previous day") { onChange(date.minusDays(1)) }
        Text(
            when (date) {
                today -> "Today"
                today.minusDays(1) -> "Yesterday"
                else -> "${date.dayOfWeek.getDisplayName(DateTextStyle.SHORT, Locale.US)} " +
                    "${date.dayOfMonth} ${date.month.getDisplayName(DateTextStyle.SHORT, Locale.US)}"
            },
            color = TextPrimary,
            style = inter(12.5f, FontWeight.W500, lineHeight = 1f),
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f),
        )
        Step(Icons.Filled.ChevronRight, "Next day", enabled = date < today) {
            onChange(date.plusDays(1))
        }
    }
}

@Composable
private fun Step(
    icon: ImageVector,
    description: String,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    Icon(
        icon,
        contentDescription = description,
        tint = if (enabled) TextPrimary else TextDim,
        modifier = Modifier
            .size(28.dp)
            .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(4.dp),
    )
}

/**
 * A bare numeric field on the design's raised chip. Material's text fields carry
 * their own theming, which fights this palette, so this draws its own ground.
 */
@Composable
private fun NumberField(
    value: String,
    onValueChange: (String) -> Unit,
    suffix: String,
    placeholder: String,
    textStyle: TextStyle,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(6.dp)
    Row(
        modifier
            .fillMaxWidth()
            .background(SurfaceRaised, shape)
            .border(1.dp, BorderChip, shape)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
            if (value.isEmpty()) {
                Text(placeholder, color = TextDim, style = textStyle)
            }
            BasicTextField(
                value = value,
                // Keyboards vary in what they offer for decimals, so the field
                // takes digits and one separator and rejects the rest outright.
                onValueChange = { new -> if (new.isNumeric()) onValueChange(new) },
                textStyle = textStyle.copy(color = TextPrimary),
                singleLine = true,
                cursorBrush = SolidColor(Accent),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        Text(
            suffix,
            color = TextMeta,
            style = inter(12f, lineHeight = 1f),
            modifier = Modifier.padding(bottom = 3.dp),
        )
    }
}

/** Digits with at most one separator — anything else never reaches the field. */
private fun String.isNumeric(): Boolean =
    length <= 6 && all { it.isDigit() || it == '.' || it == ',' } && count { it == '.' || it == ',' } <= 1

/** Accepts either separator, since the keyboard's depends on the device's locale. */
private fun String.toDecimal(): Float? = replace(',', '.').toFloatOrNull()
