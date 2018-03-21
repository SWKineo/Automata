import javafx.util.Pair;

import java.util.*;

/**
 * A class for building, storing, and running deterministic finite-state
 * automata.
 */
public class DFA extends FSA {

    // The transitions for the array
    // Transitions are accessed through the helper method
    // delta(q: String, c: String) -> String
    // setDelta(q: String, c: String, transition: String)
    protected HashMap<Pair<String, String>, String> transitions;

    public DFA(String label) {
        super(label);
        transitions = new HashMap<>();
    }

    /**
     * Process a raw line of input and add a new state, along with its
     * transitions, to the DFA.
     * @param rawState: A state followed by a list of transitions, lined
     *                 up with the order the characters from the alphabet
     *                 were defined
     * @return True: if the addition was a success, False if it wasn't (for
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
        ArrayList<String> trans = new ArrayList<>();
        while (stateReader.hasNext()) {
            trans.add(stateReader.next());
        }

        for (int i = 0; i < transitions.size(); i++) {
            setDelta(stateName, alphabet.get(i), trans.get(i));
        }

        stateReader.close();

        return true;
    }

    /**
     * Run the FSA on the given string
     * @param str: A string made up of characters in the FSA's alphabet
     * @return "accept": if the FSA reaches an accept state after
     *          reading in whole string, or "reject" if the FSA does not.
     *          Running the FSA on a string containing characters outside
     *          of the alphabet automatically returns "reject".
     */
    @Override
    public String runString(String str) {
        // Initialize the automaton with the starting state
        String currentState = startingState;

        // Read the string character by character
        for (Character c : str.toCharArray()) {
            currentState = delta(currentState, c.toString());
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
     * @return A: formatted multiline String describing the DFA
     */
    @Override
    public String toString() {
        // Prepare the toString with shared FSA formatting
        StringBuilder output = prepToString();

        // Append the states
        for (String state : states) {
            // Append the accept state formatting
            output.append(
                    (acceptStates.contains(state)) ? "*"
                            : " "
            );

            // Append the state
            output.append(state);

            // Append the transition states
            for (String letter : alphabet) {
                output.append(String.format("%4s", delta(state, letter)));
            }

            output.append("\n");
        }

        return output.toString();
    }

    /**
     * The transition function for this DFA.
     * @param q: The state, a member of states.keySet()
     * @param s: The input letter, a member of alphabet
     * @return The: state determined by the transition function, equivalent to
     *             G(q: Q, a: S)
     */
    protected String delta(String q, String s) {

        return transitions.get(new Pair<> (q, s));
    }

    /**
     * Set the transition function for a given state and character.
     * Equivalent to G(q: Q, a: S) = q'
     *
     * If the transition function is not yet defined for the given state or the
     * given character, this method will initialize the necessary fields.
     *
     * @param q: The state, a member of states.keySet()
     * @param s: The input letter, a member of alphabet
     * @param value: The desired transition state
     */
    protected void setDelta(String q, String s, String value) {
        transitions.put(new Pair<>(q, s), value);
    }

    /**
     * Generate the union of two DFA's as described in the proof for Theorem
     * 1.25
     * @param d1: The first DFA in the union, of the form
     *            States:           Q_1
     *            Alphabet:         S_1
     *            Transitions:      G_1
     *            Start State:      q_1
     *            Accept States:    F_1
     * @param d2: The second DFA in the union, of the form
     *            States:           Q_2
     *            Alphabet:         S_2
     *            Transitions:      G_2
     *            Start State:      q_2
     *            Accept States:    F_2
     * @return The: union of the two DFA's, a DFA with the form
     *            States:           Q = Q_1 X Q_2
     *            Alphabet:         S = S_1 U S_2
     *            Transitions:      G((r_1: Q_1, r_2: Q_2), a: S) ->
     *                                  (G_1(r_1, a), G_2(r_2, a))
     *            Start State:      q0 = (q_1, q_2)
     *            Accept States:    F = (F_1 X Q_2) U (Q_1 X F_2)
     */
    public static DFA union(DFA d1, DFA d2) {
        DFA u = new DFA(d1 + " U "  + d2);

        // Q = Q_1 X Q_2
        for (String q1 : d1.states) {
            for (String q2 : d2.states) {
                u.states.add(tuple(q1, q2));
            }
        }

        // S = S_1 U S_2
        u.alphabet = setUnion(d1.alphabet, d2.alphabet);

        // q_0 = (q_1, q_2)
        u.startingState =
               tuple(d1.startingState, d2.startingState);

        // F = (F_1 x Q_2) U (Q_1 x F_2)
        u.acceptStates = new ArrayList<>();
        // F_1 x Q_2
        for (String f : d1.acceptStates) {
            for (String q : d2.states) {
                u.acceptStates.add(tuple(f, q));
            }

        }
        // Q_1 x F_2
        for (String q : d1.states) {
            for (String f : d2.acceptStates) {
                u.acceptStates.add(tuple(q, f));
            }
        }

        // delta((r_1, r_2), a) = (delta_1(r_1, a), delta_2(r_2, a))
        for (String r1 : d1.states) {
            for (String r2 : d2.states) {
                for (String c : u.alphabet) {
                    u.setDelta(tuple(r1, r2), c, tuple(d1.delta(r1, c), d2.delta(r2, c)));
                }
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
     * @param d: The DFA to be converted to an NFA, of the form
     *            States:           Q
     *            Alphabet:         S
     *            Transitions:      G
     *            Start State:      q0
     *            Accept States:    F
     * @return The: NFA generated from the given DFA, with the form
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

        n.states.addAll(d.states);
        n.alphabet.addAll(d.alphabet);
        n.startingState = d.startingState;
        n.acceptStates.addAll(d.acceptStates);

        // Copy each state q as { q }
        for (String q : d.states) {
            for (String c : d.alphabet) {
                n.setDelta(q, c, Collections.singletonList(d.delta(q, c)));
            }
        }

        return n;
    }
}
