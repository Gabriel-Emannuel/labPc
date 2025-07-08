public class ProducerTask implements Runnable {

    private final Buffer buffer;
    private final int maxItems;
    private final int sleepTime;
    private final int id;
    
    public ProducerTask(int id, Buffer buffer, int maxItems, int sleepTime) {
        this.id = id;
        this.buffer = buffer;
        this.maxItems = maxItems;
        this.sleepTime = sleepTime;
    }
    @Override
    public void run() {
        (new Producer(id, buffer, maxItems, sleepTime)).produce();
    }
    
}
