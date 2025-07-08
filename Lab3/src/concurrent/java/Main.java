public class Main {
    public static void main(String[] args) throws InterruptedException {
        if (args.length != 5) {
            System.out.println("Use: java Main <num_producers> <max_items_per_producer> <producing_time> <num_consumers> <consuming_time>");
            return;
        }
        
        int numProducers = Integer.parseInt(args[0]);
        int maxItemsPerProducer = Integer.parseInt(args[1]);
        int producingTime = Integer.parseInt(args[2]);
        int numConsumers = Integer.parseInt(args[3]) * 2;
        int consumingTime = Integer.parseInt(args[4]);
        
        Buffer buffer = new Buffer(numProducers);

        Thread[] producerThreads = new Thread[numProducers];
        Thread[] consumerThreads = new Thread[numConsumers];
         

        for (int i = 0; i < numProducers; i++) {
            producerThreads[i] = new Thread(new ProducerTask(i, buffer, maxItemsPerProducer, producingTime));
            producerThreads[i].start();
        }
        
        for (int i=0; i < numConsumers; i++) {
            consumerThreads[i] = new Thread(new ConsumerTask(buffer, consumingTime, i, i % 2 == 0));
            consumerThreads[i].start();
        }
        
        for (Thread producer : producerThreads) {
            producer.join();
        }

        System.out.println("Acabamos de produzir!");

        while (!buffer.isFinished()) {
            System.out.println("Estamos consumindo agora, temos " + buffer.getSizeData() + " elementos no array.");
            System.out.println(buffer.getSizeData());
            Thread.sleep(2500);
        }

        System.out.println("Finished: " + buffer.getSizeData());
        System.exit(1);
    }
}


