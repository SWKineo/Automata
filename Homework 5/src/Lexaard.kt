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
    val registeredObjects = hashMapOf<String, Any>()
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
    }

    /**
     * Parse a literal, find an object that's been registered, or evaluate a
     * function.
     */
    private fun retrieveObject(): Any? {
        // Read in the next token to predict the kind of object
        val firstToken = input.next()

        // The object is a literal string, so return it
        // TODO: Fix this so it works with RegExpr's
        if (firstToken.matches(Regex("\".*\"")))
            return input.next()
        // The object is a literal boolean, so parse it and return it
        if (firstToken == "true")
            return true
        if (firstToken == "false")
            return false
        // The object is a registered variable
        if (firstToken in registeredObjects.keys)
            return registeredObjects[firstToken]
        // TODO FSA, RegExpr, GNFA, CFG

        return null
    }

    private fun cmdPrint() {
        val retrievedAny = retrieveObject()
    }

    private fun cmdDefine() {

    }

    private fun cmdRun() {

    }
}
