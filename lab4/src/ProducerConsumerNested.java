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

        public Producer(Warehouse w, int max, int id) {
            this.warehouse = w;
            this.random = new Random();
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
                ProducerConsumerNested.idCounts[this.id]++;
            }

        }
    }


    class Consumer implements Runnable {

        private final Warehouse warehouse;

        Random random;
        int max;
        int id;

        public Consumer(Warehouse w, int max, int id) {
            this.warehouse = w;
            this.random = new Random();
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
                ProducerConsumerNested.idCounts[this.id]++;
            }
        }
    }

    public class ProducerConsumerNested {
        static int capacity = 3; // must be at least 2*maxBound-1
        static int n_c = 2;
        static int n_p = 2;
        static int maxBound = 2;
        static int[] idCounts = new int[n_c + n_p];


        public static void main(String[] args) throws InterruptedException {
            Warehouse warehouse = new Warehouse(capacity);

            List<Thread> threadList = new LinkedList<>();

            for (int i = 0; i < n_c; i++) {
                threadList.add(new Thread(new Consumer(warehouse, maxBound, i)));
            }
            for (int i = 0; i < n_p; i++) {
                threadList.add(new Thread(new Producer(warehouse, maxBound, n_c + i)));
            }


            for (Thread thread : threadList) {
                thread.start();
            }
            while (true) {
                TimeUnit.SECONDS.sleep(1);
                System.out.println("\nID COUNTS");
                for (int i = 0; i < n_c + n_p; i++) {
                    System.out.println(idCounts[i]);
                }
            }

        }
    }
