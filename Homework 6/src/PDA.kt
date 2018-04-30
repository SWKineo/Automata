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
     * Convert this PDA into an equivalent context-free grammar.
     */
    fun toCfg(): CFG {
        /* Quick note: we can assume the provided PDA is well formed because it
         * was successfully generated as a non-null PDA by retrieveObject(). */

        return CFG.from("")!!
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
            val mTrans = hashMapOf<String, HashMap<Char?, HashMap<Char?,
                                        MutableSet<Pair<String, Char?>>?>>>()

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
        private fun addTrans(trans: HashMap<String, HashMap<Char?, HashMap<Char?,
                                MutableSet<Pair<String, Char?>>?>>>,
                             state: String, inputChar: Char?, stackChar: Char?,
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
    }
}