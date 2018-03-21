import kotlin.text.Regex

/**
 * @author Spencer Ward
 * @date March 22, 2018
 *
 * A class for parsing, running, printing, and converting regular expressions
 * . This meets problems 1 and 4 on COS 451, Homework 4
 */

class RegExp(expression: String) {

    // The internal expression, if this is a simple regular expression
    private var exp: Char? = null

    // The list of subexpressions, if this is a regular expression operation
    private var innerExpressions: List<RegExp>? = null

    // The type of operation type of the outer regular expression
    private lateinit var operation: Operator

    private enum class Operator {
        CHAR, EMPTY, NULL, UNION, CONCAT, STAR, ERROR
    }

    /**
     * Parse a simple regular expression (c, r., r/)
     */
    private fun initSimple(expression: String) {
        // We just need to assign the right operation and we're done
        if (matchCharacter.matches(expression)) {
            // Regex is a single character
            operation = Operator.CHAR
            exp = expression[0]
        } else if (expression == "r.") {
            // Regex is an empty string
            operation = Operator.EMPTY
        } else if (expression == "r/") {
            // Regex is empty
            operation = Operator.NULL
        }
    }

    /**
     * Parse a complex regular expression (union, concat, star)
     */
    private fun initComplex(expression: String) {
        // Strip the parentheses, store the operation in group 1, and
        // store the string of inner subexpressions in group 2.
        val matched = matchExpression.find(expression)

        if (matched?.groupValues == null) {
            println("Invalid Regular Expression")
            operation = Operator.ERROR
        } else {
            // Set the operation (we read in the inner expressions the
            // same way for all of the operations)
            operation = when (matched.groupValues[0]) {
                "|" -> Operator.UNION
                "." -> Operator.CONCAT
                "*" -> Operator.STAR
                else -> {
                    println("Invalid Regular Expression")
                    Operator.ERROR
                }
            }

            // Grab the inner expressions
            parseSubexpressions(matched.groupValues[1].trim())
        }
    }

    private fun parseSubexpressions(tokens: String) {
        // Create a mutable set while reading in the subexpressions
        val inner = mutableListOf<RegExp>()

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
                            tokens.subSequence(open, open + 2) as String
                        } else {
                            // The subexpression is a CHAR
                            tokens[open].toString()
                        }

                        inner.add(RegExp(token))
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
                        inner.add(RegExp(tokens
                                .substring(open, close + 1)))
                        // Start looking for the next token
                        open = close + 1
                    }
                }
                // Move on to the next character
                close++
            }
        }

        innerExpressions = inner.toList()
    }

    /* Parsing the regular expression from text */
    init {
        if (expression[0] != '(') {
            // This regex is a CHAR, EMPTY, or NULL
            initSimple(expression)
        } else {
            initComplex(expression)
        }
    }

    /**
     * Check the string against the concatenation of the inner expressions
     */
    private fun runConcat(s: String): Boolean {
        /*
         * To handle complex subexpressions, we will use a reluctant
         * search to check the string in substrings to find a match
         * for each token.
         *
         * We will use a counter 'start' to designate the start of the
         * substring, and a counter 'end' to designate the end of the
         * substring, exclusive.
         */
        var start = 0
        var end = 0
        // Loop over all but the last inner expression
        for (i in 0 until innerExpressions!!.size - 1) {
            val sub = innerExpressions!![i]
            // Expand the substring until the expression matches
            while (!sub.runOn(s.substring(start, end))) {
                // Increment the end of the substring and check for overflow
                if (++end == s.length) {
                    // We've reached the end of the string and haven't found
                    // a match
                    return false
                }
            }
            /* Start reading in a new substring
             * Since 'end' is exclusive, start = end means we start looking
             * right after the last character in our previous substring, and
             * our first check is against an empty substring.
             */
            start = end
        }
        // Now we've either found a match to all but the last expression or
        // we've already returned, so the last expression has to match the
        // rest of the string.
        return innerExpressions!!.last().runOn(s.substring(start))
    }

    /**
     * Check the string against the star of the inner expressions
     */
    private fun runStar(s: String): Boolean {
        println("Regular expression star isn't implemented yet!")
        return false
    }

    /**
     * Run the regular expression on the given string.
     * @param s The string being checked against this regular expression
     * @return True if the string matches, false otherwise
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
                for (sub in innerExpressions!!) {
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
     * Convert the inner expressions to one continuous string
     */
    private fun innerExpsToString(): String {
        val builder = StringBuilder()

        for (sub in innerExpressions!!) {
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
            Operator.UNION ->  "(r|" + innerExpsToString() + " )"
            Operator.CONCAT -> "(r." + innerExpsToString() + " )"
            Operator.STAR -> "(r*" + innerExpsToString() + ")"
        }
    }

    /**
     * Store the intermediate parsing expressions in a companion object so
     * they can be initialized for the RegExp class, rather than each RegExp
     * instance.
     */
    companion object {
        private val matchCharacter = Regex("\\w")
        private val matchExpression = Regex("\\(\\s*r([|.*])([\\w\\s()]*)\\)")
    }
}
