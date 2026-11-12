package com.layerbit.ikshana.text

import android.content.Context
import com.layerbit.ikshana.R
import com.layerbit.ikshana.engine.light.LightBand
import com.layerbit.ikshana.engine.mode.Line
import com.layerbit.ikshana.engine.mode.Mode
import com.layerbit.ikshana.engine.mode.Said

/**
 * Engine ids to words, in the language the app speaks (the family's rule: engine tables reach
 * the screen by id). A `when` rather than getIdentifier, so a missing string is a compile error
 * here and SpeechTableChecks fails before that on the JVM.
 */
object Labels {
    fun mode(m: Mode): Int = when (m) {
        Mode.READ -> R.string.mode_read
        Mode.MONEY -> R.string.mode_money
        Mode.DESCRIBE -> R.string.mode_describe
        Mode.FIND -> R.string.mode_find
        Mode.TRANSLATE -> R.string.mode_translate
        Mode.ASK -> R.string.mode_ask
        Mode.LIGHT -> R.string.mode_light
        Mode.HELP -> R.string.mode_help
    }

    fun line(l: Line): Int = when (l) {
        Line.WELCOME -> R.string.say_welcome
        Line.MODE_ON -> R.string.say_mode_on
        Line.NOT_READY -> R.string.say_not_ready
        Line.COULD_NOT_START -> R.string.say_could_not_start
        Line.LIGHTER -> R.string.say_lighter
        Line.NO_CAMERA -> R.string.say_no_camera
        Line.CAMERA_BUSY -> R.string.say_camera_busy
        Line.CAMERA_BACK -> R.string.say_camera_back
        Line.TOO_DARK -> R.string.say_too_dark
        Line.HOLD_STILL -> R.string.say_hold_still
        Line.MOVE_CLOSER -> R.string.say_move_closer
        Line.MOVE_BACK -> R.string.say_move_back
        Line.NOTHING_HERE -> R.string.say_nothing_here
        Line.RESTARTED -> R.string.say_restarted
        Line.WARM -> R.string.say_warm
        Line.COOL -> R.string.say_cool
        Line.TOO_LONG -> R.string.say_too_long
        Line.ABOUT_READ -> R.string.say_about_read
        Line.ABOUT_MONEY -> R.string.say_about_money
        Line.ABOUT_DESCRIBE -> R.string.say_about_describe
        Line.ABOUT_FIND -> R.string.say_about_find
        Line.ABOUT_TRANSLATE -> R.string.say_about_translate
        Line.ABOUT_ASK -> R.string.say_about_ask
    }

    fun light(b: LightBand): Int = when (b) {
        LightBand.DARK -> R.string.light_dark
        LightBand.DIM -> R.string.light_dim
        LightBand.BRIGHT -> R.string.light_bright
        LightBand.GLARE -> R.string.light_glare
    }

    /** Reader.CHOICES by their Tesseract codes. */
    fun reading(codes: String): Int = when (codes) {
        "mal+eng" -> R.string.read_mal_eng
        "hin+eng" -> R.string.read_hin_eng
        "tam+eng" -> R.string.read_tam_eng
        "kan+eng" -> R.string.read_kan_eng
        "tel+eng" -> R.string.read_tel_eng
        else -> R.string.read_eng
    }

    fun said(context: Context, s: Said): String {
        val names = s.modes.map { context.getString(mode(it)) }
        return context.getString(line(s.line), *names.toTypedArray())
    }
}
