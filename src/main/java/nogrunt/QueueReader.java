package nogrunt;

import java.io.BufferedReader;
import java.io.IOException;
import java.sql.SQLException;
import java.util.concurrent.BlockingQueue;

import nogrunt.exceptions.*;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

public class QueueReader extends Thread {
    private QueuingHelper queueHelper;
    private BlockingQueue<Object> queue;
    MySQlConn msc;

    public QueueReader(QueuingHelper qh) {
    	queueHelper = qh;
    	msc = new MySQlConn(null);
    }

    @Override
    public void run() {
    	TransformTS tTS = null;
    	 while (!Thread.interrupted() ) {
    		 JSONObject jsonO = new JSONObject();
    		try {           
            	jsonO = (JSONObject)queueHelper.readFromQueue();
            	if(jsonO != null) {
					tTS = new TransformTS();					
					
					try {
					    // Attempt to use the connection for some operation
					    msc.testCon.prepareStatement("SELECT 1").executeQuery();
					} catch (SQLException e) {
						msc.closeDbConn();
				        System.out.println("Connection is closed in TransformTS.");
				        msc = new MySQlConn(null);
					}
					
					tTS.setMSC(msc);
					
					Utilities userCache = (Utilities)jsonO.get("userCache");
					jsonO.remove("userCache");
					tTS.processIncStepsJson(jsonO,"Recording",
							"title",userCache);
            	}
    		}catch(TestStepBeforeTitleException e) {
    			int counter = (int)jsonO.get("addBackCounter");
    			if(counter <= 2) {
        			queueHelper.addToQueue(jsonO);
        			counter +=1;
        			jsonO.put("addBackCounter", counter);
    			}
    		} catch (Exception  e) {
    			System.err.println("Exception occurred in Queue Reader at: " + Utilities.getCurrentTimestamp());
    			e.printStackTrace();
    		} finally {
    			tTS = null;
    		}
    	 }
    }
}

