package com.example.musicapp

import android.Manifest
import android.os.Bundle
import android.os.PersistableBundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class SongPlayer : AppCompatActivity() {
     var list: ArrayList<Song> = ArrayList()
    private lateinit var songImage: ImageView
    private lateinit var songTitle: TextView
    private lateinit var songArtist: TextView
    private lateinit var previousButton: ImageButton
    private lateinit var nextButton: ImageButton
    private lateinit var playpauseButton: ImageButton
    private lateinit var shuffleButton: ImageButton
    private lateinit var loopButton: ImageButton
    private lateinit var seekBar: SeekBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.music_player)

//        list = 
    }
}