package com.caffeinewindow.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val CoffeeBrown = Color(0xFF6F4E37)
private val Cream = Color(0xFFFFF8F0)
private val Espresso = Color(0xFF3E2723)

private val LightColors = lightColorScheme(
  primary = CoffeeBrown,
  onPrimary = Color.White,
  secondary = Espresso,
  background = Cream,
  onBackground = Espresso,
  surface = Color.White,
  onSurface = Espresso,
)

private val DarkColors = darkColorScheme(
  primary = Color(0xFFD7B899),
  onPrimary = Espresso,
  secondary = Color(0xFFBCAAA4),
  background = Color(0xFF1B1411),
  onBackground = Cream,
  surface = Color(0xFF2A211D),
  onSurface = Cream,
)

@Composable
fun CaffeineWindowTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  content: @Composable () -> Unit,
) {
  MaterialTheme(
    colorScheme = if (darkTheme) DarkColors else LightColors,
    content = content,
  )
}
