import javafx.util.Pair;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A class for building, storing, and running finite-state automata.
 *
 *
 * Quick Note:
 *     Throughout the FSA, DFA, and NFA classes I use
 *         newList.addAll(oldList)
 *     in place of
 *         newList = oldList.
 *     This is just a simple way to copy all of the elements from the old list
 *     into the new one without copying the list by reference, since many of the
 *     automata operations modify these new lists.
 */
public abstract class FSA {

    // The character to use for epsilon transitions
    final static String EPSILON = "\u03B5";

    // The label for the FSA
    protected String label;

    // The states of the FSA
    protected List<String> states;

    // The usable alphabet of the FSA
    protected List<String> alphabet;

    // The set of accept states
    protected List<String> acceptStates;

    // The current state of the FSA
    // * Set to the start state during initialization
    protected String startingState;

    /**
     * Create a new FSA with the given label.
     * @param label A descriptive identifier for the automaton
     */
    public FSA(String label) {
        this.label = label;
        states = new ArrayList<>();
        alphabet = new ArrayList<>();
        acceptStates = new ArrayList<>();
    }
    
    /*
     * Helper function for performing set-style unions on list of Strings
     */
    protected static List<String> setUnion(List<String> a, List<String> b) {
        List<String> newList = new ArrayList<>();

        // Flag for epsilon
        boolean epsFlag = false;

        for (String s : a) {
            if (s.equals(EPSILON)) {
                // Don't add epsilons normally
                epsFlag = true;
            } else {
                newList.add(s);
            }
        }
        for (String s : b) {
            if (s.equals(EPSILON)) {
                // Don't add epsilons normally
                epsFlag = true;
            } else {
                // Only add values from the second list if they're not already
                // in the new list
                if (!newList.contains(s)) {
                    newList.add(s);
                }
            }
        }

        // Add the epsilon at the end if necessary
        if (epsFlag) {
            newList.add(EPSILON);
        }

        return newList;
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
        for (String c : alphabet) {
            if(!c.equals(EPSILON)) {
                output.append(c);
                output.append("   ");
            } else {
                // Output for epsilon transitions
                output.append("..");
                output.append("  ");
            }
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
        Scanner alphabetReader = new Scanner(rawAlphabet.trim());

        // Read in the characters on the line
        while (alphabetReader.hasNext()) {
            String next = alphabetReader.next();
            if (next.equals("..")) {
                type = 1;
                alphabet.add(EPSILON);
            } else {
                // Only read the next non-whitespace character
                alphabet.add(next.substring(0, 1));
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

        /*
         * This split is necessary because the states and transition functions
         * are implemented differently for DFA's and NFA's
         */
        if (fsa instanceof DFA) {
            // Save ourselves some ((DFA) fsa) casts
            DFA fsaD = (DFA) fsa;
            // Save ourselves some ((DFA) pruned) casts
            DFA prunedD = new DFA(fsa.label);
            pruned = prunedD;

            // Get the reachable states for the DFA
            List<String> reachable = reachableStatesD(fsaD, Collections
                    .singletonList(fsaD.startingState));

            // Add every reachable state to the pruned DFA
            for (String state : reachable) {
                // Add the state
                prunedD.states.add(state);
                // Add the transition function
                for (String letter : fsaD.alphabet) {
                    prunedD.setDelta(state, letter, fsaD.delta(state, letter));
                }
                // If the state is an accept state, add it to the accept states
                // of the pruned automaton
                if (fsaD.acceptStates.contains(state)) {
                    prunedD.acceptStates.add(state);
                }
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
                for (String letter : fsaN.alphabet) {
                    prunedN.setDelta(state, letter, fsaN.delta(state, letter));
                }
                // If the state is an accept state, add it to the accept states
                // of the pruned automaton
                if (fsaN.acceptStates.contains(state)) {
                    prunedN.acceptStates.add(state);
                }
            }
        }

        // Copy the alphabet and start state
        pruned.alphabet.addAll(fsa.alphabet);
        pruned.startingState = fsa.startingState;

        return pruned;
    }

    /*
     * Helper method for recursively checking accessible states of the
     * automata, starting from the start state's transition function and
     * adding other transition functions to to a list as it is being checked.
     *
     * Informal Proof.
     * ---------------
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
            for (String l : d.alphabet) {
                String unseen = d.delta(s, l);
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
            for (String l : n.alphabet) {
                List<String> possibilities = n.delta(s, l);
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
     *
     * This algorithm constructs a new automaton C defined as
     * C = (A intersect ^B) U (B intersect ^A).
     *
     * Informal Proof.
     * ---------------
     * If C contains no reachable accept states, then L(C) is empty. So there
     * are no strings accepted by A that are not accepted by B.
     * Thus A and B are equivalent.
     *
     * Similarly if A and B are equivalent, then there are no strings accepted
     * by A that are not accepted by B. So L(C) will be empty. Thus C will
     * contain no reachable accept states.
     *
     * Therefore A and B are equivalent iff C contains no accept states.
     *
     * @param a The first FSA
     * @param b The second FSA
     * @return True if the two FSA's are equivalent, false otherwise
     */
    public static boolean equivP(FSA a, FSA b) {
        // To save time, we'll convert our FSA's to NFA's
        return false;
    }
}