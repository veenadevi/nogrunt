package nogrunt;

import org.json.simple.JSONObject;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.*;

import org.json.simple.JSONArray;


public class Scheduler extends TimerTask{
	
	private MySqlConn2 msc2;
	private MySQlConn msc;
	private Utilities userCache;
	private ExecutionLogger el;
	private HashMap logmap = new HashMap();
	
	  public Scheduler() {
	    	msc2 = new MySqlConn2(null);
//	    	msc = new MySQlConn();
	    	String logFilePath = Utilities.getSchedulerLogFile();
	    	el = new ExecutionLogger("Scheduler", logFilePath);
	    }
	  
	  @Override
      public void run() {    
//		  el.logExecution("Checking for overdue jobs ");
		  checkConn();
		  JSONArray jArray = msc2.getOverdueJobs(el);
//		  el.logExecution("jobs found : " + jArray.toString());
		  if(jArray.size() > 0) {
			  msc = new MySQlConn(msc2.testCon2);
		  }
		  for(int i=0;i<jArray.size();i++) {
			  JSONObject jObj = (JSONObject)jArray.get(i);
			  el.logExecution("running job : " + jObj.toString());
			  int idscheduler = (int)jObj.get("idscheduler");
			  int suiteId = (int)jObj.get("suiteid");
			  int companyId = (int)jObj.get("companyid");
			  ExecutionLogger cel = getLogger(companyId);
			  cel.logExecution("running job : " + jObj.toString());
			  TestSuitePrep tsp = new TestSuitePrep();
			  JSONObject userDets = new JSONObject();
			  userDets.put("uname", (String)jObj.get("scheduleby"));
			  userDets.put("companyid", companyId);
			  msc.setUserDetails(userDets);
			  tsp.tsPrep(msc, suiteId, null, "undefined", -1, companyId, userCache, true, null,1,"", null);
			  LocalDateTime currentDateTime = LocalDateTime.now();
			  msc2.markJobAsExecuted(idscheduler, jObj);
			  el.logExecution("completed triggering job : " + jObj.toString());
			  cel.logExecution("completed triggering job : " + jObj.toString());
		  }	
		  if(jArray.size() > 0) {
			  msc.closeDbConn();
		  }
	  }
	  
	  private ExecutionLogger getLogger(int companyid) {
		  if(logmap.get(companyid) == null) {
			  String logFilePath = Utilities.getCompanySchedulerLogFile(companyid);
			  ExecutionLogger cel = new ExecutionLogger("Scheduler", logFilePath);
			  logmap.put(companyid, cel);
			  return cel;
		  } else {
			  return (ExecutionLogger)logmap.get(companyid);
		  }
	  }
	  
	  private void checkConn() {
		  try {
			    // Attempt to use the connection for some operation
			    msc2.testCon2.prepareStatement("SELECT 1").executeQuery();
			} catch (SQLException e) {
		        System.out.println("Connection OPENING NEW ONE");
		        msc2 = new MySqlConn2(null);
			}
	  }
}
