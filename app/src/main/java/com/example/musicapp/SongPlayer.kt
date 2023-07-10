package com.example.musicapp

import android.content.ComponentName
import android.content.Intent
import android.graphics.BitmapFactory
import android.media.AudioManager
import android.media.MediaMetadataRetriever
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import android.widget.ImageButton
import android.widget.ImageView
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
    private lateinit var seekBar: MediaSeekbar
    private lateinit var currentTimestampTV: TextView
    private lateinit var songDuration: TextView

    // The current ts we are at
    private var currentTimestampMs = 0L

    // A handle to the main (UI) thread so we can post jobs
    private val uiThreadHandler = Handler(Looper.getMainLooper())

    // If we have a job that updates the current ts or not
    private var progressTsUpdating = false

    // A Runnable job that updates the current ts.
    // It also updates the corresponding TextView so it needs to run in the UI thread
    private val progressTsUpdater = object : Runnable {
        override fun run() {
            currentTimestampMs += 1000
            currentTimestampTV.text = msToDurationStr(currentTimestampMs)

            uiThreadHandler.postDelayed(this, 1000)
        }
    }

    // Used to get artwork for the current song
    private val metadataRetriever = MediaMetadataRetriever()

    private var currentMediaId: String? = null

    private lateinit var mediaBrowser: MediaBrowserCompat
    private var currentImageIndex = 0


    private val imagesShuffle = listOf(R.drawable.shuffle_1, R.drawable.shufflePressed)
    private val imagesPlayPause =
        listOf(android.R.drawable.ic_media_pause, android.R.drawable.ic_media_play)
    private val imagesLoop = listOf(R.drawable.loop, R.drawable.androidLoopPressed)

    private val mediaController get() = MediaControllerCompat.getMediaController(this@SongPlayer)

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
        mediaBrowser.connect()

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

            seekBar.mediaController = mediaController
            mediaController.registerCallback(mediaControllerCallback)
            buildMediaControls()
            currentMediaId?.let { mediaController.transportControls.playFromMediaId(it, null) }
        }

        override fun onConnectionFailed() {
            Log.e(TAG, "Something went wrong")
        }
    }

    private val mediaControllerCallback = object : MediaControllerCompat.Callback() {
        override fun onMetadataChanged(metadata: MediaMetadataCompat) {
            super.onMetadataChanged(metadata)

            buildUiFromMeta(metadata)
        }

        override fun onPlaybackStateChanged(state: PlaybackStateCompat) {
            super.onPlaybackStateChanged(state)

            currentTimestampMs = state.position

            if (state.state == PlaybackStateCompat.STATE_PLAYING && !progressTsUpdating) {
                progressTsUpdating = true
                uiThreadHandler.post(progressTsUpdater)
            } else if (state.state == PlaybackStateCompat.STATE_PAUSED && progressTsUpdating) {
                progressTsUpdating = false
                uiThreadHandler.removeCallbacks(progressTsUpdater)
            }
        }

        override fun onSessionDestroyed() {
            mediaBrowser.disconnect()
        }
    }

    private fun buildUiFromMeta(meta: MediaMetadataCompat) {
        songTitle.text = meta.getText(MediaMetadataCompat.METADATA_KEY_DISPLAY_TITLE)
        songArtist.text = meta.getText(MediaMetadataCompat.METADATA_KEY_ARTIST)

        val durationMs = meta.getLong(MediaMetadataCompat.METADATA_KEY_DURATION)
        val durationStr = msToDurationStr(durationMs)

        songDuration.text = durationStr

        // Set the image, if found
        val mediaUri = meta.getText(MediaMetadataCompat.METADATA_KEY_MEDIA_URI)

        metadataRetriever.setDataSource(mediaUri.toString())
        metadataRetriever.embeddedPicture?.let {
            val bmp = BitmapFactory.decodeByteArray(
                it,
                0,
                it.size
            )

            songImage.setImageBitmap(bmp)
        }
    }

    private fun buildMediaControls() {
        playPauseButton.apply {
            setOnClickListener {
                // Update the index of the current image
                currentImageIndex = (currentImageIndex + 1) % imagesPlayPause.size
                // Set the new image
                playPauseButton.setImageResource(imagesPlayPause[currentImageIndex])

                mediaController.transportControls.sendCustomAction("TogglePlay", Bundle.EMPTY)
            }
        }

        previousButton.apply {
            setOnClickListener {
                mediaController.transportControls.skipToPrevious()

                // Set the correct play/pause image
                currentImageIndex = 0
                playPauseButton.setImageResource(imagesPlayPause[currentImageIndex])
            }
        }

        nextButton.apply {
            setOnClickListener {
                mediaController.transportControls.skipToNext()

                // Set the correct play/pause image
                currentImageIndex = 0
                playPauseButton.setImageResource(imagesPlayPause[currentImageIndex])
            }
        }

        shuffleButton.apply {
            setOnClickListener {
                mediaController.transportControls.setShuffleMode()

                // Set the correct shuffle image
                currentImageIndex = 0
                shuffleButton.setImageResource(imagesShuffle[currentImageIndex])
            }
        }

        loopButton.apply {
            setOnClickListener {
                mediaController.transportControls.setRepeatMode()

                // Set the correct loop image
                currentImageIndex = 0
                loopButton.setImageResource(imagesLoop[currentImageIndex])
            }
        }
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
        currentTimestampTV = findViewById(R.id.current_duration)
        songDuration = findViewById(R.id.total_duration)
    }

    public override fun onStop() {
        super.onStop()
        seekBar.disconnectController()
        // (see "stay in sync with the MediaSession")
        mediaController.unregisterCallback(mediaControllerCallback)
        mediaBrowser.disconnect()
    }
}
