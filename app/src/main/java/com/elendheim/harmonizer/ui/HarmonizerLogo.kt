package com.elendheim.harmonizer.ui

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.elendheim.harmonizer.ui.theme.LogoBright
import com.elendheim.harmonizer.ui.theme.LogoLight
import com.elendheim.harmonizer.ui.theme.LogoTeal

// The mark: three offset zigzags — stacked harmony waves. Coordinates live in a
// 100x100 logical box and scale to whatever size the Canvas is given.
private val LIGHT = listOf(
    6f to 50f, 22f to 32f, 36f to 50f, 48f to 26f, 60f to 50f, 74f to 32f, 90f to 50f
)
private val BRIGHT = listOf(
    8f to 60f, 24f to 42f, 38f to 60f, 50f to 36f, 62f to 60f, 76f to 42f, 92f to 60f
)
private val TEAL = listOf(
    10f to 70f, 26f to 52f, 40f to 70f, 52f to 46f, 64f to 70f, 78f to 52f, 94f to 70f
)

/** The Elendheim Harmonizer logo, drawn to fill [modifier]'s bounds. */
@Composable
fun HarmonizerLogo(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        // Back to front: teal shadow, bright middle, light top.
        drawZigzag(TEAL, LogoTeal)
        drawZigzag(BRIGHT, LogoBright)
        drawZigzag(LIGHT, LogoLight)
    }
}

private fun DrawScope.drawZigzag(points: List<Pair<Float, Float>>, color: Color) {
    val sx = size.width / 100f
    val sy = size.height / 100f
    val path = Path()
    points.forEachIndexed { i, (x, y) ->
        if (i == 0) path.moveTo(x * sx, y * sy) else path.lineTo(x * sx, y * sy)
    }
    drawPath(
        path = path,
        color = color,
        style = Stroke(
            width = size.minDimension * 0.058f,
            cap = StrokeCap.Round,
            join = StrokeJoin.Round,
        ),
    )
}
