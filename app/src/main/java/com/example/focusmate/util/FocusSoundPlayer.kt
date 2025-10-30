package com.example.focusmate.util

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.util.Log
import androidx.core.net.toUri

class FocusSoundPlayer(private val context: Context) {

    private var mediaPlayer: MediaPlayer? = null
    private var currentResourceId: Int = 0
    private var currentVolume: Float = 0.7f

    companion object {
        private const val TAG = "FocusSoundPlayer"
    }

    fun playDemo(resourceId: Int, volume: Float = 0.7f) {
        try {
            stopDemo()

            if (resourceId == 0) return

            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )

                setDataSource(context,
                    "android.resource://${context.packageName}/$resourceId".toUri())
                setVolume(volume, volume)
                isLooping = false

                setOnCompletionListener {
                    release()
                    mediaPlayer = null
                }

                prepare()
                start()
            }

            Log.d(TAG, "Playing demo sound: $resourceId")
        } catch (e: Exception) {
            Log.e(TAG, "Error playing demo sound", e)
            stopDemo()
        }
    }

    fun stopDemo() {
        try {
            mediaPlayer?.apply {
                if (isPlaying) {
                    stop()
                }
                release()
            }
            mediaPlayer = null
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping demo", e)
        }
    }


    fun playBackground(resourceId: Int, volume: Float = 0.7f) {
        try {

            if (resourceId == 0) {
                stopBackground()
                return
            }

            if (resourceId == currentResourceId && mediaPlayer != null && mediaPlayer!!.isPlaying) {
                currentVolume = volume
                mediaPlayer?.setVolume(volume, volume)
                Log.d(TAG, "Only updating volume for existing sound: $resourceId")
                return
            }

            stopBackground()

            currentResourceId = resourceId
            currentVolume = volume

            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )

                setDataSource(context,
                    "android.resource://${context.packageName}/$resourceId".toUri())
                setVolume(volume, volume)
                isLooping = true

                setOnErrorListener { mp, what, extra ->
                    Log.e(TAG, "MediaPlayer error: what=$what, extra=$extra")
                    true
                }

                prepare()
                start()
            }

            Log.d(TAG, "Playing background sound: $resourceId with volume: $volume")
        } catch (e: Exception) {
            Log.e(TAG, "Error playing background sound", e)
            stopBackground()
        }
    }

    fun stopBackground() {
        try {
            mediaPlayer?.apply {
                if (isPlaying) {
                    stop()
                }
                release()
            }
            mediaPlayer = null
            currentResourceId = 0
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping background", e)
        }
    }

    fun pause() {
        try {
            mediaPlayer?.pause()
        } catch (e: Exception) {
            Log.e(TAG, "Error pausing", e)
        }
    }

    fun resume() {
        try {
            mediaPlayer?.start()
        } catch (e: Exception) {
            Log.e(TAG, "Error resuming", e)
        }
    }

    fun isPlaying(): Boolean {
        return try {
            mediaPlayer?.isPlaying ?: false
        } catch (e: Exception) {
            false
        }
    }

    fun setVolume(volume: Float) {
        try {
            currentVolume = volume
            mediaPlayer?.setVolume(volume, volume)
        } catch (e: Exception) {
            Log.e(TAG, "Error setting volume", e)
        }
    }

    fun release() {
        stopBackground()
        stopDemo()
    }
}