package com.layerbit.ikshana.see

import java.io.Closeable
import java.io.File

/**
 * The vision model behind Describe, Find and Ask, held open once (loading takes seconds and
 * hundreds of megabytes). What it is shown and what of its answer is spoken are decided in
 * engine/see; this class only carries the picture in and the words out.
 *
 * The native side keeps one conversation, so [ask] and [close] are synchronized: the camera's
 * thread asks, the main thread closes, and a close mid-answer would free memory still in use.
 */
class VisionModel private constructor(private var handle: Long) : Closeable {

    /** The answer to [prompt] about an RGB picture, or null when the model failed (logged natively). */
    @Synchronized
    fun ask(rgb: ByteArray, width: Int, height: Int, prompt: String, maxTokens: Int = 96): String? {
        check(handle != 0L) { "closed" }
        return nativeAsk(handle, rgb, width, height, prompt, maxTokens)?.toString(Charsets.UTF_8)
    }

    @Synchronized
    override fun close() {
        if (handle != 0L) nativeClose(handle)
        handle = 0L
    }

    companion object {
        /** Where the model files are looked for: the app's own external files folder, which the
         *  asset pack or the "full" APK fills and adb can reach on a development phone. Never
         *  fetched by the app (it has no INTERNET permission). */
        fun folder(context: android.content.Context) =
            File(context.getExternalFilesDir(null) ?: context.filesDir, "models/vision")

        /** The model and its picture projector, if both are present: the first .gguf whose name
         *  starts "mmproj" is the projector, the other is the model. */
        fun files(dir: File): Pair<File, File>? {
            val all = dir.listFiles { f -> f.name.endsWith(".gguf") }?.toList().orEmpty()
            val proj = all.firstOrNull { it.name.startsWith("mmproj") } ?: return null
            val model = all.firstOrNull { it != proj } ?: return null
            return model to proj
        }

        private val loaded: Boolean = try {
            System.loadLibrary("ikshana_vision"); true
        } catch (e: Throwable) {
            android.util.Log.e("IkshanaVision", "native library missing", e); false
        }

        /** Opens the model in [dir], or null (library or files missing, or a load failure). */
        fun open(dir: File, threads: Int): VisionModel? {
            if (!loaded) return null
            val (model, proj) = files(dir) ?: return null
            val h = nativeOpen(model.absolutePath, proj.absolutePath, threads)
            return if (h == 0L) null else VisionModel(h)
        }

        @JvmStatic private external fun nativeOpen(model: String, proj: String, threads: Int): Long
        @JvmStatic private external fun nativeAsk(h: Long, rgb: ByteArray, w: Int, height: Int, prompt: String, maxTokens: Int): ByteArray?
        @JvmStatic private external fun nativeClose(h: Long)
    }
}
