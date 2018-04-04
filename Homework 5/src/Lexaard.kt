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
    Lexaard().run()
}

class Lexaard {
    val input = Scanner(System.`in`)
    val objectReg = hashMapOf<String, Any>()
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

    private fun cmdPrint() {
        val retrievedAny = retrieveObject()
    }

    private fun cmdDefine() {

    }

    private fun cmdRun() {

    }

    /**
     * Parse a literal, find an object that's been registered, or evaluate a
     * function.
     */
    private fun retrieveObject(): Any? {
        // Read in the next token to predict the kind of object
        val firstToken = input.next()

        /** Check Regex matches first since they aren't supported by 'when' */
        if (firstToken.matches(Regex("\".*\"")))
        // The object is a literal string
            return input.next()
        if (firstToken.matches(Regex("(?:r\\.|r/|[\\w()|.*])+")))
        // The object is a literal RegExpr
            return RegExpr.from(firstToken)

        return when(firstToken) {
        /** registered variables */
        // The object is a registered variable
            in objectReg.keys -> objectReg[firstToken]

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
                val expectedFSA = retrieveObject()

                if (expectedFSA is FSA)
                    NFA.convertToDFA(expectedFSA)
                else null
            }

            "dfaUnion" -> {
                val arg1 = retrieveObject()
                val arg2 = retrieveObject()
                if (arg1 is DFA && arg2 is DFA)
                    DFA.union(arg1, arg2)
                else null
            }

            "nfaUnion" -> {
                val arg1 = retrieveObject()
                val arg2 = retrieveObject()
                if (arg1 is NFA && arg2 is NFA)
                    NFA.union(arg1, arg2)
                else null
            }

            "nfaConcat" -> {
                val arg1 = retrieveObject()
                val arg2 = retrieveObject()
                if (arg1 is NFA && arg2 is NFA)
                    NFA.concat(arg1, arg2)
                else null
            }

            "nfaStar" -> {
                val arg = retrieveObject()
                if (arg is NFA)
                    NFA.star(arg)
                else null
            }

            "pruneFSA" -> {
                val arg = retrieveObject()
                if (arg is FSA)
                    FSA.prune(arg)
                else null
            }

            "fsaEquivP" -> {
                val arg1 = retrieveObject()
                val arg2 = retrieveObject()
                if (arg1 is FSA && arg2 is FSA)
                    FSA.equivP(arg1, arg2)
                else null
            }

            "regex2fsa" -> {
                val arg = retrieveObject()
                if (arg is RegExpr)
                    FSA.regex2Fsa(arg)
                else null
            }

            "fsa2regex" -> {
                val arg = retrieveObject()
                if (arg is FSA)
                    RegExpr.fsa2regex(arg)
                else null
            }

        // TODO: Homework 5 functions

        /** Invalid input */
            else -> null
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
