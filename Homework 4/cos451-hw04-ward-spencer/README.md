Spencer Ward
March 22, 2018
COS 451 Homework 4

Description:

An implementation of the modified Lexxard interpreter described in Homework 4

The interpreter is started by a basic main() method, and runs as an instance of the class defined in

LexaardInterpreter.java.

The interpreter uses a representation of Regular Expressions defined in the Kotlin file

RegExpr.kt

and a representation of GNFA's defined in the Kotlin file

GNFA.kt.

All of the code for parsing, printing, and running regular expressions is implemented as described in problem 1, with the exception that the code to run a star operation has not been completed (although a brief implementation is described in the comments).

The code for the function regex2fsa has been implemented as described in problem 2.

All of the code for parsing, printing, and running GNFA's is implemented as described in problem 3.

The code for the function fsa2regex has been implemented as described in problem 4.

Although these two objects and two functions are implemented and ready to use (with the exception of running the star regex operation), they have not been tied into the command line interpreter yet. Adding this functionality would be fairly straightforward, involving writing some Pattern code to recognize regular expression and GNFA objects, storing them in their respective HashMaps with their variable names, and adding Pattern based parsers for each of the functions.

Regular expressions are checked using reluctant operations, although in hindsight greedy operations might have been more effective.

Compilation:

The project may be compiled by typing

make

at the command line.

Execution:

The program can be started by typing

./lexaard

at the command line.

Especially since Kotlin has its fair share of funny quirks compared to Java, if you have any questions or comments, feel free to contact me at spencer.ward@maine.edu.