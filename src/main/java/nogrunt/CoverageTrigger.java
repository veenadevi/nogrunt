package nogrunt;

import org.json.simple.JSONObject;

//import java.time.LocalDateTime;
import java.util.*;

import org.json.simple.JSONArray;


public class CoverageTrigger extends TimerTask{
	
//	private MySqlConn2 msc2;
	private MySQlConn msc;
	private Utilities userCache;
	private ExecutionLogger el;
	private HashMap logmap = new HashMap();
	
	  public CoverageTrigger() {
//	    	msc2 = new MySqlConn2();
	    	msc = new MySQlConn(null);
	    	String logFilePath = Utilities.getSchedulerLogFile();
	    	el = new ExecutionLogger("Scheduler", logFilePath);
	    }
	  
	  @Override
      public void run() {    
//		  el.logExecution("Checking for overdue screenshot jobs for triggered testcase");
		  JSONArray jArray = msc.getOverdueSSJobs();
//		  el.logExecution("jobs found : " + jArray.size());
		  for(int i=0;i<jArray.size();i++) {
			  JSONObject jObj = (JSONObject)jArray.get(i);
			//  el.logExecution("running job : " + jObj.toString());
			  int idtestcase = (int)jObj.get("idtest_case");
			  String testCaseName = (String) jObj.get("Test_Case");
			  int companyId = (int)jObj.get("compid");
			  String randomkey = (String)jObj.get("randomkey");
				  el.logExecution("running job : " + jObj.toString());
			  if(msc.getTCCoverageStatus(idtestcase) && msc.getCoverageExecuteStatus(companyId)) {
				  Utilities.callScreenshotApi(idtestcase, companyId, randomkey);
				  msc.updateTestCaseSSStatus(idtestcase, "PickedUp");
				  el.logExecution("completed screenshot jobs for triggered testcase : " + jObj.toString());
			  } else {
				  msc.updateTestCaseSSStatus(idtestcase, "Off");
				  el.logExecution("marked testcase as off : " + jObj.toString());
			  }
		  }	
//		  el.logExecution("Completed this coverage check : " );
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
}

