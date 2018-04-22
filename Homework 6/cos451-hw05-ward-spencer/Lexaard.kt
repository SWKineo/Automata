import java.util.*

/**
 * @author Spencer Ward
 * @date April 5, 2018
 *
 * An interactive interpreter for the language Lexaard, as described in
 * Homework 2, Question 4.
 *
 * This is a recreation of the original Java interpreter using Kotlin. The
 * new version has been cleaned up and simplified significantly to take
 * advantage of parsing tricks for LL(1) grammars.
 */


fun main(args: Array<String>) {
    println(CFG.from(
            "G1 p102\r\n" +
                    "A -> 0 A 1\r\n" +
                    "A -> B | 2\r\n" +
                    "B -> # | 3\r\n" +
                    ".. 0 1 # 4\r\n" +
                    "A B C\r\n"
    ).toString())
    Lexaard().run()
}

class Lexaard {
    val input = Scanner(System.`in`)
    val varRegistry = hashMapOf<String, Any>()
    var running = true

    fun run() {
        while (running) {
            val command = input.next()

            when (command) {
                "quit" -> running = false
                "print" -> cmdPrint()
                "define" -> cmdDefine()
                "run" -> cmdRun()
                "else" -> println("Please enter a command")
            }
        }
        input.close()
    }

    /**
     * Parse the object to be printed, then print it using its '.toString'
     * method. This works because Kotlin calls the overridden method for the
     * class in memory.
     */
    private fun cmdPrint() {
        // Parse the desired object
        val retrievedAny = retrieveObject()

        /* Convert it to a String and print it.
         * We don't need to output an error if the object can't be parsed or
         * found, but we also don't want to print anything. */
        if (retrievedAny != null)
            println(retrievedAny.toString())
    }

    /**
     * Read in the name and parse the given object, then store the object in a
     * HashMap with its name as the key.
     */
    private fun cmdDefine() {
        // Read in the name of the new variable
        val varName = input.next()
        // Read in the object to be stored
        val varObject = retrieveObject()

        if (varObject == null)
            // Don't add the object if it was null (meaning it was badly formed)
            println("That object can't be parsed!")
        else
            // Otherwise, add the object as a registered variable
            varRegistry[varName] = varObject

    }

    /**
     * Parse the provided object and try running it on the given string, if it's
     * runnable. Prints an error if the object isn't runnable or the second
     * argument isn't a string.
     */
    private fun cmdRun() {
        // Parse the object to be run
        val runnable = retrieveObject()
        // Parse the string to be run on
        val str = retrieveObject()

        // Check that the second object is actually a string
        if (str !is String) {
            println("Your second argument needs to be a string!")
            return
        }

        // If the object is actually a runnable, run it on the string.
        val result = when (runnable) {
            is FSA -> runnable.runString(str)
            is RegExpr -> runnable.runOn(str)
            is GNFA -> runnable.runOn(str)
            else -> {
                println("That object isn't an automaton!")
                return
            }
        }

        // Print the result
        if (result)
            println("accept")
        else
            println("reject")
    }

    /**
     * Parse a literal, find an object that's been registered, or evaluate a
     * function.
     */
    private fun retrieveObject(): Any? {
        // Read in the next token to predict the kind of object
        val firstToken = input.next()

        /** Check registered variables first so they don't mess up the rest
         * of the parsing */
        if (firstToken in varRegistry.keys)
            return varRegistry[firstToken]
        if (firstToken.matches(Regex("\".*\"")))
        // The object is a literal string
            return firstToken

        return when(firstToken) {

        /** literal objects */
        // The object is a literal boolean
            "true" -> true
            "false" -> false

        // The object is a literal FSA
            "fsa" -> FSA.from(input)

        // The object is a literal GNFA
            "gnfa" -> GNFA.from(pullRaw())

        // The object is a literal CFG
            "cfg" -> CFG.from(pullRaw())

        /** functions */
            "nfa2dfa" -> {
                val fsa = retrieveObject()
                if (fsa is FSA)
                    NFA.convertToDFA(fsa)
                else null
            }

            "dfaUnion" -> {
                val dfa1 = retrieveObject()
                val dfa2 = retrieveObject()
                if (dfa1 is DFA && dfa2 is DFA)
                    DFA.union(dfa1, dfa2)
                else null
            }

            "nfaUnion" -> {
                val nfa1 = retrieveObject()
                val nfa2 = retrieveObject()
                if (nfa1 is NFA && nfa2 is NFA)
                    NFA.union(nfa1, nfa2)
                else null
            }

            "nfaConcat" -> {
                val nfa1 = retrieveObject()
                val nfa2 = retrieveObject()
                if (nfa1 is NFA && nfa2 is NFA)
                    NFA.concat(nfa1, nfa2)
                else null
            }

            "nfaStar" -> {
                val nfa = retrieveObject()
                if (nfa is NFA)
                    NFA.star(nfa)
                else null
            }

            "pruneFSA" -> {
                val fsa = retrieveObject()
                if (fsa is FSA)
                    FSA.prune(fsa)
                else null
            }

            "fsaEquivP" -> {
                val fsa1 = retrieveObject()
                val fsa2 = retrieveObject()
                if (fsa1 is FSA && fsa2 is FSA)
                    FSA.equivP(fsa1, fsa2)
                else null
            }

            "regex2fsa" -> {
                val reg = retrieveObject()
                if (reg is RegExpr)
                    FSA.regex2Fsa(reg)
                else null
            }

            "fsa2regex" -> {
                val reg = retrieveObject()
                if (reg is FSA)
                    RegExpr.fsa2regex(reg)
                else null
            }

            "chomskyNF" -> {
                val cfg = retrieveObject()
                if (cfg is CFG)
                    cfg.toChomskyNf()
                else null
            }

            "cfgGen" -> {
                val cfg = retrieveObject()
                val str = retrieveObject()
                if (cfg is CFG && str is String)
                    cfg.checkGen(str)
                else null
            }

            "cfgGenF" -> {
                val cfg = retrieveObject()
                val str = retrieveObject()
                if (cfg is CFG && str is String)
                    cfg.checkGenF(str)
                else null
            }

            else -> /** Check Regex matches last since they don't have an identifier */
                if (firstToken.matches(Regex("(?:r\\.|r/|[\\w()|.*])+")))
                    // The object is a literal RegExpr
                    RegExpr.from(firstToken)
                else null
        }
    }

    /**
     * Helper function to pull out the raw component of a literal object.
     *
     * Skips the first next line of input, since 'input' is assumed to start
     * on the identifier line.
     *
     * Reads in input until hitting an empty line, then returns as a complete
     * string.
     */
    private fun pullRaw(): String {
        /* For convenience, we can assume we start on the same line as the
         * identifier, so we want to move to skip the first line */
        input.nextLine()
        // StringBuilder to hold the intermediate String
        val builder = StringBuilder()
        // Keep a reference to the current line
        var currentLine = input.nextLine()
        // Loop until we hit a blank line
        while (currentLine != "") {
            // Add the current line to the builder, along with a newline
            builder.appendln(currentLine)
            // Move to the next line
            currentLine = input.nextLine()
        }
        // Return the final String
        return builder.toString()
    }
}
