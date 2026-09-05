package com.repsrox.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.repsrox.app.data.Meal
import com.repsrox.app.data.SessionKind
import com.repsrox.app.data.TRAINING_KCAL
import com.repsrox.app.data.formatKcal
import com.repsrox.app.data.summariseFuel
import com.repsrox.app.data.targetKcal
import com.repsrox.app.ui.FuelViewModel
import com.repsrox.app.ui.PlanViewModel
import com.repsrox.app.ui.components.AccentAction
import com.repsrox.app.ui.components.DateStepper
import com.repsrox.app.ui.components.Meter
import com.repsrox.app.ui.components.Panel
import com.repsrox.app.ui.components.RuledRow
import com.repsrox.app.ui.components.SectionLabel
import com.repsrox.app.ui.theme.Accent
import com.repsrox.app.ui.theme.TextDim
import com.repsrox.app.ui.theme.TextFaint
import com.repsrox.app.ui.theme.TextMeta
import com.repsrox.app.ui.theme.TextMuted
import com.repsrox.app.ui.theme.TextPrimary
import com.repsrox.app.ui.theme.TextSecondary
import com.repsrox.app.ui.theme.TextSubtle
import com.repsrox.app.ui.theme.TrackSoft
import com.repsrox.app.ui.theme.inter
import com.repsrox.app.ui.theme.mono
import com.repsrox.app.ui.theme.oswald
import java.time.LocalDate

/**
 * A day's eating: what it is aiming at, what has been banked against that, and
 * the meals themselves — which are yours to add, edit and remove. Only meals
 * checked in count towards the macros; the rest are still the plan.
 */
@Composable
fun FuelScreen(
    fuelViewModel: FuelViewModel = viewModel(),
    planViewModel: PlanViewModel = viewModel(),
) {
    val today = remember { LocalDate.now() }
    val day = fuelViewModel.day
    val all = fuelViewModel.meals.collectAsState().value
    val plan = planViewModel.sessions.collectAsState().value.orEmpty()

    var adding by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<Meal?>(null) }

    Column(
        Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        // Still reading the log off disk — hold the screen blank for the one frame
        // it takes rather than flashing the empty state.
        val meals = all?.filter { it.date == day } ?: return@Column

        // A day that trains earns the bonus, whichever kind of session it is.
        val training = plan.any { it.date == day && it.kind != SessionKind.REST }
        val summary = remember(meals, training) { summariseFuel(meals, targetKcal(training)) }

        DateStepper(date = day, onChange = fuelViewModel::goToDay, today = today)

        Column {
            SectionLabel(if (day == today) "Target for today" else "Target for the day")
            Row(
                Modifier.padding(top = 6.dp),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    "${formatKcal(summary.targetKcal)} kcal",
                    color = TextPrimary,
                    style = oswald(26f, FontWeight.W500, lineHeight = 1.15f),
                )
                if (training) {
                    Text(
                        "+$TRAINING_KCAL for training",
                        color = Accent,
                        style = inter(11f, FontWeight.W500, lineHeight = 1f),
                        modifier = Modifier.padding(bottom = 3.dp),
                    )
                }
            }
            Text(
                summary.meta(),
                color = TextMeta,
                style = mono(11f),
                modifier = Modifier.padding(top = 4.dp),
            )
        }

        Panel(verticalArrangement = Arrangement.spacedBy(13.dp)) {
            summary.macros.forEach { macro ->
                Column {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            macro.label,
                            color = TextPrimary,
                            style = inter(12f, FontWeight.W500, lineHeight = 1f),
                        )
                        Text(macro.value, color = TextSecondary, style = mono(12f))
                    }
                    Meter(
                        fraction = macro.percent / 100f,
                        color = if (macro.accented) Accent else TextSecondary,
                        track = TrackSoft,
                        height = 6.dp,
                        modifier = Modifier.padding(top = 7.dp),
                    )
                }
            }
        }

        Column {
            SectionLabel("Check-ins", modifier = Modifier.padding(bottom = 6.dp))
            if (meals.isEmpty()) {
                Text(
                    "Nothing planned for this day. Add a meal and it lands here.",
                    color = TextSubtle,
                    style = inter(11.5f, lineHeight = 1.5f),
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
            meals.forEach { meal ->
                MealRow(
                    meal = meal,
                    onToggle = { fuelViewModel.toggleLogged(meal) },
                    onEdit = { editing = meal },
                    onDelete = { fuelViewModel.delete(meal.id) },
                )
            }
        }

        AccentAction(
            "Add meal",
            icon = Icons.Filled.Add,
            modifier = Modifier.fillMaxWidth(),
            onClick = { adding = true },
        )

        Text(
            "No food search here — you write the day's meals yourself, and the target " +
                "moves with training load. Protein is the only macro with a hard floor.",
            color = TextFaint,
            style = inter(11f, lineHeight = 1.55f),
        )
    }

    if (adding) {
        MealDialog(
            date = day,
            onDismiss = { adding = false },
            onSave = { meal ->
                fuelViewModel.save(meal)
                adding = false
            },
        )
    }

    editing?.let { meal ->
        MealDialog(
            date = day,
            initial = meal,
            onDismiss = { editing = null },
            onSave = { edited ->
                fuelViewModel.save(edited)
                editing = null
            },
            onDelete = {
                fuelViewModel.delete(meal.id)
                editing = null
            },
        )
    }
}

/**
 * One meal on the day. The row itself is the check-in, since that is the gesture
 * the screen is for; editing and removing sit beside it as small targets, the
 * way a session row on the week carries them.
 */
@Composable
private fun MealRow(
    meal: Meal,
    onToggle: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    RuledRow(onClick = onToggle, verticalPadding = 12.dp) {
        Icon(
            if (meal.logged) Icons.Filled.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
            contentDescription = if (meal.logged) "Logged" else "Not logged",
            tint = if (meal.logged) Accent else TextDim,
            modifier = Modifier.size(19.dp),
        )
        Column(Modifier.weight(1f)) {
            Text(
                meal.name,
                // Once it's banked the row recedes.
                color = if (meal.logged) TextMuted else TextPrimary,
                style = inter(12.5f, FontWeight.W500, lineHeight = 1.3f),
            )
            Text(meal.meta(), color = TextMeta, style = inter(10.5f))
        }
        Text(meal.kcalLabel(), color = TextSecondary, style = mono(11.5f, FontWeight.W500))
        RowAction(Icons.Filled.Edit, "Edit ${meal.name}", onEdit)
        RowAction(Icons.Outlined.Close, "Remove ${meal.name}", onDelete)
    }
}

/** Drawn small to sit quietly in the row, with a finger-sized target around it. */
@Composable
private fun RowAction(
    icon: ImageVector,
    description: String,
    onClick: () -> Unit,
) {
    Box(
        Modifier
            .size(30.dp)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = description, tint = TextDim, modifier = Modifier.size(13.dp))
    }
}
