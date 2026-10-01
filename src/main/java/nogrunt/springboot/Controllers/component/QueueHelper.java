package nogrunt.springboot.Controllers.component;

import org.json.JSONObject;
import org.springframework.stereotype.Component;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;


@Component
public class QueueHelper {
    private BlockingQueue<Object> queue = new LinkedBlockingQueue<>();

    public void addToQueue(JSONObject step) {
        try {
            queue.put(step);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public JSONObject readFromQueue() {
        try {
            try {
                int qize = queue.size();
                if(qize > 0) {
                }
            }catch(Exception e) {
            }
            return (JSONObject) queue.take();
        } catch (InterruptedException e) {
            e.printStackTrace();
            return null;
        }
    }
}
