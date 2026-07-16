package com.elendheim.harmonizer

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.core.content.ContextCompat
import com.elendheim.harmonizer.audio.HarmonizerEngine
import com.elendheim.harmonizer.ui.HarmonizerScreen
import com.elendheim.harmonizer.ui.RecordingsScreen
import com.elendheim.harmonizer.ui.SettingsScreen
import com.elendheim.harmonizer.ui.SplashScreen
import com.elendheim.harmonizer.ui.theme.ElendheimHarmonizerTheme
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val store = SettingsStore(this)
        setContent {
            var settings by remember { mutableStateOf(store.load()) }

            // A short splash on first launch. rememberSaveable keeps it from
            // replaying when the activity is recreated, e.g. on rotation.
            var showSplash by rememberSaveable { mutableStateOf(true) }
            LaunchedEffect(Unit) {
                if (showSplash) {
                    delay(1300)
                    showSplash = false
                }
            }

            ElendheimHarmonizerTheme(
                themeMode = settings.themeMode,
                highContrast = settings.highContrast,
            ) {
                if (showSplash) {
                    SplashScreen()
                } else {
                    HarmonizerApp(
                        settings = settings,
                        onSettingsChange = {
                            settings = it
                            store.save(it)
                        },
                    )
                }
            }
        }
    }
}

@androidx.compose.runtime.Composable
private fun HarmonizerApp(
    settings: Settings,
    onSettingsChange: (Settings) -> Unit,
) {
    val context = LocalView.current.context
    val view = LocalView.current
    val haptics = LocalHapticFeedback.current

    var running by remember { mutableStateOf(false) }
    var level by remember { mutableFloatStateOf(0f) }
    var note by remember { mutableStateOf<Double?>(null) }
    var showSettings by remember { mutableStateOf(false) }
    var showRecordings by remember { mutableStateOf(false) }
    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED
        )
    }

    val recordingStore = remember { RecordingStore(context.applicationContext) }

    var savedTick by remember { mutableIntStateOf(0) }
    var savedHintVisible by remember { mutableStateOf(false) }
    LaunchedEffect(savedTick) {
        if (savedTick > 0) {
            savedHintVisible = true
            delay(2500)
            savedHintVisible = false
        }
    }

    val engine = remember {
        HarmonizerEngine(
            onLevel = { level = it },
            onNote = { note = it },
            onRecordingSaved = { savedTick++ },
        )
    }

    // Start monitoring, recording to a new file if the user keeps recordings on.
    fun startEngine() {
        val target = if (settings.saveRecordings) recordingStore.newFile() else null
        engine.start(target)
        running = engine.isRunning
    }

    // Push harmony and autotune settings into the engine whenever they change.
    LaunchedEffect(settings) {
        engine.voiceAEnabled = settings.primaryEnabled
        engine.voiceASemitones = settings.primarySemitones
        engine.voiceALevel = settings.primaryLevel
        engine.voiceBEnabled = settings.secondEnabled
        engine.voiceBSemitones = settings.secondSemitones
        engine.voiceBLevel = settings.secondLevel
        engine.autotuneEnabled = settings.autotuneEnabled
        engine.autotuneKey = settings.autotuneKey
        engine.autotuneScale = settings.autotuneScale
        engine.retuneSpeed = settings.retuneSpeed
        engine.humanizer = settings.humanizer
    }

    // Keep the screen awake while singing, if the user asked for it.
    SideEffect {
        view.keepScreenOn = settings.keepScreenOn && running
    }

    DisposableEffect(Unit) {
        onDispose { engine.stop() }
    }

    // System back closes an open screen first, rather than leaving the app.
    BackHandler(enabled = showSettings) { showSettings = false }
    BackHandler(enabled = showRecordings) { showRecordings = false }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPermission = granted
        if (granted) startEngine()
    }

    fun toggle() {
        if (!hasPermission) {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            return
        }
        if (settings.haptics) haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        if (running) {
            engine.stop()
            running = false
        } else {
            startEngine()
        }
    }

    HarmonizerScreen(
        running = running,
        hasPermission = hasPermission,
        level = level,
        note = note,
        primaryEnabled = settings.primaryEnabled,
        primarySemitones = settings.primarySemitones,
        secondEnabled = settings.secondEnabled,
        secondSemitones = settings.secondSemitones,
        primaryLevel = settings.primaryLevel,
        recording = running && settings.saveRecordings,
        savedHintVisible = savedHintVisible,
        largeText = settings.largeText,
        highContrast = settings.highContrast,
        reduceMotion = settings.reduceMotion,
        onToggle = { toggle() },
        onPrimaryLevelChange = {
            onSettingsChange(settings.copy(primaryLevel = it))
        },
        onOpenSettings = { showSettings = true },
        onOpenRecordings = { showRecordings = true },
    )

    AnimatedVisibility(
        visible = showSettings,
        enter = fadeIn(),
        exit = fadeOut(),
    ) {
        SettingsScreen(
            settings = settings,
            onChange = onSettingsChange,
            onBack = { showSettings = false },
        )
    }

    // Composed only while open, so the list is fresh each time.
    if (showRecordings) {
        RecordingsScreen(
            store = recordingStore,
            onBack = { showRecordings = false },
        )
    }
}
