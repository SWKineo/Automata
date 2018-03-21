import javafx.util.Pair;

import java.util.*;

/**
 * A class for building, storing, and running nondeterministic finite-state
 * automata.
 */
public class NFA extends FSA {

    // The transitions for the array
    // Transitions are accessed through the helper method
    // delta(q: String, c: String) -> List<String>
    // setDelta(q: String, c: String, transition: List<String>)
    protected HashMap<Pair<String, String>, List<String>> transitions;

    public NFA(String label) {
        super(label);
        transitions = new HashMap<>();
    }

    /**
     * Process a raw line of input and add a new state, along with its
     * transitions, to the NFA.
     * @param rawState: A state followed by a list of transitions, lined
     *                 up with the order the characters from the alphabet
     *                 were defined
     * @return True: if the addition was a success, False if it wasn't (for
     *                  instance if the  current automaton is a DFA but the
     *                  input is destined for an NFA)
     */
    @Override
    public boolean addState(String rawState) {
        Pair<Scanner, String> prepAdd = prepAddState(rawState);
        Scanner stateReader = prepAdd.getKey();
        String stateName = prepAdd.getValue();

        // Collect the transitions
        List<List<String>> trans = new ArrayList<>();
        while (stateReader.hasNext()) {
            List<String> transition = new ArrayList<>();
            Scanner transitionReader = new Scanner(
                    stateReader.next("[\\w,]*"));
            while (transitionReader.hasNext("\\w*")) {
                transition.add(transitionReader.next("\\w*"));
            }
            trans.add(transition);
        }

        for (int i = 0; i < trans.size(); i++) {
            setDelta(stateName, alphabet.get(i), trans.get(i));
        }

        stateReader.close();

        return true;
    }

    /**
     * Run the FSA on the given string
     * @param str: A string made up of characters in the FSA's alphabet
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
        for (String nextState : delta(state, remainder.substring(0, 1))) {
            // Return upon reaching an accept state
            if (testPath(remainder.substring(1), nextState)) {
                return true;
            }
        }

        // Check for epsilon transitions
        List<String> epsilonStates = delta(state, EPSILON);
        if (epsilonStates != null) {
            for (String nextState : delta(state, remainder.substring(0, 1))) {
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
     * @return A: formatted multiline String describing the NFA
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
                StringBuilder statesString = new StringBuilder();
                // Loop through the resulting states
                List<String> transitionStates = delta(state, letter);
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
     * @param q: The state, a member of states.keySet()
     * @param a: The input letter, a member of alphabet
     * @return The: states determined by the transition function, equivalent to
     *             G(q: Q, a: S)
     */
    protected List<String> delta(String q, String a) {
        return transitions.get(new Pair<>(q, a));
    }

    /**
     * Set the transition function for a given state and character.
     * Equivalent to G(q: Q, a: S) = { q_1, q_2, ..., q_n }
     *
     * If the transition function is not yet defined for the given state or the
     * given character, this method will initialize the necessary fields.
     *
     * @param q: The state, a member of states.keySet()
     * @param a: The input letter, a member of alphabet
     * @param value: The desired transition list
     */
    protected void setDelta(String q, String a, List<String> value) {
        transitions.put(new Pair<>(q, a), value);
    }

    /*
     * Helper method to add a epsilon to the alphabet of an NFA
     */
    private void addEps() {
        if (alphabet.contains(EPSILON)) {
            alphabet.add(EPSILON);
        }
    }

    /**
     * Produces a copy of this NFA converted to a DFA, as described in
     * the proof for Theorem 1.39.
     * @param nfa: The NFA to be converted, of the form
     *            States:           Q
     *            Alphabet:         S
     *            Transitions:      G
     *            Start State:      q_0
     *            Accept States:    F
     * @return An: equivalent DFA with the form
     *            States:           Q' = P(Q)
     *            Alphabet:         S
     *            Transitions:      G'(R: Q', a: S) -> { q: Q | q: E(G(r, a)
     *                                                         for some r: R}
     *            Start State:      E(q_0)
     *            Accept States:    F' = { R: Q' | R shares an element with F}
     *
     *         where E(R) -> { q: Q | q can be reached from R in 0 or more
     *                               epsilon transitions}
     */
    public static DFA convertToDFA(NFA nfa) {
        DFA converted = new DFA(nfa.label);


        return converted;
    }

    /**
     * Generate the union of the two provided NFA's as described in the proof for Theorem
     * 1.45.
     * @param n1: The first NFA in the union, of the form
     *            States:           Q_1
     *            Alphabet:         S_1
     *            Transitions:      G_1
     *            Start State:      q_1
     *            Accept States:    F_1
     * @param n2: The second NFA in the union, of the form
     *            States:           Q_2
     *            Alphabet:         S_2
     *            Transitions:      G_2
     *            Start State:      q_2
     *            Accept States:    F_2
     * @return The: union of n1 and n2, an NFA with the form
     *            States:           Q = { q_0 } U Q_1 U Q_2
     *            Alphabet:         S = S_1 U S_2
     *            Transitions:      G(q: Q, a: S_e) -> {
     *                                  G_1(q, a) if q: Q_1
     *                                  G_2(q, a) if q: Q_2
     *                                  { q_1, q_2 }   if q = q_0 and a = _e
     *                                  null      if q = q_0 and a != _e
     *                              }
     *            Start State:      q_0
     *            Accept States:    F = F_1 U F_2
     */
    public static NFA union(NFA n1, NFA n2) {
        NFA n = new NFA(n1.label + " U " + n2.label);

        // Find q0 that isn't already being used
        int id = 0;
        String q0 = "q0";
        while (n1.states.contains(q0) || n2.states.contains(q0)) {
            // If the key already exists, check a new ID
            id++;
            q0 = "q" + Integer.toString(id);
        }

        // Q = { q_0 } U Q_1 U Q_2
        n.states = setUnion(Collections.singletonList(q0), n1.states);
        n.states = setUnion(n.states, n2.states);

        // S = S_1 U S_2
        n.alphabet = setUnion(n1.alphabet, n2.alphabet);

        // We need epsilon transitions for this operation
        n.addEps();

        n.startingState = q0;
        // F = F_1 U F_2
        n.acceptStates = setUnion(n1.acceptStates, n2.acceptStates);

        /*
         * G(q: Q, a: S_e) -> {
         *     G_1(q, a) if q: Q_1
         *     G_2(q, a) if q: Q_2
         *     { q_1, q_2 }   if q = q_0 and a = _e
         *     null      if q = q_0 and a != _e
         * }
         */
        for (String a : n.alphabet) {
            // G_1(q, a) if q: Q_1
            for (String state : n1.states) {
                n.setDelta(state, a, n1.delta(state, a));
            }
            // G_2(q, a) if q: Q_2
            for (String state : n2.states) {
                n.setDelta(state, a, n2.delta(state, a));
            }
            // null      if q = q_0 and a != _e
            n.setDelta(q0, a, null);
        }
        // { q_1, q_2 }   if q = q_0 and a = _e
        n.setDelta(q0, EPSILON,
                Arrays.asList(n1.startingState, n2.startingState));

        return n;
    }

    /**
     * Generate the concatenation of the two provided NFA's as described in
     * the proof for Theorem 1.46.
     * @param n1: The first NFA in the union, of the form
     *            States:           Q_1
     *            Alphabet:         S_1
     *            Transitions:      G_1
     *            Start State:      q_1
     *            Accept States:    F_1
     * @param n2: The second NFA in the union, of the form
     *            States:           Q_2
     *            Alphabet:         S_2
     *            Transitions:      G_2
     *            Start State:      q_2
     *            Accept States:    F_2
     * @return The: concatenation of n1 and n2, an NFA with the form
     *            States:           Q = Q_1 U Q_2
     *            Alphabet:         S = S_1 U S_2
     *            Transitions:      G(q: Q, a: S_e) -> {
     *                                  G_1(q, a) if q: Q_1 and q !: F_1
     *                                  G_1(q, a) if q: F_1 and a != _e
     *                                  G_1(q, a) U { q_2 } if q: F_1 and a = e
     *                                  G_2(q, a) if q: Q_2
     *                              }
     *            Start State:      q_1
     *            Accept States:    F_2
     */
    public static NFA concat(NFA n1, NFA n2) {
        NFA n = new NFA(n1.label + " \u25cb " + n2.label);

        // Q = Q_1 U Q_2
        n.states = setUnion(n1.states, n2.states);

        // S = S_1 U S_2
        n.alphabet = setUnion(n1.alphabet, n2.alphabet);

        // We need epsilon transitions for this operation
        n.addEps();

        // q_0 = q_1
        n.startingState = n1.startingState;

        // F = F_2
        n.acceptStates = new ArrayList<>();
        n.acceptStates.addAll(n2.acceptStates);

        /* G(q: Q, a: S_e) -> {
         *     G_1(q, a) if q: Q_1 and q !: F_1
         *     G_1(q, a) if q: F_1 and a != _e
         *     G_1(q, a) U { q_2 } if q: F_1 and a = e
         *     G_2(q, a) if q: Q_2
         * }
         */

        for (String q : n.states) {
            for (String a : n.alphabet) {
                if (n1.states.contains(q) && !n1.acceptStates.contains(q)) {
                    n.setDelta(q, a, n1.delta(q, a));
                } else if (n1.acceptStates.contains(q)) {
                    if (a.equals(EPSILON)) {
                        n.setDelta(q, a, n1.delta(q, a));
                    } else {
                        List<String> modifiedTrans = n1.delta(q, a);
                        modifiedTrans.add(n2.startingState);
                        n.setDelta(q, a, modifiedTrans);
                    }
                } else {
                    n.setDelta(q, a, n2.delta(q, a));
                }
            }
        }

        return n;
    }

    /**
     * Generate the star of the provided NFA as described in the proof for Theorem 1.47.
     * @param n: The NFA to be starred, of the form
     *            States:           Q
     *            Alphabet:         S
     *            Transitions:      G
     *            Start State:      q_1
     *            Accept States:    F
     * @return The: NFA produced by starring n, with the form
     *            States:           Q' = { q_0 } U Q_1
     *            Alphabet:         S
     *            Transitions:      G'(q: Q', a: S) -> {
     *                                  G(q, a)           if q: Q and q !: F
     *                                  G(q, a)           if q: F and a != _e
     *                                  G(q, a) U { q_1 } if q: F and a = _e
     *                                  { q_1 }           if q = q_0 and a = _e
     *                                  null              if q = q_0 and a != _e
     *                              }
     *            Start State:      q_0
     *            Accept States:    F' = { q_0 } U F
     */
    public static NFA star(NFA n) {
        NFA newN = new NFA(n.label);

        // Find q0 that isn't already being used
        int id = 0;
        String q0 = "q0";
        while (n.states.contains(q0)) {
            // If the key already exists, check a new ID
            id++;
            q0 = "q" + Integer.toString(id);
        }

        // Q' = { q_0 } U Q_1
        newN.states = setUnion(Collections.singletonList(q0), n.states);

        // S' = S
        newN.alphabet.addAll(n.alphabet);

        // We need epsilon transitions for this operation
        newN.addEps();

        /*
         * G'(q: Q', a: S) -> {
         *     G(q, a)           if q: Q and q !: F
         *     G(q, a)           if q: F and a != _e
         *     G(q, a) U { q_1 } if q: F and a = _e
         *     { q_1 }           if q = q_0 and a = _e
         *     null              if q = q_0 and a != _e
         * }
         */

        for (String q : newN.states) {
            for (String a : newN.alphabet) {
                if ((!q.equals(q0) && !n.acceptStates.contains(q))
                        || n.acceptStates.contains(q) && !a.equals(EPSILON)) {
                    newN.setDelta(q, a, n.delta(q, a));
                } else if (n.acceptStates.contains(q) && a.equals(EPSILON)) {
                    List<String> moddedTrans = n.delta(q, a);
                    moddedTrans.add(n.startingState);
                    newN.setDelta(q, a, moddedTrans);
                } else if (q.equals(q0)) {
                    if (a.equals(EPSILON)) {
                        newN.setDelta(q, a,
                                Collections.singletonList(n.startingState));
                    } else {
                        newN.setDelta(q, a, null);
                    }
                }
            }
        }

        // start = q0
        newN.startingState = q0;

        // F' = { q_0 } U F
        newN.acceptStates =
                setUnion(Collections.singletonList(q0), n.acceptStates);

        return newN;
    }
}