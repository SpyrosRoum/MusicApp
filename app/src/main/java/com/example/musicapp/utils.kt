package com.example.musicapp

import android.content.ContentResolver
import android.net.Uri
import android.provider.MediaStore

/**
 * Get the list of songs found on the phone
 */
fun getSongs(contentResolver: ContentResolver): ArrayList<Song> {
    val songs = ArrayList<Song>()

    val uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
    // What columns we want from the DB
    val wantedColumns = arrayOf(
        MediaStore.Audio.Media.DISPLAY_NAME,
        MediaStore.Audio.Media.ALBUM,
        MediaStore.Audio.Media.DURATION,
        MediaStore.Audio.Media.DATA, // For Image
        MediaStore.Audio.Media.ARTIST,
        MediaStore.Audio.Media._ID,
    )

    val songsCursor = contentResolver.query(
        uri,
        wantedColumns,
        null,
        null,
        MediaStore.Audio.Media.DISPLAY_NAME  // How to sort the resulting rows
    ) ?: throw SongCursorException("Failed to get song cursor")

    if (!songsCursor.moveToFirst()) {
        // There are no songs
        songsCursor.close()
        throw NoSongsFoundException()
    }

    do {
        val songName =
            songsCursor.getString(songsCursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME))
        val artistName =
            songsCursor.getString(songsCursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST))
        val albumName =
            songsCursor.getString(songsCursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM))
        val imageUrl =
            Uri.parse(songsCursor.getString(songsCursor.getColumnIndexOrThrow((MediaStore.Audio.Media.DATA))))
        val duration =
            songsCursor.getDouble(songsCursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION))
        val mediaId =
            songsCursor.getLong(songsCursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID))

        songs.add(Song(mediaId.toString(), songName, imageUrl, artistName, albumName, duration))
    } while (songsCursor.moveToNext())

    songsCursor.close()
    return songs
}
