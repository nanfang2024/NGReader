package com.book.ng.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

fun Modifier.jellyGlass(
    shape: Shape = RoundedCornerShape(24.dp),
    alpha: Float = 0.6f,
    dark: Boolean = false,
): Modifier = this
    .shadow(elevation = 12.dp, shape = shape)
    .background(
        brush = Brush.linearGradient(
            colors = if (dark) {
                listOf(
                    Color(0xFF4A4A5E).copy(alpha = alpha),
                    Color(0xFF22222E).copy(alpha = alpha * 0.4f),
                )
            } else {
                listOf(
                    Color.White.copy(alpha = alpha),
                    Color.White.copy(alpha = alpha * 0.35f),
                )
            },
        ),
        shape = shape,
    )
    .border(width = 1.dp, color = Color.White.copy(alpha = if (dark) 0.25f else 0.65f), shape = shape)
