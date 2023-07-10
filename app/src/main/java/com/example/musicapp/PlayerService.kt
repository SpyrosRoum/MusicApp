package com.example.musicapp

import android.media.MediaPlayer
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat.MediaItem
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import androidx.core.os.bundleOf
import androidx.media.MediaBrowserServiceCompat


class PlayerService : MediaBrowserServiceCompat() {
    private companion object {
        const val TAG = "PlayerService"
        const val MEDIA_ROOT_ID = "empty_root_id"
    }

    private lateinit var mediaSession: MediaSessionCompat
    private lateinit var mediaPlayer: MediaPlayer

    private val songList: ArrayList<MediaItem> = arrayListOf()

    private var pbState = -1

    override fun onCreate() {
        super.onCreate()

        mediaPlayer = MediaPlayer()
        mediaPlayer.setOnPreparedListener(mediaPlayerCallbacks)
        mediaPlayer.setOnCompletionListener(mediaPlayerCallbacks)

        // Create a MediaSessionCompat
        mediaSession = MediaSessionCompat(baseContext, TAG).apply {
            setCallback(mediaPlayerCallbacks)

            // Set the session's token so that client activities can communicate with it.
            setSessionToken(sessionToken)

            isActive = true
        }

        val songs = try {
            getSongs(contentResolver)
        } catch (e: NoSongsFoundException) {
            ArrayList()
        }

        songs.forEach {
            songList.add(it.toMediaItem())
        }

        // We set the state to paused because it's closer to what we support
        updatePlayBackState(PlaybackStateCompat.STATE_PAUSED)
    }

    /**
     * We always return the root id as we don't support submenus like playlists
     */
    override fun onGetRoot(
        clientPackageName: String,
        clientUid: Int,
        rootHints: Bundle?
    ): BrowserRoot {
        return BrowserRoot(MEDIA_ROOT_ID, null)
    }

    override fun onLoadChildren(
        parentMediaId: String,
        result: Result<List<MediaItem>>
    ) {
        if (parentMediaId != MEDIA_ROOT_ID) {
            // We only support the root media id
            result.sendResult(null)
            return
        }

        result.sendResult(songList)
    }

    override fun onDestroy() {
        mediaPlayer.release()

        mediaSession.run {
            isActive = false
            release()
        }
    }

    override fun onLoadItem(itemId: String?, result: Result<MediaItem>) {
        if (itemId == null) {
            result.sendResult(null)
            return
        }

        val mediaItem = songList.find { it.mediaId == itemId }
        result.sendResult(mediaItem)
    }

    /*
     * Return what commands our service supports based on the current playback state
     *
     */
    private fun supportedActions(): Long {
        val commonActions = (
                PlaybackStateCompat.ACTION_PLAY_FROM_MEDIA_ID
                        or PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS
                        or PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS
                        or PlaybackStateCompat.ACTION_SET_SHUFFLE_MODE
                        or PlaybackStateCompat.ACTION_SET_REPEAT_MODE
                )

        val additionalActions = when (pbState) {
            PlaybackStateCompat.STATE_PLAYING -> (
                    PlaybackStateCompat.ACTION_PAUSE
                            or PlaybackStateCompat.ACTION_STOP
                            or PlaybackStateCompat.ACTION_SEEK_TO
                    )

            PlaybackStateCompat.STATE_PAUSED -> (
                    PlaybackStateCompat.ACTION_PLAY
                            or PlaybackStateCompat.ACTION_SEEK_TO
                            or PlaybackStateCompat.ACTION_STOP
                    )

            PlaybackStateCompat.STATE_STOPPED -> (
                    PlaybackStateCompat.ACTION_PLAY
                            or PlaybackStateCompat.ACTION_PAUSE
                    )

            else -> (
                    PlaybackStateCompat.ACTION_PLAY
                            or PlaybackStateCompat.ACTION_PLAY_PAUSE
                            or PlaybackStateCompat.ACTION_STOP
                            or PlaybackStateCompat.ACTION_PAUSE
                    )
        }

        return commonActions or additionalActions
    }

    private fun updatePlayBackState(newState: Int) {
        pbState = newState

        val pbState = PlaybackStateCompat.Builder()
        pbState.setState(newState, mediaPlayer.currentPosition.toLong(), 1.0f)
            .setActions(supportedActions())

        mediaSession.setPlaybackState(pbState.build())
    }

    private val mediaPlayerCallbacks =
        object : MediaSessionCompat.Callback(), MediaPlayer.OnPreparedListener,
            MediaPlayer.OnCompletionListener {
            private var currentSong = -1

            // We repeat the song list by default, so we can toggle between
            // repeating the list or repeating the one song
            private var repeatSong = false

            private fun updateMeta() {
                val mediaItem = songList[currentSong]

                val meta = MediaMetadataCompat.Builder()
                    .putText(
                        MediaMetadataCompat.METADATA_KEY_DISPLAY_TITLE,
                        mediaItem.description.title
                    )
                    .putText(
                        MediaMetadataCompat.METADATA_KEY_ARTIST,
                        mediaItem.description.subtitle
                    )
                    .putText(
                        MediaMetadataCompat.METADATA_KEY_MEDIA_URI,
                        mediaItem.description.mediaUri.toString()
                    )
                    .putLong(
                        MediaMetadataCompat.METADATA_KEY_DURATION,
                        (mediaItem.description.extras ?: bundleOf(
                            Pair(
                                "duration",
                                -1L
                            )
                        )).getLong("duration")
                    )

                mediaSession.setMetadata(meta.build())
            }

            private fun playCurrentIndex() {

                // We assume always valid index
                val mediaItem = songList[currentSong]

                val mediaUri = mediaItem.description.mediaUri ?: return

                updateMeta()
                mediaPlayer.reset()
                mediaPlayer.setDataSource(mediaUri.toString())
                mediaPlayer.prepareAsync()
            }

            override fun onPlayFromMediaId(mediaId: String?, extras: Bundle?) {
                super.onPlayFromMediaId(mediaId, extras)

                val mediaItem = songList.withIndex().find { (_, item) -> item.mediaId == mediaId }
                mediaItem?.let { (i, _) ->
                    currentSong = i
                    playCurrentIndex()
                }

            }

            override fun onSeekTo(pos: Long) {
                super.onSeekTo(pos)

                mediaPlayer.seekTo(pos.toInt())
                // We need to update the playback state because the progress changed
                updatePlayBackState(pbState)
            }

            override fun onSkipToNext() {
                super.onSkipToNext()

                if (currentSong == songList.size - 1) {
                    currentSong = 0
                } else
                    currentSong += 1

                playCurrentIndex()
            }

            override fun onSkipToPrevious() {
                super.onSkipToPrevious()

                if (repeatSong || mediaPlayer.currentPosition >= 1000) {
                    playCurrentIndex()
                    return
                }

                if (currentSong == 0) {
                    currentSong = songList.size - 1
                } else
                    currentSong -= 1

                playCurrentIndex()
            }

            override fun onPlay() {
                super.onPlay()

                if (!mediaPlayer.isPlaying) {
                    mediaPlayer.start()
                    updatePlayBackState(PlaybackStateCompat.STATE_PLAYING)
                }
            }

            override fun onSetRepeatMode(repeatMode: Int) {
                super.onSetRepeatMode(repeatMode)

                when (repeatMode) {
                    PlaybackStateCompat.REPEAT_MODE_ALL -> repeatSong = false
                    PlaybackStateCompat.REPEAT_MODE_ONE -> repeatSong = true
                    else -> Log.w(TAG, "Ignoring set repeat mode: $repeatMode")
                }
            }

            override fun onPause() {
                super.onPause()

                if (mediaPlayer.isPlaying) {
                    mediaPlayer.pause()
                    updatePlayBackState(PlaybackStateCompat.STATE_PAUSED)
                }
            }

            override fun onCustomAction(action: String?, extras: Bundle?) {
                super.onCustomAction(action, extras)

                if (action == "TogglePlay") {
                    if (mediaPlayer.isPlaying) {
                        onPause()
                    } else {
                        onPlay()
                    }
                } else if (action == "ToggleRepeat") {
                    if (repeatSong)
                        onSetRepeatMode(PlaybackStateCompat.REPEAT_MODE_ALL)
                    else
                        onSetRepeatMode(PlaybackStateCompat.REPEAT_MODE_ONE)
                }
            }

            override fun onPrepared(mp: MediaPlayer?) {
                updatePlayBackState(PlaybackStateCompat.STATE_PLAYING)
                mediaPlayer.start()
            }

            override fun onCompletion(mp: MediaPlayer?) {
                if (!repeatSong)
                    if (currentSong == songList.size - 1)
                        currentSong = 0
                    else
                        currentSong += 1

                playCurrentIndex()
            }
        }
}
