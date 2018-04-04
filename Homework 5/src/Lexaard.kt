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
    val registeredObjects = hashMapOf<String, Any>()

    fun run() {

    }
}
