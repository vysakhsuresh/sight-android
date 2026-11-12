package com.layerbit.ikshana

import android.content.Context
import com.layerbit.ikshana.engine.mode.Mode

/**
 * The little the app remembers: the last mode, the two feedback switches, and the crash marker.
 * SharedPreferences, excluded from backup and transfer (data_extraction_rules.xml) like everything
 * else the app keeps.
 */
class Prefs(context: Context) {
    private val p = context.getSharedPreferences("ikshana", Context.MODE_PRIVATE)

    var lastMode: Mode
        get() = Mode.byId(p.getString("last_mode", null) ?: "") ?: Mode.READ
        set(v) { p.edit().putString("last_mode", v.id).apply() }

    var earcons: Boolean
        get() = p.getBoolean("earcons", true)
        set(v) { p.edit().putBoolean("earcons", v).apply() }

    /** Tesseract language codes for Read; Malayalam with English first (brief: Indian languages before English). */
    var readLanguages: String
        get() = p.getString("read_languages", null) ?: "mal+eng"
        set(v) { p.edit().putString("read_languages", v).apply() }

    var haptics: Boolean
        get() = p.getBoolean("haptics", true)
        set(v) { p.edit().putBoolean("haptics", v).apply() }

    /** Whether the camera permission has been asked from Help, to tell "never asked" from "refused for good". */
    var askedCamera: Boolean
        get() = p.getBoolean("asked_camera", false)
        set(v) { p.edit().putBoolean("asked_camera", v).apply() }

    /** When the app last crashed; kept, so the handler can tell a crash loop from one crash. */
    val lastCrashAt: Long get() = p.getLong("last_crash_at", 0L)

    /**
     * Set by the crash handler; commit, not apply, because the process is about to die and an
     * apply may never reach disk.
     */
    fun markCrash(at: Long) {
        p.edit().putLong("last_crash_at", at).putBoolean("crash_pending", true).commit()
    }

    /** Read and cleared by the next start: true once per crash. */
    fun takeCrash(): Boolean {
        if (!p.getBoolean("crash_pending", false)) return false
        p.edit().putBoolean("crash_pending", false).apply()
        return true
    }
}
