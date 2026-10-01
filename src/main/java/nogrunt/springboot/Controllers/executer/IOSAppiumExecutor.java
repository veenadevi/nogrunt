package nogrunt.springboot.Controllers.executer;

import io.appium.java_client.AppiumBy;
import io.appium.java_client.AppiumDriver;
import io.appium.java_client.InteractsWithApps;
import io.appium.java_client.ios.IOSDriver;
import io.appium.java_client.ios.options.XCUITestOptions;
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
import org.openqa.selenium.By;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.ElementClickInterceptedException;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.Point;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Pause;
import org.openqa.selenium.interactions.PointerInput;
import org.openqa.selenium.interactions.Sequence;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.stereotype.Component;
import com.dd.plist.NSDictionary;
import com.dd.plist.NSObject;
import com.dd.plist.PropertyListParser;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.net.URL;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.xml.sax.InputSource;

@Component
public class IOSAppiumExecutor {

	private AppiumDriver driver;

	private IOSDriver iosDriver;

	Double duration;

	public boolean running;

	private Process simulatorRecordingProcess;

	private String mp4FilePath;

	private final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(30);

	private final Duration POLLING_INTERVAL = Duration.ofMillis(500);

	private boolean hasFailed = false;

	public org.json.simple.JSONObject step = null;

	String varName = null;

	String xpath = null;

	ConcurrentHashMap<String, String> varMap = null;

	private Map<String, WebElement> elementStore = new HashMap<>();

	private HelperMethods helperMethods;

	private String elementDescription = "N/A";

	private String packageName;

	private volatile List<String> recordingChunkPaths = new CopyOnWriteArrayList<>();

	private final AtomicBoolean isRecording = new AtomicBoolean(false);

	String testDataResult = "";

	String apkPath = "";

	AtomicBoolean stepExecuting = new AtomicBoolean(true);

	private String tempXpath = null;

	private String flow = null;

	public void setVarMap(ConcurrentHashMap vm) {
		varMap = vm;
	}

	public void runIOSAutomation(Integer Test_Case_Id, String token, MobileResultService mobileResultService,
			TokenVerificationService tokenVerificationService, MobileCapabilitiesService mobileCapabilitiesService) {

		Integer testCaseResultId = mobileResultService.saveCaseResults(token, Test_Case_Id, "STARTED", 0.0);
		long startTime = System.currentTimeMillis();

		Integer companyId1 = tokenVerificationService.getCompanyIdByToken(token);
		Utilities.createTestCaseResultsFolder(companyId1, Test_Case_Id, testCaseResultId);
		String logFilePath = Utilities.getLogDirFile(companyId1, Test_Case_Id, testCaseResultId);
		ExecutionLogger el = new ExecutionLogger(Integer.toString(testCaseResultId), logFilePath);

		String setupStatus = setup(mobileCapabilitiesService, el, null, companyId1, Test_Case_Id, null, null);

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

		} catch (Exception e) {
			updateDurationAndStatus(startTime, Test_Case_Id, "FAIL", mobileResultService);
			throw new RuntimeException("Automation failed. ");

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

	public String runIOSSuitAutomation(Integer Test_Case_Id, String token, Integer testSuitResultId,
			ExecutionLogger tsel, MobileResultService mobileResultService,
			TokenVerificationService tokenVerificationService, MobileCapabilitiesService mobileCapabilitiesService,
			Integer systemPort, Integer wdaLocalPort) {

		MySQlConn mySQlConn = new MySQlConn(null);
		Integer testCaseResultId = mobileResultService.saveCaseResults(token, Test_Case_Id, "STARTED", 0.0);

		mySQlConn.linkTSRtoTCR(testSuitResultId, testCaseResultId);

		long startTime = System.currentTimeMillis();
		updateDurationAndStatus(startTime, Test_Case_Id, "RUNNING", mobileResultService);

		Integer companyId1 = tokenVerificationService.getCompanyIdByToken(token);
		Utilities.createTestCaseResultsFolder(companyId1, Test_Case_Id, testCaseResultId);
		String logFilePath = Utilities.getLogDirFile(companyId1, Test_Case_Id, testCaseResultId);
		ExecutionLogger el = new ExecutionLogger(Integer.toString(testCaseResultId), logFilePath);

		String setupStatus = setup(mobileCapabilitiesService, el, tsel, companyId1, Test_Case_Id, systemPort,
				wdaLocalPort);
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

	public String setup(MobileCapabilitiesService mobileCapabilitiesService, ExecutionLogger el, ExecutionLogger tsel,
			Integer companyId, Integer Test_Case_Id, Integer systemPort, Integer wdaLocalPort) {
		running = true;
		hasFailed = false;
		MobileCapabilities cap = mobileCapabilitiesService.getCapabilitiesByTestCaseID(Test_Case_Id);
		apkPath = cap.getApp();
		cap.getDeviceName();
		packageName = extractBundleIdFromApp(apkPath);
		String configIp = AppProperties.configIp;
		String fullUrl = "http://" + configIp + ":4723";
		try {
			XCUITestOptions options = new XCUITestOptions().setDeviceName(cap.getDeviceName())
					.setPlatformVersion(cap.getPlatformVersion()).setApp(cap.getApp())
					.setAutomationName(cap.getAutomationName()).setPlatformName(cap.getPlatformName());
			if (systemPort != null) {
				options.amend("appium:systemPort", systemPort);
			}
			if (wdaLocalPort != null) {
				options.amend("wdaLocalPort", wdaLocalPort);
			}
//			URL url = new URL("http://localhost:4723");
			URL url = new URL(fullUrl);
			iosDriver = new IOSDriver(url, options);
			driver = iosDriver;

			if (packageName != null) {
				((InteractsWithApps) driver).activateApp(packageName);
			}

			logDeviceInformation(el, tsel);
			logAppInformation(apkPath, el, tsel);

			recordingChunkPaths = Collections.synchronizedList(new ArrayList<>());

			helperMethods = new HelperMethods();

			if (isSimulator(driver)) {
				startSimulatorRecording(el, tsel);
			} else {
				startRecordingProcess(el, tsel);
			}

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

	public String extractBundleIdFromApp(String appPath) {
		try {
			File plistFile = new File(appPath, "Info.plist");
			if (!plistFile.exists()) {
				System.err.println("Info.plist not found at: " + plistFile.getAbsolutePath());
				return null;
			}

			com.dd.plist.NSDictionary rootDict = (com.dd.plist.NSDictionary) com.dd.plist.PropertyListParser
					.parse(plistFile);

			return rootDict.objectForKey("CFBundleIdentifier").toString();

		} catch (Exception e) {
			System.err.println("Failed to extract bundle ID. ");
			return null;
		}
	}

	private boolean isSimulator(AppiumDriver driver) {
		try {
			String udid = driver.getCapabilities().getCapability("udid").toString();
			return udid.matches("[a-fA-F0-9\\-]{36}");
		} catch (Exception e) {
			return false;
		}
	}

	public void executeActions(JSONArray actionsArray, MobileResultService mobileResultService,
			TokenVerificationService tokenVerificationService, Integer testCaseResultId, String token,
			ExecutionLogger el, ExecutionLogger tsel, SelGrid sg, MySQlConn mySQlConn) {
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
				Integer companyId = tokenVerificationService.getCompanyIdByToken(token);
				String fileField = actionObject.has("fileField") ? actionObject.getString("fileField") : null;
				Utilities.executionMsgCache.put("testStepId", testStep);
				Utilities.executionMsgCache.put("StepNumber", stepNumber);
				if (params != null && params.has("selector")) {
					elementDescription = getElementDescription(params, el, tsel);
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
		varName = (String) step.get("VarName");
		xpath = (String) step.get("xpath");
		int index = (int) step.get("Step_Number");
		String wait = (String) step.get("wait");
		int waitTime = (int) step.get("waittime");
		int newWaitTime = waitTime * 1000;
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
			captureScreenshot(ssfilename, testCaseId, testCaseResultsId, companyId, el, tsel);
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
			el.logExecution("Error capturing screenshot after failure: " + ex.getMessage());
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

	private Map<String, Object> performFindAndAssignAction(String elementId, JSONObject params, ExecutionLogger el,
			ExecutionLogger tsel, String lastPassedSignature, Integer stepNumber, Integer testCaseResultsId) {

		Map<String, Object> result = new HashMap<>();
		int maxAttempts = 3;
		int currentAttempt = 0;
		while (currentAttempt < maxAttempts) {
			try {
				currentAttempt++;
				String[] strategies = { "accessibility id", "-ios class chain", "-ios predicate string", "xpath" };
				String byType = params.optString("selector", "").trim();
				WebElement element = null;
				WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(15));
				wait.ignoring(StaleElementReferenceException.class);
				if (byType.isEmpty()) {
					throw new Exception("Missing or invalid 'selector' in params.");
				}
				Utilities.executionMsgCache.put(testCaseResultsId,
						"Step number: " + stepNumber + " Locating Elements using different locators");
				el.logExecution(
						"Attempting to locate element using iOS strategies... (Attempt " + currentAttempt + ")");
				if (tsel != null)
					tsel.logExecution(
							"Attempting to locate element using iOS strategies... (Attempt " + currentAttempt + ")");
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
							case "accessibility id":
								if ("accessibility id".equals(params.optString("strategy", ""))) {
									element = driver.findElement(AppiumBy.accessibilityId(byType));
								}
								break;
							case "-ios class chain":
								if ("-ios class chain".equals(params.optString("strategy", ""))) {
									element = driver.findElement(AppiumBy.iOSClassChain(byType));
								}
								break;
							case "-ios predicate string":
								if ("-ios predicate string".equals(params.optString("strategy", ""))) {
									element = driver.findElement(AppiumBy.iOSNsPredicateString(byType));
								}
								break;
							case "xpath":
								if ("xpath".equals(params.optString("strategy", ""))) {
									element = driver.findElement(AppiumBy.xpath(byType));
								}
								break;
							default:
								el.logExecution("Unsupported strategy: " + strategy);
								if (tsel != null)
									tsel.logExecution("Unsupported strategy: " + strategy);
								break;
							}
							if (element != null) {
								el.logExecution("Element found using strategy: " + strategy);
								if (tsel != null)
									tsel.logExecution("Element found using strategy: " + strategy);
								elementFound = true;
								break;
							}
						} catch (Exception e) {
							el.logExecution("Error using strategy: " + strategy);
							if (tsel != null)
								tsel.logExecution("Error using strategy: " + strategy);
						}
					}
					if (elementFound)
						break;
					Thread.sleep(500);
				}
				if (elementFound) {
					elementStore.put(elementId, element);
					String currentSignature = buildStableElementSignature(element, params);
					Utilities.executionMsgCache.put(testCaseResultsId,
							"Step number: " + stepNumber + " Matching the Last Passed locator with current locator");
					if (lastPassedSignature != null && !lastPassedSignature.trim().isEmpty()) {
						if (!currentSignature.equals(lastPassedSignature)) {
							el.logExecution("UI element signature changed!");
							el.logExecution("Previous: " + lastPassedSignature);
							el.logExecution("Current:  " + currentSignature);
							if (currentSignature.contains("session") || currentSignature.contains("element-id")) {
								el.logExecution(
										"Possible volatile attribute (session ID or internal element ID) causing mismatch.");
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
								el.logExecution("Retrying with fallback XPath: " + fallbackXPath);
								if (tsel != null)
									tsel.logExecution("Retrying with fallback XPath: " + fallbackXPath);

								try {
									element = driver.findElement(AppiumBy.xpath(fallbackXPath));
									if (element != null) {
										elementStore.put(elementId, element);
										el.logExecution("Element reassigned successfully using fallback XPath.");
										if (tsel != null)
											tsel.logExecution("Element reassigned successfully using fallback XPath.");
										result.put("result", true);
										result.put("elementSignature", buildStableElementSignature(element, params));
										return result;
									}
								} catch (Exception e) {
									el.logExecution("Fallback XPath failed ");
									if (tsel != null)
										tsel.logExecution("Fallback XPath failed ");
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
							el.logExecution("UI element signature matches previous run.");
							el.logExecution("Signature: " + currentSignature);
						}
					} else {
						el.logExecution("No previous element snapshot found. Skipping comparison.");
					}

					el.logExecution("Element with ID '" + elementId + "' successfully assigned.");
					result.put("result", true);
					result.put("elementSignature", currentSignature);
					return result;

				} else {
					if (currentAttempt < maxAttempts) {
						el.logExecution("Element not found. Waiting 10 seconds before retrying... (Attempt "
								+ currentAttempt + ")");
						if (tsel != null)
							tsel.logExecution("Element not found. Waiting 10 seconds before retrying... (Attempt "
									+ currentAttempt + ")");
						Thread.sleep(10000);
						continue;
					} else {
						String failMessage = "Failed to locate element with ID '" + elementId + "' after " + maxAttempts
								+ " attempts.";
						el.logExecution(failMessage);
						if (tsel != null)
							tsel.logExecution(failMessage);
						result.put("result", false);
						result.put("reason",
								getReadableFailureReason(new org.openqa.selenium.NoSuchElementException(failMessage)));
					}
				}
			} catch (Exception e) {
				Map<String, String> errorDetails = helperMethods.getAutomationErrorDetails(e);
				el.logExecution("Error in performFindAndAssignAction for elementId '" + elementId + "' (Attempt "
						+ currentAttempt + "): " + errorDetails.get("message"));
				if (tsel != null)
					tsel.logExecution("Error in performFindAndAssignAction for elementId '" + elementId + "' (Attempt "
							+ currentAttempt + "): " + errorDetails.get("message"));
				result.put("result", false);
				result.put("errorCode", errorDetails.get("code"));
				result.put("errorMessage", errorDetails.get("message"));
				result.put("reason", errorDetails.get("message"));
				hasFailed = true;
			}
			break;
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
			String desc = safeGetAttr(element, "content-desc");

			return String.join(" | ", "strategy=" + strategy, "selector=" + selector, "name=" + name, "label=" + label,
					"value=" + value, "type=" + type, "desc=" + desc, "enabled=" + element.isEnabled(),
					"visible=" + element.isDisplayed());
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
			for (String part : parts) {
				String[] keyVal = part.trim().split("=", 2);
				if (keyVal.length == 2) {
					String key = keyVal[0].trim();
					String value = keyVal[1].trim();
					if (!key.equals("strategy") && !key.equals("selector") && !value.equals("null")
							&& !value.equals("error")) {
						signatureAttributes.put(key, value);
					}
				}
			}

			if (signatureAttributes.isEmpty()) {
				el.logExecution("Last signature is empty or contains no usable attributes.");
				return null;
			}

			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			DocumentBuilder builder = factory.newDocumentBuilder();
			Document doc = builder.parse(new InputSource(new StringReader(pageSource)));
			NodeList allNodes = doc.getElementsByTagName("*");

			for (int i = 0; i < allNodes.getLength(); i++) {
				Element element = (Element) allNodes.item(i);
				boolean isMatch = true;

				for (Map.Entry<String, String> entry : signatureAttributes.entrySet()) {
					String attrName = entry.getKey();
					String expectedVal = entry.getValue();
					if (!expectedVal.equals(element.getAttribute(attrName))) {
						isMatch = false;
						break;
					}
				}

				if (isMatch) {
					String tagName = element.getTagName();
					StringBuilder xpath = new StringBuilder("//").append(tagName);
					List<String> conditions = new ArrayList<>();
					for (Map.Entry<String, String> entry : signatureAttributes.entrySet()) {
						conditions.add("@" + entry.getKey() + "='" + entry.getValue() + "'");
					}
					if (!conditions.isEmpty()) {
						xpath.append("[").append(String.join(" and ", conditions)).append("]");
					}
					String finalXPath = xpath.toString();
					el.logExecution("Generated fallback XPath: " + finalXPath);
					if (tsel != null)
						tsel.logExecution("Generated fallback XPath: " + finalXPath);
					return finalXPath;
				}
			}
			el.logExecution("No matching element found in parsed page source.");
			if (tsel != null)
				tsel.logExecution("No matching element found in parsed page source.");
		} catch (Exception e) {
			el.logExecution("Error building fallback XPath ");
			if (tsel != null)
				tsel.logExecution("Error building fallback XPath ");
		}
		return null;
	}

	private Map<String, Object> performAssertPartialAction(String testData, ExecutionLogger el, ExecutionLogger tsel,
			Integer stepNumber, Integer testCaseResultsId) {

		Map<String, Object> result = new HashMap<>();
		String validationResult = "";
		boolean status = false;
		try {
			if (testData == null || testData.isEmpty()) {
				el.logExecution("Error: No text to assert provided in testData");
				if (tsel != null)
					tsel.logExecution("Error: No text to assert provided in testData");
				result.put("status", false);
				result.put("result", "Validation : FAIL - No text provided");
				return result;
			}

			String pageSource = driver.getPageSource();
			boolean isTextPresent = pageSource.contains(testData);

			if (isTextPresent) {
				el.logExecution("AssertPartial PASSED: Text '" + testData + "' is present.");
				if (tsel != null)
					tsel.logExecution("AssertPartial PASSED: Text '" + testData + "' is present.");
				validationResult = "Validation : PASS FOR" + testData;
				status = true;
			} else {
				el.logExecution("AssertPartial FAILED: Text '" + testData + "' is NOT present.");
				if (tsel != null)
					tsel.logExecution("AssertPartial FAILED: Text '" + testData + "' is NOT present.");
				validationResult = "Validation : FAIL FOR" + testData;
				status = true;
			}
			result.put("status", status);
			result.put("result", validationResult);
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

					element.click();
					element.clear();
					Thread.sleep(200);
					for (char c : value.toCharArray()) {
						element.sendKeys(String.valueOf(c));
						Thread.sleep(100);
					}
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
			el.logExecution(errorMsg);
			if (tsel != null)
				tsel.logExecution(errorMsg);
			result.put("result", false);
			result.put("reason", errorMsg);
		}
		return result;
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

	private Map<String, Object> performClickAction(String elementId, ExecutionLogger el, ExecutionLogger tsel,
			Integer stepNumber, Integer testCaseResultsId) {
		Map<String, Object> result = new HashMap<>();
		try {
			WebElement element = elementStore.get(elementId);
			Utilities.executionMsgCache.put(testCaseResultsId,
					"Step number: " + stepNumber + " Element Validation is happening");
			validateElement(elementId, el, tsel);
			if (element != null) {
				checkElementState(element, el, tsel);
				Utilities.executionMsgCache.put(testCaseResultsId,
						"Step number: " + stepNumber + " Element State is being checked");
				waitForClickable(element);
				element.click();
				result.put("result", true);
			} else {
				String msg = "No element found with ID: " + elementId;
				el.logExecution(msg);
				if (tsel != null)
					tsel.logExecution(msg);
				result.put("result", false);
				result.put("reason", getReadableFailureReason(new IllegalArgumentException(msg)));
			}
		} catch (Exception e) {
			String errorMsg = "Error in performClickAction: " + getReadableFailureReason(e);
			el.logExecution(errorMsg);
			if (tsel != null)
				tsel.logExecution(errorMsg);
			result.put("result", false);
			result.put("reason", errorMsg);
		}
		return result;
	}

	private Map<String, Object> performTapAction(JSONObject params, ExecutionLogger el, ExecutionLogger tsel,
			Integer stepNumber, Integer testCaseResultsId) {
		Map<String, Object> result = new HashMap<>();
		try {
			if (!params.has("actions")) {
				String msg = "Invalid tap action payload: missing actions.";
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
			if (tsel != null)
				tsel.logExecution("Tap Scaled Coordinates: (" + scaledX + ", " + scaledY + ")");
			if (tsel != null)
				tsel.logExecution("Screen Size: (" + screenWidth + "x" + screenHeight + ")");

			PointerInput finger = new PointerInput(PointerInput.Kind.TOUCH, "finger1");
			Sequence tapSequence = new Sequence(finger, 1);

			tapSequence.addAction(
					finger.createPointerMove(Duration.ofMillis(100), PointerInput.Origin.viewport(), scaledX, scaledY));
			tapSequence.addAction(finger.createPointerDown(PointerInput.MouseButton.LEFT.asArg()));
			tapSequence.addAction(new Pause(finger, Duration.ofMillis(200)));
			tapSequence.addAction(finger.createPointerUp(PointerInput.MouseButton.LEFT.asArg()));

			driver.perform(Collections.singletonList(tapSequence));
			Thread.sleep(500);

			result.put("result", true);
		} catch (Exception e) {
			String errorMsg = getReadableFailureReason(e);
			el.logExecution("Error in performTapAction: " + errorMsg);
			if (tsel != null)
				tsel.logExecution("Error in performTapAction: " + errorMsg);
			result.put("result", false);
			result.put("reason", errorMsg);
		}
		return result;
	}

	private Map<String, Object> performSwipeAction(JSONObject params, ExecutionLogger el, ExecutionLogger tsel,
			Integer stepNumber, Integer testCaseResultsId) {
		Map<String, Object> result = new HashMap<>();
		try {
			if (!params.has("actions")) {
				String msg = "Invalid swipe action payload: missing actions.";
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
			Utilities.executionMsgCache.put(testCaseResultsId,
					"Step number: " + stepNumber + " Co-Ordinates are being Analyzed");
			int scaledStartX = (int) (startXRatio * screenSize.getWidth());
			int scaledStartY = (int) (startYRatio * screenSize.getHeight());
			int scaledEndX = (int) (endXRatio * screenSize.getWidth());
			int scaledEndY = (int) (endYRatio * screenSize.getHeight());

			el.logExecution("Swipe Scaled Start: (" + scaledStartX + ", " + scaledStartY + ") ➡️ End: (" + scaledEndX
					+ ", " + scaledEndY + ")");
			if (tsel != null)
				tsel.logExecution("Swipe Scaled Start: (" + scaledStartX + ", " + scaledStartY + ") ➡️ End: ("
						+ scaledEndX + ", " + scaledEndY + ")");

			PointerInput finger = new PointerInput(PointerInput.Kind.TOUCH, "finger1");
			Sequence swipeSequence = new Sequence(finger, 1);
			swipeSequence.addAction(finger.createPointerMove(Duration.ofMillis(100), PointerInput.Origin.viewport(),
					scaledStartX, scaledStartY));
			swipeSequence.addAction(finger.createPointerDown(PointerInput.MouseButton.LEFT.asArg()));
			swipeSequence.addAction(finger.createPointerMove(Duration.ofMillis(750), PointerInput.Origin.viewport(),
					scaledEndX, scaledEndY));
			swipeSequence.addAction(finger.createPointerUp(PointerInput.MouseButton.LEFT.asArg()));

			driver.perform(Collections.singletonList(swipeSequence));
			Thread.sleep(500);

			result.put("result", true);
		} catch (Exception e) {
			String errorMsg = "Error in performSwipeAction: " + getReadableFailureReason(e);
			el.logExecution(errorMsg);
			if (tsel != null)
				tsel.logExecution(errorMsg);
			result.put("result", false);
			result.put("reason", errorMsg);
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

			if (isSimulator(driver)) {
				stopSimulatorRecording(el, tsel);
			} else {
				synchronized (recordingChunkPaths) {
					if (!recordingChunkPaths.isEmpty()) {
						saveAllRecordings(Test_Case_Id, token, testCaseResultId, el, tsel);
					} else {
						el.logExecution("No video recordings to save");
						if (tsel != null)
							tsel.logExecution("No video recordings to save");
					}
				}
			}

			String packageName = extractBundleIdFromApp(apkPath);
			if (driver != null) {
				try {
					if (driver.getCapabilities().getPlatformName().toString().equalsIgnoreCase("ios")) {
						((IOSDriver) driver).terminateApp(packageName);
					}
					driver.quit();
				} catch (Exception e) {
					el.logExecution("Error during driver cleanup. ");
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

	private void startSimulatorRecording(ExecutionLogger el, ExecutionLogger tsel) {
		try {
			long timestamp = System.currentTimeMillis();
			mp4FilePath = "/Users/subhamkejriwal/Desktop/sim_recording_" + timestamp + ".mp4";

			ProcessBuilder pb = new ProcessBuilder("xcrun", "simctl", "io", "booted", "recordVideo", mp4FilePath);
			pb.redirectErrorStream(true);
			simulatorRecordingProcess = pb.start();

			el.logExecution("Simulator screen recording started. Output: " + mp4FilePath);
			if (tsel != null)
				tsel.logExecution("Simulator screen recording started. Output: " + mp4FilePath);

		} catch (IOException e) {
			el.logExecution("Failed to start simulator recording. ");
			if (tsel != null)
				tsel.logExecution("Failed to start simulator recording. ");
		}
	}

	private void stopSimulatorRecording(ExecutionLogger el, ExecutionLogger tsel) {
		try {
			if (simulatorRecordingProcess != null) {
				simulatorRecordingProcess.destroy();
				simulatorRecordingProcess.waitFor();
				el.logExecution("Simulator screen recording stopped. Saved to: " + mp4FilePath);
				if (tsel != null)
					tsel.logExecution("Simulator screen recording stopped. Saved to: " + mp4FilePath);
			}
		} catch (Exception e) {
			el.logExecution("Failed to stop simulator recording. ");
			if (tsel != null)
				tsel.logExecution("Failed to stop simulator recording. ");
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

	private String getElementDescription(JSONObject params, ExecutionLogger el, ExecutionLogger tsel) {
		try {
			String selector = params.getString("selector");
			WebElement element;
			if (selector.trim().startsWith("**/")) {
				element = driver.findElement(AppiumBy.iOSClassChain(selector));
			} else {
				element = driver.findElement(AppiumBy.iOSNsPredicateString(selector));
			}
			String description = element.getAttribute("label");
			if (description == null || description.isEmpty()) {
				description = element.getAttribute("name");
			}
			if ((description == null || description.isEmpty()) && (element.getTagName().contains("TextField")
					|| element.getTagName().contains("SecureTextField"))) {
				description = element.getAttribute("value");
			}
			if (description == null || description.isEmpty()) {
				description = element.getAttribute("placeholderValue");
			}
			if (description == null || description.isEmpty()) {
				description = "Unknown element description";
			}
			return description;
		} catch (Exception e) {
			String errorMessage = "Could not get element description. ";
			el.logExecution(errorMessage);
			if (tsel != null)
				tsel.logExecution(errorMessage);
			return "N/A";
		}
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
						try {
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
								String videoData = ((IOSDriver) driver).stopRecordingScreen();
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
							((IOSDriver) driver).stopRecordingScreen();
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
			if (driver != null && driver.getCapabilities().getPlatformName().toString().equalsIgnoreCase("ios")) {
				if (isRecording.get()) {
					String finalVideo = ((IOSDriver) driver).stopRecordingScreen();
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
			el.logExecution("Validating login fields for the iOS app.");
			if (tsel != null)
				tsel.logExecution("Validating login fields for the iOS app.");

			userIdField = findFieldByHints(driver,
					new String[] { "login", "loginId", "username", "userId", "email", "ID" });
			if (userIdField != null) {
				el.logExecution(
						"User ID field found with attributes - name: " + userIdField.getAttribute("name") + ", label: "
								+ userIdField.getAttribute("label") + ", value: " + userIdField.getAttribute("value"));
				if (tsel != null)
					tsel.logExecution("User ID field found with attributes - name: " + userIdField.getAttribute("name")
							+ ", label: " + userIdField.getAttribute("label") + ", value: "
							+ userIdField.getAttribute("value"));
				userIdExists = true;
			} else {
				el.logExecution("User ID field not detected");
				if (tsel != null)
					tsel.logExecution("User ID field not detected");
			}

			passwordField = findFieldByHints(driver, new String[] { "password" });
			if (passwordField != null) {
				el.logExecution("Password field found with attributes - name: " + passwordField.getAttribute("name")
						+ ", label: " + passwordField.getAttribute("label") + ", value: "
						+ passwordField.getAttribute("value"));
				if (tsel != null)
					tsel.logExecution("Password field found with attributes - name: "
							+ passwordField.getAttribute("name") + ", label: " + passwordField.getAttribute("label")
							+ ", value: " + passwordField.getAttribute("value"));
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
			List<WebElement> fields = driver.findElements(AppiumBy.className("XCUIElementTypeTextField"));
			fields.addAll(driver.findElements(AppiumBy.className("XCUIElementTypeSecureTextField")));

			for (WebElement field : fields) {
				String name = field.getAttribute("name");
				String label = field.getAttribute("label");
				String value = field.getAttribute("value");
				String placeholder = field.getAttribute("placeholder");

				for (String keyword : hints) {
					if ((name != null && name.toLowerCase().contains(keyword))
							|| (label != null && label.toLowerCase().contains(keyword))
							|| (value != null && value.toLowerCase().contains(keyword))
							|| (placeholder != null && placeholder.toLowerCase().contains(keyword))) {
						return field;
					}
				}
			}
		} catch (Exception e) {
			System.err.println("Error in findFieldByHints (iOS). ");
		}
		return null;
	}

	public boolean verifyLoginSuccess(AppiumDriver driver, ExecutionLogger el, ExecutionLogger tsel) {
		try {
			boolean isLoginSuccessful = driver
					.findElements(AppiumBy.iOSNsPredicateString("label CONTAINS 'Welcome' OR name CONTAINS 'Welcome'"))
					.size() > 0;

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
			List<WebElement> priceElements = driver.findElements(
					AppiumBy.iOSNsPredicateString("value MATCHES '[0-9.,]*[0-9]' AND NOT (value CONTAINS '%')"));

			for (WebElement element : priceElements) {
				try {
					String elementText = element.getText();
					double price = helperMethods.parsePrice(elementText);
					String key = generatePriceKey(element);
					prices.put(key, price);
				} catch (Exception ignored) {
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

			List<WebElement> headerElements = driver.findElements(AppiumBy
					.iOSNsPredicateString("label CONTAINS '" + headerText + "' OR name CONTAINS '" + headerText + "'"));

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

			List<WebElement> priceElements = driver.findElements(
					AppiumBy.iOSNsPredicateString("value MATCHES '[0-9.,]*[0-9]' AND NOT (value CONTAINS '%')"));

			el.logExecution("Found " + priceElements.size() + " potential price elements");
			if (tsel != null)
				tsel.logExecution("Found " + priceElements.size() + " potential price elements");

			priceElements.sort(Comparator.comparing(e -> e.getLocation().getY()));

			List<Double> columnValues = new ArrayList<>();

			for (WebElement element : priceElements) {
				try {
					Point location = element.getLocation();

					if (location.getY() < stockListStartY || location.getY() > stockListEndY)
						continue;

					if (location.getX() < minX || location.getX() > maxX)
						continue;

					String text = element.getText();
					if (text == null || text.isEmpty() || text.contains("%") || text.contains("Mar"))
						continue;

					String cleanText = text.replaceAll("[^0-9.]", "");
					double value = Double.parseDouble(cleanText);

					columnValues.add(value);
					el.logExecution("Added stock price: " + value + " at Y=" + location.getY());
					if (tsel != null)
						tsel.logExecution("Added stock price: " + value + " at Y=" + location.getY());

				} catch (Exception ignored) {
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
			String platformName = driver.getCapabilities().getPlatformName().toString();

			if (platformName.equalsIgnoreCase("iOS")) {
				deviceInfo.put("deviceName", getIOSCapability("deviceName"));
				deviceInfo.put("platformVersion", getIOSCapability("platformVersion"));
				deviceInfo.put("platformName", getIOSCapability("platformName"));
				deviceInfo.put("udid", getIOSCapability("udid"));
				deviceInfo.put("bundleId", getIOSCapability("bundleId"));
				deviceInfo.put("isSimulator", isSimulator(driver));
			}

			StringBuilder logOutput = new StringBuilder();
			logOutput.append("\n========== iOS Device Information ==========\n");

			for (Map.Entry<String, Object> entry : deviceInfo.entrySet()) {
				if (entry.getValue() != null) {
					logOutput.append(String.format("%s: %s\n", formatKey(entry.getKey()), entry.getValue()));
				}
			}

			logOutput.append("============================================\n");

			el.logExecution(logOutput.toString());
			if (tsel != null)
				tsel.logExecution(logOutput.toString());
		} catch (Exception e) {
			el.logExecution("Error logging iOS device information. ");
			if (tsel != null)
				tsel.logExecution("Error logging iOS device information. ");
		}
	}

	private String formatKey(String key) {
		return key.replaceAll("([a-z])([A-Z])", "$1 $2");
	}

	private String getIOSCapability(String capability) {
		try {
			Object value = driver.getCapabilities().getCapability("appium:" + capability);
			return value != null ? value.toString() : "Unknown";
		} catch (Exception e) {
			return "Unknown";
		}
	}

	public void logAppInformation(String appPath, ExecutionLogger el, ExecutionLogger tsel) {
		if (appPath == null || appPath.isEmpty()) {
			el.logExecution("Invalid .app path provided.");
			if (tsel != null)
				tsel.logExecution("Invalid .app path provided.");
			return;
		}

		try {
			File plistFile = new File(appPath, "Info.plist");
			if (!plistFile.exists()) {
				el.logExecution("Info.plist not found in .app directory.");
				if (tsel != null)
					tsel.logExecution("Info.plist not found in .app directory.");
				return;
			}

			NSObject root = PropertyListParser.parse(plistFile);
			if (!(root instanceof NSDictionary)) {
				el.logExecution("Invalid Info.plist structure.");
				if (tsel != null)
					tsel.logExecution("Invalid Info.plist structure.");
				return;
			}

			NSDictionary dict = (NSDictionary) root;

			String bundleId = dict.objectForKey("CFBundleIdentifier").toString();
			String bundleName = dict.objectForKey("CFBundleName") != null ? dict.objectForKey("CFBundleName").toString()
					: "Unknown";
			String version = dict.objectForKey("CFBundleShortVersionString") != null
					? dict.objectForKey("CFBundleShortVersionString").toString()
					: "Unknown";
			String build = dict.objectForKey("CFBundleVersion") != null
					? dict.objectForKey("CFBundleVersion").toString()
					: "Unknown";

			long size = checkAppSize(appPath);

			StringBuilder appInfoLog = new StringBuilder();
			appInfoLog.append("\n========== .app Information ==========\n");
			appInfoLog.append(String.format("Bundle ID: \t%s\n", bundleId));
			appInfoLog.append(String.format("Name: \t\t%s\n", bundleName));
			appInfoLog.append(String.format("Version: \t%s\n", version));
			appInfoLog.append(String.format("Build: \t\t%s\n", build));
			appInfoLog.append(String.format(".app Size: \t%s MB\n", size));
			appInfoLog.append("======================================\n");

			el.logExecution(appInfoLog.toString());
			if (tsel != null)
				tsel.logExecution(appInfoLog.toString());

		} catch (Exception e) {
			el.logExecution("Error extracting .app information. ");
			if (tsel != null)
				tsel.logExecution("Error extracting .app information. ");
		}
	}

	public long checkAppSize(String appPath) {
		try {
			File appDir = new File(appPath);
			return getDirectorySize(appDir) / (1024 * 1024);
		} catch (Exception e) {
			return 0;
		}
	}

	private long getDirectorySize(File dir) {
		if (dir == null || !dir.exists())
			return 0;

		if (dir.isFile())
			return dir.length();

		long size = 0;
		File[] files = dir.listFiles();
		if (files != null) {
			for (File file : files) {
				size += getDirectorySize(file);
			}
		}
		return size;
	}

}
