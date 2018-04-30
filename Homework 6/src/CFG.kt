import java.util.*

/**
 * @author Spencer Ward
 * @date April 5, 2018
 *
 * A class to parse, store, and String-ify context-free grammars (CFGs).
 *
 * Raw CFGs are formatted as described in Homework 5, Question 1.
 */

class CFG(private val name: String,
          private val variables: Set<String>,
          private val terminals: Set<String>,
          private val start: String,
          private val rules: HashMap<String, Set<List<String>?>>) {

    /**
     * Get a String representation of the stored CFG, as described in Homework 5
     */
    override fun toString(): String {
        val builder = StringBuilder()

        // Store the variables that aren't described by any rules
        val unusedVariables = if (start in rules.keys) {
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
             * 'matchRule', so parsing would have already failed. We filter out
             * epsilon rules since epsilon isn't a terminal. */
            for (rule in rules[variable]!!.filterNot{ it == null }) {
                for (element in rule!!) {
                    // This won't make duplicates since `tempUsed` is a Set
                    if (element in terminals) tempUsed.add(element!!)
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
        val earlyVariables = unusedVariables.isNotEmpty()
                                && unusedVariables[0] == start
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
                // Handle epsilon rules
                if (chunk == null) {
                    builder.append(".. ")
                } else {
                    // Append the terminals and variables of the partial rule
                    for (element in chunk) {
                        builder.append(element)
                        builder.append(" ")
                    }
                }
                // If there are more partial rules, append a separator
                if (chunk !== rule.last()) builder.append("| ")
            }
            builder.appendln()
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
     *
     * "Oh yeah it's easy, just do this"
     */
    fun toChomskyNf(): CFG {
        // A mutable set for updating the variables.
        val gVariables = mutableSetOf<String>()
        gVariables.addAll(variables)
        /* Copy our original rules to a new mutable copy so we can preserve
         * the original CFG and edit the new rules on the fly */
        val mutRules = hashMapOf<String, MutableSet<List<String>?>>()
        for (key in rules.keys)
            mutRules[key] = rules[key]!!.toMutableSet()

        /** create a new start */
        val gStart = findNewVariable(variables)
        gVariables.add(gStart)
        mutRules[gStart] = mutableSetOf<List<String>?>(listOf(start))

        /** trim epsilon rules */
        /* This looks absolutely awful but that's the downside of
         * structuring our class this way. */
        do {
            // Assume no epsilon rules will be found
            var epsFound = false
            for (key in mutRules.keys) {
                // Check if The variable has an epsilon rule
                if (mutRules[key]!!.contains(null)) {
                    // Signal that we'll need to loop again
                    epsFound = true
                    // Remove the offending rule
                    mutRules[key]!!.remove(null)
                    // Find every use of the offending variable
                    for (vari in mutRules.keys) {
                        val variRules = mutRules[vari]!!
                        for (rule in variRules) {
                            // We don't want to check epsilon rules in this step
                            if (rule != null)
                                for (i in 0 until rule.size) {
                                    if (rule[i] == key) {
                                        /* For every occurrence of the offending
                                         * variable, add a version of this
                                         * rule with the variable removed */
                                        var tempRule: MutableList<String>? =
                                                mutableListOf()
                                        tempRule!!.addAll(rule)
                                        tempRule.removeAt(i)
                                        /* If this was a unit rule, fill it in
                                         * with an epsilon rule
                                         */
                                        if (tempRule.isEmpty())
                                            tempRule = null
                                        variRules.add(tempRule)
                                    }
                                }
                        }
                    }
                }
            }
        } while (epsFound)

        /** Trim unit rules */
        do {
            // Assume we won't find any unit rules
            var unitRules = false
            for (key in mutRules.keys) {
                // Check the variable's rules
                for (rule in mutRules[key]!!) {
                    // Check if the rule has one element
                    if (rule?.size == 1) {
                        val replaceable = rule.first()
                        // Check if that element is a variable
                        if (replaceable in gVariables) {
                            // We found a unit rule
                            unitRules = true
                            // Remove the unit rule
                            mutRules[key]!!.remove(rule)
                            // Check if 'replaceable' has any rules
                            if (mutRules[replaceable] != null)
                                /* Now we want to loop through the rules and
                                 * replace 'replaceable' -> u with 'key' -> u.
                                 */
                                for (replacedRule in mutRules[replaceable]!!) {
                                    mutRules[replaceable]!!.remove(replacedRule)
                                    mutRules[key]!!.add(replacedRule)
                                }
                        }
                    }

                }
            }
        } while (unitRules)

        /** Convert long rules to the an intermediate form */
        for (key in mutRules.keys) {
            for (rule in mutRules[key]!!) {
                // Find every rule with more than two elements
                if (rule != null && rule.size > 2) {
                    mutRules[key]!!.remove(rule)
                    // Create references to the current and next variables
                    var currentVar = key
                    var nextVar = ""
                    for (element in rule) {
                        nextVar = findNewVariable(gVariables)
                        gVariables.add(nextVar)
                        // Make sure our current variable has space for rules
                        if (mutRules[currentVar] == null)
                            mutRules[currentVar] = mutableSetOf()
                        mutRules[currentVar]!!.add(listOf(element, nextVar))
                        currentVar = nextVar
                    }
                }
            }
        }

        /** Fix rules with mixed terminals and variables */
        for (key in mutRules.keys) {
            val variRules = mutRules[key]!!
            for (rule in variRules) {
                // Find every rule with two elements
                if (rule != null && rule.size == 2) {
                    var firstElmnt = rule[0]
                    var secondElmnt = rule[1]
                    // Flag to signal that this rule needs to be replaced
                    var needsReplacement = false
                    // Check if the elements are terminals
                    if (firstElmnt in terminals) {
                        needsReplacement = true
                        // Add new variable to point to this terminal
                        val newVar = findNewVariable(gVariables)
                        gVariables.add(newVar)
                        mutRules[newVar] = mutableSetOf()
                        mutRules[newVar]!!.add(listOf(firstElmnt))
                        /* Replace the reference to the terminal with a
                         * reference to the new variable */
                        firstElmnt = newVar
                    }
                    if (secondElmnt in terminals) {
                        needsReplacement = true
                        // Add new variable to point to this terminal
                        val newVar = findNewVariable(gVariables)
                        gVariables.add(newVar)
                        mutRules[newVar] = mutableSetOf()
                        mutRules[newVar]!!.add(listOf(secondElmnt))
                        /* Replace the reference to the terminal with a
                         * reference to the new variable */
                        secondElmnt = newVar
                    }

                    if (needsReplacement) {
                        variRules.remove(rule)
                        variRules.add(listOf(firstElmnt, secondElmnt))
                    }
                }
            }
        }

        // Convert the rules map to a non-mutable version to satisfy type checks
        val gRules = hashMapOf<String, Set<List<String>?>>()
        for (key in mutRules.keys)
            gRules[key] = mutRules[key]!!.toSet()

        return CFG(
                name,
                gVariables,
                terminals,
                gStart,
                gRules
        )
    }

    /**
     * Helper method to find a variable not in the given set
     */
    private fun findNewVariable(variableSet: Set<String>): String {
        var newVar = 'A'
        while (newVar.toString() in variableSet)
            newVar++

        return newVar.toString()
    }

    /**
     * Determines if the given String can be generated by this CFG, using the
     * method described in Theorem 4.7 of the textbook.
     *
     * This is the implementation of the Lexaard function 'cfgGen'
     *
     * This doesn't work.
     */
    fun checkGen(test: String): Boolean {
        // Convert this CFG into an equivalent grammar in Chomsky normal form
        val chomsky = toChomskyNf()

        if (test.length == 0) {
            /* Since our grammar is in Chomsky normal form, the only possible
             * way to reach an empty string is if the start state has a rule
             * S -> eps */
            return if (chomsky.rules[chomsky.start] == null)
                false
            else
                chomsky.rules[chomsky.start]!!.contains(null)
        }

        // Else
        // Check all derivations of length 2n - 1, where n = 'test.length'
        return false
    }

    fun checkGenRec(test: String,
                    derivation: String,
                    derivSteps: Int,
                    currentVar: String): Boolean {
        if (test == derivation)
            return true
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

    /**
     * Converts this CFG into an equivalent pushdown automaton.
     */
    fun toPda(): PDA {
        /* Quick note: we can assume the provided CFG is well formed because it
         * was successfully generated as a non-null CFG by retrieveObject(). */

        return PDA.from("")!!
    }

    companion object {
        /** Regular expressions to match the different line types
         *
         * We initialize the Regex's in a companion object so they don't need
         * to be reinitialized for each CFG instance
         */
        val matchRule = Regex("\\w+\\s*->\\s*(?:\\S+\\s*\\|?)+")
        val matchVariables = Regex("(?:\\w+\\s*)+")
        val matchTerminals = Regex("\\.\\.\\s+(?:\\S+\\s*)+")

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
            val mutRules = hashMapOf<String, MutableSet<List<String>?>>()

            // Create a mutable variable to hold the start state
            var gStart: String? = null

            /* Set of unrecognized Strings encountered during rule parsing.
             *
             * Strings that aren't registered as variables are added as
             * terminals.
             *
             * This distinction can't be made while parsing each rule, because
             * not all of the variables can be assumed to have been registered.
             */
            val unknownElements = mutableSetOf<String>()

            // Parse each line
            while (rawScan.hasNext()) {
                // Read in the next line
                val line = rawScan.nextLine()
                val lineScan = Scanner(line)

                when {
                    line.matches(matchRule) -> {
                        /* The line is a rule */

                        // Read in the variable being described
                        val variable = lineScan.next()

                        /* If a start variable hasn't been registered yet, count
                         * this variable as the new start */
                        if (gStart == null) gStart = variable

                        val results = mutableSetOf<List<String>?>()
                        // Skip over the rule identifier
                        lineScan.next("->")
                        while (lineScan.hasNext()) {
                            /* Read until the next separator or the end of the
                             * line */
                            // Mark epsilon rules as null
                            if (lineScan.hasNext("..")) {
                                results.add(null)
                            } else {
                                val result = mutableListOf<String>()
                                while (lineScan.hasNext() &&
                                        !lineScan.hasNext("\\|")) {
                                    val elmnt = lineScan.next()
                                    result.add(elmnt)


                                    /* If the element hasn't been registered yet, mark
                                     * it to be checked once the whole CFG has been
                                     * read in */
                                    if (elmnt !in gTerminals
                                            && elmnt !in gVariables) {
                                        unknownElements.add(elmnt)
                                    }

                                }
                                // Skip the separator, if necessary
                                if (lineScan.hasNext("\\|"))
                                    lineScan.next("\\|")
                                results.add(result)
                            }

                        }

                        // Create the set of rules if it doesn't already exist
                        if (mutRules[variable] == null)
                            mutRules[variable] = mutableSetOf()
                        // Add the newly created rules to the partial CFG
                        mutRules[variable]!!.addAll(results)

                    }
                    line.matches(matchVariables) -> {
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
                            gVariables.add(lineScan.next())
                        }
                    }
                    line.matches(matchTerminals) -> {
                        /* The line is a terminal declaration */

                        // Skip the terminal signal
                        lineScan.next("..")
                        // Read in the terminals
                        while (lineScan.hasNext()) {
                            gTerminals.add(lineScan.next())
                        }
                    }
                    else -> return null /* The line is invalid */
                }
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

            /*
             * Move the rules to a non-mutable set since we don't want to change
             * the CFG after it's been created
             */
            val gRules = hashMapOf<String, Set<List<String>?>>()
            for (key in mutRules.keys)
                gRules[key] = mutRules[key]!!.toSet()

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