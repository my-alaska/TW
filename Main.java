import java.util.LinkedList;
import java.util.List;

public class Main {

    static int n = 5;
    
    static int val = 0;

    public static void main(String[] args) throws InterruptedException {
        List<Thread> threadList = new LinkedList<>();

        Thread thread;
        for(int i = 0; i < 2*n; i++){

            thread = new Thread(){ 
                public void run(){
                    for( int j = 0; j < 4242; j++){
                        val++;
                    }
                }
            };
            threadList.add(thread);
//            thread.start();

            thread = new Thread(){
                public void run(){
                    for( int j = 0; j < 4242; j++){
                        val--;
                    }
                }
            };
            threadList.add(thread);
//            thread.start();
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