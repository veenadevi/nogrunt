package nogrunt;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class AuditQueuingHelper {
	
    private BlockingQueue<Object> queue = new LinkedBlockingQueue<>();
    
    public AuditQueuingHelper() {
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
