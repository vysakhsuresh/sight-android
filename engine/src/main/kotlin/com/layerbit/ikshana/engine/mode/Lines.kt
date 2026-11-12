package com.layerbit.ikshana.engine.mode

/**
 * Every sentence the mode machine can say, by id.
 *
 * The engine never holds the words, only the id and how many modes the sentence names: the app
 * maps `say_<id>` to a string resource with a translation per language it speaks (the family's
 * rule that engine tables reach the screen by id). [english] is the reference wording that
 * docs/FALLBACKS.md and strings.xml carry; SpeechTableChecks holds the three together, so a line
 * added here with no string, or no row in the document, fails the suite instead of reaching a
 * blind user as silence.
 *
 * [modes] is how many mode names the sentence takes, in order (%1$s, %2$s).
 */
enum class Line(val id: String, val modes: Int, val english: String) {
    WELCOME("welcome", 0, "Ikshana."),
    MODE_ON("mode_on", 1, "%1\$s."),
    NOT_READY("not_ready", 2, "%1\$s is not on this phone yet. %2\$s is on instead."),
    COULD_NOT_START("could_not_start", 2, "%1\$s could not start. %2\$s is on instead."),
    LIGHTER("lighter", 1, "%1\$s is on, with the lighter model this phone can carry."),
    NO_CAMERA(
        "no_camera", 0,
        "Ikshana cannot use the camera. To allow it, open Settings, then Apps, Ikshana, Permissions, Camera. Or pick a picture from the gallery."
    ),
    CAMERA_BUSY("camera_busy", 0, "Another app is using the camera. Close it, and Ikshana will look again."),
    CAMERA_BACK("camera_back", 1, "The camera is back. %1\$s is on."),
    TOO_DARK("too_dark", 0, "Nothing found. It is too dark; more light, please."),
    HOLD_STILL("hold_still", 0, "Nothing found. Hold still."),
    MOVE_CLOSER("move_closer", 0, "Nothing found. Move closer."),
    MOVE_BACK("move_back", 0, "Nothing found. Move back a little."),
    NOTHING_HERE("nothing_here", 0, "Nothing found here. Move the phone slowly."),
    RESTARTED("restarted", 1, "Ikshana restarted. %1\$s is on. Say it again."),
    WARM("warm", 0, "The phone is warm. Ikshana is slowing down to cool it."),
    COOL("cool", 0, "The phone has cooled. Back to full speed."),
    TOO_LONG("too_long", 1, "%1\$s is taking too long. Hold still and try again.");

    companion object {
        fun byId(id: String): Line? = entries.firstOrNull { it.id == id }
    }
}

/** A line with the modes it names: what is actually spoken, and what "said twice" compares. */
data class Said(val line: Line, val modes: List<Mode> = emptyList()) {
    init {
        require(modes.size == line.modes) { "${line.id} takes ${line.modes} modes, given ${modes.size}" }
    }
}
