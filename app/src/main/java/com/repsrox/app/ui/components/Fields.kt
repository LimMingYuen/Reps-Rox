package com.repsrox.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.repsrox.app.ui.theme.Accent
import com.repsrox.app.ui.theme.AccentSet
import com.repsrox.app.ui.theme.BorderChip
import com.repsrox.app.ui.theme.SurfaceRaised
import com.repsrox.app.ui.theme.TextDim
import com.repsrox.app.ui.theme.TextMeta
import com.repsrox.app.ui.theme.TextPrimary
import com.repsrox.app.ui.theme.TextSubtle
import com.repsrox.app.ui.theme.inter
import java.time.LocalDate
import java.util.Locale
import java.time.format.TextStyle as DateTextStyle

/**
 * The design's raised chip, used as the ground under every field. Material's own
 * text fields carry theming that fights this palette, so the fields below draw
 * themselves on this instead.
 */
@Composable
private fun FieldFrame(
    modifier: Modifier = Modifier,
    verticalAlignment: Alignment.Vertical = Alignment.CenterVertically,
    content: @Composable RowScope.() -> Unit,
) {
    val shape = RoundedCornerShape(6.dp)
    Row(
        modifier
            .fillMaxWidth()
            .background(SurfaceRaised, shape)
            .border(1.dp, BorderChip, shape)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = verticalAlignment,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        content = content,
    )
}

/** A number, with the unit it is counted in sitting quietly against it. */
@Composable
fun NumberField(
    value: String,
    onValueChange: (String) -> Unit,
    suffix: String,
    placeholder: String,
    textStyle: TextStyle,
    modifier: Modifier = Modifier,
    decimal: Boolean = true,
) {
    FieldFrame(modifier, verticalAlignment = Alignment.Bottom) {
        Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
            if (value.isEmpty()) {
                Text(placeholder, color = TextDim, style = textStyle)
            }
            BasicTextField(
                value = value,
                // Keyboards vary in what they offer for decimals, so the field
                // takes digits and one separator and rejects the rest outright.
                onValueChange = { new -> if (new.isNumericInput(decimal)) onValueChange(new) },
                textStyle = textStyle.copy(color = TextPrimary),
                singleLine = true,
                cursorBrush = SolidColor(Accent),
                keyboardOptions = KeyboardOptions(
                    keyboardType = if (decimal) KeyboardType.Decimal else KeyboardType.Number,
                ),
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

/** A single line of text — a session's name, or the line a run is described by. */
@Composable
fun PlainTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    textStyle: TextStyle = inter(13.5f, FontWeight.W500, lineHeight = 1.3f),
    maxChars: Int = 40,
) {
    FieldFrame(modifier) {
        Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
            if (value.isEmpty()) {
                Text(placeholder, color = TextDim, style = textStyle)
            }
            BasicTextField(
                value = value,
                onValueChange = { new -> if (new.length <= maxChars) onValueChange(new) },
                textStyle = textStyle.copy(color = TextPrimary),
                singleLine = true,
                cursorBrush = SolidColor(Accent),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/**
 * Steps a date a day at a time, within whatever window the caller allows — the
 * weigh-in log never steps past today, the plan runs forward from it.
 */
@Composable
fun DateStepper(
    date: LocalDate,
    onChange: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
    today: LocalDate = LocalDate.now(),
    earliest: LocalDate? = null,
    latest: LocalDate? = null,
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
        Step(
            Icons.Filled.ChevronLeft,
            "Previous day",
            enabled = earliest == null || date > earliest,
        ) { onChange(date.minusDays(1)) }
        Text(
            date.stepperLabel(today),
            color = TextPrimary,
            style = inter(12.5f, FontWeight.W500, lineHeight = 1f),
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f),
        )
        Step(
            Icons.Filled.ChevronRight,
            "Next day",
            enabled = latest == null || date < latest,
        ) { onChange(date.plusDays(1)) }
    }
}

private fun LocalDate.stepperLabel(today: LocalDate): String = when (this) {
    today -> "Today"
    today.minusDays(1) -> "Yesterday"
    today.plusDays(1) -> "Tomorrow"
    else -> "${dayOfWeek.getDisplayName(DateTextStyle.SHORT, Locale.US)} " +
        "$dayOfMonth ${month.getDisplayName(DateTextStyle.SHORT, Locale.US)}"
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

/** One of a small set of choices — the kind a session is. */
@Composable
fun ChoiceChip(
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(6.dp)
    val ink = if (selected) Accent else TextSubtle
    Row(
        modifier
            .background(if (selected) AccentSet else SurfaceRaised, shape)
            .border(1.dp, if (selected) Accent else BorderChip, shape)
            .clickable(onClick = onClick)
            .padding(vertical = 9.dp, horizontal = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(5.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = ink, modifier = Modifier.size(13.dp))
        }
        Text(
            label.uppercase(),
            color = ink,
            style = inter(10.5f, FontWeight.W600, lineHeight = 1f, tracking = 0.05f),
        )
    }
}

/** Digits, and at most one separator when decimals are allowed. */
fun String.isNumericInput(decimal: Boolean = true): Boolean = length <= 6 &&
    all { it.isDigit() || (decimal && (it == '.' || it == ',')) } &&
    count { it == '.' || it == ',' } <= 1

/** Accepts either separator, since the keyboard's depends on the device's locale. */
fun String.toDecimal(): Float? = replace(',', '.').toFloatOrNull()
