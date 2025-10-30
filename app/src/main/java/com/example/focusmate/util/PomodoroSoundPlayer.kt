package com.example.focusmate.util

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.widget.Toast
import com.example.focusmate.R

class PomodoroSoundPlayer (context: Context) {
    private var soundPool: SoundPool? = null
    private val soundIds = mutableMapOf<SoundEvent, Int>()

    init {
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        soundPool = SoundPool.Builder()
            .setMaxStreams(1)
            .setAudioAttributes(audioAttributes)
            .build()

        soundIds[SoundEvent.END_FOCUS] = soundPool?.load(context, R.raw.end_pomodoro_sound_1, 1) ?: 0
        soundIds[SoundEvent.END_BREAK] = soundPool?.load(context, R.raw.end_break_sound_1, 1) ?: 0
    }

    fun playSound(event: SoundEvent) {
        val id = soundIds[event]
        if (id != null && id != 0) {
            soundPool?.play(id, 1f, 1f, 0, 0, 1f)
        }
    }

    fun release() {
        soundPool?.release()
        soundPool = null
    }
}