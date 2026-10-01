package nogrunt;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.HashMap;
import java.util.Iterator;
import org.json.simple.JSONObject;

public class AppProperties {

	public static int rootCompany = 59;
	public static String rootEmail = "deva@nogrunt.com";
	public static HashMap <String,Object> sysProps;
	public static HashMap <String,Object> custProps;
	public static HashMap <String,JSONObject> licenses;
	public static HashMap <Integer,JSONObject> companyLicenses;
	public static String delimiter;
	public static String testdatadelimiter;
	public static String gsheetdelimiter = "!";
	public static String gsheetCredsFile = "credentials.json";
	public static String paramdelimiter;
	public static String validatedelimiter;
	public static String licenselimiter;
	public static int streamfilesize;
	public static int defaultwaittime;
	public static String apicapture;
	public static String autowaitforapi;
	public static int apidefaultwaittime;
	public static int seleniumwaittime;

	public static String databaseDriver;
	public static String databaseUrl;
	public static String databaseUsername;
	public static String databasePassword;
	
	public static String codeDir;
	
	public static String MysqldatabaseDriver;
	public static String OracledatabaseDriver;
	public static String MssqldatabaseDriver;
	public static String poolingenabled;
	
	public static String awssesaccesskey;
	public static String awssessecretaccesskey;
	public static String awsregion;
	public static String fromaddress;
	public static String s3bucket;
	
	public static String environment;
	public static String welcomesubject;
	public static String testcaseexecutionsubject;
	public static String testsuiteexecutionsubject;
	public static String forgotpwdsubject;

//	public static String downloadFolder;
	public static String testcasesfolder;

	public static String datadirectory;
	public static int defaultDisplayWidth;
	public static int defaultDisplayHeight;

	public static String webdriverchromedriver;
	public static String profilepathchrome;	
	public static String webdriveredgedriver;
	public static String webdriverfirefoxdriver;
//	public static String webdriversafaridriver;
	public static String seleniumhub;
	public static String seleniumhubport;
	public static String gqlendpoint;

	public static String reactappurl;
	public static String javareacturl;
	public static String javareactinternalurl;
	public static String javaexturl;
	public static String fileurl;
	public static String crxloc;
	public static String propfilebackuploc;
	public static String propfilebackupname;
	public static String mlSummary;
	public static String imagediffurl;
	public static String outlookaccessurl;
	public static String gherkintestcases;	
	public static String resultsurlpart2;
	public static String tcresultsurlpart2;
	public static String testsuiteresultforapi;
	public static boolean chatgpton;
	public static String listlimit;
	public static int listlimitInt=50;
	public static int failTestCaseHistoryPageSize = 10;
	public static String hashingsalt = "Nogrunt_Deva";
	public static String nooffailedattemptsalowed;
	public static String primaryColor;
	public static String secondaryColor;
	public static String tertiaryColor;
	public static String brandLogo;
	public static String deploymentType;
	
	public static String ngrokurlBE;
	public static String ngrokurlFE;
	
	public static String minWidth;
	public static String minHeight;
	
	public static String lowerLimitofPageVersioning;
	public static String upperLimitofPageVersioning;
	public static String elementDiffforPageVersioning;
	
	public static String consumerCount;

	public static String custdbcreds = "dbConnectionCred";
	
	public final static String NUMBER = "NUMBER";
	public final static String CHAR = "CHAR";
	public final static String ALPHA = "ALPHA";
	public final static String EMAIL = "EMAIL";
	public final static String TODAY = "TODAY";
	public final static String RECORDING = "Recording";
	public final static String URLKEYWORD = "Url";
	public final static String TABKEYWORD = "Tab";
	public final static String WINKEYWORD = "Window";
	public final static String GETACTION = "Get";
	public final static String POSITIVEFLOW = "Positive";
	public final static String NEGATIVEFLOW = "Negative";
	public final static String URLEVENT = "urlentry";
	public final static String DYNAMICTEXT = "DynamicText";
	public final static String UPLOAD = "Upload";
	
	public static String isCleanUpOn;
	public static int cleanupRetentionCount;
	public static int cleanupRetentionDays;
	
	public static String cfurl;
	public static String cfappid;
	public static String cfsecretkey;
	
	public static String mobileSuitAPI;
	public static String configIp;

	public static void getProperties() {
		Properties properties = new Properties();
	//	System.out.println(System.getProperty("user.dir"));
		/**
		 * System.out.println(System.getProperty("project.dir")); try {
		 * properties.load(new
		 * FileInputStream("C:\\04workspace\\kl-workspace\\keyloggingv2\\config.properties"));
		 * } catch (Exception e) { e.printStackTrace(); }
		 **/
		try (InputStream input = AppProperties.class.getClassLoader().getResourceAsStream("config.properties")) {

			if (input == null) {
				System.out.println("Sorry, unable to find config.properties");
				return;
			}
		
		
			properties.load(input);
		} catch (IOException ex) {
			ex.printStackTrace();
		}


		databaseDriver = properties.getProperty("mysql.database.driver");
		databaseUrl = properties.getProperty("mysql.database.url");
		databaseUsername = properties.getProperty("mysql.database.username");
		databasePassword = properties.getProperty("mysql.database.password");
		codeDir = properties.getProperty("code.dir");
//		chatgpton = Boolean.parseBoolean(properties.getProperty("chatgpt.on"));
		
		
		MySQlConn msc = new MySQlConn(null);
		sysProps = msc.getSysProps();
		custProps = msc.getCustProps();
		HashMap licensesRes = msc.getLicenses();
		msc.closeDbConn();
		
		licenses = (HashMap)licensesRes.get("lm");
		companyLicenses = (HashMap)licensesRes.get("lcm");
		
		databaseDriver = (String)sysProps.get("databaseDriver");
		databaseUrl = (String)sysProps.get("databaseUrl");
		databaseUsername = (String)sysProps.get("databaseUsername");
		databasePassword = (String)sysProps.get("databasePassword");
//		downloadFolder = (String)sysProps.get("downloadFolder");	
		testcasesfolder = (String)sysProps.get("testcasesfolder");
		datadirectory = (String)sysProps.get("datadirectory");
		defaultDisplayWidth = Integer.parseInt((String)sysProps.get("defaultDisplayWidth"));
		defaultDisplayHeight = Integer.parseInt((String)sysProps.get("defaultDisplayHeight"));
		webdriverchromedriver = (String)sysProps.get("webdriverchromedriver");
		webdriveredgedriver = (String)sysProps.get("webdriveredgedriver");
		webdriverfirefoxdriver = (String)sysProps.get("webdriverfirefoxdriver");
		
		profilepathchrome = (String)sysProps.get("chromeprofilepath"); 
//		webdriversafaridriver = (String)sysProps.get("webdriversafaridriver");
		seleniumhubport = (String)sysProps.get("seleniumhubport");
		seleniumhub = seleniumhubport + (String)sysProps.get("seleniumhub");
		gqlendpoint = seleniumhubport + (String)sysProps.get("gqlendpoint");
		reactappurl = (String)sysProps.get("reactappurl");
		javareacturl = (String)sysProps.get("javareacturl");
		javareactinternalurl = (String)sysProps.get("javareactinternalurl");
		javaexturl = (String)sysProps.get("javaexturl");
		fileurl = (String)sysProps.get("fileurl");
		crxloc = (String)sysProps.get("crxlocation");
		mlSummary = (String)sysProps.get("mlSummary");
		imagediffurl = (String)sysProps.get("imagediffurl");
		outlookaccessurl = (String)sysProps.get("outlookaccessurl");
		gherkintestcases = (String)sysProps.get("gherkintestcases");
		propfilebackuploc = (String)sysProps.get("propfilebackuploc");
		propfilebackupname = (String)sysProps.get("propfilebackupname");
		resultsurlpart2 = (String)sysProps.get("resultsurlpart2");
		tcresultsurlpart2 = (String)sysProps.get("tcresultsurlpart2");
		testsuiteresultforapi = (String)sysProps.get("testsuiteresultforapi");
		MysqldatabaseDriver = (String)sysProps.get("mysqldatabasedriver");
		OracledatabaseDriver = (String)sysProps.get("oracledatabasedriver");
		MssqldatabaseDriver = (String)sysProps.get("mssqldatabasedriver");
		poolingenabled = (String)sysProps.get("poolingenabled");
		
		awssesaccesskey = (String)sysProps.get("awssesaccesskey");
		awssessecretaccesskey = (String)sysProps.get("awssessecretaccesskey");
		awsregion = (String)sysProps.get("awsregion");
		fromaddress = (String)sysProps.get("fromaddress");
		s3bucket = (String)sysProps.get("s3bucket");
		
		ngrokurlBE = (String)sysProps.get("ngrokurlBE");
		ngrokurlFE = (String)sysProps.get("ngrokurlFE");
		
		environment = (String)sysProps.get("environment");
		welcomesubject = (String)sysProps.get("welcomesubject");
		testcaseexecutionsubject = (String)sysProps.get("testcaseexecutionsubject");
		testsuiteexecutionsubject = (String)sysProps.get("testsuiteexecutionsubject");
		forgotpwdsubject = (String)sysProps.get("forgotpwdsubject");	
		
		delimiter = (String)sysProps.get("delimiter");
		testdatadelimiter = (String)sysProps.get("testdatadelimiter");
		paramdelimiter = (String)sysProps.get("paramdelimiter");
		validatedelimiter = (String)sysProps.get("validatedelimiter");
		licenselimiter = (String)sysProps.get("licenselimiter");
		String streamfilesizes = (String)sysProps.get("streamfilesize");
		streamfilesize = Integer.valueOf(streamfilesizes) *1024 *1024;
		String defaultwaittimestr = (String)sysProps.get("defaultwaittime");
		defaultwaittime = Integer.valueOf(defaultwaittimestr) * 1000;
		
		apicapture = (String)sysProps.get("apicapture");
		autowaitforapi = (String)sysProps.get("autowaitforapi");
		String apidefaultwaittimestr = (String)sysProps.get("apidefaultwaittime");
		apidefaultwaittime = Integer.valueOf(apidefaultwaittimestr) * 1000;
		
		String seleniumwaittimestr = (String)sysProps.get("seleniumwaittime");
		seleniumwaittime = Integer.valueOf(seleniumwaittimestr) * 1000;
		
		listlimit = (String)sysProps.get("listlimit");
		listlimitInt=Integer.parseInt(listlimit);
		nooffailedattemptsalowed = (String)sysProps.get("NoOfFaledAttemptsAllowed");
		primaryColor = (String)sysProps.get("primaryColor");
		secondaryColor = (String)sysProps.get("secondaryColor");
		tertiaryColor = (String)sysProps.get("tertiaryColor");
		brandLogo = (String)sysProps.get("brandLogo");
		
		lowerLimitofPageVersioning = (String)sysProps.get("lowerlimitforpageversion");
		upperLimitofPageVersioning = (String)sysProps.get("upperlimitforpageversion");
		elementDiffforPageVersioning = (String)sysProps.get("elementdifference");
		
		consumerCount = (String) sysProps.get("queuesaveconsumercount");
		
		deploymentType = (String)sysProps.get("deploymenttype");
		
		cfurl= (String)sysProps.get("cashfreesandboxurl");
		cfappid= (String)sysProps.get("cfappid");
		cfsecretkey= (String)sysProps.get("cfsecret");
		
		mobileSuitAPI = (String)sysProps.get("mobileSuitAPI");
		configIp = (String)sysProps.get("configIp");
		
		minWidth = (String)sysProps.get("minWidth");
		minHeight = (String)sysProps.get("minHeight");
		
		if(((String)sysProps.get("chatgpton")).equals("false")) {
			chatgpton = false;
		} else {
			chatgpton = true;
		}
		
		HashMap cMap = (HashMap)custProps.get(0);
		
		if(cMap == null || !((String)cMap.get("delimiter")).equals(delimiter)) {
			System.out.println("delimiter issues");
		}
		
		if(cMap == null || !((String)cMap.get("testdatadelimiter")).equals(testdatadelimiter)) {
			System.out.println("testdatadelimiter");
		}
		if(cMap == null || !((String)cMap.get("validatedelimiter")).equals(validatedelimiter)) {
			System.out.println("validatedelimiter");
		}
		
		String retentionCountStr = (String) sysProps.get("cleanupRetentionCount");
		String retentionDaysStr = (String) sysProps.get("cleanupRetentionDays");
		String isCleanUpOnStr = (String)sysProps.get("isCleanUpOn");
				
		isCleanUpOn = (isCleanUpOnStr != null) ? isCleanUpOnStr : "false";
		cleanupRetentionCount = retentionCountStr != null ? Integer.parseInt(retentionCountStr) : 100;
		cleanupRetentionDays = retentionDaysStr != null ? Integer.parseInt(retentionDaysStr) : 60;
		
	}
}
