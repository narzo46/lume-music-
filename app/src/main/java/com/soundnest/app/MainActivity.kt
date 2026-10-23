package com.soundnest.app

import android.Manifest
import android.content.ContentUris
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private data class Song(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long,
    val uri: String
)

class MainActivity : ComponentActivity() {
    private var mediaPlayer: MediaPlayer? = null
    private var isPlaying by mutableStateOf(false)
    private var currentSong by mutableStateOf<Song?>(null)
    private var songs by mutableStateOf<List<Song>>(emptyList())
    private var currentIndex by mutableStateOf(0)
    private var currentPosition by mutableStateOf(0)
    private var searchText by mutableStateOf("")

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) loadSongs()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_AUDIO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

        if (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED) {
            loadSongs()
        } else {
            requestPermissionLauncher.launch(permission)
        }

        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    primary = Color(0xFF1ED760),
                    background = Color(0xFF0D1117),
                    surface = Color(0xFF151B23),
                    onBackground = Color.White,
                    onSurface = Color.White
                )
            ) {
                SoundNestScreen(
                    songs = songs.filter {
                        it.title.contains(searchText, ignoreCase = true) ||
                            it.artist.contains(searchText, ignoreCase = true)
                    },
                    searchText = searchText,
                    currentSong = currentSong,
                    isPlaying = isPlaying,
                    currentPosition = currentPosition,
                    onPlaySong = { index ->
                        val track = songs[index]
                        playTrack(track, index)
                    },
                    onTogglePlay = {
                        if (currentSong == null) {
                            if (songs.isNotEmpty()) playTrack(songs.first(), 0)
                        } else if (isPlaying) {
                            pause()
                        } else {
                            resume()
                        }
                    },
                    onNext = {
                        if (songs.isNotEmpty()) {
                            val next = (currentIndex + 1) % songs.size
                            playTrack(songs[next], next)
                        }
                    },
                    onPrevious = {
                        if (songs.isNotEmpty()) {
                            val prev = (currentIndex - 1 + songs.size) % songs.size
                            playTrack(songs[prev], prev)
                        }
                    },
                    onSearchChanged = { value ->
                        searchText = value
                    },
                    onSeek = { progress ->
                        val duration = mediaPlayer?.duration ?: 0
                        val target = (duration * progress / 100).coerceAtLeast(0)
                        mediaPlayer?.seekTo(target)
                    }
                )
            }
        }

        observeProgress()
    }

    private fun loadSongs() {
        val result = mutableListOf<Song>()
        val collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DURATION
        )

        val query = contentResolver.query(
            collection,
            projection,
            null,
            null,
            "${MediaStore.Audio.Media.TITLE} ASC"
        )

        query?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val title = cursor.getString(titleColumn) ?: "Unknown"
                val artist = cursor.getString(artistColumn) ?: "Unknown Artist"
                val album = cursor.getString(albumColumn) ?: "Local Library"
                val duration = cursor.getLong(durationColumn)
                val uri = ContentUris.withAppendedId(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    id
                ).toString()

                result.add(
                    Song(
                        id = id,
                        title = title,
                        artist = artist,
                        album = album,
                        durationMs = duration,
                        uri = uri
                    )
                )
            }
        }

        songs = result
        if (songs.isNotEmpty()) {
            currentSong = songs.first()
            currentIndex = 0
        }
    }

    private fun playTrack(track: Song, index: Int) {
        try {
            if (mediaPlayer == null) {
                mediaPlayer = MediaPlayer()
            }

            mediaPlayer?.reset()
            mediaPlayer?.setDataSource(this, Uri.parse(track.uri))
            mediaPlayer?.prepare()
            mediaPlayer?.start()

            currentSong = track
            currentIndex = index
            isPlaying = true
            currentPosition = 0
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun pause() {
        mediaPlayer?.pause()
        isPlaying = false
    }

    private fun resume() {
        mediaPlayer?.start()
        isPlaying = true
    }

    private fun observeProgress() {
        lifecycleScope.launch {
            while (true) {
                delay(250)
                if (mediaPlayer != null && mediaPlayer!!.isPlaying) {
                    val duration = mediaPlayer!!.duration
                    val position = mediaPlayer!!.currentPosition
                    currentPosition = if (duration > 0) ((position * 100f) / duration).toInt() else 0
                }
            }
        }
    }

    override fun onDestroy() {
        mediaPlayer?.release()
        mediaPlayer = null
        super.onDestroy()
    }
}

@Composable
private fun SoundNestScreen(
    songs: List<Song>,
    searchText: String,
    currentSong: Song?,
    isPlaying: Boolean,
    currentPosition: Int,
    onPlaySong: (Int) -> Unit,
    onTogglePlay: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSearchChanged: (String) -> Unit,
    onSeek: (Float) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D1117))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "SoundNest",
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )

            Surface(
                color = Color(0xFF1ED760),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "Offline",
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    color = Color.Black,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        OutlinedTextField(
            value = searchText,
            onValueChange = onSearchChanged,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Поиск по трекам") },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF1ED760),
                unfocusedBorderColor = Color(0xFF2A2F38),
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            )
        )

        Spacer(modifier = Modifier.height(20.dp))

        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color(0xFF151B23),
            shape = RoundedCornerShape(20.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(82.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color(0xFF1ED760)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("♪", fontSize = 34.sp, color = Color.Black)
                }

                Spacer(modifier = Modifier.width(18.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = currentSong?.title ?: "Выберите трек",
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = currentSong?.artist ?: "Локальная библиотека",
                        color = Color(0xFFB1BBCB),
                        fontSize = 14.sp
                    )
                }

                IconButton(onClick = {}) {
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = null,
                        tint = Color(0xFFFF5C7C)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onPrevious) {
                Icon(
                    imageVector = Icons.Default.SkipPrevious,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }

            Surface(
                color = Color(0xFF1ED760),
                shape = CircleShape,
                modifier = Modifier.size(72.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    IconButton(onClick = onTogglePlay) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = Color.Black,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            }

            IconButton(onClick = onNext) {
                Icon(
                    imageVector = Icons.Default.SkipNext,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Slider(
            value = currentPosition.toFloat(),
            onValueChange = { onSeek(it) },
            colors = SliderDefaults.colors(
                thumbColor = Color(0xFF1ED760),
                activeTrackColor = Color(0xFF1ED760),
                inactiveTrackColor = Color(0xFF2A2F38)
            )
        )

        Spacer(modifier = Modifier.height(18.dp))

        Text(
            text = "Библиотека",
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(10.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(songs) { song ->
                val idx = songs.indexOf(song)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF151B23), RoundedCornerShape(14.dp))
                        .clickable { onPlaySong(idx) }
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .background(Color(0xFF1ED760), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("♫", color = Color.Black, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(song.title, color = Color.White, fontWeight = FontWeight.SemiBold)
                        Text(song.artist, color = Color(0xFFADB7C6), fontSize = 13.sp)
                    }

                    Text(
                        text = formatDuration(song.durationMs),
                        color = Color(0xFFADB7C6),
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

private fun formatDuration(ms: Long): String {
    val totalSeconds = (ms / 1000).toInt()
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}
