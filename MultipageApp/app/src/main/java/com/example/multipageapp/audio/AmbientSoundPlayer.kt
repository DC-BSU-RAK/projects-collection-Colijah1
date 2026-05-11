package com.example.multipageapp.audio

import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import com.example.multipageapp.domain.FocusSound
import kotlin.random.Random

/**
 * Lightweight ambient audio using [AudioTrack] (no bundled media files).
 */
class AmbientSoundPlayer {

    @Volatile
    private var running = false
    private var thread: Thread? = null

    fun start(sound: FocusSound) {
        stop()
        if (sound == FocusSound.NONE) return
        running = true
        thread = Thread {
            val sampleRate = 22_050
            val channelConfig = AudioFormat.CHANNEL_OUT_MONO
            val audioFormat = AudioFormat.ENCODING_PCM_16BIT
            val minBuffer = AudioTrack.getMinBufferSize(sampleRate, channelConfig, audioFormat)
                .coerceAtLeast(1024)
            @Suppress("DEPRECATION")
            val track = AudioTrack(
                AudioManager.STREAM_MUSIC,
                sampleRate,
                channelConfig,
                audioFormat,
                minBuffer * 2,
                AudioTrack.MODE_STREAM
            )
            track.setVolume(0.07f)
            track.play()
            val buffer = ShortArray(512)
            val random = Random.Default
            var smooth = 0.0
            val isRain = sound == FocusSound.RAIN
            while (running) {
                for (i in buffer.indices) {
                    var sample = random.nextDouble(-1.0, 1.0)
                    if (isRain) {
                        smooth = smooth * 0.94 + sample * 0.06
                        sample = smooth
                    }
                    buffer[i] = (sample * 900).toInt().coerceIn(
                        Short.MIN_VALUE.toInt(),
                        Short.MAX_VALUE.toInt()
                    ).toShort()
                }
                track.write(buffer, 0, buffer.size)
            }
            track.stop()
            track.release()
        }.also { it.start() }
    }

    fun stop() {
        running = false
        try {
            thread?.join(320)
        } catch (_: InterruptedException) {
            Thread.currentThread().interrupt()
        }
        thread = null
    }
}
