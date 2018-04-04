import kotlin.collections.HashMap

/**
 * @author: Spencer Ward
 * @date: March 22, 2018
 *
 * A class for building, storing, and running GNFA's, as described in problem
 * 3 on Homework 4, COS 351.
 */

class GNFA(val name: String,
           val alphabet: Set<Char>,
           val states: Set<String>,
           val transitions: HashMap<Pair<String, String>, RegExpr?>,
           val startState: String,
           val acceptState: String) {

    /**
     * Run the GNFA on the given String.
     * @param s: The String to match against the GNFA
     * @return: True if the GNFA "accepts", False if it "rejects"
     */
    fun runOn(s: String): Boolean {
        // Run recursively, beginning with the start state
        return runRec(s, startState)
    }

    /**
     * Recursive helper method for checking transitions.
     * Returns true when it or one of its children hits the accept state
     */
    private fun runRec(s: String, current: String): Boolean {
        // Return true if the current state is the accept state
        if (current == acceptState) return true

        // Check each state as a potential state
        for (potentialNew in states) {
            // Get the transition for the current state and the potential
            val stateTrans = transitions[Pair(current, potentialNew)]
            // Check if the transition is enabled
            if (stateTrans != null) {
                // Try to match the RegExp and consume the match if found
                val leftOver = stateTrans.runConsume(s)
                // Check if the match succeeded
                if (leftOver != null) {
                    // If the match succeeded, try running the remainder of
                    // the string on that state
                    if (runRec(leftOver, potentialNew)) {
                        // Return true if the recursion hit the accept state
                        return true
                    } // Otherwise, keep looking
                } // Otherwise, keep looking
            } // Otherwise, keep looking
        }

        // If the for loop finishes, none of the adjacent states lead to the
        // accept state
        return false
    }

    /**
     * Provide a String representation of this GNFA, as described in Homework
     * 4, problem 3.
     */
    override fun toString(): String {
        val builder = StringBuilder()
        /** object type and name */
        builder.appendln("gnfa")
        builder.appendln(name)
        /** alphabet */
        for (a in alphabet) {
            builder.append(a)
        }
        builder.appendln()

        /** transitions and states */

        // Copy the states to a list so they can be ordered for output
        val statesList = states.toMutableList()
        // Move the start state to the front
        statesList.remove(startState)
        statesList.add(0, startState)
        // Move the accept state to the end
        statesList.remove(acceptState)
        statesList.add(acceptState)
        // The length of the longest expression for each state, to help with
        // formatting
        val longestExps = Array(states.size, { 0 })
        val rows = mutableListOf<MutableList<String>>()
        // Loop over the row states
        for (r in 0 until states.size) {
            // Add new empty list at index r
            rows.add(mutableListOf())
            // Loop over the column states
            for (c in 0 until states.size) {
                // Find the transition expression for the given pair of states
                val exprObj = transitions[Pair(statesList[r], statesList[c])]
                // Label the transition as disabled if its RegExp is null
                val expr = if (exprObj == null) {
                    ".."
                } else {
                    exprObj.toString()
                }
                // Add it to the row at index c
                rows[r].add(expr)
                // If the expression string is longer than the longest
                // encountered for the column, set it as the new longest
                if (expr.length > longestExps[c])
                    longestExps[c] = expr.length
            }
        }

        // Append the states
        builder.append("    ")
        // Loop over the states for the columns
        for (s in 0 until states.size) {
            // Append the state
            builder.append(statesList[s])
            // Pad the state with spaces to the length of the longest expression
            // in its column
            builder.append(" ".repeat(longestExps[s]))
        }
        builder.appendln()

        // Append the transition table
        // Loop over the states for the rows
        for (r in 0 until states.size) {
            builder.append(statesList[r])
            builder.append("  ")
            for (c in 0 until states.size) {
                // Pull the expression from the list we built earlier
                val expr = rows[r][c]
                builder.append(expr)
                // Pad the expression with spaces to the length of the
                // longest expression in its column
                builder.append(" ".repeat(longestExps[c] - expr.length + 2))
            }
        }
        builder.appendln()

        return builder.toString()
    }

    companion object {

        /* We initialize the Regex's in a companion object so they don't need
         * to be reinitialized for each GNFA instance.
         */

        // Regex to match and save name, alphabet, states, and "everything else"
        private val buildExpression = Regex(
                "([\\w]+).+\\R?" +
                        "([\\w\\s]+)\\R" +
                        "([\\w\\s]+)\\R" +
                        "((?:.|\\R)*)\\z"
        )
        // Regex to match a single non-reserved alphabet character
        private val alphabetExpr = Regex("[^()./*]")
        // Regex to match and save the state and expressions for a transition
        private val rowExpr = Regex("^(\\w+)\\s([\\w\\s.()/*]+)$")
        // Regex to match an arbitrary amount of whitespace
        private val stateSplit = Regex("\\s+")

        /**
         * Builder function to parse a GNFA from a String.
         * @param raw: The raw String, in the format given in Homework 4,
         * problem 3
         * @return: The GNFA described by the provided String, null if the
         * GNFA isn't properly formed
         */
        fun from(raw: String): GNFA? {

            /* Find the the name, alphabet, and states in the string
             * Return if the raw string doesn't match the expected pattern
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
             * alphabetExpr.split(..), but that can only return a
             * List<String>, while we want a Set<Char>. We could do the
             * conversion after the fact, but this way we don't have to
             * backtrack as much.
             */

            /** Read in the alphabet */
            // Read the first letter into a MatchResult
            var charMatch = alphabetExpr.find(identifiers.groupValues[1])
            // Create a mutable set to collect the elements
            val gAlphabet = mutableSetOf<Char>()
            // Read in each character until there are no more matches
            while (charMatch != null) {
                // Record the 1st character of the match (a single char String)
                gAlphabet.add(charMatch.value[0])
                // Move to the next match
                charMatch = charMatch.next()
            }

            /** Read in the states */
            // Split the states on whitespace
            // Keep them as a List to preserve order
            val gStates = stateSplit.split(identifiers.groupValues[2])
            val gStart = gStates[0]
            val gAccept = gStates.last()

            /** Parse the transition table */
            val gTransitions = HashMap<Pair<String, String>, RegExpr?>()
            /* Split the table into rows using a regular expression to isolate
             * the state name as rowMatches[..].groupValues[0] and the rest of
             * the line as rowMatches[..].groupValues[1].
             */
            val rowMatches = rowExpr.findAll(identifiers.groupValues[3])
            for (match in rowMatches) {
                // Pull the state from the regex
                val rowState = match.groupValues[0]
                // Split the expressions using our helper function
                val rowTransitions = splitExpressions(
                        match.groupValues[1],
                        gAlphabet)
                // Pair each expression with its respective states and add
                // them to the transition map
                for (i in 0..gStates.lastIndex) {
                    // Give the transition a null RegExp if it's disabled
                    val exp = rowTransitions[i]
                    val regex = if (exp == "..") {
                        null
                    } else {
                        RegExpr.from(exp)
                    }

                    gTransitions.put(
                            Pair(rowState, gStates[i]),
                            regex)
                }
            }

            return GNFA(
                    gName,
                    gAlphabet.toSet(),
                    gStates.toSet(),
                    gTransitions,
                    gStart,
                    gAccept
            )
        }

        /**
         * A helper function to separate expressions given a row of the
         * transition table.
         *
         * This needs to be done manually because Regex's don't support
         * recursion, so there's no convenient way to match parentheses inside
         * expressions.
         */
        private fun splitExpressions(row: String,
                                     alphabet: Set<Char>): List<String> {
            val expressions = mutableListOf<String>()

            // Loop until the second to last character since we check row[i + 1]
            // We'll use a 'while' loop since Kotlin's 'for' loop indexers
            // are immutable.
            var i = 0
            while (i < row.length - 1) {
                when (row[i]) {
                    // Blank, keep searching
                    ' ' -> i++
                    // Possible empty transition
                    '.' -> if (row[i++] == '.') {
                        expressions.add("..")
                    }
                    // Possible special expression
                    'r'-> {
                        val nextChar = row[i++]
                        // Check the next character
                        if (nextChar == '.' || nextChar == '/') {
                            expressions.add("r" + nextChar)
                        } else if ('r' in alphabet) {
                            // If it isn't a special expression but it's still
                            // in the alphabet, record it
                            expressions.add("r")
                        }
                    }
                    // Character, add it normally
                    in alphabet -> expressions.add(row[i++].toString())
                    // Entering a new expression
                    '(' -> {
                        // Here comes the fun part

                        // Mark the start of the expression
                        val startIndex = i
                        // Set a depth counter to 1 to indicate we've entered
                        // the expression
                        var depth = 1
                        // Increase the depth when we enter a new subexpression
                        // Decrease the depth when we leave one
                        // Loop until we exit the original expression
                        while (++i < row.length && depth > 0) {
                            when (row[i]) {
                                '(' -> depth++
                                ')' -> depth--
                            }
                        }
                        // Now we can add the full expression and move to the
                        // next character
                        expressions.add(row.substring(startIndex, ++i))
                    }
                }
            }

            // Now we've read every character of the row except maybe the
            // last, but the only possible expression left would be a single
            // letter in our alphabet
            if (i < row.length && row.last() in alphabet)
                expressions.add(row.last().toString())

            return expressions
        }
    }
}