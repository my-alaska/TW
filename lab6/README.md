# Lab 6 - Active object

This exercise implements 2 ways of tackling the Producer-Consumer problem
One (as in previous laboratory exercises) uses Java's locks and conditions
whereas the other one uses the 'active object' pattern used in asynchronous programming

Both programs in this repository print out how many complete tasks(producer's or consumer's requests) and how much extra work
get completed on average in one second after calculating it for 10 seconds.

To test the behaviour of both methods we can compare their performance with different "extra work" and "n" parameters

"extra work" defines how many times consumer/producer threads compute difficult function in each iteration of their loop

"n" is the number of producer and consumer threads that amount to the total of 2*n threads
