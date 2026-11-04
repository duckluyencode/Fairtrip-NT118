package com.tripfair.app.ui.theme

import android.content.res.Configuration
import androidx.compose.ui.tooling.preview.Preview

/** Shows a preview in both light and dark mode at the Figma frame size (390x844). */
@Preview(name = "Light", widthDp = 390, heightDp = 844, showBackground = true)
@Preview(
    name = "Dark",
    widthDp = 390,
    heightDp = 844,
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
annotation class ThemePreviews
