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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.repsrox.app.data.WEIGHT_RANGE_KG
import com.repsrox.app.ui.components.AccentAction
import com.repsrox.app.ui.components.DateStepper
import com.repsrox.app.ui.components.NumberField
import com.repsrox.app.ui.components.Panel
import com.repsrox.app.ui.components.QuietAction
import com.repsrox.app.ui.components.SectionLabel
import com.repsrox.app.ui.components.toDecimal
import com.repsrox.app.ui.theme.Accent
import com.repsrox.app.ui.theme.BorderAction
import com.repsrox.app.ui.theme.TextMeta
import com.repsrox.app.ui.theme.inter
import com.repsrox.app.ui.theme.oswald
import java.time.LocalDate

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

            // Back-fills a missed morning, but never steps past today — there is
            // nothing to log there.
            DateStepper(
                date = date,
                onChange = { date = it },
                today = today,
                latest = today,
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
                Text(
                    "Between ${WEIGHT_RANGE_KG.start.toInt()} and " +
                        "${WEIGHT_RANGE_KG.endInclusive.toInt()} kg",
                    color = TextMeta,
                    style = inter(10.5f),
                    modifier = Modifier.padding(top = 6.dp),
                )
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
