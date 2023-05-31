package com.example.musicapp

import android.content.pm.PackageManager
import android.os.Bundle
import android.Manifest
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.LinearLayoutManager
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class SongList : AppCompatActivity() {
    private companion object {
        private const val TAG = "ActivityList"
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
            getSongs(contentResolver)
        } catch (e: SongCursorException) {
            Log.e(TAG, "Opening cursor failed", e)
            ArrayList()
        } catch (_: NoSongsFoundException) {
            Log.w(TAG, "No Songs found")
            ArrayList()
        }

        viewManager = LinearLayoutManager(this)
        viewAdapter = SongsAdapter(songsList, this)


        recyclerView = findViewById<RecyclerView>(R.id.RecyclerView).apply {
            setHasFixedSize(true)
            layoutManager = viewManager
            adapter = viewAdapter
        }
    }

    private fun checkPermissions() {
        if (
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_MEDIA_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            Log.i(TAG, "Got permissions")
            return
        }

        ActivityCompat.requestPermissions(
            this, arrayOf(Manifest.permission.READ_MEDIA_AUDIO), PERMISSIONS_REQUEST_CODE
        )
        Log.i(TAG, "Requested permissions")
    }
}
