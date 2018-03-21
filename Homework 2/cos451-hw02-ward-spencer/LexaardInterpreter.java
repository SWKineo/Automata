import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Spencer Ward
 * @date February 15, 2017
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
     * @param userInput The raw user input, trimmed of spaces
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
     * @param object The raw arguments from the command
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
            FSA newAutomaton = new FSA(input.next().trim());
            // Advance to the next line
            input.nextLine();

            // Read in and set the alphabet for the FSA
            newAutomaton.setAlphabet(input.nextLine().trim());

            // Read in the lines of states
            String nextLine = input.nextLine().trim();
            while (!nextLine.equals("")) {
                newAutomaton.addState(nextLine);
                nextLine = input.nextLine().trim();
            }

            // Register the newly created FSA
            registerFSA(fsaMatch.group(1), newAutomaton);
        }
    }

    /**
     * Register a parsed string
     * @param name The label to attach to the stored string
     * @param str The text that the string contains
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
     * @param name The label to attach to the stored FSA
     * @param fsa The object that matches the user's desired FSA
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
     * A class for building and storing FSA's.
     */
    private class FSA {

        // The label for the FSA
        private String label;

        // The usable alphabet of the FSA
        // * This could just as easily be a char[] or a String[], but this
        //      was the simplest type to work with
        private ArrayList<Character> alphabet;

        // The states of the array
        // * The states themselves are stored as keys to a map.
        // * The transitions are stored as an ArrayList value, ordered to
        //      match the indices of the alphabet characters.
        private HashMap<String, ArrayList<String>> states;

        // The set of accept states
        private ArrayList<String> acceptStates;

        // The current state of the FSA
        // * Set to the start state during initialization
        private String startingState;

        /**
         * Create a new FSA with the given label.
         * @param label A descriptive identifer for the automaton
         */
        public FSA(String label) {
            this.label = label;
            states = new HashMap<>();
            acceptStates = new ArrayList<>();
        }

        /**
         * Process a raw line of input and set the alphabet of the FSA to the
         * given values.
         * @param rawAlphabet A string of characters separated by whitespace
         */
        public void setAlphabet(String rawAlphabet) {

            alphabet = new ArrayList<>();
            Scanner alphabetReader = new Scanner(rawAlphabet.trim());

            // Read in the characters on the line
            while (alphabetReader.hasNext()) {
                // Only read the next non-whitespace character
                alphabet.add(alphabetReader.next("\\S").charAt(0));
            }

            alphabetReader.close();
        }

        /**
         * Process a raw line of input and add a new state, along with its
         * transitions, to the FSA.
         * @param rawState A state followed by a list of transitions, lined
         *                 up with the order the characters from the alphabet
         *                 were defined
         */
        public void addState(String rawState) {
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

            // Collect the transitions
            ArrayList<String> transitions = new ArrayList<>();
            while (stateReader.hasNext()) {
                transitions.add(stateReader.next());
            }

            // Add the built state to the FSA
            states.put(stateName, transitions);

            stateReader.close();
        }

        /**
         * Run the FSA on the given string
         * @param str A string made up of characters in the FSA's alphabet
         * @return "accept" if the FSA reaches an accept state after
         *          reading in whole string, or "reject" if the FSA does not.
         *          Running the FSA on a string containing characters outside
         *          of the alphabet automatically returns "reject".
         */
        public String runString(String str) {
            // Initialize the automaton with the starting state
            String currentState = startingState;

            // Read the string character by character
            for (char c : str.toCharArray()) {
                // Check the index in the alphabet of this character
                int alphIndex = alphabet.indexOf(c);

                // If the character is not in the alphabet, reject the string
                if (alphIndex == -1) {
                    return "reject";
                }

                // Get the transition function for the current state
                ArrayList<String> transitionList = states.get(currentState);
                // Update the state by reading the state at the same index as
                // the character in our alphabet
                currentState = transitionList.get(alphIndex);
            }

            // Check if the string was accepted
            if (acceptStates.contains(currentState)) {
                return "accept";
            } else {
                return "reject";
            }
        }

        /**
         * Format the FSA in the style described in Homework 2, Question 4.
         * @return A formatted multiline String describing the FSA
         */
        @Override
        public String toString() {
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
                    output.append("  ");
                    output.append(transitionState);
                }

                output.append("\n");
            }

            return output.toString();
        }
    }
}
