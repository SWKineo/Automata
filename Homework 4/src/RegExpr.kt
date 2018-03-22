import kotlin.text.Regex

/**
 * @author: Spencer Ward
 * @date: March 22, 2018
 *
 * A class for parsing, running, printing, and converting regular expressions.
 * This meets problems 1 and 4 on COS 451, Homework 4.
 */

class RegExpr(private val exp: Char?,
              private val subExpressions: List<RegExpr>? = null,
              private val operation: Operator) {

    enum class Operator {
        CHAR, EMPTY, NULL, UNION, CONCAT, STAR, EPSILON, ERROR
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
     * Check the string against the star of the subexpressions
     */
    private fun runStar(s: String): Boolean {
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
            Operator.EPSILON -> true
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
            Operator.EPSILON -> FSA.EPSILON
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
        fun regexFrom(raw: String): RegExpr {
            if (raw[0] != '(') {
                // This regex is a CHAR, EMPTY, or NULL
                return initSimple(raw)
            } else {
                return initComplex(raw)
            }
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

                            inner.add(regexFrom(token))
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
                            inner.add(regexFrom(tokens
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
