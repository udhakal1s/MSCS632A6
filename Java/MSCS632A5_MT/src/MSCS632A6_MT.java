// Umesh Dhakal
//2/21/2026
//MSCS632
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.*;

public class MSCS632A6_MT {

    // Seting true to show 1–2 failures (to prove exception handling)
    private static final boolean SHOW_SAMPLE_ERRORS = true;

    private static void printLine(String s) {
        System.out.println(LocalTime.now() + " | " + s);
    }

    // Small task object
    static class Task {
        int id;
        String data;

        Task(int id, String data) {
            this.id = id;
            this.data = data;
        }
    }

    // Shared queue with wait/notify
    static class TaskQueue {
        private final Queue<Task> queue = new ArrayDeque<>();
        private boolean closed = false;

        public synchronized void addTask(Task t) {
            if (closed) throw new IllegalStateException("Queue is closed.");
            queue.add(t);
            notifyAll();
        }

        public synchronized Task getTask() throws InterruptedException {
            while (queue.isEmpty() && !closed) {
                wait();
            }
            if (queue.isEmpty() && closed) return null;
            return queue.remove();
        }

        public synchronized void closeQueue() {
            closed = true;
            notifyAll();
        }
    }

    // Shared results (list + file).
    static class Results implements AutoCloseable {
        private final List<String> lines = Collections.synchronizedList(new ArrayList<>());
        private BufferedWriter writer; // may be null if file open fails

        Results(String fileName) {
            try {
                writer = new BufferedWriter(new FileWriter(fileName, false));
            } catch (IOException e) {
                writer = null;
                printLine("MAIN | ERROR opening file: " + fileName);
            }
        }

        public void save(String line) {
            lines.add(line);

            if (writer == null) return;

            synchronized (this) {
                try {
                    writer.write(line);
                    writer.newLine();
                    writer.flush();
                } catch (IOException e) {
                    printLine("MAIN | ERROR writing to file");
                }
            }
        }

        public int count() {
            return lines.size();
        }

        @Override
        public void close() {
            if (writer == null) return;

            synchronized (this) {
                try {
                    writer.close();
                } catch (IOException e) {
                    printLine("MAIN | ERROR closing file");
                }
            }
        }
    }

    // Worker thread
    static class Worker implements Runnable {
        private final int workerId;
        private final TaskQueue taskQueue;
        private final Results results;

        Worker(int workerId, TaskQueue taskQueue, Results results) {
            this.workerId = workerId;
            this.taskQueue = taskQueue;
            this.results = results;
        }

        @Override
        public void run() {
            printLine("Worker-" + workerId + " START");

            try {
                while (true) {
                    Task t = taskQueue.getTask();
                    // queue closed + empty
                    if (t == null) break;

                    try {
                        String line = doTask(t);
                        results.save(line);
                        printLine("Worker-" + workerId + " OK Task-" + t.id);
                    } catch (Exception ex) {
                        // log error (simple)
                        printLine("Worker-" + workerId + " ERROR Task-" + t.id + " (" + ex.getClass().getSimpleName() + ")");

                        // saveing failure to results.txt
                        results.save("Task-" + t.id + " FAILED by Worker-" + workerId +
                                " | data=" + t.data +
                                " | error=" + ex.getClass().getSimpleName());
                    }
                }
            } catch (InterruptedException e) {
                printLine("Worker-" + workerId + " INTERRUPTED");
                Thread.currentThread().interrupt();
            }

            printLine("Worker-" + workerId + " END");
        }

        // Simulate work + compute square
        private String doTask(Task t) throws InterruptedException {
            Thread.sleep(ThreadLocalRandom.current().nextInt(150, 501));

            int n = Integer.parseInt(t.data); // can throw NumberFormatException
            long sq = (long) n * n;

            //  Results.txt
            return "Task-" + t.id + " processed by Worker-" + workerId +
                    " | input=" + n +
                    " | square=" + sq;
        }
    }

    public static void main(String[] args) {
        int workers = 4;
        int tasks = 20;

        TaskQueue taskQueue = new TaskQueue();

        try (Results results = new Results("results.txt")) {

            ExecutorService pool = Executors.newFixedThreadPool(workers);
            List<Future<?>> futures = new ArrayList<>();

            for (int i = 1; i <= workers; i++) {
                futures.add(pool.submit(new Worker(i, taskQueue, results)));
            }

            for (int id = 1; id <= tasks; id++) {
                String data = String.valueOf(id);

                if (SHOW_SAMPLE_ERRORS && id == 7) data = "bad-number";
                if (SHOW_SAMPLE_ERRORS && id == 15) data = "xyz";

                try {
                    taskQueue.addTask(new Task(id, data));
                } catch (Exception e) {
                    printLine("MAIN | ERROR adding Task-" + id);
                }
            }

            taskQueue.closeQueue();

            for (Future<?> f : futures) {
                try {
                    f.get();
                } catch (ExecutionException e) {
                    printLine("MAIN | Worker crashed: " + e.getCause());
                } catch (InterruptedException e) {
                    printLine("MAIN | Interrupted while waiting");
                    Thread.currentThread().interrupt();
                }
            }

            pool.shutdown();

            printLine("MAIN | DONE | workers=" + workers + " tasks=" + tasks + " saved=" + results.count() + " file=results.txt");
        }
    }
}
