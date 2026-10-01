package nogrunt;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class CoverageHelper {
    private BlockingQueue<Object> queue = new LinkedBlockingQueue<>();
    
    public CoverageHelper() {
    	AppProperties.getProperties();
    }

    public void addToQueue(Object data) {
        try {
            queue.put(data);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public Object readFromQueue() {
        try {
            return queue.take();
        } catch (InterruptedException e) {
            e.printStackTrace();
            return null;
        }
    }
}
