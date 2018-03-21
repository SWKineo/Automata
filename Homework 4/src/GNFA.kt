import java.util.*

/**
 * @author: Spencer Ward
 * @date: March 22, 2018
 *
 * A class for building, storing, and running GNFA's, as described in problem
 * 3 on Homework 4, COS 351.
 */

fun main(args: Array<String>) {
    GNFA.gnfaFromRaw("myname\n")
}

class GNFA(val name: String,
           val alphabet: Set<Char>,
           val states: Set<String>,
           val transitions: HashMap<Pair<String, String>, RegExp>,
           val startState: String,
           val acceptState: String) {

    companion object {

        val buildExpression = Regex(
                "([\\w]+).+\\R?" +
                        "([\\w\\s]+)\\R" +
                        "([\\w\\s]+)\\R"
        )
        val alphabetExpression = Regex("[^()./*]")
        val tableRowExpression = Regex("[\\w\\s.()/*]")
        val tableColumnExpression = Regex("[\\w.()/*]")

        /**
         * Builder function to parse a GNFA from a String.
         */
        fun gnfaFromRaw(raw: String): GNFA? {
            // Find the the name, alphabet, and states in the string
            // Returns if the raw string doesn't match the expected pattern
            // Name stored in identifiers.groupValue[0]
            // Alphabet stored in identifiers.groupValue[1]
            // States stored in identifiers.groupValue[2]
            val identifiers = buildExpression.find(raw) ?: return null

            // Read in the name
            val gName = identifiers.groupValues[0]

            var charMatches = alphabetExpression.find(
                    identifiers.groupValues[1])

            val gAlphabet = mutableSetOf<Char>()
            while (charMatches != null) {
                gAlphabet.add(charMatches.)
            }
        }
    }
}