import java.util.concurrent.Semaphore;

public class ThreadDemo
{
    // Create a new semaphore initialized to 1
    // Operations:
    //   gLock.acquire()  -- This is the same thing as sem_wait
    //        - Decrements the value, and blocks it value is less than zero
    //        - A initial value of 1, means the first thread to call acquire will not block
    //   gLock.release() -- This is the same thing as sem_signal
    //        - Increments the value, and unblocks a blocked thread if the new value is zero or less
    static public Semaphore gLock = new Semaphore(1);
    static public int gCount = 0; // Data shared amongst all the threads

    // Class Inherits from the Thread class will be a specific thread
    // On the project you will need one of these per type of thread
    static public class ThreadCode extends Thread
    {
        int id; //store the id of the entity in the simulation

        ThreadCode(int id)
        {
            this.id = id;
        }

        // You need to implement run() to customise the behavior of this thread type. 
        // This will be called by the code when you call the start method that comes from the Thread class
        public void run()
        {
            try
            {
                gLock.acquire(); //The first thread can continue, everyone else must block
                // The above guarantees only one thread can execute the next three lines, at any one time
                System.out.println("Thread " + id + " has count " + gCount);
                gCount++;
                gLock.release(); //Allows one of the blocked threads to continue
            }
            catch(Exception e)
            {
                System.err.println("Error in Thread " + id + ": " + e);
            }
        }
    }

    static public void main(String[] args)
    {
        ThreadCode[] threads = new ThreadCode[5];

        for(int i=0; i<5; i++)
        {
            // Create instances of your custom thread class
            threads[i] = new ThreadCode(i);
            // Create and run the thread
            threads[i].start(); //At this point, thread i is running concurrently
        }

        // Wait for threads to exit
        for(int i=0; i<5; i++) {
            try {
                threads[i].join();
            } catch (InterruptedException e) {
                System.err.println("Error joining with Thread " + i + ": " + e);
            }
        }
    }
}
