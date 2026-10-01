package nogrunt;

import nogrunt.springboot.Controllers.service.AutomationService;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.json.simple.JSONObject;
import org.json.simple.JSONArray;
import org.openqa.selenium.remote.DesiredCapabilities;

import java.net.URL;

import org.openqa.selenium.remote.RemoteWebDriver;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import nogrunt.integrations.ADOIntegration;
import nogrunt.integrations.JiraIntegration;
import nogrunt.dbaccess.*;

import org.openqa.selenium.Platform;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.CountDownLatch;
import java.util.*;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.io.IOException;
import java.nio.file.Files;
import java.io.File;

import java.text.SimpleDateFormat;
import java.sql.Timestamp;

public class Tester extends Thread {

    int testCaseId = -1;
    int productId = -1;
    int testCaseResultsId = -1;
    String testCaseName = "";
    int testSuiteId = -1;
    String testSuiteName = "";
    int companyId = -1;
    String testCase = "";
    MySQlConn msc;
    JSONArray testSuite = null;
    JSONObject urlData = null;
    String browser = "Chrome";
    String proxyurl = null;
    String randomKey = "";
    JSONObject forEmail = null;
    EmailQueuingHelper eqh = null;
    boolean runonce = true;
    boolean supportsParallelExecution = false;
    boolean rerunInParallel = false;
    boolean stopAfterFailure = true;
    boolean indEmail = true;
    boolean rerunFailedTests = false;
    boolean crxTest = false;
    boolean headless = false;
    int stepnum = -1;
    int tillTC = -1;
    int nextnum = -1;
    String envUrl = null;
    boolean executeCompleteTS = true;
    Set dupTC = null;
    Set failedTC = null;
    ExecutionLogger el1 = null;
    SelGrid sg = new SelGrid();
    boolean continueExecution = true;
    String tsStatus = "PASS";
    AtomicInteger passCount = new AtomicInteger(0);
    AtomicInteger failCount = new AtomicInteger(0);
    int parallelThreads = 1;
    String workItem = null;
    HashMap intUserProps = new HashMap();
    boolean scheduled = false;
    JSONArray allIntDets = null;
    boolean synchScenario = false;
    boolean ssonerror = false;
    boolean ssForAllElement = false;
    GridManager gm = new GridManager();
    String mode = "";
    int analysisTC = -1;
    int iterations = 1;
    ConcurrentHashMap<String, String> varMap = new ConcurrentHashMap<>();
    String[] recipients = null;
    String buildtag = "";
    JSONArray allCases = new JSONArray();
    HashMap tsintsindex = new HashMap();
    HashMap tsSize = new HashMap();
    HashMap testSuiteStatus = new HashMap();
    JSONArray failedTestCases = new JSONArray();
    JSONArray depandantTCResults = new JSONArray();
    boolean firstRun = true;
    boolean completedRunningFailedTests = false;
    long tsStartTime = System.currentTimeMillis();
    long tsduration = 0;
    int tsrid = -1;
    ExecutionLogger tsel = null;
    JSONObject results = new JSONObject();
    JSONObject suiteForEmail = new JSONObject();
    boolean analysisRun = false;
    boolean baselineRun = false;
    String criteria = null;
    JSONObject variableJSON;
    boolean debugMode = false;

    public static void main(String[] args) throws Exception {

		Tester tester = new Tester();
		AppProperties.getProperties();
		SelGrid sg = new SelGrid();
		int companyIds = -1;
		int tcIds = -1;
	  }
	
	public void scrapper() {
		
	}
	
	public void run() {
		ExecutionLogger el = null;
		try {
			forEmail.put("runonce", runonce);			
			sg.setGM(gm);
			
			if (testCaseId != -1) {
				el =sortEL(sg,companyId,testCaseId,testCaseResultsId);
				el.logExecution("Running from runGridTest " );
				el.logExecution("Test Case Id = " +  testCaseId);
				runGridTest(msc, testCaseId, el);
			} else if(!testCase.equals("")) {
				el =sortEL(sg,companyId,testCaseId,testCaseResultsId);
				el.logExecution("Running from runTest " );
				el.logExecution("Test Case Name = " +  testCase);
				runTest(msc, testCase, el);			
			} else if(testSuite != null) {
				runGridTestForTS(msc, testSuite, false, browser,proxyurl,
						scheduled, true);				
			} 
		}catch (Exception e) {
			e.printStackTrace();
		} finally {
			if(!scheduled) {
				msc.closeDbConn();
			}
			sg.tearDown();
			if(el != null) {
				el.close();
			}
		}
		
	}
	
	public void addToFailedTCList(JSONObject tc, boolean isMaster) {
		if(!isMaster) {
			failedTestCases.add(tc);
		}
	}
	
	public void updateDependantTCResult(int tcid, String status, boolean rerunFailedTests,
			JSONObject testCaseDetails) {
		if(rerunFailedTests) {
			if(testCaseDetails.get("master") != null || testCaseDetails.get("dependantTC") != null) {
				if(this.firstRun) {
					if(status != null && status.equalsIgnoreCase("FAIL")) {					
						depandantTCResults.add(tcid);
					}					
				} 
			}			
		}
	}
	
	public boolean isDependantTC(JSONObject testCaseDetails) {
		if(testCaseDetails.get("master") != null || testCaseDetails.get("dependantTC") != null) {
			return true;
		}
		
		return false;
	}
	
	public boolean hasFailedinRun1_depandantTC(int tcid, String status) {
		
		for (int i=0;i<depandantTCResults.size();i++) {
			if(depandantTCResults.get(i) instanceof Integer) {
				int deptcid = (int)depandantTCResults.get(i);
				if(deptcid == tcid) {
					String res = tcid + "~" + status;
					depandantTCResults.add(i,res);
					return true;
				}
			}
		}
		return false;
	}
	
	public void runTest(MySQlConn msc, String title, ExecutionLogger el) {
		
		JSONObject testCaseDetails = msc.getTestCaseDetailsFromId(testCaseId);
		boolean sesSuccess = browserSetup(sg, browser, proxyurl, el, title,
				(boolean)testCaseDetails.get("enableaudio"), 
				(boolean)testCaseDetails.get("enablevideo"), null, testCaseId,
				(boolean)testCaseDetails.get("stealthMode"));
		if(!sesSuccess) {
			msc.updateTestCaseStatusToDB(testCaseResultsId, "ABORTED", 0, "ABORTED", companyId);
			el.logExecution("Test Aborted");
		} else {
			sg.downloadDir = Utilities.getTCDownloadDir(companyId,testCaseId,testCaseResultsId);
			
			JSONObject json = msc.getTestStepsFromDB(title, true);		
			
			int runonceIndex = 0;
			boolean multiRun = false;
			int mrIndex = 1;
			do {
				forEmail = sg.test(json, msc, companyId, randomKey, forEmail,runonceIndex,envUrl,
						testCaseResultsId, testCaseDetails, multiRun, mrIndex, this, false,-1,
						null, ssForAllElement, analysisRun, baselineRun, false);
				runonce = (boolean)forEmail.get("runonce");
				runonceIndex = runonceIndex + 1;			
				multiRun = (boolean)forEmail.get("multirun");
				mrIndex = (int)forEmail.get("mrIndex");			
			} while (!runonce && !multiRun);
			eqh.addToQueue(forEmail);
		}
		sg.tearDown();
		msc.closeDbConn();
		el.logExecution("returning from runTest " );
	}
	
	public ExecutionLogger sortEL(SelGrid sg, int companyId1,int testCaseId1, int testCaseResultsId1) {
		Utilities.createTestCaseResultsFolder(companyId1,testCaseId1,testCaseResultsId1);
		String logFilePath = Utilities.getLogDirFile(companyId1, testCaseId1, testCaseResultsId1);
		ExecutionLogger el = new ExecutionLogger(Integer.toString(testCaseResultsId1), logFilePath);
		sg.setExecutionLogger(el);
		
		String analysislogFilePath = Utilities.getAnalysisLogFile(companyId1, testCaseId1, testCaseResultsId1);
		ExecutionLogger al = new ExecutionLogger("Analysis_" + Integer.toString(testCaseResultsId1), analysislogFilePath);
		sg.setAnalysisLogger(al);
		
		sg.downloadDir = Utilities.getTCDownloadDir(companyId, testCaseId, testCaseResultsId);
		el.setMsc(msc);
		el.setTcrid(testCaseResultsId1);
		el.setTcid(testCaseId1);
		el.logExecution("companyId =  " + companyId );
		el.logExecution(" testCaseId = " +  testCaseId1 );
		el.logExecution(" testCaseResultsId = " + testCaseResultsId1);		
		el.logExecution("run once " + runonce);
		el.logExecution("Supports Parallel Execution " + supportsParallelExecution);
		el.logExecution("Stop After Failure " + stopAfterFailure);
		el.logExecution("Rerun Failed Tests " + rerunFailedTests);		
		return el;
	}
	
	public boolean browserSetup(SelGrid selGrid, String bwsr, String proxyurl,
			ExecutionLogger el, String tcname, boolean enableaudio, boolean enablevideo,
			String profile, int tcid, boolean stealthMode) {
		
		boolean result = false;
		for (int i=0;i<3;i++) {
			el.logExecution("Setting up the Browser and execution session for test case : " + tcname + " - " + tcid);
			el.logExecution("Is Audio/video required for this test case : " + tcname + " - " + tcid 
					+ " - enableaudio :" + enableaudio);
			
			JSONObject grid = gm.getMaxAndSessionInfo();
			long sessionCount = 0;
			long maxSessions = 0;
			
			if(grid != null && grid.size() > 0) {
				sessionCount = (long)grid.get("sessionCount");
				maxSessions = (long)grid.get("maxSession");
				el.logExecution("Session Count = " + sessionCount + " and max sessions allowed = " + maxSessions +
						" - " + tcname + " - " + tcid);
			} else {
				el.logExecution("Could not get session data from grid, retry in 5 seconds- attempt : " + i 
						+ " - " + tcname + " - " + tcid);
				try {
					Thread.sleep(5000);
				} catch (Exception e) {}
				continue;
			}
	        
	        if(sessionCount >= maxSessions) {
	        	el.logExecution("No Capacity available in the infrastructure, retry in 5 seconds - attempt : " + i
	        			+ tcname + " - " + tcid);
	        	try {
					Thread.sleep(5000);
				} catch (Exception e) {}
	        	continue;
	        } else {
	        	el.logExecution("Adequate capacity is available in the infrastructure " + tcname + " - " + tcid);
	        }
	        
	        int executionSize = Utilities.getExecutionCount(companyId);
	        if(executionSize >= parallelThreads) {
	        	el.logExecution("Number of active threads for your company :" + executionSize);
	        	el.logExecution("Number of threads subscribed for  :" + parallelThreads);
	        	el.logExecution("All subscribed threads are currently busy - so aborting - "
	        			+ "please retry after threads become available" );
	        	return false;
	        } else {
	        	el.logExecution("Adequate capacity is available in license too " + tcname + " - " + tcid);
	        }
	        
			if(crxTest) {
				selGrid.chromeGridSetupWithCRX();
				if(mode != null && !mode.equals("analysis")) {				
					selGrid.setTillStepNum(stepnum);
					selGrid.setTillTC(tillTC);
				}
				
			} else {
				int port = -1;
				if(AppProperties.apicapture.equalsIgnoreCase("true")) {
					port = PortManager.assignPort(
							(int)maxSessions, el);
					selGrid.port = port;
				}
				el.logExecution("Port being allocated is :" + port);
				if(bwsr == null || bwsr.equals("") || bwsr.equalsIgnoreCase("Chrome") ||
						bwsr.equalsIgnoreCase("All") || bwsr.equalsIgnoreCase("Any") || bwsr.equalsIgnoreCase("null")) {
					org.openqa.selenium.WebDriver wd = selGrid.chromeGridSetup(headless, proxyurl, enableaudio, enablevideo,
							profile,port,companyId,stealthMode);
					selGrid.setDriver(wd);
					el.logExecution("driver id is : " + ((RemoteWebDriver) wd).getSessionId().toString()
							+ " - "+ tcname + " - " + tcid);
				} else if (bwsr.equalsIgnoreCase("Edge")) {
					selGrid.edgeGridSetup(headless, proxyurl, enableaudio, 
							enablevideo,port,companyId,stealthMode);
				} else if (bwsr.equalsIgnoreCase("Firefox")) {
					selGrid.firefoxGridSetup(headless, proxyurl, enableaudio, 
							enablevideo,port,companyId,stealthMode);
				} else if (bwsr.equalsIgnoreCase("Safari")) {
					selGrid.safariGridSetup(headless, proxyurl,enableaudio, 
							enablevideo,port,companyId,stealthMode);
				}
			}
			
			Utilities.addExecution(companyId, selGrid.sessionId);
			
			el.logExecution("Completed the set up of Browser for test case - " + tcname  + " - " + tcid);
			
			selGrid.setVarMap(varMap);
			el.logExecution("set the varmap : " + tcid 
					+ "--" + tcname );
			result = true;
			break;
		}
		
		return result;
	}
	
	public boolean newBrowserForAV(JSONObject testCaseDetails, 
			boolean prevAudio, boolean prevVideo) {

			boolean newAudio = false;
			boolean newVideo = false;
			boolean newBrowser = false;
			if((prevAudio & !(boolean)testCaseDetails.get("enableaudio")) || 
					(!prevAudio & (boolean)testCaseDetails.get("enableaudio"))) {
				newAudio = true;
			}
			
			if((prevVideo & !(boolean)testCaseDetails.get("enablevideo")) || 
					(!prevVideo & (boolean)testCaseDetails.get("enablevideo"))) {
				newVideo = true;
			}
			
			if(newAudio || newVideo) {
				newBrowser = true;
				sg.tearDown();
			} else {
				newBrowser = false;
			}
			return newBrowser;
	}
	
	public void runGridTest(MySQlConn msc, int testCaseId, ExecutionLogger el) {
	
		
		JSONArray jsonArr = new JSONArray();
		JSONArray dependantTCArr = msc.getTestStepsForTCIDAndAncestors(testCaseId,jsonArr);
		int tcrid = -1;
		JSONObject testCaseDetails = null;
		boolean predecessor = true;
		String status = "";
		
		for(int i=0;i<dependantTCArr.size();i++) {
			if(i == dependantTCArr.size() - 1) {
				predecessor = false;
			}
			int tcid = (int)dependantTCArr.get(i);

			if(dependantTCArr.size() ==  1) {
				tcrid = testCaseResultsId;
			}
			JSONObject json = msc.getTestStepsFromDBByTestCaseId(tcid, true, false);
			
			JSONObject tc = (JSONObject)json.get("testcase");
			if(envUrl == null || envUrl.equals("")) {				
				envUrl = (String)tc.get("envurl");
			}
			
			int runonceIndex = 0;
			boolean freshSession = false;
			boolean multiRun = false;
			int mrIndex = 1;
			boolean starttc = true;
			boolean prevAudio = false;
			boolean prevVideo = false;
			boolean newBrowser = true;
			
			if(i > 0) {
				starttc = false;
				newBrowser = false;
			}
	
			do {
				testCaseDetails = msc.getTestCaseDetailsFromId(tcid);
				
				if(!starttc) {
					newBrowser = newBrowserForAV(testCaseDetails, prevAudio, prevVideo);
				}
				
				if((starttc || newBrowser) && 
						((boolean)tc.get("continuetest") && sg.getDriver() != null)) {
					starttc = false;
					newBrowser = false;
				}
				
				if((boolean)tc.get("forcenewsession")) {
					el.logExecution("Force new session is on - so killing the previous session");
					sg.tearDown();
				}
				
				if (starttc || newBrowser || (boolean)tc.get("forcenewsession")){
				
					boolean sesSuccess = browserSetup(sg, browser, proxyurl, el, 
							String.valueOf(testCaseId), (boolean)testCaseDetails.get("enableaudio"),
							(boolean)testCaseDetails.get("enablevideo"), null, testCaseId,
							(boolean)testCaseDetails.get("stealthMode"));
					if(!sesSuccess) {
						msc.updateTestCaseStatusToDB(testCaseResultsId, "ABORTED", 0, "ABORTED", companyId);
						el.logExecution("Test Aborted");
						return;
					}
					sg.downloadDir = Utilities.getTCDownloadDir(companyId, testCaseId, testCaseResultsId);
					if(crxTest) {
						sendRequestForMerge();
					}
					starttc = false;
					prevAudio = (boolean)testCaseDetails.get("enableaudio");
					prevVideo = (boolean)testCaseDetails.get("enablevideo");
				}
				sg.debugMode = debugMode;
				forEmail = sg.test(json, msc, companyId, randomKey, forEmail,runonceIndex,envUrl,
						tcrid,testCaseDetails, multiRun, mrIndex, this,false,-1,null, 
						ssForAllElement, analysisRun,baselineRun, predecessor);
				runonce = (boolean)forEmail.get("runonce");
				status = (String)forEmail.get("status");
				if(!runonce && status.equals("FAIL")) {
					sg.tearDown();
					browserSetup(sg, browser, proxyurl, el, testCase,
							(boolean)testCaseDetails.get("enableaudio"),
							(boolean)testCaseDetails.get("enablevideo"), null,
							testCaseId,(boolean)testCaseDetails.get("stealthMode"));
					sg.downloadDir = Utilities.getTCDownloadDir(companyId, testCaseId, testCaseResultsId);
				}
				runonceIndex = runonceIndex + 1;
				multiRun = (boolean)forEmail.get("multirun");
				mrIndex = (int)forEmail.get("mrIndex");
			} while (!runonce || multiRun);
			String tcResults = Utilities.getTCResultsURL(companyId, tcid, tcrid);
			forEmail.put("resultsURL", tcResults);
			eqh.addToQueue(forEmail);
			status = (String)forEmail.get("status");
		}
		if(dependantTCArr != null && dependantTCArr.size() > 1) {
			String oldLogFile = Utilities.getLogDirFile(companyId, testCaseId, testCaseResultsId);
			int latesttcrid = (int)forEmail.get("testCaseResultsId");
			String newLogFile = Utilities.getLogDirFile(companyId, testCaseId, latesttcrid);
			Utilities.copyFile(oldLogFile, newLogFile);
			msc.deleteTestCaseResultByID(testCaseResultsId, latesttcrid);
			el.setTcrid(latesttcrid);
		}
		if(!crxTest) {
			sg.tearDown();
			int ptcrid = msc.getSecondLatestPassedTestCaseResultsId(testCaseId);
			if(dependantTCArr.size() > 1) {
				ptcrid -= dependantTCArr.size();
			}
			if(status == "PASS") {
				String folder = AppProperties.datadirectory + "\\" + companyId + "\\" + testCaseId + "\\" + ptcrid + "\\" + "pagesource";
				try {
					Utilities.deleteFolderContents(folder);
				} catch(Exception e) {
					
				}
			}
		}
		if(mode != null && mode.equalsIgnoreCase("analysis")) {
			sg.tearDown();
			
			Utilities.createTestCaseResultsFolder(companyId, analysisTC, tcrid);
			
			Path sourceDir = Paths.get(Utilities.getScreenShotsDir(companyId, testCaseId, tcrid));
	        Path destinationDir = Paths.get(Utilities.getScreenShotsDir(companyId, analysisTC, tcrid));
	        
	        try {
	        	Files.delete(destinationDir);
	        	destinationDir = Paths.get(Utilities.getTCRDir(companyId, analysisTC, tcrid));
	            Files.copy(sourceDir, destinationDir.resolve(sourceDir.getFileName()));
	        } catch (IOException e) {
	            System.err.println("Failed to move folder: " + e.getMessage());
	        }
	        
			JSONObject original = msc.getTestStepsByTestCaseID(testCaseId, false);
			original = (JSONObject)original.get("data");
			JSONObject latest = msc.getTestStepsByTestCaseID(analysisTC, false);
			msc.deleteTestCase(analysisTC);
			el.logExecution("Test case - " + analysisTC + " has been deleted");
			latest = (JSONObject)latest.get("data");
			
			//analysetest(original, latest,companyId, testCaseId, testCaseId, tcrid,testCaseDetails, el);
		} else if (baselineRun) {
			String baselineDir = Utilities.getBaselineDir(companyId, testCaseId);
			Utilities.takeBackup_createFolder(baselineDir);
			
			String ssDir = Utilities.getScreenShotsDir(companyId, testCaseId, tcrid);
			Utilities.copyFolder(ssDir, baselineDir);
			msc.updateTestCaseBaselineStatus(testCaseId, 1);
		}
		el.logExecution("returning from runGridTest " );
		msc.closeDbConn();
	}
	
	public void createBaselineForSS(int companyId, int testCaseId, int tcrid) {
		String baselinedirStr = Utilities.getBaselineDir(companyId, testCaseId);
		File baselinedir = new File(baselinedirStr);
		if (!baselinedir.exists()) {
			Utilities.createFolder(baselinedirStr);
			String sourceDirPath = Utilities.getScreenShotsDir(companyId, testCaseId, tcrid);
			Utilities.copyFolder(sourceDirPath, baselinedirStr);            
		}
	}
	
	public void sendRequestForMerge() {
		try {
			JSONObject json = new JSONObject();
			json.put("stepnum", stepnum);
			json.put("nextnum", nextnum);
			json.put("testcaseid", testCaseId);
			json.put("testcasename", testCaseName);
			Utilities.callApi(AppProperties.javaexturl, "mergeTestCase", (String)forEmail.get("name"), json,
					companyId);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void runGridTestForTS(MySQlConn msc, JSONArray testCases, boolean suiteinSuite, 
			String browser1, String proxyurl1, boolean scheduled, boolean root) {
		
		
		JSONObject userDets = msc.getUserDetails();
		msc = new MySQlConn(null);
		msc.setUserDetails(userDets);
		String runonceStr = "false";		
		if(rerunFailedTests) {
			runonceStr = "true";
		}
		
		String parallelRunStr = "false";		
		if(supportsParallelExecution) {
			parallelRunStr = "true";
		}
		
		try {
			if(firstRun) {
                setInitialVarMap(variableJSON, sg);
//				JSONObject results = new JSONObject();		
                results.put("tsduration", tsduration);
                results.put("passCount", passCount.intValue());
                results.put("failCount", failCount.intValue());
                results.put("firstRun", true);
                boolean sins = false;

//				JSONObject suiteForEmail = new JSONObject();
                suiteForEmail.put("excuteddate", Utilities.getNow());
                tsrid = msc.writeTestSuiteStatusToDB(testSuiteId, "STARTED", 0.0,
                        (int) results.get("passCount"), (int) results.get("failCount"), runonceStr,
                        parallelRunStr, scheduled, testCases.size(), criteria, buildtag);
                results.put("tsStatus", getTestSuiteStatus(tsrid));
                tsintsindex.put(testSuiteId, tsrid);
                tsSize.put(testSuiteId, testCases.size());
                Utilities.createTestSuiteResultsFolder(companyId, testSuiteId, tsrid);
                String logFilePath = Utilities.getLogDirFileTS(companyId, testSuiteId, tsrid);
                tsel = new ExecutionLogger(Integer.toString(tsrid), logFilePath);
                tsel.setMsc(msc);

                for (int i = 0; i < testCases.size(); i++) {
                    JSONObject json = (JSONObject) testCases.get(i);
                    if (json.get("testsuite") != null && ((String) json.get("testsuite")).equalsIgnoreCase("true")) {

                    } else {
                        JSONObject tempcases = (JSONObject) json.get("testcases");
                        JSONObject zero = (JSONObject) tempcases.get(0);
                        testCaseId = (int) zero.get("idtest_case");
                        int atsrid = -1;
                        if (json.get("TestSuiteId") == null) {
                            atsrid = tsrid;
                        } else {
                            int tsid = (int) json.get("TestSuiteId");
                            if (tsintsindex.containsKey(tsid)) {
                                atsrid = (int) tsintsindex.get(tsid);
                            } else {
                                atsrid = msc.writeTestSuiteStatusToDB(tsid, "STARTED", 0.0,
                                        (int) results.get("passCount"), (int) results.get("failCount"), runonceStr,
                                        parallelRunStr, scheduled, testCases.size(), criteria, buildtag);
                                tsintsindex.put(tsid, atsrid);
                            }

                            if (tsid != testSuiteId) {
                                if (tsSize.containsKey(tsid)) {
                                    int count = (int) tsSize.get(tsid);
                                    count = count + 1;
                                    tsSize.put(tsid, count);
                                } else {
                                    tsSize.put(tsid, 1);
                                }

                                int tscount = (int) tsSize.get(testSuiteId);
                                tscount = tscount - 1;
                                tsSize.put(testSuiteId, tscount);
                            }
                        }

                        zero.put("tsrid", atsrid);
                        tsel.logExecution("test case id " + testCaseId + " --- Test Case Name " + (String) zero.get("Test_Case"));
                        allCases.add(json);
                    }
                }

                if (!root) {
                    return;
                } else {
                    testCases = allCases;
                }

                Iterator iter1 = tsSize.keySet().iterator();
                while (iter1.hasNext()) {
                    int tsid = (int) iter1.next();
                    int count = (int) tsSize.get(tsid);
                    int atsrid = (int) tsintsindex.get(tsid);
                    msc.updateTotalCount(atsrid, count);
                }

                Iterator iter;
                if (dupTC != null) {
                    tsel.logExecution("--------------------------------------------------------------------------------");
                    tsel.logExecution("Duplicate Test Scenarios that need to be ignored - Start List");
                    iter = dupTC.iterator();
                    while (iter.hasNext()) {
                        tsel.logExecution("Test Case will be ignored --- " + iter.next());
                    }
                    tsel.logExecution("Duplicate Test Scenarios that need to be ignored - End List");
                    tsel.logExecution("--------------------------------------------------------------------------------");
                }

                if (failedTC != null) {
                    tsel.logExecution("--------------------------------------------------------------------------------");
                    tsel.logExecution("Failed Test Scenarios that will be Executed - Start List");
                    iter = failedTC.iterator();
                    while (iter.hasNext()) {
                        tsel.logExecution("Test Case that will be executed --- " + iter.next());
                    }
                    msc.updateTotalCount(tsrid, failedTC.size());
                    tsel.logExecution("Failed Test Scenarios that will be Executed - End List");
                    tsel.logExecution("--------------------------------------------------------------------------------");
                }
                if (iterations > 1) {
                    JSONArray newtc = new JSONArray();
                    for (int j = 0; j < iterations; j++) {
                        newtc.addAll(testCases);
                    }
                    testCases = newtc;
                    msc.updateTotalCount(tsrid, testCases.size());
                }
            }

            if (supportsParallelExecution) {
                parallelExecution(testCases, tsel, forEmail, browser1, msc, dupTC,
                        failedTC, this, companyId, randomKey, envUrl, eqh, rerunFailedTests,
                        stopAfterFailure, tsStartTime, proxyurl1);
            } else if (synchScenario) {
                synchExecution(testCases, tsrid, tsel, forEmail, browser1, msc, dupTC,
                        failedTC, this, companyId, randomKey, envUrl, eqh, rerunFailedTests,
                        stopAfterFailure, tsStartTime, proxyurl1);
            } else {
                sg.tsr = tsrid;
                sg.ssonerror = ssonerror;
                if (!firstRun) {
                    msc.updateTestSuiteStatus(tsrid, "RERUNNING");
                }
                ParallelTestCaseExecutor ptce = new ParallelTestCaseExecutor();
                int n=testCases.size();
                for (int i = 0; i < testCases.size(); i++) {
                    JSONObject json = (JSONObject) testCases.get(i);
                    JSONObject tcs = (JSONObject) json.get("testcases");
                    if(tcs == null) {
                    	tsel.logExecution("Testcases is null");
                    	continue;
                    }
                    JSONObject tcs1 = (JSONObject) tcs.get(0);
                    String caseType = (String) tcs1.get("case_Type");
					int totalCount = testCases.size();
                    
                    Integer testCaseId = (Integer) tcs1.get("idtest_case");
                    try {
                    	if(caseType == null ||
                    			(caseType != null && !(caseType.equals("MOBILE")))) {
                        ptce.executeTestCase(json, tsel, forEmail, sg,
                                browser1, msc, dupTC, failedTC, this, companyId,
                                randomKey, envUrl, eqh, rerunFailedTests,
                                stopAfterFailure, true, tsStartTime, false,
                                ssForAllElement, ssonerror, proxyurl1, indEmail, mode);
                    }else {
                    	String status =callMobileSuitAPI(randomKey, testCaseId, tsrid, totalCount, tsStartTime, tsel, false);
                    }
                    }catch (Exception e) {
                        tsel.logExecution(e);
                    }
                    double per = (double) i/n;
                	double next_per = (double) (i+1)/n;
                	if(per <= 0.05 && next_per>=0.05) {
                		msc.updatePassCount(5, tsrid, i+1, -1);
                	}
                	if(per <= 0.1 && next_per>=0.1) {
                		msc.updatePassCount(10, tsrid, i+1, -1);
                	}
                	if(per <= 0.15 && next_per>=0.15) {
                		msc.updatePassCount(15, tsrid, i+1, -1);
                	}
                	if(per <= 0.2 && next_per>=0.2) {
                		msc.updatePassCount(20, tsrid, i+1, -1);
                	}
                	if(per <= 0.25 && next_per>=0.25) {
                		msc.updatePassCount(25, tsrid, i+1, -1);
                	}
                	if(per <= 0.3 && next_per>=0.3) {
                		msc.updatePassCount(30, tsrid, i+1, -1);
                	}
                	if(per <= 0.35 && next_per>=0.35) {
                		msc.updatePassCount(35, tsrid, i+1, -1);
                	}
                	if(per <= 0.4 && next_per>=0.4) {
                		msc.updatePassCount(40, tsrid, i+1, -1);
                	}
                	if(per <= 0.45 && next_per>=0.45) {
                		msc.updatePassCount(45, tsrid, i+1, -1);
                	}
                	if(per <= 0.5 && next_per>=0.5) {
                		msc.updatePassCount(50, tsrid, i+1, -1);
                	}
                	if(per <= 0.55 && next_per>=0.55) {
                		msc.updatePassCount(55, tsrid, i+1, -1);
                	}
                	if(per <= 0.60 && next_per>=0.60) {
                		msc.updatePassCount(60, tsrid, i+1, -1);
                	}
                	if(per <= 0.65 && next_per>=0.65) {
                		msc.updatePassCount(65, tsrid, i+1, -1);
                	}
                	if(per <= 0.70 && next_per>=0.70) {
                		msc.updatePassCount(70, tsrid, i+1, -1);
                	}
                	if(per <= 0.75 && next_per>=0.75) {
                		msc.updatePassCount(75, tsrid, i+1, -1);
                	}
                	if(per <= 0.80 && next_per>=0.80) {
                		msc.updatePassCount(80, tsrid, i+1, -1);
                	}
                	if(per <= 0.85 && next_per>=0.85) {
                		msc.updatePassCount(85, tsrid, i+1, -1);
                	}
                	if(per <= 0.90 && next_per>=0.90) {
                		msc.updatePassCount(90, tsrid, i+1, -1);
                	}
                	if(per <= 0.95 && next_per>=0.95) {
                		msc.updatePassCount(95, tsrid, i+1, -1);
                	}
                	if(per <= 1.00 && next_per>=1.00) {
                		msc.updatePassCount(100, tsrid, i+1, -1);
                	}
                    if (!continueExecution) {
                        break;
                    }
                }
//				}
                tsel.logExecution("Sg Tear Down");
                tsel.logExecution("Is it first Run - " + firstRun);
				sg.tearDown();
				if(firstRun) {
					msc.updateFirstRunCountToDB(tsrid);
				}
				
				tsel.logExecution("Should failed tests be rerun - " + rerunFailedTests);
				if(rerunFailedTests) {
					tsel.logExecution("Rerunning failed tests started");
	        		rerunFailedTests();
	        		tsel.logExecution("Rerunning failed tests completed");
	        	}
				tsel.logExecution("Continue Execution : " + continueExecution);
				if(continueExecution) {
					Iterator iter2 = testSuiteStatus.keySet().iterator();
	        		while(iter2.hasNext()) {
	        			int pts = (int)iter2.next();
	        			tsel.logExecution("Update Suite status for suite : " + pts);
	        			msc.updateTestSuiteStatus(pts, (String)testSuiteStatus.get(pts));
	        		}
				} else {
               		Iterator iter2 = tsintsindex.keySet().iterator();
               		while(iter2.hasNext()) {
               			int pts = (int)iter2.next();
               			int tsri = (int)tsintsindex.get(pts);
               			if(!testSuiteStatus.containsKey(tsri)) {
               				tsel.logExecution("Update Suite status for tsri : " + tsri);
               				msc.updateTestSuiteStatus(tsri, "STOPPED");
               			} else {
               				tsel.logExecution("Update Suite status for suite (no continue): " + pts + " status:" + (String)testSuiteStatus.get(pts));
               				msc.updateTestSuiteStatus(pts, (String)testSuiteStatus.get(pts));
               			}
               		}
				}
				if(!rerunFailedTests || 
						(rerunFailedTests && completedRunningFailedTests)) {
					tsel.logExecution("Sending Suite Execution completion email " );
					sendSuiteEmail(tsrid, suiteForEmail, tsStartTime,msc,tsel);
				}
			}	
			msc = new MySQlConn(null);
			DashBoardData dbd = new DashBoardData(msc);
			if(!rerunFailedTests || 
					(rerunFailedTests && completedRunningFailedTests)) {
				dbd.updateFlakiness(testSuiteId);
			}
			
			if(!suiteinSuite && !scheduled && firstRun) {
				tsel.logExecution("Update Test Suite count " );
				msc.updateTestSuiteCountAtEnd(tsrid, testCases.size());
				//msc.closeDbConn();
			}
			if(!suiteinSuite && !scheduled && !firstRun) {
				tsel.logExecution("Update Test Suite count after rerun failed Tests");
				TestSuitePrep tsp = new TestSuitePrep();
				int failedCount = tsp.getFailedTCCount(tsrid, companyId, criteria, msc, rerunFailedTests);
				msc.updateTestSuiteCountAfterRerunFailedTests(tsrid, failedCount);
			}
			msc.closeDbConn();
			tsel.logExecution("returning from runGridTestForTS " );
		} catch (Exception e) {
			System.err.println(e.getMessage());
			tsel.logExecution(e);
		} finally {
//			tsel.close();
		}
	}
	
	public void sendSuiteEmail(int tsid, JSONObject suiteForEmail, long tsStartTime,
			MySQlConn msc, ExecutionLogger el) {
		long endTime = System.currentTimeMillis();
		double dur = (endTime - tsStartTime) / 1000.0;
		MySqlConn2 msc2 = new MySqlConn2(msc);
		
		suiteForEmail.put("template", "TestSuiteResult.html");
		suiteForEmail.put("name", (String)forEmail.get("name"));
		suiteForEmail.put("recipient", (String)forEmail.get("name"));
		suiteForEmail.put("recipients", recipients);
		suiteForEmail.put("subject", AppProperties.testsuiteexecutionsubject);
		suiteForEmail.put("duration", dur);
		suiteForEmail.put("pass/fail", passCount.get() + " test scenarios out of " + 
		(passCount.get() + failCount.get()) + " Passed");
		suiteForEmail.put("testSuiteName", testSuiteName);
		suiteForEmail.put("testSuiteId", testSuiteId);
        if (envUrl == null) {
            suiteForEmail.put("envurl", "As Recorded");
        }
        String resultsUrl = Utilities.getSuiteResultsURL(companyId, testSuiteId, tsid);
        suiteForEmail.put("resultsurl", resultsUrl);
        Workbook workbook = msc2.downloadTestSuiteResultFile(tsid, companyId);
        suiteForEmail.put("xls", workbook);
        el.logExecution("Email being sent for test suite -   " + testSuiteName);
        el.logExecution("Email being sent to " + (String) forEmail.get("name"));
        el.logExecution("Additional Receipients are  " + recipients);
        eqh.addToQueue(suiteForEmail);

        String stat = getTestSuiteStatus(tsrid);

        String msg = "The Test Suite " + testSuiteName + " has " + stat + "ED. " +
                passCount.get() + " test scenarios out of " +
                (passCount.get() + failCount.get()) + " have passed. The tests were run against the environment : " +
                envUrl + " and the results can be accessed at the url : " + resultsUrl;

        msg = "{\"text\":\"" + msg + "\"}";

        sendMsgToInt(msg, el, msc2);
        msc2.closeDbConn();
    }

    private void sendMsgToInt(String msg, ExecutionLogger el, MySqlConn2 msc2) {

        if (allIntDets == null) {
            return;
        }

        for (int i = 0; i < allIntDets.size(); i++) {
            JSONObject intDets = (JSONObject) allIntDets.get(i);
            int intId = (int) intDets.get("staticintid");
            String intName = (String) intDets.get("intname");

            HashMap userprops = (HashMap) intUserProps.get(intId);
            JSONObject prodIntDets = msc2.getIntegrationdetail(productId, intName);

            if (intName.equals("MS Teams Channel")) {
                if (prodIntDets != null && prodIntDets.size() > 0) {
                    String url = (String) prodIntDets.get("adminprop");
                    ApiAccess.postToApi(url, null, null, msg, null, el, null);
                } else {
                    el.logExecution(intName + " MS Teams Channel integration not Setup");
                }
            } else if (intName.equals("Azure Dev Ops")) {
                if (userprops != null && userprops.size() > 0) {
                    String conn = (String) prodIntDets.get("adminprop");
                    if(conn != null) {
	                    String[] connDetails = conn.split(",");
	
	                    String userDets = (String) intDets.get("user_attributes");
	                    el.logExecution(intName + " integration message being sent using values: URL -  "
	                            + connDetails[0] + " token: " + connDetails[1] + "User attribute Id " + (String) userprops.get(userDets));
	                    ADOIntegration adoi = new ADOIntegration();
	                    adoi.updateTSResult(connDetails[0],
	                            connDetails[1], msg, (String) userprops.get(userDets));
                    }
                } else {
                    el.logExecution(intName + " Azure Dev Ops integration not Setup");
                }
            } else if (intId == 3) {
                if (userprops != null && userprops.size() > 0) {
                    String conn = (String) prodIntDets.get("adminprop");
                    String[] connDetails = conn.split(",");

                    String userDets = (String) intDets.get("user_attributes");
                    el.logExecution(intName + " integration message being sent using values: URL -  "
                            + connDetails[0] + " token: " + connDetails[1] + "User attribute Id " + (String) userprops.get(userDets));
                    JiraIntegration ji = new JiraIntegration();
                    ji.updateTSResult(connDetails[0],
                            connDetails[1], msg, (String) userprops.get(userDets), connDetails[2]);
                } else {
                    el.logExecution(intName + " Jira integration not Setup");
                }
            } else if (intId == 7) {
                //never send message to github.
            } else {
                el.logExecution(intName + " integration not present");
            }
//			}
        }

    }

    public void parallelExecution(JSONArray testCases, ExecutionLogger tsel,
                                  JSONObject forEmail, String browser1, MySQlConn msc, Set dupTC, Set failedTC,
                                  Tester ptr, int companyId, String randomKey, String envUrl, EmailQueuingHelper eqh,
                                  boolean rerunFailedTests, boolean stopAfterFailure, long tsStartTime, String proxyurl1) {
//        int numberOfThreads = 3; // Number of parallel threads
        ExecutorService executor = Executors.newFixedThreadPool(parallelThreads);
        Semaphore parallelismSemaphore = new Semaphore(parallelThreads); // Semaphore for managing parallelism

        JSONObject results = new JSONObject();
        CountDownLatch completionLatch = new CountDownLatch(testCases.size());
        CountDownLatch emailLatch = new CountDownLatch(2);
        
        int n = testCases.size();
        for (int i = 0; i < testCases.size(); i++) {
            JSONObject json = (JSONObject) testCases.get(i);
            JSONObject testcasesObj = (JSONObject) json.get("testcases");
            JSONObject testcaseZero = (JSONObject) testcasesObj.get(0);
            String caseType = (String) testcaseZero.get("case_Type");
            int testCaseId = (int) testcaseZero.get("idtest_case");
            String randomKeyLocal = randomKey;
            long tsStartTimeLocal = tsStartTime;
            int totalCount = testCases.size();
            executor.execute(() -> {
                SelGrid sg1 = null;
                MySQlConn msc1 = null;
                try {
                    // Acquire a permit from the semaphore to control parallelism
                    parallelismSemaphore.acquire();
                    sg1 = new SelGrid();
                    sg1.setGM(gm);
                    msc1 = new MySQlConn(null);
                    msc1.setRandomKey(msc.randomkey);
                    msc1.setUserDetails(msc.userDetails);

                    if (continueExecution) {
                        // Execute the test case here
                    	long sessionCount = 0;
            			long maxSessions = 0;
            			boolean sufficientAvailable = false;
            			if(caseType == null ||  (caseType != null && 
            					!caseType.equals("MOBILE"))) {
                    	do {
	                    	JSONObject grid = gm.getMaxAndSessionInfo();	            			
	            			
	            			if(grid != null && grid.size() > 0) {
	            				sessionCount = (long)grid.get("sessionCount");
	            				maxSessions = (long)grid.get("maxSession");
	            				if(sessionCount >= maxSessions) {
	            					tsel.logExecution("Session Count = " + sessionCount + " and max sessions allowed = " + maxSessions + 
	            							"retry after a 3 seconds ");
	            					try {
	            						Thread.sleep(3000);
	            					} catch (Exception te) {
	            						
	            					}
	            				} else {
	            					sufficientAvailable = true;
	            				}
	            				
	            			}
                    	}while(!sufficientAvailable);
                }
            			
            	if (caseType != null && ("MOBILE").equals(caseType)) {
            		boolean isParallel = true;
            		String status = callMobileSuitAPI(randomKeyLocal, testCaseId, tsrid, totalCount, tsStartTimeLocal, tsel, isParallel);
                } else {
                        ParallelTestCaseExecutor ptce = new ParallelTestCaseExecutor();
                        ptce.executeTestCase(json, tsel, forEmail, sg1, browser1, 
                        		msc1, dupTC, failedTC, ptr, companyId, randomKey, envUrl, 
                        		eqh, rerunFailedTests, stopAfterFailure, true, tsStartTime, 
                        		false, ssForAllElement, ssonerror, proxyurl1, indEmail, mode);
                }
            	completionLatch.countDown(); 
                        tsel.logExecution("testcases executed : " + (testCases.size() - completionLatch.getCount()));
                    }
                	tsel.logExecution("Should execution continue : " +  continueExecution);
                	tsel.logExecution("How many more tests are yet to be run? : " +  completionLatch.getCount());
                    if (!continueExecution || completionLatch.getCount() == 0) {
                        // If the stop flag is set or all executions are completed, cancel any further executions
                    	if(emailLatch.getCount() == 2) {
                    		emailLatch.countDown();
                    		Iterator iter = testSuiteStatus.keySet().iterator();
                    		while(iter.hasNext()) {
                    			int tsri = (int)iter.next();
                    			msc1.updateTestSuiteStatus(tsri, (String)testSuiteStatus.get(tsri));
                    		}
                    		
                    		if(!rerunFailedTests || 
                    				(rerunFailedTests && completedRunningFailedTests)) {
                    			sendSuiteEmail(testSuiteId, forEmail, tsStartTime, msc1,tsel);
                    		}
                    	}
	                    if(!continueExecution) {
	                    	Iterator iter = tsintsindex.keySet().iterator();
	                    	while(iter.hasNext()) {
	                    		int pts = (int)iter.next();
	                    		int tsri = (int)tsintsindex.get(pts);
	                    		if(!testSuiteStatus.containsKey(tsri)) {
	                    			msc1.updateTestSuiteStatus(tsri, "STOPPED");
	                    		} else {
	                    			msc1.updateTestSuiteStatus(tsri, (String)testSuiteStatus.get(tsri));
	                    		}
	                    	}
	                    	executor.shutdownNow();
	                    } else {
	                    	executor.shutdown();
	                    }
                    	tsel.close();
                    	
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    tsel.logExecution(e);
                    el1.logExecution(e);
                    tsel.close();
                } finally {
                    // Release the permit to allow the next test to execute
                    parallelismSemaphore.release();
//                    sg1.tearDown();                    
                    msc1.closeDbConn();
                }
            });

        	double per = (double) i/n;
        	double next_per = (double) (i+1)/n;
        	
            if(per <= 0.05 && next_per>=0.05) {
        		msc.updatePassCount(5, tsrid, i+1, -1);
        	}
        	if(per <= 0.1 && next_per>=0.1) {
        		msc.updatePassCount(10, tsrid, i+1, -1);
        	}
        	if(per <= 0.15 && next_per>=0.15) {
        		msc.updatePassCount(15, tsrid, i+1, -1);
        	}
        	if(per <= 0.2 && next_per>=0.2) {
        		msc.updatePassCount(20, tsrid, i+1, -1);
        	}
        	if(per <= 0.25 && next_per>=0.25) {
        		msc.updatePassCount(25, tsrid, i+1, -1);
        	}
        	if(per <= 0.3 && next_per>=0.3) {
        		msc.updatePassCount(30, tsrid, i+1, -1);
        	}
        	if(per <= 0.35 && next_per>=0.35) {
        		msc.updatePassCount(35, tsrid, i+1, -1);
        	}
        	if(per <= 0.4 && next_per>=0.4) {
        		msc.updatePassCount(40, tsrid, i+1, -1);
        	}
        	if(per <= 0.45 && next_per>=0.45) {
        		msc.updatePassCount(45, tsrid, i+1, -1);
        	}
        	if(per <= 0.5 && next_per>=0.5) {
        		msc.updatePassCount(50, tsrid, i+1, -1);
        	}
        	if(per <= 0.55 && next_per>=0.55) {
        		msc.updatePassCount(55, tsrid, i+1, -1);
        	}
        	if(per <= 0.60 && next_per>=0.60) {
        		msc.updatePassCount(60, tsrid, i+1, -1);
        	}
        	if(per <= 0.65 && next_per>=0.65) {
        		msc.updatePassCount(65, tsrid, i+1, -1);
        	}
        	if(per <= 0.70 && next_per>=0.70) {
        		msc.updatePassCount(70, tsrid, i+1, -1);
        	}
        	if(per <= 0.75 && next_per>=0.75) {
        		msc.updatePassCount(75, tsrid, i+1, -1);
        	}
        	if(per <= 0.80 && next_per>=0.80) {
        		msc.updatePassCount(80, tsrid, i+1, -1);
        	}
        	if(per <= 0.85 && next_per>=0.85) {
        		msc.updatePassCount(85, tsrid, i+1, -1);
        	}
        	if(per <= 0.90 && next_per>=0.90) {
        		msc.updatePassCount(90, tsrid, i+1, -1);
        	}
        	if(per <= 0.95 && next_per>=0.95) {
        		msc.updatePassCount(95, tsrid, i+1, -1);
        	}
        	if(per <= 1.00 && next_per>=1.00) {
        		msc.updatePassCount(100, tsrid, i+1, -1);
        	}
        }

        executor.shutdown();
        try {
            executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
            if(firstRun) {
				msc.updateFirstRunCountToDB(tsrid);
			}
            if (rerunFailedTests) {
                rerunFailedTests();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void rerunFailedTests() {
    	tsel.logExecution("Starting rerun of failed Tests");
        String stat = getTestSuiteStatus(tsrid);
        //tsel.logExecution("Suite Status: " + stat);
        if(firstRun) {
        	msc.set2ndRunCount(tsrid);
        }
        
        tsel.logExecution("Test Suite Status is : " + stat);
        if (stat != null && (stat.equals("PASS") || stat.equals("FAIL"))) {
        	tsel.logExecution("No of failed test cases is : " + failedTestCases.size());
            if (failedTestCases.size() > 0) {
            	//tsel.logExecution("Failed Test Cases Size is greater than zero");
                if(rerunInParallel) {
                	supportsParallelExecution = true;
                } else {
                	supportsParallelExecution = false;
                }
                firstRun = false;
                ssonerror = false;
                tsel.logExecution("has second runflag been turned on : " + firstRun);
				JSONArray ja = failedTestCases;
				failedTestCases = new JSONArray();
				msc.updateTestSuiteStatus(tsrid, "RERUNNING");
				
				runGridTestForTS(msc, ja, false, browser,
						proxyurl,scheduled, true);
				completedRunningFailedTests = true;
			}
		}
	}
	
	public void synchExecution1(JSONArray testCases, int tsrid, ExecutionLogger tsel, 
			JSONObject forEmail, String browser1, MySQlConn msc, Set dupTC, Set failedTC, 
			Tester ptr, int companyId, String randomKey, String envUrl, EmailQueuingHelper eqh, 
			boolean rerunFailedTests, boolean stopAfterFailure, long tsStartTime, String proxyurl1) {
		
		if(testCases == null || testCases.size() < 2) {
			return;
		}
		
		List<JSONObject> jsonList = new ArrayList<>();	
		JSONObject shellCase = null;
		
		for(int i=0;i<testCases.size();i++) {
			JSONObject nextCase = (JSONObject)testCases.get(i);
			tsel.logExecution("Reading the " + i +"th test case");
			
			JSONObject testcases = (JSONObject)nextCase.get("testcases");
			JSONObject zero = (JSONObject)testcases.get(0);
			int testCaseId = (int)zero.get("idtest_case");			
			JSONObject steps = msc.getTestStepsFromDBByTestCaseId(testCaseId, true, true);
			JSONObject data = (JSONObject) steps.get("data");
			if(i == 0) {
				this.testCaseId = testCaseId;
				shellCase = steps;
			} 
			int j = 2;
			while(j <= data.size()+1) {				
				JSONObject step = (JSONObject) data.get(j);
				if(i > 0 && j == 2) {
					String keyword = (String)step.get("Keyword");
					String action = (String)step.get("Action");
					if(keyword != null && keyword.equalsIgnoreCase("URL") &&
							action != null && action.equalsIgnoreCase("Get")) {
						step.put("Keyword", "Window");
						step.put("Action", "Create");
						step.put("VarName", "Profile " + i);
					}
				}
				jsonList.add(step);
				j = j+1;
			}
			
			 Collections.sort(jsonList, new Comparator<JSONObject>() {
	                @Override
	                public int compare(JSONObject o1, JSONObject o2) {
		                try {
	                        // Parse "createddate" into Timestamp objects
	                        Timestamp timestamp1 = (Timestamp) o1.get("createddate");
	                        Timestamp timestamp2 = (Timestamp) o2.get("createddate");
	                        return timestamp1.compareTo(timestamp2);
	                    } catch (IllegalArgumentException e) {
	                        e.printStackTrace();
	                        return 0;
	                    }
	                }
	            });
		}
		
		// Create a new JSONObject for sorted JSON objects
        JSONObject sortedJsonObject = new JSONObject();
        int index = 2;
        for (int i = 0; i < jsonList.size(); i++) {
            sortedJsonObject.put(index, jsonList.get(i));
            index = index + 1;
        }

        JSONObject data = new JSONObject();
        shellCase.put("data", sortedJsonObject);

        sg.tsr = tsrid;
		
		JSONObject testCaseDetails = msc.getTestCaseDetailsFromId(testCaseId);
		
		testCaseResultsId = msc.writeTestCaseStatusToDB(testCaseId, "STARTED",
				"SYS", 0.0, browser, (String)testCaseDetails.get("Test_Case"), companyId);
		
		ExecutionLogger tcel =sortEL(sg,companyId,testCaseId,testCaseResultsId);
		sg.setExecutionLogger(tcel);
		
		boolean sesSuccess = browserSetup(sg, browser, proxyurl, tsel, 
				String.valueOf(testCaseId), (boolean)testCaseDetails.get("enableaudio"),
				(boolean)testCaseDetails.get("enablevideo"), null, testCaseId,
				(boolean)testCaseDetails.get("stealthMode"));
		if(!sesSuccess) {
			msc.updateTestCaseStatusToDB(testCaseResultsId, "ABORTED", 0, "ABORTED", companyId);
			tsel.logExecution("Test Aborted");
			return;
		}
		sg.downloadDir = Utilities.getTCDownloadDir(companyId, testCaseId, testCaseResultsId);
		if(crxTest) {
			sendRequestForMerge();
		}

        boolean prevAudio = (boolean) testCaseDetails.get("enableaudio");
        boolean prevVideo = (boolean) testCaseDetails.get("enablevideo");

        forEmail = sg.test(shellCase, msc, companyId, randomKey, forEmail, 0, envUrl,
                testCaseResultsId, testCaseDetails, false, 0, this, false, -1, null,
                ssForAllElement, analysisRun, baselineRun, false);
        runonce = (boolean) forEmail.get("runonce");
        String status = (String) forEmail.get("status");

        long endTime = System.currentTimeMillis();
        double dur = (endTime - tsStartTime) / 1000.0;
        if (((String) forEmail.get("status")).equals("PASS")) {
            tcel.logExecution("Test Case Passed");
            msc.updateCount(tsrid, "PASS", "PASS", dur);
            msc.updateTestSuiteStatus(tsrid, "PASS");
        }

        if (((String) forEmail.get("status")).equals("FAIL")) {
            msc.updateCount(tsrid, "FAIL", "FAIL", dur);
            msc.updateTestSuiteStatus(tsrid, "FAIL");
            sg.tearDown();
        } else if (((String) forEmail.get("status")).equals("STOPP")) {
            msc.updateCount(tsrid, "STOPP", "FAIL", dur);
            msc.updateTestSuiteStatus(tsrid, "STOPP");
            sg.tearDown();
        } else if (((String) forEmail.get("status")).equals("ABEND")) {
            msc.updateCount(tsrid, "ABEND", "FAIL", dur);
            updateTestSuiteStatus(tsrid, "ABEND");
            sg.tearDown();
        } else if (((String) forEmail.get("status")).equals("TIMEOUT")) {
            msc.updateCount(tsrid, "TIMEOUT", "FAIL", dur);
            msc.updateTestSuiteStatus(tsrid, "TIMEOUT");
            sg.tearDown();
        }

    }

    public void synchExecution(JSONArray testCases, int tsrid, ExecutionLogger tsel,
                               JSONObject forEmail, String browser1, MySQlConn msc, Set dupTC, Set failedTC,
                               Tester ptr, int companyId, String randomKey, String envUrl, EmailQueuingHelper eqh,
                               boolean rerunFailedTests, boolean stopAfterFailure, long tsStartTime, String proxyurl1) {

        boolean istrue = true;

        if (istrue) {
            synchExecution1(testCases, tsrid, tsel, forEmail, browser1,
                    msc, dupTC, failedTC, ptr, companyId, randomKey, envUrl,
                    eqh, rerunFailedTests, stopAfterFailure, tsStartTime, proxyurl1);
            return;
        }

        int availableThreads = gm.getNoOfThreadsgql();

        if (availableThreads < testCases.size()) {
            tsel.logExecution("No of threads is less than number of syncronize scenarios");
            updateTestSuiteStatus(tsrid, "FAIL");
            return;
        }

        if (testCases.size() != 2) {
            tsel.logExecution("Test Cases count has to be 2");
            updateTestSuiteStatus(tsrid, "FAIL");
            return;
        }

        ExecutorService executor = Executors.newFixedThreadPool(testCases.size());
        Semaphore parallelismSemaphore = new Semaphore(testCases.size()); // Semaphore for managing parallelism

        JSONObject results = new JSONObject();
        CountDownLatch completionLatch = new CountDownLatch(testCases.size());
        CountDownLatch emailLatch = new CountDownLatch(2);

        JSONObject json0 = (JSONObject) testCases.get(0);

        JSONObject testcases0 = (JSONObject) json0.get("testcases");
        JSONObject zero0 = (JSONObject) testcases0.get(0);
        int testCaseId0 = (int) zero0.get("idtest_case");
        JSONObject testCaseDetails0 = msc.getTestCaseDetailsFromId(testCaseId0);

        int testCaseResultsId0 = msc.writeTestCaseStatusToDB(testCaseId0, "STARTED",
                "SYS", 0.0, browser, (String) testCaseDetails0.get("Test_Case"), companyId);

        JSONObject json1 = (JSONObject) testCases.get(1);

        JSONObject testcases1 = (JSONObject) json1.get("testcases");
        JSONObject zero1 = (JSONObject) testcases1.get(0);
        int testCaseId1 = (int) zero1.get("idtest_case");
        JSONObject testCaseDetails1 = msc.getTestCaseDetailsFromId(testCaseId1);

        int testCaseResultsId1 = msc.writeTestCaseStatusToDB(testCaseId1, "STARTED",
                "SYS", 0.0, browser, (String) testCaseDetails1.get("Test_Case"), companyId);

        json0.put("tcrid", testCaseResultsId0);
        json1.put("tcrid", testCaseResultsId1);
        json0.put("synchtcrid", testCaseResultsId1);
        json1.put("synchtcrid", testCaseResultsId0);
        json1.put("synchtcid", testCaseId0);
        json0.put("synchtcid", testCaseId1);

        for (int i = 0; i < testCases.size(); i++) {
            JSONObject json = (JSONObject) testCases.get(i);

            executor.execute(() -> {
                SelGrid sg1 = null;
                MySQlConn msc1 = null;
                try {
                    // Acquire a permit from the semaphore to control parallelism
                    parallelismSemaphore.acquire();

                    sg1 = new SelGrid();
                    sg1.tsr = tsrid;
                    sg1.setGM(gm);
    				boolean sesSuccess = browserSetup(sg1, browser1, proxyurl1, tsel, testCase,
    						false,false, null, testCaseId1,false);
    				
    				if(!sesSuccess) {
    					msc.writeTestCaseStatusToDB(testCaseId0, "ABORTED",
    							"SYS", 0.0, browser, (String)testCaseDetails0.get("Test_Case"),
    							companyId);
    					msc.updateCount(tsrid, "ABORTED", "ABORTED", 0.0);
    					updateTestSuiteStatus(tsrid, "ABORTED");
    					tsel.logExecution("Test Aborted");
    					throw new Exception("Not enough sessions capacity available ");
    				}
                    msc1 = new MySQlConn(null);
                    msc1.setRandomKey(msc.randomkey);
                    msc1.setUserDetails(msc.userDetails);
                    if (continueExecution) {
                        // Execute the test case here
                        ParallelTestCaseExecutor ptce = new ParallelTestCaseExecutor();
                        ptce.executeTestCase(json, tsel, forEmail, sg1, browser1,
                                msc1, dupTC, failedTC, ptr, companyId, randomKey, envUrl,
                                eqh, rerunFailedTests, stopAfterFailure, true, tsStartTime,
                                true, ssForAllElement, ssonerror, proxyurl1, indEmail, mode);
                        completionLatch.countDown();
                    }
                    if (!continueExecution || completionLatch.getCount() == 0) {
                        // If the stop flag is set or all executions are completed, cancel any further executions
                        if (emailLatch.getCount() == 2) {
                            emailLatch.countDown();
                            sendSuiteEmail(tsrid, forEmail, tsStartTime, msc1, tsel);
                            if (!continueExecution) {
                                executor.shutdownNow();
                            } else {
                                executor.shutdown();
                            }
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    tsel.logExecution(e);
                    el1.logExecution(e);
                } finally {
                    // Release the permit to allow the next test to execute
                    parallelismSemaphore.release();
                    sg1.tearDown();
                    msc1.closeDbConn();
                }
            });
        }
    }
	
	public void updateTestSuiteStatus(int tsrid, String newStatus) {
		String tsStat = null;
		if(testSuiteStatus.containsKey(tsrid)) {
			tsStat = (String)testSuiteStatus.get(tsrid);
		}
		if(tsStat == null || tsStat.equals("PASS") || 
				tsStat.equals("FAIL") || tsStat.equals("RUNNING")) {
			
			if(testSuiteStatus.get(tsrid) != null) {
				String stat = (String)testSuiteStatus.get(tsrid);
				if(stat != null) {
					if(!(stat.equals("FAIL") && newStatus.equals("PASS"))) {
						testSuiteStatus.put(tsrid, newStatus);
					}					
				} else {
					testSuiteStatus.put(tsrid, newStatus);
				}
			} else {
				testSuiteStatus.put(tsrid, newStatus);
			}
		}		
	}
	
	public String getTestSuiteStatus(int tsrid) {
		if(testSuiteStatus.containsKey(tsrid)) {
			return (String)testSuiteStatus.get(tsrid);
		}
		
		return "RUNNING";
	}
	
	public void updatePassCount() {		
			passCount.incrementAndGet();
		if(!firstRun) {
			failCount.decrementAndGet();
		}
	}
	
	public void updateFailCount() {
		if(firstRun) {
			failCount.incrementAndGet();
		}
	}
	
	public void setURLData(JSONObject ud) {
		urlData = ud;
	}
	
	public void setTestSuite(JSONArray ts) {
		testSuite = ts;
	}
	
	public void setTestCase(String tc) {
		testCase = tc;
	}
	
	public void setCompanyId(int cid) {
		companyId = cid;
	}
	
	public void setTestCaseId(int id) {
		testCaseId = id;
	}
	
	public void setTestCaseresultsId(int id) {
		testCaseResultsId = id;
	}
	
	public void setDebugMode(boolean t) {
		debugMode = t;
	}
	
	public void setTestSuiteId(int id) {
		testSuiteId = id;
	}
	
	public void setTestSuiteName(String name) {
		testSuiteName = name;
	}
	
	public void setMSC(MySQlConn mysc) {
		msc = mysc;
	}
	
	public void setBrowser(String b) {
		browser = b;
	}
	
	public void setRandomKey(String b) {
		randomKey = b;
	}
	
	public void setTestCaseName(String b) {
		testCaseName = b;
	}
	
	public void setResultsForEmail(JSONObject emailData, EmailQueuingHelper e) {
		 forEmail = emailData;
		 eqh = e;
	}
	
	public void setRunOnce(String opt) {
		if(opt == null || opt.equals("Yes") || opt.equals("")) {
			runonce = true;
		} else {
			runonce = false;
		}
	}
	
	public void setSPE(String spe, Utilities userCache) {
		if(spe == null || spe.trim().equals("true") || spe.equals("")) {
			supportsParallelExecution = true;
		} else {
			supportsParallelExecution = false;
		}
		
		setParallelThreadsCount(userCache);
	}
	
	public void setParallelThreadsCount(Utilities userCache) {

		if(msc.getUserDetails().get("parallelThreads") == null) {
			parallelThreads = msc.getparallelThreadsForCompany(companyId);
			JSONObject userDetails = msc.getUserDetails();
			String randomkey = (String)userDetails.get("randomkey");
			userDetails.put("parallelThreads",parallelThreads);
			userCache.cacheUser(randomkey, userDetails);
		} else {
			parallelThreads = (int)msc.getUserDetails().get("parallelThreads");
		}
//		int threadCount = msc.getParallelThreadCount(testSuiteId);
//		int assignedCount = msc.getparallelThreadsForCompany(companyId);
//		if(threadCount == 0) {
//			parallelThreads = msc.getparallelThreadsForCompany(companyId);
//		} else {
//			if(threadCount <= assignedCount) {
//				parallelThreads = threadCount;
//			} else {
//				parallelThreads = assignedCount;
//			}
//		}
	}
	
	public void analysetest(JSONObject original, JSONObject latest, int companyId, int testCaseId,
			int analysistc, int tcr, JSONObject testCaseDetails, ExecutionLogger el) {
		String logFilePath = Utilities.getAnalysisFile(companyId, testCaseId);
		Utilities.createTestCaseAnalysisFolder(companyId, testCaseId);
		MySqlConn2 msc2 = new MySqlConn2(msc);
//		msc2.moveTestCaseResultsForAnalysis(analysistc, tcr);
        JSONArray originalPassResult = msc.getTestStepResultFromTestCase(testCaseId, companyId);
        JSONArray latestResult = msc.getTestStepResultFromTestCaseResult(tcr, false);
        el.logExecution("Starting Analysis");

        int originalSize = original.size();
        el.logExecution("Number of steps in Original = " + originalSize);
        int latestSize = latest.size();
        el.logExecution("Number of steps in Latest = " + (latestSize + 1));

        if (originalSize == latestSize + 1) {
            el.logExecution("Number of steps matches ");
        } else {
            el.logExecution("Number of steps do not match ");
        }

        Iterator origIter = original.keySet().iterator();
        Iterator lateIter = latest.keySet().iterator();

        boolean origError = false;
        boolean lateError = false;
        JSONObject origStep = null;
        JSONObject lateStep = null;
        int origErrorStepNum = -1;

        int index = 0;

        String resultFile = Utilities.getAnalysisDir(companyId, testCaseId) + "screenshots\\";
        try {
            Path path = Paths.get(resultFile);
            if (Files.exists(path)) {
                Files.walk(path)
                        .sorted(java.util.Comparator.reverseOrder())
                        .map(Path::toFile)
                        .forEach(java.io.File::delete);
            }
        } catch (Exception e) {
            System.err.println("Issue in deleting the previous analysis screenshots");
            System.err.println(e.getMessage());
        }
        Utilities.createFolder(resultFile);
        int imagesbaselined = (int) testCaseDetails.get("imagesbaselined");
        Map<String, String> fileNames = new HashMap();
        String baselineDir = "";
        el.logExecution("Image baselined flag = " + imagesbaselined);

        if (imagesbaselined == 1) {
            baselineDir = Utilities.getBaselineDir(companyId, testCaseId);
            fileNames = Utilities.getFilenamesForCompare(baselineDir);
        }

        while (lateIter.hasNext() && (index < originalPassResult.size() - 1) &&
                (index < latestResult.size() - 1)) {
            index = index + 1;

            el.logExecution("Strated the compare  ");
            imageCompare(index, originalPassResult, latestResult,
                    resultFile, analysistc, tcr, el, imagesbaselined,
                    baselineDir, fileNames);

            origStep = (JSONObject) original.get(origIter.next());
            int origStepNum = Integer.valueOf((String) origStep.get("tsSequence"));
            if (origStepNum == 0) {
                continue;
            }

            int lateIndex = (int) lateIter.next();
            lateStep = (JSONObject) latest.get(lateIndex);
            int lateStepNum = Integer.valueOf((String) lateStep.get("tsSequence"));

            int origtsid = (int) origStep.get("idtest_step");
            int latesttsid = (int) lateStep.get("idtest_step");

            String origXPath = (String) origStep.get("Object_Xpath");
            origXPath = origXPath.replaceAll("'", "\"");
            origStep.put("Object_Xpath", origXPath);

            boolean isEqual = true;
            el.logExecution("************************************************************************************************");

            for (Object key : origStep.keySet()) {
                String keyStr = (String) key;
                if (keyStr.equalsIgnoreCase("Step_Number") ||
                        keyStr.equalsIgnoreCase("Test_Case_Id") ||
                        keyStr.equalsIgnoreCase("outerhtml") ||
                        keyStr.equalsIgnoreCase("idtest_step") ||
                        keyStr.equalsIgnoreCase("extKey") ||
                        keyStr.equalsIgnoreCase("eventTime") ||
                        keyStr.equalsIgnoreCase("indexcvrg") ||
                        keyStr.equalsIgnoreCase("tabid") ||
                        keyStr.equalsIgnoreCase("actualtime") ||
                        keyStr.equalsIgnoreCase("dynamicProcessed") ||
                        keyStr.equalsIgnoreCase("pagenumber") ||
                        keyStr.equalsIgnoreCase("waittime") ||
                        keyStr.equalsIgnoreCase("strategy") ||
                        keyStr.equalsIgnoreCase("RecordedData")) {
                    continue;
                }

                Object value1 = origStep.get(key);
                Object value2 = lateStep.get(key);

                if (value2 == null) {
                    value2 = "";
                }

                if (value1 != null && !value1.equals(value2)) {
//                	el.logExecution("Key: " + key + ", Value1: " + value1 + ", Value2: " + value2);
                    if (keyStr.equalsIgnoreCase("bgcolor")) {
                        el.logExecution("--------------------------WARNING - Key: " + key + "------------------------------------------------------------------------------");
                        el.logExecution("Original Value: " + value1);
                        el.logExecution(" Current Value: " + value2);
//                		el.logExecution("--------------------------COMPARISON END - Key: " + key + "-----------------------------------------------------------------------");
                        msc2.writeErrorForTestStep(origtsid, "WARNING", key.toString(), "WARNING", value1.toString(),
                                value2.toString(), analysistc);
                    } else {
                        el.logExecution("--------------------------CRITICAL ERROR - Key: " + key + "------------------------------------------------------------------------------");
                        el.logExecution("Original Value: " + value1);
                        el.logExecution(" Current Value: " + value2);
//                		el.logExecution("--------------------------COMPARISON END - Key: " + key + "-----------------------------------------------------------------------");
                        msc2.writeErrorForTestStep(origtsid, "CRITICAL", key.toString(), "CRITICAL", value1.toString(),
                                value2.toString(), analysistc);
                        isEqual = false;
                    }
                }
            }

            if (isEqual) {
                if (origError) {
                    el.logExecution("Step  Num " + origErrorStepNum + " is missing in the current version of the application");
                }
                el.logExecution("Step  Num " + lateStepNum + " matches the current run");
                origError = false;
                lateError = false;
            } else {
                el.logExecution("Step  Num " + lateStepNum + " has an issue");
                origError = true;
                origErrorStepNum = lateStepNum;
            }

            el.logExecution("************************************************************************************************");

        }
        msc.updateTestCaseAnalyseStatus(testCaseId, 1);
        msc2.closeDbConn();
    }

    public void imageCompare(int index, JSONArray originalPassResult, JSONArray latestResult,
                             String resultFile, int analysistc, int tcr, ExecutionLogger el, int imagesbaselined,
                             String baselineDir, Map<String, String> fileNames) {
        el.logExecution("Starting Image compare");
        try {
            JSONObject origStepResult = (JSONObject) originalPassResult.get(index);
            JSONObject latestStepResult = (JSONObject) latestResult.get(index);
            String origSSFile = origSSFile = (String) origStepResult.get("Failure_Screenshot_Location");
            origSSFile = origSSFile.substring(origSSFile.indexOf("&fileName=") + 10, origSSFile.length());

            if (imagesbaselined == 1) {
                String suffix = origSSFile.substring(origSSFile.indexOf("_Step"), origSSFile.length());
                origSSFile = fileNames.get(suffix);
            }
            String latestSSFile = (String) latestStepResult.get("Failure_Screenshot_Location");
            latestSSFile = latestSSFile.substring(latestSSFile.indexOf("&fileName=") + 10, latestSSFile.length());

            int origTcrid = (int) origStepResult.get("Test_Case_Results_Id");
            resultFile = Utilities.getAnalysisDir(companyId, testCaseId) + "screenshots\\";

            int stepNum = (int) origStepResult.get("Step_Number");
            String latestFileInAnalysisFolder = resultFile + Utilities.getAnalysisSSFileName(testCaseId, stepNum, companyId, "latest");

            String origFileInAnalysisFolder = resultFile + Utilities.getAnalysisSSFileName(testCaseId, stepNum, companyId, "orig");
            if (imagesbaselined == 1) {
                origSSFile = baselineDir + origSSFile;
            } else {
                origSSFile = Utilities.getScreenShotsDir(companyId, testCaseId, origTcrid) + origSSFile;
            }
            latestSSFile = Utilities.getScreenShotsDir(companyId, analysistc, tcr) + latestSSFile;
            el.logExecution("image diff of baseline file " + origSSFile + " and latest file " + latestSSFile);
            Utilities.callImageDiffApi(companyId, origSSFile, latestSSFile, latestFileInAnalysisFolder);
            Utilities.copyFile(origSSFile, origFileInAnalysisFolder);
            el.logExecution("Completed Image compare successfully");
        } catch (Exception e) {
            e.printStackTrace();
            el.logExecution("Issue in image diff");
        }
    }
    
//    public static void callMobileSuitAPI(String randomKey, int testCaseId, int tsrid, int totalCount, long tsStartTime2, ExecutionLogger tsel, boolean isParallel) {
//        String loggerId = UUID.randomUUID().toString();
//        LoggerRegistry.registerLogger(loggerId, tsel);
//
//        String params = "&tkn&tcid&tsrid&tCount&logId&tsStartTime&isParallel";
//        params = params.replace("&tkn", "token=" + randomKey);
//        params = params.replace("&tcid", "&Test_Case_Id=" + testCaseId);
//        params = params.replace("&tsrid", "&testSuiteResultId=" + tsrid);
//        params = params.replace("&tCount", "&totalCount=" + totalCount);
//        params = params.replace("&logId", "&logId=" + loggerId);
//        params = params.replace("&tsStartTime", "&tsStartTime=" + tsStartTime2);
//        params = params.replace("&isParallel", "&isParallel=" + isParallel);
//
//        ConcurrentHashMap<String, String> varMap = new ConcurrentHashMap<>();
//        ApiAccess.callApiTD(AppProperties.mobileSuitAPI + params, "", "", tsel, varMap, "{}");
//    }
    
	public static String callMobileSuitAPI(String randomKey, int testCaseId, int tsrid, int totalCount,
			long tsStartTime2, ExecutionLogger tsel, boolean isParallel) {
		String loggerId = UUID.randomUUID().toString();
		LoggerRegistry.registerLogger(loggerId, tsel);

		String params = "&tkn&tcid&tsrid&tCount&logId&tsStartTime&isParallel";
		params = params.replace("&tkn", "token=" + randomKey);
		params = params.replace("&tcid", "&Test_Case_Id=" + testCaseId);
		params = params.replace("&tsrid", "&testSuiteResultId=" + tsrid);
		params = params.replace("&tCount", "&totalCount=" + totalCount);
		params = params.replace("&logId", "&logId=" + loggerId);
		params = params.replace("&tsStartTime", "&tsStartTime=" + tsStartTime2);
		params = params.replace("&isParallel", "&isParallel=" + isParallel);

		ConcurrentHashMap<String, String> varMap = new ConcurrentHashMap<>();
		String response = (String) ApiAccess.callApiTD(AppProperties.mobileSuitAPI + params, "", "", tsel, varMap, "{}");
		tsel.logExecution("Received result for TestCaseID " + testCaseId + ": " + response);
		return response;
	}


    public void setStopAfterFailure(boolean saf) {
        stopAfterFailure = saf;
    }

    public void setIndEmail(boolean saf) {
        indEmail = saf;
    }

    public void setExecuteCompleteTS(boolean ects) {
        executeCompleteTS = ects;
    }

    public void setRerunFailedTests(boolean rft) {
        rerunFailedTests = rft;
    }

    public void setScheduled(boolean s) {
        scheduled = s;
    }

    public void setCrxTest(boolean crxt) {
        crxTest = crxt;
    }

    public void setHeadless(boolean hl) {
        headless = hl;
    }

    public void setStepNum(int sn) {
        stepnum = sn;
    }

    public void setTillTC(int sn) {
        tillTC = sn;
    }

    public void setNextNum(int sn) {
        nextnum = sn;
    }

    public void setEnvUrl(String e) {
        if (e != null && !e.equals("")) {
            envUrl = e;
        }
    }

    public void setDupTC(Set dtc) {
        dupTC = dtc;
    }

    public void setFailedTC(Set dtc) {
        failedTC = dtc;
    }

    public void setWorkItem(String wi) {
        workItem = wi;
    }

    public void setMode(String m) {
        mode = m;
        if (mode != null) {
            if (mode.equalsIgnoreCase("analysis")) {
                analysisRun = true;
            } else if (mode.equalsIgnoreCase("baseline")) {
                baselineRun = true;
            }
        }
    }

    public void setProxyUrl(String m) {
        proxyurl = m;
    }

    public void setIntUserProps(HashMap p) {
        intUserProps = p;
    }

    public void setAllIntDets(JSONArray j) {
        allIntDets = j;
    }

    public void setProductId(int p) {
        productId = p;
    }

    public void setSynchScenarios(boolean p) {
        synchScenario = p;
    }

    public void setSSOnError(boolean p) {
        ssonerror = p;
    }

    public void setScreenshotForAllEle(boolean ssFlag) {
        ssForAllElement = ssFlag;
    }

    public void setAnalysisTC(int p) {
        analysisTC = p;
    }

    public void setIterations(int p) {
        iterations = p;
    }

    public void setRecipients(String[] r) {
        recipients = r;
    }

    public void setBuildTag(String r) {
        buildtag = r;
    }

    public void setCriteria(String c) {
        criteria = c;
    }

    public void setRerunInParallel(boolean rp) {
		rerunInParallel = rp;
	}
    
    public void setVariablesJSON(JSONObject json) {
    	variableJSON = json;
    }
    
    public void setInitialVarMap(JSONObject json, SelGrid sg) {
    	if(json == null)return;
    	Iterator keys = json.keySet().iterator();
        while (keys.hasNext()) {
            String key = (String) keys.next();
            String value = (String) json.get(key);
            varMap.put(key, value);
        }
    }
}
