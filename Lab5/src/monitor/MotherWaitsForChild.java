public class MotherWaitsForChild {
   
    private static volatile boolean done = false;

    private static class Child implements Runnable {
        private Object lock;

        public Child(Object lock) {
            this.lock = lock;
        }
    
        public void thr_exit() {
            synchronized (lock) {
                System.out.println("Child: finished, signal mother...");
                done = true;
                lock.notify();
            }
        }

        @Override
        public void run() {
            thr_exit();
        }

    }

    private static void thr_join(Object lock) {
        synchronized (lock) {
            try {
                if (!done) {
                    lock.wait();
                }
            } catch (InterruptedException e) {}
            System.out.println("mother: woke up!");
        }
    } 

    public static void main(String[] args) {
        Object condVar = new Object();
        System.out.println("mother: begin");
        Thread childThread = new Thread(new Child(condVar), "Child-thread");
        childThread.start();
        thr_join(condVar);
        System.out.println("mother: end");
    }
}
