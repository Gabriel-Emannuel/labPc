class Consumer {
    private final Buffer buffer;
    private final int sleepTime;
    private final int id;
    private boolean isEven;

    public Consumer(int id, Buffer buffer, int sleepTime, boolean isEven) {
        this.id = id;
        this.buffer = buffer;
        this.sleepTime = sleepTime;
        this.isEven = isEven;
    }
    
    public void process() {
        while (true) {
            try {
                int item = buffer.remove();
                if (isEven == (item % 2 != 0)){
                    buffer.put(item);
                }
                else {
                    System.out.println("Consumer " + id + " consumed item " + item);
                }
                Thread.sleep(sleepTime);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}