package com.repsrox.app.ui.screens

import android.content.ClipData
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import com.repsrox.app.data.MonthExport
import com.repsrox.app.data.exportMonth
import com.repsrox.app.ui.ExportHistory
import com.repsrox.app.ui.components.AccentAction
import com.repsrox.app.ui.components.Panel
import com.repsrox.app.ui.components.QuietAction
import com.repsrox.app.ui.components.RuledRow
import com.repsrox.app.ui.components.SectionLabel
import com.repsrox.app.ui.components.Step
import com.repsrox.app.ui.theme.Accent
import com.repsrox.app.ui.theme.BorderAction
import com.repsrox.app.ui.theme.BorderChip
import com.repsrox.app.ui.theme.SurfaceRaised
import com.repsrox.app.ui.theme.TextFaint
import com.repsrox.app.ui.theme.TextPrimary
import com.repsrox.app.ui.theme.TextSubtle
import com.repsrox.app.ui.theme.inter
import com.repsrox.app.ui.theme.mono
import java.io.File
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

/** Matches the provider's authority in the manifest and the path in `export_paths.xml`. */
private const val EXPORTS_AUTHORITY_SUFFIX = ".exports"
private const val EXPORTS_DIR = "exports"

/** A month of training, meals and races, sent out as one CSV file each. */
@Composable
fun ExportMonthDialog(history: ExportHistory, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val thisMonth = remember { YearMonth.now() }
    var month by remember { mutableStateOf(thisMonth) }
    val export = remember(month, history) {
        exportMonth(month, history.sessions, history.meals, history.races)
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Panel(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            contentPadding = PaddingValues(16.dp),
        ) {
            SectionLabel("Export month")
            Text(
                "Everything banked in a month — the sets you worked, the meals you " +
                    "planned and ate, the sims you raced — as one CSV file each, ready " +
                    "for a spreadsheet.",
                color = TextFaint,
                style = inter(10.5f, lineHeight = 1.5f),
                modifier = Modifier.padding(top = 6.dp, bottom = 10.dp),
            )

            MonthStepper(month, latest = thisMonth, onChange = { month = it })

            export.files.forEach { file ->
                RuledRow(verticalPadding = 10.dp) {
                    Text(
                        file.label,
                        color = if (file.rows > 0) TextPrimary else TextSubtle,
                        style = inter(12.5f, FontWeight.W500, lineHeight = 1.3f),
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        if (file.rows > 0) "${file.rows} rows" else "nothing",
                        color = TextFaint,
                        style = mono(10.5f),
                    )
                }
            }

            Row(
                Modifier.fillMaxWidth().padding(top = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                QuietAction("Close", modifier = Modifier.weight(1f), onClick = onDismiss)
                val canShare = !export.isEmpty
                AccentAction(
                    "Share",
                    modifier = Modifier.weight(1f),
                    borderColor = if (canShare) Accent else BorderAction,
                    onClick = { if (canShare) shareExport(context, export) },
                )
            }
        }
    }
}

/** Steps a month at a time, and never past this one — there is nothing banked ahead of today. */
@Composable
private fun MonthStepper(month: YearMonth, latest: YearMonth, onChange: (YearMonth) -> Unit) {
    val shape = RoundedCornerShape(6.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .background(SurfaceRaised, shape)
            .border(1.dp, BorderChip, shape)
            .padding(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Step(Icons.Filled.ChevronLeft, "Previous month") { onChange(month.minusMonths(1)) }
        Text(
            "${month.month.getDisplayName(TextStyle.FULL, Locale.US)} ${month.year}",
            color = TextPrimary,
            style = inter(12.5f, FontWeight.W500, lineHeight = 1f),
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f),
        )
        Step(Icons.Filled.ChevronRight, "Next month", enabled = month < latest) {
            onChange(month.plusMonths(1))
        }
    }
}

/**
 * Writes the sheets that hold anything into the cache and hands them to the
 * share sheet. An empty sheet is left behind: a header over no rows is a file
 * nobody asked to receive.
 */
private fun shareExport(context: Context, export: MonthExport) {
    val dir = File(context.cacheDir, EXPORTS_DIR).apply { mkdirs() }
    // Last time's files have been read by whoever they were shared with.
    dir.listFiles()?.forEach { it.delete() }

    val uris = export.files.filter { it.rows > 0 }.map { file ->
        val target = File(dir, file.name).apply { writeText(file.text) }
        FileProvider.getUriForFile(context, context.packageName + EXPORTS_AUTHORITY_SUFFIX, target)
    }
    if (uris.isEmpty()) return

    val intent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
        type = "text/csv"
        putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris))
        putExtra(Intent.EXTRA_SUBJECT, "RepsRox · ${export.month}")
        // The chooser reads its preview, and its grant, off the clip data.
        clipData = ClipData.newRawUri(null, uris.first()).apply {
            uris.drop(1).forEach { addItem(ClipData.Item(it)) }
        }
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "Share export"))
}
