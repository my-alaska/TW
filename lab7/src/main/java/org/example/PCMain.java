package org.example;

import org.jcsp.lang.*;

import java.util.concurrent.TimeUnit;

class Producer implements CSProcess{
    private One2OneChannelInt channel;

    // constructor
    public Producer (One2OneChannelInt channel) {
        this.channel = channel;
    }

    public void run () {
        boolean running = true;
        int item;
        while(running){
            item = (int)(Math.random()*PCMain.maxVal)+1;
            channel.out().write(item);
        }
    }
}



/** Consumer class: reads one int from input channel, displays it, then
 * terminates. */
class Consumer implements CSProcess {
    private One2OneChannelInt requests;
    private One2OneChannelInt reply;

    // constructor
    public Consumer (One2OneChannelInt requests, One2OneChannelInt reply) {
        this.requests = requests;
        this.reply = reply;
    }

    // run
    public void run () {
        boolean running = true;
        int item;
        while(running){
            item = (int)(Math.random()*PCMain.maxVal)+1;
            requests.out().write(item);
            item = reply.in().read();
        }
    }
}

class Buffer implements CSProcess {

    private One2OneChannelInt[] producerChannels;
    private One2OneChannelInt[] consumerRequests;
    private One2OneChannelInt[] consumerReply;
    private int n;

    int buf;
    int capacity;

    // constructor
    public Buffer (One2OneChannelInt[] producerChannels,
                   One2OneChannelInt[] consumerRequests,
                   One2OneChannelInt[] consumerReply) {
        this.producerChannels = producerChannels;
        this.consumerRequests = consumerRequests;
        this.consumerReply = consumerReply;
        n = PCMain.n;
        buf = 0;
        capacity = 2*PCMain.maxVal-1;
    }

    // run
    public void run () {
        int i_c = 0;
        int i_p = 0;

        int p, c;
        p = producerChannels[i_p].in().read();
        c = consumerRequests[i_c].in().read();

        boolean running = true;
        while(running){
            if(buf + p <= capacity){
                buf += p;
                i_p = (i_p+1)%n;
                p = producerChannels[i_p].in().read();
            }

            if(buf - c >= 0){
                buf -= c;
                consumerReply[i_c].out().write(c);
                i_c = (i_c+1)%n;
                c = consumerRequests[i_c].in().read();
            }
            System.out.println(i_p + " " + i_c);
        }
    }
}



/** Main program class for Producer/Consumer example.
 * Sets up channel, creates one of each process then
 * executes them in parallel, using JCSP. */
public final class PCMain {
    public static int maxVal = 5;
    public static int n = 5;

    public static void main (String[] args) throws InterruptedException {
        new PCMain();
    }

    public static int p_completed;
    public static int c_completed;

    // PCMain constructor
    public PCMain () throws InterruptedException {
        // Create channel object
        One2OneChannelInt[] producerChannels = new One2OneChannelInt[n];
        One2OneChannelInt[] consumerRequests = new One2OneChannelInt[n];
        One2OneChannelInt[] consumerReply = new One2OneChannelInt[n];

        for(int i = 0; i < n; i++){
            producerChannels[i] = Channel.one2oneInt();
            consumerRequests[i] = Channel.one2oneInt();
            consumerReply[i] = Channel.one2oneInt();
        }

        CSProcess[] procList = new CSProcess[2*n+1];
        procList[0] = new Buffer(producerChannels,consumerRequests,consumerReply);

        for(int i = 0; i < n;i++){
            procList[1+i] = new Producer(producerChannels[i]);
            procList[n+1+i] = new Consumer(consumerRequests[i],consumerReply[i]);
        }


        p_completed = 0;
        c_completed = 0;


        Parallel par = new Parallel(procList); // PAR construct
        par.run(); // Execute processes in parallel

    }
}