package com.layerbit.ikshana.feedback

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import com.layerbit.ikshana.engine.feedback.Feedback
import com.layerbit.ikshana.engine.mode.Earcon

/**
 * Plays an earcon and its haptic twin together. The tones and the patterns are made in the
 * engine (Feedback, EarconChecks); this only hands them to the speaker and the motor.
 *
 * Each earcon is rendered once into a static AudioTrack and replayed from the start: a static
 * track starts in a few milliseconds where a streaming one has to fill a buffer first, and an
 * earcon that lands after the sentence it introduces is noise. Usage is accessibility, so the
 * tones follow the accessibility volume TalkBack users already have set.
 *
 * Either half may be switched off in Settings. A failure of either (no audio track, no motor) is
 * logged and the other half and the voice carry on: docs/FALLBACKS.md, "Failures below".
 */
class FeedbackPlayer(context: Context, private val earconsOn: () -> Boolean, private val hapticsOn: () -> Boolean) {
    private val rate = 48_000
    private val tracks = HashMap<Earcon, AudioTrack>()

    private val vibrator: Vibrator? = try {
        if (Build.VERSION.SDK_INT >= 31) {
            (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
    } catch (t: Throwable) { null }

    fun play(earcon: Earcon) {
        if (earconsOn()) tone(earcon)
        if (hapticsOn()) buzz(earcon)
    }

    private fun tone(earcon: Earcon) {
        try {
            val track = tracks.getOrPut(earcon) {
                val pcm = Feedback.pcm(earcon, rate)
                AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder().setSampleRate(rate)
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build()
                    )
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .setBufferSizeInBytes(pcm.size * 2)
                    .build().also { it.write(pcm, 0, pcm.size) }
            }
            track.stop()
            track.reloadStaticData()
            track.play()
        } catch (t: Throwable) {
            Log.w("Ikshana", "earcon $earcon could not play", t)
        }
    }

    private fun buzz(earcon: Earcon) {
        val v = vibrator ?: return
        try {
            if (!v.hasVibrator()) return
            val p = Feedback.twin(earcon)
            if (v.hasAmplitudeControl()) {
                v.vibrate(VibrationEffect.createWaveform(p.timingsMs, p.amplitudes, -1))
            } else {
                // No amplitude control: the shape is lost but the rhythm is kept, on for every
                // segment with any strength and off for the gaps.
                val on = p.amplitudes.map { if (it > 0) 255 else 0 }.toIntArray()
                v.vibrate(VibrationEffect.createWaveform(p.timingsMs, on, -1))
            }
        } catch (t: Throwable) {
            Log.w("Ikshana", "haptic $earcon could not play", t)
        }
    }

    fun release() {
        tracks.values.forEach { runCatching { it.release() } }
        tracks.clear()
    }
}
