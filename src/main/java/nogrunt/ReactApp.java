package nogrunt;

import nogrunt.integrations.*;
import nogrunt.nlp.*;
import nogrunt.codegen.*;
import nogrunt.codegenNew.CodeGenFull;
import nogrunt.exceptions.*;
import nogrunt.api.*;
import nogrunt.dbaccess.*;
import nogrunt.payments.*;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.net.URLDecoder;
import java.util.*;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;

import org.json.simple.JSONObject;
import org.json.simple.JSONArray;
import org.json.simple.parser.JSONParser;

import jakarta.servlet.ServletException;
import jakarta.servlet.ServletContext;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;

/**
 * Servlet implementation class ReactApp
 */
@WebServlet("/ReactApp")
public class ReactApp extends HttpServlet {
    private static final long serialVersionUID = 1L;
    public static final int DEFAULT_BUFFER_SIZE = 8192;
    private Thread auditQueueThread;
    private AuditQueuingHelper auditQueue;
    private Thread emailSenderThread;
    private EmailQueuingHelper emailQueue;
    private Thread CleanUpQueueThread;
    private CleanUpQueueHelper cleanUpQueue;
    private boolean checker = true;

    /**
     * @see HttpServlet#HttpServlet()
     */
    public ReactApp() {
        super();
        // TODO Auto-generated constructor stub
    }

    public void init() throws ServletException {

        System.setProperty("java.library.path", "C:\\Nogrunt\\libs");

        auditQueue = new AuditQueuingHelper();
        Utilities.aqh = auditQueue;
        auditQueueThread = new AuditQueueReader(auditQueue);
        auditQueueThread.start();

        emailQueue = new EmailQueuingHelper();
        Utilities.eqh = emailQueue;
        emailSenderThread = new EmailQueueReader(emailQueue);
        emailSenderThread.start();
        
        cleanUpQueue = new CleanUpQueueHelper();
        Utilities.cleanUpQueue = cleanUpQueue;
        CleanUpQueueThread = new CleanUpQueueReader(cleanUpQueue);
        CleanUpQueueThread.start();

        ServletContext servletContext = getServletContext();
        Utilities util = new Utilities();
        servletContext.setAttribute("userCache", util);
        AppProperties.getProperties();

        Timer timer = new Timer();
        // Schedule the job checker task to run every 5 minutes (adjust the interval as needed)
        timer.schedule(new Scheduler(), 0, 1 * 60 * 1000);
        timer.schedule(new CoverageTrigger(), 0, 5 * 60 * 1000);
    }

    /**
     * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
     */
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        // TODO Auto-generated method stub
        //response.getWriter().append("Served at: ").append(request.getContextPath());

        doPost(request, response);
    }

    /**
     * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
     */
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        // TODO Auto-generated method stub
        String action = request.getParameter("action");
        String randomkey = request.getParameter("token");
        JSONObject audit = new JSONObject();
        audit.put("action", action);
        MySQlConn msc = new MySQlConn(null);
        MySqlConn2 MSC = new MySqlConn2(msc);
        AuthCheck authCheck = new AuthCheck(MSC);

        ServletContext servletContext = getServletContext();
        Utilities userCache = (Utilities) servletContext.getAttribute("userCache");
        JSONObject userDetails = userCache.getUserFromCache(randomkey);

        try {
            if (!(action.equals("auth") || action.equals("registerUser") ||
                    action.equals("valkeygen") || action.equals("verifyvalkey") ||
                    action.equals("forgetPassword") || action.equals("updateRecordingCache") ||
                    action.equals("getCompanyNameFromURL") || action.equals("authResponse") ||
                    action.equals("confirmorder") || action.equals("verifyorder"))) {

                if (randomkey == null) {
                    JSONObject res = new JSONObject();
                    res.put("message", "fail");
                    audit.put("result", "fail");
                    response.getWriter().print(res);
                    return;
                } else {
                    if (userCache.getUserFromCache(randomkey) == null) {
                        JSONObject res = new JSONObject();
                        res.put("message", "fail");
                        audit.put("result", "fail");
                        response.getWriter().print(res);
                        return;
                    }
                }
                if (userDetails.size() == 0) {
                    JSONObject res = new JSONObject();
                    res.put("message", "fail");
                    audit.put("result", "fail");
                    response.getWriter().print(res);
                    return;
                }
                msc.setRandomKey(randomkey);
                msc.setUserDetails(userDetails);
                audit.put("userid", userDetails.get("uname"));
                audit.put("randomkey", randomkey);

                AuditQueuingHelper aqh = userCache.aqh;
                aqh.addToQueue(audit);
            }


            String companyIdStr = request.getParameter("companyid");
            int companyId = -1;
            if (companyIdStr != null && !companyIdStr.equals("undefined")) {
                companyId = new Integer(companyIdStr).intValue();
            }
            if (action.equals("dashboard")) {
                authCheck.isUserAuthorizedForCompany(companyId);
                JSONObject json = Utilities.getdashboard(companyId, msc, randomkey);
                response.getWriter().print(json);
            } else if(action.equals("validForWTVideo")){
                boolean isValid = authCheck.isUserAuthorizedForWTVideo((String) userDetails.get("uname"));
                response.getWriter().print(isValid);
            } else if (action.equals("latestResult")) {
                authCheck.isUserAuthorizedForCompany(companyId);
                JSONObject json = msc.getLatestResults(companyId);
                response.getWriter().print(json);
            } else if (action.equals("latestRecording")) {
//				authCheck.isUserAuthorizedForCompany(companyId);
                JSONObject json = userCache.getLatestRecordingFromCache((String) userDetails.get("uname"), msc);
                response.getWriter().print(json);
            } else if (action.equals("generatePomClasses")) {
                int page_id = Integer.parseInt(request.getParameter("pageId"));
                int prodid = Integer.parseInt(request.getParameter("productId"));
                int cid = Integer.parseInt(request.getParameter("companyid"));
                MSC.GenPomClassForTestcase(page_id, prodid, cid);
                response.getWriter().print("generated pom classes");
            } else if (action.equals("latestRecordingStatus")) {
                String tcid = request.getParameter("testcaseid");
                int tid = new Integer(tcid).intValue();
                authCheck.isUserAuthorizedForTC(tid);
                JSONObject json = msc.getLatestResultsByTCId(tid, companyId);
                response.getWriter().print(json);
            } else if (action.equals("search")) {
                String testcase = request.getParameter("testcaseid");
                int tcid = Integer.valueOf(testcase);
                authCheck.isUserAuthorizedForTC(tcid);
                JSONObject json = msc.getTestStepsByTestCaseID(tcid, false);
                String uname = (String) userDetails.get("uname");
                userCache.cacheLatestRecording(uname, json);
                response.getWriter().print(json);
            } else if (action.equals("execute") || action.equals("modalExecute") ||
                    action.equals("coverageExecute") || action.equals("analysis")) {
                MySQlConn msc2 = null;
                try {
                    msc2 = new MySQlConn(null);
                    msc2.setRandomKey(randomkey);
                    msc2.setUserDetails(userDetails);
                    String tcid = request.getParameter("testcaseid");
                    String tc = request.getParameter("testcase");
                    int tid = Integer.valueOf(tcid);
                    authCheck.isUserAuthorizedForTC(tid);
                    String browser = "Chrome";
                    if (request.getParameter("browsername") != null && !request.getParameter("browsername").equals("")) {
                        browser = (String) request.getParameter("browsername");
                    }
                    String envurl = (String) request.getParameter("envUrl");
                    String runonce = (String) request.getParameter("runonce");
                    String threshold = (String) request.getParameter("threshold");
                    String proxyurl = (String) request.getParameter("proxyurl");
                    String baselineImages = (String) request.getParameter("baseline");

                    int thresholdtime = -1;
                    if (threshold != null) {
                        thresholdtime = Integer.valueOf(threshold);
                    } else {
                        thresholdtime = msc.getThreshold(tid);
                    }
                    msc.updateTestCaseThreshold(tid, thresholdtime);
                    JSONObject jsonObj = msc.getTestStepsByTestCaseID(tid, true);
                    JSONObject testcase = (JSONObject) jsonObj.get("testcase");
                    envurl = (String) testcase.get("envurl");
                    userCache.cacheLatestRecording((String) userDetails.get("uname"), jsonObj);
                    Tester tester = new Tester();
                    tester.setTestCaseId(tid);
                    tester.setCompanyId(companyId);
                    tester.setBrowser(browser);
                    tester.setProxyUrl(proxyurl);
                    tester.setMSC(msc2);
                    tester.setParallelThreadsCount(userCache);
                    tester.setRunOnce(runonce);
                    tester.setRandomKey(randomkey);
                    tester.setEnvUrl(envurl);
                    tester.setTestCase(tc);

                    if (baselineImages != null && !baselineImages.equals("")) {
                        if (baselineImages.equals("true")) {
                            tester.setMode("baseline");
                        }
                    }

                    boolean screenshotFlag = false;
                    if (action.equals("coverageExecute")) {
                        screenshotFlag = true;
                        tester.setSSOnError(false);
                        tester.setScreenshotForAllEle(screenshotFlag);
                        tester.setHeadless(false);
                    }

                    JSONObject json = msc.getSearchByTCId(tid, companyId);

                    JSONObject emailData = new JSONObject();
                    JSONObject userDets = msc.getUserDetails();
                    emailData.put("name", (String) userDets.get("uname"));
                    emailData.put("recipient", (String) userDets.get("uname"));
                    emailData.put("subject", AppProperties.testcaseexecutionsubject);
                    emailData.put("template", "TestCaseExecution.html");
                    emailData.put("envurl", envurl);
                    EmailQueuingHelper eqh = userCache.eqh;
                    tester.setResultsForEmail(emailData, eqh);
                    if (action.equals("modalExecute")) {
                        String stepNum = request.getParameter("stepnum");
                        int sn = Integer.valueOf(stepNum);
                        String nextNum = request.getParameter("nextnum");
                        int nn = Integer.valueOf(nextNum);
                        String tcname = request.getParameter("testcasename");
                        tester.setStepNum(sn);
                        tester.setNextNum(nn);
                        tester.setCrxTest(true);
                        tester.setTillTC(tid);
                        tester.setTestCaseName(tcname);

                    } else if (action.equals("analysis")) {
                        //tester.setCrxTest(true);
                        String tcname = request.getParameter("testcase");
                        int analysistc = msc.checkAndWriteTestCaseToDB(tcname + "_Analysis",
                                "", 1, (String) userDets.get("uname"), "analysis",
                                Long.valueOf(randomkey), (int) json.get("Module"),
                                (int) json.get("width"), (int) json.get("height"));
                        //Utilities.copyFile(Utilities.getExtPropBackupFile(), AppProperties.crxloc + "//properties.file");
                        //Utilities.updateChromeExtensionProperties("analysisTC", String.valueOf(analysistc));
                        //Utilities.updateChromeExtensionProperties("analysiskey", randomkey);
                        //Utilities.updateChromeExtensionProperties("analysisname", (String) userDets.get("uname"));
                        tester.setMode(action);
                        tester.setAnalysisTC(analysistc);
                        tc = "Analysis";
                    }

                    int testCaseResultsId = msc.writeTestCaseStatusToDB(tid, "STARTED",
                            (String) userDetails.get("uname"), 0.0, browser, tc, companyId);
                    tester.setTestCaseresultsId(testCaseResultsId);
                    
                    String option = request.getParameter("option");
                    String tsidstr = request.getParameter("idtest_step");
                    int tsid = -1;
                    if(tsidstr != null && !tsidstr.equals("undefined") && !tsidstr.equals("")) {
                    	tsid = Integer.valueOf(tsidstr);
                    	JSONObject debugStep = new JSONObject();
                    	debugStep.put("option", option);
                    	debugStep.put("tsid", tsid);
                    	Utilities.debugCache.put(testCaseResultsId, debugStep);
                    	tester.setDebugMode(true);
                    }
                    
                    tester.start();
                    json = msc.getSearchByTCName(tc, companyId);
                    response.getWriter().print(json);
                } catch (Exception e) {
                    throw e;
                } finally {
//					msc2.closeDbConn();
                }
            } else if (action.equals("getDebugMessage")) {
            	String tcridstr = request.getParameter("tcrid");
            	int tcrid = -1;
            	String msg = "";
            	if(tcridstr != null && !tcridstr.equals("undefined") && !tcridstr.equals("")) {
            		tcrid = Integer.valueOf(tcridstr);
            		authCheck.isUserAuthorizedForTCR(tcrid);
            		msg = (String)Utilities.debugMsgCache.get(tcrid);
            	}
            	JSONObject json = new JSONObject();
            	json.put("debugmessage", msg);
            	response.getWriter().print(json);
            } else if (action.equals("getExecutionMessage")) {
            	String tcridstr = request.getParameter("tcrid");
            	int tcrid = -1;
            	String msg = "";
            	int testStepId = -1;
            	int stepnum = -1;
            	if(tcridstr != null && !tcridstr.equals("undefined") && !tcridstr.equals("")) {
            		tcrid = Integer.valueOf(tcridstr);
            		authCheck.isUserAuthorizedForTCR(tcrid);
            		msg = (String)Utilities.executionMsgCache.get(tcrid);
            		testStepId = (int)Utilities.executionMsgCache.get("testStepId");
            		stepnum = (int)Utilities.executionMsgCache.get("StepNumber");
            	}
            	JSONObject json = new JSONObject();
            	json.put("executionmessage", msg);
            	json.put("teststepId", testStepId);
            	json.put("stepnumber", stepnum);
            	response.getWriter().print(json);
            } else if (action.equals("getExecutionFlow")) {
            	String tcidstr = request.getParameter("tcid");
            	int tcid = -1;
            	String msg = "";
            	JSONArray executionFlow = new JSONArray();
            	if(tcidstr != null && !tcidstr.equals("undefined") && !tcidstr.equals("")) {
            		tcid = Integer.valueOf(tcidstr);
            		authCheck.isUserAuthorizedForTC(tcid);
            		executionFlow = msc.getExecutionFlow(tcid);
            	}
            	JSONObject json = new JSONObject();
            	json.put("executionFlow", executionFlow);
            	response.getWriter().print(json);
            } else if (action.equals("putDebugAction")) {
            	String tcridstr = request.getParameter("tcrid");
            	int tcrid = -1;
            	if(tcridstr != null && !tcridstr.equals("undefined") && !tcridstr.equals("")) {
            		tcrid = Integer.valueOf(tcridstr);
            		authCheck.isUserAuthorizedForTCR(tcrid);
            		String option = request.getParameter("option");
                    String tsidstr = request.getParameter("idtest_step");
                    int tsid = -1;
                    if(tsidstr != null && !tsidstr.equals("undefined") && !tsidstr.equals("")) {
                    	tsid = Integer.valueOf(tsidstr);
                    	JSONObject debugStep = new JSONObject();
                    	debugStep.put("option", option);
                    	debugStep.put("tsid", tsid);
                    	Utilities.debugCache.put(tcrid, debugStep);
                    }
            	}
            		
            } else if (action.equals("deletestep")) {
                String tsid = request.getParameter("teststepid");
                int tid = Integer.valueOf(tsid);
                String tcidstr = request.getParameter("testcaseid");
                int tcid = Integer.valueOf(tcidstr);
                authCheck.isUserAuthorizedForTC(tcid);
                msc.deleteTestStep(tid);
                JSONObject json = msc.getTestStepsByTestCaseID(tcid, false);
                userCache.cacheLatestRecording((String) userDetails.get("uname"), json);
                response.getWriter().print(json);
            } else if (action.equals("updateFlow")) {
                String tsid = request.getParameter("teststepid");
                int tid = new Integer(tsid).intValue();
                String flow = request.getParameter("flow");
                authCheck.isUserAuthorizedForTS(tid);
                msc.updateTestStepFlow(tid, flow);
                String tcIdStr = request.getParameter("testcaseid");
                JSONObject json = null;
                if (tcIdStr != null && !tcIdStr.equals("undefined")) {
                    int tcId = Integer.parseInt(tcIdStr);
                    json = msc.getTestStepsByTestCaseID(tcId, false);
                    String uname = (String) userDetails.get("uname");
                    userCache.cacheLatestRecording(uname, json);
                } else {
                    json = userCache.getLatestRecordingFromCache((String) userDetails.get("uname"), msc);
                }
                response.getWriter().print(json);
            } else if (action.equals("updateFindAndAssign")) {
            	String tc = request.getParameter("testCaseId");
            	int tcid = new Integer(tc).intValue();
                String tsid = request.getParameter("teststepid");
                String androidUIAutomator = request.getParameter("android_uiautomator");
                String xpath = request.getParameter("xpath");
                String idStrategy = request.getParameter("id_strategy");

                int tid = Integer.parseInt(tsid);
                authCheck.isUserAuthorizedForTC(tcid);
                try {
                    msc.updateFindAndAssignData(tid, action, androidUIAutomator, xpath, idStrategy);
                    JSONObject json = new JSONObject();
                    json.put("message", "Update Successful");
                    response.getWriter().print(json);
                } catch (Exception e) {
                    e.printStackTrace();
                    JSONObject errorJson = new JSONObject();
                    errorJson.put("message", "Update Failed: " + e.getMessage());
                    response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                    response.getWriter().print(errorJson);
                }
            } else if (action.equals("updateSelect")) {
                String tsid = request.getParameter("teststepid");
                int tid = new Integer(tsid).intValue();
                authCheck.isUserAuthorizedForTS(tid);
                String select = request.getParameter("select");
                msc.updateSelectOptions(tid, select);
                String tcIdStr = request.getParameter("testcaseid");
                JSONObject json = null;
                if (tcIdStr != null && !tcIdStr.equals("undefined")) {
                    int tcId = Integer.parseInt(tcIdStr);
                    json = msc.getTestStepsByTestCaseID(tcId, false);
                    String uname = (String) userDetails.get("uname");
                    userCache.cacheLatestRecording(uname, json);
                } else {
                    json = userCache.getLatestRecordingFromCache((String) userDetails.get("uname"), msc);
                }
                response.getWriter().print(json);
            } else if (action.equals("updateCheckBox") || action.equals("updateValidation")) {
                String tsid = request.getParameter("teststepid");
                int tid = Integer.valueOf(tsid);
                authCheck.isUserAuthorizedForTS(tid);
                String select = request.getParameter("select");
                msc.updateSelectOptions(tid, select);
                String tcIdStr = request.getParameter("testcaseid");
                JSONObject json = null;
                if (tcIdStr != null && !tcIdStr.equals("undefined")) {
                    int tcId = Integer.parseInt(tcIdStr);
                    json = msc.getTestStepsByTestCaseID(tcId, false);
                    String uname = (String) userDetails.get("uname");
                    userCache.cacheLatestRecording(uname, json);
                } else {
                    json = userCache.getLatestRecordingFromCache((String) userDetails.get("uname"), msc);
                }
                response.getWriter().print(json);
            } else if (action.equals("updateBefore")) {
                String tsid = request.getParameter("teststepid");
                int tid = Integer.valueOf(tsid);
                authCheck.isUserAuthorizedForTS(tid);
                String before = request.getParameter("before");
                msc.updateTestStepBefore(tid, before);
                String tcIdStr = request.getParameter("testcaseid");
                JSONObject json = null;
                if (tcIdStr != null && !tcIdStr.equals("undefined")) {
                    int tcId = Integer.parseInt(tcIdStr);
                    json = msc.getTestStepsByTestCaseID(tcId, false);
                    String uname = (String) userDetails.get("uname");
                    userCache.cacheLatestRecording(uname, json);
                } else {
                    json = userCache.getLatestRecordingFromCache((String) userDetails.get("uname"), msc);
                }
                response.getWriter().print(json);
            } else if (action.equals("updateWaitTime")) {
                String tsid = request.getParameter("teststepid");
                int tid = Integer.valueOf(tsid);
                authCheck.isUserAuthorizedForTS(tid);
                String wt = request.getParameter("waittime");
                int wti = Integer.valueOf(wt);
                msc.updateTestStepWaitTime(tid, wti);
                String tcIdStr = request.getParameter("testcaseid");
                JSONObject json = null;
                if (tcIdStr != null && !tcIdStr.equals("undefined")) {
                    int tcId = Integer.parseInt(tcIdStr);
                    json = msc.getTestStepsByTestCaseID(tcId, false);
                    String uname = (String) userDetails.get("uname");
                    userCache.cacheLatestRecording(uname, json);
                } else {
                    json = userCache.getLatestRecordingFromCache((String) userDetails.get("uname"), msc);
                }
                response.getWriter().print(json);
            } else if (action.equals("reset")) {
            	String tsid = request.getParameter("teststepid");
            	int tid = Integer.valueOf(tsid);
            	
            	msc.removeOuterHTML(tid);
            	msc.removeRecordedOuterHTML(tid);
            	
            	JSONObject json = new JSONObject();
            	json.put("message", "Reset Successful");
            	response.getWriter().print(json);
            	
            } else if (action.equals("updateTestData")) {
                String tsid = request.getParameter("teststepid");
                int tid = Integer.valueOf(tsid);

                String tcidstr = request.getParameter("testCaseId");
                int tcid = Integer.valueOf(tcidstr);
                authCheck.isUserAuthorizedForTC(tcid);

                String testdatasource = request.getParameter("testdata");

                if (testdatasource.equals("GenData")) {
                    String gendataType = request.getParameter("testdataType");
                    String testdatalen = request.getParameter("testdatalen");
                    int inttestdatalen = 0;
                    String dateFormat = request.getParameter("testdateformat");
                    String dateOffset = request.getParameter("testdateoffset");
                    if (gendataType != null && !gendataType.equals(AppProperties.TODAY)) {
                        if (!testdatalen.equals("")) {
                            inttestdatalen = Integer.valueOf(testdatalen);
                        }
                    }
                    msc.updateTestStepGenData(tid, testdatasource, gendataType,
                            inttestdatalen, dateFormat, dateOffset);
                } else if (testdatasource.equals("SameRow")) {
                    String gendataType = request.getParameter("elementType");
                    String testdatalen = request.getParameter("elementIndex");
                    int inttestdatalen = Integer.valueOf(testdatalen);
                    String strategy = request.getParameter("strategy");
                    if (strategy != null) {
                        msc.updateTestStepFindElementByData(tid, testdatasource, gendataType,
                                inttestdatalen, strategy);
                    } else {
                        msc.updateTestStepFindElementByData(tid, testdatasource, gendataType,
                                inttestdatalen, "Test Data");
                    }
                } else if (testdatasource.equals("SameRowElement")) {
                    String strategy = request.getParameter("strategy");
                    if (strategy != null) {
                        msc.updateTestStepFindElementByPrevElement(tid, testdatasource, strategy);
                    } else {
                        msc.updateTestStepFindElementByPrevElement(tid, testdatasource, "Previous");
                    }
                } else if (testdatasource.equals("UseVariableFromTheScenario")) {
                    String testdatavalue = request.getParameter("testdatavalue");
                    msc.updateTestStepScenarioVariable(tid, testdatasource, testdatavalue);
                } else if (testdatasource.equals("DependentonOtherTestScenario")) {
                    String testdatavalue = request.getParameter("testdatavalue");
                    String deptcidstr = request.getParameter("testCaseIdNew");
                    int deptcid = Integer.valueOf(deptcidstr);
                    msc.updateTestStepOtherScenarioVariable(tid, testdatasource, testdatavalue, deptcid);
                } else if (testdatasource.equals("AsRecorded")) {
                    String strategy = request.getParameter("strategy");
                    if (strategy != null && strategy.equals("X-Path")) {
                        String xpath = request.getParameter("xpath");
                        msc.updateXpathAsStrategy(tid, xpath, testdatasource, strategy);
                    } else if (strategy != null && strategy.equalsIgnoreCase("XY")) {
                        String xposs = request.getParameter("xpos");
                        int xpos = Integer.valueOf(xposs);
                        String yposs = request.getParameter("ypos");
                        int ypos = Integer.valueOf(yposs);
                        msc.updateStrategyForXY(tid, strategy, xpos, ypos);
                    } else {
                        msc.updateTestStepAsRecorded(tid, testdatasource);
                    }
                } else if (testdatasource.equals("ChangeTestData")) {
                    String testdatavalue = request.getParameter("testdatavalue");
                    msc.updateTestStepChangedData(tid, testdatasource, testdatavalue);
                } else if (testdatasource.equals("IsAVar")) {
                    msc.updateTestStepIsAVar(tid, testdatasource);
                } else if (testdatasource.equals("D0")) {
                    msc.updateTestStepD0(tid, testdatasource);
                } else if (testdatasource.equals("DNext") || testdatasource.equals("DBelow")) {
                    msc.updateTestStepDNEXT(tid, testdatasource);
                } else if (testdatasource.equals("FromFile")) {
                    String fileName = request.getParameter("name");
                    if (fileName.toLowerCase().endsWith(".json") || fileName.toLowerCase().endsWith(".xml")) {
                        String filter = request.getParameter("filter");
                        String scope = request.getParameter("scope");
                        String endRange = request.getParameter("endRange");
                        InputStream is = request.getInputStream();
                        String Name = Utilities.getInputFilePath(companyId, tcid, fileName);
                        File file = new File(Name);
                        copyInputStreamUsingFiles(is, Name);
                        msc.updateTestStepFromFile(tid, testdatasource, fileName, filter, scope, endRange);
                    } else if (fileName.toLowerCase().endsWith(".xlsx") || fileName.toLowerCase().endsWith(".xls") ||
                            fileName.toLowerCase().endsWith(".csv")) {
                        String sheet = request.getParameter("sheetId");
                        String cell = request.getParameter("cell");
                        String fileField = sheet + AppProperties.testdatadelimiter + cell;
                        String scope = request.getParameter("scope");
                        String endRange = request.getParameter("endRange");
                        InputStream is = request.getInputStream();
                        String Name = Utilities.getInputFilePath(companyId, tcid, fileName);
                        copyInputStreamUsingFiles(is, Name);
                        msc.updateTestStepFromFile(tid, testdatasource, fileName, fileField, scope, endRange);
                    } else {
                        InputStream is = request.getInputStream();
                        String Name = Utilities.getInputFilePath(companyId, tcid, fileName);
                        File file = new File(Name);
                        copyInputStreamUsingFiles(is, Name);
                        msc.updateTestStepFromFile(tid, testdatasource, fileName);
                    }
                } else if (testdatasource.equals("FromReferenceFile")) {
                    String fileName = request.getParameter("name");
                    if (fileName.toLowerCase().endsWith(".json")) {
                        String filter = request.getParameter("filter");
                        String scope = request.getParameter("scope");
                        String endRange = request.getParameter("endRange");
                        InputStream is = request.getInputStream();
                        String Name = Utilities.getInputFilePath(companyId, tcid, fileName);
                        File file = new File(Name);
                        copyInputStreamUsingFiles(is, Name);
                        msc.updatedbfortestdata(tid, testdatasource, fileName, filter, scope, endRange);
                    } else if (fileName.toLowerCase().endsWith(".xlsx") || fileName.toLowerCase().endsWith(".csv")) {
                        String sheet = request.getParameter("sheetId");
                        String cell = request.getParameter("cell");
                        String fileField = sheet + AppProperties.testdatadelimiter + cell;
                        String scope = request.getParameter("scope");
                        String endRange = request.getParameter("endRange");
                        InputStream is = request.getInputStream();
                        String Name = Utilities.getInputFilePath(companyId, tcid, fileName);
                        File file = new File(Name);
                        copyInputStreamUsingFiles(is, Name);
                        msc.updatedbfortestdata(tid, testdatasource, fileName, fileField, scope, endRange);
                    } else {
                        InputStream is = request.getInputStream();
                        String Name = Utilities.getInputFilePath(companyId, tcid, fileName);
                        File file = new File(Name);
                        copyInputStreamUsingFiles(is, Name);
                        msc.updatedbfortestdata(tid, testdatasource, fileName);
                    }
                } else if (testdatasource.equalsIgnoreCase("GoogleSheet")) {
                    String sheetId = request.getParameter("sheetId");
                    String sheet = request.getParameter("sheet");
                    String cell = request.getParameter("cell");
                    String fileField = sheet + AppProperties.testdatadelimiter + cell;
                    msc.updateTestStepFromGoogleSheet(tid, testdatasource, sheetId, fileField);
                } else if (testdatasource.equalsIgnoreCase("FromApi")) {
                    String apiName = request.getParameter("fieldName");
                    int apiid = Integer.valueOf(request.getParameter("apiid"));
                    String encodedApiParam = request.getParameter("apiParam");
                    String encodedApiQuery = request.getParameter("apiQuery");
                    String apiParam = URLDecoder.decode(encodedApiParam, "UTF-8");
                    String apiQuery = URLDecoder.decode(encodedApiQuery, "UTF-8");
                    msc.updateTestStepFromApi(tid, testdatasource, apiName, apiid, apiParam, apiQuery);
                } else if (testdatasource.equalsIgnoreCase("FromDB")) {
                    String DBQuery = request.getParameter("dbquery");
                    if (DBQuery.matches("^(?i)SELECT.*") && DBQuery.matches("(?i).*\\bWHERE\\b.*")) {
                        msc.updateTestStepFromDb(tid, testdatasource, DBQuery);
                    } else if (DBQuery.startsWith("{")) {
                        JSONParser parser = new JSONParser();
                        JSONObject dbquery = (JSONObject) parser.parse(DBQuery);
                        if (dbquery.get("collectionName") != null &&
                                dbquery.get("query") != null) {
                            msc.updateTestStepFromDb(tid, testdatasource, DBQuery);
                        }
                    } else {
                        response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                    }
                } else if (testdatasource.equalsIgnoreCase("FromEmail")) {
                    String customerEmail = request.getParameter("customerEmail");
                    String customerPassword = request.getParameter("customerPassword");
                    String EmailSelectionCriteria = request.getParameter("EmailSelectionCriteria");
                    String EmailFilter = request.getParameter("EmailFilter");
                    msc.updateTestStepFromEmail(tid, testdatasource, customerEmail, customerPassword,
                            EmailSelectionCriteria, EmailFilter);
                } else if (testdatasource.equalsIgnoreCase("fromSMS")) {
                    String regex = request.getParameter("regex");
                    regex = URLDecoder.decode(regex, "UTF-8");
                    msc.updateTestStepFromSMS(tid, testdatasource, regex);
                } else if (testdatasource.equalsIgnoreCase("FromOutlook")) {
                    String EmailSelectionCriteria = request.getParameter("EmailSelectionCriteria");
                    String EmailFilter = request.getParameter("EmailFilter");
                    msc.updateTestStepFromOutlook(tid, testdatasource, EmailSelectionCriteria,
                            EmailFilter);
                } else if (testdatasource.equalsIgnoreCase("DynamicText")) {
                    msc.updateTestDataDynamicText(tid, testdatasource);
                } else if (testdatasource.equalsIgnoreCase("Search")) {
                    String actualData = (String) request.getParameter("actualdata");
                    String strategy = (String) request.getParameter("strategy");
                    msc.searchForTextTestData(tid, actualData, testdatasource, strategy);
                } else if (testdatasource.equalsIgnoreCase("SendEmail")) {
                    String customerEmail = request.getParameter("customerEmail");
                    String customerPassword = request.getParameter("customerPassword");
                    String toAddress = request.getParameter("toAddress");
                    String subject = request.getParameter("subject");
                    String content = request.getParameter("content");
                    msc.updateTestStepSendEmail(tid, testdatasource, customerEmail,
                            customerPassword, toAddress, subject, content);
                }
                String tcIdStr = request.getParameter("testCaseId");
                JSONObject json = null;
                if (tcIdStr != null && !tcIdStr.equals("undefined")) {
                    int tcId = Integer.parseInt(tcidstr);
                    json = msc.getTestStepsByTestCaseID(tcId, false);
                    String uname = (String) userDetails.get("uname");
                    userCache.cacheLatestRecording(uname, json);
                } else {
                    json = userCache.getLatestRecordingFromCache((String) userDetails.get("uname"), msc);
                }
                response.getWriter().print(json);
            } else if (action.contentEquals("getRecordedTestDataList")) {
                String tsid = request.getParameter("teststepid");
                String tcidstr = request.getParameter("testcaseid");
                if (!tsid.equals("undefined") && !tcidstr.equals("undefined")) {
                    int tid = Integer.parseInt(tsid);
                    int tcid = Integer.parseInt(tcidstr);
                    authCheck.isUserAuthorizedForTC(tcid);
                    JSONObject json = msc.getRecordedTestDataList(tcid);
                    response.getWriter().print(json);
                } else {
                    response.getWriter().print(new JSONObject());
                }
            } else if (action.contentEquals("validate")) {
                String tsid = request.getParameter("teststepid");
                int tid = Integer.parseInt(tsid);
                String tcidstr = request.getParameter("testCaseId");
                int testCaseId = Integer.parseInt(tcidstr);
                authCheck.isUserAuthorizedForTC(testCaseId);

                String validateType = request.getParameter("valdtype");

                if (validateType.equals("OnScreen")) {
                    msc.validateFromOnScreen(tid, validateType);
                } else if (validateType.equals("ChangeTestData")) {
                    String testdatavalue = request.getParameter("testdatavalue");
                    msc.validateTestStepChangedData(tid, validateType, testdatavalue);
                } else if (validateType.equals("IsAVar")) {
                    msc.updateTestStepIsAVar(tid, validateType);
                } else if (validateType.equals("InFile")) {
                    String fileName = request.getParameter("name");
                    if (fileName.toLowerCase().endsWith(".json")) {
                        String filter = request.getParameter("filter");
                        String scope = request.getParameter("scope");
                        String endRange = request.getParameter("endRange");
                        InputStream is = request.getInputStream();
                        String Name = Utilities.getInputFilePath(companyId, testCaseId, fileName);
                        File file = new File(Name);
                        copyInputStreamToFile(is, file);
                        msc.validateFromFile(tid, validateType, fileName, filter, scope, endRange);
                    } else if (fileName.toLowerCase().endsWith(".xlsx") || fileName.toLowerCase().endsWith(".csv")) {
                        String sheet = request.getParameter("sheetId");
                        String cell = request.getParameter("cell");
                        String fileField = sheet + AppProperties.validatedelimiter + cell;
                        String scope = request.getParameter("scope");
                        String endRange = request.getParameter("endRange");
                        InputStream is = request.getInputStream();
                        String Name = Utilities.getInputFilePath(companyId, testCaseId, fileName);
                        File file = new File(Name);
                        copyInputStreamToFile(is, file);
                        msc.validateFromFile(tid, validateType, fileName, fileField, scope, endRange);
                    } else {
                        InputStream is = request.getInputStream();
                        String Name = Utilities.getInputFilePath(companyId, testCaseId, fileName);
                        File file = new File(Name);
                        copyInputStreamToFile(is, file);
                        msc.validateFromFile(tid, validateType, fileName);
                    }
                } else if (validateType.equals("InDownloadedFile")) {
                    String fileName = request.getParameter("name");
                    String sheetId = request.getParameter("sheetId");
                    String cell = request.getParameter("cell");
                    String fieldName = sheetId + AppProperties.delimiter + cell;
                    msc.validateFromDownloadedFile(tid, validateType, fileName, fieldName);
                } else if (validateType.equals("InStreamedFile")) {
                    String fileName = request.getParameter("name");
                    msc.validateFromStreamedPDFFile(tid, validateType, fileName);
                } else if (validateType.equals("InRefrenceFile")) {
                    String fileName = request.getParameter("name");
                    if (fileName.toLowerCase().endsWith(".json")) {
                        String filter = request.getParameter("filter");
                        String scope = request.getParameter("scope");
                        String endRange = request.getParameter("endRange");
                        InputStream is = request.getInputStream();
                        String Name = Utilities.getInputFilePath(companyId, testCaseId, fileName);
                        File file = new File(Name);
                        copyInputStreamToFile(is, file);
                        msc.validateFromRefrenceFile(tid, validateType, fileName, filter, scope, endRange);
                    } else if (fileName.toLowerCase().endsWith(".xlsx") || fileName.toLowerCase().endsWith(".csv")) {
                        String sheet = request.getParameter("sheetId");
                        String cell = request.getParameter("cell");
                        String fileField = sheet + AppProperties.validatedelimiter + cell;
                        String scope = request.getParameter("scope");
                        String endRange = request.getParameter("endRange");
                        InputStream is = request.getInputStream();
                        String Name = Utilities.getInputFilePath(companyId, testCaseId, fileName);
                        File file = new File(Name);
                        copyInputStreamToFile(is, file);
                        msc.validateFromRefrenceFile(tid, validateType, fileName, fileField, scope, endRange);
                    } else {
                        InputStream is = request.getInputStream();
                        String Name = Utilities.getInputFilePath(companyId, testCaseId, fileName);
                        File file = new File(Name);
                        copyInputStreamToFile(is, file);
                        msc.validateFromRefrenceFile(tid, validateType, fileName);
                    }
                } else if (validateType.equals("InGoogleSheet")) {
                    String sheetId = request.getParameter("sheetId");
                    String sheet = request.getParameter("sheet");
                    String cell = request.getParameter("cell");
                    String fileField = sheet + AppProperties.validatedelimiter + cell;
                    msc.validateFromSheet(tid, validateType, sheetId, fileField);
                } else if (validateType.equals("InApi")) {
                    String apiName = request.getParameter("fieldName");
                    int apiid = Integer.valueOf(request.getParameter("apiid"));
                    String apiParam = request.getParameter("apiParam");
                    String apiQuery = request.getParameter("apiQuery");
                    msc.validateTestStepFromApi(tid, validateType, apiName, apiid, apiParam, apiQuery);
                } else if (validateType.equals("DB")) {
                    String DBQuery = request.getParameter("dbquery");
                    if (DBQuery.matches("^(?i)SELECT.*") && DBQuery.matches("(?i).*\\bWHERE\\b.*")) {
                        msc.validateFromDB(tid, validateType, DBQuery);
                    } else {
                        response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                    }
                } else if (validateType.equals("DynamicText")) {
                    msc.validateFromDynamicText(tid, validateType);
                } else if (validateType.equals("Regex")) {
                    String regex = (String) request.getParameter("regex");
                    regex = URLDecoder.decode(regex, "UTF-8");
                    msc.validateByRegex(tid, validateType, regex);
                } else if (validateType.equals("Date")) {
                    String date = (String) request.getParameter("date");
                    String dateFormat = (String) request.getParameter("dateFormat");
                    msc.validateByDate(tid, validateType, date, dateFormat);
                } else if (validateType.equals("UseVariableFromTheScenario")) {
                    String var = (String) request.getParameter("useVar");
                    msc.validateByVar(tid, validateType, var);
                } else if (validateType.equals("DependentonOtherTestScenario")) {
                    String var = (String) request.getParameter("useVar");
                    String dId = (String) request.getParameter("dependantTC");
                    int depId = Integer.valueOf(dId);
                    msc.validateByDependence(tid, validateType, var, depId);
                } else if (validateType.equals("Search")) {
                    String testData = (String) request.getParameter("testdata");
                    String strategy = (String) request.getParameter("startegy");
                    msc.searchForTextValidate(tid, validateType, testData, strategy);
                } else if (validateType.equals("FromEmail")) {
                    String customerEmail = request.getParameter("customerEmail");
                    String customerPassword = request.getParameter("customerPassword");
                    String EmailSelectionCriteria = request.getParameter("EmailSelectionCriteria");
                    String EmailFilter = request.getParameter("EmailFilter");
                    msc.validateTestStepFromEmail(tid, validateType, customerEmail, customerPassword,
                            EmailSelectionCriteria, EmailFilter);
                } else if (validateType.equals("FromOutlook")) {
                    String EmailSelectionCriteria = request.getParameter("EmailSelectionCriteria");
                    String EmailFilter = request.getParameter("EmailFilter");
                    msc.validateTestStepFromOutlook(tid, validateType, EmailSelectionCriteria, EmailFilter);
                } else if (validateType.equals("FromOutlook")) {
                    String EmailSelectionCriteria = request.getParameter("EmailSelectionCriteria");
                    String EmailFilter = request.getParameter("EmailFilter");
                    msc.validateTestStepFromOutlook(tid, validateType, EmailSelectionCriteria, EmailFilter);
                }
                String tcIdStr = request.getParameter("testCaseId");
                JSONObject json = null;
                if (tcIdStr != null && !tcIdStr.equals("undefined")) {
                    int tcId = new Integer(tcIdStr).intValue();
                    json = msc.getTestStepsByTestCaseID(tcId, false);
                    String uname = (String) userDetails.get("uname");
                    userCache.cacheLatestRecording(uname, json);
                } else {
                    json = userCache.getLatestRecordingFromCache((String) userDetails.get("uname"), msc);
                }
                response.getWriter().print(json);
            } else if (action.equals("searchProdDropDown")) {
                authCheck.isUserAuthorizedForCompany(companyId);
                JSONArray json = msc.getProducts(companyId);
                response.getWriter().print(json);
            } else if (action.equals("searchModDropDown")) {
                String prodid = request.getParameter("prodid");
                if (prodid.equals("0")) {
                	response.setStatus(HttpServletResponse.SC_BAD_REQUEST);  // Set the status code to 400
                    response.getWriter().print("{\"error\": \"Invalid prodid value of 0\"}");  // Send an error message
//                    response.getWriter().print(new JSONArray());
                } else {
                    int pid = -1;
                    if (prodid != null && !prodid.equals("") && !prodid.equals("undefined")) {
                        pid = Integer.valueOf(prodid);
                        authCheck.isUserAuthorizedForProd(pid);
                        JSONArray json = msc.getModulesFromProduct(pid);
                        response.getWriter().print(json);
                    } else {
                    	response.setStatus(HttpServletResponse.SC_BAD_REQUEST);  // Set the status code to 400
                        response.getWriter().print("{\"error\": \"Invalid prodid value of either null, blank or undefined\"}");  // Send an error message
//                        response.getWriter().print(new JSONArray());
                    }
                }
            } else if (action.equals("checkmodule")) {
                String modid = request.getParameter("modid");
                int mid = Integer.valueOf(modid);
                authCheck.isUserAuthorizedForMods(mid);
                JSONObject json = msc.checkModuleDisability(mid, (int) userDetails.get("usertype"));
                response.getWriter().print(json);
            } else if (action.equals("searchTCDropDown")) {
                String modid = request.getParameter("modid");
                int mid = Integer.valueOf(modid);
                authCheck.isUserAuthorizedForMods(mid);
                String pageNum = request.getParameter("pnum");
                int pnum;
                if (pageNum == null || pageNum.equals("") || pageNum.equals("undefined")) {
                    pnum = 1;
                } else {
                    pnum = Integer.valueOf(pageNum);
                }
                String limitStr = AppProperties.listlimit;
                int limit = Integer.parseInt(limitStr);
                JSONArray json = msc.getTestCaseFromModule(mid, limit, pnum);
                response.getWriter().print(json);
            } else if (action.equals("searchTCSearch")) {
                String modid = request.getParameter("modid");
                int mid = -1;
                if (modid != null && !modid.equals("") &&
                        !modid.equals("undefined") && !modid.equals("0")) {
                    mid = Integer.valueOf(modid);
                    authCheck.isUserAuthorizedForMods(mid);
                    String key = request.getParameter("key");
                    String pageNum = request.getParameter("pnum");
                    int pnum;
                    if (pageNum == null || pageNum.equals("") || pageNum.equals("undefined")) {
                        pnum = 1;
                    } else {
                        pnum = Integer.valueOf(pageNum);
                    }
                    JSONArray json = msc.getTestCaseFromSearch(mid, key, AppProperties.listlimitInt, pnum);
                    response.getWriter().print(json);
                } else {
                	response.setStatus(HttpServletResponse.SC_BAD_REQUEST);  // Set the status code to 400
                    response.getWriter().print("{\"error\": \"Invalid modid value of null or blank or undefined\"}");  // Send an error message
//                    response.getWriter().print(new JSONArray());
                }
            } else if (action.equals("getTCbyTags")) {
                String prodid = request.getParameter("prodid");
                if (prodid == null) {
                    prodid = request.getParameter("product");
                }
                int pid = Integer.parseInt(prodid);
                authCheck.isUserAuthorizedForProd(pid);
                String searchTag = request.getParameter("key");
                String pageNum = request.getParameter("pnum");

                String repo = request.getParameter("repo");
                String fromDate = request.getParameter("fromdate");
                String toDate = request.getParameter("toDate");
                int pnum;
                if (pageNum == null || pageNum.equals("") || pageNum.equals("undefined")) {
                    pnum = 1;
                } else {
                    pnum = Integer.parseInt(pageNum);
                }
                JSONArray json = null;
                if (searchTag == null) {
                    json = MSC.getAllTCforTagFromGitHub(pid, repo, msc,
                            fromDate, toDate);
                } else if (searchTag.startsWith("repo:") || searchTag.startsWith("after:")) {
                    json = MSC.getAllTCforTagFromGitHub(pid, searchTag, msc);
                } else {
                    List<String> tags = Utilities.getTagsFromDesc(searchTag);
                    json = MSC.getAllTCforTag(pid, tags, msc);
                }
                response.getWriter().print(json);
            } else if (action.equals("getTCfromComments")) {
                String prodid = request.getParameter("product");
                int pid = Integer.parseInt(prodid);
                authCheck.isUserAuthorizedForProd(pid);
                BufferedReader reader = request.getReader();
                JSONParser parser = new JSONParser();
                JSONArray jsonstr = (JSONArray) parser.parse(reader);
                List<String> tags = new ArrayList<String>();

                for (int i = 0; i < jsonstr.size(); i++) {
                    String desc = (String) jsonstr.get(i);
                    List<String> tempTags = Utilities.getTagsFromDesc(desc);
                    tags = MSC.mergeAndEliminateDuplicates(tags, tempTags);
                }

                JSONArray tc = MSC.getAllTCforTag(pid, tags, msc);

                JSONArray json = tc;
                String suiteOption = request.getParameter("suiteOption");
                if (suiteOption != null && !suiteOption.equals("")) {
                    if (suiteOption.equals("NewTestSuite")) {
                        String tsname = request.getParameter("newTestSuite");
                        int tsid = msc.createTestSuiteGetId(tsname, pid);

                        json = msc.addTestSuiteCase(tc, tsid, companyId);
                        response.getWriter().print(json);
                    } else if (suiteOption.equals("ExistingTestSuite")) {
                        String tsidstr = request.getParameter("suiteId");
                        int tsid = Integer.valueOf(tsidstr);
                        json = msc.addTestSuiteCase(tc, tsid, companyId);
                    }
                }
                response.getWriter().print(json);
            } else if (action.equals("searchInSuite")) {
                String suiteid = request.getParameter("suiteid");
                int sid = Integer.valueOf(suiteid);
                authCheck.isUserAuthorizedForTSuite(sid);
                String key = request.getParameter("key");
                String pageNum = request.getParameter("pnum");
                int pnum;
                if (pageNum == null || pageNum.equals("") || pageNum.equals("undefined")) {
                    pnum = 1;
                } else {
                    pnum = Integer.valueOf(pageNum);
                }
                JSONArray json = msc.searchInSuite(sid, key, pnum);
                response.getWriter().print(json);
            } else if (action.equals("searchInAllSuite")) {
                String prodid = request.getParameter("prodid");
                int pid = Integer.valueOf(prodid);
                authCheck.isUserAuthorizedForProd(pid);
                String key = request.getParameter("key");
                String pageNum = request.getParameter("pnum");
                int pnum;
                if (pageNum == null || pageNum.equals("") || pageNum.equals("undefined")) {
                    pnum = 1;
                } else {
                    pnum = new Integer(pageNum).intValue();
                }
                JSONArray json = msc.getTestSuiteFromSearch(pid, key, pnum);
                response.getWriter().print(json);
            } else if (action.equals("searchTSDropDown")) {
                String testcaseid = request.getParameter("testcaseid");
                int tcid = Integer.valueOf(testcaseid);
                authCheck.isUserAuthorizedForTC(tcid);
                JSONArray json = msc.getTestStepFromTestCase(tcid, false);
                response.getWriter().print(json);
            } else if (action.equals("getTestSuites")) {
                String productid = request.getParameter("productid");
                if(productid ==  null || productid.equals("") || productid.equals("null")) {
                	response.setStatus(HttpServletResponse.SC_BAD_REQUEST);  // Set the status code to 400
                    response.getWriter().print("{\"error\": \"Invalid product id value of null or blank\"}");  // Send an error message
                    JSONArray json = new JSONArray();
                    response.getWriter().print(json);
                    return;
                }
                int pid = Integer.valueOf(productid);
                authCheck.isUserAuthorizedForProd(pid);
                JSONArray json = msc.getTestSuites(pid, 1);
                response.getWriter().print(json);
            } else if (action.equals("createTestSuite")) {
            	String companyid = request.getParameter("companyid");
                String productid = request.getParameter("productid");
                int cid = Integer.valueOf(companyId);
                int pid = Integer.valueOf(productid);
                authCheck.isUserAuthorizedForProd(pid);
                String tsname = request.getParameter("tsuname");
                JSONArray json = msc.createTestSuite(tsname, pid, cid);
                response.getWriter().print(json);
            } else if (action.equals("addToTestSuites") || action.equals("deleteTCfromTSuite")) {
                BufferedReader reader = request.getReader();
                JSONParser parser = new JSONParser();
                JSONArray jsonstr = (JSONArray) parser.parse(reader);
                String testSuiteId = request.getParameter("idtest_suite");
                int tsuid = Integer.valueOf(testSuiteId);
                authCheck.isUserAuthorizedForTSuite(tsuid);
                String pageNum = request.getParameter("pnum");
                int pnum;
                if (pageNum == null || pageNum.equals("") || pageNum.equals("undefined")) {
                    pnum = 1;
                } else {
                    pnum = Integer.valueOf(pageNum);
                }

                if (action.equals("deleteTCfromTSuite")) {
                    String indexStr = request.getParameter("index");
                    int index = Integer.valueOf(indexStr);
                    JSONArray jsonArr = msc.deleteTCfromTSuite(jsonstr, tsuid, pnum, index);
                    response.getWriter().print(jsonArr);
                } else {
                    JSONArray jsonArr = msc.addTestSuiteCase(jsonstr, tsuid, pnum);
                    response.getWriter().print(jsonArr);
                }
            } else if (action.equals("moveTestCases")) {
                BufferedReader reader = request.getReader();
                JSONParser parser = new JSONParser();
                JSONArray jsonstr = (JSONArray) parser.parse(reader);
                String modIdStr = request.getParameter("modid");
                int modid = Integer.valueOf(modIdStr);
                authCheck.isUserAuthorizedForMods(modid);
                msc.moveTestCases(jsonstr, modid);
                response.getWriter().print("Move was successful");
            } else if (action.equals("gettestsuiteonpage")) {
                int tsuid = Integer.parseInt(request.getParameter("idtest_suite"));
                authCheck.isUserAuthorizedForTSuite(tsuid);
                int pnum = Integer.parseInt(request.getParameter("pnum"));
                String limitStr = AppProperties.listlimit;
                int limit = Integer.parseInt(limitStr);
                JSONArray list = msc.getAllTestCasesFromDBForTestSuite(tsuid, pnum);
                JSONObject data = new JSONObject();
                JSONArray jsonArr = new JSONArray();
                data.put("idtest_suite", tsuid);
                data.put("list", list);
                data.put("pageSize", limit);
                jsonArr.add(data);
                response.getWriter().print(jsonArr);
            } else if (action.equals("saveTestSuites") || action.equals("ExecuteTestSuites")) {
                BufferedReader reader = request.getReader();
                JSONParser parser = new JSONParser();
                String prodidstr = (String) request.getParameter("prodid");
                String criteria = (String) request.getParameter("criteria");
                String companystr = (String) request.getParameter("companyid");
                int cmpnyid = Integer.valueOf(companystr);
                String iterationstr = (String) request.getParameter("iterations");
                int iterations = 1;
                if (iterationstr != null && !iterationstr.equals("") &&
                        !iterationstr.equals("undefined")) {
                    iterations = Integer.valueOf(iterationstr);
                }
                String tsrstr = (String) request.getParameter("tsrid");
                int tsrid = -1;
                if (tsrstr != null && !tsrstr.equals("") && !tsrstr.equals("undefined")) {
                    tsrid = Integer.valueOf(tsrstr);
                }

                int prodid = -1;
                if (prodidstr != null && !prodidstr.equals("undefined")) {
                    prodid = Integer.valueOf(prodidstr);
                }
                if (action.equals("saveTestSuites")) {
                } else {
                    MySQlConn msc2 = null;
                    try {
                        msc2 = new MySQlConn(null);
                        msc2.setRandomKey(randomkey);
                        msc2.setUserDetails(userDetails);
                        String executeTSId = request.getParameter("SuiteId");
                        JSONObject varJSON = (JSONObject) parser.parse(reader);
                        int tcid = -1;
                        if (executeTSId != null) {
                            tcid = Integer.valueOf(executeTSId);
                        }
                        authCheck.isUserAuthorizedForTSuite(tcid);
                        msc.updateInitialVariablesToDB(tcid, varJSON);
                        String envUrl = (String) request.getParameter("envUrl");
                        TestSuitePrep tsp = new TestSuitePrep();
                        tsp.tsPrep(msc2, tcid, envUrl, criteria, tsrid, cmpnyid,
                                userCache, false, request, iterations, randomkey, varJSON);
                        JSONObject json = msc.getSearchByTCName(executeTSId, companyId);
                        response.getWriter().print(json);
                    } catch (Exception e) {
                        throw e;
                    } finally {
//						msc2.closeDbConn();
                    }
                }
            } else if (action.equals("DLExecuteSuite")) {
                String executeTSId = request.getParameter("SuiteId");
                String workItem = request.getParameter("workitem");

                if (executeTSId == null) {
                    return;
                }
                MySQlConn msc2 = null;
                try {
                    msc2 = new MySQlConn(null);
                    msc2.setRandomKey(randomkey);
                    msc2.setUserDetails(userDetails);
                    int tsid = Integer.valueOf(executeTSId);
                    authCheck.isUserAuthorizedForTSuite(tsid);
                    JSONObject testSuiteDet = msc.getTestSuite(tsid, 0);
                    String spe = (String) testSuiteDet.get("supports_parallel_execution");
                    boolean synchScenarios = (boolean) testSuiteDet.get("synchedscenarios");
                    boolean ssonerror = (boolean) testSuiteDet.get("ssonerror");
                    JSONArray testCases = msc.getAllTestCasesFromDBForTestSuiteEx(tsid);
                    Tester tester = new Tester();
                    TestSuitePrep tsp = new TestSuitePrep();
                    testCases = tsp.processDependants(msc, testCases);
                    tester.setTestSuite(testCases);
                    tester.setTestSuiteName((String) testSuiteDet.get("Test_Suite"));
                    tester.setCompanyId(companyId);
                    tester.setRandomKey(randomkey);
                    tester.setMSC(msc2);
                    tester.setSPE(spe, userCache);
                    tester.setRunOnce("true");
                    tester.setTestSuiteId(tsid);
                    tester.setWorkItem(workItem);
                    tester.setSynchScenarios(synchScenarios);
                    tester.setSSOnError(ssonerror);

                    String inttypeStr = request.getParameter("inttype");
                    int intType = -1;

                    if (inttypeStr != null && !inttypeStr.equals("")) {
                        intType = Integer.valueOf(inttypeStr);
                        JSONArray intDets = MSC.getStaticIntegrationdetail();
                        HashMap intPropValues = new HashMap();
                        for (int i = 0; i < intDets.size(); i++) {
                            JSONObject intDet = (JSONObject) intDets.get(i);
                            if (intType == (int) intDet.get("staticintid")) {
                                HashMap propValues = new HashMap();
                                String userAttr = (String) intDet.get("user_attributes");
                                String[] userprops = userAttr.split(",");

                                for (int j = 0; j < userprops.length; j++) {
                                    String prop = userprops[j];
                                    String propValue = request.getParameter(prop);
                                    propValues.put(prop, propValue);
                                }
                                intPropValues.put(intType, propValues);
                            }
                        }
                        tester.setIntUserProps(intPropValues);
                        tester.setAllIntDets(intDets);

                        String prodStr = request.getParameter("prodid");
                        int prodId = Integer.valueOf(prodStr);
                        tester.setProductId(prodId);
                    }

                    boolean stopAfterFailure = ((String) testSuiteDet.get("stopafterfailure")).equals("true") ? true : false;
                    boolean rerunFailedTests = ((String) testSuiteDet.get("rerunfailedtests")).equals("true") ? true : false;
                    tester.setStopAfterFailure(stopAfterFailure);
                    tester.setRerunFailedTests(rerunFailedTests);
                    JSONObject emailData = new JSONObject();
                    JSONObject userDets = msc.getUserDetails();
                    emailData.put("name", (String) userDets.get("uname"));
                    emailData.put("recipient", (String) userDets.get("uname"));
                    emailData.put("subject", AppProperties.testcaseexecutionsubject);
                    emailData.put("template", "TestCaseExecution.html");
                    EmailQueuingHelper eqh = userCache.eqh;
                    tester.setResultsForEmail(emailData, eqh);
                    tester.start();
                    
                    final int MAX_RETRIES = 12;
                    final int SLEEP_DURATION_MS = 5000;
                    int iter = 0;

                    while (tester.tsrid == -1 && iter < MAX_RETRIES) {
                        try {
                            Thread.sleep(SLEEP_DURATION_MS);
                            iter++;
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt(); // Restore interrupt status
                            System.err.println("Sleep interrupted: " + e.getMessage());
                        }
                    }
                    
                    JSONObject results = new JSONObject();
                    if (tester.tsrid == -1) {
                        System.err.println("Maximum retries reached; tsrid is still -1.");
                        results.put("resultsurl", "Maximum retries reached; tsrid is still -1.");
                    } else {                    
	                    String resultsUrl = Utilities.getSuiteResultsAPI(companyId,  tester.tsrid);
	                    results.put("resultsurl", resultsUrl);
                    }
                    response.getWriter().print(results);
                } catch (Exception e) {
                    throw e;
                } finally {
//					msc2.closeDbConn();
                }
            } else if (action.equals("updateTestSuiteUrl")) {
                BufferedReader reader = request.getReader();
                JSONParser parser = new JSONParser();
                JSONObject jsonstr = (JSONObject) parser.parse(reader);
                String url = (String) jsonstr.get("url");
                String testsuiteid = request.getParameter("tsuid");
                int tsuid = Integer.valueOf(testsuiteid);
                authCheck.isUserAuthorizedForTSuite(tsuid);
                JSONObject json = msc.updateTestSuiteUrl(tsuid, url);
                response.getWriter().print(json);
            } else if (action.equals("updateTestSuiteResolution")) {
                String testsuiteid = request.getParameter("tsuid");
                int tsuid = Integer.valueOf(testsuiteid);
                authCheck.isUserAuthorizedForTSuite(tsuid);
                String hight = request.getParameter("height");
                int h = Integer.valueOf(hight);
                String width = request.getParameter("width");
                int w = Integer.valueOf(width);
                JSONObject json = msc.updateTestSuiteResolution(tsuid, h, w);
                response.getWriter().print(json);
            } else if (action.equals("updateTestSuiteBrowser")) {
                String testsuiteid = request.getParameter("tsuid");
                int tsuid = Integer.valueOf(testsuiteid);
                authCheck.isUserAuthorizedForTSuite(tsuid);
                String browser = request.getParameter("browser");
                JSONObject json = msc.updateTestSuiteBrowser(tsuid, browser);
                response.getWriter().print(json);
            } else if (action.equals("updateTestSuiteStopAfterFailure")) {
                String testsuiteid = request.getParameter("tsuid");
                int tsuid = Integer.valueOf(testsuiteid);
                authCheck.isUserAuthorizedForTSuite(tsuid);
                String value = request.getParameter("value");
                JSONObject json = msc.updateTestSuiteStopAfterFailure(tsuid, value);
                response.getWriter().print(json);
            } else if (action.equals("updateTestSuiteRerunInParallel")) {
                String testsuiteid = request.getParameter("tsuid");
                int tsuid = Integer.valueOf(testsuiteid);
                authCheck.isUserAuthorizedForTSuite(tsuid);
                String value = request.getParameter("value");
                JSONObject json = msc.updateTestSuiteRerunInParallel(tsuid, value);
                response.getWriter().print(json);
            } else if (action.equals("updateTestSuiteThreadCount")) {
                String testsuiteid = request.getParameter("tsuid");
                int tsuid = Integer.valueOf(testsuiteid);
                authCheck.isUserAuthorizedForTSuite(tsuid);
                String value = request.getParameter("value");
                int threadCount = Integer.parseInt(value);
                JSONObject json = msc.updateTestSuiteThreadCount(tsuid, threadCount);
                response.getWriter().print(json);
            } else if (action.equals("updateTestSuiteRerunFailedTests")) {
                String testsuiteid = request.getParameter("tsuid");
                int tsuid = Integer.valueOf(testsuiteid);
                authCheck.isUserAuthorizedForTSuite(tsuid);
                String value = request.getParameter("value");
                JSONObject json = msc.updateTestSuiteRerunFailedTests(tsuid, value);
                response.getWriter().print(json);
            } else if (action.equals("updateTestSuiteSupportsParallelExecution")) {
                String testsuiteid = request.getParameter("tsuid");
                int tsuid = Integer.valueOf(testsuiteid);
                authCheck.isUserAuthorizedForTSuite(tsuid);
                String value = request.getParameter("value");
                JSONObject json = msc.updateTestSuiteSupportsParallelExecution(tsuid, value);
                response.getWriter().print(json);
            } else if (action.equals("updateTestSuiteSSOnerror")) {
                String testsuiteid = request.getParameter("tsuid");
                int tsuid = Integer.valueOf(testsuiteid);
                authCheck.isUserAuthorizedForTSuite(tsuid);
                String value = request.getParameter("value");
                JSONObject json = msc.updateTestSuiteSSOnError(tsuid, value);
                response.getWriter().print(json);
            } else if (action.equals("updateTestSuiteIndEmail")) {
                String testsuiteid = request.getParameter("tsuid");
                int tsuid = Integer.valueOf(testsuiteid);
                authCheck.isUserAuthorizedForTSuite(tsuid);
                String value = request.getParameter("value");
                JSONObject json = msc.updateTestSuiteIndEmail(tsuid, value);
                response.getWriter().print(json);
            } else if (action.equals("updateTestSuiteResultsEmail")) {
                BufferedReader reader = request.getReader();
                JSONParser parser = new JSONParser();
                JSONObject jsonstr = (JSONObject) parser.parse(reader);
                String email = (String) jsonstr.get("email");
                String testsuiteid = request.getParameter("tsuid");
                int tsuid = Integer.valueOf(testsuiteid);
                authCheck.isUserAuthorizedForTSuite(tsuid);
                JSONObject json = msc.updateTestSuiteResultsEmail(tsuid, email);
                response.getWriter().print(json);
            } else if (action.equals("updateTestSuiteBuildTag")) {
                BufferedReader reader = request.getReader();
                JSONParser parser = new JSONParser();
                JSONObject jsonstr = (JSONObject) parser.parse(reader);
                String buildtag = (String) jsonstr.get("buildtag");
                String testsuiteid = request.getParameter("tsuid");
                int tsuid = Integer.valueOf(testsuiteid);
                authCheck.isUserAuthorizedForTSuite(tsuid);
                JSONObject json = msc.updateTestSuiteBuildTag(tsuid, buildtag);
                response.getWriter().print(json);
            } else if (action.equals("searchTCRDropDown")) {
                String testcaseid = request.getParameter("tcid");
                int tcid = Integer.valueOf(testcaseid);
                authCheck.isUserAuthorizedForTC(tcid);
                JSONArray json = msc.getTestCaseResultFromTestCase(tcid, companyId, false);
                response.getWriter().print(json);
            } else if (action.equals("getCurrentExecutions")) {
                authCheck.isUserAuthorizedForCompany(companyId);
                JSONArray json = msc.getCurrentExecutions(companyId);
                response.getWriter().print(json);
            } else if (action.equals("searchTSRDropDown")) {
                String testcaseresultid = request.getParameter("tcrid");
                int tcrid = Integer.valueOf(testcaseresultid);
                authCheck.isUserAuthorizedForTCR(tcrid);
                JSONArray json = msc.getTestStepResultFromTestCaseResult(tcrid, false);
                response.getWriter().print(json);
            } else if (action.equals("getTestCaseDataReport")) {
            	String cid = request.getParameter("companyid");
            	String testcaseresultid = request.getParameter("tcrid");
            	int comId = Integer.valueOf(cid);
                int tcrid = Integer.valueOf(testcaseresultid);
                authCheck.isUserAuthorizedForTCR(tcrid);
                JSONObject json = new JSONObject();
                JSONArray jsonarr = msc.getTestStepResultFromTestCaseResult(tcrid, false);
                JSONObject json1 = msc.getDataForExecutionReports(tcrid, comId);
                json.put("test_case_data", json1);
                json.put("test_step_data", jsonarr);
                response.getWriter().print(json);
            } else if (action.equals("getAnalysisForFailedTestStep")) { 
            	
            	String TestStepResultId = request.getParameter("TSRId");
            	String testCaseResultId = request.getParameter("TCRid");
            	JSONObject json;
            	
            	if (testCaseResultId != null && !testCaseResultId.isEmpty()) {
            		int TCRId = Integer.parseInt(testCaseResultId);
            		json = msc.getAnalysisForFailedTestStepByTCRid(TCRId, MSC);
            	}
            	else {
            		int TSRId = Integer.valueOf(TestStepResultId);
            		json = msc.getAnalysisForFailedTestStep(TSRId, MSC);
            		
            	}
            	response.getWriter().print(json);
                
            } else if (action.equals("getErrorCodesForTestSuite")) { 
            	String tsuiteresultid = request.getParameter("testSuiteResultId");
                int tsrid = Integer.valueOf(tsuiteresultid);
                
                JSONObject json = msc.getErrorCodeDropDownFromTestSuiteResult(tsrid);
                response.getWriter().print(json);
                
            } else if (action.equals("registerUser")) {
                BufferedReader reader = request.getReader();
                String code = request.getParameter("codegen");
                JSONParser parser = new JSONParser();
                JSONObject jsonstr = (JSONObject) parser.parse(reader);
                JSONObject jo = new JSONObject();
                try {
                    jo = msc.registerUser(jsonstr, MSC);
                } catch (Exception e) {
                    jo.put("message", "fail");
                    response.getWriter().print(jo);
                    return;
                }
                JSONObject emailData = new JSONObject();
                emailData.put("name", (String) jsonstr.get("firstname"));
                emailData.put("recipient", (String) jsonstr.get("email"));
                emailData.put("subject", AppProperties.welcomesubject);
                emailData.put("template", "welcome.html");
                EmailQueuingHelper eqh = userCache.eqh;
                eqh.addToQueue(emailData);
                jo.put("message", "success");
                System.out.println(jo.toJSONString());
                response.getWriter().print(jo);
            } else if (action.equals("confirmorder")) {
                BufferedReader reader = request.getReader();
                JSONParser parser = new JSONParser();
                JSONObject jsonstr = (JSONObject) parser.parse(reader);
                CashfreeIntegration cfi = new CashfreeIntegration();
                cfi.confirmCFOrder(jsonstr, msc, MSC);
            } else if (action.equals("verifyorder")) {
                String ngorderid = (String) request.getParameter("ngorderid");
                JSONObject orderDetails = msc.getOrderByNgorderid(ngorderid, false);
                response.getWriter().print(orderDetails);
            } else if (action.equals("auth")) {
                BufferedReader reader = request.getReader();
                JSONParser parser = new JSONParser();
                JSONObject jsonstr = (JSONObject) parser.parse(reader);
                UserActivities ua = new UserActivities();
                JSONObject auditdet = new JSONObject();
                auditdet.put("uname", jsonstr.get("uname"));
                audit.put("object", auditdet);
                JSONObject success = ua.authenticate(jsonstr, msc);
                audit.put("result", success.get("message"));
                audit.put("randomkey", success.get("randomkey"));
                userCache.aqh.addToQueue(audit);
                response.getWriter().print(success);
            } else if (action.equals("addproduct")) {
                authCheck.isUserAuthorizedForCompany(companyId);
                String productName = (String) request.getParameter("productname");
                msc.addProducts(productName, companyId);
            } else if (action.equals("setcompanyurl")) {
                authCheck.isUserAuthorizedForCompany(companyId);
                String companyurl = (String) request.getParameter("companyurl");
                msc.addCompanyUrl(companyurl, companyId);
            } else if (action.equals("addmodule")) {
                String moduleName = (String) request.getParameter("modulename");
                String prodidstr = (String) request.getParameter("prodid");
                int prodid = new Integer(prodidstr).intValue();
                authCheck.isUserAuthorizedForProd(prodid);
                msc.addModules(moduleName, prodid);
            } else if (action.equalsIgnoreCase("downloadFile") ||
                    action.equalsIgnoreCase("downloadFiless")) {
                // Get the file path and name
                String fileName = (String) request.getParameter("fileName");
                String[] parts = fileName.split("_");

                if (!fileName.startsWith("_logo") && fileName.endsWith(".png") && fileName.indexOf("/") == -1 &&
                        (StringUtils.countMatches(fileName, ".") == 1)) {
                    authCheck.isUserAuthorizedForCompany(Integer.valueOf(parts[1]));
                    if (action.equalsIgnoreCase("downloadFiless")) {
                        fileName = Utilities.getAnalysisSSDir(Integer.valueOf(parts[1]),
                                Integer.valueOf(parts[2])) + fileName;
                    } else {
                        fileName = Utilities.getScreenShotsDir(Integer.valueOf(parts[1]),
                                Integer.valueOf(parts[2]), Integer.valueOf(parts[4])) +
                                fileName;
                    }
                    // Set the content type and headers
                    String mimeType = getMimeType(fileName, request.getServletContext());
                    response.setContentType(mimeType);
                    response.setHeader("Content-Disposition", "inline; filename=\"" + fileName + "\"");
                    // Serve the file
                    try (OutputStream out = response.getOutputStream();
                         FileInputStream fis = new FileInputStream(fileName)) {
                        byte[] buffer = new byte[1024];
                        int bytesRead;
                        while ((bytesRead = fis.read(buffer)) != -1) {
                            out.write(buffer, 0, bytesRead);
                        }
                    } catch (Exception e) {

                    }
                } else if (fileName.startsWith("_logo") && (fileName.endsWith(".png") || fileName.endsWith(".PNG") || fileName.endsWith(".jpg")) && fileName.indexOf("/") == -1 &&
                        (StringUtils.countMatches(fileName, ".") == 1)) {
                    fileName = Utilities.getBrandingDir(companyId, fileName);
                    String mimeType = getMimeType(fileName, request.getServletContext());
                    response.setContentType(mimeType);
                    response.setHeader("Content-Disposition", "inline; filename=\"" + fileName + "\"");
                    try (OutputStream out = response.getOutputStream();
                         FileInputStream fis = new FileInputStream(fileName)) {
                        byte[] buffer = new byte[1024];
                        int bytesRead;
                        while ((bytesRead = fis.read(buffer)) != -1) {
                            out.write(buffer, 0, bytesRead);
                        }
                    }
                } else if (fileName != null && (fileName.equals("NoGrunt.PNG"))) {
                    fileName = Utilities.getBrandingDir(-1, fileName);
                    String mimeType = getMimeType(fileName, request.getServletContext());
                    response.setContentType(mimeType);
                    response.setHeader("Content-Disposition", "inline; filename=\"" + fileName + "\"");
                    try (OutputStream out = response.getOutputStream();
                         FileInputStream fis = new FileInputStream(fileName)) {
                        byte[] buffer = new byte[1024];
                        int bytesRead;
                        while ((bytesRead = fis.read(buffer)) != -1) {
                            out.write(buffer, 0, bytesRead);
                        }
                    }
                } else {
                    response.getWriter().print("Invalid file name");
                }
            } else if (action.equals("downloadCoverageFile")) {
                // Get the file path and name
                String fileName = (String) request.getParameter("fileName");
                // Set the content type and headers
                String mimeType = getMimeType(fileName, request.getServletContext());
                response.setContentType(mimeType);
                response.setHeader("Content-Disposition", "inline; filename=\"" + fileName + "\"");
                // Serve the file
                try (OutputStream out = response.getOutputStream();
                     FileInputStream fis = new FileInputStream(fileName)) {
                    byte[] buffer = new byte[1024];
                    int bytesRead;
                    while ((bytesRead = fis.read(buffer)) != -1) {
                        out.write(buffer, 0, bytesRead);
                    }
                }
            } else if (action.equals("pageSource")) {
            	
            	String fileName = (String) request.getParameter("stepNumber") + ".txt";
            	
            	String tcridStr = (String) request.getParameter("tcrid");
                int tcrid = Integer.parseInt(tcridStr);

                String tcidStr = (String) request.getParameter("tcid");
                int tcid = Integer.parseInt(tcidStr);
                authCheck.isUserAuthorizedForTC(tcid);

                fileName = Utilities.getPageSourceDirFile(companyId, tcid, tcrid, fileName);
                
                // Set the content type and headers
                String mimeType = getMimeType(fileName, request.getServletContext());
                response.setContentType(mimeType);
                response.setHeader("Content-Disposition", "inline; filename=\"" + fileName + "\"");
                // Serve the file
                try (OutputStream out = response.getOutputStream();
                     FileInputStream fis = new FileInputStream(fileName)) {
                    byte[] buffer = new byte[1024];
                    int bytesRead;
                    while ((bytesRead = fis.read(buffer)) != -1) {
                        out.write(buffer, 0, bytesRead);
                    }
                } catch (IOException e) {
                    // Handle the IOException here
                    String errorMessage = e.getMessage();
                    try {
                        // response.getWriter().write(errorMessage);
                        response.getWriter().print(errorMessage);
                    } catch (IOException ex) {
                        // Handle the second IOException if writing the error message to the output stream fails
                        System.err.println(Utilities.getNow());
                        ex.printStackTrace();
                    }
                }
                
            } else if (action.equals("analysisLog")) {

                String tcridStr = (String) request.getParameter("tcrid");
                int tcrid = Integer.parseInt(tcridStr);

                String tcidStr = (String) request.getParameter("tcid");
                int tcid = Integer.parseInt(tcidStr);
                authCheck.isUserAuthorizedForTC(tcid);

                String fileName = Utilities.getAnalysisLogFile(companyId, tcid, tcrid);

                // Set the content type and headers
                String mimeType = getMimeType(fileName, request.getServletContext());
                response.setContentType(mimeType);
                response.setHeader("Content-Disposition", "inline; filename=\"" + fileName + "\"");
                // Serve the file
                try (OutputStream out = response.getOutputStream();
                     FileInputStream fis = new FileInputStream(fileName)) {
                    byte[] buffer = new byte[1024];
                    int bytesRead;
                    while ((bytesRead = fis.read(buffer)) != -1) {
                        out.write(buffer, 0, bytesRead);
                    }
                } catch (IOException e) {
                    // Handle the IOException here
                    String errorMessage = e.getMessage();
                    try {
                        // response.getWriter().write(errorMessage);
                        response.getWriter().print(errorMessage);
                    } catch (IOException ex) {
                        // Handle the second IOException if writing the error message to the output stream fails
                        System.err.println(Utilities.getNow());
                        ex.printStackTrace();
                    }
                }
            	
            } else if (action.equals("executionlog")) {
                // Get the file path and name
                String fileName = null;

                if (request.getParameter("tcrid") != null) {
                    String tcridStr = (String) request.getParameter("tcrid");
                    int tcrid = Integer.parseInt(tcridStr);

                    String tcidStr = (String) request.getParameter("tcid");
                    int tcid = Integer.parseInt(tcidStr);
                    authCheck.isUserAuthorizedForTC(tcid);

                    fileName = Utilities.getLogDirFile(companyId, tcid, tcrid);
                } else {
                    String tsridStr = (String) request.getParameter("tsrid");
                    int tsrid = Integer.parseInt(tsridStr);

                    String tsidStr = (String) request.getParameter("tsid");
                    int tsid = Integer.parseInt(tsidStr);
                    authCheck.isUserAuthorizedForTSuite(tsid);

                    fileName = Utilities.getLogDirFileTS(companyId, tsid, tsrid);
                }

                // Set the content type and headers
                String mimeType = getMimeType(fileName, request.getServletContext());
                response.setContentType(mimeType);
                response.setHeader("Content-Disposition", "inline; filename=\"" + fileName + "\"");
                // Serve the file
                try (OutputStream out = response.getOutputStream();
                     FileInputStream fis = new FileInputStream(fileName)) {
                    byte[] buffer = new byte[1024];
                    int bytesRead;
                    while ((bytesRead = fis.read(buffer)) != -1) {
                        out.write(buffer, 0, bytesRead);
                    }
                } catch (IOException e) {
                    // Handle the IOException here
                    String errorMessage = e.getMessage();
                    try {
                        // response.getWriter().write(errorMessage);
                        response.getWriter().print(errorMessage);
                    } catch (IOException ex) {
                        // Handle the second IOException if writing the error message to the output stream fails
                        System.err.println(Utilities.getNow());
                        ex.printStackTrace();
                    }
                }
            } else if (action.equals("downloadTCVideoFile")) {
                // Get the file path and name
                String fileName = (String) request.getParameter("fileName");
                String testcase = (String) request.getParameter("tcid");
                int tcid = -1;
                if (testcase != null) {
                    tcid = Integer.valueOf(testcase);
                }
                String Name = Utilities.getVideoPath(companyId, tcid, fileName);
                // Set the content type and headers
                String mimeType = getMimeType(Name, request.getServletContext());
                response.setContentType(mimeType);
                response.setHeader("Content-Disposition", "inline; filename=\"" + Name + "\"");
                // Serve the file
                try (OutputStream out = response.getOutputStream();
                     FileInputStream fis = new FileInputStream(Name)) {
                    byte[] buffer = new byte[1024];
                    int bytesRead;
                    while ((bytesRead = fis.read(buffer)) != -1) {
                        out.write(buffer, 0, bytesRead);
                    }
                }
            }else if (action.equals("downloadTCRvideo")) {
                try {
                    int tcId = Integer.parseInt(request.getParameter("testcaseid"));
                    int tcResultId = Integer.parseInt(request.getParameter("testcaseresultId"));
                    int compId = Integer.parseInt(request.getParameter("companyId"));
                    String token = request.getParameter("token");
                    Integer videoIndex = request.getParameter("videoIndex") != null ? 
                            Integer.parseInt(request.getParameter("videoIndex")) : null;

                    String videoDir = Utilities.getVideoDir(compId, tcId, tcResultId);
                    File directory = new File(videoDir);
                    
                    if (!directory.exists() || !directory.isDirectory()) {
                        response.setContentType("application/json");
                        PrintWriter out = response.getWriter();
                        JSONObject errorJson = new JSONObject();
                        errorJson.put("status", "error");
                        errorJson.put("message", "Video directory not found");
                        out.print(errorJson.toString());
                        return;
                    }
                    
                    File[] files = directory.listFiles((dir, name) -> name.toLowerCase().endsWith(".mp4"));
                        
                    if (files == null || files.length == 0) {
                        response.setContentType("application/json");
                        PrintWriter out = response.getWriter();
                        JSONObject errorJson = new JSONObject();
                        errorJson.put("status", "error");
                        errorJson.put("message", "No video files found");
                        out.print(errorJson.toString());
                        return;
                    }

                    Arrays.sort(files, (f1, f2) -> f1.getName().compareTo(f2.getName()));

                    if (files.length == 1 && videoIndex == null) {
                        videoIndex = 0;
                    }
                    if (videoIndex != null) {
                        if (videoIndex < 0 || videoIndex >= files.length) {
                            videoIndex = 0;
                        }
                        
                        File videoFile = files[videoIndex];
                        response.setContentType("video/mp4");
                        response.setHeader("Content-Disposition", "inline; filename=\"" + videoFile.getName() + "\"");
                        response.setContentLengthLong(videoFile.length());
                        
                        try (OutputStream out = response.getOutputStream();
                             FileInputStream fis = new FileInputStream(videoFile);
                             BufferedInputStream bis = new BufferedInputStream(fis, 8192)) {
                            
                            byte[] buffer = new byte[8192];
                            int bytesRead;
                            while ((bytesRead = bis.read(buffer)) != -1) {
                                out.write(buffer, 0, bytesRead);
                            }
                        }
                    }else {
                        response.setContentType("application/json");
                        PrintWriter out = response.getWriter();
                        JSONObject resultJson = new JSONObject();
                        JSONArray videosArray = new JSONArray();
                        
                        for (int i = 0; i < files.length; i++) {
                            File file = files[i];
                            JSONObject videoJson = new JSONObject();
                            
                            videoJson.put("index", i);
                            videoJson.put("name", file.getName());
                            videoJson.put("size", file.length());
                            
                            String videoUrl = request.getRequestURL() + 
                                "?action=downloadTCRvideo" +
                                "&testcaseid=" + tcId +
                                "&testcaseresultId=" + tcResultId +
                                "&companyId=" + compId +
                                "&videoIndex=" + i +
                                "&token="+ token;
                            videoJson.put("url", videoUrl);
                            
                            videosArray.add(videoJson);
                        }
                        
                        resultJson.put("status", "success");
                        resultJson.put("count", files.length);
                        resultJson.put("videos", videosArray);
                        
                        out.print(resultJson.toString());
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    if (!response.isCommitted()) {
                        response.setContentType("application/json");
                        try {
                            PrintWriter out = response.getWriter();
                            JSONObject errorJson = new JSONObject();
                            errorJson.put("status", "error");
                            errorJson.put("message", "Error: " + e.getMessage());
                            out.print(errorJson.toString());
                        } catch (Exception ex) {
                            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error: " + e.getMessage());
                        }
                    }
                }
            } else if (action.equals("addURL")) {

            } else if (action.equals("addResolution")) {
                BufferedReader reader = request.getReader();
                JSONParser parser = new JSONParser();
                JSONObject jsonstr = (JSONObject) parser.parse(reader);
                int productId = Integer.parseInt(jsonstr.get("idproducts").toString());
                authCheck.isUserAuthorizedForProd(productId);
                //Extracting Url
                JSONArray resolutionsArr = (JSONArray) jsonstr.get("ResolutionList");
                JSONArray urlsArr = (JSONArray) jsonstr.get("URLList");
                for (Object resolutionObj : resolutionsArr) {
                    JSONObject resolutionJson = (JSONObject) resolutionObj;
                    int width = Integer.parseInt(resolutionJson.get("Width").toString());
                    int height = Integer.parseInt(resolutionJson.get("Height").toString());
                    if (resolutionJson.get("idresolutions") == null ||
                            ((long) resolutionJson.get("idresolutions") < 0)) {
                        msc.addResolution(productId, width, height);
                    }
                    String moduleName = width + "by" + height;
                    String rKey = Utilities.randomGen(AppProperties.NUMBER, 8, false);
                    int randomKey = new Integer(rKey).intValue();
                    int testCaseId = msc.checkandAddTestCase(moduleName, productId,
                            width, height, randomKey);
                    Utilities.createTestCaseFolder(companyId, testCaseId);
                    for (int j = 0; j < urlsArr.size(); j++) {
                        JSONObject urlJson = (JSONObject) urlsArr.get(j);
                        String urlName = urlJson.get("urlName").toString();
                        if (urlJson.get("urlId") == null ||
                                (long) urlJson.get("urlId") < 0) {
                            msc.addURLS(productId, urlName);
                            msc.insertIntoTestSteps(testCaseId, j + 1, "",
                                    "", "", AppProperties.URLKEYWORD, AppProperties.GETACTION,
                                    AppProperties.POSITIVEFLOW, urlName, "", null, randomKey,
                                    ((j + 1) * 1000), "", "", "", -1, "", "", "",
                                    1, "", ((j + 1) * 1000), AppProperties.URLEVENT, "", "", "", null,
                                    -1, null, j + 1, -1);
                        }

                    }
                }
            } else if (action.equals("addCredential")) {
                BufferedReader reader = request.getReader();
                JSONParser parser = new JSONParser();
                JSONObject jsonstr = (JSONObject) parser.parse(reader);
                int productId = Integer.parseInt(jsonstr.get("idproducts").toString());
                authCheck.isUserAuthorizedForProd(productId);
                String username = (String) jsonstr.get("email");
                String password = (String) jsonstr.get("password");
                String company = (String) jsonstr.get("company");
                msc.addCredential(username, password, company, productId);
            } else if (action.equals("getData")) {
                String productid = request.getParameter("productid");
                int pid = Integer.valueOf(productid);
                authCheck.isUserAuthorizedForProd(pid);
                JSONObject json = msc.getData(pid);
                response.getWriter().print(json);
            } else if (action.equals("executeURL")) {
                MySQlConn msc2 = null;
                try {
                    msc2 = new MySQlConn(null);
                    msc2.setRandomKey(randomkey);
                    msc2.setUserDetails(userDetails);
                    BufferedReader reader = request.getReader();
                    JSONParser parser = new JSONParser();
                    JSONObject jsonstr = (JSONObject) parser.parse(reader);
                    Tester tester = new Tester();
                    tester.setURLData(jsonstr);
                    tester.setCompanyId(companyId);
                    tester.setMSC(msc2);
                    tester.start();
                } catch (Exception e) {
                    throw e;
                } finally {
                    msc2.closeDbConn();
                }
            } else if (action.equals("getFailTestSteps")) {
                String TestStepResultId = request.getParameter("TSRId");
                int TSRId = Integer.valueOf(TestStepResultId);
                authCheck.isUserAuthorizedForTStepR(TSRId);
                JSONArray json = msc.getFailTestSteps(TSRId, companyId);
                response.getWriter().print(json);
            } else if (action.equals("addEnv")) {
                BufferedReader reader = request.getReader();
                JSONParser parser = new JSONParser();
                JSONObject jsonstr = (JSONObject) parser.parse(reader);
                int productId = Integer.parseInt(jsonstr.get("idproducts").toString());
                authCheck.isUserAuthorizedForProd(productId);
                String envname = (String) jsonstr.get("envname");
                String envurl = (String) jsonstr.get("envurl");
                int idenvdetails = Integer.parseInt(jsonstr.get("idenvdetails").toString());
                if (idenvdetails < 0) {
                    msc.addEnv(envname, envurl, productId);
                } else {
                    response.getWriter().println("The environment details already exist.");
                }
            } else if (action.equals("changePassword")) {
                BufferedReader reader = request.getReader();
                JSONParser parser = new JSONParser();
                JSONObject jsonstr = (JSONObject) parser.parse(reader);
                String username = (String) jsonstr.get("Username");
                String currentPassword = (String) jsonstr.get("CurrentPassword");
                String newPassword = (String) jsonstr.get("NewPassword");
                String RENewPassword = (String) jsonstr.get("RENewPassword");
                if (newPassword.equals(RENewPassword)) {
                    msc.changePassword(username, currentPassword, newPassword);
                } else {
                    response.getWriter().println("New password and Re-Entered password must be same.");
                }
            } else if (action.equals("getEnv")) {
                String productid = request.getParameter("productid");
                int pid = Integer.valueOf(productid);
                authCheck.isUserAuthorizedForProd(pid);
                JSONArray json = msc.getEnv(pid);
                response.getWriter().print(json);
            } else if (action.equals("submitUrlTestStep")) {
                String EnvName = request.getParameter("envName");
                String URL = request.getParameter("envUrl");
                String tsid = request.getParameter("teststepid");
                String tcIdStr = request.getParameter("testCaseId");
                int tid = Integer.valueOf(tsid);
                authCheck.isUserAuthorizedForTC(tid);
                msc.updateTestStepFromURL(tid, EnvName, URL);
            } else if (action.equals("delTestSuites")) {
                String tsuitesIdStr = request.getParameter("idtest_suite");
                int tsuid = Integer.valueOf(tsuitesIdStr);
                authCheck.isUserAuthorizedForTSuite(tsuid);
                msc.deleteTestSuiteCase(tsuid);
            } else if (action.equals("editTestSuites")) {
                String newName = request.getParameter("newName");
                String tsuitesIdStr = request.getParameter("idtest_suite");
                int tsuid = Integer.valueOf(tsuitesIdStr);
                authCheck.isUserAuthorizedForTSuite(tsuid);
                msc.editTestSuiteCase(tsuid, newName);
            } else if (action.equals("getTestSuiteResult")) {
                String tsuiteid = request.getParameter("idtest_suite");
                if (tsuiteid == null || tsuiteid.equals("")) {
                	response.setStatus(HttpServletResponse.SC_BAD_REQUEST);  // Set the status code to 400
                    response.getWriter().print("{\"error\": \"Invalid test suite id value of null or blank\"}");  // Send an error message
                    JSONArray json = new JSONArray();
                    response.getWriter().print(json);
                    return;
                }
                int tsid = Integer.valueOf(tsuiteid);
                authCheck.isUserAuthorizedForTSuite(tsid);
                String tsuiteresultid = request.getParameter("TSRId");
                if (tsuiteresultid == null || tsuiteresultid.equals("")) {
                    JSONArray json = msc.getTestSuiteResultsFromTestSuite(tsid);
                    response.getWriter().print(json);
                } else {
                    int TSRId = Integer.valueOf(tsuiteresultid);
                    JSONArray json = msc.getTestSuiteResultsFromTestSuite(tsid, TSRId);
                    response.getWriter().print(json);
                }
            } else if (action.equals("getTestCaseResultFromTestSuiteResult")) {
                String tsuiteresultid = request.getParameter("testSuiteResultId");
                int tsrid = Integer.valueOf(tsuiteresultid);
                authCheck.isUserAuthorizedForTSR(tsrid);
                String criteria = request.getParameter("criteria");
                JSONObject json = null;
                if (criteria != null && criteria.equals("FAIL")) {
                    json = msc.getFailedTestCaseResultFromTestSuiteResult(tsrid, companyId, criteria);
                } else {
                    json = msc.getTestCaseResultFromTestSuiteResult(tsrid, companyId, criteria);
                }
                response.getWriter().print(json);
                
            } else if (action.equals("getPercentageData")) {
            	String tsrId1 = request.getParameter("tsrid");
            	String tsId = request.getParameter("tsid");
            	int tsrid1 = Integer.valueOf(tsrId1);
            	int tsid = Integer.valueOf(tsId);
            	
            	int tsrid2 = msc.getLastTestSuiteResultsId(tsid);
            	JSONObject json = msc.getPercentageDataForRuns(tsrid1, tsrid2);
            	response.getWriter().print(json);
            } else if (action.equalsIgnoreCase("getAPIData")) {
            	String test_case_results_id = request.getParameter("tcrid");
            	int tcrid = Integer.valueOf(test_case_results_id);
            	JSONObject json = msc.getAPIDataFromDB(tcrid);
            	response.getWriter().print(json);
            } else if (action.equals("saveApiEndpoints")) {
            	BufferedReader reader = request.getReader();
                JSONParser parser = new JSONParser();
                JSONObject json = (JSONObject) parser.parse(reader);
            	String test_step_id = request.getParameter("testStepId");
            	int tsid = Integer.valueOf(test_step_id);
            	msc.saveApiCallToDB(json, tsid);
            } else if (action.equals("updateIgnoredStatusForTestSuiteResult")) {
            	
            	String tsuiteResultId = request.getParameter("testSuiteResultId");
                int tsrid = Integer.valueOf(tsuiteResultId);
                
                String isIgnoredParam = request.getParameter("isIgnored");
                boolean isIgnored = Boolean.parseBoolean(isIgnoredParam);

                JSONObject json = msc.updateIsIgnored(tsrid, isIgnored);
                response.getWriter().print(json);
                
            } else if (action.equals("saveUserNoteToTCResult")) {
            	
                String tcaseresultid = request.getParameter("testCaseResultId");
                int tcrid = Integer.valueOf(tcaseresultid);
                
                BufferedReader reader = request.getReader();
                JSONParser parser = new JSONParser();
                JSONObject jsonBody = (JSONObject) parser.parse(reader);
                String userNote = jsonBody.containsKey("userNote") ? (String) jsonBody.get("userNote") : null;
                String userComments = jsonBody.containsKey("userComments") ? (String) jsonBody.get("userComments") : null;
                
                JSONObject json = msc.saveUserNoteForTestCaseResult(tcrid, userNote, userComments);
                response.getWriter().print(json);
                
            } else if (action.equals("getUserNoteFromTCResult")) {
            	
                String tcaseresultid = request.getParameter("testCaseResultId");
                int tcrid = Integer.valueOf(tcaseresultid);
                
                JSONObject json = msc.getUserNoteFromTestCaseResult(tcrid);
                response.getWriter().print(json);
                
            } else if (action.equals("getAllDescriptionTagsForSuiteResult")) {

                String tsuiteResultId = request.getParameter("testSuiteResultId");
                int tsrid = Integer.valueOf(tsuiteResultId);

                JSONObject json = msc.getAllDescriptionTagsForSuiteResult(tsrid);
                response.setContentType("application/json");
                response.getWriter().print(json);
            } else if (action.equals("getAllTagsForSuiteResult")) {
            	
                String tsuiteResultId = request.getParameter("testSuiteResultId");
                int tsrid = Integer.valueOf(tsuiteResultId);

                JSONObject json = msc.getAllTagsForSuiteResult(tsrid);
                response.setContentType("application/json");
                response.getWriter().print(json);
                
            } else if (action.equals("getUserNoteStatsForTestSuiteResult")) {
            	
                String tsuiteResultId = request.getParameter("testSuiteResultId");
                int tsrid = Integer.valueOf(tsuiteResultId);

                JSONObject json = msc.getUserNoteStatsForTestSuite(tsrid);
                response.setContentType("application/json");
                response.getWriter().print(json);
                
            } else if (action.equals("getTCResultsListWithTagsForTSR")) {

                String tsuiteResultId = request.getParameter("testSuiteResultId");
                int tsrid = Integer.valueOf(tsuiteResultId);

                JSONArray jsonArray = msc.getTestCaseResultsListWithTagsForTestSuite(tsrid, companyId);

                // Wrap the result in a JSONObject with a success flag
                JSONObject jsonResponse = new JSONObject();
                jsonResponse.put("success", true);
                jsonResponse.put("testCaseResults", jsonArray);
                jsonResponse.put("count", jsonArray.size());

                response.setContentType("application/json");
                response.getWriter().print(jsonResponse);
            } else if (action.equals("getTestCaseResultsByUserTag")) {

                String tsuiteResultId = request.getParameter("testSuiteResultId");

                int tsrid = Integer.valueOf(tsuiteResultId);
                
                List<String> tags = new ArrayList<>();

                try {
                    BufferedReader reader = request.getReader();
                    JSONParser parser = new JSONParser();
                    JSONObject jsonBody = (JSONObject) parser.parse(reader);

                    if (jsonBody.containsKey("tags")) {
                        JSONArray tagsArray = (JSONArray) jsonBody.get("tags");
                        for (Object tag : tagsArray) {
                            tags.add(tag.toString());
                        }
                    }
                } catch (Exception e) {
                	System.err.println(Utilities.getNow());
                }

                JSONObject json = msc.getTestCaseResultsByUserTags(tsrid, tags);
                response.setContentType("application/json");
                response.getWriter().print(json);
                
            } else if (action.equals("getStatusPatternCountsForTestSuiteResult")) {
                String tsuiteresultid = request.getParameter("testSuiteResultId");
                int tsrid = Integer.valueOf(tsuiteresultid);
                JSONObject json = msc.getStatusPatternCountsForTestSuiteResult(tsrid, companyId);
                response.getWriter().print(json);
                
            } else if (action.equals("getSearchedTestCaseHistoryForTestsuite")) {
            	
                String tsuiteid = request.getParameter("testSuiteId");
                int tsid = Integer.parseInt(tsuiteid);
                
                String tsuiteresultid = request.getParameter("testSuiteResultId");
                Integer tsrid = (tsuiteresultid != null && !tsuiteresultid.isEmpty()) 
                	    ? Integer.valueOf(tsuiteresultid) 
                	    : null;

                String pageNum = request.getParameter("pnum");
                int pnum;
                if (pageNum == null || pageNum.equals("") || pageNum.equals("undefined")) {
                    pnum = 1;
                } else {
                    pnum = Integer.parseInt(pageNum);
                }

                String searchString = request.getParameter("searchKey");
                
                List<String> issueTypes = new ArrayList<>();
                List<String> descriptionTags = new ArrayList<>();
                
                try {
                	BufferedReader reader = request.getReader();
                    JSONParser parser = new JSONParser();
                    JSONObject jsonBody = (JSONObject) parser.parse(reader);

                    if (jsonBody.containsKey("issueTypes")) {
                        JSONArray issuesArray = (JSONArray) jsonBody.get("issueTypes");
                        for (Object issue : issuesArray) {
                            issueTypes.add(issue.toString());
                        }
                    }

                    if (jsonBody.containsKey("descriptionTags")) {
                        JSONArray tagsArray = (JSONArray) jsonBody.get("descriptionTags");
                        for (Object tag : tagsArray) {
                            String tagStr = tag.toString();
                            if (tagStr.startsWith("#") && tagStr.length() > 1) {
                                descriptionTags.add(tagStr.substring(1).toLowerCase());
                            }
                        }
                    }
                } catch (Exception e) {
                	System.err.println(Utilities.getNow());
                }
                
                
                if ((searchString == null || searchString.trim().isEmpty()) 
                		&& issueTypes.isEmpty() && descriptionTags.isEmpty()) {
                    JSONObject json = new JSONObject();
                    json.put("error", "Search string is empty and no filters are there");
                    response.getWriter().print(json);
                } 
                else {
                	JSONObject json = msc.getSearchedTestCasesHistoryFromTestSuite(tsid, tsrid, companyId, pnum, searchString, issueTypes, descriptionTags);
                    response.getWriter().print(json);
                }
                
            } else if (action.equals("getTestCaseHistoryForTestsuite")) {
            	
            	String tsuiteid = request.getParameter("testSuiteId");
                int tsid = Integer.valueOf(tsuiteid);
                String tsuiteresultid = request.getParameter("testSuiteResultId");
                Integer tsrid = (tsuiteresultid != null && !tsuiteresultid.isEmpty()) 
                	    ? Integer.valueOf(tsuiteresultid) 
                	    : null;
                String pageNum = request.getParameter("pnum");
                int pnum;
                if (pageNum == null || pageNum.equals("") || pageNum.equals("undefined")) {
                    pnum = 1;
                } else {
                    pnum = Integer.valueOf(pageNum);
                }
                
                String filterOne = request.getParameter("filterOne");
                String filterTwo = request.getParameter("filterTwo");
                String filterThree = request.getParameter("filterThree");

                if (filterOne == null || filterOne.trim().isEmpty()) filterOne = "fail";
                if (filterTwo == null || filterTwo.trim().isEmpty()) filterTwo = "any";
                if (filterThree == null || filterThree.trim().isEmpty()) filterThree = "any";
                
                JSONObject json = null;
                json = msc.getFilteredTestCasesHistoryFromTestSuite(tsid, tsrid, companyId, pnum, filterOne, filterTwo, filterThree);
                response.getWriter().print(json);
                
            } else if (action.equals("editTestSuite")) {
                BufferedReader reader = request.getReader();
                JSONParser parser = new JSONParser();
                JSONObject jsonstr = (JSONObject) parser.parse(reader);
                int testSuiteId = Integer.parseInt(jsonstr.get("id_test_suite").toString());
                authCheck.isUserAuthorizedForTSuite(testSuiteId);
                String envurl = (String) jsonstr.get("EnvironmentUrl");
                String browser = (String) jsonstr.get("EnvironmentBrowser");
                String stopafterfailure = (String) jsonstr.get("StopAfterFailure");
                String rerunfailedtests = (String) jsonstr.get("ReRunFailedTests");
                int width = Integer.parseInt(jsonstr.get("Width").toString());
                int height = Integer.parseInt(jsonstr.get("Height").toString());
                msc.editTestSuite(testSuiteId, width, height, browser, envurl, stopafterfailure, rerunfailedtests);
            } else if (action.equals("editTestCase")) {
                BufferedReader reader = request.getReader();
                JSONParser parser = new JSONParser();
                JSONObject jsonstr = (JSONObject) parser.parse(reader);
                int testCaseId = Integer.parseInt(jsonstr.get("idtest_case").toString());
                authCheck.isUserAuthorizedForTC(testCaseId);
                String envurl = (String) jsonstr.get("EnvironmentUrl");
                String envName = (String) jsonstr.get("EnvironmentName");
                String browser = (String) jsonstr.get("EnvironmentBrowser");
                int width = -1;
                int height = -1;

                if (jsonstr.get("Width") != null && !jsonstr.get("Width").equals("")) {
                    width = Integer.parseInt(jsonstr.get("Width").toString());
                }

                if (jsonstr.get("Height") != null && !jsonstr.get("Height").equals("")) {
                    height = Integer.parseInt(jsonstr.get("Height").toString());
                }
                msc.editTestCase(testCaseId, width, height, browser, envurl, envName);
                String uname = (String) userDetails.get("uname");
                JSONObject jsonObject = msc.getLatestRecording(uname);
                userCache.cacheLatestRecording(uname, jsonObject);
            } else if (action.equalsIgnoreCase("getApi")) {
                String apiid = request.getParameter("ApiId");
                int aid = Integer.valueOf(apiid);
                authCheck.isUserAuthorizedForAPI(aid);
                JSONObject json = msc.getApi(aid);
                response.getWriter().print(json);
            } else if (action.equalsIgnoreCase("getAllApi")) {
                String modstr = request.getParameter("ModuleId");
                int modid = Integer.valueOf(modstr);
                if (modid == 0) {
                	response.setStatus(HttpServletResponse.SC_BAD_REQUEST);  // Set the status code to 400
                    response.getWriter().print("{\"error\": \"Invalid prodid value of 0\"}");  // Send an error message
//                    response.getWriter().print(new JSONArray());
                } else {
                    authCheck.isUserAuthorizedForMods(modid);
                    JSONArray json = msc.getAllApi(modid);
                    response.getWriter().print(json);
                }
            } else if (action.equalsIgnoreCase("updateApi") || action.equalsIgnoreCase("updateApiSend") ||
                    action.equalsIgnoreCase("updateApiTest") || action.equalsIgnoreCase("addApi") ||
                    action.equalsIgnoreCase("addApiSend") || action.equalsIgnoreCase("addApiTest")) {
                BufferedReader reader = request.getReader();
                JSONParser parser = new JSONParser();
                JSONObject jsonstr = (JSONObject) parser.parse(reader);

                String name = (String) jsonstr.get("name");
                int version = Integer.parseInt(jsonstr.get("version").toString());
                int status = Integer.parseInt(jsonstr.get("status").toString());
                int moduleid = Integer.parseInt(jsonstr.get("moduleId").toString());
                authCheck.isUserAuthorizedForMods(moduleid);
                String envurl = (String) jsonstr.get("envurl");
                String type = (String) jsonstr.get("type");
                String body = (String) jsonstr.get("body");
                String headers = (String) jsonstr.get("headers");
                String url = (String) jsonstr.get("url");
                JSONArray json = (JSONArray) jsonstr.get("param");
                String tests = (String) jsonstr.get("tests");
                int apiid = -1;
                if (action.startsWith("update")) {
                    apiid = Integer.parseInt(jsonstr.get("idapi").toString());
                    msc.updateApi(apiid, name, status, moduleid, envurl, type, body,
                            url, json, headers, tests);
                } else if (action.startsWith("add")) {
                    apiid = msc.addApi(name, version, status, moduleid, envurl, type, body, url, json,
                            randomkey, headers, String.valueOf(version), null, -1, tests);
                }

                if (action.equalsIgnoreCase("updateApiSend") || action.equalsIgnoreCase("addApiSend")) {
                    String jsonStr = null;
                    if (json != null && json.size() > 0) {
                        jsonStr = json.toString();
                    }

                    String res = "";
                    res = ApiHandler.sendRequest(url, type, body, jsonStr, headers, null, null,
                            companyId, apiid, null);
                    JSONObject jo = new JSONObject();
                    jo.put("response", res);
                    response.getWriter().print(jo);
                } else if (action.equalsIgnoreCase("updateApiTest") || action.equalsIgnoreCase("addApiTest")) {
                    String jsonStr = null;
                    if (json != null && json.size() > 0) {
                        jsonStr = json.toString();
                    }
                    int apiresults = MSC.createApiResults(apiid, name, (String) userDetails.get("uname"), envurl,
                            type, body, url, headers, tests);
                    Utilities.createAPIResultsFolder(companyId, apiid, apiresults);
                    String logFilePath = Utilities.getAPILogDirFile(companyId, apiid, apiresults);
                    ExecutionLogger el = new ExecutionLogger(Integer.toString(apiresults), logFilePath);
                    String res = ApiHandler.sendRequest(url, type, body, jsonStr, headers, "test", tests,
                            companyId, apiid, el);
                    JSONObject jo = new JSONObject();
                    String[] resArray = res.split(AppProperties.delimiter);
                    jo.put("status", resArray[0]);
                    jo.put("result", resArray[1]);
                    jo.put("response", resArray[2]);
                    response.getWriter().print(jo);
                } else {
                    JSONObject jo = new JSONObject();
                    jo.put("response", "Successful");
                    response.getWriter().print(jo);
                }
            } else if (action.equalsIgnoreCase("deleteApi")) {
                String modstr = request.getParameter("moduleId");
                int modid = Integer.valueOf(modstr);
                authCheck.isUserAuthorizedForMods(modid);
                String apiidstr = request.getParameter("apiid");
                int apiid = Integer.valueOf(apiidstr);
                MSC.deleteApi(apiid, modid);
                JSONObject jo = new JSONObject();
                jo.put("response", "Successful");
                response.getWriter().print(jo);
            } else if (action.contentEquals("valkeygen")) {
                String uname = request.getParameter("username");
                int valkey = msc.getPwdValidationKey(uname);
                JSONObject emailData = new JSONObject();
                JSONObject userDets = msc.getUserDetails();
                emailData.put("name", uname);
                emailData.put("recipient", uname);
                emailData.put("subject", AppProperties.forgotpwdsubject);
                emailData.put("template", "ForgotPwd.html");
                emailData.put("valkey", valkey);
                EmailQueuingHelper eqh = userCache.eqh;
                eqh.addToQueue(emailData);
            } else if (action.contentEquals("verifyvalkey")) {
                String uname = request.getParameter("username");
                String keyval = request.getParameter("kayval");
                int kval = Integer.valueOf(keyval);
                if (msc.valAndChnagePwd(uname, kval))
                    response.getWriter().print("Token Verified");
                else response.getWriter().print("Token Verification fail");
            } else if (action.contentEquals("forgetPassword")) {
                BufferedReader reader = request.getReader();
                JSONParser parser = new JSONParser();
                JSONObject jsonstr = (JSONObject) parser.parse(reader);
                String uname = (String) jsonstr.get("username");
                String newPassword = (String) jsonstr.get("NewPassword");
                String RENewPassword = (String) jsonstr.get("RENewPassword");
                if (newPassword.equals(RENewPassword)) {
                    msc.changePwd(uname, newPassword);
                } else {
                    response.getWriter().println("New password and Re-Entered password must be same.");
                }
            } else if (action.contentEquals("addLicense")) {
                BufferedReader reader = request.getReader();
                JSONParser parser = new JSONParser();
                JSONObject jsonstr = (JSONObject) parser.parse(reader);
                String licensekey = (String) jsonstr.get("licensekey");
                String companyid = (String) jsonstr.get("companyid");
                int cid = Integer.valueOf(companyid);
                authCheck.isUserAuthorizedForCompany(cid);
                String validtill = (String) jsonstr.get("validtill");
                msc.addLicense(licensekey, cid, validtill);
            } else if (action.contentEquals("addStepRecording")) {
                BufferedReader reader = request.getReader();
                JSONParser parser = new JSONParser();
                JSONObject jsonstr = (JSONObject) parser.parse(reader);
                String Keyword = (String) jsonstr.get("Keyword");
                String Action = (String) jsonstr.get("Action");
                String subAction = (String) jsonstr.get("subAction");
                String Flow = (String) jsonstr.get("Flow");
                String Page_Description = (String) jsonstr.get("Page_Description");
                String Page_Name = (String) jsonstr.get("Page_Name");
                String TestData = (String) jsonstr.get("TestData");
                String variable = (String) jsonstr.get("VarName");
                String type = (String) jsonstr.get("type");
                int tcid = Integer.parseInt(jsonstr.get("Test_Case_Id").toString());
                authCheck.isUserAuthorizedForTC(tcid);
                int tsid = Integer.parseInt(jsonstr.get("Test_Step_Id").toString());
                String xpath = (String) jsonstr.get("ObjectIdentifier");
                int stepnumber = Integer.parseInt(jsonstr.get("stepnumber").toString());
                int prevStepNumber = stepnumber;

                JSONObject lr = Utilities.getLatestRecordingFromCache((String) userDetails.get("uname"), msc);
                JSONObject stepNumObj = (JSONObject) lr.get("stepnums");
                JSONArray stepNumArr = new JSONArray();

                Iterator iter = stepNumObj.keySet().iterator();
                while (iter.hasNext()) {
                    stepNumArr.add(stepNumObj.get(iter.next()));
                }

                // sort the array in ascending order
                JSONArray sortedArray = new JSONArray();
                List list = new ArrayList();
                for (int i = 0; i < stepNumArr.size(); i++) {
                    list.add(stepNumArr.get(i));
                }
                Collections.sort(list);
                for (Object obj : list) {
                    sortedArray.add(obj);
                }

                for (int i = 0; i < sortedArray.size() - 1; i++) {
                    int arrValue = (int) sortedArray.get(i);
                    if (stepnumber == arrValue) {
                        stepnumber = (int) sortedArray.get(i + 1);
                        break;
                    }
                }
                String wait = "Before";
                if (jsonstr.get("wait") != null) {
                    wait = jsonstr.get("wait").toString();
                }
                int waittime = 0;
                if (jsonstr.get("waittime") != null && !jsonstr.get("waittime").equals("")) {
                    waittime = Integer.parseInt(jsonstr.get("waittime").toString());
                }
                int teststepthreshold = 0;
                if (jsonstr.get("teststepthreshold") != null) {
                    teststepthreshold = Integer.parseInt(jsonstr.get("teststepthreshold").toString());
                }
                msc.addTestStep(Keyword, Action, Flow, Page_Description, Page_Name,
                        TestData, tcid, xpath, stepnumber, subAction, wait, waittime,
                        teststepthreshold, variable, type, tsid, prevStepNumber);
                JSONObject json = msc.getTestStepsByTestCaseID(tcid, false);
                userCache.cacheLatestRecording((String) userDetails.get("uname"), json);
                response.getWriter().print(json);
            } else if (action.contentEquals("addBlankRecording")) {
                BufferedReader reader = request.getReader();
                JSONParser parser = new JSONParser();
                JSONObject jsonstr = (JSONObject) parser.parse(reader);
                String Keyword = (String) jsonstr.get("Keyword");
                String Action = (String) jsonstr.get("Action");
                String subAction = (String) jsonstr.get("subAction");
                String Flow = (String) jsonstr.get("Flow");
                String Page_Description = (String) jsonstr.get("Page_Description");
                String Page_Name = (String) jsonstr.get("Page_Name");
                String TestData = (String) jsonstr.get("TestData");
                String variable = (String) jsonstr.get("VarName");
                String type = (String) jsonstr.get("type");
                int tcid = Integer.parseInt(jsonstr.get("Test_Case_Id").toString());
                authCheck.isUserAuthorizedForTC(tcid);
                int tsid = Integer.parseInt(jsonstr.get("tsid").toString());
                String xpath = (String) jsonstr.get("ObjectIdentifier");
                int stepnumber = Integer.parseInt(jsonstr.get("stepnumber").toString());
                int prevStepNumber = stepnumber;

                JSONObject lr = Utilities.getLatestRecordingFromCache((String) userDetails.get("uname"), msc);
                JSONObject stepNumObj = (JSONObject) lr.get("stepnums");
                JSONArray stepNumArr = new JSONArray();

                Iterator iter = stepNumObj.keySet().iterator();
                while (iter.hasNext()) {
                    stepNumArr.add(stepNumObj.get(iter.next()));
                }

                // sort the array in ascending order
                JSONArray sortedArray = new JSONArray();
                List list = new ArrayList();
                for (int i = 0; i < stepNumArr.size(); i++) {
                    list.add(stepNumArr.get(i));
                }
                Collections.sort(list);
                for (Object obj : list) {
                    sortedArray.add(obj);
                }

                for (int i = 0; i < sortedArray.size() - 1; i++) {
                    int arrValue = (int) sortedArray.get(i);
                    if (stepnumber == arrValue) {
                        stepnumber = (int) sortedArray.get(i + 1);
                        break;
                    }
                }
                String wait = "Before";
                if (jsonstr.get("wait") != null) {
                    wait = jsonstr.get("wait").toString();
                }
                int waittime = 0;
                if (jsonstr.get("waittime") != null && !jsonstr.get("waittime").equals("")) {
                    waittime = Integer.parseInt(jsonstr.get("waittime").toString());
                }
                int teststepthreshold = 0;
//				if(jsonstr.get("teststepthreshold") != null) {
//					teststepthreshold = Integer.parseInt(jsonstr.get("teststepthreshold").toString());
//				}
                msc.addBlankTestStep(Keyword, Action, Flow, Page_Description, Page_Name,
                        TestData, tcid, xpath, stepnumber, subAction, wait, waittime,
                        teststepthreshold, variable, type, tsid, prevStepNumber);
                JSONObject json = msc.getTestStepsByTestCaseID(tcid, false);
                userCache.cacheLatestRecording((String) userDetails.get("uname"), json);
                response.getWriter().print(json);
            } else if (action.contentEquals("updateStepRecording")) {
                BufferedReader reader = request.getReader();
                JSONParser parser = new JSONParser();
                JSONObject jsonstr = (JSONObject) parser.parse(reader);
                String Keyword = (String) jsonstr.get("Keyword");
                String Action = (String) jsonstr.get("Action");
                String subAction = (String) jsonstr.get("subAction");
                String Flow = (String) jsonstr.get("Flow");
                String Page_Description = (String) jsonstr.get("Page_Description");
                String Page_Name = (String) jsonstr.get("Page_Name");
                String TestData = (String) jsonstr.get("TestData");
                String variable = (String) jsonstr.get("VarName");
                String type = (String) jsonstr.get("type");
                String iframe = (String) jsonstr.get("iframexpath");
                String condLogic = (String) jsonstr.get("condLogic");
                String condexp = (String) jsonstr.get("condexp");
                int tabid = Integer.parseInt(jsonstr.get("tabid").toString());
                if (TestData == null) {
                    TestData = "";
                }
                int tcid = Integer.parseInt(jsonstr.get("Test_Case_Id").toString());
                int inlinetcid = 0;
                if (jsonstr.get("inlinedepedence") != null && !(jsonstr.get("inlinedepedence") instanceof Long)) {
                    inlinetcid = Integer.parseInt((String) jsonstr.get("inlinedepedence"));
                }
                authCheck.isUserAuthorizedForTC(tcid);
                String xpath = (String) jsonstr.get("ObjectIdentifier");
                int tsid = Integer.parseInt(jsonstr.get("tsid").toString());
                String wait = "Before";
                if (jsonstr.get("wait") != null) {
                    wait = jsonstr.get("wait").toString();
                }
                int waittime = 0;
                if (jsonstr.get("waittime") != null) {
                    waittime = Integer.parseInt(jsonstr.get("waittime").toString());
                }
                int teststepthreshold = 0;
                if (jsonstr.get("teststepthreshold") != null) {
                    teststepthreshold = Integer.parseInt(jsonstr.get("teststepthreshold").toString());
                }
                int isDynamic = 0;
                if (jsonstr.get("isDynamic") != null) {
                    isDynamic = Integer.parseInt(jsonstr.get("isDynamic").toString());
                }
                int createsAlert = 0;
                if (jsonstr.get("createsAlert") != null) {
                    createsAlert = Integer.parseInt(jsonstr.get("createsAlert").toString());
                }
                msc.updateTestStep(tsid, Keyword, Action, subAction, Flow, Page_Description, Page_Name,
                        TestData, tcid, xpath, wait, waittime, teststepthreshold, variable, type, tabid, iframe,
                        isDynamic, createsAlert, inlinetcid, condLogic,condexp );
                JSONObject json = msc.getTestStepsByTestCaseID(tcid, false);
                userCache.cacheLatestRecording((String) userDetails.get("uname"), json);
                response.getWriter().print(json);
            } else if (action.equalsIgnoreCase("deleteTestSteps")) {
                String testcaseid = request.getParameter("testCaseId");
                int tcid = Integer.valueOf(testcaseid);
                authCheck.isUserAuthorizedForTC(tcid);

                BufferedReader reader = request.getReader();
                JSONParser parser = new JSONParser();
                JSONArray testSteps = (JSONArray) parser.parse(reader);

                msc.deleteTestSteps(testSteps);
                response.getWriter().print(testSteps);
            } else if (action.equalsIgnoreCase("updateBulkFlows")) {
            	
                String flowAction = request.getParameter("Flow");
                String testcaseId = request.getParameter("testCaseId");
                int tcid = Integer.valueOf(testcaseId);

                authCheck.isUserAuthorizedForTC(tcid);

                BufferedReader reader = request.getReader();
                JSONParser parser = new JSONParser();
                JSONObject requestBody = (JSONObject) parser.parse(reader);
                
                JSONArray testStepIds = (JSONArray) requestBody.get("testStepIds");

            	JSONObject updateResult = msc.updateFlowForBulkTestSteps(flowAction, testStepIds);
                
                JSONObject successResponse = new JSONObject();
                successResponse.put("status", "success");
                successResponse.put("message", "Flow update completed.");
                successResponse.put("data", updateResult);
                
                response.setContentType("application/json");
                response.getWriter().print(successResponse.toJSONString());
                
            } else if (action.equalsIgnoreCase("updateBulkWaitTime")) {
            	
                String wait = request.getParameter("wait");
                String waittime = request.getParameter("waittime");
                String testcaseId = request.getParameter("testCaseId");
                int tcid = Integer.valueOf(testcaseId);

                authCheck.isUserAuthorizedForTC(tcid);

                BufferedReader reader = request.getReader();
                JSONParser parser = new JSONParser();
                JSONObject requestBody = (JSONObject) parser.parse(reader);
                
                JSONArray testStepIds = (JSONArray) requestBody.get("testStepIds");

            	JSONObject updateResult = msc.updateWaitForBulkTestSteps(wait, waittime, testStepIds);
                
                
                JSONObject successResponse = new JSONObject();
                successResponse.put("status", "success");
                successResponse.put("message", "Flow update completed.");
                successResponse.put("data", updateResult);
                
                response.setContentType("application/json");
                response.getWriter().print(successResponse.toJSONString());
            } else if (action.equalsIgnoreCase("updateBulkCondLogic")) {
            	
                String condLogic = request.getParameter("condLogic");
                String condexp = request.getParameter("condexp");
                String testcaseId = request.getParameter("testCaseId");
                int tcid = Integer.valueOf(testcaseId);

                authCheck.isUserAuthorizedForTC(tcid);

                BufferedReader reader = request.getReader();
                JSONParser parser = new JSONParser();
                JSONObject requestBody = (JSONObject) parser.parse(reader);
                
                JSONArray testStepIds = (JSONArray) requestBody.get("testStepIds");

            	JSONObject updateResult = msc.updateCondLogicForBulkTestSteps(condLogic, condexp, testStepIds);
                
                
                JSONObject successResponse = new JSONObject();
                successResponse.put("status", "success");
                successResponse.put("message", "Conditional Logic update completed.");
                successResponse.put("data", updateResult);
                
                response.setContentType("application/json");
                response.getWriter().print(successResponse.toJSONString());    
                
            } else if (action.equalsIgnoreCase("updateBulkKeywordsAndAction")) {
            	
                String Keyword = request.getParameter("Keyword");
                String Action = request.getParameter("Action");
                String subAction = request.getParameter("subAction");
                if (subAction == null || subAction.trim().isEmpty()) {
                    subAction = null;
                }
                String testcaseId = request.getParameter("testCaseId");
                int tcid = Integer.valueOf(testcaseId);

                authCheck.isUserAuthorizedForTC(tcid);

                BufferedReader reader = request.getReader();
                JSONParser parser = new JSONParser();
                JSONObject requestBody = (JSONObject) parser.parse(reader);
                
                JSONArray testStepIds = (JSONArray) requestBody.get("testStepIds");

            	JSONObject updateResult = msc.updateActionForBulkTestSteps(Keyword, Action, subAction, testStepIds);
                
                
                JSONObject successResponse = new JSONObject();
                successResponse.put("status", "success");
                successResponse.put("message", "Keyword and Actions are updated successfully");
                successResponse.put("data", updateResult);
                
                response.setContentType("application/json");
                response.getWriter().print(successResponse.toJSONString());
                
            } else if (action.equalsIgnoreCase("nonMatchingAttr")) {
            	String teststepid = request.getParameter("teststepid");
            	String TestStepResultId = request.getParameter("TSRId");
            	int tsid = Integer.valueOf(teststepid);
            	int tsrid = Integer.valueOf(TestStepResultId);
            	int tcid = msc.gettcidfromTestStep(tsid);
            	int passedTcrid = -1;
            	passedTcrid = msc.getLatestPassedTestCaseResultsId(tcid);
        		if(passedTcrid == -1) {
        			passedTcrid = msc.getLatestPassedTestStepResultsId(tsid);
        		}
            	
            	String nonMatchingAttr = msc.getNonMatchingAttr(tsid);
            	String foundOuterHTML = msc.getOuterHTML(tsrid);
            	JSONArray foundelements = msc.getAllFoundElementsFromDB(tsrid);
            	String recordedOuterHTML = msc.getRecordedOuterHTML(tsid);
            	String lastPassedOuterHTML = msc.getLastPassedOuterHTML(tsid, passedTcrid);
            	
            	JSONObject json = new JSONObject();
            	json.put("nonmatchingattr", nonMatchingAttr);
            	json.put("foundOuterHTML", foundOuterHTML);
            	json.put("foundelements", foundelements);
            	json.put("recordedOuterHTML", recordedOuterHTML);
            	json.put("lastPassedOuterHTML", lastPassedOuterHTML);
            	
            	if(passedTcrid != -1) {
            		json.put("ptcrid", passedTcrid);
            	} else {
            		json.put("ptcrid", "Data not available");
            	}
            	response.getWriter().print(json);
            } else if (action.equalsIgnoreCase("approveNonMatchingAttr")) {
            	String teststepid = request.getParameter("teststepid");
            	String elementId = request.getParameter("elementid");
            	int tsid = Integer.valueOf(teststepid);
            	int elementid = Integer.valueOf(elementId);
            	String uname = (String) userDetails.get("uname");
            	
            	String outerhtml = msc.getOuterHTMLWithId(elementid);
            	msc.updateApprovedElementToDB(tsid, outerhtml);
            	msc.approveAttr(tsid, uname);
            	JSONObject json = new JSONObject();
            	json.put("message", "Successfully Approved");
            	response.getWriter().print(json);
            } else if(action.equalsIgnoreCase("deletePassedOuterHTML")) {
            	String teststepresultid = request.getParameter("testStepResultId");
            	int tsrid = Integer.valueOf(teststepresultid);
            	msc.removePassedOuterHTML(tsrid);
            	JSONObject json = new JSONObject();
            	json.put("message", "Successfully Deleted");
            	response.getWriter().print(json);
            } else if (action.equalsIgnoreCase("changeToAssert")) {
                String testcaseid = request.getParameter("testCaseId");
                int tcid = Integer.valueOf(testcaseid);
                authCheck.isUserAuthorizedForTC(tcid);

                BufferedReader reader = request.getReader();
                JSONParser parser = new JSONParser();
                JSONArray testSteps = (JSONArray) parser.parse(reader);

                msc.changeToAssert(testSteps);
                response.getWriter().print(testSteps);
            } else if (action.equalsIgnoreCase("replaceTestSteps")) {
                String testcaseid = request.getParameter("testCaseId");
                String replaceTcId = request.getParameter("replaceTcId");
                int tcid = Integer.valueOf(testcaseid);
                int replacetcid = Integer.valueOf(replaceTcId);
                authCheck.isUserAuthorizedForTC(tcid);

                BufferedReader reader = request.getReader();
                JSONParser parser = new JSONParser();
                JSONArray testSteps = (JSONArray) parser.parse(reader);
                msc.updateDependantTC(tcid, replacetcid, testSteps);
                response.getWriter().print(testSteps);
            } else if (action.equalsIgnoreCase("updateFindAndAssignData")) {
				int testStepId = Integer.parseInt(request.getParameter("testStepId"));
				String stepAction = request.getParameter("stepAction");
				String newAndroidUIAutomator = request.getParameter("newAndroidUIAutomator");
				String newXpath = request.getParameter("newXpath");
				String newIdStrategy = request.getParameter("newIdStrategy");
				authCheck.isUserAuthorizedForTS(testStepId);
				try {
					msc.updateFindAndAssignData(testStepId, stepAction, newAndroidUIAutomator, newXpath, newIdStrategy);
					response.getWriter().print("Locators updated successfully");
				} catch (Exception e) {
					response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
					response.getWriter().print("Error Updating the Locators ");
					e.printStackTrace();
				}
			} else if (action.equalsIgnoreCase("updateMobileStepAction")) {
				int testStepId = Integer.parseInt(request.getParameter("testStepId"));
				String newAction = request.getParameter("newAction");
				authCheck.isUserAuthorizedForTS(testStepId);
				try {
					msc.updateMobileStepAction(testStepId, newAction);
					response.getWriter().print("Mobile action updated successfully.");
				} catch (Exception e) {
					response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
					response.getWriter().print("Error updating action. ");
					e.printStackTrace();
				}
            } else if (action.equalsIgnoreCase("inlineTestSteps")) {
                String testcaseid = request.getParameter("testCaseId");
                String inlineTcId = request.getParameter("inlineTcId");
                int tcid = Integer.valueOf(testcaseid);
                int inlinetcid = Integer.valueOf(inlineTcId);
                authCheck.isUserAuthorizedForTC(tcid);

                BufferedReader reader = request.getReader();
                JSONParser parser = new JSONParser();
                JSONArray testSteps = (JSONArray) parser.parse(reader);
                msc.updateInlineTC(tcid, inlinetcid, testSteps);
                response.getWriter().print(testSteps);
            } else if (action.contentEquals("updateStepResults")) {
                BufferedReader reader = request.getReader();
                JSONParser parser = new JSONParser();
                JSONObject jsonstr = (JSONObject) parser.parse(reader);
                String Keyword = (String) jsonstr.get("Keyword");
                String Action = (String) jsonstr.get("Action");
                String subAction = (String) jsonstr.get("subAction");
                String Flow = (String) jsonstr.get("Flow");
                String Page_Description = (String) jsonstr.get("Page_Description");
                String Page_Name = (String) jsonstr.get("Page_Name");
                String TestData = (String) jsonstr.get("TestData");
                String ThresholdStr = (String) jsonstr.get("Threshold");
                String variable = (String) jsonstr.get("Variable");
                String type = (String) jsonstr.get("Type");
                String iframe = (String) jsonstr.get("iframe");
                String condLogic = (String) jsonstr.get("condLogic");
                String condexp = (String) jsonstr.get("condexp");
                int tabid = Integer.parseInt(jsonstr.get("tabid").toString());
                int Threshold = Integer.valueOf(ThresholdStr);
                if (TestData == null) {
                    TestData = "";
                }
                int isDynamic = 0;
                if (jsonstr.get("isDynamic") != null) {
                    isDynamic = Integer.parseInt(jsonstr.get("isDynamic").toString());
                }
                int createsAlert = 0;
                if (jsonstr.get("isDynamic") != null) {
                    isDynamic = Integer.parseInt(jsonstr.get("createsAlert").toString());
                }
                long tcidl = (long) jsonstr.get("Test_Case_Id");
                int tcid = (int) tcidl;
                authCheck.isUserAuthorizedForTC(tcid);
                String xpath = (String) jsonstr.get("ObjectIdentifier");
                long tsidl = (long) jsonstr.get("tsid");
                int tsid = (int) tsidl;
                msc.updateTestStep(tsid, Keyword, Action, subAction, Flow, Page_Description, Page_Name,
                        TestData, tcid, xpath, null, 0, Threshold, variable, type, tabid, iframe, isDynamic,
                        createsAlert, 0, condLogic, condexp);
                JSONObject json = msc.getTestStepsByTestCaseID(tcid, false);
                userCache.cacheLatestRecording((String) userDetails.get("uname"), json);
                response.getWriter().print(json);
            } else if (action.contentEquals("addCustProperties")) {
                BufferedReader reader = request.getReader();
                JSONParser parser = new JSONParser();
                JSONObject jsonstr = (JSONObject) parser.parse(reader);
                String name = (String) jsonstr.get("name");
                String value = (String) jsonstr.get("value");
                int productid = Integer.parseInt(jsonstr.get("productid").toString());
                authCheck.isUserAuthorizedForProd(productid);
                String comments = (String) jsonstr.get("comments");
                msc.addCustProperties(name, value, productid, comments);
            } else if (action.contentEquals("updateCustProperties")) {
                BufferedReader reader = request.getReader();
                JSONParser parser = new JSONParser();
                JSONObject jsonstr = (JSONObject) parser.parse(reader);
                int idcustprops = Integer.parseInt(jsonstr.get("idcustprops").toString());
                String name = (String) jsonstr.get("name");
                String value = (String) jsonstr.get("value");
                int productid = Integer.parseInt(jsonstr.get("productid").toString());
                authCheck.isUserAuthorizedForProd(productid);
                String comments = (String) jsonstr.get("comments");
                msc.updateCustProperties(idcustprops, name, value, productid, comments);
            } else if (action.equals("getCustProperties")) {
                String prodid = request.getParameter("productid");
                int pid = new Integer(prodid).intValue();
                authCheck.isUserAuthorizedForProd(pid);
                JSONArray json = msc.getCustProperties(pid);
                response.getWriter().print(json);
            } else if (action.equals("updateRecordingCache")) {
                String uname = request.getParameter("uname");
                int companyid = Integer.parseInt(request.getParameter("companyid"));
                JSONObject ud = new JSONObject();
                ud.put("uname", uname);
                ud.put("companyid", companyid);
                msc.setUserDetails(ud);
                try {
                    JSONObject jsonObject = msc.getLatestRecording(uname);
                    userCache.cacheLatestRecording(uname, jsonObject);
                    response.getWriter().print("success");
                } catch (Exception e) {
                    System.err.println(Utilities.getNow());
                    e.printStackTrace();
                    userCache.uncacheLatestRecording(uname);
                }
            } else if (action.equals("logout")) {
                String uname = (String) userDetails.get("uname");
                Utilities.flushCache(uname, randomkey);
            } else if (action.equals("getLatestPass")) {
                String testcaseid = request.getParameter("testcaseId");
                int tcid = Integer.valueOf(testcaseid);
                authCheck.isUserAuthorizedForTC(tcid);
                JSONArray json = msc.getLatestPass(tcid, companyId);
                response.getWriter().print(json);
            } else if (action.equals("getTsFromTcr")) {
                String testcase = request.getParameter("tcrid");
                if(testcase ==  null || testcase.equals("") || testcase.equals("null")) {
                	response.setStatus(HttpServletResponse.SC_BAD_REQUEST);  // Set the status code to 400
                    response.getWriter().print("{\"error\": \"Invalid TCR id value of null or blank\"}");  // Send an error message
                    JSONArray json = new JSONArray();
                    response.getWriter().print(json);
                    return;
                }
                int tcid = Integer.valueOf(testcase);
                authCheck.isUserAuthorizedForTCR(tcid);
                JSONArray json = msc.getTestStepFromTestCaseResult(tcid, false);
                response.getWriter().print(json);
            } else if (action.equals("getTsrFromTc")) {
                String testcase = request.getParameter("tcid");
                int tcid = -1;
                if (testcase != null && !testcase.equals("") && !testcase.equals("null") &&
                        !testcase.equals("undefined")) {
                    tcid = Integer.valueOf(testcase);
                    authCheck.isUserAuthorizedForTC(tcid);
                    JSONArray json = msc.getTestStepResultFromTestCase(tcid, companyId);
                    response.getWriter().print(json);
                } else {
                	response.setStatus(HttpServletResponse.SC_BAD_REQUEST);  // Set the status code to 400
                    response.getWriter().print("{\"error\": \"Invalid test case id value of null or blank or undefined\"}");  // Send an error message
//                    response.getWriter().print(new JSONArray());
                }

            } else if (action.equals("copyTC")) {
                String testcase = request.getParameter("tcid");
                int tcid = Integer.valueOf(testcase);
                authCheck.isUserAuthorizedForTC(tcid);
                String tcname = request.getParameter("tcname");
                JSONObject json = msc.copyTestCase(tcid, tcname);
                response.getWriter().print(json);
            } else if (action.equals("compare")) {
                JSONObject jsonstr = new JSONObject();
                int tcid = Integer.parseInt(request.getParameter("tcid"));
                authCheck.isUserAuthorizedForTC(tcid);
                int tcrid = Integer.parseInt(request.getParameter("tcrid"));
                int tsid = Integer.parseInt(request.getParameter("tsid"));
                int stepNumber = Integer.parseInt(request.getParameter("stepNumber"));
                String failScreenShot = (String) request.getParameter("failScreenshot");
                String fss = getSplitFileName(failScreenShot);
                ImageCompare IC = new ImageCompare();
                String passScreenShot = IC.getLatestPassScreenshotUsingTestStepId(tcid, companyId, tsid, msc);
                String pss = getSplitFileName(passScreenShot);
                String[] str = IC.testCompareImage(checker, fss, pss, tcrid, tsid, tcid, companyId, stepNumber);
                String url = "http://localhost:9000/keyloggingv2/ReactApp?action=downloadFile&token=" + randomkey + "&fileName=" + str[2];
                jsonstr.put("passScreenshot", passScreenShot);
                jsonstr.put("failScreenshot", failScreenShot);
                jsonstr.put("diffScreenshot", url);
                jsonstr.put("passText", str[0]);
                jsonstr.put("failText", str[1]);
                checker = false;
                response.getWriter().print(jsonstr);
            } else if (action.equals("reorderTestcase")) {
                String testcase = request.getParameter("tcid");
                int tcid = Integer.valueOf(testcase);
                String testsuiteid = request.getParameter("tsuid");
                int tsuid = Integer.valueOf(testsuiteid);
                authCheck.isUserAuthorizedForTSuite(tsuid);
                String source = request.getParameter("index");
                int src = Integer.valueOf(source);
                String destination = request.getParameter("destindex");
                int dstn = Integer.valueOf(destination);
                String pageNum = request.getParameter("pnum");
                int pnum;
                if (pageNum == null || pageNum.equals("") || pageNum.equals("undefined")) {
                    pnum = 1;
                } else {
                    pnum = new Integer(pageNum).intValue();
                }
                JSONArray json = msc.reorderTestcase(tcid, tsuid, src, dstn, pnum);
                response.getWriter().print(json);
            } else if (action.equals("changetcname")) {
                String testcase = request.getParameter("tcid");
                int tcid = Integer.valueOf(testcase);
                authCheck.isUserAuthorizedForTC(tcid);
                String tcname = request.getParameter("tcname");
                JSONObject json = msc.changeTestCaseName(tcid, tcname);
                response.getWriter().print(json);
            } else if (action.equals("stopTestSuite")) {
                String type = request.getParameter("type");
                String stopid = request.getParameter("id");
                int id = Integer.valueOf(stopid);
                if (type.equals("suite")) {
                    authCheck.isUserAuthorizedForTSR(id);
                    Utilities.cacheStopRequest(id);
                    msc.updateTestSuiteStatus(id, "STOPPED");
                    response.getWriter().print("Caching of suite Complete");
                } else if (type.equals("case")) {
                    authCheck.isUserAuthorizedForTCR(id);
                    Utilities.cacheStopCaseRequest(id);
                    msc.stopTestCase(id, "STOPPED");
                    response.getWriter().print("Caching of case Complete");
                } else response.getWriter().print("Caching not Complete as type is not defined");
            } else if (action.equals("getlicencekey")) {
                authCheck.isUserAuthorizedForCompany(companyId);
                String licencekey = msc.getLicenceToken(companyId);
                response.getWriter().print(licencekey);
            } else if (action.equals("setEmailDetails")) {
                BufferedReader reader = request.getReader();
                JSONParser parser = new JSONParser();
                JSONObject jsonstr = (JSONObject) parser.parse(reader);
                long prod = (long) jsonstr.get("idproducts");
                int prodId = (int) prod;
                authCheck.isUserAuthorizedForProd(prodId);
                String emailAction = (String) jsonstr.get("action");

                if (emailAction != null) {
                    if (emailAction.equals("ReadEmail")) {
                        String name = "mailConnetionCred";
                        String emailhost = (String) jsonstr.get("host");
                        String protocol = (String) jsonstr.get("protocol");
                        long port = (long) jsonstr.get("port");
                        int portNo = (int) port;
                        msc.insertEmailConnectDetails(prodId, name, emailhost, protocol, portNo);
                    } else if (emailAction.equals("sendEmail")) {
                        String name = "mailSendConnectionCred";
                        String emailhost = (String) jsonstr.get("sendhost");
                        long port = (long) jsonstr.get("sendport");
                        int portNo = (int) port;
                        msc.insertSendEmailConnectDetails(prodId, name, emailhost, portNo);
                    }
                }
                response.getWriter().print("Credential added successful");
            } else if (action.equals("setDbDetails")) {
                BufferedReader reader = request.getReader();
                JSONParser parser = new JSONParser();
                JSONObject jsonstr = (JSONObject) parser.parse(reader);
                long prod = (long) jsonstr.get("idproducts");
                int prodId = (int) prod;
                authCheck.isUserAuthorizedForProd(prodId);
                String name = "dbConnectionCred";
                String DBUrl = (String) jsonstr.get("custDbUrl");
                String DBUserName = (String) jsonstr.get("custDbUsername");
                String DBPwd = (String) jsonstr.get("custDbPwd");
                String DBType = "";
                if (jsonstr.containsKey("DBOtherType")) {
                    DBType = (String) jsonstr.get("DBOtherType");
                } else DBType = (String) jsonstr.get("custDbType");
                msc.insertDbConnectDetails(prodId, name, DBUrl, DBUserName, DBPwd, DBType);
                response.getWriter().print("Credential added successful");
            } else if (action.equals("getEmailConnectionDetails")) {
                String prod = request.getParameter("idproducts");
                int prodid = Integer.valueOf(prod);
                authCheck.isUserAuthorizedForProd(prodid);
                JSONObject json = msc.getEmailConnectionDetails(prodid);
                response.getWriter().print(json);
            } else if (action.equals("getDbConnectionDetails")) {
                String prod = request.getParameter("idproducts");
                int prodid = Integer.valueOf(prod);
                authCheck.isUserAuthorizedForProd(prodid);
                JSONObject json = msc.getDbConnectionDetails(prodid);
                response.getWriter().print(json);
            } else if (action.equals("getTestCaseStatusCount")) {
                String to = request.getParameter("to");
                String from = request.getParameter("from");
                JSONArray json = msc.getTCRBetweenRanges(to, from);
                response.getWriter().print(json);
            } else if (action.equals("deleteTC")) {
                String testcaseid = request.getParameter("tcid");
                int tcid = Integer.valueOf(testcaseid);
                authCheck.isUserAuthorizedForTC(tcid);
                msc.deleteTestCase(tcid);
                response.getWriter().print("Test Case Deleted");
            } else if (action.equals("getCompanyNameFromURL")) {
                String companyurl = request.getParameter("companyUrl");
                JSONObject companyDetails = msc.getCompanyDetailsFromURL(companyurl);

                if (companyDetails.isEmpty()) {
                    response.getWriter().print("Invalid Company");
                } else {
                    response.getWriter().print(companyDetails.toJSONString());
                }
            } else if (action.equals("addBranding")) {
                authCheck.isUserAuthorizedForCompany(companyId);
                Utilities.createBrandingFolder(companyId);
                String primaryColor = request.getParameter("primaryColor");
                String secondaryColor = request.getParameter("secondaryColor");
                String tertiaryColor = request.getParameter("tertiaryColor");
                String brandingLogo = request.getParameter("brandLogo");
                JSONObject json = new JSONObject();
                if (brandingLogo == null || brandingLogo.equals("null") || brandingLogo.equals("") || brandingLogo.equals("undefined")) {
                    json = msc.addBranding(companyId, primaryColor, secondaryColor, tertiaryColor, null);
                } else {
                    InputStream is = request.getInputStream();
                    String extension = "";
                    int lastDotIndex = brandingLogo.lastIndexOf('.');
                    if (lastDotIndex >= 0) {
                        extension = brandingLogo.substring(lastDotIndex);
                    }
                    String newFilename = "_logo_" + companyId + "_" + extension;
                    String Name = Utilities.getBrandingDir(companyId, newFilename);
                    File file = new File(Name);
                    copyInputStreamUsingFiles(is, Name);
                    json = msc.addBranding(companyId, primaryColor, secondaryColor, tertiaryColor, newFilename);
                }
                response.getWriter().print(json);
            } else if (action.equals("getBranding")) {
                authCheck.isUserAuthorizedForCompany(companyId);
                JSONObject json = msc.getBranding(companyId, randomkey);
                response.getWriter().print(json);
            } else if (action.equals("suiteScheduler")) {
                int suiteId = Integer.valueOf(request.getParameter("suiteid"));
                authCheck.isUserAuthorizedForTSuite(suiteId);
                String time = request.getParameter("time");
                String date = request.getParameter("date");
                if (date == null || date.equals("null")) {
                    date = "";
                }
//				String dateTime = date + " " + time;
                String frequency = request.getParameter("frequency");
                String subfrequency = request.getParameter("subfreq");
                JSONObject json = msc.suiteScheduler(suiteId, date, time, frequency, subfrequency);
                response.getWriter().print(json);
            } else if (action.equals("getSchedule")) {
                if (request.getParameter("suiteid") != null &&
                        (!((String) request.getParameter("suiteid")).equals(""))) {
                    int suiteId = Integer.valueOf(request.getParameter("suiteid"));
                    authCheck.isUserAuthorizedForTSuite(suiteId);
                    JSONObject json = msc.getScheduler(suiteId);
                    response.getWriter().print(json.toJSONString());
                }
            } else if (action.equals("deleteSchedule")) {
                if (request.getParameter("suiteid") != null &&
                        (!((String) request.getParameter("suiteid")).equals(""))) {
                    int suiteId = Integer.valueOf(request.getParameter("suiteid"));
                    authCheck.isUserAuthorizedForTSuite(suiteId);
                    msc.deleteScheduler(suiteId);
                    response.getWriter().print("Delete Successful");
                }
            } else if (action.equals("uploadZipFile")) {
                authCheck.isUserAuthorizedForCompany(companyId);
                String fileName = request.getParameter("fileName");
                if (fileName != null || !fileName.equals("null") || !fileName.equals("") || !fileName.equals("undefined")) {
                    InputStream is = request.getInputStream();
                    Utilities.createZipFilePath(companyId);
                    String directoryPath = Utilities.getCompanyZipFolder(companyId);
                    File directory = new File(directoryPath);
                    if (directory.exists() && directory.isDirectory()) {
                        File file = new File(directory, fileName);
                        if (file.exists() && !file.isDirectory()) {
                            String timestamp = new SimpleDateFormat("yyyyMMddHHmmss").format(new java.util.Date());
                            String newFileName = timestamp + "_" + fileName;
                            File newFile = new File(directory, newFileName);
                            file.renameTo(newFile);
                        }
                    } else {
                        System.out.println("The directory does not exist.");
                    }
                    String Name = Utilities.getZipFilePath(companyId, fileName);
                    File file = new File(Name);
                    copyInputStreamUsingFiles(is, Name);
                    msc.uploadFile(companyId, fileName);
                }
            } else if (action.equals("uploadTestDataFile")) {

                int prodid = Integer.valueOf(request.getParameter("prodid"));
                authCheck.isUserAuthorizedForProd(prodid);
                String fileName = request.getParameter("fileName");
                if (fileName != null || !fileName.equals("null") || !fileName.equals("") || !fileName.equals("undefined")) {
                    InputStream is = request.getInputStream();
                    Utilities.createProductPath(companyId, prodid);
                    String Name = Utilities.getTestDataZipPath(companyId, prodid, fileName);
                    File file = new File(Name);
                    copyInputStreamUsingFiles(is, Name);
                    msc.uploadTestDataFile(companyId, prodid, fileName);
                    response.getWriter().print("Uploaded Successfully");
                }
//				response.getWriter().print("Filename is invalid");
            } else if (action.equals("getfiledata")) {
            	authCheck.isUserAuthorizedForCompany(companyId);
                JSONArray json = msc.getTestDataFiles(companyId);
                response.getWriter().print(json);
            } else if (action.equals("getfiledatanames")) {
                int prodid = Integer.valueOf(request.getParameter("productid"));
                authCheck.isUserAuthorizedForProd(prodid);
                JSONObject json = msc.getTestDataFileNames(companyId, prodid);
                response.getWriter().print(json);
            } else if (action.equals("deleteProductFile")) {
                BufferedReader reader = request.getReader();
                JSONParser parser = new JSONParser();
                JSONArray jsonArr = (JSONArray) parser.parse(reader);
                authCheck.isUserAuthorizedForCompany(companyId);
                msc.deleteFileforProduct(jsonArr, companyId);
                response.getWriter().print("Deleted Successfully");
            } else if (action.equals("refreshzip")) {
                authCheck.isUserAuthorizedForCompany(companyId);
                JSONObject json = Utilities.getZipUploadFromCache((String) userDetails.get("uname"), companyId, msc);
                response.getWriter().print(json);
            } else if (action.equals("gcreds")) {
                authCheck.isUserAuthorizedForCompany(companyId);
                String fileName = request.getParameter("fileName");
                if (fileName != null || !fileName.equals("null") || !fileName.equals("") || !fileName.equals("undefined")) {
                    InputStream is = request.getInputStream();
                    Utilities.createGoogleOAuthCredPath(companyId);
                    String Name = Utilities.getGoogleAuthCredPath(companyId, AppProperties.gsheetCredsFile);
                    File file = new File(Name);
                    copyInputStreamUsingFiles(is, Name);
                    String url = GoogleSheets.gSheetsAuthorization(companyId);
                    JSONObject json = new JSONObject();
                    json.put("url", url);
                    json.put("message", "success");
                    response.getWriter().print(json);
                }
            } else if (action.equals("getFileUploads")) {
                String compid = request.getParameter("company");
                String prodid = request.getParameter("prodid");
                String modid = request.getParameter("modid");
                String tcid = request.getParameter("tcid");
                String tsid = request.getParameter("tsid");
                if ((prodid == null || prodid.equals("")) && (modid == null || modid.equals("")) && (tcid == null || tcid.equals("")) && (compid != null || (!compid.equals("")))) {
                    int cid = Integer.valueOf(request.getParameter("company"));
                    authCheck.isUserAuthorizedForCompany(cid);
                    JSONArray json = msc.getUploadfileByCompany(cid);
                    response.getWriter().print(json);
                } else if ((modid == null || modid.equals("")) && (tcid == null || tcid.equals("")) && (prodid != null || (!prodid.equals(""))) && (tsid == null || tsid.equals(""))) {
                    int cid = Integer.valueOf(compid);
                    int pid = Integer.valueOf(prodid);
                    authCheck.isUserAuthorizedForCompany(cid);
                    JSONArray json = msc.getUploadfileByProduct(cid, pid);
                    response.getWriter().print(json);
                } else if ((tcid == null || tcid.equals("")) && (modid != null || (!modid.equals(""))) && (tsid == null || tsid.equals(""))) {
                    int cid = Integer.valueOf(compid);
                    int pid = Integer.valueOf(prodid);
                    int mid = Integer.valueOf(modid);
                    authCheck.isUserAuthorizedForCompany(cid);
                    JSONArray json = msc.getUploadfileByModule(cid, pid, mid);
                    response.getWriter().print(json);
                } else if ((prodid != null || (!prodid.equals(""))) && (compid != null || (!compid.equals(""))) && (tsid != null || (!tsid.equals(""))) && (tcid == null || tcid.equals(""))) {
                    int cid = Integer.valueOf(compid);
                    int pid = Integer.valueOf(prodid);
                    int testid = Integer.valueOf(tsid);
                    authCheck.isUserAuthorizedForCompany(cid);
                    JSONArray json = msc.getUploadfileByTestsuite(cid, pid, testid);
                    response.getWriter().print(json);
                } else {
                    int cid = Integer.valueOf(compid);
                    int pid = Integer.valueOf(prodid);
                    int mid = Integer.valueOf(modid);
                    int tid = Integer.valueOf(tcid);
                    authCheck.isUserAuthorizedForCompany(cid);
                    JSONArray json = msc.getUploadfileByTestcase(cid, pid, mid, tid);
                    response.getWriter().print(json);
                }
            } else if (action.equals("getPages")) {
                int prodid = Integer.valueOf(request.getParameter("productid"));
                authCheck.isUserAuthorizedForProd(prodid);
                JSONObject json = MSC.getpagedetailsOnProd(prodid, companyId, randomkey);
                response.getWriter().print(json);
            } else if (action.equals("getPageElements")) {
                int prodid = Integer.valueOf(request.getParameter("productid"));
                authCheck.isUserAuthorizedForProd(prodid);
                JSONObject json = MSC.getproductelementsdetail(prodid);
                response.getWriter().print(json);
            } else if (action.equals("getPageElementsByPageid")) {
                int pageid = Integer.valueOf(request.getParameter("pageid"));
//				authCheck.isUserAuthorizedForCompany(companyId);
                JSONArray json = MSC.getElementsOfPage(pageid);
                response.getWriter().print(json);
            } else if (action.equals("getPageSS")) {
                int pageid = Integer.valueOf(request.getParameter("pageid"));
                int prodid = Integer.valueOf(request.getParameter("prodid"));
                authCheck.isUserAuthorizedForProd(prodid);
                JSONObject json = MSC.getpagescreenshot(companyId, prodid, pageid, randomkey);
                response.getWriter().print(json);
            } else if (action.equals("addIntegration")) {
                int prodid = Integer.valueOf(request.getParameter("productid"));
                authCheck.isUserAuthorizedForProd(prodid);
                String intname = request.getParameter("intname");
                String adminprop = request.getParameter("adminprop");
                MSC.insertIntegrationDetails(prodid, intname, adminprop);
            } else if (action.equals("addUserIntegration")) {
                int prodid = Integer.valueOf(request.getParameter("productid"));
                authCheck.isUserAuthorizedForProd(prodid);
                String intname = request.getParameter("intname");
                String adminprop = request.getParameter("userprop");
                MSC.insertUserIntegrationDetails(prodid, intname, adminprop);
            } else if (action.equals("addStaticIntegration")) {
                authCheck.isUserAuthorizedForCompany(companyId);
                String intname = request.getParameter("intname");
                String adminattribute = request.getParameter("adminattribute");
                String userattribute = request.getParameter("userattribute");
                MSC.insertStacticIntegrationDetails(intname, adminattribute, userattribute);
            } else if (action.equals("getStaticIntegrations")) {
                authCheck.isUserAuthorizedForCompany(companyId);
                JSONArray json = MSC.getStaticIntegrationdetail();
                response.getWriter().print(json);
            } else if (action.equals("getIntegrations")) {
                int prodid = Integer.valueOf(request.getParameter("productid"));
                authCheck.isUserAuthorizedForProd(prodid);
                String intname = request.getParameter("name");
                JSONObject json = MSC.getIntegrationdetail(prodid, intname);
                response.getWriter().print(json);
            } else if (action.equals("deleteIntegrations")) {
                authCheck.isUserAuthorizedForCompany(companyId);
                int intid = Integer.valueOf(request.getParameter("intid"));
                MSC.deleteIntegration(intid);
                response.getWriter().print("Delete Successful");
            } else if (action.equals("downloadedFile")) {
                String val = (String) request.getParameter("tcid");
                if (!val.equals("undefined")) {
                    int tcid = Integer.valueOf(request.getParameter("tcid"));
                    authCheck.isUserAuthorizedForTC(tcid);
                    JSONArray json = MSC.getDownloadedFilesList(tcid);
                    response.getWriter().print(json);
                }
                //response.getWriter().print(new JSONArray());
            } else if (action.equals("testcasedescription")) {
                int tcid = Integer.valueOf(request.getParameter("tcid"));
                authCheck.isUserAuthorizedForTC(tcid);
                String desc = request.getParameter("description");
                MSC.getTcDescription(tcid, desc);
                JSONObject json = msc.getTestStepsByTestCaseID(tcid, false);
                String uname = (String) userDetails.get("uname");
                userCache.cacheLatestRecording(uname, json);
            } else if (action.equals("userStatus")) {
                authCheck.isUserAuthorizedForCompany(companyId);
                if (companyId != -1) {
                    JSONArray users = msc.getUsersByStatus(companyId);

                    if (users != null && users.size() > 0) {
                        String usersJson = users.toJSONString();
                        response.setContentType("application/json");
                        response.getWriter().print(usersJson);
                    } else {
                        response.getWriter().print("No users found with status 3 for the given company");
                    }
                } else {
                    response.getWriter().print("Missing companyId parameter");
                }
            } else if (action.equals("getUSerRoles")) {
                authCheck.isUserAuthorizedForCompany(companyId);
                JSONArray result = msc.getUSerRoles(companyId);
                response.getWriter().print(result);
            } else if (action.equals("UpdateUSerRole")) {
                authCheck.isUserAuthorizedForCompany(companyId);
                String useridstr = (String) request.getParameter("userid");
                int userid = Integer.parseInt(useridstr);
                String roleidstr = (String) request.getParameter("roleid");
                int roleid = Integer.parseInt(roleidstr);
                msc.updateUserRole(userid, roleid);
                response.getWriter().print("Update Successful");
            } else if (action.equals("updateModulePermission")) {
                String modidstr = (String) request.getParameter("moduleid");
                int modid = Integer.parseInt(modidstr);
                authCheck.isUserAuthorizedForMods(modid);
                String roleidstr = (String) request.getParameter("roleid");
                int roleid = Integer.parseInt(roleidstr);
                String permission = (String) request.getParameter("permission");
                msc.updateModulePerms(modid, roleid, permission);
                response.getWriter().print("Update Successful");
            } else if (action.equals("getModulePermission")) {
                String modidstr = (String) request.getParameter("moduleid");
                int modid = Integer.parseInt(modidstr);
                authCheck.isUserAuthorizedForMods(modid);
                JSONArray json = msc.getModulePerms(modid);
                response.getWriter().print(json);
            } else if (action.equals("authorizedUser")) {
                authCheck.isUserAuthorizedForCompany(companyId);
                String UserId = request.getParameter("iduserprofile");
                int userid = Integer.valueOf(UserId);
                String result = msc.changeUserStatus(companyId, userid);
                response.getWriter().print(result);
            } else if (action.equals("updatesync")) {
                String toggle = request.getParameter("synctoggle");
                int synctoggle = Integer.valueOf(toggle);
                String suiteid = request.getParameter("suiteid");
                int sid = Integer.valueOf(suiteid);
                authCheck.isUserAuthorizedForTSuite(sid);
                JSONObject result = msc.updateSyncToggle(synctoggle, sid);
                response.getWriter().print(result);
            } else if (action.equals("updateheadless")) {
                String toggle = request.getParameter("handlelesstoggle");
                int headlesstoggle = Integer.valueOf(toggle);
                String suiteid = request.getParameter("suiteid");
                int sid = Integer.valueOf(suiteid);
                authCheck.isUserAuthorizedForTSuite(sid);
                JSONObject result = msc.updateHeadless(headlesstoggle, sid);
                response.getWriter().print(result);
            } else if (action.equals("resetStatus")) {
                authCheck.isUserAuthorizedForCompany(companyId);
                String UserId = request.getParameter("iduserprofile");
                int userid = Integer.valueOf(UserId);
                int userStatus = 2;
                if (userStatus == 2) {
                    String result = msc.resetUserStatusAndFields(companyId, userid);
                    response.getWriter().print(result);
                }
            } else if (action.equals("clearStorage")) {
                authCheck.isUserAuthorizedForCompany(companyId);
                String beforeDate = request.getParameter("beforeDate");
                String pattern = "dd/MM/yyyy"; // Specify the format of the date string

                DateFormat dateFormat = new SimpleDateFormat(pattern);
                try {
                    Date date = dateFormat.parse(beforeDate);
                    System.out.println("Parsed Date: " + date);
                    java.sql.Date sqlDate = new java.sql.Date(date.getTime());
                    msc.clearStorage(companyId, sqlDate);
                } catch (ParseException e) {
                    System.err.println(Utilities.getNow());
                    e.printStackTrace();
                }
            } else if (action.equalsIgnoreCase("addscript") || action.equalsIgnoreCase("addBatchFile")) {
                String tsidstr = request.getParameter("teststepid");
                int tsid = -1;
                if (tsidstr != null && !tsidstr.equals("")) {
                    tsid = Integer.valueOf(tsidstr);
                }
                authCheck.isUserAuthorizedForTS(tsid);
                String fileName = request.getParameter("name");
                int lastIndex = fileName.lastIndexOf(".");
                String fileNameWOExtension = fileName.substring(0, lastIndex);
                String extension = fileName.substring(lastIndex);
                fileName = fileNameWOExtension + "_" + tsidstr + extension;
                if (fileName != null || !fileName.equals("null") || !fileName.equals("")
                        || !fileName.equals("undefined")) {
                    InputStream is = request.getInputStream();

                    if (action.equalsIgnoreCase("addscript")) {
                        String script = request.getParameter("script");
                        Utilities.createScriptsPath(companyId);
                        String Name = Utilities.getScriptFilePath(companyId, fileName);
                        File file = new File(Name);
                        copyInputStreamUsingFiles(is, Name);
                        MSC.updateTestStepWithScript(tsid, fileName, script);
                    } else {
                        Utilities.createBatchFilePath(companyId);
                        String Name = Utilities.getBatchFilePath(companyId, fileName);
                        File file = new File(Name);
                        copyInputStreamUsingFiles(is, Name);
                        MSC.updateTestStepWithCmd(tsid, fileName);
                    }

                    String tcIdStr = request.getParameter("testCaseId");
                    JSONObject json = null;
                    if (tcIdStr != null && !tcIdStr.equals("undefined")) {
                        int tcId = Integer.parseInt(tcIdStr);
                        json = msc.getTestStepsByTestCaseID(tcId, false);
                        String uname = (String) userDetails.get("uname");
                        userCache.cacheLatestRecording(uname, json);
                    } else {
                        json = userCache.getLatestRecordingFromCache((String) userDetails.get("uname"), msc);
                    }
                }
            } else if (action.equalsIgnoreCase("updateScript") || action.equalsIgnoreCase("updateBatchFile")) {
                String tsidstr = request.getParameter("teststepid");
                int tsid = -1;
                if (tsidstr != null && !tsidstr.equals("")) {
                    tsid = Integer.valueOf(tsidstr);
                }
                authCheck.isUserAuthorizedForTS(tsid);
                String fileName = request.getParameter("name");
                if (action.equalsIgnoreCase("updateScript")) {
                    String script = request.getParameter("script");
                    MSC.updateTestStepWithScript(tsid, fileName, script);
                } else {
                    MSC.updateTestStepWithCmd(tsid, fileName);
                }

                String tcIdStr = request.getParameter("testCaseId");
                JSONObject json = null;
                if (tcIdStr != null && !tcIdStr.equals("undefined")) {
                    int tcId = Integer.parseInt(tcIdStr);
                    json = msc.getTestStepsByTestCaseID(tcId, false);
                    String uname = (String) userDetails.get("uname");
                    userCache.cacheLatestRecording(uname, json);
                } else {
                    json = userCache.getLatestRecordingFromCache((String) userDetails.get("uname"), msc);
                }
            } else if (action.equalsIgnoreCase("getRepos")) {
                String pidstr = request.getParameter("productid");
                if (pidstr == null || pidstr.equals("")) {
                    response.getWriter().print("Invalid Product Id");
                }

                int prodid = Integer.valueOf(pidstr);
                authCheck.isUserAuthorizedForProd(prodid);
                JSONObject json = MSC.getIntegrationdetail(prodid, "Github");
                String adminProp = (String) json.get("adminprop");
                JSONArray jsonArray = new JSONArray();
                if (adminProp != null && !adminProp.equals("")) {
                    String[] repoDetails = adminProp.split(",");
                    String repos = repoDetails[3];
                    String[] reponames = repos.split("~");


                    for (String repo : reponames) {
                        jsonArray.add(repo);
                    }
                }
                response.getWriter().print(jsonArray);
            } else if (action.equals("scriptFiles") || action.equals("codeFiles")) {
                authCheck.isUserAuthorizedForCompany(companyId);
                // Get the file path and name
                String fileName = null;
                String fname = null;
                if (request.getParameter("fname") != null) {
                    fname = (String) request.getParameter("fname");

                    if (action.equals("scriptFiles")) fileName = Utilities.getScriptFilePath(companyId, fname);
                    else fileName = Utilities.getCodeFilePath(companyId, fname);
                }

                // Set the content type and headers
                String mimeType = getMimeType(fileName, request.getServletContext());
                response.setContentType(mimeType);

                if (action.equals("codeFiles")) {
                    // Set Content-Disposition for file download
                    response.setHeader("Content-Disposition", "attachment; filename=\"" + fname + "\"");
                } else {
                    // Set Content-Disposition for inline display
                    response.setHeader("Content-Disposition", "inline; filename=\"" + fileName + "\"");
                }

                // Initialize output stream outside the try block
                try (FileInputStream fis = new FileInputStream(fileName)) {
                    OutputStream out = response.getOutputStream();
                    byte[] buffer = new byte[1024];
                    int bytesRead;
                    while ((bytesRead = fis.read(buffer)) != -1) {
                        out.write(buffer, 0, bytesRead);
                    }
                } catch (IOException e) {
                    // Handle the IOException here
                    String errorMessage = e.getMessage();
                    try {
                        // Use the existing output stream variable
                        response.getWriter().print(errorMessage);
                    } catch (IOException ex) {
                        // Handle the second IOException if writing the error message to the output stream fails
                        System.err.println(Utilities.getNow());
                        ex.printStackTrace();
                    }
                }
            } else if (action.equals("uploadFuntionZipFile")) {
                authCheck.isUserAuthorizedForCompany(companyId);
                String fileName = request.getParameter("fileName");
                if (fileName != null || !fileName.equals("null") || !fileName.equals("") || !fileName.equals("undefined")) {
                    InputStream is = request.getInputStream();
                    String directoryPath = Utilities.createScriptZipPath(companyId);
                    File directory = new File(directoryPath);
                    if (directory.exists() && directory.isDirectory()) {
                        File file = new File(directory, fileName);
                        if (file.exists() && !file.isDirectory()) {
                            String timestamp = new SimpleDateFormat("yyyyMMddHHmmss").format(new java.util.Date());
                            String newFileName = timestamp + "_" + fileName;
                            File newFile = new File(directory, newFileName);
                            file.renameTo(newFile);
                        }
                    } else {
                        System.out.println("The directory does not exist.");
                    }
                    String Name = Utilities.getScriptZipFilePath(companyId, fileName);
                    File file = new File(Name);
                    copyInputStreamUsingFiles(is, Name);
                    msc.uploadScriptZipFile(companyId, fileName);
                }
            } else if (action.equals("getListofScriptsFunction")) {
                authCheck.isUserAuthorizedForCompany(companyId);
                String directoryPath = Utilities.getScriptFolder(companyId);
                JSONArray json = msc.getScriptFunctionNames(companyId, directoryPath);
                response.getWriter().print(json);
            } else if (action.equals("getListofBatchFiles")) {
                authCheck.isUserAuthorizedForCompany(companyId);
                String directoryPath = Utilities.getBatchFileFolder(companyId);
                JSONArray json = msc.getScriptFunctionNames(companyId, directoryPath);
                response.getWriter().print(json);
            } else if (action.equals("getListofCodes")) {
                authCheck.isUserAuthorizedForCompany(companyId);
                String directoryPath = Utilities.getCodePath(companyId);
                JSONArray json = msc.getScriptFunctionNames(companyId, directoryPath);
                response.getWriter().print(json);
            } else if (action.equals("generateUsageKey")) {
                String forCompany = request.getParameter("forCompany");
                String uname = (String) userDetails.get("uname");
                String toDate = request.getParameter("todate");
                String tcCount = request.getParameter("tcCount");

                String res = MSC.generateLicense(forCompany, uname,
                        toDate, tcCount, msc);

                response.getWriter().print(res);
            } else if (action.equals("updateUsageKey")) {
                String forCompany = request.getParameter("forCompany");
                String toDate = request.getParameter("todate");
                String tcCount = request.getParameter("tcCount");
                String codeDownloadPrmsn = request.getParameter("codeDownloadPrmsn");
                if (codeDownloadPrmsn == null || codeDownloadPrmsn.equals("")) {
                    codeDownloadPrmsn = "off";
                }

                String performanceTestingPrmsn = request.getParameter("performanceTestingPrmsn");
                if (performanceTestingPrmsn == null || performanceTestingPrmsn.equals("")) {
                    performanceTestingPrmsn = "off";
                }
                String coveragePrmsn = request.getParameter("coveragePrmsn");
                if (coveragePrmsn == null || coveragePrmsn.equals("")) {
                    coveragePrmsn = "off";
                }
                String apiDataPrmsn = request.getParameter("apiDataPrmsn");
                if (apiDataPrmsn == null || apiDataPrmsn.equals("")) {
                    apiDataPrmsn = "off";
                }
                String parallelThreadCnt = request.getParameter("parallelThreadCnt");
                if (parallelThreadCnt == null || parallelThreadCnt.equals("")) {
                    parallelThreadCnt = "1";
                }
                String vRecPrmsn = request.getParameter("vRecPrmsn");
                if (vRecPrmsn == null || vRecPrmsn.equals("")) {
                    vRecPrmsn = "off";
                }
                String NLPCreationPrmsn = request.getParameter("NLPCreationPrmsn");
                if (NLPCreationPrmsn == null || NLPCreationPrmsn.equals("")) {
                    NLPCreationPrmsn = "off";
                }
                String chatBotPrmsn = request.getParameter("chatBotPrmsn");
                if (chatBotPrmsn == null || chatBotPrmsn.equals("")) {
                    chatBotPrmsn = "off";
                }

                String res = MSC.updateLicenseUsage(forCompany, toDate, tcCount, codeDownloadPrmsn,
                        performanceTestingPrmsn, coveragePrmsn, apiDataPrmsn, parallelThreadCnt,
                        vRecPrmsn, NLPCreationPrmsn, chatBotPrmsn);

                response.getWriter().print(res);
            } else if (action.equals("savetcsettings")) {
                BufferedReader reader = request.getReader();
                JSONParser parser = new JSONParser();
                JSONObject jsonstr = (JSONObject) parser.parse(reader);
                String browser = (String) jsonstr.get("Browser");
                String envName = (String) jsonstr.get("EnvironmentName");
                String envUrl = (String) jsonstr.get("EnvironmentUrl");
                String proxy = (String) jsonstr.get("Proxy");
                String res = (String) jsonstr.get("Resolution");
                String tcDescription = (String) jsonstr.get("description");
                boolean wait = (boolean) jsonstr.get("keepbrowseropen");
                String isWait = (wait == true) ? "true" : "false";
                long runOncel = 0;
                if (jsonstr.get("RunOnce") instanceof Long) {
                    runOncel = (long) jsonstr.get("RunOnce");
                }

                boolean cont = (boolean) jsonstr.get("continuetest");
                boolean video = (boolean) jsonstr.get("enablevideo");
                boolean force = (boolean) jsonstr.get("forcenewsession");
                boolean audio = video;
                boolean stealthMode = (boolean) jsonstr.get("stealthMode");
                int runOnce = (int) runOncel;

                int threshold = 0;

                if (jsonstr.get("Threshold") instanceof String) {
                    String thresholdl = (String) jsonstr.get("Threshold");
                    threshold = Integer.valueOf(thresholdl);
                } else {
                    long thresholdl = (long) jsonstr.get("Threshold");
                    threshold = (int) thresholdl;
                }

                long tcidl = (long) jsonstr.get("testCaseId");
                int tcid = (int) tcidl;
                authCheck.isUserAuthorizedForTC(tcid);

                JSONObject result = MSC.updateTCSettings(browser, envUrl, 
                		proxy, res, runOnce, threshold, tcid, envName, 
                		cont, audio, video, force, tcDescription, isWait,
                		stealthMode);
                JSONObject json = msc.getTestStepsByTestCaseID(tcid, false);
                String uname = (String) userDetails.get("uname");
                userCache.cacheLatestRecording(uname, json);
                response.getWriter().print(json);
            } else if (action.equals("createTestCase")) {
                String tcname = (String) request.getParameter("newTestCase");
                String uname = (String) userDetails.get("uname");
                String moduleIdStr = (String) request.getParameter("module");
                String steps = (String) request.getParameter("stepDetails");
                
                //getting from body
                if (steps == null || steps.trim().isEmpty()) {
                    StringBuilder sb = new StringBuilder();
                    BufferedReader reader = request.getReader();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        sb.append(line).append("\n");
                    }
                    steps = sb.toString().trim();
                }
                
                String saveType = "AI_GEN";

                JSONObject result = new JSONObject();
                if (moduleIdStr == null && moduleIdStr.equals("")) {
                    result.put("message", "failed to create test case - no module id");
                    response.getWriter().print(result);
                }

                long rklong = Long.valueOf(randomkey);
                int moduleId = Integer.valueOf(moduleIdStr);
                authCheck.isUserAuthorizedForMods(moduleId);

                CreateNLPSteps cnlps = new CreateNLPSteps();
                JSONObject json = cnlps.createNLPTestCase(tcname,
                        "", 1, uname, saveType, rklong, moduleId,
                        -1, -1, steps, msc, companyId);

                int testcaseId = (int) json.get("id");

                if (testcaseId == -1) {
                    result.put("message", "failed to create test case - error in writing to nlp store");
                    response.getWriter().print(result);
                } else {
                    result.put("message", "success");
                    result.put("newTestCaseId", testcaseId);
                }

                response.getWriter().print(json);
            } else if (action.equals("generateTC")) {
                String req = (String) request.getParameter("req");

                JSONObject result = new JSONObject();
                authCheck.isUserAuthorizedForCompany(companyId);

                GenerateTestCases gtc = new GenerateTestCases();
                JSONArray ja = gtc.generateTC(companyId, req, msc);

                response.getWriter().print(ja);
            } else if (action.equals("generateScript")) {
                BufferedReader reader = request.getReader();
                JSONParser parser = new JSONParser();
                JSONArray jsonArr = (JSONArray) parser.parse(reader);
                authCheck.isUserAuthorizedForCompany(companyId);
                JSONArray json = null;
                CreateNLPSteps cnlps = new CreateNLPSteps();
                json = cnlps.getNLPTestCase(jsonArr, msc, companyId);

                if (json == null) {
                    try {
                        Thread.sleep(10000);
                        //					String js = "[{\"persona\":\"company admin\",\"automationScripts\":\"{\\\"nlp\\\":{\\\"11\\\":\\\"Enter Number of Codes\\\",\\\"12\\\":\\\"Select Verify Hologram Color\\\",\\\"13\\\":\\\"Click Submit\\\",\\\"1\\\":\\\"Enter ~company admin username~ into Username\\\",\\\"2\\\":\\\"Enter ~company admin password~ into Password\\\",\\\"3\\\":\\\"Click Login\\\",\\\"4\\\":\\\"Click Dashboard\\\",\\\"5\\\":\\\"Click Code Management\\\",\\\"6\\\":\\\"Click Generate Codes\\\",\\\"7\\\":\\\"Select Type of Code\\\",\\\"8\\\":\\\"Select MI10 Set Name\\\",\\\"9\\\":\\\"Enter Batch Name\\\",\\\"10\\\":\\\"Enter Batch Code\\\"}}\",\"description\":\"Company admin generates QR code with specific length and pattern\",\"tcid\":1,\"gherkin\":\"Given the company admin is logged in, When generating a QR code with specific length and pattern, Then the QR code is successfully created\",\"device\":\"web\"}]";
                        String js = "{\"automationScripts\":\"{\\\"nlp\\\":{\\\"11\\\":\\\"Enter Number of Codes\\\",\\\"12\\\":\\\"Select Verify Hologram Color\\\",\\\"13\\\":\\\"Click Submit\\\",\\\"1\\\":\\\"Enter ~company admin username~ into Username\\\",\\\"2\\\":\\\"Enter ~company admin password~ into Password\\\",\\\"3\\\":\\\"Click Login\\\",\\\"4\\\":\\\"Click Dashboard\\\",\\\"5\\\":\\\"Click Code Management\\\",\\\"6\\\":\\\"Click Generate Codes\\\",\\\"7\\\":\\\"Select Type of Code\\\",\\\"8\\\":\\\"Select MI10 Set Name\\\",\\\"9\\\":\\\"Enter Batch Name\\\",\\\"10\\\":\\\"Enter Batch Code\\\"}}\"}";
                        JSONObject as = (JSONObject) parser.parse(js);
                        JSONObject jo = (JSONObject) jsonArr.get(0);
                        jo.put("automationScripts", as.get("automationScripts"));
                        //					json = (JSONArray) parser.parse(js);
                    } catch (Exception e) {

                    }
                }
                response.getWriter().print(jsonArr);
            } else if (action.equals("saveScript")) {
                String moduleIdStr = (String) request.getParameter("modid");
                int modid = Integer.valueOf(moduleIdStr);
                BufferedReader reader = request.getReader();
                JSONParser parser = new JSONParser();
                JSONArray jsonArr = (JSONArray) parser.parse(reader);
                authCheck.isUserAuthorizedForCompany(companyId);
                String uname = (String) userDetails.get("uname");
                CreateNLPSteps cnlps = new CreateNLPSteps();
                jsonArr = cnlps.writeTctoDB(jsonArr, msc, randomkey,
                        modid, uname);
                response.getWriter().print(jsonArr);
            } else if (action.equals("getnlpfortc")) {
                String testcase = request.getParameter("testcaseid");
                int tcid = Integer.valueOf(testcase);
                authCheck.isUserAuthorizedForTC(tcid);
                JSONObject result = MSC.getNLPForTC(tcid);
                response.getWriter().print(result);
            } else if (action.equals("genCode")) {
                String testcase = request.getParameter("testcaseid");
                String tcname = request.getParameter("testcasename");
                int tcid = Integer.valueOf(testcase);
                authCheck.isUserAuthorizedForTC(tcid);
                
                //modular implementation
				boolean isLicenseValid = false;
				String genType = msc.getGetTypeForCompany(companyId);
				JSONObject json = msc.getTestStepsByTestCaseID(tcid, true);	
				String requestedBy = (String)userDetails.get("uname");
                CodeGenFull cgf = new CodeGenFull();
    			cgf.setMsc(msc);
    			isLicenseValid = cgf.genCode(json, companyId, randomkey, userCache, 
    					tcname, response, requestedBy, genType);
    			
    			//old impementation
//                CodeGenFacade cgf = new CodeGenFacade();
//                boolean isLicenseValid = cgf.codeGen(msc, tcid, companyId, randomkey,
//                        userCache, tcname, response, userDetails);
    			
                if (!isLicenseValid) {
                    JSONObject res = new JSONObject();
                    res.put("status", "Invalid License");
                    response.getWriter().print(res);
                }
            } else if (action.equals("getapiontc")) {
                String testcase = request.getParameter("testcaseid");
                int tcid = Integer.valueOf(testcase);
                authCheck.isUserAuthorizedForTC(tcid);
                JSONArray result = MSC.getApiOnTestCase(tcid);
                response.getWriter().print(result);
            } else if (action.equals("getapidetailsontc")) {
                String apiid = request.getParameter("Apiid");
                int aid = Integer.valueOf(apiid);
                authCheck.isUserAuthorizedForAPI(aid);
                JSONObject result = MSC.getApiDetailOnTestCase(aid);
                response.getWriter().print(result);
            } else if (action.equals("testing12")) {
                String suiteid = request.getParameter("suiteid");
                int sid = Integer.valueOf(suiteid);
                authCheck.isUserAuthorizedForTSuite(sid);
                MSC.getAIGenNameofStepsofSuite(sid);
            } else if (action.equals("teststeptotestsequence")) {
                String testcase = request.getParameter("testcaseid");
                int tcid = -1;
                if (!(testcase == null || testcase.equals("") || testcase.equals("undefined"))) {
                    tcid = Integer.valueOf(testcase);
                    authCheck.isUserAuthorizedForTC(tcid);
                }
                String tcTill = request.getParameter("numoftestcase");
                authCheck.isUserAuthorizedForCompany(companyId);
                String result = "";
                if (!(tcTill == null || tcTill.equals("") || tcTill.equals("undefined"))) {
                    int tcLimit = Integer.valueOf(tcTill);
                    result = MSC.updateTestSteptoTestSequence(companyId, tcid, tcLimit);
                }
                result = MSC.updateTestSteptoTestSequence(companyId, tcid, -2);
                response.getWriter().print(result);
            } else if (action.equals("testingonpage")) {
                String pageid = request.getParameter("pageid");
                int pid = Integer.valueOf(pageid);
                MSC.getNearestNameOnPage(pid);
            } else if (action.equals("testingPRonpage")) {
                String pageid = request.getParameter("pageid");
                int pid = Integer.valueOf(pageid);
                MSC.getPageRelationForAi(pid);
            } else if (action.equals("downloadTestSuiteResults")) {
                String tsrid = request.getParameter("suiteresultid");
                int tid = Integer.valueOf(tsrid);
                authCheck.isUserAuthorizedForTSR(tid);
                MSC.downloadTestSuiteResultFile(tid, companyId, response);
            } else if (action.equals("downloadTestCaseResults")) {
                String tcrid = request.getParameter("caseresultid");
                int tid = Integer.valueOf(tcrid);
                authCheck.isUserAuthorizedForTCR(tid);
                MSC.downloadTestCaseResultFile(tid, companyId, response);
            } else if (action.equals("triggerBulkApi")) {
                authCheck.isUserAuthorizedForCompany(companyId);
                String key = request.getParameter("key");
                String randomKey = request.getParameter("token");
                ApiThread apiThread = new ApiThread(key, randomKey, companyId);
                Thread thread = new Thread(apiThread);
                thread.start();
                response.getWriter().print("The Bulk Api is Triggered");
            } else if (action.equalsIgnoreCase("getStepDiff")) {
                String tcidstr = request.getParameter("tcid");
                String tsidstr = request.getParameter("tsid");
                String stepNumstr = request.getParameter("stepNumber");
                int tsid = Integer.valueOf(tsidstr);
                int tcid = Integer.valueOf(tcidstr);
                authCheck.isUserAuthorizedForTC(tcid);
                int stepNumber = Integer.valueOf(stepNumstr);

                JSONObject json = MSC.getErrorForTestStep(tsid, stepNumber, companyId, tcid, randomkey);
                response.getWriter().print(json);
            } else if (action.equalsIgnoreCase("getStepImageDiff")) {
                String tcidstr = request.getParameter("tcid");
                String tsidstr = request.getParameter("tsid");
                String tcridstr = request.getParameter("tcrid");
                String stepNumstr = request.getParameter("stepNumber");
                int tsid = Integer.valueOf(tsidstr);
                int tcid = Integer.valueOf(tcidstr);
                int tcrid = Integer.valueOf(tcridstr);
                authCheck.isUserAuthorizedForTC(tcid);
                int stepNumber = Integer.valueOf(stepNumstr);
                JSONObject json = MSC.getImageDiffForTestStep(tsid, stepNumber, companyId,
                        tcid, randomkey, tcrid);
                response.getWriter().print(json);
            } else if (action.equalsIgnoreCase("downloadTestSteps")) {
                String tcidstr = request.getParameter("tcid");
                int tcid = Integer.valueOf(tcidstr);
                authCheck.isUserAuthorizedForTC(tcid);
                String str = MSC.downloadTestSteps(tcid, randomkey, response);
            } else if (action.equalsIgnoreCase("gpcTesting")) {
                String prodidstr = request.getParameter("pid");
                int pid = Integer.valueOf(prodidstr);
                authCheck.isUserAuthorizedForProd(pid);
                MSC.DownloadPomClassForTestcase(pid, companyId, randomkey, response);
            } else if (action.equalsIgnoreCase("genAllPOMFiles")) {
                String prodidstr = request.getParameter("prodid");
                int pid = Integer.valueOf(prodidstr);
                authCheck.isUserAuthorizedForProd(pid);
                MSC.GenAllPomClassesForProduct(pid, companyId);
            } else if (action.equalsIgnoreCase("genPOMFile")) {
                String prodidstr = request.getParameter("prodid");
                int pid = Integer.valueOf(prodidstr);
                String pageidstr = request.getParameter("pageid");
                int pageid = Integer.valueOf(pageidstr);
                authCheck.isUserAuthorizedForProd(pid);
                MSC.GenPomClassForTestcase(pageid, pid, companyId);
            } else if (action.equalsIgnoreCase("authResponse")) {
                String cidstr = request.getParameter("companyid");
                companyId = Integer.valueOf(cidstr);
                String authCode = request.getParameter("code");
                JSONObject json = new JSONObject();
                try {
                    GoogleSheets.handleCallback(authCode, companyId);
                    json.put("authmsg", "Authorization was successfull");
                } catch (Exception e) {
                    json.put("authmsg", e.getMessage());
                }
                response.getWriter().print(json);
            } else if (action.equalsIgnoreCase("ingestSwagger")) {
                String pidstr = request.getParameter("product");
                int productId = Integer.valueOf(pidstr);
                String midstr = request.getParameter("module");
                int modId = Integer.valueOf(midstr);
                InputStream is = request.getInputStream();
                String name = Utilities.getCompanyFolder(companyId);
                name = name + "\\sif.json";
                File file = new File(name);
                copyInputStreamToFile(is, file);
                SwaggerJSONIngestor sji = new SwaggerJSONIngestor();
                sji.parseSwagger(name, msc, modId, randomkey);
                response.getWriter().print("Success");
            } else if (action.equalsIgnoreCase("getTop5")) {
                authCheck.isUserAuthorizedForCompany(companyId);
                DashBoardData dbd = new DashBoardData(msc);
                JSONArray ja = dbd.getTop5(request);
                response.getWriter().print(ja);
            } else if (action.equalsIgnoreCase("getAuthors")) {
                authCheck.isUserAuthorizedForCompany(companyId);
                DashBoardData dbd = new DashBoardData(msc);
                JSONArray ja = dbd.getAuthors(companyId);
                response.getWriter().print(ja);
            } else if (action.equalsIgnoreCase("getCountByAuthor")) {
                authCheck.isUserAuthorizedForCompany(companyId);
                DashBoardData dbd = new DashBoardData(msc);
                JSONArray ja = dbd.getCountByAuthor(request);
                response.getWriter().print(ja);
            } else if (action.equalsIgnoreCase("getTopCards")) {
                authCheck.isUserAuthorizedForCompany(companyId);
                DashBoardData dbd = new DashBoardData(msc);
                JSONArray ja = dbd.getTopCards(request);
                response.getWriter().print(ja);
            } else if (action.equalsIgnoreCase("getExecutionThreadChart")) {
                authCheck.isUserAuthorizedForCompany(companyId);
                DashBoardData dbd = new DashBoardData(msc);
                JSONArray ja = dbd.getExecutionThreadChart(request);
                response.getWriter().print(ja);
            } else if (action.equalsIgnoreCase("getLast5Suites")) {
                authCheck.isUserAuthorizedForCompany(companyId);
                DashBoardData dbd = new DashBoardData(msc);
                JSONArray ja = dbd.getLast5Suites(request);
                response.getWriter().print(ja);
            } else if (action.equalsIgnoreCase("getFlakiness")) {
                authCheck.isUserAuthorizedForCompany(companyId);
                DashBoardData dbd = new DashBoardData(msc);
                JSONObject ja = dbd.getFlakiness(request);
                response.getWriter().print(ja);
            } else if (action.equals("enrichElements")) {
                BufferedReader reader = request.getReader();
                JSONParser parser = new JSONParser();
                JSONObject inputJson = (JSONObject) parser.parse(reader);
                int productId = 736;
                JSONObject enrichedJson = MSC.enrichElementsWithIds(inputJson, productId);
                response.setContentType("application/json");
                response.getWriter().print(enrichedJson.toJSONString());
            }

        } catch (AuthorizationException ae) {
            System.err.println(Utilities.getNow());
            ae.printStackTrace();
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);  // Set the status code to 400
            response.getWriter().print("{\"error\": \"Not Authorized\"}");  // Send an error message
        } catch (Exception e) {
            System.err.println(Utilities.getNow());
            e.printStackTrace();
        } finally {
            msc.closeDbConn();
            MSC.closeDbConn();
        }
    }

    private static void copyInputStreamToFile(InputStream inputStream, File file)
            throws IOException {

        // append = false
        try (FileOutputStream outputStream = new FileOutputStream(file, false)) {
            int read;
            byte[] bytes = new byte[DEFAULT_BUFFER_SIZE];
            while ((read = inputStream.read(bytes)) != -1) {
                outputStream.write(bytes, 0, read);
            }
            outputStream.close();
        }
    }

    private static void copyInputStreamToFile(InputStream inputStream, String filePath)
            throws IOException {

        try {

            FileOutputStream outputStream = new FileOutputStream(filePath);
            int read;
            byte[] bytes = new byte[DEFAULT_BUFFER_SIZE];
            while ((read = inputStream.read(bytes)) != -1) {
                outputStream.write(bytes, 0, read);
            }
            inputStream.close();
            outputStream.close();
        } catch (Exception e) {
            System.err.println(Utilities.getNow());
            e.printStackTrace();
        }
    }

    private static void copyInputStreamUsingFiles(InputStream inputStream, String outputFilePath) throws IOException {
        Path outputPath = Paths.get(outputFilePath);
        Files.copy(inputStream, outputPath, StandardCopyOption.REPLACE_EXISTING);
        inputStream.close();
    }


    private String getMimeType(String fileName, ServletContext servletContext) {
        String mimeType = servletContext.getMimeType(fileName);
        if (mimeType == null) {
            mimeType = "application/octet-stream";
        }
        return mimeType;
    }

    public static String getSplitFileName(String url) {

        String filename = "";
        int lastEqualsIndex = url.lastIndexOf('=');
        filename = url.substring(lastEqualsIndex + 1);
        String[] splitItems = filename.split("_");

        // Create and return the HashMap
        HashMap<String, String[]> resultMap = new HashMap<>();
        resultMap.put(filename, splitItems);
        String fileName = resultMap.keySet().iterator().next();

        splitItems = resultMap.get(fileName);
        int cid = Integer.parseInt(splitItems[1]);
        System.out.println("splitItems[2]" + splitItems[2]);
        int testcid = Integer.parseInt(splitItems[2]);
        int testcrid = Integer.parseInt(splitItems[4]);
        String Screenshot = "C:\\Nogrunt\\" + cid + "\\" + testcid + "\\" + testcrid + "\\screenshots\\" + fileName;
        return Screenshot;
    }
}
