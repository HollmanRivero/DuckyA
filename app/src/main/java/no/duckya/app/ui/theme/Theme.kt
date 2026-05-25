package no.duckya.app.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DuckyYellow = Color(0xFFFFC940)
private val DuckyOrange = Color(0xFFE89F00)

private val LightColors = lightColorScheme(
    primary = DuckyOrange,
    secondary = DuckyYellow,
    tertiary = Color(0xFF7B5E00)
)

private val DarkColors = darkColorScheme(
    primary = DuckyYellow,
    secondary = DuckyOrange,
    tertiary = Color(0xFFFFD980)
)

@Composable
fun DuckyTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val scheme = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val ctx = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(ctx) else dynamicLightColorScheme(ctx)
        }
        darkTheme -> DarkColors
        else -> LightColors
    }
    MaterialTheme(colorScheme = scheme, content = content)
}
