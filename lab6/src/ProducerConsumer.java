import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Random;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

enum Type{
    PRODUCER, CONSUMER, FACTORY
}


class Pack{
    int val;
    int id;
    Type type;
    Pack(int val, int id, Type t){
        this.val = val;
        this.id = id;
        this.type = t;
    }
}

class Warehouse{
    volatile private Pack[] packs;
    volatile private boolean[] flags;
    Warehouse(int n){
        packs = new Pack[2*n];
        flags = new boolean[2*n];
        for(int i = 0; i < 2*n; i++){flags[i] = false;}
    }

    void setFlag(int i){
        flags[i] = true;
    }
    boolean checkFlag(int i){
        if(flags[i]) {
            flags[i] = false;
            return true;
        }else return false;
    }
    synchronized void setPack(Pack pack){
        packs[pack.id] = pack;
    }

}




class Factory implements Runnable{
    int n;
    int capacity;
    LinkedBlockingQueue<Pack> proxy;
    Queue<Pack> internalQueue = new LinkedList<>();
    Warehouse warehouse;
    Factory(int c, LinkedBlockingQueue<Pack> proxy, Warehouse warehouse){
        n = 0;
        capacity = c;
        this.proxy = proxy;
        this.warehouse = warehouse;
    }



    private boolean fits(Pack pack){
        if(pack.type == Type.PRODUCER && pack.val + n <= capacity){
            return true;
        }else return pack.type == Type.CONSUMER && n - pack.val >= 0;
    }



    private void process(Pack pack){
        if(pack.type == Type.PRODUCER){
            n += pack.val;
            pack.val = 0;
        }else{
            n -= pack.val;
        }
        pack.type = Type.FACTORY;
        warehouse.setPack(pack);
//        warehouse.packs[pack.id] = pack;
        warehouse.setFlag(pack.id);
//        warehouse.flags[pack.id] = true;
//        warehouse.setFlag(pack.id);
    }



    @Override
    public void run() {
        Pack pack;

        while(true){

            while(!internalQueue.isEmpty()){
                pack = internalQueue.peek();
                if(fits(pack)){
                    internalQueue.remove();
                    // extra work parameter(task time)
                    process(pack);
                }else break;
            }

            while(!proxy.isEmpty()){
                pack = proxy.remove();
                if(fits(pack)){
                    process(pack);
                    // extra work parameter(task time)
                    break;
                }else internalQueue.add(pack);
            }
        }
    }
}




class Producer implements Runnable{
    Random random;
    int max;
    int id;
    int extraWork;
    LinkedBlockingQueue<Pack> proxy;
    Warehouse warehouse;
    public Producer(int max, int id, LinkedBlockingQueue<Pack> proxy, Warehouse warehouse, int extraWork){
        this.random = new Random();
        this.max = max;
        this.id = id;
        this.proxy = proxy;
        this.warehouse = warehouse;
        this.extraWork = extraWork;
    }

    @Override
    public void run() {
        int i;
//        Pack pack;
        double val;
        while(true){
            i = random.nextInt(max)+1;

            try {
                proxy.put(new Pack(i,this.id,Type.PRODUCER));
            } catch (InterruptedException e) {throw new RuntimeException(e);}

            for(int j = 0; j < extraWork; j++) val = Math.sin(42);
            while(! warehouse.checkFlag(id)) val = Math.sin(42);
            ProducerConsumer.counts[id]++;
//            pack = warehouse.packs[id];
        }
    }
}




class Consumer implements Runnable{
    Random random;
    int max;
    int id;
    int extraWork;
    LinkedBlockingQueue<Pack> proxy;
    Warehouse warehouse;
    public Consumer(int max, int id, LinkedBlockingQueue<Pack> proxy, Warehouse warehouse, int extraWork){
        this.random = new Random();
        this.max = max;
        this.id = id;
        this.proxy = proxy;
        this.warehouse = warehouse;
        this.extraWork = extraWork;
    }
    @Override
    public void run() {
        int i;
//        Pack pack;
        double val;
        while(true){
            i = random.nextInt(max)+1;

            try {
                proxy.put(new Pack(i,this.id,Type.CONSUMER));
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }

            for(int j = 0; j < extraWork; j++) val = Math.sin(42);
            while(! warehouse.checkFlag(id)) val = Math.sin(42);
            ProducerConsumer.counts[id]++;
//            pack = warehouse.packs[id];
        }
    }
}

public class ProducerConsumer{
    static int n = 10;
    static int maxBound = 1000;
    static int extraWork = 20000;
    static int[] counts;

    public static void main(String[] args) throws InterruptedException {
        LinkedBlockingQueue<Pack> proxy = new LinkedBlockingQueue<>();
        Warehouse warehouse = new Warehouse(n);

        Thread factory = new Thread(new Factory(2*maxBound-1, proxy, warehouse));

        counts = new int[2*n];
        for(int i = 0; i < 2*n; i++)counts[i] = 0;

        List<Thread> threadList = new LinkedList<>();
        for(int i = 0; i < n; i++) threadList.add(new Thread(new Consumer(maxBound,i,proxy,warehouse,extraWork)));
        for(int i = 0; i < n; i++) threadList.add(new Thread(new Producer(maxBound,n+i,proxy,warehouse,extraWork)));


        int epoch = 1;
        long start = System.nanoTime();
        long nano;

        factory.start();
        for (Thread thread : threadList) thread.start();
        ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
        long[] allThreadIds = threadMXBean.getAllThreadIds();
        int numberOfOperations;
        while (true) {
            TimeUnit.SECONDS.sleep(10);
            System.out.println("epoch " + epoch);
            numberOfOperations = 0;
            for(int i = 0; i < 2*n;i++)numberOfOperations += counts[i];
            System.out.println("n operations/real time second \n" + numberOfOperations/((double)(System.nanoTime() - start)/1_000_000_000));
            nano = 0;
            for (long id : allThreadIds) nano += threadMXBean.getThreadCpuTime(id);
            System.out.println("n operations/cpu time second \n" + numberOfOperations/((double) nano / 1000_000_000));
            System.out.println("\n");
            epoch++;
        }
    }
}