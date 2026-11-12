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
    /** Each line with the language to say it in; null is the phone's own. */
    private val pending = ArrayDeque<Pair<String, String?>>()
    private var ready = false
    private var failed = false
    private var paused = false
    private var current: Pair<String, String?>? = null
    private var language: String? = null
    var last: String? = null; private set
    private var lastTag: String? = null
    private val ids = AtomicInteger()
    private val lock = Any()

    private val tts: TextToSpeech = TextToSpeech(context.applicationContext) { status ->
        synchronized(lock) {
            if (status == TextToSpeech.SUCCESS) {
                ready = true
                flush()
            } else {
                failed = true
                pending.lastOrNull()?.let { onUnavailable(it.first) }
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

    fun say(text: String, languageTag: String? = null) = synchronized(lock) {
        last = text
        lastTag = languageTag
        if (failed) { onUnavailable(text); return@synchronized }
        pending.addLast(text to languageTag)
        if (ready && !paused && current == null) flush()
    }

    /** Swipe down: the last line again, at the front. */
    fun repeat() = synchronized(lock) {
        val l = last ?: return@synchronized
        if (failed) { onUnavailable(l); return@synchronized }
        tts.stop()
        current = null
        pending.addFirst(l to lastTag)
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
        val item = pending.removeFirstOrNull() ?: return
        current = item
        val (text, tag) = item
        if (tag != language) {
            // A line in a script the engine has no voice for is still said, in the phone's own
            // voice, rather than dropped: badly read is better than silent (docs/FALLBACKS.md).
            val r = tts.setLanguage(if (tag == null) Locale.getDefault() else Locale.forLanguageTag(tag))
            if (r == TextToSpeech.LANG_MISSING_DATA || r == TextToSpeech.LANG_NOT_SUPPORTED) {
                android.util.Log.w("Ikshana", "no voice for $tag; using the phone's own")
                tts.setLanguage(Locale.getDefault())
            }
            language = tag
        }
        val r = tts.speak(text, TextToSpeech.QUEUE_ADD, null, "ik-${ids.incrementAndGet()}")
        if (r != TextToSpeech.SUCCESS) { current = null; onUnavailable(text) }
    }

    fun shutdown() {
        tts.stop()
        tts.shutdown()
    }
}
