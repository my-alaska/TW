import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

class WarehouseNested {
    int n;
    int c;


    private ReentrantLock globalLock = new ReentrantLock();
    private Condition globalCondition = globalLock.newCondition();
    private ReentrantLock producerLock = new ReentrantLock();
    private ReentrantLock consumerLock = new ReentrantLock();

    WarehouseNested(int capacity) {
        n = 0;
        c = capacity;
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

    public ProducerNested(WarehouseNested w, int max, int id, int seed, int extraWork) {
        this.warehouse = w;
        this.random = new Random(seed);
        this.max = max;
        this.id = id;
        this.extraWork = extraWork;
    }

    @Override
    public void run() {
        double val;
        while (true) {
            try {
                warehouse.put(random.nextInt(max) + 1);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            for(int i = 0; i < extraWork;i++){
                val = Math.sin(42);
            }
            ProducerConsumerNested.counts[id]++;
        }
    }
}


class ConsumerNested implements Runnable {

    private final WarehouseNested warehouse;

    Random random;
    int max;
    int id;
    int extraWork;

    public ConsumerNested(WarehouseNested w, int max, int id, int seed,int extraWork) {
        this.warehouse = w;
        this.random = new Random(seed);
        this.max = max;
        this.id = id;
        this.extraWork = extraWork;
    }


    @Override
    public void run() {
        double val;
        while (true) {
            try {
                warehouse.take(random.nextInt(max) + 1);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            for(int i = 0; i < extraWork;i++){
                val = Math.sin(42);
            }
            ProducerConsumerNested.counts[id]++;
        }
    }
}

public class ProducerConsumerNested {
    static int n = 10;
    static int maxBound = 1000;
    static int extraWork = 20000;
    static int[] counts;


    public static void main(String[] args) throws InterruptedException {
        WarehouseNested warehouse = new WarehouseNested(maxBound * 2 - 1);

        List<Thread> threadList = new LinkedList<>();

        for (int i = 0; i < n; i++) threadList.add(new Thread(new ConsumerNested(warehouse, maxBound, i, 42, extraWork)));
        for (int i = 0; i < n; i++) threadList.add(new Thread(new ProducerNested(warehouse, maxBound, n + i, 42,extraWork)));

        int epoch = 1;
        long start = System.nanoTime();
        long nano;

        counts = new int[2*n];
        for(int i = 0; i < 2*n; i++)counts[i] = 0;

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