/**
 * @author Spencer Ward
 * @date April 26, 2018
 *
 * A class to parse, store, and String-ify pushdown automata (PDAs), as
 * described in COS 451, Homework 6
 */

class PDA() {

    /**
     * Convert this PDA into an equivalent context-free grammar.
     */
    fun toCfg(): CFG {
        /* Quick note: we can assume the provided PDA is well formed because it
         * was successfully generated as a non-null PDA by retrieveObject(). */

        return CFG.from("")!!
    }

    override fun toString(): String {
        return super.toString()
    }

    companion object {
        /**
         * Parses the provided String and returns the PDA it describes.
         *
         * @return null if the String is badly formed
         */
        fun from(raw: String): PDA? {
            return PDA()
        }
    }
}