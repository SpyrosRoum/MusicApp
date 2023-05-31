package com.example.musicapp

import android.os.Bundle
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

    private lateinit var songsList: ArrayList<Song>
    private lateinit var currentSong: Song

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.music_player)

        val songIndex = intent.getIntExtra("songIndex", -1)
        if (songIndex == -1)
            Log.e(TAG, "Missing song index from intent")

        songsList = getSongs(contentResolver)
        currentSong = songsList[songIndex]

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

        Log.i(TAG, "Starting player with song #$songIndex: $currentSong")
    }
}
