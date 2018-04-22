Spencer Ward
April 5, 2018
COS 451 Homework 5

Description:

An implementation of the modified Lexxard interpreter described in Homework 5

The interpreter is started by a basic main() method, and runs as an instance of the class defined in

Lexaard.kt

This interpreter has been greatly simplified and rewritten in Kotlin since Homework 4, so now it properly reads and processes literals, functions, and variables of most types (I haven't had a chance to go back through and clean up all of the old parsing code for each class, but they should work in theory).

Lexaard uses a model of context-free grammars given in 'CFG.kt', which allows parsing from a String using 'CFG.from(<str>)' -> <cfg>, and printing using '<cfg>.toString'.
Conversion to Chomsky normal form using 'chomskyNF' is partially working but hasn't been debugged, so the function can be run from the interpreter but the CFGs it returns are usually not actually in CNF. 
Checking if a CFG generates a string using 'cfgGen' is partially implemented but not fully working, so the function can be called from the interpreter but it will always return 'false'.

Compilation:

The project may be compiled by typing

make

at the command line.

Execution:

The program can be started by typing

./lexaard

at the command line.

Especially since Kotlin has its fair share of funny quirks compared to Java, if you have any questions or comments, feel free to contact me at spencer.ward@maine.edu.