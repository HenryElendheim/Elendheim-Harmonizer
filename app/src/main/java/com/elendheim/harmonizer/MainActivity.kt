package com.elendheim.harmonizer

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.elendheim.harmonizer.audio.HarmonizerEngine
import com.elendheim.harmonizer.ui.HarmonizerScreen
import com.elendheim.harmonizer.ui.theme.ElendheimHarmonizerTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ElendheimHarmonizerTheme {
                HarmonizerApp()
            }
        }
    }
}

@androidx.compose.runtime.Composable
private fun HarmonizerApp() {
    val context = LocalContext.current

    var running by remember { mutableStateOf(false) }
    var level by remember { mutableFloatStateOf(0f) }
    var note by remember { mutableStateOf<Double?>(null) }
    var fifthLevel by remember { mutableFloatStateOf(0.85f) }
    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED
        )
    }

    // One engine for the lifetime of this screen; state changes flow back via callbacks.
    val engine = remember {
        HarmonizerEngine(
            onLevel = { level = it },
            onNote = { note = it },
        )
    }

    // Stop cleanly if the screen goes away.
    DisposableEffect(Unit) {
        onDispose {
            engine.stop()
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPermission = granted
        if (granted) {
            engine.start()
            running = engine.isRunning
        }
    }

    HarmonizerScreen(
        running = running,
        hasPermission = hasPermission,
        level = level,
        note = note,
        fifthLevel = fifthLevel,
        onToggle = {
            if (!hasPermission) {
                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                return@HarmonizerScreen
            }
            if (running) {
                engine.stop()
                running = false
            } else {
                engine.start()
                running = engine.isRunning
            }
        },
        onFifthLevelChange = {
            fifthLevel = it
            engine.fifthLevel = it
        },
    )
}
