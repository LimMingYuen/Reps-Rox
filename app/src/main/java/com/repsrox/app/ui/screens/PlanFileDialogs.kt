package com.repsrox.app.ui.screens

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.repsrox.app.data.ParsedPlan
import com.repsrox.app.data.parsePlan
import com.repsrox.app.ui.components.AccentAction
import com.repsrox.app.ui.components.Panel
import com.repsrox.app.ui.components.QuietAction
import com.repsrox.app.ui.components.SectionLabel
import com.repsrox.app.ui.theme.Accent
import com.repsrox.app.ui.theme.BorderAction
import com.repsrox.app.ui.theme.BorderChip
import com.repsrox.app.ui.theme.SurfaceRaised
import com.repsrox.app.ui.theme.TextDim
import com.repsrox.app.ui.theme.TextFaint
import com.repsrox.app.ui.theme.TextMeta
import com.repsrox.app.ui.theme.TextPrimary
import com.repsrox.app.ui.theme.inter
import com.repsrox.app.ui.theme.mono

/** How much of an opened file gets read — long past any real plan, short of a memory problem. */
private const val MAX_IMPORT_CHARS = 200_000
private const val MAX_PROBLEMS_SHOWN = 4

/** The week, written out as a document that can be shared, saved, and edited by hand. */
@Composable
fun ExportPlanDialog(markdown: String, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    var copied by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Panel(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            contentPadding = PaddingValues(16.dp),
        ) {
            SectionLabel("Export plan")
            Text(
                "Your strength sessions' exercises, plus recent training and weigh-ins " +
                    "as context. Only the Sessions table is read back in on import.",
                color = TextFaint,
                style = inter(10.5f, lineHeight = 1.5f),
                modifier = Modifier.padding(top = 6.dp, bottom = 10.dp),
            )
            Box(
                Modifier
                    .fillMaxWidth()
                    .heightIn(max = 320.dp)
                    .background(SurfaceRaised, RoundedCornerShape(6.dp))
                    .border(1.dp, BorderChip, RoundedCornerShape(6.dp))
                    .padding(10.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                Text(markdown, color = TextPrimary, style = mono(10.5f).copy(lineHeight = 16.sp))
            }
            Row(
                Modifier.fillMaxWidth().padding(top = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                QuietAction(
                    if (copied) "Copied" else "Copy",
                    modifier = Modifier.weight(1f),
                    onClick = {
                        clipboard.setText(AnnotatedString(markdown))
                        copied = true
                    },
                )
                AccentAction(
                    "Share",
                    modifier = Modifier.weight(1f),
                    borderColor = Accent,
                    onClick = {
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, markdown)
                        }
                        context.startActivity(Intent.createChooser(intent, "Share plan"))
                    },
                )
            }
            QuietAction("Close", modifier = Modifier.fillMaxWidth().padding(top = 10.dp), onClick = onDismiss)
        }
    }
}

/** Reads a plan document back — pasted, or opened from a file — and applies what it parses. */
@Composable
fun ImportPlanDialog(onDismiss: () -> Unit, onApply: (ParsedPlan) -> Unit) {
    val context = LocalContext.current
    var text by remember { mutableStateOf("") }
    val parsed = remember(text) { text.takeIf { it.isNotBlank() }?.let(::parsePlan) }

    val openFile = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        runCatching {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                text = stream.bufferedReader().readText().take(MAX_IMPORT_CHARS)
            }
        }
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Panel(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            contentPadding = PaddingValues(16.dp),
        ) {
            SectionLabel("Import plan")
            Text(
                "Paste an edited plan document, or open one as a file. Only sessions " +
                    "still on this week's plan are updated.",
                color = TextFaint,
                style = inter(10.5f, lineHeight = 1.5f),
                modifier = Modifier.padding(top = 6.dp, bottom = 10.dp),
            )

            Box(
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = 120.dp, max = 220.dp)
                    .background(SurfaceRaised, RoundedCornerShape(6.dp))
                    .border(1.dp, BorderChip, RoundedCornerShape(6.dp))
                    .padding(10.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                if (text.isEmpty()) {
                    Text("Paste your edited plan here…", color = TextDim, style = mono(10.5f))
                }
                BasicTextField(
                    value = text,
                    onValueChange = { text = it.take(MAX_IMPORT_CHARS) },
                    textStyle = mono(10.5f).copy(color = TextPrimary, lineHeight = 16.sp),
                    cursorBrush = SolidColor(Accent),
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            QuietAction(
                "Open a file",
                modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                onClick = { openFile.launch(arrayOf("text/*", "application/octet-stream")) },
            )

            parsed?.let { result ->
                Text(
                    result.summary(),
                    color = TextMeta,
                    style = mono(11f),
                    modifier = Modifier.padding(top = 10.dp),
                )
                result.problems.take(MAX_PROBLEMS_SHOWN).forEach { problem ->
                    Text(problem, color = TextDim, style = inter(10f), modifier = Modifier.padding(top = 3.dp))
                }
                val hidden = result.problems.size - MAX_PROBLEMS_SHOWN
                if (hidden > 0) {
                    Text("…and $hidden more skipped", color = TextDim, style = inter(10f), modifier = Modifier.padding(top = 3.dp))
                }
            }

            Row(
                Modifier.fillMaxWidth().padding(top = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                QuietAction("Cancel", modifier = Modifier.weight(1f), onClick = onDismiss)
                val canApply = parsed != null && !parsed.isEmpty
                AccentAction(
                    "Apply",
                    modifier = Modifier.weight(1f),
                    borderColor = if (canApply) Accent else BorderAction,
                    onClick = { if (canApply) onApply(parsed!!) },
                )
            }
        }
    }
}
