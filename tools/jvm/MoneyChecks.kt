import com.layerbit.ikshana.engine.money.Money
import com.layerbit.ikshana.engine.money.MoneyVote

/**
 * Money, interim. The cost of a wrong answer is someone's money, so the checks lean on refusing:
 * serial numbers, prices and two values at once must give nothing; one frame is never enough;
 * every value's buzz pattern must be told apart from every other by feel.
 */
object MoneyChecks : Suite {
    override val name = "money"

    override fun run() {
        section("values read from a note's text")
        check(Money.valueOf(listOf("RESERVE BANK OF INDIA", "500")) == 500) { "plain 500" }
        check(Money.valueOf(listOf("₹500", "पाँच सौ रुपये")) == 500) { "₹500" }
        check(Money.valueOf(listOf("Rs. 2000", "2000")) == 2000) { "2000 twice is one value" }
        check(Money.valueOf(listOf("2 0 0")) == 200) { "spaces the reader put inside the numeral" }
        check(Money.valueOf(listOf("8AB 123500")) == null) { "a serial number ending in 500" }
        check(Money.valueOf(listOf("1500", "5000", "AB500")) == null) { "not notes" }
        check(Money.valueOf(listOf("500", "100")) == null) { "two values in one frame: neither" }
        check(Money.valueOf(listOf("")) == null) { "nothing" }

        section("a value needs two frames of three to agree")
        val v = MoneyVote()
        check(v.onFrame(500) == null) { "one frame is never enough" }
        check(v.onFrame(500) == 500) { "two agree" }
        check(v.onFrame(500) == null) { "the same note held still is said once" }
        check(v.onFrame(100) == null && v.onFrame(100) == 100) { "a new note, once it agrees" }
        val w = MoneyVote()
        check(w.onFrame(500) == null && w.onFrame(100) == null && w.onFrame(null) == null) { "disagreement says nothing" }
        val x = MoneyVote()
        x.onFrame(200); x.onFrame(200)
        x.onFrame(null); x.onFrame(null); x.onFrame(null)
        check(x.onFrame(200) == null && x.onFrame(200) == 200) { "the same note after looking away is said again" }

        section("words and buzzes, one each per note, all different")
        check(Money.words(500) == "Five hundred rupees.") { "words" }
        val felt = Money.NOTES.map { n -> val p = Money.pattern(n); p.amplitudes.count { it > 0 } to p.timingsMs.first() }
        check(felt.toSet().size == Money.NOTES.size) { "two notes feel the same: ${Money.NOTES.zip(felt)}" }
        check(Money.pattern(500).amplitudes.count { it > 0 } == 5) { "one pulse per hundred" }
    }
}
