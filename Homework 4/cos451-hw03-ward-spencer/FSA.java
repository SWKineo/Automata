import javafx.util.Pair;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A class for building, storing, and running finite-state automata.
 */
public abstract class FSA {

    // The label for the FSA
    protected String label;

    /* states and the transition function are handled in subclasses */

    // The usable alphabet of the FSA
    protected ArrayList<Character> alphabet;

    // The set of accept states
    protected ArrayList<String> acceptStates;

    // The current state of the FSA
    // * Set to the start state during initialization
    protected String startingState;

    /**
     * Create a new FSA with the given label.
     * @param label A descriptive identifier for the automaton
     */
    public FSA(String label) {
        this.label = label;
        acceptStates = new ArrayList<>();
    }


    /**
     * Process a raw line of input and add a new state, along with its
     * transitions, to the FSA.
     * @param rawState A state followed by a list of transitions, lined
     *                 up with the order the characters from the alphabet
     *                 were defined
     * @return True if the addition was a success, False if it wasn't (for
     *                  instance if the  current automaton is a DFA but the
     *                  input is destined for an NFA)
     */
    public abstract boolean addState(String rawState);

    /*
     * Helper function for shared formatting
     */
    protected Pair<Scanner, String> prepAddState(String rawState) {
        Scanner stateReader = new Scanner(rawState.trim());

        String stateName;

        // This is kind of ugly, but for some reason I like this better
        // than messing around with substrings and checking character
        // locations.
        // Regex to match the form '<state>' or '*<state>'
        Matcher stateMatcher = Pattern.compile("(\\*?)(\\w+)").matcher
                (stateReader.next());

        // Search the line for the regex
        stateMatcher.find();

        // Check if the state is an accept state
        if (stateMatcher.group(1).equals("*")) {
            // Only grab the label so we ignore the * notation
            stateName = stateMatcher.group(2);
            acceptStates.add(stateName);
        } else {
            stateName = stateMatcher.group(2);
        }

        // If this is the first state created, set it as the starting state
        if (startingState == null) {
            startingState = stateName;
        }

        return new Pair<>(stateReader, stateName);
    }

    /**
     * Format the FSA in the style described in Homework 2, Question 4.
     * @return A formatted multiline String describing the FSA
     */
    @Override
    public abstract String toString();

    /*
     * Helper function for shared formatting
     */
    protected StringBuilder prepToString() {
        StringBuilder output = new StringBuilder();

        // Append the label
        output.append(label);
        output.append("\n");

        // Append the alphabet
        output.append("     ");
        for (Character c : alphabet) {
            output.append(c);
            output.append("   ");
        }

        output.append("\n");

        return output;
    }

    /**
     * Run the FSA on the given string
     * @param str A string made up of characters in the FSA's alphabet
     * @return "accept" if the FSA reaches an accept state after
     *          reading in whole string, or "reject" if the FSA does not.
     *          Running the FSA on a string containing characters outside
     *          of the alphabet automatically returns "reject".
     */
    public abstract String runString(String str);

    /**
     * Process a raw line of input and set the alphabet of the FSA to the
     * given values.
     * @param rawAlphabet A string of characters separated by whitespace
     * @return 0 if the input seems to be destined for a DFA, 1 if the input
     * is destined for an NFA (i.e. its alphabet includes epsilon)
     */
    public int setAlphabet(String rawAlphabet) {
        // The type of the FSA: 0 for DFA, 1 for NFA
        int type = 0;
        alphabet = new ArrayList<>();
        Scanner alphabetReader = new Scanner(rawAlphabet.trim());

        // Read in the characters on the line
        while (alphabetReader.hasNext()) {
            String next = alphabetReader.next();
            if (next.equals("..")) {
                type = 1;
                alphabet.add(null);
            } else {
                // Only read the next non-whitespace character
                alphabet.add(next.charAt(0));
            }
        }

        alphabetReader.close();

        return type;
    }

    /**
     * Remove unreachable states.
     * @param fsa The FSA to be pruned
     * @return A copy of the FSA that has been pruned
     */
    public static FSA prune(FSA fsa) {
        // Create a new DFA or NFA, copying the label from the old one
        FSA pruned;
        Set<String> unseenStates;
        if (fsa instanceof DFA) {
            // Save ourselves some ((DFA) fsa) casts
            DFA fsaD = (DFA) fsa;
            // Save ourselves some ((DFA) pruned) casts
            DFA prunedD = new DFA(fsa.label);
            pruned = prunedD;

            // Get the reachable states for the DFA
            List<String> reachable = reachableStatesD(fsaD, Collections
                    .singletonList(fsaD.startingState));

            // Add every reachable state and its transition function to the
            // pruned DFA
            for (String state : reachable) {
                prunedD.states.put(state, fsaD.states.get(state));
            }
        } else {
            // Save ourselves some ((DFA) fsa) casts
            NFA fsaN = (NFA) fsa;
            // Save ourselves some ((NFA) pruned) casts
            NFA prunedN = new NFA(fsa.label);
            pruned = prunedN;

            // Get the reachable states for the DFA
            List<String> reachable = reachableStatesN(fsaN, Collections
                    .singletonList(fsaN.startingState));

            // Add every reachable state and its transition function to the
            // pruned NFA
            for (String state : reachable) {
                prunedN.states.put(state, fsaN.states.get(state));
            }
        }

        // Copy the alphabet and start state
        pruned.alphabet = fsa.alphabet;
        pruned.startingState = fsa.startingState;

        return pruned;
    }

    /*
     * Helper method for recursively checking accessible states of the
     * automata, starting from the start state's transition function and
     * adding other transition functions to to a list as it is being checked.
     *
     * Informal proof of correctness:
     * Initialization: This method is started with seenStates containing only
     *      the start state. Therefore at the start of the method seenStates
     *      contains only reachable states.
     * Maintenance: On every run, the method only adds a single node to the
     *      list of seenStates, and only after it's been found in the
     *      transition method for a previously seen state. The method then
     *      calls itself, with the condition held that seenStates contains
     *      only reachable states.
     * Termination: When none of the transition functions for the states in
     *      seenStates contain unchecked states, seenStates contains every
     *      reachable state, and the method exits.
     */
    private static List<String> reachableStatesD(DFA d,
                                                List<String> seenStates) {
        // Check the transitions for every previously encountered state
        for (String s : seenStates) {
            // Check every state in the transition for the encountered state
            for (String unseen : d.states.get(s)) {
                // If an unencountered state is found in the transition,
                if (!seenStates.contains(unseen)) {
                    // Add it to the encountered states list
                    seenStates.add(unseen);
                    // Rerun the method with the new seenStates list, return
                    // on completion
                    return reachableStatesD(d, seenStates);
                }
            }
        }

        // The loop above will only complete if there are no new unchecked
        // states. Therefore all reachable states have been found.
        return seenStates;
    }

    /*
     * This method is the same as the one above, with the slight change that
     * it loop through each sublist of the transition functions to check
     * every state.
     */
    private static List<String> reachableStatesN(NFA n,
                                                 List<String> seenStates) {
        // Check the transitions for every previously encountered state
        for (String s : seenStates) {
            // Check every sublist in the transition
            for (List<String> possibilities : n.states.get(s)) {
                // Check every state in the transition for the encountered state
                for (String unseen : possibilities) {
                    // If an unencountered state is found in the transition,
                    if (!seenStates.contains(unseen)) {
                        // Add it to the encountered states list
                        seenStates.add(unseen);
                        // Rerun the method with the new seenStates list, return
                        // on completion
                        return reachableStatesN(n, seenStates);
                    }
                }
            }
        }

        // The loop above will only complete if there are no new unchecked
        // states. Therefore all reachable states have been found.
        return seenStates;
    }

    /**
     * Check if the given FSA's are equivalent, as described in the proof for
     * Theorem 4.5.
     * @param f1 The first FSA
     * @param f2 The second FSA
     * @return True if the two FSA's are equivalent, false otherwise
     */
    public static boolean equivP(FSA f1, FSA f2) {
        return false;
    }
}