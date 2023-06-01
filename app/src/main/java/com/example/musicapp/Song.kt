package com.example.musicapp

import android.net.Uri

data class Song(
    val name: String,
    val image: Uri,
    val artist: String,
    val album: String,
    val duration: Double,
) {
    val mediaId = image.toString()
}
