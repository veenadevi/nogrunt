package nogrunt;

import java.sql.SQLException;
import java.util.concurrent.BlockingQueue;

import org.json.simple.JSONObject;

public class CoverageReader extends Thread{
    private CoverageHelper coverageHelper;
    private MySqlConn2 msc2;
    private BlockingQueue<Object> queue;

    public CoverageReader(CoverageHelper qh) {
    	msc2 = new MySqlConn2(null);
    	coverageHelper = qh;
    }

    @Override
    public void run() {
    	TransformTS tTS = null;
    	 while (!Thread.interrupted() ) {	
    		try {
    			JSONObject jsonO = (JSONObject)coverageHelper.readFromQueue();
    			try {
				    // Attempt to use the connection for some operation
				    msc2.testCon2.prepareStatement("SELECT 1").executeQuery();
				} catch (SQLException e) {
					msc2.closeDbConn();
			        System.out.println("Connection is closed in Coverage reader.");
			        msc2 = new MySqlConn2(null);
				}
    			msc2.updateDbForElementDetails(jsonO);
    		} catch (Exception  e) {
    			System.err.println("Exception occurred in Queue Reader at: " + Utilities.getCurrentTimestamp());
    			e.printStackTrace();
    		} finally {
    			tTS = null;
    		}
    	 }
    }
}
