import java.util.LinkedList;
import java.util.List;



public class Problem {

    static int val;
    static int n;
    static int iterations;

    public static void main(String[] args) throws InterruptedException {
        n = 5;
        val = 0;
        iterations = 4242;

        List<MyThread> threadList = new LinkedList<>();

        for(int i = 0; i < n; i++){

            threadList.add(new MyThread(1,iterations));
            threadList.add(new MyThread(-1,iterations));
//
        }

        for(Thread thread2: threadList){
            thread2.start();
        }


        for(Thread thread3: threadList){
            thread3.join();
        }



        System.out.println(val);
    }
}

class MyThread extends Thread{
    int delta;
    int iterations;

    MyThread(int delta,  int iterations){
        this.delta = delta;

        this.iterations = iterations;
    }

    public void run() {
        for (int i = 0; i < iterations; i++) {
            Problem.val += delta;
        }
    }
}