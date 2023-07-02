package com.example.musicapp

import android.content.ComponentName
import android.content.Intent
import android.media.AudioManager
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.session.MediaControllerCompat
import android.util.Log
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class SongPlayer : AppCompatActivity() {
    private companion object {
        const val TAG = "SongPlayer"
    }

    private lateinit var songImage: ImageView
    private lateinit var songTitle: TextView
    private lateinit var songArtist: TextView
    private lateinit var previousButton: ImageButton
    private lateinit var nextButton: ImageButton
    private lateinit var playPauseButton: ImageButton
    private lateinit var shuffleButton: ImageButton
    private lateinit var loopButton: ImageButton
    private lateinit var seekBar: SeekBar
    private lateinit var currentTimestamp: TextView
    private lateinit var songDuration: TextView

    private var currentMediaId: String? = null

    private lateinit var mediaBrowser: MediaBrowserCompat
    private var currentImageIndex = 0

    private val images = listOf(android.R.drawable.ic_media_pause, android.R.drawable.ic_media_play)


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.music_player)

        bindWidgets()

        mediaBrowser = MediaBrowserCompat(
            this,
            ComponentName(this, PlayerService::class.java),
            mediaBrowserConnectionCallback,
            null // optional Bundle
        )

        // We get to this screen from the song list, which means we should have the index
        // of the selected song in the intents
        currentMediaId = intent.getStringExtra("mediaId")
        if (currentMediaId == null) {
            // songIndex was missing, so log it and return to the song list
            Log.e(TAG, "Missing song index from intent")
            startActivity(Intent(this, SongList::class.java))
        }

        Log.i(TAG, "Starting player with song #$currentMediaId")
    }

    public override fun onStart() {
        super.onStart()
        mediaBrowser.connect()
    }

    public override fun onResume() {
        super.onResume()
        volumeControlStream = AudioManager.STREAM_MUSIC
    }

    private val mediaBrowserConnectionCallback = object : MediaBrowserCompat.ConnectionCallback() {
        override fun onConnected() {
            // Get the token for the MediaSession
            mediaBrowser.sessionToken.also { token ->
                // Create a MediaControllerCompat
                val mediaController = MediaControllerCompat(
                    this@SongPlayer, // Context
                    token
                )
                // Save the controller
                MediaControllerCompat.setMediaController(this@SongPlayer, mediaController)
            }

            buildMediaControls()
            currentMediaId?.let { playSong(it) }
        }

        override fun onConnectionFailed() {
            Log.e(TAG, "Something went wrong")
        }
    }

    private val mediaControllerCallback = object : MediaControllerCompat.Callback() {
        override fun onSessionDestroyed() {
            mediaBrowser.disconnect()
        }
    }

    private fun playSong(mediaId: String) {
        mediaBrowser.getItem(mediaId, object : MediaBrowserCompat.ItemCallback() {
            override fun onItemLoaded(item: MediaBrowserCompat.MediaItem?) {
                super.onItemLoaded(item)
                if (item == null) return

                songTitle.text = item.description.title
                songArtist.text = item.description.subtitle

                val mediaController = MediaControllerCompat.getMediaController(this@SongPlayer)
                mediaController.transportControls.playFromMediaId(currentMediaId, null)
            }
        })
    }

    private fun buildMediaControls() {
        val mediaController = MediaControllerCompat.getMediaController(this)
        playPauseButton.apply {
            setOnClickListener {
                // Update the index of the current image
                currentImageIndex = (currentImageIndex + 1) % images.size
                // Set the new image
                playPauseButton.setImageResource(images[currentImageIndex])

                mediaController.transportControls.sendCustomAction("TogglePlay", Bundle.EMPTY)
            }
        }

        mediaController.registerCallback(mediaControllerCallback)
    }

    /**
     * Bind the widgets of the screen to local variables
     */
    private fun bindWidgets() {
        // Syncing widgets with variables
        songImage = findViewById(R.id.playing_song_image)
        songTitle = findViewById(R.id.playing_song_title)
        songArtist = findViewById(R.id.playing_song_artist)
        previousButton = findViewById(R.id.previous_song)
        nextButton = findViewById(R.id.next_song)
        playPauseButton = findViewById(R.id.play_pause)
        shuffleButton = findViewById(R.id.shuffle_button)
        loopButton = findViewById(R.id.loop_button)
        seekBar = findViewById(R.id.playing_bar)
        currentTimestamp = findViewById(R.id.current_duration)
        songDuration = findViewById(R.id.total_duration)
    }

    public override fun onStop() {
        super.onStop()
        // (see "stay in sync with the MediaSession")
        MediaControllerCompat.getMediaController(this)
            ?.unregisterCallback(mediaControllerCallback)
        mediaBrowser.disconnect()
    }
}
