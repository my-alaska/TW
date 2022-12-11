package org.example;

import org.jcsp.lang.*;
import org.jcsp.util.Buffer;
import org.jcsp.util.ints.ChannelDataStoreInt;

import java.util.ArrayList;

class Producer implements CSProcess{
    // List of buffer channels that producer uses to send to randomly chosen buffer
    private Any2OneChannelInt[] channels;

    // Constructor
    public Producer(Any2OneChannelInt[] producerChannels) {
        this.channels = producerChannels;
    }

    public void run () {
        boolean running = true;
        int i;
        while(running){
            // randomly chosen buffer to send to
            i = (int)(Math.random()*PCMain.bufferNumber);
            channels[i].out().write(1);
        }
    }
}





class Consumer implements CSProcess {
    // List of buffer channels that consumer uses to receive from randomly chosen buffer
    private Any2OneChannelInt[] channels;

    // constructor
   public Consumer(Any2OneChannelInt[] consumerChannels) {
        this.channels = consumerChannels;
    }

    public void run () {
        boolean running = true;
        int i;
        while(running){
            // randomly chosen buffer to receive from
            i = (int)(Math.random()*PCMain.bufferNumber);
            channels[i].out().write(-1);
        }
    }
}

class PCBuffer implements CSProcess {

    // Channels to send to other buffers
    private ArrayList<One2OneChannelInt> linksSend;

    // channels to receive from other buffers
    private ArrayList<One2OneChannelInt> linksReceive;

    // channel in which the consumers send their "products"
    private Any2OneChannelInt consumerChannel;

    // channel in which consumers sne their requests
    private Any2OneChannelInt producerChannel;

    // size of the buffer
    int bufferSize;

    // number of elements in thebuffer
    int bufferStatus;

    // elements sent by other buffers
    int otherBuffersContent;

    // buffer identifier
    int id;


    // constructor
    public PCBuffer(int id, Any2OneChannelInt producerChannel, Any2OneChannelInt consumerChannel, ArrayList<One2OneChannelInt> linksSend, ArrayList<One2OneChannelInt> linksReceive) {
        this.id = id;
        this.producerChannel = producerChannel;
        this.consumerChannel = consumerChannel;
        this.bufferSize = PCMain.bufferSize;
        this.bufferStatus = 0;
        this.linksSend = linksSend;
        this.linksReceive = linksReceive;
        this.otherBuffersContent = 0;
    }

    // sending half of buffer contents distributed equally to other buffers
    private void buffersSend(){
        int v = (bufferStatus + otherBuffersContent);
        int x = v / (2*linksSend.size());
        for(One2OneChannelInt link : linksSend){
            link.out().write(x);
            v -= x;
        }
        if(v+bufferStatus <= bufferSize){
            bufferStatus += v;
            otherBuffersContent = 0;
        }else{
            otherBuffersContent = (v+bufferStatus)-bufferSize;
            bufferStatus = bufferSize;
        }
    }

    // receiving buffer contents from other buffers
    private void buffersReceive(){
        int v = 0;
        for(One2OneChannelInt link :linksReceive){
            v += link.in().read();
        }
        if(v+bufferStatus <= bufferSize){
            bufferStatus += v;
        }else{
            otherBuffersContent += v-bufferSize;
            bufferStatus = bufferSize;
        }
    }



    // run
    public void run () {

        int p, c;
        p = producerChannel.in().read();
        c = consumerChannel.in().read();

        boolean running = true;
        int i = 0;
        while(running){
            // Processing producers
            if(bufferStatus + p <= bufferSize){
                bufferStatus += p;
                p = producerChannel.in().read();

            }


            // if too much product is received from other buffers they should be processed first
            if(otherBuffersContent > 0){
                if(otherBuffersContent + bufferStatus <= bufferSize){
                    bufferStatus += otherBuffersContent;
                    otherBuffersContent = 0;
                }else{
                    otherBuffersContent -= (bufferSize - bufferStatus);
                    bufferStatus = bufferSize;
                }
            // Otherwise we can process the consumers
            }else if(bufferStatus - c >= 0){
                bufferStatus -= c;
                c = consumerChannel.in().read();

            }

            // redistributing contents
            if(i == 1000){
                buffersSend();
                buffersReceive();
                buffersSend();
                buffersReceive();
                i = 0;
            }else i++;
            System.out.println(i);
        }
    }
}

public final class PCMain {
    public static int producerNumber = 13;
    public static int consumerNumber = 13;
    public static int bufferSize = 100;
    public static int bufferNumber = 6;

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
        ArrayList<ArrayList<One2OneChannelInt>> bufferLinksSend = new ArrayList<ArrayList<One2OneChannelInt>>();
        ArrayList<ArrayList<One2OneChannelInt>> bufferLinksReceive = new ArrayList<ArrayList<One2OneChannelInt>>();
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

            list1Send = bufferLinksSend.get(i);
            list2Send = bufferLinksSend.get(j);
            One2OneChannelInt channelSend = Channel.one2oneInt();
            list1Send.add(channelSend);
            list2Send.add(channelSend);

            list1Receive = bufferLinksReceive.get(i);
            list2Receive = bufferLinksReceive.get(j);
            One2OneChannelInt channelReceive = Channel.one2oneInt();
            list1Receive.add(channelReceive);
            list2Receive.add(channelReceive);
        }

        // creating producers, consumer and buffer threads
        CSProcess[] procList = new CSProcess[producerNumber + consumerNumber + bufferNumber];
        i = 0;
        for(j = 0; j < producerNumber; j++){
            procList[i] = new Producer(producerChannels);
            i++;
        }
        for(j = 0; j < consumerNumber; j++){
            procList[i] = new Consumer(consumerChannels);
            i++;
        }
        for(j = 0; j < bufferNumber; j++) {
            procList[i] = new PCBuffer(j, producerChannels[j], consumerChannels[j] ,bufferLinksSend.get(j),bufferLinksReceive.get(j));
            i++;
        }

        Parallel par = new Parallel(procList); // PAR construct
        par.run(); // Execute processes in parallel
    }
}