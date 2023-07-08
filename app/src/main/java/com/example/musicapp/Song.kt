package com.example.musicapp

import android.net.Uri

data class Song(
    val mediaId: String,
    val name: String,
    val image: Uri,
    val artist: String,
    val album: String,
    val durationMs: Long,
)
