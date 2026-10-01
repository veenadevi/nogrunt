package nogrunt.springboot.Controllers.service;

import nogrunt.ExecutionLogger;
import nogrunt.springboot.Controllers.executer.IOSAppiumExecutor;
import nogrunt.springboot.Controllers.model.MobileCapabilities;
import java.io.IOException;
import java.net.ServerSocket;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import nogrunt.springboot.Controllers.executer.AppiumExecutor;

@Service
public class AutomationService {

	@Autowired
	private MobileResultService mobileResultService;

	@Autowired
	private TokenVerificationService tokenVerificationService;

	@Autowired
	private MobileCapabilitiesService mobileCapabilitiesService;

	private static final Map<Integer, Set<Integer>> completedTestCases = new ConcurrentHashMap<>();
	private Map<Integer, AtomicInteger> passCountMap = new ConcurrentHashMap<>();
	private Map<Integer, AtomicInteger> failCountMap = new ConcurrentHashMap<>();
	private static final Set<Integer> allAssignedPorts = Collections.synchronizedSet(new HashSet<>());
	private static final Map<Integer, Integer> testCaseToPortMap = new ConcurrentHashMap<>();
	private static final Map<Integer, Integer> testCaseToWdaPortMap = new ConcurrentHashMap<>();
	private static final Map<Integer, ConcurrentHashMap<String, String>> suiteVarMaps = new ConcurrentHashMap<>();
	private static final Map<Integer, AppiumExecutor> runningAndroidExecutors = new ConcurrentHashMap<>();
	private static final Map<Integer, IOSAppiumExecutor> runningIOSExecutors = new ConcurrentHashMap<>();
	private static final Map<Integer, Boolean> runningSuites = new ConcurrentHashMap<>();

	public String runAutomation(Integer testCaseId, String token) {
		String platform;
		try {
			MobileCapabilities cap = mobileCapabilitiesService.getCapabilitiesByTestCaseID(testCaseId);
			platform = cap.getPlatformName();

			if (platform == null) {
				return "Platform is not specified for Test Case ID: " + testCaseId;
			}

			if (platform.equalsIgnoreCase("Android")) {
				ConcurrentHashMap<String, String> varMap = new ConcurrentHashMap<>();
				AppiumExecutor androidExecutor = new AppiumExecutor();
				runningAndroidExecutors.put(testCaseId, androidExecutor);
				androidExecutor.setVarMap(varMap);
				androidExecutor.runAndroidAutomation(testCaseId, token, mobileResultService, tokenVerificationService,
						mobileCapabilitiesService);
				return "Android test case " + testCaseId + " executed successfully.";
			} else if (platform.equalsIgnoreCase("iOS")) {
				ConcurrentHashMap<String, String> varMap = new ConcurrentHashMap<>();
				IOSAppiumExecutor iosExecutor = new IOSAppiumExecutor();
				runningIOSExecutors.put(testCaseId, iosExecutor);
				iosExecutor.setVarMap(varMap);
				iosExecutor.runIOSAutomation(testCaseId, token, mobileResultService, tokenVerificationService,
						mobileCapabilitiesService);
				return "iOS test case " + testCaseId + " executed successfully.";
			} else {
				return "Unsupported platform: " + platform + " for Test Case ID: " + testCaseId;
			}
		} catch (Exception e) {
			System.err.println("Error while executing Test Case ID: " + testCaseId);
			e.printStackTrace();
			return "Test case " + testCaseId + " failed due to exception.";
		}
	}

	public String runSuitAutomation(Integer Test_Case_Id, String token, Integer testSuitResultId, ExecutionLogger tsel,
			boolean isParallel, Integer totalCount, long tsStartTime) {
		String platform;
		try {
			completedTestCases.putIfAbsent(testSuitResultId, ConcurrentHashMap.newKeySet());
			passCountMap.putIfAbsent(testSuitResultId, new AtomicInteger(0));
			failCountMap.putIfAbsent(testSuitResultId, new AtomicInteger(0));

			MobileCapabilities cap = mobileCapabilitiesService.getCapabilitiesByTestCaseID(Test_Case_Id);
			platform = cap.getPlatformName();

			if (platform == null) {
				return "Platform is not specified for Test Case ID: " + Test_Case_Id;
			}

			String finalPlatform = platform.toLowerCase();
			runningSuites.put(testSuitResultId, true);

			Callable<String> task = () -> {
				String status = "";
				Integer systemPort = getDynamicSystemPort(true, tsel);
				Integer wdaLocalPort = getDynamicWdaPort(true, tsel);
				if (systemPort != null) {
					testCaseToPortMap.put(Test_Case_Id, systemPort);
				}
				if (wdaLocalPort != null) {
					testCaseToWdaPortMap.put(Test_Case_Id, wdaLocalPort);
				}

				try {
					if (!runningSuites.getOrDefault(testSuitResultId, true)) {
						tsel.logExecution(
								"Suite execution manually stopped before running test case ID: " + Test_Case_Id);
						releaseSystemPortForTestCase(Test_Case_Id, tsel);
						return "STOPPED";
					}
					if (finalPlatform.equalsIgnoreCase("android")) {
						suiteVarMaps.putIfAbsent(testSuitResultId, new ConcurrentHashMap<>());
						ConcurrentHashMap<String, String> varMap = suiteVarMaps.get(testSuitResultId);
						AppiumExecutor appium = new AppiumExecutor();
						appium.setVarMap(varMap);
						status = appium.runAndroidSuitAutomation(Test_Case_Id, token, testSuitResultId, tsel,
								mobileResultService, tokenVerificationService, mobileCapabilitiesService, systemPort);
					} else if (finalPlatform.equalsIgnoreCase("ios")) {
						suiteVarMaps.putIfAbsent(testSuitResultId, new ConcurrentHashMap<>());
						ConcurrentHashMap<String, String> varMap = suiteVarMaps.get(testSuitResultId);
						IOSAppiumExecutor iosExecutor = new IOSAppiumExecutor();
						iosExecutor.setVarMap(varMap);
						status = iosExecutor.runIOSSuitAutomation(Test_Case_Id, token, testSuitResultId, tsel,
								mobileResultService, tokenVerificationService, mobileCapabilitiesService, systemPort,
								wdaLocalPort);
					} else {
						status = "Unsupported platform: " + finalPlatform;
						tsel.logExecution(status);
					}

					if ("PASS".equalsIgnoreCase(status)) {
						passCountMap.get(testSuitResultId).incrementAndGet();
					} else {
						failCountMap.get(testSuitResultId).incrementAndGet();
					}
					releaseSystemPortForTestCase(Test_Case_Id, tsel);
					return status;
				} catch (Exception e) {
					failCountMap.get(testSuitResultId).incrementAndGet();
					tsel.logExecution("Test case execution failed for ID: " + Test_Case_Id);
					e.printStackTrace();
					return "FAIL";
				} finally {
					completedTestCases.get(testSuitResultId).add(Test_Case_Id);

					int completed = completedTestCases.get(testSuitResultId).size();

					if (completed >= totalCount) {
						int pass = passCountMap.get(testSuitResultId).get();
						int fail = failCountMap.get(testSuitResultId).get();

						AppiumExecutor executor = new AppiumExecutor();
						executor.updateTestSuitStatus(testSuitResultId, totalCount, tsStartTime, pass, fail);
						cleanUpLeftoverPorts(tsel);
						completedTestCases.remove(testSuitResultId);
						runningSuites.remove(testSuitResultId);
						passCountMap.remove(testSuitResultId);
						failCountMap.remove(testSuitResultId);
					}
				}
			};

			if (isParallel) {
				ExecutorService executor = Executors.newSingleThreadExecutor();
				Future<String> result = executor.submit(task);
				String finalStatus = result.get();
				executor.shutdown();
				return finalStatus;
			} else {
				return task.call();
			}

		} catch (Exception e) {
			System.err.println("Error while executing Test Case ID: " + Test_Case_Id);
			e.printStackTrace();
			return "Error Occurred";
		}
	}

	public Integer getDynamicSystemPort(boolean isParallel, ExecutionLogger tsel) {
		if (!isParallel) {
			return null;
		}
		try (ServerSocket socket = new ServerSocket(0)) {
			int port = socket.getLocalPort();
			allAssignedPorts.add(port);
			if (tsel != null)
				tsel.logExecution("Acquired systemPort: " + port);
			if (tsel != null)
				tsel.logExecution("All assigned system ports so far: " + allAssignedPorts);
			return port;
		} catch (IOException e) {
			if (tsel != null)
				tsel.logExecution("Failed to acquire dynamic system port: " + e.getMessage());
			return null;
		}
	}

	public Integer getDynamicWdaPort(boolean isParallel, ExecutionLogger tsel) {
		if (!isParallel) {
			return null;
		}
		try (ServerSocket socket = new ServerSocket(0)) {
			int port = socket.getLocalPort();
			allAssignedPorts.add(port);
			if (tsel != null)
				tsel.logExecution("Acquired wdaLocalPort: " + port);
			if (tsel != null)
				tsel.logExecution("All assigned system ports so far (including WDA ports): " + allAssignedPorts);
			return port;
		} catch (IOException e) {
			if (tsel != null)
				tsel.logExecution("Failed to acquire dynamic wdaLocalPort: " + e.getMessage());
			return null;
		}
	}

	private void releaseSystemPortForTestCase(Integer testCaseId, ExecutionLogger tsel) {
		Integer port = testCaseToPortMap.remove(testCaseId);
		if (port != null) {
			releaseSystemPort(port, tsel);
		}
		Integer wdaPort = testCaseToWdaPortMap.remove(testCaseId);
		if (wdaPort != null) {
			releaseSystemPort(wdaPort, tsel);
		}
	}

	public void releaseSystemPort(Integer port, ExecutionLogger tsel) {
		if (port != null && allAssignedPorts.remove(port)) {
			if (tsel != null)
				tsel.logExecution("Released systemPort: " + port);
			if (tsel != null)
				tsel.logExecution("Remaining assigned ports: " + allAssignedPorts);
		}
	}

	private void cleanUpLeftoverPorts(ExecutionLogger tsel) {
		if (!allAssignedPorts.isEmpty()) {
			if (tsel != null) {
				tsel.logExecution("Cleaning up leftover ports: " + allAssignedPorts);

				for (Map.Entry<Integer, Integer> entry : testCaseToPortMap.entrySet()) {
					Integer testCaseId = entry.getKey();
					Integer port = entry.getValue();
					if (allAssignedPorts.contains(port)) {
						tsel.logExecution("Leak Detected: TestCaseID " + testCaseId + " had un-released port " + port);
					}
				}
			}
			allAssignedPorts.clear();
			testCaseToPortMap.clear();
		} else {
			if (tsel != null) {
				tsel.logExecution("No leftover ports after suite execution.");
			}
		}
	}

	public String stopExecution(Integer testCaseId, String token, Integer testCaseResultId) {
		try {
			if (runningAndroidExecutors.containsKey(testCaseId)) {
				AppiumExecutor executor = runningAndroidExecutors.remove(testCaseId);
				String result = executor.tearDown(testCaseId, token, null, testCaseResultId, null);
				releaseSystemPortForTestCase(testCaseId, null);
				return "Android execution stopped: " + result;
			}
			if (runningIOSExecutors.containsKey(testCaseId)) {
				IOSAppiumExecutor executor = runningIOSExecutors.remove(testCaseId);
				String result = executor.tearDown(testCaseId, token, null, testCaseResultId, null);
				releaseSystemPortForTestCase(testCaseId, null);
				return "iOS execution stopped: " + result;
			}
			return "No active execution found for Test Case ID: " + testCaseId;
		} catch (Exception e) {
			return "Failed to stop execution: " + e.getMessage();
		}
	}

	public void stopEntireSuite(Integer testSuitResultId) {
		runningSuites.put(testSuitResultId, false);
	}

}
