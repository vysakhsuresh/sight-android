import com.layerbit.ikshana.engine.light.LightBand
import com.layerbit.ikshana.engine.mode.Line
import com.layerbit.ikshana.engine.mode.Mode
import java.io.File

/**
 * The machine's sentences, the app's strings and docs/FALLBACKS.md are one table in three places.
 *
 * A Line with no `say_<id>` string would reach the voice as nothing at all: the one outcome the
 * app exists to prevent. So every Line must have its string, with the same number of mode
 * placeholders and the same English wording; every mode its `mode_<id>` name; every language
 * the app speaks the same placeholders as English; and docs/FALLBACKS.md must name every line,
 * so the document a tester reads is the one the code says.
 */
object SpeechTableChecks : Suite {
    override val name = "speech"

    private fun root(): File = listOf(File("."), File("..")).first { File(it, "app/src/main/res").isDirectory }

    private val STRING = Regex("""<string name="([a-z0-9_]+)"[^>]*>(.*?)</string>""", RegexOption.DOT_MATCHES_ALL)
    private val PLACEHOLDER = Regex("%(\\d)\\\$s")

    fun strings(file: File): Map<String, String> =
        STRING.findAll(file.readText(Charsets.UTF_8)).associate { it.groupValues[1] to unescape(it.groupValues[2]) }

    private fun unescape(s: String) = s.replace("\\'", "'").replace("\\\"", "\"").replace("&amp;", "&")
        .replace("&lt;", "<").replace("&gt;", ">").trim().removeSurrounding("\"")

    private fun placeholders(s: String) = PLACEHOLDER.findAll(s).map { it.groupValues[1].toInt() }.toSortedSet()

    override fun run() {
        val res = File(root(), "app/src/main/res")
        val english = strings(File(res, "values/strings.xml"))

        section("every line has its English string, wording and placeholders")
        for (line in Line.entries) {
            val s = english["say_${line.id}"] ?: throw CheckFailed("no say_${line.id} in values/strings.xml: ${line.english}")
            check(s == line.english) { "say_${line.id} reads \"$s\", the machine's reference is \"${line.english}\"" }
            check(placeholders(s) == (1..line.modes).toSortedSet()) { "say_${line.id} has placeholders ${placeholders(s)}, takes ${line.modes} modes" }
            check(s.codePointCount(0, s.length) <= 200) { "say_${line.id} is too long to hear in one breath" }
        }
        check(english.keys.filter { it.startsWith("say_") }.all { k -> Line.entries.any { "say_${it.id}" == k } }) {
            "a say_ string no line uses: ${english.keys.filter { it.startsWith("say_") && Line.byId(it.removePrefix("say_")) == null }}"
        }

        section("every mode has a name")
        for (m in Mode.entries) check(english["mode_${m.id}"]?.isNotBlank() == true) { "no mode_${m.id} in values/strings.xml" }
        for (b in LightBand.entries) check(english["light_${b.id}"]?.isNotBlank() == true) { "no light_${b.id} in values/strings.xml" }

        section("every translation keeps the placeholders")
        res.listFiles { f -> f.isDirectory && f.name.startsWith("values-") }!!.forEach { dir ->
            val f = File(dir, "strings.xml")
            if (!f.exists()) return@forEach
            for ((k, v) in strings(f)) {
                val en = english[k] ?: throw CheckFailed("${dir.name} has $k, English does not")
                check(placeholders(v) == placeholders(en)) { "${dir.name}/$k placeholders ${placeholders(v)} vs English ${placeholders(en)}" }
            }
        }

        section("docs/FALLBACKS.md names every line")
        val doc = File(root(), "docs/FALLBACKS.md").readText(Charsets.UTF_8)
        for (line in Line.entries) check("`say_${line.id}`" in doc) { "docs/FALLBACKS.md does not list say_${line.id}" }
    }
}
