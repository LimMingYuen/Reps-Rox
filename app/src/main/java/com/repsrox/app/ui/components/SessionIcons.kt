package com.repsrox.app.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.SportsScore
import androidx.compose.material.icons.outlined.Bedtime
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.ui.graphics.vector.ImageVector
import com.repsrox.app.data.DayStatus
import com.repsrox.app.data.SessionKind

/** The design's Phosphor set mapped onto Material's, keeping the same reading. */
fun SessionKind.icon(): ImageVector = when (this) {
    SessionKind.RUN -> Icons.AutoMirrored.Filled.DirectionsRun
    SessionKind.STRENGTH -> Icons.Filled.FitnessCenter
    SessionKind.RACE -> Icons.Filled.SportsScore
    SessionKind.REST -> Icons.Outlined.Bedtime
}

fun DayStatus.icon(kind: SessionKind): ImageVector = when (this) {
    DayStatus.DONE -> Icons.Filled.CheckCircle
    DayStatus.TODAY -> Icons.Filled.PlayCircle
    DayStatus.PLANNED -> Icons.Outlined.RadioButtonUnchecked
    DayStatus.REST -> kind.icon()
}
