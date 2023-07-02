package com.example.musicapp


import android.content.ComponentName
import android.media.AudioManager
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.session.MediaControllerCompat
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView


class SongPlayerClient : AppCompatActivity() {
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

        // Create MediaBrowserServiceCompat
        mediaBrowser = MediaBrowserCompat(
            this,
            ComponentName(this, SongPlayerService::class.java),
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
            // Get the token for the MediaSession
            mediaBrowser.sessionToken.also { token ->
                // Create a MediaControllerCompat
                val mediaController = MediaControllerCompat(
                    this@SongPlayerClient, // Context
                    token
                )
                // Save the controller
                MediaControllerCompat.setMediaController(this@SongPlayerClient, mediaController)
            }

            mediaBrowser.subscribe(
                mediaBrowser.root,
                object : MediaBrowserCompat.SubscriptionCallback() {
                    override fun onChildrenLoaded(
                        parentId: String,
                        children: MutableList<MediaBrowserCompat.MediaItem>
                    ) {
                        super.onChildrenLoaded(parentId, children)

                        Log.i(TAG, "Got ${children.count()} songs")

                        viewManager = LinearLayoutManager(this@SongPlayerClient)
                        viewAdapter = SongsAdapter(children, this@SongPlayerClient)
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
        // (see "stay in sync with the MediaSession")
        MediaControllerCompat.getMediaController(this)
            ?.unregisterCallback(object : MediaControllerCompat.Callback() {
                override fun onSessionDestroyed() {
                    mediaBrowser.disconnect()
                }
            })
        mediaBrowser.disconnect()
    }
}
