package com.elendheim.harmonizer.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.elendheim.harmonizer.R
import com.elendheim.harmonizer.audio.PitchDetector
import com.elendheim.harmonizer.audio.semitonesToRatio
import com.elendheim.harmonizer.intervalShortLabel
import com.elendheim.harmonizer.ui.theme.FifthAccent
import com.elendheim.harmonizer.ui.theme.Muted
import com.elendheim.harmonizer.ui.theme.VoiceAccent

/**
 * The home screen: a big button you tap to sing through, a live readout of your
 * note and the harmony stacked on it, one quick level slider, and a gear that
 * opens the full settings.
 */
@Composable
fun HarmonizerScreen(
    running: Boolean,
    hasPermission: Boolean,
    level: Float,
    note: Double?,
    primarySemitones: Int,
    secondEnabled: Boolean,
    secondSemitones: Int,
    primaryLevel: Float,
    largeText: Boolean,
    highContrast: Boolean,
    reduceMotion: Boolean,
    onToggle: () -> Unit,
    onPrimaryLevelChange: (Float) -> Unit,
    onOpenSettings: () -> Unit,
) {
    val scale = if (largeText) 1.22f else 1f
    val muted = if (highContrast) Color(0xFFD5DBE0) else Muted

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(horizontal = 28.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            TopBar(scale = scale, onOpenSettings = onOpenSettings)

            Spacer(Modifier.weight(1f))

            NoteReadout(
                running = running,
                note = note,
                primarySemitones = primarySemitones,
                secondEnabled = secondEnabled,
                secondSemitones = secondSemitones,
                scale = scale,
                muted = muted,
            )

            Spacer(Modifier.height(28.dp))

            SingButton(running = running, level = level, reduceMotion = reduceMotion, onToggle = onToggle)

            Spacer(Modifier.height(20.dp))

            Text(
                text = when {
                    !hasPermission -> "Tap to allow the microphone"
                    running -> "Listening. Sing something."
                    else -> "Tap and sing"
                },
                fontSize = 17.sp * scale,
                color = muted,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.weight(1f))

            QuickLevel(primaryLevel = primaryLevel, muted = muted, scale = scale, onChange = onPrimaryLevelChange)

            Spacer(Modifier.height(12.dp))

            Text(
                text = "Best with headphones, so the speaker doesn't loop back.",
                fontSize = 14.sp * scale,
                color = muted,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun TopBar(scale: Float, onOpenSettings: () -> Unit) {
    Box(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "Elendheim Harmonizer",
                fontSize = 20.sp * scale,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = "Sing. Hear a fifth on top.",
                fontSize = 15.sp * scale,
                color = Muted,
            )
        }
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .size(44.dp)
                .clip(RoundedCornerShape(percent = 50))
                .clickable(onClick = onOpenSettings),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_settings),
                contentDescription = "Settings",
                tint = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.size(26.dp),
            )
        }
    }
}

@Composable
private fun NoteReadout(
    running: Boolean,
    note: Double?,
    primarySemitones: Int,
    secondEnabled: Boolean,
    secondSemitones: Int,
    scale: Float,
    muted: Color,
) {
    val you = PitchDetector.noteName(note)
    val primary = PitchDetector.noteName(note?.let { it * semitonesToRatio(primarySemitones) })
    val second = PitchDetector.noteName(note?.let { it * semitonesToRatio(secondSemitones) })

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        NotePill(label = "You", value = if (running) you else "--", accent = VoiceAccent, scale = scale, muted = muted)
        Chip(text = intervalShortLabel(primarySemitones), muted = muted, scale = scale)
        NotePill(label = "Harmony", value = if (running) primary else "--", accent = FifthAccent, scale = scale, muted = muted)
    }

    if (secondEnabled) {
        Spacer(Modifier.height(10.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Chip(text = intervalShortLabel(secondSemitones), muted = muted, scale = scale)
            NotePill(
                label = "2nd voice",
                value = if (running) second else "--",
                accent = VoiceAccent,
                scale = scale * 0.8f,
                muted = muted,
            )
        }
    }
}

@Composable
private fun Chip(text: String, muted: Color, scale: Float) {
    Text(
        text = text,
        color = muted,
        fontSize = 15.sp * scale,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(horizontal = 16.dp),
    )
}

@Composable
private fun NotePill(label: String, value: String, accent: Color, scale: Float, muted: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            fontSize = 44.sp * scale,
            fontWeight = FontWeight.SemiBold,
            color = accent,
        )
        Text(
            text = label,
            fontSize = 15.sp * scale,
            fontWeight = FontWeight.SemiBold,
            color = muted,
        )
    }
}

@Composable
private fun SingButton(running: Boolean, level: Float, reduceMotion: Boolean, onToggle: () -> Unit) {
    val target = if (running) 0.15f + level.coerceIn(0f, 1f) * 0.85f else 0.08f
    val animated by animateFloatAsState(targetValue = target, label = "pulse")
    val pulse = if (reduceMotion) target else animated

    Box(
        modifier = Modifier
            .fillMaxWidth(0.62f)
            .aspectRatio(1f)
            .clip(RoundedCornerShape(percent = 50))
            .clickable(onClick = onToggle),
        contentAlignment = Alignment.Center,
    ) {
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
        cornerRadius = CornerRadius(s * 0.18f, s * 0.18f),
    )
}

private fun DrawScope.drawMicGlyph(color: Color) {
    val w = size.width
    val h = size.height
    val capsuleW = w * 0.34f
    val capsuleH = h * 0.5f
    val cx = w / 2f
    drawRoundRect(
        color = color,
        topLeft = Offset(cx - capsuleW / 2f, h * 0.1f),
        size = Size(capsuleW, capsuleH),
        cornerRadius = CornerRadius(capsuleW / 2f, capsuleW / 2f),
    )
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
private fun QuickLevel(primaryLevel: Float, muted: Color, scale: Float, onChange: (Float) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("Harmony level", color = muted, fontSize = 15.sp * scale, fontWeight = FontWeight.SemiBold)
            Text(
                "${(primaryLevel * 100).toInt()}%",
                color = FifthAccent,
                fontSize = 15.sp * scale,
                fontWeight = FontWeight.SemiBold,
            )
        }
        Slider(
            value = primaryLevel,
            onValueChange = onChange,
            valueRange = 0f..1f,
            colors = SliderDefaults.colors(
                thumbColor = FifthAccent,
                activeTrackColor = FifthAccent,
            ),
        )
    }
}
