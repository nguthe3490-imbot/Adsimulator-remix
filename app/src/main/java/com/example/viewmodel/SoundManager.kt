package com.example.viewmodel

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlin.math.sin

object SoundManager {
    private const val SAMPLE_RATE = 22050

    fun playTone(frequency: Double, durationMs: Int, volume: Float) {
        if (volume <= 0f) return
        Thread {
            try {
                val numSamples = (durationMs * SAMPLE_RATE / 1000)
                val sample = DoubleArray(numSamples)
                val generatedSnd = ByteArray(2 * numSamples)

                for (i in 0 until numSamples) {
                    sample[i] = sin(2 * Math.PI * i / (SAMPLE_RATE / frequency))
                }

                var idx = 0
                for (dVal in sample) {
                    val valShort = (dVal * 32767 * volume).toInt().coerceIn(-32768, 32767).toShort()
                    generatedSnd[idx++] = (valShort.toInt() and 0x00ff).toByte()
                    generatedSnd[idx++] = ((valShort.toInt() and 0xff00) ushr 8).toByte()
                }

                @Suppress("DEPRECATION")
                val audioTrack = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_GAME)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(SAMPLE_RATE)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(generatedSnd.size)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()

                audioTrack.write(generatedSnd, 0, generatedSnd.size)
                audioTrack.play()
                Thread.sleep(durationMs.toLong() + 50)
                audioTrack.stop()
                audioTrack.release()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }.start()
    }

    fun playClick(volume: Float) {
        playTone(1000.0, 50, volume * 0.4f)
    }

    fun playTick(volume: Float) {
        playTone(700.0, 30, volume * 0.3f)
    }

    fun playReward(volume: Float) {
        Thread {
            try {
                playTone(523.25, 100, volume * 0.8f) // C5
                Thread.sleep(120)
                playTone(659.25, 100, volume * 0.8f) // E5
                Thread.sleep(120)
                playTone(783.99, 100, volume * 0.8f) // G5
                Thread.sleep(120)
                playTone(1046.50, 200, volume * 0.9f) // C6
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }.start()
    }

    fun playScare(volume: Float) {
        Thread {
            try {
                for (i in 0..5) {
                    playTone(100.0 + i * 50, 40, volume)
                    Thread.sleep(30)
                }
                playTone(3000.0, 350, volume)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }.start()
    }

    fun playWin(volume: Float) {
        Thread {
            try {
                playTone(523.25, 100, volume * 0.8f) // C5
                Thread.sleep(110)
                playTone(659.25, 100, volume * 0.8f) // E5
                Thread.sleep(110)
                playTone(783.99, 100, volume * 0.8f) // G5
                Thread.sleep(110)
                playTone(1046.50, 100, volume * 0.8f) // C6
                Thread.sleep(110)
                playTone(1318.51, 250, volume * 0.9f) // E6
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }.start()
    }

    fun playLoss(volume: Float) {
        Thread {
            try {
                playTone(392.00, 120, volume * 0.8f) // G4
                Thread.sleep(130)
                playTone(349.23, 120, volume * 0.8f) // F4
                Thread.sleep(130)
                playTone(311.13, 120, volume * 0.8f) // Eb4
                Thread.sleep(130)
                playTone(261.63, 250, volume * 0.8f) // C4
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }.start()
    }
}
