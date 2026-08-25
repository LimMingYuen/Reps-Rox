package com.repsrox.app.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.repsrox.app.R
import com.repsrox.app.ui.components.Mark
import com.repsrox.app.ui.theme.RepsRoxTheme
import com.repsrox.app.ui.theme.ScreenBg
import com.repsrox.app.ui.theme.TextPrimary
import com.repsrox.app.ui.theme.oswald
import androidx.compose.material3.Text
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * The cold-start moment: the ring spins up and settles, then the wordmark
 * resolves out of wide tracking.
 *
 * The first frame is deliberately the mark *at rest* — same mark, same ground
 * as the system splash that precedes it — so the handoff is a continuation
 * rather than a cut. Motion only starts once we own the screen.
 *
 * [onFinished] fires when the sequence is done; the caller owns the dissolve
 * into the app itself.
 */
@Composable
fun Splash(onFinished: () -> Unit) {
    val spin = remember { Animatable(0f) }
    val wordAlpha = remember { Animatable(0f) }
    val tracking = remember { Animatable(TRACKING_START) }

    LaunchedEffect(Unit) {
        launch { spin.animateTo(SPIN_DEGREES, tween(SPIN_MS, easing = EmphasizedDecelerate)) }

        // The word starts while the ring is still settling — a clean handover
        // between the two reads as one gesture, two beats reads as a queue.
        delay(WORD_DELAY_MS)
        launch { wordAlpha.animateTo(1f, tween(WORD_MS, easing = LinearOutSlowInEasing)) }
        tracking.animateTo(TRACKING_END, tween(WORD_MS, easing = EmphasizedDecelerate))

        delay(HOLD_MS)
        onFinished()
    }

    SplashContent(
        ringRotation = spin.value,
        wordAlpha = wordAlpha.value,
        wordTracking = tracking.value,
    )
}

@Composable
private fun SplashContent(ringRotation: Float, wordAlpha: Float, wordTracking: Float) {
    Box(
        Modifier
            .fillMaxSize()
            .background(ScreenBg),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(GAP),
        ) {
            Mark(size = MARK_SIZE, ringRotation = ringRotation)
            Text(
                stringResource(R.string.app_name).uppercase(),
                color = TextPrimary,
                style = oswald(WORD_SIZE, FontWeight.W700, tracking = wordTracking),
                modifier = Modifier.alpha(wordAlpha),
            )
        }
    }
}

/**
 * Sized to sit close to the system splash icon so the mark doesn't jump on
 * handoff. If it does jump on a given launcher, this is the value to nudge.
 */
private val MARK_SIZE = 112.dp
private val GAP = 22.dp
private const val WORD_SIZE = 26f

/** Material 3's emphasized-decelerate: leaves fast, arrives soft. */
private val EmphasizedDecelerate = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)

/** One revolution — 360° is a multiple of the ring's 45° period, so it lands clean. */
private const val SPIN_DEGREES = 360f
private const val SPIN_MS = 820

/** Tracking in em, matching how [oswald] takes it. Resolves to the top bar's .09em. */
private const val TRACKING_START = 0.30f
private const val TRACKING_END = 0.09f

private const val WORD_DELAY_MS = 420L
private const val WORD_MS = 520
private const val HOLD_MS = 170L

@Preview(showBackground = true, device = "spec:width=412dp,height=892dp")
@Composable
private fun SplashSettledPreview() {
    RepsRoxTheme {
        SplashContent(ringRotation = SPIN_DEGREES, wordAlpha = 1f, wordTracking = TRACKING_END)
    }
}

@Preview(showBackground = true, device = "spec:width=412dp,height=892dp")
@Composable
private fun SplashMidSpinPreview() {
    RepsRoxTheme {
        SplashContent(ringRotation = 118f, wordAlpha = 0.35f, wordTracking = 0.21f)
    }
}
