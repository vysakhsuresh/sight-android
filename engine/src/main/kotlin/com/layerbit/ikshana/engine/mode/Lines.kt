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
    NOT_READY("not_ready", 2, "%1\$s is not ready yet. %2\$s is on."),
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
    TOO_LONG("too_long", 1, "%1\$s is taking too long. Hold still and try again."),
    // Said when Describe starts looking: the model takes seconds, and seconds of nothing after
    // choosing a mode is the silence the owner's second rule forbids.
    LOOKING("looking", 0, "Looking."),
    // What each mode does, said after "not ready yet" so nobody is left guessing what a
    // button is for (owner, 12 Nov: "if I cannot understand what Translate translates, how
    // will a visually impaired man?").
    ABOUT_READ("about_read", 0, "Read speaks any text the camera sees: signs, pages, medicine strips."),
    ABOUT_MONEY("about_money", 0, "Money tells you the value of an Indian note. Hold the note flat, a hand's length away."),
    ABOUT_DESCRIBE("about_describe", 0, "Describe will tell you what is in front of you. It is still being built."),
    ABOUT_FIND("about_find", 0, "Find will lead you to a door, a cup, or anything you name, buzzing stronger as you get close. It is still being built."),
    ABOUT_TRANSLATE("about_translate", 0, "Translate will read a sign in one Indian language and speak it in yours, like a Kannada board in Malayalam. It is still being built."),
    ABOUT_ASK("about_ask", 0, "Ask will answer your question about what the camera sees. It is still being built.");

    companion object {
        fun byId(id: String): Line? = entries.firstOrNull { it.id == id }

        fun about(mode: Mode): Line? = when (mode) {
            Mode.READ -> ABOUT_READ; Mode.MONEY -> ABOUT_MONEY; Mode.DESCRIBE -> ABOUT_DESCRIBE
            Mode.FIND -> ABOUT_FIND; Mode.TRANSLATE -> ABOUT_TRANSLATE; Mode.ASK -> ABOUT_ASK
            else -> null
        }
    }
}

/** A line with the modes it names: what is actually spoken, and what "said twice" compares. */
data class Said(val line: Line, val modes: List<Mode> = emptyList()) {
    init {
        require(modes.size == line.modes) { "${line.id} takes ${line.modes} modes, given ${modes.size}" }
    }
}
