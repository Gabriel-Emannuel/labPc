import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Semaphore;

class Buffer {
    
    private final int SIZE = 100;

    private final Semaphore mutexProducer = new Semaphore(1);
    private final Semaphore mutex = new Semaphore(1);

    private final Semaphore putSema = new Semaphore(SIZE);
    private final Semaphore removeSema = new Semaphore(0);

    private final List<Integer> data = new ArrayList<>();

    private int producersFinished;

    private int counterProducersFineshed = 0;

    public Buffer(int producersFinished) {
        this.producersFinished = producersFinished;
    }

    public boolean isFinished() {
        return this.producersFinished == counterProducersFineshed && this.data.isEmpty();
    }

    public void finishOneProducer() throws InterruptedException {
        mutexProducer.acquire();
        this.counterProducersFineshed++;
        mutexProducer.release();
    }

    public void put(int value) throws InterruptedException {
        // Antes de adicionar, é verificado se já foi alcançado número máximo de elementos
        this.putSema.acquire();
        this.mutex.acquire();
            
            data.add(value);
            System.out.println("Inserted: " + value + " | Buffer size: " + data.size());

            // Depois de adicionar o elemento, permite que um elemento seja removido
            this.removeSema.release();
        this.mutex.release();
    }
    
    public int remove() throws InterruptedException {
        // Antes de remover, é verificado se existem elementos para a remoção
        this.removeSema.acquire();
        this.mutex.acquire();
            
            int value = data.remove(0);
            System.out.println("Removed: " + value + " | Buffer size: " + data.size());

            // Depois de remover o elemento, permite que um elemento seja adicionado        
            this.putSema.release();
        this.mutex.release();
        return value;
    }

    public int getSizeData() {
        return this.data.size();
    }
}