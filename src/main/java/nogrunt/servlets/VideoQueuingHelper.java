package nogrunt.servlets;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class VideoQueuingHelper {

    // Create a blocking queue to hold video files
    private BlockingQueue<Object> videoQueue;

    public VideoQueuingHelper() {
        // Initialize the video queue (can specify a maximum capacity if needed)
        videoQueue = new LinkedBlockingQueue<>();
    }

    public void addToQueue(Object videoFile) {
        try {
            videoQueue.put(videoFile);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public Object readFromQueue() {
        try {
            return videoQueue.take();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        }
    }
}
