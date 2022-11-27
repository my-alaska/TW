import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

class WarehouseNested {
    int n;
    int c;
    double val;
    int factoryWork;


    private ReentrantLock globalLock = new ReentrantLock();
    private Condition globalCondition = globalLock.newCondition();
    private ReentrantLock producerLock = new ReentrantLock();
    private ReentrantLock consumerLock = new ReentrantLock();

    WarehouseNested(int capacity, int factoryWork) {
        n = 0;
        c = capacity;
        this.factoryWork = factoryWork;
    }

    void put(int i) throws InterruptedException {
        producerLock.lock();
        try {

            globalLock.lock();
            try {
                while (n + i < 0) {
                    globalCondition.await();
                }
                n += i;
                for(int j = 0; j < factoryWork; j++) val = Math.sin(42);
                globalCondition.signal();
            } finally {
                globalLock.unlock();
            }

        } finally {
            producerLock.unlock();
        }
    }

    void take(int i) throws InterruptedException {
        consumerLock.lock();
        try {

            globalLock.lock();
            try {
                while (n - i < 0) {
                    globalCondition.await();
                }
                n -= i;
                for(int j = 0; j < factoryWork; j++) val = Math.sin(42);
                globalCondition.signal();
            } finally {
                globalLock.unlock();
            }

        } finally {
            consumerLock.unlock();
        }

    }
}

class ProducerNested implements Runnable {
    private final WarehouseNested warehouse;

    Random random;
    int max;
    int id;
    int extraWork;
    boolean exit = false;

    public ProducerNested(WarehouseNested w, int max, int id, int extraWork) {
        this.warehouse = w;
        this.random = new Random(42);
        this.max = max;
        this.id = id;
        this.extraWork = extraWork;
    }

    @Override
    public void run() {
        double val;
        while (!exit) {
            try {
                warehouse.put(random.nextInt(max) + 1);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            for(int i = 0; i < extraWork;i++){
                val = Math.sin(42);
            }
            ProducerConsumerNested.workCounts[id]+=extraWork;
            ProducerConsumerNested.requestCounts[id]++;
        }
    }
    public void stop() {
        exit = true;
    }
}


class ConsumerNested implements Runnable {

    private final WarehouseNested warehouse;

    Random random;
    int max;
    int id;
    int extraWork;
    boolean exit = false;

    public ConsumerNested(WarehouseNested w, int max, int id, int extraWork) {
        this.warehouse = w;
        this.random = new Random(42);
        this.max = max;
        this.id = id;
        this.extraWork = extraWork;
    }


    @Override
    public void run() {
        double val;
        while (!exit) {
            try {
                warehouse.take(random.nextInt(max) + 1);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            for(int i = 0; i < extraWork;i++){
                val = Math.sin(42);
            }
            ProducerConsumerNested.workCounts[id]+=extraWork;
            ProducerConsumerNested.requestCounts[id]++;
        }
    }

    public void stop() {
        exit = true;
    }
}

public class ProducerConsumerNested {
    static int maxBound = 1000;
    static int[] requestCounts;
    static long[] workCounts;


    static void test(int extraWork, int factoryWork, int n) throws InterruptedException {

        WarehouseNested warehouse = new WarehouseNested(n,factoryWork);



        requestCounts = new int[2*n];
        workCounts = new long[2*n];
        for(int i = 0; i < 2*n; i++) {
            requestCounts[i] = 0;
            workCounts[i] = 0;
        }

        List<Thread> threadList = new LinkedList<>();
        for (int i = 0; i < n; i++) threadList.add(new Thread(new ConsumerNested(warehouse, maxBound, i, extraWork)));
        for (int i = 0; i < n; i++) threadList.add(new Thread(new ProducerNested(warehouse, maxBound, n + i,extraWork)));


        long start = System.nanoTime();



        for (Thread thread : threadList) thread.start();


        TimeUnit.SECONDS.sleep(10);
        for (Thread thread : threadList) thread.stop();
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
        test(200,200,4);
    }

}