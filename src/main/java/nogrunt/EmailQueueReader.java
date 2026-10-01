package nogrunt;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.concurrent.BlockingQueue;

import org.json.simple.JSONObject;

public class EmailQueueReader extends Thread{
	private EmailQueuingHelper queueHelper;
    private BlockingQueue<Object> queue;
//    private MySQlConn msc;

    public EmailQueueReader(EmailQueuingHelper qh) {
    	queueHelper = qh;
//    	msc = new MySQlConn();
    }

    @Override
    public void run() {
    	 while (!Thread.interrupted() ) {	
    		try {           
    			JSONObject jsonstr = (JSONObject)queueHelper.readFromQueue();
            	if(jsonstr != null) {
            		EmailService es = new EmailService();
            		es.sendMail(jsonstr);
            	}
    		} catch (Exception  e) {
    			e.printStackTrace();
    		}
    	 }
    }

}
