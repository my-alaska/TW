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
    boolean exit = false;
    int factoryWork;
    Factory(int c, LinkedBlockingQueue<Pack> proxy, Warehouse warehouse, int factoryWork){
        n = 0;
        capacity = c;
        this.proxy = proxy;
        this.warehouse = warehouse;
        this.factoryWork = factoryWork;
    }



    private boolean fits(Pack pack){
        if(pack.type == Type.PRODUCER && pack.val + n <= capacity){
            return true;
        }else return pack.type == Type.CONSUMER && n - pack.val >= 0;
    }


    double val;
    private void process(Pack pack){
        if(pack.type == Type.PRODUCER){
            n += pack.val;
            pack.val = 0;
        }else{
            n -= pack.val;
        }
        pack.type = Type.FACTORY;
        warehouse.setPack(pack);
        warehouse.setFlag(pack.id);
        for(int j = 0; j < factoryWork; j++) val = Math.sin(42);

    }



    @Override
    public void run() {
        Pack pack;

        while(!exit){

            while(!internalQueue.isEmpty()){
                pack = internalQueue.peek();
                if(fits(pack)){
                    internalQueue.remove();
                    process(pack);
                }else break;
            }

            while(!proxy.isEmpty()){
                pack = proxy.remove();
                if(fits(pack)){
                    process(pack);
                    break;
                }else internalQueue.add(pack);
            }
        }
    }
    public void stop() {
        exit = true;
    }
}




class Producer implements Runnable{
    Random random;
    int max;
    int id;
    int extraWork;
    boolean exit = false;
    LinkedBlockingQueue<Pack> proxy;
    Warehouse warehouse;
    public Producer(int max, int id, LinkedBlockingQueue<Pack> proxy, Warehouse warehouse, int extraWork){
        this.random = new Random(42);
        this.max = max;
        this.id = id;
        this.proxy = proxy;
        this.warehouse = warehouse;
        this.extraWork = extraWork;
    }

    long counter;
    @Override
    public void run() {
        int i;
        double val;
        while(!exit){
            i = random.nextInt(max)+1;

            try {
                proxy.put(new Pack(i,this.id,Type.PRODUCER));
            } catch (InterruptedException e) {throw new RuntimeException(e);}

            for(int j = 0; j < extraWork; j++) {
                val = Math.sin(42);
            }
            counter = 0;
            while(! warehouse.checkFlag(id)) {
                val = Math.sin(42);
              counter++;
            }
            ProducerConsumer.workCounts[id]+=(extraWork+counter);
            ProducerConsumer.requestCounts[id]++;
        }
    }
    public void stop() {
        exit = true;
    }
}




class Consumer implements Runnable{
    Random random;
    int max;
    int id;
    int extraWork;
    boolean exit = false;
    LinkedBlockingQueue<Pack> proxy;
    Warehouse warehouse;
    public Consumer(int max, int id, LinkedBlockingQueue<Pack> proxy, Warehouse warehouse, int extraWork){
        this.random = new Random(42);
        this.max = max;
        this.id = id;
        this.proxy = proxy;
        this.warehouse = warehouse;
        this.extraWork = extraWork;
    }
    long counter;
    @Override
    public void run() {
        int i;
        double val;
        while(!exit){
            i = random.nextInt(max)+1;

            try {
                proxy.put(new Pack(i,this.id,Type.CONSUMER));
            } catch (InterruptedException e) {

            }

            for(int j = 0; j < extraWork; j++) {
                val = Math.sin(42);
            }
            counter = 0;
            while(! warehouse.checkFlag(id)) {
                val = Math.sin(42);
//                counter++;
            }
            ProducerConsumer.workCounts[id]+=(extraWork+counter);
            ProducerConsumer.requestCounts[id]++;
        }
    }
    public void stop() {
        exit = true;
    }
}

public class ProducerConsumer{
    static int maxBound = 1000;
    static int[] requestCounts;
    static long[] workCounts;


    static void test(int extraWork, int factoryWork, int n) throws InterruptedException {

        LinkedBlockingQueue<Pack> proxy = new LinkedBlockingQueue<>();
        Warehouse warehouse = new Warehouse(n);

        Thread factory = new Thread(new Factory(2*maxBound-1, proxy, warehouse,factoryWork));

        requestCounts = new int[2*n];
        workCounts = new long[2*n];
        for(int i = 0; i < 2*n; i++) {
            requestCounts[i] = 0;
            workCounts[i] = 0;
        }

        List<Thread> threadList = new LinkedList<>();
        for(int i = 0; i < n; i++) threadList.add(new Thread(new Consumer(maxBound,i,proxy,warehouse,extraWork)));
        for(int i = 0; i < n; i++) threadList.add(new Thread(new Producer(maxBound,n+i,proxy,warehouse,extraWork)));


        long start = System.nanoTime();


        factory.start();
        for (Thread thread : threadList) thread.start();


        TimeUnit.SECONDS.sleep(10);
        for (Thread thread : threadList) thread.stop();
        factory.stop();
        double end = (System.nanoTime() - start);

        int numberOfOperations = 0;
        long finishedWork = 0;
        for(int i = 0; i < 2*n;i++) {
            numberOfOperations += requestCounts[i];
            finishedWork += workCounts[i];
        }


        System.out.println("n operations /real time second per thread \n" + (numberOfOperations/(2*n))/(end/1_000_000_000));
        System.out.println("work         /real time second per thread \n" + (finishedWork/(2*n))/(end/1_000_000_000));
        System.out.println("\n");
    }


    public static void main(String[] args) throws InterruptedException {
        test(200, 200,4);
    }


}