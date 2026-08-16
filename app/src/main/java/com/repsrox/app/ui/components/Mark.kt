package com.repsrox.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import com.repsrox.app.ui.theme.Accent

/**
 * The Reps & Rox mark, `ring` variant: a broken ring of eight arcs around a
 * chevron. Geometry is the design's own 108-unit square, scaled to [size].
 *
 * [ringRotation] turns the ring in degrees, leaving the chevron upright — the
 * segments carry the motion, the arrow stays readable. Because the eight
 * segments sit on 45° centres, any multiple of 45° is visually identical to
 * rest, so an animation can land on one and stop without a seam.
 */
@Composable
fun Mark(
    size: Dp,
    modifier: Modifier = Modifier,
    color: Color = Accent,
    ringRotation: Float = 0f,
) {
    Canvas(modifier.size(size)) {
        val k = this.size.minDimension / VIEW_BOX

        // circle r=38, stroke-width=12, stroke-dasharray="22 7.845", dashoffset=11
        val radius = 38f * k
        val stroke = 12f * k
        val inset = (54f * k) - radius
        repeat(RING_SEGMENTS) { i ->
            drawArc(
                color = color,
                startAngle = i * 45f - 16.586f + ringRotation,
                sweepAngle = 33.171f,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = Size(radius * 2f, radius * 2f),
                style = Stroke(width = stroke),
            )
        }

        drawPath(chevron(k), color)
    }
}

private const val VIEW_BOX = 108f
private const val RING_SEGMENTS = 8

/** polygon points="42,38 56,54 42,70 52,70 66,54 52,38" */
private fun chevron(k: Float): Path = Path().apply {
    moveTo(42f * k, 38f * k)
    lineTo(56f * k, 54f * k)
    lineTo(42f * k, 70f * k)
    lineTo(52f * k, 70f * k)
    lineTo(66f * k, 54f * k)
    lineTo(52f * k, 38f * k)
    close()
}
