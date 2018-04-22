import kotlin.text.Regex

/**
 * @author: Spencer Ward
 * @date: March 22, 2018
 *
 * A class for parsing, running, printing, and converting regular expressions.
 * This meets problems 1 and 4 on COS 451, Homework 4.
 */

class RegExpr(val exp: Char?,
              val subExpressions: List<RegExpr>?,
              val operation: Operator) {

    enum class Operator {
        CHAR, EMPTY, NULL, UNION, CONCAT, STAR, ERROR
    }



    /**
     * Check the string against the concatenation of the subexpressions.
     *
     * Uses a reluctant search for the sake of simplicity.
     * A more thorough concat run would try a greedy and a reluctant search
     * for each subexpression
     */
    private fun runConcat(s: String): Boolean {
        // Copy our string to a mutable so it can be reassigned across loop
        // iterations, and nullable for compatibility with runConsume(..)
        var sTemp: String? = s

        // Loop over the subexpressions until the last one
        for (sub in 0 until subExpressions!!.lastIndex) {
            // Try matching the subexpression against the remainder and
            // consume the match if it exists
            sTemp = subExpressions[sub].runConsume(sTemp!!)
            // If the match failed, the concatenated match fails
            if (sTemp == null)
                return false
        }

        // Try to match the rest of the String against the last subexpression
        return subExpressions.last().runOn(sTemp!!)
    }

//        private fun runConcat(s: String): Boolean {
//        /*
//         * To handle complex subexpressions, we will use a reluctant
//         * search to check the string in substrings to find a match
//         * for each token.
//         *
//         * We will use a counter 'start' to designate the start of the
//         * substring, and a counter 'end' to designate the end of the
//         * substring, exclusive.
//         */
//        var start = 0
//        var end = 0
//        // Loop over all but the last subexpression
//        for (i in 0 until subExpressions!!.size - 1) {
//            val sub = subExpressions!![i]
//            // Expand the substring until the expression matches
//            while (!sub.runOn(s.substring(start, end))) {
//                // Increment the end of the substring and check for overflow
//                if (++end == s.length) {
//                    // We've reached the end of the string and haven't found
//                    // a match
//                    return false
//                }
//            }
//            /* Start reading in a new substring
//             * Since 'end' is exclusive, start = end means we start looking
//             * right after the last character in our previous substring, and
//             * our first check is against an empty substring.
//             */
//            start = end
//        }
//        // Now we've either found a match to all but the last expression or
//        // we've already returned, so the last expression has to match the
//        // rest of the string.
//        return subExpressions!!.last().runOn(s.substring(start))
//    }

    /**
     * Check the string against the star of the subexpression
     */
    private fun runStar(s: String): Boolean {
        /**
         * This hasn't been implemented yet, but an implementation would
         * involve splitting the string into 0, 1, 2, ..., s.length
         * substrings, and checking each of these substrings against this
         * regex.
         */
        println("Regular expression star isn't implemented yet!")
        return false
    }

    /**
     * Run the regular expression on the given string.
     * @param s: The string being checked against this regular expression
     * @return True: if the string matches, false otherwise
     */
    fun runOn(s: String): Boolean {
        return when (operation) {
            Operator.ERROR -> false
            Operator.CHAR -> s[0] == exp
            Operator.EMPTY -> s.isEmpty()
            Operator.NULL -> false
            Operator.UNION -> {
                // Accept if any of the subexpressions accepts
                var accepted = false
                for (sub in subExpressions!!) {
                    if (sub.runOn(s)) {
                        accepted = true
                    }
                }
                accepted
            }
            Operator.CONCAT -> runConcat(s)
            Operator.STAR -> runStar(s)
        }
    }

    /**
     * Run the regular expression reluctantly on the given String and return
     * the remainder.
     *
     * @param s: The String to match against
     * @return: The remainder of the String after the first match if one is
     * found, otherwise null
     */
    fun runConsume(s: String): String? {
        // Try each substring length from 0 up to the length of the String
        for (l in 0..s.length) {
            // Test for a match against the first l characters of the String
            if (runOn(s.substring(0, l))) {
                return if (l < s.length) {
                    // If there are any characters left, return them
                    s.substring(l)
                } else {
                    // Otherwise return the empty string.
                    ""
                    // substring(0, l) doesn't include the last
                    // character when l == s.length - 1 and substring(l)
                    // would give an out of bounds exception when l == s.length.
                }
            }
        }

        return null
    }

    /**
     * Convert the subexpressions to one continuous string
     */
    private fun subExpressionsString(): String {
        val builder = StringBuilder()

        for (sub in subExpressions!!) {
            builder.append(" ")
            builder.append(sub.toString())
            builder.append("")
        }

        return builder.toString()
    }

    /**
     * Convert the expression to a string as described in Homework 4
     */
    override fun toString(): String {
        return when (operation) {
            Operator.ERROR -> "Invalid Regular Expression"
            Operator.CHAR -> exp.toString()
            Operator.EMPTY -> "r."
            Operator.NULL -> "r/"
            Operator.UNION ->  "(r|" + subExpressionsString() + " )"
            Operator.CONCAT -> "(r." + subExpressionsString() + " )"
            Operator.STAR -> "(r*" + subExpressionsString() + ")"
        }
    }

    /**
     * Store the intermediate parsing expressions in a companion object so
     * they can be initialized for the RegExpr class, rather than each RegExpr
     * instance.
     */
    companion object {
        private val matchCharacter = Regex("\\w")
        private val matchExpression =
                Regex("\\(\\s*r([|.*])([\\w\\s()]*)\\)")

        /**
         * Builder to produce regular expressions from text
         */
        fun from(raw: String): RegExpr {
            return if (raw[0] != '(') {
                // This regex is a CHAR, EMPTY, or NULL
                initSimple(raw)
            } else {
                initComplex(raw)
            }
        }

        /**
         * Generate a null RegExpr (to make life easier)
         */
        private fun nullExpr() = RegExpr(null, null, Operator.NULL)

        /**
         * Convert an FSA to a regular expression, as described in the proof
         * for lemme 1.60.
         */
        fun fsa2regex(fsa: FSA): RegExpr {
            val dfa = if (fsa is NFA) {
                NFA.convertToDFA(fsa)
            } else {
                fsa as DFA
            }

            /* Construct an equivalent GNFA */
            val gAlphabet = mutableSetOf<Char>()
            for (a in fsa.alphabet) {
                gAlphabet.add(a[0])
            }
            val gTransitions = HashMap<Pair<String, String>, RegExpr?>()
            val gStates = mutableSetOf<String>()

            // Add the start and accept states
            val validStateId = fsa.validState
            val gStart = "q" + validStateId
            val gAccept = "q" + validStateId + 1
            gStates.add(gStart)
            gStates.add(gAccept)

            // Add an epsilon transition from the new start to the old one
            gTransitions.put(
                    Pair(gStart, dfa.startingState),
                    RegExpr(null, null, Operator.EMPTY))
            // Add epsilon transitions from the old accept states to the new
            // ones
            for (accept in dfa.acceptStates) {
                gTransitions.put(
                        Pair(accept, gAccept),
                        RegExpr(null, null, Operator.EMPTY))
            }

            for (state in dfa.states) {
                for (a in dfa.alphabet) {
                    gTransitions.put(
                            Pair(state, dfa.delta(state, a)),
                            from(a))
                }
            }

            // No need to add null transitions since transitions.get(..) returns
            // null if the Pair isn't found.

            // Generate the intermediate GNFA
            val gnfa = GNFA(
                    "intermediate GNFA",
                    gAlphabet.toSet(),
                    gStates,
                    gTransitions,
                    gStart,
                    gAccept
            )

            return convertGnfa(gnfa)
        }

        /**
         * Convert a GNFA to an equivalent regular expression
         * (as described in CONVERT(G) in the proof for Lemma 1.60)
         */
        fun convertGnfa(gnfa: GNFA): RegExpr {
            // If the GNFA only has two states, return their transition
            if (gnfa.states.size == 2) {
                // Return the transition or a null RegExpr if the transition
                // is null
                return gnfa.transitions[Pair(gnfa.startState, gnfa.acceptState)]
                    ?: RegExpr(null, null, Operator.NULL)
            }

            // Copy the old states to a mutable list for indexing
            // Pick a state q_rip that isn't the accept or start state
            val newStates = gnfa.states.toMutableList()
            var qRipIndex = 0
            var qRip = newStates[qRipIndex]
            while (qRip == gnfa.acceptState || qRip == gnfa.startState) {
                qRipIndex++
                qRip = newStates[qRipIndex]
            }
            // remove q_rip from the states list
            newStates.removeAt(qRipIndex)

            // Compute new transitions
            val newTransitions = HashMap<Pair<String, String>, RegExpr?>()
            for (qI in newStates) {
                if (qI != gnfa.acceptState) {
                    for (qJ in newStates) {
                        if (qJ != gnfa.startState) {
                            // Get the partial transitions, marking them as
                            // a null expression if the transition is null
                            val r1 = gnfa.transitions.get(Pair(qI, qRip))
                                        ?: nullExpr()
                            val r2 = gnfa.transitions.get(Pair(qRip, qRip))
                                        ?: nullExpr()
                            val r3 = gnfa.transitions.get(Pair(qRip, qJ))
                                        ?: nullExpr()
                            val r4 = gnfa.transitions.get(Pair(qI, qJ))
                                        ?: nullExpr()

                            val regStar = RegExpr(null,
                                    listOf(r2), Operator.STAR)
                            val regConcat = RegExpr(null,
                                    listOf(r1, regStar, r3), Operator.CONCAT)
                            val regUnion = RegExpr(null,
                                    listOf(regConcat, r4), Operator.UNION)
                            newTransitions.put(
                                    Pair(qI, qJ),
                                    regUnion
                            )
                        }
                    }
                }
            }

            return convertGnfa(GNFA(
                    "intermediate",
                    gnfa.alphabet,
                    newStates.toSet(),
                    newTransitions,
                    gnfa.startState,
                    gnfa.acceptState))
        }

        /**
         * Parse a simple regular expression (c, r., r/)
         */
        private fun initSimple(expression: String): RegExpr {
            // We just need to assign the right operation and we're done
            return if (matchCharacter.matches(expression)) {
                // Regex is a single character
                RegExpr(expression[0], null, Operator.CHAR)
            } else if (expression == "r.") {
                // Regex is an empty string
                RegExpr(null, null, Operator.EMPTY)
            } else if (expression == "r/") {
                // Regex is empty
                RegExpr(null, null, Operator.NULL)
            } else {
                RegExpr(null, null, Operator.ERROR)
            }
        }

        /**
         * Parse a complex regular expression (union, concat, star)
         */
        private fun initComplex(expression: String): RegExpr {
            // Strip the parentheses, store the operation in group 1, and
            // store the string of inner subexpressions in group 2.
            val matched = matchExpression.find(expression)
            if (matched?.groupValues == null) {
                println("Invalid Regular Expression")
                return RegExpr(null, null, Operator.ERROR)
            }

            // Set the operation (we read in the inner expressions the
            // same way for all of the operations)
            val operation = when (matched.groupValues[0]) {
                "|" -> Operator.UNION
                "." -> Operator.CONCAT
                "*" -> Operator.STAR
                else -> {
                    println("Invalid Regular Expression")
                    Operator.ERROR
                }
            }

            // Grab the inner expressions
            val subExpressions =
                    parseSubexpressions(matched.groupValues[1].trim())

            return RegExpr(null, subExpressions, operation)
        }

        private fun parseSubexpressions(tokens: String): List<RegExpr> {
            // Create a mutable set while reading in the subexpressions
            val inner = mutableListOf<RegExpr>()

            // The location of the start of each token
            var open = 0
            // The location of the end of each token
            var close = 0
            // The depth of inner expressions
            // For a token (r. 3 (r| 5 ) )
            // '3' would have depth 1, '5' would have depth 2
            var depth = 0

            // Loop over the inner expressions to split them into tokens
            // (regular expressions to be parsed)
            while (close < tokens.length && open < tokens.length) {
                if (depth == 0) {
                    // Not inside a token
                    when (tokens[open]) {
                        ' ' -> {
                            // Whitespace between tokens, move on
                            open++
                        }
                        '(' -> {
                            // Entering a new subexpression token
                            depth = 1
                            close = open + 1
                        }
                        else -> {
                            // The subexpression is a CHAR, EMPTY, or NULL

                            val token = if (tokens[open + 1] == '.' ||
                                    tokens[open + 1] == '/') {
                                // The subexpression is EMPTY or NULL
                                tokens.substring(open, open + 2)
                            } else {
                                // The subexpression is a CHAR
                                tokens[open].toString()
                            }

                            inner.add(from(token))
                            open++
                        }
                    }
                } else {
                    // Inside a token
                    if (tokens[close] == '(') {
                        // Increase depth
                        depth++
                    } else if (tokens[close] == ')'){
                        // Decrease depth
                        depth--

                        if (depth == 0) {
                            // We have reached the end of the token
                            inner.add(from(tokens
                                    .substring(open, close + 1)))
                            // Start looking for the next token
                            open = close + 1
                        }
                    }
                    // Move on to the next character
                    close++
                }
            }

            return inner.toList()
        }
    }
}
