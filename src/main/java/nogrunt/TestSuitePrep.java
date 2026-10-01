package nogrunt;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import jakarta.servlet.http.HttpServletRequest;

public class TestSuitePrep {
	
	public void tsPrep(MySQlConn msc, int tsid, String envUrl, String criteria,
			int tsrid, int cmpnyid, Utilities userCache, boolean scheduled,
			HttpServletRequest request, int iterations, String randomkey, JSONObject varJSON) {

		JSONObject testSuite = msc.getTestSuite(tsid, 0);
		JSONArray testcases = (JSONArray)testSuite.get("list");
		JSONArray newTestcases = new JSONArray(); 
		
		for (int i = 0; i < testcases.size(); i++) {
		    JSONObject json = (JSONObject) testcases.get(i);
		    newTestcases.add(json);

		    if (json.containsKey("testsuite") && json.get("testsuite").equals("true")) {
		        JSONObject testsuite = (JSONObject) json.get("testcases");
				JSONObject zero = (JSONObject)testsuite.get(0);
		        JSONArray list = (JSONArray) zero.get("list");

		        for (int j = 0; j < list.size(); j++) {
		            newTestcases.add(list.get(j));
		        }
		    }
		}
		
//		JSONArray finalTestcases = processDependants(msc, newTestcases);
		JSONArray finalTestcases = newTestcases;
		
		String spe= "false";
		boolean stopAfterFailure = false;
		boolean rerunFailedTests = false;
		boolean rerunInParallel = false;
		boolean synchScenarios = false;
		boolean ssonerror = false;
		boolean indEmail = true;
		boolean headless = false;
		String browser = "chrome";
		
		if(criteria != null && criteria.equals("FAIL") && request != null) {
			spe = request.getParameter("RunsTestsinParallel");
			
			if(request.getParameter("StopTestAfterFailure") != null) {
				stopAfterFailure = (request.getParameter("StopTestAfterFailure")).equals("true")?true:false;
			}
			
			if(request.getParameter("RerunFailTest") != null) {
				rerunFailedTests = (request.getParameter("RerunFailTest")).equals("true")?true:false;
			}
			
			if(request.getParameter("ssonerror") != null) {
				ssonerror = (request.getParameter("ssonerror")).equals("true")?true:false;
			}
			
			if(request.getParameter("indEmail") != null) {
				indEmail = (request.getParameter("indEmail")).equals("true")?true:false;
			}
			
			if(request.getParameter("rerun_in_parallel") != null) {
				rerunInParallel = (request.getParameter("rerun_in_parallel")).equals("true")?true:false;
			}
			
			browser = request.getParameter("browsername");
			browser = browser.trim();

		} else {

			spe = (String)testSuite.get("supports_parallel_execution");

			if((String)testSuite.get("stopafterfailure") != null) {
				stopAfterFailure = ((String)testSuite.get("stopafterfailure")).equals("true")?true:false;
			}
			
			if((String)testSuite.get("rerunfailedtests") != null) {
				rerunFailedTests = ((String)testSuite.get("rerunfailedtests")).equals("true")?true:false;
			}
			
			if((String)testSuite.get("rerun_in_parallel") != null) {
				rerunInParallel = ((String)testSuite.get("rerun_in_parallel")).equals("true")?true:false;
			}
			
			synchScenarios = (boolean)testSuite.get("synchedscenarios");
			ssonerror = (boolean)testSuite.get("ssonerror");
			indEmail = (boolean)testSuite.get("indEmail");
			headless = (boolean)testSuite.get("headless");
	
			browser = (String)testSuite.get("browser");
			if(envUrl == null || envUrl.trim().equals("")) {
				envUrl = (String)testSuite.get("envurl");
			}
		
		}
		
		if((int)testSuite.get("idtest_suite") == tsid) {
			Tester tester = new Tester();
			if(criteria.equals(null) || criteria.equals("undefined") ||
					criteria.equals("")) {
//				tester.setTestSuite(testcases);
				tester.setTestSuite(finalTestcases);
			} else if(criteria.equals("STARTED")) {
				Set<Integer> incompleteTc = getIncompleteTests(msc, tsrid, cmpnyid, criteria,testcases,
						rerunFailedTests);
//				tester.setTestSuite(incompleteTc);
				tester.setTestSuite(finalTestcases);
				tester.setExecuteCompleteTS(false);
				tester.setFailedTC(incompleteTc);
			} else {
				Set<Integer> failedTC = getFailedTestCases(tsrid, cmpnyid, criteria, msc,rerunFailedTests);
				JSONArray toBeRemoved = new JSONArray();
				for(int i=0;i<finalTestcases.size();i++) {
					JSONObject json = (JSONObject) finalTestcases.get(i);
                    JSONObject tcs = (JSONObject) json.get("testcases");
                    JSONObject tcs1 = (JSONObject) tcs.get(0);
                    int tcid = (int) tcs1.get("idtest_case");
                    if(!failedTC.contains(tcid)) {
                    	toBeRemoved.add(json);
                    }
				}
				for(int i=0;i<toBeRemoved.size();i++) {
					finalTestcases.remove(toBeRemoved.get(i));
				}
				tester.setTestSuite(finalTestcases);
				if(!criteria.equals("ALL") && !criteria.equals(null)) {
					tester.setExecuteCompleteTS(false);
					tester.setFailedTC(failedTC);
				}
				if(criteria.equals("FAIL")) {
					tester.setMode("analysis");
				}
			}
			if((int) testSuite.get("analysis") == 1) {
				tester.setMode("analysis");
			}
			tester.setCompanyId(cmpnyid);
			tester.setMSC(msc);
			tester.setTestSuiteId(tsid);
			tester.setSPE(spe, userCache);
			tester.setSynchScenarios(synchScenarios);
			tester.setSSOnError(ssonerror);
			tester.setStopAfterFailure(stopAfterFailure);
			tester.setIndEmail(indEmail);
			tester.setRerunFailedTests(rerunFailedTests);
			tester.setRerunInParallel(rerunInParallel);
			tester.setRunOnce("true");
			tester.setTestSuiteName((String)testSuite.get("Test_Suite"));
			tester.setEnvUrl(envUrl);
			tester.setBrowser(browser);
			tester.setScheduled(scheduled);
			tester.setRandomKey(randomkey);
			tester.setHeadless(headless);
			tester.setIterations(iterations);
			tester.setVariablesJSON(varJSON);
			String[] emailAddresses;
			String resultsemailstr = (String)testSuite.get("resultsemail");
			String buildtag = (String)testSuite.get("buildtag");
            if (resultsemailstr == null || resultsemailstr.trim().isEmpty()) {
                emailAddresses = new String[]{""}; // Array with a single blank element
            } else {
                String[] splitEmails = resultsemailstr.split(",\\s*");
                emailAddresses = new String[splitEmails.length + 1];
                System.arraycopy(splitEmails, 0, emailAddresses, 0, splitEmails.length);
            }
			tester.setRecipients(emailAddresses);
			tester.setBuildTag(buildtag);
			JSONObject emailData = new JSONObject();
			JSONObject userDets = msc.getUserDetails();
			emailData.put("name", (String)userDets.get("uname"));
			emailData.put("recipient", (String)userDets.get("uname"));
			emailData.put("subject", AppProperties.testcaseexecutionsubject);
			emailData.put("template", "TestCaseExecution.html");
			EmailQueuingHelper eqh = userCache.eqh;
			tester.setResultsForEmail(emailData, eqh);
			tester.setCriteria(criteria);
			tester.start();
		}		
	}
	
	public JSONArray processDependants(MySQlConn msc, JSONArray newTestcases) {
		
		JSONArray finalTestcases = new JSONArray();
		for (int i = 0; i < newTestcases.size(); i++) {
		    JSONObject json = (JSONObject) newTestcases.get(i);
		    if (!json.containsKey("testsuite")) {
		    	JSONObject tc = (JSONObject)json.get("testcases");
				JSONObject zero = (JSONObject)tc.get(0);
				int tcid = (int)zero.get("idtest_case");
//				JSONArray dependantTCArr = msc.getTestStepsForTCIDAndAncestors(tcid,new JSONArray());
				JSONArray dependantTCArr = null;

				if(dependantTCArr != null && dependantTCArr.size() > 1) {
					for(int depi=0;depi<dependantTCArr.size() - 1;depi++) {
						int deptcid = (int)dependantTCArr.get(depi);
						JSONObject depjson = msc.getTestCaseDetailsFromId(deptcid);
						if(zero.get("tsrid") != null) {
							depjson.put("tsrid", zero.get("tsrid"));
						}
						
						depjson.put("master", "yes");
						depjson.put("masterOf", tcid);
						
						JSONObject indexjson = new JSONObject();
						indexjson.put(0,depjson);
						
						JSONObject JSONData = new JSONObject();
						JSONData.put("testcases", indexjson);
						

						JSONData.put("TestcaseIndex", 0);
						JSONData.put("totalPages", 1);
						finalTestcases.add(JSONData);
					}
				}
				finalTestcases.add(json);
		    }
		}
		return finalTestcases;
	}
	
	public Set getFailedTestCases(int tsrid, int cmpnyid, String criteria, MySQlConn msc, boolean rerunFailedTests) {
		JSONObject tsr = msc.getTestCaseResultFromTestSuiteResult(tsrid, cmpnyid, criteria);
		JSONArray tcResults = (JSONArray)tsr.get("testcaseresult");
		Set<Integer> dupTC = new HashSet();
		
		for(int i=tcResults.size() - 1; i>= 0;i--) {
			JSONObject tcResult = (JSONObject)tcResults.get(i);
			int testcaseid = (int)tcResult.get("Test_Case_Id");	
			String status = (String)tcResult.get("Status");
			
			if(status != null && !status.equals("PASS")) {
				dupTC.add(testcaseid);
			} else if(status != null && status.equals("PASS")) {
				dupTC.remove(testcaseid);
			}
		}
		
		return processDependantTC(dupTC, msc);
		
	}
	
	private Set getIncompleteTestCases(int tsrid, int cmpnyid, String criteria, MySQlConn msc, boolean rerunFailedTests) {
		JSONObject tsr = msc.getTestCaseResultFromTestSuiteResult(tsrid, cmpnyid, criteria);
		JSONArray tcResults = (JSONArray)tsr.get("testcaseresult");
		Set<Integer> dupTC = new HashSet();
		
		for(int i=0; i<tcResults.size();i++) {
			JSONObject tcResult = (JSONObject)tcResults.get(i);
			int testcaseid = (int)tcResult.get("Test_Case_Id");	
			dupTC.add(testcaseid);
		}
		
		return dupTC;
		
	}
	
	public Set processDependantTC(Set dupTC, MySQlConn msc) {
		Iterator iter = dupTC.iterator();
		Set<Integer> finalTC = new HashSet();
		
		while(iter.hasNext()) {
			int tcid = (int)iter.next();
			
			JSONArray jsonArr = new JSONArray();
//			JSONArray dependantTCArr = msc.getTestStepsForTCIDAndAncestors(tcid,jsonArr);
			JSONArray dependantTCArr = null;
			if(dependantTCArr != null && dependantTCArr.size() > 0) {
				for(int j=0;j<dependantTCArr.size();j++) {
					int dtc = (int)dependantTCArr.get(j);
					finalTC.add(dtc);
				}
			}
			finalTC.add(tcid);
		}
		
		return finalTC;
	}
	
	private Set<Integer> getIncompleteTests(MySQlConn msc, int tsrid, int companyid, String criteria,
			JSONArray testcases, boolean rerunFailedTests) {
		Set<Integer> allExecutedTC = getIncompleteTestCases(tsrid, companyid, criteria, msc,rerunFailedTests);
		
		Set<Integer> incompleteTC = new HashSet();
		
		JSONArray incompleteTc = new JSONArray();
		for(int i=0; i<testcases.size();i++) {
			JSONObject json = (JSONObject) testcases.get(i);
			JSONObject testsuite = (JSONObject)json.get("testcases");
			JSONObject zero = (JSONObject)testsuite.get(0);
			int testcaseid = (int)zero.get("idtest_case");	
			if(!allExecutedTC.contains(testcaseid)) {
				incompleteTC.add(testcaseid);
			}
		}
		
		Set<Integer> newIncompleteTC = new HashSet();
		Iterator iter = incompleteTC.iterator();
		while(iter.hasNext()) {
			int testcaseid = (int)iter.next();	
			JSONArray jsonArr = new JSONArray();
//			JSONArray dependantTCArr = msc.getTestStepsForTCIDAndAncestors(testcaseid, jsonArr);
			JSONArray dependantTCArr = new JSONArray();
			for(int j=0;j<dependantTCArr.size();j++) {
				int deptcid = (int)dependantTCArr.get(j);
				newIncompleteTC.add(deptcid);
			}
			newIncompleteTC.add(testcaseid);
		}
		
		
	//	tester.setTestSuite(incompleteTc);
		return newIncompleteTC;
	}
	
	public int getFailedTCCount(int tsrid, int cmpnyid, String criteria, MySQlConn msc, boolean rerunFailedTests) {
		Set result = getFailedTestCases(tsrid, cmpnyid, criteria, msc, rerunFailedTests);
		return result.size();
	}

}
