package com.elendheim.harmonizer.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.elendheim.harmonizer.R
import com.elendheim.harmonizer.Settings
import com.elendheim.harmonizer.ThemeMode
import com.elendheim.harmonizer.intervalName
import com.elendheim.harmonizer.intervalShortLabel
import com.elendheim.harmonizer.ui.theme.FifthAccent
import com.elendheim.harmonizer.ui.theme.Muted
import com.elendheim.harmonizer.ui.theme.VoiceAccent

/**
 * The full settings: how the harmony sounds, and a stack of accessibility
 * options. Everything applies immediately and is remembered between sessions.
 */
@Composable
fun SettingsScreen(
    settings: Settings,
    onChange: (Settings) -> Unit,
    onBack: () -> Unit,
) {
    val scale = if (settings.largeText) 1.22f else 1f
    val muted = if (settings.highContrast) Color(0xFFD5DBE0) else Muted

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 8.dp),
        ) {
            // Header with a back button.
            Row(
                modifier = Modifier.fillMaxWidth().height(56.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(percent = 50))
                        .clickable(onClick = onBack),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_arrow_back),
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.size(24.dp),
                    )
                }
                Spacer(Modifier.size(8.dp))
                Text(
                    "Settings",
                    fontSize = 22.sp * scale,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground,
                )
            }

            SectionTitle("Harmony", muted, scale)

            StepperRow(
                label = "Interval",
                valueLabel = intervalShortLabel(settings.primarySemitones),
                caption = intervalName(settings.primarySemitones),
                scale = scale,
                muted = muted,
                onDecrease = {
                    onChange(settings.copy(primarySemitones = (settings.primarySemitones - 1).coerceAtLeast(Settings.MIN_SEMITONES)))
                },
                onIncrease = {
                    onChange(settings.copy(primarySemitones = (settings.primarySemitones + 1).coerceAtMost(Settings.MAX_SEMITONES)))
                },
            )
            LevelRow("Harmony level", settings.primaryLevel, muted, scale) {
                onChange(settings.copy(primaryLevel = it))
            }

            Spacer(Modifier.height(8.dp))
            SwitchRow(
                label = "Second voice",
                caption = "Stack a low and a high at once",
                checked = settings.secondEnabled,
                scale = scale,
                muted = muted,
            ) { onChange(settings.copy(secondEnabled = it)) }

            if (settings.secondEnabled) {
                StepperRow(
                    label = "2nd interval",
                    valueLabel = intervalShortLabel(settings.secondSemitones),
                    caption = intervalName(settings.secondSemitones),
                    scale = scale,
                    muted = muted,
                    onDecrease = {
                        onChange(settings.copy(secondSemitones = (settings.secondSemitones - 1).coerceAtLeast(Settings.MIN_SEMITONES)))
                    },
                    onIncrease = {
                        onChange(settings.copy(secondSemitones = (settings.secondSemitones + 1).coerceAtMost(Settings.MAX_SEMITONES)))
                    },
                )
                LevelRow("2nd voice level", settings.secondLevel, muted, scale) {
                    onChange(settings.copy(secondLevel = it))
                }
            }

            Spacer(Modifier.height(20.dp))
            SectionTitle("Accessibility", muted, scale)

            ThemeRow(settings.themeMode, scale, muted) { onChange(settings.copy(themeMode = it)) }

            SwitchRow("Large text", "Bigger notes and labels", settings.largeText, scale, muted) {
                onChange(settings.copy(largeText = it))
            }
            SwitchRow("High contrast", "Stronger colours and text", settings.highContrast, scale, muted) {
                onChange(settings.copy(highContrast = it))
            }
            SwitchRow("Reduce motion", "Calm the glow animation", settings.reduceMotion, scale, muted) {
                onChange(settings.copy(reduceMotion = it))
            }
            SwitchRow("Haptics", "A tap when you start and stop", settings.haptics, scale, muted) {
                onChange(settings.copy(haptics = it))
            }
            SwitchRow("Keep screen on", "Don't sleep while singing", settings.keepScreenOn, scale, muted) {
                onChange(settings.copy(keepScreenOn = it))
            }

            Spacer(Modifier.height(20.dp))
            SectionTitle("About", muted, scale)
            Text(
                "Elendheim Harmonizer v1.1",
                fontSize = 16.sp * scale,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                "Sing and hear a harmony stacked on top of your voice, live. " +
                    "Listens to your voice, stacks the interval you pick in real time, " +
                    "and plays the blend back as you sing.",
                fontSize = 14.sp * scale,
                color = muted,
                modifier = Modifier.padding(top = 4.dp),
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SectionTitle(text: String, muted: Color, scale: Float) {
    Text(
        text = text.uppercase(),
        fontSize = 13.sp * scale,
        fontWeight = FontWeight.Bold,
        color = FifthAccent,
        modifier = Modifier.padding(top = 8.dp, bottom = 8.dp),
    )
}

@Composable
private fun StepperRow(
    label: String,
    valueLabel: String,
    caption: String,
    scale: Float,
    muted: Color,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, fontSize = 17.sp * scale, color = MaterialTheme.colorScheme.onBackground)
            Text(caption, fontSize = 13.sp * scale, color = muted)
        }
        StepButton("-", scale, onDecrease)
        Text(
            valueLabel,
            fontSize = 20.sp * scale,
            fontWeight = FontWeight.Bold,
            color = FifthAccent,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        StepButton("+", scale, onIncrease)
    }
}

@Composable
private fun StepButton(symbol: String, scale: Float, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(symbol, fontSize = 22.sp * scale, fontWeight = FontWeight.Bold, color = VoiceAccent)
    }
}

@Composable
private fun LevelRow(label: String, value: Float, muted: Color, scale: Float, onChange: (Float) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, fontSize = 15.sp * scale, color = muted)
            Text("${(value * 100).toInt()}%", fontSize = 15.sp * scale, color = FifthAccent, fontWeight = FontWeight.SemiBold)
        }
        Slider(
            value = value,
            onValueChange = onChange,
            valueRange = 0f..1f,
            colors = SliderDefaults.colors(thumbColor = FifthAccent, activeTrackColor = FifthAccent),
        )
    }
}

@Composable
private fun SwitchRow(
    label: String,
    caption: String,
    checked: Boolean,
    scale: Float,
    muted: Color,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, fontSize = 17.sp * scale, color = MaterialTheme.colorScheme.onBackground)
            Text(caption, fontSize = 13.sp * scale, color = muted)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color(0xFF0B0D10),
                checkedTrackColor = VoiceAccent,
            ),
        )
    }
}

@Composable
private fun ThemeRow(mode: ThemeMode, scale: Float, muted: Color, onChange: (ThemeMode) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp)) {
        Text("Theme", fontSize = 17.sp * scale, color = MaterialTheme.colorScheme.onBackground)
        Spacer(Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ThemeChip("Dark", mode == ThemeMode.DARK, scale) { onChange(ThemeMode.DARK) }
            ThemeChip("Light", mode == ThemeMode.LIGHT, scale) { onChange(ThemeMode.LIGHT) }
            ThemeChip("System", mode == ThemeMode.SYSTEM, scale) { onChange(ThemeMode.SYSTEM) }
        }
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.ThemeChip(
    label: String,
    selected: Boolean,
    scale: Float,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .weight(1f)
            .height(44.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) VoiceAccent else MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            fontSize = 15.sp * scale,
            fontWeight = FontWeight.SemiBold,
            color = if (selected) Color(0xFF0B0D10) else MaterialTheme.colorScheme.onBackground,
        )
    }
}
