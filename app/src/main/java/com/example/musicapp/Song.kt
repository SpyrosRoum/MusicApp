package com.example.musicapp

import android.net.Uri
import android.support.v4.media.MediaBrowserCompat.MediaItem
import android.support.v4.media.MediaDescriptionCompat
import androidx.core.os.bundleOf

data class Song(
    val mediaId: String,
    val name: String,
    val mediaUri: Uri,
    val artist: String,
    val album: String,
    val durationMs: Long,
) {
    fun toMediaItem(): MediaItem {
        val mediaDescription = MediaDescriptionCompat.Builder()
            .setTitle(name)
            .setSubtitle(artist)
            .setDescription(album)
            .setMediaUri(mediaUri)
            .setMediaId(mediaId)
            .setExtras(
                bundleOf(
                    Pair("duration", durationMs)
                )
            )
            .build()

        return MediaItem(mediaDescription, MediaItem.FLAG_PLAYABLE)
    }
}
