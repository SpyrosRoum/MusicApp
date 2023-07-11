package com.example.musicapp

import android.animation.ValueAnimator
import android.content.Context
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.AttributeSet
import android.view.animation.LinearInterpolator
import android.widget.SeekBar
import androidx.appcompat.widget.AppCompatSeekBar

class MediaSeekbar : AppCompatSeekBar {
    private var userIsSeeking = false
    var mediaController: MediaControllerCompat? = null
        set(mediaController) {
            // If media controller is not null, register callbacks,
            // else if the current field is not null, unregister the callbacks
            if (mediaController != null) {
                mediaController.registerCallback(controllerCallback)
            } else field?.unregisterCallback(controllerCallback)

            field = mediaController
        }

    private var progressAnimator: ValueAnimator? = null

    private val onSeekBackChangeListener = object : OnSeekBarChangeListener {
        override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {}

        override fun onStartTrackingTouch(seekBar: SeekBar?) {
            userIsSeeking = true
        }

        override fun onStopTrackingTouch(seekBar: SeekBar?) {
            userIsSeeking = false
            mediaController?.transportControls?.seekTo(progress.toLong())
        }
    }

    constructor(ctx: Context) : super(ctx) {
        super.setOnSeekBarChangeListener(onSeekBackChangeListener)
    }

    constructor(ctx: Context, attrs: AttributeSet) : super(ctx, attrs) {
        super.setOnSeekBarChangeListener(onSeekBackChangeListener)
    }

    constructor(ctx: Context, attrs: AttributeSet, defStyleAttr: Int) : super(
        ctx, attrs, defStyleAttr
    ) {
        super.setOnSeekBarChangeListener(onSeekBackChangeListener)
    }

    fun disconnectController() {
        mediaController?.run { unregisterCallback(controllerCallback) }
        mediaController = null
    }

    private val controllerCallback =
        object : MediaControllerCompat.Callback(), ValueAnimator.AnimatorUpdateListener {

            override fun onPlaybackStateChanged(state: PlaybackStateCompat) {
                super.onPlaybackStateChanged(state)

                // If there's an ongoing animation, stop it now.
                progressAnimator?.apply { cancel() }
                progressAnimator = null

                val progress = state.position

                setProgress(progress.toInt())

                // If the media is playing then the seekbar should follow it, and the easiest
                // way to do that is to create a ValueAnimator to update it so the bar reaches
                // the end of the media the same time as playback gets there (or close enough).
                if (state.state == PlaybackStateCompat.STATE_PLAYING) {
                    val timeToEnd = max - progress

                    progressAnimator =
                        ValueAnimator.ofInt(progress.toInt(), max).setDuration(timeToEnd)

                    val updateListener = this
                    progressAnimator?.apply {
                        interpolator = LinearInterpolator()
                        addUpdateListener(updateListener)
                        start()
                    }
                }
            }

            override fun onMetadataChanged(metadata: MediaMetadataCompat) {
                super.onMetadataChanged(metadata)

                val m = metadata.getLong(MediaMetadataCompat.METADATA_KEY_DURATION).toInt()

                progress = 0
                max = m
            }

            override fun onAnimationUpdate(animation: ValueAnimator) {
                // If the user is changing the slider, cancel the animation.
                if (userIsSeeking) {
                    animation.cancel()
                    return
                }
                val animatedIntValue = animation.animatedValue as Int

                progress = animatedIntValue
            }

        }
}
