package com.elendheim.harmonizer.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.foundation.Canvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.elendheim.harmonizer.audio.PitchDetector
import com.elendheim.harmonizer.ui.theme.FifthAccent
import com.elendheim.harmonizer.ui.theme.Muted
import com.elendheim.harmonizer.ui.theme.VoiceAccent

/**
 * The whole app is one screen: a big button you tap to sing through, a live
 * readout of your note and the fifth stacked on top, and one slider for how
 * loud that fifth sits.
 */
@Composable
fun HarmonizerScreen(
    running: Boolean,
    hasPermission: Boolean,
    level: Float,
    note: Double?,
    fifthLevel: Float,
    onToggle: () -> Unit,
    onFifthLevelChange: (Float) -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 28.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Header()

            Spacer(Modifier.weight(1f))

            NoteReadout(running = running, note = note)

            Spacer(Modifier.height(28.dp))

            SingButton(running = running, level = level, onToggle = onToggle)

            Spacer(Modifier.height(20.dp))

            Text(
                text = when {
                    !hasPermission -> "Tap to allow the microphone"
                    running -> "Listening. Sing something."
                    else -> "Tap and sing"
                },
                style = MaterialTheme.typography.bodyLarge,
                color = Muted,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.weight(1f))

            FifthMix(fifthLevel = fifthLevel, onChange = onFifthLevelChange)

            Spacer(Modifier.height(12.dp))

            Text(
                text = "Best with headphones, so the speaker doesn't loop back.",
                style = MaterialTheme.typography.bodyLarge,
                color = Muted,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun Header() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "Elendheim Harmonizer",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
            text = "Sing. Hear a fifth on top.",
            style = MaterialTheme.typography.bodyLarge,
            color = Muted,
        )
    }
}

@Composable
private fun NoteReadout(running: Boolean, note: Double?) {
    val you = PitchDetector.noteName(note)
    val fifth = PitchDetector.noteName(note?.let { it * 1.5 })
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        NotePill(label = "You", value = if (running) you else "--", accent = VoiceAccent)
        Text(
            text = "+5th",
            color = Muted,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        NotePill(label = "Fifth", value = if (running) fifth else "--", accent = FifthAccent)
    }
}

@Composable
private fun NotePill(label: String, value: String, accent: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            fontSize = 44.sp,
            fontWeight = FontWeight.SemiBold,
            color = accent,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = Muted,
        )
    }
}

@Composable
private fun SingButton(running: Boolean, level: Float, onToggle: () -> Unit) {
    // The ring breathes with your voice when active, and sits calm when idle.
    val target = if (running) 0.15f + level.coerceIn(0f, 1f) * 0.85f else 0.08f
    val pulse by animateFloatAsState(targetValue = target, label = "pulse")

    Box(
        modifier = Modifier
            .fillMaxWidth(0.62f)
            .aspectRatio(1f)
            .clip(RoundedCornerShape(percent = 50))
            .clickable(onClick = onToggle),
        contentAlignment = Alignment.Center,
    ) {
        // Glow ring that reacts to level.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(percent = 50))
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            VoiceAccent.copy(alpha = 0.05f + pulse * 0.30f),
                            Color.Transparent,
                        )
                    )
                )
        )
        // Core disc with the glyph drawn on top (72% of the button).
        Box(
            modifier = Modifier
                .fillMaxWidth(0.72f)
                .aspectRatio(1f)
                .clip(RoundedCornerShape(percent = 50))
                .background(
                    Brush.linearGradient(
                        colors = if (running) {
                            listOf(VoiceAccent, FifthAccent)
                        } else {
                            listOf(Color(0xFF1B2128), Color(0xFF11151A))
                        },
                        start = Offset.Zero,
                        end = Offset.Infinite,
                    )
                ),
            contentAlignment = Alignment.Center,
        ) {
            val glyphColor = if (running) Color(0xFF0B0D10) else VoiceAccent
            Canvas(modifier = Modifier.fillMaxWidth(0.42f).aspectRatio(1f)) {
                if (running) drawStopGlyph(glyphColor) else drawMicGlyph(glyphColor)
            }
        }
    }
}

private fun DrawScope.drawStopGlyph(color: Color) {
    val s = size.minDimension * 0.72f
    val topLeft = Offset((size.width - s) / 2f, (size.height - s) / 2f)
    drawRoundRect(
        color = color,
        topLeft = topLeft,
        size = Size(s, s),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(s * 0.18f, s * 0.18f),
    )
}

private fun DrawScope.drawMicGlyph(color: Color) {
    val w = size.width
    val h = size.height
    val capsuleW = w * 0.34f
    val capsuleH = h * 0.5f
    val cx = w / 2f
    // Capsule body of the mic.
    drawRoundRect(
        color = color,
        topLeft = Offset(cx - capsuleW / 2f, h * 0.1f),
        size = Size(capsuleW, capsuleH),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(capsuleW / 2f, capsuleW / 2f),
    )
    // Cradle arc.
    val stroke = Stroke(width = w * 0.07f)
    val arcSize = capsuleW * 1.9f
    drawArc(
        color = color,
        startAngle = 20f,
        sweepAngle = 140f,
        useCenter = false,
        topLeft = Offset(cx - arcSize / 2f, h * 0.2f),
        size = Size(arcSize, arcSize),
        style = stroke,
    )
    // Stem and base.
    drawLine(
        color = color,
        start = Offset(cx, h * 0.78f),
        end = Offset(cx, h * 0.9f),
        strokeWidth = w * 0.07f,
    )
    drawLine(
        color = color,
        start = Offset(cx - w * 0.13f, h * 0.9f),
        end = Offset(cx + w * 0.13f, h * 0.9f),
        strokeWidth = w * 0.07f,
    )
}

@Composable
private fun FifthMix(fifthLevel: Float, onChange: (Float) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("Fifth level", color = Muted, style = MaterialTheme.typography.labelLarge)
            Text(
                "${(fifthLevel * 100).toInt()}%",
                color = FifthAccent,
                style = MaterialTheme.typography.labelLarge,
            )
        }
        Slider(
            value = fifthLevel,
            onValueChange = onChange,
            valueRange = 0f..1f,
            colors = SliderDefaults.colors(
                thumbColor = FifthAccent,
                activeTrackColor = FifthAccent,
            ),
        )
    }
}
