package nogrunt;

import java.io.BufferedReader;
import java.io.IOException;
import java.sql.SQLException;
import java.util.concurrent.BlockingQueue;

import org.json.simple.JSONObject;

public class AuditQueueReader extends Thread{
	private AuditQueuingHelper queueHelper;
    private BlockingQueue<Object> queue;
    private MySQlConn msc;

    public AuditQueueReader(AuditQueuingHelper qh) {
    	queueHelper = qh;
    	msc = new MySQlConn(null);
    }

    @Override
    public void run() {
    	 while (!Thread.interrupted() ) {	
    		try {           
    			JSONObject jsonstr = (JSONObject)queueHelper.readFromQueue();
            	if(jsonstr != null) {
            		try {
					    // Attempt to use the connection for some operation
					    msc.testCon.prepareStatement("SELECT 1").executeQuery();
					} catch (SQLException e) {
				        System.out.println("Audit Connection OPENING NEW ONE");
				        msc = new MySQlConn(null);
					}
            		msc.audit(jsonstr);
            	}
    		} catch (Exception  e) {
    			e.printStackTrace();
    		}
    	 }
    }

}
