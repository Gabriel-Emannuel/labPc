public class MotherWaitsForChild {
    private static volatile boolean done = false;

    public static void main(String[] args) {
        Runnable childTask = () -> {
            System.out.println("child");
            done = true;
        };
        System.out.println("mother: begin");
        Thread childThread = new Thread(childTask, "child-thread");
        childThread.start();
        while (!done) {}
        System.out.println("mother: end");
    }
}


// Primeiro estagio

// public class MotherWaitsForChild {
//     public static void main(String[] args) {
//         Runnable childTask = () -> {
//             System.out.println("child");
//         };
//         System.out.println("mother: begin");
//         Thread childThread = new Thread(childTask, "child-thread");
//         childThread.start();
//         System.out.println("mother: end");
//     }
// }