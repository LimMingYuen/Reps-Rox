package com.repsrox.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.SportsScore
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.SportsScore
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.repsrox.app.ui.components.Mark
import com.repsrox.app.ui.screens.BodyScreen
import com.repsrox.app.ui.screens.BuildSessionScreen
import com.repsrox.app.ui.screens.FuelScreen
import com.repsrox.app.ui.screens.LiveScreen
import com.repsrox.app.data.weekStart
import com.repsrox.app.ui.screens.PlanScreen
import com.repsrox.app.ui.screens.ProfileScreen
import com.repsrox.app.ui.screens.RaceScreen
import com.repsrox.app.ui.screens.RunScreen
import com.repsrox.app.ui.screens.SummaryScreen
import com.repsrox.app.ui.screens.TodayScreen
import com.repsrox.app.ui.theme.Accent
import com.repsrox.app.ui.theme.BorderSoft
import com.repsrox.app.ui.theme.NavBg
import com.repsrox.app.ui.theme.ScreenBg
import com.repsrox.app.ui.theme.TextDim
import com.repsrox.app.ui.theme.TextMeta
import com.repsrox.app.ui.theme.TextPrimary
import com.repsrox.app.ui.theme.inter
import com.repsrox.app.ui.theme.oswald
import kotlinx.coroutines.delay

@Composable
fun RepsRoxApp(
    viewModel: RepsRoxViewModel = viewModel(),
    planViewModel: PlanViewModel = viewModel(),
) {
    // The rest clock and the two running timers all advance off one ticker.
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            viewModel.tick()
        }
    }

    BackHandler(enabled = viewModel.screen != Screen.Today) { viewModel.back() }

    Column(
        Modifier
            .fillMaxSize()
            .background(ScreenBg),
    ) {
        TopBar(title = viewModel.screen.title, modifier = Modifier.statusBarsPadding())

        // A fresh scroll position per screen, as switching screens does in the design.
        key(viewModel.screen) {
            Box(
                Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
            ) {
                when (viewModel.screen) {
                    Screen.Today -> TodayScreen(viewModel)
                    Screen.Plan -> PlanScreen(viewModel)
                    Screen.Build -> BuildSessionScreen(
                        date = viewModel.buildDate,
                        editing = viewModel.editingSession,
                        onSave = { session ->
                            planViewModel.save(session)
                            // Show the week the session landed in, not the one you left.
                            viewModel.goToWeek(session.date.weekStart())
                            viewModel.go(Screen.Plan)
                        },
                        // Out the way it was come in by — the week, or Today's card.
                        onCancel = { viewModel.back() },
                    )
                    Screen.Live -> LiveScreen(viewModel)
                    Screen.Run -> RunScreen(viewModel)
                    Screen.Race -> RaceScreen(viewModel)
                    Screen.Fuel -> FuelScreen()
                    Screen.Body -> BodyScreen()
                    Screen.Summary -> SummaryScreen(viewModel)
                    Screen.Profile -> ProfileScreen(onBody = { viewModel.go(Screen.Body) })
                }
            }
        }

        BottomNav(
            current = viewModel.screen,
            onSelect = viewModel::selectTab,
            modifier = Modifier.navigationBarsPadding(),
        )
    }
}

@Composable
private fun TopBar(title: String, modifier: Modifier = Modifier) {
    Row(
        modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        Mark(size = 20.dp)
        Text(
            title,
            color = TextPrimary,
            style = oswald(14f, FontWeight.W600, tracking = 0.09f),
        )
        Spacer(Modifier.weight(1f))
        Icon(
            Icons.Outlined.Notifications,
            contentDescription = "Notifications",
            tint = TextMeta,
            modifier = Modifier.size(17.dp),
        )
    }
}

@Composable
private fun BottomNav(current: Screen, onSelect: (NavTab) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth().background(NavBg)) {
        Box(Modifier.fillMaxWidth().height(1.dp).background(BorderSoft))
        Row(Modifier.fillMaxWidth()) {
            NavTab.entries.forEach { tab ->
                NavItem(tab, active = current in tab.group, onSelect = { onSelect(tab) })
            }
        }
    }
}

@Composable
private fun RowScope.NavItem(tab: NavTab, active: Boolean, onSelect: () -> Unit) {
    val tint = if (active) Accent else TextDim
    Column(
        Modifier
            .weight(1f)
            .clickable(onClick = onSelect),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // The active tab is marked by a rule above it, not a filled pill.
        Box(
            Modifier
                .fillMaxWidth()
                .height(2.dp)
                .background(if (active) Accent else Color.Transparent),
        )
        Column(
            Modifier.padding(top = 9.dp, bottom = 11.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(
                tab.icon(active),
                contentDescription = tab.label,
                tint = tint,
                modifier = Modifier.size(20.dp),
            )
            Text(
                tab.label,
                color = tint,
                style = inter(9.5f, FontWeight.W500, lineHeight = 1f, tracking = 0.06f),
            )
        }
    }
}

/** Phosphor's duotone/fill pairing maps onto Material's filled/outlined pairs. */
private fun NavTab.icon(active: Boolean): ImageVector = when (this) {
    NavTab.Today -> if (active) Icons.Filled.Home else Icons.Outlined.Home
    NavTab.Train -> if (active) Icons.Filled.FitnessCenter else Icons.Outlined.FitnessCenter
    NavTab.Race -> if (active) Icons.Filled.SportsScore else Icons.Outlined.SportsScore
    NavTab.Fuel -> if (active) Icons.Filled.Restaurant else Icons.Outlined.Restaurant
    NavTab.You -> if (active) Icons.Filled.Person else Icons.Outlined.Person
}
