import java.io.*;
import java.util.*;

public class FileSimilarity {

    // Total sum of all files
    static long totalSum = 0;
    static Object condVarTotalSum = new Object();

    static int BASE_THREADS_ARGS= 5,
               BASE_THREADS_FILES = 2;

    static int threadsSum = BASE_THREADS_ARGS,
                 threadsSimmilarity = BASE_THREADS_FILES;

    static int  sumAchieved = 0,
                similarityAchieved = 0;

    
    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.err.println("Usage: java Sum filepath1 filepath2 filepathN");
            System.exit(1);
        }

        int numFiles = args.length/2;
        if (BASE_THREADS_ARGS > numFiles)  threadsSum = numFiles;
        if (BASE_THREADS_FILES > numFiles) threadsSimmilarity = numFiles;

        // Create a map to store the fingerprint for each file
        Map<String, List<Long>> fileFingerprints = new HashMap<>();

        Thread[] threadsFileSum = new Thread[threadsSum];
        Thread[] threadsFileSimilarity = new Thread[threadsSimmilarity];
        
        Buffer<String> inputs = new Buffer(),
                       outputs = new Buffer();

        Buffer<PairFiles> files = new Buffer();
        
        Object condVar = new Object();

        for (int i = 0; i < threadsSum; i++)
        {
            threadsFileSum[i] = new Thread(new ThreadSum(inputs, outputs, fileFingerprints, condVar));
            threadsFileSum[i].start();
        }

        for (int i = 0; i < threadsSimmilarity; i++)
        {
            threadsFileSimilarity[i] = new Thread(new ThreadSimilarity(files));
            threadsFileSimilarity[i].start();
        }

        for (String arg: args)
        {
            inputs.add(arg);
        }
        inputs.add(null);

        PairFiles pair;

        String output;

        List<String> paths = new ArrayList<>();

        while ((output = outputs.remove()) != null)
        {
            for (String path: paths) {
                pair = new PairFiles(path, output, fileFingerprints.get(path), fileFingerprints.get(output));
                files.add(pair);
            }
            paths.add(output);
        }

        files.add(null);

        for (int i = 0; i < threadsSimmilarity; i++)
        {
            threadsFileSimilarity[i].join();
        }

        // Printing totalSum
        System.out.println("Total sum: " + totalSum);
    }

    private static List<Long> fileSum(String filePath) throws IOException {
        File file = new File(filePath);
        List<Long> chunks = new ArrayList<>();
        try (FileInputStream inputStream = new FileInputStream(file)) {
            byte[] buffer = new byte[100];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                long sum = sum(buffer, bytesRead);
                chunks.add(sum);
                synchronized (condVarTotalSum) {
                    totalSum += sum;
                }
            }
        }
        return chunks;
    }

    private static long sum(byte[] buffer, int length) {
        long sum = 0;
        for (int i = 0; i < length; i++) {
            sum += Byte.toUnsignedInt(buffer[i]);
        }
        return sum;
    }

    private static float similarity(List<Long> base, List<Long> target) {
        int counter = 0;
        List<Long> targetCopy = new ArrayList<>(target);

        for (Long value : base) {
            if (targetCopy.contains(value)) {
                counter++;
                targetCopy.remove(value);
            }
        }

        return (float) counter / base.size();
    }

    private static class ThreadSum implements Runnable {

        Buffer<String> pathsInput;
        
        Buffer<String> pathsOutputs;

        Map<String, List<Long>> fileFingerprints;

        public ThreadSum(FileSimilarity.Buffer<String> pathsInput, FileSimilarity.Buffer<String> pathsOutputs,
                Map<String, List<Long>> fileFingerprints, Object condvar) {
            this.pathsInput = pathsInput;
            this.pathsOutputs = pathsOutputs;
            this.fileFingerprints = fileFingerprints;
            this.condvar = condvar;
        }

        Object condvar;

        @Override
        public void run() {
            String path;
            try {
                while ((path = pathsInput.remove()) != null) {
                    List<Long> fingerprint = fileSum(path);
                    synchronized (condvar)
                    {
                        fileFingerprints.put(path, fingerprint);
                    }
                    pathsOutputs.add(path);
                }
            } catch (InterruptedException | IOException e) {
                e.printStackTrace();
            } finally {
                synchronized (condvar) {
                    if (++sumAchieved == threadsSum) {
                        pathsOutputs.add(null);
                    }
                }
                pathsInput.add(null);
            }
        }

    }

    private static class PairFiles {
        String path1;
        String path2;
        List<Long> path1Similarity;
        List<Long> path2Similarity;

        public PairFiles(String path1, String path2, List<Long> path1Similarity, List<Long> path2Similarity) {
            this.path1 = path1;
            this.path2 = path2;
            this.path1Similarity = path1Similarity;
            this.path2Similarity = path2Similarity;
        }

    }

    private static class ThreadSimilarity implements Runnable {

        Buffer<PairFiles> filesToRun;

        public ThreadSimilarity(FileSimilarity.Buffer<FileSimilarity.PairFiles> filesToRun) {
            this.filesToRun = filesToRun;
        }

        @Override
        public void run() {
            PairFiles files;
            try {
                while ((files = this.filesToRun.remove()) != null)
                {
                    float similarityScore = similarity(files.path1Similarity, files.path2Similarity);
                    System.out.println("Similarity between " + files.path1 + " and " + files.path2 + ": " + (similarityScore * 100) + "%");
                }
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } finally {
                filesToRun.add(null);
            }
        }

    }

    private static class Buffer<T> {

        private Queue<T> buffer;
        
        private int contador;

        public Buffer() {
            this.buffer = new LinkedList<>();
        }

        public synchronized void add(T element) {
            buffer.add(element);
            this.notify();
        } 

        public synchronized T remove() throws InterruptedException {
            while (this.buffer.isEmpty()) {
                this.wait();
            }
            return buffer.remove();
        }
    }
}
