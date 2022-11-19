# lab 1 - The basic problem

The aim of this laboratory exercise is to present students with the most basic problem solved by concurrent programming

The program runs two threads that simultaneously increment and decrement certain variable initialized as 0 for a given(and equal) number of times.

After that the program prints the value of the variable. Normally we'd expect that the printed value is 0 however, that number is different due to the lack of protection over the variable
