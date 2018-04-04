import java.util.*

/**
 * @author Spencer Ward
 * @date April 5, 2018
 *
 * A class to parse, store, and String-ify context-free grammars (CFGs).
 *
 * Raw CFGs are formatted as described in Homework 5, Question 1.
 */

class CFG(val name: String,
          val variables: Set<String>,
          val terminals: Set<String>,
          val start: String,
          val rules: HashMap<String, Set<Set<String>>>) {

    /**
     * Get a String representation of the stored CFG, as described in Homework 5
     */
    override fun toString(): String {
        val builder = StringBuilder()

        // Store the variables that aren't described by any rules
        val unusedVariables = if (start !in rules.keys) {
            // A sorted list of the variables not described by any rules
            variables.filterNot { it in rules.keys }.sorted()
        } else {
            /* If the start variable was unused, we want it to be at the start of
             * the variable list.
             *
             * This could be done with less backtracking by
             * checking the start variable against 'rules.keys' then adding that
             * to the sorted list and adding the rest of the sorted filtered
             * variables into this list, but I'd much rather muddle an edge case
             * than make the normal case unnecessarily convoluted. */

            // Filter and sort all of the unused states except the start state
            val tempUnusedVar = variables.filterNot {
                it != start && it in rules.keys
            }.sorted().toMutableList()
            // Add the start state to the beginning
            tempUnusedVar.add(0, start)
            // Use this as the filtered List
            tempUnusedVar
        }

        /* Filtering terminals is a little more tricky, since they need to be
         * checked against the values of the HashMap 'rules'. */
        val tempUsed = mutableSetOf<String>()
        for (variable in rules.keys) {
            /* We can skip the null check since a rule without a right-hand
             * side would be considered a badly-formed line by the Regex
             * 'matchRule', so parsing would have already failed */
            for (rule in rules[variable]!!) {
                for (element in rule) {
                    // This won't make duplicates since `tempUsed` is a Set
                    if (element in terminals) tempUsed.add(element)
                }
            }
        }

        // Now we can store the terminals that aren't used in any rules
        val unusedTerminals = terminals.filterNot { it in tempUsed }.sorted()

        /* Append basic formatting identifier and the name */
        builder.appendln("cfg")
        builder.appendln(name)

        /* Append the unused variables early and set a flag if they contain
         * the start state */
        val earlyVariables = unusedVariables[0] == start
        if (earlyVariables) {
            for (variable in unusedVariables) {
                builder.append(variable)
                builder.append(" ")
            }
            builder.appendln()
        }

        /* Append the rules */
        for (variable in rules.keys) {
            builder.append(variable)
            builder.append(" -> ")

            /* We can skip the null check since a rule without a right-hand
             * side would be considered a badly-formed line by the Regex
             * 'matchRule', so parsing would have already failed */
            val rule = rules[variable]!!

            // Append every partial rule
            for (chunk in rule) {
                // Append the terminals and variables of the partial rule
                for (element in chunk) {
                    builder.append(element)
                    builder.append(" ")
                }
                // If there are more partial rules, append a separator
                if (chunk !== rule.last()) builder.append(" | ")
            }
        }

        /* If the unused variables weren't appended earlier, append them now */
        if (!earlyVariables) {
            for (variable in unusedVariables) {
                builder.append(variable)
                builder.append(" ")
            }
            builder.appendln()
        }

        /* Append the unused terminals */
        if (unusedTerminals.isNotEmpty()) {
            builder.append(".. ")
            for (terminal in unusedTerminals) {
                builder.append(terminal)
                builder.append(" ")
            }
            builder.appendln()
        }

        return builder.toString()
    }

    /**
     * Returns a copy of this CFG that has been converted to Chomsky Normal
     * Form, as described in Theorem 2.9 of the textbook.
     *
     * This is the implementation of the Lexaard function 'chomskyNF'
     */
    fun toChomskyNf(): CFG {
        // TODO
        return CFG(
                name,
                variables,
                terminals,
                start,
                rules
        )
    }

    /**
     * Determines if the given String can be generated by this CFG, using the
     * method described in Theorem 4.7 of the textbook.
     *
     * This is the implementation of the Lexaard function 'cfgGen'
     */
    fun checkGen(test: String): Boolean {
        // TODO
        return false
    }

    /**
     * Determines if the given String can be generated by this CFG, using the
     * method described in Theorem 7.16
     *
     * This is the implementation of the Lexaard function 'cfgGenF'
     */
    fun checkGenF(test: String): Boolean {
        // TODO
        return false
    }

    companion object {
        /** Regular expressions to match the different line types
         *
         * We initialize the Regex's in a companion object so they don't need
         * to be reinitialized for each CFG instance
         */
        val matchRule = Regex("\\w+\\s+->\\s+(?:\\w+\\s+\\|?)+")
        val matchVariables = Regex("(?:\\w+\\s+)+")
        val matchTerminals = Regex("\\.\\.\\s+(?:\\w+\\s+)+")

        /**
         * Creates a new CFG from the given raw input String.
         *
         * @return null if the input is badly formed
         */
        fun from(raw: String): CFG? {
            // Use a scanner to tokenize input
            val rawScan = Scanner(raw.trim())

            // Read in the name
            val gName = rawScan.next()
            // Skip the rest of the line
            rawScan.nextLine()

            // Create collections to start reading in input
            val gVariables = mutableSetOf<String>()
            val gTerminals = mutableSetOf<String>()
            val gRules = hashMapOf<String, Set<Set<String>>>()

            // Create a mutable variable to hold the start state
            var gStart: String? = null

            /* Set of unrecognized Strings encountered during rule parsing.
             *
             * Strings that aren't registered as variables are added as
             * terminals.
             *
             * This distinction can't be made while parsing the rule, because
             * not all of the variables can be assumed to have been registered.
             */
            val unknownElements = mutableSetOf<String>()

            // Parse each line
            while (rawScan.hasNext()) {
                // Read in the next line
                val line = rawScan.nextLine()
                val lineScan = Scanner(line)

                if (line.matches(matchRule)) {
                    /* The line is a rule */

                    // Read in the variable being described
                    val variable = lineScan.next()

                    /* If a start variable hasn't been registered yet, count
                     * this variable as the new start */
                    if (gStart == null) gStart = variable

                    val results = mutableSetOf<Set<String>>()
                    // Skip over the rule identifier
                    lineScan.next("->")
                    while(lineScan.hasNext()) {
                        val result = mutableSetOf<String>()
                        // Read until the next separator or the end of the line
                        while (lineScan.hasNext() &&
                                !lineScan.hasNext("\\|")) {
                            val elmnt = lineScan.next()
                            result.add(elmnt)

                            /* If the element hasn't been registered yet, mark
                             * it to be checked once the whole CFG has been
                             * read in */
                            if (elmnt !in gTerminals && elmnt !in gVariables)
                                unknownElements.add(elmnt)
                        }
                        // Skip the separator, if necessary
                        lineScan.next("\\|")
                        results.add(result)
                    }

                    // Add the newly created rule to the partial CFG
                    gRules.put(variable, results)

                } else if (line.matches(matchVariables)) {
                    /* The line is a variable declaration */

                    /* If the start variable hasn't been set yet, read in the
                     * first variable and set it as the new start.
                     *
                     * The start variable is read in separately from the rest so
                     * that this null check doesn't need to be done for every
                     * variable. */
                    if (gStart == null) {
                        gStart = lineScan.next()
                        gVariables.add(gStart)
                    }

                    // Read in (the rest of) the variables
                    while (lineScan.hasNext()) {
                        gTerminals.add(lineScan.next())
                    }

                } else if (line.matches(matchTerminals)) {
                    /* The line is a terminal declaration */

                    // Skip the terminal signal
                    lineScan.next("..")
                    // Read in the terminals
                    while (lineScan.hasNext()) {
                        gTerminals.add(lineScan.next())
                    }

                } else return null /* The line is invalid */
            }

            /* Go through all of the unknown Strings encountered while parsing
             * this CFG's rules and assign any that aren't registered as
             * variables to be terminals. */
            for (obj in unknownElements) {
                if (obj !in gVariables) {
                    /* Since gTerminals is a Set, this line won't add
                     * duplicate terminals. */
                    gTerminals.add(obj)
                }
            }

            /* Since every line involving terminals sets the start variables if
             * it hasn't already been set, gStart will only be null if the CFG
             * does not have any variables, and is therefore considered
             * badly formed. */
            if (gStart == null) return null

            return CFG(
                    gName,
                    gVariables,
                    gTerminals,
                    gStart,
                    gRules
            )
        }
    }
}