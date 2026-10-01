package nogrunt;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;


	public class QueuingHelper  {
    private BlockingQueue<Object> queue = new LinkedBlockingQueue<>();
//    private ExecutionLogger el;
    
    public QueuingHelper() {
    	AppProperties.getProperties();
//    	String logFilePath = Utilities.getAuthoringLogFile();
//    	el = new ExecutionLogger("Authoring", logFilePath);
//    	el.logExecution("Size      Total Mem      Free Mem      Max Mem      Used Mem");
    }

    public void addToQueue(Object data) {
        try {
            queue.put(data);
            System.out.println("Data added to queue: " + data);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public Object readFromQueue() {
        try {
        	try {
	        	int qize = queue.size();
	        	if(qize > 0) {
//	        		el.logExecution("Queue Helper Queue Size" + qize);
	        		
//	        		Runtime runtime = Runtime.getRuntime();
//	        		
//	        		// Calculate different memory values
//	                long totalMemory = (runtime.totalMemory())/1000000; // Total memory currently in use
//	                long freeMemory = (runtime.freeMemory())/1000000; // Free memory available for use
//	                long maxMemory = (runtime.maxMemory())/1000000; // Maximum memory that can be used
//	                long usedMemory = totalMemory - freeMemory; // Currently used memory
	                
//	                el.logExecution("Total Memory - " + totalMemory);
//	                el.logExecution("Free Memory - " + freeMemory);
//	                el.logExecution("Max Memory" + maxMemory);
//	                el.logExecution("Used Memory" + usedMemory);	
//	                el.logExecution(qize + "       " + totalMemory + "MB      " + freeMemory + "MB      " + maxMemory + "MB      " + usedMemory + "MB");
	        	}
        	}catch(Exception e) {
//        		el.logExecution(e);
        	}
            return queue.take();
        } catch (InterruptedException e) {
            e.printStackTrace();
            return null;
        }
    }
}

