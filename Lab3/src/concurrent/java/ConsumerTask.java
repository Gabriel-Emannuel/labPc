public class ConsumerTask implements Runnable {


    private final Buffer buffer;    
    private final int sleepTime;
    private final int id;
    private boolean isEven;
    
    public ConsumerTask(Buffer buffer, int sleepTime, int id, boolean isEven) {
        this.buffer = buffer;
        this.sleepTime = sleepTime;
        this.id = id;
        this.isEven = isEven;
    }

    @Override
    public void run() {
        (new Consumer(id, buffer, sleepTime, isEven)).process();
    }
    
}
