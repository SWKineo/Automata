import javafx.util.Pair;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author: Spencer Ward
 * @date: February 15, 2017
 *
 * An interactive interpreter for the language Lexaard, defined in Homework 2
 * Question 4.
 */

public class LexaardInterpreter {

    // The registered objects of the interpreter
    private HashMap<String, String> registeredStrings;
    private HashMap<String, FSA> registeredAutoma;
    private Scanner input;
    private Flag signal;

    // Runtime flags
    private enum Flag {
        RUN, ERROR, EXIT
    }

    public static void main(String... args) {
        new LexaardInterpreter().run();
    }

    public LexaardInterpreter() {
        registeredStrings = new HashMap<>();
        registeredAutoma = new HashMap<>();
        signal = Flag.RUN;
        input = new Scanner(System.in);
    }

    /**
     * Prompt for input and process commands until the user requests to exit.
     */
    private void run() {
        do {
            String newLine = input.nextLine().trim();
            if (!newLine.equals("")) {
                processCommand(newLine);
                if (signal == Flag.ERROR) {
                    System.out.println("Please check your command formatting");
                }
            }
        } while (signal != Flag.EXIT);

        input.close();
    }

    /**
     * Process commands and their arguments.
     * @param userInput: The raw user input, trimmed of spaces
     */
    private void processCommand(String userInput) {
        Scanner line = new Scanner(userInput.trim());
        String command = line.next();

        try {
            // Determine the command and process it
            switch(command) {
                case "quit":
                    signal = Flag.EXIT;
                    break;
                case "print":
                    String key = line.next();

                    // Check if the object exists, then print it if it does
                    if (registeredStrings.containsKey(key)) {
                        System.out.println(registeredStrings.get(key));
                    } else if (registeredAutoma.containsKey(key)) {
                        System.out.println(registeredAutoma.get(key).toString());
                    }

                    signal = Flag.RUN;
                    break;
                case "define":
                    registerObject(line.nextLine().trim());
                    signal = Flag.RUN;
                    break;
                case "run":
                    // Read in the FSA label
                    String fsaName = line.next();
                    String str;

                    Matcher stringMatcher = Pattern.compile("(\"?)(\\w*)")
                            .matcher(line.next());

                    // Search the line for the regex
                    stringMatcher.find();

                    if (stringMatcher.group(1).equals("\"")) {
                        str = stringMatcher.group(2);
                    } else {
                        String strName = stringMatcher.group(2);
                        str = registeredStrings.get(strName);
                    }

                    FSA requestedFSA = registeredAutoma.get(fsaName);
                    System.out.println(requestedFSA.runString(str));

                    signal = Flag.RUN;
                    break;
                default:
                    signal = Flag.ERROR;
            }
        } catch (Exception e) {
            signal = Flag.ERROR;
        }

        line.close();
    }

    /**
     * Register a string or an FSA by label.
     * @param object: The raw arguments from the command
     */
    private void registerObject(String object) {
        // Regex to match the form '<label> "<text>"'
        Matcher stringMatch = Pattern.compile("(\\w+)\\s+\"(\\w*)\"")
                .matcher(object);
        // Regex to match the form '<label> fsa'
        Matcher fsaMatch = Pattern.compile("(\\w+)\\s+fsa").matcher(object);

        if (stringMatch.matches()) {
            // Register a new string with the given label and text
            registerString(stringMatch.group(1), stringMatch.group(2));
        } else if (fsaMatch.matches()) {

            // Create a new FSA as it's being built
            FSA newAutomaton = new DFA(input.next().trim());
            // Advance to the next line
            input.nextLine();

            // Read in and set the alphabet for the FSA
            int nfaFlag = newAutomaton.setAlphabet(input.nextLine().trim());

            // Check if the automaton needs to be converted to an NFA
            if (nfaFlag == 1) {
                newAutomaton = DFA.convertToNFA((DFA) newAutomaton);
            }

            // Read in the lines of states
            String nextLine = input.nextLine().trim();
            while (!nextLine.equals("")) {
                if (!newAutomaton.addState(nextLine)
                        && newAutomaton instanceof DFA) {
                    // If the add fails, the DFA needs to be converted to an NFA
                    newAutomaton = DFA.convertToNFA((DFA) newAutomaton);
                    // Try again with the converted automaton
                    newAutomaton.addState(nextLine);
                }
                nextLine = input.nextLine().trim();
            }

            // Register the newly created FSA
            registerFSA(fsaMatch.group(1), newAutomaton);
        }
    }

    /**
     * Register a parsed string
     * @param name: The label to attach to the stored string
     * @param str: The text that the string contains
     */
    private void registerString(String name, String str) {
        // If an automaton is already registered with this label, replace it
        // with the new string
        if (registeredAutoma.containsKey(name)) {
            registeredAutoma.remove(name);
        }

        // Add the new string
        registeredStrings.put(name, str);
    }

    /**
     * Register a parsed FSA
     * @param name: The label to attach to the stored FSA
     * @param fsa: The object that matches the user's desired FSA
     */
    private void registerFSA(String name, FSA fsa) {
        // If a string is already registered by this label, replace it with
        // the new automaton
        if (registeredStrings.containsKey(name)) {
            registeredStrings.remove(name);
        }

        registeredAutoma.put(name, fsa);
    }

    /**
     * Convert the given NFA to an equivalent DFA.
     * @param fsa: The NFA (or DFA) to be converted
     * @return The: provided FSA converted to a DFA
     */
    private DFA nfa2dfa(FSA fsa) {
        if (fsa instanceof NFA) {
            return NFA.convertToDFA((NFA) fsa);
        } else {
            // If the FSA is not an NFA, then no conversion is necessary
            return (DFA) fsa;
        }
    }

    /**
     * Compute the union of two DFA's.
     * @param dfa1: The first DFA in the union
     * @param dfa2: The second DFA in the union
     * @return The: union of the two provided DFA's
     */
    private DFA dfaUnion(FSA dfa1, FSA dfa2) {
        if (dfa1 instanceof DFA && dfa2 instanceof DFA) {
            return DFA.union((DFA) dfa1, (DFA) dfa2);
        } else {
            return null;
        }
    }

    /**
     * Compute the union of two NFA's.
     * @param nfa1: The first NFA in the union
     * @param nfa2: The second NFA in the union
     * @return The: union of the two provided NFA's
     */
    private NFA nfaUnion(FSA nfa1, FSA nfa2) {
        if (nfa1 instanceof NFA && nfa2 instanceof  NFA) {
            return NFA.union((NFA) nfa1, (NFA) nfa2);
        } else {
            return null;
        }
    }

    /**
     * Generate the concatenation of the two provided NFA's.
     * @param nfa1: The first NFA in the concatenation
     * @param nfa2: The second NFA in the concatenation
     * @return The: concatenation of the two NFA's
     */
    private NFA nfaConcat(FSA nfa1, FSA nfa2) {
        if (nfa1 instanceof NFA && nfa2 instanceof  NFA) {
            return NFA.concat((NFA) nfa1, (NFA) nfa2);
        } else {
            return null;
        }
    }

    /**
     * Generate the star of the provided NFA.
     * @param nfa: The NFA to take the star of
     * @return The: result of starring the NFA
     */
    private NFA nfaStar(FSA nfa) {
        if (nfa instanceof NFA) {
            return NFA.star((NFA) nfa);
        } else {
            return null;
        }
    }

    /**
     * Remove unreachable states from the given FSA.
     * @param fsa: The FSA to be pruned
     * @return A: copy of the FSA that has been pruned
     */
    private FSA pruneFSA(FSA fsa) {
        return FSA.prune(fsa);
    }

    /**
     * Check if the given FSA's are equivalent, as described in Theorem 4.5
     * @param fsa1: The first FSA
     * @param fsa2: The second FSA
     * @return True: if the two FSA's are equivalent, false otherwise
     */
    private boolean fsaEquivP(FSA fsa1, FSA fsa2) {
        return FSA.equivP(fsa1, fsa2);
    }
}
