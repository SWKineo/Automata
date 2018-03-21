Spencer Ward
February 27, 2018
COS 451 Homework 3

Description:

An implementation of the modified Lexxard interpreter described in Homework 3

The interpreter is started by a basic main() method, and runs as an instance of the class defined in

LexaardInterpreter.java.

The interpreter uses an abstract class contained in 

FSA.java

to represent generic FSA's, and implementing classes contained in

DFA.java and NFA.java

to represent concrete examples. All FSA's start as DFA's as they are being built, then are converted to NFA's if NFA-centric components (such as epsilon transitions or multiple possible states for a transition).

This project does not fully match the description given in Homework 3. It should match the specification given in Homework 2, with the addition of support for NFA's (unfortunately not properly tested due to time limitations), but the finite-state automata model is largely redesigned from the implementation provided with Homework 2 to be much easier to manipulate for various operations. Of the operations requested in Homework 3,  DFA.union(dfa1, dfa2) and FSA.prune(fsa) are both implemented, and the rest of the operations have methods defined and documented in LexxardInterpreter.java, FSA.java, DFA.java, and NFA.java. Due to time constraints, the functions nfa2dfa, nfaUnion, nfaConcat, nfaStar, and equivP do not have concrete implementations, and none of the new operations are tied into the command line interpreter. Adding this additional support would not be difficult, since all that remains to be done is implementing the algorithms defined in the textbook proofs (and described in the documentation comments) and adding some extra text processing.

Comment Notation:

The operation descriptions provided in the JavaDocs use the following special notation:

a: A 	<==> 	a is an element of A

a !: A 	<==> 	a is not an element of A

_e		<==>	the epsilon character

Compilation:

The project may be compiled by typing

make

at the command line.

Execution:

The program can be started by typing

./lexaard

at the command line.


If you have any questions or comments, contact spencer.ward@maine.edu.