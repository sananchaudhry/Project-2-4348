import java.util.concurrent.Semaphore;
import java.util.Random;

public class SignalDemo
{
    static int shared = 0;

    // Extend Thread Class For one type of thread, the sending thread
    public static class SendingThread extends Thread{
        Semaphore semaphore1 = null;
        Semaphore semaphore2 = null;
        Random rand = new Random(); 

        // In this example, we pass the semaphore as a parameter
        // This will probably not work as well with your project, because you need
        // Too many semaphores
        public SendingThread(Semaphore semaphore1, Semaphore semaphore2){
            this.semaphore1 = semaphore1;
            this.semaphore2 = semaphore2;
        }

        //Implement behavior of this thread type
        public void run(){
            try {
                while(true){
                    //do something, then signal
                    int tmp = rand.nextInt(1000);
                    System.out.println("Send: " + tmp);
                    shared = tmp;

                    // Random number is ready, inform Receiver by signally semaphore1
//                    this.semaphore1.release();
                    // We do not want to write over the shared variable while the receiver is using it
                    // wait for a signal from the receiver
//                    this.semaphore2.acquire();

                }
            } catch (Exception e)
            {
                System.out.println(e);
            }
        }
    }

    // Extend Thread Class For one type of thread, the receiving thread
    public static class ReceivingThread extends Thread{
        Semaphore semaphore1 = null;
        Semaphore semaphore2 = null;

        // In this example, we pass the semaphore as a parameter
        // This will probably not work as well with your project, because you need
        // Too many semaphores
        public ReceivingThread(Semaphore semaphore1, Semaphore semaphore2){
            this.semaphore1 = semaphore1;
            this.semaphore2 = semaphore2;
        }

        //Implement behavior of this thread type
        public void run(){
            try{
                while(true){
//                    this.semaphore1.acquire(); //Wait for sender to have number ready
                    //Do Something
                    System.out.println("Received: " + shared);
                    //We are done with the shared number, signal the sender to tell it
                    //to continue
 //                   this.semaphore2.release();
                }
        } catch (Exception e)
        {
            System.out.println(e);
        }
    }
}

static public void main(String[] args)
{
    // Create semaphores locally. This works because we are passing them in as parameters
    // to the constructors. This will likely not work well for your project
    // They are initialized to zero. So every thread that 'acquire' them will have to block
    // until another thread calls 'release'
    Semaphore semaphore1 = new Semaphore(0);
    Semaphore semaphore2 = new Semaphore(0);

    // create instance of the sender thread class
    SendingThread sender = new SendingThread(semaphore1,semaphore2);

    // create instance of the receiver thread class
    ReceivingThread receiver = new ReceivingThread(semaphore1,semaphore2);

    // Create and run the threads
    receiver.start(); // Receiver is now running concurrently
    sender.start(); // Sender is now running concurrently
}
}
