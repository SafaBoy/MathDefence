package com.safaboy.mathdefence.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import com.safaboy.mathdefence.game.GameSoundEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

class SoundEffects {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val sampleRate = 22050

    fun play(event: GameSoundEvent) {
        scope.launch {
            runCatching {
                val samples = when (event) {
                    GameSoundEvent.CORRECT_FIRE -> sweepTone(520.0, 1280.0, 140, 0.35)
                    GameSoundEvent.ENEMY_LASER -> sweepTone(760.0, 220.0, 150, 0.32)
                    GameSoundEvent.BOSS_LASER -> dualTone(190.0, 92.0, 280, 0.48)
                    GameSoundEvent.SHIELD_HIT -> dualTone(310.0, 195.0, 160, 0.36)
                    GameSoundEvent.SHIELD_BREAK -> sweepTone(480.0, 90.0, 320, 0.45)
                    GameSoundEvent.CASTLE_HIT -> sweepTone(180.0, 65.0, 260, 0.45)
                    GameSoundEvent.SHIELD_RESTORE -> arpeggioTone(doubleArrayOf(523.25, 659.25, 783.99), 210, 0.32)
                    GameSoundEvent.TIMER_TICK -> sweepTone(880.0, 820.0, 55, 0.22)
                    GameSoundEvent.BOSS_APPEAR -> arpeggioTone(doubleArrayOf(220.0, 196.0, 174.61, 146.83), 460, 0.44)
                    GameSoundEvent.BOSS_COMPONENT_DESTROYED -> sweepTone(340.0, 75.0, 240, 0.42)
                    GameSoundEvent.BOSS_DEFEATED -> arpeggioTone(doubleArrayOf(523.25, 659.25, 783.99, 1046.5), 420, 0.42)
                    GameSoundEvent.PAUSE_DENIED -> dualTone(165.0, 150.0, 140, 0.35)
                    GameSoundEvent.GAME_OVER -> arpeggioTone(doubleArrayOf(392.0, 329.63, 261.63, 196.0), 480, 0.40)
                }
                playPcm(samples)
            }
        }
    }

    private fun sweepTone(startHz: Double, endHz: Double, durationMs: Int, volume: Double): ShortArray {
        val count = (sampleRate * durationMs) / 1000
        val buffer = ShortArray(count)
        var phase = 0.0
        for (i in 0 until count) {
            val t = i.toDouble() / count
            val freq = startHz + (endHz - startHz) * t
            phase += 2.0 * PI * freq / sampleRate
            val env = (1.0 - t) * exp(-t * 1.4)
            val sample = sin(phase) * env * volume * Short.MAX_VALUE
            buffer[i] = sample.toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return buffer
    }

    private fun dualTone(hzA: Double, hzB: Double, durationMs: Int, volume: Double): ShortArray {
        val count = (sampleRate * durationMs) / 1000
        val buffer = ShortArray(count)
        for (i in 0 until count) {
            val t = i.toDouble() / sampleRate
            val progress = i.toDouble() / count
            val env = (1.0 - progress) * exp(-progress * 1.8)
            val wave = 0.6 * sin(2.0 * PI * hzA * t) + 0.4 * sin(2.0 * PI * hzB * t)
            buffer[i] = (wave * env * volume * Short.MAX_VALUE)
                .toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return buffer
    }

    private fun arpeggioTone(notes: DoubleArray, totalDurationMs: Int, volume: Double): ShortArray {
        val count = (sampleRate * totalDurationMs) / 1000
        val buffer = ShortArray(count)
        val segLen = (count / notes.size).coerceAtLeast(1)
        for (i in 0 until count) {
            val seg = (i / segLen).coerceAtMost(notes.lastIndex)
            val localT = (i % segLen).toDouble() / segLen
            val env = (1.0 - localT * 0.7) * (1.0 - (i.toDouble() / count) * 0.25)
            val wave = sin(2.0 * PI * notes[seg] * (i.toDouble() / sampleRate))
            buffer[i] = (wave * env * volume * Short.MAX_VALUE)
                .toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return buffer
    }

    private fun playPcm(samples: ShortArray) {
        val byteSize = samples.size * 2
        val minBuf = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        val track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(maxOf(byteSize, minBuf))
            .setTransferMode(AudioTrack.MODE_STATIC)
            .build()

        try {
            track.write(samples, 0, samples.size)
            track.play()
            val sleepMs = (samples.size * 1000L / sampleRate) + 40L
            Thread.sleep(sleepMs)
        } finally {
            runCatching { track.stop() }
            runCatching { track.release() }
        }
    }
}
