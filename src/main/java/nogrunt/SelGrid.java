package nogrunt;

import nogrunt.scripts.*;
import nogrunt.api.ApiHandler;
import nogrunt.integrations.*;

import java.util.logging.*;
import java.io.File;
import java.io.FileReader;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.FilenameFilter;
import java.net.URL;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Optional;
import java.time.Duration;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.util.Arrays;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.remote.Augmenter;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.openqa.selenium.Keys;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.Point;
import org.openqa.selenium.NoSuchWindowException;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.NoSuchSessionException;
import org.openqa.selenium.Proxy;
import org.openqa.selenium.chrome.*;
import org.openqa.selenium.Capabilities;
import org.openqa.selenium.remote.CapabilityType;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.Alert;

import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.edge.EdgeOptions;
import org.openqa.selenium.safari.SafariDriver;

import java.awt.AWTException;
import java.awt.Robot;
import java.awt.event.InputEvent;

import java.util.stream.Collectors;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.InetSocketAddress;

import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;

import java.awt.*;
import java.awt.datatransfer.*;

import org.testng.annotations.*;
//import atu.testrecorder.ATUTestRecorder;

import java.net.MalformedURLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

//import org.apache.http.HttpResponse;
//import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import org.apache.commons.csv.*;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;

public class SelGrid {
	WebDriver driver;
	String sessionId;
	HashMap<String, String> randomMap = new HashMap<>();
	ConcurrentHashMap<String, String> varMap = null;
	HashMap<Integer, JSONObject> distanceMap = new HashMap<>();
	int d0index = -1;
	String BROWSER = null;
	String runIssues = "";
	String errorCode = "";
	String nonMatchingAttr = "";
	ExecutionLogger el;
	ExecutionLogger al;
	boolean indexUsed = false;
	int mrIndex = -1;
	int mrFieldIndex = 0;
	int xlsColCount = -1;
	int xlsRowCount = -1;
	boolean eof = false;
	int tillStepNum = -1;
	int tillTC = -1;
	String downloadDir = "";
	int tsr = -1;
	boolean ssonerror = false;
	MySQlConn msc;
	GridManager gm;
	int cid = -1;
	boolean enableaudio;
	boolean enablevideo;
	boolean headless;
	boolean stealthMode;
	String proxyUrl;
	HashMap windowDrivers = new HashMap();
	boolean inline = false;
	boolean dependantTC = false;
	String prevRowiframexpath = "";
	int prevTabId = -1;
	int prevWindowId = -1;
	int port = 0;
	boolean debugMode = false;
	
	public WebDriver getDriver() {
		return driver;
	}
	
	public void setDriver(WebDriver wd) {
		driver = wd;
	}
	
	public void setVarMap(ConcurrentHashMap vm) {
		varMap = vm;
	}
	
	public void setTillStepNum(int sn) {
		tillStepNum = sn;
	}
	
	public void setTillTC(int sn) {
		tillTC = sn;
	}
	
	public void setGM(GridManager g) {
		gm = g;
	}
	
	public void chromeGridSetupWithCRX() {
		System.setProperty("webdriver.chrome.driver", AppProperties.webdriverchromedriver);
    	ChromeOptions options = new ChromeOptions();
    	options.addArguments("load-extension=" + AppProperties.crxloc);
		options.addArguments("--remote-allow-origins=*");
		
		DesiredCapabilities capabilities = new DesiredCapabilities();
		capabilities.setCapability(ChromeOptions.CAPABILITY, options);
		capabilities.setBrowserName("chrome");
		//capabilities.setCapability("nodeid", "vm1");
		try {
			driver = new RemoteWebDriver(new URL(AppProperties.seleniumhub), capabilities);
		} catch (Exception e) {
			e.printStackTrace();
		}
		BROWSER = "Chrome";
		getDriverSession(driver);
	}

	public WebDriver chromeGridSetup(boolean headless, String proxyurl, boolean enableaudio,
	        boolean enablevideo, String profileName, int port, int companyId,
	        boolean stealthMode) {
	    System.setProperty("webdriver.chrome.driver", AppProperties.webdriverchromedriver);
	    this.enableaudio = enableaudio;
	    this.enablevideo = enablevideo;
	    Logger.getLogger("").setLevel(Level.WARNING);

	    if (!headless) {
	        System.setProperty("java.awt.headless", "false"); // Force GUI mode
	    }

	    ChromeOptions options = new ChromeOptions();
	    options.addArguments("--remote-allow-origins=*");
	    if(AppProperties.apicapture.equalsIgnoreCase("true")) {
	    	options.addArguments("--remote-debugging-port=" + port);
		}
	    options.addArguments("--remote-debugging-address=0.0.0.0");
	    options.addArguments("--no-sandbox"); // Sometimes helps with connectivity issues
	    
	    // Disable web security for CDP connections (only if needed)
	    options.addArguments("--disable-web-security");

	    if (enablevideo) {
	        options.addArguments("use-fake-device-for-media-stream");
	    }

	    if (enableaudio || enablevideo) {
	        options.addArguments("use-fake-ui-for-media-stream");
	        el.logExecution("enabled audio/video");
	    }
	    if (headless) {
	        options.addArguments("--headless"); // Add this line for headless mode
	        el.logExecution("enabled headless");
	    }
//	    profileName = "Profile 6";
	    if (profileName != null && !profileName.equals("")) {
	        options.addArguments("user-data-dir=" + Utilities.getCompanyChromeProfile(companyId));
	        options.addArguments("profile-directory=" + profileName);
	        el.logExecution("enabled profile");
	        el.logExecution("profile path is " + Utilities.getCompanyChromeProfile(companyId));
	        el.logExecution("profile identifier is " + profileName);
	    }

	    // Set Chrome preferences to disable password manager and leak detection
	    HashMap<String, Object> chromePrefs = new HashMap<>();
	    chromePrefs.put("profile.default_content_settings.popups", 0);
		chromePrefs.put("download.default_directory", downloadDir);
	    chromePrefs.put("credentials_enable_service", false);
	    chromePrefs.put("profile.password_manager_enabled", false);
	    chromePrefs.put("profile.password_manager_leak_detection", false); // Important to suppress the warning
	    
//	    options.addArguments("--user-agent=LowesDigitalQA");
	    if(stealthMode) {
		    options.addArguments("--disable-blink-features=AutomationControlled");
	
			 // This removes the "enable-automation" switch that Chrome adds by default
			 options.setExperimentalOption("excludeSwitches", Arrays.asList("enable-automation"));
		
			 // This disables the Chrome Automation Extension
			 options.setExperimentalOption("useAutomationExtension", false);
	    }
	    options.setExperimentalOption("prefs", chromePrefs);

	    DesiredCapabilities capabilities = new DesiredCapabilities();
	    capabilities.setBrowserName("chrome");
	    capabilities.setCapability(ChromeOptions.CAPABILITY, options);
	    WebDriver driver1 = null;

	    try {
	        driver1 = new RemoteWebDriver(new URL(AppProperties.seleniumhub), capabilities);
	        
	        // Navigate to a blank page first to run our JavaScript
	        driver1.get("about:blank");
	        
	        JavascriptExecutor jsExecutor = (JavascriptExecutor) driver1;
	        
	        if(stealthMode) {
		        // Use JavaScript to modify properties that help avoid detection
		        jsExecutor = (JavascriptExecutor) driver1;
		        
		        // Use JavaScript to modify properties that help avoid detection
		        jsExecutor.executeScript(
		            "Object.defineProperty(navigator, 'webdriver', {get: () => undefined});" +
		            "Object.defineProperty(navigator, 'plugins', {get: () => [" +
		            "    {description: 'Portable Document Format',filename: 'internal-pdf-viewer',name: 'Chrome PDF Plugin'}," +
		            "    {description: '',filename: 'mhjfbmdgcfjbbpaeojofohoefgiehjai',name: 'Chrome PDF Viewer'}," +
		            "    {description: '',filename: 'internal-nacl-plugin',name: 'Native Client'}" +
		            "]});" +
		            "window.chrome = {runtime: {}};" +
		            // Additional stealth properties
		            "const originalQuery = window.navigator.permissions.query;" +
		            "window.navigator.permissions.query = (parameters) => (" +
		            "    parameters.name === 'notifications' ?" +
		            "        Promise.resolve({state: Notification.permission}) :" +
		            "        originalQuery(parameters)" +
		            ");"
		        );
		        
//		        driver1.get("https://bot.sannysoft.com");
	        }
	        
	        if (profileName != null && !profileName.equals("")) {
	        	 // Navigate to chrome://version to fetch profile details
	            driver1.get("chrome://version");

	            // Find the profile path element
	            String profileDirectory = driver1.findElement(By.xpath("//td[text()='Profile Path']/following-sibling::td")).getText();

	            // Confirm it's using Profile 6
	            if (profileDirectory.contains(profileName)) {
	                System.out.println("✅ Correct Profile is being used: " + profileDirectory);
	            } else {
	                System.out.println("❌ Wrong profile loaded. Currently using: " + profileDirectory);
	            }
	        }
	    } catch (Exception e) {
	        el.logExecution(e);
	        try {
	            driver1.quit();
	        } catch (Exception ex) {
	            // do nothing
	        }
	        e.printStackTrace();
	    }
	    BROWSER = "Chrome";
	    getDriverSession(driver1);
	    return driver1;
	}

	
	public void edgeGridSetup(boolean headless, String proxyurl, boolean enableaudio, 
			boolean enablevideo, int port, int companyId,
	        boolean stealthMode) {
		System.setProperty("webdriver.edge.driver", AppProperties.webdriveredgedriver); 	
		this.enableaudio = enableaudio;
		this.enablevideo = enablevideo;
		
		EdgeOptions options = new EdgeOptions();
		 if (enableaudio || enablevideo) {
			 options.addArguments("use-fake-ui-for-media-stream");
	     }
	     if (headless) {
	         options.addArguments("--headless"); // Add this line for headless mode
	     }
	     
	    if(AppProperties.apicapture.equalsIgnoreCase("true")) {
	    	options.addArguments("--remote-debugging-port=" + port);
		}
	        
		DesiredCapabilities capabilities = new DesiredCapabilities();
		capabilities.setBrowserName("edge");

		try {
//			driver = new EdgeDriver();
			driver = new RemoteWebDriver(new URL(AppProperties.seleniumhub), options);
		} catch (Exception e) {
			try {
				driver.quit();
			}catch (Exception ex){
				// do nothing
			}
			e.printStackTrace();
		}
		BROWSER = "Edge";
		getDriverSession(driver);
	}
	
	public void firefoxGridSetup(boolean headless, String proxyurl, boolean enableaudio, 
			boolean enablevideo, int port, int companyId,
	        boolean stealthMode) {
		System.setProperty("webdriver.chrome.driver", AppProperties.webdriverfirefoxdriver); 	
		this.enableaudio = enableaudio;
		this.enablevideo = enablevideo;
		
		FirefoxOptions options = new FirefoxOptions();
		options.addPreference("browser.download.folderList", 2);
        options.addPreference("browser.download.dir", downloadDir);
        
        if (enableaudio || enablevideo) {
            options.addPreference("media.navigator.permission.disabled", true);
        }
        if (headless) {
            options.addArguments("-headless"); // Add this line for headless mode
        }
        
        if(AppProperties.apicapture.equalsIgnoreCase("true")) {
        	options.addArguments("-start-debugger-server", String.valueOf(port));
		}

		try {
			driver = new RemoteWebDriver(new URL(AppProperties.seleniumhub), options);
		} catch (Exception e) {
			try {
				driver.quit();
			}catch (Exception ex){
				// do nothing
			}
			e.printStackTrace();
		}
		BROWSER = "Firefox";
		getDriverSession(driver);
	}
	
	public void safariGridSetup(boolean headless, String proxyurl, boolean enableaudio, 
			boolean enablevideo, int port, int companyId,
	        boolean stealthMode) {
//		System.setProperty("webdriver.chrome.driver", AppProperties.webdriversafaridriver); 	

		DesiredCapabilities capabilities = new DesiredCapabilities();
		capabilities.setBrowserName("safari");

		try {
			driver = new RemoteWebDriver(new URL(AppProperties.seleniumhub), capabilities);
		} catch (Exception e) {
			try {
				driver.quit();
			}catch (Exception ex){
				// do nothing
			}
			e.printStackTrace();
		}
		BROWSER = "Safari";
		getDriverSession(driver);
	}
	
	private static String getChromePreferences(String downloadFolder) {
		String res = "{\"download.default_directory\": \"" + downloadFolder.replace("\\", "\\\\") + "\"}";
        return res;
    }
	
	public void setExecutionLogger (ExecutionLogger els) {
		el = els;
	}
	
	public void setAnalysisLogger (ExecutionLogger els) {
		al = els;
	}
	
	public String getDriverSession(WebDriver wd) {
		if(sessionId == null) {
			sessionId = ((RemoteWebDriver) wd).getSessionId().toString();
		}
		
		return sessionId;
	}	

	public JSONObject test(JSONObject jsonObj, MySQlConn msc1, int companyId, String randomKey,
			JSONObject results, int runonceIndex, String envUrl, int testCaseResultsId,
			JSONObject testCaseDetails, boolean multiRun, int mrIndexInc, Tester tester,
			boolean synchScenario, int synchResultid, JSONObject synchObj, 
			boolean ssForAllElement, boolean analyseFailures, boolean baselineRun, boolean predecessor) {
	
		el.logExecution("Started Test");
		el.logExecution("Session id --> " + sessionId);
		if(analyseFailures) {
			el.logExecution("Running in analysis Mode");
		}
		msc = msc1;
		cid = companyId;
		JSONObject step = null;
		int stepnum = -1;
		String xpath = null;
		String keyword = null;
		String locationStrategy = null;
		String action = null;
		String testData = null;
		String varName = null; 
		String tc_execStatus = null;
		String status =  null;
		String ssfilename = null;
		String ssfilenameAF = null;
		int testStepId = -1; 
		double duration = 0.00; 
		String validateIn = null;
		long tc_startTime = System.currentTimeMillis();
		String tc_status = null;
		String ts_execStatus = null;
		boolean runonce = false;
		mrIndex = mrIndexInc;
		mrFieldIndex = 0;
		int totalIterations = (int)testCaseDetails.get("multirun");
		Set<Integer> completedPageSet = new HashSet<>();
		boolean isTCMultiRun = false;
		int screenX = -1;
        int screenY = -1;
        String saveType = null;
        List<String> foundelements = new ArrayList<String>();
        Devtools devtools = null;
		
		if(!multiRun) {
			indexUsed = false;
			xlsColCount = -1;
			xlsRowCount = -1;
		}
		try {  
			
			el.logExecution("Inside Try");
			tc_status = "STARTED";
			tc_execStatus = "PASS";
			JSONObject steps = (JSONObject) jsonObj.get("data");
			step = (JSONObject) steps.get(2);
			JSONObject testCase = (JSONObject) jsonObj.get("testcase");
			saveType = (String) testCase.get("SaveType");

			int testCaseId = -1;
			try {
				testCaseId = (int) step.get("Test_Case_Id");
			} catch (Exception e) {
				el.logExecution("Looks like this test case has no steps " );
	        	msc.writeTestStepStatusToDB(testCaseResultsId, 1, (String) step.get("Page_Name"),
						(String) step.get("Page_Description"), "", keyword, action, (String) step.get("Flow"), 
						"", "", "FAIL", ssfilename, "", (int) step.get("idtest_step"), "", duration, validateIn,
						runIssues, errorCode, "FAIL", "", null, foundelements,
						devtools);
	        	tc_status = "FAIL";
				tc_execStatus = "FAIL";
				return results;
			}
			
			if(testCaseResultsId == -1) {
				if(testCaseDetails == null) {
					testCaseDetails = msc.getTestCaseDetailsFromId(testCaseId);
				}				
				testCaseResultsId = msc.writeTestCaseStatusToDB(testCaseId, tc_status,
					"SYS", 0.0, BROWSER, (String)testCaseDetails.get("Test_Case"), companyId);
			}
			
			el.logExecution("Starting execution of Test Case : " + (String)testCaseDetails.get("Test_Case"));
			int tcThreshHold = (int)testCaseDetails.get("testcasethreshold");
			
			if(tsr > -1 && !multiRun && !inline) {
				el.logExecution("Linked testCaseResultsId("+ testCaseResultsId +") to Test Suite Result(" + tsr + ")");
				msc.linkTSRtoTCR(tsr, testCaseResultsId);
			}
			
			if(totalIterations > 0) {
				isTCMultiRun = true;
				
				if(mrIndex == (int)testCaseDetails.get("multirun")) {
					multiRun = false;
				} else {
					multiRun = true;
				}
			}
			
			try {
				JSONObject sessionInfo = gm.getSessionInfo(sessionId);
				String nodeUri = (String)sessionInfo.get("nodeUri");
				msc.updateSessionforTc(testCaseResultsId,sessionId,nodeUri);
				el.logExecution("Node running the Test is " + nodeUri );
				int test_case_id = (int)testCaseDetails.get("idtest_case");
				
				if(AppProperties.apicapture.equalsIgnoreCase("true")) {
					devtools = new Devtools();
					devtools.tcid = test_case_id;
					devtools.tcrid = testCaseResultsId;
					devtools.captureApi(nodeUri,el);
				}
			} catch (Exception e) {
				el.logExecution(e);
				el.logExecution("Driver is NULL - looks like issue with execution capacity");
				msc.updateTestCaseStatusToDB(testCaseResultsId, "FAIL", duration, "FAIL", companyId);

				results.put("status", "FAIL");
				results.put("duration", duration);
				if(!runonce) {
					runonce = !indexUsed;
					results.put("runonce", runonce);
				}
				el.logExecution("Returning from SG.Test " );
				e.printStackTrace();
				return results;
			}
			
			el.logExecution("Before Resolution");
			int width = -1;
			int height = -1;
			JSONObject resolution = (JSONObject) jsonObj.get("resolution");
			if(resolution != null) {
				width = (int)resolution.get("width");
				height = (int)resolution.get("height");
			}
			
			if(width <= 0 || height <= 0) {
				el.logExecution("Resolution not captured - going back to default 1920 by 1080");
				width = AppProperties.defaultDisplayWidth;
				height = AppProperties.defaultDisplayWidth;
			} 
			// needed to adjust as the dimensions are not the same as when they were recorded
			
			results.put("excuteddate", Utilities.getNow());
			results.put("browser", BROWSER);
			results.put("envurl", "Default");
			results.put("width", width);
			results.put("height", height);
			runonce = (boolean)results.get("runonce");
			boolean inRetrial = false;
			int retrialStep = -1;
			
			HashMap windowTabs = new HashMap();			
			HashMap tabs = new HashMap();
			HashMap windowIds = new HashMap();
	
			el.logExecution("Before Tab");
			int tabid = (int)step.get("tabid");			
			tabs.put(tabid, tabs.size());
			int windowid = (int)step.get("windowid");
			windowDrivers.put(windowid, driver);
			windowTabs.put(windowid, tabs);
			
			int i =1;
			String url = (String) step.get("TestData");
			if(url != null && url.startsWith(AppProperties.delimiter) && 
					url.endsWith(AppProperties.delimiter)) {
				String temp = varMap.get(url);
				if(temp != null) {
					url = temp;
				}
			}
			if(envUrl != null && !envUrl.equals("") && !envUrl.equals("undefined")) {
				url = envUrl;	
				el.logExecution("Using Environment URL " + envUrl );
			} else if (url == null || url.equals("") || !url.startsWith("http")) {
				url = (String) step.get("pageurl");
				i =0;
			}
					
			results.put("envurl", url);
			action = (String) step.get("Action");
			keyword = (String) step.get("Keyword");
			locationStrategy = (String) step.get("strategy");
			tc_startTime = System.currentTimeMillis();
			long startTime = System.currentTimeMillis();
			el.logExecution("Before Resizing Window");
			if(width >= Integer.parseInt(AppProperties.minWidth) && height >= Integer.parseInt(AppProperties.minHeight)) {
				driver.manage().window().maximize();
			} else {
				driver.manage().window().setSize(new Dimension(width,height));
			}
			Point browserPosition = driver.manage().window().getPosition();
	        Dimension browserSize = driver.manage().window().getSize();
	        el.logExecution("Width of the browser window: " + browserSize.getWidth());
	        el.logExecution("Height of the browser window: " + browserSize.getHeight());
	        el.logExecution("Launch URL");
	        try {
	        	if(!(boolean)testCaseDetails.get("continuetest") && !inline) {
	        		el.logExecution("Launching URL : " + url);
	        		driver.get(url);       		
	        	} else {
	        		if((boolean)testCaseDetails.get("continuetest")) {
	        			el.logExecution("Test case continued from previous test"   );
	        		} else if(inline){
	        			el.logExecution("Test case continued from previous test as this is an inline test case"   );
	        		}
	        	}
	        } catch (Exception e) {
	        	el.logExecution("Error in launching URL " + url );
	        	msc.writeTestStepStatusToDB(testCaseResultsId, 1, (String) step.get("Page_Name"),
						(String) step.get("Page_Description"), "", keyword, action, (String) step.get("Flow"), 
						url, "", "FAIL", ssfilename, "", (int) step.get("idtest_step"), "", duration, validateIn,
						runIssues, errorCode, "FAIL", "", null, 
						foundelements, devtools);
	        	tc_status = "FAIL";
				tc_execStatus = "FAIL";
	        }
			long endTime = System.currentTimeMillis();
			duration = (endTime - startTime)/1000.0;
			
			String flow = (String) step.get("Flow");
			validateIn = (String) step.get("valDevice");
			String iframexpath = (String)step.get("iframexpath");
			
			
			results.put("testcase", testCaseDetails.get("Test_Case"));
			
			String licenseKey = msc.getLicenceToken(companyId);
			
			String fileurl = AppProperties.fileurl + "?action=executionlog&token="+licenseKey+"&companyid="+companyId+"&tcid="+testCaseId+"&tcrid="+testCaseResultsId;
			results.put("logfile", fileurl);
			results.put("testCaseResultsId", testCaseResultsId);
			
			el.logExecution("Log File URL " + fileurl);
			el.logExecution("Browser " + BROWSER);
			el.logExecution("Resolution - width " + width + " by height " + height);
			el.logExecution("Test Case Id - " + testCaseId );
			el.logExecution("Test Case Name - " + testCaseDetails.get("Test_Case") );
			el.logExecution("Starting Execution " );
			el.logExecution(step.toJSONString());
			el.logExecution("Step Start Time " + startTime + "Step End Time " + endTime +"URL - " + url);
			
			ssfilenameAF = "";
			if(multiRun) {
				ssfilename = Utilities.getSSFileName(testCaseId, (int) step.get("idtest_step"), testCaseResultsId, 0, 
						companyId, "MR" + mrIndex);
			} else {
				ssfilename = Utilities.getSSFileName(testCaseId, (int) step.get("idtest_step"), testCaseResultsId, 0, 
					companyId, null);
			}
			takeSS(ssfilename, testCaseId,  testCaseResultsId, companyId,analyseFailures);
	
			el.logExecution("ScreenShot " + ssfilename);
			msc.writeTestStepStatusToDB(testCaseResultsId, 1, (String) step.get("Page_Name"),
					(String) step.get("Page_Description"), "", keyword, action, (String) step.get("Flow"), 
					url, "", "PASS", ssfilename, "", (int) step.get("idtest_step"), "", duration, validateIn,
					runIssues, errorCode, "PASS", "", null, 
					foundelements, devtools);
			el.logExecution("Step Status PASS");
			long prevRowEventTime = 0;
			
			int attempt = 0;
			WebElement we = null;
			WebElement dragStart = null;
			String foundby = null;
			String temporaryFoundBy = null;
			el.logExecution("Starting Steps");
			while (i < steps.size()) {
				
				long ts_startTime = System.currentTimeMillis();
				el.logExecution("xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx ");
				el.logExecution("Starting Next Step ");
				
				String Xpath = "";
				int pageNumber = -1;
				boolean isTestDataProcessed = false;
				
				ExecutionUtility executionUtility = new ExecutionUtility();
				
				if(attempt == 0) {
					el.logExecution("Resetting Strategy to null ");
					foundby = null;
					temporaryFoundBy = null;
					foundelements.clear();
				} else {
					el.logExecution("attempt count : " + attempt + "- found by : " + foundby);
				}
				
				tc_status = stopExecution(tester, testCaseResultsId, tc_status);
				if(tc_status != null && tc_status.equals("STOPP")) {
					break;
				}
				
				attempt = attempt + 1;
				step = (JSONObject) steps.get(i + 2);
				int test_step_id = (int) step.get("idtest_step");
				Utilities.executionMsgCache.put("testStepId", test_step_id);
				stepnum = (int) step.get("Step_Number");
				Utilities.executionMsgCache.put("StepNumber", stepnum);
				if(AppProperties.apicapture.equalsIgnoreCase("true")) {
					try{
						devtools.tsid = test_step_id;
						devtools.stepnum = stepnum;
					} catch(Exception e) {
						
					}
				}
				
				JSONObject debugStep = (JSONObject)Utilities.debugCache.get(testCaseResultsId);
				if(debugMode && debugStep != null) {					
                	String option = (String)debugStep.get("option");
                	int debugtsid = (int)debugStep.get("tsid");
                	long degubWaitingElapsedTime;
                	if(option.equalsIgnoreCase("runtill")) {
                		if(test_step_id == debugtsid) {
                			Utilities.debugMsgCache.put(testCaseResultsId, "Run till Step option chosen for debugging - Reached Step " + debugtsid + " waiting for further user action");
                			el.logExecution("Run till Step option chosen for debugging - Reached Step " + debugtsid + " waiting for further user action");
                			Utilities.debugCache.remove(testCaseResultsId);
                			do {
                				debugStep = (JSONObject)Utilities.debugCache.get(testCaseResultsId);
                				long now = System.currentTimeMillis();
                				degubWaitingElapsedTime = now - startTime;
                			} while(debugStep == null && degubWaitingElapsedTime < AppProperties.seleniumwaittime);
                			continue;
                		}                		
                	} else if(option.equalsIgnoreCase("gotostep")) {
                		if(test_step_id != debugtsid) {
                			i = i+1;
                			Utilities.debugMsgCache.put(testCaseResultsId, "Go to Step option chosen for debugging - Skipping Step " + debugtsid );
                			el.logExecution("Go to Step option chosen for debugging - Skipping Step " + debugtsid );
                			continue;
                		} else if(test_step_id == debugtsid) {
                			Utilities.debugMsgCache.put(testCaseResultsId, "Go to Step option chosen for debugging - Reached Step " + debugtsid + " waiting for further user action");
                			el.logExecution("Go to Step option chosen for debugging - Reached Step " + debugtsid + " waiting for further user action");
                			Utilities.debugCache.remove(testCaseResultsId);
                			do {
                				debugStep = (JSONObject)Utilities.debugCache.get(testCaseResultsId);
                				long now = System.currentTimeMillis();
                				degubWaitingElapsedTime = now - startTime;
                			} while(debugStep == null && degubWaitingElapsedTime < AppProperties.seleniumwaittime);
                			continue;
                		}
                	} else if(option.equalsIgnoreCase("resumeafter")) {
                		if(test_step_id != debugtsid) {
                			i = i+1;
                			Utilities.debugMsgCache.put(testCaseResultsId, "Resume After Step option chosen for debugging - Skipping Step " + debugtsid );
                			el.logExecution("Resume After Step option chosen for debugging - Skipping Step " + debugtsid );
                			continue;
                		} else if(test_step_id == debugtsid) {
                			Utilities.debugMsgCache.put(testCaseResultsId, "Resume After Step option chosen for debugging - Reached Step " + debugtsid + " executing rest of the test case");
                			el.logExecution("Resume After Step option chosen for debugging - Reached Step " + debugtsid + " executing rest of the test case");
                			Utilities.debugCache.remove(testCaseResultsId);
                		}
                	}
				}
				String condLogic = (String)step.get("condLogic");	
				String condexp = (String)step.get("condexp");
				if(condLogic != null && !condLogic.equals("")) {
					if (condLogic.equalsIgnoreCase("If-true")) {
						boolean expValue = true;
						try {
							if(condexp != null && !condexp.equals("")) {
								Utilities.executionMsgCache.put(testCaseResultsId, "Step number: " + stepnum + " - Evaluating the conditional expression");
								expValue = ExpressionEvaluator.evaluate(testCaseResultsId, condexp, varMap,el);
							}
						} catch (Exception eee) {
							
						}
						if(expValue != true) {
							Utilities.executionMsgCache.put(testCaseResultsId, "Step number: " + stepnum + " - Conditional Logic Failed");
							el.logExecution("Expression evaluated to: " + expValue);
							el.logExecution(" Skipping Executing this step");
							i = i+1;
							continue;
						}
					} else if (condLogic.equalsIgnoreCase("If-false")) {
						boolean expValue = false;
						try {
							if(condexp != null && !condexp.equals("")) {
								Utilities.executionMsgCache.put(testCaseResultsId, "Step number: " + stepnum + " - Evaluating the conditional expression");
								expValue = ExpressionEvaluator.evaluate(testCaseResultsId, condexp, varMap, el);
							}
						} catch (Exception eee) {
							
						}
						if(expValue != false) {
							Utilities.executionMsgCache.put(testCaseResultsId, "Step number: " + stepnum + " - Conditional Logic Failed");
							el.logExecution("Expression evaluated to: " + expValue);
							el.logExecution(" Skipping Executing this step");
							i = i+1;
							continue;
						}
					}
				}
				String foundOuterhtml = "";
				flow = (String) step.get("Flow");
				if(flow != null && flow.equalsIgnoreCase("Disable")) {
					Utilities.executionMsgCache.put(testCaseResultsId, "Step number: " + stepnum + " - Flow is Disable - so skipping this step");
					el.logExecution("Step " + stepnum + " : Flow is Disable - so skipping this step ");
					i = i + 1;
					attempt = 0;
					continue;
				}
				
				if(synchScenario) {
					boolean canContinue = readyForNext(step, synchResultid,  synchObj, 
							msc, el, steps, tester, testCaseResultsId, tc_status);
					if(!canContinue) {
						el.logExecution("Error in launching URL " + url );
			        	msc.writeTestStepStatusToDB(testCaseResultsId, 1, (String) step.get("Page_Name"),
								(String) step.get("Page_Description"), "", keyword, action, (String) step.get("Flow"), 
								url, "", "FAIL", ssfilename, "", (int) step.get("idtest_step"), "", duration, validateIn,
								runIssues, errorCode, "FAIL", foundOuterhtml, 
								foundby, foundelements, devtools);
			        	tc_status = "FAIL";
						tc_execStatus = "FAIL";
						break;
					}
				}
				
				int tsThreshHold = (int)step.get("teststepthreshold");
				int threshold = getThreshold(tcThreshHold, tsThreshHold);
				
				tabid = (int)step.get("tabid");
				windowid = (int)step.get("windowid");
				if(windowTabs.get(windowid) == null) {
					HashMap newTab = new HashMap();
					newTab.put(tabid, newTab.size());
					tabs = newTab;
					windowTabs.put(windowid, newTab);
				} else {
					tabs = (HashMap)windowTabs.get(windowid);
				}
				
				
				if(testCaseId == tillTC && tillStepNum == stepnum) {
					try {
						Thread.sleep(AppProperties.seleniumwaittime);
					} catch (Exception et) {
						el.logExecution("thread8 sleeping for " + AppProperties.seleniumwaittime + " interupted");
					}
					break;
				}
				xpath = (String) step.get("Object_Xpath");
				Xpath = xpath;
				
				action = (String) step.get("Action");
				keyword = (String) step.get("Keyword");
				locationStrategy = (String) step.get("strategy");
				testData = (String) step.get("TestData");
				varName = (String) step.get("VarName");
				String gendateoffset = (String)step.get("gendateoffset");
				int createsAlert = (int)step.get("createsAlert");
				Alert alert = null;
				try {
					ProcessTestData ptd = new ProcessTestData();
					Utilities.executionMsgCache.put(testCaseResultsId, "Step number: " + stepnum + " - Processsing Test Data");
					testData = ptd.processTestData(testData, step, varName, testCaseId,
						companyId, runonceIndex, msc, randomKey, gendateoffset, this,el);
					xpath = processXPath(xpath);
				} catch (Exception e) {
					el.logExecution("Taking screen shot Due to error in processing test Data");
					if(multiRun) {
						ssfilenameAF = Utilities.getSSFileName(testCaseId, testStepId, testCaseResultsId, stepnum, 
								companyId, "MR" + mrIndex);
					} else {
						ssfilenameAF = Utilities.getSSFileName(testCaseId, testStepId, testCaseResultsId, stepnum, 
							companyId, null);
					}
					String ssloc = getSSFullLoc(ssfilenameAF, testCaseId,testCaseResultsId , 
							companyId);
					Utilities.executionMsgCache.put(testCaseResultsId, "Step number: " + stepnum + " - Taking screen shot After error in processing test data");
					changeBGtakess(we, ssloc, status,analyseFailures,baselineRun,
							el, locationStrategy, screenX, screenY,createsAlert, keyword);
					el.logExecution("Finished Taking screen shot After error in processing test data");
					el.logExecution("Step  " + stepnum + " test data errror " + xpath + " " );
					throw e;
				}
				String recordedData = (String) step.get("TestData");
				if(testData != null && recordedData != null && !testData.equals(recordedData)) {
					isTestDataProcessed = true;
				}
				
				if(eof) {
					runonce = true;
					results.put("runonce", runonce);
					return results;
				}
				validateIn = (String) step.get("ValDevice");
				iframexpath = (String)step.get("iframexpath");
				if(iframexpath == null) {
					iframexpath = "";
				}
				testStepId = (int) step.get("idtest_step");
				String outerhtml = (String)step.get("outerhtml");
				String wait = (String)step.get("wait");
				int waitTime = (int)step.get("waittime");
				flow = (String) step.get("Flow");
				processVariables(varName, testData, xpath, i, step);
				String manuallyAdded = (String)step.get("manuallyAdded");
				String inputType = (String)step.get("type");
				
				status = "STARTED";
				ts_execStatus = "PASS";
				endTime = 0;
				
				if(step.get("type") != null && ((String)step.get("type")).equalsIgnoreCase("password")) {
					step.put("TestData", "********");
				} 
				el.logExecution(step.toJSONString());
				if(step.get("type") != null && ((String)step.get("type")).equalsIgnoreCase("password")) {
					step.put("TestData", testData);
				}
				
				long curRowEventTime = (long) step.get("actualtime");
				long timeToWait = 1000;
				double elapsedTime = 500.0;
				if (prevRowEventTime > 0.0) {
					timeToWait = curRowEventTime - prevRowEventTime;
				}
				
				runIssues = "";
				errorCode = "";
				nonMatchingAttr = "";
	            
				if(((wait != null && wait.equals("Before")) ||
						wait == null && waitTime > 0) && attempt == 1) {
					try {
						el.logExecution("THREAD Sleep BEFORE STEP for (milliseconds) ");
						el.logExecution("Explicit wait time set : " + waitTime * 1000);
						el.logExecution("Time difference between steps while automating : " + timeToWait);
						if(timeToWait > AppProperties.defaultwaittime) {
							el.logExecution("Time difference between steps while automating is greater than default wait time - so resetting timeToWait to 3 seconds");
							timeToWait = 3000;
						}
						if((waitTime * 1000) > 0) {
							el.logExecution("waiting for (milliseconds) - using explicit wait time set -" + waitTime * 1000);
							Utilities.executionMsgCache.put(testCaseResultsId, "Step number: " + stepnum + " - Waiting for " + waitTime + " seconds before the step");
							Thread.sleep(waitTime * 1000);
						} else {
//							el.logExecution("waiting for (milliseconds) - using default time during automating : " + timeToWait);
//							Thread.sleep(timeToWait);
						}
						startTime = System.currentTimeMillis();
					}catch (Exception e) {
						el.logExecution("thread9 sleeping for waittime * 1000 interupted");
					}
				}
				if(ssForAllElement) {
					List<Integer> failedElements = new ArrayList<>();
					pageNumber = (int)step.get("pagenumber");
					testStepId = (int) step.get("idtest_step");
					JSONArray json = new JSONArray();
					JSONArray jsonProd = new JSONArray();
					if(completedPageSet.add(pageNumber)) {
						int prodId = msc.getProdIdFromTestStepId(testStepId);
						int pageId = msc.getPageid(pageNumber, testCaseId, prodId);
						Utilities.createProductPath(companyId, prodId);
						json = msc.getPageElement(pageId, testCaseId, prodId);  // ALL Element that are covered
						jsonProd = msc.getPageElementOnProd(pageId, testCaseId, prodId);
			            
			            for (Object obj : jsonProd) {
			                JSONObject element = (JSONObject) obj;
			                pageId = (int)element.get("pageId");
			                int elementId = (int)element.get("elementId");
			                String elementName = (String)element.get("elementName");
			                String elementXpath = (String)element.get("xpath");
			                WebElement ele = null;
			                try {
			                	ele = driver.findElement(By.xpath(elementXpath));
			            		String originalBgColor = "";
		            			((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block: 'center'});", ele);
		            			originalBgColor = (String)((JavascriptExecutor) driver).executeScript("var originalBackgroundColor = arguments[0].style.backgroundColor; arguments[0].style.backgroundColor = 'orange'; return originalBackgroundColor;", ele);
			                } catch(Exception e) {
			                	failedElements.add(elementId);
			                }
						}
			            
			            for (Object obj : json) {
			                JSONObject element = (JSONObject) obj;
			                pageId = (int)element.get("pageId");
			                int elementId = (int)element.get("elementId");
			                String elementName = (String)element.get("elementName");
			                String elementXpath = (String)element.get("xpath");
			                WebElement ele = null;
			                try {
			                	ele = driver.findElement(By.xpath(elementXpath));
			            		String originalBgColor = "";
		            			((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block: 'center'});", ele);
		            			originalBgColor = (String)((JavascriptExecutor) driver).executeScript("var originalBackgroundColor = arguments[0].style.backgroundColor; arguments[0].style.backgroundColor = 'red'; return originalBackgroundColor;", ele);
			                } catch(Exception e) {
			                	failedElements.add(elementId);
			                }
						}
			            
						takeCoverageSS(companyId, prodId, pageId);
					}
					
				}
				
				if (!((String)step.get("testdata_source")).equals("SameRowElement")) {
					we = null;
				}
				String weType = null;
				boolean found = false;
				if(keyword.equals(AppProperties.URLKEYWORD)) {
					try {
						driver.get(testData);
					} catch (Exception e) {
						if(e instanceof org.openqa.selenium.InvalidArgumentException) {
							errorCode = "E0017";
							runIssues = "Invalid Url";
						}
					}
					driver.get(testData);
					el.logExecution("Called URL " + testData);
				} else {

					try {
						
						if(prevWindowId != -1 && prevWindowId != windowid && (manuallyAdded == null || 
								(manuallyAdded != null && (!manuallyAdded.equals("merged") && 
										!manuallyAdded.equals("true"))))) {
							el.logExecution("Previous Window Id: " + prevWindowId + " and Current Window Id: " + windowid);
							if(keyword.equalsIgnoreCase(AppProperties.WINKEYWORD) && 
									action.equalsIgnoreCase("Create")) {
								el.logExecution("Entered Opening New Browser Window Section ");
								
								Set<String> windowHandlesBefore = driver.getWindowHandles();
								el.logExecution("Currently runnign windows before create are - " + windowHandlesBefore.toString());
								
								el.logExecution("Opening New Browser Window  - with values ");
								el.logExecution("headless " + headless);
								el.logExecution("proxyUrl " + proxyUrl);
								el.logExecution("enableaudio " + enableaudio);
								el.logExecution("Profile " + varName);
								
								WebDriver newDriver = chromeGridSetup(headless, proxyUrl, enableaudio, enableaudio, varName
										,port,companyId, stealthMode);
								 
								windowDrivers.put(windowid, newDriver);
								el.logExecution("New Browser Window Creation was successful - windowhandle : ");
								driver = newDriver;
								driver.get(testData);
								prevWindowId = windowid;
								prevTabId = tabid;
								i = i + 1;
								continue;								
							} else {
								if(windowid == -1) {
									el.logExecution("Switching to window with windowid : " + windowid);
									el.logExecution("There is an error with the window setup - window id cannotbe -1  " );
									status = "FAIL";
									break;
								} else {
									el.logExecution("Switching to window  " + windowid);
									//driver = (WebDriver)windows.get(windowid);
									if(windowIds.get(windowid) == null) {
									String mainWindowHandle = driver.getWindowHandle();
									Set<String> windowHandles = driver.getWindowHandles();
								        
								        // Switch to the new window
								        for (String handle : windowHandles) {
								            if (!handle.equals(mainWindowHandle)) {
								                driver.switchTo().window(handle);
								                break;  // Stop after switching to the new window
								            }
								        }
									} else {
										String handle = (String) windowIds.get(windowid);
										driver.switchTo().window(handle);
									}
									el.logExecution("Switching to window was successful " + windowid);
									prevWindowId = windowid;
								}
							}
						}
						
						String windowHandle = driver.getWindowHandle();
						if(windowIds.get(windowid) == null) {
							windowIds.put(windowid, windowHandle);
							windowDrivers.put(windowid, driver);
						}
						
						if(stepnum > 2 && prevWindowId == windowid && prevTabId != tabid && 
								(manuallyAdded == null || (manuallyAdded != null && !manuallyAdded.equals("merged")))) {
							el.logExecution("Previous Tab Id: " + prevTabId + " and Current Tab Id: " + tabid);
							if(keyword.equalsIgnoreCase(AppProperties.TABKEYWORD)) {
								el.logExecution("Creating New Tab ");
								((JavascriptExecutor) driver).executeScript("window.open();");
								driver.manage().timeouts().implicitlyWait(5, TimeUnit.SECONDS);
								el.logExecution("Completed creating New Tab ");
							}
							
							el.logExecution("Switching to Different Tab ");
							int tabIndex = tabs.size();
							if(tabs.get(tabid) == null) {
								tabs.put(tabid, tabIndex);
							} else {
								tabIndex = (int)tabs.get(tabid);
							}
							
							
							// Get the window handles
					        String[] windowHandles = driver.getWindowHandles().toArray(new String[0]);
					        if(tabIndex >= windowHandles.length) {
					        	throw new ArrayIndexOutOfBoundsException("Tab Index is out of Bound");
					        }
					        String tab2Handle = windowHandles[tabIndex];
					        driver.switchTo().window(tab2Handle);
					        Utilities.executionMsgCache.put(testCaseResultsId, "Step number: " + stepnum + " - Switching to Different Tab");
					        el.logExecution("Switched to Different Tab ");
					        prevTabId = tabid;
					        
					        if(keyword.equals(AppProperties.TABKEYWORD)) {
					        	driver.get(testData);
					        	prevTabId = tabid;
					        	i = i + 1;
					        	continue;
					        }
						}
						
						if(!prevRowiframexpath.equals(iframexpath)) {
							
							Utilities.executionMsgCache.put(testCaseResultsId, "Step number: " + stepnum + " - Locating Iframe");
							List<WebElement> iframes = driver.findElements(By.tagName("iframe"));
							
							int j=1;
							for(WebElement iframe : iframes) {
								el.logExecution("In iframe" + j);
								
								try {
									try {
										String iframeOuterHTML = iframe.getAttribute("outerHTML");
										el.logExecution("Iframe Outer HTML: " + iframeOuterHTML);
									} catch(Exception e) {
										
									}
									
									try {
										driver.switchTo().frame(iframe);
									} catch(Exception e) {
										
									}
									we = driver.findElement(By.xpath(xpath));
									
									String recordedOuterHTML = (String) step.get("outerhtml");
									el.logExecution("Recorded Outer HTML: " + recordedOuterHTML);
									
									String actualOuterHTML = we.getAttribute("outerHTML");
									el.logExecution("Actual Outer HTML: " + actualOuterHTML);
									
								} catch(Exception e) {
									
									if(e instanceof NoSuchElementException) {
										el.logExecution("Did not find Element with xpath " + xpath);
									}
								}
								el.logExecution("Switching to Default Frame ");
								driver.switchTo().defaultContent();
								j++;
							}
							
							if(iframexpath != null && iframexpath.equals("")) {
								el.logExecution("Switching to Default Frame ");
								driver.switchTo().defaultContent();
								el.logExecution("Switched to Default Frame Success");
								prevRowiframexpath = iframexpath;
							} else {
								do {
									el.logExecution("Step  " + stepnum + " Looking for iframe xpath " + iframexpath);
									try {
										if(iframexpath != null && !iframexpath.equals("") &&
												iframexpath.startsWith("iframeindex=")) {
											String indexStr = iframexpath.substring(12,iframexpath.length());
											int index = Integer.valueOf(indexStr);
											driver.switchTo().frame(index);
										} else {
											we = driver.findElement(By.xpath(iframexpath));
											driver.switchTo().frame(we);
										}										
										
										WebElement currentIframe = driver.switchTo().activeElement();
								        el.logExecution("Currently selected iframe with id: " + currentIframe.getAttribute("id"));
								        el.logExecution("Currently selected iframe with id: " + currentIframe.getAttribute("src"));
										found = true;
										el.logExecution("Step  " + stepnum + " Switched Successfully to iframe xpath " + iframexpath);
									} catch (org.openqa.selenium.NoSuchElementException e) {
										if(!iframexpath.equals("") && !prevRowiframexpath.equals("")) {
											el.logExecution("Hopping from 1 iframe to another ");
											el.logExecution("Switching to Default Frame ");
											driver.switchTo().defaultContent();
											el.logExecution("Switched to Default Frame Success");
											el.logExecution("Switching to Frame " + iframexpath);
											we = driver.findElement(By.xpath(iframexpath));
											driver.switchTo().frame(we);
											found = true;
											el.logExecution("Step  " + stepnum + " Switched Successfully to iframe xpath " + iframexpath);
										} 
											
//										if (elapsedTime <= timeToWait && elapsedTime < 30000) {
										if (elapsedTime < AppProperties.defaultwaittime) {
											try {
												Thread.sleep(500);
											} catch (Exception et) {
												el.logExecution("thread10 sleeping for 500 interupted");
											}
											elapsedTime = elapsedTime + 500.0;
											el.logExecution("Step  " + stepnum + " Looking for iframe xpath " + iframexpath
													+ " total wait time " + elapsedTime);
//											runIssues = runIssues + "Step  " + stepnum + " Looking for iframe xpath " + iframexpath
//													+ " total wait time " + elapsedTime;
										} else {
											errorCode = "E0013";
											runIssues = "Unable to find the Iframe";
//											runIssues = runIssues + "Step  " + stepnum + " DID NOT FIND ELEMENT " + iframexpath
//													+ " total wait time " + elapsedTime;
											el.logExecution("Step  " + stepnum + " DID NOT FIND IFRAME ELEMENT " + iframexpath
													+ " total wait time " + elapsedTime);
											throw e;
										}
									}
									if(!found) {
										Utilities.executionMsgCache.put(testCaseResultsId, "Step number: " + stepnum + " - Locating Iframe - will try for upto 15s - elapsed time  is : "+ elapsedTime);
									}
								} while (!found);
							}
						}
						
						if(found) {
							prevRowiframexpath = iframexpath;
						}
						found = false;
						
						do {
							el.logExecution("Step  " + stepnum + " Looking for xpath " + xpath + " " );
							try {
								if(saveType != null && saveType.equalsIgnoreCase("AI_GEN") && action.equalsIgnoreCase("enter")) {
									we = findLocatorForAiGenerated(driver, xpath);
									found = true;
									foundby = "xpath";
								}
								else if((keyword != null && (keyword.equalsIgnoreCase("Api") || keyword.equalsIgnoreCase("Inline") || keyword.equalsIgnoreCase("Clipboard") || 
										keyword.equalsIgnoreCase("if")) ) || (action != null && action.equalsIgnoreCase("Test"))) {
									el.logExecution("Step  " + stepnum +  " Keyword is " + keyword );
									found = true;
									foundby = "by-na";
								} else if(keyword != null && keyword.equalsIgnoreCase("Alert")) {
									el.logExecution("Step  " + stepnum + " looking for alert " );
									alert = driver.switchTo().alert();
									el.logExecution("Step  " + stepnum + " Alert found " );
									foundby = "by-na";
								} else if(locationStrategy != null && locationStrategy.equalsIgnoreCase("XY")) {
									el.logExecution("Step  " + stepnum + " using absolute position as strategy " );
									int recordedX = (int) step.get("xpos");
							        int recordedY = (int) step.get("ypos");
							        el.logExecution("X coordinates as recorded are = " + recordedX);
							        el.logExecution("Y coordinates as recorded are = " + recordedY);
							        
							        System.setProperty("java.awt.headless", "false");
							        Robot robot = new Robot();
							        
							     // Calculate the absolute coordinates on the screen
//						            screenX = driver.manage().window().getPosition().getX() + recordedX;
//						            screenY = driver.manage().window().getPosition().getY() + recordedY;
							        screenX = recordedX;
							        screenY = recordedY;
						            
						            el.logExecution("X coordinates as calculated are = " + screenX);
							        el.logExecution("Y coordinates as calculated are = " + screenY);
							        
							        String headlessValue = System.getProperty("java.awt.headless", "false");
							        System.out.println("Is headless: " + headlessValue);
							        el.logExecution("java.awt.headless value is : " + headlessValue);
							           
						            Actions actions = new Actions(driver);
						            if(keyword.equalsIgnoreCase("Mouse") && action.equalsIgnoreCase("Hover")) {
						            	actions.moveToLocation(screenX, screenY).perform();
						            	el.logExecution("Performed Mouse Hover using XY strategy");
						            } else {
						            	// Move to the coordinates and perform a click
						            	actions.moveToLocation(screenX, screenY).click().build().perform();
						            	el.logExecution("performed a click using absolute strategy" );
						            }
									try {
										Thread.sleep(2000);
									} catch (Exception e) {
										
									}

							        // Optionally wait to see the highlight (not mandatory)
							        try {
							            Thread.sleep(2000); // 2 seconds
							        } catch (InterruptedException e) {
							            e.printStackTrace();
							        }

							        foundby = "by-xy";
								} else if(keyword.equals("Check Box") && ((action.equals("ByText") || action.equals("ById")))) {
									el.logExecution("Step  " + stepnum + " check box find by " + action + " " + testData);
									String id = testData.substring(0,testData.indexOf(AppProperties.delimiter));
									String searchText = testData.substring(
											testData.indexOf(AppProperties.delimiter) + AppProperties.delimiter.length(),
											testData.length());
							        WebElement textElement = driver.findElement(By.xpath("//*[text()= '" + searchText + "']"));						        
							        we = findCheckbox(textElement);
							        action = "click";
							        foundby = "by-text";
								} else if (keyword.equals("File") || 
										(validateIn != null && validateIn.equals("InDownloadedFile")) ||
										(testData != null && !testData.equals(AppProperties.validatedelimiter + AppProperties.DYNAMICTEXT + AppProperties.validatedelimiter)  && 
												validateIn != null && validateIn.equals("InFile")) ||
										keyword.equals("Send") && action.equalsIgnoreCase("Email")) {
									found = true;
									foundby = "by-na";
									break;
								} else if (((String)step.get("testdata_source")).equals("SameRow")) {
									WebElement textElement = null;
									if(saveType.equalsIgnoreCase("AI_GEN")) {
										textElement = driver.findElement(By.xpath("//*[translate(text(), 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz') = '" + testData.toLowerCase() + "']"));
										el.logExecution("Element found by searching for " + "//*[translate(text(), 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz') = '" + testData.toLowerCase() + "']");
									} else {
										textElement = driver.findElement(By.xpath("//*[not(self::script)][text()= '" + testData + "']"));
										el.logExecution("Element found by searching for " + "//*[not(self::script)][text()= '" + testData + "']");
									}
									printWebElementInfo(textElement);
									String srtype = (String)step.get("samerowtype");
									int index = (int)step.get("samerowindex");
									we = findElementonSameRow(textElement, srtype, index,-1);
									foundby = "by-samerow";
								} else if (((String)step.get("testdata_source")).equals("SameRowElement") ||
										(locationStrategy != null && locationStrategy.equalsIgnoreCase("Previous"))) {
									we = findElementonSameRowElement(we, xpath);
									foundby = "by-samerowelement";
								} else if (action.equalsIgnoreCase("Javascript") && xpath.equals("")) {
									found = true;
									foundby = "by-na";
									break;
								} else if (!((boolean)step.get("uniqueisbackup")) &&
										((String)step.get("uniquetext") != null && 
										!((String)step.get("uniquetext")).equals(""))) {
									String uniquetext = (String)step.get("uniquetext");
									String uniquetoparent = (String)step.get("uniquetoparent");
									String parenttotarget = (String)step.get("parenttotarget");
									we = findUniqueImage(uniquetext, uniquetoparent, parenttotarget,el);
									foundby = "by-uniquetext";
								} else {
									int isDynamic = (int)step.get("isDynamic");
									//isDynamic = 0;
//									el.logExecution("Step " + stepnum + " xpath " + xpath
//											+ " turning off the dynamic check as it is causing issues -- " + isDynamic );
									boolean isShadow = false;
									if(step.get("shadowdom") instanceof Integer) {
										int isShadowInt = (int)step.get("shadowdom");
										if(isShadowInt == 1) {
											isShadow = false;
										}
									} else {
										isShadow = (boolean)step.get("shadowdom");
									}
//									boolean isShadow = (boolean)step.get("shadowdom");
									el.logExecution("Step " + stepnum + " xpath " + xpath
											+ " the isDynamic value is -- " + isDynamic + " "+ elapsedTime  + " " );
									el.logExecution("Step " + stepnum + " the isShadow value is " + isShadow  );
									
									if(isDynamic == 1 || ((String)step.get("testdata_source")).equals("Search")) {
										el.logExecution("Step " + stepnum 
												+ " since this step is dynamic, searching on page by default for " + testData  + 
												"  - elapsed time is -" + elapsedTime  + " " );
										if(testData.endsWith("]")) {
											testData = testData.substring(0,testData.length()-1);
											int ind = testData.lastIndexOf("[");
											String indexStr = testData.substring(ind+1,testData.length());
											testData = testData.substring(0,ind);
											try {
												int index = Integer.valueOf(indexStr);
												el.logExecution("The search is :" + "//*[text()= '" + testData + "']");
												List<WebElement> elements = driver.findElements(By.xpath("//*[text()= '" + testData + "']"));
												el.logExecution("The index is :" + index);
												we = elements.get(index);
												
											} catch (Exception inde) {
												el.logExecution("The index is not a number");
												status = "FAIL";
												el.logExecution(inde);
												throw inde;
											}
											
										} else {
											xpath = "//*[text()='" + testData + "']";
											try {
												we = driver.findElement(By.xpath(xpath));
												el.logExecution("Step " + stepnum 
														+ " successfully found dynamic value on page " + testData + 
														elapsedTime  + " " );
											}catch (org.openqa.selenium.NoSuchElementException dyne) {
												el.logExecution("Step " + stepnum 
														+ " search for dynamic value failed resorting to path search " + xpath + " " +
														elapsedTime  + " " );
												we = driver.findElement(By.xpath(xpath));
												el.logExecution("Step " + stepnum 
														+ " path search successful " + xpath + " " +
														elapsedTime  + " " );
											}
										}
										foundby = "by-search";
										el.logExecution("Found by using Location Strategy: Search on Page");
									} else {	
										if(foundby != null && foundby.equalsIgnoreCase("by-xpath")) {

											el.logExecution("earlier was found by xpath but couldnt interact - so now we will try with other alternatives");
											throw new NoSuchElementException("need to change strategy");
										}
										we = driver.findElement(By.xpath(xpath));
										foundby = "by-xpath";
										el.logExecution("Found by using Location Strategy: xpath");
										if(step.get("manuallyAdded") != null && step.get("manuallyAdded").equals("nlp")) {
											we = getNearestForNLP(step, we, keyword, action);
										}
										if(isShadow) {
											errorCode = "E0016";
											runIssues = "Shadow DOM is true but the element is not Shadow element";
											// Execute JavaScript to access the Shadow DOM and retrieve descendants
									        String script = "return arguments[0].shadowRoot.querySelectorAll('*');";
									        JavascriptExecutor jsExecutor = (JavascriptExecutor) driver;
									        Object result = jsExecutor.executeScript(script, we);

									        if (result instanceof java.util.List<?>) {
									            java.util.List<?> descendants = (java.util.List<?>) result;
									            
									            int howManyFound = 0;
									            String lookingFor = (String)step.get("shadowelement");
									            int lookingForIndex = (int)step.get("shadowindex");
									            boolean shadowFound = false;
									            for (Object descendant : descendants) {
									                if (descendant instanceof WebElement) {
									                    WebElement shadowDomElement = (WebElement) descendant;
									                    // Process each descendant element as needed
									                    String elementType = shadowDomElement.getTagName();
									                    if(elementType.equalsIgnoreCase(lookingFor)) {
									                    	if(lookingForIndex == howManyFound + 1) {
									                    		we = shadowDomElement;
									                    		shadowFound = true;
									                    		break;
									                    	} else {
									                    		howManyFound = howManyFound + 1;
									                    		if(howManyFound >= lookingForIndex) {
									                    			shadowFound = false;
										                    		break;
									                    		}
									                    	}
									                    }
									                }
									            }
									            
									            if(!shadowFound) {
									            	throw new NoSuchElementException("Descendant element not found");
									            }
									        }
									        foundby = "by-shadow";
									        errorCode = "";
									        runIssues = "";
										}
									}
								}
								if(keyword != null && !keyword.equalsIgnoreCase("Alert") && !keyword.equalsIgnoreCase("Api") && 
										!keyword.equalsIgnoreCase("Inline") && !keyword.equalsIgnoreCase("Clipboard") && !keyword.equalsIgnoreCase("if")) {
									if(locationStrategy == null || !locationStrategy.equalsIgnoreCase("XY") ) {
										weType = we.getTagName();
										if(!we.isEnabled() && elapsedTime < AppProperties.defaultwaittime && !action.equals("Properties") &&
												weType != null && !weType.equals("span")) {
											el.logExecution("Step " + stepnum + " xpath " + xpath
													+ " found but element is not enabled retrying " + elapsedTime  + " " );
//											runIssues = runIssues + "Step " + stepnum + " xpath " + xpath
//													+ "found but element is not enabled retrying" + elapsedTime  + " " + 
//													Utilities.getNow();
											try {
												Thread.sleep(500);
											} catch (Exception et) {
												el.logExecution("thread11 sleeping for 500 interupted");
											}
											elapsedTime = System.currentTimeMillis() -  startTime;
											continue;
										}
									}
								}
								found = true;
							} catch (Exception e) {								
								
								if(!(e instanceof NoSuchElementException) && 
										!(e instanceof NoSuchSessionException)) {
									el.logExecution("A exception occured");
									el.logExecution(e);
									status = "FAIL";
									
									 if(flow.equalsIgnoreCase("Optional")) {
							    		   el.logExecution("Optional & element not found, so this scenario will continue " + xpath);
							    		    
							    	 } else  if(flow.equalsIgnoreCase("Negative")) {
							    		   el.logExecution("Negative flow & element not found, so this scenario will continue " + xpath);
	 
							    	 }
									 break;
								}								
								
								if ((elapsedTime < AppProperties.defaultwaittime) ||
										(wait != null && wait.equalsIgnoreCase("WaitTill") && elapsedTime < AppProperties.seleniumwaittime)) {
									el.logExecution("Step  " + stepnum + " DID NOT FIND ELEMENT " + xpath
											+ " total wait time " + elapsedTime + " ");
									
									if (flow != null && flow.equals("Negative") && 
											(keyword != null && keyword.equals("Validation"))) {
										break;
									}							
									
									String xpath1 = null;
									String xpath2 = null;
									try {
										if(xpath != null && !xpath.equals("") && xpath.contains("|")) {
											xpath1 = xpath.substring(0, xpath.indexOf("|") - 1);
											xpath2 = xpath.substring(xpath.indexOf("|") + 2, xpath.length());
											Xpath = xpath2;
										} else {
											el.logExecution("Step  " + stepnum + " Xpath does not contain | " + xpath1
													+ " total wait time " + elapsedTime + " ");
											throw new Exception("Xpath1 not valid");
										}
										if(foundby != null && (foundby.equalsIgnoreCase("by-relative") || 
												foundby.equalsIgnoreCase("by-xpath") || 
												foundby.equalsIgnoreCase("by-absolute") || 
												foundby.equalsIgnoreCase("by-xpath") || 
												foundby.equalsIgnoreCase("by-altpath") || 
												foundby.equalsIgnoreCase("by-description") ||
												foundby.equalsIgnoreCase("by-unique"))) {
											el.logExecution("earlier was found by xpath - so we need to try next strategy");
											throw new Exception("Xpath1 - try next");
										}
										el.logExecution("Step  " + stepnum + " Looking using id based path " + xpath1
												+ " total wait time " + elapsedTime + " ");
										we = driver.findElement(By.xpath(xpath1));
										xpath = xpath1;
										foundby = "by-relative";
										found = true;
										el.logExecution("Found by using Location Strategy: Relative xpath");
										el.logExecution("Found by xpath: " + xpath);
										el.logExecution("Step  " + stepnum + " found by xpath1 " + xpath1
												+ " total wait time " + elapsedTime + " ");
										break;
									} catch (Exception expath1) {
										el.logExecution("Step  " + stepnum + " Looking using absolute path - couldnt find using relative path " + xpath1
												+ " total wait time " + elapsedTime + " ");
										try {
											if(xpath2 == null) {
												el.logExecution("Step  " + stepnum + " Absolute Path is null " + xpath2
														+ " total wait time " + elapsedTime + " ");
												throw new Exception("Xpath2 not valid");
											} else if(foundby != null && (foundby.equalsIgnoreCase("by-absolute") || 
													foundby.equalsIgnoreCase("by-xpath") || 
													foundby.equalsIgnoreCase("by-altpath") || 
													foundby.equalsIgnoreCase("by-description") ||
													foundby.equalsIgnoreCase("by-unique"))) {
												el.logExecution("earlier was found by xpath - so we need to try next strategy");
												throw new Exception("Xpath2 - try next");
											} 
											
											we = driver.findElement(By.xpath(xpath2));
											xpath = xpath2;
											foundby = "by-absolute";
											found = true;
											el.logExecution("Found by using Location Strategy: Absolute xpath");
											el.logExecution("Found by xpath: " + xpath);
											el.logExecution("Step  " + stepnum + " found by Absolute Xpath " + xpath2
													+ " total wait time " + elapsedTime + " ");
											break;
										} catch (Exception expath2) {
											el.logExecution("Step  " + stepnum + " Looking using altpath - couldnt find using absolute path " + xpath2
													+ " total wait time " + elapsedTime + " ");
											String altpath = (String)step.get("altpath");
											try {
												if(foundby != null && (foundby.equalsIgnoreCase("by-altpath") || 
														foundby.equalsIgnoreCase("by-description") ||
														foundby.equalsIgnoreCase("by-unique"))) {
													el.logExecution("earlier was found by altpath - so we need to try next strategy");
													throw new Exception("altpath - try next");
												}
												if(altpath != null && !altpath.equals("")) {
													el.logExecution("Step  " + stepnum + " Looking using altpath - couldnt find searching by page description " );
													String xalt = altpath;
													el.logExecution("Step  " + stepnum + " altpath is  " + xalt + " ");
													we = driver.findElement(By.xpath(xalt));
													xpath = altpath;
													found = true;
													foundby = "by-altpath";
													el.logExecution("Found by using Location Strategy: Altpath");
													el.logExecution("Found by xpath: " + xpath);
													el.logExecution("Step  " + stepnum + " found by altpath " + altpath
															+ " total wait time " + elapsedTime + " ");
													break;
												} else {
													throw new Exception("No alt path");
												}												
											} catch (Exception searchExep) {
												el.logExecution("Step  " + stepnum + " Looking using text on element - couldnt find using alt path " + altpath
														+ " total wait time " + elapsedTime + " ");
//												String altpath = (String)step.get("altpath");
												try {	
													if(foundby != null && (foundby.equalsIgnoreCase("by-description") ||
															foundby.equalsIgnoreCase("by-unique"))) {
														el.logExecution("earlier was found by description - so we need to try next strategy");
														throw new Exception("desc - try next");
													}
													//String paged = (String)step.get("Page_Description");
													String paged = testData;
													if(paged != null && !paged.equals("")) {
														el.logExecution("Step  " + stepnum + " Locating using page description ");
//														String salt = "//*[text()='" + paged + "']";
														String salt = "";
														if(saveType.equalsIgnoreCase("AI_GEN")) {
															salt = "//*[contains(translate(text(), 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz'), '" + testData.toLowerCase() +"')]";
														} else {
															salt = "//*[contains(text(), \"" + paged + "\")]";
														}
														el.logExecution("Step  " + stepnum + " Locating using page description  " + salt + " ");
														we = driver.findElement(By.xpath(salt));
														xpath = salt;
														foundby = "by-description";
														found = true;
														el.logExecution("Found by using Location Strategy: Page Description");
														el.logExecution("Found by xpath: " + xpath);
														el.logExecution("Step  " + stepnum + " found by searching by page descritpion " + salt
																+ " total wait time " + elapsedTime + " ");
														break;
													} else {
														el.logExecution("Step  " + stepnum + " Couldnt use text on element as element has no text ");
														throw new Exception("No page decsrption to search");
													}
													
												} catch (Exception ealtpath) {
													try {
														if(foundby != null && (foundby.equalsIgnoreCase("by-unique"))) {
															el.logExecution("earlier was found by unique - so we need to try next strategy");
															throw new Exception("desc - try next");
														}
														el.logExecution("Step  " + stepnum + " Looking using proximity approach - couldnt find by usin alt path " + altpath
																+ " total wait time " + elapsedTime + " ");
														if (((boolean)step.get("uniqueisbackup")) &&
																((String)step.get("uniquetext") != null && 
																!((String)step.get("uniquetext")).equals(""))) {
															String uniquetext = (String)step.get("uniquetext");
															String uniquetoparent = (String)step.get("uniquetoparent");
															String parenttotarget = (String)step.get("parenttotarget");
															we = findUniqueImage(uniquetext, uniquetoparent, parenttotarget,el);														
															if(we == null) {
																throw new Exception("Proximity approach doenst exist");
															} else {
																found = true;
																foundby = "by-unique";
																el.logExecution("Found by using Location Strategy: Proximity approach");
																el.logExecution("Step  " + stepnum + " found by proximity approach " + 
																		"unique text -- " + uniquetext + 
																		" total wait time " + elapsedTime + " ");
																break;
															}
														} else {
															throw new Exception("Proximity approach doenst exist");
														}
													} catch (Exception eprox) {
														try {
														el.logExecution("Step  " + stepnum + " Looking using approximation approach - couldnt find using proximity " 
																+ " total wait time " + elapsedTime + " ");
														we = findByApprox(xpath, outerhtml, keyword);
														if(we == null) {
												    	   if(flow.equals("Optional")) {
												    		   el.logExecution("Optional & element not found, so this step is a pass " + xpath);
																break;  
												    	   } else {
												    		   el.logExecution("Step  " + stepnum + " Did not find by Approx path " + xpath
																	+ "outerhtml -->" + outerhtml + "keyword -" + keyword + 
																	" total wait time " + elapsedTime + " ");
												    		   throw e;
													    	   }
													       } else {
													    	   found = true;
													    	   foundby = "by-approx";
													    	   el.logExecution("Found by using Location Strategy: Approx xpath");
													    	   el.logExecution("Step  " + stepnum + " found by Approx xpath " + xpath
																	+ "outerhtml -->" + outerhtml + "keyword -" + keyword + 
																	" total wait time " + elapsedTime + " ");
													       }
														}catch (Exception ohtml) {
															try {
																Thread.sleep(500);
															} catch (Exception et) {
																el.logExecution("thread sleeping for 500 interupted");
															}
															elapsedTime = System.currentTimeMillis() -  startTime;
															el.logExecution("Step  " + stepnum + " Looking for xpath " + xpath
																	+ " total wait time " + elapsedTime  + " ");
														}
													}
												}
											}
										}
									}
									el.logExecution("--------------------------------------------------------------------------------------------------------------------");
								} else {
									
									if(analyseFailures && (attempt >= 3 || !found) && !flow.equalsIgnoreCase("Optional")) {
										el.logExecution("In analyse failures block");
										boolean isMatched = false;
										
										if(!isMatched) {
											String pageSource1 = driver.getPageSource();
											int outerhtmlMatches = 0;
											if(outerhtml != null && !outerhtml.equals("")) {
												outerhtmlMatches = StringUtils.countMatches(pageSource1, outerhtml);
											}
											String nearxpath = "";
											if(outerhtmlMatches > 1) {
												el.logExecution("Since we have multiple outer HTML matches, trying to build the nearest xpath");
												int z =0;
												HashMap attributes = new HashMap();
												executionUtility.getAllAttributes(attributes, outerhtml);
												
												String tagg = "svg";
												Pattern pattern = Pattern.compile("<\\s*(\\w+)");
										        Matcher matcher = pattern.matcher(outerhtml);
					
										        if (matcher.find()) {
										            tagg = matcher.group(1);
										        }
												
												String childXpath = ".//*[name()='" + tagg + "']";
												childXpath += "[";
												for (Object obj : attributes.entrySet()) {
													Map.Entry entry = (Map.Entry) obj;
													if(entry.getKey().equals("text") || entry.getKey().equals("nested")) {
										            	continue;
										            }
													String key = (String)entry.getKey();
													if(key.equalsIgnoreCase("viewbox")) {
														key = "viewBox";
													}
													childXpath += "@" + key + "='" + (String)entry.getValue() + "' and ";
												}
												childXpath = childXpath.substring(0, childXpath.length()-5);
												childXpath += "]";
												while(Xpath.contains("/")) {
													try {
														z++;
														Xpath = Xpath.substring(0,Xpath.lastIndexOf("/"));
														el.logExecution("In try " + z);
													    WebElement parent = driver.findElement(By.xpath(Xpath));
													    el.logExecution(Xpath + " .....");
													    el.logExecution("Under parent " + z);
//													    List<WebElement> data = parent.findElements(By.xpath(
//													    		".//*[name()='svg'][@viewBox='64 64 896 896' and " +
//													    			    "@focusable='false' and " +
//													    			    "@data-icon='search' and " +
//													    			    "@width='1em' and " +
//													    			    "@height='1em' and " +
//													    			    "@fill='currentColor' and " +
//													    			    "@aria-hidden='true']"
//													    			    ));

													    List<WebElement> data = parent.findElements(By.xpath(childXpath));
													    List<String> xpaths = new ArrayList<String>();
													    for (WebElement svg : data) {
													    	WebElement par = svg;
													    	String tag = "";
													    	String svgXpath = "";
													    	while(par != null) {
													    		try {
													    			tag = par.getTagName();
													    			if(tag.equals("svg")) {
													    				tag = "*[name()='svg']";
													    			}
													    			if(tag.equals("html")) {
													    				svgXpath = "html" + svgXpath;
													    				break;
													    			}
													    			int index = 1;
													    			boolean foundpar = false;
													    			par = findParentElement(par);
													    			String temp = "";
													    			while(true) {
													    				temp = "/" + tag + "[" + index + "]" + svgXpath;
													    				try {
													    					if(foundpar || index>10) {
													   							break;
													   						}
													    					index++;
													    					WebElement child = par.findElement(By.xpath("./" + temp));
													   						String childouterhtml = child.getAttribute("outerHTML");
													   						if(childouterhtml.equals(outerhtml)) {
													   							foundpar = true;
													   						}
													   						
												    					} catch(Exception ev) {
												    						
												    					}
													    			}
													    			svgXpath = "/" + tag + "[" + (index-1) + "]" + svgXpath;
													    			
													    		} catch(NoSuchElementException ec) {
													    			
													    		}
													    	}
													    	xpaths.add(svgXpath);
													    	el.logExecution("Xpath built: " + svgXpath);
													    }
													    int mini = 10000000;
													    for(int k=0;k<xpaths.size();k++) {
													    	int count = executionUtility.comparexpath(Xpath, xpaths.get(k));
													    	if(count<mini) {
													    		mini=count;
													    		nearxpath = xpaths.get(k);
													    	}
													    }
													    el.logExecution("Nearest Xpath built: " + nearxpath);
													    if(data.size() > 1)break;
													} catch (NoSuchElementException en) {
														
													}
												}
											}
											try {
												el.logExecution("Trying to find the using nearest Xpath");
												we = driver.findElement(By.xpath(nearxpath));
												found = true;
												el.logExecution("Found using nearest xpath");
												String prevXpath = (String) step.get("Object_Xpath");
												msc.updateXpath(testStepId, nearxpath);
												msc.writexpathToDB(testStepId, prevXpath);
											}catch(Exception em) {
												el.logExecution("Unable to find element using nearest Xpath");
											}
										}
									}
									
									if(!flow.equalsIgnoreCase("Optional")) {
										el.logExecution("Started xpath building");
										String pageSource1 = driver.getPageSource();
										String actualxpath = Xpath;
										int outerhtmlMatches = 0;
										String passedOuterHTML = "";
										int ptcrid = executionUtility.getPassedTcrid(msc, testStepId, testCaseId);
										passedOuterHTML = msc.getPassedOuterhtml(ptcrid, testStepId);
										if(passedOuterHTML == null || passedOuterHTML.equals("")) {
											passedOuterHTML = outerhtml;
										}
										if(passedOuterHTML != null && !passedOuterHTML.equals("")) {
											outerhtmlMatches = StringUtils.countMatches(pageSource1, passedOuterHTML);
										}
										String nearxpath = "";
										if(outerhtmlMatches == 1) {
											el.logExecution("Since there is only single outerhtml match, building the xpath");
											int z =0;
											
											HashMap attributes = new HashMap();
											executionUtility.getAllAttributes(attributes, passedOuterHTML);
											
											String tagg = "svg";
											Pattern pattern = Pattern.compile("<\\s*(\\w+)");
									        Matcher matcher = pattern.matcher(passedOuterHTML);
				
									        if (matcher.find()) {
									            tagg = matcher.group(1);
									        }
											
											String childXpath = ".//*[name()='" + tagg + "']";
											childXpath += "[";
											for (Object obj : attributes.entrySet()) {
												Map.Entry entry = (Map.Entry) obj;
												if(entry.getKey().equals("text") || entry.getKey().equals("nested")) {
									            	continue;
									            }
												String key = (String)entry.getKey();
												if(key.equalsIgnoreCase("viewbox")) {
													key = "viewBox";
												}
												childXpath += "@" + key + "='" + (String)entry.getValue() + "' and ";
											}
											childXpath = childXpath.substring(0, childXpath.length()-5);
											childXpath += "]";
											while(Xpath.contains("/")) {
												try {
													z++;
													Xpath = Xpath.substring(0,Xpath.lastIndexOf("/"));
													el.logExecution("In try " + z);
												    WebElement parent = driver.findElement(By.xpath(Xpath));
												    el.logExecution(Xpath + " .....");
												    el.logExecution("Under parent " + z);

												    List<WebElement> data = parent.findElements(By.xpath(childXpath));
												    List<String> xpaths = new ArrayList<String>();
												    for (WebElement ele : data) {
												    	WebElement par = ele;
												    	String tag = "";
												    	String builtxpath = "";
												    	while(par != null) {
												    		try {
												    			tag = par.getTagName();
												    			if(tag.equals("svg")) {
												    				tag = "*[name()='svg']";
												    			}
												    			if(tag.equals("html")) {
												    				builtxpath = "html" + builtxpath;
												    				break;
												    			}
												    			int index = 1;
												    			boolean foundpar = false;
												    			par = findParentElement(par);
												    			String temp = "";
												    			while(true) {
												    				temp = "/" + tag + "[" + index + "]" + builtxpath;
												    				try {
												    					if(foundpar || index>10) {
												   							break;
												   						}
												    					index++;
												    					WebElement child = par.findElement(By.xpath("./" + temp));
												   						String childouterhtml = child.getAttribute("outerHTML");
												   						if(childouterhtml.equals(passedOuterHTML)) {
												   							foundpar = true;
												   						}
												   						
											    					} catch(Exception ev) {
											    						
											    					}
												    			}
												    			builtxpath = "/" + tag + "[" + (index-1) + "]" + builtxpath;
												    			
												    		} catch(NoSuchElementException ec) {
												    			
												    		}
												    	}
												    	xpaths.add(builtxpath);
												    	el.logExecution("Xpath built: " + builtxpath);
												    }
												    int mini = 10000000;
												    for(int k=0;k<xpaths.size();k++) {
												    	int count = executionUtility.comparexpath(actualxpath, xpaths.get(k));
												    	if(count<mini) {
												    		mini=count;
												    		nearxpath = xpaths.get(k);
												    	}
												    }
												    el.logExecution("Nearest Xpath built: " + nearxpath);
												    if(data.size() >= 1)break;
												} catch (NoSuchElementException en) {
													
												}
											}
										}
										if(outerhtmlMatches > 1) {
											el.logExecution("Since we have multiple outer HTML matches, trying to build the nearest xpath");
											int z =0;
											
											HashMap attributes = new HashMap();
											executionUtility.getAllAttributes(attributes, passedOuterHTML);
											
											String tagg = "svg";
											Pattern pattern = Pattern.compile("<\\s*(\\w+)");
									        Matcher matcher = pattern.matcher(passedOuterHTML);
				
									        if (matcher.find()) {
									            tagg = matcher.group(1);
									        }
											
											String childXpath = ".//*[name()='" + tagg + "']";
											childXpath += "[";
											for (Object obj : attributes.entrySet()) {
												Map.Entry entry = (Map.Entry) obj;
												if(entry.getKey().equals("text") || entry.getKey().equals("nested")) {
									            	continue;
									            }
												String key = (String)entry.getKey();
												if(key.equalsIgnoreCase("viewbox")) {
													key = "viewBox";
												}
												childXpath += "@" + key + "='" + (String)entry.getValue() + "' and ";
											}
											childXpath = childXpath.substring(0, childXpath.length()-5);
											childXpath += "]";
											while(Xpath.contains("/")) {
												try {
													z++;
													Xpath = Xpath.substring(0,Xpath.lastIndexOf("/"));
													el.logExecution("In try " + z);
												    WebElement parent = driver.findElement(By.xpath(Xpath));
												    el.logExecution(Xpath + " .....");
												    el.logExecution("Under parent " + z);

												    List<WebElement> data = parent.findElements(By.xpath(childXpath));
												    List<String> xpaths = new ArrayList<String>();
												    for (WebElement ele : data) {
												    	WebElement par = ele;
												    	String tag = "";
												    	String builtxpath = "";
												    	while(par != null) {
												    		try {
												    			tag = par.getTagName();
												    			if(tag.equals("svg")) {
												    				tag = "*[name()='svg']";
												    			}
												    			if(tag.equals("html")) {
												    				builtxpath = "html" + builtxpath;
												    				break;
												    			}
												    			int index = 1;
												    			boolean foundpar = false;
												    			par = findParentElement(par);
												    			String temp = "";
												    			while(true) {
												    				temp = "/" + tag + "[" + index + "]" + builtxpath;
												    				try {
												    					if(foundpar || index>10) {
												   							break;
												   						}
												    					index++;
												    					WebElement child = par.findElement(By.xpath("./" + temp));
												   						String childouterhtml = child.getAttribute("outerHTML");
												   						if(childouterhtml.equals(passedOuterHTML)) {
												   							foundpar = true;
												   						}
												   						
											    					} catch(Exception ev) {
											    						
											    					}
												    			}
												    			builtxpath = "/" + tag + "[" + (index-1) + "]" + builtxpath;
												    			
												    		} catch(NoSuchElementException ec) {
												    			
												    		}
												    	}
												    	xpaths.add(builtxpath);
												    	el.logExecution("Xpath built: " + builtxpath);
												    }
												    int mini = 10000000;
												    for(int k=0;k<xpaths.size();k++) {
												    	int count = executionUtility.comparexpath(actualxpath, xpaths.get(k));
												    	if(count<mini) {
												    		mini=count;
												    		nearxpath = xpaths.get(k);
												    	}
												    }
												    el.logExecution("Nearest Xpath built: " + nearxpath);
												    if(data.size() >= 1)break;
												} catch (NoSuchElementException en) {
													
												}
											}
										}
										try {
											el.logExecution("Trying to find the using built Xpath");
											we = driver.findElement(By.xpath(nearxpath));
											found = true;
											foundby = "by-builtxapth";
											el.logExecution("Found using built xpath");
											String prevXpath = (String) step.get("Object_Xpath");
											msc.updateXpath(testStepId, nearxpath);
											msc.writexpathToDB(testStepId, prevXpath);
										}catch(Exception em) {
											el.logExecution("Unable to find element using built Xpath");
										}
										el.logExecution("Ended Building of xpath");
									}
									
									if(!found) {
										throw e;
									}
								}
							}
							if(!found) {
								Utilities.executionMsgCache.put(testCaseResultsId, "Step number: " + stepnum + "Locating Element - will try for upto 15s - elapsed time is : "+ elapsedTime + " and attempt count is: " + attempt);
							}
						} while (!found);
						
						if(analyseFailures && attempt >= 3 && found) {
							el.logExecution("In analyse failures block");
							boolean isMatched = false;
							String foundOuterHTML = "";
							String pageSource = driver.getPageSource();
							if(found) {
								foundOuterHTML = (String)we.getAttribute("outerHTML");
								boolean[] flag = {false};
								isMatched = !(executionUtility.comparePassedOuterHTML(msc, testStepId, testCaseId, foundOuterHTML, outerhtml, pageSource, this, flag, foundby));
							}
							
							if(!isMatched) {
								String pageSource1 = driver.getPageSource();
								int outerhtmlMatches = 0;
								if(outerhtml != null && !outerhtml.equals("")) {
									outerhtmlMatches = StringUtils.countMatches(pageSource1, outerhtml);
								}
								String nearxpath = "";
								if(outerhtmlMatches >= 1) {
									el.logExecution("Since we have multiple outer HTML matches, trying to build the nearest xpath");
									int z =0;
									HashMap attributes = new HashMap();
									executionUtility.getAllAttributes(attributes, outerhtml);
									
									String tagg = "svg";
									Pattern pattern = Pattern.compile("<\\s*(\\w+)");
							        Matcher matcher = pattern.matcher(outerhtml);
		
							        if (matcher.find()) {
							            tagg = matcher.group(1);
							        }
									
									String childXpath = ".//*[name()='" + tagg + "']";
									childXpath += "[";
									for (Object obj : attributes.entrySet()) {
										Map.Entry entry = (Map.Entry) obj;
										if(entry.getKey().equals("text") || entry.getKey().equals("nested")) {
							            	continue;
							            }
										String key = (String)entry.getKey();
										if(key.equalsIgnoreCase("viewbox")) {
											key = "viewBox";
										}
										childXpath += "@" + key + "='" + (String)entry.getValue() + "' and ";
									}
									childXpath = childXpath.substring(0, childXpath.length()-5);
									childXpath += "]";
									while(Xpath.contains("/")) {
										try {
											z++;
											Xpath = Xpath.substring(0,Xpath.lastIndexOf("/"));
											el.logExecution("In try " + z);
										    WebElement parent = driver.findElement(By.xpath(Xpath));
										    el.logExecution(Xpath + " .....");
										    el.logExecution("Under parent " + z);
//										    List<WebElement> data = parent.findElements(By.xpath(
//										    		".//*[name()='svg'][@viewBox='64 64 896 896' and " +
//										    			    "@focusable='false' and " +
//										    			    "@data-icon='search' and " +
//										    			    "@width='1em' and " +
//										    			    "@height='1em' and " +
//										    			    "@fill='currentColor' and " +
//										    			    "@aria-hidden='true']"
//										    			    ));
										    
										    List<WebElement> data = parent.findElements(By.xpath(childXpath));
										    List<String> xpaths = new ArrayList<String>();
										    for (WebElement svg : data) {
										    	WebElement par = svg;
										    	String tag = "";
										    	String svgXpath = "";
										    	while(par != null) {
										    		try {
										    			tag = par.getTagName();
										    			if(tag.equals("svg")) {
										    				tag = "*[name()='svg']";
										    			}
										    			if(tag.equals("html")) {
										    				svgXpath = "html" + svgXpath;
										    				break;
										    			}
										    			int index = 1;
										    			boolean foundpar = false;
										    			par = findParentElement(par);
										    			String temp = "";
										    			while(true) {
										    				temp = "/" + tag + "[" + index + "]" + svgXpath;
										    				try {
										    					if(foundpar || index>10) {
										   							break;
										   						}
										    					index++;
										    					WebElement child = par.findElement(By.xpath("./" + temp));
										   						String childouterhtml = child.getAttribute("outerHTML");
										   						if(childouterhtml.equals(outerhtml)) {
										   							foundpar = true;
										   						}
										   						
									    					} catch(Exception e) {
									    						
									    					}
										    			}
										    			svgXpath = "/" + tag + "[" + (index-1) + "]" + svgXpath;
										    			
										    		} catch(NoSuchElementException e) {
										    			
										    		}
										    	}
										    	xpaths.add(svgXpath);
										    	el.logExecution("Xpath built: " + svgXpath);
										    }
										    int mini = 10000000;
										    for(int k=0;k<xpaths.size();k++) {
										    	int count = executionUtility.comparexpath(Xpath, xpaths.get(k));
										    	if(count<mini) {
										    		mini=count;
										    		nearxpath = xpaths.get(k);
										    	}
										    }
										    el.logExecution("Nearest Xpath built: " + nearxpath);
										    if(data.size() > 1)break;
										} catch (NoSuchElementException e) {
											
										}
									}
								}
								try {
									el.logExecution("Trying to find the using nearest Xpath");
									we = driver.findElement(By.xpath(nearxpath));
									found = true;
									el.logExecution("Found using nearest xpath");
									String prevXpath = (String) step.get("Object_Xpath");
									msc.updateXpath(testStepId, nearxpath);
									msc.writexpathToDB(testStepId, prevXpath);
								}catch(Exception e) {
									el.logExecution("Unable to find element using nearest Xpath");
								}
							}
						}
						
						if(found) {
							if(foundby != null && !foundby.equalsIgnoreCase("by-na")) {
								try {
									foundOuterhtml = we.getAttribute("outerHTML");
									foundelements.add(foundOuterhtml);
									el.logExecution("Outer HTML of element found: " + foundOuterhtml);
								} catch(Exception e) {
									
								}
							}
							String pageSource = driver.getPageSource();
							boolean canContinue = false;
							boolean[] flag = {false, false};
							ExecutionUtility executionUtility1 = new ExecutionUtility();
							
							try {
								el.logExecution("Starting the String compare of outer HTML");
								if(action.equalsIgnoreCase("DynamicText")) {
									el.logExecution("Since action is Dynamic Text, not comparing the outerhtml");
								} else if(foundOuterhtml.length() > 1000) { 
									el.logExecution("Since the size of found outerhtml is more than 1000 characters, not comparing the outerhtml");
								} else {
									Utilities.executionMsgCache.put(testCaseResultsId, "Step number: " + stepnum + "Comparing outerhtml against last passed outerhtml");
									canContinue = executionUtility1.comparePassedOuterHTML(msc, testStepId, testCaseId, foundOuterhtml, outerhtml, pageSource, this, flag, foundby);
									el.logExecution("Is outer HTML matched: " + !(canContinue));
									if(flag[0] && attempt < 3 && !action.equalsIgnoreCase("DynamicText")) {
										el.logExecution("Since previous passed outerhtml is present once, we are using next location strategy");
										if(attempt == 1) {
											temporaryFoundBy = foundby;
										}
										continue;
									}
								}
								if(canContinue) {
									Utilities.executionMsgCache.put(testCaseResultsId, "Step number: " + stepnum + "Comparing Attributes of outerhtml against last passed outerhtml");
									el.logExecution("Since outer HTML is not matched, comparing all the attributes");
									el.logExecution("Started Attr Compare");
									canContinue = !(executionUtility1.attrWiseCompare(msc, stepnum, testStepId, testCaseId, foundOuterhtml, outerhtml, this, al));
									el.logExecution("Is outer HTML matched based on attributes: " + !(canContinue));
									if(canContinue) {
										el.logExecution("Updating the non matching attributes");
										msc.updateTestStepAttr(testCaseResultsId, testStepId, nonMatchingAttr);
									}
									el.logExecution("Ended Attr Compare");
								}
							} catch(Exception e) {
								
							}
							if(analyseFailures) {
								if(!(((String)step.get("testdata_source")).equals("Search") && isTestDataProcessed)) {
									if(canContinue && attempt < 3) {
										
										int outerHTMLMatches = 0;
										if(outerhtml != null && !outerhtml.equals("")) {
											outerHTMLMatches = StringUtils.countMatches(pageSource, outerhtml);
										}
										if(outerHTMLMatches > 1) {
											try {
												boolean childFound = false;
												WebElement parent = we;
												WebElement child = null;
												while(!childFound) {
													parent = findParentElement(parent);
													if(parent == null) {
														break;
													}
													String paged = (String)step.get("Page_Description");
													if(paged == null || paged.equals("")) {
														break;
													}
													String childxpath = ".//*[text()='" + paged + "']";
//    	  											String childxpath = ".//*[contains(text(),'" + paged + "')]";
//													String childxpath = ".//a[text()='" + paged + "']";
													try {
														child = parent.findElement(By.xpath(childxpath));
														childFound = true;
													} catch(Exception e) {
														
													}
												}
												if(child != null) {
													canContinue = false;
												}
												we = child;
												foundOuterhtml = child.getAttribute("outerHTML");
												canContinue = executionUtility1.comparePassedOuterHTML(msc, testStepId, testCaseId, foundOuterhtml, outerhtml, pageSource, this, flag, foundby);
												try {
													el.logExecution(we.getAttribute("outerHTML"));
												} catch(Exception e) {
													
												}
											} catch(Exception e) {
												
											}
										}
										if(canContinue && attempt < 3) {
											el.logExecution("Since the outer HTML is not matched, trying the next location strategy");
											continue;
										}	
									}
								}
							}
							el.logExecution("Taking screen shot After finding element");
							if(multiRun) {
								ssfilenameAF = Utilities.getSSFileName(testCaseId, testStepId, testCaseResultsId, stepnum, 
										companyId, "MR" + mrIndex);
							} else {
								ssfilenameAF = Utilities.getSSFileName(testCaseId, testStepId, testCaseResultsId, stepnum, 
									companyId, null);
							}
							String ssloc = getSSFullLoc(ssfilenameAF, testCaseId,testCaseResultsId , 
									companyId);
							Utilities.executionMsgCache.put(testCaseResultsId, "Step number: " + stepnum + " - Taking screen shot After finding element");
							changeBGtakess(we, ssloc, status,analyseFailures,baselineRun, 
									el, locationStrategy, screenX, screenY,createsAlert, keyword);
							el.logExecution("Finished Taking screen shot After finding element");
							el.logExecution("Step  " + stepnum + " Found using strategy : " + foundby + " " );
							prevRowEventTime = curRowEventTime;
							prevRowiframexpath = iframexpath;
							prevTabId = tabid;
							prevWindowId = windowid;
							if(inRetrial && action.equalsIgnoreCase("click")) {
 								keyword = "Mouse";
 								action = "click";
 								el.logExecution("Since the action is click and we are in retrial step using mouse click");
 							}
							if(keyword.equalsIgnoreCase("Check Box")){
								boolean isDisplayed = false;
								isDisplayed = we.isDisplayed();
								el.logExecution("isDisplayed = " +  isDisplayed);
								if(!isDisplayed) {
									keyword = "Mouse";
									action = "click";
									el.logExecution("Since isDisplayed is false for Check Box, changing to Mouse-Click");
								}
							}
							if (keyword != null && 
									(!keyword.equalsIgnoreCase("Mouse") && 
									action.equalsIgnoreCase("click")) &&
									(locationStrategy == null || 
									locationStrategy != null && 
									!locationStrategy.equalsIgnoreCase("XY"))) {
								el.logExecution("Action = Click " );
								boolean isDisplayed = false;
								boolean isException = false;
								if(locationStrategy == null || !locationStrategy.equalsIgnoreCase("XY") ) {
									try {		
										isDisplayed = we.isDisplayed();
										el.logExecution("isDisplayed = " +  isDisplayed);
										
									} catch (Exception e) {
										isException = true;
									} 
								} else {
									el.logExecution("Not clicking here as locator strategy is absolute coordinates " );
								}
								if(isException || !isDisplayed) {
									if(attempt <= 3) {
										el.logExecution("thread sleeping for 500ms " +  attempt);
										try {
											Thread.sleep(500);
										} catch (Exception et) {
											el.logExecution("thread2 sleeping for 500 interupted");
										}
										el.logExecution("waiting for isDisplayed = true - attempt " +  attempt);
										continue;
									} else {
										errorCode = "E0004";
										runIssues = "Is Displayed never turned ON";
										el.logExecution("isDisplayed never turned true after attempts(s)" +  attempt);
										el.logExecution("the element was found by " +  foundby + " try a diferent approach");
										if(isException) {
											throw new Exception("Element not displayed");
										} 
									}
								}
								if (isDisplayed) {
									try {
										el.logExecution("Checking if element is clickable");
										WebDriverWait weWait = new WebDriverWait(driver, Duration.ofSeconds(10));
										weWait.until(ExpectedConditions.elementToBeClickable(we));
		
										el.logExecution("Step  " + stepnum + " clicking - element is visible " + xpath+ " " );
										long exec_startTime = System.currentTimeMillis();
										we.click();
										Utilities.executionMsgCache.put(testCaseResultsId, "Step number: " + stepnum + " - Successfully Clicked the element");
										ts_execStatus = determineExecStatus(exec_startTime,threshold);
										el.logExecution("Step  " + stepnum + " successfully clicked " + xpath + " " );
		
									} catch (Exception e) {
										el.logExecution("Normal click failed");
										Utilities.executionMsgCache.put(testCaseResultsId, "Step number: " + stepnum + "Normal click failed, clicking on the boundary of the element");
										
										try {
											el.logExecution("Clicking bottom-right of the element");
											clickOnEdgesOfElement(driver, we, "bottom-right");
											el.logExecution("Successfully clicked bottom-right of the element");
										} catch (Exception ce) {
											
											Utilities.executionMsgCache.put(testCaseResultsId, "Step number: " + stepnum + "Clicking on boundary is failed, clicking with Mouse");
										try {
											Actions actions = new Actions(driver);
											el.logExecution("clicking with mouse : " );
									        actions.moveToElement(we).click().perform();
									        el.logExecution("clicked with mouse" );
										} catch(Exception nce) {
											Utilities.executionMsgCache.put(testCaseResultsId, "Step number: " + stepnum + "Clicking with Mouse failed, trying to click after removing the SPAN/DIV that followed the Button in relative path");
										try {
											el.logExecution("Step  " + stepnum + " unable to click - check if relative contains Span or div after the button ");
											String lowerXpath = xpath.substring(0,xpath.indexOf("|"));
											lowerXpath = lowerXpath.toLowerCase();
											el.logExecution("Checking if relative xpath has button followed by Span or div");
											if((lowerXpath.contains("button")) && !(lowerXpath.endsWith("button"))) {
												lowerXpath = lowerXpath.substring(0,lowerXpath.indexOf("button")+6);
												el.logExecution("The new xpath is " + lowerXpath);
												WebElement lwe = driver.findElement(By.xpath(lowerXpath));
												lwe.click();
												we = lwe;
												el.logExecution("Able to click after removing the SPAN/DIV that followed the Button ");
											} else {
												//try {
													el.logExecution("Step  " + stepnum + " unable to click - check if absolute path contains Span or div after the button ");
													String absXpath = xpath.substring(xpath.indexOf("|") + 1,xpath.length());
													absXpath = absXpath.toLowerCase();
													el.logExecution("Checking if relative xpath has button followed by Span or div");
													if((absXpath.contains("button")) && !(absXpath.endsWith("button"))) {
														absXpath = absXpath.substring(0,absXpath.indexOf("button")+6);
														el.logExecution("The new xpath is " + absXpath);
														WebElement lwe = driver.findElement(By.xpath(absXpath));
														lwe.click();
														we = lwe;
														el.logExecution("Able to click after removing the SPAN/DIV that followed the Button ");
													} else {
														throw new Exception();
													}
												//} catch (Exception le2) {
													//el.logExecution("Removing the elements after button for absolute path did not work");
												//}
											}
										} catch (Exception le) {
											Utilities.executionMsgCache.put(testCaseResultsId, "Clicking after removing the SPAN/DIV that followed the Button is failed, trying to click after removing the SPAN/DIV that followed the Button in absolute path");
											el.logExecution("Removing the elements after button for relative path did not work. Trying for absolute path");
											try {
												el.logExecution("Step  " + stepnum + " unable to click - check if absolute path contains Span or div after the button ");
												String absXpath = xpath.substring(xpath.indexOf("|") + 1,xpath.length());
												absXpath = absXpath.toLowerCase();
												el.logExecution("Checking if relative xpath has button followed by Span or div");
												if((absXpath.contains("button")) && !(absXpath.endsWith("button"))) {
													absXpath = absXpath.substring(0,absXpath.indexOf("button")+6);
													el.logExecution("The new xpath is " + absXpath);
													WebElement lwe = driver.findElement(By.xpath(absXpath));
													lwe.click();
													we = lwe;
													el.logExecution("Able to click after removing the SPAN/DIV that followed the Button ");
												} else {
													throw new Exception();
												}
											} catch (Exception le2) {
												el.logExecution("Removing the elements after button for absolute path did not work");
												if(attempt <= 3) {
													Utilities.executionMsgCache.put(testCaseResultsId, "Removing the elements after button for absolute path did not work, retrying the step again and attempt count is: " + attempt);
													continue;
												}
											}
												el.logExecution("Step  " + stepnum + " unable to click - trying alternative ");
												
										try {
											el.logExecution("Since Normal click failed, maximizing the window and trying again");
											Utilities.executionMsgCache.put(testCaseResultsId, "Unable to click the element after 3 attempts, maximizing the window and trying to click");
											driver.manage().window().maximize();
											we.click();
										} catch(Exception et) {
											try {
												long exec_startTime = System.currentTimeMillis();
												((JavascriptExecutor) driver).executeScript("arguments[0].click();", we);
												ts_execStatus = determineExecStatus(exec_startTime,threshold);
											
											} catch(Exception e22) {
												el.logExecution("Click execution failed - using javascript executor");
		
//											if(attempt <= 3) {
//												el.logExecution(
//														"Step  " + stepnum + " unable to click - trying alternative attempt " + attempt + "xpath " + xpath);
//												continue;
//											} else {
												el.logExecution(
														"Step  " + stepnum + " alternative attempt to click failed after attempt(s) " + attempt + "xpath " + xpath);
												el.logExecution(
														"Step  " + stepnum + " trying yet another approach "  + xpath);
												Actions builder = new Actions(driver);
												long exec_startTime = System.currentTimeMillis();
												builder.click(we).build().perform();
												ts_execStatus = determineExecStatus(exec_startTime,threshold);
												//throw e22;
												status = "FAIL";
//												if(status.equals("FAIL") && attempt <= 4) {
//													el.logExecution("Since Normal click failed, maximizing the window and trying again");
//													driver.manage().window().maximize();
//													continue;
//												} 
											}
										}
										el.logExecution("Step  " + stepnum + " successfully clicked alternative" + xpath);
										}
									}
									}
									}
								} else {	
									if(locationStrategy == null || !locationStrategy.equalsIgnoreCase("XY")) {
										el.logExecution("Step  " + stepnum + " clicking - even though element is not visible " + xpath);									
										long exec_startTime = System.currentTimeMillis();
										((JavascriptExecutor) driver).executeScript("arguments[0].click();", we);
										ts_execStatus = determineExecStatus(exec_startTime,threshold);				
										el.logExecution("Step  " + stepnum + " successfully clicked even though it was not visible" + xpath);	
									}
								}
							} else if (action.equalsIgnoreCase("submit")) {
								if (we.isDisplayed()) {
									try {
		
										el.logExecution("Step  " + stepnum + " submitting - it is visible " + xpath);
										Utilities.executionMsgCache.put(testCaseResultsId, "Step number: " + stepnum + " - submitting ......");
										long exec_startTime = System.currentTimeMillis();
										we.submit();
										ts_execStatus = determineExecStatus(exec_startTime,threshold);
										el.logExecution("Step  " + stepnum + " successfully submitted " + xpath);
										Utilities.executionMsgCache.put(testCaseResultsId, "Step number: " + stepnum + " - successfully submitted");
		
									} catch (Exception e) {
		
										el.logExecution(
												"Step  " + stepnum + " unable to submit - even though it is visible - trying alternative " + xpath);
										Utilities.executionMsgCache.put(testCaseResultsId, "Step number: " + stepnum + " - submitting using alternative .....");
//										runIssues = ";" + runIssues + "Step  " + stepnum + " unable to submit - trying alternative " + xpath;
										long exec_startTime = System.currentTimeMillis();
										((JavascriptExecutor) driver).executeScript("arguments[0].click();", we);
										ts_execStatus = determineExecStatus(exec_startTime,threshold);
										el.logExecution("Step  " + stepnum + " successfully submitted alternative" + xpath);
										Utilities.executionMsgCache.put(testCaseResultsId, "Step number: " + stepnum + " - successfully submitted alternative");
//										runIssues = ";" + runIssues + "Step  " + stepnum + " successfully submitted alternative" + xpath;
		
									}
								} else {
		
									el.logExecution("Step  " + stepnum + " submitting - it is not visible " + xpath);
									Utilities.executionMsgCache.put(testCaseResultsId, "Step number: " + stepnum + " - submitting - it is not visible");
//									runIssues = ";" + runIssues + "Step  " + stepnum + " submitting - it is not visible " + xpath;
									long exec_startTime = System.currentTimeMillis();
									((JavascriptExecutor) driver).executeScript("arguments[0].click();", we);
									ts_execStatus = determineExecStatus(exec_startTime,threshold);
									el.logExecution("Step  " + stepnum + " successfully submitted " + xpath);
									Utilities.executionMsgCache.put(testCaseResultsId, "Step number: " + stepnum + " - successfully submitted");
//									runIssues = ";" + runIssues + "Step  " + stepnum + " successfully submitted " + xpath;
		
								}
							} else if (action.equalsIgnoreCase("upload")) {
								
								String fileName = Utilities.getInputFilePath(companyId, testCaseId, testData);
								el.logExecution("Step  " + stepnum + " file upload " + fileName + " for " + xpath);
								Utilities.executionMsgCache.put(testCaseResultsId, "Step  " + stepnum + " file upload " + fileName );

								long exec_startTime = System.currentTimeMillis();
								we.sendKeys(fileName);
								ts_execStatus = determineExecStatus(exec_startTime,threshold);
								el.logExecution(
										"Step  " + stepnum + " file upload successful " + fileName + " for " + xpath);
								Utilities.executionMsgCache.put(testCaseResultsId, "Step number: " + stepnum + " file upload successful");
		
							} else if (action.equalsIgnoreCase("clear")) {
		
								el.logExecution("Step  " + stepnum + " clearing text field " + xpath);
								Utilities.executionMsgCache.put(testCaseResultsId, "Step number: " + stepnum + " clearing text field ");
								long exec_startTime = System.currentTimeMillis();
								we.clear();
//								we.sendKeys("");
								we.sendKeys(Keys.CONTROL + "a"); // Select all text
								we.sendKeys(Keys.DELETE);
								ts_execStatus = determineExecStatus(exec_startTime,threshold);
								el.logExecution("Step  " + stepnum + " successfully cleared text field " + xpath);
								Utilities.executionMsgCache.put(testCaseResultsId, "Step number: " + stepnum + " successfully cleared text field");
		
							} else if ((action.equalsIgnoreCase("enter") || action.equalsIgnoreCase("clear & enter")) && inputType != null && inputType.equals("date")) {
								enterRightDate(stepnum, testData, we, xpath, testStepId);
							} else if ((action.equalsIgnoreCase("enter") || action.equalsIgnoreCase("clear & enter")) && inputType != null && inputType.equals("datetime-local")) {	
								enterRightDateTimeLocal(stepnum, testData, we, xpath, testStepId);
							} else if(action.equalsIgnoreCase("clear & enter")) {

								el.logExecution("Step  " + stepnum + " clearing text field " + xpath);
								Utilities.executionMsgCache.put(testCaseResultsId, "Step number: " + stepnum + " clearing text field ");
								long exec_startTime = System.currentTimeMillis();
								we.clear();
								we.sendKeys(Keys.CONTROL + "a"); // Select all text
								we.sendKeys(Keys.DELETE);
								ts_execStatus = determineExecStatus(exec_startTime,threshold);
								el.logExecution("Step  " + stepnum + " successfully cleared text field " + xpath);
								Utilities.executionMsgCache.put(testCaseResultsId, "Step number: " + stepnum + " successfully cleared text field ");
								String readonly = we.getAttribute("readonly");
								if (readonly == null || (readonly != null && readonly.equals("false"))) {
									el.logExecution(
											"Step  " + stepnum + " entering " + testData + " to  text field " + xpath);
									Utilities.executionMsgCache.put(testCaseResultsId, "Step number: " + stepnum + " - entering " + testData + " to  text field ");
									try {
										getElementAttrs(we);
										((JavascriptExecutor) driver).executeScript("arguments[0].value = '';", we);
										getElementAttrs(we);
										((JavascriptExecutor) driver).executeScript("arguments[0].removeAttribute('value');", we);
										getElementAttrs(we);
										we.sendKeys(testData);
										Utilities.executionMsgCache.put(testCaseResultsId, "Step number: " + stepnum + " - successfully entered the data to text field");
									} catch (Exception e) {
										JavascriptExecutor js = (JavascriptExecutor) driver;
										js.executeScript("arguments[0].removeAttribute('disabled');", we);
										
										WebElement parentElement = we.findElement(By.xpath(".."));

										((JavascriptExecutor) driver).executeScript("arguments[0].removeAttribute('disabled');", parentElement);

										((JavascriptExecutor) driver).executeScript(
									            "arguments[0].classList.remove('disabled');", parentElement
									        );
										try {
											we.clear();
											we.sendKeys(testData);
										} catch (Exception e2) {
											el.logExecution(
													"Step  " + stepnum + " send keys failed even after we removed disabled from text field "  + xpath);
											el.logExecution(
													"Step  " + stepnum + " send keys failed entering " + testData + " to  text field " + xpath);
											
											if(attempt < 3) {
												continue;
											} else {
												throw e;
											}
										}
										el.logExecution(
												"Step  " + stepnum + " send keys was successful after removing disabled and entered " + testData + " to  text field " + xpath);
									}
									String enteredValue = null;
									try {
										enteredValue = we.getAttribute("value");
									} catch(Exception e) {
										el.logExecution(e.getMessage());
										el.logExecution(getStackTraceAsString(e));
										el.logExecution(
												"Step  " + stepnum + " looks like the page has transistioned based on this input - check if this is expected behaviour " + testData + " to date field " + xpath);
									}
									if(inputType != null && inputType.equals("date") && !testData.equals(enteredValue) ) {
										el.logExecution(
												"Step  " + stepnum + " input type data mismatch " + testData + " to date field " + xpath);
										JavascriptExecutor js = (JavascriptExecutor) driver;
										js.executeScript("arguments[0].removeAttribute('onkeydown')", we);
										String formattedDate = java.time.LocalDate.parse(testData).format(DateTimeFormatter.ofPattern("dd-MM-yyyy"));
										we.sendKeys(formattedDate);
									}
									ts_execStatus = determineExecStatus(exec_startTime,threshold);
									el.logExecution("Step  " + stepnum + " successfully entered " + testData
											+ " to  text field - the entered value is : " + enteredValue);
								} else {
									el.logExecution("Step  " + stepnum + " entering " + testData
											+ "to (readonly) text field " + xpath);
									((JavascriptExecutor) driver).executeScript("arguments[0].value='" + testData + "';", we);
									ts_execStatus = determineExecStatus(exec_startTime,threshold);
									el.logExecution("Step  " + stepnum + " successfully entered " + testData
											+ "to (readonly) text field " + xpath);
								}
								
							} else if (action.equalsIgnoreCase("enter")) {
		
								String readonly = we.getAttribute("readonly");
								if (readonly == null || (readonly != null && readonly.equals("false"))) {
									el.logExecution(
											"Step  " + stepnum + " entering " + testData + " to  text field " + xpath);
									Utilities.executionMsgCache.put(testCaseResultsId, "Step number: " + stepnum + " - entering " + testData + " to  text field ");
									long exec_startTime = System.currentTimeMillis();
									try {
										getElementAttrs(we);
										((JavascriptExecutor) driver).executeScript("arguments[0].value = '';", we);
										getElementAttrs(we);
										((JavascriptExecutor) driver).executeScript("arguments[0].removeAttribute('value');", we);
										getElementAttrs(we);
										we.sendKeys(testData);
										Utilities.executionMsgCache.put(testCaseResultsId, "Step number: " + stepnum + " - successfully entered the data to text field");
									} catch (Exception e) {
										JavascriptExecutor js = (JavascriptExecutor) driver;
										js.executeScript("arguments[0].removeAttribute('disabled');", we);
										
										WebElement parentElement = we.findElement(By.xpath(".."));

										((JavascriptExecutor) driver).executeScript("arguments[0].removeAttribute('disabled');", parentElement);

										((JavascriptExecutor) driver).executeScript(
									            "arguments[0].classList.remove('disabled');", parentElement
									        );
										try {
											we.clear();
											we.sendKeys(testData);
										} catch (Exception e2) {
											el.logExecution(
													"Step  " + stepnum + " send keys failed even after we removed disabled from text field "  + xpath);
											el.logExecution(
													"Step  " + stepnum + " send keys failed entering " + testData + " to  text field " + xpath);
											
											if(attempt < 3) {
												continue;
											} else {
												throw e;
											}
										}
										el.logExecution(
												"Step  " + stepnum + " send keys was successful after removing disabled and entered " + testData + " to  text field " + xpath);
									}
									String enteredValue = null;
									try {
										enteredValue = we.getAttribute("value");
									} catch(Exception e) {
										el.logExecution(e.getMessage());
										el.logExecution(getStackTraceAsString(e));
										el.logExecution(
												"Step  " + stepnum + " looks like the page has transistioned based on this input - check if this is expected behaviour " + testData + " to date field " + xpath);
									}
									if(inputType != null && inputType.equals("date") && !testData.equals(enteredValue) ) {
										el.logExecution(
												"Step  " + stepnum + " input type data mismatch " + testData + " to date field " + xpath);
										JavascriptExecutor js = (JavascriptExecutor) driver;
										js.executeScript("arguments[0].removeAttribute('onkeydown')", we);
										String formattedDate = java.time.LocalDate.parse(testData).format(DateTimeFormatter.ofPattern("dd-MM-yyyy"));
										we.sendKeys(formattedDate);
									}
									ts_execStatus = determineExecStatus(exec_startTime,threshold);
									el.logExecution("Step  " + stepnum + " successfully entered " + testData
											+ " to  text field - the entered value is : " + enteredValue);
								} else {
									el.logExecution("Step  " + stepnum + " entering " + testData
											+ "to (readonly) text field " + xpath);
//									runIssues = ";" + runIssues + "Step  " + stepnum + " entering " + testData
//											+ "to (readonly) text field " + xpath;
									long exec_startTime = System.currentTimeMillis();
									((JavascriptExecutor) driver).executeScript("arguments[0].value='" + testData + "';", we);
									ts_execStatus = determineExecStatus(exec_startTime,threshold);
									el.logExecution("Step  " + stepnum + " successfully entered " + testData
											+ "to (readonly) text field " + xpath);
//									runIssues = ";" + runIssues + "Step  " + stepnum + " successfully entered " + testData
//											+ "to (readonly) text field " + xpath;
								}
		
							} else if (action.equalsIgnoreCase("validatePartial") || action.equalsIgnoreCase("validateExact")) {
								if (validateIn == null || validateIn.equals("SCREEN") || validateIn.equals("OnScreen") ||
										validateIn.equals("DependentonOtherTestScenario") || validateIn.equals("Search") || 
										validateIn.equals("UseVariableFromTheScenario")) {
									String subAction = (String)step.get("subAction");
									Utilities.executionMsgCache.put(testCaseResultsId, "Step number: " + stepnum + " - validating .....");
									status = screenAssertion(we, stepnum, testData, xpath, flow, status,action, subAction);						
								} else if (validateIn.equals("InFile") || validateIn.equals("InDownloadedFile") ||
										validateIn.equals("InRefrenceFile")) {
									String filename = (String) step.get("filename");
									String fileField = (String) step.get("fileField");
									if(validateIn.equals("InDownloadedFile") && filename.contains(AppProperties.validatedelimiter)) {
										el.logExecution("Processing file name for Downloaded files " + filename);
										String[] names = filename.split(AppProperties.validatedelimiter);
										filename = names[0];
										el.logExecution("Actual filename is : " + filename);
										if((filename.toLowerCase().endsWith("xlsx") || filename.toLowerCase().endsWith("csv")) &&
												names.length == 3) {
											fileField = names[1] + AppProperties.validatedelimiter + names[2];
											el.logExecution("Constructed File field for xlsx or csv is : " + fileField);
										}
									}
									Utilities.executionMsgCache.put(testCaseResultsId, "Step number: " + stepnum + " - validating from file .....");
									status = fileAssertion(we, stepnum, testData, xpath, flow, status, 
											filename,fileField,testCaseId,companyId,testCaseResultsId,
											runonceIndex, validateIn, step);
									if(varMap != null && varMap.containsKey("textInFile")) {
										testData = varMap.get("textInFile");
										varMap.remove("textInFile");
									}
								} else if(validateIn.equalsIgnoreCase("Regex")) {
									String regex = (String)step.get("regex");
									Utilities.executionMsgCache.put(testCaseResultsId, "Step number: " + stepnum + " - validating regex .....");
									status = regexAssertion(we, stepnum, testData, xpath, flow, status, regex);
								} else if(validateIn.equalsIgnoreCase("Date")) {
									Utilities.executionMsgCache.put(testCaseResultsId, "Step number: " + stepnum + " - validating date .....");
									status = dateAssertion(we, stepnum, testData, xpath, flow, status);
								} else if(validateIn.equalsIgnoreCase("DB")) {
									String query = (String)step.get("DBQuery");
									GetUserDbConn udb = new GetUserDbConn();
									int tsid = (int) step.get("idtest_step");
									testData = udb.getCustomerDbConn(msc, query, tsid,el);
									Utilities.executionMsgCache.put(testCaseResultsId, "Step number: " + stepnum + " - validating from DB .....");
									status = justAssert(we, stepnum, testData, xpath, flow, status, null);	
								} else if(validateIn.equalsIgnoreCase("InAPi")) {
									
									Object obj = callAPI(step, randomKey, validateIn);
									
									if(obj == null) {
										testData = null;
									} else {
										testData = obj.toString();
									}
									
									Utilities.executionMsgCache.put(testCaseResultsId, "Step number: " + stepnum + " - validating from API .....");
									status = justAssert(we, stepnum, testData, xpath, flow, status, null);	

								} else if (validateIn.equalsIgnoreCase("InGoogleSheet")) {
									String spreadsheetId = (String)step.get("filename");
									String range = (String)step.get("fileField");
									try {
										testData =  GoogleSheets.getCellValue(spreadsheetId, range, companyId);
									} catch (Exception e) {
										el.logExecution(e);
									}
									Utilities.executionMsgCache.put(testCaseResultsId, "Step number: " + stepnum + " - validating from Google Sheet .....");
									status = justAssert(we, stepnum, testData, xpath, flow, status, null);
								} else if(validateIn.equalsIgnoreCase("FromEmail")) {
									String email = (String)step.get("customerEmail");
									String pwd = (String)step.get("customerPassword");
									String select = (String)step.get("EmailSelectionCriteria");
									String filter = (String)step.get("EmailFilter");
									int tsid = (int) step.get("idtest_step");
									MailAccess ma = new MailAccess();
									testData =  ma.getFromEmail(tsid, email, pwd, select, filter, el,msc);
									Utilities.executionMsgCache.put(testCaseResultsId, "Step number: " + stepnum + " - validating from EMAIL .....");
									status = justAssert(we, stepnum, testData, xpath, flow, status, null);
								} else if(validateIn.equalsIgnoreCase("FromOutlook")) {
									String select = (String)step.get("EmailSelectionCriteria");
									String filter = (String)step.get("EmailFilter");
									String src = null;
									if(testData != null && testData.startsWith("src:")) {
										src = ApiAccess.callApiForDesktop(AppProperties.outlookaccessurl, 
												select, filter, el, varMap);
										testData = testData.substring(4,testData.length());
									} else {
										testData = ApiAccess.callApiForDesktop(AppProperties.outlookaccessurl, 
												select, filter, el, varMap);
									}
									Utilities.executionMsgCache.put(testCaseResultsId, "Step number: " + stepnum + " - validating from Outlook.....");
									status = justAssert(we, stepnum, testData, xpath, flow, status, src);
								} else if(validateIn.equalsIgnoreCase("InStreamedFile")) {
									String streamurl = (String)step.get("filename");
									if(streamurl.startsWith(AppProperties.delimiter) && streamurl.endsWith(AppProperties.delimiter)) {
										streamurl = streamurl.substring(1, streamurl.length() - 1);
										streamurl = we.getAttribute(streamurl);
									}
									el.logExecution("The streaming URL is :" + streamurl);
									if(streamurl != null && streamurl.contains(AppProperties.delimiter)) {
										String var = streamurl.substring(streamurl.indexOf(AppProperties.delimiter),streamurl.lastIndexOf(AppProperties.delimiter)+1);
										String value = (String)varMap.get(var);
										el.logExecution("found variable in url:" + var);
										el.logExecution("Variable will be replaced with value:" + value);
										streamurl = streamurl.replace(var, value);
										el.logExecution("New URL after value has been replaced:" + streamurl);
									}
									Utilities.executionMsgCache.put(testCaseResultsId, "Step number: " + stepnum + " - validating from streamed files .....");
									if(streamurl.contains(".xml")) {
										status = ApiHandler.streamValidateXML(streamurl, testData, el);
									} else {
										status = ApiHandler.streamValidatePDF(streamurl, testData, el);
									}
								}
							} else if (action.equalsIgnoreCase("Properties") ) {
								boolean propTrue = false;
								String subAction = (String)step.get("subAction");
								Utilities.executionMsgCache.put(testCaseResultsId, "Step number: " + stepnum + " - validating with condition " + subAction);
								if(subAction.equalsIgnoreCase("validateActive")) {
									if(we.isEnabled()) {
										propTrue = true;
									}
								} else if(subAction.equalsIgnoreCase("validateChecked")) {
									if(we.isSelected()) {
										propTrue = true;
									}
								} else if(subAction.equalsIgnoreCase("validateEditable")) {
									if(we.isEnabled()) {
										propTrue = true;
									}
								} else if(subAction.equalsIgnoreCase("validatefcolor")) {
									String color = (String) step.get("color");
									String attribute = "color";
									String result = colorAssertion(we, stepnum, xpath, status, color, flow, attribute);
									if(result.equalsIgnoreCase("PASS")) {
										propTrue = true;
									}
								} else if(subAction.equalsIgnoreCase("validatebgcolor")) {
									String color = (String) step.get("bgcolor");
									String attribute = "background-color";
									String result = colorAssertion(we, stepnum, xpath, status, color, flow, attribute);
									if(result.equalsIgnoreCase("PASS")) {
										propTrue = true;
									}
								} else if(subAction.equalsIgnoreCase("validateFontFam")) {
									String fontFamily = we.getCssValue("font-family");
									el.logExecution("Step  " + stepnum +  " " + subAction
											+ " the font family that was found is " +  fontFamily);
									if(fontFamily != null && testData != null && 
											fontFamily.toLowerCase().contains(testData.toLowerCase())) {
										propTrue = true;
									}
								} else if(subAction.equalsIgnoreCase("validateFontSize")) {
									String fontSize = we.getCssValue("font-size");
									el.logExecution("Step  " + stepnum +  " " + subAction
											+ " the font size that was found is " +  fontSize);
									if(fontSize != null && testData != null && fontSize.equals(testData)) {
										propTrue = true;
									}
								} else if(subAction.equalsIgnoreCase("validateHref")) {
									String href = we.getAttribute("href");
									el.logExecution("Step  " + stepnum +  " " + subAction
											+ " the href that was found is " +  href);
									if(href != null && testData != null && href.equals(testData)) {
										propTrue = true;
									}
								} else if(subAction.equalsIgnoreCase("validateSrc")) {
									String src = we.getAttribute("src");
									el.logExecution("Step  " + stepnum +  " " + subAction
											+ " the src that was found is " +  src);
									if(src != null && testData != null && src.equals(testData)) {
										propTrue = true;
									}	
								} else if(subAction.equalsIgnoreCase("validateCount")) {
									List<WebElement> elements = driver.findElements(By.xpath(xpath));
									if(elements != null) {
										String strSize = String.valueOf(elements.size());
										el.logExecution("Step  " + stepnum +  " " + subAction
												+ " the count that was found is " +  strSize);
										if(testData != null && strSize.equals(testData)) {
											propTrue = true;
										}
									} else {
										el.logExecution("Step  " + stepnum +  " " + subAction
												+ " no elements were found ");
									}
								} else if(subAction.equalsIgnoreCase("validatePageUrl")) {
									String pageURL = driver.getCurrentUrl();
									el.logExecution("Step  " + stepnum +  " " + subAction
											+ " the page url that was found is " +  pageURL);
									if(pageURL != null && testData != null && pageURL.equals(testData)) {
										propTrue = true;
									}
								} else if(subAction.equalsIgnoreCase("validateMulti")) {
									List<WebElement> elements = driver.findElements(By.xpath(xpath));
									elements = elements.stream()
							                .filter(element -> element.isDisplayed())
							                .collect(Collectors.toList());
									
									String[] testd = testData.split(AppProperties.delimiter);
									if(elements == null || elements.size() == 0) {
										el.logExecution("Step  " + stepnum +  " " + subAction
												+ " no elements were found  " );
									} else {
										el.logExecution("Step  " + stepnum +  " " + subAction
												+ " elements found are  " + elements.size());
									
										if(testData == null || testData.equals("")) {
											el.logExecution("Step  " + stepnum +  " " + subAction
													+ " test data is blank or null  " );
										} else {
											el.logExecution("Step  " + stepnum +  " " + subAction
													+ " number of values to be compared are  " + testd.length);
										
											if(elements != null && testd != null && elements.size() == testd.length) {
												el.logExecution("Step  " + stepnum +  " " + subAction
														+ " number of elements and values that need to be asserted match " );
											
												boolean allMatched = true;
												for(int j = 0; j < elements.size();j++) {
													WebElement element = elements.get(j);
													String text = element.getText();
													if(text != null && text.equals(testd[j])) {
														el.logExecution("Step  " + stepnum +  " " + subAction
																+ " element at index " + j + " with value " + text + 
																" matched the value " + testd[j] );										
													} else {
														el.logExecution("Step  " + stepnum +  " " + subAction
																+ " element at index " + j + " with value " + text + 
																" did not match the value " + testd[j] );
														allMatched = false;
													}										
												}
												propTrue = allMatched;											
											} else {
												el.logExecution("Step  " + stepnum +  " " + subAction
														+ " number of elements and values that need to be asserted do not match " );
											}
										}
									}
								} 
								if(subAction.equalsIgnoreCase("validateActive")){
									String outerHTML = we.getAttribute("outerHTML");
									if(outerHTML.contains("disabled")){
										propTrue = false;
									}
								}
								if(!propTrue) {
									if ((flow == null || flow.equals("Positive"))) {
										status = "FAIL";
										el.logExecution("Step  " + stepnum +  " " + subAction
												+ " looking to be true, but it is false and flow = " + flow );
					
									} else if (flow.equals("Optional")) {
										el.logExecution("Step  " + stepnum +  " " + subAction
												+ " looking to be true, but it is false and flow = " + flow );
									} else {
										el.logExecution("Step  " + stepnum +  " " + subAction
												+ " looking to be false, and it is false and flow = " + flow );
									}
								} else {
									if ((flow == null || flow.equals("Positive"))) {
										el.logExecution("Step  " + stepnum +  " " + subAction
												+ " looking to be true, and it is true and flow = " + flow );							
					
									} else if (flow.equals("Optional")) {
										el.logExecution("Step  " + stepnum +  " " + subAction
												+ " looking to be true, but it is true and flow = " + flow );
									} else {
										status = "FAIL";
										el.logExecution("Step  " + stepnum +  " " + subAction
												+ " looking to be false, but it is true and flow = " + flow );
									}
								}
							} else if (keyword.equalsIgnoreCase("DropDown") && 
									action.startsWith("By") || action.startsWith("Select")) {
								Select select = new Select(we);
		
								el.logExecution("Step  " + stepnum + " selecting " + action + " " + testData );
								Utilities.executionMsgCache.put(testCaseResultsId, "Step number: " + stepnum + " - selecting " + action + " " + testData);
								String id = null;
								String value = null;
								String text = testData;
								if(testData.contains(AppProperties.delimiter)) {
									id = testData.substring(0,testData.indexOf(AppProperties.delimiter));
									String wipData = testData.substring(AppProperties.delimiter.length(),testData.length());
									
									wipData = wipData.substring(wipData.indexOf(AppProperties.delimiter) + AppProperties.delimiter.length(),wipData.length());
									value = wipData.substring(0,wipData.indexOf(AppProperties.delimiter));
									wipData = wipData.substring(wipData.indexOf(AppProperties.delimiter) + AppProperties.delimiter.length(),wipData.length());
									text = wipData.substring(0,wipData.length());
								}
								
								if(action.equalsIgnoreCase("ById")) {
									int index = Integer.valueOf(id);
									select.selectByIndex(index);
								} else if(action.equalsIgnoreCase("ByValue")) {
									select.selectByValue(value);
								} else {
									select.selectByVisibleText(text);
								}
								
								el.logExecution(
										"Step  " + stepnum + " selecting " + action + " " +  testData + xpath);
							} else if (action.equalsIgnoreCase("dblClick")) {
								el.logExecution("Step  " + stepnum + " double click " + xpath);
								Utilities.executionMsgCache.put(testCaseResultsId, "Step number: " + stepnum + " - double clicking .... ");
								Actions actions = new Actions(driver);
								long exec_startTime = System.currentTimeMillis();								
								actions.doubleClick(we).perform();
								String script = "var evt = new MouseEvent('dblclick', {" +
						                "bubbles: true," +
						                "cancelable: true," +
						                "view: window" +
						                "});" +
						                "arguments[0].dispatchEvent(evt);";
						((JavascriptExecutor) driver).executeScript(script, we);
						
								ts_execStatus = determineExecStatus(exec_startTime,threshold);
								el.logExecution(
										"Step  " + stepnum + " double clicked " + xpath);	
							} else if (action.equalsIgnoreCase("DynamicText")) {
								el.logExecution("Step  " + stepnum + " Dynamic Text " + xpath);	
								Utilities.executionMsgCache.put(testCaseResultsId, "Step number: " + stepnum + " - Dynamic Text ");
								String textOnScreen = we.getText();
								String attrType = (String)we.getAttribute("type");
								if(attrType != null && attrType.equals("text")) {
									textOnScreen = we.getAttribute("value");
								}
								varMap.put(varName, textOnScreen);
								testData = textOnScreen;
								el.logExecution(
										"Step  " + stepnum + " Dynamic Text was successful" + xpath);
							} else if (action.equalsIgnoreCase("Search")) {
								el.logExecution("Step  " + stepnum + " Search on Page " + testData  + "found");	
							} else if (action.equalsIgnoreCase("dnddrag")) {
								el.logExecution("Step  " + stepnum + " Start Drag " + xpath  + "found");
								dragStart = we;
							} else if (action.equalsIgnoreCase("dnddrop")) {
								el.logExecution("Step  " + stepnum + " Dropping at " + xpath  + "found");
								dnd(dragStart, we);
							} else if (action.startsWith("Arrow")) {
								el.logExecution("Step  " + stepnum + " " + action + " " + xpath  + "found");
								if(action.equalsIgnoreCase("ArrowUp")) {
									we.sendKeys(Keys.ARROW_UP);
								} else if (action.equalsIgnoreCase("ArrowDown")) {
									we.sendKeys(Keys.ARROW_DOWN);
								}
								el.logExecution("Step  " + stepnum + " " + action + " " + xpath  + "completed successfully");
							} else if (action.startsWith("rightclick")) {
								el.logExecution("Step  " + stepnum + " " + action + " " + xpath  + "found");
								Actions actions = new Actions(driver);
								// Perform right-click action
						        actions.contextClick(we).build().perform();
								el.logExecution("Step  " + stepnum + " " + action + " " + xpath  + "completed successfully");
							} else if (action.equalsIgnoreCase("Scroll")) {
								Utilities.executionMsgCache.put(testCaseResultsId, "Step number: " + stepnum + " - Scrolling to the element");
								el.logExecution("Step  " + stepnum + " Scrolling to the element " + xpath  );
								int count = 0;
								int elementCount = 0;
								boolean cont = true;
								do {
									count = elementCount;
									List<WebElement> elements = driver.findElements(By.xpath("//a[@class='ng-binding']"));
									elementCount = elements.size();
									el.logExecution("Step  " + stepnum + " element count " + elementCount + " for element " + xpath  );
									if(count <= 500 && count < elementCount) {
										cont = true;
										WebElement element = elements.get(elementCount - 1);
										((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);", element);
										try {
											Thread.sleep(10000);
										} catch (Exception et) {
											el.logExecution("thread3 sleeping for 10000 interupted");
										}
									} else {
										el.logExecution("Step  " + stepnum + " break out at element count " + elementCount + " for element " + xpath  );
										
										// find the first child element
										WebElement firstChild = driver.findElement(By.xpath("HTML/BODY/DIV/DIV[3]/PRODUCT-DECK/SECTION/DIV[2]/DIV[4]/DIV/DIV/DIV/DIV[2]/DIV/DIV[51]/PRODUCT-TEMPLATE/DIV/DIV[4]/DIV/A"));
										printWebElementInfo(firstChild);
										// find the second child element
										WebElement secondChild = driver.findElement(By.xpath("HTML/BODY/DIV/DIV[3]/PRODUCT-DECK/SECTION/DIV[2]/DIV[4]/DIV/DIV/DIV/DIV[2]/DIV/DIV[51]/PRODUCT-TEMPLATE/DIV/DIV[4]/DIV[2]/DIV/SPAN/BUTTON/SPAN"));
										printWebElementInfo(secondChild);
										// find the common ancestor element
										//WebElement ancestor = firstChild.findElement(By.xpath("ancestor::*[.//*[.='" + secondChild.getText() + "']]"));
										WebElement ancestor = driver.findElement(By.xpath("//*[.//*[.='" + firstChild.getText() + "']]//*[.//*[.='" + secondChild.getText() + "']]"));
										HashMap output = new HashMap();
										printWebElementInfo(ancestor);			
										break;
									}
								} while (cont);
								Utilities.executionMsgCache.put(testCaseResultsId, "Step number: " + stepnum + " - Scrolled to the element");
								el.logExecution("Step  " + stepnum + " Scrolled to the element " + xpath  );
							} else if (action.equals("DNext")) {
								String pathForCompare = xpath;
								for(int j = d0index; j<i;j++) {
									JSONObject stepsObj = (JSONObject)steps.get(j + 2);
									String pathtoUpdate = (String)stepsObj.get("Object_Xpath");
									String oldPath = pathtoUpdate;
									fixDistance(pathtoUpdate, pathForCompare, j, i, steps);
									pathForCompare = oldPath;
								}
								
								i = d0index;
							} else if (action.equals("Download")) {
							} else if (action.equals("Email") && keyword.equals("Send")) {
								sendEmail(step, el, msc);
							} else if (action.equalsIgnoreCase("Hover")) {
								if(locationStrategy != null && !locationStrategy.equalsIgnoreCase("XY")) {
									Actions actions = new Actions(driver);
									actions.moveToElement(we).perform();
								}
							} else if (action.equalsIgnoreCase("javascript")) {
								String scriptFileName = (String)step.get("scriptfile");
//								String scriptFileName = "add.txt";
								String filePath = Utilities.getScriptFilePath(companyId, scriptFileName);
								JSHandler jsh = new JSHandler();
							    String script = jsh.readJavaScriptFromFile(filePath);
							    Object result = null;
							    if (script != null) {
							    	if (testData == null || testData.equals("")) {
							    		result = ((JavascriptExecutor)driver).executeScript("return " + script);
							    	} else {
							    		List<String> values = Utilities.replaceJSParams(testData);
							    		Object[] valuesArray = values.toArray(new Object[0]);
							    		result = jsh.executeJavaScriptFunction(driver, script, valuesArray);
							    	}
							    	
							    	testData = result.toString();
							    	
							    	if(varName != null && !varName.equals("")) {
										randomMap.put(varName, testData);
									} else {
										randomMap.put(testData, testData);
									}
							    }
							} else if (action.equalsIgnoreCase("CommandLine")) {
								String scriptFileName = (String)step.get("cmdfile");
//								String scriptFileName = "add.txt";
								String filePath = Utilities.getBatchFilePath(companyId, scriptFileName);
								String deploymentType = AppProperties.deploymentType;
								if(deploymentType != null && deploymentType.equalsIgnoreCase("OnPrem")) {
							        // Create a ProcessBuilder object with the command to run the batch file
							        ProcessBuilder processBuilder = new ProcessBuilder("cmd.exe", "/c", filePath);
							        
							        // Start the process
							        Process process = processBuilder.start();
							        
							        // Read the output of the process (if needed)
							        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
							        String line;
							        while ((line = reader.readLine()) != null) {
							        	el.logExecution(line);
							        }
							        
							        // Wait for the process to finish and get the exit code
							        try {
							            int exitCode = process.waitFor();
										el.logExecution("Process exited with code " + exitCode + "And process completed successfully");
							        } catch (InterruptedException e) {
							            e.printStackTrace();
							        }
								} else {
									el.logExecution("Can't execute as trying to run on "+ AppProperties.deploymentType +" environment");
								}
							} else if (action != null && action.equalsIgnoreCase("AlertText")) {
								el.logExecution("Reading Alert Text");
								String alertText = alert.getText();
								el.logExecution("Alert Text : " + alertText);
								if(varName != null && !varName.equals("")) {
									randomMap.put(varName, alertText);
									el.logExecution("Updating " + varName  + " to have value  : " + alertText);
								} 
							} else if (action != null && action.equalsIgnoreCase("AlertOK")) {
								el.logExecution("Clicking Ok on alert");
								alert.accept();
								el.logExecution("Clicking Ok on alert was successful"); 
							} else if(keyword != null && keyword.equalsIgnoreCase("Inline") && 
									action != null && action.equalsIgnoreCase("Test")) {
								JSONObject inlinejson = msc.getTestStepsFromDBByTestCaseId(Integer.valueOf(testData), 
										true,false);
								Utilities.executionMsgCache.put(testCaseResultsId, "Step number: " + stepnum + " - Starting to execute inline test case");
								el.logExecution("Starting to execute test case " + testData);
								inline = true;
								el.logExecution("changed inline to : " + inline);
								this.test(inlinejson, msc1, companyId, randomKey, results, 
										runonceIndex, envUrl, testCaseResultsId, testCaseDetails, 
										isTCMultiRun, mrIndexInc, tester, synchScenario, 
										synchResultid, synchObj, ssForAllElement, analyseFailures, 
										baselineRun, predecessor);
								inline = false;
								el.logExecution("changed inline to : " + inline);
								Utilities.executionMsgCache.put(testCaseResultsId, "Step number: " + stepnum + " - Finished executing inline test case");
								el.logExecution("Finished executing test case " + testData);
							} else if (action != null && action.equalsIgnoreCase("Test")) {
								String res = callAPI(step, randomKey, action);
								String[] retres = res.split(AppProperties.delimiter);
								status = retres[0];
								if(retres.length == 1) {
									testData = "There were no tests";
								} else {
									testData = retres[1];
								}
								el.logExecution("The status of assertion is   : " + status);
							} else if (keyword != null && keyword.equals("Clipboard")) {
							} else if (keyword != null && keyword.equalsIgnoreCase("Mouse")) {
								// Create an Actions object
						        Actions actions = new Actions(driver);
						        if(action != null && action.equalsIgnoreCase("Slide")) {
						        	String coords = testData;
						        	String xcoord = coords.substring(1,coords.indexOf(","));
						        	String ycoord = coords.substring(coords.indexOf(",")+1, coords.length() -1);
						        	int xco = Integer.valueOf(xcoord);
						        	int yco = Integer.valueOf(ycoord);
						        	
						        	el.logExecution("pixels to move along X axis : " + xco);
							        el.logExecution("pixels to move along Y axis : "  + yco);
						        	
						        	actions.clickAndHold(we)   // Click and hold the slider handle
						               .moveByOffset(xco, yco)         // Move 100 pixels to the right (adjust as needed)
						               .release()                     // Release the handle
						               .build()
						               .perform();
						        } else {

						        	Utilities.executionMsgCache.put(testCaseResultsId, "Step number: " + stepnum + " - Clicking with Mouse");
							        // Move to the element and perform a click
						        	el.logExecution("clicking with mouse : " );
							        actions.moveToElement(we).click().perform();
							        el.logExecution("clicked with mouse" );
						        }
							} else {
								if(locationStrategy != null && 
										locationStrategy.equalsIgnoreCase("XY")) {
									//ignore - as XY by default already clicks
								} else {
								el.logExecution("Action : " + action + " has not been handled");
//								runIssues = ";" + runIssues + "Action : " + action + "has not been handled";
								status = "FAIL";
								}
							}	
							if(retrialStep != stepnum) {
								inRetrial = false;
								retrialStep= -1;
							}
						}
					} catch (Exception e) {
						try {
							String storedxpath = (String) step.get("Object_Xpath");
							By locator = By.xpath(storedxpath);
							smartFindElement(driver, locator, 10, el);
						} catch(Exception se) {
							el.logExecution("Error in checking for page load");
						}
						el.logExecution(e.getMessage());
						el.logExecution(getStackTraceAsString(e));
						if(e instanceof java.lang.ArrayIndexOutOfBoundsException) {
							if(e.getMessage().equalsIgnoreCase("Tab Index is out of Bound")) {
								errorCode = "E0012";
								runIssues = "Tab index is out of bound";
							}
						}
						if(e instanceof org.openqa.selenium.ElementNotInteractableException) {
							errorCode = "E0015";
							runIssues = "Element is not in interactable state";
						}
						if(e instanceof java.lang.IllegalArgumentException || e instanceof org.openqa.selenium.NoSuchSessionException ||
								e instanceof org.openqa.selenium.InvalidArgumentException) {
							status = "FAIL";
							el.logExecution(e);
							e.printStackTrace();
						} else {
							if(temporaryFoundBy != null) {
								errorCode = "E0014";
								runIssues = "Element Not Found - The found element did not match the outerhtml of the target element, so alternate locators were used and they couldnt locate the element.";
							}
							try {
								if(errorCode.equals("")) {
									Utilities.executionMsgCache.put(testCaseResultsId, "Step number: " + stepnum + " - Checking for the previous passed or recorded outerhtml in the pagesource");
									el.logExecution("Started checking for the previous passed or recorded outerhtml in the pagesource");
									String pageSource = driver.getPageSource();
									executionUtility.getErrorCode(msc, testStepId, testCaseId, outerhtml, pageSource, this);
									el.logExecution("Ended checking for the previous passed or recorded outerhtml in the pagesource");
								}
							} catch (Exception ea) {
								
							}
							el.logExecution("Did not find xpath " + xpath + " STATUS = FAIL");
							el.logExecution("Is it in retrial " + inRetrial);
							JSONObject prevStep= null;
							if(!inRetrial && i > 0 && flow != null && !flow.equalsIgnoreCase("Optional")) {
								el.logExecution("Checking to see if previous step did actually complete " );
								prevStep = (JSONObject) steps.get((i - 1) + 2);
								String prevXpath = (String) prevStep.get("Object_Xpath");
								prevXpath = processXPath(prevXpath);
								el.logExecution("Previous Element is --> " + prevXpath );
								boolean prevEleFound = false;
								try {
									WebElement prevwe = driver.findElement(By.xpath(prevXpath));
									prevEleFound = true;
									int prevTestStepId = (int) prevStep.get("idtest_step");
									if(prevStep.get("waittime") != null) {
										int prevWaitTime = (int)prevStep.get("waittime");
										if(prevWaitTime == 0) {
											msc.updateTestStepBeforeAndTime(prevTestStepId, "Before", 5);
											el.logExecution("Added a 5s delay automatically --> " + prevXpath );
										}
									}
									el.logExecution("Previous Element is found --> " + prevXpath );
								} catch(org.openqa.selenium.NoSuchElementException preve) {
									el.logExecution("Previous Element NOT found --> " + prevXpath );
									el.logExecution(e.getMessage());
									el.logExecution(getStackTraceAsString(e));
								}
									
								if(prevEleFound && step.get("Flow") != null && 
										!((String)step.get("Flow")).equalsIgnoreCase("Optional")) {
									endTime = System.currentTimeMillis();
									duration = (endTime - startTime)/ 1000.0;
									msc.writeTestStepStatusToDB(testCaseResultsId, stepnum, 
											(String) step.get("Page_Name"),
											(String) step.get("Page_Description"), xpath, keyword, action, 
											(String) step.get("Flow"), testData,
											varName, "FAIL", ssfilename, ssfilenameAF, testStepId, "", duration, 
											validateIn, runIssues, errorCode, "FAIL", foundOuterhtml, foundby, 
											foundelements, devtools);
									i = i -1;
									inRetrial = true;
									retrialStep = (int) prevStep.get("Step_Number");
									Utilities.executionMsgCache.put(testCaseResultsId, "Step number: " + stepnum + " - Retrying the previous step");
									el.logExecution("Retrying the previous step ");
									continue;
								} else {
									e.printStackTrace();
									status = "FAIL";
								}
							} else {
								e.printStackTrace();
								status = "FAIL";
							}	
						}
					} 	
				}
				
				if (status.equals("STARTED")) {
					status = "PASS";
					el.logExecution(" STATUS = PASS");
				}
				
				endTime = System.currentTimeMillis();
				duration = (endTime - startTime)/ 1000.0;
				startTime = endTime;
				ssfilename = "";
	
				el.logExecution("Taking screen shot Before updating step result");
				if(multiRun) {
					ssfilename = Utilities.getSSFileName(testCaseId, testStepId, testCaseResultsId, stepnum, 
							companyId, "MR" + mrIndex +"afterAction");
				} else {
					ssfilename = Utilities.getSSFileName(testCaseId, testStepId, testCaseResultsId, stepnum, 
							companyId, "afterAction");
				}
					String ssloc = getSSFullLoc(ssfilename, testCaseId,testCaseResultsId , 
							companyId);
					Utilities.executionMsgCache.put(testCaseResultsId, "Step number: " + stepnum + " - Taking screen shot Before updating step result");
					changeBGtakess(we, ssloc, status,analyseFailures,baselineRun, 
							el, locationStrategy, screenX, screenY, createsAlert, keyword);
					el.logExecution("Finished Taking screen shot Before updating step result");
				
				if(step.get("type") != null && ((String)step.get("type")).equalsIgnoreCase("password")) {
					testData = "********";
				} 
				
				el.logExecution("Updating test step status");
				msc.writeTestStepStatusToDB(testCaseResultsId, stepnum, 
						(String) step.get("Page_Name"),
						(String) step.get("Page_Description"), xpath, keyword, action, 
						(String) step.get("Flow"), testData,
						varName, status, ssfilename, ssfilenameAF, testStepId, "", duration, 
						validateIn, runIssues, errorCode, ts_execStatus, 
						foundOuterhtml, foundby, foundelements, devtools);
				el.logExecution("Completed Updating test step status");
				
				long tc_endTime = System.currentTimeMillis();
				long tc_duration = tc_endTime - tc_startTime;
				msc.updateTestCaseStatusToDB(testCaseResultsId, "RUNNING", tc_duration, tc_execStatus, companyId);
				el.logExecution("Completed Updating test case status");
				
				//runIssues = "";
				if(ts_execStatus.equals("FAIL")) {
					tc_execStatus = "FAIL";
				}
				int tcrid = el.getTcrid();
				el.logExecution("Started storing page source");
				Utilities.executionMsgCache.put(testCaseResultsId, "Step number: " + stepnum + " - storing page source ......");
				String pageSourceFile = AppProperties.datadirectory + "\\" + companyId + "\\" + testCaseId + "\\" + tcrid + "\\" + "pagesource" + "\\" + stepnum + ".txt";
				try {
					//ExecutionUtility executionUtility = new ExecutionUtility();
					executionUtility.getPageSource(driver, pageSourceFile);
				} catch(Exception e) {
					
				}
				el.logExecution("Finished storing page source");
				if (((flow == null || flow.equalsIgnoreCase("Positive"))  && status.equalsIgnoreCase("FAIL")) || 
						(((flow == null || flow.equalsIgnoreCase("Negative"))  && status.equalsIgnoreCase("PASS")))) {
					
					if(errorCode.equals("")) {
						errorCode = "E0000";
						runIssues = "Element Not Found - Does not match any of the known causes";
					}
					el.logExecution("writin test step status update");
					msc.writeTestStepStatusToDB(testCaseResultsId, stepnum, 
							(String) step.get("Page_Name"),
							(String) step.get("Page_Description"), xpath, keyword, action, 
							(String) step.get("Flow"), testData,
							varName, status, ssfilename, ssfilenameAF, testStepId, "", duration, 
							validateIn, runIssues, errorCode, ts_execStatus,
							foundOuterhtml, foundby, foundelements,devtools);
					el.logExecution("completed test step status update");
				}
				
				if (((flow == null || flow.equalsIgnoreCase("Positive"))  && status.equalsIgnoreCase("FAIL")) || 
						(((flow == null || flow.equalsIgnoreCase("Negative"))  && status.equalsIgnoreCase("PASS")))) {
					tc_status = "FAIL";
					el.logExecution("Marked status as fail");
					String continueOnFailure = (String) step.get("continueOnFail");
					if(continueOnFailure == null || continueOnFailure.equalsIgnoreCase("false")) {
						break;
					}
				}
				
				if(wait != null && wait.equalsIgnoreCase("WaitForApi") &&
						AppProperties.apicapture != null && 
						AppProperties.apicapture.equalsIgnoreCase("true")) {
					Utilities.executionMsgCache.put(testCaseResultsId, "Step number: " + stepnum + " - checking for long running apis ");
					checkForLongRunningAPIs(devtools);
				} else {
					if(wait != null && wait.equalsIgnoreCase("WaitForApi")) {
						if(AppProperties.apicapture == null || 
								AppProperties.apicapture.equalsIgnoreCase("false")) {
							el.logExecution("Configurations for API Capture is false - so not waiting for API to complete");
						}
					}
				}
				if(AppProperties.apicapture.equalsIgnoreCase("true")) {
					boolean retry = processErrorAPI(step, testCaseResultsId,url,
							test_step_id, el, devtools);
				
					if(retry) {
						devtools.attempt = devtools.attempt + 1;
						Utilities.executionMsgCache.put(testCaseResultsId, "Step number: " + stepnum + " - Retrying the step based on api config ");
						continue;
					} else {
						devtools.attempt = 1;
					}
				}
				if(wait != null && wait.equals("After")) {
					try {
						el.logExecution("THREAD Sleeping AFTER STEP for (milliseconds) " + waitTime * 1000);
						Utilities.executionMsgCache.put(testCaseResultsId, "Step number: " + stepnum + " - waiting for " + waitTime + " seconds after the step");
						Thread.sleep(waitTime * 1000);
						
					}catch (Exception e) {
						el.logExecution("thread5 sleeping for waittime * 1000 interupted");
					}
				}
				
				long ts_endTime = System.currentTimeMillis();
				double ts_duration = (ts_endTime - ts_startTime)/ 1000.0;
				msc.updateDurationForTestStep(testStepId, ts_duration);
				i = i + 1;
				attempt = 0;
				el.logExecution("----------------------------------------------------------------------------------------------------");
				//scrapePage();
			}
		} catch(Exception e) {
			el.logExecution(e.getMessage());
			el.logExecution(getStackTraceAsString(e));
			if(step != null) {
			msc.writeTestStepStatusToDB(testCaseResultsId, stepnum, 
					(String) step.get("Page_Name"),
					(String) step.get("Page_Description"), xpath, keyword, action, 
					(String) step.get("Flow"), testData,
					varName, "FAIL", ssfilename, ssfilenameAF, testStepId, "", duration, 
					validateIn, runIssues, errorCode, ts_execStatus, "", 
					null, foundelements,devtools);
			} else {
				msc.writeTestStepStatusToDB(testCaseResultsId, stepnum, 
						"","", xpath, keyword, action,"", testData,
						varName, "FAIL", ssfilename, ssfilenameAF, testStepId, "", duration, 
						validateIn, runIssues, errorCode, ts_execStatus, "",
						null, foundelements,devtools);
			}
			tc_execStatus = "FAIL";
			if (e instanceof NoSuchWindowException) {
				tc_status = "ABEND";
				el.logExecution("Test Case Status ABEND " );
			} else if (e instanceof java.util.concurrent.TimeoutException) {
				tc_status = "TIMEOUT";
				el.logExecution("Test Case Status TIMEOUT " );
			} else {
				el.logExecution("Test Case Status FAIL " );
				tc_status = "FAIL";
			}
			
		} finally {
			try {
				//recorder.stop();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		long tc_endTime = System.currentTimeMillis();
		double tc_duration = (tc_endTime - tc_startTime) / 1000.0;
		if (tc_status.equals("STARTED")) {
			tc_status = "PASS";
		}
		
		el.logExecution("Writing test case status update");
		msc.updateTestCaseStatusToDB(testCaseResultsId, tc_status, tc_duration, tc_execStatus, companyId);
		if(ssForAllElement) {
			msc.updateTestCaseSSStatus((int)testCaseDetails.get("idtest_case"), tc_status);
		}
		el.logExecution("Completed Writing test case status update");

		if(tc_status.equalsIgnoreCase("FAIL")) {
			boolean isWait = msc.getWaitConditionFromDB((int)testCaseDetails.get("idtest_case"));
			if(isWait) {
				try {
					el.logExecution("THREAD Sleeping for 15 minutes");
					Thread.sleep(900000);
				} catch(Exception e) {
					
				}
			}
		}
		results.put("status", tc_status);
		results.put("duration", tc_duration);
		
		if(indexUsed) {
			allRowsProcessed(results, xlsRowCount, mrIndex);
		} else {
			results.put("multirun", multiRun);
			results.put("mrIndex", mrIndex);
		}
		
		if(!runonce) {
			runonce = !indexUsed;
			results.put("runonce", runonce);
		}		
		
		if(isTCMultiRun) {
			el.logExecution("Multi run - completed iteration number : " + mrIndex + "out of total " + totalIterations );
			mrIndex++;
			results.put("mrIndex", mrIndex);			
		}
		
		el.logExecution("Returning from SG.Test for test case: " + (String)testCaseDetails.get("Test_Case"));
		
//		if(tc_status != null && tc_status.equals("PASS") && !predecessor) {
//			el.logExecution("Starting to delete passed data from execution log");
//			el.deletePreviousPass();
//			el.logExecution("Completed the delete of passed data from execution log");
//		}
		msc.summarizeStatusCodeCountToDB(testCaseResultsId);
		
        if (Utilities.cleanUpQueue != null && AppProperties.isCleanUpOn.equalsIgnoreCase("true")) {
        	Utilities.cleanUpQueue.addToQueue((long) testCaseResultsId);
        	el.logExecution("cleaning old results for the testcase: " + (String)testCaseDetails.get("Test_Case"));
        }
		
		return results;
	}
	
	public void allRowsProcessed(HashMap results, int xlsRowCount, int mrIndex) {
		if(indexUsed && mrIndex < xlsRowCount-1) {
			results.put("multirun", true);
			mrIndex = mrIndex + 1;
		} else {
			results.put("multirun", false);			
		}
		results.put("mrIndex", mrIndex);
	}
	
	public WebElement findByApprox(String xpath, String outerhtml, String keyword) {
		
		if(!xpath.contains("/")) {
			return null;
		}
		String type = "";
		if(keyword.equals("EditBox")) {
			type = "text";
		}
		
		String tagName = xpath.substring(xpath.lastIndexOf('/') + 1);
		String searchPath = xpath;
		if(searchPath.contains("|")) {
			searchPath = searchPath.substring(0, xpath.indexOf('|') - 1);
		}
		if(searchPath.contains("/")) {
			searchPath = searchPath.substring(0, searchPath.lastIndexOf('/'));
		}
		
		Map<String, String> recordedOuterHtml = getAttr(outerhtml);
		
		boolean found = false;
		do {
			if(searchPath.contains("/")) {
				searchPath = searchPath.substring(0, searchPath.lastIndexOf('/'));
			} else {
				return null;
			}
			String xpathstr = "";
			if(type.equals("")) {
				xpathstr = searchPath + "//" + tagName;
			} else {
				xpathstr = searchPath + "//" + tagName + "[@type='" + type +  "']";
			}
			List<WebElement> searchElements = driver.findElements(By.xpath(xpathstr));
			for(WebElement searchElement : searchElements) {
				Map<String, String> latestOuterHtml = getAttr(searchElement.getAttribute("outerHTML"));
				
				if(latestOuterHtml.size() != recordedOuterHtml.size()) {
					continue;
				}
				
				if((!latestOuterHtml.keySet().containsAll(recordedOuterHtml.keySet())) ||
						(!recordedOuterHtml.keySet().containsAll(latestOuterHtml.keySet()))) {
					continue;
				}
				
				
				Iterator latestIter = latestOuterHtml.keySet().iterator();
				while(latestIter.hasNext()) {
					String latestKey = (String)latestIter.next();
					String latestValue = (String)latestOuterHtml.get(latestKey);
					String recordedValue = (String)recordedOuterHtml.get(latestKey);
					
					if(!latestValue.equals(recordedValue)) {
						if(latestKey.equals("id") || latestKey.equals("value")) {
							//expected at this stage
						} else {
							continue;
						}
					}
				}
				return searchElement;		
			}
		} while(!found || searchPath.contains("/"));

		
		return null;
	}
	
	public Map<String, String> getAttr(String outerhtml){
		Pattern attributesPattern = Pattern.compile("(\\w+)=\"([^\"]*)\"");
        Matcher attributesMatcher = attributesPattern.matcher(outerhtml);
        Map<String, String> attributes = new HashMap<>();
        while (attributesMatcher.find()) {
            attributes.put(attributesMatcher.group(1), attributesMatcher.group(2));
        }
        return attributes;
	}

	@AfterTest
	public void tearDown() {
		try {
//			el.logExecution("Tearing down for testcase : "  + sessionId);
			Utilities.endExecution(cid, sessionId);
			BROWSER = null;
			sessionId = null;
			PortManager.releasePort(port, el);
			
			Iterator iter = windowDrivers.keySet().iterator();
			while(iter.hasNext()) {
				driver = (WebDriver)windowDrivers.get(iter.next());
				if(driver != null) {
					try {
						driver.close();
					} catch (Exception e) {
//						e.printStackTrace();
					}
					try {
						driver.quit();
					}catch (Exception e) {
//						e.printStackTrace();
					}
				}
			}
		}catch (Exception e) {
//			e.printStackTrace();
		}
	}
	
	public String takeSS(String ssfilename, int testCaseId,  int testCaseResultsId,int companyId,
			boolean analysisRun) {
		
		if(analysisRun) {
			try {
				Thread.sleep(2000);
			}catch (Exception e) {
				e.printStackTrace();
			}
		}
		File screenshot = null;
		try {
			screenshot = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
		} catch(Exception e) {
			
		}
		String fsl = Utilities.getScreenShotsDir(companyId, testCaseId, testCaseResultsId) + ssfilename;
		try {
			FileUtils.copyFile(screenshot, new File(fsl));
		}catch (Exception e) {
			e.printStackTrace();
		}
		return ssfilename;
	}
	
	public String getSSFullLoc(String ssfilename, int testCaseId,int testCaseResultsId , 
			int companyId) {
		String fsl = Utilities.getScreenShotsDir(companyId, testCaseId, testCaseResultsId) + ssfilename;
		return fsl;
	}
	
	public String getPIFileName(int testCaseId, int testStepId, int testCaseResultsId, int i , int companyId) {

		String pifilename = "_" + companyId + "_" + testCaseId + "_" + testStepId + "_" + testCaseResultsId + 
				"_Step" + i + ".html";
		String fsl = Utilities.getPageInfoDir(companyId, testCaseId, testCaseResultsId) + pifilename;

		return fsl;
	}
	
	public static void waitForPageToLoad(WebDriver driver, ExecutionLogger el) {
        JavascriptExecutor jsExecutor = (JavascriptExecutor) driver;
        long startTime = System.currentTimeMillis();

        // Wait for the document readyState to be 'complete'
        boolean isPageLoaded = false;
        while (!isPageLoaded && (System.currentTimeMillis() - startTime < 30000)) {
            isPageLoaded = (Boolean) jsExecutor.executeScript("return document.readyState === 'complete'");
        }
        
        boolean isJQueryDefined = (Boolean) jsExecutor.executeScript("return typeof jQuery !== 'undefined'");
        el.logExecution("Is djquery defined : " + isJQueryDefined);
        
        boolean areAjaxRequestsCompleted = false;
        if (isJQueryDefined) {
	        while (!areAjaxRequestsCompleted && (System.currentTimeMillis() - startTime < 30000)) {
	            areAjaxRequestsCompleted = (Boolean) jsExecutor.executeScript("return jQuery.active === 0");
	            el.logExecution("waiting for jquery active to be 0 : " );
	        }
        }
        
        try {
        	Thread.sleep(2000);
        } catch(Exception e) {
        	
        }
        
    }
	

	public void changeBGtakess(WebElement we, String ssfilename, String tsStatus, 
			boolean analysisRun, boolean baselineRun, ExecutionLogger el,
			String locationStrategy, int screenX, int screenY, int createsAlert,
			String keyword) {
		
		if(ssonerror && !tsStatus.equals("FAIL")) {
			return;
		}
		
		if(createsAlert == 1 || (keyword != null && keyword.equalsIgnoreCase("Alert"))) {
			return;
		}
		
		if(analysisRun || baselineRun) {
			waitForPageToLoad(driver, el);
		}
		
		String originalBgColor = "";
			if(locationStrategy == null || !locationStrategy.equalsIgnoreCase("XY")) {
				try {
					((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block: 'center'});", we);
					originalBgColor = (String)((JavascriptExecutor) driver).executeScript("var originalBackgroundColor = arguments[0].style.backgroundColor; arguments[0].style.backgroundColor = 'green'; return originalBackgroundColor;", we);
				} catch (Exception e) {
					e.printStackTrace();
				}
			} else {
				// Highlight a pixel at coordinates (100, 200)
		        highlightPixel(driver, screenX, screenY);
			}
	
		try {
			try {
				driver = new Augmenter().augment(driver);
			} catch (java.lang.NoClassDefFoundError er) {
				
			}
			File screenshot = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);		
			FileUtils.copyFile(screenshot, new File(ssfilename));
			Files.delete(screenshot.toPath());
		}catch (Exception e) {
			//e.printStackTrace();
		} 
		
		if(locationStrategy == null || !locationStrategy.equalsIgnoreCase("XY")) {
			try {
				((JavascriptExecutor) driver).executeScript("arguments[0].style.backgroundColor = '" + originalBgColor + "'", we);
			} catch (Exception e) {
				//e.printStackTrace();
			}
		} else {
			removeHighlight(driver);
		}
	}

	private String processXPath(String xpath) {
		if (xpath != null && xpath.contains(AppProperties.delimiter)) {
			String random = xpath.substring(xpath.indexOf(AppProperties.delimiter),
					xpath.lastIndexOf(AppProperties.delimiter) + 1);
			if (randomMap.get(random) != null) {
				String rand = (String) randomMap.get(random);
				xpath = xpath.replace(random, rand);
			} else if(varMap.get(random) != null) {
				String rand = (String) varMap.get(random);
				xpath = xpath.replace(random, rand);
			}
		}
		return xpath;
	}

	public void processVariables(String varName, String testData, String xpath, int index, JSONObject step) {
		if(varName == null) {
			return;
		}
		if (varName.startsWith(AppProperties.delimiter) && varName.endsWith(AppProperties.delimiter)
				&& !varName.contains("_")) {
			
			if((varName.equals(AppProperties.delimiter + "D0" + AppProperties.delimiter)) ||
					(varName.equals(AppProperties.delimiter + "DNext" + AppProperties.delimiter))){
				varMap.put(varName, xpath);
				d0index = index - 2;
			} else {
				varMap.put(varName, testData);
			}
		} else if(varName.contains(":")) {
			String url = (String) step.get("pageurl");
			varName = varName.substring(0, varName.indexOf(":"));
			varMap.put(varName, url);
		}
	}
	
	public void sendEmail(JSONObject step, ExecutionLogger el, MySQlConn msc) {
		MailAccess ma = new MailAccess();
		
		String fromEmail = (String)step.get("customerEmail");
		String pwd = (String)step.get("customerPassword");
		String toEmail = (String)step.get("toaddress");
		String subject = (String)step.get("emailsubject");
		String content = (String)step.get("emailcontent");
		int tsid = (int) step.get("idtest_step");
		
		subject = replaceWithVar(subject);
		content = replaceWithVar(content);
		
		ma.sendEmail(tsid, fromEmail, pwd, toEmail, subject, content, el, msc);
	}
	
	public String replaceWithVar(String text) {
		
		if(varMap == null || varMap.size() == 0) {
			return text;
		}
		
		Iterator iter = varMap.keySet().iterator();
		while(iter.hasNext()) {
			String var = (String)iter.next();
			String varValue = (String)varMap.get(var);
			
			if(var != null && var.startsWith(AppProperties.delimiter) &&  text.contains(var)) {
				el.logExecution("found var " + var + ":" + varValue);
				el.logExecution("Before replacing " + text);				
				text = text.replace(var, varValue);
				el.logExecution("After replacing " + text);
			}
		}
		
		return text;
	}

//	public String processFileTypes(String filename,String fieldname, int testCaseId,
//			int companyId, int runonceIndex, ExecutionLogger el, String scope) {
//
//		if (filename.endsWith(".json")) {
//			return getFromJSONFile(filename, fieldname, testCaseId, companyId);
//		} else if (filename.endsWith(".xlsx")) {
//			if(scope != null && scope.equals("Multi Run")) {
//				indexUsed = true;
//			} else {
//				indexUsed = false;
//			}
//			return getFromXLSXFile(filename, fieldname, testCaseId, companyId,runonceIndex,
//					indexUsed, el);
//		} else if (filename.endsWith(".xls")) {
//			if(scope != null && scope.equals("Multi Run")) {
//				indexUsed = true;
//			} else {
//				indexUsed = false;
//			}
//			return getFromXLSFile(filename, fieldname, testCaseId, companyId,runonceIndex,
//					indexUsed, el);
//		}
//
//		return null;
//
//	}
	
	public String getFromXLSFile(String filename, String fieldname, int testCaseId, 
			int companyId, int runonceIndex, boolean useIndex, ExecutionLogger el) {
		ExcelWriter2 ew2 = new ExcelWriter2();
		if(useIndex) {
			xlsColCount = ew2.getColCount(ew2, filename, fieldname);
			xlsRowCount = ew2.getRowCount(ew2, filename, fieldname);
			mrFieldIndex = mrFieldIndex + 1;
		}
		return ew2.getXLSCell(ew2, filename, fieldname, runonceIndex, useIndex, this,
				mrIndex,mrFieldIndex, el);
	}

	public String getFromXLSXFile(String filename, String fieldname, int testCaseId, 
			int companyId, int runonceIndex, boolean useIndex, ExecutionLogger el) {
		ExcelWriter2 ew2 = new ExcelWriter2();
		if(useIndex) {
			xlsColCount = ew2.getColCount(ew2, filename, fieldname);
			xlsRowCount = ew2.getRowCount(ew2, filename, fieldname);
			mrFieldIndex = mrFieldIndex + 1;
		}
		return ew2.getCell(ew2, filename, fieldname, runonceIndex, useIndex, this,
				mrIndex,mrFieldIndex, el);
	}

	public String defaultTestData(String testData, boolean AIGenerated, String varName,
			String keyword, String action, String gendateoffset) {
		
		if(testData == null) {
			testData = "";
		}
		int tildeMatches = StringUtils.countMatches(testData, AppProperties.delimiter);
		if (tildeMatches == 2 && testData.startsWith(AppProperties.delimiter) && 
				testData.endsWith(AppProperties.delimiter)) {
			if(testData.length() == 2) {
				return "";
			} else if (randomMap.get(testData) != null) {
				String td = (String) randomMap.get(testData);
				if(varName != null && !varName.equals("")) {
					randomMap.put(varName, td);
				}
				return td;
			} else if (!testData.contains("_")) {
				String result = testData;
				result =  (String) varMap.get(result);
				if(result == null) {
					return testData;
				} else {
					return result;
				}
			} else if (testData.startsWith(AppProperties.delimiter)) {
				String res = "";
				if (testData.startsWith(AppProperties.delimiter + "RANDOM" + AppProperties.EMAIL)) {
					String s1 , s2, s3 = "";
					if(AIGenerated) {
						String prompt = "Please generate a random string of 8 ASCII letters.";
						s1 = OpenAIClient.genData( prompt, 1, 10);
						prompt = "Please generate a random string of 5 ASCII letters.";
						s2 = OpenAIClient.genData( prompt, 1, 10);
						prompt = "Please generate a random string of 3 ASCII letters.";
						s3 = OpenAIClient.genData( prompt, 1, 10);
					} else {
						s1 = Utilities.randomGen(AppProperties.CHAR, 8,AIGenerated);
						s2 = Utilities.randomGen(AppProperties.CHAR, 5,AIGenerated);
						s3 = Utilities.randomGen(AppProperties.CHAR, 3, AIGenerated);
						res = s1 + "@" + s2 + "." + s3;
					}
					res = s1 + "@" + s2 + "." + s3;
				} else if (testData.startsWith(AppProperties.delimiter + "RANDOM" + AppProperties.TODAY)) {
					String data = testData.substring(7, testData.length() - 1);
					String[] dataSeg = data.split("_");
					String type = dataSeg[0];
					String[] dataLen = dataSeg[1].split("-");
					
					if(gendateoffset != null && !gendateoffset.equals("")) {
						res = Utilities.getNow(dataLen[1], gendateoffset);
					} else {
						res = Utilities.getNow(dataLen[1]);	
					}
				} else if (testData.startsWith(AppProperties.delimiter + "DATE_" + AppProperties.delimiter)) {
					String prefix = testData.substring((AppProperties.delimiter + "DATE_" + AppProperties.delimiter).length(),
							testData.length());
					String date = prefix.substring(0,prefix.indexOf(AppProperties.delimiter));
					prefix = prefix.substring(prefix.indexOf(AppProperties.delimiter) + AppProperties.delimiter.length(), 
												prefix.length());
					String dateFromat = prefix.substring(0,prefix.indexOf(AppProperties.delimiter));
					res = Utilities.getCurrentTimestamp(dateFromat);
				} else {
					String data = testData.substring(7, testData.length() - 1);
					String[] dataSeg = data.split("_");
					String type = dataSeg[0];
					String[] dataLen = dataSeg[1].split("-");
					int len = new Integer(dataLen[1]).intValue();
					res = Utilities.randomGen(type, len, AIGenerated);
				}
				if(varName != null && !varName.equals("")) {
					randomMap.put(varName, res);
				} else {
					randomMap.put(testData, res);
				}
				return res;
			} else if (!testData.contains("_")) {
				return (String) varMap.get(testData);
			}
		} else if (testData.contains(AppProperties.delimiter)) {
			
			do {
				tildeMatches = StringUtils.countMatches(testData, AppProperties.delimiter);
				if(tildeMatches <= 0) {
					break;
				}
				int tildeMod = tildeMatches % 2;
				if (tildeMod == 0) {			
					String rand = testData.substring(testData.indexOf(AppProperties.delimiter) + 1, testData.length());
					rand = rand.substring(0, rand.indexOf(AppProperties.delimiter) + 1);
					rand = AppProperties.delimiter + rand;
					String randResult = defaultTestData(rand, AIGenerated, varName, keyword, action, gendateoffset);
					if(randResult.equals(rand)) {
						el.logExecution("issue in test data - cannot process" + rand + "came back as is - infinite loop");
						break;
					}
					testData = testData.replace(rand, randResult);
				} else {
					el.logExecution("issue with no if delimiters - should be an even number of delimiters");
					break;
				}
			} while (true);
			
		} else if(keyword != null && action != null && 
				keyword.equals("Clipboard") && action.equals("Get")) {
			el.logExecution("Clipboard doesn't work in headless mode");
			testData = getFromClipBoard();
			el.logExecution("The data copied from clipboard is - " + testData);
		}

		return testData;
	}

	private String regexGen(String regex) {
		Pattern pattern = Pattern.compile(regex);
		Matcher matcher;
		String randomString;
		Random rand = new Random();
		return "";
	}

	public void scrapper() {
		driver.get("https://seek.com.au");
		List<WebElement> elements = driver.findElements(By.xpath("//*[not(*)]"));
		for (WebElement element : elements) {
			el.logExecution(element.getText());
		}
	}
	
	public String colorAssertion(WebElement we, int stepnum, String xpath, String status,
			 String color, String flow, String attribute){
		
		String actualColor = we.getCssValue(attribute);
		actualColor = actualColor.replaceAll("rgba?\\(", "").replaceAll("\\s+", "").split(",")[0] +
                "," + actualColor.split(",")[1] +
                "," + actualColor.split(",")[2];
		actualColor = "rgb(" + actualColor + ")";

		if (actualColor.equals(color)) {
			if (flow == null || flow.equals("Positive") || flow.equals("Optional")) {
				status = "PASS";
				el.logExecution("Step  " + stepnum + " validating " + color
						+ " present on screen PASSED as color is present on screen and flow = positive "
						+ xpath);
			} else {
				status = "FAIL";
				el.logExecution("Step  " + stepnum + " validating " + color 
						+ " present on screen FAILED as color is present on screen " + actualColor + " and flow = negative "
						+ xpath);
//				runIssues = ";" + runIssues + "Step  " + stepnum + " validating " + color
//						+ " present on screen FAILED as color is present on screen " + actualColor +" and flow = negative "
//						+ xpath;
			}
		} else {
			if (flow == null || flow.equals("Positive") || flow.equals("Optional")) {
				status = "FAIL";
				el.logExecution("Step  " + stepnum + " validating " + color
					+ " present on screen FAILED as color not on screen " + actualColor  + " and flow = positive " + xpath);
//				runIssues = ";" + runIssues + "Step  " + stepnum + " validating " + color
//					+ " present on screen FAILED as color not on screen " + actualColor + " and flow = positive " + xpath;
			} else {
				status = "PASS";
				el.logExecution("Step  " + stepnum + " validating " + color
					+ " present on screen PASSED as color not on screen and flow = negative" + xpath);
				}
		}
	
		return status;
	}
	
	public String screenAssertion(WebElement we, int stepnum, String testData, String xpath, 
			String flow, String status, String action, String subAction) {
		if(subAction != null && subAction.equals("validateExact")) {
			el.logExecution(
					"Step  " + stepnum + " validating that " + testData + " is present exactly on the screen " + xpath);
		} else {
			el.logExecution(
				"Step  " + stepnum + " validating that " + testData + " is at least partially present on the screen " + xpath);
		}
		String textOnScreen = we.getText();
		String type = (String)we.getAttribute("type");
		if(type != null && (type.equals("text") || type.equals("number") ||
				type.equals("textarea"))) {
			textOnScreen = we.getAttribute("value");
		}
		
		boolean stringFound = false;
		
		if(subAction != null && subAction.equals("validateExact")) {
			if (textOnScreen.equals(testData)) {
				stringFound = true;
			}
		} else {
			if (textOnScreen.toLowerCase().contains(testData.toLowerCase()) || 
					testData.toLowerCase().contains(textOnScreen.toLowerCase())) {
				stringFound = true;
			}
		}
		
		
		if (!stringFound) {
			if (flow == null || flow.equals("Positive")) {
				status = "FAIL";
				el.logExecution("Step  " + stepnum + " validating [" + testData
						+ "] present on screen FAILED - data on screen [" + textOnScreen + "] and flow = " + flow +  " " + xpath);
//				runIssues = ";" + runIssues + "Step  " + stepnum + " validating [" + testData
//						+ "] present on screen FAILED - data on screen [" + textOnScreen + "] and flow = " + flow + " " + xpath;
			} else if (flow.equals("Optional")) {
					status = "FAIL";
					el.logExecution("Step  " + stepnum + " validating [" + testData
							+ "] present on screen FAILED - data on screen [" + textOnScreen + "] and flow = Optional " + xpath);
//					runIssues = ";" + runIssues + "Step  " + stepnum + " validating " + testData
//							+ " present on screen FAILED - data on screen [" + textOnScreen + "] and flow = Optional " + xpath;
			} else {
				el.logExecution("Step  " + stepnum + " validating [" + testData
						+ "] present on screen PASSED data on screen [" + textOnScreen + "] and flow = negative " + xpath);
			}
		} else {
			if (flow == null || flow.equals("Positive") || flow.equals("Optional")) {
				status = "PASS";
				el.logExecution("Step  " + stepnum + " validating [" + testData
						+ "] present on screen PASSED as data is present on screen [" + textOnScreen + "] and flow = positive "
						+ xpath);
			} else {
				status = "FAIL";
				el.logExecution("Step  " + stepnum + " validating [" + testData
						+ "] present on screen FAILED as data is present on screen [" + textOnScreen + "] and flow = negative "
						+ xpath);
//				runIssues = ";" + runIssues + "Step  " + stepnum + " validating [" + testData
//						+ "] present on screen FAILED as data is present on screen [" + textOnScreen + "] and flow = negative "
//						+ xpath;
			}
		}
		
		return status;
	}
	
	public String dateAssertion(WebElement we, int stepnum, String testData, String xpath, 
			String flow, String status) {
		el.logExecution(
				"Step  " + stepnum + " validating that " + testData + " is present on the screen " + xpath);
		String textOnScreen = we.getText();
		String type = (String)we.getAttribute("type");
		if(type != null && type.equals("text")) {
			textOnScreen = we.getAttribute("value");
		}
		if (!(textOnScreen.contains(testData))) {
			if (flow == null || flow.equals("Positive")) {
				status = "FAIL";
				el.logExecution("Step  " + stepnum + " validating " + testData
						+ " present on screen FAILED as data not on screen " + textOnScreen  + " and flow = positive " + xpath);
//				runIssues = ";" + runIssues + "Step  " + stepnum + " validating " + testData
//						+ " present on screen FAILED as data not on screen " + textOnScreen + " and flow = positive " + xpath;
			} else {
				el.logExecution("Step  " + stepnum + " validating " + testData
						+ " present on screen PASSED as data not on screen and flow = negative" + xpath);
			}
		} else {
			if (flow == null || flow.equals("Positive") || flow.equals("Optional")) {
				status = "PASS";
				el.logExecution("Step  " + stepnum + " validating " + testData
						+ " present on screen PASSED as data is present on screen and flow = positive "
						+ xpath);
			} else {
				el.logExecution("Step  " + stepnum + " validating " + testData
						+ " present on screen FAILED as data is present on screen " + textOnScreen + " and flow = negative "
						+ xpath);
//				runIssues = ";" + runIssues + "Step  " + stepnum + " validating " + testData
//						+ " present on screen FAILED as data is present on screen " + textOnScreen +" and flow = negative "
//						+ xpath;
			}
		}
		
		return status;
	}
	
	public String dbAssertion(WebElement we, int stepnum, String testData, String xpath, 
			String flow, String status) {
		el.logExecution(
				"Step  " + stepnum + " validating that " + testData + " is present on the screen " + xpath);
		String textOnScreen = we.getText();
		String type = (String)we.getAttribute("type");
		if(type != null && type.equals("text")) {
			textOnScreen = we.getAttribute("value");
		}
		return assertion(textOnScreen, testData, xpath, 
				flow, status, stepnum);
	}
	
	public String justAssert(WebElement we, int stepnum, String testData, String xpath, 
			String flow, String status, String src) {
		String textOnScreen = null;
		if(src == null || src.equals("")) {
			el.logExecution(
					"Step  " + stepnum + " validating that " + testData + " is present on the screen " + xpath);
			textOnScreen = we.getText();
			String type = (String)we.getAttribute("type");
			if(type != null && type.equals("text")) {
				textOnScreen = we.getAttribute("value");
			}
		} else {
			textOnScreen = src;
		}
		return assertion(textOnScreen, testData, xpath, 
				flow, status, stepnum);
	}
	
	public String assertion(String textOnScreen, String testData, String xpath, 
			String flow, String status, int stepnum) {
		if (!(textOnScreen.contains(testData))) {
			if (flow == null || flow.equals("Positive") || flow.equals("Optional")) {
				status = "FAIL";
				el.logExecution("Step  " + stepnum + " validating " + testData
						+ " present on screen FAILED as data not on screen " + textOnScreen  + " and flow = positive " + xpath);
//				runIssues = ";" + runIssues + "Step  " + stepnum + " validating " + testData
//						+ " present on screen FAILED as data not on screen " + textOnScreen + " and flow = positive " + xpath;
			} else {
				status = "FAIL";
				el.logExecution("Step  " + stepnum + " validating " + testData
						+ " present on screen PASSED as data not on screen and flow = negative" + xpath);
			}
		} else {
			if (flow == null || flow.equals("Positive") || flow.equals("Optional")) {
				status = "PASS";
				el.logExecution("Step  " + stepnum + " validating " + testData
						+ " present on screen PASSED as data is present on screen and flow = positive "
						+ xpath);
			} else {
				status = "FAIL";
				el.logExecution("Step  " + stepnum + " validating " + testData
						+ " present on screen FAILED as data is present on screen " + textOnScreen + " and flow = negative "
						+ xpath);
//				runIssues = ";" + runIssues + "Step  " + stepnum + " validating " + testData
//						+ " present on screen FAILED as data is present on screen " + textOnScreen +" and flow = negative "
//						+ xpath;
			}
		}
		
		return status;
	}
	
	public String regexAssertion(WebElement we, int stepnum, String testData, String xpath, 
			String flow, String status, String regex) {
		el.logExecution(
				"Step  " + stepnum + " validating " + testData + "present on screen using regex" + xpath);
		RegexUtil ru = new RegexUtil();
		boolean textFound = ru.validateRegex(we.getText(), regex);
		if (!(textFound)) {
			if (flow == null || flow.equals("Positive")) {
				status = "FAIL";
				el.logExecution("Step  " + stepnum + " validating " + testData
						+ " present on screen using regex " + regex + "FAILED as data not on screen and flow = positive " + xpath);
//				runIssues = ";" + runIssues + "Step  " + stepnum + " validating " + testData
//						+ " present on screen using regex " + regex + "FAILED as data not on screen and flow = positive " + xpath;
			} else {
				el.logExecution("Step  " + stepnum + " validating " + testData
						+ " present on screen using regex " + regex + "PASSED as data not on screen and flow = negative" + xpath);
			}
		} else {
			if (flow == null || flow.equals("Positive") || flow.equals("Optional")) {
				status = "PASS";
				el.logExecution("Step  " + stepnum + " validating " + testData
						+ " present on screen using regex " + regex + "PASSED as data is present on screen and flow = positive "
						+ xpath);
			} else {
				el.logExecution("Step  " + stepnum + " validating " + testData
						+ " present on screen using regex " + regex + "FAILED as data is present on screen and flow = negative "
						+ xpath);
//				runIssues = ";" + runIssues + "Step  " + stepnum + " validating " + testData
//						+ " present on screen using regex" + regex + "FAILED as data is present on screen and flow = negative "
//						+ xpath;
			}
		}
		
		return status;
	}
	
	public String fileAssertion(WebElement we, int stepnum, String testData, String xpath, 
			String flow, String status, String filename, String fileField, int testCaseId,
			int companyId, int tcrid, int runonceIndex, String validateIn, JSONObject step) {
		el.logExecution(
				"Step  " + stepnum + " validating " + testData + " present on screen " + xpath);
		String fileText = "";
		
		boolean textFound = false;
		String textToBeFound = "";
		String textInFile = "Nothing Found";
		
		if(testData.equalsIgnoreCase(AppProperties.validatedelimiter + AppProperties.DYNAMICTEXT + AppProperties.validatedelimiter)) {
			textToBeFound = we.getText();	
			el.logExecution("Validating dynamic data in the file : " + textToBeFound);
		} else {
			textToBeFound = testData;
			el.logExecution("Validating testdata in the file : " + textToBeFound);
		}
		
		String fullFileName = "";
		if(validateIn != null && validateIn.equals("InDownloadedFile")) {
			if(filename != null && filename.contains("*")) {
				File[] matchingFiles = Utilities.getFilesWithMatchingPattern(downloadDir,
						filename);
				if (matchingFiles != null && matchingFiles.length > 0) {
					fullFileName = downloadDir + "\\" + matchingFiles[0];
				} else {
					//check if there is a zip file, if yes - unzip it
					String globToRegex = Utilities.globToRegex("*.zip");
					File[] zipFiles = Utilities.getFilesWithMatchingPattern(downloadDir,
							globToRegex);
					if(zipFiles != null && zipFiles.length > 0) {
						for(int izip =0;izip < zipFiles.length; izip++) {
							String zipFile = zipFiles[izip].toString();
							Utilities.unZipFile(zipFile, downloadDir);
							globToRegex = Utilities.globToRegex(filename);
							matchingFiles = Utilities.getFilesWithMatchingPattern(downloadDir,
									globToRegex);
							if (matchingFiles != null && matchingFiles.length > 0) {
								fullFileName = matchingFiles[0].toString();
								filename = matchingFiles[0].getName();
								break;
							}
						}
					}
				}
			} else if (filename.contains("(")) {
				String ext = filename.substring(filename.indexOf("."),filename.length());
				String filen = filename.substring(0,filename.indexOf("("));
				filename = filen+ext;
				fullFileName = downloadDir + "\\" + filename;
			} else {
				fullFileName = downloadDir + "\\" + filename;
			}
		} else {
			fullFileName = Utilities.getInputFilePath(companyId, testCaseId,  filename);
		}
		
		if (filename.endsWith(".pdf") || filename.contains(".pdf" + AppProperties.delimiter)) {	
			
			if(filename.contains(".pdf" + AppProperties.delimiter)) {
				filename = filename.substring(0, filename.indexOf(AppProperties.delimiter));
				fullFileName = Utilities.getTCDownloadDir(companyId, testCaseId, tcrid) + filename;
			}
			fileText = PDFTextExtractor.getpdfText(fullFileName);
			
			if(testData.contains(filename + AppProperties.delimiter)) {
				testData = testData.substring(testData.indexOf(AppProperties.delimiter) + 
						AppProperties.delimiter.length(), 
						testData.length());
				testData = defaultTestData(testData, AppProperties.chatgpton,"","","","");
				if ((textToBeFound.contains(testData))) {
					el.logExecution("the value provided :" + testData + " is found in the file");
					textFound = true;
				}
			} else {
			
				if ((textToBeFound.contains(fileText))) {
					el.logExecution("the value provided :" + testData + " is found in the file");
					textFound = true;
				}
			}
		} else if(filename.endsWith(".csv")) {
			el.logExecution("In csv Assertion");
			BufferedReader br = null;
			List<String[]> rows = new ArrayList<>();
			String csvSplitBy = ",";
			try {
				br = new BufferedReader(new FileReader(fullFileName));
				String line = "";
				if(fileField.equals("")) {							
					while ((line = br.readLine()) != null) {
		                String[] row = line.split(csvSplitBy);
		                for(int i=0;i<row.length;i++) {
		                	if(row[i].equals(textToBeFound)) {
		                		textFound = true;
		                		break;
		                	}
		                }

		                if(textFound) {
		                	break;
		                }
		            }					
				} else {
					if(fileField.contains(AppProperties.testdatadelimiter)) {
						el.logExecution("Full Cell details provided like A2");
						String[] rc = fileField.split(AppProperties.testdatadelimiter);
						if(rc[1].contains("Count")) {
							int column = (int) (rc[1].substring(6,7).toUpperCase().charAt(0) - 'A');
							int rowCount = 0;
							while((line = br.readLine()) != null) {
								String[] columns = line.split(",");
					            if (column < columns.length && !columns[column].trim().isEmpty()) {
					                rowCount++;
					            }
							}
							int rowsToBeFound = Integer.parseInt(textToBeFound);
							if(rowCount == rowsToBeFound) {
								textFound = true;
							}
						} else if(rc[1].contains("ROW")) {	
							try (Reader reader = Files.newBufferedReader(Paths.get(fullFileName))) {

					            // Build a CSVParser with a custom configuration (you can adjust as needed)
								CSVParser parser = CSVParser.parse(reader, CSVFormat.RFC4180);
					            
								// Skip rows until we reach our row
								//index starts at 0
								
								String numstr = rc[1].substring(4,rc[1].length());
								int num = Integer.valueOf(numstr);
				                int currentRow = 0;
				                textInFile = "";
				                for (CSVRecord record : parser) {
				                    if (currentRow == num) {
				                    	el.logExecution("values for row (index starts at 0): " + num +
				                        		" the data is : " + record.toString());
				                        
				                     // For each value in the row, search on the webpage
				                        for (String value : record) {
				                        	if(value == null || value.equals("")) {
				                        		el.logExecution("Ignoring since cell data is null or blank: " + value);
				                        		continue;
				                        	}
				                            System.out.println("Searching for: " + value);

				                            try {
				                            	String searchpath = "//*[text()='" + value + "']";
				                            	WebElement element = driver.findElement(By.xpath(searchpath));
				                            	el.logExecution("Found element with text: " + element.getText());
				                            	textInFile += element.getText();
				                            	textInFile += " ";
				                            } catch (NoSuchElementException e) {
				                                status = "FAIL";
				                                el.logExecution("Could not find element with text: " + value);
				                            }
				                        }
				                        varMap.put("textInFile", textInFile);
				                        textFound = true;
				                        break;  // Stop after reading the row
				                    }
				                    currentRow++;
				                }

					        } catch (IOException e) {
					            e.printStackTrace();
					        } catch (java.lang.NoClassDefFoundError e) {
					            e.printStackTrace();
					        }
						} else {
//						    int row = Integer.valueOf(rc[1].substring(0,1));
							int col = (int) (rc[1].substring(0,1).toUpperCase().charAt(0) - 'A') + 1;
							int row = Integer.valueOf(rc[1].substring(1,rc[1].length()));
							int index =1;
							while ((line = br.readLine()) != null) {
								if(index == row) {
					                String[] lineRow = line.split(csvSplitBy);
					                textInFile = lineRow[col -1];
					                el.logExecution("Text to be found is :" + textToBeFound);	
					        		el.logExecution("Text in file is :" + textInFile);
					                if(lineRow[col -1] != null && lineRow[col -1].equals(textToBeFound)) {
					                	textFound = true;			                	
					                }
					                break;
								}
				                index = index + 1;
				            }
						}
					} else if(fileField.startsWith("R")) {
						el.logExecution("Only Row details provided like 1,2 ...");
						int row = Integer.valueOf(fileField.substring(1,fileField.length()));
						int index =0;
						while ((line = br.readLine()) != null) {
							if(index == row) {
				                String[] lineRow = line.split(csvSplitBy);
				                for(int i=0;i<lineRow.length;i++) {
				                	if(lineRow[i].equals(textToBeFound)) {
				                		textFound = true;
				                		break;
				                	}
				                }
			                	break;
							}
			                index = index + 1;
			            }						
					} else if(fileField.startsWith("C")) {
						el.logExecution("Only Column details provided like A,B ...");
						int col = Integer.valueOf(fileField.substring(1,fileField.length()));
						while ((line = br.readLine()) != null) {
			                String[] lineRow = line.split(csvSplitBy);
		                	if(lineRow[col] != null && lineRow[col].equals(textToBeFound)) {
		                		textFound = true;
		                		break;
		                	}
			            }
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
	            if (br != null) {
	                try {
	                    br.close();
	                } catch (IOException e) {
	                    e.printStackTrace();
	                }
	            }
	        }
		} else if(filename.endsWith(".xlsx")) {
			
			if(validateIn.equalsIgnoreCase("InRefrenceFile")) {
				int testStepId = (int)step.get("idtest_step");
				int prodid = msc.getProdIdFromTestStepId(testStepId);
				filename = Utilities.getProductInputFile(companyId, prodid, testStepId, filename);
			} else if (validateIn.equalsIgnoreCase("InDownloadedFile")) {
				filename = fullFileName;
			} else {
				filename = Utilities.getInputFilePath(companyId,testCaseId, filename);
			}
			ExcelWriter2 ew2 = new ExcelWriter2();
			if(!fileField.equals("") && !(fileField.contains(AppProperties.testdatadelimiter))) {
				textFound = ew2.checkFile(ew2, fullFileName, fileField, textToBeFound);
			} else if(fileField.contains(AppProperties.testdatadelimiter)) {	
				indexUsed = fileField.matches(".*~[a-zA-Z]");
				if(indexUsed) {
					xlsColCount = ew2.getColCount(ew2, filename, fileField);
					mrFieldIndex = mrFieldIndex + 1;
				}
				textFound = ew2.checkCell(ew2, filename, fileField, textToBeFound, false,
						runonceIndex, indexUsed, this, mrIndex, mrFieldIndex, el);
			} 
			
		} else if(filename.endsWith(".json")) {
			
		}
		if (!textFound) {
			status = "FAIL";
			if (flow == null || flow.equals("Positive")) {
				
				el.logExecution("Step  " + stepnum + " FAILED as the text " + textToBeFound
						+ " is not present in the file at the specified cell & flow = positive " );
			} else {
				el.logExecution("Step  " + stepnum + " PASSED as the text " + textToBeFound
						+ " is not present in the file at the specified cell & flow = negative ");
			}
		} else {
			status = "PASS";
			if (flow == null || flow.equals("Positive") || flow.equals("Optional")) {
				
				el.logExecution("Step  " + stepnum + " PASSED as the text " + textToBeFound
						+ " is present in the file at the specified cell & flow = positive ");
			} else {
				el.logExecution("Step  " + stepnum + " FAILED as the text " + textToBeFound
						+ " is present in the file at the specified cell & flow = negative ");
			}
		}
		
		return status;
	}
	
	public void edgeSetup() {
		// Launch Edge browser
		System.setProperty("webdriver.edge.driver", AppProperties.webdriveredgedriver);
		driver = new EdgeDriver();
		driver.manage().timeouts().implicitlyWait(10, TimeUnit.SECONDS);
		driver.manage().timeouts().setScriptTimeout(10, TimeUnit.SECONDS);
		BROWSER = "Edge";
	}
	
	public void chromeSetup() {
		// Launch Chrome browser
		
		Logger.getLogger("").setLevel(Level.INFO);
		System.setProperty("webdriver.chrome.driver", AppProperties.webdriverchromedriver);
		driver = new ChromeDriver();
		driver.manage().timeouts().implicitlyWait(10, TimeUnit.SECONDS);
		driver.manage().timeouts().setScriptTimeout(10, TimeUnit.SECONDS);

		BROWSER = "Chrome";
	}
	
	
	
	private void scrapePage() {
		//System.out.println(driver.getPageSource());
		
		List<WebElement> elements = driver.findElements(By.xpath("//*"));
		String startTime = Utilities.getNow();
		System.out.println(Utilities.getNow() + "---" + "No of Elements " + elements.size());
		// Loop through the visible elements and print their text content
		int errorCount = 0;
	
		for (WebElement element : elements) {
			try {
				if(!element.getTagName().equals("script") &&
						!element.getTagName().equals("html") &&
						!element.getTagName().equals("body") &&
						!element.getTagName().equals("style") &&
						!element.getTagName().equals("head")) {
			        
			        String uvalText = (String) ((JavascriptExecutor) driver).executeScript("return Array.prototype.slice.call(arguments[0].childNodes).filter(node => node.nodeType === Node.TEXT_NODE).map(node => node.textContent.trim()).join(' ');", element);
			        if(uvalText != null && !(uvalText.trim().equals(""))) {
			        	System.out.println("uvalText: " + uvalText);
			        }
			        
			        if(element.getTagName().equalsIgnoreCase("input") && element.getAttribute("type").equalsIgnoreCase("submit")) {
			        	System.out.println("---------------------------------------------");
			        	System.out.println("Button found " + uvalText);
			        	System.out.println("---------------------------------------------");
			        } 
				
				}
			}catch(Exception e) {
				errorCount = errorCount + 1;
			}
		}
		
		elements = driver.findElements(By.cssSelector("*"));
        JavascriptExecutor js = (JavascriptExecutor) driver;

        for (WebElement element : elements) {
            String onClick = element.getAttribute("onclick");
            Boolean hasClickEventListener = (Boolean) js.executeScript("return !!(arguments[0].onclick || ($._data && $._data(arguments[0], 'events') && $._data(arguments[0], 'events').click))", element);
            String cursorStyle = element.getCssValue("cursor");

            if (onClick != null || hasClickEventListener || "pointer".equals(cursorStyle)) {
                System.out.println("Actionable element found: " + element.getTagName() + " - " + element.getAttribute("outerHTML"));
            }
        }
		
		
		System.out.println("No of Elements " + elements.size() + "--- Start Time ->" + startTime + "---" + Utilities.getNow() + "---" + "Completed " );
	}
	
	public WebElement findCheckbox(WebElement textElement) {
        // Find the parent element of the text element
        WebElement parentElement = textElement.findElement(By.xpath("./.."));
        
        // Find the checkbox element within the parent element
        WebElement checkbox = null;
        try {
            checkbox = parentElement.findElement(By.xpath(".//input[@type='checkbox']"));
        } catch (Exception e) {
            // If no checkbox is found, continue searching recursively up the DOM tree
            checkbox = findCheckbox(parentElement);
        }
        
        return checkbox;
    }
	
	public WebElement findElementonSameRow(WebElement parentElement, String type, int index,
			int findExactly) {
		
		if(type != null && type.equals("SVG")) {
			type = "*[name() = 'svg']";
		}
		
		if (index < 0 ) {
            return null;
        }		
		if (type.startsWith(AppProperties.delimiter) && type.endsWith(AppProperties.delimiter)) {
			type = type.substring(1, type.length()-1);
        	type = "*[text()='" + type + "']";
        }
        // Find the checkbox element within the parent element
        List<WebElement> elements = null;
        try {
        	elements = parentElement.findElements(By.xpath(".//" + type));
        } catch (Exception e) {
            // If no checkbox is found, continue searching recursively up the DOM tree
        	parentElement = findParentElement(parentElement);
        	return findElementonSameRow(parentElement,type, index,findExactly);
        }
        
        if(elements.size() == 0) {
        	parentElement = findParentElement(parentElement);
        	return findElementonSameRow(parentElement,type, index,findExactly);
        }
        
     // If the index is out of range, return null
         if(elements.size() < index) {
        	 parentElement = findParentElement(parentElement);
        	return findElementonSameRow(parentElement,type, index,findExactly);
        }

        // Return the element at the specified index
         if(findExactly > -1 && elements.size() != findExactly) {
        	 return null;
         }
         
         WebElement we = elements.get(index - 1);
         el.logExecution("Element being returned for Same row search");
         printWebElementInfo(we);
         
        return we;
    }
	
	public WebElement findParentElement(WebElement textElement) {
		// Find the parent element of the text element
        WebElement parentElement = textElement.findElement(By.xpath("./.."));
        
        if(parentElement == null) {
        	return null;
        }
        
        el.logExecution("The Parent Element");
        printWebElementInfo(parentElement);
        return parentElement;
	}
	
	public WebElement findUniqueImage(String uniquetext, String uniquetoparent, 
			String parenttotarget, ExecutionLogger el) {
		
		el.logExecution("Using the proximity approach : ");
		el.logExecution("Looking for Text : " + uniquetext);
		WebElement textElement = null;
		try {
			textElement = driver.findElement(By.xpath("//*[text()= '" + uniquetext + "']"));
		} catch (Exception e) {
			el.logExecution("Did not find the Text : " + uniquetext);
//			printWebElementInfo(textElement);
			throw e;
		}		
		el.logExecution("Found the Text : " + uniquetext);
		
		if(uniquetoparent != null && uniquetoparent.equals(".")) {
			el.logExecution("unique to parent is refrring to the same node so exiting : " + uniquetoparent);
			return textElement;
		}
		
		el.logExecution("Looking for Node : " + uniquetoparent);
		WebElement parentElement = null;
		try {
			parentElement = textElement.findElement(By.xpath(uniquetoparent));
		} catch (Exception e) {
			el.logExecution("Did not find the Node : " + uniquetoparent);
			el.logExecution("Printing Text Node details : ");
			printWebElementInfo(textElement);
			throw e;
		}
//		printWebElementInfo(parentElement);
		el.logExecution("Found the Node : " + uniquetoparent);
		
		el.logExecution("Looking for Element : " + parenttotarget);
		WebElement targetElement = null;
		try {
			targetElement = parentElement.findElement(By.xpath(parenttotarget));
		} catch (Exception e) {
			el.logExecution("Did not find the Element : " + parenttotarget);
			el.logExecution("Printing Text Node details : " );
			printWebElementInfo(textElement);
			el.logExecution("Printing Parent element details : " );
			printWebElementInfo(parentElement);
			throw e;
		}
		el.logExecution("Found the Element : " + parenttotarget);
		
		return targetElement;
	}
	
	public WebElement findElementonSameRowElement(WebElement textElement, String xpath) {
		
        // Find the parent element of the text element
        WebElement parentElement = textElement.findElement(By.xpath("./.."));
        
        if(parentElement == null) {
        	return null;
        }
        
        el.logExecution("The Parent Element");
        printWebElementInfo(parentElement);
        
        // Find the checkbox element within the parent element
        WebElement element = null;
        try {
        	element = parentElement.findElement(By.xpath(".//" + xpath));
        } catch (Exception e) {
            // If no checkbox is found, continue searching recursively up the DOM tree
        	return findElementonSameRowElement(parentElement,xpath);
        }
         
         printWebElementInfo(element);
         
        return element;
    }
	
	public void fixDistance(String d0xpath, String xpath, int tobeChangedIndex, int currentIndex, 
			JSONObject steps) {
		try {
			JSONObject distance = findDistance(d0xpath, xpath, tobeChangedIndex);
			String dnextxpath = xpath.substring(xpath.indexOf("|") + 1, xpath.length());
			
			int offset = (int)distance.get("offset");
			
			String dnextxpath1 = (String)distance.get("p1");
			int d0xpath2int = (int)distance.get("p2");
			String dnextxpath3 = (String)distance.get("p3");
			
			String newD0xpath = dnextxpath1 + d0xpath2int + dnextxpath3;
			JSONObject newD0Step = (JSONObject) steps.get(tobeChangedIndex + 2);
			newD0Step.put("Object_Xpath", newD0xpath);
			
			int dNextxpath2int = d0xpath2int + offset;
			String newDNextxpath = dnextxpath1 + dNextxpath2int + dnextxpath3;
			
			JSONObject newDnextStep = (JSONObject) steps.get(currentIndex + 2);
			newDnextStep.put("Object_Xpath", newDNextxpath);

		}catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	public JSONObject findDistance(String d0xpath, String dnextxpath, int index) {
		JSONObject distance = distanceMap.get(index);
		
		if(distance != null) {
			return distance;
		} else {
			distance = new JSONObject();
		}		
		
//		String d0xpath = varMap.get(AppProperties.delimiter + "D0" + AppProperties.delimiter);
		String d0xpath1 = "";
		String d0xpath2 = d0xpath;
		
		if(d0xpath.contains("|")) {
			d0xpath1 = d0xpath.substring(0,d0xpath.indexOf("|"));
			d0xpath2 = d0xpath.substring(d0xpath.indexOf("|") + 1, d0xpath.length());
		}
		
//		String dnextxpath = xpath;
		String dnextxpath1 = "";
		String dnextxpath2 = dnextxpath;
		if(dnextxpath.contains("|")) {
			dnextxpath1 = dnextxpath.substring(0,dnextxpath.indexOf("|"));
			dnextxpath2 = dnextxpath.substring(dnextxpath.indexOf("|") + 1, dnextxpath.length());
		}
		
		
		int brokenIndex = findBrokenIndex(d0xpath2, dnextxpath2, 0);
		
		String xpath1 = dnextxpath2.substring(0,brokenIndex);
		String xpath2 = dnextxpath2.substring(brokenIndex, dnextxpath2.length());
		String dopath1 = d0xpath2.substring(0,brokenIndex);
		String dopath2 = d0xpath2.substring(brokenIndex,d0xpath2.length());
		int offset = 0;
		String dnextxp1="";
		String dnextxp2="";
		String dnextxp3="";
		int dnextxp2int =-1;
		
		if(xpath1.equals(dopath1)) {
			int diffIndex = -1;
			if(xpath2.length() > dopath2.length()) {
				diffIndex = xpath2.indexOf(dopath2);
				String diffStr = xpath2.substring(0,diffIndex);
				if(diffStr.startsWith("[") && diffStr.endsWith("]")) {
					diffStr = diffStr.substring(1,diffStr.length() - 1);
					int diff = Integer.valueOf(diffStr);
					offset = diff - 1;
					brokenIndex = brokenIndex + 1;
				}
				dnextxp1 = dnextxpath2.substring(0,brokenIndex);
				dnextxp2 = dnextxpath2.substring(brokenIndex,dnextxpath2.indexOf("]",brokenIndex));
				dnextxp3 = dnextxpath2.substring(dnextxpath2.indexOf("]",brokenIndex),dnextxpath2.length());
				dnextxp2int = Integer.valueOf(dnextxp2);
				dnextxp2int = dnextxp2int;
				
			} else if(xpath2.length() == 0) {
				String diffStr = dopath2;
				if(diffStr.startsWith("[")) {
					diffStr = diffStr.substring(1,diffStr.indexOf("]"));
					int diff = Integer.valueOf(diffStr);
					offset = diff - 1;
					brokenIndex = brokenIndex + 1;
				}
				
				dnextxp1 = d0xpath2.substring(0,brokenIndex);
				dnextxp2 = d0xpath2.substring(brokenIndex,d0xpath2.indexOf("]",brokenIndex));
				dnextxp3 = d0xpath2.substring(d0xpath2.indexOf("]",brokenIndex),d0xpath2.length());
				dnextxp2int = Integer.valueOf(dnextxp2);
				dnextxp2int = dnextxp2int;
			}
		}
		
		
		
		
		distance.put("offset", offset);
		distance.put("p1", dnextxp1);
		distance.put("p2", dnextxp2int);
		distance.put("p3", dnextxp3);
		
		distanceMap.put(index, distance);
		
		return distance;
	}
	
	private int findBrokenIndex(String doxpath2, String dnextxpath2, int brokenIndex) {
		
		int doxpath2len = -1;
		
		if(doxpath2.length() < dnextxpath2.length()) {
			doxpath2len = doxpath2.length();
		} else {
			doxpath2len = dnextxpath2.length();
		}
		
		if(doxpath2.length() == 1 || dnextxpath2.length() == 1) {
			if(doxpath2.equals(dnextxpath2)) {
				brokenIndex = brokenIndex + 1;
			} else if(doxpath2.startsWith(dnextxpath2) || dnextxpath2.startsWith(doxpath2)) {
				brokenIndex = brokenIndex + 1;
			}
			
			return brokenIndex;
		}
		int dividedNumber;

		if (doxpath2len % 2 == 0) {
		    // number is even
		    dividedNumber = doxpath2len / 2;
		} else {
		    // number is odd
		    dividedNumber = (doxpath2len + 1) / 2;
		}
		
		String doxpath2part1 = doxpath2.substring(0,dividedNumber);
		String dnextxpath2part1 = dnextxpath2.substring(0,dividedNumber);
		
		if(doxpath2part1.equals("") || dnextxpath2part1.equals("")) {
			return brokenIndex;
		}
		
		if(doxpath2part1.equals(dnextxpath2part1)) {
			String doxpath2part2 = doxpath2.substring(dividedNumber,doxpath2.length());
			String dnextxpath2part2 = dnextxpath2.substring(dividedNumber,dnextxpath2.length());
			brokenIndex = brokenIndex + dividedNumber;
			brokenIndex= findBrokenIndex(doxpath2part2, dnextxpath2part2,brokenIndex);
		} else {
			brokenIndex = findBrokenIndex(doxpath2part1, dnextxpath2part1,brokenIndex);
		}
		
		return brokenIndex;
	}
	
	public void printWebElementInfo(WebElement element) {
		try {
		    String text = element.getText();
		    String tagName = element.getTagName();
		    String href = element.getAttribute("href");
	//	    Point location = element.getLocation();
	//	    Dimension size = element.getSize();
		    el.logExecution("-------------------------------------------------------------------------------------------------------");
		    el.logExecution("Text: " + text);
		    el.logExecution("Tag name: " + tagName);
		    el.logExecution("Href attribute: " + href);
	//	    el.logExecution("Location: (" + location.x + ", " + location.y + ")");
	//	    el.logExecution("Size: " + size.width + " x " + size.height);
	//	    el.logExecution("Is Displayed: " + element.isDisplayed());
	//	    el.logExecution("Is Enabled: " + element.isEnabled());
	//	    el.logExecution("Is Selected: " + element.isSelected());
		    el.logExecution("Class Attribute: " + element.getAttribute("class"));
		    el.logExecution("Src Attribute: " + element.getAttribute("src"));
		    el.logExecution("-------------------------------------------------------------------------------------------------------");
		} catch (Exception e) {
			el.logExecution("Error in logging info");
		}
	}
	
	public String determineExecStatus(long exec_startTime, long threshold) {

		long exec_endTime = System.currentTimeMillis();
		long exec_duration = exec_endTime - exec_startTime;
		if(threshold > 0 && exec_duration > (threshold * 1000)) {
			return "FAIL";
		}
		
		return "PASS";
	}
	
	public int getThreshold(int tcThreshHold, int tsThreshHold) {
		if(tsThreshHold > 0 && tcThreshHold > 0) {
			if(tsThreshHold <= tcThreshHold) {
				return tsThreshHold;
			} else  {
				return tcThreshHold;
			}
		} else if(tsThreshHold > 0 && tcThreshHold <= 0) {
			return tsThreshHold;
		} else if(tsThreshHold <= 0 && tcThreshHold > 0) {
			return tcThreshHold;
		}
		
		return -1;
	}
	
	
	public void dnd(WebElement sourceElement, WebElement targetElement) {
		String script = "function createEvent(typeOfEvent) {\n" +
                "    var event = document.createEvent(\"CustomEvent\");\n" +
                "    event.initCustomEvent(typeOfEvent, true, true, null);\n" +
                "    event.dataTransfer = {\n" +
                "        data: {},\n" +
                "        setData: function (key, value) {\n" +
                "            this.data[key] = value;\n" +
                "        },\n" +
                "        getData: function (key) {\n" +
                "            return this.data[key];\n" +
                "        }\n" +
                "    };\n" +
                "    return event;\n" +
                "}\n" +
                "\n" +
                "function dispatchEvent(element, event, transferData) {\n" +
                "    if (transferData !== undefined) {\n" +
                "        event.dataTransfer = transferData;\n" +
                "    }\n" +
                "    if (element.dispatchEvent) {\n" +
                "        element.dispatchEvent(event);\n" +
                "    } else if (element.fireEvent) {\n" +
                "        element.fireEvent(\"on\" + event.type, event);\n" +
                "    }\n" +
                "}\n" +
                "\n" +
                "function simulateHTML5DragAndDrop(element, target) {\n" +
                "    var dragStartEvent = createEvent('dragstart');\n" +
                "    dispatchEvent(element, dragStartEvent);\n" +
                "    var dropEvent = createEvent('drop');\n" +
                "    dispatchEvent(target, dropEvent, dragStartEvent.dataTransfer);\n" +
                "    var dragEndEvent = createEvent('dragend');\n" +
                "    dispatchEvent(element, dragEndEvent, dropEvent.dataTransfer);\n" +
                "}\n" +
                "\n" +
                "var sourceElement = arguments[0];\n" +
                "var targetElement = arguments[1];\n" +
                "simulateHTML5DragAndDrop(sourceElement, targetElement);";

			((JavascriptExecutor) driver).executeScript(script, sourceElement, targetElement);
	}
	
	public String getVarValue(String var) {
		return varMap.get(var);
	}
	
	public static String getStackTraceAsString(Throwable throwable) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        throwable.printStackTrace(pw);
        return sw.toString();
    }
	
	public void enterRightDate(int stepnum, String testData, WebElement we, String xpath, int testStepId) {
		String formattedDate = null;
		String dateFmt = null;
		
		// Remove this inhibtor if present
		JavascriptExecutor js = (JavascriptExecutor) driver;
		js.executeScript("arguments[0].removeAttribute('onkeydown')", we);
		
		if(testData.startsWith("(") && testData.endsWith(")")) {
			String dateStr = testData.substring(1,testData.indexOf(","));
			dateStr = dateStr.trim();
			dateFmt = testData.substring(testData.indexOf(",")+1, testData.indexOf(")") - 1);
			dateFmt = dateFmt.trim();
			el.logExecution(
					"Step  " + stepnum + " date = " + dateStr + " date format = " + dateFmt);
			formattedDate = java.time.LocalDate.parse(dateStr).format(DateTimeFormatter.ofPattern(dateFmt));
			try {
				we.sendKeys(formattedDate);
			} catch (Exception e) {
				el.logExecution(
					"Step  " + stepnum + " send keys failed entering " + dateStr + " to date field in format" + dateFmt);
				throw e;
			}
			
			el.logExecution(
					"Step  " + stepnum + " successfully enter date = " + dateStr + " and format = " + dateFmt);
			
		} else {		
			el.logExecution(
					"Step  " + stepnum + " entering " + testData + " to date field " + xpath);
			dateFmt = "MM-dd-yyyy";
			formattedDate = java.time.LocalDate.parse(testData).format(DateTimeFormatter.ofPattern(dateFmt));
			try { 
				we.clear();
				we.sendKeys(formattedDate);
				String enteredValue = we.getAttribute("value");
				if(!enteredValue.equals(testData)) {
					el.logExecution(
							"Step  " + stepnum + " date format is not " + dateFmt +", so trying dd-MM-yyyy " + testData + " to date field " + xpath);
					dateFmt = "dd-MM-yyyy";
					formattedDate = java.time.LocalDate.parse(testData).format(DateTimeFormatter.ofPattern(dateFmt));
					we.sendKeys(formattedDate);
					enteredValue = we.getAttribute("value");
					if(!enteredValue.equals(testData)) {
						dateFmt = "yyyy-MM-dd";
						el.logExecution(
								"Step  " + stepnum + " date format is not " + dateFmt + " either " + testData + " to date field " + xpath);
						el.logExecution("Trying fall back method");
						we.clear();
						js = (JavascriptExecutor) driver;
						js.executeScript("arguments[0].value='" + testData + "';", we);
						enteredValue = we.getAttribute("value");
						if(!enteredValue.equals(testData)) {
							el.logExecution("Fall back method also did not work - " + enteredValue);
						} else {
							el.logExecution("Fall back method worked - " + enteredValue);
						}
					} else {
						el.logExecution(
								"Step  " + stepnum + " date format is " + dateFmt +  "either " + testData + " to date field " + xpath);
					}
				}
			} catch (Exception e) {
				el.logExecution(
					"Step  " + stepnum + " send keys failed entering " + testData + " to date field " + xpath);
				throw e;
			}
			el.logExecution(
					"Step  " + stepnum + " successfully entered " + testData + " to date field " + xpath);
			String updatedTestData = "(" + testData + "," + dateFmt + ")";
//			msc.updateTestStepChangedData(testStepId, "", updatedTestData);
		}
	}
	
	public void enterDateValue() {
		
	}
	

	public void enterRightDateTimeLocal(int stepnum, String testData, WebElement we, String xpath, int testStepId) {
	    String formattedDateTime = null;
	    String dateTimeFmt = null;

	    // Remove this inhibitor if present
	    //JavascriptExecutor js = (JavascriptExecutor) driver;
	   // js.executeScript("arguments[0].removeAttribute('ng-disabled')", we);

	        el.logExecution(
	            "Step  " + stepnum + " entering " + testData + " to datetime-local field " + xpath);
	        try {
	        	((JavascriptExecutor) driver).executeScript("arguments[0].value='" + testData + "';", we);
	        	//we.sendKeys(testData);
	        	String enteredValue = we.getAttribute("value");
	            if (!enteredValue.equals(testData)) {
	                el.logExecution("Step  " + stepnum + " datetime format is not working");
	            } else {
	            	el.logExecution("Step  " + stepnum + " datetime was entered as " + enteredValue);
	            }
	        } catch (Exception e) {
	            el.logExecution(
	                "Step  " + stepnum + " send keys failed entering " + testData + " to datetime-local field " + xpath);
	            throw e;
	        }
	        el.logExecution(
	            "Step  " + stepnum + " successfully entered " + testData + " to datetime-local field " + xpath);
	}
	
	public void getElementAttrs(WebElement element) {
		// Get and print specific attributes
        String idAttribute = element.getAttribute("id");
        String classAttribute = element.getAttribute("class");
        String hrefAttribute = element.getAttribute("href");
        String disabledAttribute = element.getAttribute("disabled");
        String valueAttribute = element.getAttribute("value");
        String typeAttribute = element.getAttribute("type");

//        System.out.println("ID: " + idAttribute);
//        System.out.println("Class: " + classAttribute);
//        System.out.println("Href: " + hrefAttribute);
//        System.out.println("disabled: " + disabledAttribute);
//        System.out.println("value: " + valueAttribute);
//        System.out.println("type: " + typeAttribute);
	}
	
	public boolean readyForNext(JSONObject step, int synchResultid,  JSONObject synchObj,
			MySQlConn msc, ExecutionLogger el, JSONObject origSteps, Tester tester, 
			int testCaseResultsId, String tc_status){
		
		tc_status = stopExecution(tester, testCaseResultsId, tc_status);
		
		if(tc_status.equals("STOPP")) {
			el.logExecution(" exiting with false execution has been requested to be stopped  -" + synchResultid + "--" + 
					"STOPP");
			return false;
		}
		
		long startTime = System.currentTimeMillis();
		
		long stepActualTime = (long)step.get("actualtime");
		int stepStepNum = (int)step.get("Step_Number");
		el.logExecution(" The actual time for the step and step number waiting to be "
				+ "executed is -" + stepActualTime + "--" + stepStepNum);
		
		JSONObject synchsteps = (JSONObject) synchObj.get("data");
		int synchStepNum = -1;
		int synchtestCaseId = -1;
		
		for(int i=2;i<synchsteps.size();i++) {
			JSONObject synchStep = (JSONObject)synchsteps.get(i);
			long synchStepActualTime = (long)synchStep.get("actualtime");
		if(synchStepActualTime >= stepActualTime) {
				synchtestCaseId = (int) synchStep.get("Test_Case_Id");
				el.logExecution(" The actual time for the Synch Object step and Synch Object "
						+ "step number that has been found is  -" + synchStepActualTime + "--" + 
						synchStepNum);
				break;
			} else {
				synchStepNum = (int)synchStep.get("Step_Number");
			}
			el.logExecution(" The actual time for the Synch Object step and Synch Object "
					+ "step number processed now is -" + synchStepActualTime + "--" + 
					synchStepNum);
		}
		
		JSONObject user = msc.getUserDetails();
		
		int companyId = (int)user.get("companyid");
		JSONArray synchtsr= null;
		do {
			String synchStatus = msc.getTestCaseResultStatus(synchResultid);
			el.logExecution(" The status of this run is  -" + synchResultid + "--" + 
					synchStatus);
			
			if(synchStatus != null && synchStatus.equals("FAIL")) {
				el.logExecution(" exiting with false as it has failed  -" + synchResultid + "--" + 
						synchStatus);
				return false;
			}
			
			synchtsr = msc.getTestStepResultFromTestCaseResult(synchResultid, true);
			if(synchtsr != null && synchtsr.size() > 0) {
				JSONObject synchLastStep = (JSONObject)synchtsr.get(0);
				int synchResultStepNum = (int)synchLastStep.get("Step_Number");
				el.logExecution("The other test case has completed Step  -" + synchResultid + "--" + 
						synchResultStepNum);
				
				if(synchResultStepNum >= synchStepNum) {
					el.logExecution(" exiting with true as condition satisfied  -" + synchResultid + "--" + 
							synchStatus);
					return true;
				}
				
				for(int i=2;i<synchsteps.size();i++) {
					JSONObject synchStep2 = (JSONObject)synchsteps.get(i);					
					int synchStepNum2 = (int)synchStep2.get("Step_Number");
					
					if(synchStepNum2 == synchResultStepNum) {
						long synchStepActualTime2 = (long)synchStep2.get("actualtime");
						JSONObject prevStep = (JSONObject)origSteps.get(i + 1);
						long prevStepTime = (long)prevStep.get("actualtime");
						if(stepActualTime <= synchStepActualTime2) {
							el.logExecution(" exiting with true as this one still needs to progress  -" + synchResultid + "--" + 
									synchStatus);
							return true;
						}
					} else if (synchStepNum2 > synchResultStepNum) {
						break;
					}
				}
			}
			
			long endTime = System.currentTimeMillis();
			long elapsedTime = endTime - startTime;
			
			 if (elapsedTime > 300000) {
				 el.logExecution(" exiting with false as meth0d timed out 300s  -" + synchResultid + "--" + 
							"false");
				 return false;
	            }
				
			try {
				el.logExecution(" sleeping for 2 seconds");
				Thread.sleep(2000);
			} catch (Exception e) {
				el.logExecution("thread6 sleeping for 2000 interupted");
			}	
			
		}while(true);
	}
	
	public String stopExecution(Tester tester, int testCaseResultsId, String tc_status) {
		if(tsr > -1) {
			if(Utilities.stopSuiteRequestCache.contains(tsr)) {
//				tc_status = "STOPP";
				tester.continueExecution = false;
				return "STOPP";
			}
		} 
		
		if(testCaseResultsId > -1) {
			if(Utilities.stopCaseRequestCache.contains(testCaseResultsId)) {
//				tc_status = "STOPP";
				tester.continueExecution = false;
				return "STOPP";
			}
		}
		
		return tc_status;
	}
	
	public void takeCoverageSS(int companyId, int prodId, int pageId) {
		File screenshot = null;
		try {
	        // Add a 2-second wait before taking the screenshot
	        Thread.sleep(2000);
	        
			screenshot = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
		} catch(Exception e) {
			e.printStackTrace();
		}
		
		String fsl = Utilities.getCoverageSSPath(companyId, prodId, pageId);
		String name = Utilities.getCoverageSSFileName(companyId, prodId, pageId);
		
	    File file = new File(fsl);
	    if (file.exists()) {
	        // If the file exists, move it to a backup folder
	        String backupFolderPath = Utilities.getBackupSSPath(companyId, prodId);
	        String backupFilePath = backupFolderPath + "/" + name; // Backup file path
	        
	        File backupFolder = new File(backupFolderPath);
	        if (!backupFolder.exists()) {
	            backupFolder.mkdirs();
	        }
	        
	        // Move existing file to the backup folder
	        File backupFile = new File(backupFilePath);
	        boolean fileMoved = file.renameTo(backupFile);
	        if (!fileMoved) {
	            // Handle file move failure
	            System.out.println("XXXXXXXXXXXXXXXXXXXXXX  Failed to move the existing file to the backup folder.  XXXXXXXXXXXXXXXXXXXXXX");
	        }
	    }
	    
		try {
			FileUtils.copyFile(screenshot, new File(fsl));
			msc.updateCoverageSSPath(name, pageId);
		}catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public WebElement getNearestForNLP(JSONObject step, WebElement we, String keyword, String action) {
		
		if(keyword.equalsIgnoreCase("editbox") && action.equalsIgnoreCase("enter")) {
			we = we.findElement(By.xpath("./following::input[1]"));
		}
		return we;
	}
	
	public String getFromClipBoard() {
		
		String clipboardText = "";
		Toolkit toolkit = Toolkit.getDefaultToolkit();
		
		// Get the System Clipboard
        Clipboard clipboard = toolkit.getSystemClipboard();
        
     // Get the clipboard's contents
        Transferable clipboardData = clipboard.getContents(null);
        
     // Check if clipboard has data
        if (clipboardData != null && clipboardData.isDataFlavorSupported(DataFlavor.stringFlavor)) {
            try {
                // Get clipboard data as string
                clipboardText = (String) clipboardData.getTransferData(DataFlavor.stringFlavor);

                // Print or process the clipboard text
                System.out.println("Clipboard text: " + clipboardText);

                // Now you can assert or perform actions based on the clipboard text
            } catch (UnsupportedFlavorException | IOException e) {
                e.printStackTrace();
            }
        } else {
            System.out.println("Clipboard does not contain string data");
        }
        
        return clipboardText;
	}
	
	public String callAPI(JSONObject step, String randomKey, String src) {
		el.logExecution("Processing From API");
		JSONObject apiInfo = msc.getApi((int)step.get("apiid"));
		String url = (String)apiInfo.get("url");
		el.logExecution("Calling URL : " + url);
		String headers = (String)apiInfo.get("headers");
		el.logExecution("The headers being passed are   : " + headers);
		String param = (String)step.get("apiparam");
		el.logExecution("Parameters being used are  : " + param);
		String query = (String)step.get("DBQuery");
		el.logExecution("The response will be processed as  : " + query);
		String body = (String)apiInfo.get("body");
		el.logExecution("The body of the api is  : " + body);
		String type = (String)apiInfo.get("type");
		el.logExecution("The type of api call is  : " + type);
		String tests = (String)apiInfo.get("tests");
		el.logExecution("The tests to be asserted ar  : " + tests);
		
		if(param!=null && param.contains("action=triggerBulkApi")) {
			param = param.replaceAll("token=\\w+", "token=" + randomKey);
		}
		if(param!=null && !param.equals("")) {
			url = url + param;
		}
		String res = "";
		Object resObj = null;
		
		if(src != null) {
			url = replaceWithVar(url);
			param = replaceWithVar(param);
			query = replaceWithVar(query);
			body = replaceWithVar(body);
			headers = replaceWithVar(headers);
			el.logExecution("----------------------After Replacing any variables-----------------------");
			el.logExecution("Final URl :" + url);
			el.logExecution("Final Param :" + param);
			el.logExecution("Final headers :" + headers);
			el.logExecution("Final body :" + body);
			el.logExecution("Final response processing query :" + query);
			el.logExecution("--------------------------------------------------------------------------");
			if(src.equalsIgnoreCase("FromApi") || src.equalsIgnoreCase("InAPi")) {
				if(type != null && type.equalsIgnoreCase("post")) {
					resObj =  ApiAccess.postToApi(url, param , query, body, headers, el, varMap);
				} else {
					resObj =  ApiAccess.callApiTD(url, param, query, el, varMap, headers);
				}
				el.logExecution("The response after processing and value assigned to testdata is   : " + res);
				if (query != null && !query.equals("")) {
					return ApiAccess.getDataFromJSON(resObj, query, el);
				} else {		
					return resObj.toString();
				}
			} else if(src.equalsIgnoreCase("Test")){
				res =  ApiAccess.testPostApi(url, param , query, body, headers, el, varMap,tests);
				return res;
			}
		}
		
		return null;
	}
	
	 public static void highlightPixel(WebDriver driver, int x, int y) {
	        JavascriptExecutor js = (JavascriptExecutor) driver;

	        // Execute JavaScript to create a small red dot at the specified pixel coordinate
	        js.executeScript("var div = document.createElement('div');"
	                         + "div.setAttribute('id', 'highlightedPixel');"
	                         + "div.style.width='15px';"
	                         + "div.style.height='15px';"
	                         + "div.style.borderRadius='50%';"
	                         + "div.style.background='red';"
	                         + "div.style.position='absolute';"
	                         + "div.style.left='" + x + "px';"
	                         + "div.style.top='" + y + "px';"
	                         + "document.body.appendChild(div);");
	    }

	    // Method to remove the highlighted pixel
	    public static void removeHighlight(WebDriver driver) {
	        JavascriptExecutor js = (JavascriptExecutor) driver;

	        // Execute JavaScript to remove the highlighted pixel by its ID
	        js.executeScript("var elem = document.getElementById('highlightedPixel');"
	                         + "if (elem) elem.parentNode.removeChild(elem);");
	    }

	    public static WebElement smartFindElement(WebDriver driver, By locator, int timeoutInSeconds, ExecutionLogger el) {
	        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(timeoutInSeconds));

	        try {
	            // Wait until document.readyState === 'complete'
	            wait.until(webDriver -> ((JavascriptExecutor) webDriver)
	                    .executeScript("return document.readyState").equals("complete"));

	            // Once page is fully loaded, wait for element to be present
	            return wait.until(ExpectedConditions.presenceOfElementLocated(locator));
	        } catch (TimeoutException e) {
	            el.logExecution("Timeout: Page may not be fully loaded or element doesn't exist.");
	        } catch (NoSuchElementException e) {
	        	el.logExecution("Element not found.");
	        }
	        return null;
	    }

	    public WebElement findLocatorForAiGenerated(WebDriver driver, String xpath) {
	    	WebElement we = null;
	    	try {
				we = driver.findElement(By.xpath(xpath));
				if(!(we.getTagName().equalsIgnoreCase("input") || (we.getAttribute("type").equalsIgnoreCase("input")))) {
					we = getNearestInput(we);
				}
			} catch (Exception en) {
				xpath = xpath.replace("text()", "@placeholder");
				try {
					we = driver.findElement(By.xpath(xpath));
					if(!(we.getTagName().equalsIgnoreCase("input") || (we.getAttribute("type").equalsIgnoreCase("input")))) {
						we = getNearestInput(we);
					}
				} catch(Exception e) {
					
				}
			}
	    	return we;
	    }
	    
	    public WebElement getNearestInput(WebElement we) {
	    	WebElement parentElement = null;
	    	try {
	    		parentElement = we.findElement(By.xpath("./.."));
	    	} catch(Exception e) {
	    		
	    	}
	    	if(parentElement == null) {
	    		return null;
	    	}
	    	WebElement input = null;
	    	try {
	    		input = parentElement.findElement(By.tagName("input"));
	    	} catch(Exception e) {
	    		return getNearestInput(parentElement);
	    	}
	    	return input;
	    }
	    
	    public void checkForLongRunningAPIs(Devtools devtools) {
	    	if(AppProperties.autowaitforapi.equalsIgnoreCase("false")) {
	    		el.logExecution("Check for long running apis for test step id is turned off : ");
	    		return;
	    	}
	    	long start = System.currentTimeMillis();
	    	boolean pendingapis = true;				
			if(devtools != null && devtools.tsrid != -1) {
				long elapsedTime = 0;
				do {
					el.logExecution("Check for long running apis for test step id : " + devtools.tsrid);
					pendingapis = msc.getPendingAPICalls(devtools.tcrid, devtools.tsid);
					el.logExecution("Are there any long running apis  for test step results id: " + devtools.tsrid + " : " +
					pendingapis);
					el.logExecution("Total time execution will wait for api to complete is (ms): " + AppProperties.apidefaultwaittime );
					if(pendingapis) {
						el.logExecution("Waiting for 5 seconds for long running apis to complete for test step id : " + devtools.tsrid);
						try {
							Thread.sleep(5000);
						} catch (Exception e) {
							
						}
					}
					elapsedTime = System.currentTimeMillis() - start;
					el.logExecution("Long Runnign API check - Elapsed Time (ms) : " + elapsedTime + "out of max wait time of : " + AppProperties.apidefaultwaittime);
				} while(pendingapis && (elapsedTime < AppProperties.apidefaultwaittime));
				el.logExecution("No longer waiting for api to complete as");
				el.logExecution("Total time that execution waited for the api to complete is : " + 
						elapsedTime + "out of max wait time of : " + 
						AppProperties.apidefaultwaittime);
				el.logExecution("Are there any long running APIs : " + pendingapis);
			} else {
				el.logExecution("Skipped running the long running api check");
				if(devtools == null) {
					el.logExecution("API Capture has not been initiated was null");
				} else {
					el.logExecution("test step result id is : " + devtools.tsrid);
				}
			}
	    }
	    
	    public void clickOnEdgesOfElement(WebDriver driver, WebElement we, String edge) {
	    	JavascriptExecutor js = (JavascriptExecutor) driver;
	    	
	    	if (edge.equalsIgnoreCase("bottom-right")) {
				js.executeScript(
				    "var rect = arguments[0].getBoundingClientRect();" +
				    "var x = rect.right - 1;" +       
				    "var y = rect.bottom - 1;" +         
				    "var clickEvent = new MouseEvent('click', {" +
				    "  bubbles: true," +
				    "  cancelable: true," +
				    "  clientX: x," +
				    "  clientY: y" +
				    "});" +
				    "arguments[0].dispatchEvent(clickEvent);", 
				    we
				);
	    	} else if (edge.equalsIgnoreCase("bottom-left")) {
				js.executeScript(
				    "var rect = arguments[0].getBoundingClientRect();" +
				    "var x = rect.left + 1;" +       
				    "var y = rect.bottom - 1;" +         
				    "var clickEvent = new MouseEvent('click', {" +
				    "  bubbles: true," +
				    "  cancelable: true," +
				    "  clientX: x," +
				    "  clientY: y" +
				    "});" +
				    "arguments[0].dispatchEvent(clickEvent);", 
				    we
				);
		    } else if (edge.equalsIgnoreCase("top-right")) {
				js.executeScript(
				    "var rect = arguments[0].getBoundingClientRect();" +
				    "var x = rect.right - 1;" +       
				    "var y = rect.top + 1;" +         
				    "var clickEvent = new MouseEvent('click', {" +
				    "  bubbles: true," +
				    "  cancelable: true," +
				    "  clientX: x," +
				    "  clientY: y" +
				    "});" +
				    "arguments[0].dispatchEvent(clickEvent);", 
				    we
				);
		    } else if (edge.equalsIgnoreCase("top-left")) {
				js.executeScript(
				    "var rect = arguments[0].getBoundingClientRect();" +
				    "var x = rect.left + 1;" +     
				    "var y = rect.top + 1;" +       
				    "var clickEvent = new MouseEvent('click', {" +
				    "  bubbles: true," +
				    "  cancelable: true," +
				    "  clientX: x," +
				    "  clientY: y" +
				    "});" +
				    "arguments[0].dispatchEvent(clickEvent);", 
				    we
				);
		    } 	    	
	    }
	    
	    public boolean processErrorAPI(JSONObject step, int testCaseResultsId,
	    		String url, int test_step_id, ExecutionLogger el, Devtools devtools) {
	    	el.logExecution("Processing Error API for test step id : " + test_step_id);
	    	JSONObject apiDataJSON = (JSONObject) step.get("apiData");
            String shouldRetry = null;
            JSONObject retryConfig = null;
            try {
	            if(apiDataJSON != null && !apiDataJSON.equals("")) {
	            	el.logExecution("APIs to be focussed on are : " + apiDataJSON);
	            	JSONArray apiEndPoints = (JSONArray) apiDataJSON.get("endpoints");
		            
	            	for(Object obj:apiEndPoints) {
		            	JSONObject json = (JSONObject) obj;
		            	String urlToBeCalled = (String) json.get("url");
		            	retryConfig = (JSONObject) json.get("retryConfig");
		            	JSONArray statusCodes = (JSONArray) retryConfig.get("statusCodes");
		            	JSONObject failureConfig = (JSONObject) json.get("failureConfig");
		            	List<JSONObject> apisCalled = msc.getApiDataForRecentStep(testCaseResultsId, 
		            			test_step_id, devtools.attempt);
		            	el.logExecution("API called for this attempt for this test step are : " + apisCalled);
		            	for(JSONObject it:apisCalled) {
		            		if(ApiData.areApiCallsEquivalent((String) it.get("url"), urlToBeCalled)) {
		            			el.logExecution("This API matched : API that was called is : " + it.get("url") + 
		            					" : API that was on the match list is : " + urlToBeCalled);
		            			if(statusCodes.contains(String.valueOf(it.get("statuscode")))) {
		            				el.logExecution("Status code that matched is : " +  it.get("statuscode"));
		            				long waitTimeToRetry = (long) retryConfig.get("waitTime");
		            				try {
		            					el.logExecution("waiting for (s) : " +  waitTimeToRetry);
		            					Thread.sleep(waitTimeToRetry * 1000); 
		            				} catch(Exception wte) {
		            					
		            				}
		            				long retryCount = (long) retryConfig.get("retryCount");
		            				el.logExecution("Attempt number : " + devtools.attempt);
		            				if(retryCount > 0) {
		            					retryConfig.put("retryCount", retryCount-1);
		            					shouldRetry = "true";
		            					break;
		            				} else {
		            					String actionAfterRetry = (String) failureConfig.get("action");
		            					if(actionAfterRetry.equals("notify") || actionAfterRetry.equals("both")) {
		            						JSONArray notifyOptions = (JSONArray) failureConfig.get("notifications");
		            						for(Object notOption:notifyOptions) {
		            							JSONObject jsonNotify = (JSONObject) obj;
		            							String intName = (String) jsonNotify.get("provider");
		            							String webHookUrl = (String) jsonNotify.get("webhookUrl");
		            							String msg = "";
		            							if (intName.equals("MS Teams Channel")) {
		            								ApiAccess.postToApi(url, null, null, msg, null, el, null);
		            				            } else if (intName.equals("azure")) {
		            				                
		            				            }
		            						}
		            					} 
		            					shouldRetry = "false";
			            				break;
		            				}
		            			}
		            		}
		            	}
		            	if(shouldRetry != null) {
            				break;
		            	} 
		            }
		            if(shouldRetry != null && shouldRetry.equals("true")) {
		            	shouldRetry = "true";
		            } else if (shouldRetry != null && shouldRetry.equals("false")) {
		            	shouldRetry = "false";
		            }
	            } else {
	            	el.logExecution("No APIs to be focussed on for errors " );
	            }
            } catch (Exception adt) {
            	adt.printStackTrace();
            }
            
            boolean ret = false;
            if(shouldRetry == null || shouldRetry.equalsIgnoreCase("false")) {
            	ret = false;
            } else {
            	ret = true;
            }
            el.logExecution("Completed Processing Error API for test step id : " + test_step_id);
            return ret;
	    }
}
