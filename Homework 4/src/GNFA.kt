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
                        "([\\w\\s]+)\\R" +
                        "(.|\\R*)\\z"
        )
        val alphabetExpression = Regex("[^()./*]")
        val tableRowExpression = Regex("^(\\w+)\\s([\\w\\s.()/*]+)$")
        val stateSplit = Regex("\\s+")
        val transition = Regex("(?:\\.\\.)|(?:(<!\\()(?:r\\.|r/))")

        /**
         * Builder function to parse a GNFA from a String.
         */
        fun gnfaFromRaw(raw: String): GNFA? {

            /* Find the the name, alphabet, and states in the string
             * Returns if the raw string doesn't match the expected pattern
             *
             * Name stored in identifiers.groupValue[0]
             * Alphabet stored in identifiers.groupValue[1]
             * States stored in identifiers.groupValue[2]
             * Rest of the input stored in identifiers.groupValue[3]
             */
            val identifiers = buildExpression.find(raw) ?: return null

            /** Read in the name */
            val gName = identifiers.groupValues[0]

            /* The code below could be done more easily with
             * alphabetExpression.split(..), but that can only return a
             * List<String>, while we want a Set<Char>. We could do the
             * conversion after the fact, but this way we don't have to
             * backtrack as much.
             */

            /** Read in the alphabet */
            // Read the first letter into a MatchResult
            var charMatch = alphabetExpression.find(identifiers.groupValues[1])
            // Create a mutable set to collect the elements
            val mutableAlphabet = mutableSetOf<Char>()
            // Read in each character until there are no more matches
            while (charMatch != null) {
                // Record the 1st character of the match (a single char String)
                mutableAlphabet.add(charMatch.value[0])
                // Move to the next match
                charMatch = charMatch.next()
            }

            val gAlphabet = mutableAlphabet.toSet()

            /** Read in the states */
            // Split the states on whitespace
            // Temporarily store them in a List to preserve order
            val gStatesList = tokenSplit.split(identifiers.groupValues[2])
            val gStart = gStatesList[0]
            val gAccept = gStatesList.last()

            /** Parse the transition table */
            /* Split the table into rows using a regular expression to isolate
             * the state name as rowMatches[..].groupValues[0] and the rest of
             * the line as rowMatches[..].groupValues[1].
             */
            val rowMatches = tableRowExpression.findAll(
                    identifiers.groupValues[3])
            for (match in rowMatches) {
                val rowState = match.groupValues[0]
                val rowTransitions = splitExpressions(
                        match.groupValues[1],
                        gAlphabet)
            }

            return GNFA(
                    gName,
                    gAlphabet,
                    gStatesList.toSet(),
                    transitions,
                    gStart,
                    gAccept
            )
        }

        /**
         * A helper function to separate expressions given a row of the
         * transition table.
         *
         * This needs to be done by hand because Regex's don't support
         * recursion, so there's no convenient way to match parentheses inside
         * expressions.
         */
        private fun splitExpressions(row: String, alphabet: Set<Char>): List<String> {
            val exps = mutableListOf<String>()

            var i = 0
            while (i < row.length) {
                when (row[i]) {
                    ' ' -> i++
                    '.' -> if (i + 1 < row.length && row[i + 1] == '.') {
                        exps.add("..")
                        i++
                    }
                    'r'-> {
                        val nextChar = row[i + 1]
                        if (nextChar == '.' || nextChar == '/') {
                            exps.add("r" + nextChar)
                        }
                    }
                    in alphabet -> exps.add(row[i].toString())
                    '(' -> {
                        // Here comes the fun part

                        // Set a depth counter to 1 to indicate we've entered
                        // the expression
                        var depth = 1
                        // Loop through the characters in
                        while (++i < row.length && depth > 0) {
                            when (row[++i]) {
                                '(' -> depth++
                                ')' -> depth--
                            }
                        }
                    }
                }
            }
        }
    }
}