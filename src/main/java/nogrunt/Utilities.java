package nogrunt;

import java.io.File;
import java.io.FileWriter;
import java.io.FileReader;
import java.nio.file.Paths;
import java.io.FilenameFilter;
import java.util.Arrays;
import java.util.Comparator;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Properties;
import java.io.FileInputStream;

import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.ArrayList;
//import java.util.Date;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.text.ParseException;
import java.util.Calendar;
import java.sql.Date;
import java.sql.Time;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.SecureRandom;
import java.io.IOException;

public class Utilities {
	
	public static AuditQueuingHelper aqh;
	public static EmailQueuingHelper eqh;
	public static CleanUpQueueHelper cleanUpQueue;
	public static HashMap userCache = new HashMap();
	public static HashMap licenseCache = new HashMap();
	public static HashMap latestRecordingCache = new HashMap();
	public static HashMap latestResultsCache = new HashMap();
	public static HashMap zipUploadCache = new HashMap();
	private static HashMap executionCache = new HashMap();
	public static List<Integer> stopSuiteRequestCache = new ArrayList<Integer>();
	public static List<Integer> stopCaseRequestCache = new ArrayList<Integer>();
	public static HashMap dashboardData = new HashMap();
	public static HashMap mergeTestReqCache = new HashMap();
	public static HashMap mergeStepCache = new HashMap();
	public static HashMap dashboardCache = new HashMap();
	public static HashMap debugCache = new HashMap();
	public static HashMap debugMsgCache = new HashMap();
	public static HashMap executionMsgCache = new HashMap();
	//static MySQlConn msc = new MySQlConn();
	
	public static void main(String[] args) {
//		flushCache("deva@nogrunt.com","");
	}
	
	public static HashMap getExecutionCache() {
		return executionCache;
	}
	
	public static HashMap getExecutionCache(int companyid) {
		HashMap ec = (HashMap)executionCache.get(companyid);
		
		if(ec == null) {
			ec = new HashMap();
		}
		
		return ec;
	}
	
	public static boolean addExecution(int companyid, String session) {
		HashMap ec = getExecutionCache(companyid);
		ec.put(session, session);
		executionCache.put(companyid, ec);
		return true;
	}
	
	public static boolean endExecution(int companyid, String session) {
		HashMap ec = getExecutionCache(companyid);
		ec.remove(session);
		return true;
	}
	
	public static int getExecutionCount(int companyid) {
		HashMap ec = getExecutionCache(companyid);
		return ec.size();
	}
	
	public static void flushCache(String uname, String randomKey) {
		flushUserCache(randomKey);
		uncacheLatestRecording(uname);
	}
	
	public static void flushUserCache(String randomkey) {
		userCache.remove(randomkey);
	}
	
	public static void uncacheLatestRecording(String uname) {
		latestRecordingCache.remove(uname);
	}
	
	public static void cacheUser(String randomkey, JSONObject details) {
		userCache.put(randomkey,details);
	}
	
	public static JSONObject getUserFromCache(String randomkey) {
		if(userCache.get(randomkey) == null) {
			if(AppProperties.licenses.get(randomkey) == null) {
				return new JSONObject();
			} else {
				return (JSONObject)AppProperties.licenses.get(randomkey);
			}
		} else {
			return (JSONObject)userCache.get(randomkey);
		}
	}
	
	public static void cacheLicense(int companyId, JSONObject details) {
		licenseCache.put(companyId,details);
	}
	
	public static JSONObject getLicenseFromCache(int cid, MySQlConn msc, MySqlConn2 msc2) {
		if(licenseCache.get(cid) == null) {
			JSONObject json = PasswordUtils.getLicenseDetails(cid, msc, msc2);
			Utilities.cacheLicense(cid, json);
			return json;
		} else {
			return (JSONObject)licenseCache.get(cid);
		}
	}
	
	public static void uncacheLicense(int cid) {
		licenseCache.remove(cid);
	}
	
	public static boolean isUserInCache(String randomkey) {
		if(userCache.get(randomkey) == null) {
			if(AppProperties.licenses.get(randomkey) == null) {
				return false;
			} else {
				return true;
			}
		} else {
			return true;
		}
	}
	
	//32 to 44 added by sanjeev for latest recording
	public static void cacheLatestRecording(String uname, JSONObject recording) {
		latestRecordingCache.put(uname, recording);
	}
	
	public static JSONObject getdashboard (int companyId, MySQlConn msc, String randomkey) {
		
		JSONObject dbdata = new JSONObject();
		
		if(dashboardCache.containsKey(companyId)) {
			JSONObject dashboard = (JSONObject)dashboardCache.get(companyId);
			long lastexecuted = (long)dashboard.get("lastexecuted");
			
			long currentTime = System.currentTimeMillis();

			// Add the desired duration (3600 seconds) to the current date
			int dbrr = (int)dashboard.get("dashboardrefreshrate");
			if(dbrr < 0) {
				dbrr = 3600000;
			} 

			// Compare lastexecuted with the thresholdDate
			if (currentTime -lastexecuted > dbrr) {
				dbdata = msc.getDashboardData(companyId, lastexecuted);			
				
				dbdata.put("dashboardrefreshrate", dbrr);
				
				long currentTimestamp = System.currentTimeMillis();
				dbdata.put("lastexecuted", currentTimestamp);
			    
				dashboardCache.put(companyId, dbdata);
			} else {
				dbdata = dashboard;
			}
		} else {
			dbdata = msc.getDashboardData(companyId, -1);
			JSONObject compdata = msc.getCompany(companyId, randomkey);
			int dbrr = (int)compdata.get("dashboardrefreshrate");
			if(dbrr < 0) {
				dbrr = 3600000;
			}
			dbdata.put("dashboardrefreshrate", dbrr);
			
			long currentTimestamp = System.currentTimeMillis();
			dbdata.put("lastexecuted", currentTimestamp);
			
			dashboardCache.put(companyId, dbdata);
		}
		
		return dbdata;
	}
	
	public static JSONObject refreshLatestRecordingFromCache(String uname, MySQlConn msc) {
		uncacheLatestRecording(uname);
		return getLatestRecordingFromCache(uname, msc);
	}
	
	public static JSONObject getLatestRecordingFromCache(String uname, MySQlConn msc) {
	if(latestRecordingCache.get(uname) == null) {
			JSONObject json = msc.getLatestRecording(uname);
			Utilities.cacheLatestRecording(uname,json);
			return json;
		} else {
			return (JSONObject)latestRecordingCache.get(uname);
		}
	}
	//Added by sanjeev for caching upload data
	public static void cacheZipUpload(String uname, JSONObject recording) {
		zipUploadCache.put(uname, recording);
	}
	
	public static void uncacheZipUpload(String uname) {
		zipUploadCache.remove(uname);
	}
	
//	public static JSONArray refreshZipUploadFromCache(String uname, MySQlConn msc) {
//		uncacheZipUpload(uname);
//		return getZipUploadFromCache(uname, msc);
//	}
	
	public static JSONObject getZipUploadFromCache(String uname, int cid, MySQlConn msc) {
		JSONObject jsonObj = new JSONObject();
		if(zipUploadCache.get(uname) == null) {
			JSONArray json = msc.getUploadfileByCompany(cid);
			jsonObj.put("detail", json);
			jsonObj.put("refresh", "running");
			Utilities.cacheZipUpload(uname, jsonObj);
			return jsonObj;
		} else {
			return (JSONObject)zipUploadCache.get(uname);
		}
	}
	//
	
	public static void cacheStopRequest(int tsrid) {
		stopSuiteRequestCache.add(tsrid);
	}
	
	public static void cacheStopCaseRequest(int tcid) {
		stopCaseRequestCache.add(tcid);
	}
	
	public static void cachedashboardData(int cid, JSONObject recording) {
		dashboardData.put(cid, recording);
	}
	
	public static JSONObject getDashboardDataFromCache(int cid, MySQlConn msc) {
		if(dashboardData.get(cid) == null) {
			JSONObject json = msc.getDashboardData(cid, -1);
			Utilities.cachedashboardData(cid,json);
			return json;
		} else {
			return (JSONObject)dashboardData.get(cid);
		}
	}
	
	public static void cacheMergeRequest(String uname, JSONObject mergeReq) {
		mergeTestReqCache.put(uname, mergeReq);
	}
	
	public static JSONObject getMergeRequest(String uname) {
		return (JSONObject)mergeTestReqCache.get(uname);
	}
	
	public static JSONObject flushMergeRequest(String uname) {
		return (JSONObject)mergeTestReqCache.remove(uname);
	}
	
	public static JSONObject getMergeStepRequest(String uname) {
		return (JSONObject)mergeStepCache.get(uname);
	}
	
	public static JSONObject flushMergeStepRequest(String uname) {
		return (JSONObject)mergeStepCache.remove(uname);
	}
	
	public static void cacheMergeStepRequest(String uname, JSONObject mergeReq) {
		mergeStepCache.put(uname, mergeReq);
	}
	
	public static String getTestCasePath(int companyid, int testCaseId) {
		return AppProperties.datadirectory + companyid + "\\"+ testCaseId;
	}
	
	public static String getVideoPath(int companyid, int testCaseId, String filename) {
		return AppProperties.datadirectory + companyid + "\\"+ testCaseId +  "\\VideoFile\\" + filename;
	}
	
	public static void createVideoFolderPath(int companyid, int testCaseId) {
		String folderPath = AppProperties.datadirectory + companyid + "\\"+ testCaseId +  "\\VideoFile\\";
		createFolder( folderPath);
	}
	
	public static String getInputFilePath(int companyid, int testCaseId, String filename) {
		String tcFolder = getTestCasePath( companyid, testCaseId);
		return  tcFolder + "\\" + filename;
	}
	
	public static String getProductInputFile(int companyid, int prodid, int testStepId, String filename) {
		String filePath = Utilities.getTestDataPath(companyid,  prodid);
		return  filePath + "\\" + filename;
	}
	
	public static void createScriptsPath(int companyid) {
		String folderPath = AppProperties.datadirectory + companyid + "\\Scripts";
		createFolder( folderPath);
	}
	
	public static String createScriptZipPath(int companyid) {
		String folderPath = AppProperties.datadirectory + companyid + "\\Scripts\\ZipStore";
		createFolder(folderPath);
		return folderPath;
	}
	
	public static String createScriptBackupPath(int companyid) {
		String folderPath = AppProperties.datadirectory + companyid + "\\Scripts\\Backup";
		createFolder(folderPath);
		return folderPath;
	}
	
	
	public static String createBatchFilePath(int companyid) {
		String folderPath = AppProperties.datadirectory + companyid + "\\Scripts\\BatchFile";
		createFolder(folderPath);
		return folderPath;
	}
	
	public static String getBatchFileFolder(int companyid) {
		String folderPath = getCompanyFolder(companyid) + "\\Scripts\\BatchFile";
		return folderPath;
	}
	
	public static String getScriptFolder(int companyid) {
		String folderPath = getCompanyFolder(companyid) + "\\Scripts";
		return folderPath;
	}
	
	public static String getScriptZipFilePath(int companyid, String fileName) {
		String folderPath = AppProperties.datadirectory + companyid + "\\Scripts\\ZipStore\\" + fileName;
		return folderPath;
	}
	
	public static void createCodePath(int companyid) {
		String folderPath = AppProperties.datadirectory + companyid + "\\code";
		createFolder( folderPath);
	}
	
	public static String getCodePath(int companyid) {
		String folderPath = AppProperties.datadirectory + companyid + "\\code";
		return folderPath;
	}
	
	public static void createZipFilePath(int companyid) {
		String folderPath = AppProperties.datadirectory + companyid + "\\OrignalZip";
		createFolder( folderPath);
	}
	
	public static void createProductPath(int companyid, int prodid) {
		String folderPath = AppProperties.datadirectory + companyid + "\\Products\\" + prodid+ "\\TestData";
		createFolder(folderPath);
		String zipPath = AppProperties.datadirectory + companyid + "\\Products\\" + prodid+ "\\ZipStore";
		createFolder(zipPath);
	}
	
	public static String getTestDataPath(int companyid, int prodid) {
		String folderPath = AppProperties.datadirectory + companyid + "\\Products\\" + prodid+ "\\TestData";
		return folderPath;
	}
	
//	public static String getZipPath(int companyid, int prodid) {
//		String zipPath = AppProperties.datadirectory + companyid + "\\Products\\" + prodid+ "\\ZipStore";
//		return zipPath;
//	}
	
	public static String getTestDataZipPath(int companyid, int prodid, String fileName) {
		String folderPath = AppProperties.datadirectory + companyid + "\\Products\\" + prodid+ "\\ZipStore\\" + fileName;
		return folderPath;
	}
	
	public static String getCoverageSSPath(int companyid, int prodid, int pageId) {
		String folderPath = AppProperties.datadirectory + companyid + "\\Products\\" + prodid+ "\\Coverage\\";
		folderPath = folderPath + getCoverageSSFileName(companyid, prodid, pageId);
		return folderPath;
	}
	
	public static String getBackupSSPath(int companyid, int prodid) {
		String folderPath = AppProperties.datadirectory + companyid + "\\Products\\" + prodid+ "\\BackupSS\\";
		return folderPath;
	}
	
	//Page ScreenShot File Name
	public static String getCoverageSSFileName(int companyId, int prodId, int pageId) {
		String ssfilename = "_" + companyId + "_" + prodId + "_" + pageId + ".png";
		return ssfilename;
	}
	
	public static void createGoogleOAuthCredPath(int companyid) {
		String folderPath = getGoogleOAuthCredPath(companyid);
		createFolder( folderPath);
	}
	
	public static String getGoogleOAuthCredPath(int companyid) {
		String folderPath = AppProperties.datadirectory + companyid + "\\GOACP";
		return folderPath;
	}
	
	public static String getExtPropBackupFile() {
		String folderPath = AppProperties.propfilebackuploc + AppProperties.propfilebackupname;
		return folderPath;
	}
	
	public static String getGoogleAuthCredPath(int companyid, String fileName) {
		String folderPath = AppProperties.datadirectory + companyid + "\\GOACP\\" + fileName;
		return folderPath;
	}
	
	public static String getZipFilePath(int companyid, String fileName) {
		String folderPath = AppProperties.datadirectory + companyid + "\\OrignalZip\\" + fileName;
		return folderPath;
	}
	
	public static String getScriptFilePath(int companyid, String fileName) {
		String folderPath = AppProperties.datadirectory + companyid + "\\Scripts\\" + fileName;
		return folderPath;
	}
	
	public static String getBatchFilePath(int companyid, String fileName) {
		String folderPath = AppProperties.datadirectory + companyid + "\\Scripts\\BatchFile\\" + fileName;
		return folderPath;
	}
	
	public static String getCodeFilePath(int companyid, String fileName) {
		String folderPath = AppProperties.datadirectory + companyid + "\\code\\" + fileName;
		return folderPath;
	}
	
	public static String getGenPomFilePath(int companyid, int prodid, String pomFolder, String fileName) {
		String folderPath = getPomFilesPath(companyid, prodid, pomFolder) +"\\"+ fileName ;
		return folderPath;
	}
	
	public static String getApiDataPath(int companyid, int testCaseId) {
		String tcFolder = getTestCasePath( companyid, testCaseId);
		return  tcFolder + "\\" + "ApiData";
	}
	
	public static String getPomFilesPath(int companyid, int prodid, String foldername) {
		String folderPath = AppProperties.datadirectory + companyid + "\\Products\\" + prodid+ "\\" + foldername;
		return folderPath;
	}
	
	public static void createGenPomFolder(int companyid, int prodid, String foldername) {
		String folderPath =  AppProperties.datadirectory + companyid + "\\Products\\" + prodid+ "\\" + foldername;
		createFolder( folderPath);
	}
	
	public static String getPomZipFile(int companyid, int pid, String folderName) {
		String folderPath = AppProperties.datadirectory + companyid + "\\Products\\" + pid+ "\\" + folderName;
		return folderPath;
	}
	
	public static void createCompanyFolder(int companyid) {
		String folderPath = getCompanyFolder(companyid);
		createFolder( folderPath);
	}
	
	public static String getCompanyFolder(int companyid) {
		String folderPath = AppProperties.datadirectory + companyid;
		return folderPath;
	}
	
	public static String getCompanyZipFolder(int companyid) {
		String folderPath = getCompanyFolder(companyid) + "\\OrignalZip";
		return folderPath;
	}
	
	public static void createBrandingFolder(int companyid) {
		String folderPath = getCompanyFolder(companyid) + "\\Branding" ;
		createFolder( folderPath);
	}
	
	public static String getBrandingDir(int companyid, String filename) {
		String folderPath = "";
		if(companyid == -1) {
			folderPath = AppProperties.datadirectory +  "\\" + filename;
		} else {
			folderPath = getCompanyFolder(companyid) + "\\Branding\\" + filename;
		}
		return folderPath;
	}
	
	public static void createTestCaseFolder(int companyid, int testCaseId) {
		String folderPath = AppProperties.datadirectory + companyid + "\\" + testCaseId;
		createFolder( folderPath);
		String apiDataPath = folderPath + "\\" + "ApiData";
		createFolder( apiDataPath);
	}
	
	public static void createTestCaseResultsFolder(int companyid, int testCaseId, int testResults) {
		String folderPath = getTCRDir(companyid, testCaseId, testResults);
		createTestCaseResultsFolder(folderPath);
	}
	
	public static void createAPIResultsFolder(int companyid, int apiid, int apiResults) {
		String folderPath = getAPIRDir(companyid, apiid, apiResults);
		createTestCaseResultsFolder(folderPath);
	}
	
	public static void createBaselineFolder(int companyid, int testCaseId) {
		String folderPath = getBaselineDir(companyid, testCaseId);
		createFolder(folderPath);
	}
	
	public static void createTestCaseAnalysisFolder(int companyid, int testCaseId) {
		String folderPath = getAnalysisDir(companyid, testCaseId);
		createTestCaseAnalysisFolder(folderPath);
	}
	
	public static void createTestSuiteResultsFolder(int companyid, int testSuiteId, int testSuiteResults) {
		String folderPath = AppProperties.datadirectory + companyid + "\\ts" + testSuiteId + "\\ts" + testSuiteResults;
		createTestCaseResultsFolder(folderPath);
	}
	
	public static void createTestCaseResultsFolder(String folderPath) {
		//String folderPath = AppProperties.datadirectory + companyid + "\\" + testCaseId + "\\" + testResults;
		createFolder( folderPath);
		
		String outputPath = folderPath + "\\output"; 
		createFolder( outputPath);
		
		String ssPath = folderPath + "\\screenshots"; 
		createFolder( ssPath);
		
		String piPath = folderPath + "\\pageinfo"; 
		createFolder( piPath);
		
		String dlPath = folderPath + "\\download"; 
		createFolder( dlPath);
		
		String psPath = folderPath + "\\pagesource";
		createFolder( psPath);
	}
	
	public static void createTestCaseAnalysisFolder(String folderPath) {
		
		String outputPath = folderPath; 
		createFolder( outputPath);
	}
	
	public static String getScreenShotsDir(int companyid, int testCaseId, int testResults) {
		String folderPath = AppProperties.datadirectory + companyid + "\\" + testCaseId + "\\" + testResults;
		folderPath = folderPath + "\\screenshots\\";
		return folderPath;
	}

	public static String getVideoDir(int companyid, int testCaseId, int idtest_case_results){
		String folderPath = AppProperties.datadirectory + companyid + "\\" + testCaseId + "\\" + idtest_case_results;
		folderPath = folderPath + "\\videos\\";
		return folderPath;
	}
//	public static String getVideoDir(int companyid, int testCaseId, int testResults) {
//		String folderPath = AppProperties.datadirectory + companyid + "\\" + testCaseId + "\\" + testResults;
//		folderPath = folderPath + "\\";
//		return folderPath;
//	}
	
	public static String getOutputDir(int companyid, int testCaseId, int testResults) {
		String folderPath = AppProperties.datadirectory + companyid + "\\" + testCaseId + "\\" + testResults;
		folderPath = folderPath + "\\output\\";
		return folderPath;
	}
	
	public static String getTCRDir(int companyid, int testCaseId, int testResults) {
		String folderPath = AppProperties.datadirectory + companyid + "\\" + testCaseId + "\\" + testResults;
		return folderPath;
	}
	
	public static String getAPIRDir(int companyid, int apiid, int apiResults) {
		String folderPath = AppProperties.datadirectory + companyid + "\\API\\" + apiid + "\\" + apiResults;
		return folderPath;
	}
	
	public static String getAnalysisDir(int companyid, int testCaseId) {
		String folderPath = AppProperties.datadirectory + companyid + "\\" + testCaseId + "\\analysis\\" ;
		return folderPath;
	}
	
	public static String getBaselineDir(int companyid, int testCaseId) {
		String folderPath = AppProperties.datadirectory + companyid + "\\" + testCaseId + "\\baseline\\" ;
		return folderPath;
	}
	
	public static String getAnalysisSSDir(int companyid, int testCaseId) {
		String folderPath = getAnalysisDir(companyid, testCaseId) + "screenshots\\";
		return folderPath;
	}
	
	public static String getPageInfoDir(int companyid, int testCaseId, int testResults) {
		//String folderPath = AppProperties.datadirectory + companyid + "\\" + testCaseId + "\\" + testResults;
		String folderPath = getTCRDir(companyid, testCaseId, testResults) ;
		folderPath = folderPath + "\\pageinfo\\";
		return folderPath;
	}
	
	public static String getPageSourceDir(int companyid, int testCaseId, int testResults) {
		//String folderPath = AppProperties.datadirectory + companyid + "\\" + testCaseId + "\\" + testResults;
		String folderPath = getTCRDir(companyid, testCaseId, testResults) ;
		folderPath = folderPath + "\\pagesource\\";
		return folderPath;
	}
	
	public static String getPageSourceDirFile(int companyid, int testCaseId, int testResults, String filename) {
		String folderPath = getTCRDir(companyid, testCaseId, testResults) ;
		folderPath = folderPath + "\\pagesource\\" + filename;
		return folderPath;
	}
	
	public static String getLogDirFile(int companyid, int testCaseId, int testResults) {
		String folderPath = getTCRDir(companyid, testCaseId, testResults);
		folderPath = folderPath + "\\execution.log";
		return folderPath;
	}
	
	public static String getAnalysisLogFile(int companyid, int testCaseId, int testResults) {
		String folderPath = getTCRDir(companyid, testCaseId, testResults);
		folderPath = folderPath + "\\analysis.log";
		return folderPath;
	}
	
	public static String getAPILogDirFile(int companyid, int apiid, int apiResults) {
		String folderPath = getAPIRDir(companyid, apiid, apiResults);
		folderPath = folderPath + "\\execution.log";
		return folderPath;
	}
	
	public static String getAnalysisFile(int companyid, int testCaseId) {
		String folderPath = getAnalysisDir(companyid, testCaseId);
		folderPath = folderPath +  Utilities.getNow("yyyyMMddHHmmss") + ".log";
		return folderPath;
	}
	
	public static String getTCDownloadDir(int companyid, int testCaseId, int testResults) {
		String folderPath = getTCRDir(companyid, testCaseId, testResults);
		folderPath = folderPath + "\\download\\";
		return folderPath;
	}
	
	public static String getLogDirTS(int companyid, int testSuiteId, int testResultsId) {
		String folderPath = AppProperties.datadirectory + companyid + "\\ts" + testSuiteId + "\\ts" + testResultsId;
		return folderPath;
	}
	
	public static String getLogDirFileTS(int companyid, int testCaseId, int testResults) {
		String folderPath = getLogDirTS(companyid, testCaseId, testResults);
		folderPath = folderPath + "\\execution.log";
		return folderPath;
	}
	
	public static String getSchedulerLogFile() {
		String folderPath = AppProperties.datadirectory;
		folderPath = folderPath + "\\Scheduler"+ Utilities.getNow("dd-MM-HH-mm") + ".log";
		return folderPath;
	}
	
	public static String getAuthoringLogFile() {
		String folderPath = AppProperties.datadirectory;
		folderPath = folderPath + "\\authoring"+ Utilities.getNow("dd-MM-HH-mm") + ".log";
		return folderPath;
	}
	
	public static String getSSFileName(int testCaseId, int testStepId, int testCaseResultsId, int i , int companyId,
			String prefix) {
		String ssfilename = "";
		if(prefix != null && !prefix.equals("")) {
			ssfilename = "_" + companyId + "_" + testCaseId + "_" + testStepId + "_" + testCaseResultsId + 
				"_Step" + i + "_" + prefix + ".png";
		} else {
			ssfilename = "_" + companyId + "_" + testCaseId + "_" + testStepId + "_" + testCaseResultsId + 
					"_Step" + i + ".png";
		}
		return ssfilename;
	}
	
	public static String getCompanySchedulerLogFile(int companyid) {
		String folderPath = getCompanyFolder(companyid);
		folderPath = folderPath + "\\Scheduler"+ Utilities.getNow("dd-MM-HH-mm") + ".log";
		return folderPath;
	}
	
	public static String getCompanyChromeProfile(int companyid) {
		String folderPath = getCompanyFolder(companyid);
		folderPath = folderPath + "\\profile";
		return folderPath;
	}
	
	public static String getSuiteResultsURL(int companyid, int testSuiteId, int testSuiteResultsId) {
		String resultsPath = AppProperties.reactappurl + AppProperties.resultsurlpart2;
		resultsPath = resultsPath.replace("1?", String.valueOf(companyid));
		resultsPath = resultsPath.replace("2?", String.valueOf(testSuiteId));
		resultsPath = resultsPath.replace("3?", String.valueOf(testSuiteResultsId));
		resultsPath = resultsPath.replace("4?", String.valueOf(AppProperties.companyLicenses.get(companyid)));
		return resultsPath;
	}
	
	public static String getSuiteResultsAPI(int companyid, int testSuiteResultsId) {
		String resultsPath = AppProperties.javareacturl + AppProperties.testsuiteresultforapi;
		resultsPath = resultsPath.replace("1?", String.valueOf(companyid));
		resultsPath = resultsPath.replace("2?", String.valueOf(AppProperties.companyLicenses.get(companyid)));
		resultsPath = resultsPath.replace("3?", String.valueOf(testSuiteResultsId));
		return resultsPath;
	}
	
	public static String getTCResultsURL(int companyid, int testCaseId, int testCaseResultsId) {
		String resultsPath = AppProperties.reactappurl + AppProperties.tcresultsurlpart2;
		resultsPath = resultsPath.replace("1?", String.valueOf(companyid));
		resultsPath = resultsPath.replace("2?", String.valueOf(testCaseId));
		resultsPath = resultsPath.replace("3?", String.valueOf(testCaseResultsId));
		resultsPath = resultsPath.replace("4?", String.valueOf(AppProperties.companyLicenses.get(companyid)));
		return resultsPath;
	}
	
	public static void createFolder(String folderPath) {
		 File folder = new File(folderPath);

	        if (!folder.exists()) {
	            boolean success = folder.mkdirs();
	            if (success) {
//	                System.out.println(folderPath + " folder created successfully!");
	            } else {
	                System.out.println(folderPath +  " Failed to create folder!");
	            }
	        } else {
	            System.out.println(folderPath +  " Folder already exists!");
	        }
	}
	
	public static void takeBackup_createFolder(String folderPath) {
		 File folder = new File(folderPath);

	        if (folder.exists()) {
	            // Backup the existing folder
	        	Timestamp timestamp = new Timestamp(System.currentTimeMillis());
	            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd_HHmmss");
	            String formattedTimestamp = dateFormat.format(timestamp);
	            
	            folderPath = folderPath.substring(0, folderPath.length() - 1);
	            String backupFolderPath = folderPath + "_" + formattedTimestamp;
	            File baselineFolder = new File(folderPath);
	            File backUpFolder = new File(backupFolderPath);

	            if (!backUpFolder.exists()) {
	                boolean backupSuccess = baselineFolder.renameTo(backUpFolder);
	                if (!backupSuccess) {
	                    System.out.println("Failed to backup folder!");
	                    return;
	                }
	                System.out.println("Folder backed up successfully!");
	            }

	            // Create a fresh folder
	            boolean createSuccess = folder.mkdirs();
	            if (createSuccess) {
	                System.out.println("Fresh folder created successfully!");
	            } else {
	                System.out.println("Failed to create fresh folder!");
	            }
	        } else {
	            boolean createSuccess = folder.mkdirs();
	            if (createSuccess) {
	                System.out.println("Folder created successfully!");
	            } else {
	                System.out.println("Failed to create folder!");
	            }
	        }
	    }
	
	public static File[] getFilesInFolder(String folderPath) {
        File folder = new File(folderPath);
        File[] fileList = folder.listFiles();
        return fileList;
    }
	
	public static Map<String, String> getFilenamesForCompare(String folderPath) {
		Map<String, String> fileMap = new HashMap<>();
        File[] fileList = Utilities.getFilesInFolder(folderPath);

        if (fileList != null) {
            for (File file : fileList) {
                if (file.isFile()) {
                	String filename = file.getName();
                	String suffix = filename.substring(filename.indexOf("_Step"),filename.length());
                    fileMap.put(suffix, filename);
                }
            }
        } else {
            System.out.println("Folder does not exist or is empty: " + folderPath);
        }

        return fileMap;
    }
	
	public static Timestamp getCurrentTimestamp() {
        long currentTimeMillis = System.currentTimeMillis();
//        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
//        String formattedDate = dateFormat.format(currentTimeMillis);
//        return Timestamp.valueOf(formattedDate);
        return getTimestamp(currentTimeMillis);
    }
	
	public static Timestamp getTimestamp(long timeinmilli) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
        String formattedDate = dateFormat.format(timeinmilli);
        return Timestamp.valueOf(formattedDate);
    }
	
	public static String getCurrentTimestamp(String format) {
        long currentTimeMillis = System.currentTimeMillis();
        SimpleDateFormat dateFormat = new SimpleDateFormat(format);
        String formattedDate = dateFormat.format(currentTimeMillis);
        //return Timestamp.valueOf(formattedDate);
        return formattedDate;
    }
	
	 public static void callApi(String url, String action, String uname, JSONObject testSteps,
			 int companyid) {
		 HttpURLConnection connection = null;
		 DataOutputStream outputStream = null;
		 BufferedReader in = null;
	     try {
		 // Set the API endpoint URL
	        url = url + "?action="+action;


	        JSONParser parser = new JSONParser();
			//String jsonstr = testSteps.toString();

	        // Create an HTTP connection to the API endpoint
	        System.out.println(getCurrentTimestamp() + " Starting to update the react app API " );
	        System.out.println(getCurrentTimestamp() + " URL being called " +  url);
	        URL apiURL = new URL(url);
	        connection = (HttpURLConnection) apiURL.openConnection();
	        connection.setRequestMethod("POST");
	        connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
	        connection.setDoOutput(true);

	        // Write the form parameters to the request body
	        String body = "";
	        if(testSteps == null) {
	        	body = "uname=" + uname + "&companyid=" +  companyid;
	        } else {
	        	body = "uname=" + uname + "&companyid=" +  companyid + "&json=" +  testSteps;
	        }
	        
//	        body = "uname=" + uname + "&json=" +  testSteps;
	        System.out.println(getCurrentTimestamp() + " body is - " +  body);
	        outputStream = new DataOutputStream(connection.getOutputStream());
	        outputStream.writeBytes(body);
	        outputStream.flush();
	        
	        
	        System.out.println(getCurrentTimestamp() + " API was called successfully " +  url);
	        
	        System.out.println(getCurrentTimestamp() + " Let us see what the response is " +  url);

	        // Read the API response
	        in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
	        String inputLine;
	        StringBuffer response = new StringBuffer();
	        while ((inputLine = in.readLine()) != null) {
	            response.append(inputLine);
	        }
	        

	        // Print the API response
	        System.out.println(getCurrentTimestamp() + " API Response - " + response.toString());

	      
	     }catch(Exception e) {
	    	 e.printStackTrace();
	     } finally {
	    	  // Disconnect from the API endpoint
		        connection.disconnect();
		        try {
			        outputStream.close();
			        in.close();
		        } catch (Exception e) {
		        	e.printStackTrace();
		        }
	     }
	    }
	 
	 public static void copyFile(String sourcePath, String destinationPath) {	        
	        try {
	            // Copy the file from source to destination
	            Files.copy(Path.of(sourcePath), Path.of(destinationPath), 
	            		StandardCopyOption.REPLACE_EXISTING);
	            System.out.println("File copied successfully! from source " + 
	            sourcePath + " to destination "  + destinationPath);
	        } catch (IOException e) {
	            System.out.println("An error occurred while copying the file: " + 
	        e.getMessage());
	        }
	 }
	 
	 public static void replaceContent(Path filePath, String originalText, String replacementText) throws IOException {
			
			String content = Files.lines(filePath, StandardCharsets.UTF_8)
	                .collect(Collectors.joining("\n"));
			// Replace text
			content = content.replace(originalText.trim(), replacementText);
			// Write the new content back to the file
			Files.write(filePath, content.getBytes(StandardCharsets.UTF_8));
	}
	 
	 public static void writeDataToFile(String filePath, String content) {
		 if(content == null) {
			 content = "";
		 }

        try {
            // Create a FileWriter to write the JSON data to the file
            FileWriter fileWriter = new FileWriter(filePath);
            
            fileWriter.write(content);
            fileWriter.close();
            System.err.println(Utilities.getNow() + " JSON data stored in the file: " + filePath);
        } catch (IOException e) {
        	System.err.println(Utilities.getNow());
            e.printStackTrace();
        }
	 }
	 
	 public static String readDataFromFile(String filePath) {

		 StringBuilder content = new StringBuilder();
	     try {   
		 
	        BufferedReader reader = new BufferedReader(new FileReader(filePath));
	        String line;
	        while ((line = reader.readLine()) != null) {
	            content.append(line).append("\n");
	        }
	     } catch (Exception e) {
	      	e.printStackTrace();
	     }

	        return content.toString();
	 }
	 
	 public static String randomGen(String type, int len, boolean AIGenerated) {
			StringBuilder sb = null;
			
				final String CHAR_LOWER = "abcdefghijklmnopqrstuvwxyz";
				final String CHAR_UPPER = CHAR_LOWER.toUpperCase();
				final String NUMBER = "0123456789";
				String DATA_FOR_RANDOM_STRING = CHAR_LOWER + CHAR_UPPER + NUMBER;
				SecureRandom random = new SecureRandom();
				String prompt = "";
				String s1 = "";
				if(type.equals(AppProperties.CHAR) || type.equals(AppProperties.ALPHA)) {				
					if (type.equals(AppProperties.CHAR)) {
						if(AIGenerated) {
							prompt = "Please generate a random string of " +  len + " ASCII letters.";
						} else {
							DATA_FOR_RANDOM_STRING = CHAR_LOWER + CHAR_UPPER;
						}
					} else if (type.equals(AppProperties.ALPHA)) {
						if(AIGenerated) {
							prompt = "Please generate a random alphanumeric string of length " + len + ". "
									+ "The string should only contain letters A-Z (both uppercase and lowercase) and the numbers 0-9.";	
						} else {
							DATA_FOR_RANDOM_STRING = CHAR_LOWER + CHAR_UPPER + NUMBER;
						}
						if(AIGenerated) {
							s1 = OpenAIClient.genData( prompt, 1, len);
							s1 = s1.replaceAll("\\s", "");
							if(s1.length() < len) {						
								String s2 = OpenAIClient.genData( prompt, 1, len);
								s2 = s2.replaceAll("\\s", "");
								return (s1 + s2).substring(0,len);
							} else if(s1.length() > len) {
								return s1.substring(0,len);
							}
						} else {
							s1 = genData(len, DATA_FOR_RANDOM_STRING);
						}
						return s1;
					}
				} else if (type.equals(AppProperties.NUMBER)) {
					if(AIGenerated) {
						prompt = "Please generate a random " + len + "-digit number."
								+ "";
						s1 = OpenAIClient.genData( prompt, 1, len);
						return s1;
					} else {
						DATA_FOR_RANDOM_STRING = NUMBER;
					}
				}
				sb = new StringBuilder(len);
				for (int i = 0; i < len; i++) {
					int rndCharAt = random.nextInt(DATA_FOR_RANDOM_STRING.length());
					char rndChar = DATA_FOR_RANDOM_STRING.charAt(rndCharAt);
					sb.append(rndChar);
				}
			

			return sb.toString();
		}
		
		public static String genData(int len, String ranstr) {
			SecureRandom random = new SecureRandom();
			StringBuilder sb = new StringBuilder(len);
			for (int i = 0; i < len; i++) {
				int rndCharAt = random.nextInt(ranstr.length());
				char rndChar = ranstr.charAt(rndCharAt);
				sb.append(rndChar);
			}
			
			return sb.toString();
		}
		
		public static String getNow() {
			LocalDateTime currentDateTime = LocalDateTime.now();
		    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
		    String formattedDateTime = currentDateTime.format(formatter);
		    return formattedDateTime;
		}
		
		public static String getNow(String pattern) {
			LocalDateTime currentDateTime = LocalDateTime.now();
		    DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern);
		    String formattedDateTime = currentDateTime.format(formatter);
		    return formattedDateTime;
		}
		
		public static String getNow(String pattern, String offset) {
			LocalDateTime currentDateTime = LocalDateTime.now();
			if(offset != null && !offset.equals("")) {
				offset = offset.trim();
				String offs = offset;
				if(offset.startsWith("+") || offset.startsWith("-")) {
					offs = offset.substring(1,offset.length()); 
				}
				int offsetInt = Integer.valueOf(offs);
				if(offset.startsWith("-")) {
					currentDateTime = currentDateTime.minusDays(offsetInt);
				} else  {
					currentDateTime = currentDateTime.plusDays(offsetInt);
				}
			}
		    DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern);
		    String formattedDateTime = currentDateTime.format(formatter);
		    return formattedDateTime;
		}
		
		public static void deleteFolderContents(String folderPath) throws IOException {
	        Path directory = Paths.get(folderPath);

	        if (Files.exists(directory) && Files.isDirectory(directory)) {
	            // Delete all files and directories within the folder (excluding the folder itself)
	            Files.walk(directory)
	                 .sorted((path1, path2) -> -path1.compareTo(path2))
	                 .forEach(Utilities::deleteFileOrDirectory);

	            System.out.println(Utilities.getCurrentTimestamp() + "Folder contents deleted successfully." + folderPath);
	        } else {
	            System.out.println("Folder does not exist or is not a directory.");
	        }
	    }
		
		public static void copyFolder(String sourceDirPath, String destDirPath) {
			try {
				Files.walk(Paths.get(sourceDirPath))
	            .filter(Files::isRegularFile)
	            .forEach(sourceFilePath -> {
	                Path destinationFilePath = Paths.get(destDirPath, sourceFilePath.getFileName().toString());
	                try {
	                    Files.copy(sourceFilePath, destinationFilePath);
	                } catch (IOException e) {
	                    System.out.println("Failed to copy file: " + sourceFilePath);
	                    e.printStackTrace();
	                }
	            });
				System.out.println("Files copied successfully.");
        } catch (IOException e) {
            System.out.println("Failed to copy files: " + e.getMessage());
            e.printStackTrace();
        }
		}

	    public static void recreateFolder(String folderPath) throws IOException {
	        Path directory = Paths.get(folderPath);

	        // Recreate the folder
	        Files.createDirectories(directory);

	        System.out.println("Folder recreated successfully.");
	    }

	    private static void deleteFileOrDirectory(Path path) {
	        try {
	            Files.delete(path);
	        } catch (IOException e) {
	            System.err.println("Failed to delete: " + path + ". Error: " + e.getMessage());
	        }
	    }
	    
	    public static Calendar getCalender(String time, String format) {
	        SimpleDateFormat sdfInput = new SimpleDateFormat(format);  
	        Calendar cal = null;
	        try {
	            java.util.Date parsedDate = sdfInput.parse(time);
	            cal = Calendar.getInstance();
	            cal.setTime(parsedDate); 
	            cal.set(Calendar.SECOND, 0);
	            cal.set(Calendar.MILLISECOND, 0);
	        } catch (ParseException e) {
	            e.printStackTrace();
	        }
	    	return cal;
	    }
	    
	    public static Date getDate(Calendar cal) {
	    	Date sqlDate = new Date(cal.getTimeInMillis());
	    	return sqlDate;
	    }
	    
	    public static Time getTime(Calendar cal) {
            Time sqlTime = new Time(cal.getTimeInMillis());
           return sqlTime;
	    }
	    
	    public static List<String> getTagsFromDesc(String input) {
//			String input = "This is a #sample #string with #hashtags of different #lengths";

	        // Define a regular expression to match hashtags
	        String regex = "#\\w+";

	        // Create a Pattern object
	        Pattern pattern = Pattern.compile(regex);

	        // Create a Matcher object
	        Matcher matcher = pattern.matcher(input);

	        // Initialize a list to store hashtags
	        List<String> hashtags = new ArrayList<>();

	        // Find and add hashtags to the list
	        while (matcher.find()) {
	            String hashtag = matcher.group();
	            hashtag = hashtag.substring(1,hashtag.length());
	            hashtags.add(hashtag);
	        }

	        // Print the list of hashtags
	        return hashtags;
			
		}
	    
		 public static void callScreenshotApi(int testcaseid, int companyid, String randomkey) {
		     try {
		            
		            String apiUrl = AppProperties.javareactinternalurl+"?action=coverageExecute&companyid="+companyid+"&token="+randomkey+"&testcaseid="+testcaseid;
		            
		            // Create a URL object with the API endpoint
		            URL url = new URL(apiUrl);

		            // Open a connection to the URL
		            HttpURLConnection connection = (HttpURLConnection) url.openConnection();

		            // Set request method to GET
		            connection.setRequestMethod("GET");

		            // Get the response code
		            int responseCode = connection.getResponseCode();
		            System.out.println("Response Code: " + responseCode);

		            // Read the response from the API
		            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
		            String inputLine;
		            StringBuilder response = new StringBuilder();

		            while ((inputLine = reader.readLine()) != null) {
		                response.append(inputLine);
		            }
		            reader.close();

		            // Print the response
		            System.out.println("Response: " + response.toString());

		            // Close the connection
		            connection.disconnect();
		     }catch(Exception e) {
		    	 e.printStackTrace();
		     }
		 }
		 
		 public static List<String> replaceJSParams(String params) {
			 
			 List<String> resultList = new ArrayList<String>();
			 
			 if(!params.contains(AppProperties.paramdelimiter)) {
				 resultList.add(params);
			 } else {
				 String[] paramArr = params.split(AppProperties.paramdelimiter);
				  
				 for(int i=1; i < paramArr.length; i++) {
					 String param = paramArr[i];
					 resultList.add(param);
				 }
			 }
			 
			 return resultList;
		 }
		 
		 public static File getlatestFileFromDir(String codeDir, String key, String ext) {
			 File mostRecentFile = null;
			 File dir = new File(codeDir);  // Replace with your directory path
		        File[] files = dir.listFiles(new FilenameFilter() {
		            @Override
		            public boolean accept(File dir, String name) {
		                return name.endsWith("_" + key + ext);  // Filter files that end with "_key"
		            }
		        });

		        if (files != null && files.length > 0) {
		            Arrays.sort(files, new Comparator<File>() {
		                @Override
		                public int compare(File f1, File f2) {
		                    return Long.compare(f2.lastModified(), f1.lastModified());
		                }
		            });

		            // The most recently created file
		            mostRecentFile = files[0];
		        }
		        return mostRecentFile;
		 }
		 
		 public static void updateChromeExtensionProperties(String key, String value) {
			 String filePath = AppProperties.crxloc + "//properties.file";
		        Properties properties = new Properties();
		        try (FileWriter fileWriter = new FileWriter(filePath, true)) {
		            // Open the file for appending (true)
		            fileWriter.write(System.lineSeparator() + key + "=" + value);
		        } catch (IOException e) {
		            e.printStackTrace();
		        }
		    }
		 
		 public static void callImageDiffApi(int companyid, String leftImg, String rightImg,
				 String resultImg) {
		     try {		            
		            // Create a URL object with the API endpoint
		            URL url = new URL(AppProperties.imagediffurl);

		            // Open a connection to the URL
		            HttpURLConnection connection = (HttpURLConnection) url.openConnection();

		            // Set request method to GET
		            connection.setRequestMethod("GET");
		            connection.setRequestProperty("clientid", String.valueOf(companyid));
		            connection.setRequestProperty("Content-Type", "application/json");
		            connection.setDoOutput(true);
		            
		            JSONObject imgdata = new JSONObject();
		            imgdata.put("leftimg", leftImg);
		            imgdata.put("rightimg", rightImg);
		            imgdata.put("outfile", resultImg);
		            
		            DataOutputStream outputStream = new DataOutputStream(connection.getOutputStream());
			        outputStream.writeBytes(imgdata.toString());
			        outputStream.flush();			        

		            // Get the response code
		            int responseCode = connection.getResponseCode();
		            System.out.println("Response Code: " + responseCode);
		            connection.disconnect();
		     }catch(Exception e) {
		    	 e.printStackTrace();
		     }
		 }
		 
		 public static String getAnalysisSSFileName(int testCaseId, int i , int companyId, 
				 String prefix) {
			String ssfilename = "";
			ssfilename = "_" + companyId + "_" + testCaseId + "_" + "_Step" + i + "_" + 
			prefix + ".png";
			return ssfilename;
		}
		 
		 public static void unZipFile(String zipFilePath, String extractDirectory) {
			    try (ZipInputStream zipInputStream = new ZipInputStream(new FileInputStream(zipFilePath))) {
			        ZipEntry entry;
			        while ((entry = zipInputStream.getNextEntry()) != null) {
			            String entryName = entry.getName();
			            File destinationFile = new File(extractDirectory, entryName);
				        if (!destinationFile.getParentFile().exists()) {
				            destinationFile.getParentFile().mkdirs();
				        }
				        try (FileOutputStream fos = new FileOutputStream(destinationFile)) {
				            byte[] buffer = new byte[1024];
				            int bytesRead;
				
				            while ((bytesRead = zipInputStream.read(buffer)) != -1) {
				                fos.write(buffer, 0, bytesRead);
				            }
			
				        } catch (IOException e) {
				        	System.err.println(Utilities.getNow());
				            e.printStackTrace();
				            System.err.println("Error writing file: " + entryName);
				    	}
				    } 
				   zipInputStream.closeEntry(); 
				} catch (IOException e) {
					System.err.println(Utilities.getNow());
			        e.printStackTrace();
			    }
		 }
		 
		 public static JSONArray getFileNamesInZipFile(String zipFilePath) {
			 JSONArray result = new JSONArray();
			 try (ZipInputStream zipInputStream = new ZipInputStream(new FileInputStream(zipFilePath))) {
				 ZipEntry entry;
			     while ((entry = zipInputStream.getNextEntry()) != null) {
			    	 String entryName = entry.getName();
				     result.add(entryName);
			     } 
				 zipInputStream.closeEntry(); 
			 } catch (IOException e) {
				 System.err.println(Utilities.getNow());
			     e.printStackTrace();
			 }
			 return result;
		 }
		 
		 public static File[] getFilesWithMatchingPattern(String downloadDir, String filename) {
			// Define the pattern for the filename
		        String pattern = filename;
				// Create a FilenameFilter to filter files based on the pattern
		        FilenameFilter filter = new FilenameFilter() {
		            @Override
		            public boolean accept(File dir, String name) {
		                return name.matches(pattern);
		            }
		        };
		        
		     // List files that match the pattern
		        File directory = new File(downloadDir);
		        File[] matchingFiles = directory.listFiles(filter);
		        return matchingFiles;
		 }
		 
		// Convert a glob pattern to a regular expression
		    public static String globToRegex(String glob) {
		        StringBuilder regex = new StringBuilder("^");
		        for (int i = 0; i < glob.length(); i++) {
		            char ch = glob.charAt(i);
		            switch (ch) {
		                case '*':
		                    regex.append(".*");
		                    break;
		                case '?':
		                    regex.append(".");
		                    break;
		                case '.':
		                case '\\':
		                case '^':
		                case '$':
		                case '[':
		                case ']':
		                case '{':
		                case '}':
		                case '|':
		                    regex.append("\\").append(ch);
		                    break;
		                default:
		                    regex.append(ch);
		                    break;
		            }
		        }
		        regex.append("$");
		        return regex.toString();
		    }
}
