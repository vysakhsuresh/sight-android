package com.layerbit.ikshana.read

import android.content.Context
import android.util.Log
import com.googlecode.tesseract.android.TessBaseAPI
import com.layerbit.ikshana.engine.read.OcrLine
import java.io.File

/**
 * The interim recogniser: Tesseract's fast LSTM models, offline, for Read. Everything it returns
 * goes through the engine's rules (ReadText.clean, ReadMemory, Scripts) before anything is said.
 *
 * Tesseract wants its language files on disk under `<dir>/tessdata/`, so the first open copies
 * them out of the APK's assets once (about 20 MB). Not thread-safe: one Reader, used only from the
 * camera's analysis thread.
 *
 * Any failure to open (a missing file, a full disk, a native crash on init) returns false, and the
 * session reports `ModelFailed`, which the mode machine turns into Light level and its sentence.
 */
class Reader(private val context: Context) {
    private var api: TessBaseAPI? = null
    private var openLanguages: String? = null

    fun open(languages: String): Boolean {
        if (api != null && openLanguages == languages) return true
        close()
        return try {
            val root = File(context.filesDir, "ocr")
            val dir = File(root, "tessdata").apply { mkdirs() }
            for (lang in languages.split('+')) {
                val f = File(dir, "$lang.traineddata")
                if (!f.exists() || f.length() == 0L) {
                    val part = File(dir, "$lang.traineddata.part")
                    context.assets.open("tessdata/$lang.traineddata").use { input -> part.outputStream().use { input.copyTo(it) } }
                    if (!part.renameTo(f)) throw IllegalStateException("could not place $lang")
                }
            }
            val t = TessBaseAPI()
            if (!t.init(root.absolutePath, languages, TessBaseAPI.OEM_LSTM_ONLY)) {
                t.recycle(); false
            } else {
                // Scene text and pages alike: let it find the blocks itself.
                t.pageSegMode = TessBaseAPI.PageSegMode.PSM_AUTO
                api = t; openLanguages = languages; true
            }
        } catch (e: Throwable) {
            Log.w("Ikshana", "reader failed to open $languages", e)
            false
        }
    }

    /** Lines in reading order with their mean word confidence, from an upright 8-bit luma image. */
    fun read(luma: ByteArray, width: Int, height: Int): List<OcrLine> {
        val t = api ?: return emptyList()
        t.setImage(luma, width, height, 1, width)
        // The text drives recognition; the iterator then gives each line's confidence.
        t.getUTF8Text()
        val out = ArrayList<OcrLine>()
        val it = t.resultIterator ?: return emptyList()
        try {
            it.begin()
            do {
                val text = it.getUTF8Text(TessBaseAPI.PageIteratorLevel.RIL_TEXTLINE) ?: continue
                val conf = it.confidence(TessBaseAPI.PageIteratorLevel.RIL_TEXTLINE)
                out += OcrLine(text.trim(), conf.toInt())
            } while (it.next(TessBaseAPI.PageIteratorLevel.RIL_TEXTLINE))
        } finally {
            it.delete()
            t.clear()
        }
        return out
    }

    fun close() {
        api?.recycle()
        api = null
        openLanguages = null
    }

    companion object {
        /** The reading choices Settings offers: Tesseract language codes, Indian languages first. */
        val CHOICES = listOf("mal+eng", "hin+eng", "tam+eng", "kan+eng", "tel+eng", "eng")

        fun inPack(context: Context): Boolean = try {
            context.assets.list("tessdata")?.contains("eng.traineddata") == true
        } catch (_: Throwable) { false }
    }
}
