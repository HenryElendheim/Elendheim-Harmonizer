package com.elendheim.harmonizer

import android.content.Context
import kotlin.math.abs

/** Which colour scheme to use. The app is designed dark-first. */
enum class ThemeMode { DARK, LIGHT, SYSTEM }

/**
 * Everything the user can change, in one place. Kept as a plain immutable value
 * so the UI can copy-and-update it, and the store just reads/writes the fields.
 */
data class Settings(
    // Sound
    val primarySemitones: Int = 7,      // perfect fifth by default
    val primaryLevel: Float = 0.85f,
    val secondEnabled: Boolean = false,
    val secondSemitones: Int = -12,     // an octave down, for a bass voice
    val secondLevel: Float = 0.7f,
    // Accessibility
    val themeMode: ThemeMode = ThemeMode.DARK,
    val largeText: Boolean = false,
    val highContrast: Boolean = false,
    val reduceMotion: Boolean = false,
    val haptics: Boolean = true,
    val keepScreenOn: Boolean = true,
    // Recordings
    val saveRecordings: Boolean = true,
) {
    companion object {
        /** Intervals are capped at an octave either way. */
        const val MIN_SEMITONES = -12
        const val MAX_SEMITONES = 12
    }
}

/** Reads and writes [Settings] to SharedPreferences. */
class SettingsStore(context: Context) {
    private val prefs = context.applicationContext
        .getSharedPreferences("harmonizer_settings", Context.MODE_PRIVATE)

    fun load(): Settings {
        val d = Settings()
        return Settings(
            primarySemitones = prefs.getInt("primarySemitones", d.primarySemitones),
            primaryLevel = prefs.getFloat("primaryLevel", d.primaryLevel),
            secondEnabled = prefs.getBoolean("secondEnabled", d.secondEnabled),
            secondSemitones = prefs.getInt("secondSemitones", d.secondSemitones),
            secondLevel = prefs.getFloat("secondLevel", d.secondLevel),
            themeMode = runCatching { ThemeMode.valueOf(prefs.getString("themeMode", d.themeMode.name)!!) }
                .getOrDefault(d.themeMode),
            largeText = prefs.getBoolean("largeText", d.largeText),
            highContrast = prefs.getBoolean("highContrast", d.highContrast),
            reduceMotion = prefs.getBoolean("reduceMotion", d.reduceMotion),
            haptics = prefs.getBoolean("haptics", d.haptics),
            keepScreenOn = prefs.getBoolean("keepScreenOn", d.keepScreenOn),
            saveRecordings = prefs.getBoolean("saveRecordings", d.saveRecordings),
        )
    }

    fun save(s: Settings) {
        prefs.edit().apply {
            putInt("primarySemitones", s.primarySemitones)
            putFloat("primaryLevel", s.primaryLevel)
            putBoolean("secondEnabled", s.secondEnabled)
            putInt("secondSemitones", s.secondSemitones)
            putFloat("secondLevel", s.secondLevel)
            putString("themeMode", s.themeMode.name)
            putBoolean("largeText", s.largeText)
            putBoolean("highContrast", s.highContrast)
            putBoolean("reduceMotion", s.reduceMotion)
            putBoolean("haptics", s.haptics)
            putBoolean("keepScreenOn", s.keepScreenOn)
            putBoolean("saveRecordings", s.saveRecordings)
        }.apply()
    }
}

/** Short signed label for an interval, e.g. "+7", "-5", "0". */
fun intervalShortLabel(semitones: Int): String =
    if (semitones > 0) "+$semitones" else semitones.toString()

/** Musical name for an interval, e.g. "Perfect 5th up", "Octave down". */
fun intervalName(semitones: Int): String {
    val names = arrayOf(
        "Unison", "Minor 2nd", "Major 2nd", "Minor 3rd", "Major 3rd", "Perfect 4th",
        "Tritone", "Perfect 5th", "Minor 6th", "Major 6th", "Minor 7th", "Major 7th", "Octave"
    )
    val steps = abs(semitones)
    val base = if (steps <= 12) names[steps] else "$steps semitones"
    return when {
        semitones > 0 -> "$base up"
        semitones < 0 -> "$base down"
        else -> "Unison"
    }
}
