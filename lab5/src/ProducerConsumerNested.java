import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

class Warehouse {
    int n;
    int c;


    private ReentrantLock globalLock = new ReentrantLock();
    private Condition globalCondition = globalLock.newCondition();
    private ReentrantLock producerLock = new ReentrantLock();
    private ReentrantLock consumerLock = new ReentrantLock();

    Warehouse(int capacity) {
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

class Producer implements Runnable {
    private final Warehouse warehouse;

    Random random;
    int max;
    int id;

    public Producer(Warehouse w, int max, int id, int seed) {
        this.warehouse = w;
        this.random = new Random(seed);
        this.max = max;
        this.id = id;
    }

    @Override
    public void run() {
        while (true) {
            try {
                warehouse.put(random.nextInt(max) + 1);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            ProducerConsumerNested.numberOfOperations++;
        }

    }
}


class Consumer implements Runnable {

    private final Warehouse warehouse;

    Random random;
    int max;
    int id;

    public Consumer(Warehouse w, int max, int id, int seed) {
        this.warehouse = w;
        this.random = new Random(seed);
        this.max = max;
        this.id = id;
    }


    @Override
    public void run() {
        while (true) {
            try {
                warehouse.take(random.nextInt(max) + 1);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            ProducerConsumerNested.numberOfOperations++;
        }
    }
}

public class ProducerConsumerNested {
    static int n = 10000;

    static int maxBound = 5;

    static int numberOfOperations = 0;


    public static void main(String[] args) throws InterruptedException {
        Warehouse warehouse = new Warehouse(maxBound * 2 - 1);

        List<Thread> threadList = new LinkedList<>();

        for (int i = 0; i < n; i++) {
            threadList.add(new Thread(new Consumer(warehouse, maxBound, i, 42)));
        }
        for (int i = 0; i < n; i++) {
            threadList.add(new Thread(new Producer(warehouse, maxBound, n + i, 42)));
        }


        int epoch = 1;
        long start = System.nanoTime();
        long nano;

        for (Thread thread : threadList) {
            thread.start();
        }

        ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
        long[] allThreadIds = threadMXBean.getAllThreadIds();

        while (true) {
            TimeUnit.SECONDS.sleep(1);
            System.out.println("epoch " + epoch);
            System.out.println("n operations/real time second \n" + numberOfOperations/((double)(System.nanoTime() - start)/1_000_000_000));
            nano = 0;
            for (long id : allThreadIds) {
                nano += threadMXBean.getThreadCpuTime(id);
            }
            System.out.println("n operations/cpu time second \n" + numberOfOperations/((double) nano / 1000_000_000));
            System.out.println("\n");
            epoch++;
        }
    }
}