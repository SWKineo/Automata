import javafx.util.Pair;

import java.util.*;

/**
 * A class for building, storing, and running deterministic finite-state
 * automata.
 */
public class DFA extends FSA {

    // The states of the array
    // * The states are accessible through states.keySet(): Set<String>
    // * The transition function is used through
    //      delta(state: String, letter: String): String
    // * The transitions are stored as an List value, ordered to
    //      match the indices of the alphabet characters.
    protected HashMap<String, List<String>> states;

    public DFA(String label) {
        super(label);
        states = new HashMap<>();
    }

    /**
     * Process a raw line of input and add a new state, along with its
     * transitions, to the DFA.
     * @param rawState A state followed by a list of transitions, lined
     *                 up with the order the characters from the alphabet
     *                 were defined
     * @return True if the addition was a success, False if it wasn't (for
     *                  instance if the  current automaton is a DFA but the
     *                  input is destined for an NFA)
     */
    @Override
    public boolean addState(String rawState) {
        // Check if the input is destined for an NFA
        if (rawState.contains(",")) return false;

        Pair<Scanner, String> prepAdd = prepAddState(rawState);
        Scanner stateReader = prepAdd.getKey();
        String stateName = prepAdd.getValue();

        // Collect the transitions
        ArrayList<String> transitions = new ArrayList<>();
        while (stateReader.hasNext()) {
            transitions.add(stateReader.next());
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
        String currentState = startingState;

        // Read the string character by character
        for (char c : str.toCharArray()) {
            currentState = delta(currentState, c);
        }

        // Check if the string was accepted
        if (acceptStates.contains(currentState)) {
            return "accept";
        } else {
            return "reject";
        }
    }

    /**
     * Format the DFA in the style described in Homework 2, Question 4.
     * @return A formatted multiline String describing the DFA
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
            for (String transitionState : states.get(state)) {
                output.append(String.format("%4s", transitionState));
            }

            output.append("\n");
        }

        return output.toString();
    }

    /**
     * The transition function for this DFA.
     * @param q The state, a member of states.keySet()
     * @param l The input letter, a member of alphabet
     * @return The state determined by the transition function, equivalent to
     *             G(q: Q, a: S)
     */
    protected String delta(String q, char l) {
        // Get the transition function for the current state
        List<String> transitionList = states.get(q);
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
     * Generate the union of two DFA's as described in the proof for Theorem
     * 1.25
     * @param d1 The first DFA in the union, of the form
     *            States:           Q_1
     *            Alphabet:         S_1
     *            Transitions:      G_1
     *            Start State:      q_1
     *            Accept States:    F_1
     * @param d2 The second DFA in the union, of the form
     *            States:           Q_2
     *            Alphabet:         S_2
     *            Transitions:      G_2
     *            Start State:      q_2
     *            Accept States:    F_2
     * @return The union of the two DFA's, a DFA with the form
     *            States:           Q = Q_1 X Q_2
     *            Alphabet:         S = S_1 U S_2
     *            Transitions:      G((r_1: Q_1, r_2: Q_2), a: S) ->
     *                                  (G_1(r_1, a), G_2(r_2, a))
     *            Start State:      q0 = (q_1, q_2)
     *            Accept States:    F = (F_1 X Q_2) U (Q_1 X F_2)
     */
    public static DFA union(DFA d1, DFA d2) {
        DFA u = new DFA(d1 + " U "  + d2);

        // S = S_1 U S_2
        u.alphabet = new ArrayList<>(d1.alphabet);
        u.alphabet.addAll(d2.alphabet);

        // q_0 = (q_1, q_2)
        u.startingState =
               tuple(d1.startingState, d2.startingState);

        // F = (F_1 x Q_2) U (Q_1 x F_2)
        u.acceptStates = new ArrayList<>();
        for (String f : d1.acceptStates) {
            for (String q : d2.states.keySet()) {
                u.acceptStates.add(tuple(f, q));
            }
        }
        for (String q : d1.states.keySet()) {
            for (String f : d2.acceptStates) {
                u.acceptStates.add(tuple(q, f));
            }
        }

        // delta((r_1, r_2), a) = (delta_1(r_1, a), delta_2(r_2, a))
        for (String q1 : d1.states.keySet()) {
            for (String q2 : d2.states.keySet()) {
                List<String> transitions = new ArrayList<>();
                for (char c : u.alphabet) {
                    transitions.add(tuple(d1.delta(q1, c), d2.delta(q2, c)));
                }
                // Add the transitions to the state
                // Q = Q_1 x Q2
                u.states.put(tuple(q1, q2), transitions);
            }
        }

        return u;
    }

    /*
     * Helper method for formatting 2-tuples
     */
    private static String tuple(String a, String b) {
        return String.format("(%s, %s)", a, b);
    }

    /**
     * Converts the given DFA to an NFA, programmatically
     * (this is a trivial helper method used for processing input)
     * @param d The DFA to be converted to an NFA, of the form
     *            States:           Q
     *            Alphabet:         S
     *            Transitions:      G
     *            Start State:      q0
     *            Accept States:    F
     * @return The NFA generated from the given DFA, with the form
     *            States:           Q
     *            Alphabet:         S
     *            Transitions:      G'(q: Q, a: S_e) -> {
     *                                  null            if a == _e
     *                                  { G(q, a) }     if a != _e
     *                              }
     *            Start State:      q0
     *            Accept States:    F
     */
    public static NFA convertToNFA(DFA d) {
        // Copy the fields of the DFA to the NFA
        NFA n = new NFA(d.label);
        n.alphabet = d.alphabet;
        n.startingState = d.startingState;
        n.acceptStates = d.acceptStates;

        // Copy each state q as {q}
        for (String q : d.states.keySet()) {
            List<List<String>> transitions = new ArrayList<>();
            for (char c : d.alphabet) {
                transitions.add(new ArrayList<>(
                        Collections.singletonList(d.delta(q, c))));
            }
            n.states.put(q, transitions);
        }

        return n;
    }
}
