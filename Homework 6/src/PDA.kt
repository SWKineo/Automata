import java.util.*

/**
 * @author Spencer Ward
 * @date April 26, 2018
 *
 * A class to parse, store, and String-ify pushdown automata (PDAs), as
 * described in COS 451, Homework 6.
 */


class PDA(val title: String,
          val states: Set<String>,
          val startState: String,
          val acceptStates: Set<String>,
          val alphabet: Set<Char?>,
          val stackAlphabet: Set<Char?>,
          val trans: Map<String, Map<Char?, Map<Char?,
                  Set<Pair<String, Char?>>?>>>) {
    /**
     * An accessor for the transition function. Returns a nullable Set of
     * Pairs of (state, stack-char?).
     *
     * ----------------------------------------------------------------------- *
     *
     * Some implementation details:
     *
     * The transition function is formally defined as
     *      Delta: Q x Sig_eps x Gamma_eps -> P*(Q x Gamma_eps).
     * This is a three dimensional map from
     *      (states, input-char, stack-char)
     * to some set of pairs
     *      {(state_i, stack-char_i), ... } .
     *
     * Our transition function is stored as a 3 dimensional Map from
     *      (String, Char?, Char?)
     * to a nullable Set of Pairs
     *      {(String, Char?), ... }? ,
     * where a null Char is equivalent to epsilon (since Maps in Java
     * and Kotlin support null keys), and a null Set
     * is equivalent to a null transition (which works nicely since
     * map.get(key: K): V? returns a nullable value anyway).
     *
     * This implementation leaves unused parts of the map uninitialized,
     * since we don't need to distinguish between a null value stored in
     * the map and a null "key-not-found" value (either way the
     * transition doesn't exist). Thus accessing looks like
     *      val transitions = trans[state]?.get(inputChar)?.get(stackChar),
     * which returns a nullable set. To help with readability, we use an
     * alias function 'delta(..)'.
     *
     * Reading transitions looks like
     *      val transitions = delta(state, inputChar, stackChar)
     * which returns a nullable set traversable by
     *      for ((state, stack) in transitions!!) {
     *          println("We move to state $state and push $stack to"
     *                      + "our stack")
     *      } .
     */
    private fun delta(state: String, inputChar: Char?, stackChar: Char?)
                = trans[state]?.get(inputChar)?.get(stackChar)

    /**
     * Convert this PDA into an equivalent context-free grammar, as outlined
     * in Lemma 2.27
     */
    fun toCfg(): CFG {
        /* Quick note: we can assume the provided PDA is well formed because it
         * was successfully generated as a non-null PDA by retrieveObject(). */

        /* Modify the PDA to make the conversion simpler */
        // (The inline function 'toMutableList(..)' makes a copy of the set)
        val newStates = states.toMutableList()
        val emptyStackState = addNewState(newStates)
        val newAccept = addNewState(newStates)
        val newTrans = mutableMapOf<String, MutableMap<Char?, MutableMap<Char?,
                            MutableSet<Pair<String, Char?>>?>>>()
        // Make a mutable copy of the transition map
        for (state in trans.keys) {
            newTrans[state] = mutableMapOf()
            for (a in trans[state]!!.keys) {
                newTrans[state]!![a] = mutableMapOf()
                for (s in trans[state]!![a]!!.keys) {
                    val desired = trans[state]!![a]!![s]
                    // Don't need to add a transition if the transition is null
                    if (desired != null)
                        newTrans[state]!![a]!![s]!!.toMutableSet()
                }
            }
        }
        // Reduce the PDA so its only accept state is 'newAccept'
        for (accept in acceptStates) {
            addTrans(newTrans,
                    accept, null, null,
                    Pair(emptyStackState, null))
        }
        // Make the PDA empty its stack before accepting
        for (a in alphabet) {
            /* Loop on the state 'emptyStackState' until there are no more
             * letters to remove */
            addTrans(newTrans,
                    emptyStackState, null, a,
                    Pair(emptyStackState, null))
        }
        // Remove the final 'S' and '$'
        addTrans(newTrans,
                emptyStackState, null, 'S',
                Pair(emptyStackState, null))
        // Move to the accept state after removing the end marker
        addTrans(newTrans,
                emptyStackState, null, '$',
                Pair(newAccept, null))
        /* Set the automaton to either push or pop on every
         * transition */
        for (state in newTrans.keys) {
            for (a in trans[state]!!.keys) {
                for (pop in trans[state]!![a]!!.keys) {
                    val dest = trans[state]!![a]!![pop]
                    if (dest != null) for ((next, push) in dest) {
                        // Replace simultaneous pushes and pops
                        if (pop != null && push != null) {
                            val between = addNewState(newStates)
                            addTrans(newTrans,
                                    state, a, push,
                                    Pair(between, null))
                            addTrans(newTrans,
                                    between, null, null,
                                    Pair(next, pop))
                        } else if (pop == null && push == null) {
                            val between = addNewState(newStates)
                            addTrans(newTrans,
                                    state, a, null,
                                    Pair(between, '$'))
                            addTrans(newTrans,
                                    between, null, '$',
                                    Pair(next, null))
                        }
                    }
                }
            }
        }

        /* Start of actual conversion */

        /* Generate the variables of G by using a Map to keep track of them
         * while building the automaton. This way we can access variables
         * implicitly using vars[p]!![q]!! to represent A_p,q */
        var varName = 'A'
        val vars = mutableMapOf<String, MutableMap<String, Char>>()
        // Loop over the states
        for (p in newStates) {
            for (q in newStates) {
                // Initialize parts of the map if they don't exist
                if (vars[p] == null) vars[p] = mutableMapOf()
                // Add the variable name and move to the next letter
                vars[p]!![q] = varName
                varName++
            }
        }

        // Create fields for the new CFG
        val pName = title
        val pVariables = mutableSetOf<Char>()
        val pTerminals = mutableSetOf<Char>()
        val pStart = vars[startState]!![newAccept]!!
        val pRules = mutableMapOf<Char, MutableSet<List<Char>?>>()

        /* step 1 */
        for (p in newStates) for (q in newStates) for (r in newStates)
            for (s in newStates) for (u in stackAlphabet) {
                // Create a modified alphabet to ensure epsilon is included
                val alphaEps = alphabet.toMutableSet()
                alphaEps.add(null)
                for (a in alphaEps) for (b in alphaEps) {
                    // Safely find the desired transitions
                    val firstCheck = newTrans[p]?.get(a)?.get(null)
                    val secondCheck = newTrans[s]?.get(b)?.get(u)
                    // Check the transitions if the exist
                    if (firstCheck != null && secondCheck != null
                            && firstCheck.contains(Pair(r, u))
                            && secondCheck.contains(Pair(q, null))) {
                        /* If they do, add the necessary rule.
                         *
                         * Since null terminals represent epsilon
                         * transitions, removing them from the rule has the
                         * same effect as leaving them in, as long as there
                         * are some non-epsilon terminals left in the rule.
                         *
                         * If there aren't, we label this as an epsilon rule
                         * by marking it as 'null'
                         */
                        val destination = listOfNotNull(a, vars[r]!![s]!!, b)
                        if (destination.isEmpty())
                            addRule(pRules, vars[p]!![q]!!, null)
                        else
                            addRule(pRules, vars[p]!![q]!!, destination)
                    }
                }
        }
        /* step 2 */
        for (p in newStates) for (q in newStates) for (r in newStates) {
                    addRule(pRules, vars[p]!![q]!!,
                            listOf(vars[p]!![r]!!, vars[r]!![q]!!))
        }
        /* step 3 */
        for (p in newStates) {
            addRule(pRules, vars[p]!![p]!!, null)
        }

        return CFG(
                pName,
                pVariables,
                pTerminals,
                pStart,
                pRules)
    }

    /**
     * Helper function to set rules in CFGs
     *
     * (I don't know why I didn't make one of these for the last assignment)
     */
    private fun addRule(rules: MutableMap<Char, MutableSet<List<Char>?>>,
                        variable: Char,
                        addition: List<Char>?) {
        if (rules[variable] == null) rules[variable] = mutableSetOf()
        rules[variable]!!.add(addition)
    }

    /**
     * Convert this PDA into an equivalent String, using the format described
     * in Homework 6.
     *
     *
     * This currently doesn't properly handle formatting for transitions with
     * multiple destinations, but fixing that would require rearranging most of
     * the method, so I'm going to leave it like this for now
     */
    override fun toString(): String {
        // Hold the contents of the string as we're building it
        val builder = StringBuilder()
        builder.appendln("pda")
        builder.appendln(title)
        builder.appendln("")

        /* Turn the alphabet into a lexicographically sorted List, treating
         * as infinity to make epsilon the last character printed */
        val orderedAlphabet = alphabet.sortedBy {
                it?.toInt() ?: Int.MAX_VALUE
        }
        // Do the same thing to the stack alphabet
        val orderedStack = stackAlphabet.sortedBy {
                it?.toInt() ?: Int.MAX_VALUE
        }

        builder.append("\t")
        /* Print out the alphabet, repeating each letter once for each letter
         * in the stack alphabet. */
        orderedAlphabet.forEach {
            // Loop once for each letter of the stack alphabet
            for (i in 0 until stackAlphabet.size) {
                if (it == null)
                    builder.append("..    ")
                else
                    builder.append("$it     ")
            }
            builder.append("\t")
        }
        builder.appendln()
        builder.append("\t")
        /* Print a copy of the stack alphabet for each letter in the input
         * alphabet */
        for (i in 0 until alphabet.size) {
            orderedStack.forEach {
                if (it == null)
                    builder.append("..    ")
                else
                    builder.append("$it     ")
            }
            builder.append("\t")
        }
        builder.appendln()

        // Sort the states lexicographically and store them in a List
        val statesOrdered = states.sorted().toMutableList()
        // Move the start state to be the first in the list
        if (statesOrdered.contains(startState)) {
            statesOrdered.remove(startState)
            statesOrdered.add(0, startState)
        }

        // Loop over the states to build the table
        statesOrdered.forEach { state ->

            if (state in acceptStates)
                builder.append("*")
            else
                builder.append(" ")
            builder.append("$state\t")

            /* Loop through the stack alphabet for every loop of the input
             * alphabet */
            for (input in orderedAlphabet) {
                for (stack in orderedStack) {
                    /* start of alphabet loops */

                    // Find the Set of possible destinations
                    val destinations = delta(state, input, stack)

                    if (destinations == null) {
                        // Mark the transition as null if necessary
                        builder.append("..    ")
                    } else {
                        // Sort the destinations lexicographically by state
                        val destSorted = destinations.sortedBy { it.first }

                        // Print each of the destinations
                        for ((dState, dStack) in destSorted) {
                            builder.append("$dState,")
                            // Mark epsilon transitions if necessary
                            if (dStack == null)
                                builder.append(".. ")
                            else
                                builder.append("$dStack  ")
                        }
                    }

                    /* end of alphabet loops */
                }
            }
        }

        return builder.toString()
    }

    companion object {
        /**
         * Simple Regex to match a line of the transition table. The
         * useful parts of the Regex are the groups, which hold the accept
         * marker '*' in 'accept', the name of the state in 'state', and all
         * of the transition pairs in 'trans'.
         */
        private val matchTransition =
                Regex("\\s*(?<accept>\\*?)(?<state>\\w+)\\s++(?<trans>.+)")

        /**
         * Parses the provided String and returns the PDA it describes.
         *
         * @return null if the String is badly formed
         */
        fun from(raw: String): PDA? {
            val lines = Scanner(raw)

            // Read in the title
            val mTitle = lines.nextLine()

            /* Read the input alphabet into a List<String>, splitting on
             * whitespace */
            var alphabetString = lines.nextLine().split("\\s+")
            /* Kotlin idiom, map the List<String> above to a List<Char?>,
             * replacing '..' with null to represent epsilon.
             * The new list is then trimmed of duplciates by 'distinct()'. */
            val alphabetOrdered = alphabetString.map{
                if (it == "..") null
                else it[0]
            }.distinct()

            // Do the same thing for the stack alphabet
            alphabetString = lines.nextLine().split("\\s+")
            val stackAlphabetOrdered = alphabetString.map{
                if (it == "..") null
                else it[0]
            }.distinct()

            // Create variables for handling states
            var mStart: String? = null
            val mAccept = mutableSetOf<String>()
            val mStates = mutableSetOf<String>()
            val mTrans = mutableMapOf<String, MutableMap<Char?,
                    MutableMap<Char?, MutableSet<Pair<String, Char?>>?>>>()

            // Parse the transition table
            var line = lines.nextLine()
            while (line != "") {
                // Check that the line is a transition, otherwise return null
                val match = matchTransition.matchEntire(line) ?: return null

                /* Read in the state, register it, and mark it as an accept
                 * state if necessary */
                val state = match.groups["state"]!!.value
                mStates.add(state)
                if (match.groups["accept"]!!.value == "*")
                    mAccept.add(state)
                // If no start state has been set yet, set it as this one
                if (mStart == null) mStart = state

                // Read in the rest of the transitions as a List of tokens
                val tranSplit = match.groups["trans"]!!.value.split("\\s+")
                // Convert the transitions into Pairs of (state, stack)
                val tranPairs = tranSplit.map {
                    /* Put the transition state into 'pair[0]' and the stack
                     * character (or "..") into 'pair[0]'.
                     * If the transition is null, the split will not find any
                      * commas, so 'pair' will have size 1. */
                    val pair = it.split(",")
                    when {
                        pair.size < 2 -> null
                        pair[1] == ".." -> Pair(pair[0], null)
                        else -> Pair(pair[0], pair[1][0])
                    }
                }
                val tranIterator = tranPairs.iterator()

                // Read the transitions into the transition table
                for (input in alphabetOrdered) {
                    for (stack in stackAlphabetOrdered) {
                        val destination = tranIterator.next()
                        // If the transition isn't null, add it to the table
                        if (destination != null)
                            addTrans(mTrans,
                                    state, input, stack,
                                    destination)
                    }
                }

                // Move on to the next line
                line = lines.nextLine()
            }

            lines.close()
            return PDA(mTitle,
                    mStates,
                    mStart!!,
                    mAccept,
                    alphabetOrdered.toSet(),
                    stackAlphabetOrdered.toSet(),
                    mTrans)
        }

        /**
         * Helper function to add a destination to a transition.
         *
         * Initializes everything that hasn't already been accessed
         */
        fun addTrans(trans: MutableMap<String,
                            MutableMap<Char?,
                            MutableMap<Char?, MutableSet<Pair<String, Char?>>?>>>,
                     state: String,
                     inputChar: Char?,
                     stackChar: Char?,
                     pair: Pair<String, Char?>) {
            // Make sure everything's initialized
            if (trans[state] == null)
                trans[state] = hashMapOf()
            if (trans[state]!![inputChar] == null)
                trans[state]!![inputChar] = hashMapOf()
            if (trans[state]!![inputChar]!![stackChar] == null)
                trans[state]!![inputChar]!![stackChar] = mutableSetOf()

            trans[state]!![inputChar]!![stackChar]!!.add(pair)
        }

        /**
         * A helper function to add a new state to the automata. Returns the
         * name of the state that was just added.\
         */
        fun addNewState(states: MutableCollection<String>): String {
            // Create the base for the state name and its identifier
            var stateBase = "q"
            var stateIdentifier = 0

            // Create a variable to hold the state as it's being tested
            var newState: String

            /* Check each combination of base and identifier until we find one
             * that hasn't been used before */
            do {
                newState = "$stateBase$stateIdentifier"
                stateIdentifier++
            } while (newState in states)

            // Add the new state to the automata
            states.add(newState)

            return newState
        }
    }
}