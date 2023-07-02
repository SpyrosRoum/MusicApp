package com.example.musicapp


import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat.MediaItem
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.media.MediaBrowserServiceCompat

private const val LOG_TAG = "PlayerService"
private const val MEDIA_ROOT_ID = "empty_root_id"


class SongPlayerService : MediaBrowserServiceCompat() {
    private lateinit var mediaSession: MediaSessionCompat
    private lateinit var stateBuilder: PlaybackStateCompat.Builder

    private val songList: ArrayList<MediaItem> = arrayListOf()

    override fun onCreate() {
        super.onCreate()

        // Create a MediaSessionCompat
        mediaSession = MediaSessionCompat(baseContext, LOG_TAG).apply {
            // Indicate what commands our service supports.
            // Currently we support:
            // play-pause toggle, seek, goto next/previous, repeat, and shuffle.
            stateBuilder = PlaybackStateCompat.Builder()
                .setActions(
                    PlaybackStateCompat.ACTION_PLAY_PAUSE
                            or PlaybackStateCompat.ACTION_SEEK_TO
                            or PlaybackStateCompat.ACTION_SKIP_TO_NEXT
                            or PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS
                            or PlaybackStateCompat.ACTION_SET_REPEAT_MODE
                            or PlaybackStateCompat.ACTION_SET_SHUFFLE_MODE
                )
            setPlaybackState(stateBuilder.build())

            // MySessionCallback() has methods that handle callbacks from a media controller
            // setCallback(MySessionCallback())

            // Set the session's token so that client activities can communicate with it.
            setSessionToken(sessionToken)
        }

        val songs = try {
            getSongs(contentResolver)
        } catch (e: NoSongsFoundException) {
            ArrayList()
        }

        songs.forEach {
            val mediaDescription = MediaDescriptionCompat.Builder()
                .setTitle(it.name)
                .setSubtitle(it.artist)
                .setDescription(it.album)
                .setMediaUri(it.image)
                .setMediaId(it.mediaId)
                .build()

            val mediaItem = MediaItem(mediaDescription, MediaItem.FLAG_PLAYABLE)
            songList.add(mediaItem)
        }

        mediaSession.isActive = true
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
}
