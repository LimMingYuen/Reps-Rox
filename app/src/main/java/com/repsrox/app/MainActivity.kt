package com.repsrox.app

import android.graphics.Color
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.tooling.preview.Preview
import com.repsrox.app.ui.RepsRoxApp
import com.repsrox.app.ui.Splash
import com.repsrox.app.ui.theme.RepsRoxTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // The app is dark-only, so both system bars keep light icons throughout.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        val animate = animatorsEnabled()
        setContent {
            RepsRoxTheme {
                RepsRoxRoot(animateLaunch = animate)
            }
        }
    }

    /**
     * Developer options and the accessibility "remove animations" setting both
     * zero the animator scale. Honouring it here means the launch animation is
     * skipped outright rather than played fast — a spin nobody asked to see is
     * the first thing to cut.
     */
    private fun animatorsEnabled(): Boolean =
        Settings.Global.getFloat(contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) > 0f
}

/**
 * Holds the launch animation in front of the app until it finishes, then
 * dissolves between the two. [rememberSaveable] keeps a rotation or a process
 * restore from replaying it — this is a cold-start moment, not a screen.
 */
@Composable
private fun RepsRoxRoot(animateLaunch: Boolean) {
    var ready by rememberSaveable { mutableStateOf(!animateLaunch) }
    Crossfade(
        targetState = ready,
        animationSpec = tween(EXIT_MS),
        label = "launch",
    ) { done ->
        if (done) RepsRoxApp() else Splash(onFinished = { ready = true })
    }
}

private const val EXIT_MS = 220

@Preview(showBackground = true, device = "spec:width=412dp,height=892dp")
@Composable
private fun RepsRoxAppPreview() {
    RepsRoxTheme {
        RepsRoxApp()
    }
}
