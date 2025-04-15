import java.util.Random;
import java.util.concurrent.Semaphore;

public class BankSimulation {
    // Shared resources
    private static final Semaphore bankOpen = new Semaphore(0); // Controls when bank opens
    private static final Semaphore safe = new Semaphore(2); // Only 2 tellers in safe at once
    private static final Semaphore manager = new Semaphore(1); // Only 1 teller with manager at once
    private static final Semaphore door = new Semaphore(2); // Only 2 customers can enter at once

    private static final Semaphore[] tellerAvailable = new Semaphore[3]; // Indicates if teller is ready
    private static final Semaphore[] customerAtTeller = new Semaphore[3]; // Customer at teller
    private static final Semaphore[] transactionComplete = new Semaphore[3]; // Transaction completed

    private static volatile int[] customerTransaction = new int[3]; // 0=Deposit, 1=Withdrawal
    private static volatile int[] currentCustomer = new int[3]; // ID of current customer for each teller
    private static volatile int customersLeft = 0; // Count of customers who have left

    private static final Semaphore customerLine = new Semaphore(0); // Customers waiting in line
    private static final Semaphore mutex = new Semaphore(1); // For protecting shared variables

    private static final Random random = new Random();

    // Teller class
    static class Teller extends Thread {
        private final int id;

        public Teller(int id) {
            this.id = id;
        }

        @Override
        public void run() {
            try {
                // Teller is ready
                System.out.println("Teller " + id + " []: ready to serve");
                tellerAvailable[id].release();

                // Wait for bank to open (all tellers ready)
                bankOpen.acquire();

                while (customersLeft < 50) {
                    // Wait for customer to approach
                    System.out.println("Teller " + id + " []: waiting for a customer");
                    customerAtTeller[id].acquire();

                    // Get customer info
                    int customerId = currentCustomer[id];
                    int transaction = customerTransaction[id];
                    String transactionType = transaction == 0 ? "deposit" : "withdrawal";

                    // Ask for transaction
                    System.out.println("Teller " + id + " [Customer " + customerId + "]: serving a customer");
                    System.out.println("Teller " + id + " [Customer " + customerId + "]: asks for transaction");

                    // If withdrawal, go to manager
                    if (transaction == 1) {
                        System.out.println("Teller " + id + " [Customer " + customerId + "]: going to the manager");
                        manager.acquire();
                        System.out.println("Teller " + id + " [Customer " + customerId + "]: getting manager's permission");

                        // Simulate time with manager
                        int managerTime = random.nextInt(26) + 5; // 5-30ms
                        Thread.sleep(managerTime);

                        System.out.println("Teller " + id + " [Customer " + customerId + "]: got manager's permission");
                        manager.release();
                    }

                    // Go to safe
                    System.out.println("Teller " + id + " [Customer " + customerId + "]: going to safe");
                    safe.acquire();
                    System.out.println("Teller " + id + " [Customer " + customerId + "]: enter safe");

                    // Simulate transaction in safe
                    int safeTime = random.nextInt(41) + 10; // 10-50ms
                    Thread.sleep(safeTime);

                    System.out.println("Teller " + id + " [Customer " + customerId + "]: leaving safe");
                    safe.release();

                    // Transaction complete
                    System.out.println("Teller " + id + " [Customer " + customerId + "]: finishes " + transactionType + " transaction");
                    transactionComplete[id].release();

                    // Ready for next customer
                    tellerAvailable[id].release();
                }

                System.out.println("Teller " + id + " []: no more customers, bank is closing");
            } catch (InterruptedException e) {
                System.err.println("Teller " + id + " interrupted: " + e.getMessage());
            }
        }
    }

    // Customer class
    static class Customer extends Thread {
        private final int id;
        private int transaction; // 0=Deposit, 1=Withdrawal

        public Customer(int id) {
            this.id = id;
        }

        @Override
        public void run() {
            try {
                // Decide transaction type
                transaction = random.nextInt(2); // 0=Deposit, 1=Withdrawal
                String transactionType = transaction == 0 ? "deposit" : "withdrawal";
                System.out.println("Customer " + id + " []: wants to perform a " + transactionType + " transaction");

                // Random wait before entering bank (0-100ms)
                int enterTime = random.nextInt(101);
                Thread.sleep(enterTime);

                // Going to bank
                System.out.println("Customer " + id + " []: going to bank.");

                // Try to enter bank through door
                door.acquire();
                System.out.println("Customer " + id + " []: entering bank.");

                // Get in line
                System.out.println("Customer " + id + " []: getting in line.");

                int tellerID = -1;
                mutex.acquire();

                // Check for available teller
                for (int i = 0; i < 3; i++) {
                    if (tellerAvailable[i].tryAcquire()) {
                        tellerID = i;
                        break;
                    }
                }

                if (tellerID == -1) {
                    // No teller available, wait in line
                    System.out.println("Customer " + id + " []: waiting in line");
                    mutex.release();
                    customerLine.acquire();

                    mutex.acquire();
                    // After being signaled, find available teller
                    for (int i = 0; i < 3; i++) {
                        if (tellerAvailable[i].tryAcquire()) {
                            tellerID = i;
                            break;
                        }
                    }
                }

                // Found a teller
                currentCustomer[tellerID] = id;
                customerTransaction[tellerID] = transaction;
                mutex.release();

                // Approach teller
                System.out.println("Customer " + id + " [Teller " + tellerID + "]: selects teller");
                System.out.println("Customer " + id + " [Teller " + tellerID + "]: introduces itself");
                customerAtTeller[tellerID].release();

                // Wait for transaction to complete
                transactionComplete[tellerID].acquire();

                // Leave bank
                System.out.println("Customer " + id + " []: goes to door");
                System.out.println("Customer " + id + " []: leaves the bank");

                mutex.acquire();
                customersLeft++;
                if (customersLeft < 50 && customerLine.hasQueuedThreads()) {
                    customerLine.release(); // Signal next customer in line
                }
                mutex.release();

                door.release();
            } catch (InterruptedException e) {
                System.err.println("Customer " + id + " interrupted: " + e.getMessage());
            }
        }
    }

    public static void main(String[] args) {
        // Initialize semaphores
        for (int i = 0; i < 3; i++) {
            tellerAvailable[i] = new Semaphore(1); // Start as available
            customerAtTeller[i] = new Semaphore(0);
            transactionComplete[i] = new Semaphore(0);
        }

        // Create teller threads
        Teller[] tellers = new Teller[3];
        for (int i = 0; i < 3; i++) {
            tellers[i] = new Teller(i);
            tellers[i].start();
        }

        // Wait for all tellers to be ready
        try {
            for (int i = 0; i < 3; i++) {
                tellerAvailable[i].acquire(); // Wait for each teller to signal ready
            }

            // Open the bank
            bankOpen.release(3);

            // Create customer threads
            Customer[] customers = new Customer[50];
            for (int i = 0; i < 50; i++) {
                customers[i] = new Customer(i);
                customers[i].start();
            }

            // Wait for all customers to finish
            for (int i = 0; i < 50; i++) {
                customers[i].join();
            }

            System.out.println("All customers served, bank is closed");

        } catch (InterruptedException e) {
            System.err.println("Main thread interrupted: " + e.getMessage());
        }
    }
}