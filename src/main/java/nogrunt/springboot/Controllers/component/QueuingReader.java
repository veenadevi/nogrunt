package nogrunt.springboot.Controllers.component;

import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import nogrunt.springboot.Controllers.dtoModel.MobileAutomationDTO;
import nogrunt.springboot.Controllers.service.MobileAutomationService;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import static nogrunt.springboot.Controllers.service.MobileAutomationService.mapToDTO;


@Component
public class QueuingReader {

    private volatile boolean running = true;

    private final QueueHelper queueingHelper;
    private final MobileAutomationService mobileAutomationService;


    private final BlockingQueue<JSONObject> processingQueue = new LinkedBlockingQueue<>();

    public QueuingReader(QueueHelper queueingHelper, MobileAutomationService mobileAutomationService) {
        this.queueingHelper = queueingHelper;
        this.mobileAutomationService = mobileAutomationService;
    }

    @PostConstruct
    public void startProcessing() {

        // Start main queue reading thread
        new Thread(() -> {
            while (running) {
                try {
                    JSONObject data = (JSONObject) queueingHelper.readFromQueue();
                    if (data != null) {
                        processingQueue.offer(data); // Add to processing queue
                    } else {
                        Thread.sleep(500);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    System.err.println("Main queue reader thread interrupted.");
                } catch (Exception e) {
                    System.err.println("Error in main queue reader: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }).start();

        new Thread(() -> {
            while (running) {
                try {
                    JSONObject data = processingQueue.poll();
                    if (data != null) {
                        processData(data);
                    } else {
                        Thread.sleep(500);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    System.err.println("Processing thread interrupted.");
                } catch (Exception e) {
                    System.err.println("Error in processing thread: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private void processData(JSONObject data) {
        try {
            Map<String, Object> dataMap = data.toMap();
            MobileAutomationDTO dto = mapToDTO(dataMap);
            mobileAutomationService.saveActions(dto);
        } catch (Exception e) {
            System.err.println("Error processing data: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void stopProcessing() {
        running = false;
        System.out.println("Queue Reader is stopping...");
    }

}