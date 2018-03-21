import javafx.util.Pair;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Scanner;

/**
 * A class for building, storing, and running nondeterministic finite-state
 * automata.
 */
public class NFA extends FSA {

    // The states of the array
    // * The states are accessible through states.keySet(): Set<String>
    // * The transition function is used through
    //      delta(state: String, letter: String): List<String>
    // * The transitions are stored as an List value, ordered to
    //      match the indices of the alphabet characters.
    protected HashMap<String, List<List<String>>> states;

    public NFA(String label) {
        super(label);
        states = new HashMap<>();
    }

    /**
     * Process a raw line of input and add a new state, along with its
     * transitions, to the NFA.
     * @param rawState A state followed by a list of transitions, lined
     *                 up with the order the characters from the alphabet
     *                 were defined
     * @return True if the addition was a success, False if it wasn't (for
     *                  instance if the  current automaton is a DFA but the
     *                  input is destined for an NFA)
     */
    @Override
    public boolean addState(String rawState) {
        Pair<Scanner, String> prepAdd = prepAddState(rawState);
        Scanner stateReader = prepAdd.getKey();
        String stateName = prepAdd.getValue();

        // Collect the transitions
        List<List<String>> transitions = new ArrayList<>();
        while (stateReader.hasNext()) {
            List<String> transition = new ArrayList<>();
            Scanner transitionReader = new Scanner(
                    stateReader.next("[\\w,]*"));
            while (transitionReader.hasNext("\\w*")) {
                transition.add(transitionReader.next("\\w*"));
            }
            transitions.add(transition);
        }

        // Add the built state to the FSA
        states.put(stateName, transitions);

        stateReader.close();

        return true;
    }

    /**
     * Run the FSA on the given string
     * @param str A string made up of characters in the FSA's alphabet
     * @return "accept" if the FSA reaches an accept state after
     *          reading in whole string, or "reject" if the FSA does not.
     *          Running the FSA on a string containing characters outside
     *          of the alphabet automatically returns "reject".
     */
    @Override
    public String runString(String str) {
        // Initialize the automaton with the starting state
        // Check if the string was accepted
        if (testPath(str, startingState)) {
            return "accept";
        } else {
            return "reject";
        }
    }

    /*
     * Helper method for processing nondeterministic paths
     */
    private boolean testPath(String remainder, String state) {
        if (remainder.length() == 0) {
            return acceptStates.contains(state);
        }

        // Check each of the possible paths
        for (String nextState : delta(state, remainder.charAt(0))) {
            // Return upon reaching an accept state
            if (testPath(remainder.substring(1), nextState)) {
                return true;
            }
        }

        // Check for epsilon transitions
        List<String> epsilonStates = delta(state, null);
        if (epsilonStates != null) {
            for (String nextState : delta(state, remainder.charAt(0))) {
                if (testPath(remainder, nextState)) {
                    return true;
                }
            }
        }

        // If the method is still running, no accept states have been reached
        return false;
    }

    /**
     * Format the NFA in the style described in Homework 2, Question 4.
     * @return A formatted multiline String describing the NFA
     */
    @Override
    public String toString() {
        // Prepare the toString with shared FSA formatting
        StringBuilder output = prepToString();

        // Append the states
        for (String state : states.keySet()) {
            // Append the accept state formatting
            output.append(
                    (acceptStates.contains(state)) ? "*"
                            : " "
            );

            // Append the state
            output.append(state);

            // Append the transition states
            for (List<String> transitionStates : states.get(state)) {
                StringBuilder statesString = new StringBuilder();
                // Loop through the resulting states
                for (int i = 0; i < transitionStates.size(); i++) {
                    // Add each state
                    statesString.append(transitionStates.size());
                    // If the state isn't the last, follow it with a comma
                    if (i < transitionStates.size() - 1) {
                        statesString.append(",");
                    }
                }
                output.append(String.format("%4s", statesString.toString()));
            }

            output.append("\n");
        }

        return output.toString();
    }

    /**
     * The transition function for this DFA.
     * @param q The state, a member of states.keySet()
     * @param l The input letter, a member of alphabet
     * @return The states determined by the transition function
     */
    protected List<String> delta(String q, Character l) {
        // Get the transition function for the current state
        List<List<String>> transitionList = states.get(q);

        // Check if the input is epsilon
        if (l == null) {
            // Check if the state has an epsilon transition
            if (transitionList.size() == alphabet.size()
                    && alphabet.contains(null)) {
                // If it does, return the epsilon transition
                return transitionList.get(alphabet.size() - 1);
            } else {
                // If not, return null as normal
                return null;
            }
        }

        // Check the index in the alphabet of this character
        int alphIndex = alphabet.indexOf(l);
        // If the character is not in the alphabet, return null
        if (alphIndex == -1) {
            return null;
        }
        // Return the state by reading the state at the same index as
        // the character in our alphabet
        return transitionList.get(alphIndex);
    }

    /**
     * Produces a copy of this NFA converted to a DFA, as described in
     * the proof for Theorem 1.39.
     * @return An equivalent deterministic FSA
     */
    public static DFA convertToDFA(NFA nfa) {
        DFA converted = new DFA(nfa.label);
        return converted;
    }

    /**
     * Generate the union of the two provided NFA's as described in the proof for Theorem
     * 1.45.
     * @param n1 The first NFA in the union
     * @param n2 The second NFA in the union
     * @return The union of n1 and n2
     */
    public static NFA union(NFA n1, NFA n2) {
        return n1;
    }

    /**
     * Generate the concatenation of the two provided NFA's as described in
     * the proof for Theorem 1.46.
     * @param n1 The first NFA in the concatenation
     * @param n2 The second NFA in the concatenation
     * @return The concatenation of n1 and n2
     */
    public static NFA concat(NFA n1, NFA n2) {
        return n1;
    }

    /**
     * Generate the star of the provided NFA as described in the proof for Theorem 1.47.
     * @param n The NFA to take the star of
     * @return The result of starring n
     */
    public static NFA star(NFA n) {
        return n;
    }
}