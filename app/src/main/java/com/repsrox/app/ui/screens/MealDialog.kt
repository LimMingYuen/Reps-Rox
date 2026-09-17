package com.repsrox.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import com.repsrox.app.data.KCAL_RANGE
import com.repsrox.app.data.MACRO_RANGE_G
import com.repsrox.app.data.MEAL_DETAIL_MAX_CHARS
import com.repsrox.app.data.MEAL_NAME_MAX_CHARS
import com.repsrox.app.data.Meal
import com.repsrox.app.data.sanitise
import com.repsrox.app.ui.components.AccentAction
import com.repsrox.app.ui.components.NumberField
import com.repsrox.app.ui.components.Panel
import com.repsrox.app.ui.components.PlainTextField
import com.repsrox.app.ui.components.QuietAction
import com.repsrox.app.ui.components.SectionLabel
import com.repsrox.app.ui.newMeal
import com.repsrox.app.ui.theme.Accent
import com.repsrox.app.ui.theme.BorderAction
import com.repsrox.app.ui.theme.TextFaint
import com.repsrox.app.ui.theme.TextMeta
import com.repsrox.app.ui.theme.inter
import com.repsrox.app.ui.theme.oswald
import java.time.LocalDate

/**
 * A meal and what it is worth. Figures are optional throughout — a meal you have
 * named but not costed is a real line on a day still being planned — so only the
 * name is held to. [initial] opens the dialog pre-filled to edit a meal already
 * on the day; [onDelete] is offered only alongside one. Editing never disturbs
 * the check-in: whether you ate it is not something this dialog decides.
 */
@Composable
fun MealDialog(
    date: LocalDate,
    initial: Meal? = null,
    onDismiss: () -> Unit,
    onSave: (Meal) -> Unit,
    onDelete: (() -> Unit)? = null,
) {
    var name by remember { mutableStateOf(initial?.name.orEmpty()) }
    var detail by remember { mutableStateOf(initial?.detail.orEmpty()) }
    var kcal by remember { mutableStateOf(initial?.kcal?.takeIf { it > 0 }?.toString().orEmpty()) }
    var protein by remember { mutableStateOf(initial?.proteinG?.takeIf { it > 0 }?.toString().orEmpty()) }
    var carbs by remember { mutableStateOf(initial?.carbsG?.takeIf { it > 0 }?.toString().orEmpty()) }

    // A blank figure is zero, which is a real answer; a typed one has to read.
    val kcalValue = kcal.figure()
    val proteinValue = protein.figure()
    val carbsValue = carbs.figure()

    val cleanName = sanitise(name)
    val canSave = cleanName.isNotEmpty() &&
        kcalValue != null && kcalValue in KCAL_RANGE &&
        proteinValue != null && proteinValue in MACRO_RANGE_G &&
        carbsValue != null && carbsValue in MACRO_RANGE_G

    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { if (initial == null) focus.requestFocus() }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Panel(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            contentPadding = PaddingValues(16.dp),
        ) {
            SectionLabel(if (initial == null) "Add meal" else "Edit meal")

            PlainTextField(
                value = name,
                onValueChange = { name = it },
                placeholder = "Breakfast",
                maxChars = MEAL_NAME_MAX_CHARS,
                modifier = Modifier
                    .padding(top = 14.dp)
                    .focusRequester(focus),
            )
            PlainTextField(
                value = detail,
                onValueChange = { detail = it },
                placeholder = "Oats, whey, banana",
                maxChars = MEAL_DETAIL_MAX_CHARS,
                textStyle = inter(12.5f, lineHeight = 1.3f),
                modifier = Modifier.padding(top = 10.dp),
            )

            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Figure("Calories", kcal, { kcal = it }, "kcal", Modifier.weight(1.2f))
                Figure("Protein", protein, { protein = it }, "g", Modifier.weight(1f))
                Figure("Carbs", carbs, { carbs = it }, "g", Modifier.weight(1f))
            }

            Text(
                "Leave a figure empty if you're not counting it.",
                color = TextFaint,
                style = inter(10.5f),
                modifier = Modifier.padding(top = 8.dp),
            )
            if (kcal.isNotBlank() && (kcalValue == null || kcalValue !in KCAL_RANGE)) {
                MealHint("Up to ${KCAL_RANGE.last} kcal")
            }
            if (protein.isNotBlank() && (proteinValue == null || proteinValue !in MACRO_RANGE_G)) {
                MealHint("Up to ${MACRO_RANGE_G.last} g protein")
            }
            if (carbs.isNotBlank() && (carbsValue == null || carbsValue !in MACRO_RANGE_G)) {
                MealHint("Up to ${MACRO_RANGE_G.last} g carbs")
            }

            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                QuietAction("Cancel", modifier = Modifier.weight(1f), onClick = onDismiss)
                AccentAction(
                    if (initial == null) "Add" else "Save",
                    modifier = Modifier.weight(1f),
                    borderColor = if (canSave) Accent else BorderAction,
                    onClick = {
                        if (canSave) {
                            val cleanDetail = sanitise(detail)
                            onSave(
                                initial?.copy(
                                    name = cleanName,
                                    detail = cleanDetail,
                                    kcal = kcalValue!!,
                                    proteinG = proteinValue!!,
                                    carbsG = carbsValue!!,
                                ) ?: newMeal(
                                    date = date,
                                    name = cleanName,
                                    detail = cleanDetail,
                                    kcal = kcalValue!!,
                                    proteinG = proteinValue!!,
                                    carbsG = carbsValue!!,
                                ),
                            )
                        }
                    },
                )
            }

            if (onDelete != null) {
                QuietAction(
                    "Remove from the day",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    onClick = onDelete,
                )
            }
        }
    }
}

@Composable
private fun Figure(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    suffix: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        SectionLabel(label, tracking = 0.10f, modifier = Modifier.padding(bottom = 5.dp))
        NumberField(
            value = value,
            onValueChange = onValueChange,
            suffix = suffix,
            placeholder = "—",
            textStyle = oswald(18f, FontWeight.W500),
            decimal = false,
        )
    }
}

@Composable
private fun MealHint(text: String) {
    Text(text, color = TextMeta, style = inter(10.5f), modifier = Modifier.padding(top = 5.dp))
}

/** A blank figure is zero; anything else has to read as a whole one. */
private fun String.figure(): Int? = if (isBlank()) 0 else toIntOrNull()
