import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

class WarehouseLock{
    int n;
    int c;
    private ReentrantLock lock = new ReentrantLock();
    private Condition consumerCondition = lock.newCondition();
    private Condition producerCondition = lock.newCondition();
    private Condition firstConsumerCondition = lock.newCondition();
    private Condition firstProducerCondition = lock.newCondition();

    WarehouseLock(int capacity){
        n = 0;
        c = capacity;
    }

    boolean firstConsumerWaiting = false;
    boolean firstProducerWaiting = false;
    void put(int i) throws InterruptedException {
        lock.lock();
        try{
            while(firstProducerWaiting){
                producerCondition.await();
            }
            firstProducerWaiting = true;
            while(n + i > c){
                firstProducerCondition.await();
            }

            n+=i;
//            System.out.println(n);
            firstConsumerCondition.signal();
            producerCondition.signal();
            firstProducerWaiting = false;
        }finally{
            lock.unlock();
        }
    }

    void take(int i) throws InterruptedException {
        lock.lock();
        try{
            while(firstConsumerWaiting){
                consumerCondition.await();
            }
            firstConsumerWaiting = true;
            while(n-i < 0){
                firstConsumerCondition.await();
            }
            n-=i;

//            System.out.println(n);
            firstProducerCondition.signal();
            consumerCondition.signal();
            firstConsumerWaiting = false;
        }finally{
            lock.unlock();
        }
    }

}


class ProducerLock implements Runnable{
    private final WarehouseLock warehouse;

    Random random;
    int max;
    int id;

    public ProducerLock(WarehouseLock w, int max, int id, int seed){
        this.warehouse = w;
        this.random = new Random(seed);
        this.max = max;
        this.id = id;
    }

    @Override
    public void run() {
        while(true){
            try {
                warehouse.put(random.nextInt(max)+1);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            ProducerConsumerLock.numberOfOperations++;
        }

    }
}




class ConsumerLock implements Runnable{

    private final WarehouseLock warehouse;

    Random random;
    int max;
    int id;

    public ConsumerLock (WarehouseLock w, int max, int id, int seed){
        this.warehouse = w;
        this.random = new Random(seed);
        this.max = max;
        this.id = id;
    }


    @Override
    public void run() {
        while(true){
            try {
                warehouse.take(random.nextInt(max)+1);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            ProducerConsumerLock.numberOfOperations++;
        }
    }
}

public class ProducerConsumerLock {
    static int n = 10000;

    static int maxBound = 5;

    static int numberOfOperations = 0;


    public static void main(String[] args) throws InterruptedException {
        WarehouseLock warehouse = new WarehouseLock(maxBound * 2 - 1);

        List<Thread> threadList = new LinkedList<>();

        for(int i = 0; i < n; i++){
            threadList.add(new Thread(new ConsumerLock(warehouse, maxBound,i,42)));
        }
        for(int i = 0; i < n; i++){
            threadList.add(new Thread(new ProducerLock(warehouse,maxBound,n+i,42)));
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
            System.out.println("epoch" + epoch);
            System.out.println("n operations/real time second " + numberOfOperations/((double)(System.nanoTime() - start)/1_000_000_000));
            nano = 0;
            for (long id : allThreadIds) {
                nano += threadMXBean.getThreadCpuTime(id);
            }
            System.out.println("n operations/cpu time second " + numberOfOperations/((double) nano / 1000_000_000));
            System.out.println("\n");
            epoch++;
        }
    }
}
