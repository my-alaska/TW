import java.util.LinkedList;
import java.util.List;

/*
ZAKLESZCZENIE

2 producentów: P1, P2
2 konsumentów: C1, C2
status wyjściowy bufora 0/1: 0

status bufora: 0
1) C1 i C2 chcą po kolei pobrać z bufora. Oboje nie mogą więc aktywowują "wait()"
status bufora: 0
2) P1 chce zapisać do bufora: może to zrobić i aktywuje "notify()"
status bufora: 1
3) P2 chce zapisać do bufora, nie może więc aktywuje "wait()"
status bufora: 1
4) P2 chce zapisać do bufora, nie może więc aktywuje "wait()"
status bufora: 1
5) C1 odbiera "notify()" aktywowane przez P1. pobiera z bufora i aktywuje "notify()"
status bufora: 0
6) C1 chce pobrać z bufora, nie może więc aktywuje "wait()"
status bufora: 0
7) C2 odbiera "notify()" aktywowane przez C1. nie może więc aktywuje "wait()"
status bufora: 0

W ten sposób wszystkie wątki czekają
*/

class Warehouse{

    int n;
    int c;

    Warehouse(int capacity){
        n = 0;
        c = capacity;
    }

    synchronized void put() throws InterruptedException {
        while(n == c){
            wait();
        }
        n++;
        System.out.println(n);
        notify();
    }

    synchronized void take() throws InterruptedException {
        while(n == 0){
            wait();
       }
       n--;
       System.out.println(n);
       notify();
   }

}


class Producer implements Runnable{
    private final Warehouse warehouse;

    public Producer(Warehouse w){
        this.warehouse = w;
    }

    @Override
    public void run() {
        while(true){
            try {
                warehouse.put();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }
}




class Consumer implements Runnable{

    private final Warehouse warehouse;



    public Consumer (Warehouse w){
        this.warehouse = w;
    }


    @Override
    public void run() {
        while(true){
            try {
                warehouse.take();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }
}

public class ProducerConsumerProblem {
    static int capacity = 1;
    static int n_c = 1;
    static int n_p = 1;


    public static void main(String[] args) throws InterruptedException {
        Warehouse warehouse = new Warehouse(capacity);

        List<Thread> threadList = new LinkedList<>();

        for(int i = 0; i < n_c; i++){
            threadList.add(new Thread(new Consumer(warehouse)));
        }
        for(int i = 0; i < n_p; i++){
            threadList.add(new Thread(new Producer(warehouse)));
        }



        for(Thread thread: threadList){
            thread.start();
        }

    }




}