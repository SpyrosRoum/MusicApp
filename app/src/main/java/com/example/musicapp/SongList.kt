package com.example.musicapp

import android.content.pm.PackageManager
import android.os.Bundle

import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.LinearLayoutManager
import android.provider.MediaStore
import android.net.Uri
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class SongList : AppCompatActivity() {
    private companion object {
        var TAG = "ActivityList"
    }

    private lateinit var recyclerView: RecyclerView
    private lateinit var viewAdapter: RecyclerView.Adapter<*>
    private lateinit var viewManager: RecyclerView.LayoutManager
    private lateinit var songsList: ArrayList<Song>
    private val PERMISSIONS_REQUEST_CODE = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.i(TAG, "ActivityList Created")

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_list)

        checkPermissions()

        songsList = try {
            getSongs()
        } catch (e: SongCursorException) {
            Log.e(TAG, "Opening cursor failed", e)
            ArrayList()
        } catch (_: NoSongsFoundException) {
            Log.w(TAG, "No Songs found")
            ArrayList()
        }

        viewManager = LinearLayoutManager(this)
        viewAdapter = SongsAdapter(songsList)


        recyclerView = findViewById<RecyclerView>(R.id.RecyclerView).apply {
            setHasFixedSize(true)
            layoutManager = viewManager
            adapter = viewAdapter
        }
    }

    /**
     * Get the list of songs found on the phone
     */
    private fun getSongs(): ArrayList<Song> {
        val songs = ArrayList<Song>()

        val uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        // What columns we want from the DB
        val wantedColumns = arrayOf(
            MediaStore.Audio.Media.DISPLAY_NAME,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DATA, // For Image
            MediaStore.Audio.Media.ARTIST,
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

            songs.add(Song(songName, imageUrl, artistName, albumName, duration))
        } while (songsCursor.moveToNext())

        songsCursor.close()
        Log.d(TAG, "Found: "+ songs.count() + " songs")
        return songs
    }

    private fun checkPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_AUDIO) // TODO not finding permissions
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.READ_MEDIA_AUDIO),
                PERMISSIONS_REQUEST_CODE
            )
        } else {
            // Permission has already been granted
            getSongs()
        }
    }


    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            PERMISSIONS_REQUEST_CODE -> {
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    // Permission was granted
                    getSongs()
                } else {
                    // Permission denied
                    Toast.makeText(this, "Permission denied to read your External storage", Toast)
                }
            }
        }
    }
}
