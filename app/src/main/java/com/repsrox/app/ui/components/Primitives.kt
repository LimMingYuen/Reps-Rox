package com.repsrox.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.repsrox.app.ui.theme.Accent
import com.repsrox.app.ui.theme.AccentRingNow
import com.repsrox.app.ui.theme.BorderAction
import com.repsrox.app.ui.theme.BorderSoft
import com.repsrox.app.ui.theme.Hairline
import com.repsrox.app.ui.theme.Kicker
import com.repsrox.app.ui.theme.RingTodo
import com.repsrox.app.ui.theme.SurfaceBg
import com.repsrox.app.ui.theme.TextFaint
import com.repsrox.app.ui.theme.TextMeta
import com.repsrox.app.ui.theme.TextOnAction
import com.repsrox.app.ui.theme.TextPrimary
import com.repsrox.app.ui.theme.inter
import com.repsrox.app.ui.theme.oswald

/** The all-caps rule that opens most sections. Tracking tightens a little inside cards. */
@Composable
fun SectionLabel(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = TextFaint,
    tracking: Float = 0.14f,
) {
    val style = if (tracking == 0.14f) Kicker else inter(10f, FontWeight.W400, lineHeight = 1f, tracking = tracking)
    Text(text.uppercase(), modifier, color = color, style = style)
}

/** The design's one card: #1a1b1e, a hairline outline, 8px radius. */
@Composable
fun Panel(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    borderColor: Color = BorderSoft,
    background: Color = SurfaceBg,
    contentPadding: PaddingValues = PaddingValues(14.dp),
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(8.dp)
    Column(
        modifier
            .clip(shape)
            .background(background, shape)
            .border(1.dp, borderColor, shape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(contentPadding),
        verticalArrangement = verticalArrangement,
        content = content,
    )
}

/** A number over its caption — the repeated "5 / exercises" pairing. */
@Composable
fun StatBlock(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
    valueSize: Float = 17f,
) {
    Column(modifier) {
        Text(value, color = TextPrimary, style = oswald(valueSize))
        Text(label, color = TextMeta, style = inter(10f))
    }
}

/** A list row under a hairline rule, as every list in the design is drawn. */
@Composable
fun RuledRow(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    verticalPadding: Dp = 11.dp,
    content: @Composable RowScope.() -> Unit,
) {
    Column(modifier.fillMaxWidth()) {
        Box(Modifier.fillMaxWidth().height(1.dp).background(Hairline))
        Row(
            Modifier
                .fillMaxWidth()
                .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
                .padding(horizontal = 2.dp, vertical = verticalPadding),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(11.dp),
            content = content,
        )
    }
}

/** A horizontal progress track with a solid fill. */
@Composable
fun Meter(
    fraction: Float,
    modifier: Modifier = Modifier,
    color: Color = Accent,
    track: Color = Hairline,
    height: Dp = 5.dp,
) {
    val shape = RoundedCornerShape(percent = 50)
    Box(
        modifier
            .fillMaxWidth()
            .height(height)
            .clip(shape)
            .background(track),
    ) {
        Box(
            Modifier
                .fillMaxWidth(fraction.coerceIn(0f, 1f))
                .height(height)
                .background(color),
        )
    }
}

/**
 * The segmented progress ring: eight dashes around a 108-unit circle, filled up
 * to [done] with the next one ghosted. [strokeWidth] is in those same units.
 */
@Composable
fun SegmentRing(
    size: Dp,
    done: Int,
    modifier: Modifier = Modifier,
    total: Int = 8,
    strokeWidth: Float = 9f,
    doneColor: Color = Accent,
    nowColor: Color = AccentRingNow,
    todoColor: Color = RingTodo,
    center: @Composable BoxScope.() -> Unit = {},
) {
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(size)) {
            val k = this.size.minDimension / VIEW_BOX
            val radius = 40f * k
            val inset = (54f * k) - radius
            repeat(total) { i ->
                drawArc(
                    color = when {
                        i < done -> doneColor
                        i == done -> nowColor
                        else -> todoColor
                    },
                    startAngle = i * 44.977f - 14.324f,
                    sweepAngle = 30.080f,
                    useCenter = false,
                    topLeft = Offset(inset, inset),
                    size = Size(radius * 2f, radius * 2f),
                    style = Stroke(width = strokeWidth * k),
                )
            }
        }
        center()
    }
}

private const val VIEW_BOX = 108f

/** The primary action: an accent outline on nothing, never a fill. */
@Composable
fun AccentAction(
    label: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    background: Color = Color.Transparent,
    borderColor: Color = Accent,
    verticalPadding: Dp = 12.dp,
    onClick: () -> Unit,
) = OutlinedAction(
    label, modifier, icon, Accent, borderColor, background, verticalPadding, 12.dp, onClick,
)

/** The secondary action: a neutral outline, dimmer label. */
@Composable
fun QuietAction(
    label: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    verticalPadding: Dp = 12.dp,
    horizontalPadding: Dp = 12.dp,
    onClick: () -> Unit,
) = OutlinedAction(
    label, modifier, icon, TextOnAction, BorderAction, Color.Transparent,
    verticalPadding, horizontalPadding, onClick,
)

@Composable
private fun OutlinedAction(
    label: String,
    modifier: Modifier,
    icon: ImageVector?,
    contentColor: Color,
    borderColor: Color,
    background: Color,
    verticalPadding: Dp,
    horizontalPadding: Dp,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(6.dp)
    Row(
        modifier
            .clip(shape)
            .background(background, shape)
            .border(1.dp, borderColor, shape)
            .clickable(onClick = onClick)
            .padding(vertical = verticalPadding, horizontal = horizontalPadding),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = contentColor, modifier = Modifier.size(13.dp))
        }
        Text(
            label.uppercase(),
            color = contentColor,
            style = inter(12f, FontWeight.W600, lineHeight = 1f, tracking = 0.06f),
        )
    }
}

/** The short vertical rule inside the week-plan rows. */
@Composable
fun VerticalRule(height: Dp = 32.dp, modifier: Modifier = Modifier) {
    Box(modifier.width(1.dp).height(height).background(BorderSoft))
}
