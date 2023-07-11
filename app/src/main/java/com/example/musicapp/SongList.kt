package com.example.musicapp


import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView


class SongList : AppCompatActivity() {
    private companion object {
        const val TAG = "SongList"
    }

    private lateinit var recyclerView: RecyclerView
    private lateinit var viewAdapter: RecyclerView.Adapter<*>
    private lateinit var viewManager: RecyclerView.LayoutManager

    private lateinit var mediaBrowser: MediaBrowserCompat

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_list)

        val intent = Intent(this, PlayerService::class.java)
        startService(intent)

        // Create MediaBrowserServiceCompat
        mediaBrowser = MediaBrowserCompat(
            this,
            ComponentName(this, PlayerService::class.java),
            mediaBrowserConnectionCallbacks,
            null // optional Bundle
        )
    }

    public override fun onStart() {
        super.onStart()
        mediaBrowser.connect()
    }

    private val mediaBrowserConnectionCallbacks = object : MediaBrowserCompat.ConnectionCallback() {

        override fun onConnected() {
            mediaBrowser.subscribe(mediaBrowser.root,
                object : MediaBrowserCompat.SubscriptionCallback() {
                    override fun onChildrenLoaded(
                        parentId: String, children: MutableList<MediaBrowserCompat.MediaItem>
                    ) {
                        super.onChildrenLoaded(parentId, children)

                        Log.i(TAG, "Got ${children.count()} songs")

                        viewManager = LinearLayoutManager(this@SongList)
                        viewAdapter = SongsAdapter(children, this@SongList)
                        recyclerView = findViewById<RecyclerView>(R.id.RecyclerView).apply {
                            setHasFixedSize(true)
                            layoutManager = viewManager
                            adapter = viewAdapter
                        }
                    }
                })
        }

        override fun onConnectionFailed() {
            Log.e(TAG, "Something went wrong")
        }
    }

    public override fun onStop() {
        super.onStop()
        mediaBrowser.disconnect()
    }
}
