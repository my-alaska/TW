//package org.example;
//
//import org.jcsp.lang.*;
//
//class Producer_ implements CSProcess{
//    private One2OneChannelInt channel;
//
//    // constructor
//    public Producer_ (final One2OneChannelInt out) {
//        channel = out;
//    }
//
//    // run
//    public void run () {
//        int item = (int)(Math.random()*100)+1;
//        channel.out().write(item);
//    }
//}
//
//
//
///** Consumer class: reads one int from input channel, displays it, then
// * terminates. */
//class Consumer_ implements CSProcess {
//    private One2OneChannelInt channel;
//
//    // constructor
//    public Consumer_ (final One2OneChannelInt in) {
//        channel = in;
//    }
//
//    // run
//    public void run () {
//        int item = channel.in().read();
//        System.out.println(item);
//    }
//}
//
//
//
///** Main program class for Producer/Consumer example.
// * Sets up channel, creates one of each process then
// * executes them in parallel, using JCSP. */
//public final class PCMainOne {
//    public static void main (String[] args) {
//        new PCMainOne();
//    }
//
//
//    // PCMain constructor
//    public PCMainOne () {
//        // Create channel object
//        final One2OneChannelInt channel = Channel.one2oneInt();
//
//        // Create and run parallel construct with a list of processes
//        CSProcess[] procList = { new Producer_(channel), new Consumer_(channel) };
//
//        // Processes
//        Parallel par = new Parallel(procList); // PAR construct
//        par.run(); // Execute processes in parallel
//    }
//}
