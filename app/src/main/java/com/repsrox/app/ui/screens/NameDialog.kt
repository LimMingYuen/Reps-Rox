package com.repsrox.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import com.repsrox.app.data.PROFILE_NAME_MAX_CHARS
import com.repsrox.app.ui.components.AccentAction
import com.repsrox.app.ui.components.Panel
import com.repsrox.app.ui.components.PlainTextField
import com.repsrox.app.ui.components.QuietAction
import com.repsrox.app.ui.components.SectionLabel
import com.repsrox.app.ui.theme.Accent
import com.repsrox.app.ui.theme.BorderAction

/**
 * Who the training belongs to. The name is the profile's alone — nothing else
 * reads it — so there is one field here and no more.
 */
@Composable
fun NameDialog(
    name: String?,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
    onClear: () -> Unit,
) {
    var typed by remember { mutableStateOf(name.orEmpty()) }

    // A blank name is what an unnamed install already has; clearing is what the
    // remove action is for, so saving nothing does nothing.
    val canSave = typed.isNotBlank()

    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }

    Dialog(
        onDismissRequest = onDismiss,
        // Same reason as the race dialog: the platform's own width is narrower
        // than the design's gutter.
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Panel(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            contentPadding = PaddingValues(16.dp),
        ) {
            SectionLabel(if (name == null) "Add your name" else "Your name")

            PlainTextField(
                value = typed,
                onValueChange = { typed = it },
                placeholder = "Alex Carter",
                maxChars = PROFILE_NAME_MAX_CHARS,
                modifier = Modifier
                    .padding(top = 14.dp)
                    .focusRequester(focus),
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
                    borderColor = if (canSave) Accent else BorderAction,
                    onClick = { if (canSave) onSave(typed) },
                )
            }

            if (name != null) {
                QuietAction(
                    "Remove name",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    onClick = onClear,
                )
            }
        }
    }
}
