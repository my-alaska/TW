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

    public ProducerLock(WarehouseLock w, int max, int id){
        this.warehouse = w;
        this.random = new Random();
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
            ProducerConsumerLock.idCounts[this.id]++;
        }

    }
}




class ConsumerLock implements Runnable{

    private final WarehouseLock warehouse;

    Random random;
    int max;
    int id;

    public ConsumerLock (WarehouseLock w, int max, int id){
        this.warehouse = w;
        this.random = new Random();
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
            ProducerConsumerLock.idCounts[this.id]++;
        }
    }
}

public class ProducerConsumerLock {
    static int capacity = 3; // must be at least 2*maxBound-1
    static int n_c = 2;
    static int n_p = 2;
    static int maxBound = 2;
    static int[] idCounts = new int[n_c+n_p];


    public static void main(String[] args) throws InterruptedException {
        WarehouseLock warehouse = new WarehouseLock(capacity);

        List<Thread> threadList = new LinkedList<>();

        for(int i = 0; i < n_c; i++){
            threadList.add(new Thread(new ConsumerLock(warehouse, maxBound,i)));
        }
        for(int i = 0; i < n_p; i++){
            threadList.add(new Thread(new ProducerLock(warehouse,maxBound,n_c+i)));
        }


        for(Thread thread: threadList){
            thread.start();
        }
        while(true){
            TimeUnit.SECONDS.sleep(1);
            System.out.println("\nID COUNTS");
            for(int i = 0; i < n_c+n_p;i++){
                System.out.println(idCounts[i]);
            }
        }

    }
}
