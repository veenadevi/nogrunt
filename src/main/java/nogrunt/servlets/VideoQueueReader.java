package nogrunt.servlets;

import java.util.concurrent.BlockingQueue;

import org.json.simple.JSONObject;

import nogrunt.TransformTS;
import nogrunt.Utilities;

public class VideoQueueReader extends Thread {
	private BlockingQueue<Object> videoQueue;
	private VideoQueuingHelper videoQueueHelper;

	public VideoQueueReader(BlockingQueue<Object> videoQueue) {	 
		super();
		this.videoQueue = videoQueue;
	}
	
	@Override
	public void run() {
		try {
			while(!Thread.interrupted()) {
	    		try {           
	            	JSONObject jsonO = (JSONObject)videoQueueHelper.readFromQueue();
	            	if(jsonO != null) {
	            		
	            	}
	    		} catch (Exception  e) {
	    			System.err.println("Exception occurred in Queue Reader at: " + Utilities.getCurrentTimestamp());
	    			e.printStackTrace();
	    		}
			}	
		} catch (Exception e) {
            Thread.currentThread().interrupt(); // Restore the interrupted status
        }
	}
}
