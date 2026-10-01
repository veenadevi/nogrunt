package nogrunt;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class CleanUpQueueHelper {
    private BlockingQueue<Long> queue = new LinkedBlockingQueue<>();

    public void addToQueue(Long testcaseId) {
        try {
            queue.put(testcaseId);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public Long readFromQueue() {
        try {
            return queue.take(); // Blocking call
        } catch (InterruptedException e) {
            e.printStackTrace();
            return null;
        }
    }
}
