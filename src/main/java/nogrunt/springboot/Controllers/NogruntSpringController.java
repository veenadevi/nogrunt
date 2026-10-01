package nogrunt.springboot.Controllers;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import nogrunt.ExecutionLogger;
import nogrunt.LoggerRegistry;
import nogrunt.springboot.Controllers.dtoModel.MobileTestCaseDto;
import nogrunt.springboot.Controllers.model.MobileCapabilities;
import nogrunt.springboot.Controllers.service.ActionPreProcessingService;
import nogrunt.springboot.Controllers.service.ApkHandler;
import nogrunt.springboot.Controllers.service.AutomationService;
import nogrunt.springboot.Controllers.service.MobileCapabilitiesService;
import nogrunt.springboot.Controllers.service.MobileTestService;
import nogrunt.springboot.Controllers.service.TokenVerificationService;

@RestController
@CrossOrigin(origins = "http://localhost:5173")
public class NogruntSpringController {

	private static final Logger logger = LoggerFactory.getLogger(NogruntSpringController.class);

	@Autowired
	private AutomationService automationService;

	@Autowired
	private ActionPreProcessingService actionPreProcessingService;

	@Autowired
	private MobileTestService testService;

	@Autowired
	private TokenVerificationService tokenVerificationService;

	@Autowired
	private ApkHandler apkHandler;

	@Autowired
	private MobileCapabilitiesService mobileCapabilitiesService;

	@GetMapping("/spring-endpoint")
	public String handleRequest() {
		return "Hello from Spring Boot!";
	}

	@PostMapping("/execute")
	public ResponseEntity<String> executeAutomation(@RequestParam(value = "token") String token,
			@RequestParam(value = "Test_Case_Id") Integer Test_Case_Id) {
		try {
			boolean tokenCache = tokenVerificationService.isTokenValid(token);
			if (!tokenCache) {
				return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid token.");
			}
			automationService.runAutomation(Test_Case_Id, token);
			return ResponseEntity.ok("Automation executed successfully!");
		} catch (Exception e) {
			e.printStackTrace();
			return ResponseEntity.internalServerError().body("Error executing automation: " + e.getMessage());
		}
	}

	@PostMapping("/executeSuite")
	public ResponseEntity<String> executeSuitAutomation(@RequestParam(value = "token") String token,
			@RequestParam(value = "Test_Case_Id") Integer Test_Case_Id,
			@RequestParam(value = "testSuiteResultId") Integer testSuitResultId,
			@RequestParam(value = "totalCount") Integer totalCount, @RequestParam("logId") String logId,
			@RequestParam(value = "tsStartTime") long tsStartTime,
			@RequestParam(value = "isParallel") boolean isParallel) {
		try {
			boolean tokenCache = tokenVerificationService.isTokenValid(token);
			if (!tokenCache) {
				return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid token.");
			}
			ExecutionLogger tsel = LoggerRegistry.getLogger(logId);
			tsel.logExecution("Executing Test Case: " + Test_Case_Id);
			String result = automationService.runSuitAutomation(Test_Case_Id, token, testSuitResultId, tsel, isParallel,
					totalCount, tsStartTime);

			if ("PASS".equalsIgnoreCase(result) || "FAIL".equalsIgnoreCase(result)) {
				return ResponseEntity.ok(result);
			} else if ("Unsupported platform: android/ios/etc.".equalsIgnoreCase(result)) {
				return ResponseEntity.badRequest().body(result);
			} else {
				return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
						.body("Execution error for Test Case ID: " + Test_Case_Id + " → " + result);
			}
		} catch (Exception e) {
			e.printStackTrace();
			return ResponseEntity.internalServerError().body("Exception occurred: " + e.getMessage());
		}
	}

//	@PostMapping("/mobileAutomation/action")
//	public ResponseEntity<String> preprocessAndSaveActions(@RequestBody List<Map<String, Object>> actions,
//			@RequestParam(value = "token") String token) {
//		try {
//			boolean tokenCache = tokenVerificationService.isTokenValid(token);
//			if (tokenCache == false) {
//				return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid token.");
//			}
//			for (Map<String, Object> action : actions) {
//				actionPreProcessingService.preprocessAction(action);
//			}
//			return ResponseEntity.ok("Actions processed and saved successfully!");
//		} catch (Exception e) {
//			e.printStackTrace();
//			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
//					.body("Failed to process and save actions: " + e.getMessage());
//		}
//	}
	@PostMapping("/mobileAutomation/action")
	public ResponseEntity<String> preprocessAndSaveActions(@RequestBody List<Map<String, Object>> actions) {
		try {
			for (Map<String, Object> action : actions) {
				actionPreProcessingService.preprocessAction(action);
			}
			return ResponseEntity.ok("Actions processed and saved successfully!");
		} catch (Exception e) {
			e.printStackTrace();
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body("Failed to process and save actions: " + e.getMessage());
		}
	}

	@PostMapping("/saveTestCase")
	public ResponseEntity<String> saveOrUpdateTestCase(@RequestParam(value = "token") String token,
			@RequestParam(value = "continueRecording") boolean continueRecording,
			@RequestParam(value = "testCaseId", required = false) Integer testCaseId,
			@RequestParam(value = "capabilities_id", required = false) Integer capabilities_id,
			@RequestBody MobileTestCaseDto mobileTestCaseDto) {
		try {
			boolean tokenCache = tokenVerificationService.isTokenValid(token);
			if (tokenCache == false) {
				return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid token.");
			}
			if (continueRecording) {
				if (testCaseId != null && testService.testCaseExists(testCaseId)) {
					String result = testService.updateCaseType(testCaseId);
					return ResponseEntity.ok(result);
				} else {
					testService.saveTestCase(mobileTestCaseDto, capabilities_id);
					return ResponseEntity.ok("Test Case saved successfully with ID: ");
				}
			} else {
				testService.saveTestCase(mobileTestCaseDto, capabilities_id);
				return ResponseEntity.ok("Test Case saved successfully with ID: ");
			}
		} catch (Exception e) {
			e.printStackTrace();
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body("An error occurred while processing the request: " + e.getMessage());
		}
	}

	@GetMapping("/stopExecution/{Test_Case_Id}/{token}/{testCaseResultId}")
	public ResponseEntity<String> stopExecution(@PathVariable("Test_Case_Id") Integer Test_Case_Id,
			@PathVariable("token") String token, @PathVariable("testCaseResultId") Integer testCaseResultId) {
		try {
			String result = automationService.stopExecution(Test_Case_Id, token, testCaseResultId);
			return ResponseEntity.ok("Execution stopped with result: " + result);
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body("Failed to stop execution: " + e.getMessage());
		}
	}

	@GetMapping("/stopSuiteExecution/{testSuitResultId}")
	public ResponseEntity<String> stopSuiteExecution(@PathVariable Integer testSuitResultId) {
		automationService.stopEntireSuite(testSuitResultId);
		return ResponseEntity.ok("Suite stop requested for suite ID: " + testSuitResultId);
	}

	@PostMapping("/apkHandler")
	public ResponseEntity<?> handleApk(@RequestParam(value = "action", required = true) String action,
			@RequestParam(value = "companyId", required = true) int companyid,
			@RequestParam(value = "file", required = false) MultipartFile file) {

		try {
			switch (action.toLowerCase()) {
			case "upload":
				if (file == null) {
					return ResponseEntity.badRequest().body("File is required for upload action");
				}
				String fileName = apkHandler.uploadApk(file, companyid);
				return ResponseEntity.ok("APK uploaded successfully. Saved as: " + fileName);

			case "choose":
				List<String> apkFiles = apkHandler.getApkList(companyid);
				return ResponseEntity.ok(apkFiles);

			default:
				return ResponseEntity.badRequest().body("Invalid action. Supported actions are 'upload' and 'list'");
			}

		} catch (IOException ex) {
			return ResponseEntity.badRequest().body(ex.getMessage());
		}
	}

	@PostMapping("/saveCapabilities")
	public ResponseEntity<?> saveCapabilities(@RequestParam(value = "companyId") Integer companyId,
			@RequestParam(value = "token") String token, @RequestBody JsonNode requestBody) {
		try {
			boolean tokenCache = tokenVerificationService.isTokenValid(token);
			if (tokenCache == false) {
				return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid token.");
			}
			ObjectMapper mapper = new ObjectMapper();
			JsonNode capNode = requestBody.get("capabilities");

			MobileCapabilities mobileCapabilities = new MobileCapabilities();

			mobileCapabilities.setName(requestBody.get("name").asText());

			mobileCapabilities.setPlatformName(capNode.get("platformName").asText());
			mobileCapabilities.setPlatformVersion(capNode.get("platformVersion").asText());
			mobileCapabilities.setDeviceName(capNode.get("deviceName").asText());
			mobileCapabilities.setAutomationName(capNode.get("automationName").asText());
			mobileCapabilities.setApp(capNode.get("app").asText());

			mobileCapabilities.setCompanyId(companyId);
			Integer id = mobileCapabilitiesService.saveMobileCapabilities(companyId, mobileCapabilities);
			return ResponseEntity.ok(id);
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(-1);
		}
	}

//	@GetMapping("/mobile-capabilities/{capabilities_id}/{token}")
//	public ResponseEntity<?> getAppiumCapabilities(@PathVariable("capabilities_id") Integer capabilities_id,
//			@PathVariable("token") String token) {
//		try {
//			boolean tokenCache = tokenVerificationService.isTokenValid(token);
//			if (tokenCache == false) {
//				return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid token.");
//			}
//			logger.info("Fetching Appium capabilities for capabilities_id: {}", capabilities_id);
//			Map<String, Object> result = mobileCapabilitiesService.getDesiredCapabilities(capabilities_id);
//			return ResponseEntity.ok(result);
//		} catch (Exception e) {
//			logger.error("Error fetching capabilities: {}", e.getMessage());
//			return ResponseEntity.status(500).body(Map.of("error", "Internal Server Error", "message", e.getMessage()));
//		}
//	}
	@GetMapping("/mobile-capabilities/{capabilities_id}")
	public ResponseEntity<?> getAppiumCapabilities(@PathVariable("capabilities_id") Integer capabilities_id) {
		try {
			logger.info("Fetching Appium capabilities for capabilities_id: {}", capabilities_id);
			Map<String, Object> result = mobileCapabilitiesService.getDesiredCapabilities(capabilities_id);
			return ResponseEntity.ok(result);
		} catch (Exception e) {
			logger.error("Error fetching capabilities: {}", e.getMessage());
			return ResponseEntity.status(500).body(Map.of("error", "Internal Server Error", "message", e.getMessage()));
		}
	}

	@GetMapping("/mobile-capabilities/getAll/{companyId}/{token}")
	public ResponseEntity<?> getAllCapabilitiesByCompany(@PathVariable("companyId") Integer companyId,
			@PathVariable("token") String token) {
		try {
			boolean tokenCache = tokenVerificationService.isTokenValid(token);
			if (tokenCache == false) {
				return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid token.");
			}
			logger.info("Fetching all capabilities for companyId: {}", companyId);
			List<Map<String, Object>> result = mobileCapabilitiesService.getAllCapabilitiesByCompanyId(companyId);
			return ResponseEntity.ok(result);
		} catch (Exception e) {
			logger.error("Error fetching capabilities for companyId {}: {}", companyId, e.getMessage());
			return ResponseEntity.status(500).body(Map.of("error", "Internal Server Error", "message", e.getMessage()));
		}
	}

//	private Map<String, Object> createCapability(String type, String name, Object value) {
//		Map<String, Object> capability = new HashMap<>();
//		capability.put("type", type);
//		capability.put("name", name);
//		capability.put("value", value);
//		return capability;
//	}
//
//	@GetMapping("/mobile-capabilities/get/cp1")
//	public ResponseEntity<?> getCapabilityById() {
//		try {
//			logger.info("Received request for capability with ID: {}");
//
//			// Create a list to store capabilities in the new format
//			List<Map<String, Object>> capabilities = new ArrayList<>();
//
//			// Add each capability as a map with type, name, and value
//			capabilities.add(createCapability("text", "appium:platformName", "ios"));
//			capabilities.add(createCapability("text", "appium:platformVersion", "18.4"));
////			capabilities.add(createCapability("text", "appium:udid", "6C1B4B32-682A-4E42-9DE2-BCCB6C010636"));
//			capabilities.add(createCapability("text", "appium:deviceName", "iPhone 16 Plus"));
//			capabilities.add(createCapability("text", "appium:automationName", "XCUITest"));
//			capabilities.add(createCapability("text", "appium:app", "/Users/subhamkejriwal/Downloads/sora.app"));
////			capabilities.add(createCapability("text", "appium:appPackage", "com.sharekhan.androidsharemobile"));
////			capabilities.add(
////					createCapability("text", "appium:appActivity", "com.sharekhan.androidsharemobile.MainActivity"));
////			capabilities.add(createCapability("text", "appium:appWaitActivity", "com.sharekhan.androidsharemobile.*"));
////                capabilities.add(createCapability("text", "appium:app", "C:\\Users\\Subham\\Download\\sampleCode\\sample-code\\apps\\ApiDemos\\bin\\ApiDemos-debug.apk"));
////                capabilities.add(createCapability("text", "appium:appPackage", "io.appium.android.apis"));
////                capabilities.add(createCapability("text", "appium:appActivity", "io.appium.android.apis.MainActivity"));
////                capabilities.add(createCapability("text", "appium:appWaitActivity", "io.appium.android.apis.*"));
////			capabilities.add(createCapability("boolean", "appium:fullReset", false));
////			capabilities.add(createCapability("boolean", "appium:noReset", true));
////			capabilities.add(createCapability("number", "appium:appWaitDuration", 30000));
////			capabilities.add(createCapability("number", "appium:deviceReadyTimeout", 30));
////			capabilities.add(createCapability("number", "appium:autoGrantPermissions", true));
////			capabilities.add(createCapability("number", "appium:grantPermissions", true));
////			capabilities.add(createCapability("boolean", "appium:skipDeviceInitialization", true));
////			capabilities.add(createCapability("boolean", "appium:skipServerInstallation", true));
////			capabilities.add(createCapability("boolean", "appium:disableIdLocatorAutocompletion", true));
//
//			// Log the generated list
//			logger.info("Returning capabilities: {}", capabilities);
//
//			return ResponseEntity.ok(capabilities);
//
//		} catch (Exception e) {
//			logger.error("Error processing request for capability with ID: " + e);
//			return ResponseEntity.status(500).body(Map.of("error", "Internal server error", "message", e.getMessage()));
//		}
//	}

//    @GetMapping("/mobile-capabilities/get/cp1")
//    public ResponseEntity<?> getCapabilityById() {
//        try {
//            logger.info("Received request for LambdaTest device capabilities");
//
//            List<Map<String, Object>> capabilities = new ArrayList<>();
//
//            capabilities.add(createCapability("text", "platformName", "Android"));
//            capabilities.add(createCapability("text", "deviceName", "Galaxy S20"));
//            capabilities.add(createCapability("text", "platformVersion", "11"));
//
//            capabilities.add(createCapability("text", "username", "firdousnogrunt"));
//            capabilities.add(createCapability("text", "accessKey", "4v1wgIJu4w1GiEYQmpe87bpnu4T4it4QxKfKjMJOUDr6aVTwFq"));
//
//            capabilities.add(createCapability("text", "remoteConnectionUrl", "https://firdousnogrunt:4v1wgIJu4w1GiEYQmpe87bpnu4T4it4QxKfKjMJOUDr6aVTwFq@mobile-hub.lambdatest.com/wd/hub"));
//            capabilities.add(createCapability("text", "app", "lt://APP123456789"));
//            capabilities.add(createCapability("text", "appPackage", "com.sharekhan.androidsharemobile"));
//            capabilities.add(createCapability("text", "appActivity", "com.sharekhan.androidsharemobile.MainActivity"));
//            capabilities.add(createCapability("text", "appWaitActivity", "com.sharekhan.androidsharemobile.*"));
//
//            capabilities.add(createCapability("text", "automationName", "UiAutomator2"));
//            capabilities.add(createCapability("boolean", "isRealMobile", true));
//            capabilities.add(createCapability("boolean", "autoGrantPermissions", true));
//            capabilities.add(createCapability("boolean", "noReset", true));
//            capabilities.add(createCapability("boolean", "fullReset", false));
//            capabilities.add(createCapability("number", "appWaitDuration", 30000));
//            capabilities.add(createCapability("number", "deviceReadyTimeout", 30));
//            capabilities.add(createCapability("boolean", "skipDeviceInitialization", true));
//            capabilities.add(createCapability("boolean", "skipServerInstallation", true));
//
//            // Test identification
//            capabilities.add(createCapability("text", "project", "SharekhanApp"));
//            capabilities.add(createCapability("text", "build", "Android Testing"));
//            capabilities.add(createCapability("text", "name", "Appium Inspector Session"));
//
//            logger.info("Returning hardcoded LambdaTest capabilities: {}", capabilities);
//            return ResponseEntity.ok(capabilities);
//
//        } catch (Exception e) {
//            logger.error("Error processing request for capability: " + e);
//            return ResponseEntity.status(500)
//                    .body(Map.of(
//                            "error", "Internal server error",
//                            "message", e.getMessage()
//                    ));
//        }
//    }

}
