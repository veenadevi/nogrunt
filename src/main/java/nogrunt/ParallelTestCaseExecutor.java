package nogrunt;

import java.util.Set;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

public class ParallelTestCaseExecutor {
	
	public JSONObject executeTestCase(JSONObject json, ExecutionLogger tsel, 
			JSONObject emailData, SelGrid selGrid, String browser1, MySQlConn msc2,
			Set dupTC, Set failedTC, Tester tester, int companyId, String randomKey,
			String envUrl, EmailQueuingHelper eqh, boolean rerunFailedTests, 
			boolean stopAfterFailure, boolean firstRun, long tsStartTime,
			boolean synchScenario, boolean ssForAllElement, boolean ssonerror,
			String proxyurl1, boolean indEmail, String mode) {
		
		JSONObject original = (JSONObject)json.get("testcases");
		JSONObject testcases = (JSONObject)json.get("testcases");
		JSONObject zero = (JSONObject)testcases.get(0);
		int testCaseId = (int)zero.get("idtest_case");
		int tsrid = (int)zero.get("tsrid");
		msc2.updateNotExecutedCount(tsrid);
		JSONArray jsonArr = new JSONArray();
		JSONArray dependantTCArr = msc2.getTestStepsForTCIDAndAncestors(testCaseId,jsonArr);
		int depTcCount = dependantTCArr.size()-1;
		if (depTcCount > 0) {
			if (tester.firstRun) msc2.updateDepTcCountRunOne(tsrid, depTcCount);
			else msc2.updateDepTcCountRunTwo(tsrid, depTcCount);
		}
		
		JSONObject result = new JSONObject();
		int tcid = -1;
		String tcname = "";
		boolean analyseFailures = false;
		if(mode != null && mode.equalsIgnoreCase("analysis")) {
			analyseFailures = true;
		}
		
		for(int i=0;i<dependantTCArr.size();i++) {
			tcid = (int)dependantTCArr.get(i);
			zero.put("idtest_case",tcid);
			
			
			JSONObject depjson = msc2.getTestCaseDetailsFromId(tcid);
			if(zero.get("tsrid") != null) {
				depjson.put("tsrid", zero.get("tsrid"));
			}
			tcname = (String)depjson.get("Test_Case");
//			if(i > 0) {
//				depjson.put("continuetest",true);
//			}
			
			testcases.put(0,depjson);
			
			result =  executeTestCase(json, tsel, emailData, selGrid, browser1, msc2,
				dupTC, failedTC, tester, companyId, randomKey, envUrl,  eqh, 
				rerunFailedTests, stopAfterFailure, firstRun, tsStartTime,
				synchScenario, ssForAllElement, ssonerror, proxyurl1, indEmail, 
				true, analyseFailures, dependantTCArr);
			
			if(result.get("status") == null ||
					((String)result.get("status")).equals("FAIL")) {
				//tester.addToFailedTCList(original, indEmail);
				break;
			}			
		}
		long endTime = System.currentTimeMillis();
		double dur = (endTime - tsStartTime) / 1000.0;
		String status = (String)result.get("status");
		
		if(tester.firstRun) {
			if(status == null) {
				msc2.updateCount(tsrid, "FAIL", "RUNNING", dur);
			} else if(status.equalsIgnoreCase("ignore")) {
			} else if(status.equalsIgnoreCase("PASS")) {
				msc2.updateCount(tsrid, "PASS", "RUNNING", dur);
			} else if(status.equalsIgnoreCase("FAIL")){
				msc2.updateCount(tsrid, "FAIL", "RUNNING", dur);
			} else if(status.equalsIgnoreCase("STOPP")){
				msc2.updateCount(tsrid, "FAIL", "STOPP", dur);
			} else {
				msc2.updateCount(tsrid, "FAIL", "RUNNING", dur);
			} 
		} else {
			if(status.equalsIgnoreCase("PASS")) {
				msc2.update2ndRun(tsrid, dur);
				msc2.update2ndRunPassCount(tsrid);
			} else {
				msc2.updateDur(tsrid, dur);
				msc2.update2ndRunFailCount(tsrid);
			}
		}
		result.put("tsduration", dur);
		tsel.logExecution("Tearing Down Session - test case completed - " + 
				tcid + " - " + tcname);
		selGrid.tearDown();
		for(int i=0;i<dependantTCArr.size();i++) {
			int tcId = (int)dependantTCArr.get(i);
			int ptcrid = msc2.getSecondLatestPassedTestCaseResultsId(tcId);
			if(status.equalsIgnoreCase("PASS")) {
				String folder = AppProperties.datadirectory + "\\" + companyId + "\\" + tcId + "\\" + ptcrid + "\\" + "pagesource";
				try {
					Utilities.deleteFolderContents(folder);
				} catch(Exception e) {
					
				}
			}
		}
		return result;
	}
	
	public JSONObject executeTestCase(JSONObject json, ExecutionLogger tsel, 
			JSONObject emailData, SelGrid selGrid, String browser1, MySQlConn msc2,
			Set dupTC, Set failedTC, Tester tester, int companyId, String randomKey,
			String envUrl, EmailQueuingHelper eqh, boolean rerunFailedTests, 
			boolean stopAfterFailure, boolean firstRun, long tsStartTime,
			boolean synchScenario, boolean ssForAllElement, boolean ssonerror,
			String proxyurl1, boolean indEmail, boolean individualTC, boolean analyseFailures, JSONArray dependantTCArr) {
		
		JSONObject results = new JSONObject();
		if(json.get("testsuite") != null && ((String)json.get("testsuite")).equalsIgnoreCase("true")) {
			return results;
		}
		JSONObject testcases = (JSONObject)json.get("testcases");
		JSONObject zero = (JSONObject)testcases.get(0);
		int testCaseId = (int)zero.get("idtest_case");
		int tsrid = (int)zero.get("tsrid");
		selGrid.tsr = tsrid;
		String master = (String)zero.get("master");
		boolean isMaster = false;
		if(master != null && master.equalsIgnoreCase("yes")) {
			isMaster = true;
		}
		if(dupTC != null && !dupTC.contains(testCaseId)) {
			tsel.logExecution("Failed the dupTC test test case id " +  testCaseId + " --- Test Case Name " + (String)zero.get("Test_Case"));
			return results;
		}
		
		JSONObject testcase = msc2.getTestStepsFromDBByTestCaseId(testCaseId, true, false);
		JSONObject testCaseDetails = zero;
		
		int synchResultid = -1;
		int synchtcid = -1;
		if(json.get("synchtcid") != null ) {
			synchResultid = (int)json.get("synchtcrid");
			synchtcid = (int)json.get("synchtcid");
		}
		int testCaseResultsId = -1;
		JSONObject synchtestcase = null;
		long startTime = System.currentTimeMillis();
		if(!synchScenario) {
			testCaseResultsId = msc2.writeTestCaseStatusToDB(testCaseId, "STARTED",
					"SYS", 0.0, browser1, (String)testCaseDetails.get("Test_Case"),
					companyId);
		} else {
			testCaseResultsId = (int)json.get("tcrid");
			synchtestcase = msc2.getTestStepsFromDBByTestCaseId(synchtcid, true, false);
		}
		ExecutionLogger tcel = tester.sortEL(selGrid, companyId, testCaseId, testCaseResultsId);
		json.put("tcrid", testCaseResultsId);
		boolean newBrowser = false;
		
		if(selGrid.BROWSER != null) {
			newBrowser = tester.newBrowserForAV(testCaseDetails, 
					selGrid.enableaudio, selGrid.enablevideo);
		}
		
		if((boolean)testCaseDetails.get("forcenewsession")) {
			tsel.logExecution("Tearing Down Session - forcing a new session : " + testCaseId 
					+ "--" + (String)zero.get("Test_Case"));
			selGrid.tearDown();
			newBrowser = true;
		}
		
		if(selGrid.BROWSER == null || newBrowser) {
			try {
				boolean sesSuccess = tester.browserSetup(selGrid, browser1, proxyurl1, tsel, 
						(String)testCaseDetails.get("Test_Case"),
						(boolean)testCaseDetails.get("enableaudio"),
						(boolean)testCaseDetails.get("enablevideo"), null,
						testCaseId, (boolean)testCaseDetails.get("stealthMode"));
				
				if(!sesSuccess) {
					msc2.writeTestCaseStatusToDB(testCaseId, "ABORTED",
							"SYS", 0.0, selGrid.BROWSER, (String)testCaseDetails.get("Test_Case"),
							companyId);
					msc2.linkTSRtoTCR(tsrid, testCaseResultsId);
					tsel.logExecution("Test Aborted");
					tester.updateTestSuiteStatus(tsrid, "FAILED");
					return results;
				}
			} catch (Exception e) {
				tsel.logExecution(e);
				msc2.writeTestCaseStatusToDB(testCaseId, "FAILED",
						"SYS", 0.0, selGrid.BROWSER, (String)testCaseDetails.get("Test_Case"),
						companyId);
				return results;
			}
		}
		
		tcel.logExecution("Running from runGridTestForTS " );
		tsel.logExecution("Started Test Case : " + testCaseId 
				+ "--" + (String)zero.get("Test_Case") );
		tsel.logExecution("The Session Id is  : " + selGrid.sessionId + testCaseId 
				+ "--" + (String)zero.get("Test_Case") );
		selGrid.downloadDir = Utilities.getTCDownloadDir(companyId, testCaseId, testCaseResultsId);
		selGrid.ssonerror = ssonerror;
		boolean multiRun= false;
		int runonceIndex = 0;
		results = emailData;
		int mrIndex = 1;
		do {
			results = selGrid.test(testcase, msc2, companyId, randomKey, results,runonceIndex, envUrl,
				testCaseResultsId, testCaseDetails, multiRun, mrIndex, tester, synchScenario, 
				synchResultid, synchtestcase, ssForAllElement, analyseFailures, false, false);
		
			multiRun = (boolean)results.get("multirun");
			runonceIndex = runonceIndex + 1;
			mrIndex = (int)results.get("mrIndex");
			tester.updateDependantTCResult(testCaseId, (String)results.get("status"), rerunFailedTests,
					testCaseDetails);
		}while(multiRun);
		tsel.logExecution("Test Case Ended : " + testCaseId 
		+ "--" + (String)zero.get("Test_Case") + "--" + (String)results.get("status"));
		
		if(((String)results.get("status")).equals("PASS")) {
			tcel.logExecution("Test Case Passed");
			if(tester.firstRun) {
				tcel.logExecution("First Run");
				tester.updatePassCount();
				tester.updateTestSuiteStatus(tsrid,"PASS");
			} else {
				tcel.logExecution("Second Run - increment Pass count & decrement fail count");
				tester.updatePassCount();
			}
		}
		
		if(indEmail) {
			JSONObject sendMail = new JSONObject();
			sendMail.put("testcase", (String)zero.get("Test_Case"));
			sendMail.put("status", (String)results.get("status"));
			sendMail.put("duration", (double)results.get("duration"));
			sendMail.put("excuteddate", (String)results.get("excuteddate"));
			sendMail.put("browser", (String)results.get("browser"));
			sendMail.put("envurl", (String)results.get("envurl"));
			sendMail.put("width", (int)results.get("width"));
			sendMail.put("height", (int)results.get("height"));
			sendMail.put("logfile", (String)results.get("logfile"));
			sendMail.put("template", (String)emailData.get("template"));
			sendMail.put("subject", (String)emailData.get("subject"));
			sendMail.put("recipient", (String)emailData.get("recipient"));
			sendMail.put("name", (String)emailData.get("name"));
			
			String tcResults = Utilities.getTCResultsURL(companyId, testCaseId, testCaseResultsId);
			sendMail.put("resultsURL", tcResults);
			
			eqh.addToQueue(sendMail);
		}
		
		
			if(((String)results.get("status")).equals("FAIL")) {
				if(rerunFailedTests && firstRun && !synchScenario) {
					if(tester.firstRun) {
						int tcid = (int)dependantTCArr.get(dependantTCArr.size()-1);
						zero.put("idtest_case",tcid);
						JSONObject depjson = msc2.getTestCaseDetailsFromId(tcid);
						depjson.put("tsrid", tsrid);
						testcases.put(0,depjson);
						tester.addToFailedTCList(json, false);		
					}
				} else {			
					if(stopAfterFailure) {
						tester.continueExecution = false;
//						msc2.updateCount(tsrid, "FAIL", "FAIL", dur);
						return results;
					}				
				}
				tester.updateFailCount();
				tester.updateTestSuiteStatus(tsrid, "FAIL");
				if(tester.firstRun) {
//					msc2.updateCount(tsrid, "FAIL", "RUNNING", dur);
				} else {
//					msc2.updateDur(tsrid, dur);
				}
				tsel.logExecution("Tearing Down Session - test case failed : " + testCaseId + " - " + (String)zero.get("Test_Case"));
//				selGrid.tearDown();
			} else if(((String)results.get("status")).equals("STOPP")) {
				tester.updateFailCount();
				tester.updateTestSuiteStatus(tsrid, "STOPP");
				tsel.logExecution("Tearing Down Session - Suite has been forcefully stopped : " + testCaseId + " - " + (String)zero.get("Test_Case"));
//				selGrid.tearDown();
				tester.continueExecution = false;
				if(tester.firstRun) {
//					msc2.updateCount(tsrid, "FAIL", "STOPPED", dur);
				}else {
//					msc2.updateDur(tsrid, dur);
				}
				return results;
			} else if(((String)results.get("status")).equals("ABEND")) {
				tester.updateFailCount();
				tester.updateTestSuiteStatus(tsrid, "ABEND");
				if(tester.firstRun) {
//					msc2.updateCount(tsrid, "FAIL", "RUNNING", dur);
				} else {
//					msc2.updateDur(tsrid, dur);
				}
				try {
					tsel.logExecution("Tearing Down Session - due to abnormal end : " + testCaseId + " - " + (String)zero.get("Test_Case"));
//					selGrid.tearDown();
				}catch (Exception e) {
					//expect to fail
				}
			} else if(((String)results.get("status")).equals("TIMEOUT")) {
				tester.updateFailCount();
				tester.updateTestSuiteStatus(tsrid, "TIMEOUT");
				if(tester.firstRun) {
//					msc2.updateCount(tsrid, "FAIL", "RUNNING", dur);
				} else {
//					msc2.updateDur(tsrid, dur);
				}
				try {
					tsel.logExecution("Tearing Down Session - due to timeout");
//					selGrid.tearDown();
				}catch (Exception e) {
					//expect to fail
				}	
			}
			
			if(results.get("status") != null && (((String)results.get("status")).equals("PASS"))) {
				tsel.logExecution("Exiting method : " + testCaseId 
						+ "--" + (String)zero.get("Test_Case") + "--" + (String)results.get("status"));
			} else {
				tsel.logExecution("Exiting method  : " + testCaseId 
					+ "--" + (String)zero.get("Test_Case") + "--" + (String)results.get("status"));
			}
			if(!tester.firstRun) {
				msc2.updateRerunInTestCaseResultsToDB(testCaseResultsId);
			}
			long endTime = System.currentTimeMillis();
			double dur = (endTime - startTime) / 1000.0;
			if(testCaseResultsId != -1) {
				msc2.updateDuration(testCaseResultsId, dur);
			}
		return results;
	}
}
