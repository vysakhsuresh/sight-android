package com.layerbit.ikshana.voice

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale
import java.util.concurrent.atomic.AtomicInteger

/**
 * The voice: the phone's own text-to-speech for now. Squish's converted Indic neural voices come
 * in P2 (item 16), with this as their fallback.
 *
 * Lines are queued in order and never dropped while the engine starts: TextToSpeech binds
 * asynchronously, and "Ikshana" said before the bind would otherwise be lost, which is the one
 * sentence the P0 check asks for.
 *
 * Pause is stop-and-remember (Android's engine has no pause): a tap stops the current line and
 * keeps it with everything queued behind it; the second tap says it again from its start.
 *
 * If the engine is missing or fails, [onUnavailable] fires once and every later line goes to it
 * instead: the screen shows it large and the feedback player buzzes, because with no voice
 * engine there is nothing left to speak with (docs/FALLBACKS.md says so plainly).
 */
class Voice(context: Context, private val onUnavailable: (String) -> Unit) {
    private val pending = ArrayDeque<String>()
    private var ready = false
    private var failed = false
    private var paused = false
    private var current: String? = null
    var last: String? = null; private set
    private val ids = AtomicInteger()
    private val lock = Any()

    private val tts: TextToSpeech = TextToSpeech(context.applicationContext) { status ->
        synchronized(lock) {
            if (status == TextToSpeech.SUCCESS) {
                ready = true
                flush()
            } else {
                failed = true
                pending.lastOrNull()?.let(onUnavailable)
                pending.clear()
            }
        }
    }

    init {
        tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {}
            override fun onDone(utteranceId: String?) {
                synchronized(lock) { if (!paused) { current = null; flush() } }
            }
            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
                synchronized(lock) { current = null; flush() }
            }
        })
    }

    /** Speak in the phone's language; P2 chooses the user's language and the neural voice. */
    fun setLanguage(locale: Locale) {
        synchronized(lock) { if (ready) tts.language = locale }
    }

    fun say(text: String) = synchronized(lock) {
        last = text
        if (failed) { onUnavailable(text); return@synchronized }
        pending.addLast(text)
        if (ready && !paused && current == null) flush()
    }

    /** Swipe down: the last line again, at the front. */
    fun repeat() = synchronized(lock) {
        val l = last ?: return@synchronized
        if (failed) { onUnavailable(l); return@synchronized }
        tts.stop()
        current = null
        pending.addFirst(l)
        paused = false
        if (ready) flush()
    }

    /** Swipe up: skip what is being said and go to the next queued line. */
    fun next(): Boolean = synchronized(lock) {
        if (pending.isEmpty()) return@synchronized false
        tts.stop(); current = null; if (!paused) flush()
        true
    }

    /** Tap: stop now and keep the line; tap again: say it from its start. Returns paused. */
    fun togglePause(): Boolean = synchronized(lock) {
        paused = !paused
        if (paused) {
            current?.let { pending.addFirst(it) }
            current = null
            tts.stop()
        } else if (ready) flush()
        paused
    }

    val isPaused: Boolean get() = synchronized(lock) { paused }

    private fun flush() {
        if (paused || current != null) return
        val text = pending.removeFirstOrNull() ?: return
        current = text
        val r = tts.speak(text, TextToSpeech.QUEUE_ADD, null, "ik-${ids.incrementAndGet()}")
        if (r != TextToSpeech.SUCCESS) { current = null; onUnavailable(text) }
    }

    fun shutdown() {
        tts.stop()
        tts.shutdown()
    }
}
