package nogrunt;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import org.json.simple.JSONObject;

public class EmailQueuingHelper {
	
private BlockingQueue<Object> queue = new LinkedBlockingQueue<>();
    
    public EmailQueuingHelper() {
    	AppProperties.getProperties();
    }

    public void addToQueue(JSONObject data) {
        try {
            queue.put(data);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public JSONObject readFromQueue() {
        try {
            return (JSONObject)queue.take();
        } catch (InterruptedException e) {
            e.printStackTrace();
            return null;
        }
    }

}
