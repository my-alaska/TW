package org.example;

import org.jcsp.lang.*;

import java.nio.Buffer;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;


class Producer implements CSProcess{
    private Any2OneChannelInt[] channels;
    int completed;
    boolean running;
    public Producer(Any2OneChannelInt[] producerChannels) {
        this.channels = producerChannels;
        this.completed = 0;
    }

    public void stop(){
        running = false;
    }

    public void run () {
        this.running = true;
        int i;
        while(running){
            // randomly chosen buffer to send to
            i = (int)(Math.random()*PCMain.bufferNumber);
            channels[i].out().write(1);
            completed++;
        }
    }
}

class Consumer implements CSProcess {
    private Any2OneChannelInt[] channels;
    int completed;
    boolean running;
    public Consumer(Any2OneChannelInt[] consumerChannels) {
        this.channels = consumerChannels;
        this.completed = 0;
    }

    public void stop(){
        running = false;
    }

    public void run () {
        running = true;
        int i;
        while(running){
            // randomly chosen buffer to receive from
            i = (int)(Math.random()*PCMain.bufferNumber);
            channels[i].out().write(-1);
            completed++;
        }
    }
}

class PCBuffer implements CSProcess {

    // Channels to send or receive to or from other buffers
    private ArrayList<One2OneChannelInt> linksSend;
    private ArrayList<One2OneChannelInt> linksReceive;
    int neighboursNumber;
    // channel for producers and consumer
    private Any2OneChannelInt consumerChannel;
    private Any2OneChannelInt producerChannel;

    private Guard[] guards;
    private Alternative alt;

    // size of the buffer
    int bufferSize;
    // number of elements in thebuffer
    int bufferStatus;
    // elements sent by other buffers
    int otherBuffersContent;
    // buffer identifier
    int id;
    boolean running;

    // constructor
    public PCBuffer(int id, Any2OneChannelInt producerChannel, Any2OneChannelInt consumerChannel, ArrayList<One2OneChannelInt> linksSend, ArrayList<One2OneChannelInt> linksReceive) {
        this.id = id;
        this.producerChannel = producerChannel;
        this.consumerChannel = consumerChannel;

        this.neighboursNumber = linksReceive.size();
        this.guards = new Guard[this.neighboursNumber+2];
        for(int i = 0; i < this.neighboursNumber;i++) this.guards[i] = linksReceive.get(i).in();
        this.guards[this.neighboursNumber] = producerChannel.in();
        this.guards[this.neighboursNumber+1] = consumerChannel.in();


        this.alt = new Alternative(guards);

        this.bufferSize = PCMain.bufferSize;
        this.bufferStatus = 0;
        this.linksSend = linksSend;
        this.linksReceive = linksReceive;
        this.otherBuffersContent = 0;
    }


    // sending half of buffer contents to a random neighbour
    private void buffersSend(int ind){
        int valSend = bufferStatus/2;
        bufferStatus -= valSend;
        linksSend.get(ind).out().write(valSend);

    }

    public void stop(){
        this.running = false;
    }

    public void run () {

        running = true;
        int i = 1000;
        int val;
        int ind;
        boolean[] waitingFor = new boolean[neighboursNumber];
        for(int j = 0; j < neighboursNumber; j++) waitingFor[j] = false;
        while(running){
            // Processing producers

            ind = this.alt.fairSelect();

//            if(ind >= 2)System.out.println(ind);

            if(ind < this.neighboursNumber ){
                val = linksReceive.get(ind).in().read();
                if(!waitingFor[ind]){
                    buffersSend(ind);
                }else waitingFor[ind] = false;

                bufferStatus += val;
                continue;
            }else{
                ind -= this.neighboursNumber;
            }


            if(ind == 0){
                if(bufferStatus < bufferSize){
                    val = producerChannel.in().read();
                    bufferStatus += val;
                }
            }else{
                if(bufferStatus > 0) {
                    val = consumerChannel.in().read();
                    bufferStatus += val;
                }
            }


            if(i % 100000 == (int)(Math.random()*1000) && (int)(Math.random()*3) == 1){

                i = (int) (Math.random() * neighboursNumber);
                waitingFor[i] = true;
                buffersSend(i);
                i = 1000;

            }else i++;

        }
    }
}


class Killer implements CSProcess {

    public void run () {
        try {
            TimeUnit.SECONDS.sleep(5);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        for(int j = 0; j < PCMain.producerNumber; j++){
            PCMain.producers[j].stop();
        }

        for(int j = 0; j < PCMain.consumerNumber; j++){
            PCMain.consumers[j].stop();
        }

        for(int j = 0; j < PCMain.bufferNumber; j++) {
            PCMain.buffers[j].stop();
        }

        System.out.println("Completed Producer Tasks");
        for(int j = 0; j < PCMain.producerNumber; j++){
            System.out.println(PCMain.producers[j].completed);
        }

        System.out.println("\nCompleted Consumer Tasks");

        for(int j = 0; j < PCMain.consumerNumber; j++){
            System.out.println(PCMain.consumers[j].completed);
        }

        System.out.println("\n ending buffer status");
        for(int j = 0; j < PCMain.bufferNumber; j++) {
            System.out.println(PCMain.buffers[j].bufferStatus);
        }

    }
}

public final class PCMain {
    public static int producerNumber = 13;
    public static int consumerNumber = 13;
    public static int bufferSize = 10000;
    public static int bufferNumber = 6;
    public static Producer[] producers;
    public static Consumer[] consumers;
    public static PCBuffer[] buffers;

    public static void main (String[] args) throws InterruptedException {
        new PCMain();
    }

    // PCMain constructor
    public PCMain () throws InterruptedException {

        // Create channel object
        Any2OneChannelInt[] producerChannels = new Any2OneChannelInt[bufferNumber];
        Any2OneChannelInt[] consumerChannels = new Any2OneChannelInt[bufferNumber];

        // Initialize producer-buffer and consumer-buffer channels
        for(int i = 0; i < bufferNumber; i++){
            producerChannels[i] = Channel.any2oneInt();
            consumerChannels[i] = Channel.any2oneInt();
        }

        // Initialize connection list between buffers
        ArrayList<ArrayList<One2OneChannelInt>> bufferLinksSend = new ArrayList<>();
        ArrayList<ArrayList<One2OneChannelInt>> bufferLinksReceive = new ArrayList<>();
        for(int i = 0; i < bufferNumber; i++){
            ArrayList<One2OneChannelInt> listSend = new ArrayList<>();
            ArrayList<One2OneChannelInt> listReceive = new ArrayList<>();
            bufferLinksSend.add(listSend);
            bufferLinksReceive.add(listReceive);
        }

        // graph of connections
        int[][] links = {{0,1},{1,2},{2,3},{3,4},{4,5},{5,0}};

        // putting channels in the graph
        int i,j;
        ArrayList<One2OneChannelInt> list1Send, list2Send, list1Receive, list2Receive;
        for(int[] link : links){
            i = link[0];
            j = link[1];

            One2OneChannelInt channelA = Channel.one2oneInt();
            One2OneChannelInt channelB = Channel.one2oneInt();

            list1Send = bufferLinksSend.get(i);
            list2Send = bufferLinksSend.get(j);
            list1Send.add(channelA);
            list2Send.add(channelB);

            list1Receive = bufferLinksReceive.get(i);
            list2Receive = bufferLinksReceive.get(j);
            list1Receive.add(channelB);
            list2Receive.add(channelA);
        }

        // creating producers, consumer and buffer threads
        CSProcess[] procList = new CSProcess[producerNumber + consumerNumber + bufferNumber+1];
        i = 0;

        producers = new Producer[producerNumber];
        for(j = 0; j < producerNumber; j++){
            producers[j] = new Producer(producerChannels);
            procList[i] = producers[j];
            i++;
        }

        consumers = new Consumer[consumerNumber];
        for(j = 0; j < consumerNumber; j++){
            consumers[j] = new Consumer(consumerChannels);
            procList[i] = consumers[j];
            i++;
        }

        buffers = new PCBuffer[bufferNumber];
        for(j = 0; j < bufferNumber; j++) {
            buffers[j] = new PCBuffer(j, producerChannels[j], consumerChannels[j] ,bufferLinksSend.get(j),bufferLinksReceive.get(j));
            procList[i] = buffers[j];
            i++;
        }

        procList[producerNumber + consumerNumber + bufferNumber] = new Killer();

        Parallel par = new Parallel(procList); // PAR construct
        par.run(); // Execute processes in parallel





    }
}