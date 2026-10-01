package nogrunt.springboot.Controllers.executer;

import io.appium.java_client.AppiumBy;
import io.appium.java_client.AppiumDriver;
import io.appium.java_client.InteractsWithApps;
import io.appium.java_client.TouchAction;
import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.remote.options.BaseOptions;
import net.dongliu.apk.parser.ApkFile;
import net.dongliu.apk.parser.bean.ApkMeta;
import nogrunt.AppProperties;
import nogrunt.ExecutionLogger;
import nogrunt.MySQlConn;
import nogrunt.ProcessTestData;
import nogrunt.SelGrid;
import nogrunt.Utilities;
import nogrunt.springboot.Controllers.model.MobileCapabilities;
import nogrunt.springboot.Controllers.service.MobileCapabilitiesService;
import nogrunt.springboot.Controllers.service.MobileResultService;
import nogrunt.springboot.Controllers.service.TokenVerificationService;
import org.json.JSONArray;
import org.json.JSONObject;
import org.openqa.selenium.*;
import org.openqa.selenium.interactions.Pause;
import org.openqa.selenium.interactions.PointerInput;
import org.openqa.selenium.interactions.Sequence;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URL;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.imageio.ImageIO;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

@Component
public class AppiumExecutor {

	Double duration;

	public String userName = System.getenv("firdousnogrunt") == null ? "firdousnogrunt"
			: System.getenv("firdousnogrunt");
	public String accessKey = System.getenv("4v1wgIJu4w1GiEYQmpe87bpnu4T4it4QxKfKjMJOUDr6aVTwFq") == null
			? "4v1wgIJu4w1GiEYQmpe87bpnu4T4it4QxKfKjMJOUDr6aVTwFq"
			: System.getenv("4v1wgIJu4w1GiEYQmpe87bpnu4T4it4QxKfKjMJOUDr6aVTwFq");

	private AppiumDriver driver;
	private AndroidDriver androidDriver;

	public boolean running;

	private Map<String, WebElement> elementStore = new HashMap<>();

	private final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(30);

	private final Duration POLLING_INTERVAL = Duration.ofMillis(500);

	private boolean hasFailed = false;

	private String elementDescription = "N/A";

	public org.json.simple.JSONObject step = null;

	String varName = null;

	String xpath = null;

	ConcurrentHashMap<String, String> varMap = null;

	private HelperMethods helperMethods;

	private String packageName;

	private String deviceId;

	private volatile List<String> recordingChunkPaths = new CopyOnWriteArrayList<>();

	private final AtomicBoolean isRecording = new AtomicBoolean(false);

	private final Map<String, Object> deviceTimeouts = new HashMap<>();

	String testDataResult = "";

	String apkPath = "";

	AtomicBoolean stepExecuting = new AtomicBoolean(true);

	private String tempXpath = null;

	private String flow = null;

	public void setVarMap(ConcurrentHashMap vm) {
		varMap = vm;
	}

	public void runAndroidAutomation(Integer Test_Case_Id, String token, MobileResultService mobileResultService,
			TokenVerificationService tokenVerificationService, MobileCapabilitiesService mobileCapabilitiesService) {

		Integer testCaseResultId = mobileResultService.saveCaseResults(token, Test_Case_Id, "STARTED", 0.0);
		long startTime = System.currentTimeMillis();

		Integer companyId1 = tokenVerificationService.getCompanyIdByToken(token);
		Utilities.createTestCaseResultsFolder(companyId1, Test_Case_Id, testCaseResultId);
		String logFilePath = Utilities.getLogDirFile(companyId1, Test_Case_Id, testCaseResultId);
		ExecutionLogger el = new ExecutionLogger(Integer.toString(testCaseResultId), logFilePath);

		String setupStatus = setUp(mobileCapabilitiesService, el, null, companyId1, Test_Case_Id, null);

		if ("FAIL".equals(setupStatus)) {
			updateDurationAndStatus(startTime, Test_Case_Id, "FAIL", mobileResultService);
			return;
		}

		try {

			MySQlConn mySQlConn = new MySQlConn(null);
			mySQlConn.setCalledFromSpecificLocation(true);
			org.json.simple.JSONObject actionsObject = mySQlConn.getTestStepsByTestCaseID(Test_Case_Id, false);
			actionsObject = (org.json.simple.JSONObject) actionsObject.get("data");
			mySQlConn.setCalledFromSpecificLocation(false);

			if (actionsObject == null) {
				updateDurationAndStatus(startTime, Test_Case_Id, "FAIL", mobileResultService);
				throw new IllegalArgumentException("Test Case Doesn't have any Test Steps");
			}

			JSONArray actions = DatabaseHandler.getActionsForMobileDevice(actionsObject);

			Thread statusUpdater = new Thread(() -> {
				while (isRunning()) {
					updateDurationAndStatus(startTime, Test_Case_Id, "RUNNING", mobileResultService);
					try {
						Thread.sleep(2000);
					} catch (InterruptedException e) {
						Thread.currentThread().interrupt();
					}
				}
			});
			statusUpdater.start();

			SelGrid sg = new SelGrid();
			executeActions(actions, mobileResultService, tokenVerificationService, testCaseResultId, token, el, null,
					sg, mySQlConn);

			statusUpdater.join();

		} catch (Exception e) {
			updateDurationAndStatus(startTime, Test_Case_Id, "FAIL", mobileResultService);
			throw new RuntimeException("Automation failed ");
		} finally {
			String status = tearDown(Test_Case_Id, token, el, testCaseResultId, null);
			updateDurationAndStatus(startTime, Test_Case_Id, status, mobileResultService);
		}
	}

	private void updateDurationAndStatus(long startTime, Integer testCaseId, String Status,
			MobileResultService mobileResultService) {
		long endTime = System.currentTimeMillis();
		duration = (endTime - startTime) / 1000.0;
		mobileResultService.updateCaseResultStatus(testCaseId, Status, duration);
	}

	public String runAndroidSuitAutomation(Integer Test_Case_Id, String token, Integer testSuitResultId,
			ExecutionLogger tsel, MobileResultService mobileResultService,
			TokenVerificationService tokenVerificationService, MobileCapabilitiesService mobileCapabilitiesService,
			Integer systemPort) {

		MySQlConn mySQlConn = new MySQlConn(null);
		Integer testCaseResultId = mobileResultService.saveCaseResults(token, Test_Case_Id, "STARTED", 0.0);

		mySQlConn.linkTSRtoTCR(testSuitResultId, testCaseResultId);

		long startTime = System.currentTimeMillis();
		updateDurationAndStatus(startTime, Test_Case_Id, "RUNNING", mobileResultService);

		Integer companyId1 = tokenVerificationService.getCompanyIdByToken(token);
		Utilities.createTestCaseResultsFolder(companyId1, Test_Case_Id, testCaseResultId);
		String logFilePath = Utilities.getLogDirFile(companyId1, Test_Case_Id, testCaseResultId);
		ExecutionLogger el = new ExecutionLogger(Integer.toString(testCaseResultId), logFilePath);

		String setupStatus = setUp(mobileCapabilitiesService, el, tsel, companyId1, Test_Case_Id, systemPort);
		String finalStatus = "FAIL";

		if ("FAIL".equals(setupStatus)) {
			updateDurationAndStatus(startTime, Test_Case_Id, setupStatus, mobileResultService);
			updateTestSuitResultStatus(startTime, setupStatus, testSuitResultId);
			return "FAIL";
		}

		try {
			mySQlConn.setCalledFromSpecificLocation(true);
			org.json.simple.JSONObject actionsObject = mySQlConn.getTestStepsByTestCaseID(Test_Case_Id, false);
			actionsObject = (org.json.simple.JSONObject) actionsObject.get("data");
			mySQlConn.setCalledFromSpecificLocation(false);

			if (actionsObject == null) {
				updateDurationAndStatus(startTime, Test_Case_Id, "FAIL", mobileResultService);
				updateTestSuitResultStatus(startTime, "FAIL", testSuitResultId);
				throw new IllegalArgumentException("Test Case Doesn't have any Test Steps");
			}

			JSONArray actions = DatabaseHandler.getActionsForMobileDevice(actionsObject);

			Thread statusUpdater = new Thread(() -> {
				while (isRunning()) {
					updateDurationAndStatus(startTime, Test_Case_Id, "RUNNING", mobileResultService);
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						Thread.currentThread().interrupt();
					}
				}
			});
			statusUpdater.start();

			SelGrid sg = new SelGrid();
			executeActions(actions, mobileResultService, tokenVerificationService, testCaseResultId, token, el, tsel,
					sg, mySQlConn);

			statusUpdater.join();

		} catch (Exception e) {
			updateDurationAndStatus(startTime, Test_Case_Id, "FAIL", mobileResultService);
			updateTestSuitResultStatus(startTime, "FAIL", testSuitResultId);
			e.printStackTrace();
			finalStatus = "FAIL";
		} finally {
			String status = tearDown(Test_Case_Id, token, el, testCaseResultId, tsel);
			updateDurationAndStatus(startTime, Test_Case_Id, status, mobileResultService);
			updateTestSuitResultStatus(startTime, status, testSuitResultId);
			finalStatus = status;
		}
		return finalStatus;
	}

	private void updateTestSuitResultStatus(long startTime, String status, Integer testSuitResultId) {
		MySQlConn mySQlConn = new MySQlConn(null);
		long endTime = System.currentTimeMillis();
		duration = (endTime - startTime) / 1000.0;
		if ("PASS".equals(status)) {
			mySQlConn.updateCount(testSuitResultId, "PASS", "RUNNING", duration);
		} else if ("FAIL".equals(status)) {
			mySQlConn.updateCount(testSuitResultId, "FAIL", "RUNNING", duration);
		}
	}

	public void updateTestSuitStatus(Integer testSuitResultId, Integer totalCount, long tsStartTime, int passCount,
			int failCount) {
		MySQlConn mySQlConn = new MySQlConn(null);
		long endTime = System.currentTimeMillis();
		double duration = (endTime - tsStartTime) / 1000.0;

		if (failCount == 0 || passCount == totalCount) {
			mySQlConn.updateTestSuiteStatus(testSuitResultId, "PASS");
		} else {
			mySQlConn.updateTestSuiteStatus(testSuitResultId, "FAIL");
		}
		mySQlConn.updateDur(testSuitResultId, duration);
	}

	public String setUp(MobileCapabilitiesService mobileCapabilitiesService, ExecutionLogger el, ExecutionLogger tsel,
			Integer companyId, Integer Test_Case_Id, Integer systemPort) {
		running = true;
		hasFailed = false;
		MobileCapabilities cap = mobileCapabilitiesService.getCapabilitiesByTestCaseID(Test_Case_Id);
		apkPath = cap.getApp();
		cap.getDeviceName();
		packageName = extractPackageNameFromAPK(apkPath);
		String configIp = AppProperties.configIp;
		String fullUrl = "http://" + configIp + ":4723";
		try {
			BaseOptions options = new BaseOptions().amend("appium:platformName", cap.getPlatformName())
					.amend("appium:platformVersion", cap.getPlatformVersion())
					.amend("appium:deviceName", cap.getDeviceName())
					.amend("appium:automationName", cap.getAutomationName()).amend("appium:app", apkPath)
					.amend("appium:noReset", true).amend("appium:fullReset", false)
					.amend("appium:appWaitDuration", 50000).amend("appium:deviceReadyTimeout", 30)
					.amend("appium:newCommandTimeout", 3600).amend("appium:autoGrantPermissions", true)
					.amend("appium:ignoreUnimportantViews", true).amend("appium:disableWindowAnimation", true)
					.amend("appium:autoLaunch", false).amend("appium:takesScreenshot", true)
					.amend("appium:autoStartRecording", true).amend("appium:enableVideo", true)
					.amend("appium:recordingLimit", 14400).amend("appium:recordingTimeLimit", 14400000)
					.amend("adbExecTimeout", 60000);
			if (systemPort != null) {
				options.amend("appium:systemPort", systemPort);
			}
//			androidDriver = new AndroidDriver(new URL("http://127.0.0.1:4723"), options);
			androidDriver = new AndroidDriver(new URL(fullUrl), options);
			driver = androidDriver;

			if (packageName != null) {
				((InteractsWithApps) driver).activateApp(packageName);
			}

			logAPKInformation(apkPath, el, tsel);
			logDeviceInformation(el, tsel);

			recordingChunkPaths = Collections.synchronizedList(new ArrayList<>());
			startRecordingProcess(el, tsel);

			helperMethods = new HelperMethods();

			el.logExecution("Session started successfully.");
			if (tsel != null)
				tsel.logExecution("Session started successfully.");
			return "SUCCESS";
		} catch (Exception e) {
			el.logExecution("Error starting the Appium session. ");
			if (tsel != null)
				tsel.logExecution("Error starting the Appium session. ");
			return "FAIL";
		}
	}

	public String extractPackageNameFromAPK(String apkPath) {
		try (ApkFile apkFile = new ApkFile(new File(apkPath))) {
			ApkMeta apkMeta = apkFile.getApkMeta();
			return apkMeta.getPackageName();
		} catch (Exception e) {
			System.err.println("Failed to extract package name. ");
			return null;
		}
	}

//    public  void setUp(ExecutionLogger el) {
//        running = true;
//        hasFailed = false;
//        try {
//            // Create UiAutomator2Options (or AppiumOptions)
//            DesiredCapabilities capabilities = new DesiredCapabilities();
//            HashMap<String, Object> ltOptions = new HashMap<String, Object>();
//            ltOptions.put("w3c", true);
//            ltOptions.put("platformName", "android");
//            ltOptions.put("deviceName", "Realme GT2 Pro");
//            ltOptions.put("platformVersion", "12");
//            ltOptions.put("app", "lt://APP10160241301741066765965570");
//            ltOptions.put("isRealMobile", true);
//            capabilities.setCapability("lt:options", ltOptions);
//
//            driver = new AppiumDriver(
//                    new URL("https://" + userName + ":" + accessKey + "@mobile-hub.lambdatest.com/wd/hub"),
//                    capabilities);
//
//            System.out.println("Session ID: " + driver.getSessionId());
//
//            logDeviceInformation(el);
//
//            recordingPath = "recording_" + System.currentTimeMillis() + ".mp4";
//            startChunkedRecording(el);
//            Thread.sleep(2000);
//
//            Thread.sleep(2000);
//
//            helperMethods = new HelperMethods();
//
//            System.out.println("LambdaTest session started successfully.");
//
//        } catch (MalformedURLException e) {
//            System.out.println("Malformed URL: " + e.getMessage());
//        } catch (Exception e) {
//            System.out.println("Error starting the LambdaTest session: " + e.getMessage());
//            e.printStackTrace();
//        }
//    }

	public void executeActions(JSONArray actionsArray, MobileResultService mobileResultService,
			TokenVerificationService tokenVerificationService, Integer testCaseResultId, String token,
			ExecutionLogger el, ExecutionLogger tsel, SelGrid sg, MySQlConn mySQlConn) {
		running = true;
		try {
			String findAssignScreenshot = null;
			String findandassignOuterHtml = null;
			for (int i = 0; i < actionsArray.length(); i++) {
				JSONObject actionObject = actionsArray.getJSONObject(i);
				String action = actionObject.getString("action");
				if (!isDriverHealthy(driver)) {
					el.logExecution("Device or session disconnected unexpectedly during execution.");
					throw new Exception("Device disconnected or Appium session invalidated.");
				}
				if (!isDriverSessionValid(driver)) {
					el.logExecution("Appium session lost! WDA likely crashed.");
					throw new Exception("WDA Crash detected, session lost.");
				}
				String elementId = actionObject.optString("element_id", null);
				JSONObject params = null;
				if (actionObject.has("testdata")) {
					String testdataStr = actionObject.getString("testdata");
					if (testdataStr != null && !testdataStr.isEmpty()) {
						try {
							if (!testdataStr.trim().startsWith("{")) {
								testdataStr = "{\"params\":{\"strategy\":\"fallback\",\"selector\":\""
										+ testdataStr.replace("\"", "\\\"") + "\"}}";
							}
							JSONObject testdata = new JSONObject(testdataStr);
							params = testdata.optJSONObject("params");
						} catch (Exception e) {
							el.logExecution("Warning: Invalid JSON in testdata. ");
							if (tsel != null)
								tsel.logExecution("Warning: Invalid JSON in testdata. ");
							params = null;
						}
					}
				}
				Integer testStep = actionObject.getInt("Test_Step");
				Integer stepNumber = actionObject.getInt("Step_Number");
				Integer testCaseId = actionObject.getInt("test_case_id");
				String filename = actionObject.has("filename") ? actionObject.getString("filename") : null;
				String testdata_source = actionObject.has("testdata_source") ? actionObject.getString("testdata_source")
						: null;
				String testData = actionObject.optString("TestData");
				JSONObject dataStep = actionObject;
				String Keyword = actionObject.getString("Keyword");
				String regex = actionObject.optString("regex");
				String customerEmail = actionObject.optString("customerEmail");
				String customerPassword = actionObject.optString("customerPassword");
				String emailSelectionCriteria = actionObject.optString("EmailSelectionCriteria");
				String emailFilter = actionObject.optString("EmailFilter");
				int outerHtmlStep = testStep + 1;
				int lastPassedTCRID = mySQlConn.getLatestPassedTestCaseResultsId(testCaseId);
				String lastPassedSignature = mySQlConn.getLastPassedOuterHTML(outerHtmlStep, lastPassedTCRID);
				if (actionObject.has("dataStep")) {
					step = new org.json.simple.JSONObject();
					org.json.JSONObject dataStepJson = actionObject.getJSONObject("dataStep");

					for (String key : dataStepJson.keySet()) {
						step.put(key, dataStepJson.get(key));
					}
				}
				varName = (String) step.get("VarName");
				xpath = (String) step.get("xpath");
				Integer companyId = tokenVerificationService.getCompanyIdByToken(token);
				String fileField = actionObject.has("fileField") ? actionObject.getString("fileField") : null;
				Utilities.executionMsgCache.put("testStepId", testStep);
				Utilities.executionMsgCache.put("StepNumber", stepNumber);
				sg.processVariables(varName, testData, xpath, i, step);
				if (params != null && params.has("selector")) {
					elementDescription = getReadableElementLabel(params, elementId);
				}
				if ("Assertion".equals(Keyword)) {
					long startTime = System.currentTimeMillis();
					String pageSource = driver.getPageSource();
					Utilities.executionMsgCache.put(testCaseResultId,
							"Step number: " + stepNumber + " Text Assertion is being done");
					boolean isElementVisible = helperMethods.validateAssertion(testData, pageSource, el, tsel);
					if (isElementVisible) {
						el.logExecution("Assertion Passed: Element with description '" + testData + "' is visible.");
						if (tsel != null)
							tsel.logExecution(
									"Assertion Passed: Element with description '" + testData + "' is visible.");
						testDataResult = "Validation : PASS FOR " + testData;
					} else {
						el.logExecution(
								"Assertion Failed: Element with description '" + testData + "' is NOT visible.");
						if (tsel != null)
							tsel.logExecution(
									"Assertion Failed: Element with description '" + testData + "' is NOT visible.");
						testDataResult = "Validation : FAIL FOR " + testData;
						String status = "FAIL";
						String keyword = elementId != null ? elementId : "N/A";
						flow = (String) step.get("Flow");
						Exception assertionError = new Exception("Assertion failed for test data: " + testData);
						Map<String, String> errorDetails = helperMethods.getAutomationErrorDetails(assertionError);
						String stepErrorCode = errorDetails.get("code");
						String stepErrorMessage = errorDetails.get("message");
						LocalDateTime executedDate = LocalDateTime.now();
						String ssfilename = Utilities.getSSFileName(testCaseId, testStep, testCaseResultId, 0,
								companyId, null);
						captureScreenshot(ssfilename, testCaseId, testCaseResultId, companyId, el, tsel);
						long endTime = System.currentTimeMillis();
						double duration = (endTime - startTime) / 1000.0;
						String thresholdStatus = "PASS".equals(status) ? "PASS" : "FAIL";
						Integer idStepResult = mobileResultService.saveMobileStepsResults(stepNumber, "", keyword,
								action, flow, testDataResult, status, testStep, duration, testCaseResultId,
								executedDate, thresholdStatus, ssfilename, elementDescription, ssfilename,
								stepErrorCode, null, stepErrorMessage);
						mySQlConn.updateTestCaseErrorCodeToDB(testCaseResultId, stepErrorCode, stepErrorMessage,
								idStepResult);
						el.logExecution("Stopping Aautomation Due To Failure To Locate Data On Screen");
						if (tsel != null)
							tsel.logExecution("Stopping Aautomation Due To Failure To Locate Data On Screen");
						elementDescription = "";
						testDataResult = "";
						hasFailed = true;
						running = false;
						return;
					}
				}

				if (testData != null && testData.toLowerCase().startsWith("validateloginfields")) {
					Utilities.executionMsgCache.put(testCaseResultId,
							"Step number: " + stepNumber + " Login Fields Availability Being Checked");
					validateLoginFieldsAvailability(driver, el, tsel);
				}

				boolean isPriceChangeValidation = false;
				String expectedChange = null;

				if (testData != null && testData.toLowerCase().startsWith("comparechange:")) {
					isPriceChangeValidation = true;
					expectedChange = testData.substring("comparechange:".length()).trim().toLowerCase();

					if (expectedChange.equals("same") || expectedChange.equals("equal")) {
						expectedChange = "equals";
					} else if (expectedChange.equals("less") || expectedChange.equals("smaller")) {
						expectedChange = "lower";
					} else if (expectedChange.equals("higher")) {
						expectedChange = "greater";
					} else if (expectedChange.equals("asc") || expectedChange.equals("ascending")) {
						expectedChange = "ascending";
					} else if (expectedChange.equals("dsc") || expectedChange.equals("descending")) {
						expectedChange = "descending";
					}

					el.logExecution("Found price change validation for '" + expectedChange
							+ "' - will execute after the action");
					if (tsel != null)
						tsel.logExecution("Found price change validation for '" + expectedChange
								+ "' - will execute after the action");
				}

				String originalTestDataResult = testDataResult;

				Thread watchdog = new Thread(() -> {
					try {
						Thread.sleep(AppProperties.defaultwaittime + 60);
						if (stepExecuting.get()) {
							throw new RuntimeException("Step execution timeout after 120 seconds");
						}
					} catch (InterruptedException ignored) {
					}
				});
				watchdog.start();

				Map<String, Object> result = executeActionAndValidate(action, elementId, params, stepNumber, testStep,
						testCaseResultId, mobileResultService, i, filename, testdata_source, testCaseId, token,
						companyId, fileField, elementDescription, testData, el, regex, customerEmail, customerPassword,
						emailSelectionCriteria, emailFilter, tsel, step, sg, lastPassedSignature);

				stepExecuting.set(false);
				watchdog.interrupt();

				String status = (String) result.get("status");
				double duration = (Double) result.get("duration");
				String ssfilename = (String) result.get("ssfilename");
				String ssfilenameBefore = (String) result.get("ssfilenameBefore");
				String failureReason = (String) result.get("failureReason");
				String stepErrorCode = (String) result.get("errorCode");
				String stepErrorMessage = (String) result.get("errorMessage");
				String outerhtml = (String) result.get("elementLocator");
				if (!"Assertion".equals(Keyword) && result.get("testDataResult") != null) {
					testDataResult = (String) result.get("testDataResult");
				} else {
					testDataResult = originalTestDataResult;
				}
				if ("findAndAssign".equals(action)) {
					findAssignScreenshot = ssfilename;
					findandassignOuterHtml = outerhtml;
				}

				if (isPriceChangeValidation) {
					boolean validationPassed;
					if (expectedChange.equals("ascending") || expectedChange.equals("descending")) {
						validationPassed = validateColumnSorting("LTP", expectedChange, el, tsel);

						if (validationPassed) {
							el.logExecution("Sorting Validation Passed: '" + expectedChange + "' order verified.");
							if (tsel != null)
								tsel.logExecution(
										"Sorting Validation Passed: '" + expectedChange + "' order verified.");
							testDataResult = "Sorting Validation Passed: '" + expectedChange + "' order verified.";
						} else {
							el.logExecution("Sorting Validation Failed: '" + expectedChange + "' order not observed.");
							if (tsel != null)
								tsel.logExecution(
										"Sorting Validation Failed: '" + expectedChange + "' order not observed.");
							testDataResult = "Sorting Validation Failed: '" + expectedChange + "' order not observed.";
						}
					} else {
						validationPassed = validatePriceChange(expectedChange, el, tsel);

						if (validationPassed) {
							el.logExecution(
									"Price Change Validation Passed: Direction '" + expectedChange + "' verified.");
							if (tsel != null)
								tsel.logExecution(
										"Price Change Validation Passed: Direction '" + expectedChange + "' verified.");
							testDataResult = "Price Change Validation Passed: Direction '" + expectedChange
									+ "' verified.";
						} else {
							el.logExecution(
									"Price Change Validation Failed: Direction '" + expectedChange + "' not observed.");
							if (tsel != null)
								tsel.logExecution("Price Change Validation Failed: Direction '" + expectedChange
										+ "' not observed.");
							testDataResult = "Price Change Validation Failed: Direction '" + expectedChange
									+ "' not observed.";
						}
					}
				}

				LocalDateTime executedDate = LocalDateTime.now();

				String thresholdStatus = "PASS".equals(status) ? "PASS" : "FAIL";

				String keyword = elementId != null ? elementId : "N/A";
				flow = (String) step.get("Flow");
				if ("FAIL".equals(status) && !"optional".equalsIgnoreCase(flow)) {
					if ("negative".equalsIgnoreCase(flow)) {
						status = "PASS";
					} else {
						Integer idStepResult = mobileResultService.saveMobileStepsResults(stepNumber, "", keyword,
								action, flow, testDataResult, "FAIL", testStep, duration, testCaseResultId,
								executedDate, thresholdStatus, ssfilename, elementDescription, ssfilenameBefore,
								stepErrorCode, findandassignOuterHtml, stepErrorMessage);
						mySQlConn.updateTestCaseErrorCodeToDB(testCaseResultId, stepErrorCode, stepErrorMessage,
								idStepResult);
						el.logExecution(
								"Step failed during action: '" + action + "' | Failure Reason: " + failureReason);
						if (tsel != null)
							tsel.logExecution(
									"Step failed during action: '" + action + "' | Failure Reason: " + failureReason);
						el.logExecution("Stopping automation due to failure: " + action);
						if (tsel != null)
							tsel.logExecution("Stopping automation due to failure: " + action);
						elementDescription = "";
						testDataResult = "";
						hasFailed = true;
						running = false;
						return;
					}
				} else if ("PASS".equals(status) && "negative".equalsIgnoreCase(flow)) {
					status = "FAIL";
					Integer idStepResult = mobileResultService.saveMobileStepsResults(stepNumber, "", keyword, action,
							flow, testDataResult, "FAIL", testStep, duration, testCaseResultId, executedDate,
							thresholdStatus, ssfilename, elementDescription, ssfilenameBefore, stepErrorCode,
							findandassignOuterHtml, stepErrorMessage);
					mySQlConn.updateTestCaseErrorCodeToDB(testCaseResultId, stepErrorCode,
							"Step expected to fail but passed", idStepResult);
					el.logExecution("Step marked as FAIL since it was expected to fail but passed.");
					if (tsel != null)
						tsel.logExecution("Step marked as FAIL since it was expected to fail but passed.");
					elementDescription = "";
					testDataResult = "";
					hasFailed = true;
					running = false;
					return;
				}

				if ("findAndAssign".equals(action)) {
					findAssignScreenshot = ssfilename;
				}

				if (!"findAndAssign".equals(action)) {
					mobileResultService.saveMobileStepsResults(stepNumber, "", keyword, action, flow, testDataResult,
							status, testStep, duration, testCaseResultId, executedDate, thresholdStatus, ssfilename,
							elementDescription, ssfilenameBefore, null, findandassignOuterHtml, null);
					elementDescription = "";
					testDataResult = "";
				}
				el.logExecution("Step " + (i + 1) + " Result: " + status + " (Duration: " + duration + " seconds)");
				if (tsel != null)
					tsel.logExecution(
							"Step " + (i + 1) + " Result: " + status + " (Duration: " + duration + " seconds)");
			}
		} catch (Exception e) {
			el.logExecution("Automation stopped due to failure at step. ");
			if (tsel != null)
				tsel.logExecution("Automation stopped due to failure at step. ");
		} finally {
			running = false;
		}
	}

	public boolean isDriverHealthy(AppiumDriver driver) {
		try {
			if (driver != null) {
				driver.getSessionId();
				return true;
			}
		} catch (Exception e) {
			System.err.println("Driver is not healthy. ");
		}
		return false;
	}

	public boolean isDriverSessionValid(AppiumDriver driver) {
		try {
			if (driver != null && driver.getSessionId() != null) {
				return true;
			}
		} catch (Exception e) {
			System.err.println("Driver session invalid. ");
		}
		return false;
	}

	private Map<String, Object> executeActionAndValidate(String action, String elementId, JSONObject params,
			Integer stepNumber, Integer testStep, Integer testCaseResultsId, MobileResultService mobileResultService,
			int stepIndex, String filename, String testdata_source, Integer testCaseId, String token, Integer companyId,
			String fileField, String elementDescription, String testData, ExecutionLogger el, String regex,
			String customerEmail, String customerPassword, String emailSelectionCriteria, String emailFilter,
			ExecutionLogger tsel, org.json.simple.JSONObject step, SelGrid sg, String lastPassedSignature)
			throws Exception {
		String status = "PASS";
		double duration = 0.0;
		String ssfilename = null;
		String ssfilenameBefore = null;
		String value = null;
		String failureReason = null;
		String errorCode = null;
		String errorMessage = null;
		String currentSignature = null;
		String wait = (String) step.get("wait");
		int waitTime = (int) step.get("waittime");
		int newWaitTime = waitTime * 1000;
		varName = (String) step.get("VarName");
		xpath = (String) step.get("xpath");
		int index = (int) step.get("Step_Number");
		if ("findandassign".equalsIgnoreCase(action) && xpath != null) {
			tempXpath = xpath;
		}
		try {
			driver.getPageSource();
		} catch (Exception e) {
			el.logExecution("Appium server likely went down. Cannot get page source.");
			throw new Exception("Appium server disconnected during execution.");
		}
		if ("sendKeys".equals(action)) {
			if (testdata_source != null) {
				if (testdata_source.equalsIgnoreCase("fromSMS") || testdata_source.equalsIgnoreCase("FromEmail")) {
					Thread.sleep(3000);
				}
				Utilities.executionMsgCache.put(testCaseResultsId,
						"Step number: " + stepNumber + " Test Data is being fetched from : " + testdata_source);
				org.json.simple.JSONObject stepnew = new org.json.simple.JSONObject();
				stepnew.put("test_case_id", testCaseId);
				stepnew.put("filename", filename);
				stepnew.put("testdata_source", testdata_source);
				stepnew.put("Action", action);
				stepnew.put("fileField", fileField);
				stepnew.put("regex", regex);
				stepnew.put("customerEmail", customerEmail);
				stepnew.put("customerPassword", customerPassword);
				stepnew.put("EmailSelectionCriteria", emailSelectionCriteria);
				stepnew.put("EmailFilter", emailFilter);
				stepnew.put("idtest_step", testStep);
				String testDataMap = handleDataProcessing(testData, stepnew, testCaseId, companyId, token, el, tsel,
						sg);
				value = testDataMap;
			} else {
				try {
					if (testData != null && !testData.isEmpty() && !testData.trim().startsWith("{")) {
						testData = "{\"params\":{\"value\":\"" + testData.replace("\"", "\\\"") + "\"}}";
					}

					if (testData != null && !testData.isEmpty()) {
						JSONObject jsonData = new JSONObject(testData);
						JSONObject NewParams = jsonData.optJSONObject("params");
						if (NewParams != null) {
							value = NewParams.optString("value", testData);
						} else {
							value = testData;
						}
					} else {
						value = "";
					}
				} catch (Exception e) {
					el.logExecution("Warning: Error parsing testData. ");
					if (tsel != null)
						tsel.logExecution("Warning: Error parsing testData. ");
					value = testData != null ? testData : "";
				}
			}
		}

		if (varName != null && tempXpath != null) {
			sg.setVarMap(varMap);
			sg.processVariables(varName, value, tempXpath, index, step);
			tempXpath = null;
		}
		try {
			if (wait.equals("Before")) {
				Thread.sleep(newWaitTime);
			}
			long startTime = System.currentTimeMillis();
			Integer stepnumBefore = stepIndex + 1;
			ssfilenameBefore = Utilities.getSSFileName(testCaseId, testStep, testCaseResultsId, stepnumBefore,
					companyId, "before");
			captureScreenshot(ssfilenameBefore, testCaseId, testCaseResultsId, companyId, el, tsel);
			switch (action) {
			case "swipe": {
				Map<String, Object> swipeResult = performSwipeAction(params, el, tsel, stepNumber, testCaseResultsId);
				boolean isSuccess = (boolean) swipeResult.getOrDefault("result", false);
				if (!isSuccess) {
					String reason = (String) swipeResult.getOrDefault("reason", "Swipe action failed.");
					throw new Exception(reason);
				}
				status = "PASS";
				break;
			}
			case "tapAndHold": {
				Map<String, Object> tapResult = performTapAndHoldAction(params, el, tsel, newWaitTime, stepNumber,
						testCaseResultsId);
				boolean isSuccess = (boolean) tapResult.getOrDefault("result", false);
				if (!isSuccess) {
					String reason = (String) tapResult.getOrDefault("reason", "Tap action failed.");
					throw new Exception(reason);
				}
				status = "PASS";
				break;
			}
			case "tap": {
				Map<String, Object> tapResult = performTapAction(params, el, tsel, stepNumber, testCaseResultsId);
				boolean isSuccess = (boolean) tapResult.getOrDefault("result", false);
				if (!isSuccess) {
					String reason = (String) tapResult.getOrDefault("reason", "Tap action failed.");
					throw new Exception(reason);
				}
				status = "PASS";
				break;
			}
			case "uploadfile": {
				if (params != null) {
					boolean uploaded = performUploadFile(elementId, params, testCaseId, companyId, el, tsel, stepNumber,
							testCaseResultsId);
					if (!uploaded) {
						throw new Exception("File upload failed.");
					}
				}
				status = "PASS";
				break;
			}
			case "findAndAssign": {
				Map<String, Object> findAssignResult = performFindAndAssignAction(elementId, params, el, tsel,
						lastPassedSignature, stepNumber, testCaseResultsId);
				boolean isSuccess = (boolean) findAssignResult.getOrDefault("result", false);
				if (!isSuccess) {
					String reason = (String) findAssignResult.getOrDefault("reason", "ACTION_FAILED");
					throw new Exception(reason);
				}
				currentSignature = (String) findAssignResult.get("elementSignature");
				status = "PASS";
				break;
			}
			case "sendKeys": {
				if (value != null) {
					Map<String, Object> sendKeysResult = performSendKeysAction(elementId, value, el, tsel, stepNumber,
							testCaseResultsId);
					boolean isSuccess = (boolean) sendKeysResult.getOrDefault("result", false);
					if (!isSuccess) {
						String reason = (String) sendKeysResult.getOrDefault("reason", "SendKeys action failed.");
						throw new Exception(reason);
					}
				}
				status = "PASS";
				break;
			}
			case "click": {
				if (elementId != null) {
					Map<String, Object> clickResult = performClickAction(elementId, el, tsel, stepNumber,
							testCaseResultsId);
					boolean isSuccess = (boolean) clickResult.getOrDefault("result", false);
					if (!isSuccess) {
						String reason = (String) clickResult.getOrDefault("reason", "Click action failed.");
						throw new Exception(reason);
					}
				}
				status = "PASS";
				break;
			}
			case "validatePartial": {
				Map<String, Object> assertResult = performAssertPartialAction(testData, el, tsel, stepNumber,
						testCaseResultsId);
				boolean isSuccess = (boolean) assertResult.getOrDefault("status", false);
				if (!isSuccess) {
					String reason = (String) assertResult.getOrDefault("reason", "Validation failed: Text not found.");
					throw new AssertionError(reason);
				}
				testDataResult = (String) assertResult.getOrDefault("result", "");
				status = "PASS";
				break;
			}
			default:
				el.logExecution("Unknown action: " + action);
				if (tsel != null)
					tsel.logExecution("Unknown action: " + action);
				status = "FAIL";
			}
			if (wait.equals("After")) {
				Thread.sleep(waitTime);
			}
			long endTime = System.currentTimeMillis();
			duration = (endTime - startTime) / 1000.0;

			Integer stepnum = stepIndex + 1;
			ssfilename = Utilities.getSSFileName(testCaseId, testStep, testCaseResultsId, stepnum, companyId, null);
			captureScreenshot(ssfilename, testCaseId, testCaseResultsId, companyId, el, tsel);
			testDataResult = value;
		} catch (Throwable e) {
			status = "FAIL";
			failureReason = getReadableFailureReason(e);
			logAndCaptureError(stepIndex, action, e, el, tsel, testCaseId, testStep, testCaseResultsId, companyId);
			Map<String, String> errorInfo = helperMethods.getAutomationErrorDetails(e);
			errorCode = errorInfo.get("code");
			errorMessage = errorInfo.get("message");
		}
		Map<String, Object> result = new HashMap<>();
		if (currentSignature != null) {
			result.put("elementLocator", currentSignature);
		} else {
			result.put("elementLocator", "N/A");
		}
		result.put("status", status);
		result.put("duration", duration);
		result.put("ssfilename", ssfilename);
		result.put("ssfilenameBefore", ssfilenameBefore);
		result.put("testDataResult", testDataResult);
		result.put("failureReason", failureReason);
		result.put("errorCode", errorCode);
		result.put("errorMessage", errorMessage);
		return result;
	}

	private void logAndCaptureError(int stepIndex, String action, Throwable e, ExecutionLogger el, ExecutionLogger tsel,
			Integer testCaseId, Integer testStep, Integer testCaseResultsId, Integer companyId) {
		hasFailed = true;
		el.logExecution("Step " + (stepIndex + 1) + ": Error in action '" + action);
		if (tsel != null) {
			tsel.logExecution("Step " + (stepIndex + 1) + ": Error in action '" + action);
		}

		try {
			Integer stepnum = stepIndex + 1;
			String ssfilename = Utilities.getSSFileName(testCaseId, testStep, testCaseResultsId, stepnum, companyId,
					"MR-1");
			captureScreenshot(ssfilename, testCaseId, testCaseResultsId, companyId, el, tsel);
		} catch (Exception ex) {
			el.logExecution("Error capturing screenshot after failure. ");
		}
	}

	private String getReadableFailureReason(Throwable e) {
		if (e instanceof org.openqa.selenium.NoSuchElementException) {
			return "Element not found.";
		} else if (e instanceof org.openqa.selenium.StaleElementReferenceException) {
			return "Element became stale (DOM changed).";
		} else if (e instanceof org.openqa.selenium.TimeoutException) {
			return "Timeout waiting for element.";
		} else if (e instanceof AssertionError) {
			return "Assertion failed.";
		} else if (e instanceof org.openqa.selenium.ElementClickInterceptedException) {
			return "Element not clickable (intercepted by another element).";
		} else if (e instanceof org.openqa.selenium.WebDriverException && e.getMessage() != null
				&& e.getMessage().toLowerCase().contains("session not created")) {
			return "App crash or session not created.";
		} else {
			String raw = e.getMessage();
			if (raw != null && raw.length() > 100) {
				raw = raw.substring(0, 100) + "...";
			}
			return "Unknown failure: " + (raw != null ? raw : "No error message.");
		}
	}

	private Map<String, Object> performAssertPartialAction(String testData, ExecutionLogger el, ExecutionLogger tsel,
			Integer stepNumber, Integer testCaseResultsId) {

		Map<String, Object> result = new HashMap<>();
		String validationResult = "";
		boolean status = false;
		try {
			if (testData == null || testData.isEmpty()) {
				String reason = "No text provided for validation.";
				el.logExecution("Error: " + reason);
				if (tsel != null)
					tsel.logExecution("Error: " + reason);
				result.put("status", false);
				result.put("reason", reason);
				result.put("result", "Validation failed: No text provided.");
				return result;
			}

			String pageSource = driver.getPageSource();
			boolean isTextPresent = pageSource.contains(testData);

			if (isTextPresent) {
				String passMsg = "AssertPartial PASSED: Text '" + testData + "' is present.";
				el.logExecution(passMsg);
				if (tsel != null)
					tsel.logExecution(passMsg);
				result.put("status", true);
				result.put("result", "Validation passed: Found text '" + testData + "'.");
			} else {
				String failMsg = "AssertPartial FAILED: Text '" + testData + "' is NOT present.";
				el.logExecution(failMsg);
				if (tsel != null)
					tsel.logExecution(failMsg);
				result.put("status", false);
				result.put("reason", getReadableFailureReason(new AssertionError("Expected text missing")));
				result.put("result", "Validation failed: Text '" + testData + "' not found.");
			}
			return result;
		} catch (Exception e) {
			String errorMsg = "Error during partial assert. ";
			el.logExecution(errorMsg);
			if (tsel != null)
				tsel.logExecution(errorMsg);
			result.put("status", false);
			result.put("reason", getReadableFailureReason(e));
			result.put("result", "Validation error occurred.");
			return result;
		}
	}

	private Map<String, Object> performTapAndHoldAction(JSONObject params, ExecutionLogger el, ExecutionLogger tsel,
			int newWaitTime, Integer stepNumber, Integer testCaseResultsId) {
		Map<String, Object> result = new HashMap<>();
		try {
			if (!params.has("actions")) {
				String msg = "Invalid tapAndHold action payload: missing 'actions' field.";
				el.logExecution(msg);
				if (tsel != null)
					tsel.logExecution(msg);
				result.put("result", false);
				result.put("reason", getReadableFailureReason(new IllegalArgumentException(msg)));
				return result;
			}
			JSONArray actionsArray = params.getJSONArray("actions");
			if (actionsArray.length() == 0) {
				String msg = "Empty actions array in tapAndHold action payload.";
				el.logExecution(msg);
				if (tsel != null)
					tsel.logExecution(msg);
				result.put("result", false);
				result.put("reason", getReadableFailureReason(new IllegalArgumentException(msg)));
				return result;
			}
			JSONObject pointerAction = actionsArray.getJSONObject(0);
			JSONArray fingerActions = pointerAction.getJSONArray("actions");
			JSONObject moveAction = fingerActions.getJSONObject(0);

			double xRatio = moveAction.getDouble("xRatio");
			double yRatio = moveAction.getDouble("yRatio");

			int holdDurationSeconds = newWaitTime;

			Dimension screenSize = driver.manage().window().getSize();
			int screenWidth = screenSize.getWidth();
			int screenHeight = screenSize.getHeight();
			Utilities.executionMsgCache.put(testCaseResultsId,
					"Step number: " + stepNumber + " Co-Ordinates are being Analyzed");
			int scaledX = (int) (xRatio * screenWidth);
			int scaledY = (int) (yRatio * screenHeight);

			el.logExecution("Tap and Hold Scaled Coordinates: (" + scaledX + ", " + scaledY + ")");
			el.logExecution("Hold Duration: " + holdDurationSeconds + " seconds");
			el.logExecution("Screen Size: (" + screenWidth + "x" + screenHeight + ")");
			if (tsel != null) {
				tsel.logExecution("Tap and Hold Scaled Coordinates: (" + scaledX + ", " + scaledY + ")");
				tsel.logExecution("Hold Duration: " + holdDurationSeconds + " seconds");
				tsel.logExecution("Screen Size: (" + screenWidth + "x" + screenHeight + ")");
			}

			PointerInput finger = new PointerInput(PointerInput.Kind.TOUCH, "finger1");
			Sequence tapHoldSequence = new Sequence(finger, 1);

			tapHoldSequence.addAction(
					finger.createPointerMove(Duration.ofMillis(0), PointerInput.Origin.viewport(), scaledX, scaledY));
			tapHoldSequence.addAction(finger.createPointerDown(PointerInput.MouseButton.LEFT.asArg()));
			tapHoldSequence.addAction(new Pause(finger, Duration.ofSeconds(holdDurationSeconds)));
			tapHoldSequence.addAction(finger.createPointerUp(PointerInput.MouseButton.LEFT.asArg()));

			driver.perform(Collections.singletonList(tapHoldSequence));
			result.put("result", true);
			return result;
		} catch (Exception e) {
			String errorMsg = "Error in performTapAndHoldAction: " + getReadableFailureReason(e);
			el.logExecution("❗ " + errorMsg);
			if (tsel != null)
				tsel.logExecution("❗ " + errorMsg);
			result.put("result", false);
			result.put("reason", getReadableFailureReason(e));
			return result;
		}
	}

	private Map<String, Object> performTapAction(JSONObject params, ExecutionLogger el, ExecutionLogger tsel,
			Integer stepNumber, Integer testCaseResultsId) {
		Map<String, Object> result = new HashMap<>();
		try {
			if (!params.has("actions")) {
				String msg = "Invalid tap action payload: missing 'actions' field.";
				el.logExecution(msg);
				if (tsel != null)
					tsel.logExecution(msg);
				result.put("result", false);
				result.put("reason", getReadableFailureReason(new IllegalArgumentException(msg)));
				return result;
			}
			JSONArray actionsArray = params.getJSONArray("actions");
			if (actionsArray.length() == 0) {
				String msg = "Empty actions array in tap action payload.";
				el.logExecution(msg);
				if (tsel != null)
					tsel.logExecution(msg);
				result.put("result", false);
				result.put("reason", getReadableFailureReason(new IllegalArgumentException(msg)));
				return result;
			}

			JSONObject pointerAction = actionsArray.getJSONObject(0);
			JSONArray fingerActions = pointerAction.getJSONArray("actions");

			JSONObject moveAction = fingerActions.getJSONObject(0);

			double xRatio = moveAction.getDouble("xRatio");
			double yRatio = moveAction.getDouble("yRatio");

			Dimension screenSize = driver.manage().window().getSize();
			int screenWidth = screenSize.getWidth();
			int screenHeight = screenSize.getHeight();
			Utilities.executionMsgCache.put(testCaseResultsId,
					"Step number: " + stepNumber + " Co-Ordinates are being Analyzed");
			int scaledX = (int) (xRatio * screenWidth);
			int scaledY = (int) (yRatio * screenHeight);

			el.logExecution("Tap Scaled Coordinates: (" + scaledX + ", " + scaledY + ")");
			el.logExecution("Screen Size: (" + screenWidth + "x" + screenHeight + ")");
			if (tsel != null) {
				tsel.logExecution("Tap Scaled Coordinates: (" + scaledX + ", " + scaledY + ")");
				tsel.logExecution("Screen Size: (" + screenWidth + "x" + screenHeight + ")");
			}

			PointerInput finger = new PointerInput(PointerInput.Kind.TOUCH, "finger1");
			Sequence tapSequence = new Sequence(finger, 1);

			tapSequence.addAction(
					finger.createPointerMove(Duration.ofMillis(5), PointerInput.Origin.viewport(), scaledX, scaledY));
			tapSequence.addAction(finger.createPointerDown(PointerInput.MouseButton.LEFT.asArg()));
			tapSequence.addAction(new Pause(finger, Duration.ofMillis(50)));
			tapSequence.addAction(finger.createPointerUp(PointerInput.MouseButton.LEFT.asArg()));
			driver.perform(Collections.singletonList(tapSequence));
			result.put("result", true);
			return result;
		} catch (Exception e) {
			String errorMsg = "Error in performTapAction: " + getReadableFailureReason(e);
			el.logExecution("❗ " + errorMsg);
			if (tsel != null)
				tsel.logExecution("❗ " + errorMsg);
			result.put("result", false);
			result.put("reason", getReadableFailureReason(e));
			return result;
		}
	}

	private Map<String, Object> performSwipeAction(JSONObject params, ExecutionLogger el, ExecutionLogger tsel,
			Integer stepNumber, Integer testCaseResultsId) {
		Map<String, Object> result = new HashMap<>();
		try {
			if (!params.has("actions")) {
				String msg = "Invalid swipe action payload: missing 'actions' field.";
				el.logExecution(msg);
				if (tsel != null)
					tsel.logExecution(msg);
				result.put("result", false);
				result.put("reason", getReadableFailureReason(new IllegalArgumentException(msg)));
				return result;
			}
			JSONArray actionsArray = params.getJSONArray("actions");
			if (actionsArray.length() == 0) {
				String msg = "Empty actions array in swipe action payload.";
				el.logExecution(msg);
				if (tsel != null)
					tsel.logExecution(msg);
				result.put("result", false);
				result.put("reason", getReadableFailureReason(new IllegalArgumentException(msg)));
				return result;
			}

			JSONObject pointerAction = actionsArray.getJSONObject(0);
			JSONArray fingerActions = pointerAction.getJSONArray("actions");

			JSONObject startMove = fingerActions.getJSONObject(0);
			JSONObject swipeMove = fingerActions.getJSONObject(2);

			double startXRatio = startMove.getDouble("xRatio");
			double startYRatio = startMove.getDouble("yRatio");
			double endXRatio = swipeMove.getDouble("xRatio");
			double endYRatio = swipeMove.getDouble("yRatio");

			Dimension screenSize = driver.manage().window().getSize();
			int screenWidth = screenSize.getWidth();
			int screenHeight = screenSize.getHeight();

			Utilities.executionMsgCache.put(testCaseResultsId,
					"Step number: " + stepNumber + " Co-Ordinates are being Analyzed");

			int scaledStartX = (int) (startXRatio * screenWidth);
			int scaledStartY = (int) (startYRatio * screenHeight);
			int scaledEndX = (int) (endXRatio * screenWidth);
			int scaledEndY = (int) (endYRatio * screenHeight);

			el.logExecution("Swipe Scaled Start: (" + scaledStartX + ", " + scaledStartY + ") ➡️ End: (" + scaledEndX
					+ ", " + scaledEndY + ")");
			el.logExecution("Screen Size: (" + screenWidth + "x" + screenHeight + ")");
			if (tsel != null) {
				tsel.logExecution("Swipe Scaled Start: (" + scaledStartX + ", " + scaledStartY + ") ➡️ End: ("
						+ scaledEndX + ", " + scaledEndY + ")");
				tsel.logExecution("Screen Size: (" + screenWidth + "x" + screenHeight + ")");
			}

			PointerInput finger = new PointerInput(PointerInput.Kind.TOUCH, "finger1");
			Sequence swipeSequence = new Sequence(finger, 1);

			swipeSequence.addAction(finger.createPointerMove(Duration.ofMillis(100), PointerInput.Origin.viewport(),
					scaledStartX, scaledStartY));
			swipeSequence.addAction(finger.createPointerDown(PointerInput.MouseButton.LEFT.asArg()));
			swipeSequence.addAction(finger.createPointerMove(Duration.ofMillis(750), PointerInput.Origin.viewport(),
					scaledEndX, scaledEndY));
			swipeSequence.addAction(finger.createPointerUp(PointerInput.MouseButton.LEFT.asArg()));
			driver.perform(Collections.singletonList(swipeSequence));
			result.put("result", true);
			return result;
		} catch (Exception e) {
			String errorMsg = "Error in performSwipeAction: " + getReadableFailureReason(e);
			el.logExecution("❗ " + errorMsg);
			if (tsel != null)
				tsel.logExecution("❗ " + errorMsg);
			result.put("result", false);
			result.put("reason", getReadableFailureReason(e));
			return result;
		}
	}

	private boolean performUploadFile(String elementId, JSONObject params, Integer testCaseId, Integer companyId,
			ExecutionLogger el, ExecutionLogger tsel, Integer stepNumber, Integer testCaseResultsId) {
		try {

			String platformName = (String) driver.getCapabilities().getCapability("platformName");

			String filePath = params.getString("filePath");

			String localDirectoryPath = AppProperties.datadirectory + companyId + "\\" + testCaseId;

			File localDirectory = new File(localDirectoryPath);
			if (!localDirectory.exists()) {
				localDirectory.mkdirs();
			}

			String localFilePath = localDirectoryPath + File.separator + elementId;
			File localFile = new File(localFilePath);

			if (localFile.exists()) {
				el.logExecution("File already exists at: " + localFilePath);
				if (tsel != null)
					tsel.logExecution("File already exists at: " + localFilePath);
				return true;
			}

			String deviceFilePath = filePath + elementId;

			if (platformName.equalsIgnoreCase("Android")) {
				byte[] fileBytes = ((AndroidDriver) driver).pullFile(deviceFilePath);

				FileOutputStream outputStream = new FileOutputStream(localFilePath);
				outputStream.write(fileBytes);
				outputStream.close();
			}
			el.logExecution("File copied successfully");
			if (tsel != null)
				tsel.logExecution("File copied successfully.");
			return true;
		} catch (Exception e) {
			hasFailed = true;
			el.logExecution("Error in performing Upload Action. ");
			if (tsel != null)
				tsel.logExecution("Error in performing Upload Action. ");
			return false;
		}
	}

	private Map<String, Object> performFindAndAssignAction(String elementId, JSONObject params, ExecutionLogger el,
			ExecutionLogger tsel, String lastPassedSignature, Integer stepNumber, Integer testCaseResultsId) {
		Map<String, Object> result = new HashMap<>();
		try {
			String[] strategies = { "-android uiautomator", "id", "xpath" };
			String byType = params.optString("selector", "").trim();
			WebElement element = null;

			if (byType.isEmpty()) {
				throw new Exception("Missing or invalid 'selector' in params.");
			}

			el.logExecution("Raw selector before sanitization: " + byType);
			if (tsel != null)
				tsel.logExecution("Raw selector before sanitization: " + byType);

			el.logExecution("Sanitized selector: " + byType);
			if (tsel != null)
				tsel.logExecution("Sanitized selector: " + byType);

			int maxRetries = 3;
			int retryWaitMs = 10000;
			int retryCount = 0;

			while (retryCount < maxRetries) {
				Utilities.executionMsgCache.put(testCaseResultsId,
						"Step number: " + stepNumber + " Locating Elements using different locators");
				el.logExecution("Attempt #" + (retryCount + 1) + ": Locating element...");
				if (tsel != null)
					tsel.logExecution("Attempt #" + (retryCount + 1) + ": Locating element...");

				WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(15));
				wait.ignoring(StaleElementReferenceException.class).ignoring(ElementClickInterceptedException.class)
						.ignoring(ElementNotInteractableException.class);

				long startTime = System.currentTimeMillis();
				long timeout = 15000;
				boolean elementFound = false;

				while ((System.currentTimeMillis() - startTime) < timeout) {
					for (String strategy : strategies) {
						if (elementFound)
							break;

						el.logExecution("Trying strategy: " + strategy);
						if (tsel != null)
							tsel.logExecution("Trying strategy: " + strategy);
						try {
							switch (strategy) {
							case "-android uiautomator":
								if ("-android uiautomator".equals(params.optString("strategy", ""))) {
									element = driver.findElement(AppiumBy.androidUIAutomator(byType));
								}
								break;
							case "id":
								if ("id".equals(params.optString("strategy", ""))) {
									element = driver.findElement(AppiumBy.id(byType));
								}
								break;
							case "xpath":
								if ("xpath".equals(params.optString("strategy", ""))) {
									element = driver.findElement(AppiumBy.xpath(byType));
								}
								break;
							}
							if (element != null) {
								elementFound = true;
								el.logExecution("Element found using strategy: " + strategy);
								if (tsel != null)
									tsel.logExecution("Element found using strategy: " + strategy);
								break;
							}
						} catch (Exception e) {
							el.logExecution("Error using strategy: " + strategy + ". Trying next...");
							if (tsel != null)
								tsel.logExecution("Error using strategy: " + strategy + ". Trying next...");
						}
					}
					if (elementFound)
						break;
					Thread.sleep(500);
				}
				if (elementFound) {
					elementStore.put(elementId, element);
					String currentSignature = buildStableElementSignature(element, params);
					Map<String, String> prevAttrs = parseSignatureToMap(lastPassedSignature);
					Map<String, String> currAttrs = parseSignatureToMap(currentSignature);
					Utilities.executionMsgCache.put(testCaseResultsId,
							"Step number: " + stepNumber + " Matching the Last Passed locator with current locator");
					if (lastPassedSignature != null && !lastPassedSignature.trim().isEmpty()) {
						if (!currentSignature.equals(lastPassedSignature)) {
							el.logExecution("UI element signature changed!");
							el.logExecution("Previous: " + lastPassedSignature);
							el.logExecution("Current:  " + currentSignature);
							if (currentSignature.contains("session") || currentSignature.contains("element-id")) {
								el.logExecution("Volatile attribute may be causing mismatch.");
							}
							if (isSoftSignatureMatch(prevAttrs, currAttrs)) {
								el.logExecution(
										"Soft match detected: minor attribute changes (e.g., content-desc). Continuing.");
								if (tsel != null) {
									tsel.logExecution("Soft match fallback accepted. Proceeding with current element.");
								}
								result.put("result", true);
								result.put("elementSignature", currentSignature);
								return result;
							}
							if (tsel != null) {
								tsel.logExecution("UI element signature mismatch.");
								tsel.logExecution("Previous: " + lastPassedSignature);
								tsel.logExecution("Current:  " + currentSignature);
							}

							String pageSource = driver.getPageSource();
							String fallbackXPath = buildFallbackXPathFromPageSource(pageSource, lastPassedSignature, el,
									tsel);
							Utilities.executionMsgCache.put(testCaseResultsId,
									"Step number: " + stepNumber + " Trying to create a new Locator");
							if (fallbackXPath != null) {
								try {
									element = driver.findElement(AppiumBy.xpath(fallbackXPath));
									if (element != null) {
										elementStore.put(elementId, element);
										el.logExecution("Element reassigned using fallback XPath.");
										if (tsel != null)
											tsel.logExecution("Element reassigned using fallback XPath.");
										result.put("result", true);
										result.put("elementSignature", buildStableElementSignature(element, params));
										return result;
									}
								} catch (Exception ignored) {
									el.logExecution("Fallback XPath failed.");
									if (tsel != null)
										tsel.logExecution("Fallback XPath failed.");
									Map<String, String> errorDetails = helperMethods.getAutomationErrorDetails(
											new Exception("XPath mismatch and fallback XPath also failed."));
									result.put("result", false);
									result.put("errorCode", errorDetails.get("code"));
									result.put("errorMessage", errorDetails.get("message"));
									result.put("reason", errorDetails.get("message"));
									hasFailed = true;
								}
							}
							throw new Exception("UI element mismatch from last passing run.");
						} else {
							el.logExecution("UI element signature matches previous run: " + currentSignature);
						}
					} else {
						el.logExecution("No previous element snapshot found. Skipping comparison.");
					}
					el.logExecution("Element with ID '" + elementId + "' successfully assigned.");
					result.put("result", true);
					result.put("elementSignature", currentSignature);
					return result;
				} else {
					retryCount++;
					if (retryCount < maxRetries) {
						el.logExecution("Element not found. Retrying after " + (retryWaitMs / 1000) + " seconds...");
						if (tsel != null)
							tsel.logExecution(
									"Element not found. Retrying after " + (retryWaitMs / 1000) + " seconds...");
						Thread.sleep(retryWaitMs);
					}
				}
			}

			String failMessage = "Failed to locate element with ID '" + elementId
					+ "' using any strategy after retries.";
			el.logExecution(failMessage);
			if (tsel != null)
				tsel.logExecution(failMessage);
			result.put("result", false);
			result.put("reason", getReadableFailureReason(new org.openqa.selenium.NoSuchElementException(failMessage)));
		} catch (Exception e) {
			Map<String, String> errorDetails = helperMethods.getAutomationErrorDetails(e);
			el.logExecution("Error in performFindAndAssignAction for elementId '" + elementId + "': "
					+ errorDetails.get("message"));
			if (tsel != null)
				tsel.logExecution("Error in performFindAndAssignAction for elementId '" + elementId + "': "
						+ errorDetails.get("message"));
			result.put("result", false);
			result.put("errorCode", errorDetails.get("code"));
			result.put("errorMessage", errorDetails.get("message"));
			result.put("reason", errorDetails.get("message"));
			hasFailed = true;
		}
		return result;
	}

	public static String buildStableElementSignature(WebElement element, JSONObject params) {
		try {
			String strategy = params.optString("strategy", "unknown");
			String selector = params.optString("selector", "unknown");
			String name = safeGetAttr(element, "name");
			String label = safeGetAttr(element, "label");
			String value = safeGetAttr(element, "value");
			String type = safeGetAttr(element, "type");
			String text = safeGetAttr(element, "text");
			String resourceId = safeGetAttr(element, "resource-id");
			String desc = safeGetAttr(element, "content-desc");

			return String.join(" | ", "strategy=" + strategy, "selector=" + selector, "name=" + name, "label=" + label,
					"value=" + value, "type=" + type, "text=" + text, "resource-id=" + resourceId, "desc=" + desc,
					"enabled=" + element.isEnabled(), "visible=" + element.isDisplayed());
		} catch (Exception e) {
			return "error_building_signature";
		}
	}

	private static String safeGetAttr(WebElement element, String attr) {
		try {
			String val = element.getAttribute(attr);
			return val != null ? val.trim() : "null";
		} catch (Exception e) {
			return "error";
		}
	}

	private String buildFallbackXPathFromPageSource(String pageSource, String lastSignature, ExecutionLogger el,
			ExecutionLogger tsel) {
		try {
			Map<String, String> signatureAttributes = new HashMap<>();
			String[] parts = lastSignature.split("\\|");
			String elementType = null;
			for (String part : parts) {
				String[] keyVal = part.trim().split("=", 2);
				if (keyVal.length == 2) {
					String key = keyVal[0].trim();
					String value = keyVal[1].trim();
					if (key.equalsIgnoreCase("type") && !value.equalsIgnoreCase("error")
							&& !value.equalsIgnoreCase("null")) {
						elementType = value;
						continue;
					}
					if (!key.equalsIgnoreCase("strategy") && !key.equalsIgnoreCase("selector")
							&& !value.equalsIgnoreCase("null") && !value.equalsIgnoreCase("error")
							&& !value.isEmpty()) {
						signatureAttributes.put(key, value);
					}
				}
			}
			DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
			Document doc = builder.parse(new InputSource(new StringReader(pageSource)));
			String resourceId = signatureAttributes.get("resource-id");
			if (resourceId != null && !resourceId.isEmpty()) {
				String xpath = "//*[@resource-id='" + resourceId.replace("'", "&apos;") + "']";
				el.logExecution("Trying resource-id based XPath: " + xpath);
				return xpath;
			}
			String targetText = signatureAttributes.get("text");
			if (targetText != null && targetText.length() > 3) {
				NodeList nodes = doc.getElementsByTagName("*");
				for (int i = 0; i < nodes.getLength(); i++) {
					Element element = (Element) nodes.item(i);
					String nodeText = element.getTextContent().trim();
					if (!nodeText.isEmpty() && (nodeText.contains(targetText) || targetText.contains(nodeText))) {
						String tag = element.getTagName();
						String xpath = "//" + tag + "[contains(@text,'" + nodeText.replace("'", "&apos;") + "')]";
						el.logExecution("Generated partial text match XPath: " + xpath);
						return xpath;
					}
				}
			}
			if (elementType != null) {
				NodeList nodes = doc.getElementsByTagName(elementType);
				for (int i = 0; i < nodes.getLength(); i++) {
					Element element = (Element) nodes.item(i);
					boolean allMatch = true;
					List<String> conditions = new ArrayList<>();
					for (Map.Entry<String, String> entry : signatureAttributes.entrySet()) {
						String attr = entry.getKey();
						String expectedVal = entry.getValue();
						String actualVal = attr.equalsIgnoreCase("text") ? element.getTextContent().trim()
								: element.getAttribute(attr);
						if (actualVal == null || !actualVal.trim().equalsIgnoreCase(expectedVal)) {
							allMatch = false;
							break;
						}
						if (!attr.equalsIgnoreCase("text")) {
							conditions.add("@" + attr + "='" + expectedVal.replace("'", "&apos;") + "'");
						}
					}
					if (allMatch && !conditions.isEmpty()) {
						String xpath = "//" + elementType + "[" + String.join(" and ", conditions) + "]";
						el.logExecution("Generated signature-based XPath: " + xpath);
						return xpath;
					}
				}
			}
			if (targetText != null && !targetText.isEmpty()) {
				String xpath = "//*[contains(@text,'" + targetText.replace("'", "&apos;") + "')]";
				el.logExecution("Trying final fallback XPath with loose contains(): " + xpath);
				return xpath;
			}
			el.logExecution("No matching element found in Android page source (all strategies exhausted).");
		} catch (Exception e) {
			el.logExecution("Exception in fallback XPath generation ");
			if (tsel != null)
				tsel.logExecution("Exception in fallback XPath generation ");
		}
		return null;
	}

	private boolean isSoftSignatureMatch(Map<String, String> prev, Map<String, String> curr) {
		return Objects.equals(prev.get("strategy"), curr.get("strategy"))
				&& Objects.equals(prev.get("className"), curr.get("className"))
				&& Objects.equals(prev.get("instance"), curr.get("instance"))
				&& !Objects.equals(prev.get("desc"), curr.get("desc"));
	}

	public Map<String, String> parseSignatureToMap(String signature) {
		Map<String, String> map = new HashMap<>();
		String[] parts = signature.split("\\|");
		for (String part : parts) {
			String[] keyValue = part.trim().split("=", 2);
			if (keyValue.length == 2) {
				map.put(keyValue[0].trim(), keyValue[1].trim());
			}
		}
		return map;
	}

	private Map<String, Object> performSendKeysAction(String elementId, String value, ExecutionLogger el,
			ExecutionLogger tsel, Integer stepNumber, Integer testCaseResultsId) {
		Map<String, Object> result = new HashMap<>();
		try {
			WebElement element = elementStore.get(elementId);
			validateElement(elementId, el, tsel);
			Utilities.executionMsgCache.put(testCaseResultsId,
					"Step number: " + stepNumber + " Element Validation is happening");
			if (element != null) {
				if (value != null) {
					el.logExecution("Sending keys: " + value);
					if (tsel != null)
						tsel.logExecution("Sending keys: " + value);
					element.sendKeys(value);
					result.put("result", true);
				} else {
					String msg = "No value provided for sendKeys.";
					el.logExecution(msg);
					if (tsel != null)
						tsel.logExecution(msg);
					result.put("result", false);
					result.put("reason", getReadableFailureReason(new IllegalArgumentException(msg)));
				}
			} else {
				String msg = "No element found with ID: " + elementId;
				el.logExecution(msg);
				if (tsel != null)
					tsel.logExecution(msg);
				result.put("result", false);
				result.put("reason", getReadableFailureReason(new IllegalArgumentException(msg)));
			}
		} catch (Exception e) {
			String errorMsg = "Error in performSendKeysAction: " + getReadableFailureReason(e);
			el.logExecution("❗ " + errorMsg);
			if (tsel != null)
				tsel.logExecution("❗ " + errorMsg);
			result.put("result", false);
			result.put("reason", getReadableFailureReason(e));
			hasFailed = true;
		}
		return result;
	}

	private Map<String, Object> performClickAction(String elementId, ExecutionLogger el, ExecutionLogger tsel,
			Integer stepNumber, Integer testCaseResultsId) {
		Map<String, Object> result = new HashMap<>();
		try {
			WebElement element = elementStore.get(elementId);
			Utilities.executionMsgCache.put(testCaseResultsId,
					"Step number: " + stepNumber + " Element Validation is happening");
			validateElement(elementId, el, tsel);
			if (element != null) {
				Utilities.executionMsgCache.put(testCaseResultsId,
						"Step number: " + stepNumber + " Element State is being checked");
				checkElementState(element, el, tsel);
				waitForClickable(element);
				try {
					element.click();
					el.logExecution("Click Action Performed Successfully");
					if (tsel != null)
						tsel.logExecution("Click Action Performed Successfully");
					result.put("result", true);
				} catch (ElementClickInterceptedException e) {
					driver.executeScript("arguments[0].click();", element);
					result.put("result", true);
				}
			} else {
				String msg = "No element found with ID: " + elementId;
				el.logExecution(msg);
				if (tsel != null)
					tsel.logExecution(msg);
				result.put("result", false);
				result.put("reason", msg);
			}
		} catch (Exception e) {
			String errorMsg = "Error in performClickAction: " + getReadableFailureReason(e);
			el.logExecution("❗ " + errorMsg);
			if (tsel != null)
				tsel.logExecution("❗ " + errorMsg);
			result.put("result", false);
			result.put("reason", getReadableFailureReason(e));
		}
		return result;
	}

	public String tearDown(Integer Test_Case_Id, String token, ExecutionLogger el, Integer testCaseResultId,
			ExecutionLogger tsel) {
		running = false;

		try {
			Thread.sleep(500);
			captureFinalRecording(el, tsel);
			Thread.sleep(1000);

			synchronized (recordingChunkPaths) {
				if (!recordingChunkPaths.isEmpty()) {
					saveAllRecordings(Test_Case_Id, token, testCaseResultId, el, tsel);
				} else {
					el.logExecution("No video recordings to save");
					if (tsel != null)
						tsel.logExecution("No video recordings to save");
				}
			}
			String packageName = extractPackageNameFromAPK(apkPath);
			if (driver != null) {
				try {
					if (driver.getCapabilities().getPlatformName().toString().equalsIgnoreCase("Android")) {
						((AndroidDriver) driver).terminateApp(packageName);
					}
					driver.quit();
				} catch (Exception e) {
					el.logExecution("Error during driver cleanup ");
				}
			}
			if (!hasFailed) {
				el.logExecution("Session ended successfully");
				if (tsel != null)
					tsel.logExecution("Session ended successfully");
				return "PASS";
			} else {
				el.logExecution("Session failed");
				if (tsel != null)
					tsel.logExecution("Session failed");
				return "FAIL";
			}
		} catch (Exception e) {
			el.logExecution("Error in tearDown. ");
			if (tsel != null)
				tsel.logExecution("Error in tearDown. ");
			e.printStackTrace();
			return "FAIL";
		}
	}

	public boolean isRunning() {
		return running;
	}

	private String captureScreenshot(String ssfilename, int testCaseId, int testCaseResultsId, int companyId,
			ExecutionLogger el, ExecutionLogger tsel) {
		try {
			File screenshot = driver.getScreenshotAs(OutputType.FILE);
			return helperMethods.saveScreenshot(screenshot, ssfilename, testCaseId, testCaseResultsId, companyId);
		} catch (Exception e) {
			el.logExecution("Error capturing screenshot from driver. ");
			if (tsel != null)
				tsel.logExecution("Error capturing screenshot from driver. ");
			return ssfilename;
		}
	}

	private String handleDataProcessing(String testData, org.json.simple.JSONObject stepnew, Integer testCaseId,
			Integer companyId, String token, ExecutionLogger el, ExecutionLogger tsel, SelGrid sg) throws Exception {
		ProcessTestData ptd = new ProcessTestData();
		sg.setVarMap(varMap);
		MySQlConn msc = new MySQlConn(null);
		String NewTestData = ptd.processTestData(testData, step, varName, testCaseId, companyId, 0, msc, token, null,
				sg, el);
		el.logExecution("Extracted data : " + NewTestData);
		if (tsel != null)
			tsel.logExecution("Extracted data : " + NewTestData);
		return NewTestData;
	}

	private boolean validateElement(String elementId, ExecutionLogger el, ExecutionLogger tsel) {
		try {
			WebElement element = elementStore.get(elementId);
			if (element == null) {
				String msg = "Element '" + elementId + "' is not found in elementStore — possibly not yet rendered.";
				el.logExecution(msg);
				if (tsel != null)
					tsel.logExecution(msg);
				return false;
			}
			WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(15));
			wait.until(driver -> {
				boolean visible = element.isDisplayed();
				if (!visible) {
					String msg = "⏳ Waiting for element '" + elementId + "' to become visible...";
					el.logExecution(msg);
					if (tsel != null)
						tsel.logExecution(msg);
				}
				return visible;
			});
			return helperMethods.validateElement(elementId, element, el, tsel);
		} catch (StaleElementReferenceException staleEx) {
			String msg = "Element '" + elementId
					+ "' is stale — it may have been removed or replaced after screen change.";
			el.logExecution(msg);
			if (tsel != null)
				tsel.logExecution(msg);
			return false;
		} catch (org.openqa.selenium.NoSuchElementException noSuchEx) {
			String msg = "Element '" + elementId + "' was not found on the screen.";
			el.logExecution(msg);
			if (tsel != null)
				tsel.logExecution(msg);
			return false;
		} catch (TimeoutException timeoutEx) {
			String msg = "Timed out waiting for element '" + elementId + "' to become visible.";
			el.logExecution(msg);
			if (tsel != null)
				tsel.logExecution(msg);
			return false;
		} catch (Exception e) {
			String msg = "Unexpected error while validating element '" + elementId;
			el.logExecution(msg);
			if (tsel != null)
				tsel.logExecution(msg);
			return false;
		}
	}

	private String getReadableElementLabel(JSONObject params, String elementId) {
		try {
			if (params != null && params.has("selector")) {
				String selector = params.optString("selector");
				if (selector.startsWith("//") || selector.startsWith("(")) {
					if (selector.contains("@text='")) {
						return selector.split("@text='")[1].split("'")[0];
					} else if (selector.contains("@content-desc='")) {
						return selector.split("@content-desc='")[1].split("'")[0];
					} else {
						return "UI Element";
					}
				} else {
					return selector;
				}
			}
		} catch (Exception ignored) {
		}
		return elementId != null ? elementId : "unknown element";
	}

	private void startRecordingProcess(ExecutionLogger el, ExecutionLogger tsel) {
		final CountDownLatch recordingStartedLatch = new CountDownLatch(1);
		try {
			Thread recordingThread = new Thread(() -> {
				try {
					final int CHUNK_DURATION_SECONDS = 170;
					final int MAX_CHUNKS = 100;
					final int CHECK_INTERVAL_MS = 1000;
					for (int chunkIndex = 0; chunkIndex < MAX_CHUNKS; chunkIndex++) {
						if (!running) {
							el.logExecution(
									"Recording not started for chunk " + (chunkIndex + 1) + " because test ended");
							break;
						}
						String chunkId = "chunk_" + chunkIndex + "_" + System.currentTimeMillis();
						try {
							String result = ((AndroidDriver) driver).startRecordingScreen();
							isRecording.set(true);
							if (chunkIndex == 0) {
								recordingStartedLatch.countDown();
							}
							long startTime = System.currentTimeMillis();
							boolean chunkTimedOut = false;
							while (running && !chunkTimedOut) {
								Thread.sleep(CHECK_INTERVAL_MS);
								long elapsed = System.currentTimeMillis() - startTime;
								chunkTimedOut = elapsed >= CHUNK_DURATION_SECONDS * 1000;
							}
							if (isRecording.get()) {
								String videoData = ((AndroidDriver) driver).stopRecordingScreen();
								isRecording.set(false);

								if (videoData != null && !videoData.isEmpty()) {
									String filename = "recording_" + chunkIndex + "_" + System.currentTimeMillis()
											+ ".mp4";

									synchronized (recordingChunkPaths) {
										recordingChunkPaths.add(filename + "|" + videoData);
									}
								} else {
								}
							}
						} catch (Exception e) {
							isRecording.set(false);
							if (chunkIndex == 0 && recordingStartedLatch.getCount() > 0) {
								recordingStartedLatch.countDown();
							}

							Thread.sleep(500);
						}
						if (!running) {
							el.logExecution("Recording stopped - test completed");
							if (tsel != null)
								tsel.logExecution("Recording stopped - test completed");
							break;
						}
					}
				} catch (Exception e) {
					el.logExecution("Fatal error in recording thread. ");
					if (tsel != null)
						tsel.logExecution("Fatal error in recording thread. ");
					e.printStackTrace();

					if (recordingStartedLatch.getCount() > 0) {
						recordingStartedLatch.countDown();
					}
				} finally {
					if (isRecording.get() && driver != null) {
						try {
							((AndroidDriver) driver).stopRecordingScreen();
							el.logExecution("Recording stopped during cleanup");
							if (tsel != null)
								tsel.logExecution("Recording stopped during cleanup");
						} catch (Exception ex) {
							el.logExecution("Error stopping recording during cleanup: " + ex.getMessage());
							if (tsel != null)
								tsel.logExecution("Error stopping recording during cleanup: " + ex.getMessage());
						}
					}
				}
			});
			recordingThread.setDaemon(true);
			recordingThread.setName("AppiumRecordingThread");
			recordingThread.start();
			try {
				boolean started = recordingStartedLatch.await(5, TimeUnit.SECONDS);
				if (!started) {
					el.logExecution("Warning: Recording initialization timed out after 15 seconds");
					if (tsel != null)
						tsel.logExecution("Warning: Recording initialization timed out after 15 seconds");
				}
			} catch (InterruptedException ie) {
				el.logExecution("Warning: Interrupted while waiting for recording to start. ");
				if (tsel != null)
					tsel.logExecution("Warning: Interrupted while waiting for recording to start. ");
			}
		} catch (Exception e) {
			el.logExecution("Failed to start recording thread. ");
			if (tsel != null)
				tsel.logExecution("Failed to start recording thread. ");
			e.printStackTrace();
		}
	}

	private void captureFinalRecording(ExecutionLogger el, ExecutionLogger tsel) {
		try {
			if (driver != null && driver.getCapabilities().getPlatformName().toString().equalsIgnoreCase("Android")) {
				if (isRecording.get()) {
					String finalVideo = ((AndroidDriver) driver).stopRecordingScreen();
					if (finalVideo != null && !finalVideo.isEmpty()) {
						String filename = "recording_final_" + System.currentTimeMillis() + ".mp4";
						synchronized (recordingChunkPaths) {
							recordingChunkPaths.add(filename + "|" + finalVideo);
						}
					}
					isRecording.set(false);
				}
			}
		} catch (Exception e) {
			// silently ignore
		}
	}

	private void saveAllRecordings(Integer testCaseId, String token, Integer testCaseResultId, ExecutionLogger el,
			ExecutionLogger tsel) {
		try {
			TokenVerificationService tokenVerificationService = new TokenVerificationService();
			Integer companyId = tokenVerificationService.getCompanyIdByToken(token);
			int successCount = 0;
			List<String> recordingsToProcess;
			synchronized (recordingChunkPaths) {
				recordingsToProcess = new ArrayList<>(recordingChunkPaths);
			}
			for (int i = 0; i < recordingsToProcess.size(); i++) {
				String recordingData = recordingsToProcess.get(i);
				String[] parts = recordingData.split("\\|", 2);

				if (parts.length < 2) {
					el.logExecution("Invalid recording data format for chunk " + (i + 1));
					continue;
				}
				String filename = parts[0];
				String videoData = parts[1];
				try {
					String standardFileName = "video_" + testCaseId + "_" + testCaseResultId + "_" + (i + 1) + ".mp4";
					helperMethods.saveRecording(videoData, standardFileName, testCaseId, companyId, testCaseResultId);
					successCount++;
				} catch (Exception e) {
					el.logExecution("Error processing recording chunk " + (i + 1));
					if (tsel != null)
						tsel.logExecution("Error processing recording chunk " + (i + 1));
				}
			}
			el.logExecution("Video recordings saved successfully. Total videos: " + successCount);
			if (tsel != null)
				tsel.logExecution("Video recordings saved successfully. Total videos: " + successCount);
		} catch (Exception e) {
			el.logExecution("Error saving recordings. ");
			if (tsel != null)
				tsel.logExecution("Error saving recordings. ");
			e.printStackTrace();
		}
	}

	public void checkElementState(WebElement element, ExecutionLogger el, ExecutionLogger tsel) {
		try {
			if (element == null) {
				String msg = "Cannot check element state — the WebElement reference is null.";
				el.logExecution(msg);
				if (tsel != null)
					tsel.logExecution(msg);
				return;
			}
			boolean isDisplayed = element.isDisplayed();
			boolean isEnabled = element.isEnabled();
			Point location = element.getLocation();
			Dimension size = element.getSize();
			helperMethods.checkElementState(element, el, isDisplayed, isEnabled, location, size, tsel);
		} catch (StaleElementReferenceException staleEx) {
			String msg = "Element became stale — it is no longer present on the screen. This may happen after a screen transition or app update.";
			el.logExecution(msg);
			if (tsel != null)
				tsel.logExecution(msg);
		} catch (org.openqa.selenium.NoSuchElementException nse) {
			String msg = "Element was not found in the current DOM — it might not be visible or fully loaded.";
			el.logExecution(msg);
			if (tsel != null)
				tsel.logExecution(msg);
		} catch (Exception e) {
			String msg = "Unexpected error during element state check: " + e.getClass().getSimpleName();
			el.logExecution(msg);
			if (tsel != null)
				tsel.logExecution(msg);
		}
	}

	public void checkGestureParameters(int startX, int startY, int endX, int endY, ExecutionLogger el,
			ExecutionLogger tsel) {
		try {
			Dimension screenSize = driver.manage().window().getSize();
			helperMethods.checkGestureParameters(startX, startY, endX, endY, screenSize, el, tsel);
		} catch (Exception e) {
			el.logExecution("Failed to get screen size. ");
			if (tsel != null)
				tsel.logExecution("Failed to get screen size. ");
		}
	}

	public WebElement waitForClickable(WebElement element) {
		WebDriverWait wait = new WebDriverWait(driver, DEFAULT_TIMEOUT);
		wait.pollingEvery(POLLING_INTERVAL).ignoring(StaleElementReferenceException.class)
				.ignoring(ElementClickInterceptedException.class);
		return wait.until(ExpectedConditions.elementToBeClickable(element));
	}

	public boolean validateLoginFieldsAvailability(AppiumDriver driver, ExecutionLogger el, ExecutionLogger tsel) {
		boolean userIdExists = false;
		boolean passwordExists = false;
		WebElement userIdField = null;
		WebElement passwordField = null;
		try {
			el.logExecution("Validating login fields for the app.");
			if (tsel != null)
				tsel.logExecution("Validating login fields for the app.");
			userIdField = findFieldByHints(driver,
					new String[] { "login", "loginId", "username", "userId", "email", "ID" });
			if (userIdField != null) {
				el.logExecution("User ID field found with attributes - resource-id: "
						+ userIdField.getAttribute("resource-id") + ", text: " + userIdField.getAttribute("text")
						+ ", content-desc: " + userIdField.getAttribute("content-desc"));
				if (tsel != null)
					tsel.logExecution("User ID field found with attributes - resource-id: "
							+ userIdField.getAttribute("resource-id") + ", text: " + userIdField.getAttribute("text")
							+ ", content-desc: " + userIdField.getAttribute("content-desc"));
				userIdExists = true;
			} else {
				el.logExecution("User ID field not detected");
				if (tsel != null)
					tsel.logExecution("User ID field not detected");
			}
			passwordField = findFieldByHints(driver, new String[] { "password" });
			if (passwordField != null) {
				el.logExecution("Password field found with attributes - resource-id: "
						+ passwordField.getAttribute("resource-id") + ", text: " + passwordField.getAttribute("text")
						+ ", content-desc: " + passwordField.getAttribute("content-desc"));
				if (tsel != null)
					tsel.logExecution("Password field found with attributes - resource-id: "
							+ passwordField.getAttribute("resource-id") + ", text: "
							+ passwordField.getAttribute("text") + ", content-desc: "
							+ passwordField.getAttribute("content-desc"));
				passwordExists = true;
			} else {
				el.logExecution("Password field not detected");
				if (tsel != null)
					tsel.logExecution("Password field not detected");
			}
			if (userIdExists && passwordExists) {
				el.logExecution("VALIDATION PASSED: Both User ID and Password fields are available.");
				if (tsel != null)
					tsel.logExecution("VALIDATION PASSED: Both User ID and Password fields are available.");
				return true;
			} else {
				el.logExecution("VALIDATION FAILED: Missing login fields.");
				if (tsel != null)
					tsel.logExecution("VALIDATION FAILED: Missing login fields.");
				return false;
			}
		} catch (Exception e) {
			el.logExecution("ERROR during login field validation. ");
			if (tsel != null)
				tsel.logExecution("ERROR during login field validation. ");
			return false;
		}
	}

	private WebElement findFieldByHints(AppiumDriver driver, String[] hints) {
		try {
			List<WebElement> fields = driver.findElements(AppiumBy.className("android.widget.EditText"));
			for (WebElement field : fields) {
				String resourceId = field.getAttribute("resource-id");
				String hint = field.getAttribute("hint");
				String text = field.getAttribute("text");
				String contentDesc = field.getAttribute("content-desc");
				for (String keyword : hints) {
					if ((resourceId != null && resourceId.toLowerCase().contains(keyword))
							|| (hint != null && hint.toLowerCase().contains(keyword))
							|| (text != null && text.toLowerCase().contains(keyword))
							|| (contentDesc != null && contentDesc.toLowerCase().contains(keyword))) {
						return field;
					}
				}
			}
		} catch (Exception e) {
			System.err.println("Error in findFieldByHints. ");
		}
		return null;
	}

	public boolean verifyLoginSuccess(AppiumDriver driver, ExecutionLogger el, ExecutionLogger tsel) {
		try {
			boolean isLoginSuccessful = driver
					.findElements(AppiumBy.xpath("//android.widget.TextView[contains(@text, 'Welcome')]")).size() > 0;
			if (isLoginSuccessful) {
				el.logExecution("Login was successful.");
				if (tsel != null)
					tsel.logExecution("Login was successful.");
				return true;
			} else {
				el.logExecution("Login failed or verification of success was inconclusive.");
				if (tsel != null)
					tsel.logExecution("Login failed or verification of success was inconclusive.");
				return false;
			}
		} catch (Exception e) {
			el.logExecution("ERROR during login success verification. ");
			if (tsel != null)
				tsel.logExecution("ERROR during login success verification. ");
			return false;
		}
	}

	private boolean validatePriceChange(String expectedChange, ExecutionLogger el, ExecutionLogger tsel) {
		try {
			Map<String, Double> initialPrices = capturePrices(el, tsel);
			Thread.sleep(5000);
			performRefreshSwipe(el, tsel);
			Thread.sleep(5000);
			Map<String, Double> updatedPrices = capturePrices(el, tsel);
			return helperMethods.validatePriceChanges(initialPrices, updatedPrices, expectedChange, el, tsel);
		} catch (Exception e) {
			el.logExecution("Error in price validation process. ");
			if (tsel != null)
				tsel.logExecution("Error in price validation process. ");
			return false;
		}
	}

	private Map<String, Double> capturePrices(ExecutionLogger el, ExecutionLogger tsel) {
		Map<String, Double> prices = new HashMap<>();
		try {
			List<WebElement> priceElements = driver
					.findElements(AppiumBy.xpath("//android.widget.TextView[matches(@text, '[0-9,]+\\.[0-9]+')]"));

			for (WebElement element : priceElements) {
				try {
					String elementText = element.getText();
					double price = helperMethods.parsePrice(elementText);
					String key = generatePriceKey(element);
					prices.put(key, price);
				} catch (Exception e) {
					continue;
				}
			}
			el.logExecution("Captured " + prices.size() + " price values");
			if (tsel != null)
				tsel.logExecution("Captured " + prices.size() + " price values");
			return prices;
		} catch (Exception e) {
			el.logExecution("Error capturing prices. ");
			if (tsel != null)
				tsel.logExecution("Error capturing prices. ");
			return prices;
		}
	}

	public boolean validateColumnSorting(String headerText, String expectedOrder, ExecutionLogger el,
			ExecutionLogger tsel) {
		try {
			WebElement headerElement = null;
			List<WebElement> headerElements = driver
					.findElements(AppiumBy.xpath("//android.widget.TextView[contains(@text, '" + headerText + "')]"));
			if (!headerElements.isEmpty()) {
				headerElement = headerElements.get(0);
				el.logExecution("Found header containing '" + headerText + "' at: " + headerElement.getLocation());
				el.logExecution("Header text: '" + headerElement.getText() + "'");
				if (tsel != null)
					tsel.logExecution(
							"Found header containing '" + headerText + "' at: " + headerElement.getLocation());
				if (tsel != null)
					tsel.logExecution("Header text: '" + headerElement.getText() + "'");
			} else {
				el.logExecution("Could not find any header containing '" + headerText + "'");
				if (tsel != null)
					tsel.logExecution("Could not find any header containing '" + headerText + "'");
				return false;
			}
			int headerX = headerElement.getLocation().getX();
			int headerY = headerElement.getLocation().getY();
			int columnWidth = headerElement.getSize().getWidth();
			Dimension screenSize = driver.manage().window().getSize();
			int minX = Math.max(0, headerX - columnWidth / 4);
			int maxX = Math.min(screenSize.width, headerX + columnWidth / 2);
			el.logExecution("Focusing on column X range: " + minX + " to " + maxX);
			if (tsel != null)
				tsel.logExecution("Focusing on column X range: " + minX + " to " + maxX);
			int stockListStartY = headerY + 100;
			int stockListEndY = screenSize.height - 200;
			el.logExecution("Stock list Y range: " + stockListStartY + " to " + stockListEndY);
			if (tsel != null)
				tsel.logExecution("Stock list Y range: " + stockListStartY + " to " + stockListEndY);
			List<WebElement> priceElements = driver
					.findElements(AppiumBy.xpath("//android.widget.TextView[matches(@text, '[0-9,]+\\.[0-9]+')]"));
			el.logExecution("Found " + priceElements.size() + " potential price elements");
			if (tsel != null)
				tsel.logExecution("Found " + priceElements.size() + " potential price elements");
			priceElements.sort(Comparator.comparing(e -> e.getLocation().getY()));
			List<Double> columnValues = new ArrayList<>();
			for (WebElement element : priceElements) {
				try {
					Point location = element.getLocation();
					if (location.getY() < stockListStartY || location.getY() > stockListEndY) {
						continue;
					}
					if (location.getX() < minX || location.getX() > maxX) {
						continue;
					}
					String text = element.getText();
					if (text == null || text.isEmpty() || text.contains("%") || text.contains("Mar")) {
						continue;
					}
					if (columnValues.isEmpty() || (columnValues.size() > 0
							&& Math.abs(location.getY() - columnValues.size() * 140 - stockListStartY) < 50)) {
						String cleanText = text.replaceAll("[^0-9.]", "");
						double value = Double.parseDouble(cleanText);
						columnValues.add(value);
						el.logExecution("Added stock price: " + value + " at Y=" + location.getY());
						if (tsel != null)
							tsel.logExecution("Added stock price: " + value + " at Y=" + location.getY());
					}
				} catch (Exception e) {
				}
			}
			if (columnValues.size() < 2) {
				el.logExecution("Not enough values to validate order: " + columnValues.size());
				if (tsel != null)
					tsel.logExecution("Not enough values to validate order: " + columnValues.size());
				return false;
			}
			el.logExecution("Final stock prices: " + columnValues);
			if (tsel != null)
				tsel.logExecution("Final stock prices: " + columnValues);
			List<Double> sortedValues = new ArrayList<>(columnValues);
			if (expectedOrder.equalsIgnoreCase("ascending")) {
				Collections.sort(sortedValues);
			} else {
				Collections.sort(sortedValues, Collections.reverseOrder());
			}
			boolean isSorted = columnValues.equals(sortedValues);
			el.logExecution("Values are sorted in " + expectedOrder + " order: " + isSorted);
			if (tsel != null)
				tsel.logExecution("Values are sorted in " + expectedOrder + " order: " + isSorted);
			if (!isSorted) {
				el.logExecution("Expected order: " + sortedValues);
				el.logExecution("Actual order: " + columnValues);
				if (tsel != null)
					tsel.logExecution("Expected order: " + sortedValues);
				if (tsel != null)
					tsel.logExecution("Actual order: " + columnValues);
			}
			return isSorted;
		} catch (Exception e) {
			el.logExecution("Error validating column sorting. ");
			if (tsel != null)
				tsel.logExecution("Error validating column sorting. ");
			e.printStackTrace();
			return false;
		}
	}

	private String generatePriceKey(WebElement element) {
		Point location = element.getLocation();
		return "Price_" + location.x + "_" + location.y;
	}

	private void performRefreshSwipe(ExecutionLogger el, ExecutionLogger tsel) {
		try {
			Dimension size = driver.manage().window().getSize();
			int startX = size.width / 2;
			int startY = size.height / 3;
			int endY = size.height * 2 / 3;
			PointerInput finger = new PointerInput(PointerInput.Kind.TOUCH, "finger1");
			Sequence swipe = new Sequence(finger, 1)
					.addAction(finger.createPointerMove(Duration.ofMillis(0), PointerInput.Origin.viewport(), startX,
							startY))
					.addAction(finger.createPointerDown(PointerInput.MouseButton.LEFT.asArg())).addAction(finger
							.createPointerMove(Duration.ofMillis(300), PointerInput.Origin.viewport(), startX, endY))
					.addAction(finger.createPointerUp(PointerInput.MouseButton.LEFT.asArg()));
			driver.perform(Collections.singletonList(swipe));
			el.logExecution("Performed refresh swipe");
			if (tsel != null)
				tsel.logExecution("Performed refresh swipe");
		} catch (Exception e) {
			el.logExecution("Error performing refresh swipe. ");
			if (tsel != null)
				tsel.logExecution("Error performing refresh swipe. ");
		}
	}

	private void logDeviceInformation(ExecutionLogger el, ExecutionLogger tsel) {
		try {
			Map<String, Object> deviceInfo = new HashMap<>();
			Map<String, Object> args = new HashMap<>();
			String platformName = driver.getCapabilities().getPlatformName().toString();
			if (platformName.equalsIgnoreCase("Android")) {
				deviceInfo.put("manufacturer",
						getCapabilityOrDefault((AndroidDriver) driver, "deviceManufacturer", null));
				deviceInfo.put("model", getCapabilityOrDefault((AndroidDriver) driver, "deviceModel", null));
				deviceInfo.put("platformVersion",
						getCapabilityOrDefault((AndroidDriver) driver, "platformVersion", null));
				deviceInfo.put("deviceName", getCapabilityOrDefault((AndroidDriver) driver, "deviceName", null));
				deviceInfo.put("udid", getCapabilityOrDefault((AndroidDriver) driver, "deviceUDID", null));
				deviceInfo.put("apiLevel", getCapabilityOrDefault((AndroidDriver) driver, "deviceApiLevel", null));
				deviceInfo.put("screenResolution",
						getCapabilityOrDefault((AndroidDriver) driver, "deviceScreenSize", null));
			}
			try {
				args.put("command", "cat /proc/meminfo | grep MemTotal");
				String totalRam = (String) driver.executeScript("mobile: shell", args);
				deviceInfo.put("totalRAM", totalRam != null ? totalRam.trim() : null);
			} catch (Exception ignored) {
			}
			try {
				args.put("command", "settings get global wifi_on");
				String wifiStatus = (String) driver.executeScript("mobile: shell", args);
				deviceInfo.put("isWifiEnabled", "1".equals(wifiStatus.trim()));
			} catch (Exception ignored) {
			}
			try {
				args.put("command", "settings get global mobile_data");
				String dataStatus = (String) driver.executeScript("mobile: shell", args);
				deviceInfo.put("isDataEnabled", "1".equals(dataStatus.trim()));
			} catch (Exception ignored) {
			}
			try {
				args.put("command", "dumpsys battery | grep level");
				String batteryLevel = (String) driver.executeScript("mobile: shell", args);
				if (batteryLevel != null) {
					String level = batteryLevel.split(":")[1].trim();
					deviceInfo.put("batteryLevel", level);
				}
				args.put("command", "dumpsys battery | grep powered");
				String chargingStatus = (String) driver.executeScript("mobile: shell", args);
				if (chargingStatus != null) {
					deviceInfo.put("isCharging", chargingStatus.contains("true"));
				}
			} catch (Exception ignored) {
			}
			StringBuilder logOutput = new StringBuilder();
			logOutput.append("\n========== Device Information ==========\n");
			for (Map.Entry<String, Object> entry : deviceInfo.entrySet()) {
				if (entry.getValue() != null) {
					logOutput.append(String.format("%s: %s\n", formatKey(entry.getKey()), entry.getValue()));
				}
			}
			logOutput.append("======================================\n");
			el.logExecution(logOutput.toString());
			if (tsel != null)
				tsel.logExecution(logOutput.toString());
			try {
				String apiLevelStr = (String) deviceInfo.get("apiLevel");
				if (apiLevelStr != null) {
					int apiLevelInt = Integer.parseInt(apiLevelStr);
					Duration deviceTimeout;
					Duration devicePolling;
					long deviceAnimationTimeout;
					if (apiLevelInt >= 29) {
						deviceTimeout = Duration.ofSeconds(40);
						devicePolling = Duration.ofMillis(750);
						deviceAnimationTimeout = 2500;
					} else {
						deviceTimeout = Duration.ofSeconds(30);
						devicePolling = Duration.ofMillis(500);
						deviceAnimationTimeout = 2000;
					}
					el.logExecution(String.format(
							"Set device-specific timeouts - Default: %s seconds, Polling: %s ms, Animation: %s ms",
							deviceTimeout.getSeconds(), devicePolling.toMillis(), deviceAnimationTimeout));
					if (tsel != null)
						tsel.logExecution(String.format(
								"Set device-specific timeouts - Default: %s seconds, Polling: %s ms, Animation: %s ms",
								deviceTimeout.getSeconds(), devicePolling.toMillis(), deviceAnimationTimeout));
					updateDeviceTimeouts(deviceTimeout, devicePolling, deviceAnimationTimeout);
				}
			} catch (Exception ignored) {
			}
		} catch (Exception e) {
			el.logExecution("Error logging device information. ");
			if (tsel != null)
				tsel.logExecution("Error logging device information. ");
		}
	}

	private String getCapabilityOrDefault(AndroidDriver driver, String capability, String defaultValue) {
		try {
			Object value = driver.getCapabilities().getCapability("appium:" + capability);
			return value != null ? value.toString() : defaultValue;
		} catch (Exception e) {
			return defaultValue;
		}
	}

	private String formatKey(String key) {
		return key.replaceAll("([a-z])([A-Z])", "$1 $2");
	}

	public void logAPKInformation(String apkPath, ExecutionLogger el, ExecutionLogger tsel) {
		if (apkPath == null || apkPath.isEmpty()) {
			el.logExecution("Invalid APK path provided.");
			if (tsel != null)
				tsel.logExecution("Invalid APK path provided.");
			return;
		}
		try (ApkFile apkFile = new ApkFile(new File(apkPath))) {
			ApkMeta apkMeta = apkFile.getApkMeta();
			String packageName = apkMeta.getPackageName();
			String label = apkMeta.getLabel();
			String icon = apkMeta.getIcon();
			String versionName = apkMeta.getVersionName();
			Long versionCode = apkMeta.getVersionCode();
			String minSdkVersion = apkMeta.getMinSdkVersion();
			String targetSdkVersion = apkMeta.getTargetSdkVersion();
			String maxSdkVersion = apkMeta.getMaxSdkVersion();
			Long apkSize = checkApkSize(apkPath);
			StringBuilder apkInfoLog = new StringBuilder();
			apkInfoLog.append("\n========== APK Information ==========\n");
			apkInfoLog.append(String.format("Package Name: \t%s\n", packageName));
			apkInfoLog.append(String.format("Label: \t%s\n", label != null ? label : "Unknown"));
			apkInfoLog.append(String.format("Icon: \t%s\n", icon != null ? icon : "Unknown"));
			apkInfoLog.append(String.format("Version Name: \t%s\n", versionName != null ? versionName : "Unknown"));
			apkInfoLog.append(String.format("Version Code: \t%s\n", versionCode != null ? versionCode : "Unknown"));
			apkInfoLog.append(
					String.format("Min SDK Version: \t%s\n", minSdkVersion != null ? minSdkVersion : "Unknown"));
			apkInfoLog.append(String.format("Target SDK Version: \t%s\n",
					targetSdkVersion != null ? targetSdkVersion : "Unknown"));
			apkInfoLog.append(
					String.format("Max SDK Version: \t%s\n", maxSdkVersion != null ? maxSdkVersion : "Unknown"));
			apkInfoLog.append(String.format("APK Size: \t%s MB\n", apkSize));
			apkInfoLog.append("======================================\n");
			el.logExecution(apkInfoLog.toString());
			if (tsel != null)
				tsel.logExecution(apkInfoLog.toString());
		} catch (Exception e) {
			el.logExecution("Error extracting APK information. ");
			if (tsel != null)
				tsel.logExecution("Error extracting APK information. ");
		}
	}

	public Long checkApkSize(String apkPath) {
		File apk = new File(apkPath);
		long sizeInMB = apk.length() / (1024 * 1024);
		return sizeInMB;
	}

	private void updateDeviceTimeouts(Duration timeout, Duration polling, long animationTimeout) {
		deviceTimeouts.put("timeout", timeout);
		deviceTimeouts.put("polling", polling);
		deviceTimeouts.put("animation", animationTimeout);
	}

}
