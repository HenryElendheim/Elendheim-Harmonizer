package com.elendheim.harmonizer.ui

import android.media.MediaPlayer
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.elendheim.harmonizer.R
import com.elendheim.harmonizer.Recording
import com.elendheim.harmonizer.RecordingStore
import com.elendheim.harmonizer.ui.theme.FifthAccent
import com.elendheim.harmonizer.ui.theme.Muted
import com.elendheim.harmonizer.ui.theme.VoiceAccent

/**
 * The list of saved recordings: play one back, export it to your files as a
 * .wav, or delete it. Each row shows when it was recorded and how long it is.
 */
@Composable
fun RecordingsScreen(
    store: RecordingStore,
    onBack: () -> Unit,
) {
    val context = LocalContext.current

    var refresh by remember { mutableIntStateOf(0) }
    val recordings = remember(refresh) { store.list() }

    val player = remember { MediaPlayer() }
    var playingPath by remember { mutableStateOf<String?>(null) }

    fun stopPlayback() {
        runCatching { if (player.isPlaying) player.stop() }
        runCatching { player.reset() }
        playingPath = null
    }

    fun play(recording: Recording) {
        stopPlayback()
        runCatching {
            player.setDataSource(recording.file.path)
            player.prepare()
            player.start()
            playingPath = recording.file.path
            player.setOnCompletionListener { playingPath = null }
        }
    }

    DisposableEffect(Unit) {
        onDispose { runCatching { player.release() } }
    }

    var exportTarget by remember { mutableStateOf<Recording?>(null) }
    val exporter = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("audio/x-wav")
    ) { uri ->
        val target = exportTarget
        exportTarget = null
        if (uri != null && target != null) {
            runCatching {
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    target.file.inputStream().use { it.copyTo(out) }
                }
            }
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(horizontal = 20.dp),
        ) {
            RecordingsTopBar(onBack = onBack)

            if (recordings.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "No recordings yet.\nSing something and it'll show up here.",
                        color = Muted,
                        fontSize = 16.sp,
                    )
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(recordings, key = { it.file.path }) { recording ->
                        RecordingRow(
                            recording = recording,
                            isPlaying = playingPath == recording.file.path,
                            onPlayToggle = {
                                if (playingPath == recording.file.path) stopPlayback() else play(recording)
                            },
                            onExport = {
                                exportTarget = recording
                                exporter.launch(recording.exportName)
                            },
                            onDelete = {
                                if (playingPath == recording.file.path) stopPlayback()
                                store.delete(recording)
                                refresh++
                            },
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.surface)
                    }
                }
            }
        }
    }
}

@Composable
private fun RecordingsTopBar(onBack: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
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
        Text(
            text = "Recordings",
            modifier = Modifier.align(Alignment.Center),
            fontSize = 20.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onBackground,
        )
    }
}

@Composable
private fun RecordingRow(
    recording: Recording,
    isPlaying: Boolean,
    onPlayToggle: () -> Unit,
    onExport: () -> Unit,
    onDelete: () -> Unit,
) {
    Column(modifier = Modifier.padding(vertical = 12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = recording.displayDate,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                )
                Text(
                    text = recording.displayDuration,
                    color = Muted,
                    fontSize = 14.sp,
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            TextButton(onClick = onPlayToggle) {
                Text(if (isPlaying) "Stop" else "Play", color = VoiceAccent, fontWeight = FontWeight.SemiBold)
            }
            TextButton(onClick = onExport) {
                Text("Export", color = FifthAccent, fontWeight = FontWeight.SemiBold)
            }
            TextButton(onClick = onDelete) {
                Text("Delete", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
