package com.tripfair.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

// Corner radii from the Figma design. Pills and chips use CircleShape.
val TripFairShapes = Shapes(
    // Small images, hint boxes
    extraSmall = RoundedCornerShape(8.dp),
    // Buttons, inputs, cards
    small = RoundedCornerShape(14.dp),
    // Quick action cards
    medium = RoundedCornerShape(16.dp),
    // Photo hero cards
    large = RoundedCornerShape(22.dp),
    // Bottom sheets and dialogs
    extraLarge = RoundedCornerShape(28.dp),
)
