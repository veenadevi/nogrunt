package nogrunt;

import java.sql.DriverManager;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
//import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.sql.Time;
import java.sql.Date;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.owasp.encoder.Encode;
import org.json.simple.parser.JSONParser;

import nogrunt.exceptions.*;
import nogrunt.nlp.CreateNLPSteps;
import nogrunt.dbaccess.*;
import nogrunt.payments.*;

import jakarta.servlet.http.HttpServletRequest;
import java.net.URLDecoder;

import java.io.File;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

import javax.naming.Context;
import javax.sql.DataSource;
import java.sql.Timestamp;
import javax.naming.InitialContext;
import javax.naming.NamingException;

public class MySQlConn {

	public Connection testCon;
	HashMap issuesMap = new HashMap();
	String randomkey = "";
	JSONObject userDetails = null;
	long processedTill = -1;
	
	private boolean calledFromSpecificLocation = false;

	public MySQlConn(Connection tc) {
		if (tc == null) {
			getDbConn();
		} else {
			testCon = tc;
		}

		try {
			// Attempt to use the connection for some operation
			testCon.prepareStatement("SELECT 1").executeQuery();
		} catch (SQLException e) {
			System.err.println("Connection in MSC is closed - create new");
			getDbConn();
		}
	}

	public void setRandomKey(String rk) {
		randomkey = rk;
	}

	public void setUserDetails(JSONObject ud) {
		userDetails = ud;
	}

	public String getRandomKey() {
		return randomkey;
	}

	public JSONObject getUserDetails() {
		return userDetails;
	}

    public void setCalledFromSpecificLocation(boolean value) {
        this.calledFromSpecificLocation = value;
    }

    public boolean isCalledFromSpecificLocation() {
        return calledFromSpecificLocation;
    }

	public void getDbConn() {
		try {

			if (AppProperties.poolingenabled != null && AppProperties.poolingenabled.equals("true")) {
				Context initContext = new InitialContext();
				Context envContext = (Context) initContext.lookup("java:/comp/env");
				DataSource ds = (DataSource) envContext.lookup("jdbc/cta");
				testCon = ds.getConnection();
//		        System.out.println("------------------------------------------------------------------------------");
//		        printCallStack(4);
//		        System.out.println("Created Connection MSC: " + testCon.hashCode());
//		        
//		     // Cast to Tomcat's DataSource
//	            org.apache.tomcat.jdbc.pool.DataSource tomcatDataSource = (org.apache.tomcat.jdbc.pool.DataSource) ds;
//
//	            // Now you can get various stats
//	            System.out.println("Active Connections MSC: " + tomcatDataSource.getActive());
//	            System.out.println("Idle Connections MSC: " + tomcatDataSource.getIdle());
//	            System.out.println("------------------------------------------------------------------------------");
				// Add more stats as per your requirement

			} else {
				// Step 1: Load the JDBC driver
				Class.forName(AppProperties.databaseDriver);

				// Step 2: Establish the connection
				String url = AppProperties.databaseUrl;
				String username = AppProperties.databaseUsername;
				String password = AppProperties.databasePassword;
				testCon = DriverManager.getConnection(url, username, password);
			}

			issuesMap.put("FATAL001", ErrorMessages.FATAL001);
			issuesMap.put("FATAL002", ErrorMessages.FATAL002);
			issuesMap.put("WARN001", ErrorMessages.WARN001);
			issuesMap.put("WARN002", ErrorMessages.WARN002);

		} catch (SQLException | ClassNotFoundException e) {
			System.err.println(Utilities.getNow());
			e.printStackTrace();
		} catch (NamingException e) {
			System.err.println(Utilities.getNow());
			e.printStackTrace();
		}
	}

	public void writeTestStepStatusToDB(int test_case, int step_number, String page_name, String page_desc,
			String xpath, String keyword, String action, String flow, String testdata, String varname, String status,
			String fsl, String fslAF, int test_step, String executedBy, double duration, String device, String issues,
			String issuetype, String ts_execStatus, String outerhtml, String foundby, List<String> foundelements,
			Devtools devtools) {

		String sql = "INSERT INTO cta.test_step_result (" + "Test_Case_Results_Id, Step_Number, Page_Name, "
				+ "Page_Description, Object_Xpath, Keyword, Action, Flow, "
				+ "TestData, VarName, Status, Failure_Screenshot_Location, "
				+ "Test_Step, Executed_By, Duration, ValDevice,Executed_Date,"
				+ "issues, issuetype, screenshot1, ThreshholdStatus, outerhtml, foundby) " + "VALUES (?, ?, ?, ?, ?, "
				+ "?, ?, ?, ?, ?," + " ?, ?, ?, ?, ?," + "?,?, ?, ?, ?, ?, ?, ?)";

		try {
			PreparedStatement stmt = testCon.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);

			// Set the values for the prepared statement
			stmt.setInt(1, test_case);
			stmt.setInt(2, step_number);
			stmt.setString(3, page_name);
			stmt.setString(4, page_desc);
			stmt.setString(5, xpath);
			stmt.setString(6, keyword);
			stmt.setString(7, action);
			stmt.setString(8, flow);
			if (testdata.length() > 1000) {
				testdata = testdata.substring(testdata.length() - 995, testdata.length());
			}
			stmt.setString(9, testdata);
			stmt.setString(10, varname);
			stmt.setString(11, status);
			stmt.setString(12, fsl);
			stmt.setInt(13, test_step);
			stmt.setString(14, executedBy);

//			int dur = (int)duration;
			stmt.setDouble(15, duration);
			stmt.setString(16, device);
			stmt.setTimestamp(17, Utilities.getCurrentTimestamp());
			if (issues.length() > 1000) {
				issues = issues.substring(issues.length() - 995, issues.length());
			}
			stmt.setString(18, issues);
			stmt.setString(19, issuetype);
			stmt.setString(20, fslAF);
			stmt.setString(21, ts_execStatus);
			if(outerhtml.length() > 1000) {
				outerhtml = outerhtml.substring(0, 1000);
			}
			stmt.setString(22, outerhtml);
			stmt.setString(23, foundby);
			// Execute the prepared statement
			stmt.executeUpdate();
			
	        ResultSet rs = stmt.getGeneratedKeys();
	        if (rs.next()) {
	            int test_step_result_id = rs.getInt(1); // Get the auto-generated ID
	            if (status.equalsIgnoreCase("FAIL")) {
					this.updateTestCaseErrorCodeToDB(test_case, issuetype, issues, test_step_result_id);
				}
	            
	            if(foundelements.size() > 1) {
					for(int i=0;i<foundelements.size();i++) {
						insertFoundElementToDB(test_step_result_id, foundelements.get(i));
					}
				}
	            if(devtools != null) {
	            	updateTestStepResultIdToApiData(test_step, test_step_result_id);	            
	            	devtools.tsrid = test_step_result_id;
	            }
	        }
	        this.updateWarningToDB(test_case, issuetype);
			
	        rs.close();
			stmt.close();
		} catch (Exception e) {
			System.err.println(Utilities.getNow());
			e.printStackTrace();
		}
	}
	
public void updateWarningToDB(int Test_Case_Results_Id, String issuetype) {
		
		String sql = "UPDATE cta.test_case_results SET warning = ? WHERE idtest_case_results = ? AND warning IS NULL";

		try {
			PreparedStatement stmt = testCon.prepareStatement(sql);

			// Set the values for the prepared statement
			stmt.setString(1, issuetype);
			stmt.setInt(2, Test_Case_Results_Id);

			// Execute the prepared statement
			stmt.executeUpdate();
			stmt.close();
		} catch (Exception e) {
			System.err.println(Utilities.getNow());
			e.printStackTrace();
		}

	}
	
	public void updateTestCaseErrorCodeToDB(int Test_Case_Results_Id, String issuetype, String issues, int test_step_result_id) {
		
		String sql = "UPDATE cta.test_case_results set issuetype = ?, issues = ?, failed_test_step_result_id = ? where idtest_case_results = ?";
		
		try {
			PreparedStatement stmt = testCon.prepareStatement(sql);
			
			if(issues != null && issues.length() > 1000) {
				issues = issues.substring(0,999);
			}
			// Set the values for the prepared statement
			stmt.setString(1, issuetype);
			stmt.setString(2, issues);
			stmt.setInt(3, test_step_result_id);
			stmt.setInt(4, Test_Case_Results_Id);

			// Execute the prepared statement
			stmt.executeUpdate();
			stmt.close();
		} catch (Exception e) {
			System.err.println(Utilities.getNow());
			e.printStackTrace();
		}

	}
	
	public void updateDepTcCountRunOne(int testSuiteResultsId, int countToAdd) {
		String sql = "UPDATE cta.test_suite_results " +
		             "SET depTcCountRunOne = COALESCE(depTcCountRunOne, 0) + ? " +
		             "WHERE idtest_suite_results = ?";

		try {
			PreparedStatement stmt = testCon.prepareStatement(sql);
			stmt.setInt(1, countToAdd);
			stmt.setInt(2, testSuiteResultsId);

			stmt.executeUpdate();
			stmt.close();
		} catch (Exception e) {
			System.err.println(Utilities.getNow());
			e.printStackTrace();
		}
	}
	
	public void updateDepTcCountRunTwo(int testSuiteResultsId, int countToAdd) {
		String sql = "UPDATE cta.test_suite_results " +
		             "SET depTcCountRunTwo = COALESCE(depTcCountRunTwo, 0) + ? " +
		             "WHERE idtest_suite_results = ?";

		try {
			PreparedStatement stmt = testCon.prepareStatement(sql);
			stmt.setInt(1, countToAdd);
			stmt.setInt(2, testSuiteResultsId);

			stmt.executeUpdate();
			stmt.close();
		} catch (Exception e) {
			System.err.println(Utilities.getNow());
			e.printStackTrace();
		}
	}


	public void updateSessionforTc(int Test_Case_Results_Id, String sessionid, String nodeUri) {

		String sql = "UPDATE cta.test_case_results set sessionid = ?, nodeUri = ? " + "where idtest_case_results = ?";

		try {
			PreparedStatement stmt = testCon.prepareStatement(sql);

			// Set the values for the prepared statement
			stmt.setString(1, sessionid);
			stmt.setString(2, nodeUri);
			stmt.setInt(3, Test_Case_Results_Id);

			// Execute the prepared statement
			stmt.executeUpdate();
			stmt.close();
		} catch (Exception e) {
			System.err.println(Utilities.getNow());
			e.printStackTrace();
		}
	}

	public void updateTestCaseStatusToDB(int Test_Case_Results_Id, String Status, double duration, String tc_execStatus,
			int companyId) {

		String sql = "UPDATE cta.test_case_results set Status = ?, Duration = ?, ThresholdStatus = ? "
				+ "where idtest_case_results = ?";

		try {
			PreparedStatement stmt = testCon.prepareStatement(sql);

			// Set the values for the prepared statement
			stmt.setString(1, Status);
			stmt.setDouble(2, duration);

			stmt.setString(3, tc_execStatus);
			stmt.setInt(4, Test_Case_Results_Id);

			// Execute the prepared statement
			stmt.executeUpdate();
			stmt.close();
		} catch (Exception e) {
			System.err.println(Utilities.getNow());
			e.printStackTrace();
		}

		DashBoardData dbd = new DashBoardData(this);
		dbd.updateDashBoardForStatus(companyId, Status);
	}

	public void stopTestCase(int Test_Case_Results_Id, String Status) {

		String sql = "UPDATE cta.test_case_results set Status = ? " + "where idtest_case_results = ?";

		try {
			PreparedStatement stmt = testCon.prepareStatement(sql);

			// Set the values for the prepared statement
			stmt.setString(1, Status);
			stmt.setInt(2, Test_Case_Results_Id);

			// Execute the prepared statement
			stmt.executeUpdate();
			stmt.close();
		} catch (Exception e) {
			System.err.println(Utilities.getNow());
			e.printStackTrace();
		}
	}

	public void updateTestCaseKey(int Test_Case_Id, long randomKey) {

		String sql = "UPDATE cta.test_case set extKey = ? " + "where idtest_case = ?";

		try {
			PreparedStatement stmt = testCon.prepareStatement(sql);

			// Set the values for the prepared statement
			stmt.setLong(1, randomKey);
			stmt.setInt(2, Test_Case_Id);

			// Execute the prepared statement
			stmt.executeUpdate();
			stmt.close();
		} catch (Exception e) {
			System.err.println(Utilities.getNow());
			e.printStackTrace();
		}

	}

	public void linkTSRtoTCR(int Test_Suite_Id, int Test_Case_Id) {

		String sql = "INSERT INTO cta.test_suite_case_results (test_suite_results_id, test_case_results_id) "
				+ "VALUES (?, ?)";
		int pk = -1;

		try {
			PreparedStatement stmt = testCon.prepareStatement(sql);

			// Set the values for the prepared statement
			stmt.setInt(1, Test_Suite_Id);
			stmt.setInt(2, Test_Case_Id);

			// Execute the prepared statement
			stmt.executeUpdate();
			stmt.close();
		} catch (Exception e) {
			System.err.println(Utilities.getNow());
			e.printStackTrace();
		}
	}

	public void updateTestSuiteStatus(int tsrid, String Status) {

		String sql = "UPDATE cta.test_suite_results set Status = ? " + " where idtest_suite_results = ?";

		try {
			PreparedStatement stmt = testCon.prepareStatement(sql);

			// Set the values for the prepared statement
			stmt.setString(1, Status);
			stmt.setInt(2, tsrid);

			// Execute the prepared statement
			stmt.executeUpdate();
			stmt.close();
		} catch (Exception e) {
			System.err.println(Utilities.getNow());
			e.printStackTrace();
		}
		DashBoardData dbd = new DashBoardData(this);
		dbd.updateDashBoardForTSStatus(tsrid, Status);
	}
	
	public void updateTestSuiteCountAtEnd(int tsrid, int total) {
		String sql = "SELECT COUNT(DISTINCT tcr.idtest_case_results) AS failed_test_count "
				+ "FROM cta.test_case_results tcr "
				+ "JOIN cta.test_suite_case_results tscr "
				+ "    ON tscr.test_case_results_id = tcr.idtest_case_results "
				+ "WHERE tscr.test_suite_results_id = ? "
				+ "  AND tcr.status = 'FAIL'";
		String sql1 = "SELECT not_executed FROM cta.test_suite_results WHERE idtest_suite_results= ?";
		try {
			PreparedStatement stmt = testCon.prepareStatement(sql);

			// Set the values for the prepared statement
			stmt.setInt(1, tsrid);

			// Execute the prepared statement
			ResultSet rs = stmt.executeQuery();
			if(rs.next()) {
				int failedTestCount = rs.getInt("failed_test_count");
				stmt = testCon.prepareStatement(sql1);
				stmt.setInt(1, tsrid);
				ResultSet rs1 = stmt.executeQuery();
				int notExecuted = 0;
				if(rs1.next()) {
					notExecuted = rs1.getInt("not_executed");
				}
				int passedTestCount = total - failedTestCount - notExecuted;
				String sql2 = "UPDATE cta.test_suite_results SET passCount = ?, failCount = ? "
						+ "where idtest_suite_results = ?";
				stmt = testCon.prepareStatement(sql2);
				stmt.setInt(1, passedTestCount);
				stmt.setInt(2, failedTestCount);
				stmt.setInt(3, tsrid);
				stmt.executeUpdate();
			}
			rs.close();
			stmt.close();
		} catch (Exception e) {
			System.err.println(Utilities.getNow());
			e.printStackTrace();
		}
	}

	public void updateTotalCount(int tsrid, int count) {

		String sql = null;

		sql = "UPDATE cta.test_suite_results SET totalCount = ? " + " where idtest_suite_results = ?";

		try {
			PreparedStatement stmt = testCon.prepareStatement(sql);
			stmt.setInt(1, count);
			stmt.setInt(2, tsrid);

			// Execute the prepared statement
			stmt.executeUpdate();
			stmt.close();
		} catch (Exception e) {
			System.err.println(Utilities.getNow());
			e.printStackTrace();
		}
	}

	public void updateDur(int tsrid, double dur) {

		String sql = null;

		sql = "UPDATE cta.test_suite_results SET Duration = ? where idtest_suite_results = ?";

		try {
			PreparedStatement stmt = testCon.prepareStatement(sql);
			stmt.setDouble(1, dur);
			stmt.setInt(2, tsrid);

			// Execute the prepared statement
			stmt.executeUpdate();
			stmt.close();
		} catch (Exception e) {
			System.err.println(Utilities.getNow());
			e.printStackTrace();
		}
	}

	public void updateCount(int tsrid, String type, String status, double dur) {

		String sql = null;

		if (type.equals("PASS")) {
			sql = "UPDATE cta.test_suite_results SET passCount = passCount + 1, Status = ?,"
					+ "Duration = ? where idtest_suite_results = ?";
		} else if (type.equals("ABORTED")) {
			sql = "UPDATE cta.test_suite_results SET Status = ?" + "where idtest_suite_results = ?";
		} else {
			sql = "UPDATE cta.test_suite_results SET failCount = failCount + 1, Status = ?,"
					+ "Duration = ? where idtest_suite_results = ?";
		}

		try {
			PreparedStatement stmt = testCon.prepareStatement(sql);
			if (!type.equals("ABORTED")) {
				stmt.setString(1, status);
				stmt.setDouble(2, dur);
				stmt.setInt(3, tsrid);
			} else {
				stmt.setString(1, status);
				stmt.setInt(2, tsrid);
			}

			// Execute the prepared statement
			stmt.executeUpdate();
			stmt.close();
		} catch (Exception e) {
			System.err.println(Utilities.getNow());
			e.printStackTrace();
		}
	}

	public void update2ndRun(int tsrid, double dur) {

		String sql = null;

		sql = "UPDATE cta.test_suite_results SET passCount = passCount + 1, " + "failCount = failCount - 1, "
				+ "Duration = ? where idtest_suite_results = ?";

		try {
			PreparedStatement stmt = testCon.prepareStatement(sql);
			stmt.setDouble(1, dur);
			stmt.setInt(2, tsrid);
			stmt.executeUpdate();
			stmt.close();
		} catch (Exception e) {
			System.err.println(Utilities.getNow());
			e.printStackTrace();
		}
	}

	public int writeTestSuiteStatusToDB(int Test_Suite_Id, String Status, double duration, int passCount, int failCount,
			String runonce, String parallelRun, boolean scheduled, int totalCount, String runtype, String buildtag) {

		String Executed_By = "";
		if (!scheduled) {
			Executed_By = (String) userDetails.get("uname");
			if (Executed_By == null) {
				Executed_By = "Integration";
			}
		} else {
			Executed_By = "Scheduler";
		}
		String sql = "INSERT INTO cta.test_suite_results (Test_Suite_Id, Status, Executed_By, Executed_Date,"
				+ "Duration, passCount, failCount, rerunfailedtests, parallelrun, totalCount, runtype, buildtag, not_executed) "
				+ "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

		String sqlUpdate = "UPDATE cta.test_suite_results  set Status = 'INCOMPLETE' where Test_Suite_Id = ?"
				+ " AND (Status = 'RUNNING' OR Status = 'STARTED')";
		int pk = -1;

		try {
			PreparedStatement stmt = testCon.prepareStatement(sqlUpdate);
			stmt.setInt(1, Test_Suite_Id);
			stmt.executeUpdate();

			stmt = testCon.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);

			// Set the values for the prepared statement
			stmt.setInt(1, Test_Suite_Id);
			stmt.setString(2, Status);
			stmt.setString(3, Executed_By);
			stmt.setTimestamp(4, Utilities.getCurrentTimestamp());

			stmt.setDouble(5, duration);
			stmt.setInt(6, passCount);
			stmt.setInt(7, failCount);
			stmt.setString(8, runonce);
			stmt.setString(9, parallelRun);
			stmt.setInt(10, totalCount);
			stmt.setString(11, runtype);
			stmt.setString(12, buildtag);
			stmt.setInt(13, totalCount);
			// Execute the prepared statement
			stmt.executeUpdate();
			ResultSet generatedKeys = stmt.getGeneratedKeys();
			if (generatedKeys.next()) {
				pk = (generatedKeys.getBigDecimal(1)).intValue();
			}

			// If buildtag was null or blank, update it to the generated key
			if (buildtag == null || buildtag.trim().isEmpty()) {
				String tagSql = "UPDATE cta.test_suite_results SET buildtag = ? WHERE idtest_suite_results = ?";
				PreparedStatement updateBuildTagStmt = testCon.prepareStatement(tagSql);
				updateBuildTagStmt.setInt(1, pk); // Set the generated ID as buildtag
				updateBuildTagStmt.setInt(2, pk); // Assuming the ID is the primary key
				updateBuildTagStmt.executeUpdate();
				updateBuildTagStmt.close();
			}

			generatedKeys.close();

			stmt.close();
		} catch (Exception e) {
			System.err.println(Utilities.getNow());
			e.printStackTrace();
		}
		DashBoardData dbd = new DashBoardData(this);
		dbd.updateDashBoardForTSStatus(pk, Status);
		return pk;
	}

	public int writeTestCaseStatusToDB(int Test_Case_Id, String Status, String Executed_By, double duration,
			String browser, String testcase, int companyid) {

		Executed_By = (String) userDetails.get("uname");
		String sql = "INSERT INTO cta.test_case_results (Test_Case_Id, Status, Executed_By, Executed_Date,"
				+ "Duration, Browser, test_case_name) " + "VALUES (?, ?, ?, ?, ?, ?, ?)";
		int pk = -1;

		try {
			PreparedStatement stmt = testCon.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);

			// Set the values for the prepared statement
			stmt.setInt(1, Test_Case_Id);
			stmt.setString(2, Status);
			stmt.setString(3, Executed_By);
			stmt.setTimestamp(4, Utilities.getCurrentTimestamp());
			stmt.setDouble(5, duration);
			stmt.setString(6, browser);
			stmt.setString(7, testcase);

			// Execute the prepared statement
			stmt.executeUpdate();
			ResultSet generatedKeys = stmt.getGeneratedKeys();
			if (generatedKeys.next()) {
				pk = (generatedKeys.getBigDecimal(1)).intValue();
			}

			generatedKeys.close();
			stmt.close();
		} catch (Exception e) {
			System.err.println(Utilities.getNow());
			e.printStackTrace();
		}

		DashBoardData dbd = new DashBoardData(this);
		dbd.updateDashBoardForStatus(companyid, Status);

		return pk;
	}

	public boolean getNumberOfTestSteps(int Test_Case_Id, int stepNum) {
		String sql = "SELECT Step_Number FROM  cta.test_step  WHERE "
				+ "Test_Case_Id = ? AND Step_Number = ? AND status = 0";
		boolean exists = false;

		try {
			PreparedStatement stmt = testCon.prepareStatement(sql);

			// Set the values for the prepared statement
			stmt.setInt(1, Test_Case_Id);
			stmt.setInt(2, stepNum);

			// Execute the prepared statement
			ResultSet resultSet = stmt.executeQuery();

			if (resultSet.next()) {
				exists = true;
			}
			resultSet.close();
			stmt.close();
			resultSet.close();
		} catch (Exception e) {
			System.err.println(Utilities.getNow());
			e.printStackTrace();
		}

		return exists;
	}

	public void deleteTestSteps(JSONArray testSteps) {

		for (int i = 0; i < testSteps.size(); i++) {
			long tsidl = (long) testSteps.get(i);
			int tsid = (int) tsidl;
			deleteTestStep(tsid);
		}
	}

	public void deleteTestStep(int teststepid) {
		
		String sql = "UPDATE cta.test_step SET status = 1 WHERE  " + "idtest_step = ?";
		String checkMobileAutomationSql = "SELECT action FROM cta.mobile_automation WHERE idtest_step = ?";
	    String deleteMobileAutomationSql = "DELETE FROM cta.mobile_automation WHERE idtest_step = ?";

		try {
			PreparedStatement stmt = testCon.prepareStatement(sql);
			stmt.setInt(1, teststepid);
			stmt.executeUpdate();
			
			String action = null;
	        try (PreparedStatement checkStmt = testCon.prepareStatement(checkMobileAutomationSql)) {
	            checkStmt.setInt(1, teststepid);
	            ResultSet rs = checkStmt.executeQuery();
	            if (rs.next()) {
	                action = rs.getString("action");
	            }
	        }
	        
	        if (action != null) {
	            int targetTestStepId1 = teststepid;
	            int targetTestStepId2 = ( 
	                "click".equalsIgnoreCase(action) || "sendKeys".equalsIgnoreCase(action) 
	            ) ? teststepid - 1 : -1;

	            try (PreparedStatement deleteStmt = testCon.prepareStatement(deleteMobileAutomationSql)) {
	                deleteStmt.setInt(1, targetTestStepId1);
	                deleteStmt.executeUpdate();
	            }
	            
	            if (targetTestStepId2 != -1) {
	                try (PreparedStatement deleteStmt = testCon.prepareStatement(deleteMobileAutomationSql)) {
	                    deleteStmt.setInt(1, targetTestStepId2);
	                    deleteStmt.executeUpdate();
	                }
	                
	                try (PreparedStatement stmt1 = testCon.prepareStatement(sql)) {
	                    stmt1.setInt(1, targetTestStepId2);
	                    stmt1.executeUpdate();
	                }
	            }
	        }

			String uname = (String) userDetails.get("uname");
			JSONObject latestTestSteps = this.getLatestRecording(uname);
			Utilities.cacheLatestRecording(uname, latestTestSteps);
			stmt.close();
		} catch (Exception e) {
			System.err.println(Utilities.getNow());
			e.printStackTrace();
		}
	}
	
	public JSONObject updateFlowForBulkTestSteps(String flowAction, JSONArray testStepIds) throws Exception {
		
		JSONObject result = new JSONObject();
	    JSONArray updatedIds = new JSONArray();
	    JSONArray notUpdatedIds = new JSONArray();

	    if (testStepIds == null || testStepIds.isEmpty()) {
	        result.put("updatedTestStepIds", updatedIds);
	        result.put("notUpdatedTestStepIds", notUpdatedIds);
	        return result;
	    }

	    String sql = "UPDATE cta.test_step SET Flow = ? WHERE idtest_step = ?";
	    
	    try (PreparedStatement stmt = testCon.prepareStatement(sql)) {
	        for (Object idObj : testStepIds) {
	            Long id = (Long) idObj;
	            stmt.setString(1, flowAction);
	            stmt.setLong(2, id);
	            int rowsAffected = stmt.executeUpdate();

	            if (rowsAffected > 0) {
	                updatedIds.add(id);
	            } else {
	                notUpdatedIds.add(id);
	            }
	        }
	    } catch (Exception e) {
	        System.err.println(Utilities.getNow());
	        e.printStackTrace();
	    }

	    result.put("updatedTestStepIds", updatedIds);
	    result.put("notUpdatedTestStepIds", notUpdatedIds);
	    return result;
	}
	
public JSONObject updateWaitForBulkTestSteps(String wait, String waittime, JSONArray testStepIds) {
		
		JSONObject result = new JSONObject();
	    JSONArray updatedIds = new JSONArray();
	    JSONArray notUpdatedIds = new JSONArray();

	    if (testStepIds == null || testStepIds.isEmpty()) {
	        result.put("updatedTestStepIds", updatedIds);
	        result.put("notUpdatedTestStepIds", notUpdatedIds);
	        return result;
	    }

	    String sql = "UPDATE cta.test_step SET wait = ?, waittime = ? WHERE idtest_step = ?";
	    
	    try (PreparedStatement stmt = testCon.prepareStatement(sql)) {
	        for (Object idObj : testStepIds) {
	            Long id = (Long) idObj;
	            stmt.setString(1, wait);
	            stmt.setString(2, waittime);
	            stmt.setLong(3, id);
	            int rowsAffected = stmt.executeUpdate();

	            if (rowsAffected > 0) {
	                updatedIds.add(id);
	            } else {
	                notUpdatedIds.add(id);
	            }
	        }
	    } catch (Exception e) {
	        System.err.println(Utilities.getNow());
	        e.printStackTrace();
	    }

	    result.put("updatedTestStepIds", updatedIds);
	    result.put("notUpdatedTestStepIds", notUpdatedIds);
	    return result;
	}

	public JSONObject updateCondLogicForBulkTestSteps(String condLogic, String condexp, JSONArray testStepIds) {
	
		JSONObject result = new JSONObject();
	    JSONArray updatedIds = new JSONArray();
	    JSONArray notUpdatedIds = new JSONArray();
	
	    if (testStepIds == null || testStepIds.isEmpty()) {
	        result.put("updatedTestStepIds", updatedIds);
	        result.put("notUpdatedTestStepIds", notUpdatedIds);
	        return result;
	    }
	
	    String sql = "UPDATE cta.test_step_attr SET condLogic = ?, condexp = ? WHERE test_step = ?";
	    
	    try (PreparedStatement stmt = testCon.prepareStatement(sql)) {
	        for (Object idObj : testStepIds) {
	            Long id = (Long) idObj;
	            stmt.setString(1, condLogic);
	            stmt.setString(2, condexp);
	            stmt.setLong(3, id);
	            int rowsAffected = stmt.executeUpdate();
	
	            if (rowsAffected > 0) {
	                updatedIds.add(id);
	            } else {
	                notUpdatedIds.add(id);
	            }
	        }
	    } catch (Exception e) {
	        System.err.println(Utilities.getNow());
	        e.printStackTrace();
	    }
	
	    result.put("updatedTestStepIds", updatedIds);
	    result.put("notUpdatedTestStepIds", notUpdatedIds);
	    return result;
	}

	public JSONObject updateActionForBulkTestSteps(String Keyword, String Action, String subAction, JSONArray testStepIds) throws Exception {
		
		JSONObject result = new JSONObject();
	    JSONArray updatedIds = new JSONArray();
	    JSONArray notUpdatedIds = new JSONArray();
	
	    if (testStepIds == null || testStepIds.isEmpty()) {
	        result.put("updatedTestStepIds", updatedIds);
	        result.put("notUpdatedTestStepIds", notUpdatedIds);
	        return result;
	    }
	
	    String sql = "UPDATE cta.test_step SET Keyword=?, Action=?, subAction=? WHERE idtest_step = ?";
	    
	    try (PreparedStatement stmt = testCon.prepareStatement(sql)) {
	        for (Object idObj : testStepIds) {
	            Long id = (Long) idObj;
	            stmt.setString(1, Keyword);
				stmt.setString(2, Action);
				stmt.setString(3, subAction);
	            stmt.setLong(4, id);
	            int rowsAffected = stmt.executeUpdate();
	
	            if (rowsAffected > 0) {
	                updatedIds.add(id);
	            } else {
	                notUpdatedIds.add(id);
	            }
	        }
	    } catch (Exception e) {
	        System.err.println(Utilities.getNow());
	        e.printStackTrace();
	    }
	
	    result.put("updatedTestStepIds", updatedIds);
	    result.put("notUpdatedTestStepIds", notUpdatedIds);
	    return result;
	}

	public void changeToAssert(JSONArray testSteps) {

		for (int i = 0; i < testSteps.size(); i++) {
			long tsidl = (long) testSteps.get(i);
			int tsid = (int) tsidl;
			changeToAssert(tsid);
		}
	}

	public void changeToAssert(int teststepid) {
		String sql = "UPDATE cta.test_step SET Keyword = 'Assertion',"
				+ " Action = 'validatePartial', subAction='validatePartial' WHERE  " + "idtest_step = ?";
		try {
			PreparedStatement stmt = testCon.prepareStatement(sql);
			stmt.setInt(1, teststepid);
			stmt.executeUpdate();

			String uname = (String) userDetails.get("uname");
			JSONObject latestTestSteps = this.getLatestRecording(uname);
			Utilities.cacheLatestRecording(uname, latestTestSteps);
			stmt.close();
		} catch (Exception e) {
			System.err.println(Utilities.getNow());
			e.printStackTrace();
		}
	}

	public void updateTestStepFlow(int teststepid, String flow) {
		String sql = "UPDATE cta.test_step SET Flow = ? WHERE  " + "idtest_step = ?";
		try {
			PreparedStatement stmt = testCon.prepareStatement(sql);
			stmt.setString(1, flow);
			stmt.setInt(2, teststepid);
			stmt.executeUpdate();

			stmt.close();
		} catch (Exception e) {
			System.err.println(Utilities.getNow());
			e.printStackTrace();
		}
	}

	public void updateSelectOptions(int teststepid, String select) {
		String sql = "UPDATE cta.test_step SET Action = 'Select', subAction = ?  WHERE  " + "idtest_step = ?";
		try {
			PreparedStatement stmt = testCon.prepareStatement(sql);
			stmt.setString(1, select);
			stmt.setInt(2, teststepid);
			stmt.executeUpdate();

			stmt.close();
		} catch (Exception e) {
			System.err.println(Utilities.getNow());
			e.printStackTrace();
		}
	}

	public void updateCheckBoxOptions(int teststepid, String select) {
		String sql = "UPDATE cta.test_step SET Action = ? WHERE  " + "idtest_step = ?";
		try {
			PreparedStatement stmt = testCon.prepareStatement(sql);
			stmt.setString(1, select);
			stmt.setInt(2, teststepid);
			stmt.executeUpdate();

			stmt.close();
		} catch (Exception e) {
			System.err.println(Utilities.getNow());
			e.printStackTrace();
		}
	}

	public void updateTestStepBeforeAndTime(int teststepid, String before, int waittime) {
		String sql = "UPDATE cta.test_step SET wait = ?, waittime = ? WHERE  " + "idtest_step = ?";
		try {
			PreparedStatement stmt = testCon.prepareStatement(sql);
			stmt.setString(1, before);
			stmt.setInt(2, waittime);
			stmt.setInt(3, teststepid);
			stmt.executeUpdate();

			stmt.close();
		} catch (Exception e) {
			System.err.println(Utilities.getNow());
			e.printStackTrace();
		}
	}

	public void updateTestStepBefore(int teststepid, String before) {
		String sql = "UPDATE cta.test_step SET wait = ? WHERE  " + "idtest_step = ?";
		try {
			PreparedStatement stmt = testCon.prepareStatement(sql);
			stmt.setString(1, before);
			stmt.setInt(2, teststepid);
			stmt.executeUpdate();

			stmt.close();
		} catch (Exception e) {
			System.err.println(Utilities.getNow());
			e.printStackTrace();
		}
	}

	public void updateTestStepWaitTime(int teststepid, int waittime) {
		String sql = "UPDATE cta.test_step SET waittime = ? WHERE  " + "idtest_step = ?";
		try {
			PreparedStatement stmt = testCon.prepareStatement(sql);
			stmt.setInt(1, waittime);
			stmt.setInt(2, teststepid);
			stmt.executeUpdate();

			stmt.close();
		} catch (Exception e) {
			System.err.println(Utilities.getNow());
			e.printStackTrace();
		}
	}

	public void updateTestStepThreshold(int teststepid, int thresholdtime) {
		String sql = "UPDATE cta.test_step SET teststepthreshold = ? WHERE  " + "idtest_step = ?";
		try {
			PreparedStatement stmt = testCon.prepareStatement(sql);
			stmt.setInt(1, thresholdtime);
			stmt.setInt(2, teststepid);
			stmt.executeUpdate();

			stmt.close();
		} catch (Exception e) {
			System.err.println(Utilities.getNow());
			e.printStackTrace();
		}
	}

	public void updateTestCaseThreshold(int testcaseid, int thresholdtime) {
		String sql = "UPDATE cta.test_case SET testcasethreshold = ? WHERE  " + "idtest_case = ?";
		try {
			PreparedStatement stmt = testCon.prepareStatement(sql);
			stmt.setInt(1, thresholdtime);
			stmt.setInt(2, testcaseid);
			stmt.executeUpdate();

			stmt.close();
		} catch (Exception e) {
			System.err.println(Utilities.getNow());
			e.printStackTrace();
		}
	}

	public void updateTestStepIsAVar(int teststepid, String testdatasource) {

		String varName = Utilities.randomGen("ALPHA", 6, AppProperties.chatgpton);
		varName = AppProperties.delimiter + varName + AppProperties.delimiter;

		try {

			String sql = "SELECT RecordedData FROM cta.test_step " + " WHERE  idtest_step = ?";
			PreparedStatement stmt = testCon.prepareStatement(sql);
			stmt.setInt(1, teststepid);
			ResultSet rs = stmt.executeQuery();

			if (rs.next()) {
				String orgTD = rs.getString("RecordedData");
				sql = "UPDATE cta.test_step SET VarName = ?" + " WHERE  idtest_step = ?";
				stmt = testCon.prepareStatement(sql);
				stmt.setString(1, varName);
				stmt.setInt(2, teststepid);
				stmt.executeUpdate();
				updateFutureTestDataWithVar(teststepid, varName, orgTD, false);
			}

			rs.close();
			stmt.close();
		} catch (Exception e) {
			System.err.println(Utilities.getNow());
			e.printStackTrace();
		}
	}

	public void updateTestStepD0(int teststepid, String testdatasource) {

		String varName = AppProperties.delimiter + testdatasource + AppProperties.delimiter;

		try {

			String sql = "SELECT RecordedData FROM cta.test_step " + " WHERE  idtest_step = ?";
			PreparedStatement stmt = testCon.prepareStatement(sql);
			stmt.setInt(1, teststepid);
			ResultSet rs = stmt.executeQuery();

			if (rs.next()) {
				String orgTD = rs.getString("RecordedData");
				sql = "UPDATE cta.test_step SET testdata_source = ?, VarName = ?" + " WHERE  idtest_step = ?";
				stmt = testCon.prepareStatement(sql);
				stmt.setString(1, testdatasource);
				stmt.setString(2, varName);
				stmt.setInt(3, teststepid);
				stmt.executeUpdate();

			}

			rs.close();
			stmt.close();
		} catch (Exception e) {
			System.err.println(Utilities.getNow());
			e.printStackTrace();
		}
	}

	public void updateTestStepDNEXT(int teststepid, String testdatasource) {

		String varName = AppProperties.delimiter + testdatasource + AppProperties.delimiter;

		try {
			String sql = "UPDATE cta.test_step SET testdata_source = ?, VarName = ?, Action = ?"
					+ " WHERE  idtest_step = ?";
			PreparedStatement stmt = testCon.prepareStatement(sql);
			stmt = testCon.prepareStatement(sql);
			stmt.setString(1, testdatasource);
			stmt.setString(2, varName);
			stmt.setString(3, testdatasource);
			stmt.setInt(4, teststepid);
			stmt.executeUpdate();
			stmt.close();
		} catch (Exception e) {
			System.err.println(Utilities.getNow());
			e.printStackTrace();
		}
	}

	public void updateFutureTestDataWithVar(int teststepid, String varName, String origTestData, boolean undo) {
		try {

			String sql = "SELECT Test_Case_Id, Step_Number FROM cta.test_step " + " WHERE  idtest_step = ?";
			PreparedStatement stmt = testCon.prepareStatement(sql);
			stmt.setInt(1, teststepid);
			ResultSet rs = stmt.executeQuery();

			if (rs.next()) {
				int testcaseid = rs.getInt(1);
				int stepnum = rs.getInt(2);

				if (undo) {
					sql = "SELECT idtest_step, Object_Xpath, TestData FROM cta.test_step "
							+ " WHERE  Test_Case_id = ? AND Step_Number > ?";
				} else {
					sql = "SELECT idtest_step, Object_Xpath, RecordedData FROM cta.test_step "
							+ " WHERE  Test_Case_id = ? AND Step_Number > ?";
				}
				stmt = testCon.prepareStatement(sql);
				stmt.setInt(1, testcaseid);
				stmt.setInt(2, stepnum);

				ResultSet rs2 = stmt.executeQuery();

				while (rs2.next()) {
					int teststepid_x = rs2.getInt(1);
					String xpath = rs2.getString(2);
					String testd = rs2.getString(3);

					boolean found = false;
					if (xpath.contains(origTestData)) {
						xpath = xpath.replace(origTestData, varName);
						found = true;
					}

					if (testd.contains(origTestData)) {
						testd = testd.replace(origTestData, varName);
						found = true;
					}

					if (found) {
						sql = "UPDATE cta.test_step SET Object_Xpath = ?, TestData = ?" + " WHERE  idtest_step = ?";
						stmt = testCon.prepareStatement(sql);
						stmt.setString(1, xpath);
						stmt.setString(2, testd);
						stmt.setInt(3, teststepid_x);
						stmt.executeUpdate();
					}
				}
				rs2.close();
			}
			rs.close();
			stmt.close();
		} catch (Exception e) {
			System.err.println(Utilities.getNow());
			e.printStackTrace();
		}
	}

// Generating #RANDOMCHAR1_6#
	int countChar = 0, countNumber = 0, countAlpha = 0, countToday = 0, countEmail = 0;

	public String generateRandomString(int teststepid, String type, String length) {
		// int countChar = 0, countNumber = 0, countAlpha = 0, countToday = 0,
		// countEmail = 0;

//		  Random random = new Random();
		StringBuilder randomString = new StringBuilder();
		randomString.append(AppProperties.delimiter + "RANDOM");

		if (type.equals("CHAR")) {
			randomString.append(type);
		} else if (type.equals("NUMBER")) {
			randomString.append(type);
		} else if (type.equals("ALPHA")) {
			randomString.append(type);
		} else if (type.equals("TODAY")) {
			randomString.append(type);
		} else if (type.equals("EMAIL")) {
			randomString.append(type);
		}
//	      int count = getRandomCount(teststepid, randomString.toString() + "%");
//	      if(count >= 0) {
//	    	  count = count + 1;
//	      } else {
//	    	  return "ERROR";
//	      }
		String countStr = Utilities.randomGen(AppProperties.NUMBER, 5, false);
		int count = Integer.valueOf(countStr);
		randomString.append("_").append(count);
		randomString.append("-").append(length).append(AppProperties.delimiter);
		return randomString.toString();
	}

	public int getRandomCount(int teststepid, String type) {

		int count = -1;
		try {
			int testcaseid = getTestCaseFromTestStep(teststepid);
			String sql = "SELECT COUNT(*) FROM cta.test_step WHERE Test_Case_Id = ? and " + "TestData LIKE ?";
			PreparedStatement stmt = testCon.prepareStatement(sql);
			stmt.setInt(1, testcaseid);
			stmt.setString(2, type);
			ResultSet rs = stmt.executeQuery();
			if (rs.next()) {
				count = rs.getInt(1);
			}
			rs.close();
			stmt.close();
		} catch (Exception e) {
			System.err.println(Utilities.getNow());
			e.printStackTrace();
		}

		return count;
	}

	public int getTestCaseFromTestStep(int teststepid) {

		int testcaseid = -1;
		try {
			String sql = "SELECT Test_Case_Id, Step_Number FROM cta.test_step " + " WHERE  idtest_step = ?";
			PreparedStatement stmt = testCon.prepareStatement(sql);
			stmt.setInt(1, teststepid);
			ResultSet rs = stmt.executeQuery();

			if (rs.next()) {
				testcaseid = rs.getInt(1);
			}
			rs.close();
			stmt.close();
		} catch (Exception e) {
			System.err.println(Utilities.getNow());
			e.printStackTrace();
		}

		return testcaseid;
	}

// To Update the Generated Data in test_step table
	public void updateTestStepGenData(int teststepid, String testdatasource, String gendataType, int gendatalen,
			String dateFormat, String dateOffset) {
		String testData = "";
		if (gendataType.equals(AppProperties.TODAY)) {
			testData = generateRandomString(teststepid, gendataType, dateFormat);
		} else {
			testData = generateRandomString(teststepid, gendataType, String.valueOf(gendatalen));
		}

		String sql = null;

		try {
			sql = "UPDATE cta.test_step SET testdata_source = ?, TestData = ?, "
					+ "gendataType = ?, gendatalen = ?, gendateformat=?, gendateoffset=? " + " WHERE  idtest_step = ?";
			PreparedStatement stmt = testCon.prepareStatement(sql);
			stmt.setString(1, testdatasource);
			stmt.setString(2, testData);
			stmt.setString(3, gendataType);
			stmt.setInt(4, gendatalen);
			stmt.setString(5, dateFormat);
			stmt.setString(6, dateOffset);
			stmt.setInt(7, teststepid);
			stmt.executeUpdate();
			stmt.close();
		} catch (Exception e) {
			System.err.println(Utilities.getNow());
			e.printStackTrace();
		}
	}

	public void updateTestStepFindElementByData(int teststepid, String testdatasource, String elementType,
			int elementIndex, String strategy) {

		String sql = null;

		try {
			sql = "UPDATE cta.test_step SET testdata_source = ?, samerowtype = ?, samerowindex = ? "
					+ " WHERE  idtest_step = ?";
			PreparedStatement stmt = testCon.prepareStatement(sql);
			stmt.setString(1, testdatasource);
			stmt.setString(2, elementType);
			stmt.setInt(3, elementIndex);
			stmt.setInt(4, teststepid);
			stmt.executeUpdate();

			String sql_atr = "UPDATE cta.test_step_attr SET locationstrategy = ? WHERE test_step = ?";
			stmt = testCon.prepareStatement(sql_atr);
			stmt.setString(1, strategy);
			stmt.setInt(2, teststepid);
			stmt.executeUpdate();

			stmt.close();
		} catch (Exception e) {
			System.err.println(Utilities.getNow());
			e.printStackTrace();
		}
	}

	public void updateStrategyForXY(int teststepid, String strategy, int xpos, int ypos) {

		try {
			String sql = "UPDATE cta.test_step_attr SET locationstrategy = ?, xpos = ?, ypos = ? "
					+ "WHERE test_step = ?";
			PreparedStatement stmt = testCon.prepareStatement(sql);
			stmt.setString(1, strategy);
			stmt.setInt(2, xpos);
			stmt.setInt(3, ypos);
			stmt.setInt(4, teststepid);
			stmt.executeUpdate();

			stmt.close();
		} catch (Exception e) {
			System.err.println(Utilities.getNow());
			e.printStackTrace();
		}
	}

	public void updateTestStepFindElementByPrevElement(int teststepid, String testdatasource, String strategy) {

		String sql = null;

		try {
			sql = "UPDATE cta.test_step SET testdata_source = ? " + " WHERE  idtest_step = ?";
			PreparedStatement stmt = testCon.prepareStatement(sql);
			stmt.setString(1, testdatasource);
			stmt.setInt(2, teststepid);
			stmt.executeUpdate();

			String sql_atr = "UPDATE cta.test_step_attr SET locationstrategy = ? WHERE test_step = ?";
			stmt = testCon.prepareStatement(sql_atr);
			stmt.setString(1, strategy);
			stmt.setInt(2, teststepid);
			stmt.executeUpdate();

			stmt.close();
		} catch (Exception e) {
			System.err.println(Utilities.getNow());
			e.printStackTrace();
		}
	}

	public void updateTestDataDynamicText(int teststepid, String testdatasource) {

		String testData = AppProperties.delimiter + AppProperties.DYNAMICTEXT + AppProperties.delimiter;
		String varName = Utilities.randomGen("ALPHA", 6, AppProperties.chatgpton);
		varName = AppProperties.delimiter + varName + AppProperties.delimiter;
		String sql = null;

		try {
			sql = "UPDATE cta.test_step SET testdata_source = ?, TestData = ?, VarName = ? "
					+ " WHERE  idtest_step = ?";
			PreparedStatement stmt = testCon.prepareStatement(sql);
			stmt.setString(1, testdatasource);
			stmt.setString(2, testData);
			stmt.setString(3, varName);
			stmt.setInt(4, teststepid);
			stmt.executeUpdate();

			stmt.close();
		} catch (Exception e) {
			System.err.println(Utilities.getNow());
			e.printStackTrace();
		}
	}

	public void updateTestStepScenarioVariable(int teststepid, String testdatasource, String testdatavalue) {
		String sql = null;
		try {
			sql = "UPDATE cta.test_step SET  TestData = ?, testdata_source = ? " + " WHERE idtest_step = ?";
			PreparedStatement stmt = testCon.prepareStatement(sql);
			stmt.setString(1, testdatavalue);
			stmt.setString(2, testdatasource);
			stmt.setInt(3, teststepid);
			stmt.executeUpdate();

			stmt.close();
		} catch (Exception e) {
			System.err.println(Utilities.getNow());
			e.printStackTrace();
		}
	}

	public void updateTestStepOtherScenarioVariable(int teststepid, String testdatasource, String testdatavalue,
			int depttc) {
		String sql = null;
		try {
			sql = "UPDATE cta.test_step SET TestData = ?, dependantTC =? , testdata_source = ? " + " "
					+ "WHERE idtest_step = ?";
			PreparedStatement stmt = testCon.prepareStatement(sql);
			stmt.setString(1, testdatavalue);
			stmt.setInt(2, depttc);
			stmt.setString(3, testdatasource);
			stmt.setInt(4, teststepid);
			stmt.executeUpdate();

			int tcid = gettcidfromTestStep(teststepid);
			turnOnContinueTest(tcid);

			stmt.close();
		} catch (Exception e) {
			System.err.println(Utilities.getNow());
			e.printStackTrace();
		}
	}

	public void updateTestStepFromApi(int teststepid, String testdatasource, String apiName, int apiid, String apiParam,
			String apiQuery) {
		String sql = null;
		try {
			sql = "UPDATE cta.test_step SET testdata_source = ?, TestData = ?, apiid =?, apiparam=?, DBQuery=?"
					+ "WHERE idtest_step = ?";
			PreparedStatement stmt = testCon.prepareStatement(sql);
			stmt.setString(1, testdatasource);
			stmt.setString(2, apiName);
			stmt.setInt(3, apiid);
			stmt.setString(4, apiParam);
			stmt.setString(5, apiQuery);
			stmt.setInt(6, teststepid);
			stmt.executeUpdate();

			stmt.close();
		} catch (Exception e) {
			System.err.println(Utilities.getNow());
			e.printStackTrace();
		}
	}

	public void validateTestStepFromApi(int teststepid, String valtype, String apiName, int apiid, String apiParam,
			String apiQuery) {
		String sql = null;
		try {
			sql = "UPDATE cta.test_step SET ValDevice = ?, TestData = ?, apiid =?, apiparam=?, DBQuery=?"
					+ "WHERE idtest_step = ?";
			PreparedStatement stmt = testCon.prepareStatement(sql);
			stmt.setString(1, valtype);
			stmt.setString(2, apiName);
			stmt.setInt(3, apiid);
			stmt.setString(4, apiParam);
			stmt.setString(5, apiQuery);
			stmt.setInt(6, teststepid);
			stmt.executeUpdate();

			stmt.close();
		} catch (Exception e) {
			System.err.println(Utilities.getNow());
			e.printStackTrace();
		}
	}

	public void updateTestStepFromFile(int teststepid, String testdatasource, String fileName, String fileField,
			String scope, String endrange) {
		String sql = null;
		try {
			sql = "UPDATE cta.test_step SET testdata_source = ?, "
					+ "filename = ?, fileField = ?, TestData = ? WHERE idtest_step = ?";
			String sql_atrr = "UPDATE cta.test_step_attr SET scope=?, endrange=? WHERE test_step=?";
			PreparedStatement stmt = testCon.prepareStatement(sql);
			stmt.setString(1, testdatasource);
			stmt.setString(2, fileName);
			stmt.setString(3, fileField);
			if (fileField != null && !fileField.equals("") && !fileField.equals("null") && !fileField.equals("~")
					&& !fileField.contains("undefined")) {
				stmt.setString(4, fileName + AppProperties.testdatadelimiter + fileField);
			} else {
				stmt.setString(4, fileName);
			}
			stmt.setInt(5, teststepid);
			stmt.executeUpdate();

			stmt = testCon.prepareStatement(sql_atrr);
			stmt.setString(1, scope);
			stmt.setString(2, endrange);
			stmt.setInt(3, teststepid);
			stmt.executeUpdate();
			stmt.close();
		} catch (Exception e) {
			System.err.println(Utilities.getNow());
			e.printStackTrace();
		}
	}

	public void updateTestStepFromFile(int teststepid, String testdatasource, String fileName) {
		String sql = null;
		try {
			sql = "UPDATE cta.test_step SET testdata_source = ?, " + "filename = ?, TestData = ? WHERE idtest_step = ?";
			PreparedStatement stmt = testCon.prepareStatement(sql);
			stmt.setString(1, testdatasource);
			stmt.setString(2, fileName);
			stmt.setString(3, fileName);
			stmt.setInt(4, teststepid);
			stmt.executeUpdate();
			stmt.close();
		} catch (Exception e) {
			System.err.println(Utilities.getNow());
			e.printStackTrace();
		}
	}

	public void updatedbfortestdata(int teststepid, String testdatasource, String fileName) {
		String sql = null;
		try {
			sql = "UPDATE cta.test_step SET testdata_source = ?, " + "filename = ?, TestData = ? WHERE idtest_step = ?";
			PreparedStatement stmt = testCon.prepareStatement(sql);
			stmt.setString(1, testdatasource);
			stmt.setString(2, fileName);
			stmt.setString(3, fileName);
			stmt.setInt(4, teststepid);
			stmt.executeUpdate();
			stmt.close();
		} catch (Exception e) {
			System.err.println(Utilities.getNow());
			e.printStackTrace();
		}
	}

	public void updatedbfortestdata(int teststepid, String testdatasource, String fileName, String fileField,
			String scope, String endrange) {
		String sql = null;
		try {
			sql = "UPDATE cta.test_step SET testdata_source = ?, "
					+ "filename = ?, fileField = ?, TestData = ? WHERE idtest_step = ?";
			String sql_atrr = "UPDATE cta.test_step_attr SET scope=?, endrange=? WHERE test_step=?";
			PreparedStatement stmt = testCon.prepareStatement(sql);
			stmt.setString(1, testdatasource);
			stmt.setString(2, fileName);
			stmt.setString(3, fileField);
			if (fileField != null && !fileField.equals("") && !fileField.equals("null")) {
				stmt.setString(4, fileName + AppProperties.testdatadelimiter + fileField);
			} else {
				stmt.setString(4, fileName);
			}
			stmt.setInt(5, teststepid);
			stmt.executeUpdate();

			stmt = testCon.prepareStatement(sql_atrr);
			stmt.setString(1, scope);
			stmt.setString(2, endrange);
			stmt.setInt(3, teststepid);
			stmt.executeUpdate();
			stmt.close();
		} catch (Exception e) {
			System.err.println(Utilities.getNow());
			e.printStackTrace();
		}
	}

	public void updateTestStepFromGoogleSheet(int teststepid, String testdatasource, String sheetId, String fileField) {
		String sql = null;
		try {
			sql = "UPDATE cta.test_step SET testdata_source = ?, " + "filename = ?, fileField = ?, TestData = ? "
					+ "WHERE idtest_step = ?";
			PreparedStatement stmt = testCon.prepareStatement(sql);
			stmt.setString(1, testdatasource);
			stmt.setString(2, sheetId);
			stmt.setString(3, fileField);
			if (fileField != null && !fileField.equals("") && !fileField.equals("null")) {
				stmt.setString(4, sheetId + AppProperties.testdatadelimiter + fileField);
			} else {
				stmt.setString(4, sheetId);
			}
			stmt.setInt(5, teststepid);
			stmt.executeUpdate();
			stmt.close();
		} catch (Exception e) {
			System.err.println(Utilities.getNow());
			e.printStackTrace();
		}
	}

	public JSONObject getRecordedTestDataList(int testcaseid) {
		String sql = null;
		sql = "select VarName from cta.test_step where Test_Case_Id = ? and VarName != \"\"";
		JSONObject jsonObject = new JSONObject();
		try {
			PreparedStatement stmt = testCon.prepareStatement(sql);
			stmt.setInt(1, testcaseid);
			ResultSet rs = stmt.executeQuery();
			Set<String> uniqueValues = new HashSet<>();
			List<String> values = new ArrayList<>();
			JSONArray jsonArray = new JSONArray();
			while (rs.next()) {
				String value = rs.getString("VarName");
				if (!uniqueValues.contains(value)) {
					uniqueValues.add(value);
					jsonArray.add(value);
				}
			}

			jsonObject.put("list", jsonArray);
			rs.close();
			stmt.close();
		} catch (Exception e) {
			System.err.println(Utilities.getNow());
			e.printStackTrace();
		}
		return jsonObject;
	}

	public void updateTestStepAsRecorded(int teststepid, String testdatasource) {

		String sql = null;
		sql = "SELECT * FROM cta.test_step WHERE  " + "idtest_step = ?";
		try {
			PreparedStatement stmt = testCon.prepareStatement(sql);
			stmt.setInt(1, teststepid);
			ResultSet rs = stmt.executeQuery();
			String recordedData = "";
			String varName = null;
			if (rs.next()) {
				JSONObject prevRow = getTestStepsFromDb(rs, false, false);
				recordedData = (String) prevRow.get("RecordedData");
				varName = (String) prevRow.get("VarName");
			}

			sql = "UPDATE cta.test_step SET testdata_source = ?, TestData = ?, VarName = ?,"
					+ "dependantTC = NULL WHERE  idtest_step = ?";
			stmt = testCon.prepareStatement(sql);
			stmt.setString(1, testdatasource);
			stmt.setString(2, recordedData);
			stmt.setString(3, "");
			stmt.setInt(4, teststepid);
			stmt.executeUpdate();

			if (varName != null && !varName.equals("")) {
				updateFutureTestDataWithVar(teststepid, recordedData, varName, true);
			}

			stmt.close();
			rs.close();
		} catch (Exception e) {
			System.err.println(Utilities.getNow());
			e.printStackTrace();
		}
	}

	public void updateTestStepFromDb(int teststepid, String testdatasource, String DBQuery) {
		String sql = null;
		try {
			sql = "UPDATE cta.test_step SET testdata_source = ?, DBUrl = ?, DBQuery = ?, DBType = ?"
					+ " WHERE idtest_step = ?";
			int prodId = getProdIdFromTestStepId(teststepid);
			JSONObject json = getDbConnectionDetails(prodId);
			PreparedStatement stmt = testCon.prepareStatement(sql);
			stmt.setString(1, testdatasource);
			stmt.setString(2, (String) json.get("custDbUrl"));
			stmt.setString(3, DBQuery);
			stmt.setString(4, (String) json.get("custDbType"));
			stmt.setInt(5, teststepid);
			stmt.executeUpdate();

			stmt.close();
		} catch (Exception e) {
			System.err.println(Utilities.getNow());
			e.printStackTrace();
		}
	}

	public void updateTestStepFromEmail(int teststepid, String testdatasource, String custEmail, String custPwd,
			String SelectionCriterion, String Filter) {
		String sql = null;
		try {
			sql = "UPDATE cta.test_step SET testdata_source = ?, customerEmail = ?, customerPassword = ?, EmailSelectionCriteria = ?, EmailFilter = ?"
					+ " WHERE idtest_step = ?";
			PreparedStatement stmt = testCon.prepareStatement(sql);
			stmt.setString(1, testdatasource);
			stmt.setString(2, custEmail);
			stmt.setString(3, custPwd);
			stmt.setString(4, SelectionCriterion);
			stmt.setString(5, Filter);
			stmt.setInt(6, teststepid);
			stmt.executeUpdate();

			stmt.close();
		} catch (Exception e) {
			System.err.println(Utilities.getNow());
			e.printStackTrace();
		}
	}

	public void updateTestStepFromSMS(int teststepid, String testdatasource, String regex) {
		String sql = null;
		try {
			sql = "UPDATE cta.test_step SET testdata_source = ?, regex = ? " + " WHERE idtest_step = ?";
			PreparedStatement stmt = testCon.prepareStatement(sql);
			stmt.setString(1, testdatasource);
			stmt.setString(2, regex);
			stmt.setInt(3, teststepid);
			stmt.executeUpdate();

			stmt.close();
		} catch (Exception e) {
			System.err.println(Utilities.getNow());
			e.printStackTrace();
		}
	}

	public void updateTestStepFromOutlook(int teststepid, String testdatasource, String SelectionCriterion,
			String Filter) {
		String sql = null;
		try {
			sql = "UPDATE cta.test_step SET testdata_source = ?, EmailSelectionCriteria = ?, "
					+ "EmailFilter = ? WHERE idtest_step = ?";
			PreparedStatement stmt = testCon.prepareStatement(sql);
			stmt.setString(1, testdatasource);
			stmt.setString(2, SelectionCriterion);
			stmt.setString(3, Filter);
			stmt.setInt(4, teststepid);
			stmt.executeUpdate();

			stmt.close();
		} catch (Exception e) {
			System.err.println(Utilities.getNow());
			e.printStackTrace();
		}
	}

	public void updateTestStepSendEmail(int teststepid, String testdatasource, String custEmail, String custPwd,
			String toAddress, String subject, String content) {
		String sql = null;
		try {
			sql = "UPDATE cta.test_step SET testdata_source = ?, customerEmail = ?, "
					+ "customerPassword = ? WHERE idtest_step = ?";
			PreparedStatement stmt = testCon.prepareStatement(sql);
			stmt.setString(1, testdatasource);
			stmt.setString(2, custEmail);
			stmt.setString(3, custPwd);
			stmt.setInt(4, teststepid);
			stmt.executeUpdate();

			sql = "UPDATE cta.test_step_attr SET toaddress = ?, "
					+ "emailsubject = ?, emailcontent = ? WHERE test_step = ?";
			stmt = testCon.prepareStatement(sql);
			stmt.setString(1, toAddress);
			stmt.setString(2, subject);
			stmt.setString(3, content);
			stmt.setInt(4, teststepid);
			stmt.executeUpdate();

			stmt.close();
		} catch (Exception e) {
			System.err.println(Utilities.getNow());
			e.printStackTrace();
		}
	}

	public void writeTestStepAttrToDB(int test_step, long randomKey, String imgpath, String bgcolor, String color,
			String url, String uniqueText, String uniqueToParent, String parenttotarget, String pagename, int pageno,
			boolean isShadow, String shadowEle, int shadowIndex, String shadowPath, boolean uniqueisBackup,
			String nearestname, String parenttotargetcvrg, String uniquetoparentcvrg, long index, int xpos, int ypos,
			String elementplaceholder, int elementplaceholderindex, String classes, int classIndex, String dataValue,
			int dataValueIndex, int typeIndex, String href, int hrefIndex) {

		String sql = "INSERT INTO cta.test_step_attr (test_step, "
				+ "imgpath, bgcolor, color, pageurl, uniquetext, uniquetoparent, "
				+ "parenttotarget, pagename, pagenumber, shadowdom, shadowindex, shadowelement, "
				+ "shadowpath, uniqueisbackup, nearestname, parenttotargetcvrg, uniquetoparentcvrg, "
				+ "indexcvrg, xpos, ypos, elementplaceholder, elementplaceholderindex, classname,"
				+ "classnameindex, data_value, data_value_index, typeindex, href, hrefindex) "
				+ "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, " + "?, ?, ?, ?, ?, ?, ?, ?, ?, ?, "
				+ "?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

		String testcase = "UPDATE cta.test_case SET laststeprecordedtime = ? WHERE idtest_case=?";

		try {
			PreparedStatement stmt = testCon.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);

			// Set the values for the prepared statement
			stmt.setInt(1, test_step);
			if (imgpath != null && imgpath.length() > 500) {
				imgpath = imgpath.substring(0, 499);
			}
			stmt.setString(2, imgpath);
			stmt.setString(3, bgcolor);
			stmt.setString(4, color);
			stmt.setString(5, url);
			stmt.setString(6, uniqueText);
			stmt.setString(7, uniqueToParent);
			stmt.setString(8, parenttotarget);
			stmt.setString(9, pagename);
			stmt.setInt(10, pageno);
			stmt.setBoolean(11, isShadow);
			stmt.setInt(12, shadowIndex);
			stmt.setString(13, shadowEle);
			stmt.setString(14, shadowPath);
			stmt.setBoolean(15, uniqueisBackup);
			stmt.setString(16, nearestname);
			stmt.setString(17, parenttotargetcvrg);
			stmt.setString(18, uniquetoparentcvrg);
			stmt.setLong(19, index);
			stmt.setInt(20, xpos);
			stmt.setInt(21, ypos);
			stmt.setString(22, elementplaceholder);
			stmt.setInt(23, elementplaceholderindex);
			stmt.setString(24, classes);
			stmt.setInt(25, classIndex);
			stmt.setString(26, dataValue);
			stmt.setInt(27, dataValueIndex);
			stmt.setInt(28, typeIndex);
			stmt.setString(29, href);
			stmt.setInt(30, hrefIndex);
			stmt.executeUpdate();

			int tcid = getTestCaseFromTestStep(test_step);

			stmt = testCon.prepareStatement(testcase);
			stmt.setTimestamp(1, Utilities.getCurrentTimestamp());
			stmt.setInt(2, tcid);
			stmt.executeUpdate();

			stmt.close();

		} catch (Exception e) {
			System.err.print(Utilities.getNow());
			System.err.println(test_step + "--" + randomKey + "--" + imgpath + "--" + bgcolor + "--" + color + "--"
					+ url + "--" + uniqueText + "--" + uniqueToParent + "--" + parenttotarget + "--" + pagename + "--"
					+ pageno + "--" + isShadow + "--" + shadowEle + "--" + shadowIndex + "--" + shadowPath + "--"
					+ uniqueisBackup);
			System.err.println(Utilities.getNow());
			e.printStackTrace();
		}

		// TO get the test steps and store it to the cache
		// userDetails = Utilities.getUserFromCache(String.valueOf(randomKey)); //
		// Commented by sanjeev, As feel of no use
		String uname = (String) userDetails.get("uname");
		int companyid = (int) userDetails.get("companyid");
		// JSONObject latestTestSteps = this.getLatestRecording(uname);
		Utilities.callApi(AppProperties.javareactinternalurl, "updateRecordingCache", uname, null, companyid);
	}

	public int getTestCaseIdFromKey(long randomKey) {
		String sql = "SELECT idtest_case FROM cta.test_case WHERE extKey = ? " + "order by Created_Date Desc Limit 1";
		PreparedStatement stmt = null;
		int test_case = -1;
		try {

			stmt = testCon.prepareStatement(sql);

			// Set the values for the prepared statement
			stmt.setLong(1, randomKey);
			ResultSet resultSet = stmt.executeQuery();

			if (resultSet.next()) {
				test_case = resultSet.getInt("idtest_case");
			}
			resultSet.close();
			stmt.close();
		} catch (Exception e) {
			System.err.println(Utilities.getNow());
			e.printStackTrace();
		}

		return test_case;
	}

	public int getLastStepForTestCase(int test_case) {
		String sql = "SELECT * FROM cta.test_step WHERE Test_Case_Id = ?" + " ORDER BY tsSequence DESC LIMIT 1";
		PreparedStatement stmt = null;
		int testStepId = -1;
		try {
			stmt = testCon.prepareStatement(sql);
			stmt.setInt(1, test_case);

			ResultSet resultSet = stmt.executeQuery();
			if (resultSet.next()) {
				testStepId = resultSet.getInt("idtest_step");
			}

			resultSet.close();
			stmt.close();
		} catch (Exception e) {
			System.err.println(Utilities.getNow());
			e.printStackTrace();
		}

		return testStepId;
	}

	public void writeApiDataToDB(String request, String responseBody, long randomKey, long actualTime, int pagenum) {

		System.err.println(actualTime);

		int testCaseId = getTestCaseIdFromKey(randomKey);
		// int testStepId = getLastStepForTestCase(testCaseId);
		int modid = getModuleFromTestcase(testCaseId);
		int prodid = getProductsFromModule(modid);

		int companyid = (int) userDetails.get("companyid");

		String folder = Utilities.getApiDataPath(companyid, testCaseId);
		String randomPair = Utilities.randomGen("CHAR", 6, false);
		String responseFilePath = folder + "\\response_" + randomPair + ".txt";
		String requestFilePath = folder + "\\request_" + randomPair + ".txt";

		Utilities.writeDataToFile(responseFilePath, responseBody);
		Utilities.writeDataToFile(requestFilePath, request);

		String sql = "INSERT into cta.apidata (testcase, extKey, randomString,"
				+ "actualtime, createddate, url) values (?, ?, ?, ?, ?, ?)";
		PreparedStatement stmt = null;
		int apiid = -1;
		try {
			stmt = testCon.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);

			stmt.setInt(1, testCaseId);
			stmt.setLong(2, randomKey);
			stmt.setString(3, randomPair);
			stmt.setLong(4, actualTime);
			stmt.setTimestamp(5, Utilities.getCurrentTimestamp());
			stmt.setString(6, request);
			stmt.executeUpdate();

			ResultSet generatedKeys = stmt.getGeneratedKeys();
			if (generatedKeys.next()) {
				apiid = (generatedKeys.getBigDecimal(1)).intValue();
			}

			sql = "INSERT INTO cta.page_testcase_api (apiid, testcase, pagenumber, prodid) VALUES (?,?,?,?)";
			stmt = testCon.prepareStatement(sql);
			stmt.setInt(1, apiid);
			stmt.setInt(2, testCaseId);
			stmt.setInt(3, pagenum);
			stmt.setInt(4, prodid);
			stmt.executeUpdate();

			stmt.close();
		} catch (Exception e) {
			System.err.println(Utilities.getNow());
			System.err.println(sql);
			e.printStackTrace();
		}

	}

	public String getGetTypeForCompany(int companyId) {
		String sql = "SELECT gentype FROM cta.company where idcompany = ? ";
		PreparedStatement stmt = null;
		String gentype = "recording";

		try {
			stmt = testCon.prepareStatement(sql);
			stmt.setInt(1, companyId);
			ResultSet resultSet = stmt.executeQuery();
			if (resultSet.next()) {
				gentype = resultSet.getString(1);
			}
			resultSet.close();
			stmt.close();
		} catch (Exception e) {
			System.err.println(Utilities.getNow());
			e.printStackTrace();
		}

		return gentype;
	}

	public String generateMultipleLocators(int companyId) {
		String sql = "SELECT generate_multiple_locators FROM cta.company where idcompany = ? ";
		PreparedStatement stmt = null;
		String generateMultipleLocators = "false";

		try {
			stmt = testCon.prepareStatement(sql);
			stmt.setInt(1, companyId);
			ResultSet resultSet = stmt.executeQuery();
			if (resultSet.next()) {
				generateMultipleLocators = resultSet.getString(1);
			}
			resultSet.close();
			stmt.close();
		} catch (Exception e) {
			System.err.println(Utilities.getNow());
			e.printStackTrace();
		}

		return generateMultipleLocators;
	}

	public int writeTestStepToDB(boolean merging, int test_case, int step_number, String page_name, String page_desc,
			String xpath, String keyword, String action, String flow, String testdata, String varname, String ValDevice,
			long randomKey, double eventTime, String filename, String fileField, String DBUrl, int apiid,
			String apiparam, String DBQuery, String DBType, int clickable, String issues, long actualTime,
			String eventName, String iframexpath, String altpath, String outerhtml, String manuallyAdded, long tabid,
			String type, boolean isShadow, String shadowPath, int tsSequence, long windowid) {

		String sql = "SELECT idtest_case FROM cta.test_case WHERE extKey = ? " + "order by Created_Date Desc Limit 1";
		PreparedStatement stmt = null;
		int pk = -1;

		try {

			stmt = testCon.prepareStatement(sql);

			// Set the values for the prepared statement
			stmt.setLong(1, randomKey);
			ResultSet resultSet = stmt.executeQuery();

			if (resultSet.next()) {
				test_case = resultSet.getInt("idtest_case");

				if (!merging) {
					sql = "SELECT * FROM cta.test_step WHERE Test_Case_Id = ?" + " ORDER BY tsSequence DESC LIMIT 1";
					stmt = testCon.prepareStatement(sql);
					stmt.setInt(1, test_case);
				} else {
					sql = "SELECT * FROM cta.test_step WHERE Test_Case_Id = ? AND "
							+ " Step_Number < ? ORDER BY tsSequence DESC LIMIT 1";
					stmt = testCon.prepareStatement(sql);
					stmt.setInt(1, test_case);
					stmt.setInt(2, step_number);
				}
				resultSet = stmt.executeQuery();
				if (!resultSet.next()) {
					pk = insertIntoTestSteps(test_case, 1, page_name, page_desc, xpath, keyword, action, flow, testdata,
							varname, ValDevice, randomKey, eventTime, filename, fileField, DBUrl, apiid, apiparam,
							DBQuery, DBType, clickable, issues, actualTime, eventName, iframexpath, altpath, outerhtml,
							manuallyAdded, tabid, type, tsSequence, windowid);
				} else {
					JSONObject prevRow = getTestStepsFromDb(resultSet, true, false);

					if (((!isShadow && prevRow.get("Object_Xpath") != null && prevRow.get("Object_Xpath").equals(xpath))
							|| isShadow && prevRow.get("Object_Xpath").equals(xpath)
									&& prevRow.get("shadowpath") != null
									&& prevRow.get("shadowpath").equals(shadowPath))
							&&

							(keyword.equals("EditBox") || keyword.equals("TextArea"))
							&& (((String) prevRow.get("Keyword")).equals("EditBox")
									|| ((String) prevRow.get("Keyword")).equals("TextArea"))
							&& ((action.equals("Clear & Enter") && ((String) prevRow.get("Action")).equals("Clear & Enter"))
									|| (action.equals("Enter") && ((String) prevRow.get("Action")).equals("Enter")) )) {
						sql = "UPDATE cta.test_step SET TestData = ?, RecordedData = ? WHERE " + "idtest_step = ?";
						stmt = testCon.prepareStatement(sql);
						stmt.setString(1, testdata);
						stmt.setString(2, testdata);
						stmt.setInt(3, (int) prevRow.get("idtest_step"));
						stmt.executeUpdate();
					} else {
						int stepnum = (int) prevRow.get("Step_Number") + 1;
						if (merging) {
							stepnum = step_number;
						}

						if (keyword != null && keyword.equalsIgnoreCase("File") && action != null
								&& action.equalsIgnoreCase("Download")) {
							if (windowid == -1) {
								Integer windowidl = (Integer) prevRow.get("windowid");
								windowid = windowidl.intValue();
							}
							if (tabid == -1) {
								Integer tabidl = (Integer) prevRow.get("tabid");
								tabid = tabidl.intValue();
							}
						}
						pk = insertIntoTestSteps(test_case, stepnum, page_name, page_desc, xpath, keyword, action, flow,
								testdata, varname, ValDevice, randomKey, eventTime, filename, fileField, DBUrl, apiid,
								apiparam, DBQuery, DBType, clickable, issues, actualTime, eventName, iframexpath,
								altpath, outerhtml, manuallyAdded, tabid, type, tsSequence, windowid);
					}
				}
			} else {
				throw new TestStepBeforeTitleException("Test Step failed. Adding failed step back to the queue");
			}
			resultSet.close();
			stmt.close();
			updateTestCaseSSStatus(test_case, "NULL");
		} catch (TestStepBeforeTitleException e) {
			throw e;
		} catch (Exception e) {
			System.err.println(Utilities.getNow());
			System.err.println(merging + "--" + test_case + "--" + step_number + "--" + page_name + "--" + page_desc
					+ "--" + xpath + "--" + keyword + "--" + action + "--" + flow + "--" + testdata + "--" + varname
					+ "--" + ValDevice + "--" + randomKey + "--" + eventTime + "--" + filename + "--" + fileField + "--"
					+ DBUrl + "--" + apiid + "--" + apiparam + "--" + DBQuery + "--" + DBType + "--" + clickable + "--"
					+ issues + "--" + actualTime + "--" + eventName + "--" + iframexpath + "--" + altpath + "--"
					+ outerhtml + "--" + manuallyAdded + "--" + tabid + "--" + type + "--" + isShadow + "--"
					+ shadowPath);
			e.printStackTrace();
		} finally {

		}

		return pk;
	}

	/*
	 * Moved this check from DB to code as we need the xpath to be expandable and
	 * this was causing issues with unique key li,it of 3072 chars
	 */
	public boolean checkForUniqueTestStep(int test_case, String xpath, String keyword, String action,
			double eventTime) {
		String sql = "select * from cta.test_step where Test_Case_Id = ? AND "
				+ "Object_Xpath = ? AND keyword = ? AND action = ? AND eventTime = ? AND manuallyAdded <> 'true'";

		try {
			PreparedStatement stmt = testCon.prepareStatement(sql);

			stmt.setInt(1, test_case);
			stmt.setString(2, xpath);
			stmt.setString(3, keyword);
			stmt.setString(4, action);
			stmt.setDouble(5, eventTime);

			ResultSet rs = stmt.executeQuery();

			if (rs.next()) {
				return false;
			}
			rs.close();
			stmt.close();
			return true;

		} catch (Exception e) {
			System.err.println(Utilities.getNow());
			e.printStackTrace();
		}

		return false;
	}

	public int insertIntoTestSteps(int test_case, int step_number, String page_name, String page_desc, String xpath,
			String keyword, String action, String flow, String testdata, String varname, String ValDevice,
			long randomKey, double eventTime, String filename, String fileField, String DBUrl, int apiid,
			String apiparam, String DBQuery, String DBType, int clickable, String issues, long actualTime,
			String eventName, String iframexpath, String altpath, String outerhtml, String manuallyAdded, long tabid,
			String type, int tsSequence, long windowid) {

		boolean noDup = checkForUniqueTestStep(test_case, xpath, keyword, action, eventTime);

		int pk = -1;
		if (noDup) {
			String subAction = null;
			if (keyword != null && keyword.equalsIgnoreCase("Assertion") && action != null
					&& action.equalsIgnoreCase("validatePartial")) {
				subAction = "validatePartial";
			}
			String sql = "INSERT INTO cta.test_step (Test_Case_Id, " + "Step_Number, Page_Name, Page_Description, "
					+ "Object_Xpath, Keyword, Action, Flow, TestData,"
					+ " VarName, ValDevice,extKey,eventTime, RecordedData,"
					+ "filename, fileField, DBUrl, apiid, apiparam, DBQuery, "
					+ "status, DBType, clickable, Issues, actualtime, eventname, "
					+ "iframexpath, altpath, outerhtml, manuallyAdded, tabid, type,"
					+ "createddate, tsSequence, windowid, subAction) " + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?,"
					+ "?, ?, ?, ?,?,?,?,?,?,?," + "?,?, ?, ?, ?, ?, ?, ?, ?, ?, " + "?, ?, ?, ?, ?,?)";
			try {
				PreparedStatement stmt = testCon.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);

				// Set the values for the prepared statement
				stmt.setInt(1, test_case);
				stmt.setInt(2, step_number);
				stmt.setString(3, page_name);
				stmt.setString(4, page_desc);
				stmt.setString(5, xpath);
				stmt.setString(6, keyword);
				stmt.setString(7, action);
				stmt.setString(8, flow);
				if (testdata != null && testdata.length() > 1000) {
					testdata = testdata.substring(0, 999);
				}
				stmt.setString(9, testdata);
				stmt.setString(10, varname);
				stmt.setString(11, ValDevice);
				stmt.setLong(12, randomKey);
				stmt.setDouble(13, eventTime);
				stmt.setString(14, testdata);
				stmt.setString(15, filename);
				stmt.setString(16, fileField);
				stmt.setString(17, DBUrl);
				stmt.setInt(18, apiid);
				stmt.setString(19, apiparam);
				stmt.setString(20, DBQuery);
				stmt.setInt(21, 0);
				stmt.setString(22, DBType);
				stmt.setInt(23, clickable);
				stmt.setString(24, issues);
				stmt.setLong(25, actualTime);
				stmt.setString(26, eventName);
				stmt.setString(27, iframexpath);
				stmt.setString(28, altpath);
				if (outerhtml != null && outerhtml.length() > 4000) {
					outerhtml = outerhtml.substring(0, 3999);
				}
				stmt.setString(29, outerhtml);
				stmt.setString(30, manuallyAdded);
				stmt.setLong(31, tabid);
				stmt.setString(32, type);
				stmt.setTimestamp(33, Utilities.getCurrentTimestamp());
				stmt.setInt(34, tsSequence);
				stmt.setLong(35, windowid);
				stmt.setString(36, subAction);
				stmt.executeUpdate();
				ResultSet generatedKeys = stmt.getGeneratedKeys();
				if (generatedKeys.next()) {
					pk = (generatedKeys.getBigDecimal(1)).intValue();
				}

				generatedKeys.close();
				stmt.close();

			} catch (Exception e) {
				if (!e.getMessage().contains("Duplicate entry ") && !e.getMessage().contains("test_step.uniquestep")) {
					System.err.println(Utilities.getNow());
					e.printStackTrace();
				}
			}
		}
//		DashBoardData dbd = new DashBoardData(this);
//		dbd.updateDashBoardForTestStepCount(pk);
		return pk;
	}
	
	public void writeNlpStepAttributesToDB(int test_step, String nlpAction, String nlpElement, String nlpTestData, String nlpCloseBy) {
	    String sql = "INSERT INTO cta.test_step_attr (test_step, is_nlp, nlp_action, nlp_element, nlp_testData, nlp_closeBy) "
	               + "VALUES (?, ?, ?, ?, ?, ?)";

	    try {
	        PreparedStatement stmt = testCon.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
	        stmt.setInt(1, test_step);
	        stmt.setString(2, "true");
	        stmt.setString(3, nlpAction);
	        stmt.setString(4, nlpElement);
	        stmt.setString(5, nlpTestData);
	        stmt.setString(6, nlpCloseBy);
	        stmt.executeUpdate();
	        stmt.close();
	    } catch (Exception e) {
	        System.err.println("Error inserting NLP attributes for test_step: " + test_step);
	        e.printStackTrace();
	    }
	}

	public int getCompanyFromModule(int idmodules) {
		String sql = "SELECT company FROM cta.products t2 " + "JOIN cta.modules t1 ON t2.idproducts = t1.product "
				+ "WHERE t1.idmodules = ?";
		int companyId = -1;
		try {
			PreparedStatement stmt = testCon.prepareStatement(sql);
			stmt.setInt(1, idmodules);
			ResultSet rs = stmt.executeQuery();
			if (rs.next()) {
				companyId = rs.getInt("company");
			}

			rs.close();
			stmt.close();
		} catch (Exception e) {
			System.err.println(Utilities.getNow());
			e.printStackTrace();
		}

		return companyId;
	}

	public int getCompanyFromUser(String uname) {
		String sql = "SELECT company FROM cta.userprofile " + "WHERE username = ?";
		int companyId = -1;
		try {
			PreparedStatement stmt = testCon.prepareStatement(sql);
			stmt.setString(1, uname);
			ResultSet rs = stmt.executeQuery();
			if (rs.next()) {
				companyId = rs.getInt("company");
			}

			rs.close();
			stmt.close();
		} catch (Exception e) {
			System.err.println(Utilities.getNow());
			e.printStackTrace();
		}

		return companyId;
	}

	public JSONObject getDashboardData(int companyId, long lastExecuted) {

		JSONObject jsonArr = null;
		if (lastExecuted == -1) {
			jsonArr = this.getDashBoardForCompany(companyId);

			if (jsonArr != null && jsonArr.size() > 0) {
				return jsonArr;
			}
//			lastExecuted = 1672511400000L;
			lastExecuted = System.currentTimeMillis();
		}

		String sql = "SELECT COUNT(DISTINCT Test_Case) FROM cta.test_case ";
		sql = "SELECT count(*) FROM cta.test_case t4 " + "JOIN cta.modules t3 ON t4.Module = t3.idmodules "
				+ "JOIN cta.products t2 ON t3.product = t2.idproducts "
				+ "JOIN cta.company t1 ON t2.company = t1.idcompany " + "WHERE t4.Created_Date > ? and t1.idcompany = "
				+ companyId + " and t4.Status = 0";
		int testCaseCount = 0;
		int testStepsCount = 0;
		int stepsPassCount = 0;
		double totalDuration = 0;
		int testSuiteCount = 0;
		int suitesPassCount = 0;
		int totalSuiteRuns = 0;
		try {
			PreparedStatement stmt = testCon.prepareStatement(sql);
			stmt.setTimestamp(1, Utilities.getTimestamp(lastExecuted));
			ResultSet resultSet = stmt.executeQuery();
			resultSet.next();
			testCaseCount = resultSet.getInt(1);

			sql = " SELECT count(*) " + "From cta.test_step_result t6 "
					+ "JOIN cta.test_step t5 on t6.Test_Step = t5.idtest_step "
					+ "JOIN cta.test_case t4 on t5.Test_Case_Id = t4.idtest_case "
					+ "JOIN cta.modules t3 ON t4.Module = t3.idmodules "
					+ "JOIN cta.products t2 ON t3.product = t2.idproducts "
					+ "JOIN cta.company t1 ON t2.company = t1.idcompany "
					+ "WHERE t6.Executed_Date > ? and t1.idcompany = ? and " + "t5.createddate > ? and "
					+ "t4.Created_Date > ?";
			stmt = testCon.prepareStatement(sql);
			stmt.setTimestamp(1, Utilities.getTimestamp(lastExecuted));
			stmt.setInt(2, companyId);
			stmt.setTimestamp(3, Utilities.getTimestamp(lastExecuted));
			stmt.setTimestamp(4, Utilities.getTimestamp(lastExecuted));
			resultSet = stmt.executeQuery();
			resultSet.next();
			testStepsCount = resultSet.getInt(1);

			sql = " SELECT count(*), sum(Duration) " + "From cta.test_step_result t6 "
					+ "JOIN cta.test_step t5 on t6.Test_Step = t5.idtest_step "
					+ "JOIN cta.test_case t4 on t5.Test_Case_Id = t4.idtest_case "
					+ "JOIN cta.modules t3 ON t4.Module = t3.idmodules "
					+ "JOIN cta.products t2 ON t3.product = t2.idproducts "
					+ "JOIN cta.company t1 ON t2.company = t1.idcompany "
					+ "WHERE t6.Executed_Date >  ? AND t1.idcompany = ? AND t6.Status='PASS'";
			stmt = testCon.prepareStatement(sql);
			stmt.setTimestamp(1, Utilities.getTimestamp(lastExecuted));
			stmt.setInt(2, companyId);
			resultSet = stmt.executeQuery();
			resultSet.next();
			stepsPassCount = resultSet.getInt(1);
			totalDuration = resultSet.getDouble(2);

			sql = " SELECT count(*) " + "From cta.test_suite t3 "
					+ "JOIN cta.products t2 ON t3.productid = t2.idproducts "
					+ "JOIN cta.company t1 ON t2.company = t1.idcompany "
					+ "WHERE t3.Created_Date > ? AND t1.idcompany = ? AND t3.status= 0";
			stmt = testCon.prepareStatement(sql);
			stmt.setTimestamp(1, Utilities.getTimestamp(lastExecuted));
			stmt.setInt(2, companyId);
			resultSet = stmt.executeQuery();
			resultSet.next();
			testSuiteCount = resultSet.getInt(1);

			sql = "SELECT COUNT(*) as TotalRecords, SUM(CASE WHEN t4.Status='PASS' THEN 1 ELSE 0 END) as TotalPassRecords "
					+ "FROM cta.test_suite_results t4 "
					+ "JOIN cta.test_suite t3 ON t4.Test_Suite_Id = t3.idtest_suite "
					+ "JOIN cta.products t2 ON t3.productid = t2.idproducts "
					+ "JOIN cta.company t1 ON t2.company = t1.idcompany "
					+ "WHERE t4.Executed_Date > ? AND t1.idcompany = ?;";
			stmt = testCon.prepareStatement(sql);
			stmt.setTimestamp(1, Utilities.getTimestamp(lastExecuted));
			stmt.setInt(2, companyId);
			resultSet = stmt.executeQuery();
			resultSet.next();
			suitesPassCount = resultSet.getInt(2);
			totalSuiteRuns = resultSet.getInt(1);

			resultSet.close();
			stmt.close();

		} catch (Exception e) {
			System.err.println(Utilities.getNow());
			e.printStackTrace();
		}

		jsonArr = new JSONObject();
		jsonArr.put("testCaseCount", testCaseCount);
		jsonArr.put("testStepsCount", testStepsCount);
		jsonArr.put("stepsPassCount", stepsPassCount);
		jsonArr.put("totalDuration", totalDuration);
		jsonArr.put("testSuiteCount", testSuiteCount);
		jsonArr.put("totalSuiteRuns", totalSuiteRuns);
		jsonArr.put("suitesPassCount", suitesPassCount);

		jsonArr = updateDashboardData(jsonArr, companyId, testCaseCount, testStepsCount, stepsPassCount, totalDuration,
				testSuiteCount, totalSuiteRuns, suitesPassCount);
		return jsonArr;
	}

	public JSONObject updateDashboardData(JSONObject jsonArr, int companyid, int testCaseCount, int testStepsCount,
			int stepsPassCount, double totalDuration, int testSuiteCount, int totalSuiteRuns, int suitesPassCount) {
		String insert_sql = "INSERT into cta.dashboard (companyid, testCaseCount, testStepsCount, "
				+ "stepsPassCount, totalDuration, testSuiteCount, totalSuiteRuns,"
				+ "suitesPassCount, lastexecuted) values " + "(?, ?, ?, ?, ?, ?, ?, ?, ?)";

		String update_sql = "UPDATE cta.dashboard set " + "testCaseCount = testCaseCount + ?, "
				+ "testStepsCount = testStepsCount + ?, " + "stepsPassCount = stepsPassCount + ?, "
				+ "totalDuration = totalDuration + ?, " + "testSuiteCount = testSuiteCount + ?, "
				+ "totalSuiteRuns = totalSuiteRuns + ?, " + "suitesPassCount = suitesPassCount + ?, "
				+ "lastexecuted = ? where companyid = ?";

		String select_sql = "select iddashboard from cta.dashboard where companyid = ?";

		try {
			PreparedStatement stmt = testCon.prepareStatement(select_sql);
			stmt.setInt(1, companyid);
			ResultSet rs = stmt.executeQuery();
			if (rs.next()) {
				stmt = testCon.prepareStatement(update_sql);
				stmt.setInt(1, testCaseCount);
				stmt.setInt(2, testStepsCount);
				stmt.setInt(3, stepsPassCount);
				stmt.setDouble(4, totalDuration);
				stmt.setInt(5, testSuiteCount);
				stmt.setInt(6, totalSuiteRuns);
				stmt.setInt(7, suitesPassCount);
				stmt.setTimestamp(8, Utilities.getCurrentTimestamp());
				stmt.setInt(9, companyid);

				stmt.executeUpdate();
			} else {
				stmt = testCon.prepareStatement(insert_sql);
				stmt.setInt(1, companyid);
				stmt.setInt(2, testCaseCount);
				stmt.setInt(3, testStepsCount);
				stmt.setInt(4, stepsPassCount);
				stmt.setDouble(5, totalDuration);
				stmt.setInt(6, testSuiteCount);
				stmt.setInt(7, totalSuiteRuns);
				stmt.setInt(8, suitesPassCount);
				stmt.setTimestamp(9, Utilities.getCurrentTimestamp());
				stmt.executeUpdate();
			}

			String get_sql = "SELECT * from cta.dashboard where companyid = ?";
			stmt = testCon.prepareStatement(get_sql);
			stmt.setInt(1, companyid);
			rs = stmt.executeQuery();
			if (rs.next()) {
				jsonArr.put("testCaseCount", rs.getInt("testCaseCount"));
				jsonArr.put("testStepsCount", rs.getInt("testStepsCount"));
				jsonArr.put("stepsPassCount", rs.getInt("stepsPassCount"));
				jsonArr.put("totalDuration", rs.getDouble("totalDuration"));
				jsonArr.put("testSuiteCount", rs.getInt("testSuiteCount"));
				jsonArr.put("totalSuiteRuns", rs.getInt("totalSuiteRuns"));
				jsonArr.put("suitesPassCount", rs.getInt("suitesPassCount"));
			}

			rs.close();
			stmt.close();
		} catch (Exception e) {
			System.err.println(Utilities.getNow());
			e.printStackTrace();
		}

		return jsonArr;
	}

	public JSONObject getDashBoardForCompany(int companyid) {

		try {
			String get_sql = "SELECT * from cta.dashboard where companyid = ?";
			PreparedStatement stmt = testCon.prepareStatement(get_sql);
			stmt.setInt(1, companyid);
			ResultSet rs = stmt.executeQuery();

			JSONObject jsonArr = new JSONObject();
			if (rs.next()) {
				jsonArr.put("testCaseCount", rs.getInt("testCaseCount"));
				jsonArr.put("testStepsCount", rs.getInt("testStepsCount"));
				jsonArr.put("stepsPassCount", rs.getInt("stepsPassCount"));
				jsonArr.put("totalDuration", rs.getDouble("totalDuration"));
				jsonArr.put("testSuiteCount", rs.getInt("testSuiteCount"));
				jsonArr.put("totalSuiteRuns", rs.getInt("totalSuiteRuns"));
				jsonArr.put("suitesPassCount", rs.getInt("suitesPassCount"));
			}

			rs.close();
			stmt.close();
			return jsonArr;
		} catch (Exception e) {
			System.err.println(Utilities.getNow());
			e.printStackTrace();
		}

		return null;
	}

	public JSONObject getLatestRecording(String username) {

		String sql = "SELECT * FROM cta.test_case " + "WHERE Created_By = ? AND SaveType = 'Recording' AND "
				+ "Status != 1 ORDER BY Created_Date DESC LIMIT 1";
		JSONObject jsonArr = null;
		String prod_sql = "SELECT product FROM cta.modules WHERE idmodules = ?";
		try {
			PreparedStatement stmt = testCon.prepareStatement(sql);
			stmt.setString(1, username);
			ResultSet resultSet = stmt.executeQuery();

			if (resultSet.next()) {
				JSONObject tcjson = getTestCaseAsJSON(resultSet, false);
				int testCaseId = (int) tcjson.get("idtest_case");
				int Module = (int) tcjson.get("Module");
				stmt = testCon.prepareStatement(prod_sql);
				stmt.setInt(1, Module);
				ResultSet rs = stmt.executeQuery();
				int productid = 0;
				if (rs.next()) {
					productid = (int) rs.getInt(1);
				}
				sql = "SELECT * FROM cta.test_step WHERE " + "Test_Case_Id = ? AND (status = 0 OR status = NULL)"
						+ "ORDER BY tsSequence, eventTime ASC";
				stmt = testCon.prepareStatement(sql);
				stmt.setInt(1, testCaseId);
				ResultSet resultSet2 = stmt.executeQuery();

				jsonArr = getExecutableTestStepsAsJSON(resultSet2, false, false, false,false);
				List<Map.Entry<String, Integer>> tabObj = new ArrayList<>();

				int tabIndex = 1;
				JSONArray jsonArray = new JSONArray();

				JSONObject stepData = (JSONObject) jsonArr.get("data");
				JSONObject firstTabObj = (JSONObject) stepData.get(2); // Assuming tabId is present in the first step
				if (firstTabObj == null) {
				} else {
					int firstTabId = Integer.parseInt(firstTabObj.get("tabid").toString());
					String firstTabKey = "Tab " + tabIndex;
					tabObj.add(new AbstractMap.SimpleEntry<>(firstTabKey, firstTabId));
					tabIndex++;

					for (int i = 3; i < stepData.size(); i++) {
						JSONObject obj = (JSONObject) stepData.get(i);
						int tabId = Integer.parseInt(obj.get("tabid").toString());
						if (!tabObj.stream().anyMatch(entry -> entry.getValue().equals(tabId))) {
							String tabKey = "Tab " + tabIndex;
							tabObj.add(1, new AbstractMap.SimpleEntry<>(tabKey, tabId));
							tabIndex++;
						}
					}

					for (Map.Entry<String, Integer> entry : tabObj) {
						JSONObject entryJson = new JSONObject();
						entryJson.put("tabName", entry.getKey());
						entryJson.put("tabId", entry.getValue());
						jsonArray.add(entryJson);
					}
				}
				jsonArr.put("testcase", tcjson);
				jsonArr.put("tabs", jsonArray);
				JSONArray env = getEnvDetailsForModule((int) tcjson.get("Module"));
				jsonArr.put("envDetails", env);
				String ModuleName = getModuleName(Module);
				jsonArr.put("Modules", Module);
				String ProdName = getModuleName(productid);
				jsonArr.put("Product", productid);
				String TCName = getTCName(testCaseId);
				jsonArr.put("Testcase", testCaseId);
				resultSet2.close();
			}
			resultSet.close();
			stmt.close();
		} catch (Exception e) {
			System.err.println(Utilities.getNow());
			e.printStackTrace();
		}

		return jsonArr;

	}

	public JSONArray getEnvDetailsForModule(int moduleId) {

		String sql = "SELECT * FROM cta.envdetails t2 " + "JOIN cta.modules t1 ON t2.productid = t1.product "
				+ "WHERE t1.idmodules = ?";
		JSONArray jsonArr = new JSONArray();
		try {
			PreparedStatement stmt = testCon.prepareStatement(sql);
			stmt.setInt(1, moduleId);
			ResultSet rs = stmt.executeQuery();
			jsonArr = getEnvDetailsAsJSON(rs);

			rs.close();
			stmt.close();
		} catch (Exception e) {
			System.err.println(Utilities.getNow());
			e.printStackTrace();
		}

		return jsonArr;
	}

	public JSONArray getEnvDetailsAsJSON(ResultSet rs) {
		JSONArray jsonArr = new JSONArray();
		try {
			while (rs.next()) {
				JSONObject jsono = new JSONObject();
				jsono.put("idenvdetails", rs.getInt("idenvdetails"));
				jsono.put("envname", rs.getString("envname"));
				jsono.put("envurl", rs.getString("envurl"));
				jsonArr.add(jsono);
			}
		} catch (Exception e) {
			System.err.println(Utilities.getNow());
			e.printStackTrace();
		}

		return jsonArr;
	}

	public JSONObject getLatestResults(int companyId) {
		String Executed_By = (String) userDetails.get("uname");
		String sql = "SELECT idtest_case_results, test_case_name, Test_Case_Id,t5.Status "
				+ "FROM cta.test_case_results t5 " + "join cta.test_case as tc on t5.Test_Case_Id = tc.idtest_case "
				+ "where Executed_By = ? and tc.Status = 0 " + "order by idtest_case_results desc LIMIT 1";

		String sql2 = "select Module , t2.product from cta.test_case t1 "
				+ "join cta.modules t2 on t2.idmodules = t1.Module " + "where idtest_case= ?";

		JSONObject jsonObj = null;

		try {
			PreparedStatement stmt = testCon.prepareStatement(sql);
			stmt.setString(1, Executed_By);
			ResultSet resultSet = stmt.executeQuery();

			while (resultSet.next()) {
				int testCaseResultId = resultSet.getInt(1);
				String tcName = resultSet.getString(2);
				int tcid = resultSet.getInt(3);
				PreparedStatement stmt3 = testCon.prepareStatement(sql2);
				stmt3.setInt(1, tcid);
				ResultSet resultSet3 = stmt3.executeQuery();
				int modid = -1;
				int prodid = -1;
				if (resultSet3.next()) {
					modid = resultSet3.getInt(1);
					prodid = resultSet3.getInt(2);
				}

				resultSet3.close();
				stmt3.close();

				String tcStatus = resultSet.getString(4);

				sql = "SELECT *, t5.status as tcstatus FROM cta.test_step_result tsr "
						+ "JOIN cta.test_case_results t5 on t5.idtest_case_results = tsr.Test_Case_Results_Id "
						+ "JOIN cta.test_case tc on tc.idtest_case = t5.Test_Case_Id "
						+ " WHERE tsr.Test_Case_Results_Id = ? ";

				stmt = testCon.prepareStatement(sql);
				stmt.setInt(1, testCaseResultId);
				ResultSet resultSet2 = stmt.executeQuery();

				jsonObj = getTestStepsResultsAsJSON(resultSet2, true, companyId, true, tcStatus);
				JSONObject testcases = (JSONObject) jsonObj.get("result");
				jsonObj.put("Product", prodid);
				jsonObj.put("Modules", modid);
				jsonObj.put("Testcase", tcid);
				JSONObject tcjson = getTestCaseDetailsFromId(tcid);
				jsonObj.put("testcase", tcjson);
				testcases.put("testcasename", tcName);
				String fileurl = AppProperties.fileurl + "?action=executionlog&token=" + randomkey + "&companyid="
						+ companyId + "&tcid=" + tcid + "&tcrid=" + testCaseResultId;
				testcases.put("logFile", fileurl);
				resultSet2.close();
			}
			resultSet.close();
			stmt.close();
		} catch (Exception e) {
			System.err.println(Utilities.getNow());
			e.printStackTrace();
		}

		return jsonObj;

	}

	public JSONObject getSearchByTCName(String testCase, int companyId) {

		String sql = " SELECT * " + "FROM cta.test_case t4 " + "JOIN cta.modules t3 ON t4.Module = t3.idmodules "
				+ "JOIN cta.products t2 ON t3.product = t2.idproducts "
				+ "JOIN cta.company t1 ON t2.company = t1.idcompany "
				+ "WHERE t1.idcompany = ? AND SaveType = 'Recording' AND " + "t4.Status != 1 AND Test_Case = ? "
				+ "ORDER BY Created_Date DESC LIMIT 1";
		JSONObject jsonArr = null;

		try {
			PreparedStatement stmt = testCon.prepareStatement(sql);
			stmt.setInt(1, companyId);
			stmt.setString(2, testCase);
			ResultSet resultSet = stmt.executeQuery();

			if (resultSet.next()) {
				JSONObject tcjson = getTestCaseAsJSON(resultSet, false);
				jsonArr = getTestStepsByTestCaseID((int) tcjson.get("idtest_case"), false);
				jsonArr.put("testcase", tcjson);

			}
			resultSet.close();
			stmt.close();
		} catch (Exception e) {
			System.err.println(Utilities.getNow());
			e.printStackTrace();
		}

		return jsonArr;

	}

	public JSONObject getSearchByTCId(int tcid, int companyId) {

		String sql = " SELECT * " + "FROM cta.test_case t4 " + "JOIN cta.modules t3 ON t4.Module = t3.idmodules "
				+ "JOIN cta.products t2 ON t3.product = t2.idproducts "
				+ "JOIN cta.company t1 ON t2.company = t1.idcompany "
				+ "WHERE t1.idcompany = ? AND SaveType = 'Recording' AND " + "t4.Status != 1 AND idtest_case = ? "
				+ "ORDER BY Created_Date DESC LIMIT 1";
		JSONObject jsonArr = null;

		try {
			PreparedStatement stmt = testCon.prepareStatement(sql);
			stmt.setInt(1, companyId);
			stmt.setInt(2, tcid);
			ResultSet resultSet = stmt.executeQuery();

			if (resultSet.next()) {
				jsonArr = getTestCaseAsJSON(resultSet, false);
			}
			resultSet.close();
			stmt.close();
		} catch (Exception e) {
			System.err.println(Utilities.getNow());
			e.printStackTrace();
		}

		return jsonArr;

	}

	public JSONObject getTestStepsByTestCaseID(int tcid, boolean getPwd) {

		String sql = "SELECT * FROM cta.test_step  WHERE "
				+ "Test_Case_Id = ? AND (cta.test_step.status = 0 OR cta.test_step.status = NULL)"
				+ "ORDER BY cta.test_step.tsSequence, cta.test_step.eventTime ASC";
		String prod_sql = "SELECT product FROM cta.modules WHERE idmodules = ?";
		ResultSet resultSet2 = null;
		JSONObject jsonObj = new JSONObject();
		try {
			PreparedStatement stmt = testCon.prepareStatement(sql);
			stmt.setInt(1, tcid);
			resultSet2 = stmt.executeQuery();
			jsonObj = getExecutableTestStepsAsJSON(resultSet2, getPwd, false, false, calledFromSpecificLocation);
			List<Map.Entry<String, Integer>> tabObj = new ArrayList<>();
			int tabIndex = 1;
			JSONArray jsonArray = new JSONArray();

			JSONObject stepData = (JSONObject) jsonObj.get("data");
			JSONObject firstTabObj = (JSONObject) stepData.get(2); // Assuming tabId is present in the first step
			if (firstTabObj == null) {
			} else {
				int firstTabId = Integer.parseInt(firstTabObj.get("tabid").toString());
				String firstTabKey = "Tab " + tabIndex;
				tabObj.add(new AbstractMap.SimpleEntry<>(firstTabKey, firstTabId));
				tabIndex++;

				for (int i = 3; i < stepData.size(); i++) {
					JSONObject obj = (JSONObject) stepData.get(i);
					int tabId = Integer.parseInt(obj.get("tabid").toString());
					if (!tabObj.stream().anyMatch(entry -> entry.getValue().equals(tabId))) {
						String tabKey = "Tab " + tabIndex;
						tabObj.add(1, new AbstractMap.SimpleEntry<>(tabKey, tabId));
						tabIndex++;
					}
				}

				for (Map.Entry<String, Integer> entry : tabObj) {
					JSONObject entryJson = new JSONObject();
					entryJson.put("tabName", entry.getKey());
					entryJson.put("tabId", entry.getValue());
					jsonArray.add(entryJson);
				}
			}

			JSONObject tcjson = getTestCaseDetailsFromId(tcid);
			jsonObj.put("tabs", jsonArray);
			jsonObj.put("testcase", tcjson);
			int Module = (int) tcjson.get("Module");
			JSONArray env = getEnvDetailsForModule(Module);
			jsonObj.put("envDetails", env);
			PreparedStatement stmt2 = testCon.prepareStatement(prod_sql);
			stmt2.setInt(1, Module);
			ResultSet rs = stmt2.executeQuery();
			int productid = 0;
			if (rs.next()) {
				productid = (int) rs.getInt(1);
			}

			String ModuleName = getModuleName(Module);
			jsonObj.put("Modules", Module);
			String ProdName = getModuleName(productid);
			jsonObj.put("Product", productid);
			String TCName = getTCName(tcid);
			jsonObj.put("Testcase", tcid);

			resultSet2.close();
			rs.close();
			stmt2.close();
			stmt.close();
		} catch (Exception e) {
			System.err.println(Utilities.getNow());
			e.printStackTrace();
		}
		return jsonObj;
	}

	public JSONObject getLatestResultsByTCId(int testCaseId, int companyId) {

		String sql = "SELECT idtest_case_results, Status FROM cta.test_case_results "
				+ "WHERE Test_Case_Id = ? ORDER BY idtest_case_results " + "DESC LIMIT 1";
		JSONObject jsonArr = null;

		try {
			PreparedStatement stmt = testCon.prepareStatement(sql);
			stmt.setInt(1, testCaseId);
			ResultSet resultSet = stmt.executeQuery();

			while (resultSet.next()) {
				int testCaseRId = resultSet.getInt(1);
				String tcStatus = resultSet.getString(2);

				sql = "SELECT * FROM cta.test_step_result WHERE " + "Test_Case_Results_Id = ? ";
				stmt = testCon.prepareStatement(sql);
				stmt.setInt(1, testCaseRId);
				ResultSet resultSet2 = stmt.executeQuery();

				jsonArr = getTestStepsResultsAsJSON(resultSet2, false, companyId, true, tcStatus);
				jsonArr.put("results", jsonArr.get("testcases"));
				resultSet2.close();
			}
			resultSet.close();
			stmt.close();
		} catch (Exception e) {
			System.err.println(Utilities.getNow());
			e.printStackTrace();
		}

		return jsonArr;

	}

	public void deleteTestStepsFromDB(int Test_Case_Id) {
		String sql = "UPDATE cta.test_case set Status = 1 where " + "idtest_case = ?";

		try {
			PreparedStatement stmt = testCon.prepareStatement(sql);

			// Set the values for the prepared statement
			stmt.setInt(1, Test_Case_Id);

			// Execute the prepared statement
			stmt.executeUpdate();
			stmt.close();

		} catch (Exception e) {
			System.err.println(Utilities.getNow());
			e.printStackTrace();
		}
	}

	public void markTestCaseAsCompleted(int Test_Case_Id) {
		String sql = "UPDATE cta.test_case set SaveType = 'COMPLETED' where idtest_case = ?";

		try {
			PreparedStatement stmt = testCon.prepareStatement(sql);

			// Set the values for the prepared statement
			stmt.setInt(1, Test_Case_Id);

			// Execute the prepared statement
			stmt.executeUpdate();
			stmt.close();

		} catch (Exception e) {
			System.err.println(Utilities.getNow());
			e.printStackTrace();
		}
	}

	public JSONObject getTestStepsFromDB(String test_case, boolean getPwd) {

		String sql = "SELECT idtest_case FROM cta.test_case WHERE Test_Case = ? ORDER BY "
				+ "Created_Date DESC LIMIT 1";
		JSONObject jsonArr = null;
		try {
			PreparedStatement stmt = testCon.prepareStatement(sql);

			// Set the values for the prepared statement
			stmt.setString(1, test_case);

			// Execute the prepared statement
			ResultSet resultSet = stmt.executeQuery();

			if (resultSet.next()) {
				int testCaseId = resultSet.getInt("idtest_case");
				jsonArr = getTestStepsFromDBByTestCaseId(testCaseId, getPwd, false);
			}
			resultSet.close();
			stmt.close();

		} catch (Exception e) {
			System.err.println(Utilities.getNow());
			e.printStackTrace();
		}

		return jsonArr;
	}

	public JSONObject getCompany(int companyid, String randomkey) {

		PreparedStatement stmt = null;
		ResultSet rs = null;
		try {
			String sql = "SELECT * FROM cta.company WHERE idcompany = ?";
			stmt = testCon.prepareStatement(sql);
			stmt.setInt(1, companyid);
			rs = stmt.executeQuery();
			if (rs.next()) {
				JSONObject company = getCompanyAsJSON(rs);
				JSONObject jsonObj = getBranding(companyid, randomkey);
				company.put("primaryColor", (String) jsonObj.get("primaryColor"));
				company.put("secondaryColor", (String) jsonObj.get("secondaryColor"));
				company.put("tertiaryColor", (String) jsonObj.get("tertiaryColor"));
				company.put("brandLogo", (String) jsonObj.get("brandLogo"));
				company.put("companyurl", (String) jsonObj.get("companyurl"));

				String logintype = (String) company.get("logintype");
				if (logintype != null && logintype.equals("codegen")) {
					int companyId = (int) company.get("idcompany");
					JSONObject license = PasswordUtils.getLicenseDetails(companyId, this, new MySqlConn2(this));
					company.put("licensedetails", license);
					int tccount = (int) license.get("tcCount");
					JSONObject plan = getCompanyPlan(companyId);
					plan.put("available_credits", tccount);
					company.put("plan", plan);
				}
				return company;
			}

			stmt.close();
			rs.close();
		} catch (Exception e) {
			System.err.println(Utilities.getNow());
			e.printStackTrace();
		} finally {
			try {
				rs.close();
				stmt.close();
			} catch (Exception e) {
				System.err.println(Utilities.getNow());
				e.printStackTrace();
			}
		}

		return null;
	}

	public JSONArray getUserDetails(String uname, String pwd) {

		String sql = "SELECT * FROM cta.userprofile WHERE username = ? AND" + " password = ? AND status = 0 ";
		PreparedStatement stmt = null;
		ResultSet rs = null;
		try {
			stmt = testCon.prepareStatement(sql);
			stmt.setString(1, uname);
			stmt.setString(2, pwd);
			rs = stmt.executeQuery();

			if (rs.next()) {
				return getSuccessfulLoginDetails(rs, uname);
			} else {
				int failedAttempts = incrementNoOfFailedAttempts(uname);
				int allowedAttempts = Integer.parseInt(AppProperties.nooffailedattemptsalowed);
				if (failedAttempts > allowedAttempts) {
					sql = "UPDATE cta.userprofile SET status = 2 WHERE username = ?";
					stmt = testCon.prepareStatement(sql);
					stmt.setString(1, uname);
					stmt.executeUpdate();
				}
				return null;
			}
		} catch (Exception e) {
			System.err.println(Utilities.getNow());
			e.printStackTrace();
		} finally {
			try {
				rs.close();
				stmt.close();
			} catch (Exception e) {
				System.err.println(Utilities.getNow());
				e.printStackTrace();
			}
		}

		return null;
	}

	public JSONArray refreshUserDetails(String uname) {

		String sql = "SELECT * FROM cta.userprofile WHERE username = ? " + "AND status = 0 ";
		try {
			PreparedStatement stmt = testCon.prepareStatement(sql);
			stmt.setString(1, uname);
			ResultSet rs = stmt.executeQuery();

			if (rs.next()) {
				return getSuccessfulLoginDetails(rs, uname);
			}

			rs.close();
			stmt.close();
		} catch (Exception e) {
			System.err.println(Utilities.getNow());
			e.printStackTrace();
		}

		return null;
	}

	public JSONArray getSuccessfulLoginDetails(ResultSet rs, String uname) throws Exception {
		int companyid = rs.getInt("company");
		int usertype = rs.getInt("usertype");
		int iduserprofile = rs.getInt("iduserprofile");

		String sql = "UPDATE cta.userprofile SET status = 0, nooffailedattempts = 0 WHERE username = ?";
		PreparedStatement stmt = testCon.prepareStatement(sql);
		stmt.setString(1, uname);
		stmt.executeUpdate();

		stmt.close();
		JSONArray res = getModulesForCompany(companyid);
		if (res != null && res.size() > 0) {
			JSONObject j1 = (JSONObject) res.get(0);
			j1.put("usertype", usertype);
			j1.put("username", uname);
			j1.put("iduserprofile", iduserprofile);
			j1.put("companyid", companyid);
		} else {
			JSONObject compObj = getCompany(companyid, sql);
			JSONObject j1 = new JSONObject();
			res = new JSONArray();
			j1.put("usertype", usertype);
			j1.put("username", uname);
			j1.put("iduserprofile", iduserprofile);
			j1.put("companyid", companyid);
			j1.put("company", companyid);
			res.add(0, j1);
		}

		stmt.close();
		return res;
	}

	public JSONArray getUserDetails(String uname, String pwd, String company) {
		try {

			String sql = "SELECT idcompany FROM cta.company WHERE companyname = ? AND" + " status = 0 ";
			PreparedStatement stmt = testCon.prepareStatement(sql);
			stmt.setString(1, company);
			ResultSet resultSet = stmt.executeQuery();

			if (resultSet.next()) {
				int compid = resultSet.getInt(1);
				sql = "SELECT * FROM cta.userprofile WHERE username = ? AND"
						+ " password = ? AND company = ? AND status = 0 ";
				stmt = testCon.prepareStatement(sql);
				stmt.setString(1, uname);
				stmt.setString(2, pwd);
				stmt.setInt(3, compid);
				resultSet = stmt.executeQuery();

				if (resultSet.next()) {
					int companyid = resultSet.getInt("company");
					int usertype = resultSet.getInt("usertype");
					int iduserprofile = resultSet.getInt("iduserprofile");

					JSONArray res = getModulesForCompany(companyid);
					if (res != null && res.size() > 0) {
						JSONObject j1 = (JSONObject) res.get(0);
						j1.put("usertype", usertype);
						j1.put("username", uname);
						j1.put("iduserprofile", iduserprofile);
					}
					return res;
				}

				resultSet.close();
				stmt.close();
			}

		} catch (Exception e) {
			System.err.println(Utilities.getNow());
			e.printStackTrace();
		}

		return null;
	}

	public JSONObject getUserDetails(String uname, int companyId) {
		try {

			String sql = "SELECT idcompany FROM cta.company WHERE idcompany = ? AND" + " status = 0 ";
			PreparedStatement stmt = testCon.prepareStatement(sql);
			stmt.setInt(1, companyId);
			ResultSet resultSet = stmt.executeQuery();

			if (resultSet.next()) {
				int compid = resultSet.getInt(1);
				sql = "SELECT * FROM cta.userprofile WHERE username = ? AND" + "  company = ? AND status = 0 ";
				stmt = testCon.prepareStatement(sql);
				stmt.setString(1, uname);
				stmt.setInt(2, compid);
				resultSet = stmt.executeQuery();

				if (resultSet.next()) {
					int companyid = resultSet.getInt("company");
					int usertype = resultSet.getInt("usertype");
					int iduserprofile = resultSet.getInt("iduserprofile");

					JSONObject j1 = new JSONObject();
					j1.put("usertype", usertype);
					j1.put("username", uname);
					j1.put("iduserprofile", iduserprofile);

					return j1;
				}

				resultSet.close();
				stmt.close();
			}

		} catch (Exception e) {
			System.err.println(Utilities.getNow());
			e.printStackTrace();
		}

		return null;
	}

	public JSONArray getModulesForCompany(int companyid) {
		String sql = "SELECT p.*" + "FROM cta.modules m " + "JOIN cta.products p ON m.product = p.idproducts "
				+ "JOIN cta.company c ON p.company = c.idcompany " + "WHERE c.idcompany =" + companyid
				+ " GROUP BY m.product";
		JSONArray result = new JSONArray();
		try {
			Statement stmt = testCon.createStatement();
			ResultSet rs = stmt.executeQuery(sql);
			JSONObject prodJson = new JSONObject();
			while (rs.next()) {
				JSONObject prod = getProductsAsJSON(rs, true);
				result.add(prod);
			}
			rs.close();
			stmt.close();

		} catch (Exception e) {
			System.err.println(Utilities.getNow());
			e.printStackTrace();
		}

		return result;
	}

	public JSONObject getProductsAsJSON(ResultSet rs, boolean getModules) {
		JSONObject result = new JSONObject();
		try {
			result.put("idproducts", rs.getInt("idproducts"));
			result.put("productname", rs.getString("productname"));
			result.put("company", rs.getInt("company"));
			if (getModules) {
				JSONArray modArr = getModulesFromProduct(rs.getInt("idproducts"));
				if (modArr == null) {
					modArr = new JSONArray();
				}
				result.put("modules", modArr);
			}
		} catch (Exception e) {
			System.err.println(Utilities.getNow());
			e.printStackTrace();
		}
		return result;
	}

	public JSONObject getModulesAsJSON(ResultSet rs) {
		JSONObject result = new JSONObject();
		try {
			result.put("idmodules", rs.getInt("idmodules"));
			result.put("modulename", rs.getString("modulename"));
			result.put("product", rs.getInt("product"));
		} catch (Exception e) {
			System.err.println(Utilities.getNow());
			e.printStackTrace();
		}

		return result;
	}

	public JSONObject checkModuleDisability(int modid, int usertype) {
		String sql = "SELECT permission FROM cta.modulepermission WHERE moduleid= ? AND roleid = ?";
		JSONObject json = new JSONObject();
		try {
			PreparedStatement stmt = testCon.prepareStatement(sql);
			stmt.setInt(1, modid);
			stmt.setInt(2, usertype);
			ResultSet rs = stmt.executeQuery();
			if (rs.next()) {
				String perm = rs.getString("permission");
				if (perm.equals("FullAccess")) {
					json.put("isdisable", 0);
				} else {
					json.put("isdisable", 1);
				}
			} else {
				json.put("isdisable", 0);
			}

			rs.close();
			stmt.close();
		} catch (Exception e) {
			System.err.println(Utilities.getNow());
			e.printStackTrace();
		}
		return json;
	}

	public void updateImageLocation(String testCase, long randomKey, double eventTime, String filename) {
		try {
			String sql = "SELECT idtest_case FROM cta.test_case WHERE Test_Case = ? AND" + " extKey = ? ";
			PreparedStatement stmt = testCon.prepareStatement(sql);
			stmt.setString(1, testCase);
			stmt.setLong(2, randomKey);
			ResultSet resultSet = stmt.executeQuery();

			if (resultSet.next()) {
				int testcaseid = resultSet.getInt(1);

				sql = "SELECT idtest_step FROM cta.test_step WHERE Test_Case_Id = ? AND"
						+ " extKey = ? AND eventTime = ?";
				stmt = testCon.prepareStatement(sql);
				stmt.setInt(1, testcaseid);
				stmt.setLong(2, randomKey);
				stmt.setDouble(3, eventTime);
				resultSet = stmt.executeQuery();

				if (resultSet.next()) {
					int teststepid = resultSet.getInt(1);

					sql = "UPDATE cta.test_step SET screenshot = ? WHERE idtest_step = ?";
					stmt = testCon.prepareStatement(sql);
					stmt.setString(1, filename);
					stmt.setInt(2, teststepid);
					stmt.executeUpdate();
				}
			}

			resultSet.close();
			stmt.close();

		} catch (Exception e) {
			System.err.println(Utilities.getNow());
			e.printStackTrace();
		}
	}

	public JSONObject getTestCaseDetailsFromId(int testCaseId) {

		JSONObject jsonArr = new JSONObject();
		try {
			String sql = "SELECT * FROM cta.test_case WHERE idtest_case = ? AND Status != 1";
			PreparedStatement stmt = testCon.prepareStatement(sql);
			stmt.setInt(1, testCaseId);
			ResultSet resultSet = stmt.executeQuery();
			while (resultSet.next()) {
				jsonArr = getTestCaseAsJSON(resultSet, false);
			}
			resultSet.close();
			stmt.close();

		} catch (Exception e) {
			System.err.println(Utilities.getNow());
			e.printStackTrace();
		}

		return jsonArr;
	}

	public JSONArray getTestStepsForTCIDAndAncestors(int testCaseId, JSONArray jsonArr) {

//		JSONArray jsonArr = new JSONArray();
		try {
			String sql = "SELECT DISTINCT dependantTC FROM cta.test_step WHERE dependantTC > 0 and Test_Case_Id = ?";

			PreparedStatement stmt = testCon.prepareStatement(sql);
			stmt.setInt(1, testCaseId);
			ResultSet resultSet = stmt.executeQuery();
			while (resultSet.next()) {
				jsonArr = getTestStepsForTCIDAndAncestors(resultSet.getInt(1), jsonArr);
				// jsonArr.add(resultSet.getInt(1));
			}
			jsonArr.add(testCaseId);
			resultSet.close();
			stmt.close();

		} catch (Exception e) {
			System.err.println(Utilities.getNow());
			e.printStackTrace();
		}

		return jsonArr;
	}

	public JSONObject getTestStepsFromDBByTestCaseId(int testCaseId, boolean getPwd, boolean withCreatedDate) {

		JSONObject jsonArr = null;
		try {
			String sql = "SELECT * FROM cta.test_step WHERE Test_Case_Id = ? AND" + " status = 0 "
					+ "ORDER BY tsSequence";
			PreparedStatement stmt = testCon.prepareStatement(sql);
			stmt.setInt(1, testCaseId);
			ResultSet resultSet = stmt.executeQuery();
			jsonArr = getExecutableTestStepsAsJSON(resultSet, getPwd, withCreatedDate, true,false);
			JSONObject resolution = getResolutionForTestCase(testCaseId);
			jsonArr.put("resolution", resolution);
			JSONObject tc = getTestCaseDetailsFromId(testCaseId);
			jsonArr.put("testcase", tc);
			resultSet.close();
			stmt.close();

		} catch (Exception e) {
			System.err.println(Utilities.getNow());
			e.printStackTrace();
		}

		return jsonArr;
	}

	public JSONObject getResolutionForTestCase(int testCaseId) {
		JSONObject jsonO = new JSONObject();
		try {
			String sql = "Select width, height from cta.test_case where idtest_case = ?";
			PreparedStatement stmt = testCon.prepareStatement(sql);
			stmt.setInt(1, testCaseId);
			ResultSet rs = stmt.executeQuery();

			if (rs.next()) {
				jsonO.put("width", rs.getInt("width"));
				jsonO.put("height", rs.getInt("height"));
			}

			rs.close();
			stmt.close();
		} catch (Exception e) {
			System.err.println(Utilities.getNow());
			e.printStackTrace();
		}
		return jsonO;
	}

	private JSONObject getTestStepsResultsAsJSON(ResultSet rs, boolean tcStatus, int companyId,
			boolean getTestCaseResults, String tcrStatus) {

		JSONObject JSONarr = new JSONObject();
		JSONObject testcases = new JSONObject();
		int index = 2;
		double durationSum = 0.0;
		try {
			while (rs.next()) {
				JSONObject jsonRow = new JSONObject();

				jsonRow.put("idtest_step_result", rs.getInt("idtest_step_result"));
				jsonRow.put("Test_Case_Results_Id", rs.getInt("Test_Case_Results_Id"));
				jsonRow.put("Step_Number", rs.getInt("Step_Number"));
				jsonRow.put("Page_Name", rs.getString("Page_Name"));
				jsonRow.put("Page_Description", rs.getString("Page_Description"));
				jsonRow.put("Object_Xpath", rs.getString("Object_Xpath"));
				jsonRow.put("Keyword", rs.getString("Keyword"));
				jsonRow.put("Action", rs.getString("Action"));
				jsonRow.put("Flow", rs.getString("Flow"));
				jsonRow.put("TestData", rs.getString("TestData"));
				jsonRow.put("VarName", rs.getString("VarName"));
				jsonRow.put("Status", rs.getString("Status"));
				jsonRow.put("test_step_status", rs.getString("Status"));
				String filename = rs.getString("Failure_Screenshot_Location");
				if (filename != null && !filename.equals("")) {
					String fileurl = AppProperties.fileurl + "?action=downloadFile&token=" + randomkey + "&fileName="
							+ filename;
					jsonRow.put("Failure_Screenshot_Location", fileurl);
				} else {
					jsonRow.put("Failure_Screenshot_Location", rs.getString("Failure_Screenshot_Location"));
				}
				String filename2 = rs.getString("screenshot1");
				if (filename2 != null && !filename2.equals("")) {
					String fileurl2 = AppProperties.fileurl + "?action=downloadFile&token=" + randomkey + "&fileName="
							+ filename2;
					jsonRow.put("screenshot1", fileurl2);
				} else {
					jsonRow.put("screenshot1", rs.getString("screenshot1"));
				}
				jsonRow.put("Test_Step", rs.getInt("Test_Step"));
				jsonRow.put("Executed_By", rs.getString("Executed_By"));
				double divisionResult = (double) rs.getDouble("Duration");
				String formattedResult = String.format("%.2f", divisionResult);
				jsonRow.put("Duration", formattedResult);
				jsonRow.put("test_step_duration", formattedResult);

				durationSum += divisionResult;
				String formattedDurationSum = String.format("%.2f", durationSum);

				String val = (String) rs.getString("valDevice");

				if (val == null || val.equals("")) {
					val = "On Screen";
				}
				jsonRow.put("ValDevice", val);
				jsonRow.put("Executed_Date", rs.getString("Executed_Date"));
				jsonRow.put("test_step_issues", rs.getString("issues"));
				jsonRow.put("test_step_issuetype", rs.getString("issuetype"));

				JSONarr.put(index, jsonRow);
				index = index + 1;

				if (getTestCaseResults) {
					testcases = getTestCaseResultAsJSON(rs, companyId, true);
				}
				testcases.put("DurationSum", formattedDurationSum);
			}

			if (JSONarr.size() == 0) {
				JSONObject jsonRow = new JSONObject();
				jsonRow.put("idtest_step_result", 0000);
				jsonRow.put("Test_Case_Results_Id", 0000);
				jsonRow.put("Step_Number", 1);
				jsonRow.put("Page_Name", "");
				jsonRow.put("Page_Description", "");
				jsonRow.put("Object_Xpath", "");
				jsonRow.put("Keyword", "");
				jsonRow.put("Action", "");
				jsonRow.put("Flow", "");
				jsonRow.put("TestData", "No execution data was found");
				jsonRow.put("VarName", "");
				jsonRow.put("Status", tcrStatus);
				jsonRow.put("test_step_status", "");
				JSONarr.put(index, jsonRow);
			}
		} catch (Exception e) {
			System.err.println(Utilities.getNow());
			e.printStackTrace();
		}

		JSONObject JSONData = new JSONObject();
		JSONData.put("data", JSONarr);
		JSONData.put("result", testcases);

		return JSONData;
	}

	public JSONObject getExecutableTestStepsAsJSON(ResultSet rs, boolean getPwd, boolean withCreatedDate,
			boolean insertTC, boolean skipFindAndAssign) {

		JSONObject JSONarr = new JSONObject();
		JSONObject stepNums = new JSONObject();
		int index = 2;
		processedTill = -1;
		HashMap responses = new HashMap();

		try {
			JSONObject prevRow = null;
			JSONObject findAndAssignData = null;
			while (rs.next()) {
				JSONObject jsonRow = getTestStepsFromDb(rs, getPwd, withCreatedDate);

				if (rs.getString("ts_sequence") != null) {
					String curKeyword = (String) jsonRow.get("Keyword");
					String curAction = (String) jsonRow.get("Action");
						
					if (!skipFindAndAssign) {
	                    if ("findAndAssign".equals(curAction)) {
	                        findAndAssignData = new JSONObject();
	                        findAndAssignData.put("android_uiautomator", jsonRow.get("android_uiautomator"));
	                        findAndAssignData.put("xpath", jsonRow.get("xpath"));
	                        findAndAssignData.put("id_strategy", jsonRow.get("id_strategy"));
	                        continue;
	                    }

	                    if (findAndAssignData != null) {
	                        jsonRow.put("android_uiautomator", findAndAssignData.get("android_uiautomator"));
	                        jsonRow.put("xpath", findAndAssignData.get("xpath"));
	                        jsonRow.put("id_strategy", findAndAssignData.get("id_strategy"));
	                        findAndAssignData = null;
	                    }
	                }
				        
				        if (curKeyword != null && curAction != null) {
							jsonRow.put("processedKeyword", curKeyword);
							jsonRow.put("processedAction", curAction);

						JSONarr.put(index, jsonRow);
						stepNums.put(index, (int) jsonRow.get("Step_Number"));
						index++;
					}
					continue;
				}

				if (JSONarr.size() == 0 || (jsonRow.get("manuallyAdded") != null
						&& (((String) jsonRow.get("manuallyAdded")).equals("true")
								|| ((String) jsonRow.get("manuallyAdded")).equals("nlp")))) {
					isDynamic(jsonRow, processedTill, responses);
					processedTill = (long) jsonRow.get("actualtime");
					JSONarr.put(index, jsonRow);
					stepNums.put(index, (int) jsonRow.get("Step_Number"));

					if (withCreatedDate && (String) jsonRow.get("manuallyAdded") != null
							&& (((String) jsonRow.get("manuallyAdded")).equals("true")
									|| ((String) jsonRow.get("manuallyAdded")).equals("nlp"))) {
						if (prevRow == null) {
							Timestamp ts = new Timestamp(0);
							jsonRow.put("createddate", ts);
						} else {
							Timestamp ts = (Timestamp) prevRow.get("createddate");
							long milliseconds = ts.getTime();
							milliseconds += 1;
							Timestamp newTs = new Timestamp(milliseconds);
							jsonRow.put("createddate", newTs);
						}
					}
					String curKeyword = (String) jsonRow.get("Keyword");
					String curAction = (String) jsonRow.get("Action");
					if (insertTC && curKeyword != null && curAction != null && curKeyword.equalsIgnoreCase("TestCase")
							&& curAction.equalsIgnoreCase("Include")) {
						int tabid = (int) prevRow.get("tabid");
						int windowid = (int) prevRow.get("windowid");
						String td = (String) jsonRow.get("TestData");
						int tcid = Integer.valueOf(td);
						JSONObject tcResult = getTestStepsFromDBByTestCaseId(tcid, true, false);
						JSONObject data = (JSONObject) tcResult.get("data");

						for (int i = 0; i < data.size(); i++) {
							JSONObject step = (JSONObject) data.get(i + 2);
							step.put("tabid", tabid);
							step.put("windowid", windowid);
							JSONarr.put(index, step);
							index = index + 1;
						}

						index = index - 1;
					} else if (jsonRow.get("manuallyAdded") == null || index == 2) {
						prevRow = jsonRow;
					}
					index = index + 1;
				} else if (prevRow != null) {
					long prevRowEventTime = (long) prevRow.get("actualtime");
					long curRowEventTime = (long) jsonRow.get("actualtime");
					String prevEvent = (String) prevRow.get("eventname");
					String curEvent = (String) jsonRow.get("eventname");
					String prevKeyword = (String) prevRow.get("Keyword");
					String curKeyword = (String) jsonRow.get("Keyword");
					String prevXPath = (String) prevRow.get("Object_Xpath");
					String curXPath = (String) jsonRow.get("Object_Xpath");
					String prevAction = (String) prevRow.get("Action");
					String curAction = (String) jsonRow.get("Action");
					String prevPageD = (String) prevRow.get("Page_Description");
					String curPageD = (String) jsonRow.get("Page_Description");
					boolean isShadow = false;
					if (jsonRow.get("shadowdom") != null) {
						isShadow = (boolean) jsonRow.get("shadowdom");
					}
					String shadowPath = (String) jsonRow.get("shadowpath");
					boolean prevIsShadow = false;
					if (prevRow.get("shadowdom") != null) {
						prevIsShadow = (boolean) prevRow.get("shadowdom");
					}
					String prevShadowPath = (String)prevRow.get("shadowpath");
					
					if(((curRowEventTime - prevRowEventTime) > 300) ||
							(((curRowEventTime - prevRowEventTime) <= 0) &&
									jsonRow.get("manuallyAdded") == null && (prevRow.get("manuallyAdded") != null &&
											(prevRow.get("manuallyAdded").equals("merged") || prevRow.get("manuallyAdded").equals("hover")))) ||
							(jsonRow.get("manuallyAdded") != null && jsonRow.get("manuallyAdded").equals("hover"))  ||
							(((curRowEventTime - prevRowEventTime) <= 0) && 
							(curEvent != null && (curEvent).equals("filedownload"))) ||
							(((curRowEventTime - prevRowEventTime) <= 0) && 
									(curEvent != null && curAction.equalsIgnoreCase("error")))    ){
						if(curEvent != null && (curEvent).equals("dblClick")) {
							index = cleanUpForDblClick(JSONarr, stepNums, jsonRow,index);
						} 
						
						isDynamic(jsonRow, processedTill, responses);
						processedTill = (long) jsonRow.get("actualtime");

						if ((!isShadow && curAction != null && curAction.equalsIgnoreCase("Enter") && prevAction != null
								&& prevAction.equalsIgnoreCase("Click")
								&& (prevXPath != null && curXPath != null && prevXPath.equalsIgnoreCase(curXPath)))
								|| (!isShadow && curAction != null && curAction.equalsIgnoreCase("Clear & Enter") && prevAction != null
										&& prevAction.equalsIgnoreCase("Click")
										&& (prevXPath != null && curXPath != null && prevXPath.equalsIgnoreCase(curXPath)))
								|| (!isShadow && curAction != null && curAction.equalsIgnoreCase("validatePartial")
										&& prevAction != null && prevAction.equalsIgnoreCase("Click")
										&& (prevXPath != null && curXPath != null
												&& prevXPath.equalsIgnoreCase(curXPath)))
								|| (!isShadow && curAction != null && curAction.equalsIgnoreCase("Upload")
										&& prevAction != null && prevAction.equalsIgnoreCase("Click")
										&& (prevXPath != null && curXPath != null
												&& prevXPath.equalsIgnoreCase(curXPath)))
								|| (!isShadow && curAction != null && curAction.equalsIgnoreCase("dnddrop")
										&& prevAction != null && prevAction.equalsIgnoreCase("Click")
										&& (prevXPath != null && curXPath != null
												&& prevXPath.equalsIgnoreCase(curXPath)))
								|| (!isShadow && curAction != null && curAction.equalsIgnoreCase("dnddrag")
										&& prevAction != null && prevAction.equalsIgnoreCase("Click")
										&& (prevPageD != null && curPageD != null
												&& prevPageD.equalsIgnoreCase(curPageD)))
								|| (isShadow && prevIsShadow && curAction != null && curAction.equalsIgnoreCase("Clear & Enter")
										&& prevAction != null && prevAction.equalsIgnoreCase("Click")
										&& (prevXPath != null && curXPath != null
												&& prevXPath.equalsIgnoreCase(curXPath)))
								|| (isShadow && prevIsShadow && curAction != null && curAction.equalsIgnoreCase("Enter")
										&& prevAction != null && prevAction.equalsIgnoreCase("Click")
										&& (prevXPath != null && curXPath != null
												&& prevXPath.equalsIgnoreCase(curXPath)))
										&& (prevShadowPath.equalsIgnoreCase(shadowPath))) {
							JSONarr.remove(index - 1);
							JSONarr.put(index - 1, jsonRow);
							stepNums.remove(index - 1);
							stepNums.put(index - 1, (int) jsonRow.get("Step_Number"));
						} else {
							JSONarr.put(index, jsonRow);
							stepNums.put(index, (int) jsonRow.get("Step_Number"));
							index = index + 1;
						}
						prevRow = jsonRow;

					} else {

						if (prevEvent != null && prevEvent.equalsIgnoreCase("mousedown") && curEvent != null
								&& curEvent.equalsIgnoreCase("input")) {
						} else if (prevEvent != null && prevEvent.equalsIgnoreCase("input") && curEvent != null
								&& curEvent.equalsIgnoreCase("change")) {
							if (prevKeyword.equals(curKeyword) && (prevXPath.equals(curXPath))
									&& (curKeyword.equals("File Upload"))) {
								isDynamic(jsonRow, processedTill, responses);
								processedTill = (long) jsonRow.get("actualtime");
								JSONarr.remove(index - 1);
								JSONarr.put(index - 1, jsonRow);
								stepNums.remove(index - 1);
								stepNums.put(index - 1, (int) jsonRow.get("Step_Number"));
							}
						} else if (prevEvent != null && prevEvent.equalsIgnoreCase("mousedown") && curEvent != null
								&& curEvent.equalsIgnoreCase("change")) {
							if (!(prevXPath.equals(curXPath))) {
								if (prevKeyword.equals("Button") && curKeyword.equals("EditBox")) {
									isDynamic(jsonRow, processedTill, responses);
									processedTill = (long) jsonRow.get("actualtime");
									JSONarr.remove(index - 1);
									JSONarr.put(index - 1, jsonRow);
									stepNums.remove(index - 1);
									stepNums.put(index - 1, (int) jsonRow.get("Step_Number"));
									JSONarr.put(index, prevRow);
									stepNums.put(index, (int) prevRow.get("Step_Number"));
//									prevRow = jsonRow;
									index = index + 1;
								}else if((prevKeyword.equals("") && curKeyword.equals("EditBox")) &&
										((curRowEventTime - prevRowEventTime) <= 300)) {
									
								} else {
									isDynamic(jsonRow, processedTill, responses);
									processedTill = (long) jsonRow.get("actualtime");
									JSONarr.remove(index - 1);
									JSONarr.put(index - 1, jsonRow);
									stepNums.remove(index - 1);
									stepNums.put(index - 1, (int) jsonRow.get("Step_Number"));
								}
							}
						} else if (curEvent != null && curEvent.equalsIgnoreCase("dblClick")) {
							index = cleanUpForDblClick(JSONarr, stepNums, jsonRow, index);
//							index = JSONarr.size();
							isDynamic(jsonRow, processedTill, responses);
							processedTill = (long) jsonRow.get("actualtime");
							JSONarr.put(index, jsonRow);
							stepNums.put(index, (int) jsonRow.get("Step_Number"));
							index = index + 1;
						} else if (curEvent != null && curEvent.equalsIgnoreCase("contextmenu")) {
							index = cleanUpForRightClick(JSONarr, stepNums, jsonRow, index);
							// index = JSONarr.size();
							isDynamic(jsonRow, processedTill, responses);
							processedTill = (long) jsonRow.get("actualtime");
							JSONarr.put(index, jsonRow);
							stepNums.put(index, (int) jsonRow.get("Step_Number"));
							index = index + 1;
						} else if ((!isShadow && curAction != null && curAction.equalsIgnoreCase("dnddrag")
								&& prevAction != null && prevAction.equalsIgnoreCase("Click")
								&& (prevPageD != null && curPageD != null && prevPageD.equalsIgnoreCase(curPageD)))) {
							JSONarr.remove(index - 1);
							JSONarr.put(index - 1, jsonRow);
							stepNums.remove(index - 1);
							stepNums.put(index - 1, (int) jsonRow.get("Step_Number"));
							prevRow = jsonRow;
						} else if (curKeyword != null && curKeyword.equalsIgnoreCase(AppProperties.WINKEYWORD)
								&& curAction != null && curAction.equals("Create")) {
							processedTill = (long) jsonRow.get("actualtime");
							JSONarr.put(index, jsonRow);
							stepNums.put(index, (int) jsonRow.get("Step_Number"));
							index = index + 1;

						}
					}
				}

			}
		} catch (Exception e) {
			System.err.println(Utilities.getNow());
			e.printStackTrace();
		}

		JSONObject JSONData = new JSONObject();
		JSONData.put("data", JSONarr);
		JSONData.put("stepnums", stepNums);

		return JSONData;
	}

	private void isDynamic(JSONObject jsonRow, long processedTill, HashMap responses) {

		//the dynamic needs more testing - turning off till then
		boolean pleasReturn = true;
		if(pleasReturn) {
			return;
		}
		
		int dp = (int) jsonRow.get("dynamicProcessed");
		long lasTime = (long) jsonRow.get("actualtime");
		int tcid = (int) jsonRow.get("Test_Case_Id");
		int companyId = (int) userDetails.get("companyid");

		if (dp == 1) {
			return;
		}

		// get responses after processedTill to current row time
		String sql = "select * from  cta.apidata where actualtime >= ? and " + "testcase = ?";

		try {
			PreparedStatement stmt = testCon.prepareStatement(sql);
			stmt.setLong(1, processedTill);
			stmt.setInt(2, tcid);
			ResultSet rs = stmt.executeQuery();

			while (rs.next()) {
				String randomPair = rs.getString("randomString");
				if (!responses.keySet().contains(randomPair)) {
					String filePath = Utilities.getApiDataPath(companyId, tcid);
					String responseFilePath = filePath + "\\response_" + randomPair + ".txt";
					String res = Utilities.readDataFromFile(responseFilePath);
					responses.put(randomPair, res);
				}
			}

			String testData = (String) jsonRow.get("TestData");
			boolean isDynamic = false;
			int tsid = (int) jsonRow.get("idtest_step");

			if (testData != null && !testData.equals("")) {
				Iterator iter = responses.keySet().iterator();
				while (iter.hasNext()) {
					String res = (String) responses.get(iter.next());
					if (res.contains(testData)) {
						sql = "update cta.test_step_attr set dynamicProcessed = 1, isDynamic =1 "
								+ "where test_step = ?  ";
						stmt = testCon.prepareStatement(sql);
						stmt.setInt(1, tsid);
						stmt.executeUpdate();
						jsonRow.put("isDynamic", "1");
						isDynamic = true;
						break;
					}
				}
			}
			if (!isDynamic) {
				sql = "update cta.test_step_attr set dynamicProcessed = 1 " + "where test_step = ?  ";
				stmt = testCon.prepareStatement(sql);
				stmt.setInt(1, tsid);
				stmt.executeUpdate();
			}
			jsonRow.put("dynamicProcessed", "1");

			rs.close();
			stmt.close();
		} catch (Exception e) {
			System.err.println(Utilities.getNow());
			e.printStackTrace();
		}
	}

	private int cleanUpForDblClick(JSONObject JSONarr, JSONObject stepNums, JSONObject jsonRow, int index) {

		do {
			int i = index - 1;
			JSONObject prevRow = (JSONObject) JSONarr.get(i);

			if (prevRow == null) {
				break;
			}

			String prevXPath = (String) prevRow.get("Object_Xpath");
			String curXPath = (String) jsonRow.get("Object_Xpath");
			String prevAction = (String) prevRow.get("Action");
			String curAction = (String) jsonRow.get("Action");

			if (!prevXPath.equals(curXPath)) {
				return index;
			} else if (prevAction.equalsIgnoreCase("validatePartial") || prevAction.equalsIgnoreCase("Click")) {

				JSONObject removedRow = (JSONObject) JSONarr.get(i);
				processedTill = (long) removedRow.get("actualtime");
				JSONarr.remove(i);
				stepNums.remove(i);
				index = index - 1;
			} else {
				return index;
			}
		} while (index > 0);
		return index;
	}

	private int cleanUpForRightClick(JSONObject JSONarr, JSONObject stepNums, JSONObject jsonRow, int index) {

		// for (int i= JSONarr.size() -1; i>= 0; i--) {
		int i = index - 1;
		JSONObject prevRow = (JSONObject) JSONarr.get(i);

		String prevXPath = (String) prevRow.get("Object_Xpath");
		String curXPath = (String) jsonRow.get("Object_Xpath");
		String prevAction = (String) prevRow.get("Action");
		String curAction = (String) jsonRow.get("Action");

		if (prevXPath.equals(curXPath) && (prevAction.equalsIgnoreCase("Click"))) {
			JSONarr.remove(i);
			stepNums.remove(i);
			index = index - 1;
		}

		return index;
	}

	public JSONObject getTestStepsFromDb(ResultSet rs, boolean getPwd, boolean withCreatedDate) {
		JSONObject jsonRow = new JSONObject();
		try {
			jsonRow.put("idtest_step", rs.getInt("idtest_step"));
			jsonRow.put("Test_Case_Id", rs.getInt("Test_Case_Id"));
			jsonRow.put("Step_Number", rs.getInt("tsSequence"));
			jsonRow.put("Page_Name", rs.getString("Page_Name"));
			if (rs.getString("Page_Description") == null)
				jsonRow.put("Page_Description", "");
			else
				jsonRow.put("Page_Description", rs.getString("Page_Description"));
			jsonRow.put("Object_Xpath", rs.getString("Object_Xpath"));
			jsonRow.put("Keyword", rs.getString("Keyword"));
			jsonRow.put("Action", rs.getString("Action"));
			jsonRow.put("subAction", rs.getString("subAction"));
			jsonRow.put("Flow", rs.getString("Flow"));
			jsonRow.put("TestData", rs.getString("TestData"));
			jsonRow.put("VarName", rs.getString("VarName"));
			jsonRow.put("ValDevice", rs.getString("ValDevice"));
			jsonRow.put("extKey", rs.getString("extKey"));
			jsonRow.put("eventTime", rs.getString("eventTime"));
			jsonRow.put("RecordedData", rs.getString("RecordedData"));
			jsonRow.put("continueOnFail", rs.getString("continueon_fail"));
			String apiData = rs.getString("apiData");
			JSONParser parser = new JSONParser();
            JSONObject apiDataJSON = null;
            if(apiData != null) {
            	apiDataJSON = (JSONObject) parser.parse(apiData);
            }
            jsonRow.put("apiData", apiDataJSON);
			jsonRow.put("filename", rs.getString("filename"));
			String fileField = rs.getString("fileField");
			if (fileField != null && !fileField.equals("") && !fileField.equals("null")) {
				String[] splitParts = fileField.split(AppProperties.testdatadelimiter);
				if (splitParts.length >= 2) {
					String sheet = splitParts[0];
					String cell = splitParts[1];
					jsonRow.put("sheet", sheet);
					jsonRow.put("cell", cell);
				}
			}
			jsonRow.put("fileField", rs.getString("fileField"));
			jsonRow.put("DBUrl", rs.getString("DBUrl"));
			int apiid = rs.getInt("apiid");
			jsonRow.put("apiid", apiid);
			int modid = 0;
			int prodid = 0;
			if (apiid > 0) {
				String sql = "SELECT moduleid FROM cta.api WHERE idapi = ? AND status != 1";
				PreparedStatement stmt = testCon.prepareStatement(sql);
				stmt.setInt(1, apiid);
				ResultSet rs1 = stmt.executeQuery();
				if (rs1.next()) {
					modid = rs1.getInt(1);
					prodid = getProductsFromModule(modid);
				}
				rs1.close();
				stmt.close();
			}
			jsonRow.put("apimodule", modid);
			jsonRow.put("apiproduct", prodid);
			jsonRow.put("apiparam", rs.getString("apiparam"));
			jsonRow.put("DBQuery", rs.getString("DBQuery"));
			jsonRow.put("status", rs.getString("status"));
			jsonRow.put("DBType", rs.getString("DBType"));
			jsonRow.put("testdata_source", rs.getString("testdata_source"));
			jsonRow.put("gendataType", rs.getString("gendataType"));
			jsonRow.put("gendatalen", rs.getString("gendatalen"));
			jsonRow.put("gendateformat", rs.getString("gendateformat"));
			jsonRow.put("gendateoffset", rs.getString("gendateoffset"));
			jsonRow.put("DBVersion", rs.getString("DBVersion"));
			jsonRow.put("clickable", rs.getInt("clickable"));
			jsonRow.put("Issues", rs.getString("Issues"));
			String issueDesc = issueDesc(rs.getString("Issues"));
			jsonRow.put("IssuesDesc", issueDesc);
			jsonRow.put("screenshot", rs.getString("screenshot"));
			jsonRow.put("actualtime", rs.getLong("actualtime"));
			jsonRow.put("eventname", rs.getString("eventname"));
			jsonRow.put("regex", rs.getString("regex"));
			jsonRow.put("iframexpath", rs.getString("iframexpath"));
			jsonRow.put("altpath", rs.getString("altpath"));
			jsonRow.put("outerhtml", rs.getString("outerhtml"));
			if (rs.getString("wait") == null) {
				jsonRow.put("wait", "Before");
			} else {
				jsonRow.put("wait", rs.getString("wait"));
			}
			jsonRow.put("waittime", rs.getInt("waittime"));
			jsonRow.put("manuallyAdded", rs.getString("manuallyAdded"));
			jsonRow.put("samerowtype", rs.getString("samerowtype"));
			jsonRow.put("samerowindex", rs.getInt("samerowindex"));
			jsonRow.put("tabid", rs.getInt("tabid"));
			jsonRow.put("windowid", rs.getInt("windowid"));
			jsonRow.put("teststepthreshold", rs.getInt("teststepthreshold"));
			jsonRow.put("tsSequence", rs.getString("tsSequence"));
			int dependentTC = rs.getInt("dependantTC");
			jsonRow.put("dependentTC", dependentTC);
			int module = 0, product = 0;
			String ProdName = "", ModName = "", TCName = "";
			if (dependentTC > 0) {
				String sql = "SELECT m.* FROM cta.test_case tc JOIN cta.modules m ON m.idmodules = tc.Module "
						+ "WHERE tc.idtest_case = ? AND tc.Status != 1";
				PreparedStatement stmt = testCon.prepareStatement(sql);
				stmt.setInt(1, dependentTC);
				ResultSet rs1 = stmt.executeQuery();
				if (rs1.next()) {
					product = rs1.getInt("product");
					ProdName = getProdName(product);
					module = rs1.getInt("idmodules");
					ModName = getModuleName(module);
					TCName = getTCName(dependentTC);
				}
				rs1.close();
				stmt.close();
			}

			jsonRow.put("DependentModule", module);
			jsonRow.put("DependentProduct", product);
			jsonRow.put("DependentTC", TCName);

			int inlineTC = rs.getInt("inlinedepedence");
			jsonRow.put("inlinedepedence", inlineTC);
			if (inlineTC > 0) {
				String sql = "SELECT m.* FROM cta.test_case tc JOIN cta.modules m ON m.idmodules = tc.Module "
						+ "WHERE tc.idtest_case = ? AND tc.Status != 1";
				PreparedStatement stmt = testCon.prepareStatement(sql);
				stmt.setInt(1, inlineTC);
				ResultSet rs1 = stmt.executeQuery();
				if (rs1.next()) {
					product = rs1.getInt("product");
					ProdName = getProdName(product);
					module = rs1.getInt("idmodules");
					ModName = getModuleName(module);
					TCName = getTCName(inlineTC);
				}
				rs1.close();
				stmt.close();
			}

			jsonRow.put("InlineModule", module);
			jsonRow.put("InlineProduct", product);
			jsonRow.put("InlineTC", TCName);

			jsonRow.put("customerEmail", rs.getString("customerEmail"));
			jsonRow.put("customerPassword", rs.getString("customerPassword"));
			jsonRow.put("EmailSelectionCriteria", rs.getString("EmailSelectionCriteria"));
			jsonRow.put("EmailFilter", rs.getString("EmailFilter"));
			jsonRow.put("type", rs.getString("type"));
			if (withCreatedDate) {
				jsonRow.put("createddate", rs.getTimestamp("createddate"));
			}

			if (!getPwd && rs.getString("type") != null && rs.getString("type").equalsIgnoreCase("password")) {
				jsonRow.put("TestData", "********");
			}

			String attrSql = "SELECT * FROM cta.test_step_attr WHERE " + "test_step = ? ";
			PreparedStatement attrStmt = testCon.prepareStatement(attrSql);
			attrStmt.setInt(1, rs.getInt("idtest_step"));
			ResultSet resultSet2 = attrStmt.executeQuery();
			if (resultSet2.next()) {
				jsonRow.put("imgpath", resultSet2.getString("imgpath"));
				jsonRow.put("bgcolor", resultSet2.getString("bgcolor"));
				jsonRow.put("color", resultSet2.getString("color"));
				jsonRow.put("pageurl", resultSet2.getString("pageurl"));
				jsonRow.put("uniquetext", resultSet2.getString("uniquetext"));
				jsonRow.put("uniquetoparent", resultSet2.getString("uniquetoparent"));
				jsonRow.put("parenttotarget", resultSet2.getString("parenttotarget"));
				jsonRow.put("isDynamic", resultSet2.getInt("isDynamic"));
				jsonRow.put("createsAlert", resultSet2.getInt("createsAlert"));
				jsonRow.put("dynamicProcessed", resultSet2.getInt("dynamicProcessed"));
				jsonRow.put("endRange", resultSet2.getString("endrange"));
				jsonRow.put("scope", resultSet2.getString("scope"));
				jsonRow.put("pagename", resultSet2.getString("pagename"));
				jsonRow.put("pagenumber", resultSet2.getInt("pagenumber"));
				jsonRow.put("strategy", resultSet2.getString("locationstrategy"));
				jsonRow.put("searchpageXpath", resultSet2.getString("searchpageXpath"));
				jsonRow.put("shadowdom", resultSet2.getBoolean("shadowdom"));
				jsonRow.put("shadowindex", resultSet2.getInt("shadowindex"));
				jsonRow.put("shadowelement", resultSet2.getString("shadowelement"));
				jsonRow.put("shadowpath", resultSet2.getString("shadowpath"));
				jsonRow.put("toaddress", resultSet2.getString("toaddress"));
				jsonRow.put("emailsubject", resultSet2.getString("emailsubject"));
				jsonRow.put("emailcontent", resultSet2.getString("emailcontent"));
				jsonRow.put("uniqueisbackup", resultSet2.getBoolean("uniqueisbackup"));
				jsonRow.put("elementId", resultSet2.getInt("elementId"));
				jsonRow.put("scriptfile", resultSet2.getString("scriptfile"));
				jsonRow.put("script", resultSet2.getString("script"));
				jsonRow.put("cmdfile", resultSet2.getString("cmdfile"));
				jsonRow.put("nearestname", resultSet2.getString("nearestname"));
				jsonRow.put("parenttotargetcvrg", resultSet2.getString("parenttotargetcvrg"));
				jsonRow.put("uniquetoparentcvrg", resultSet2.getString("uniquetoparentcvrg"));
				jsonRow.put("indexcvrg", resultSet2.getInt("indexcvrg"));
				jsonRow.put("getattribute", resultSet2.getInt("getattribute"));
				jsonRow.put("xpos", resultSet2.getInt("xpos"));
				jsonRow.put("ypos", resultSet2.getInt("ypos"));
				jsonRow.put("elementplaceholder", resultSet2.getString("elementplaceholder"));
				jsonRow.put("elementplaceholderindex", resultSet2.getInt("elementplaceholderindex"));
				jsonRow.put("classname", resultSet2.getString("classname"));
				jsonRow.put("classnameindex", resultSet2.getInt("classnameindex"));
				jsonRow.put("dataValue", resultSet2.getString("data_value"));
				jsonRow.put("dataValueIndex", resultSet2.getInt("data_value_index"));
				jsonRow.put("typeindex", resultSet2.getInt("typeindex"));
				jsonRow.put("href", resultSet2.getString("href"));
				jsonRow.put("hrefindex", resultSet2.getInt("hrefindex"));
				jsonRow.put("condLogic", resultSet2.getString("condLogic"));
				jsonRow.put("condexp", resultSet2.getString("condexp"));
			} else {
				jsonRow.put("imgpath", null);
				jsonRow.put("bgcolor", null);
				jsonRow.put("color", null);
				jsonRow.put("pageurl", null);
				jsonRow.put("uniquetext", null);
				jsonRow.put("uniquetoparent", null);
				jsonRow.put("parenttotarget", null);
				jsonRow.put("isDynamic", 0);
				jsonRow.put("createsAlert", 0);
				jsonRow.put("dynamicProcessed", 1);
				jsonRow.put("endRange", null);
				jsonRow.put("scope", null);
				jsonRow.put("pagename", null);
				jsonRow.put("pagenumber", 0);
				jsonRow.put("strategy", null);
				jsonRow.put("searchpageXpath", null);
				jsonRow.put("shadowdom", false);
				jsonRow.put("shadowindex", 0);
				jsonRow.put("shadowelement", null);
				jsonRow.put("shadowpath", null);
				jsonRow.put("toaddress", null);
				jsonRow.put("emailsubject", null);
				jsonRow.put("emailcontent", null);
				jsonRow.put("uniqueisbackup", false);
				jsonRow.put("elementId", 0);
				jsonRow.put("scriptfile", null);
				jsonRow.put("script", null);
				jsonRow.put("cmdfile", null);
				jsonRow.put("nearestname", null);
				jsonRow.put("parenttotargetcvrg", null);
				jsonRow.put("uniquetoparentcvrg", null);
				jsonRow.put("indexcvrg", 0);
				jsonRow.put("getattribute", null);
				jsonRow.put("xpos", 0);
				jsonRow.put("ypos", 0);
				jsonRow.put("elementplaceholder", null);
				jsonRow.put("elementplaceholderindex", -1);
				jsonRow.put("classname", null);
				jsonRow.put("classnameindex", -1);
				jsonRow.put("dataValue", null);
				jsonRow.put("dataValueIndex", -1);
				jsonRow.put("typeindex", -1);
				jsonRow.put("href", null);
				jsonRow.put("hrefindex", -1);
				jsonRow.put("condLogic", null);
				jsonRow.put("condexp", null);
			}
			resultSet2.close();
			attrStmt.close();

			String mtsSql = "SELECT * FROM cta.mobile_automation WHERE " + "idtest_step = ?";
			PreparedStatement mtsStmt = testCon.prepareStatement(mtsSql);
			mtsStmt.setInt(1, rs.getInt("idtest_step"));
			ResultSet resultSet3 = mtsStmt.executeQuery();
			if (resultSet3.next()) {
				jsonRow.put("idtest_step", resultSet3.getInt("idtest_step"));
				jsonRow.put("id", resultSet3.getInt("id"));
				jsonRow.put("Action", resultSet3.getString("action"));
				jsonRow.put("Keyword", resultSet3.getString("device"));
				jsonRow.put("element_id", resultSet3.getString("element_id"));
				jsonRow.put("Step_Number", resultSet3.getInt("sequence_number"));
				jsonRow.put("sequence_number", resultSet3.getInt("sequence_number"));
				jsonRow.put("strategy_map", resultSet3.getString("strategy_map"));
				jsonRow.put("android_uiautomator", resultSet3.getString("android_uiautomator"));
				jsonRow.put("xpath", resultSet3.getString("xpath"));
				jsonRow.put("id_strategy", resultSet3.getString("id_strategy"));
				jsonRow.put("test_case_id", resultSet3.getInt("test_case_id"));
				if (resultSet3.getTimestamp("timestamp") != null) {
					jsonRow.put("timestamp", (resultSet3.getTimestamp("timestamp")).toString());
				} else {
					jsonRow.put("timestamp", 0);
				}
				if (resultSet3.getTimestamp("appium_timestamp") != null) {
					jsonRow.put("appium_timestamp", (resultSet3.getTimestamp("appium_timestamp")).toString());
				} else {
					jsonRow.put("appium_timestamp", 0);
				}
			}
//			else {
//				jsonRow.put("idtest_step", null);
//				jsonRow.put("id",null);
//				jsonRow.put("Action", null);
//				jsonRow.put("Keyword", null);
//				jsonRow.put("element_id", null);
//				jsonRow.put("sequence_number", null);
//				jsonRow.put("strategy_map", null);
//				jsonRow.put("android_uiautomator", null);
//				jsonRow.put("xpath", null);
//				jsonRow.put("id_strategy", null);
//				jsonRow.put("test_case_id", null);
//			}
			resultSet3.close();
			mtsStmt.close();
		} catch (Exception e) {
			System.err.println(Utilities.getNow());
			e.printStackTrace();
		}

		return jsonRow;
	}

	private String issueDesc(String issues) {
		String result = "";
		if (issues != null && !issues.equals("")) {
			if (issues.contains(";")) {
				String[] issueList = issues.split(";");
				for (int i = 0; i < issueList.length; i++) {
					String issue = issueList[i];
					result = result + issue + "-" + issuesMap.get(issue) + ";";
				}
			} else {
				result = issues + "-" + issuesMap.get(issues) + ";";
			}
		}

		return result;
	}

	private JSONObject getTestCasesAsJSON(ResultSet rs) {

		JSONObject JSONarr = new JSONObject();
		int index = 0;

		try {
			while (rs.next()) {
				JSONObject jsonRow = getTestCaseAsJSON(rs, false);
				JSONarr.put(index, jsonRow);
				index = index + 1;
			}
		} catch (Exception e) {
			System.err.println(Utilities.getNow());
			e.printStackTrace();
		}

		JSONObject JSONData = new JSONObject();
		JSONData.put("testcases", JSONarr);

		return JSONData;
	}

	private JSONObject getTestCaseAsJSON(ResultSet rs, boolean tcStatus) {

		JSONObject jsonRow = new JSONObject();
		try {
			int tcid = rs.getInt("idtest_case");
			jsonRow.put("idtest_case", tcid);
			jsonRow.put("Test_Case", rs.getString("Test_Case"));
			jsonRow.put("Test_Case_Desc", rs.getString("testcasedescription"));
			jsonRow.put("XLS_Location", rs.getString("XLS_Location"));
			jsonRow.put("Version", rs.getInt("Version"));
//				jsonRow.put("Created_By", rs.getString("Created_By"));
			if (rs.getTimestamp("Created_Date") != null) {
				jsonRow.put("Created_Date", (rs.getTimestamp("Created_Date")).toString());
			} else {
				jsonRow.put("Created_Date", 0);
			}
			jsonRow.put("SaveType", rs.getString("SaveType"));
			if (!tcStatus) {
				jsonRow.put("Status", rs.getInt("Status"));
			} else {
				jsonRow.put("Status", rs.getInt("tcStatus"));
			}
			jsonRow.put("extKey", rs.getInt("extKey"));
			int moduleId = rs.getInt("Module");
			jsonRow.put("Module", moduleId);
			jsonRow.put("width", rs.getInt("width"));
			jsonRow.put("height", rs.getInt("height"));
			jsonRow.put("browser", rs.getString("browser"));
			jsonRow.put("envname", rs.getString("envname"));
			jsonRow.put("envurl", rs.getString("envurl"));
			jsonRow.put("copiedfrom", rs.getInt("copiedfrom"));
			jsonRow.put("testcasethreshold", rs.getInt("testcasethreshold"));
			jsonRow.put("continuetest", rs.getBoolean("continuetest"));
			jsonRow.put("multirun", rs.getInt("multirun"));
			jsonRow.put("proxyurl", rs.getString("proxyurl"));
			jsonRow.put("beenanalysed", rs.getInt("beenanalysed"));
			jsonRow.put("enableaudio", rs.getBoolean("enableaudio"));
			jsonRow.put("enablevideo", rs.getBoolean("enablevideo"));
			jsonRow.put("stealthMode", rs.getBoolean("stealthMode"));
			jsonRow.put("forcenewsession", rs.getBoolean("forcenewsession"));
			if (rs.getTimestamp("analysisdate") != null) {
				jsonRow.put("analysisdate", (rs.getTimestamp("analysisdate")).toString());
			} else {
				jsonRow.put("analysisdate", 0);
			}
			jsonRow.put("imagesbaselined", rs.getInt("imagesbaselined"));
			if (rs.getTimestamp("baselinedate") != null) {
				jsonRow.put("baselinedate", (rs.getTimestamp("baselinedate")).toString());
			} else {
				jsonRow.put("baselinedate", 0);
			}
			int cid = getCompanyFromModule(moduleId);
			String fileName = rs.getString("videofilename");
			String url = AppProperties.fileurl + "?action=downloadTCVideoFile&companyid=" + cid + "&token=" + randomkey
					+ "&tcid=" + tcid + "&fileName=" + fileName;
			jsonRow.put("videofilename", rs.getString("videofilename"));
			jsonRow.put("videofileurl", url);
			jsonRow.put("case_Type", rs.getString("case_Type"));
			String isWait = rs.getString("isWait");
			boolean keepbrowseropen = false;
			if(isWait != null && isWait.equalsIgnoreCase("true")) {
				keepbrowseropen = true;
			}
			jsonRow.put("keepbrowseropen", keepbrowseropen);
		} catch (Exception e) {
			System.err.println(Utilities.getNow());
			e.printStackTrace();
		}
		return jsonRow;
	}

	public void updateViewPortForTestCase(long width, long height, long randomkey) {
		String sql = "update cta.test_case set width = ?, height = ? where extkey = ? "
				+ "ORDER BY Created_Date DESC LIMIT 1";

		try {
			PreparedStatement stmt = testCon.prepareStatement(sql);
			stmt.setLong(1, width);
			stmt.setLong(2, height);
			stmt.setLong(3, randomkey);
			stmt.executeUpdate();
			stmt.close();
		} catch (Exception e) {
			System.err.println(Utilities.getNow());
			e.printStackTrace();
		}
	}

	public int writeIncTestCaseToDB(String testCase, String xlsLoc, int version, String createdBy, String saveType,
			long randomKey, int moduleId, int width, int height) {

		String sql = "SELECT SaveType, idtest_case, Status FROM " + "cta.test_case WHERE Test_Case = ? AND "
				+ "extKey = ? ORDER BY " + "Created_Date DESC LIMIT 1";
		int testCaseId = -1;
		try {
			PreparedStatement stmt = testCon.prepareStatement(sql);

			// Set the values for the prepared statement
			stmt.setString(1, testCase);
			stmt.setLong(2, randomKey);

			// Execute the prepared statement
			ResultSet resultSet = stmt.executeQuery();

			if (resultSet.next()) {
				String tcSaveType = resultSet.getString("SaveType");
				int status = resultSet.getInt("Status");
				if ((tcSaveType == null || tcSaveType.equals("Completed"))
						|| (tcSaveType.equals("Recording") && status == 1)) {
					testCaseId = writeTestCaseToDB(testCase, xlsLoc, version, createdBy, saveType, randomKey, moduleId,
							width, height);
				} else {
					testCaseId = resultSet.getInt("idtest_case");
				}
			} else {
				testCaseId = writeTestCaseToDB(testCase, xlsLoc, version, createdBy, saveType, randomKey, moduleId,
						width, height);
			}

			resultSet.close();
			stmt.close();

		} catch (Exception e) {
			System.err.println(Utilities.getNow());
			e.printStackTrace();
		}

		return testCaseId;
	}

	public int createNLPTestCase(int tcid, String steps) {

		String sql = "INSERT INTO cta.test_case_nlp (test_case, nlpsteps) VALUES(?, ?)";
		try {
			PreparedStatement stmt = testCon.prepareStatement(sql);
			stmt.setInt(1, tcid);
			stmt.setString(2, steps);
			stmt.execute();
			stmt.close();
		} catch (Exception e) {
			System.err.println(Utilities.getNow());
			e.printStackTrace();
			tcid = -1;
		}
		DashBoardData dbd = new DashBoardData(this);
		dbd.updateDashBoardForTCCount(tcid);
		return tcid;
	}

	public int checkAndWriteTestCaseToDB(String testCase, String xlsLoc, int version, String createdBy, String saveType,
			long randomKey, int moduleId, int width, int height) {

		int tcid = -1;
		if (saveType.equals("Recording")) {
			tcid = writeIncTestCaseToDB(testCase, xlsLoc, version, createdBy, saveType, randomKey, moduleId, width,
					height);
		} else {
			tcid = writeTestCaseToDB(testCase, xlsLoc, version, createdBy, saveType, randomKey, moduleId, width,
					height);
		}
		int cid = getCompanyFromModule(moduleId);
		String licence = getLicenceToken(cid);
		if (getBulkTestStatus(cid)) {
			String url = AppProperties.javareacturl + "?";

			JSONArray jsonArray = new JSONArray();

			JSONObject param1 = new JSONObject();
			param1.put("key", "action");
			param1.put("value", "triggerBulkApi");
			jsonArray.add(param1);

			JSONObject param2 = new JSONObject();
			param2.put("key", "token");
			param2.put("value", licence);
			jsonArray.add(param2);

			JSONObject param3 = new JSONObject();
			param3.put("key", "companyid");
			param3.put("value", Integer.toString(cid));
			jsonArray.add(param3);

			JSONObject param4 = new JSONObject();
			param4.put("key", "key");
			param4.put("value", Long.toString(randomKey));
			jsonArray.add(param4);

			addApi(testCase + "_MasterApi", 1, 0, moduleId, null, "GET", null, url, jsonArray, Long.toString(randomKey),
					null, "1", null, -1, null);
		}
		return tcid;
	}

	public int writeTestCaseToDB(String testCase, String xlsLoc, int version, String createdBy, String saveType,
			long randomKey, int moduleId, int width, int height) {

		String sql = "INSERT INTO cta.test_case (Test_Case, XLS_Location, Version, Created_By,"
				+ "Created_Date, SaveType, extKey, Status, Module, width, height) "
				+ "VALUES (?, ?, ?, ?, ?, ?, ?,?, ?, ?, ?)";
		int pk = -1;

		SimpleDateFormat dateFormat = new SimpleDateFormat("dd:MM:yyyy HH:mm:ss");
		String timestamp = dateFormat.format(new java.util.Date());

		try {
			PreparedStatement stmt = testCon.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);

			// Set the values for the prepared statement
			stmt.setString(1, testCase);
			stmt.setString(2, xlsLoc);
			stmt.setInt(3, version);
			stmt.setString(4, createdBy);
			stmt.setTimestamp(5, Utilities.getCurrentTimestamp());
			stmt.setString(6, saveType);
			stmt.setLong(7, randomKey);
			stmt.setInt(8, 0);
			stmt.setInt(9, moduleId);
			stmt.setInt(10, width);
			stmt.setInt(11, height);

			// Execute the prepared statement
			stmt.executeUpdate();

			ResultSet generatedKeys = stmt.getGeneratedKeys();
			if (generatedKeys.next()) {
				pk = (generatedKeys.getBigDecimal(1)).intValue();
			}

			generatedKeys.close();
			stmt.close();

		} catch (Exception e) {
			System.err.println(Utilities.getNow());
			e.printStackTrace();
		}
		DashBoardData dbd = new DashBoardData(this);
		dbd.updateDashBoardForTCCount(pk);
		return pk;
	}

	public void closeDbConn() {
		try {

//			Context initContext = new InitialContext();
//	        Context envContext  = (Context) initContext.lookup("java:/comp/env");
//	        DataSource ds = (DataSource) envContext.lookup("jdbc/cta");

			// Cast to Tomcat's DataSource
//            org.apache.tomcat.jdbc.pool.DataSource tomcatDataSource = (org.apache.tomcat.jdbc.pool.DataSource) ds;

			// Now you can get various stats
//            System.out.println("------------------------------------------------------------------------------");
//            System.out.println("Active Connections before close MSC: " + tomcatDataSource.getActive());
//            System.out.println("Idle Connections before close MSC : " + tomcatDataSource.getIdle());
//            
//			// Step 6: Close the connection
//            printCallStack(3);
//            System.out.println("closing Connection MSC: " + testCon.hashCode());
			testCon.close();

//			initContext = new InitialContext();
//	        envContext  = (Context) initContext.lookup("java:/comp/env");
//	        ds = (DataSource) envContext.lookup("jdbc/cta");
//			
//			// Cast to Tomcat's DataSource
//            tomcatDataSource = (org.apache.tomcat.jdbc.pool.DataSource) ds;
//
//            // Now you can get various stats
//            System.out.println("Active Connections after close MSC: " + tomcatDataSource.getActive());
//            System.out.println("Idle Connections after close MSC: " + tomcatDataSource.getIdle());
//            System.out.println("------------------------------------------------------------------------------");
		} catch (SQLException e) {
			System.err.println(Utilities.getNow());
			e.printStackTrace();
		}
//		} catch (NamingException e) {
//			e.printStackTrace();
//		}
	}

	public void printCallStack(int i) {
		StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
		for (int j = 0; j < 10 && j < stackTrace.length; j++) {
			System.err.println("call stack MSC: " + stackTrace[j].toString());
		}
	}

	public void validateFromOnScreen(int teststepid, String validateType) {

		String sql = null;
		sql = "SELECT * FROM  cta.test_step WHERE  " + "idtest_step = ?";
		try {
			PreparedStatement stmt = testCon.prepareStatement(sql);
			stmt.setInt(1, teststepid);
			ResultSet rs = stmt.executeQuery();
			String recordedData = "";
			if (rs.next()) {
				JSONObject prevRow = getTestStepsFromDb(rs, false, false);
				recordedData = (String) prevRow.get("RecordedData");
			}

			sql = "UPDATE cta.test_step SET ValDevice = ?, TestData = ?" + " WHERE  idtest_step = ?";
			stmt = testCon.prepareStatement(sql);
			stmt.setString(1, validateType);
			stmt.setString(2, recordedData);
			stmt.setInt(3, teststepid);
			stmt.executeUpdate();

			rs.close();
			stmt.close();
		} catch (Exception e) {
			System.err.println(Utilities.getNow());
			e.printStackTrace();
		}
	}

	public void updateTestStepIsAVarValidate(int teststepid, String validate) {

		String varName = Utilities.randomGen("ALPHA", 6, AppProperties.chatgpton);
		varName = AppProperties.delimiter + varName + AppProperties.delimiter;

		try {

			String sql = "SELECT RecordedData FROM cta.test_step " + " WHERE  idtest_step = ?";
			PreparedStatement stmt = testCon.prepareStatement(sql);
			stmt.setInt(1, teststepid);
			ResultSet rs = stmt.executeQuery();

			if (rs.next()) {
				String orgTD = rs.getString("RecordedData");
				sql = "UPDATE cta.test_step SET VarName = ?" + " WHERE  idtest_step = ?";
				stmt = testCon.prepareStatement(sql);
				stmt.setString(1, varName);
				stmt.setInt(2, teststepid);
				stmt.executeUpdate();

				rs.close();
				stmt.close();
			}
		} catch (Exception e) {
			System.err.println(Utilities.getNow());
			e.printStackTrace();
		}
	}

public void clearStorage(int companyId, java.sql.Date beforeDate) {
	
	try {
		
		String sql = "SELECT lastclearedtcr FROM cta.company "
				+ " WHERE  idcompany = ?";
		PreparedStatement stmt = testCon.prepareStatement(sql);
		stmt.setInt(1, companyId);
		ResultSet rs = stmt.executeQuery();
		
		int lasttcr = 0;
		if(rs.next()) {
			lasttcr = rs.getInt(1);
		}
		
		sql = "SELECT * FROM cta.test_case_results "
				+ " WHERE  Executed_Date <= ? and idtest_case_results > ?";
		stmt = testCon.prepareStatement(sql);
		stmt.setDate(1, beforeDate);
		stmt.setInt(2, lasttcr);
		rs = stmt.executeQuery();
		
		int count = 0;
		int tcrid = -1;
		
		System.err.println(Utilities.getCurrentTimestamp() + "Start CleanUp");
		while(rs.next()) {
			int tcid = rs.getInt("Test_Case_Id");
			tcrid = rs.getInt("idtest_case_results");
			String ssdir = Utilities.getScreenShotsDir(companyId, tcid, tcrid);
			Utilities.deleteFolderContents(ssdir);
			Utilities.recreateFolder(ssdir);
			System.err.println(Utilities.getCurrentTimestamp() + "Screeshots folder deleted for - test case result : " +  tcrid);
			
			try {
				String psdir = Utilities.getPageSourceDir(companyId, tcid, tcrid);
				Utilities.deleteFolderContents(psdir);
				Utilities.recreateFolder(psdir);
				System.err.println(Utilities.getCurrentTimestamp() + "pagesource folder deleted for - test case result : " +  tcrid);
			}catch(Exception e) {
				//ignore
			}
			
			count = count + 1;
			
			if(count >= 100) {
				sql = "UPDATE cta.company set lastclearedtcr = ?, storageclearedTillDate = ?,"
						+ "storageClearedOn = ? WHERE  idcompany = ?";
				PreparedStatement stmt2 = testCon.prepareStatement(sql);
				stmt2.setInt(1, tcrid);
				stmt2.setDate(2, beforeDate);
				stmt2.setTimestamp(3, Utilities.getCurrentTimestamp());
				stmt2.setInt(4, companyId);
				stmt2.executeUpdate();
				count = 0;
				stmt2.close();
			}
		}
		
		try {
			sql = "UPDATE cta.company set lastclearedtcr = ?, storageclearedTillDate = ?,"
					+ "storageClearedOn = ?,storageclearancesuccessful = 1 WHERE  idcompany = ?";
			PreparedStatement stmt2 = testCon.prepareStatement(sql);
			stmt2.setInt(1, tcrid);
			stmt2.setDate(2, beforeDate);
			stmt2.setTimestamp(3, Utilities.getCurrentTimestamp());
			stmt2.setInt(4, companyId);
			stmt2.executeUpdate();
			stmt2.close();
		} catch (Exception ex) {
			System.err.println(Utilities.getNow());
			ex.printStackTrace();
		}
		
		rs.close();
		stmt.close();
	} catch(Exception e) {
		try {
			String sql = "UPDATE cta.company set storageclearancesuccessful = 0 WHERE  idcompany = ?";
			PreparedStatement stmt2 = testCon.prepareStatement(sql);
			stmt2.setInt(1, companyId);
			stmt2.executeUpdate();
			stmt2.close();
		} catch (Exception ex) {
			
		}
		System.err.println(Utilities.getNow());
		e.printStackTrace();
	}
	System.err.println(Utilities.getCurrentTimestamp() + "Cleanup Ended");
}

	public void validateTestStepFromEmail(int teststepid, String testdatasource, String custEmail, String custPwd,
			String SelectionCriterion, String Filter) {
		String sql = null;
		try {
			sql = "UPDATE cta.test_step SET ValDevice = ?, customerEmail = ?, customerPassword = ?, EmailSelectionCriteria = ?, EmailFilter = ?"
					+ " WHERE idtest_step = ?";
			PreparedStatement stmt = testCon.prepareStatement(sql);
			stmt.setString(1, testdatasource);
			stmt.setString(2, custEmail);
			stmt.setString(3, custPwd);
			stmt.setString(4, SelectionCriterion);
			stmt.setString(5, Filter);
			stmt.setInt(6, teststepid);
			stmt.executeUpdate();
			stmt.close();
		} catch (Exception e) {
			System.err.println(Utilities.getNow());
			e.printStackTrace();
		}
	}

	public void validateTestStepFromOutlook(int teststepid, String testdatasource, String SelectionCriterion,
			String Filter) {
		String sql = null;
		try {
			sql = "UPDATE cta.test_step SET ValDevice = ?, EmailSelectionCriteria = ?, "
					+ "EmailFilter = ?  WHERE idtest_step = ?";
			PreparedStatement stmt = testCon.prepareStatement(sql);
			stmt.setString(1, testdatasource);
			stmt.setString(2, SelectionCriterion);
			stmt.setString(3, Filter);
			stmt.setInt(4, teststepid);
			stmt.executeUpdate();
			stmt.close();
		} catch (Exception e) {
			System.err.println(Utilities.getNow());
			e.printStackTrace();
		}
	}

	public void validateFromDynamicText(int teststepid, String validateType) {

		String sql = null;
		sql = "SELECT * FROM  cta.test_step WHERE  " + "idtest_step = ?";
		try {
			PreparedStatement stmt = testCon.prepareStatement(sql);
			stmt.setInt(1, teststepid);
			ResultSet rs = stmt.executeQuery();
			String recordedData = "";
			if (rs.next()) {
				JSONObject prevRow = getTestStepsFromDb(rs, false, false);
				recordedData = (String) prevRow.get("RecordedData");
			}

			sql = "UPDATE cta.test_step SET TestData = ?, ValDevice = ?" + " WHERE  idtest_step = ?";
			stmt = testCon.prepareStatement(sql);
			stmt.setString(1, AppProperties.validatedelimiter + validateType + AppProperties.validatedelimiter);
			stmt.setString(2, validateType);
			stmt.setInt(3, teststepid);
			stmt.executeUpdate();

			rs.close();
			stmt.close();
		} catch (Exception e) {
			System.err.println(Utilities.getNow());
			e.printStackTrace();
		}
	}

	public void validateByRegex(int teststepid, String validateType, String regex) {

		String sql = null;
		sql = "SELECT * FROM  cta.test_step WHERE  " + "idtest_step = ?";
		try {
			PreparedStatement stmt = testCon.prepareStatement(sql);
			stmt.setInt(1, teststepid);
			ResultSet rs = stmt.executeQuery();
			String recordedData = "";
			if (rs.next()) {
				JSONObject prevRow = getTestStepsFromDb(rs, false, false);
				recordedData = (String) prevRow.get("RecordedData");
			}

			sql = "UPDATE cta.test_step SET ValDevice = ?, TestData = ?, regex = ?" + " WHERE  idtest_step = ?";
			stmt = testCon.prepareStatement(sql);
			stmt.setString(1, validateType);
			stmt.setString(2, AppProperties.validatedelimiter + "REGEX" + AppProperties.validatedelimiter);
			stmt.setString(3, regex);
			stmt.setInt(4, teststepid);
			stmt.executeUpdate();

			rs.close();
			stmt.close();
		} catch (Exception e) {
			System.err.println(Utilities.getNow());
			e.printStackTrace();
		}
	}

	public void validateByDate(int teststepid, String validateType, String date, String dateFormat) {

		try {
			String sql = "UPDATE cta.test_step SET ValDevice = ?, TestData = ?" + " WHERE  idtest_step = ?";
			PreparedStatement stmt = testCon.prepareStatement(sql);
			stmt.setString(1, validateType);
			stmt.setString(2, AppProperties.validatedelimiter + "DATE_" + AppProperties.validatedelimiter + date
					+ AppProperties.validatedelimiter + dateFormat + AppProperties.validatedelimiter);
			stmt.setInt(3, teststepid);
			stmt.executeUpdate();
			stmt.close();
		} catch (Exception e) {
			System.err.println(Utilities.getNow());
			e.printStackTrace();
		}
	}

	public void validateByDependence(int teststepid, String validateType, String useVar, int depTC) {

		try {

			String sql = "UPDATE cta.test_step SET TestData = ?, dependantTC = ?, ValDevice = ?"
					+ " WHERE  idtest_step = ?";
			PreparedStatement stmt = testCon.prepareStatement(sql);
			stmt.setString(1, useVar);
			stmt.setInt(2, depTC);
			stmt.setString(3, validateType);
			stmt.setInt(4, teststepid);
			stmt.executeUpdate();

			int tcid = gettcidfromTestStep(teststepid);
			turnOnContinueTest(tcid);
			stmt.close();
		} catch (Exception e) {
			System.err.println(Utilities.getNow());
			e.printStackTrace();
		}
	}

	public int getTestStep(JSONObject dataBeforeDeletion, int tsid) {
		for (int i = 2; i <= dataBeforeDeletion.size(); i++) {
			JSONObject data2BeforeDeletion = (JSONObject) dataBeforeDeletion.get(i);
			int teststepid = (int) data2BeforeDeletion.get("idtest_step");
			if (teststepid == tsid) {
				String str = (String) data2BeforeDeletion.get("tsSequence");
				return Integer.valueOf(str);
			}
		}
		return -1;
	}

	public void updateDependantTC(int tcid, int depTC, JSONArray delTestSteps) {

		try {

			deleteForReplace(tcid, delTestSteps);
			JSONObject testSteps = getTestStepsByTestCaseID(tcid, false);
			JSONObject data = (JSONObject) testSteps.get("data");
			JSONObject data2 = (JSONObject) data.get(2);
			int teststepid = (int) data2.get("idtest_step");

			String sql = "UPDATE cta.test_step SET dependantTC = ?" + " WHERE  idtest_step = ?";
			PreparedStatement stmt = testCon.prepareStatement(sql);

			stmt.setInt(1, depTC);
			stmt.setInt(2, teststepid);
			stmt.executeUpdate();

//			String sqlct = "UPDATE cta.test_case SET continuetest = 1 "
//					+ " where idtest_case = ?";
//			stmt = testCon.prepareStatement(sqlct);
//
//			stmt.setInt(1, tcid);
//			stmt.executeUpdate();
			turnOnContinueTest(tcid);
			stmt.close();
		} catch (Exception e) {
			System.err.println(Utilities.getNow());
			e.printStackTrace();
		}
	}

	public void turnOnContinueTest(int tcid) {
		String sqlct = "UPDATE cta.test_case SET continuetest = 1 " + " where idtest_case = ?";
		try {
			PreparedStatement stmt = testCon.prepareStatement(sqlct);
			stmt.setInt(1, tcid);
			stmt.executeUpdate();
			stmt.close();
		} catch (Exception e) {
			System.err.println(Utilities.getNow());
			e.printStackTrace();
		}
	}

	public void deleteForReplace(int tcid, JSONArray delTestSteps) {
		JSONObject testStepsBeforeDeletion = getTestStepsByTestCaseID(tcid, false);
		JSONObject dataBeforeDeletion = (JSONObject) testStepsBeforeDeletion.get("data");

		deleteTestSteps(delTestSteps);

		int tsidsmallest = -1;
		int tsidbiggest = -1;
		int tsseqsmallest = -1;
		int tsseqbiggest = -1;

		for (int i = 0; i < delTestSteps.size(); i++) {
			long tsidl = (long) delTestSteps.get(i);
			int tsid = (int) tsidl;
			if (i == 0) {
				tsidsmallest = tsid;
				tsidbiggest = tsid;
				tsseqsmallest = getTestStep(dataBeforeDeletion, tsid);
				tsseqbiggest = tsseqsmallest;
			} else {
				if (tsid < tsidsmallest) {
					tsidsmallest = tsid;
					tsseqsmallest = getTestStep(dataBeforeDeletion, tsid);
				} else if (tsid > tsidbiggest) {
					tsidbiggest = tsid;
					tsseqbiggest = getTestStep(dataBeforeDeletion, tsid);
				}
			}
		}

		JSONObject testSteps = getTestStepsByTestCaseID(tcid, false);
		JSONObject data = (JSONObject) testSteps.get("data");
		for (int i = 2; i <= data.size(); i++) {
			JSONObject data2 = (JSONObject) data.get(i);
			int teststepid = (int) data2.get("idtest_step");
			String tsseqstr = (String) data2.get("tsSequence");
			int tsseq = Integer.valueOf(tsseqstr);
			if ((teststepid >= tsidsmallest && teststepid <= tsidbiggest)
					|| (tsseq >= tsseqsmallest && tsseq <= tsseqbiggest)) {
				deleteTestStep(teststepid);
			}
		}
	}

	public void updateInlineTC(int tcid, int depTC, JSONArray delTestSteps) {

		try {
			long tsidl = (long) delTestSteps.get(0);
			int firsttsid = (int) tsidl;

			JSONObject testStepsBeforeDeletion = getTestStepsByTestCaseID(tcid, false);
			JSONObject dataBeforeDeletion = (JSONObject) testStepsBeforeDeletion.get("data");

			int stepnumber = -1;
			JSONObject prevStep = null;
			for (int i = 2; i <= dataBeforeDeletion.size(); i++) {
				JSONObject data2 = (JSONObject) dataBeforeDeletion.get(i);
				int teststepid = (int) data2.get("idtest_step");
				if (firsttsid == teststepid) {
					String tsseqstr = (String) data2.get("tsSequence");
					stepnumber = Integer.valueOf(tsseqstr);
					break;
				}
				prevStep = data2;
			}

			String tsseqstr = (String) prevStep.get("tsSequence");
			int prevstepnumber = Integer.valueOf(tsseqstr);

			deleteForReplace(tcid, delTestSteps);

			addTestStep("Inline", "Test", "Positive", "Inline Dependence", "Inline Dependence", String.valueOf(depTC),
					tcid, null, stepnumber, "", "Before", 0, 0, "", null, firsttsid, prevstepnumber);
		} catch (Exception e) {
			System.err.println(Utilities.getNow());
			e.printStackTrace();
		}
	}

	public void validateByVar(int teststepid, String validateType, String useVar) {

		try {
			String sql = "UPDATE cta.test_step SET TestData = ?, ValDevice = ? " + " WHERE  idtest_step = ?";
			PreparedStatement stmt = testCon.prepareStatement(sql);
			stmt.setString(1, useVar);
			stmt.setString(2, validateType);
			stmt.setInt(3, teststepid);
			stmt.executeUpdate();
			stmt.close();
		} catch (Exception e) {
			System.err.println(Utilities.getNow());
			e.printStackTrace();
		}
	}

	public String searchOnPageXpath(String testData) {
		return "//*[contains(text(),'" + testData + "')]";
	}

	public void searchForTextValidate(int teststepid, String validateType, String testData, String strategy) {

		try {
			String sql = "UPDATE cta.test_step SET ValDevice = ? WHERE idtest_step = ?";
			String sql_atr = "UPDATE cta.test_step_attr SET locationstrategy = ?, searchpageXpath = ? WHERE test_step = ?";
			PreparedStatement stmt = testCon.prepareStatement(sql);
			String xpath = searchOnPageXpath(testData);
			stmt.setString(1, validateType);
			stmt.setInt(2, teststepid);
			stmt.executeUpdate();
			stmt = testCon.prepareStatement(sql_atr);
			stmt.setString(1, strategy);
			stmt.setString(2, xpath);
			stmt.setInt(3, teststepid);
			stmt.close();
		} catch (Exception e) {
			System.err.println(Utilities.getNow());
			e.printStackTrace();
		}
	}

	public void searchForTextTestData(int teststepid, String testData, String testDataSource, String strategy) {

		try {
			String sql = "UPDATE cta.test_step SET testdata_source = ? WHERE idtest_step = ?";
			String sql_atr = "UPDATE cta.test_step_attr SET locationstrategy = ?, searchpageXpath = ? WHERE test_step = ?";
			PreparedStatement stmt = testCon.prepareStatement(sql);
			String xpath = searchOnPageXpath(testData);
			stmt.setString(1, testDataSource);
			stmt.setInt(2, teststepid);
			stmt.executeUpdate();
			stmt = testCon.prepareStatement(sql_atr);
			stmt.setString(1, strategy);
			stmt.setString(2, xpath);
			stmt.setInt(3, teststepid);
			stmt.executeUpdate();
			stmt.close();
		} catch (Exception e) {
			System.err.println(Utilities.getNow());
			e.printStackTrace();
		}
	}

	public void updateXpathAsStrategy(int teststepid, String objPath, String testDataSource, String strategy) {

		try {
			String sql = "UPDATE cta.test_step SET testdata_source = ?, Object_Xpath = ? WHERE idtest_step = ?";
			String sql_atr = "UPDATE cta.test_step_attr SET locationstrategy = ? WHERE test_step = ?";
			PreparedStatement stmt = testCon.prepareStatement(sql);
			stmt.setString(1, testDataSource);
			stmt.setString(2, objPath);
			stmt.setInt(3, teststepid);
			stmt.executeUpdate();
			stmt = testCon.prepareStatement(sql_atr);
			stmt.setString(1, strategy);
			stmt.setInt(2, teststepid);
			stmt.executeUpdate();
			stmt.close();
		} catch (Exception e) {
			System.err.println(Utilities.getNow());
			e.printStackTrace();
		}
	}

	public void validateFromFile(int teststepid, String validateType, String fileName, String fileField, String scope,
			String endrange) {
		String sql = null;
		try {
			sql = "UPDATE cta.test_step SET ValDevice = ?, filename = ?, fileField = ?, TestData = ?"
					+ " WHERE idtest_step = ?";
			String sql_atrr = "UPDATE cta.test_step_attr SET scope=?, endrange=? WHERE test_step=?";
			PreparedStatement stmt = testCon.prepareStatement(sql);
			stmt.setString(1, validateType);
			stmt.setString(2, fileName);
			stmt.setString(3, fileField);
			if (fileField != null && !fileField.equals("") && !fileField.equals("null")) {
				stmt.setString(4, fileName + AppProperties.validatedelimiter + fileField);
			} else {
				stmt.setString(4, fileName);
			}
			stmt.setInt(5, teststepid);
			stmt.executeUpdate();

			stmt = testCon.prepareStatement(sql_atrr);
			stmt.setString(1, scope);
			stmt.setString(2, endrange);
			stmt.setInt(3, teststepid);
			stmt.executeUpdate();
			stmt.close();
		} catch (Exception e) {
			System.err.println(Utilities.getNow());
			e.printStackTrace();
		}
	}

	public void validateFromFile(int teststepid, String testdatasource, String fileName) {
		String sql = null;
		try {
			sql = "UPDATE cta.test_step SET ValDevice = ?, " + "filename = ?, TestData = ? WHERE idtest_step = ?";
			PreparedStatement stmt = testCon.prepareStatement(sql);
			stmt.setString(1, testdatasource);
			stmt.setString(2, fileName);
			stmt.setString(3, fileName);
			stmt.setInt(4, teststepid);
			stmt.executeUpdate();
			stmt.close();
		} catch (Exception e) {
			System.err.println(Utilities.getNow());
			e.printStackTrace();
		}
	}

	public void validateFromDownloadedFile(int teststepid, String validateType, String fileName, String fileField) {
		String sql = null;
		try {
			sql = "UPDATE cta.test_step SET ValDevice = ?, filename = ?, filefield = ?" + " WHERE idtest_step = ?";
			PreparedStatement stmt = testCon.prepareStatement(sql);
			stmt.setString(1, validateType);
			stmt.setString(2, fileName);
			stmt.setString(3, fileField);
			stmt.setInt(4, teststepid);
			stmt.executeUpdate();
			stmt.close();
		} catch (Exception e) {
			System.err.println(Utilities.getNow());
			e.printStackTrace();
		}
	}

	public void validateFromStreamedPDFFile(int teststepid, String validateType, String fileName) {
		String sql = null;
		try {
			sql = "UPDATE cta.test_step SET ValDevice = ?, filename = ?" + " WHERE idtest_step = ?";
			PreparedStatement stmt = testCon.prepareStatement(sql);
			stmt.setString(1, validateType);
			stmt.setString(2, fileName);
			stmt.setInt(3, teststepid);
			stmt.executeUpdate();
			stmt.close();
		} catch (Exception e) {
			System.err.println(Utilities.getNow());
			e.printStackTrace();
		}
	}

	public void validateFromRefrenceFile(int teststepid, String validateType, String fileName, String fileField,
			String scope, String endrange) {
		String sql = null;
		try {
			sql = "UPDATE cta.test_step SET ValDevice = ?, filename = ?, fileField = ?, TestData = ?"
					+ " WHERE idtest_step = ?";
			String sql_atrr = "UPDATE cta.test_step_attr SET scope=?, endrange=? WHERE test_step=?";
			PreparedStatement stmt = testCon.prepareStatement(sql);
			stmt.setString(1, validateType);
			stmt.setString(2, fileName);
			stmt.setString(3, fileField);
			if (fileField != null && !fileField.equals("") && !fileField.equals("null")) {
				stmt.setString(4, fileName + AppProperties.validatedelimiter + fileField);
			} else {
				stmt.setString(4, fileName);
			}
			stmt.setInt(5, teststepid);
			stmt.executeUpdate();

			stmt = testCon.prepareStatement(sql_atrr);
			stmt.setString(1, scope);
			stmt.setString(2, endrange);
			stmt.setInt(3, teststepid);
			stmt.executeUpdate();
			stmt.close();
		} catch (Exception e) {
			System.err.println(Utilities.getNow());
			e.printStackTrace();
		}
	}

	public void validateFromRefrenceFile(int teststepid, String testdatasource, String fileName) {
		String sql = null;
		try {
			sql = "UPDATE cta.test_step SET ValDevice = ?, " + "filename = ?, TestData = ? WHERE idtest_step = ?";
			PreparedStatement stmt = testCon.prepareStatement(sql);
			stmt.setString(1, testdatasource);
			stmt.setString(2, fileName);
			stmt.setString(3, fileName);
			stmt.setInt(4, teststepid);
			stmt.executeUpdate();
			stmt.close();
		} catch (Exception e) {
			System.err.println(Utilities.getNow());
			e.printStackTrace();
		}
	}

	public void validateFromSheet(int teststepid, String validateType, String sheetid, String fileField) {
		String sql = null;
		try {
			sql = "UPDATE cta.test_step SET ValDevice = ?, filename = ?, fileField = ?, TestData = ?"
					+ " WHERE idtest_step = ?";
			PreparedStatement stmt = testCon.prepareStatement(sql);
			stmt.setString(1, validateType);
			stmt.setString(2, sheetid);
			stmt.setString(3, fileField);
			if (fileField != null && !fileField.equals("") && !fileField.equals("null")) {
				stmt.setString(4, sheetid + AppProperties.testdatadelimiter + fileField);
			} else {
				stmt.setString(4, sheetid);
			}
			stmt.setInt(5, teststepid);
			stmt.executeUpdate();
			stmt.close();
		} catch (Exception e) {
			System.err.println(Utilities.getNow());
			e.printStackTrace();
		}
	}

	public void validateFromDB(int teststepid, String validateType, String DBQuery) {
		String sql = null;
		try {
			sql = "UPDATE cta.test_step SET ValDevice = ?, DBUrl = ?, DBQuery = ?, DBType = ?"
					+ " WHERE idtest_step = ?";
			int prodId = getProdIdFromTestStepId(teststepid);
			JSONObject json = getDbConnectionDetails(prodId);
			PreparedStatement stmt = testCon.prepareStatement(sql);
			stmt.setString(1, validateType);
			stmt.setString(2, (String) json.get("custDbUrl"));
			stmt.setString(3, DBQuery);
			stmt.setString(4, (String) json.get("custDbType"));
			stmt.setInt(5, teststepid);
			stmt.executeUpdate();
			stmt.close();
		} catch (Exception e) {
			System.err.println(Utilities.getNow());
			e.printStackTrace();
		}
	}

	public JSONArray getTestSuites(int pid, int flag) {

		String sql = "SELECT * FROM cta.test_suite WHERE productid = ? AND status = '0' "
				+ "GROUP BY idtest_suite, Test_Suite";
		JSONArray result = new JSONArray();
		try {
			PreparedStatement stmt = testCon.prepareStatement(sql);
			stmt.setInt(1, pid);
			ResultSet rs = stmt.executeQuery();
			JSONArray prod = null;
			while (rs.next()) {
				prod = getTestSuitesAsJSON(rs, flag);
				result.add(prod);
			}
			rs.close();
			stmt.close();
		} catch (Exception e) {
			System.err.println(Utilities.getNow());
			e.printStackTrace();
		}

		return result;
	}

	public JSONObject getTestSuite(int testSuiteId, int flag) {
		String sql = "SELECT * FROM cta.test_suite where idtest_suite = ?";
		JSONObject result = new JSONObject();
		try {
			PreparedStatement stmt = testCon.prepareStatement(sql);
			stmt.setInt(1, testSuiteId);
			ResultSet rs = stmt.executeQuery();
			while (rs.next()) {
				JSONArray ja = getTestSuitesAsJSON(rs, flag);
				result = (JSONObject) ja.get(0);
			}
			rs.close();
			stmt.close();
		} catch (Exception e) {
			System.err.println(Utilities.getNow());
			e.printStackTrace();
		}

		return result;
	}

	public JSONArray getAllTestCasesFromDBForTestSuiteEx(int idtest_suite) {

		// to count the object sequence
		String count_sql = "SELECT Count(*) FROM cta.test_suite_case_map WHERE Test_Suite_Id = ?";

		String sql = "SELECT Object_Id, Object_Type, Object_Index FROM cta.test_suite_case_map "
				+ "WHERE Test_Suite_Id = ? ORDER BY Object_Index";
//will give object id, object type and object sequence "SELECT object_id, object_type, object_sequence FROM cta.testsuite_objectsequence WHERE parent_suite_id = 1 ORDER BY object_sequence"
		JSONObject json = null;
		JSONArray jsonArr = new JSONArray();
		String limitStr = AppProperties.listlimit;
		int limit = Integer.parseInt(limitStr);
		int index = 0;

		try {
			PreparedStatement stmt = testCon.prepareStatement(count_sql);
			stmt.setInt(1, idtest_suite);
			ResultSet resultSet1 = stmt.executeQuery();
			resultSet1.next();
			int objectCount = resultSet1.getInt(1); // Total Object count
			stmt = testCon.prepareStatement(sql);
			stmt.setInt(1, idtest_suite);
			ResultSet resultSet = stmt.executeQuery();

			while (resultSet.next()) {
				int objectId = resultSet.getInt("Object_Id"); // resultSet.getInt("object_id");
				int objectIndex = resultSet.getInt("Object_Index"); // resultSet.getInt("object_sequence");
				String objectType = resultSet.getString("Object_Type");
				if (objectType.equals("case")) {
					sql = "SELECT * FROM cta.test_case WHERE idtest_case = ? AND Status != 1";
					stmt = testCon.prepareStatement(sql);
					stmt.setInt(1, objectId);
					ResultSet resultSet2 = stmt.executeQuery();

					json = getTestCasesAsJSON(resultSet2);
					json.put("TestSuiteId", idtest_suite);
					json.put("TestcaseIndex", objectIndex);
					json.put("totalPages", (objectCount == limit ? (objectCount / limit) : (objectCount / limit) + 1));
					jsonArr.add(json);
					resultSet2.close();
				} else if (objectType.equals("suite")) {
					json = getTestSuite(objectId, 0);
					String testsuite = (String) json.get("Test_Suite");
					int tsid = ((Integer) json.get("idtest_suite")).intValue();
					json.put("Test_Case", "Suite:" + testsuite);
					json.put("idtest_case", tsid);
					JSONObject JSONarr = new JSONObject();
					JSONarr.put(0, json);
					JSONObject JSONData = new JSONObject();
					JSONData.put("testcases", JSONarr);
					JSONData.put("testsuite", "true");
					JSONData.put("TestcaseIndex", objectIndex);
					JSONData.put("totalPages",
							(objectCount == limit ? (objectCount / limit) : (objectCount / limit) + 1));
					jsonArr.add(JSONData);
				}
			}
			resultSet.close();
			resultSet1.close();
			stmt.close();
		} catch (Exception e) {
			System.err.println(Utilities.getNow());
			e.printStackTrace();
		}
		return jsonArr;
	}

	public JSONArray getAllTestCasesFromDBForTestSuite(int idtest_suite, int pnum) {

		// to count the object sequence
		String count_sql = "SELECT Count(*) FROM cta.test_suite_case_map WHERE Test_Suite_Id = ?";

		String sql = "SELECT idTest_Suite_Case_Map, Object_Id, Object_Type, Object_Index FROM cta.test_suite_case_map "
				+ "WHERE Test_Suite_Id = ? ORDER BY Object_Index LIMIT ? OFFSET ?";
//will give object id, object type and object sequence "SELECT object_id, object_type, object_sequence FROM cta.testsuite_objectsequence WHERE parent_suite_id = 1 ORDER BY object_sequence"
		JSONObject json = null;
		JSONArray jsonArr = new JSONArray();
		String limitStr = AppProperties.listlimit;
		int limit = Integer.parseInt(limitStr);
		int offset = (pnum - 1) * limit;
		int index = 0;

		try {
			PreparedStatement stmt = testCon.prepareStatement(count_sql);
			stmt.setInt(1, idtest_suite);
			ResultSet resultSet1 = stmt.executeQuery();
			resultSet1.next();
			int objectCount = resultSet1.getInt(1); // Total Object count
			stmt = testCon.prepareStatement(sql);
			stmt.setInt(1, idtest_suite);
			stmt.setInt(2, limit);
			stmt.setInt(3, offset);
			ResultSet resultSet = stmt.executeQuery();

			while (resultSet.next()) {
				int objectId = resultSet.getInt("Object_Id"); // resultSet.getInt("object_id");
				int objectIndex = resultSet.getInt("Object_Index"); // resultSet.getInt("object_sequence");
				String objectType = resultSet.getString("Object_Type");
				int key = resultSet.getInt("idTest_Suite_Case_Map");
				if (objectType.equals("case")) {
					sql = "SELECT * FROM cta.test_case WHERE idtest_case = ? AND Status != 1";
					stmt = testCon.prepareStatement(sql);
					stmt.setInt(1, objectId);
					ResultSet resultSet2 = stmt.executeQuery();

					json = getTestCasesAsJSON(resultSet2);
					json.put("TestSuiteId", idtest_suite);
					json.put("TestcaseIndex", objectIndex);
					json.put("totalPages", (objectCount == limit ? (objectCount / limit) : (objectCount / limit) + 1));
					jsonArr.add(json);
					resultSet2.close();
				} else if (objectType.equals("suite")) {
					json = getTestSuite(objectId, 1);
					String testsuite = (String) json.get("Test_Suite");
					int tsid = ((Integer) json.get("idtest_suite")).intValue();
					json.put("Test_Case", "Suite:" + testsuite);
					json.put("idtest_case", tsid);
					JSONObject JSONarr = new JSONObject();
					JSONarr.put(0, json);
					JSONObject JSONData = new JSONObject();
					JSONData.put("testcases", JSONarr);
					JSONData.put("testsuite", "true");
					JSONData.put("TestcaseIndex", objectIndex);
					JSONData.put("totalPages",
							(objectCount == limit ? (objectCount / limit) : (objectCount / limit) + 1));
					jsonArr.add(JSONData);
				}

				String usql = "UPDATE cta.test_suite_case_map SET Object_Index = ? " + "WHERE idTest_Suite_Case_Map=? ";
				PreparedStatement ustmt = testCon.prepareStatement(usql);
				ustmt.setInt(1, offset + index);
				ustmt.setInt(2, key);
				ustmt.execute();
				ustmt.close();
				index = index + 1;
			}

			resultSet.close();
			stmt.close();
		} catch (Exception e) {
			System.err.println(Utilities.getNow());
			e.printStackTrace();
		}
		return jsonArr;
	}

	public JSONArray createTestSuite(String tsuname, int prodid, int cid) {
		String Executed_By = (String) userDetails.get("uname");
		String create_testSuite = "INSERT INTO cta.test_suite(Test_Suite, Version, Created_By, Created_Date, productid, status, "
				+ "width, height, browser, envurl, stopafterfailure, rerunfailedtests, supports_parallel_execution) VALUES "
				+ "(?, ?, ?, ?, ?, 0, 0, 0, 'Chrome', '', 'true', 'false', 'false')";
		JSONArray jsonArr = new JSONArray();
		try {
			PreparedStatement stmt_ts = testCon.prepareStatement(create_testSuite);
			stmt_ts.setString(1, tsuname);
			stmt_ts.setInt(2, 1);
			stmt_ts.setString(3, Executed_By);
			stmt_ts.setTimestamp(4, Utilities.getCurrentTimestamp());
			stmt_ts.setInt(5, prodid);
			stmt_ts.executeUpdate();
			jsonArr = getTestSuites(prodid, 1);
			stmt_ts.close();
		} catch (Exception e) {
			System.err.println(Utilities.getNow());
			e.printStackTrace();
		}
		DashBoardData dbd = new DashBoardData(this);
		dbd.updateDashBoardForTSCount(cid);
		return jsonArr;
	}

	public int createTestSuiteGetId(String tsuname, int prodid) {
		String Executed_By = (String) userDetails.get("uname");
		String create_testSuite = "INSERT INTO cta.test_suite(Test_Suite, Version, Created_By, Created_Date, productid, status, "
				+ "width, height, browser, envurl, stopafterfailure, rerunfailedtests, supports_parallel_execution) VALUES "
				+ "(?, ?, ?, ?, ?, 0, 0, 0, 'Chrome', '', 'true', 'false', 'false')";
		JSONArray jsonArr = new JSONArray();
		int ts_pk = -1;
		try {
			PreparedStatement stmt_ts = testCon.prepareStatement(create_testSuite, Statement.RETURN_GENERATED_KEYS);
			stmt_ts.setString(1, tsuname);
			stmt_ts.setInt(2, 1);
			stmt_ts.setString(3, Executed_By);
			stmt_ts.setTimestamp(4, Utilities.getCurrentTimestamp());
			stmt_ts.setInt(5, prodid);
			stmt_ts.executeUpdate();
			ResultSet generatedKeys = stmt_ts.getGeneratedKeys();
			if (generatedKeys.next()) {
				ts_pk = (generatedKeys.getBigDecimal(1)).intValue();
			}
			generatedKeys.close();
			stmt_ts.close();
		} catch (Exception e) {
			System.err.println(Utilities.getNow());
			e.printStackTrace();
		}
		return ts_pk;
	}

	public JSONArray getUSerRoles(int companyid) {

		String sql = "SELECT * FROM cta.user_roles where static = 0 or companyid = ?";
		JSONArray jsonArr = new JSONArray();
		try {
			PreparedStatement stmt_ts = testCon.prepareStatement(sql);
			stmt_ts.setInt(1, companyid);
			ResultSet rs = stmt_ts.executeQuery();

			while (rs.next()) {
				JSONObject obj = new JSONObject();
				obj.put("iduser_roles", rs.getInt("iduser_roles"));
				obj.put("user_role", rs.getString("user_role"));
				obj.put("static", rs.getInt("static"));
				obj.put("companyid", rs.getInt("companyid"));
				jsonArr.add(obj);
			}
			rs.close();
			stmt_ts.close();
		} catch (Exception e) {
			System.err.println(Utilities.getNow());
			e.printStackTrace();
		}
		return jsonArr;
	}

	public void updateModulePerms(int moduleid, int roleid, String perm) {

		String sql = "SELECT * FROM cta.modulepermission where moduleid = ? and roleid = ?";
		JSONArray jsonArr = new JSONArray();
		try {
			PreparedStatement stmt_ts = testCon.prepareStatement(sql);
			stmt_ts.setInt(1, moduleid);
			stmt_ts.setInt(2, roleid);
			ResultSet rs = stmt_ts.executeQuery();

			if (rs.next()) {
				int modpermid = rs.getInt(1);
				sql = "UPDATE cta.modulepermission SET permission = ? where idmodulepermission = ?";
				stmt_ts = testCon.prepareStatement(sql);
				stmt_ts.setString(1, perm);
				stmt_ts.setInt(2, modpermid);
				stmt_ts.executeUpdate();
			} else {
				sql = "INSERT INTO cta.modulepermission (moduleid, roleid, permission) VALUES (?, ?, ?)";
				stmt_ts = testCon.prepareStatement(sql);
				stmt_ts.setInt(1, moduleid);
				stmt_ts.setInt(2, roleid);
				stmt_ts.setString(3, perm);
				stmt_ts.executeUpdate();
			}

			rs.close();
			stmt_ts.close();
		} catch (Exception e) {
			System.err.println(Utilities.getNow());
			e.printStackTrace();
		}
	}

	public void updateUserRole(int userid, int roleid) {

		String sql = "UPDATE cta.userprofile SET usertype = ? where iduserprofile = ?";

		try {
			PreparedStatement stmt_ts = testCon.prepareStatement(sql);
			stmt_ts.setInt(1, roleid);
			stmt_ts.setInt(2, userid);
			stmt_ts.execute();
			stmt_ts.close();
		} catch (Exception e) {
			System.err.println(Utilities.getNow());
			e.printStackTrace();
		}
	}

	public JSONArray getModulePerms(int moduleid) {

		String sql = "SELECT * FROM cta.modulepermission where moduleid = ? ";
		JSONArray jsonArr = new JSONArray();
		try {
			PreparedStatement stmt_ts = testCon.prepareStatement(sql);
			stmt_ts.setInt(1, moduleid);
			ResultSet rs = stmt_ts.executeQuery();

			while (rs.next()) {
				JSONObject obj = new JSONObject();
				obj.put("idmodulepermission", rs.getInt("idmodulepermission"));
				obj.put("moduleid", rs.getInt("moduleid"));
				obj.put("roleid", rs.getInt("roleid"));
				obj.put("permission", rs.getString("permission"));
				jsonArr.add(obj);
			}
			rs.close();
			stmt_ts.close();
		} catch (Exception e) {
			System.err.println(Utilities.getNow());
			e.printStackTrace();
		}
		return jsonArr;
	}

	public void moveTestCases(JSONArray json, int modid) {

		String sql = "UPDATE cta.test_case SET Module = ? where idtest_case in (";

//	String list = "(";

		for (int i = 0; i < json.size(); i++) {
			JSONObject tc = (JSONObject) json.get(i);
			long tcid = (long) tc.get("idtest_case");
			if (i > 0) {
				sql = sql + ", " + tcid;
			} else {
				sql = sql + tcid;
			}
		}

		sql = sql + " )";

		try {
			PreparedStatement stmt = testCon.prepareStatement(sql);
			stmt.setInt(1, modid);
//		stmt.setString(2, list);
			stmt.execute();
			stmt.close();
		} catch (Exception e) {
			System.err.println(Utilities.getNow());
			e.printStackTrace();
		}

	}

	public JSONArray addTestSuiteCase(JSONArray json, int tsuid, int pnum) {
		String Executed_By = (String) userDetails.get("uname");
		String insert_sql = "INSERT INTO cta.test_suite_case_map (Test_Suite_Id, Object_Id, Object_Index, Object_Type) VALUES (?, ?, ?, ?)";
		String tcIndex = "SELECT Object_Index FROM cta.test_suite_case_map WHERE Test_Suite_Id = " + tsuid
				+ " ORDER BY Object_Index DESC Limit 1";
		String check = "SELECT Object_Type FROM cta.test_suite_case_map WHERE Test_Suite_Id = ? AND Object_Id = ?";

		JSONArray jsonArr = new JSONArray();
		JSONObject data = new JSONObject();
		try {
			Statement stmt = testCon.createStatement();
			ResultSet rs = stmt.executeQuery(tcIndex);
			int lastIndex;
			if (rs.next()) {
				lastIndex = rs.getInt(1);
			} else
				lastIndex = -1;

			int size = json.size();
			for (int i = 0; i < json.size(); i++) {
				JSONObject object = (JSONObject) json.get(i);
				if (object.containsKey("idtest_case")) {
					String objectType = "case";
					int objectId = -1;
					if (object.get("idtest_case") instanceof Long) {
						objectId = ((Long) object.get("idtest_case")).intValue();
					} else {
						objectId = (int) object.get("idtest_case");
					}
					PreparedStatement insertstmt = testCon.prepareStatement(insert_sql);
					insertstmt.setInt(1, tsuid);
					insertstmt.setInt(2, objectId);
					insertstmt.setInt(3, lastIndex + 1 + i);
					insertstmt.setString(4, objectType);
					insertstmt.executeUpdate();
					insertstmt.close();
				} else if (object.containsKey("idtest_suite")) {
					String objectType = "suite";
					int objectId = ((Long) object.get("idtest_suite")).intValue();
					PreparedStatement stmt_check = testCon.prepareStatement(check);
					stmt_check.setInt(1, objectId);
					stmt_check.setInt(2, tsuid);
					ResultSet rs2 = stmt_check.executeQuery();
					if (rs2.next()) {
						String parentTs = getTestSuiteName(tsuid);
						String childTs = getTestSuiteName(objectId);
						String str = "Circular addition of test suites is not valid - " + childTs
								+ " currently includes " + parentTs + " , so adding " + childTs + " into " + parentTs
								+ " is not valid";
						data.put("Failure:", str);
					} else if (objectId == tsuid) {
						String childTs = getTestSuiteName(objectId);
						String str = "Adding Suite " + childTs + " into " + childTs + " is not supported";
						data.put("Failure:", str);
					} else {
						PreparedStatement insertstmt = testCon.prepareStatement(insert_sql);
						insertstmt.setInt(1, tsuid);
						insertstmt.setInt(2, objectId);
						insertstmt.setInt(3, lastIndex + 1 + i);
						insertstmt.setString(4, objectType);
						insertstmt.executeUpdate();
						insertstmt.close();
					}
					rs2.close();
					stmt_check.close();
				}
			}
			JSONArray list = getAllTestCasesFromDBForTestSuite(tsuid, pnum);
			data.put("idtest_suite", tsuid);
			data.put("list", list);
			jsonArr.add(data);
			rs.close();
			stmt.close();
		} catch (Exception e) {
			System.err.println(Utilities.getNow());
			e.printStackTrace();
		}
		return jsonArr;
	}

	public JSONArray deleteTCfromTSuite(JSONArray json, int tsuid, int pnum, int tcindex) {
		String select_sql = "SELECT Object_Index FROM cta.test_suite_case_map WHERE Test_Suite_Id = ? AND Object_Id=?";
		String del_sql = "DELETE FROM cta.test_suite_case_map WHERE Test_Suite_Id = ? "
				+ "AND Object_Id=? AND Object_Index=?";
		String updateIndex = "UPDATE cta.test_suite_case_map SET Object_Index = Object_Index - 1 "
				+ "WHERE Test_Suite_Id = ? AND Object_Index > ? ";
		String tsName = "SELECT Test_Suite FROM cta.test_suite WHERE idtest_suite = " + tsuid;

		JSONArray jsonArr = new JSONArray();
		JSONObject data = new JSONObject();
		try {
			for (int i = 0; i < json.size(); i++) {
				int index = 0;
				int tcid = 0;
				JSONObject object = (JSONObject) json.get(i);
				if (object.containsKey("idtest_case")) {
					tcid = ((Long) object.get("idtest_case")).intValue();
				} else if (object.containsKey("idtest_suite")) {
//            	data = (JSONObject) object.get("testcases");
//                JSONObject testSuiteDetailsObject = (JSONObject) data.get("0");
					tcid = ((Long) object.get("idtest_suite")).intValue();
				}
				PreparedStatement deletestmt = testCon.prepareStatement(select_sql);
				deletestmt.setInt(1, tsuid);
				deletestmt.setInt(2, tcid);
				ResultSet rs = deletestmt.executeQuery();
				if (rs.next())
					index = rs.getInt(1);

				deletestmt = testCon.prepareStatement(del_sql);
				deletestmt.setInt(1, tsuid);
				deletestmt.setInt(2, tcid);
				deletestmt.setInt(3, index);
				deletestmt.executeUpdate();

				deletestmt = testCon.prepareStatement(updateIndex);
				deletestmt.setInt(1, tsuid);
				deletestmt.setInt(2, index);
				deletestmt.executeUpdate();
				rs.close();
				deletestmt.close();
			}
			Statement stmt = testCon.createStatement();
			ResultSet rs = stmt.executeQuery(tsName);
			String TsName = "";
			if (rs.next()) {
				TsName = rs.getString(1);
			}
			JSONArray list = getAllTestCasesFromDBForTestSuite(tsuid, pnum);
			data.put("idtest_suite", tsuid);
			data.put("Test_Suite", TsName);
			data.put("list", list);
			jsonArr.add(data);
			rs.close();
			stmt.close();
		} catch (Exception e) {
			System.err.println(Utilities.getNow());
			e.printStackTrace();
		}
		return jsonArr;
	}

	public JSONArray reorderTestcase(int tcid, int tsuid, int src, int dest, int pnum) {
		String sql = "UPDATE cta.test_suite_case_map SET Object_Index = ? WHERE Test_Suite_Id=? AND Object_Id=? AND Object_Index = ?";
		String increment_sql = "UPDATE cta.test_suite_case_map SET Object_Index = Object_Index + 1 WHERE Test_Suite_Id= ? AND Object_Index BETWEEN ? AND ?";
		String decrement_sql = "UPDATE cta.test_suite_case_map SET Object_Index = Object_Index - 1 WHERE Test_Suite_Id= ? AND Object_Index BETWEEN ? AND ?";
		JSONArray jsonArr = new JSONArray();
		JSONObject data = new JSONObject();
		int noOfRecords = (pnum - 1) * AppProperties.listlimitInt;
		int originalSrc = noOfRecords + src;
		try {
			if (dest < src) {
				src = noOfRecords + src;
				dest = noOfRecords + dest;
				PreparedStatement stmt = testCon.prepareStatement(increment_sql);
				stmt.setInt(1, tsuid);
				stmt.setInt(2, dest);
				stmt.setInt(3, src - 1);
				stmt.executeUpdate();
				stmt.close();
			} else {
				src = noOfRecords + src;
				dest = noOfRecords + dest;
				PreparedStatement stmt = testCon.prepareStatement(decrement_sql);
				stmt.setInt(1, tsuid);
				stmt.setInt(2, src + 1);
				stmt.setInt(3, dest);
				stmt.executeUpdate();
				stmt.close();
			}
			PreparedStatement reorder_stmt = testCon.prepareStatement(sql);
			reorder_stmt.setInt(1, dest);
			reorder_stmt.setInt(2, tsuid);
			reorder_stmt.setInt(3, tcid);
			reorder_stmt.setInt(4, originalSrc);
			reorder_stmt.executeUpdate();
			JSONArray list = getAllTestCasesFromDBForTestSuite(tsuid, pnum);
			data.put("idtest_suite", tsuid);
			data.put("list", list);
			jsonArr.add(data);

			reorder_stmt.close();
		} catch (Exception e) {
			System.err.println(Utilities.getNow());
			e.printStackTrace();
		}
		return jsonArr;
	}

	private JSONArray getTestSuitesAsJSON(ResultSet rs, int flag) {
		// JSONObject data = new JSONObject();
		String limitStr = AppProperties.listlimit;
		int limit = Integer.parseInt(limitStr);
		JSONArray jsonArr = new JSONArray();
		JSONObject jsonRow = new JSONObject();
		try {
			int totalTestSuite = 0;
			int prodid = (int) rs.getInt("productid");
			String sql = "SELECT count(*) FROM cta.test_suite WHERE productid = " + prodid + " AND status = '0'"
					+ " GROUP BY idtest_suite, Test_Suite";
			Statement stmt = testCon.createStatement();
			ResultSet rs2 = stmt.executeQuery(sql);
			if (rs2.next()) {
				totalTestSuite = rs2.getInt(1);
			}
			rs2.close();
			int companyId = (int)userDetails.get("companyid");
			String companyThreadCountSql = "SELECT parallelThreads FROM cta.company where idcompany = " + companyId;
			stmt = testCon.createStatement();
			ResultSet rs3 = stmt.executeQuery(companyThreadCountSql);
			int companyThreadCount = 0;
			if (rs3.next()) {
				companyThreadCount = rs3.getInt(1);
			}
			rs3.close();
			stmt.close();
			jsonRow.put("idtest_suite", rs.getInt("idtest_suite"));
			jsonRow.put("Test_Suite", rs.getString("Test_Suite"));
			jsonRow.put("Version", rs.getInt("Version"));
			jsonRow.put("Created_By", rs.getString("Created_By"));
			jsonRow.put("Created_Date", (rs.getTimestamp("Created_Date")).toString());
			jsonRow.put("productid", prodid);
			JSONArray list = new JSONArray();
			if (flag == 0) {
				list = getAllTestCasesFromDBForTestSuiteEx(rs.getInt("idtest_suite"));
			} else {
				list = getAllTestCasesFromDBForTestSuite(rs.getInt("idtest_suite"), 1);
			}
			jsonRow.put("list", list);
			jsonRow.put("width", rs.getInt("width"));
			jsonRow.put("height", rs.getInt("height"));
			jsonRow.put("browser", rs.getString("browser"));
			jsonRow.put("envurl", rs.getString("envurl"));
			jsonRow.put("stopafterfailure", rs.getString("stopafterfailure"));
			jsonRow.put("rerunfailedtests", rs.getString("rerunfailedtests"));
			jsonRow.put("supports_parallel_execution", rs.getString("supports_parallel_execution"));
			jsonRow.put("synctoggle", rs.getInt("synchedscenarios"));
			jsonRow.put("resultsemail", rs.getString("resultsemail"));
			jsonRow.put("buildtag", rs.getString("buildtag"));
			jsonRow.put("synchedscenarios", rs.getBoolean("synchedscenarios"));
			jsonRow.put("ssonerror", rs.getBoolean("ssonerror"));
			jsonRow.put("indEmail", rs.getBoolean("indEmail"));
			jsonRow.put("headless", rs.getBoolean("headless"));
			jsonRow.put("analysis", rs.getInt("analysis"));
			jsonRow.put("rerun_in_parallel", rs.getString("rerun_in_parallel"));
			jsonRow.put("threadcount", rs.getInt("threadcount"));
			jsonRow.put("companyThreadCount", companyThreadCount);
			String initialVar = rs.getString("initialvariables");
			JSONParser parser = new JSONParser();
            JSONObject initialVariables = null;
            if(initialVar != null) {
            	initialVariables = (JSONObject) parser.parse(initialVar);
            }
			jsonRow.put("variables", initialVariables);
			jsonRow.put("pageSize", limit);
			jsonRow.put("totalPages",
					(totalTestSuite == limit ? (totalTestSuite / limit) : (totalTestSuite / limit) + 1));
			jsonArr.add(jsonRow);
		} catch (Exception e) {
			System.err.println(Utilities.getNow());
			e.printStackTrace();
		}
		return jsonArr;
	}

	public JSONObject getTestSuiteCaseMap(int testSuiteId) {

		JSONObject jsonArr = null;
		try {
			String sql = "SELECT * FROM cta.test_suite_case_map WHERE Test_Suite = ?";
			PreparedStatement stmt = testCon.prepareStatement(sql);
			stmt.setInt(1, testSuiteId);
			ResultSet resultSet = stmt.executeQuery();
			if (resultSet.next()) {
				jsonArr = getTestSuiteCaseMapAsJSON(resultSet);
			}
			resultSet.close();
			stmt.close();

		} catch (Exception e) {
			System.err.println(Utilities.getNow());
			e.printStackTrace();
		}

		return jsonArr;
	}

	private JSONObject getTestSuiteCaseMapAsJSON(ResultSet rs) {

		JSONObject jsonRow = new JSONObject();
		try {
			// jsonRow.put("idTest_Suite_Case_Map", rs.getInt("idTest_Suite_Case_Map"));
			jsonRow.put("Test_Suite", rs.getInt("Test_Suite"));
			jsonRow.put("Test_Case", rs.getInt("Test_Case"));
		} catch (Exception e) {
			System.err.println(Utilities.getNow());
			e.printStackTrace();
		}
		return jsonRow;
	}

	public JSONArray getProducts(int companyId) {
		String sql = "SELECT * From cta.products where company = ?";
		JSONArray result = new JSONArray();
		try {
			PreparedStatement stmt = testCon.prepareStatement(sql);
			stmt.setInt(1, companyId);
			ResultSet rs = stmt.executeQuery();
			while (rs.next()) {
				JSONObject prod = getProductsAsJSON(rs, false);
				result.add(prod);
			}
			rs.close();
			stmt.close();
		} catch (Exception e) {
			System.err.println(Utilities.getNow());
			e.printStackTrace();
		}

		return result;
	}

	public int getProductsFromModule(int mid) {
		String sql = "SELECT product From cta.modules where idmodules = ?";
		int prod = -1;
		try {
			PreparedStatement stmt = testCon.prepareStatement(sql);
			stmt.setInt(1, mid);
			ResultSet rs = stmt.executeQuery();
			if (rs.next()) {
				prod = rs.getInt(1);
			}
			rs.close();
			stmt.close();
		} catch (Exception e) {
			System.err.println(Utilities.getNow());
			e.printStackTrace();
		}
		return prod;
	}

	public int getModuleFromTestcase(int tcid) {
		String sql = "SELECT Module FROM cta.test_case where idtest_case = ?";
		int modid = -1;
		try {
			PreparedStatement stmt = testCon.prepareStatement(sql);
			stmt.setInt(1, tcid);
			ResultSet rs = stmt.executeQuery();
			if (rs.next()) {
				modid = rs.getInt(1);
			}

			rs.close();
			stmt.close();
		} catch (Exception e) {
			System.err.println(Utilities.getNow());
			e.printStackTrace();
		}
		return modid;
	}

	public JSONArray getModulesFromProduct(int prodid) {
		String sql = "SELECT m.* , p.*" + "FROM cta.modules m " + "JOIN cta.products p ON m.product = p.idproducts "
				+ "WHERE p.idproducts =" + prodid;
		JSONArray result = new JSONArray();
		try {
			Statement stmt = testCon.createStatement();
			ResultSet rs = stmt.executeQuery(sql);
			JSONObject modJson = new JSONObject();
			// JSONObject prodJson = new JSONObject();
			while (rs.next()) {
				JSONObject prod = getModulesAsJSON(rs);
				result.add(prod);
			}
			rs.close();
			stmt.close();
		} catch (Exception e) {
			System.err.println(Utilities.getNow());
			e.printStackTrace();
		}

		return result;
	}

	public String getModuleName(int mod) {
		String sql = "SELECT modulename FROM cta.modules WHERE idmodules = " + mod;
		String ModName = "";
		try {
			Statement stmt = testCon.createStatement();
			ResultSet rs = stmt.executeQuery(sql);
			if (rs.next()) {
				ModName = rs.getString(1);
			}
			rs.close();
			stmt.close();
		} catch (Exception e) {
			System.err.println(Utilities.getNow());
			e.printStackTrace();
		}
		return ModName;
	}

	public String getProdName(int prod) {
		String sql = "SELECT productname FROM cta.products WHERE idproducts = " + prod;
		String ProdName = "";
		try {
			Statement stmt = testCon.createStatement();
			ResultSet rs = stmt.executeQuery(sql);
			if (rs.next()) {
				ProdName = rs.getString(1);
			}

			rs.close();
			stmt.close();
		} catch (Exception e) {
			System.err.println(Utilities.getNow());
			e.printStackTrace();
		}
		return ProdName;
	}

	public String getTCName(int tc) {
		String sql = "SELECT Test_Case FROM cta.test_case WHERE idtest_case = " + tc;
		String TCName = "";
		try {
			Statement stmt = testCon.createStatement();
			ResultSet rs = stmt.executeQuery(sql);
			if (rs.next()) {
				TCName = rs.getString(1);
			}

			rs.close();
			stmt.close();
		} catch (Exception e) {
			System.err.println(Utilities.getNow());
			e.printStackTrace();
		}
		return TCName;
	}

	public JSONArray getTestCaseFromModule(int modid, int limit, int pnum) {
		String sql = "SELECT m.* , tc.*" + "FROM cta.modules m " + "JOIN cta.test_case tc ON m.idmodules = tc.Module "
				+ "WHERE m.idmodules = ? AND tc.Status != 1 ORDER BY idtest_case DESC LIMIT ? OFFSET ?";
		String count_sql = "SELECT COUNT(*) FROM cta.modules m JOIN cta.test_case tc ON m.idmodules = tc.Module"
				+ " WHERE m.idmodules = " + modid;
		JSONArray result = new JSONArray();
		try {
			Statement Cstmt = testCon.createStatement();
			ResultSet rs2 = Cstmt.executeQuery(count_sql);
			rs2.next();
			int testCaseCount = rs2.getInt(1);
			int offset = (pnum - 1) * limit;
			PreparedStatement stmt = testCon.prepareStatement(sql);
			stmt.setInt(1, modid);
			stmt.setInt(2, limit);
			stmt.setInt(3, offset);
			ResultSet rs = stmt.executeQuery();
			JSONObject modJson = new JSONObject();
			// JSONObject prodJson = new JSONObject();
			while (rs.next()) {
				JSONObject prod = getTestCaseAsJSON(rs, false);
				prod.put("pageSize", limit);
				prod.put("totalPages",
						(testCaseCount == limit ? (testCaseCount / limit) : (testCaseCount / limit) + 1));
				result.add(prod);
			}

			rs2.close();
			rs.close();
			stmt.close();
			Cstmt.close();
		} catch (Exception e) {
			System.err.println(Utilities.getNow());
			e.printStackTrace();
		}

		return result;
	}

	public JSONArray getTestCaseFromSearch(int modid, String key, int limit, int pnum) {
		int offset = (pnum - 1) * limit;
		String countStr = "SELECT Count(*)" + "FROM cta.modules m "
				+ "JOIN cta.test_case tc ON m.idmodules = tc.Module " + "WHERE m.idmodules =" + modid
				+ " AND Status=0 AND LOWER(tc.Test_Case) LIKE '%" + key.toLowerCase() + "%' ";
		String sql = "SELECT m.* , tc.*" + "FROM cta.modules m " + "JOIN cta.test_case tc ON m.idmodules = tc.Module "
				+ "WHERE m.idmodules =" + modid + " AND Status=0 AND LOWER(tc.Test_Case) LIKE '%" + key.toLowerCase()
				+ "%' " + "ORDER BY idtest_case DESC LIMIT " + limit + " OFFSET " + offset;

		return tcsearch(sql, countStr, limit);
	}

	public JSONArray tcsearch(String sql, String countStr, int limit) {
		JSONArray result = new JSONArray();
		try {
			Statement stmt = testCon.createStatement();
			ResultSet rs = stmt.executeQuery(sql);
			Statement countStmt = testCon.createStatement();
			ResultSet rs2 = countStmt.executeQuery(countStr);
			int count = -1;
			if (rs2.next()) {
				count = rs2.getInt(1);
			}
			while (rs.next()) {
				JSONObject prod = getTestCaseAsJSON(rs, false);
				prod.put("totalPages", (count == limit ? (count / limit) : (count / limit) + 1));
				result.add(prod);
			}

			rs2.close();
			rs.close();
			stmt.close();
			countStmt.close();
		} catch (Exception e) {
			System.err.println(Utilities.getNow());
			e.printStackTrace();
		}

		return result;
	}

	public JSONArray getTestSuiteFromSearch(int prodid, String key, int pnum) {
		String limitStr = AppProperties.listlimit;
		int limit = Integer.parseInt(limitStr);
		int offset = (pnum - 1) * limit;
		String countStr = "SELECT Count(*) FROM cta.test_suite " + "WHERE productid =" + prodid
				+ " AND status='0' AND LOWER(Test_Suite) LIKE '%" + key.toLowerCase() + "%' ";
		String sql = "SELECT * FROM cta.test_suite " + "WHERE productid =" + prodid
				+ " AND status='0' AND LOWER(Test_Suite) LIKE '%" + key.toLowerCase() + "%' "
				+ "ORDER BY idtest_suite DESC LIMIT " + limit + " OFFSET " + offset;
		JSONArray result = new JSONArray();
		try {
			Statement stmt = testCon.createStatement();
			ResultSet rs = stmt.executeQuery(sql);
			Statement countStmt = testCon.createStatement();
			ResultSet rs2 = countStmt.executeQuery(countStr);
			rs2.next();
			int count = rs2.getInt(1);
			JSONArray prod = null;
			while (rs.next()) {
				prod = getTestSuitesAsJSON(rs, 0);
//				prod.put("totalPages", (count == limit?(count/limit):(count/limit)+1));
				result.add(prod);
			}

			rs2.close();
			rs.close();
			stmt.close();
			countStmt.close();
		} catch (Exception e) {
			System.err.println(Utilities.getNow());
			e.printStackTrace();
		}
		return result;
	}

	public JSONArray searchInSuite(int suiteid, String key, int pnum) {
		String limitStr = AppProperties.listlimit;
		int limit = Integer.parseInt(limitStr);
		int offset = (pnum - 1) * limit;
		String countStr = "SELECT COUNT(*) FROM cta.test_case tc JOIN cta.test_suite_case_map tscm ON tc.idtest_case = tscm.Object_Id "
				+ "WHERE Test_Suite_Id = " + suiteid + " AND LOWER(tc.Test_Case) LIKE '%" + key.toLowerCase() + "%' ";
		String sql = "SELECT tc.*, tscm.* FROM cta.test_case tc JOIN cta.test_suite_case_map tscm ON tc.idtest_case = tscm.Object_Id "
				+ "WHERE Test_Suite_Id = " + suiteid + " AND LOWER(tc.Test_Case) LIKE '%" + key.toLowerCase() + "%' "
				+ "ORDER BY idtest_case DESC LIMIT " + limit + " OFFSET " + offset;
		JSONArray result = new JSONArray();
		try {
			Statement stmt = testCon.createStatement();
			ResultSet rs = stmt.executeQuery(sql);
			Statement countStmt = testCon.createStatement();
			ResultSet rs2 = countStmt.executeQuery(countStr);
			rs2.next();
			int count = rs2.getInt(1);
			while (rs.next()) {
				JSONObject prod = getTestCaseAsJSON(rs, false);
				int Test_Suite_Id = (int) rs.getInt("Test_Suite_Id");
				int Object_Id = (int) rs.getInt("Object_Id");
				int Object_Index = (int) rs.getInt("Object_Index");
				String Object_Type = (String) rs.getString("Object_Type");
				int idTest_Suite_Case_Map = (int) rs.getInt("idTest_Suite_Case_Map");
				prod.put("Test_Suite_Id", Test_Suite_Id);
				prod.put("Object_Id", Object_Id);
				prod.put("Object_Index", Object_Index);
				prod.put("Object_Type", Object_Type);
				prod.put("idTest_Suite_Case_Map", idTest_Suite_Case_Map);
				prod.put("totalPages", (count == limit ? (count / limit) : (count / limit) + 1));
				result.add(prod);
			}

			rs.close();
			rs2.close();
			countStmt.close();
			stmt.close();

		} catch (Exception e) {
			System.err.println(Utilities.getNow());
			e.printStackTrace();
		}
		return result;
	}

	public JSONArray getTestStepFromTestCase(int tcid, boolean getPwd) {
		String sql = "SELECT tc.* , ts.*" + "FROM cta.test_case tc "
				+ "JOIN cta.test_step ts ON tc.idtest_case = ts.Test_Case_Id " + "WHERE tc.idtest_case =" + tcid
				+ " ORDER BY ts.Step_Number ASC";
		JSONArray result = new JSONArray();
		try {
			Statement stmt = testCon.createStatement();
			ResultSet rs = stmt.executeQuery(sql);
			JSONObject modJson = new JSONObject();
			// JSONObject prodJson = new JSONObject();
			while (rs.next()) {
				JSONObject prod = getTestStepsFromDb(rs, getPwd, false);
				result.add(prod);
			}
			rs.close();
			stmt.close();
		} catch (Exception e) {
			System.err.println(Utilities.getNow());
			e.printStackTrace();
		}

		return result;
	}

	public void createLicenseKeyDuringRegistration(int companyid, String licUsage) {

		String lKey = Utilities.randomGen(AppProperties.CHAR, 16, false);
		String sql = "INSERT into cta.licenses (licensekey, companyid, validtill,licusage)" + " VALUES(?,?,?,?)";

		try {
			PreparedStatement stmt = testCon.prepareStatement(sql);
			stmt.setString(1, lKey);
			stmt.setInt(2, companyid);
			stmt.setTimestamp(3, Utilities.getCurrentTimestamp());
			stmt.setString(4, licUsage);
			stmt.executeUpdate();
			stmt.close();
		} catch (Exception e) {
			System.err.println(Utilities.getNow());
			e.printStackTrace();
		}
	}

	public JSONObject registerUser(JSONObject json, MySqlConn2 msc2) throws Exception {
		String sql_fetch = "INSERT INTO cta.company (companyname, companycontactname, contactemail, contactphone, status, approvalrequired, gentype, logintype,"
				+ "coverage,backendcoverage) " + "VALUES (?, ?, ?, ?, ?, 0,?, ?, ?,?)";
		String sql = "INSERT INTO cta.userprofile (username, password, company, firstname, lastname, email, status, "
				+ "phonenumber, usertype, createdDate) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
		String sql_company = "SELECT idcompany FROM cta.company WHERE companyname = ?";
		String create_prod = "INSERT INTO cta.products (productname, company )" + " VALUES(?, ?) ";
		String create_mod = "INSERT INTO cta.modules (modulename, product )" + " VALUES(?, ?) ";
		String fetch_approved = "SELECT approvalrequired FROM cta.company WHERE companyname=?";

		JSONObject resultJson = null;

		try {
			PreparedStatement stmt = testCon.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
			PreparedStatement stmt_fetch = testCon.prepareStatement(sql_fetch, Statement.RETURN_GENERATED_KEYS);
			PreparedStatement stmt_company = testCon.prepareStatement(sql_company);
			PreparedStatement stmt_prod = testCon.prepareStatement(create_prod, Statement.RETURN_GENERATED_KEYS);
			PreparedStatement stmt_mod = testCon.prepareStatement(create_mod);
			PreparedStatement stmt_approved = testCon.prepareStatement(fetch_approved);

			JSONObject userdetails = json;
			String firstName = (String) userdetails.get("firstname");
			String firstNameEncoded = Encode.forHtml(firstName);
			String lastName = (String) userdetails.get("lastname");
			String lastNameEncoded = Encode.forHtml(lastName);
			if (!(validateName(firstName) && validateName(lastName))) {
				System.err.println("Invalid entry for firstName or LastName");
				resultJson.put("status", "fail");
				resultJson.put("message", "Invalid entry for firstName or LastName");
				return resultJson;
			}
			String compName = (String) userdetails.get("company");
			String compNameEncoded = Encode.forHtml(compName);
			String phoneNumber = (String) userdetails.get("phonenumber");
			String phoneNumberEncoded = Encode.forHtml(phoneNumber);
			String email = (String) userdetails.get("email");
			String emailEncoded = Encode.forHtml(email);
			String password = (String) userdetails.get("password");
			String hashedPwd = PasswordUtils.hashPassword(password, AppProperties.hashingsalt);
			String gentype = (String) userdetails.get("gentype");
			String liccode = (String) userdetails.get("codegen");
			String techstack = (String) userdetails.get("techstack");
			String logintype = null;
			if (gentype == null || gentype.equals("")) {
				gentype = "recording";
				logintype = "normal";
			} else if (gentype != null || gentype.equals("codegen")) {
				gentype = techstack;
				logintype = "codegen";
			}
			String backendcoverage = "false";
			String coverage = "false";
			if (userdetails.get("optForPageObject") != null) {
				boolean optForPageObject = (boolean) userdetails.get("optForPageObject");
				if (optForPageObject) {
					backendcoverage = "true";
					coverage = "true";
				}
			}
			// String passwordEncoded = Encode.forHtml(password);
			// int usertype = ((Long) userdetails.get("usertype")).intValue();
			int usertype = 2;
			String product = (String) userdetails.get("product");
			String productEncoded = Encode.forHtml(product);
			String module = (String) userdetails.get("module");
			String moduleEncoded = Encode.forHtml(module);
			int status = -1;

			int isApprovalRequired = -1;
			stmt_approved.setString(1, compNameEncoded);
			ResultSet rs_approved = stmt_approved.executeQuery();
			if (rs_approved.next()) {
				isApprovalRequired = rs_approved.getInt("approvalrequired");
			}

			if (isApprovalRequired == 1) {
				status = 3;
			} else {
				status = 0;
			}

			stmt_company.setString(1, compNameEncoded);
			ResultSet rs = stmt_company.executeQuery();
			int compId = 0;
			if (rs.next() && !(liccode != null && liccode.equals("codegen"))) {
				compId = rs.getInt("idcompany");
			} else {
				stmt_fetch.setString(1, compNameEncoded);
				stmt_fetch.setString(2, firstNameEncoded);
				stmt_fetch.setString(3, emailEncoded);
				stmt_fetch.setString(4, phoneNumberEncoded);
				stmt_fetch.setInt(5, status);
				stmt_fetch.setString(6, gentype);
				stmt_fetch.setString(7, logintype);
				stmt_fetch.setString(8, coverage);
				stmt_fetch.setString(9, backendcoverage);
				stmt_fetch.executeUpdate();
				ResultSet generatedKeys = stmt_fetch.getGeneratedKeys();
				if (generatedKeys.next()) {
					compId = (generatedKeys.getBigDecimal(1)).intValue();
					Utilities.createCompanyFolder(compId);
				}
				generatedKeys.close();
				getDashboardData(compId, -1);

				String brandStr = "INSERT into cta.branding (idCompany, primaryColor,"
						+ "secondaryColor, tertiaryColor, brandLogo) VALUES (?,?,?,?,?)";
				PreparedStatement brantstmt = testCon.prepareStatement(brandStr);
				brantstmt.setInt(1, compId);
				brantstmt.setString(2, "#0FC6EE");
				brantstmt.setString(3, "#032F40");
				brantstmt.setString(4, "#D3DDE6");
				brantstmt.setString(5, "_logo_1_.png");
				brantstmt.executeUpdate();
				brantstmt.close();
				Utilities.createBrandingFolder(compId);
				Utilities.copyFile(
						Utilities.getBrandingDir(AppProperties.rootCompany,
								"_logo_" + AppProperties.rootCompany + "_.png"),
						Utilities.getBrandingDir(compId, "_logo_1_.png"));
			}

			// stmt.setInt(1, userID);
			stmt.setString(1, emailEncoded);
			stmt.setString(2, hashedPwd);
			stmt.setInt(3, compId);
			stmt.setString(4, firstNameEncoded);
			stmt.setString(5, lastNameEncoded);
			stmt.setString(6, emailEncoded);
			stmt.setInt(7, status);
			stmt.setString(8, phoneNumberEncoded);
			stmt.setInt(9, 2);
			stmt.setTimestamp(10, Utilities.getCurrentTimestamp());
			stmt.executeUpdate();
			ResultSet generatedKeys2 = stmt.getGeneratedKeys();
			if (generatedKeys2.next()) {
				int userid = (generatedKeys2.getBigDecimal(1)).intValue();
				userdetails.put("iduserprofile", userid);
			}
			generatedKeys2.close();

			if (liccode != null && liccode.equals("codegen")) {
				productEncoded = "Automation";
				moduleEncoded = "CodeGeneration";
			}

			if (json.containsKey("product") || (liccode != null && liccode.equals("codegen"))) {
				stmt_prod.setString(1, productEncoded);
				stmt_prod.setInt(2, compId);
				stmt_prod.executeUpdate();
				ResultSet generatedKeys = stmt_prod.getGeneratedKeys();
				if (generatedKeys.next()) {
					int prod_pk = (generatedKeys.getBigDecimal(1)).intValue();
					stmt_mod.setString(1, moduleEncoded);
					stmt_mod.setInt(2, prod_pk);
					stmt_mod.executeUpdate();
				}
				generatedKeys.close();
			}
			stmt_approved.close();
			stmt_prod.close();
			stmt_company.close();
			stmt_fetch.close();
			stmt_mod.close();
			stmt.close();

			String usagelic = null;

			if (liccode != null && liccode.equals("codegen")) {
				String plan = (String) userdetails.get("plan");
				JSONObject planDetails = getPlan(1);
				CashfreeIntegration cfi = new CashfreeIntegration();
				String psid = cfi.callCashfree(userdetails, compId, planDetails, this);
				resultJson = new JSONObject();
				resultJson.put("status", "success");
				resultJson.put("paymentSessionId", psid);
//				usagelic = generateUsageLicense(liccode, compId, "deva@nogrunt.com",
//						msc2, (int)planDetails.get("quantity"));
			}

			createLicenseKeyDuringRegistration(compId, usagelic);

		} catch (Exception e) {
			System.err.println(Utilities.getNow());
			e.printStackTrace();
			throw e;
		}

		return resultJson;
	}

	public JSONObject getOrderByNgorderid(String ngorderid, boolean includeCompletionJson) {
		String sql = "SELECT * from orders where nogruntorderid = ? " + "order by idorders desc LIMIT 1";
		JSONObject result = new JSONObject();
		try {
			PreparedStatement stmt = testCon.prepareStatement(sql);
			stmt.setString(1, ngorderid);
			ResultSet rs = stmt.executeQuery();
			if (rs.next()) {
				result.put("idorders", rs.getInt("idorders"));
				result.put("companyid", rs.getInt("companyid"));
				result.put("userid", rs.getInt("userid"));
				result.put("planid", rs.getInt("planid"));
				result.put("nogruntorderid", rs.getString("nogruntorderid"));
				result.put("pgorderid", rs.getString("pgorderid"));
				result.put("amount", rs.getBigDecimal("amount"));
				result.put("status", rs.getString("status"));
				result.put("pgpaymentid", rs.getString("pgpaymentid"));
				result.put("createddate", (rs.getTimestamp("createddate")).toString());
				result.put("completeddate", (rs.getTimestamp("completeddate")).toString());
				result.put("activateddate", (rs.getTimestamp("activateddate")).toString());
				if (includeCompletionJson) {
					result.put("completionjson", rs.getString("completionjson"));
				}
			}
			rs.close();
			stmt.close();
		} catch (Exception e) {
			System.err.println(Utilities.getNow());
			e.printStackTrace();
		}

		return result;
	}

	public JSONObject getPlan(int planid) {
		String sql = "SELECT * from pricingplans where idpricingplans = ?";
		JSONObject res = new JSONObject();
		try {
			PreparedStatement stmt = testCon.prepareStatement(sql);
			stmt.setInt(1, planid);
			ResultSet rs = stmt.executeQuery();
			if (rs.next()) {
				res.put("idpricingplans", rs.getInt("idpricingplans"));
				res.put("planname", rs.getString("planname"));
				res.put("description", rs.getString("description"));
				res.put("price", rs.getBigDecimal("price"));
				res.put("currency", rs.getString("currency"));
				res.put("currency", rs.getString("currency"));
				res.put("createddate", rs.getTimestamp("createddate"));
				res.put("isactive", rs.getBoolean("isactive"));
				res.put("updateddate", rs.getTimestamp("updateddate"));
				res.put("inactivedate", rs.getTimestamp("inactivedate"));
				res.put("quantity", rs.getInt("quantity"));
			}
			rs.close();
			stmt.close();
		} catch (Exception e) {
			System.err.println(Utilities.getNow());
			e.printStackTrace();
		}

		return res;
	}

	public JSONObject getCompanyPlan(int companyid) {
		String sql = "select * from pricingplans as pp " + "join orders as orders on orders.planid = pp.idpricingplans "
				+ "join company as comp on comp.idcompany = orders.companyid " + "where comp.idcompany = ?;";
		JSONObject res = new JSONObject();
		try {
			PreparedStatement stmt = testCon.prepareStatement(sql);
			stmt.setInt(1, companyid);
			ResultSet rs = stmt.executeQuery();
			if (rs.next()) {
				res.put("idpricingplans", rs.getInt("idpricingplans"));
				res.put("planname", rs.getString("planname"));
				res.put("quantity", rs.getInt("quantity"));
				res.put("idorders", rs.getInt("idorders"));
			}
			rs.close();
			stmt.close();
		} catch (Exception e) {
			System.err.println(Utilities.getNow());
			e.printStackTrace();
		}

		return res;
	}

	public String generateUsageLicense(String liccode, int compId, String requestedBy, MySqlConn2 msc2, int count) {
		String fetch_liccode = "SELECT * from cta.licenseinfo where liccode = ?";
		try {
			PreparedStatement stmt = testCon.prepareStatement(fetch_liccode);
			stmt.setString(1, liccode);
			ResultSet rs = stmt.executeQuery();

			if (rs.next()) {
				int tccount = rs.getInt("testcasescount");
				int validFor = rs.getInt("validfor");
				String validTill = null;
				if (rs.getTimestamp("validtill") != null) {
					validTill = (rs.getTimestamp("validtill")).toString();
				}

				if (validFor > 0) {
					LocalDate currentDate = LocalDate.now();

					// Subtract 2 weeks from the current date
					LocalDate dateplusvaliddays = currentDate.plusDays(validFor);

					// Format the date as a string
					DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
					validTill = dateplusvaliddays.format(formatter);
				}

				String lic = msc2.generateLicense(compId, requestedBy, validTill, String.valueOf(tccount), this);
				rs.close();
				stmt.close();
				return lic;
			} else {
				LocalDate currentDate = LocalDate.now();
				// Subtract 2 weeks from the current date
				LocalDate dateplusvaliddays = currentDate.plusDays(45);

				// Format the date as a string
				DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
				String validTill = dateplusvaliddays.format(formatter);
				String lic = msc2.generateLicense(compId, requestedBy, validTill, String.valueOf(count), this);
			}
		} catch (Exception e) {
			System.err.println(Utilities.getNow());
			e.printStackTrace();
		}

		return null;
	}

	public boolean validateName(String name) {
		String nameRegex = "^[^<>]*$";
		return name.matches(nameRegex);
	}

	public JSONArray getTestCaseResultFromTestCase(int tcid, int companyId, boolean justLastOne) {

		String sql = "";
		if (!justLastOne) {
			sql = "SELECT tc.Test_Case, tc.Version, tc.Created_By, tc.Created_Date, tcr.* " + "FROM cta.test_case tc "
					+ "JOIN cta.test_case_results tcr ON tc.idtest_case = tcr.Test_Case_Id " + "WHERE tc.idtest_case = "
					+ tcid + " order by tcr.Executed_Date Desc";
		} else {
			sql = "SELECT tc.Test_Case, tc.Version, tc.Created_By, tc.Created_Date, tcr.* " + "FROM cta.test_case tc "
					+ "JOIN cta.test_case_results tcr ON tc.idtest_case = tcr.Test_Case_Id " + "WHERE tc.idtest_case = "
					+ tcid + " order by tcr.Executed_Date Desc limit 1";
		}

		JSONArray result = new JSONArray();
		try {
			Statement stmt = testCon.createStatement();
			ResultSet rs = stmt.executeQuery(sql);
			while (rs.next()) {
				JSONObject tcr = getTestCaseResultAsJSON(rs, companyId, false);
				result.add(tcr);
			}

			rs.close();
			stmt.close();
		} catch (Exception e) {
			System.err.println(Utilities.getNow());
			e.printStackTrace();
		}
		return result;
	}

	public JSONArray getCurrentExecutions(int companyId) {

		String sql = "";
		sql = "SELECT * FROM cta.test_case_results as tcr "
				+ "join cta.test_case tc on tcr.Test_Case_Id = tc.idtest_case "
				+ "join cta.modules mods on tc.Module = mods.idmodules "
				+ "join cta.products prod on mods.product = prod.idproducts "
				+ "join cta.company comp on prod.company = comp.idcompany " + "where comp.idcompany =  ? "
				+ "order by idtest_case_results desc limit 50";

		JSONArray result = new JSONArray();
		try {
			PreparedStatement stmt = testCon.prepareStatement(sql);
			stmt.setInt(1, companyId);
			ResultSet rs = stmt.executeQuery();
			while (rs.next()) {
				JSONObject tcr = getTestCaseResultAsJSON(rs, companyId, false);
				result.add(tcr);
			}

			rs.close();
			stmt.close();
		} catch (Exception e) {
			System.err.println(Utilities.getNow());
			e.printStackTrace();
		}
		return result;
	}

	private JSONObject getTestCaseResultAsJSON(ResultSet rs, int companyId, boolean getLatestResults) {
		JSONObject jsonRow = new JSONObject();
		String sql = "SELECT Module FROM cta.test_case WHERE idtest_case = ?";
		try {
			jsonRow.put("idtest_case_results", rs.getInt("idtest_case_results"));
			jsonRow.put("Test_Case_Id", rs.getInt("Test_Case_Id"));
			PreparedStatement stmt = testCon.prepareStatement(sql);
			stmt.setInt(1, rs.getInt("Test_Case_Id"));
			ResultSet rs2 = stmt.executeQuery();
			int module = 0;
			if (rs2.next())
				module = (int) rs2.getInt(1);
			if (getLatestResults) {
				jsonRow.put("Status", rs.getString("tcstatus"));
			} else {
				jsonRow.put("Status", rs.getString("Status"));
			}
			jsonRow.put("Executed_By", rs.getString("Executed_By"));
			jsonRow.put("Executed_Date", (rs.getTimestamp("Executed_Date")).toString());
			jsonRow.put("Duration", rs.getDouble("Duration"));
			jsonRow.put("Browser", rs.getString("Browser"));
//			jsonRow.put("width", rs.getInt("width"));
//			jsonRow.put("height", rs.getInt("height"));
			jsonRow.put("envurl", rs.getString("envurl"));
			jsonRow.put("testcase", rs.getString("test_case_name"));
			String fileurl = AppProperties.fileurl + "?action=executionlog&token=" + randomkey + "&companyid="
					+ companyId + "&tcid=" + rs.getInt("Test_Case_Id") + "&tcrid=" + rs.getInt("idtest_case_results");
			jsonRow.put("logfile", fileurl);
			jsonRow.put("ModuleId", module);
			jsonRow.put("issuetype", rs.getString("issuetype"));
			jsonRow.put("issues", rs.getString("issues"));
			jsonRow.put("firstrun", rs.getString("firstrun"));

			rs2.close();
			stmt.close();
		} catch (Exception e) {
			System.err.println(Utilities.getNow());
			e.printStackTrace();
		}
		return jsonRow;
	}
	
	private JSONObject getTestCaseResultAsJSONVersionTwo(ResultSet rs, int companyId, boolean getLatestResults) {
		JSONObject jsonRow = new JSONObject();
		String sql = "SELECT Module FROM cta.test_case WHERE idtest_case = ?";
		try {
			jsonRow.put("idtest_case_results", rs.getInt("idtest_case_results"));
			jsonRow.put("Test_Case_Id", rs.getInt("Test_Case_Id"));
			PreparedStatement stmt = testCon.prepareStatement(sql);
			stmt.setInt(1, rs.getInt("Test_Case_Id"));
			ResultSet rs2 = stmt.executeQuery();
			int module = 0;
			if (rs2.next())
				module = (int) rs2.getInt(1);
			if (getLatestResults) {
				jsonRow.put("Status", rs.getString("tcstatus"));
			} else {
				jsonRow.put("Status", rs.getString("Status"));
			}
			jsonRow.put("Executed_By", rs.getString("Executed_By"));
			jsonRow.put("Executed_Date", (rs.getTimestamp("Executed_Date")).toString());
			jsonRow.put("Duration", rs.getDouble("Duration"));
			jsonRow.put("Browser", rs.getString("Browser"));
//			jsonRow.put("width", rs.getInt("width"));
//			jsonRow.put("height", rs.getInt("height"));
			jsonRow.put("envurl", rs.getString("envurl"));
			jsonRow.put("testcase", rs.getString("test_case_name"));
			String fileurl = AppProperties.fileurl + "?action=executionlog&token=" + randomkey + "&companyid="
					+ companyId + "&tcid=" + rs.getInt("Test_Case_Id") + "&tcrid=" + rs.getInt("idtest_case_results");
			jsonRow.put("logfile", fileurl);
			jsonRow.put("ModuleId", module);
			jsonRow.put("issuetype", rs.getString("issuetype"));
			jsonRow.put("issues", rs.getString("issues"));
			jsonRow.put("user_note", rs.getString("user_note"));
			jsonRow.put("user_comments", rs.getString("user_comments"));
			jsonRow.put("tcDescription", rs.getString("testcasedescription"));

			rs2.close();
			stmt.close();
		} catch (Exception e) {
			System.err.println(Utilities.getNow());
			e.printStackTrace();
		}
		return jsonRow;
	}

public JSONArray getTestStepResultFromTestCaseResult(int tcrid, boolean justLastOne) {
	String sql = null;
	if(!justLastOne) {
		sql = "SELECT tcr.*, tcr.duration as test_case_duration, tsr.*, "
			+ "tsr.duration as test_step_duration, tsr.Status as test_step_status, tsr.Executed_Date as tsr_executed_date, " +
			"tsr.issuetype as test_step_issuetype, tsr.issues as test_step_issues " +
			"FROM cta.test_case_results tcr " +
			"JOIN cta.test_step_result tsr ON tcr.idtest_case_results = tsr.Test_Case_Results_Id " +
			"WHERE tcr.idtest_case_results = " + tcrid ;
	} else {
		sql = "SELECT tcr.*, tcr.duration as test_case_duration, tsr.*, "
				+ "tsr.duration as test_step_duration, tsr.Status as test_step_status, tsr.Executed_Date as tsr_executed_date " +
				"tsr.issuetype as test_step_issuetype, tsr.issues as test_step_issues " +
				"FROM cta.test_case_results tcr " +
				"JOIN cta.test_step_result tsr ON tcr.idtest_case_results = tsr.Test_Case_Results_Id " +
				"WHERE tcr.idtest_case_results = " + tcrid + " order by tsr_executed_date desc limit 1";
	}
	JSONArray result = new JSONArray();
	try {
		Statement stmt = testCon.createStatement();
		ResultSet rs = stmt.executeQuery(sql);
		while(rs.next()) {
			JSONObject tsr = getTestStepResultAsJSON(rs);
			result.add(tsr);
		}
		rs.close();
		stmt.close();
	} catch(Exception e) {
		System.err.println(Utilities.getNow());
		e.printStackTrace();
	}
	return result;
}


public JSONObject getAnalysisForFailedTestStepByTCRid(int TCRId, MySqlConn2 msc2) {
	
	String fetchTSRIdQuery = "SELECT idtest_step_result FROM cta.test_step_result "
			+ "WHERE Test_Case_Results_Id = ? AND Status = 'FAIL' "
			+ "ORDER BY idtest_step_result DESC LIMIT 1";
	
	JSONObject json = new JSONObject();
	
	try {
		PreparedStatement stmt = testCon.prepareStatement(fetchTSRIdQuery);
		stmt.setInt(1, TCRId);
		ResultSet rs = stmt.executeQuery();

		if (rs.next()) {  // Check if a failed test step exists
			int TSRId = rs.getInt("idtest_step_result");
			json = getAnalysisForFailedTestStep(TSRId, msc2);  // Call the existing method
		} else {
			json.put("error", "No failed test steps found for the given TestCaseResult id: " + TCRId);
		}

		rs.close();
		stmt.close();
	} catch (Exception e) {
		System.err.println(Utilities.getNow());
		e.printStackTrace();
		json.put("error", "An error occurred while fetching the analysis for TestCaseResult id: " + TCRId);
	}
	
	return json;
	
}

public JSONObject getAnalysisForFailedTestStep(int TSRId, MySqlConn2 msc2) {
	
	String sql = "SELECT Test_Step FROM cta.test_step_result WHERE idtest_step_result = ?";
	String fail_step_sql = "SELECT tsr.*, tsr.duration AS test_step_duration, "
			+ "tsr.Status AS test_step_status, tsr.Executed_Date AS tsr_executed_date, "
			+ "tsr.issuetype as test_step_issuetype, tsr.issues as test_step_issues, "
			+ "tcr.Test_Case_Id, tcr.idtest_case_results, tcr.duration AS test_case_duration "
			+ "FROM cta.test_step_result tsr JOIN cta.test_case_results tcr "
			+ "ON tsr.Test_Case_Results_Id = tcr.idtest_case_results "
			+ "WHERE tsr.Test_Step = ? ORDER BY tsr_executed_date DESC;";
	String sql_recommended = "SELECT * FROM cta.recommended_actions WHERE error_code = ?";
	
	JSONObject json = new JSONObject();
	JSONObject ourAssessment = new JSONObject();
	JSONArray result = new JSONArray();
	try {
		PreparedStatement stmt = testCon.prepareStatement(sql);
		stmt.setInt(1, TSRId);
		ResultSet rs = stmt.executeQuery();
		rs.next();
		int TSId = rs.getInt(1);
		
		stmt = testCon.prepareStatement(fail_step_sql);
		stmt.setInt(1, TSId);
		ResultSet rs2 = stmt.executeQuery();
		int companyId = (int)userDetails.get("companyid");
		boolean foundAssessment = false;
		
		while(rs2.next()) {
			JSONObject tsr = getTestStepResultAsJSON(rs2);
			if (rs2.getInt("Step_Number") != 1) {
				String fileurl = AppProperties.fileurl + "?action=pageSource&token="+randomkey+"&companyid="+companyId+
						"&tcid="+rs2.getInt("Test_Case_Id")+"&tcrid="+rs2.getInt("Test_Case_Results_Id")+"&stepNumber="+rs2.getInt("Step_Number");
				tsr.put("page_source_file", fileurl);
			}
			String analysisFileurl = AppProperties.fileurl + "?action=analysisLog&token="+randomkey+"&companyid="+companyId+
					"&tcid="+rs2.getInt("Test_Case_Id")+"&tcrid="+rs2.getInt("Test_Case_Results_Id");
			tsr.put("analysis_log_file", analysisFileurl);
			String logFileurl = AppProperties.fileurl + "?action=executionlog&token="+randomkey+"&companyid="+companyId+"&tcid="+rs2.getInt("Test_Case_Id")+"&tcrid="+rs2.getInt("Test_Case_Results_Id");
			tsr.put("logFile", logFileurl);
			tsr.put("outerhtml", rs2.getString("outerhtml"));
			
			JSONObject imageDiffs = msc2.getImageDiffForTestStep(rs2.getInt("Test_Step"), rs2.getInt("Step_Number"), companyId,
                    rs2.getInt("Test_Case_Id"), randomkey, rs2.getInt("Test_Case_Results_Id"));
			tsr.put("imageDiff", imageDiffs);
			result.add(tsr);
			
			// Check for the first non-empty issueType and issues
            if (!foundAssessment) {
                String issueType = rs2.getString("test_step_issuetype");
                String issues = rs2.getString("test_step_issues");

                if (issueType != null && !issueType.trim().isEmpty()) {
                    ourAssessment.put("issueType", issueType);
                    ourAssessment.put("issues", issues);
                    stmt = testCon.prepareStatement(sql_recommended);
            		stmt.setString(1, issueType);
            		ResultSet rs3 = stmt.executeQuery();
            		String recommended_action = "";
            		if(rs3.next()) {
            			recommended_action = rs3.getString("recommended_action");
            		}
            		ourAssessment.put("recommended_action", recommended_action);
                    foundAssessment = true;
                }
            }
		}
		
		JSONObject details = new JSONObject();
		String detailsQuery = "SELECT " +
		               "ts.idtest_step, " +
		               "ts.tsSequence AS step_number, " +
		               "ts.Test_Case_Id, " +
		               "tc.idtest_case, " +
		               "tc.Test_Case AS test_case_name " +
		               "FROM cta.test_step ts " +
		               "JOIN cta.test_case tc ON ts.Test_Case_Id = tc.idtest_case " +
		               "WHERE ts.idtest_step = ?";

		stmt = testCon.prepareStatement(detailsQuery);
		stmt.setInt(1, TSId);
		ResultSet detailsRs = stmt.executeQuery();

		if (detailsRs.next()) {
		    details.put("test_step_id", detailsRs.getInt("idtest_step"));
		    details.put("step_number", detailsRs.getInt("step_number"));
		    details.put("test_case_id", detailsRs.getInt("idtest_case"));
		    details.put("test_case_name", detailsRs.getString("test_case_name"));
		}

		detailsRs.close();

		json.put("details", details);
		json.put("tableData", result);
		json.put("ourAssessment", ourAssessment);
		
		rs.close();
		rs2.close();
		stmt.close();
	} catch (Exception e) {
		System.err.println(Utilities.getNow());
		e.printStackTrace();
	}
	
	return json;
}

	private JSONObject getTestStepResultAsJSON(ResultSet rs) {

		JSONObject jsonRow = new JSONObject();
		try {
			jsonRow.put("idtest_step_result", rs.getInt("idtest_step_result"));
			jsonRow.put("Step_Number", rs.getInt("Step_Number"));
			jsonRow.put("Page_Name", rs.getString("Page_Name"));
			jsonRow.put("Page_Description", rs.getString("Page_Description"));
			jsonRow.put("Object_Xpath", rs.getString("Object_Xpath"));
			jsonRow.put("Keyword", rs.getString("Keyword"));
			jsonRow.put("Action", rs.getString("Action"));
			jsonRow.put("Flow", rs.getString("Flow"));
			jsonRow.put("TestData", rs.getString("TestData"));
			jsonRow.put("VarName", rs.getString("VarName"));
			jsonRow.put("Status", rs.getString("Status"));
			jsonRow.put("test_step_status", rs.getString("test_step_status"));
			String filename = rs.getString("Failure_Screenshot_Location");
			if (filename != null && !filename.equals("")) {
				String fileurl = AppProperties.fileurl + "?action=downloadFile&token=" + randomkey + "&fileName="
						+ filename;
				jsonRow.put("Failure_Screenshot_Location", fileurl);
			} else {
				jsonRow.put("Failure_Screenshot_Location", rs.getString("Failure_Screenshot_Location"));
			}
			String filename2 = rs.getString("screenshot1");
			if (filename2 != null && !filename2.equals("")) {
				String fileurl2 = AppProperties.fileurl + "?action=downloadFile&token=" + randomkey + "&fileName="
						+ filename2;
				jsonRow.put("screenshot1", fileurl2);
			} else {
				jsonRow.put("screenshot1", rs.getString("screenshot1"));
			}
			jsonRow.put("Test_Step", rs.getInt("Test_Step"));
			jsonRow.put("Executed_By", rs.getString("Executed_By"));
			jsonRow.put("Duration", rs.getDouble("Duration"));
//			int dur = rs.getInt("test_step_duration");
			double divisionResult = rs.getDouble("test_step_duration");
			String formattedResult = String.format("%.2f", divisionResult);
			Float duration = Float.parseFloat(formattedResult);
			jsonRow.put("test_step_duration", duration);
			jsonRow.put("Test_Case_Results_Id", rs.getInt("Test_Case_Results_Id"));
			jsonRow.put("ValDevice", rs.getString("ValDevice"));
			jsonRow.put("Executed_Date", (rs.getTimestamp("Executed_Date")).toString());
			jsonRow.put("test_step_issues", rs.getString("test_step_issues"));
			jsonRow.put("test_step_issuetype", rs.getString("test_step_issuetype"));
			jsonRow.put("Test_Case_Results_Id", rs.getInt("Test_Case_Results_Id"));
			jsonRow.put("issueType", duration);
			int companyId = (int)userDetails.get("companyid");

		} catch (Exception e) {
			System.err.println(Utilities.getNow());
			e.printStackTrace();
		}
		return jsonRow;
	}

	public JSONObject getCompany(String company) {

		try {
			String sql = "SELECT * FROM cta.company WHERE companyname = ?";
			PreparedStatement stmt = testCon.prepareStatement(sql);
			stmt.setString(1, company);
			ResultSet rs = stmt.executeQuery();
			if (rs.next()) {
				JSONObject Company = getCompanyAsJSON(rs);
				return Company;
			}
			rs.close();
			stmt.close();
		} catch (Exception e) {
			System.err.println(Utilities.getNow());
			e.printStackTrace();
		}

		return null;
	}

	public int getparallelThreadsForCompany(int companyId) {

		try {
			String sql = "SELECT parallelThreads FROM cta.company WHERE idcompany = ?";
			PreparedStatement stmt = testCon.prepareStatement(sql);
			stmt.setInt(1, companyId);
			ResultSet rs = stmt.executeQuery();
			if (rs.next()) {
				return rs.getInt(1);
			}
			rs.close();
			stmt.close();
		} catch (Exception e) {
			System.err.println(Utilities.getNow());
			e.printStackTrace();
		}

		return 1;
	}

	public JSONObject getCompanyAsJSON(ResultSet rs) {
		JSONObject result = new JSONObject();
		try {
			result.put("idcompany", rs.getInt("idcompany"));
			result.put("companyname", rs.getString("companyname"));
			result.put("companycontactname", rs.getString("companycontactname"));
			result.put("status", rs.getInt("status"));
			result.put("parallelThreads", rs.getInt("parallelThreads"));
			result.put("stepsrecording", rs.getString("stepsrecording"));
			result.put("coverage", rs.getString("coverage"));
			result.put("backendcoverage", rs.getString("backendcoverage"));
			result.put("apidata", rs.getString("apidata"));
			result.put("videorecording", rs.getString("videorecording"));
			result.put("bulktest", rs.getString("bulktest"));
			result.put("gentype", rs.getString("gentype"));
			result.put("tabopenwaittime", rs.getInt("tabopenwaittime"));
			result.put("downloadwaittime", rs.getInt("downloadwaittime"));
			result.put("dashboardrefreshrate", rs.getInt("dashboardrefreshrate"));
			result.put("companyurl", rs.getString("companyurl"));
			result.put("logintype", rs.getString("logintype"));

		} catch (Exception e) {
			System.err.println(Utilities.getNow());
			e.printStackTrace();
		}

		return result;
	}

	public void addProducts(String productName, int companyId) {
		String sql = "INSERT INTO cta.products (productname, company) VALUES " + "(?,?)";
		try {
			PreparedStatement stmt = testCon.prepareStatement(sql);

			stmt.setString(1, productName);
			stmt.setInt(2, companyId);
			stmt.executeUpdate();
			stmt.close();
		} catch (Exception e) {
			System.err.println(Utilities.getNow());
			e.printStackTrace();
		}
	}

	public void addCompanyUrl(String companyUrl, int companyId) {
		String sql = "UPDATE cta.company SET companyurl = " + "(?) where idcompany = ?";
		try {
			PreparedStatement stmt = testCon.prepareStatement(sql);

			stmt.setString(1, companyUrl);
			stmt.setInt(2, companyId);
			stmt.executeUpdate();
			stmt.close();
		} catch (Exception e) {
			System.err.println(Utilities.getNow());
			e.printStackTrace();
		}
	}

	public int addModules(String moduleName, int prodid) {
		String sql = "INSERT INTO cta.modules (modulename, product) VALUES " + "(?,?)";
		int modid = -1;
		try {
			PreparedStatement stmt = testCon.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
			stmt.setString(1, moduleName);
			stmt.setInt(2, prodid);
			stmt.executeUpdate();
			ResultSet generatedKeys = stmt.getGeneratedKeys();
			if (generatedKeys.next()) {
				modid = (generatedKeys.getBigDecimal(1)).intValue();
			}

			generatedKeys.close();
			stmt.close();
		} catch (Exception e) {
			System.err.println(Utilities.getNow());
			e.printStackTrace();
		}

		return modid;
	}

	public int checkandAddModule(String moduleName, int prodid) {
		String sql = "SELECT * FROM cta.modules where product = ? " + "AND modulename = ?";
		int moduleId = -1;
		try {
			PreparedStatement stmt = testCon.prepareStatement(sql);
			stmt.setInt(1, prodid);
			stmt.setString(2, moduleName);
			ResultSet rs = stmt.executeQuery();

			if (!rs.next()) {
				moduleId = addModules(moduleName, prodid);
			} else {
				moduleId = rs.getInt("idmodules");
			}
			rs.close();
			stmt.close();
		} catch (Exception e) {
			System.err.println(Utilities.getNow());
			e.printStackTrace();
		}

		return moduleId;
	}

	public int checkandAddTestCase(String moduleName, int prodid, int width, int height, int randomkey) {

		int moduleId = checkandAddModule(moduleName, prodid);
		String sql = "SELECT * FROM cta.test_case where Module = ? " + "AND Test_Case = ? AND Status != 1";
		int testcaseId = -1;
		try {
			PreparedStatement stmt = testCon.prepareStatement(sql);
			stmt.setInt(1, moduleId);
			stmt.setString(2, moduleName);
			ResultSet rs = stmt.executeQuery();

			if (!rs.next()) {
				testcaseId = checkAndWriteTestCaseToDB(moduleName, "NA", 1, "SYS", AppProperties.RECORDING, randomkey,
						moduleId, width, height);
			} else {
				testcaseId = rs.getInt("idtest_case");
			}
			rs.close();
			stmt.close();
		} catch (Exception e) {
			System.err.println(Utilities.getNow());
			e.printStackTrace();
		}

		return testcaseId;
	}

	public void addURLS(int productId, String urlname) {
		String sql = "INSERT INTO cta.urls (urls, productid) VALUES " + "(?,?)";
		try {
			PreparedStatement stmt = testCon.prepareStatement(sql);
			stmt.setString(1, urlname);
			stmt.setInt(2, productId);
			stmt.executeUpdate();
			stmt.close();
		} catch (Exception e) {
			System.err.println(Utilities.getNow());
			e.printStackTrace();
		}
	}

	public void addResolution(int productId, int width, int height) {
		String sql = "INSERT INTO cta.resolutions (width, height, productid) VALUES " + "(?,?,?)";
		try {
			PreparedStatement stmt = testCon.prepareStatement(sql);
			stmt.setInt(1, width);
			stmt.setInt(2, height);
			stmt.setInt(3, productId);
			stmt.executeUpdate();
			stmt.close();
		} catch (Exception e) {
			System.err.println(Utilities.getNow());
			e.printStackTrace();
		}
	}

	public void addCredential(String username, String password, String company, int productId) {
		String sql = "INSERT INTO cta.creds (username, password, company, productid) VALUES " + "(?,?,?,?)";
		try {
			PreparedStatement stmt = testCon.prepareStatement(sql);
			stmt.setString(1, username);
			stmt.setString(2, password);
			stmt.setString(3, company);
			stmt.setInt(4, productId);
			stmt.executeUpdate();
			stmt.close();
		} catch (Exception e) {
			System.err.println(Utilities.getNow());
			e.printStackTrace();
		}
	}

	public JSONObject getData(int prodid) {
		String url_sql = "SELECT * FROM cta.urls WHERE productid = ?";
		String res_sql = "SELECT * FROM cta.resolutions WHERE productid = ?";
		String cred_sql = "SELECT * FROM cta.creds WHERE productid = ?";

		JSONObject json = new JSONObject();
		try {
			JSONArray urlList = new JSONArray();

			PreparedStatement stmt_url = testCon.prepareStatement(url_sql);
			stmt_url.setInt(1, prodid);
			ResultSet rs = stmt_url.executeQuery();
			while (rs.next()) {
				JSONObject Urls = getUrlAsJSON(rs);
				urlList.add(Urls);
			}

			json.put("URLList", urlList);

			PreparedStatement stmt_res = testCon.prepareStatement(res_sql);
			stmt_res.setInt(1, prodid);
			rs = stmt_res.executeQuery();
			JSONArray resolutionList = new JSONArray();
			while (rs.next()) {
				JSONObject resolution = getResolutionAsJSON(rs);
				resolutionList.add(resolution);
			}

			json.put("ResolutionList", resolutionList);

			PreparedStatement stmt_cred = testCon.prepareStatement(cred_sql);
			stmt_cred.setInt(1, prodid);
			rs = stmt_cred.executeQuery();
			if (rs.next()) {
				json.put("idcreds", rs.getInt("idcreds"));
				json.put("email", rs.getString("username"));
				json.put("password", rs.getString("password"));
				json.put("company", rs.getString("company"));
			}

			json.put("idproducts", prodid);

			rs.close();
			stmt_cred.close();
			stmt_res.close();
			stmt_url.close();

		} catch (Exception e) {
			System.err.println(Utilities.getNow());
			e.printStackTrace();
		}
		return json;
	}

	private JSONObject getUrlAsJSON(ResultSet rs) {
		JSONObject result = new JSONObject();
		try {
			result.put("urlId", rs.getInt("idurls"));
			result.put("urlName", rs.getString("urls"));
			result.put("idproducts", rs.getInt("productid"));
		} catch (Exception e) {
			System.err.println(Utilities.getNow());
			e.printStackTrace();
		}

		return result;
	}

	private JSONObject getResolutionAsJSON(ResultSet rs) {
		JSONObject result = new JSONObject();
		try {
			result.put("idresolutions", rs.getInt("idresolutions"));
			result.put("Width", rs.getInt("width"));
			result.put("Height", rs.getInt("height"));
			result.put("productid", rs.getInt("productid"));
		} catch (Exception e) {
			System.err.println(Utilities.getNow());
			e.printStackTrace();
		}

		return result;
	}

	private JSONObject getCredentialAsJSON(ResultSet rs) {
		JSONObject result = new JSONObject();
		try {
			result.put("idcreds", rs.getInt("idcreds"));
			result.put("username", rs.getString("username"));
			result.put("password", rs.getString("password"));
			result.put("company", rs.getString("company"));
			result.put("productid", rs.getInt("productid"));
		} catch (Exception e) {
			System.err.println(Utilities.getNow());
			e.printStackTrace();
		}

		return result;
	}

	public HashMap getSysProps() {
		HashMap sysprops = new HashMap();
		try {
			String sql = "SELECT * FROM cta.sysprops";
			PreparedStatement stmt = testCon.prepareStatement(sql);
			ResultSet rs = stmt.executeQuery();
			while (rs.next()) {

				sysprops.put(rs.getString("name"), rs.getString("value"));
			}

			rs.close();
			stmt.close();
		} catch (Exception e) {
			System.err.println(Utilities.getNow());
			e.printStackTrace();
		}
		return sysprops;
	}

	public boolean getCoverageStatus(int cid) {
		boolean result = false;
		try {
			String sql = "SELECT backendcoverage FROM cta.company WHERE idcompany = ?";
			PreparedStatement stmt = testCon.prepareStatement(sql);
			stmt.setInt(1, cid);
			ResultSet rs = stmt.executeQuery();
			if (rs.next()) {
				String status = rs.getString(1);
				if (status.equals("false") || status.equals("")) {
					result = false;
				} else
					result = true;
			}
		} catch (Exception e) {
			System.err.println(Utilities.getNow());
			e.printStackTrace();
		}
		return result;
	}

	public boolean getTCCoverageStatus(int tcid) {
		boolean result = false;
		try {
			String sql = "SELECT coverageflag FROM cta.test_case WHERE idtest_case = ?";
			PreparedStatement stmt = testCon.prepareStatement(sql);
			stmt.setInt(1, tcid);
			ResultSet rs = stmt.executeQuery();
			if (rs.next()) {
				String status = rs.getString(1);
				if (status.equals("false") || status.equals("")) {
					result = false;
				} else
					result = true;
			}
			rs.close();
			stmt.close();
		} catch (Exception e) {
			System.err.println(Utilities.getNow());
			e.printStackTrace();
		}
		return result;
	}

	public boolean getCoverageExecuteStatus(int cid) {
		boolean result = false;
		try {
			String sql = "SELECT coveragetrigger FROM cta.company WHERE idcompany = ?";
			PreparedStatement stmt = testCon.prepareStatement(sql);
			stmt.setInt(1, cid);
			ResultSet rs = stmt.executeQuery();
			if (rs.next()) {
				String status = rs.getString(1);
				if (status.equals("false") || status.equals("")) {
					result = false;
				} else
					result = true;
			}
			rs.close();
			stmt.close();
		} catch (Exception e) {
			System.err.println(Utilities.getNow());
			e.printStackTrace();
		}
		return result;
	}

	public void updateTCCoverageStatus(int tcid) {
		try {
			String sql = "UPDATE cta.test_case SET coverageflag='true' WHERE idtest_case = ?";
			PreparedStatement stmt = testCon.prepareStatement(sql);
			stmt.setInt(1, tcid);
			stmt.executeUpdate();

			stmt.close();
		} catch (Exception e) {
			System.err.println(Utilities.getNow());
			e.printStackTrace();
		}
	}

	public HashMap getCustProps() {
		HashMap custProps = new HashMap();
		try {
			String sql = "SELECT * FROM cta.custprops";
			PreparedStatement stmt = testCon.prepareStatement(sql);
			ResultSet rs = stmt.executeQuery();
			while (rs.next()) {
				int prodid = rs.getInt("productid");
				Object map = custProps.get(prodid);
				HashMap prodMap = null;
				if (map == null) {
					prodMap = new HashMap();
					custProps.put(prodid, prodMap);
				} else {
					prodMap = (HashMap) map;
				}
				prodMap.put(rs.getString("name"), rs.getString("value"));
			}

			rs.close();
			stmt.close();
		} catch (Exception e) {
			System.err.println(Utilities.getNow());
			e.printStackTrace();
		}
		return custProps;
	}

	public int gettcidfromTestStep(int TSId) {
		int TCId = -1;
		try {
			String sql = "SELECT Test_Case_Id FROM cta.test_step WHERE idtest_step = ?";
			PreparedStatement stmt = testCon.prepareStatement(sql);
			stmt.setInt(1, TSId);
			ResultSet rs = stmt.executeQuery();
			rs.next();
			TCId = rs.getInt(1);
			rs.close();
			stmt.close();
		} catch (Exception e) {

		}

		return TCId;
	}

	public JSONArray getFailTestSteps(int TSRId, int companyId) {
		String sql = "SELECT Test_Step FROM cta.test_step_result WHERE idtest_step_result = ?";
		String fail_sql = "SELECT * FROM cta.test_step_result WHERE Test_Step = ? order by Executed_Date desc";
		JSONArray result = new JSONArray();
		try {
			PreparedStatement stmt = testCon.prepareStatement(sql);
			stmt.setInt(1, TSRId);
			ResultSet rs = stmt.executeQuery();
			rs.next();
			int TSId = rs.getInt(1);

//		sql = "SELECT Test_Case_Id FROM cta.test_step WHERE idtest_step = ?";
//		stmt = testCon.prepareStatement(sql);
//		stmt.setInt(1, TSId);
//		rs = stmt.executeQuery();
//		rs.next();
//		int TCId = rs.getInt(1);
			int TCId = gettcidfromTestStep(TSId);

			sql = "SELECT Test_Case FROM cta.test_case WHERE idtest_case = ?";
			stmt = testCon.prepareStatement(sql);
			stmt.setInt(1, TCId);
			rs = stmt.executeQuery();
			rs.next();
			String TCName = rs.getString(1);

			stmt = testCon.prepareStatement(fail_sql);
			stmt.setInt(1, TSId);
			ResultSet rs2 = stmt.executeQuery();
			do {
				JSONObject jsonArr = getTestStepsResultsAsJSON(rs2, false, companyId, false, "FAILED");
				jsonArr.put("TestCase", TCName);
				result.add(jsonArr);
			} while (rs2.next());

			rs.close();
			rs2.close();
			stmt.close();
		} catch (Exception e) {
			System.err.println(Utilities.getNow());
			e.printStackTrace();
		}
		return result;
	}

	public void updateTestStepChangedData(int teststepid, String testdatasource, String testdatavalue) {
		String sql = null;
		try {
//		sql = "UPDATE cta.test_step SET testdata_source = ?, TestData = ? " + " WHERE idtest_step = ?";
			sql = "UPDATE cta.test_step SET TestData = ? " + " WHERE idtest_step = ?";
			PreparedStatement stmt = testCon.prepareStatement(sql);
//		stmt.setString(1, testdatasource);
			stmt.setString(1, testdatavalue);
			stmt.setInt(2, teststepid);
			stmt.executeUpdate();
			stmt.close();
		} catch (Exception e) {
			System.err.println(Utilities.getNow());
			e.printStackTrace();
		}
	}

	public void validateTestStepChangedData(int teststepid, String testdatasource, String testdatavalue) {
		String sql = null;
		try {
			sql = "UPDATE cta.test_step SET TestData = ? " + " WHERE idtest_step = ?";
			PreparedStatement stmt = testCon.prepareStatement(sql);
//		stmt.setString(1, testdatasource);
			stmt.setString(1, testdatavalue);
			stmt.setInt(2, teststepid);
			stmt.executeUpdate();
			stmt.close();
		} catch (Exception e) {
			System.err.println(Utilities.getNow());
			e.printStackTrace();
		}
	}

	public void changePassword(String username, String currentPassword, String newPassword) {
		String sql = "UPDATE cta.userprofile SET password = ?" + " WHERE username = ?";
		String val_sql = "SELECT password FROM cta.userprofile WHERE username = ?";
		try {
			String hashedPwd = PasswordUtils.hashPassword(newPassword, AppProperties.hashingsalt);
			PreparedStatement stmt = testCon.prepareStatement(val_sql);
			stmt.setString(1, username);
			ResultSet rs = stmt.executeQuery();
			rs.next();
			String prevPassword = rs.getString(1);
			String hashedCurPwd = PasswordUtils.hashPassword(currentPassword, AppProperties.hashingsalt);
			if (prevPassword.equals(hashedCurPwd)) {
				stmt = testCon.prepareStatement(sql);
				stmt.setString(1, hashedPwd);
				stmt.setString(2, username);
				stmt.executeUpdate();
			} else {
				System.err.println("Wrong current password or username doesnot exist");
			}

			rs.close();
			stmt.close();
		} catch (Exception e) {
			System.err.println(Utilities.getNow());
			e.printStackTrace();
		}

	}

	public JSONArray getEnv(int pid) {
		String sql = "SELECT * FROM cta.envdetails WHERE productid = ?";
		JSONArray result = new JSONArray();
		try {
			PreparedStatement stmt = testCon.prepareStatement(sql);
			stmt.setInt(1, pid);
			ResultSet rs = stmt.executeQuery();
			while (rs.next()) {
				JSONObject json = getEnvAsJSON(rs);
				result.add(json);
			}

			rs.close();
			stmt.close();
		} catch (Exception e) {
			System.err.println(Utilities.getNow());
			e.printStackTrace();
		}
		return result;
	}

	public JSONObject getEnvAsJSON(ResultSet rs) {
		JSONObject result = new JSONObject();
		try {
			result.put("idenvdetails", rs.getInt("idenvdetails"));
			result.put("envname", rs.getString("envname"));
			result.put("envurl", rs.getString("envurl"));
			result.put("productid", rs.getInt("productid"));
		} catch (Exception e) {
			System.err.println(Utilities.getNow());
			e.printStackTrace();
		}
		return result;
	}

	public void addEnv(String envname, String envurl, int productId) {
		String sql = "INSERT INTO cta.envdetails (envname, envurl, productid) VALUES " + "(?,?,?)";
		try {
			PreparedStatement stmt = testCon.prepareStatement(sql);
			stmt.setString(1, envname);
			stmt.setString(2, envurl);
			stmt.setInt(3, productId);
			stmt.executeUpdate();
		} catch (Exception e) {
			System.err.println(Utilities.getNow());
			e.printStackTrace();
		}
	}

	public void updateTestStepFromURL(int tsid, String URLName, String URL) {
		String sql = "UPDATE cta.test_step SET TestData = ? " + " WHERE idtest_step = ?";
		String update_sql = "UPDATE cta.test_step SET TestData = ?, RecordedData = ? " + "WHERE idtest_step = ?";
		String comp_sql = "SELECT * FROM cta.test_step WHERE idtest_step = ?";
		try {

			PreparedStatement stmt = testCon.prepareStatement(sql);
			PreparedStatement update_stmt = testCon.prepareStatement(update_sql);
			PreparedStatement comp_stmt = testCon.prepareStatement(comp_sql);
			comp_stmt.setInt(1, tsid);
			ResultSet rs = comp_stmt.executeQuery();
			while (rs.next()) {
				String recordedData = rs.getString("RecordedData");
				String testData = rs.getString("TestData");
				if (!recordedData.equals("")) {
					stmt.setString(1, URL);
					stmt.setInt(2, tsid);
					stmt.executeUpdate();
				} else {
					update_stmt.setString(1, URL);
					update_stmt.setString(2, testData);
					update_stmt.setInt(3, tsid);
					update_stmt.executeUpdate();
				}
			}

		} catch (Exception e) {
			System.err.println(Utilities.getNow());
			e.printStackTrace();
		}

	}

	public void audit(JSONObject jsonStr) {
		int randomkey = -1;
		Object randomkeyObj = jsonStr.get("randomkey");
		if (randomkeyObj != null) {
			if (jsonStr.get("randomkey") instanceof Long) {
				randomkey = ((Long) randomkeyObj).intValue();
			} else {
				randomkey = Integer.valueOf((String) jsonStr.get("randomkey"));
			}
		}

		String user = (String) jsonStr.get("userid");
		String action = (String) jsonStr.get("action");
		String object = "";
		if (jsonStr.get("object") != null) {
			object = ((JSONObject) jsonStr.get("object")).toString();
		}
		String result = (String) jsonStr.get("result");
		String adddet = (String) jsonStr.get("adddet");

		String sql = "insert into cta.auditlog (randomkey, user, timestamp, action,"
				+ "object, result, additionaldetails) VALUES (?, ?, ?, ?, ?, ?, ?)";
		try {
			if (testCon == null | testCon.isClosed()) {
				getDbConn();
			}
			PreparedStatement stmt = testCon.prepareStatement(sql);

			stmt.setInt(1, randomkey);
			stmt.setString(2, user);
			stmt.setTimestamp(3, Utilities.getCurrentTimestamp());
			stmt.setString(4, action);
			stmt.setString(5, object);
			stmt.setString(6, result);
			stmt.setString(7, adddet);
			stmt.executeUpdate();
			stmt.close();
		} catch (Exception e) {
			System.err.println(Utilities.getNow());
			e.printStackTrace();
		}
	}

	public void deleteTestSuiteCase(int testsuiteid) {
		String del_sql = "UPDATE cta.test_suite SET status = '1' WHERE idtest_suite = ?";
		try {
			PreparedStatement deletestmt = testCon.prepareStatement(del_sql);
			deletestmt.setInt(1, testsuiteid);
			deletestmt.executeUpdate();
		} catch (Exception e) {
			System.err.println(Utilities.getNow());
			e.printStackTrace();
		}
	}

	public void editTestSuiteCase(int testsuiteid, String newName) {
		String edit_sql = "UPDATE cta.test_suite SET Test_Suite = ? WHERE idtest_suite = ?";
		try {
			PreparedStatement editstmt = testCon.prepareStatement(edit_sql);
			editstmt.setString(1, newName);
			editstmt.setInt(2, testsuiteid);
			editstmt.executeUpdate();
		} catch (Exception e) {
			System.err.println(Utilities.getNow());
			e.printStackTrace();
		}
	}

	public JSONArray getTestSuiteResultsFromTestSuite(int testSuiteId) {

		JSONArray jsonArr = new JSONArray();
		JSONObject json = null;
		try {
			String sql = "SELECT * FROM cta.test_suite_results WHERE Test_Suite_Id = ? "
					+ "order by Executed_Date desc LIMIT 50";
			PreparedStatement stmt = testCon.prepareStatement(sql);
			stmt.setInt(1, testSuiteId);
			ResultSet resultSet = stmt.executeQuery();
			while (resultSet.next()) {
				json = getTestSuiteResultsAsJSON(resultSet);
				jsonArr.add(json);
			}
			resultSet.close();
			stmt.close();
		} catch (Exception e) {
			System.err.println(Utilities.getNow());
			e.printStackTrace();
		}
		return jsonArr;
	}

	public JSONArray getTestSuiteResultsFromTestSuite(int testSuiteId, int TSRid) {
		String email_sql = "SELECT * FROM cta.test_suite_results WHERE Test_Suite_Id = ? AND idtest_suite_results = ?";
		String sql = "SELECT * FROM cta.test_suite_results WHERE Test_Suite_Id = ? order by Executed_Date desc";
		JSONArray jsonArr = new JSONArray();
		JSONObject json = null;
		JSONObject email_json = null;
		try {
			PreparedStatement email_stmt = testCon.prepareStatement(email_sql);
			email_stmt.setInt(1, testSuiteId);
			email_stmt.setInt(2, TSRid);
			ResultSet rs = email_stmt.executeQuery();
			if (rs.next()) {
				email_json = getTestSuiteResultsAsJSON(rs);
				jsonArr.add(email_json);
			}
			PreparedStatement stmt = testCon.prepareStatement(sql);
			stmt.setInt(1, testSuiteId);
			ResultSet resultSet = stmt.executeQuery();
			while (resultSet.next()) {
				json = getTestSuiteResultsAsJSON(resultSet);
				jsonArr.add(json);
			}
			resultSet.close();
			stmt.close();
		} catch (Exception e) {
			System.err.println(Utilities.getNow());
			e.printStackTrace();
		}
		return jsonArr;
	}

	private JSONObject getTestSuiteResultsAsJSON(ResultSet rs) {
		JSONObject jsonRow = new JSONObject();
		try {
			int tsrid = rs.getInt("idtest_suite_results");
			int tsid = rs.getInt("Test_Suite_Id");
			jsonRow.put("idtest_suite_results", tsrid);
			jsonRow.put("Test_Suite_Id", tsid);
			jsonRow.put("Status", rs.getString("Status"));
			jsonRow.put("Executed_By", rs.getString("Executed_By"));
			jsonRow.put("Executed_Date", (rs.getTimestamp("Executed_Date")).toString());
			jsonRow.put("Duration", rs.getDouble("Duration"));
			jsonRow.put("passCount", rs.getInt("passCount"));
			jsonRow.put("failCount", rs.getInt("failCount"));
			jsonRow.put("rerunfailedtests", rs.getString("rerunfailedtests"));
			jsonRow.put("parallelrun", rs.getString("parallelrun"));
			int companyid = (int) userDetails.get("companyid");
			String fileurl = AppProperties.fileurl + "?action=executionlog&token=" + randomkey + "&companyid="
					+ companyid + "&tsrid=" + tsrid + "&tsid=" + tsid;
			jsonRow.put("logfile", fileurl);
			jsonRow.put("totalCount", rs.getInt("totalCount"));
			jsonRow.put("buildtag", rs.getString("buildtag"));
			int isIgnoredValue = rs.getInt("isIgnored");
			boolean isIgnored = isIgnoredValue == 1;
			jsonRow.put("isIgnored", isIgnored);
			jsonRow.put("depTcCountRunOne", rs.getInt("depTcCountRunOne"));
			jsonRow.put("depTcCountRunTwo", rs.getInt("depTcCountRunTwo"));
			jsonRow.put("firstRunPassCount", rs.getInt("firstRun_passCount"));
			jsonRow.put("firstRunFailCount", rs.getInt("firstRun_failCount"));
			jsonRow.put("secondRunPassCount", rs.getInt("secondRun_passCount"));
			jsonRow.put("secondRunFailCount", rs.getInt("secondRun_failCount"));
		} catch (Exception e) {
			System.err.println(Utilities.getNow());
			e.printStackTrace();
		}
		return jsonRow;
	}

	public JSONObject getTestCaseResultFromTestSuiteResult(int tsrid, int companyId, String criteria) {
		String all_sql = "SELECT * FROM cta.test_case_results tcr JOIN cta.test_suite_case_results tscr ON "
				+ "tscr.test_case_results_id = tcr.idtest_case_results WHERE tscr.test_suite_results_id = ?"
				+ " order by tcr.Executed_Date Desc";

		JSONArray tcresult = new JSONArray();
		JSONObject json = new JSONObject();
		ResultSet rs = null;
		try {
			PreparedStatement stmt = testCon.prepareStatement(all_sql);
			stmt.setInt(1, tsrid);
			rs = stmt.executeQuery();

			while (rs.next()) {
				JSONObject tcr = getTestCaseResultAsJSON(rs, companyId, false);
				tcresult.add(tcr);
			}
			json.put("testcaseresult", tcresult);
			rs.close();
			stmt.close();
		} catch (Exception e) {
			System.err.println(Utilities.getNow());
			e.printStackTrace();
		}
		return json;
	}

	public void deleteTestCaseResultByID(int oldtsrid, int newtsrid) {
		String sql = "DELETE FROM test_case_results where idtest_case_results = ?";
		String sql2 = "UPDATE testcaseexecutionlog set Test_Case_Results_Id = ? where " + "Test_Case_Results_Id = ?";

		try {
			PreparedStatement stmt = testCon.prepareStatement(sql2);
			stmt.setInt(1, newtsrid);
			stmt.setInt(2, oldtsrid);
			stmt.execute();

			stmt = testCon.prepareStatement(sql);
			stmt.setInt(1, oldtsrid);
			stmt.execute();
			stmt.close();
		} catch (Exception e) {
			System.err.println(Utilities.getNow());
			e.printStackTrace();
		}
	}

	public JSONObject getFailedTestCaseResultFromTestSuiteResult(int tsrid, int companyId, String criteria) {
		String all_sql = "SELECT * FROM cta.test_case_results tcr JOIN cta.test_suite_case_results tscr ON "
				+ "tscr.test_case_results_id = tcr.idtest_case_results WHERE tscr.test_suite_results_id = ? AND Status <> \"PASS\""
				+ " order by tcr.Executed_Date Desc";

	JSONArray tcresult = new JSONArray();
	JSONObject json = new JSONObject();
	ResultSet rs = null;
	try {
		PreparedStatement stmt = testCon.prepareStatement(all_sql);
		stmt.setInt(1, tsrid);
		rs = stmt.executeQuery();
		
		while(rs.next()) {
			JSONObject tcr = getTestCaseResultAsJSON(rs, companyId, false);
			tcresult.add(tcr);
		}
		json.put("testcaseresult", tcresult);
	} catch(Exception e) {
		System.err.println(Utilities.getNow());
		e.printStackTrace();
	}
	return json;
}


	public JSONObject updateIsIgnored(int testSuiteResultId, boolean isIgnored) {
	    JSONObject json = new JSONObject();
	    String updateSql = "UPDATE cta.test_suite_results SET isIgnored = ? WHERE idtest_suite_results = ?";

	    try (PreparedStatement stmt = testCon.prepareStatement(updateSql)) {
	        stmt.setInt(1, isIgnored ? 1 : 0);
	        stmt.setInt(2, testSuiteResultId);
	        int rowsAffected = stmt.executeUpdate();

	        if (rowsAffected == 0) {
	            json.put("status", "fail");
	            json.put("message", "No test suite result found with ID: " + testSuiteResultId);
	        } else {
	            json.put("status", "success");
	            json.put("message", "isIgnored set to " + (isIgnored ? 1 : 0) + " for ID " + testSuiteResultId);
	        }
	    } catch (Exception e) {
	        json.put("status", "fail");
	        json.put("message", "unknown error");
	        e.printStackTrace();
	    }

	    return json;
	}
	
	public JSONObject saveUserNoteForTestCaseResult(int testCaseResultId, String userNote, String userComments) {
	    JSONObject result = new JSONObject();

	    try {
	    	
	    	if (userNote == null || !userNote.trim().matches("(#\\w+\\s*)+")) {
	    	    result.put("success", false);
	    	    result.put("message", "User note must be space-separated words, each starting with '#' character.");
	    	    return result;
	    	}
	    	
	        String updateSql = "UPDATE cta.test_case_results SET user_note = ?, user_comments = ? WHERE idtest_case_results = ?";
	        PreparedStatement stmt = testCon.prepareStatement(updateSql);
	        stmt.setString(1, userNote); 
	        stmt.setString(2, userComments);
	        stmt.setInt(3, testCaseResultId);
	        int updated = stmt.executeUpdate();
	        stmt.close();

	        if (updated > 0) {
	            result.put("success", true);
	            result.put("message", "User note updated successfully");
	        } else {
	            result.put("success", false);
	            result.put("message", "No record updated - check testCaseResultId");
	        }

	    } catch (Exception e) {
	        System.err.println(Utilities.getNow());
	        e.printStackTrace();
	        result.put("error", "Exception occurred while saving user note");
	    }

	    return result;
	}
	
	public JSONObject getUserNoteFromTestCaseResult(int testCaseResultId) {
		
		JSONObject result = new JSONObject();
	    try {
	        String querySql = "SELECT user_note, user_comments FROM cta.test_case_results WHERE idtest_case_results = ?";
	        PreparedStatement stmt = testCon.prepareStatement(querySql);
	        stmt.setInt(1, testCaseResultId);
	        ResultSet rs = stmt.executeQuery();

	        if (rs.next()) {
	            String userNote = rs.getString("user_note");
	            String userComments = rs.getString("user_comments");

	            result.put("success", true);
	            result.put("user_note", userNote != null ? userNote : "");
	            result.put("user_comments", userComments != null ? userComments : "");
	        } else {
	            result.put("success", false);
	            result.put("message", "No record found for the given testCaseResultId");
	        }

	        rs.close();
	        stmt.close();

	    } catch (Exception e) {
	        System.err.println(Utilities.getNow());
	        e.printStackTrace();
	        result.put("error", "Exception occurred while fetching user note");
	    }

	    return result;
	}
	
	public JSONObject getAllDescriptionTagsForSuiteResult(int suiteResultId) {
	    
	    Map<String, Integer> tagCountMap = new HashMap<>();
	    JSONArray resultArray = new JSONArray();
	    JSONObject json = new JSONObject();

	    try {
	        // Step 1: Fetch testcasedescription for all test cases in the suite result
	        String sql = "SELECT tc.testcasedescription FROM cta.test_case_results tcr " +
	                     "JOIN cta.test_suite_case_results tscr ON tscr.test_case_results_id = tcr.idtest_case_results " +
	                     "JOIN cta.test_case tc ON tc.idtest_case = tcr.Test_Case_Id " +
	                     "WHERE tscr.test_suite_results_id = ?";

	        PreparedStatement stmt = testCon.prepareStatement(sql);
	        stmt.setInt(1, suiteResultId);

	        ResultSet rs = stmt.executeQuery();

	        // Step 2: Extract and count hashtags
	        while (rs.next()) {
	            String tcDescription = rs.getString("testcasedescription");
	            if (tcDescription == null || tcDescription.isEmpty()) continue;

	            String[] tokens = tcDescription.split("\\s+");

	            for (String token : tokens) {
	                if (token.startsWith("#") && token.length() > 1) {
	                    String tag = token.toLowerCase();
	                    tagCountMap.put(tag, tagCountMap.getOrDefault(tag, 0) + 1);
	                }
	            }
	        }
	        rs.close();
	        stmt.close();

	        // Step 3: Sort tags by count descending
	        List<Map.Entry<String, Integer>> sortedTags = new ArrayList<>(tagCountMap.entrySet());
	        sortedTags.sort((a, b) -> b.getValue().compareTo(a.getValue()));

	        // Step 4: Convert to JSONArray
	        for (Map.Entry<String, Integer> entry : sortedTags) {
	            JSONObject tagObj = new JSONObject();
	            tagObj.put("tag", entry.getKey());
	            tagObj.put("count", entry.getValue());
	            resultArray.add(tagObj);
	        }

	    } catch (Exception e) {
	        System.err.println(Utilities.getNow());
	        e.printStackTrace();
	    }

	    json.put("tcDescription_tag_list", resultArray);
	    return json;
	}

	
	public JSONObject getAllTagsForSuiteResult(int suiteResultId) {
		
	    Map<String, Integer> tagCountMap = new HashMap<>();
	    JSONArray resultArray = new JSONArray();
	    JSONObject json = new JSONObject();

	    try {
	        // Step 1: Query all user_notes from given suiteResultId
	        String sql = "SELECT tcr.user_note FROM cta.test_case_results tcr " +
	                     "JOIN cta.test_suite_case_results tscr ON tscr.test_case_results_id = tcr.idtest_case_results " +
	                     "WHERE tscr.test_suite_results_id = ?";

	        PreparedStatement stmt = testCon.prepareStatement(sql);
	        stmt.setInt(1, suiteResultId);

	        ResultSet rs = stmt.executeQuery();

	        // Step 2: Extract tags and count them
	        while (rs.next()) {
	            String userNote = rs.getString("user_note");
	            if (userNote == null || userNote.isEmpty()) continue;

	            String[] tokens = userNote.split("\\s+");

	            for (String token : tokens) {
	                if (token.startsWith("#") && token.length() > 1) {
	                    String tag = token.toLowerCase();
	                    tagCountMap.put(tag, tagCountMap.getOrDefault(tag, 0) + 1);
	                }
	            }
	        }
	        rs.close();
	        stmt.close();

	        // Step 3: Sort tags by count descending
	        List<Map.Entry<String, Integer>> sortedTags = new ArrayList<>(tagCountMap.entrySet());
	        sortedTags.sort((a, b) -> b.getValue().compareTo(a.getValue())); // Descending sort

	        // Step 4: Convert to JSONArray
	        for (Map.Entry<String, Integer> entry : sortedTags) {
	            JSONObject tagObj = new JSONObject();
	            tagObj.put("tag", entry.getKey());
	            tagObj.put("count", entry.getValue());
	            resultArray.add(tagObj);
	        }

	    } catch (Exception e) {
	        System.err.println(Utilities.getNow());
	        e.printStackTrace();
	    }

	    json.put("tag_list", resultArray);
	    return json;
	}
	
	public JSONArray getTestCaseResultsListWithTagsForTestSuite(int testSuiteResultsId, int companyId) {

	    JSONArray resultArray = new JSONArray();

	    try {
	        // SQL to fetch test case results with user_note and user_comments
	        String sql =
	            "SELECT tcr.* FROM cta.test_case_results tcr " +
	            "JOIN cta.test_suite_case_results tscr ON tscr.test_case_results_id = tcr.idtest_case_results " +
	            "WHERE tscr.test_suite_results_id = ? " +
	            "AND tcr.user_note IS NOT NULL " +
	            "ORDER BY Executed_Date DESC";

	        PreparedStatement stmt = testCon.prepareStatement(sql);
	        stmt.setInt(1, testSuiteResultsId);
	        ResultSet rs = stmt.executeQuery();

	        while (rs.next()) {
	            String testCaseName = rs.getString("test_case_name");
	            int testCaseId = rs.getInt("Test_Case_Id");
	            int testCaseResultId = rs.getInt("idtest_case_results");
	            String userNote = rs.getString("user_note");
	            String userComments = rs.getString("user_comments");

	            // Extract tags from user_note into JSON array
	            JSONArray tagsArray = new JSONArray();
	            if (userNote != null && !userNote.isEmpty()) {
	                String[] tokens = userNote.trim().split("\\s+");
	                for (String token : tokens) {
	                    if (token.startsWith("#") && token.length() > 1) {
	                        tagsArray.add(token.toLowerCase());
	                    }
	                }
	            }

	            JSONObject testCaseResultObj = new JSONObject();
	            testCaseResultObj.put("Executed_Date", (rs.getTimestamp("Executed_Date")).toString());
	            testCaseResultObj.put("test_case_name", testCaseName);
	            testCaseResultObj.put("test_case_id", testCaseId);
	            testCaseResultObj.put("test_case_result_id", testCaseResultId);
	            String fileurl = AppProperties.fileurl + "?action=executionlog&token=" + randomkey + "&companyid="
						+ companyId + "&tcid=" + testCaseId + "&tcrid=" + testCaseResultId;
	            testCaseResultObj.put("logfile", fileurl);
	            testCaseResultObj.put("Status", rs.getString("Status"));
	            testCaseResultObj.put("tags", tagsArray);
	            testCaseResultObj.put("user_comments", userComments != null ? userComments : "");

	            resultArray.add(testCaseResultObj);
	        }

	        rs.close();
	        stmt.close();

	    } catch (Exception e) {
	        e.printStackTrace();
	    }

	    return resultArray;
	}

	
	public JSONObject getUserNoteStatsForTestSuite(int testSuiteResultsId) {
		
		JSONObject result = new JSONObject();
	    JSONArray tagStatsArray = new JSONArray();
	    Map<String, Integer> tagCountMap = new HashMap<>();

	    try {
	        // Step 1: Get all user_notes for this suite result
	        String notesSql =
	            "SELECT tcr.user_note FROM cta.test_case_results tcr " +
	            "JOIN cta.test_suite_case_results tscr ON tscr.test_case_results_id = tcr.idtest_case_results " +
	            "WHERE tscr.test_suite_results_id = ? AND tcr.user_note IS NOT NULL";

	        PreparedStatement stmt = testCon.prepareStatement(notesSql);
	        stmt.setInt(1, testSuiteResultsId);
	        ResultSet rs = stmt.executeQuery();

	        // Step 2: Extract tags and count
	        while (rs.next()) {
	            String userNote = rs.getString("user_note");
	            if (userNote == null || userNote.isEmpty()) continue;

	            String[] tokens = userNote.trim().split("\\s+");

	            for (String token : tokens) {
	                if (token.startsWith("#") && token.length() > 1) {
	                    String tag = token.toLowerCase();
	                    tagCountMap.put(tag, tagCountMap.getOrDefault(tag, 0) + 1);
	                }
	            }
	        }
	        rs.close();
	        stmt.close();

	        // Step 3: Sort tags by count descending
	        List<Map.Entry<String, Integer>> sortedTags = new ArrayList<>(tagCountMap.entrySet());
	        sortedTags.sort((a, b) -> b.getValue().compareTo(a.getValue()));

	        // Step 4: For each tag, get top 3 comments from rows where tag is the first word in user_note
	        for (Map.Entry<String, Integer> entry : sortedTags) {
	            String tag = entry.getKey(); // includes #
	            int count = entry.getValue();

	            JSONArray commentArray = new JSONArray();

	            String commentSql =
	                "SELECT tcr.user_comments FROM cta.test_case_results tcr " +
	                "JOIN cta.test_suite_case_results tscr ON tscr.test_case_results_id = tcr.idtest_case_results " +
	                "WHERE tscr.test_suite_results_id = ? AND tcr.user_comments IS NOT NULL " +
	                "AND (LOWER(tcr.user_note) = ? OR LOWER(tcr.user_note) LIKE ?) " +
	                "ORDER BY tcr.Executed_Date DESC LIMIT 3";

	            PreparedStatement commentStmt = testCon.prepareStatement(commentSql);
	            commentStmt.setInt(1, testSuiteResultsId);
	            commentStmt.setString(2, tag);
	            commentStmt.setString(3, tag + " %"); // space after tag for first-word match

	            ResultSet commentRs = commentStmt.executeQuery();
	            while (commentRs.next()) {
	                commentArray.add(commentRs.getString("user_comments"));
	            }
	            commentRs.close();
	            commentStmt.close();

	            JSONObject tagObj = new JSONObject();
	            tagObj.put("note", tag);
	            tagObj.put("count", count);
	            tagObj.put("topComments", commentArray);

	            tagStatsArray.add(tagObj);
	        }

	        result.put("success", true);
	        result.put("userNotes", tagStatsArray);

	    } catch (Exception e) {
	        e.printStackTrace();
	        result.put("success", false);
	        result.put("error", "Exception occurred while retrieving tag stats.");
	    }

	    return result;
	}
	
	public JSONObject getTestCaseResultsByUserTags(int testSuiteResultsId, List<String> tags) {
	    JSONObject result = new JSONObject();
	    JSONObject tagResults = new JSONObject();

	    if (tags == null || tags.isEmpty()) {
	        result.put("success", false);
	        result.put("error", "At least one tag must be provided.");
	        return result;
	    }

	    try {
	        String testCaseSql =
	            "SELECT tcr.test_case_name, tcr.Test_Case_Id, tcr.idtest_case_results, tcr.user_comments FROM cta.test_case_results tcr " +
	            "JOIN cta.test_suite_case_results tscr ON tscr.test_case_results_id = tcr.idtest_case_results " +
	            "WHERE tscr.test_suite_results_id = ? AND tcr.user_note IS NOT NULL " +
	            "AND LOWER(tcr.user_note) LIKE ?";

	        for (String tag : tags) {
	        	
	        	if (tag == null || !tag.trim().startsWith("#")) {
	                continue; // skip tags that don't start with '#'
	            }
	            JSONArray testCaseArray = new JSONArray();
	            JSONObject tagResult = new JSONObject();

	            PreparedStatement stmt = testCon.prepareStatement(testCaseSql);
	            stmt.setInt(1, testSuiteResultsId);
	            stmt.setString(2, "%" + tag.toLowerCase() + "%");

	            ResultSet rs = stmt.executeQuery();

	            while (rs.next()) {
	                JSONObject testCaseObj = new JSONObject();
	                testCaseObj.put("test_case_name", rs.getString("test_case_name"));
	                testCaseObj.put("test_case_id", rs.getInt("Test_Case_Id"));
	                testCaseObj.put("user_comments", rs.getString("user_comments"));
	                testCaseObj.put("test_case_results_id", rs.getInt("idtest_case_results"));
	                testCaseArray.add(testCaseObj);
	            }

	            rs.close();
	            stmt.close();

	            tagResult.put("count", testCaseArray.size());
	            tagResult.put("testCases", testCaseArray);
	            tagResults.put(tag, tagResult);
	        }

	        result.put("success", true);
	        result.put("results", tagResults);

	    } catch (Exception e) {
	        e.printStackTrace();
	        result.put("success", false);
	        result.put("error", "Exception occurred while retrieving test cases by tags.");
	    }

	    return result;
	}

	
	public JSONObject getSearchedTestCasesHistoryFromTestSuite(
		    int testSuiteId,
		    Integer suiteResultId,
		    int companyId,
		    int pnum,
		    String searchString,
		    List<String> issueTypes,
		    List<String> descriptionTags
		) {
		    final int pageSize = AppProperties.failTestCaseHistoryPageSize;
		    int offset = (pnum - 1) * pageSize;

		    JSONArray tcresult = new JSONArray();
		    JSONObject json = new JSONObject();
		    List<Integer> suiteResultIds = new ArrayList<>();

		    try {
		        // Step 1: Fetch latest 3 suite result IDs
		        String latestThreeSuitesSql =
		            "SELECT idtest_suite_results FROM cta.test_suite_results " +
		            "WHERE Test_Suite_Id = ? AND (isIgnored IS NULL OR isIgnored = 0) " +
		            (suiteResultId != null ? "AND idtest_suite_results <= ? " : "") +
		            "ORDER BY Executed_Date DESC LIMIT 3";

		        PreparedStatement stmt = testCon.prepareStatement(latestThreeSuitesSql);
		        stmt.setInt(1, testSuiteId);
		        if (suiteResultId != null) stmt.setInt(2, suiteResultId);

		        ResultSet rs = stmt.executeQuery();
		        while (rs.next()) suiteResultIds.add(rs.getInt("idtest_suite_results"));

		        if (suiteResultIds.isEmpty()) {
		            json.put("testcaseresult", tcresult);
		            json.put("total_count", 0);
		            json.put("total_pages", 0);
		            json.put("page_num", pnum);
		            return json;
		        }

		        int latestSuiteResultId = suiteResultIds.get(0);

		        // Step 2: Build WHERE clause
		        StringBuilder whereClause = new StringBuilder("tscr.test_suite_results_id = ?");
		        List<Object> paramList = new ArrayList<>();
		        paramList.add(latestSuiteResultId);

		        boolean isNumeric = searchString != null && searchString.matches("\\d+");
		        boolean isHashSearch = searchString != null && searchString.startsWith("#");

		        // Existing search logic
		        if (searchString != null && !searchString.trim().isEmpty()) {
		            if (isHashSearch) {
		                List<String> hashtags = Arrays.stream(searchString.trim().split("\\s+"))
		                        .filter(tag -> tag.startsWith("#"))
		                        .map(tag -> tag.substring(1).toLowerCase())
		                        .collect(Collectors.toList());

		                if (!hashtags.isEmpty()) {
		                    String tagPattern = String.join("|", hashtags);
		                    whereClause.append(" AND LOWER(tcr.user_note) REGEXP ?");
		                    paramList.add("#(" + tagPattern + ")");
		                }

		            } else if (isNumeric) {
		                whereClause.append(" AND (tcr.Test_Case_Id = ? OR LOWER(tcr.test_case_name) LIKE ?)");
		                paramList.add(Integer.parseInt(searchString));
		                paramList.add("%" + searchString.toLowerCase() + "%");

		            } else {
		                whereClause.append(" AND LOWER(tcr.test_case_name) LIKE ?");
		                paramList.add("%" + searchString.toLowerCase() + "%");
		            }
		        }

		        // ---------- NEW FILTER: Issue Types ----------
		        if (issueTypes != null && !issueTypes.isEmpty()) {
		            String inClause = issueTypes.stream().map(x -> "?").collect(Collectors.joining(","));
		            whereClause.append(" AND tcr.issuetype IN (" + inClause + ")");
		            paramList.addAll(issueTypes);
		        }

		        // ---------- NEW FILTER: Hashtags in Description ----------
		        if (descriptionTags != null && !descriptionTags.isEmpty()) {
		            String tagPattern = String.join("|", descriptionTags);
		            whereClause.append(" AND LOWER(tc.testcasedescription) REGEXP ?");
		            paramList.add("#(" + tagPattern.toLowerCase() + ")");
		        }

		        // Step 3: Count query
		        String countSql = "SELECT COUNT(*) FROM cta.test_case_results tcr " +
		                          "JOIN cta.test_suite_case_results tscr ON tscr.test_case_results_id = tcr.idtest_case_results " +
		                          "JOIN cta.test_case tc ON tc.idtest_case = tcr.Test_Case_Id " +       // <-- NEW JOIN for description
		                          "WHERE " + whereClause;

		        stmt = testCon.prepareStatement(countSql);
		        for (int i = 0; i < paramList.size(); i++) stmt.setObject(i + 1, paramList.get(i));
		        rs = stmt.executeQuery();

		        int totalCount = 0;
		        if (rs.next()) totalCount = rs.getInt(1);
		        int totalPages = (int) Math.ceil((double) totalCount / pageSize);

		        // Step 4: Data query
		        String dataSql = "SELECT tcr.*, tc.testcasedescription FROM cta.test_case_results tcr " +
		                         "JOIN cta.test_suite_case_results tscr ON tscr.test_case_results_id = tcr.idtest_case_results " +
		                         "JOIN cta.test_case tc ON tc.idtest_case = tcr.Test_Case_Id " +       // <-- NEW JOIN for description
		                         "WHERE " + whereClause + " ORDER BY tcr.Executed_Date DESC LIMIT ? OFFSET ?";

		        stmt = testCon.prepareStatement(dataSql);
		        for (int i = 0; i < paramList.size(); i++) stmt.setObject(i + 1, paramList.get(i));
		        stmt.setInt(paramList.size() + 1, pageSize);
		        stmt.setInt(paramList.size() + 2, offset);
		        rs = stmt.executeQuery();

		        // Step 5: Map result
		        while (rs.next()) {
		            JSONObject item = new JSONObject();
		            JSONObject tcr = getTestCaseResultAsJSONVersionTwo(rs, companyId, false);
		            int testCaseId = rs.getInt("Test_Case_Id");

		            // ---------- NEW FIELD: add description to response ----------
//		            tcr.put("description", rs.getString("description"));

		            item.put("latest", tcr);

		            JSONArray prevRuns = getPreviousExecutionsFromSuiteRuns(testCaseId, suiteResultIds, companyId);
		            if (prevRuns.size() > 0) {
		                JSONArray runMinusOne = (JSONArray) prevRuns.get(0);
		                if (runMinusOne != null && !runMinusOne.isEmpty()) {
		                    item.put("run_minus_one", runMinusOne);
		                }
		            }

		            if (prevRuns.size() > 1) {
		                JSONArray runMinusTwo = (JSONArray) prevRuns.get(1);
		                if (runMinusTwo != null && !runMinusTwo.isEmpty()) {
		                    item.put("run_minus_two", runMinusTwo);
		                }
		            }

		            tcresult.add(item);
		        }

		        json.put("testcaseresult", tcresult);
		        json.put("total_count", totalCount);
		        json.put("total_pages", totalPages);
		        json.put("page_num", pnum);

		    } catch (Exception e) {
		        System.err.println(Utilities.getNow());
		        e.printStackTrace();
		    }

		    return json;
		}


	
	public JSONObject getStatusPatternCountsForTestSuiteResult(int suiteResultId, int companyId) {
	    JSONObject result = new JSONObject();

	    try {
	        // 1. Check suite status and stored pattern
	        String checkSql = "SELECT * FROM cta.test_suite_results WHERE idtest_suite_results = ?";
	        PreparedStatement stmt = testCon.prepareStatement(checkSql);
	        stmt.setInt(1, suiteResultId);
	        ResultSet rs = stmt.executeQuery();

	        if (!rs.next()) {
	            result.put("error", "Suite result not found");
	            return result;
	        }

	        String suiteStatus = rs.getString("Status");
	        String storedPatterns = rs.getString("status_patterns");
	        int testSuiteId = rs.getInt("Test_Suite_Id");

	        result.put("suite_status", suiteStatus);

	        if ("RUNNING".equalsIgnoreCase(suiteStatus) || "STARTED".equalsIgnoreCase(suiteStatus)) {
	            // Compute and return without storing
	            JSONObject computed = computeStatusPatternCountsForTestSuiteResult(testSuiteId, suiteResultId, companyId);
	            JSONObject patterns = (JSONObject) computed.get("patterns");
	            result.put("patterns", patterns);
	            result.put("source", "computed");
	            return result;
	        }

	        // 2. Not running - try to use cached data if present
	        if (storedPatterns != null && !storedPatterns.trim().isEmpty()) {
	        	JSONObject cachedPatterns = (JSONObject) new JSONParser().parse(storedPatterns); // Parse JSON string to object
	            result.put("patterns", cachedPatterns);
	            result.put("source", "cached");
	            return result;
	        }

	        // 3. Not running - no cached data, compute and store
	        JSONObject computed = computeStatusPatternCountsForTestSuiteResult(testSuiteId, suiteResultId, companyId);
	        JSONObject patterns = (JSONObject) computed.get("patterns");

	        // Store the computed patterns in DB
	        String updateSql = "UPDATE cta.test_suite_results SET status_patterns = ? WHERE idtest_suite_results = ?";
	        stmt = testCon.prepareStatement(updateSql);
	        stmt.setString(1, patterns.toString()); // Save as JSON string
	        stmt.setInt(2, suiteResultId);
	        stmt.executeUpdate();

	        result.put("patterns", patterns);
	        result.put("source", "computed");

	    } catch (Exception e) {
	        System.err.println(Utilities.getNow());
	        e.printStackTrace();
	        result.put("error", "Exception occurred while computing test case pattern counts");
	    }

	    return result;
	}

	
	public JSONObject computeStatusPatternCountsForTestSuiteResult(int testSuiteId, int suiteResultId, int companyId) {
	    JSONObject result = new JSONObject();
	    List<Integer> suiteResultIds = new ArrayList<>();

	    // Define patterns to track (all lowercase for consistent comparison)
	    String[] patternsToTrack = {
	        "fail-fail-fail", "fail-pass-fail", "fail-fail-pass", "fail-pass-pass", 
	        "pass-fail-fail", "pass-pass-pass", "pass-fail-pass", "pass-pass-fail"
	    };

	    JSONObject patternCounts = new JSONObject();
	    for (String pattern : patternsToTrack) {
	        patternCounts.put(pattern, 0);
	    }

	    try {
	        // 1. Get the current + 2 previous suite result IDs for this test suite
	        String latestThreeSuitesSql = "SELECT idtest_suite_results FROM cta.test_suite_results " +
	                "WHERE Test_Suite_Id = ? AND idtest_suite_results <= ?  AND (isIgnored IS NULL OR isIgnored = 0)" +
	                "ORDER BY Executed_Date DESC LIMIT 3";
	        PreparedStatement stmt = testCon.prepareStatement(latestThreeSuitesSql);
	        stmt.setInt(1, testSuiteId);
	        stmt.setInt(2, suiteResultId);
	        ResultSet rs = stmt.executeQuery();
	        while (rs.next()) suiteResultIds.add(rs.getInt("idtest_suite_results"));

	        if (suiteResultIds.isEmpty()) {
	            result.put("patterns", patternCounts);
	            return result;
	        }

	        int latestSuiteResultId = suiteResultIds.get(0);

	        // 2. Get all test cases from the specified suite result
	        String dataSql = "SELECT * FROM cta.test_case_results tcr " +
	                "JOIN cta.test_suite_case_results tscr ON tscr.test_case_results_id = tcr.idtest_case_results " +
	                "WHERE tscr.test_suite_results_id = ?";
	        stmt = testCon.prepareStatement(dataSql);
	        stmt.setInt(1, latestSuiteResultId);
	        rs = stmt.executeQuery();

	        while (rs.next()) {
	            int testCaseId = rs.getInt("Test_Case_Id");

	            String status1 = rs.getString("Status") != null ? rs.getString("Status").toLowerCase() : "none";
	            JSONArray prevRuns = getPreviousExecutionsFromSuiteRunsForStatusCode(testCaseId, suiteResultIds, companyId);

	            String status2 = "none";
	            if (prevRuns.size() > 0) {
	                Object s2 = ((JSONObject) prevRuns.get(0)).get("Status");
	                status2 = s2 != null ? s2.toString().toLowerCase() : "none";
	            }

	            String status3 = "none";
	            if (prevRuns.size() > 1) {
	                Object s3 = ((JSONObject) prevRuns.get(1)).get("Status");
	                status3 = s3 != null ? s3.toString().toLowerCase() : "none";
	            }

	            // Only track valid pass/fail statuses
	            if (status1.equals("pass") || status1.equals("fail")) {
	                String pattern = status1 + "-" + status2 + "-" + status3;

	                // Add new pattern if not already tracked
	                if (!patternCounts.containsKey(pattern)) {
	                    patternCounts.put(pattern, 0);
	                }

	                int count = (int) patternCounts.get(pattern);
	                patternCounts.put(pattern, count + 1);
	            }
	        }

	        result.put("patterns", patternCounts);

	    } catch (Exception e) {
	        System.err.println(Utilities.getNow());
	        e.printStackTrace();
	    }

	    return result;
	}


	
	private boolean matchesStatusPattern(String s2, String s3, String f2, String f3) {
		return (f2 == null || f2.equalsIgnoreCase("any") || s2.equalsIgnoreCase("none") || s2.equalsIgnoreCase("both") || f2.equalsIgnoreCase(s2)) &&
		(f3 == null || f3.equalsIgnoreCase("any") || s3.equalsIgnoreCase("none") || s3.equalsIgnoreCase("both") || f3.equalsIgnoreCase(s3));
	}

	
	public JSONObject getFilteredTestCasesHistoryFromTestSuite(
		    int testSuiteId, Integer suiteResultId, int companyId, int pnum, String status1Filter, String status2Filter, String status3Filter) {

		    final int pageSize = AppProperties.failTestCaseHistoryPageSize;
		    int offset = (pnum - 1) * pageSize;

		    JSONArray tcresult = new JSONArray();
		    JSONObject json = new JSONObject();

		    List<Integer> suiteResultIds = new ArrayList<>();

		    try {
		        // 1. Get latest 3 suite result IDs
		    	String latestThreeSuitesSql = 
		    		    "SELECT idtest_suite_results FROM cta.test_suite_results " +
		    		    "WHERE Test_Suite_Id = ?  AND (isIgnored IS NULL OR isIgnored = 0) " +
		    		    (suiteResultId != null ? "AND idtest_suite_results <= ? " : "") +
		    		    "ORDER BY Executed_Date DESC LIMIT 3";

	    		PreparedStatement stmt = testCon.prepareStatement(latestThreeSuitesSql);
	    		stmt.setInt(1, testSuiteId);

	    		if (suiteResultId != null) {
	    		    stmt.setInt(2, suiteResultId);
	    		}

		    	ResultSet rs = stmt.executeQuery();

		        while (rs.next()) suiteResultIds.add(rs.getInt("idtest_suite_results"));

		        if (suiteResultIds.isEmpty()) {
		            json.put("testcaseresult", tcresult);
		            json.put("total_count", 0);
		            json.put("total_pages", 0);
		            json.put("page_num", pnum);
		            return json;
		        }

		        int latestSuiteResultId = suiteResultIds.get(0);

		        // 2. Fetch filtered test cases from latest suite result
		        String dataSql;
		        if (status1Filter == null || status1Filter.equalsIgnoreCase("any")) {
		            dataSql = "SELECT tcr.*, tc.testcasedescription FROM cta.test_case_results tcr " +
		                      "JOIN cta.test_suite_case_results tscr ON tscr.test_case_results_id = tcr.idtest_case_results " +
		                      "JOIN cta.test_case tc ON tc.idtest_case = tcr.Test_Case_Id " +
		                      "WHERE tscr.test_suite_results_id = ? ORDER BY tcr.Executed_Date DESC";
		            stmt = testCon.prepareStatement(dataSql);
		            stmt.setInt(1, latestSuiteResultId);
		        } else {
		            dataSql = "SELECT tcr.*, tc.testcasedescription FROM cta.test_case_results tcr " +
		                      "JOIN cta.test_suite_case_results tscr ON tscr.test_case_results_id = tcr.idtest_case_results " +
		                      "JOIN cta.test_case tc ON tc.idtest_case = tcr.Test_Case_Id " +
		                      "WHERE tscr.test_suite_results_id = ? AND LOWER(tcr.Status) = ? ORDER BY tcr.Executed_Date DESC";
		            stmt = testCon.prepareStatement(dataSql);
		            stmt.setInt(1, latestSuiteResultId);
		            stmt.setString(2, status1Filter.toLowerCase());
		        }
		        rs = stmt.executeQuery();

		        List<JSONObject> filteredList = new ArrayList<>();

		        while (rs.next()) {
		        	JSONObject item = new JSONObject();
		            JSONObject tcr = getTestCaseResultAsJSONVersionTwo(rs, companyId, false);
		            int testCaseId = rs.getInt("Test_Case_Id");
		            // latest
		            item.put("latest", tcr);

		            // previous
		            JSONArray prevRuns = getPreviousExecutionsFromSuiteRuns(testCaseId, suiteResultIds, companyId);
		            String status2 = "none";
		            if (prevRuns.size() > 0) {
		                JSONArray runMinusOne = (JSONArray) prevRuns.get(0);
		                status2 = getStatusFromRunArray(runMinusOne);
		                if (!"none".equals(status2)) item.put("run_minus_one", runMinusOne);
		            }

		            String status3 = "none";
		            if (prevRuns.size() > 1) {
		                JSONArray runMinusTwo = (JSONArray) prevRuns.get(1);
		                status3 = getStatusFromRunArray(runMinusTwo);
		                if (!"none".equals(status3)) item.put("run_minus_two", runMinusTwo);
		            }

		            // status pattern check
		            if (matchesStatusPattern(status2, status3, status2Filter, status3Filter)) {
		                filteredList.add(item);
		            }
		        }

		        // 3. Pagination
		        int totalCount = filteredList.size();
		        int totalPages = (int) Math.ceil((double) totalCount / pageSize);

		        int toIndex = Math.min(offset + pageSize, totalCount);
		        List<JSONObject> pageItems = filteredList.subList(Math.min(offset, totalCount), toIndex);
		        for (JSONObject obj : pageItems) {
		            tcresult.add(obj);
		        }

		        json.put("testcaseresult", tcresult);
		        json.put("total_count", totalCount);
		        json.put("total_pages", totalPages);
		        json.put("page_num", pnum);

		    } catch (Exception e) {
		        System.err.println(Utilities.getNow());
		        e.printStackTrace();
		    }

		    return json;
		}
	
	
	private String getStatusFromRunArray(JSONArray runArray) {
	    boolean hasPass = false;
	    boolean hasFail = false;

	    for (Object obj : runArray) {
	        if (obj instanceof JSONObject) {
	            JSONObject run = (JSONObject) obj;
	            Object statusObj = run.get("Status");
	            if (statusObj != null) {
	                String status = statusObj.toString().toLowerCase();
	                if ("pass".equals(status)) {
	                    hasPass = true;
	                } else if ("fail".equals(status)) {
	                    hasFail = true;
	                }
	            }
	        }
	        if (hasPass && hasFail) return "both";
	    }

	    if (hasPass && hasFail) return "both";
	    if (hasFail) return "fail";
	    if (hasPass) return "pass";
	    return "none";
	}

	
	private JSONArray getPreviousExecutionsFromSuiteRuns(int testCaseId, List<Integer> suiteResultIds, int companyId) {
	    JSONArray previousRuns = new JSONArray();

	    if (suiteResultIds.size() < 2) return previousRuns;

	    for (int i = 1; i < suiteResultIds.size(); i++) {  // starting from second latest (index 1)
	        int suiteResultId = suiteResultIds.get(i);

	        String sql = "SELECT tcr.*, tc.testcasedescription FROM cta.test_case_results tcr " +
	                     "JOIN cta.test_suite_case_results tscr ON tscr.test_case_results_id = tcr.idtest_case_results " +
	                     "JOIN cta.test_case tc ON tc.idtest_case = tcr.Test_Case_Id " +
	                     "WHERE tscr.test_suite_results_id = ? AND tcr.Test_Case_Id = ?";

	        try {
	            PreparedStatement stmt = testCon.prepareStatement(sql);
	            stmt.setInt(1, suiteResultId);
	            stmt.setInt(2, testCaseId);
	            ResultSet rs = stmt.executeQuery();

	            JSONArray runArray = new JSONArray(); // <-- collect multiple results

	            while (rs.next()) {
	                JSONObject json = getTestCaseResultAsJSONVersionTwo(rs, companyId, false);
	                runArray.add(json);
	            }

	            previousRuns.add(runArray);  // <-- add this array to previousRuns

	        } catch (Exception e) {
	            System.err.println(Utilities.getNow());
	            e.printStackTrace();
	            previousRuns.add(new JSONArray()); // <-- add empty array in case of error
	        }
	    }

	    return previousRuns;
	}
	
	private JSONArray getPreviousExecutionsFromSuiteRunsForStatusCode(int testCaseId, List<Integer> suiteResultIds, int companyId) {
	    JSONArray previousRuns = new JSONArray();

	    if (suiteResultIds.size() < 2) return previousRuns;

	    for (int i = 1; i < suiteResultIds.size(); i++) {  // starting from second latest (index 1)
	        int suiteResultId = suiteResultIds.get(i);

	        String sql = "SELECT tcr.*, tc.testcasedescription FROM cta.test_case_results tcr " +
	                     "JOIN cta.test_suite_case_results tscr ON tscr.test_case_results_id = tcr.idtest_case_results " +
	                     "JOIN cta.test_case tc ON tc.idtest_case = tcr.Test_Case_Id " +
	                     "WHERE tscr.test_suite_results_id = ? AND tcr.Test_Case_Id = ?";

	        try {
	            PreparedStatement stmt = testCon.prepareStatement(sql);
	            stmt.setInt(1, suiteResultId);
	            stmt.setInt(2, testCaseId);
	            ResultSet rs = stmt.executeQuery();

	            if (rs.next()) {
	                JSONObject json = getTestCaseResultAsJSONVersionTwo(rs, companyId, false);
	                previousRuns.add(json);
	            } else {
	                previousRuns.add(new JSONObject()); // add empty object if no result found
	            }

	        } catch (Exception e) {
	            System.err.println(Utilities.getNow());
	            e.printStackTrace();
	            previousRuns.add(new JSONObject()); // add empty object in case of error
	        }
	    }

	    return previousRuns;
	}



private JSONArray getLatestThreeExecutions(int testcaseId, int companyId) {
    String executionsSql = "SELECT * FROM cta.test_case_results WHERE Test_Case_Id = ? " +
                            "ORDER BY Executed_Date DESC LIMIT 3";
    JSONArray executions = new JSONArray();
    ResultSet rs = null;
    try {
        PreparedStatement stmt = testCon.prepareStatement(executionsSql);
        stmt.setInt(1, testcaseId);
        rs = stmt.executeQuery();
        
        while (rs.next()) {
            JSONObject execJson = getTestCaseResultAsJSON(rs, companyId, false);
            executions.add(execJson);
        }
    } catch (Exception e) {
        System.err.println(Utilities.getNow());
        e.printStackTrace();
    }
    return executions;
}


public JSONObject getErrorCodeDropDownFromTestSuiteResult(int tsrid) {
	String all_sql = "SELECT * FROM cta.test_case_results tcr JOIN cta.test_suite_case_results tscr ON " +
			"tscr.test_case_results_id = tcr.idtest_case_results WHERE tscr.test_suite_results_id = ? AND Status = 'Fail'" +
			" order by tcr.idtest_case_results Desc";

	JSONObject errorCodes = new JSONObject();
    JSONObject json = new JSONObject();
    ResultSet rs = null;
    
    try {
        PreparedStatement stmt = testCon.prepareStatement(all_sql);
        stmt.setInt(1, tsrid);
        rs = stmt.executeQuery();
        
        Map<String, JSONObject> errorMap = new HashMap<>();
        
        while (rs.next()) {
            String errorCode = rs.getString("issuetype");
            String errorMessage = rs.getString("issues");
            String firstRunStr = rs.getString("firstrun");

            boolean isFirstRun = (firstRunStr != null) && firstRunStr.equalsIgnoreCase("true");
            
            if (errorCode != null && !errorCode.trim().isEmpty()) {
                if (!errorMap.containsKey(errorCode)) {
                    JSONObject errorDetails = new JSONObject();
                    errorDetails.put("message", errorMessage != null ? errorMessage : "");
                    errorDetails.put("failedTestCasesCount", 1);
                    errorDetails.put("firstRunCount", isFirstRun ? 1 : 0);
                    errorDetails.put("rerunCount", isFirstRun ? 0 : 1);
                    errorMap.put(errorCode, errorDetails);
                } else {
                    JSONObject errorDetails = errorMap.get(errorCode);
                    int currentCount = (int) errorDetails.get("failedTestCasesCount");
                    errorDetails.put("failedTestCasesCount", currentCount + 1);
                    if (isFirstRun) {
                        int firstRunCount = (int) errorDetails.get("firstRunCount");
                        errorDetails.put("firstRunCount", firstRunCount + 1);
                    } else {
                        int rerunCount = (int) errorDetails.get("rerunCount");
                        errorDetails.put("rerunCount", rerunCount + 1);
                    }
                }
            }
        }
        
        for (Map.Entry<String, JSONObject> entry : errorMap.entrySet()) {
            errorCodes.put(entry.getKey(), entry.getValue());
        }
        
        json.put("error_codes", errorCodes);
	} catch(Exception e) {
		System.err.println(Utilities.getNow());
		e.printStackTrace();
		json.put("error", "An error occurred while fetching error codes.");
	}
	return json;
}

public void editTestSuite(int testSuiteId, int width, int height, String browser, String envurl, String stopafterfailure, String rerunfailedtests) {
	String sql = "UPDATE cta.test_suite SET width = ?, height = ?, browser = ?, envurl = ?, stopafterfailure = ?, " + 
			"rerunfailedtests = ? WHERE idtest_suite = "+ testSuiteId;
	try {
	PreparedStatement stmt = testCon.prepareStatement(sql);
	stmt.setInt(1,width);
	stmt.setInt(2,height);
	stmt.setString(3,browser);
	stmt.setString(4,envurl);
	stmt.setString(5,stopafterfailure);
	stmt.setString(6,rerunfailedtests);
	stmt.executeUpdate();
	} catch (Exception e) {
		System.err.println(Utilities.getNow());
		e.printStackTrace();
	}
}

	public void editTestCase(int testCaseId, int width, int height, String browser, String envurl, String envName) {

		if (width == -1 || width == 0) {
			editTestCaseNoRes(testCaseId, browser, envurl, envName);
			return;
		}

		String sql = "UPDATE cta.test_case SET width = ?, height = ?, browser = ?, envurl = ?, "
				+ " envname = ? WHERE idtest_case = " + testCaseId;
		try {
			PreparedStatement stmt = testCon.prepareStatement(sql);
			stmt.setInt(1, width);
			stmt.setInt(2, height);
			stmt.setString(3, browser);
			stmt.setString(4, envurl);
			stmt.setString(5, envName);
			stmt.executeUpdate();
			stmt.close();
		} catch (Exception e) {
			System.err.println(Utilities.getNow());
			e.printStackTrace();
		}
	}

	public void editTestCaseNoRes(int testCaseId, String browser, String envurl, String envName) {
		String sql = "UPDATE cta.test_case SET browser = ?, envurl = ?, envname = ?" + " WHERE idtest_case = "
				+ testCaseId;
		try {
			PreparedStatement stmt = testCon.prepareStatement(sql);
			stmt.setString(1, browser);
			stmt.setString(2, envurl);
			stmt.setString(3, envName);
			stmt.executeUpdate();
		} catch (Exception e) {
			System.err.println(Utilities.getNow());
			e.printStackTrace();
		}
	}

	public JSONObject getApi(int apiid) {

		String sql = "SELECT * FROM cta.api WHERE " + "idapi = ? ORDER BY cta.api.createddate, cta.api.updateddate ASC";
		String sql_param = "SELECT * FROM cta.apiparams WHERE apiid = ?";
		ResultSet rs = null;
		JSONObject jsono = new JSONObject();
		JSONArray param = new JSONArray();
		try {
			PreparedStatement stmt = testCon.prepareStatement(sql);
			stmt.setInt(1, apiid);
			rs = stmt.executeQuery();
			if (rs.next()) {
				jsono.put("idapi", rs.getInt("idapi"));
				jsono.put("name", rs.getString("name"));
				jsono.put("url", rs.getString("url"));
				jsono.put("version", rs.getInt("version"));
				jsono.put("createdby", rs.getString("createdby"));
				if (rs.getTimestamp("createddate") != null) {
					jsono.put("createddate", (rs.getTimestamp("createddate")).toString());
				} else {
					jsono.put("createddate", null);
				}
				jsono.put("status", rs.getInt("status"));
				jsono.put("moduleid", rs.getInt("moduleid"));
				jsono.put("envurl", rs.getString("envurl"));
				if (rs.getTimestamp("updateddate") != null) {
					jsono.put("updateddate", (rs.getTimestamp("updateddate")).toString());
				} else {
					jsono.put("updateddate", null);
				}
				jsono.put("type", rs.getString("type"));
				jsono.put("body", rs.getString("body"));
				jsono.put("headers", rs.getString("headers"));
				jsono.put("tests", rs.getString("tests"));
				PreparedStatement stmt_param = testCon.prepareStatement(sql_param);
				stmt_param.setInt(1, apiid);
				rs = stmt_param.executeQuery();
				param = getParamAsJSON(rs);
				jsono.put("param", param);
				rs.close();
				stmt_param.close();
				stmt.close();
			}
		} catch (Exception e) {
			System.err.println(Utilities.getNow());
			e.printStackTrace();
		}
		return jsono;
	}

	public JSONArray getAllApi(int moduleId) {

		String sql = "SELECT * FROM cta.api WHERE status=0 AND moduleid = ? "
				+ "ORDER BY cta.api.createddate, cta.api.updateddate ASC";
		ResultSet rs = null;
		JSONArray json = new JSONArray();

		try {
			PreparedStatement stmt = testCon.prepareStatement(sql);
			stmt.setInt(1, moduleId);
			rs = stmt.executeQuery();
			json = getApiAsJSON(rs);
			stmt.close();
		} catch (Exception e) {
			System.err.println(Utilities.getNow());
			e.printStackTrace();
		}
		return json;
	}

	public int addApiDef(String name, int modid) {
		String sql = "INSERT INTO cta.apidefinition ( moduleid, objectname) VALUES " + "(?,?)";
		int generatedKey = -1;
		try {
			PreparedStatement stmt = testCon.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
			stmt.setInt(1, modid);
			stmt.setString(2, name);
			stmt.executeUpdate();
			ResultSet rs = stmt.getGeneratedKeys();
			if (rs.next()) {
				generatedKey = rs.getInt(1);
			}
			rs.close();
			stmt.close();
		} catch (Exception e) {
			System.err.println(Utilities.getNow());
			e.printStackTrace();
		}

		return generatedKey;
	}

	public int addApiLink(int paramId, int defId) {
		String sql = "INSERT INTO cta.apiparamdeflink ( idapiparam, idapidefinition) VALUES " + "(?,?)";
		int generatedKey = -1;
		try {
			PreparedStatement stmt = testCon.prepareStatement(sql);
			stmt.setInt(1, paramId);
			stmt.setInt(2, defId);
			stmt.executeUpdate();
			stmt.close();
		} catch (Exception e) {
			System.err.println(Utilities.getNow());
			e.printStackTrace();
		}

		return generatedKey;
	}

	public int addApiDefParams(int apiDefId, String name, String value, String valueType, String enumValue,
			String defaultValue) {
		String sql = "INSERT INTO cta.apidefinitionparams ( idapidefinition, keyname, value,"
				+ "valueType, enumvalues,defaultvalue) VALUES " + "(?,?,?,?,?,?)";
		int generatedKey = -1;
		try {
			PreparedStatement stmt = testCon.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
			stmt.setInt(1, apiDefId);
			stmt.setString(2, name);
			stmt.setString(3, value);
			stmt.setString(4, valueType);
			stmt.setString(5, enumValue);
			stmt.setString(6, defaultValue);
			stmt.executeUpdate();
			ResultSet rs = stmt.getGeneratedKeys();
			if (rs.next()) {
				generatedKey = rs.getInt(1);
			}
			stmt.close();
		} catch (Exception e) {
			System.err.println(Utilities.getNow());
			e.printStackTrace();
		}

		return generatedKey;
	}

	public int addApi(String name, int version, int status, int moduleid, String envurl, String type, String body,
			String url, JSONArray param, String randomKey, String headers, String versionStr, JSONObject responses,
			int collectionId, String tests) {

		String sql = "INSERT INTO cta.api ( name, version, createdby, status, envurl, type, body, "
				+ "createddate, url, randomkey, headers, moduleid, versionStr, collection, tests) VALUES "
				+ "(?,?,?,?,?,?,?,CURRENT_TIMESTAMP,?,?,?,?,?,?,?)";
		String sql_param = "INSERT INTO cta.apiparams (apiid, keyname, value, valueType) " + "VALUES (?,?,?,?)";
		int generatedKey = -1;
		PreparedStatement stmt = null;
		try {
			if (moduleid == -1) {
				sql = "SELECT Module FROM cta.test_case WHERE extKey = ? " + "order by Created_Date Desc Limit 1";
				stmt = testCon.prepareStatement(sql);
				int extkey = Integer.valueOf(randomKey);
				stmt.setInt(1, extkey);
				ResultSet rs = stmt.executeQuery();
				if (rs.next()) {
					moduleid = rs.getInt(1);
				}

				sql = "INSERT INTO cta.api ( name, version, createdby, status, envurl, type, body, "
						+ "createddate, url, randomkey, headers, moduleid, versionStr, collection, tests) VALUES "
						+ "(?,?,?,?,?,?,?,CURRENT_TIMESTAMP,?,?,?,?,?,?,?)";
			}
			stmt = testCon.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
			stmt.setString(1, name);
			stmt.setInt(2, version);
			JSONObject ud = getUserDetails();
			String createdBy = (String) ud.get("uname");
			stmt.setString(3, createdBy);
			stmt.setInt(4, status);
			stmt.setString(5, envurl);
			stmt.setString(6, type);
			if (body != null && body.length() > 10000) {
				body = body.substring(0, 9999);
			}
			stmt.setString(7, body);
			stmt.setString(8, url);
			stmt.setString(9, randomKey);
			stmt.setString(10, headers);
//	if(moduleid != -1) stmt.setInt(11, moduleid);
			stmt.setInt(11, moduleid);
			stmt.setString(12, versionStr);
			stmt.setInt(13, collectionId);
			stmt.setString(14, tests);

			stmt.executeUpdate();
			ResultSet rs = stmt.getGeneratedKeys();
			if (rs.next()) {
				generatedKey = rs.getInt(1);
				if (param != null) {
					PreparedStatement stmt_param = testCon.prepareStatement(sql_param, Statement.RETURN_GENERATED_KEYS);
					for (Object paramObj : param) {
						JSONObject jsonObj = (JSONObject) paramObj;
						String key = (String) jsonObj.get("key");
						String value = (String) jsonObj.get("value");
						String valueType = (String) jsonObj.get("valueType");
						stmt_param.setInt(1, generatedKey);
						stmt_param.setString(2, key);
						stmt_param.setString(3, value);
						stmt_param.setString(4, valueType);
						stmt_param.executeUpdate();
						ResultSet rsParam = stmt_param.getGeneratedKeys();
						if (rsParam.next()) {
							int paramKey = rsParam.getInt(1);
							if (jsonObj.get("itemId") != null) {
								int itemId = (int) jsonObj.get("itemId");
								if (itemId > 0) {
									addApiLink(paramKey, itemId);
								}
							}
						}
						rsParam.close();
					}
					stmt_param.close();
				}

				if (responses != null && responses.size() > 0) {
					addResponses(responses, generatedKey);
				}
			}

			rs.close();
			stmt.close();
		} catch (Exception e) {
			System.err.println(Utilities.getNow());
			e.printStackTrace();
		}

		return generatedKey;
	}

	public void addResponses(JSONObject responses, int apiid) {
		String sql = "INSERT INTO cta.apiresponses ( statuscode, description, idapi ) VALUES " + "(?,?,?)";

		try {
			PreparedStatement stmt = testCon.prepareStatement(sql);
			Iterator iter = responses.keySet().iterator();
			while (iter.hasNext()) {
				String statusCode = (String) iter.next();
				JSONObject descObj = (JSONObject) responses.get(statusCode);
				String desc = (String) descObj.get("description");
				stmt.setString(1, statusCode);
				stmt.setString(2, desc);
				stmt.setInt(3, apiid);
				stmt.executeUpdate();
			}
			stmt.close();
		} catch (Exception e) {
			System.err.println(Utilities.getNow());
			e.printStackTrace();
		}
	}

	public int addDefinition(String name, String Desc, int modid) {
		String sql = "INSERT INTO cta.apicollection ( collectionname, collectiondesc, moduleid ) VALUES " + "(?,?,?)";
		int key = -1;
		try {
			PreparedStatement stmt = testCon.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
			stmt.setString(1, name);
			stmt.setString(2, Desc);
			stmt.setInt(3, modid);
			stmt.executeUpdate();
			ResultSet rs = stmt.getGeneratedKeys();
			if (rs.next()) {
				key = rs.getInt(1);
			}
			stmt.close();
		} catch (Exception e) {
			System.err.println(Utilities.getNow());
			e.printStackTrace();
		}

		return key;
	}

	public JSONObject updateApi(int apiid, String name, int status, int moduleid, String envurl, String type,
			String body, String url, JSONArray param, String headers, String tests) {
		String ver_sql = "SELECT version FROM cta.api WHERE idapi = ?";
		String sql = "UPDATE cta.api SET name = ?, version=?, status=?, moduleid=?, envurl=?, "
				+ "updateddate=CURRENT_TIMESTAMP, type =?, body=?, url=?, headers = ?, tests = ? " + "WHERE idapi = ?";
		String sql_param = "UPDATE cta.apiparams SET keyname=?, value=? WHERE idapiparams=?";
		String insert_sql_param = "INSERT INTO cta.apiparams (apiid, keyname, value) VALUES (?,?,?)";
		String del_sql_param = "DELETE FROM cta.apiparams WHERE apiid = ? AND idapiparams " + "NOT IN (&param)";
		String del_all_param = "DELETE FROM cta.apiparams WHERE apiid = ?";
		String does_param_exist = "SELECT * FROM cta.apiparams WHERE apiid = ?";

		try {
			PreparedStatement stmt_ver = testCon.prepareStatement(ver_sql);
			stmt_ver.setInt(1, apiid);
			ResultSet rs = stmt_ver.executeQuery();
			rs.next();
			int prevVersion = rs.getInt(1);
			PreparedStatement stmt = testCon.prepareStatement(sql);

			// Set the values for the prepared statement
			stmt.setString(1, name);
			stmt.setInt(2, prevVersion + 1);
			// stmt.setString(3, createdby);
			stmt.setInt(3, status);
			stmt.setInt(4, moduleid);
			stmt.setString(5, envurl);
			stmt.setString(6, type);
			stmt.setString(7, body);
			stmt.setString(8, url);
			stmt.setString(9, headers);
			stmt.setString(10, tests);
			stmt.setInt(11, apiid);
			stmt.executeUpdate();

			JSONObject origAPI = getApi(apiid);
			JSONArray paramArr = (JSONArray) origAPI.get("param");

			PreparedStatement stmt_param = testCon.prepareStatement(sql_param);
			PreparedStatement insert_stmt_param = testCon.prepareStatement(insert_sql_param);
			PreparedStatement check_param = testCon.prepareStatement(does_param_exist);

			PreparedStatement del_stmt_param = testCon.prepareStatement(del_all_param);
			del_stmt_param.setInt(1, apiid);
			del_stmt_param.execute();
			del_stmt_param.close();

			String apiparamList = "";
			if (param != null && param.size() > 0) {
				for (Object paramObj : param) {
					JSONObject jsonObj = (JSONObject) paramObj;
					String key = (String) jsonObj.get("key");
					String value = (String) jsonObj.get("value");

					if (jsonObj.get("idapiparams") instanceof Long) {
						long idapiparams = (long) jsonObj.get("idapiparams");

						check_param.setLong(1, idapiparams);
						ResultSet cp = check_param.executeQuery();

						if (cp.next()) {
							stmt_param.setString(1, key);
							stmt_param.setString(2, value);
							stmt_param.setLong(3, idapiparams);
							stmt_param.executeUpdate();
						} else {
							insert_stmt_param.setInt(1, apiid);
							insert_stmt_param.setString(2, key);
							insert_stmt_param.setString(3, value);
							insert_stmt_param.executeUpdate();
						}
//				    apiparamList = apiparamList + idapiparams + ",";
						cp.close();

					} else {
						insert_stmt_param.setInt(1, apiid);
						insert_stmt_param.setString(2, key);
						insert_stmt_param.setString(3, value);
						insert_stmt_param.executeUpdate();
					}
				}
			}

//	    if(apiparamList != null && !apiparamList.equals("")) {
//	    	apiparamList = apiparamList.substring(0,apiparamList.length()-1);
//	    	del_sql_param = del_sql_param.replace("&param",apiparamList);
//	    	PreparedStatement del_stmt_param = testCon.prepareStatement(del_sql_param);
//	    	del_stmt_param.setInt(1,  apiid);
//	    	del_stmt_param.execute();
//	    	del_stmt_param.close();
//	    } else {
//	    	PreparedStatement del_stmt_param = testCon.prepareStatement(del_all_param);
//	    	del_stmt_param.setInt(1,  apiid);
//	    	del_stmt_param.execute();
//	    	del_stmt_param.close();
//	    }
			check_param.close();
			insert_stmt_param.close();
			stmt_param.close();
			stmt.close();
		} catch (Exception e) {
			System.err.println(Utilities.getNow());
			e.printStackTrace();
		}
		return getApi(apiid);
	}

	public JSONArray getParamAsJSON(ResultSet rs) {
		JSONArray jsonArr = new JSONArray();
		try {
			while (rs.next()) {
				JSONObject jsono = new JSONObject();
				jsono.put("idapiparams", rs.getInt("idapiparams"));
				jsono.put("keyname", rs.getString("keyname"));
				jsono.put("value", rs.getString("value"));
				jsonArr.add(jsono);
			}
		} catch (Exception e) {
			System.err.println(Utilities.getNow());
			e.printStackTrace();
		}

		return jsonArr;
	}

	public void deleteApi(int apiid) {
		String del_sql = "DELETE FROM cta.apis WHERE idapi = ?";
		try {
			PreparedStatement deletestmt = testCon.prepareStatement(del_sql);
			deletestmt.setInt(1, apiid);
			deletestmt.executeUpdate();
		} catch (Exception e) {
			System.err.println(Utilities.getNow());
			e.printStackTrace();
		}
	}

	public JSONArray getApiAsJSON(ResultSet rs) {
		JSONArray jsonArr = new JSONArray();
		try {
			while (rs.next()) {
				JSONObject jsono = new JSONObject();
				jsono.put("idapi", rs.getInt("idapi"));
				jsono.put("name", rs.getString("name"));
				jsono.put("url", rs.getString("url"));
				jsono.put("version", rs.getInt("version"));
				jsono.put("createdby", rs.getString("createdby"));
				if (rs.getTimestamp("createddate") != null) {
					jsono.put("createddate", (rs.getTimestamp("createddate")).toString());
				} else {
					jsono.put("createddate", null);
				}
				jsono.put("status", rs.getInt("status"));
				jsono.put("moduleid", rs.getInt("moduleid"));
				jsono.put("envurl", rs.getString("envurl"));
				if (rs.getTimestamp("updateddate") != null) {
					jsono.put("updateddate", (rs.getTimestamp("updateddate")).toString());
				} else {
					jsono.put("updateddate", null);
				}
				jsono.put("type", rs.getString("type"));
				jsono.put("body", rs.getString("body"));
				jsono.put("tests", rs.getString("tests"));

				jsonArr.add(jsono);
			}
		} catch (Exception e) {
			System.err.println(Utilities.getNow());
			e.printStackTrace();
		}

		return jsonArr;
	}

	public int getPwdValidationKey(String username) {
		String sql = "SELECT count(username),status FROM cta.userprofile WHERE username = ?";
		String update_Sql = "UPDATE cta.userprofile SET pwdchgcompletedtime = null, pwdchangetoken = ?, pwdchgrequestedtime = CURRENT_TIMESTAMP, verificationstatus = null,"
				+ " verificationtimer = null WHERE username = ?";
		int valKey = 000000;
		try {
			PreparedStatement stmt = testCon.prepareStatement(sql);
			stmt.setString(1, username);
			ResultSet rs = stmt.executeQuery();
			while (rs.next()) {
				int count = rs.getInt(1);
				int status = rs.getInt(2);
				if (count > 0 && status != 1) {
					String vKey = Utilities.randomGen(AppProperties.NUMBER, 6, false);
					valKey = new Integer(vKey).intValue();
					PreparedStatement update_stmt = testCon.prepareStatement(update_Sql);
					update_stmt.setInt(1, valKey);
					update_stmt.setString(2, username);
					update_stmt.executeUpdate();
				}
			}
		} catch (Exception e) {
			System.err.println(Utilities.getNow());
			e.printStackTrace();
		}

		return valKey;
	}

	public boolean valAndChnagePwd(String username, int valKey) {
		String sql = "SELECT count(pwdchangetoken) FROM cta.userprofile WHERE username = ? AND pwdchangetoken = ?";
		String update_sql = "UPDATE cta.userprofile SET pwdchangetoken = null, pwdchgrequestedtime = null, verificationstatus = 'true', verificationtimer = CURRENT_TIMESTAMP"
				+ " WHERE username = ?";
		try {
			PreparedStatement stmt = testCon.prepareStatement(sql);
			stmt.setString(1, username);
			stmt.setInt(2, valKey);
			ResultSet rs = stmt.executeQuery();
			while (rs.next()) {
				int count = rs.getInt(1);
				if (count > 0) {
					PreparedStatement updt_stmt = testCon.prepareStatement(update_sql);
					updt_stmt.setString(1, username);
					updt_stmt.executeUpdate();
					return true;
				}
			}
		} catch (Exception e) {
			System.err.println(Utilities.getNow());
			e.printStackTrace();
		}
		return false;
	}

	public void changePwd(String username, String newPassword) {
		String sql = "UPDATE cta.userprofile SET password = ?, pwdchgcompletedtime = CURRENT_TIMESTAMP, status=0"
				+ " WHERE username = ? AND verificationstatus = 'true' AND TIMESTAMPDIFF(MINUTE, verificationtimer, CURRENT_TIMESTAMP) <= 5";
		try {
			String hashedPwd = PasswordUtils.hashPassword(newPassword, AppProperties.hashingsalt);
			PreparedStatement stmt = testCon.prepareStatement(sql);
			stmt.setString(1, hashedPwd);
			stmt.setString(2, username);
			stmt.executeUpdate();
		} catch (Exception e) {
			System.err.println(Utilities.getNow());
			e.printStackTrace();
		}
	}

	public void addLicense(String licensekey, int companyid, String validtill) {
		String sql = "INSERT INTO cta.licenses (licensekey, companyid, validtill) VALUES " + "(?,?,?)";
		try {
			PreparedStatement stmt = testCon.prepareStatement(sql);
			stmt.setString(1, licensekey);
			stmt.setInt(2, companyid);
			stmt.setString(3, validtill);
			stmt.executeUpdate();
		} catch (Exception e) {
			System.err.println(Utilities.getNow());
			e.printStackTrace();
		}
	}

	public HashMap<String, JSONObject> getLicenses() {
		String sql = "SELECT * FROM cta.licenses";
		HashMap<String, JSONObject> licenseMap = new HashMap<>();
		HashMap<Integer, String> licenseCompanyMap = new HashMap<>();
		try {
			Statement stmt = testCon.createStatement();
			ResultSet rs = stmt.executeQuery(sql);
			while (rs.next()) {
				int licenseId = rs.getInt("idlicenses");
				String licenseKey = rs.getString("licensekey");
				int companyId = rs.getInt("companyid");
				Date validTill = rs.getDate("validtill");
				JSONObject licenseJson = new JSONObject();
				licenseJson.put("idlicenses", licenseId);
				licenseJson.put("licensekey", licenseKey);
				licenseJson.put("companyid", companyId);
				licenseJson.put("validtill", validTill.toString());
				licenseMap.put(licenseKey, licenseJson);
				licenseCompanyMap.put(companyId, licenseKey);
			}
		} catch (Exception e) {
			System.err.println(Utilities.getNow());
			e.printStackTrace();
		}
		HashMap res = new HashMap();
		res.put("lcm", licenseCompanyMap);
		res.put("lm", licenseMap);
		return res;
	}

	private JSONObject getLicense(int companyId) {

		String sql = "SELECT * FROM cta.licenseusage where companyid = ?";

		JSONObject licenseJson = null;
		PreparedStatement stmt = null;
		ResultSet rs = null;
		try {
			stmt = testCon.prepareStatement(sql);
			stmt.setInt(1, companyId);
			rs = stmt.executeQuery(sql);

			licenseJson = new JSONObject();
			licenseJson.put("idlicenseusage", rs.getInt("idlicenseusage"));
			licenseJson.put("companyid", rs.getInt("companyid"));
			licenseJson.put("testcaseid", rs.getInt("testcaseid"));
			licenseJson.put("testcasename", rs.getString("testctestcasenameaseid"));
			licenseJson.put("key", rs.getInt("key"));
			licenseJson.put("datecreated", rs.getTimestamp("datecreated"));
			licenseJson.put("createdBy", rs.getString("createdBy"));
			licenseJson.put("licusage", rs.getString("licusage"));
		} catch (Exception e) {
			System.err.println(Utilities.getNow());
			e.printStackTrace();
		} finally {
			try {
				rs.close();
				stmt.close();
			} catch (Exception e) {

			}
		}

		return licenseJson;
	}

	public void addMobileStepIncStepnum(int tcid, int stepnumber) {
		String check_sql = "UPDATE cta.mobile_automation SET sequence_number = sequence_number + 1 WHERE Test_Case_Id = ? AND sequence_number >= ?";
		String checkStep_sql = "UPDATE cta.test_step SET ts_sequence = ts_sequence + 1 WHERE Test_Case_Id = ? AND ts_sequence >= ?";
		try {
			PreparedStatement check_stmt = testCon.prepareStatement(check_sql);
			check_stmt.setInt(1, tcid);
			check_stmt.setInt(2, stepnumber);
			check_stmt.executeUpdate();

			PreparedStatement checkStep_stmt = testCon.prepareStatement(checkStep_sql);
			checkStep_stmt.setInt(1, tcid);
			checkStep_stmt.setInt(2, stepnumber);
			checkStep_stmt.executeUpdate();

		} catch (Exception e) {
			System.err.println(Utilities.getNow());
			e.printStackTrace();
		}
	}

	public void addTestStepIncStepnum(int tcid, int stepnumber) {
		String check_sql = "UPDATE cta.test_step SET Step_Number = Step_Number + 1, tsSequence = tsSequence + 1 WHERE Test_Case_Id = ? AND tsSequence >= ?";
		try {
			PreparedStatement check_stmt = testCon.prepareStatement(check_sql);
			check_stmt.setInt(1, tcid);
			check_stmt.setInt(2, stepnumber);
			check_stmt.executeUpdate();
		} catch (Exception e) {
			System.err.println(Utilities.getNow());
			e.printStackTrace();
		}
	}

	public void addTestStep(String Keyword, String Action, String Flow, String Page_Description, String Page_Name,
			String TestData, int tcid, String xpath, int stepnumber, String subAction, String wait, int waittime,
			int teststepthreshold, String variable, String type, int tsid, int prevStepNumber) {

		boolean isMobileAutomation = false;

		if (tsid > 0) {
			try {
				String checkSQL = "SELECT COUNT(*) FROM cta.mobile_automation WHERE idtest_step = ?";
				PreparedStatement checkStmt = testCon.prepareStatement(checkSQL);
				checkStmt.setInt(1, tsid);
				ResultSet checkRs = checkStmt.executeQuery();

				if (checkRs.next() && checkRs.getInt(1) > 0) {
					isMobileAutomation = true;
				}
			} catch (Exception e) {
				System.err.println(Utilities.getNow());
				e.printStackTrace();
			}
		}

		if (isMobileAutomation) {
			try {
				addMobileStepIncStepnum(tcid, stepnumber);

				String mobileInsertSQL = "INSERT INTO cta.mobile_automation (action, device, sequence_number, idtest_step, test_case_id) VALUES (?, ?, ?, ?, ?)";
				String sql = "INSERT INTO cta.test_step (Flow, Page_Description, "
						+ "TestData, RecordedData, Test_Case_Id," + "ts_sequence) VALUES " + "(?, ?, ?, ?, ?, ?)";

				PreparedStatement mobileStepStmt = testCon.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
				mobileStepStmt.setString(1, Flow);
				mobileStepStmt.setString(2, Page_Description);
				mobileStepStmt.setString(3, TestData);
				mobileStepStmt.setString(4, TestData);
				mobileStepStmt.setInt(5, tcid);
				mobileStepStmt.setInt(6, stepnumber);

				mobileStepStmt.executeUpdate();

				ResultSet generatedKeys = mobileStepStmt.getGeneratedKeys();
				int teststepid = -1;
				if (generatedKeys.next()) {
					teststepid = (generatedKeys.getBigDecimal(1)).intValue();
				}
				generatedKeys.close();

				mobileStepStmt = testCon.prepareStatement(mobileInsertSQL);
				mobileStepStmt.setString(1, Action);
				mobileStepStmt.setString(2, Keyword);
				mobileStepStmt.setInt(3, stepnumber);
				mobileStepStmt.setInt(4, teststepid);
				mobileStepStmt.setInt(5, tcid);

				mobileStepStmt.executeUpdate();

				String sql_atr = "SELECT * FROM cta.test_step_attr WHERE test_step=?";
				mobileStepStmt = testCon.prepareStatement(sql_atr);
				mobileStepStmt.setInt(1, tsid);
				ResultSet rs = mobileStepStmt.executeQuery();

				if (rs.next()) {
					replicateTSAttr(rs, teststepid);
				}

				String uname = (String) userDetails.get("uname");
				JSONObject latestTestSteps = this.getLatestRecording(uname);
				Utilities.cacheLatestRecording(uname, latestTestSteps);
				mobileStepStmt.close();
			} catch (Exception e) {
				System.err.println(Utilities.getNow());
				e.printStackTrace();
			}
		} else {
			String sql = "INSERT INTO cta.test_step (Keyword, Action, Flow, Page_Description, Page_Name, "
					+ "TestData, RecordedData, VarName, Test_Case_Id, Object_Xpath, Step_Number, "
					+ "iframexpath, manuallyAdded, subAction, wait, waittime, teststepthreshold, tabid, "
					+ "type, tsSequence, windowid) VALUES "
					+ "(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

			try {
				int tabid = -1;
				String iframe = "";
				int windowid = -1;
				if (Keyword.equalsIgnoreCase("Tab")) {
					tabid = Integer.parseInt(Utilities.randomGen(AppProperties.NUMBER, 8, false));
				} else {
					JSONObject jsonTab = getTabId(tcid, prevStepNumber);
					if (jsonTab.get("tabid") != null) {
						tabid = (int) jsonTab.get("tabid");
						if (stepnumber != 2) {
							iframe = (String) jsonTab.get("iframe");
							windowid = (int) jsonTab.get("windowid");
						}
					}
				}

				addTestStepIncStepnum(tcid, stepnumber);

				PreparedStatement stmt = testCon.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
				stmt.setString(1, Keyword);
				stmt.setString(2, Action);
				stmt.setString(3, Flow);
				stmt.setString(4, Page_Description);
				stmt.setString(5, Page_Name);
				stmt.setString(6, TestData);
				stmt.setString(7, TestData);
				if (Action.equals("DynamicText")) {
					String varName = Utilities.randomGen("ALPHA", 6, AppProperties.chatgpton);
					varName = AppProperties.delimiter + varName + AppProperties.delimiter;
					stmt.setString(8, varName);
				} else if ((variable != null) && (!(variable.equals(null) || variable.equals("")))) {
					stmt.setString(8, variable);
				} else {
					stmt.setString(8, "");
				}
				stmt.setInt(9, tcid);
				stmt.setString(10, xpath);
				stmt.setInt(11, stepnumber);
				stmt.setString(12, iframe);
				stmt.setString(13, "true");
				stmt.setString(14, subAction);
				stmt.setString(15, wait);
				stmt.setInt(16, waittime);
				stmt.setInt(17, teststepthreshold);
				stmt.setInt(18, tabid);
				stmt.setString(19, type);
				stmt.setInt(20, stepnumber);
				stmt.setInt(21, windowid);

				stmt.executeUpdate();
				ResultSet generatedKeys = stmt.getGeneratedKeys();
				int teststepid = -1;
				if (generatedKeys.next()) {
					teststepid = (generatedKeys.getBigDecimal(1)).intValue();
				}
				generatedKeys.close();

				String sql_atr = "SELECT * FROM cta.test_step_attr WHERE test_step=?";
				stmt = testCon.prepareStatement(sql_atr);
				stmt.setInt(1, tsid);
				ResultSet rs = stmt.executeQuery();

				if (rs.next()) {
					replicateTSAttr(rs, teststepid);
				}

				String uname = (String) userDetails.get("uname");
				JSONObject latestTestSteps = this.getLatestRecording(uname);
				Utilities.cacheLatestRecording(uname, latestTestSteps);
				stmt.close();
			} catch (Exception e) {
				System.err.println(Utilities.getNow());
				e.printStackTrace();
			}
		}
//		DashBoardData dbd = new DashBoardData(this);
//		dbd.updateDashBoardForTestStepCount(tsid);
	}

	public void addBlankTestStep(String Keyword, String Action, String Flow, String Page_Description, String Page_Name,
			String TestData, int tcid, String xpath, int stepnumber, String subAction, String wait, int waittime,
			int teststepthreshold, String variable, String type, int tsid, int prevStepNumber) {

		boolean isMobileAutomation = false;

		if (tsid > 0) {
			try {
				String checkSQL = "SELECT COUNT(*) FROM cta.mobile_automation WHERE idtest_step = ?";
				PreparedStatement checkStmt = testCon.prepareStatement(checkSQL);
				checkStmt.setInt(1, tsid);
				ResultSet checkRs = checkStmt.executeQuery();

				if (checkRs.next() && checkRs.getInt(1) > 0) {
					isMobileAutomation = true;
				}
			} catch (Exception e) {
				System.err.println(Utilities.getNow());
				e.printStackTrace();
			}
		}

		if (isMobileAutomation) {
			try {
				addMobileStepIncStepnum(tcid, stepnumber);

				String mobileInsertSQL = "INSERT INTO cta.mobile_automation (action, device, sequence_number, idtest_step, test_case_id) VALUES (?, ?, ?, ?, ?)";
				String sql = "INSERT INTO cta.test_step (Flow, Page_Description, "
						+ "TestData, RecordedData, Test_Case_Id," + "ts_sequence) VALUES " + "(?, ?, ?, ?, ?, ?)";

				PreparedStatement mobileStepStmt = testCon.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
				mobileStepStmt.setString(1, Flow);
				mobileStepStmt.setString(2, null);
				mobileStepStmt.setString(3, null);
				mobileStepStmt.setString(4, null);
				mobileStepStmt.setInt(5, tcid);
				mobileStepStmt.setInt(6, stepnumber);

				mobileStepStmt.executeUpdate();

				ResultSet generatedKeys = mobileStepStmt.getGeneratedKeys();
				int teststepid = -1;
				if (generatedKeys.next()) {
					teststepid = (generatedKeys.getBigDecimal(1)).intValue();
				}
				generatedKeys.close();

				mobileStepStmt = testCon.prepareStatement(mobileInsertSQL);
				mobileStepStmt.setString(1, Action);
				mobileStepStmt.setString(2, Keyword);
				mobileStepStmt.setInt(3, stepnumber);
				mobileStepStmt.setInt(4, teststepid);
				mobileStepStmt.setInt(5, tcid);

				mobileStepStmt.executeUpdate();

				String sql_atr = "SELECT * FROM cta.test_step_attr WHERE test_step=?";
				mobileStepStmt = testCon.prepareStatement(sql_atr);
				mobileStepStmt.setInt(1, tsid);
				ResultSet rs = mobileStepStmt.executeQuery();

				if (rs.next()) {
					replicateTSAttrForBlankStep(rs, teststepid);
				}

				String uname = (String) userDetails.get("uname");
				JSONObject latestTestSteps = this.getLatestRecording(uname);
				Utilities.cacheLatestRecording(uname, latestTestSteps);
				mobileStepStmt.close();
			} catch (Exception e) {
				System.err.println(Utilities.getNow());
				e.printStackTrace();
			}
		} else {
			String sql = "INSERT INTO cta.test_step (Keyword, Action, Flow, Page_Description, Page_Name, "
					+ "TestData, RecordedData, VarName, Test_Case_Id, Object_Xpath, Step_Number, "
					+ "iframexpath, manuallyAdded, subAction, wait, waittime, teststepthreshold, tabid, "
					+ "type, tsSequence, windowid) VALUES "
					+ "(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

			try {
				int tabid = -1;
				String iframe = "";
				int windowid = -1;
				if (Keyword.equalsIgnoreCase("Tab")) {
					tabid = Integer.parseInt(Utilities.randomGen(AppProperties.NUMBER, 8, false));
				} else {
					JSONObject jsonTab = getTabId(tcid, prevStepNumber);
					tabid = (int)jsonTab.get("tabid");
					if (stepnumber != 2) {
						iframe = (String) jsonTab.get("iframe");
						windowid = (int) jsonTab.get("windowid");
					}
				}

				addTestStepIncStepnum(tcid, stepnumber);

				PreparedStatement stmt = testCon.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
				stmt.setString(1, Keyword);
				stmt.setString(2, Action);
				stmt.setString(3, Flow);
				stmt.setString(4, null);
				stmt.setString(5, Page_Name);
				stmt.setString(6, null);
				stmt.setString(7, null);
				if (Action.equals("DynamicText")) {
					String varName = Utilities.randomGen("ALPHA", 6, AppProperties.chatgpton);
					varName = AppProperties.delimiter + varName + AppProperties.delimiter;
					stmt.setString(8, varName);
				} else if (!(variable.equals(null) || variable.equals(""))) {
					stmt.setString(8, variable);
				} else {
					stmt.setString(8, "");
				}
				stmt.setInt(9, tcid);
				stmt.setString(10, null);
				stmt.setInt(11, stepnumber);
				stmt.setString(12, null);
				stmt.setString(13, "true");
				stmt.setString(14, subAction);
				stmt.setString(15, wait);
				stmt.setInt(16, waittime);
				stmt.setInt(17, teststepthreshold);
				stmt.setInt(18, tabid);
				stmt.setString(19, type);
				stmt.setInt(20, stepnumber);
				stmt.setInt(21, windowid);

				stmt.executeUpdate();
				ResultSet generatedKeys = stmt.getGeneratedKeys();
				int teststepid = -1;
				if (generatedKeys.next()) {
					teststepid = (generatedKeys.getBigDecimal(1)).intValue();
				}
				generatedKeys.close();

				String sql_atr = "SELECT * FROM cta.test_step_attr WHERE test_step=?";
				stmt = testCon.prepareStatement(sql_atr);
				stmt.setInt(1, tsid);
				ResultSet rs = stmt.executeQuery();

				if (rs.next()) {
					replicateTSAttrForBlankStep(rs, teststepid);
				}

				String uname = (String) userDetails.get("uname");
				JSONObject latestTestSteps = this.getLatestRecording(uname);
				Utilities.cacheLatestRecording(uname, latestTestSteps);
				stmt.close();
			} catch (Exception e) {
				System.err.println(Utilities.getNow());
				e.printStackTrace();
			}
		}
	}
//public void addTestStep(String Keyword, String Action, String Flow, String Page_Description, 
//		String Page_Name, String TestData, int tcid, String xpath, int stepnumber, String subAction,
//		String wait, int waittime, int teststepthreshold, String variable, String type, int tsid, 
//		int prevStepNumber) {
//	String sql = "INSERT INTO cta.test_step (Keyword, Action, Flow, Page_Description, Page_Name, "
//			+ "TestData, RecordedData, VarName, Test_Case_Id, Object_Xpath, Step_Number, "
//			+ "iframexpath, manuallyAdded, subAction, wait, waittime, teststepthreshold, tabid, "
//			+ "type, tsSequence, windowid) VALUES "
//			+ "(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
//	
//	try {		
//		int tabid = -1;
//		String iframe = "";
//		int windowid = -1;
//		if(Keyword.equalsIgnoreCase("Tab")) {
//			tabid = Integer.parseInt(Utilities.randomGen(AppProperties.NUMBER, 8, false));
//		} else {
//			JSONObject jsonTab = getTabId(tcid, prevStepNumber);
//			if(jsonTab.get("tabid") != null) {
//			tabid = (int)jsonTab.get("tabid");
//			if(stepnumber != 2) {
//				iframe = (String) jsonTab.get("iframe");
//				windowid = (int) jsonTab.get("windowid");
//			}
//			}
//		}
//		
//		addTestStepIncStepnum( tcid, stepnumber);
//		
//		PreparedStatement stmt = testCon.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
//		stmt.setString(1,Keyword);
//		stmt.setString(2,Action);
//		stmt.setString(3,Flow);
//		stmt.setString(4,Page_Description);
//		stmt.setString(5,Page_Name);
//		stmt.setString(6,TestData);
//		stmt.setString(7,TestData);
//		if(Action.equals("DynamicText")) {
//			String varName = Utilities.randomGen("ALPHA", 6, AppProperties.chatgpton);
//			varName = AppProperties.delimiter + varName + AppProperties.delimiter;
//			stmt.setString(8,varName);
//		} else if((variable != null)&&(!(variable.equals(null) || variable.equals("")))) {
//			stmt.setString(8, variable);
//		} else {
//			stmt.setString(8,"");
//		}
//		stmt.setInt(9,tcid);
//		stmt.setString(10,xpath);
//		stmt.setInt(11,stepnumber);
//		stmt.setString(12,iframe);
//		stmt.setString(13,"true");
//		stmt.setString(14,subAction);
//		stmt.setString(15,wait);
//		stmt.setInt(16,waittime);
//		stmt.setInt(17,teststepthreshold);
//		stmt.setInt(18,tabid);
//		stmt.setString(19,type);
//		stmt.setInt(20,stepnumber);
//		stmt.setInt(21,windowid);
//
//		stmt.executeUpdate();
//		ResultSet generatedKeys = stmt.getGeneratedKeys();
//		int teststepid = -1;
//		if (generatedKeys.next()) {
//			teststepid = (generatedKeys.getBigDecimal(1)).intValue();
//		}
//		generatedKeys.close();
//		
//		String sql_atr = "SELECT * FROM cta.test_step_attr WHERE test_step=?";
//		stmt = testCon.prepareStatement(sql_atr);
//		stmt.setInt(1, tsid);
//		ResultSet rs = stmt.executeQuery();
//		
//		if(rs.next()) {
//			replicateTSAttr(rs, teststepid);
//		}
//		
//		String uname = (String)userDetails.get("uname");
//		JSONObject latestTestSteps = this.getLatestRecording(uname);
//		Utilities.cacheLatestRecording(uname,  latestTestSteps);
//		stmt.close();
//		} catch (Exception e) {
//			System.err.println(Utilities.getNow());
//			e.printStackTrace();
//		}
//}
//
//public void addBlankTestStep(String Keyword, String Action, String Flow, String Page_Description, 
//		String Page_Name, String TestData, int tcid, String xpath, int stepnumber, String subAction,
//		String wait, int waittime, int teststepthreshold, String variable, String type, int tsid, 
//		int prevStepNumber) {
//
//	String sql = "INSERT INTO cta.test_step (Keyword, Action, Flow, Page_Description, Page_Name, "
//			+ "TestData, RecordedData, VarName, Test_Case_Id, Object_Xpath, Step_Number, "
//			+ "iframexpath, manuallyAdded, subAction, wait, waittime, teststepthreshold, tabid, "
//			+ "type, tsSequence, windowid) VALUES "
//			+ "(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
//	
//	try {		
//		int tabid = -1;
//		String iframe = "";
//		int windowid = -1;
//		if(Keyword.equalsIgnoreCase("Tab")) {
//			tabid = Integer.parseInt(Utilities.randomGen(AppProperties.NUMBER, 8, false));
//		} else {
//			JSONObject jsonTab = getTabId(tcid, prevStepNumber);
//			//tabid = (int)jsonTab.get("tabid");
//			if(stepnumber != 2) {
//				iframe = (String) jsonTab.get("iframe");
//				windowid = (int) jsonTab.get("windowid");
//			}
//		}
//		
//		addTestStepIncStepnum( tcid, stepnumber);
//		
//		PreparedStatement stmt = testCon.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
//		stmt.setString(1,Keyword);
//		stmt.setString(2,Action);
//		stmt.setString(3,Flow);
//		stmt.setString(4,null);
//		stmt.setString(5,Page_Name);
//		stmt.setString(6,null);
//		stmt.setString(7,null);
//		if(Action.equals("DynamicText")) {
//			String varName = Utilities.randomGen("ALPHA", 6, AppProperties.chatgpton);
//			varName = AppProperties.delimiter + varName + AppProperties.delimiter;
//			stmt.setString(8,varName);
//		} else if(!(variable.equals(null) || variable.equals(""))) {
//			stmt.setString(8, variable);
//		} else {
//			stmt.setString(8,"");
//		}
//		stmt.setInt(9,tcid);
//		stmt.setString(10,null);
//		stmt.setInt(11,stepnumber);
//		stmt.setString(12,null);
//		stmt.setString(13,"true");
//		stmt.setString(14,subAction);
//		stmt.setString(15,wait);
//		stmt.setInt(16,waittime);
//		stmt.setInt(17,teststepthreshold);
//		stmt.setInt(18,tabid);
//		stmt.setString(19,type);
//		stmt.setInt(20,stepnumber);
//		stmt.setInt(21,windowid);
//
//		stmt.executeUpdate();
//		ResultSet generatedKeys = stmt.getGeneratedKeys();
//		int teststepid = -1;
//		if (generatedKeys.next()) {
//			teststepid = (generatedKeys.getBigDecimal(1)).intValue();
//		}
//		generatedKeys.close();
//		
//		String sql_atr = "SELECT * FROM cta.test_step_attr WHERE test_step=?";
//		stmt = testCon.prepareStatement(sql_atr);
//		stmt.setInt(1, tsid);
//		ResultSet rs = stmt.executeQuery();
//		
//		if(rs.next()) {
//			replicateTSAttrForBlankStep(rs, teststepid);
//		}
//		
//		String uname = (String)userDetails.get("uname");
//		JSONObject latestTestSteps = this.getLatestRecording(uname);
//		Utilities.cacheLatestRecording(uname,  latestTestSteps);
//		stmt.close();
//		} catch (Exception e) {
//			System.err.println(Utilities.getNow());
//			e.printStackTrace();
//		}
//
//}

	public JSONObject getTabId(int tcid, int prevStepNumber) {
		JSONObject json = new JSONObject();
		String str = "SELECT tabid, iframexpath, windowid FROM cta.test_step WHERE Test_Case_Id = ? "
				+ "AND tsSequence = ?";
		try {
			PreparedStatement stmt = testCon.prepareStatement(str);
			stmt.setInt(1, tcid);
			stmt.setInt(2, prevStepNumber);
			ResultSet rs = stmt.executeQuery();
			if (rs.next()) {
				int tabid = rs.getInt("tabid");
				json.put("tabid", tabid);
				String iframexpath = rs.getString("iframexpath");
				json.put("iframe", iframexpath);
				int windowid = rs.getInt("windowid");
				json.put("windowid", windowid);
			}
			rs.close();
			stmt.close();
		} catch (Exception e) {
			System.err.println(Utilities.getNow());
			e.printStackTrace();
		}
		return json;
	}

	public void updateTestStep(int tsid, String Keyword, String Action, String subAction, String Flow,
			String Page_Description, String Page_Name, String TestData, int tcid, String xpath, String wait,
			int waittime, int teststepthreshold, String var, String type, int tabid, String iframe, int isDynamic,
			int createsAlert, int inlinetcid, String condLogic,String condexp) {

		String sql = "UPDATE cta.test_step SET Keyword=?, Action=?, subAction=?, Flow=?, "
				+ "Page_Description=?, Page_Name=?, TestData=?, Object_Xpath=?, wait=?, "
				+ "waittime=?, teststepthreshold=?, VarName=?, type=?, tabid=?, iframexpath=?, "
				+ " inlinedepedence = ? WHERE Test_Case_Id = ? AND idtest_step=?";
		String sql_atr = "UPDATE cta.test_step_attr SET isDynamic=?, createsAlert=?, condLogic=?, condexp=? WHERE test_step=?";
		String check_mobile = "SELECT COUNT(*) FROM cta.mobile_automation WHERE idtest_step =?";
		String sql_mobile = "UPDATE cta.mobile_automation SET device =? WHERE idtest_step =?";
		try {
			PreparedStatement stmt = testCon.prepareStatement(sql);
			stmt.setString(1, Keyword);
			stmt.setString(2, Action);
			stmt.setString(3, subAction);
			stmt.setString(4, Flow);
			stmt.setString(5, Page_Description);
			stmt.setString(6, Page_Name);
			stmt.setString(7, TestData);
			stmt.setString(8, xpath);
			stmt.setString(9, wait);
			stmt.setInt(10, waittime);
			stmt.setInt(11, teststepthreshold);
			stmt.setString(12, var);
			stmt.setString(13, type);
			stmt.setInt(14, tabid);
			stmt.setString(15, iframe);
//		stmt.setInt(16, inlinetcid);
			if (inlinetcid > 0) {
				stmt.setInt(16, inlinetcid); // Set the valid foreign key value
			} else {
				stmt.setNull(16, java.sql.Types.INTEGER); // Use NULL if no value exists
			}
			stmt.setInt(17, tcid);
			stmt.setInt(18, tsid);
			stmt.executeUpdate();

			stmt = testCon.prepareStatement(sql_atr);
			stmt.setInt(1, isDynamic);
			stmt.setInt(2, createsAlert);
			stmt.setString(3, condLogic);
			stmt.setString(4, condexp);
			stmt.setInt(5, tsid);
			stmt.executeUpdate();

			PreparedStatement checkStmt = testCon.prepareStatement(check_mobile);
			checkStmt.setInt(1, tsid);
			ResultSet rs = checkStmt.executeQuery();

			if (rs.next() && rs.getInt(1) > 0) {
				stmt = testCon.prepareStatement(sql_mobile);
				stmt.setString(1, Keyword);
				stmt.setInt(2, tsid);
				stmt.executeUpdate();
			}

			String uname = (String) userDetails.get("uname");
			JSONObject latestTestSteps = this.getLatestRecording(uname);
			Utilities.cacheLatestRecording(uname, latestTestSteps);

		} catch (Exception e) {
			System.err.println(Utilities.getNow());
			e.printStackTrace();
		}
	}

	public void addCustProperties(String name, String value, int productid, String comments) {
		String sql = "INSERT INTO cta.custprops (productid, name, value, comments)" + " VALUES (?, ?, ?, ?)";
		try {
			PreparedStatement stmt = testCon.prepareStatement(sql);
			stmt.setInt(1, productid);
			stmt.setString(2, name);
			stmt.setString(3, value);
			stmt.setString(4, comments);
			stmt.executeUpdate();

		} catch (Exception e) {
			System.err.println(Utilities.getNow());
			e.printStackTrace();
		}
	}

	public void updateCustProperties(int idcustprops, String name, String value, int productid, String comments) {
		String sql = "UPDATE cta.custprops SET productid=?, name=?, value=?, comments=?" + " WHERE idcustprops=?";
		try {
			PreparedStatement stmt = testCon.prepareStatement(sql);
			stmt.setInt(1, productid);
			stmt.setString(2, name);
			stmt.setString(3, value);
			stmt.setString(4, comments);
			stmt.setInt(5, idcustprops);
			stmt.executeUpdate();

		} catch (Exception e) {
			System.err.println(Utilities.getNow());
			e.printStackTrace();
		}
	}

	public JSONArray getCustProperties(int pid) {
		String sql = "SELECT * FROM cta.custprops WHERE productid=?";
		JSONArray jsonArr = new JSONArray();
		try {
			PreparedStatement stmt = testCon.prepareStatement(sql);
			stmt.setInt(1, pid);
			ResultSet rs = stmt.executeQuery();
			while (rs.next()) {
				JSONObject jsono = new JSONObject();
				jsono.put("idcustprops", rs.getInt("idcustprops"));
				jsono.put("name", rs.getString("name"));
				jsono.put("value", rs.getString("value"));
				jsono.put("productid", rs.getInt("productid"));
				jsono.put("comments", rs.getString("comments"));
				jsonArr.add(jsono);
			}
		} catch (Exception e) {
			System.err.println(Utilities.getNow());
			e.printStackTrace();
		}
		return jsonArr;
	}

	public JSONObject getNamedCustProp(int pid, String prop) {
		String sql = "SELECT * FROM cta.custprops WHERE productid=? AND name = ?";
		JSONObject jsono = new JSONObject();
		try {
			PreparedStatement stmt = testCon.prepareStatement(sql);
			stmt.setInt(1, pid);
			stmt.setString(2, prop);
			ResultSet rs = stmt.executeQuery();
			while (rs.next()) {

				jsono.put("idcustprops", rs.getInt("idcustprops"));
				jsono.put("name", rs.getString("name"));
				jsono.put("value", rs.getString("value"));
				jsono.put("productid", rs.getInt("productid"));
				jsono.put("comments", rs.getString("comments"));
			}
		} catch (Exception e) {
			System.err.println(Utilities.getNow());
			e.printStackTrace();
		}
		return jsono;
	}

	public JSONArray getLatestPass(int tcid, int cid) {
		String sql = "SELECT tsr.* " + "FROM cta.test_step_result tsr JOIN ( SELECT MAX(Executed_Date) AS MaxExecDate "
				+ "FROM cta.test_case_results WHERE Status = 'PASS' AND cta.test_case_results.Test_Case_Id=? "
				+ ") tcr ON tsr.Test_Case_Results_Id = ( SELECT cta.test_case_results.idtest_case_results "
				+ "FROM cta.test_case_results WHERE cta.test_case_results.Executed_Date = tcr.MaxExecDate "
				+ ") UNION SELECT tsr.* FROM cta.test_step_result tsr JOIN ( SELECT MAX(Executed_Date) AS MaxExecDate "
				+ "FROM cta.test_step_result WHERE Status <> 'PASS' ) tcr ON tsr.Test_Case_Results_Id = ( "
				+ "SELECT cta.test_step_result.Test_Case_Results_Id FROM cta.test_step_result "
				+ "WHERE cta.test_step_result.Executed_Date = tcr.MaxExecDate ) WHERE NOT EXISTS ( "
				+ "SELECT 1 FROM cta.test_case_results WHERE Status = 'PASS' "
				+ "AND cta.test_case_results.Test_Case_Id=? ) ORDER BY Executed_Date DESC";
		JSONArray jsonArr = new JSONArray();
		try {
			PreparedStatement stmt = testCon.prepareStatement(sql);
			stmt.setInt(1, tcid);
			stmt.setInt(2, tcid);
			ResultSet rs = stmt.executeQuery();
			while (rs.next()) {
				JSONObject jsono = getTestStepsResultsAsJSON(rs, true, cid, false, "PASSED"); // making last one false
																								// because getting some
																								// issue in image
																								// compare.
				jsonArr.add(jsono);
			}
		} catch (Exception e) {
			System.err.println(Utilities.getNow());
			e.printStackTrace();
		}
		return jsonArr;
	}

	public int getLatestPassResult(int tcid) {
		String sql = "SELECT idtest_case_results FROM cta.test_case_results "
				+ " where Status = 'PASS' and Test_Case_Id = ?";
		JSONArray jsonArr = new JSONArray();
		try {
			PreparedStatement stmt = testCon.prepareStatement(sql);
			stmt.setInt(1, tcid);
			ResultSet rs = stmt.executeQuery();
			while (rs.next()) {
				return rs.getInt("idtest_case_results");
			}
		} catch (Exception e) {
			System.err.println(Utilities.getNow());
			e.printStackTrace();
		}
		return -1;
	}

	public JSONArray getLatestPassStep(int tcid, int cid, int tsid) {

		String sql = "SELECT * FROM cta.test_step_result where Test_Step = ? and "
				+ "Status = \"PASS\" ORDER By Executed_Date LIMIT 1";
		JSONArray jsonArr = new JSONArray();
		try {
			PreparedStatement stmt = testCon.prepareStatement(sql);
			stmt.setInt(1, tsid);
			// stmt.setInt(2, tcid);
			ResultSet rs = stmt.executeQuery();
			// while(rs.next()) {
			JSONObject jsono = getTestStepsResultsAsJSON(rs, true, cid, false, "PASSED"); // making last one false
																							// because getting some
																							// issue in image compare.
			jsonArr.add(jsono);
			// }
		} catch (Exception e) {
			System.err.println(Utilities.getNow());
			e.printStackTrace();
		}
		return jsonArr;
	}

	public JSONArray getTestStepFromTestCaseResult(int tcrid, boolean getPwd) {
		String sql = "SELECT Test_Case_Id FROM cta.test_case_results WHERE idtest_case_results = " + tcrid;
		JSONArray result = new JSONArray();
		try {
			Statement stmt = testCon.createStatement();
			ResultSet rs = stmt.executeQuery(sql);
			while (rs.next()) {
				int tcid = rs.getInt(1);
				JSONObject ts = getTestStepsByTestCaseID(tcid, getPwd);
				result.add(ts);
			}
			rs.close();
			stmt.close();
		} catch (Exception e) {
			System.err.println(Utilities.getNow());
			e.printStackTrace();
		}
		return result;
	}

	public String getTestCaseResultStatus(int tcrid) {
		String sql = "SELECT Status FROM cta.test_case_results WHERE idtest_case_results = " + tcrid;
		String tcStatus = null;
		try {
			Statement stmt = testCon.createStatement();
			ResultSet rs = stmt.executeQuery(sql);
			while (rs.next()) {
				tcStatus = rs.getString(1);
			}
			rs.close();
			stmt.close();
		} catch (Exception e) {
			System.err.println(Utilities.getNow());
			e.printStackTrace();
		}
		return tcStatus;
	}

	public int incrementNoOfFailedAttempts(String username) {
		String sql = "UPDATE cta.userprofile SET nooffailedattempts = nooffailedattempts + 1 WHERE username = ?";
		int noOfAttempts = 0;
		try {
			PreparedStatement stmt = testCon.prepareStatement(sql);
			stmt.setString(1, username);
			int rowUpdated = stmt.executeUpdate();
			if (rowUpdated > 0) {
				String query = "SELECT nooffailedattempts FROM cta.userprofile WHERE username = ?";
				stmt = testCon.prepareStatement(query);
				stmt.setString(1, username);
				ResultSet rs = stmt.executeQuery();
				if (rs.next()) {
					noOfAttempts = rs.getInt("nooffailedattempts");
				}
				rs.close();
			}
			stmt.close();
		} catch (Exception e) {
			System.err.println(Utilities.getNow());
			e.printStackTrace();
		}
		return noOfAttempts;
	}

	public JSONObject updateTestSuiteUrl(int tsid, String url) {
		String sql = "UPDATE cta.test_suite SET envurl = ? WHERE idtest_suite = ?";
		JSONObject json = new JSONObject();
		try {
			PreparedStatement stmt = testCon.prepareStatement(sql);
			stmt.setString(1, url);
			stmt.setInt(2, tsid);
			stmt.executeUpdate();
			json = getTestSuite(tsid, 0);
			stmt.close();
		} catch (Exception e) {
			System.err.println(Utilities.getNow());
			e.printStackTrace();
		}
		return json;
	}

	public JSONObject updateTestSuiteResolution(int tsid, int height, int width) {
		String sql = "UPDATE cta.test_suite SET height = ?, width = ? WHERE idtest_suite = ?";
		JSONObject json = new JSONObject();
		try {
			PreparedStatement stmt = testCon.prepareStatement(sql);
			stmt.setInt(1, height);
			stmt.setInt(2, width);
			stmt.setInt(3, tsid);
			stmt.executeUpdate();
			json = getTestSuite(tsid, 0);
			stmt.close();
		} catch (Exception e) {
			System.err.println(Utilities.getNow());
			e.printStackTrace();
		}
		return json;
	}

	public JSONObject updateTestSuiteBrowser(int tsid, String browser) {
		String sql = "UPDATE cta.test_suite SET browser = ? WHERE idtest_suite = ?";
		JSONObject json = new JSONObject();
		try {
			PreparedStatement stmt = testCon.prepareStatement(sql);
			stmt.setString(1, browser);
			stmt.setInt(2, tsid);
			stmt.executeUpdate();
			json = getTestSuite(tsid, 0);
			stmt.close();
		} catch (Exception e) {
			System.err.println(Utilities.getNow());
			e.printStackTrace();
		}
		return json;
	}

	public JSONObject updateTestSuiteStopAfterFailure(int tsid, String value) {
		String sql = "UPDATE cta.test_suite SET stopafterfailure = ? WHERE idtest_suite = ?";
		JSONObject json = new JSONObject();
		try {
			PreparedStatement stmt = testCon.prepareStatement(sql);
			stmt.setString(1, value);
			stmt.setInt(2, tsid);
			stmt.executeUpdate();
			json = getTestSuite(tsid, 0);
			stmt.close();
		} catch (Exception e) {
			System.err.println(Utilities.getNow());
			e.printStackTrace();
		}
		return json;
	}
	
	public JSONObject updateTestSuiteRerunInParallel(int tsid, String value) {
	    String sql = "UPDATE cta.test_suite SET rerun_in_parallel = ? WHERE idtest_suite = ?";
	    JSONObject json = new JSONObject();
	    try {
	        PreparedStatement stmt = testCon.prepareStatement(sql);
	        stmt.setString(1, value);
	        stmt.setInt(2, tsid);
	        stmt.executeUpdate();
	        json = getTestSuite(tsid, 0);
	        stmt.close();
	    } catch (Exception e) {
	        System.err.println(Utilities.getNow());
	        e.printStackTrace();
	    }
	    return json;
	}
	
	public JSONObject updateTestSuiteThreadCount(int tsid, int value) {
	    JSONObject json = new JSONObject();
	    try {
	        int companyId = (int) userDetails.get("companyid");
	        String companyThreadCountSql = "SELECT parallelThreads FROM cta.company WHERE idcompany = ?";
	        PreparedStatement stmt1 = testCon.prepareStatement(companyThreadCountSql);
	        stmt1.setInt(1, companyId);
	        ResultSet rs1 = stmt1.executeQuery();

	        int companyThreadCount = 0;
	        if (rs1.next()) {
	            companyThreadCount = rs1.getInt("parallelThreads");
	        }
	        rs1.close();
	        stmt1.close();

	        if (value > companyThreadCount) {
	            json.put("error", "Requested thread count (" + value + ") exceeds the company's limit (" + companyThreadCount + ").");
	            return json;
	        }

	        String sql = "UPDATE cta.test_suite SET threadcount = ? WHERE idtest_suite = ?";
	        PreparedStatement stmt = testCon.prepareStatement(sql);
	        stmt.setInt(1, value);
	        stmt.setInt(2, tsid);
	        stmt.executeUpdate();
	        json = getTestSuite(tsid, 0);
	        stmt.close();

	    } catch (Exception e) {
	        System.err.println(Utilities.getNow());
	        e.printStackTrace();
	        json.put("error", "An unexpected error occurred while updating thread count.");
	    }
	    return json;
	}



	public JSONObject updateTestSuiteRerunFailedTests(int tsid, String value) {
		String sql = "UPDATE cta.test_suite SET rerunfailedtests = ? WHERE idtest_suite = ?";
		JSONObject json = new JSONObject();
		try {
			PreparedStatement stmt = testCon.prepareStatement(sql);
			stmt.setString(1, value);
			stmt.setInt(2, tsid);
			stmt.executeUpdate();
			json = getTestSuite(tsid, 0);
			stmt.close();
		} catch (Exception e) {
			System.err.println(Utilities.getNow());
			e.printStackTrace();
		}
		return json;
	}

	public JSONObject updateTestSuiteSupportsParallelExecution(int tsid, String value) {
		String sql = "UPDATE cta.test_suite SET supports_parallel_execution = ? WHERE idtest_suite = ?";
		JSONObject json = new JSONObject();
		try {
			PreparedStatement stmt = testCon.prepareStatement(sql);
			stmt.setString(1, value);
			stmt.setInt(2, tsid);
			stmt.executeUpdate();
			json = getTestSuite(tsid, 0);
			stmt.close();
		} catch (Exception e) {
			System.err.println(Utilities.getNow());
			e.printStackTrace();
		}
		return json;
	}

	public JSONObject updateTestSuiteSSOnError(int tsid, String value) {
		String sql = "UPDATE cta.test_suite SET ssonerror = ? WHERE idtest_suite = ?";
		JSONObject json = new JSONObject();
		try {
			PreparedStatement stmt = testCon.prepareStatement(sql);
			int valueint = 0;
			if (value != null && value.equals("true")) {
				valueint = 1;
			}
			stmt.setInt(1, valueint);
			stmt.setInt(2, tsid);
			stmt.executeUpdate();
			json = getTestSuite(tsid, 0);
			stmt.close();
		} catch (Exception e) {
			System.err.println(Utilities.getNow());
			e.printStackTrace();
		}
		return json;
	}

	public JSONObject updateTestSuiteIndEmail(int tsid, String value) {
		String sql = "UPDATE cta.test_suite SET indEmail = ? WHERE idtest_suite = ?";
		JSONObject json = new JSONObject();
		try {
			PreparedStatement stmt = testCon.prepareStatement(sql);
			int valueint = 0;
			if (value != null && value.equals("true")) {
				valueint = 1;
			}
			stmt.setInt(1, valueint);
			stmt.setInt(2, tsid);
			stmt.executeUpdate();
			json = getTestSuite(tsid, 0);
			stmt.close();
		} catch (Exception e) {
			System.err.println(Utilities.getNow());
			e.printStackTrace();
		}
		return json;
	}

	public JSONObject updateTestSuiteResultsEmail(int tsid, String email) {
		String sql = "UPDATE cta.test_suite SET resultsemail = ? WHERE idtest_suite = ?";
		JSONObject json = new JSONObject();
		try {
			PreparedStatement stmt = testCon.prepareStatement(sql);
			stmt.setString(1, email);
			stmt.setInt(2, tsid);
			stmt.executeUpdate();
			json = getTestSuite(tsid, 0);
			stmt.close();
		} catch (Exception e) {
			System.err.println(Utilities.getNow());
			e.printStackTrace();
		}
		return json;
	}

	public JSONObject updateTestSuiteBuildTag(int tsid, String buildtag) {
		String sql = "UPDATE cta.test_suite SET buildtag = ? WHERE idtest_suite = ?";
		JSONObject json = new JSONObject();
		try {
			PreparedStatement stmt = testCon.prepareStatement(sql);
			stmt.setString(1, buildtag);
			stmt.setInt(2, tsid);
			stmt.executeUpdate();
			json = getTestSuite(tsid, 0);
			stmt.close();
		} catch (Exception e) {
			System.err.println(Utilities.getNow());
			e.printStackTrace();
		}
		return json;
	}

public JSONArray getTestStepResultFromTestCase(int tcid, int companyId) {
	String sql= "SELECT tsr.*, tsr.duration as test_step_duration, tsr.Status as test_step_status, "
			+ "tsr.issuetype as test_step_issuetype, tsr.issues as test_step_issues "
			+ "FROM cta.test_step_result tsr JOIN ( SELECT MAX(Executed_Date) AS MaxExecDate "
			+ "FROM cta.test_case_results WHERE Status = 'PASS' AND cta.test_case_results.Test_Case_Id=? "
			+ ") tcr ON tsr.Test_Case_Results_Id = ( SELECT cta.test_case_results.idtest_case_results "
			+ "FROM cta.test_case_results WHERE cta.test_case_results.Executed_Date = tcr.MaxExecDate "
			+ ") UNION SELECT tsr.*, tsr.duration as test_step_duration, tsr.Status as test_step_status, "
			+ "tsr.issuetype as test_step_issuetype, tsr.issues as test_step_issues "
			+ "FROM cta.test_step_result tsr JOIN ( SELECT MAX(Executed_Date) AS MaxExecDate "
			+ "FROM cta.test_step_result WHERE Status <> 'PASS' AND cta.test_step_result.Test_Case_Results_Id=?) tcr ON tsr.Test_Case_Results_Id = ( "
			+ "SELECT cta.test_step_result.Test_Case_Results_Id FROM cta.test_step_result "
			+ "WHERE cta.test_step_result.Executed_Date = tcr.MaxExecDate LIMIT 1) WHERE NOT EXISTS ( "
			+ "SELECT 1 FROM cta.test_case_results WHERE Status = 'PASS' "
			+ "AND cta.test_case_results.Test_Case_Id=? ) ORDER BY Step_Number";
	JSONArray jsonArr = new JSONArray();
	try {
        PreparedStatement stmt = testCon.prepareStatement(sql);
        stmt.setInt(1, tcid);
        stmt.setInt(2, tcid);
        stmt.setInt(3, tcid);
        ResultSet rs = stmt.executeQuery();
        while(rs.next()) {	
        	JSONObject jsono = getTestStepResultAsJSON(rs);
        	int tcrid = (int)jsono.get("Test_Case_Results_Id");
    		String fileurl = AppProperties.fileurl + "?action=executionlog&token="+randomkey+"&companyid="+companyId+"&tcid="+tcid+"&tcrid="+tcrid;
    		jsono.put("logFile", fileurl);
			jsonArr.add(jsono);
        }
	} catch (Exception e) {
		System.err.println(Utilities.getNow());
		e.printStackTrace();
	}
	return jsonArr;	
}

	public JSONObject copyTestCase(int tcid, String tcName) {
		String Executed_By = (String) userDetails.get("uname");
		String sql = "INSERT INTO cta.test_case (Test_Case, XLS_Location, Version, Created_By,"
				+ "Created_Date, SaveType, extKey, Status, Module, width, height, browser, envurl, copiedfrom, testcasethreshold) "
				+ "VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
		String sql_ts = "SELECT * FROM cta.test_step WHERE Test_Case_Id=? AND status=0";
		String sql2 = "INSERT INTO cta.test_step_attr (test_step, imgpath, bgcolor, color, pageurl, uniquetext, uniquetoparent, parenttotarget)"
				+ " VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

		String sql_atr = "SELECT * FROM cta.test_step_attr WHERE test_step=?";
		JSONObject jsonTC = new JSONObject();
		try {
			JSONObject json = getTestCaseDetailsFromId(tcid);
			PreparedStatement stmt = testCon.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
			stmt.setString(1, tcName);
			stmt.setString(2, (String) json.get("XLS_Location"));
			stmt.setInt(3, ((int) json.get("Version") + 1));
			stmt.setString(4, Executed_By);
			stmt.setString(5, (String) json.get("SaveType"));
			stmt.setInt(6, (int) json.get("extKey"));
			stmt.setInt(7, (int) json.get("Status"));
			stmt.setInt(8, (int) json.get("Module"));
			stmt.setInt(9, (int) json.get("width"));
			stmt.setInt(10, (int) json.get("height"));
			stmt.setString(11, (String) json.get("browser"));
			stmt.setString(12, (String) json.get("envurl"));
			stmt.setInt(13, tcid);
			stmt.setInt(14, (int) json.get("testcasethreshold"));
			stmt.executeUpdate();
			ResultSet generatedKeys = stmt.getGeneratedKeys();
			int testcaseid = -1;
			if (generatedKeys.next()) {
				testcaseid = (generatedKeys.getBigDecimal(1)).intValue();
				DashBoardData dbd = new DashBoardData(this);
				dbd.updateDashBoardForTCCount(testcaseid);
			}
			generatedKeys.close();
			stmt = testCon.prepareStatement(sql_ts);
			stmt.setInt(1, tcid);
			ResultSet rs = stmt.executeQuery();
			while (rs.next()) {
				JSONObject ts_json = getTestStepsFromDb(rs, true, false);
				int idtest_step = (int) ts_json.get("idtest_step");
				int step_number = (int) ts_json.get("Step_Number");
				String page_name = (String) ts_json.get("Page_Name");
				String page_desc = (String) ts_json.get("Page_Description");
				String keyword = (String) ts_json.get("Keyword");
				String xpath = (String) ts_json.get("Object_Xpath");
				String action = (String) ts_json.get("Action");
				String flow = (String) ts_json.get("Flow");
				String testdata = (String) ts_json.get("TestData");
				String varname = (String) ts_json.get("VarName");
				String ValDevice = (String) ts_json.get("ValDevice");
				int extkey = -1;
				if ((String) ts_json.get("extKey") == null) {
					extkey = 0;
				} else
					extkey = Integer.parseInt((String) ts_json.get("extKey"));
				double eventTime = -1;
				Object eventTimeValue = ts_json.get("eventTime");
				if (eventTimeValue != null && eventTimeValue == "") {
					try {
						eventTime = Double.parseDouble(eventTimeValue.toString());
					} catch (NumberFormatException e) {
						System.err.println(Utilities.getNow());
						e.printStackTrace();
					}
				} else {
					eventTime = 0;
				}
				String filename = (String) ts_json.get("filename");
				String fileField = (String) ts_json.get("fileField");
				String DBUrl = (String) ts_json.get("DBUrl");
				int apiid = (int) ts_json.get("apiid");
				String apiparam = (String) ts_json.get("apiparam");
				String DBQuery = (String) ts_json.get("DBQuery");
				String DBType = (String) ts_json.get("DBType");
				int clickable = (int) ts_json.get("clickable");
				String issues = (String) ts_json.get("Issues");
				long actualTime = (long) ts_json.get("actualtime");
				String eventName = (String) ts_json.get("eventname");
				String iframexpath = (String) ts_json.get("iframexpath");
				String altpath = (String) ts_json.get("altpath");
				String outerhtml = (String) ts_json.get("outerhtml");
				String manuallyAdded = (String) ts_json.get("manuallyAdded");
				long tabid = (int) ts_json.get("tabid");
				long windowid = (int) ts_json.get("windowid");
				String type = (String) ts_json.get("type");
				int tsSequence = step_number;

				int newTestStep = insertIntoTestSteps(testcaseid, step_number, page_name, page_desc, xpath, keyword,
						action, flow, testdata, varname, ValDevice, extkey, eventTime, filename, fileField, DBUrl,
						apiid, apiparam, DBQuery, DBType, clickable, issues, actualTime, eventName, iframexpath,
						altpath, outerhtml, manuallyAdded, tabid, type, tsSequence, windowid);

				PreparedStatement stmt_atr = testCon.prepareStatement(sql_atr);
				stmt_atr.setInt(1, idtest_step);
				ResultSet rs_atr = stmt_atr.executeQuery();
				if (rs_atr.next()) {
					int test_step = newTestStep;
					replicateTSAttr(rs_atr, test_step);
					rs_atr.close();
				}

			}
			jsonTC = getTestCaseDetailsFromId(testcaseid);

			String uname = (String) userDetails.get("uname");
			JSONObject latestTestSteps = this.getLatestRecording(uname);
			Utilities.cacheLatestRecording(uname, latestTestSteps);
			rs.close();
			stmt.close();
		} catch (Exception e) {
			System.err.println(Utilities.getNow());
			e.printStackTrace();
		}
		return jsonTC;
	}

	public void replicateTSAttr(ResultSet rs, int teststep) {
		String sql = "INSERT INTO cta.test_step_attr (test_step, "
				+ "imgpath, bgcolor, color, pageurl, uniquetext, uniquetoparent,"
				+ "parenttotarget, isDynamic, dynamicProcessed, endrange, scope, pagename, pagenumber, "
				+ "locationstrategy, searchpageXpath, shadowdom, shadowindex, shadowelement,"
				+ "shadowpath, toaddress, emailsubject, emailcontent, uniqueisbackup, elementId, scriptfile,"
				+ "createsAlert) " + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, " + "?, ?, ?, ?, ?, ?, ?, ?, ?, ?, "
				+ "?, ?, ?, ?, ?, ?, ?)";

		try {
			PreparedStatement stmt = testCon.prepareStatement(sql);
			stmt.setInt(1, teststep);
			stmt.setString(2, rs.getString("imgpath"));
			stmt.setString(3, rs.getString("bgcolor"));
			stmt.setString(4, rs.getString("color"));
			stmt.setString(5, rs.getString("pageurl"));
			stmt.setString(6, rs.getString("uniquetext"));
			stmt.setString(7, rs.getString("uniquetoparent"));
			stmt.setString(8, rs.getString("parenttotarget"));
			stmt.setInt(9, rs.getInt("isDynamic"));
			stmt.setInt(10, rs.getInt("dynamicProcessed"));
			stmt.setString(11, rs.getString("endrange"));
			stmt.setString(12, rs.getString("scope"));
			stmt.setString(13, rs.getString("pagename"));
			stmt.setInt(14, rs.getInt("pagenumber"));
			stmt.setString(15, rs.getString("locationstrategy"));
			stmt.setString(16, rs.getString("searchpageXpath"));
			stmt.setInt(17, rs.getInt("shadowdom"));
			stmt.setInt(18, rs.getInt("shadowindex"));
			stmt.setString(19, rs.getString("shadowelement"));
			stmt.setString(20, rs.getString("shadowpath"));
			stmt.setString(21, rs.getString("toaddress"));
			stmt.setString(22, rs.getString("emailsubject"));
			stmt.setString(23, rs.getString("emailcontent"));
			stmt.setInt(24, rs.getInt("uniqueisbackup"));

			int eleid = rs.getInt("elementId");
			if (eleid == 0) {
				stmt.setString(25, null);
			} else {
				stmt.setInt(25, eleid);
			}

			stmt.setString(26, rs.getString("scriptfile"));
			stmt.setString(27, rs.getString("createsAlert"));
			stmt.executeUpdate();
			stmt.close();
		} catch (Exception e) {
			System.err.println(Utilities.getNow());
			e.printStackTrace();
		}
	}

	public void replicateTSAttrForBlankStep(ResultSet rs, int teststep) {
		String sql = "INSERT INTO cta.test_step_attr (test_step, "
				+ "imgpath, bgcolor, color, pageurl, uniquetext, uniquetoparent,"
				+ "parenttotarget, isDynamic, dynamicProcessed, endrange, scope, pagename, pagenumber, "
				+ "locationstrategy, searchpageXpath, shadowdom, shadowindex, shadowelement,"
				+ "shadowpath, toaddress, emailsubject, emailcontent, uniqueisbackup, elementId, scriptfile,"
				+ "createsAlert) " + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, " + "?, ?, ?, ?, ?, ?, ?, ?, ?, ?, "
				+ "?, ?, ?, ?, ?, ?, ?)";

		try {
			PreparedStatement stmt = testCon.prepareStatement(sql);
			stmt.setInt(1, teststep);
			stmt.setString(2, rs.getString("imgpath"));
			stmt.setString(3, rs.getString("bgcolor"));
			stmt.setString(4, rs.getString("color"));
			stmt.setString(5, rs.getString("pageurl"));
			stmt.setString(6, null);
			stmt.setString(7, null);
			stmt.setString(8, null);
			stmt.setInt(9, rs.getInt("isDynamic"));
			stmt.setInt(10, rs.getInt("dynamicProcessed"));
			stmt.setString(11, rs.getString("endrange"));
			stmt.setString(12, rs.getString("scope"));
			stmt.setString(13, rs.getString("pagename"));
			stmt.setInt(14, rs.getInt("pagenumber"));
			stmt.setString(15, rs.getString("locationstrategy"));
			stmt.setString(16, null);
			stmt.setInt(17, rs.getInt("shadowdom"));
			stmt.setInt(18, rs.getInt("shadowindex"));
			stmt.setString(19, rs.getString("shadowelement"));
			stmt.setString(20, rs.getString("shadowpath"));
			stmt.setString(21, rs.getString("toaddress"));
			stmt.setString(22, rs.getString("emailsubject"));
			stmt.setString(23, rs.getString("emailcontent"));
			stmt.setInt(24, rs.getInt("uniqueisbackup"));

			int eleid = rs.getInt("elementId");
			if (eleid == 0) {
				stmt.setString(25, null);
			} else {
				stmt.setInt(25, eleid);
			}

			stmt.setString(26, rs.getString("scriptfile"));
			stmt.setString(27, rs.getString("createsAlert"));
			stmt.executeUpdate();
			stmt.close();
		} catch (Exception e) {
			System.err.println(Utilities.getNow());
			e.printStackTrace();
		}
	}

	public JSONObject changeTestCaseName(int tcid, String tcname) {
		String sql = "UPDATE cta.test_case SET Test_Case = ? where idtest_case = ?";
		JSONObject json = new JSONObject();
		try {
			PreparedStatement stmt = testCon.prepareStatement(sql);
			stmt.setInt(2, tcid);
			stmt.setString(1, tcname);
			stmt.executeUpdate();
			json = getTestCaseDetailsFromId(tcid);

			String uname = (String) userDetails.get("uname");
			JSONObject latestTestSteps = this.getLatestRecording(uname);
			Utilities.cacheLatestRecording(uname, latestTestSteps);
		} catch (Exception e) {
			System.err.println(Utilities.getNow());
			e.printStackTrace();
		}
		return json;
	}

	public String getLicenceToken(int companyId) {
		String sql = "SELECT licensekey FROM cta.licenses WHERE companyid = " + companyId;
		String token = "";
		try {
			Statement stmt = testCon.createStatement();
			ResultSet rs = stmt.executeQuery(sql);
			if (rs.next()) {
				token = rs.getString(1);
			}
		} catch (Exception e) {
			System.err.println(Utilities.getNow());
			e.printStackTrace();
		}
		return token;
	}

	public JSONObject getCompanyDetailsFromURL(String companyUrl) {
		JSONObject companyDetails = new JSONObject();
		String companyname = "";
		int companyId = -1;

		String sql = "SELECT companyname, idcompany FROM cta.company WHERE companyurl = ? AND (registrationcheck IS NOT NULL AND registrationcheck = 1)";

		try (PreparedStatement stmt = testCon.prepareStatement(sql)) {
			stmt.setString(1, companyUrl);
			ResultSet rs = stmt.executeQuery();

			if (rs.next()) {
				companyname = rs.getString("companyname");
				companyId = rs.getInt("idcompany");
			}
		} catch (SQLException e) {
			System.err.println(Utilities.getNow());
			e.printStackTrace();
		}

		companyDetails.put("companyname", companyname);
		companyDetails.put("companyId", companyId);

		return companyDetails;
	}

	public JSONArray getUsersByStatus(int companyId) {
		JSONArray users = new JSONArray();
		String sql = "SELECT * FROM cta.userprofile WHERE company = ?";

		try (PreparedStatement stmt = testCon.prepareStatement(sql)) {
			stmt.setInt(1, companyId);
			ResultSet rs = stmt.executeQuery();

			while (rs.next()) {
				JSONObject user = new JSONObject();
				user.put("iduserprofile", rs.getInt("iduserprofile"));
				user.put("username", rs.getString("username"));
				user.put("firstname", rs.getString("firstname"));
				user.put("lastname", rs.getString("lastname"));
				user.put("status", rs.getInt("status"));
				user.put("phonenumber", rs.getString("phonenumber"));
				user.put("email", rs.getString("email"));
				user.put("usertype", rs.getInt("usertype"));

				users.add(user);
			}
		} catch (SQLException e) {
			System.err.println(Utilities.getNow());
			e.printStackTrace();
		}

		return users;
	}

	public String changeUserStatus(int companyId, int iduserprofile) {
		String sql = "UPDATE cta.userprofile SET status = 0 WHERE iduserprofile = ? AND company = ?";
		int updatedCount = 0;

		try (PreparedStatement stmt = testCon.prepareStatement(sql)) {
			stmt.setInt(1, iduserprofile);
			stmt.setInt(2, companyId);
			updatedCount = stmt.executeUpdate();

		} catch (SQLException e) {
			System.err.println(Utilities.getNow());
			e.printStackTrace();
		}

		if (updatedCount > 0) {
			return "Success";
		} else {
			return "Failed";
		}
	}

	public String resetUserStatusAndFields(int companyId, int iduserprofile) {
		int resetCount = 0;
		String updateSql = "UPDATE cta.userprofile SET status = 0, "
				+ "pwdchangetoken = NULL, pwdchgrequestedtime = NULL, "
				+ "pwdchgcompletedtime = NULL, verificationstatus = NULL, "
				+ "verificationtimer = NULL, nooffailedattempts = 0 "
				+ "WHERE status = 2 AND iduserprofile = ? AND company = ?";

		try (PreparedStatement stmt = testCon.prepareStatement(updateSql)) {
			stmt.setInt(1, iduserprofile);
			stmt.setInt(2, companyId);
			stmt.executeUpdate();
		} catch (SQLException e) {
			System.err.println(Utilities.getNow());
			e.printStackTrace();
		}
		if (resetCount == 0) {
			return "Success";
		} else {
			return "Failed";
		}
	}

	public int getThreshold(int tid) {
		String sql = "SELECT testcasethreshold FROM cta.test_case WHERE idtest_case = " + tid;
		int threshold = 0;
		try {
			Statement stmt = testCon.createStatement();
			ResultSet rs = stmt.executeQuery(sql);
			if (rs.next()) {
				threshold = rs.getInt(1);
			}
		} catch (Exception e) {
			System.err.println(Utilities.getNow());
			e.printStackTrace();
		}
		return threshold;
	}

	public void insertEmailConnectDetails(int prodId, String name, String emailhost, String protocol, int portNo) {
		String sql = "INSERT INTO cta.custprops (productid, value, name, comments) " + "VALUES (?, ?, ?, ?)";
		String check = "SELECT * FROM cta.custprops WHERE productid=? AND name=?";
		try {
			PreparedStatement stmt = testCon.prepareStatement(check);
			stmt.setInt(1, prodId);
			stmt.setString(2, name);
			ResultSet rs = stmt.executeQuery();
			if (rs.next()) {
				updateEmailConnectDetails(prodId, name, emailhost, protocol, portNo);
			} else {
				stmt = testCon.prepareStatement(sql);
				String value = protocol + ";" + portNo + ";" + emailhost + ";";
				stmt.setInt(1, prodId);
				stmt.setString(2, value);
				stmt.setString(3, name);
				stmt.setString(4, "Email connection credentials");
				stmt.executeUpdate();
			}

		} catch (Exception e) {
			System.err.println(Utilities.getNow());
			e.printStackTrace();
		}
	}

	public void insertSendEmailConnectDetails(int prodId, String name, String emailhost, int portNo) {
		String sql = "INSERT INTO cta.custprops (productid, value, name, comments) " + "VALUES (?, ?, ?, ?)";
		String check = "SELECT * FROM cta.custprops WHERE productid=? AND name=?";
		try {
			PreparedStatement stmt = testCon.prepareStatement(check);
			stmt.setInt(1, prodId);
			stmt.setString(2, name);
			ResultSet rs = stmt.executeQuery();
			if (rs.next()) {
				updateSendEmailConnectDetails(prodId, name, emailhost, portNo);
			} else {
				stmt = testCon.prepareStatement(sql);
				String value = emailhost + ";" + portNo + ";";
				stmt.setInt(1, prodId);
				stmt.setString(2, value);
				stmt.setString(3, name);
				stmt.setString(4, "Email Sending Creds");
				stmt.executeUpdate();
			}

		} catch (Exception e) {
			System.err.println(Utilities.getNow());
			e.printStackTrace();
		}
	}

	public void updateEmailConnectDetails(int prodId, String name, String emailhost, String protocol, int portNo) {
		String sql = "Update cta.custprops SET value = ?" + " WHERE productid = ? AND name = ?";
		try {
			PreparedStatement stmt = testCon.prepareStatement(sql);
			String value = protocol + ";" + portNo + ";" + emailhost + ";";
			stmt.setString(1, value);
			stmt.setInt(2, prodId);
			stmt.setString(3, name);
			stmt.executeUpdate();
		} catch (Exception e) {
			System.err.println(Utilities.getNow());
			e.printStackTrace();
		}
	}

	public void updateSendEmailConnectDetails(int prodId, String name, String emailhost, int portNo) {
		String sql = "Update cta.custprops SET value = ?" + " WHERE productid = ? AND name = ?";
		try {
			PreparedStatement stmt = testCon.prepareStatement(sql);
			String value = emailhost + ";" + portNo + ";";
			stmt.setString(1, value);
			stmt.setInt(2, prodId);
			stmt.setString(3, name);
			stmt.executeUpdate();
		} catch (Exception e) {
			System.err.println(Utilities.getNow());
			e.printStackTrace();
		}
	}

	public void insertDbConnectDetails(int prodId, String name, String DBUrl, String DBUserName, String DBPwd,
			String DBType) {
		String sql = "INSERT INTO cta.custprops (productid, value, name, comments) " + "VALUES (?, ?, ?, ?)";
		String check = "SELECT * FROM cta.custprops WHERE productid=? AND name=?";
		try {
			PreparedStatement stmt = testCon.prepareStatement(check);
			stmt.setInt(1, prodId);
			stmt.setString(2, name);
			ResultSet rs = stmt.executeQuery();
			if (rs.next()) {
				updateDbConnectDetails(prodId, name, DBUrl, DBUserName, DBPwd, DBType);
			} else {
				stmt = testCon.prepareStatement(sql);
				String value = DBType + ";" + DBUrl + ";" + DBUserName + ";" + DBPwd + ";";
				stmt.setInt(1, prodId);
				stmt.setString(2, value);
				stmt.setString(3, name);
				stmt.setString(4, "Db connection credentials");
				stmt.executeUpdate();
			}

		} catch (Exception e) {
			System.err.println(Utilities.getNow());
			e.printStackTrace();
		}
	}

	public void updateDbConnectDetails(int prodId, String name, String DBUrl, String DBUserName, String DBPwd,
			String DBType) {
		String sql = "Update cta.custprops SET value = ?" + " WHERE productid = ? AND name = ?";
		try {
			PreparedStatement stmt = testCon.prepareStatement(sql);
			String value = DBType + ";" + DBUrl + ";" + DBUserName + ";" + DBPwd + ";";
			stmt.setString(1, value);
			stmt.setInt(2, prodId);
			stmt.setString(3, name);
			stmt.executeUpdate();
		} catch (Exception e) {
			System.err.println(Utilities.getNow());
			e.printStackTrace();
		}
	}

	public JSONObject getEmailConnectionDetails(int prodId) {
		String sql = "SELECT SUBSTRING_INDEX(value, ';', 1) AS protocol,"
				+ " SUBSTRING_INDEX(SUBSTRING_INDEX(value, ';', 2), ';', -1) AS port,"
				+ " SUBSTRING_INDEX(SUBSTRING_INDEX(value, ';', 3), ';', -1) AS host "
				+ "FROM cta.custprops WHERE productid=? AND name=?";
		JSONObject json = new JSONObject();
		try {
			PreparedStatement stmt = testCon.prepareStatement(sql);
			stmt.setInt(1, prodId);
			stmt.setString(2, "mailConnetionCred");
			ResultSet rs = stmt.executeQuery();
			if (rs.next()) {
				String protocol = rs.getString("protocol");
				int port = rs.getInt("port");
				String host = rs.getString("host");
				json.put("protocol", protocol);
				json.put("port", port);
				json.put("host", host);
			}

			sql = "SELECT SUBSTRING_INDEX(value, ';', 1) AS host,"
					+ " SUBSTRING_INDEX(SUBSTRING_INDEX(value, ';', 2), ';', -1) AS port "
//    			+ " SUBSTRING_INDEX(SUBSTRING_INDEX(value, ';', 3), ';', -1) AS host "
					+ "FROM cta.custprops WHERE productid=? AND name=?";
			stmt = testCon.prepareStatement(sql);
			stmt.setInt(1, prodId);
			stmt.setString(2, "mailSendConnectionCred");
			rs = stmt.executeQuery();
			if (rs.next()) {
				int port = rs.getInt("port");
				String host = rs.getString("host");
				json.put("sendport", port);
				json.put("sendhost", host);
			}
		} catch (Exception e) {
			System.err.println(Utilities.getNow());
			e.printStackTrace();
		}
		return json;
	}

	public JSONObject getSendEmailConnectionDetails(int prodId) {
		String sql = "SELECT " + " SUBSTRING_INDEX(SUBSTRING_INDEX(value, ';', 1), ';', -1) AS host,"
				+ " SUBSTRING_INDEX(SUBSTRING_INDEX(value, ';', 2), ';', -1) AS port "
				+ "FROM cta.custprops WHERE productid=? AND name=?";
		JSONObject json = new JSONObject();
		try {
			PreparedStatement stmt = testCon.prepareStatement(sql);
			stmt.setInt(1, prodId);
			stmt.setString(2, "mailSendConnectionCred");
			ResultSet rs = stmt.executeQuery();
			if (rs.next()) {
//        	String protocol = rs.getString("protocol");
				int port = rs.getInt("port");
				String host = rs.getString("host");
//            json.put("protocol", protocol);
				json.put("port", port);
				json.put("host", host);
			}
		} catch (Exception e) {
			System.err.println(Utilities.getNow());
			e.printStackTrace();
		}
		return json;
	}

	public JSONObject getDbConnectionDetails(int prodId) {
		String sql = "SELECT SUBSTRING_INDEX(value, ';', 1) AS custDbType,"
				+ " SUBSTRING_INDEX(SUBSTRING_INDEX(value, ';', 2), ';', -1) AS custDbUrl, "
				+ " SUBSTRING_INDEX(SUBSTRING_INDEX(value, ';', 3), ';', -1) AS custDbUsername, "
				+ " SUBSTRING_INDEX(SUBSTRING_INDEX(value, ';', 4), ';', -1) AS custDbPwd "
				+ "FROM cta.custprops WHERE productid=? AND name=?";
		JSONObject json = new JSONObject();
		try {
			PreparedStatement stmt = testCon.prepareStatement(sql);
			stmt.setInt(1, prodId);
			stmt.setString(2, "dbConnectionCred");
			ResultSet rs = stmt.executeQuery();
			if (rs.next()) {
				String custDbType = rs.getString("custDbType");
				String custDbUrl = rs.getString("custDbUrl");
				String custDbUsername = rs.getString("custDbUsername");
				String custDbPwd = rs.getString("custDbPwd");
				json.put("custDbType", custDbType);
				json.put("custDbUrl", custDbUrl);
				json.put("custDbUsername", custDbUsername);
				json.put("custDbPwd", custDbPwd);
			}
		} catch (Exception e) {
			System.err.println(Utilities.getNow());
			e.printStackTrace();
		}
		return json;
	}

	public int getProdIdFromTestStepId(int tsid) {
		String sql = "SELECT m.product From cta.test_step ts "
				+ "	JOIN cta.test_case tc on ts.Test_Case_Id = tc.idtest_case"
				+ "	JOIN cta.modules m ON tc.Module = m.idmodules WHERE ts.idtest_step = ?";
		int prodId = -1;
		try {
			PreparedStatement stmt = testCon.prepareStatement(sql);
			stmt.setInt(1, tsid);
			ResultSet rs = stmt.executeQuery();
			if (rs.next()) {
				prodId = rs.getInt("product");
			}
		} catch (Exception e) {
			System.err.println(Utilities.getNow());
			e.printStackTrace();
		}
		return prodId;
	}

	public JSONArray getTCRBetweenRanges(String To, String From) {
		String sql = "SELECT DATE(Executed_Date) AS Date, "
				+ "SUM(CASE WHEN Status = 'PASS' THEN 1 ELSE 0 END) AS PassCount, "
				+ "SUM(CASE WHEN Status = 'FAIL' THEN 1 ELSE 0 END) AS FailCount " + "FROM cta.test_case_results "
				+ "WHERE Executed_Date BETWEEN ? AND ? " + "GROUP BY DATE(Executed_Date)";
		JSONArray json = new JSONArray();
		try {
			PreparedStatement stmt = testCon.prepareStatement(sql);
			stmt.setString(1, From);
			stmt.setString(2, To);
			ResultSet rs = stmt.executeQuery();
			while (rs.next()) {
				JSONObject jsonObj = new JSONObject();
				jsonObj.put("Date", (String) rs.getString("Date"));
				jsonObj.put("PassCount", (int) rs.getInt("PassCount"));
				jsonObj.put("FailCount", (int) rs.getInt("FailCount"));
				json.add(jsonObj);
			}
		} catch (Exception e) {
			System.err.println(Utilities.getNow());
			e.printStackTrace();
		}
		return json;
	}

	public void deleteTestCase(int tcid) {
		String sql = "UPDATE cta.test_case SET Status = 1 WHERE idtest_case= " + tcid;
		String del_sql = "DELETE FROM cta.test_suite_case_map WHERE Object_Id = " + tcid + " AND Object_Type = 'case'";
		try {
			Statement stmt = testCon.createStatement();
			stmt.executeUpdate(sql);
			stmt.executeUpdate(del_sql);

			String uname = (String) userDetails.get("uname");
			JSONObject latestTestSteps = this.getLatestRecording(uname);
			Utilities.cacheLatestRecording(uname, latestTestSteps);
		} catch (Exception e) {
			System.err.println(Utilities.getNow());
			e.printStackTrace();
		}
	}

	public String getTestSuiteName(int id) {
		String str = "SELECT Test_Suite FROM cta.test_suite WHERE idtest_suite = " + id;
		String tsName = "";
		try {
			Statement stmt = testCon.createStatement();
			ResultSet rs = stmt.executeQuery(str);
			if (rs.next()) {
				tsName = rs.getString("Test_Suite");
			}
		} catch (Exception e) {
			System.err.println(Utilities.getNow());
			e.printStackTrace();
		}
		return tsName;
	}

	public JSONObject addBranding(int cid, String primaryColor, String secondaryColor, String tertiaryColor,
			String brandingLogo) {
		String sql = "INSERT INTO cta.branding (idCompany, primaryColor, secondaryColor, tertiaryColor, brandLogo) VALUES "
				+ "(?,?,?,?,?)";
		String sql_update = "UPDATE cta.branding SET primaryColor=?, secondaryColor=?, tertiaryColor=?, brandLogo=? WHERE idCompany=?";
		String sql_wb = "UPDATE cta.branding SET primaryColor=?, secondaryColor=?, tertiaryColor=? WHERE idCompany=?";
		String sqlCheck = "SELECT idCompany FROM cta.branding WHERE idCompany= " + cid;
		JSONObject jsonObj = new JSONObject();
		try {
			Statement stmt_check = testCon.createStatement();
			ResultSet rs = stmt_check.executeQuery(sqlCheck);
			if (rs.next()) {
				if (brandingLogo == null) {
					PreparedStatement stmt = testCon.prepareStatement(sql_wb);
					stmt.setString(1, primaryColor);
					stmt.setString(2, secondaryColor);
					stmt.setString(3, tertiaryColor);
					stmt.setInt(4, cid);
					stmt.executeUpdate();
				} else {
					PreparedStatement stmt = testCon.prepareStatement(sql_update);
					stmt.setString(1, primaryColor);
					stmt.setString(2, secondaryColor);
					stmt.setString(3, tertiaryColor);
					stmt.setString(4, brandingLogo);
					stmt.setInt(5, cid);
					stmt.executeUpdate();
				}
			} else {
				PreparedStatement stmt = testCon.prepareStatement(sql);
				stmt.setInt(1, cid);
				stmt.setString(2, primaryColor);
				stmt.setString(3, secondaryColor);
				stmt.setString(4, tertiaryColor);
				stmt.setString(5, brandingLogo);
				stmt.executeUpdate();
			}
			jsonObj = getBranding(cid, randomkey);
		} catch (Exception e) {
			System.err.println(Utilities.getNow());
			e.printStackTrace();
		}
		return jsonObj;
	}

	public JSONObject getBranding(int cid, String randomkey) {
		String sql = "SELECT * FROM cta.branding WHERE idCompany = " + cid;
		String sql2 = "SELECT companyurl FROM cta.company WHERE idCompany = " + cid;
		JSONObject jsonObj = new JSONObject();
		try {
			Statement stmt_check = testCon.createStatement();
			ResultSet rs = stmt_check.executeQuery(sql);
			Statement cu = testCon.createStatement();
			ResultSet rsu = cu.executeQuery(sql2);
			if (rs.next()) {
				String primaryColor = rs.getString("primaryColor");
				String secondaryColor = rs.getString("secondaryColor");
				String tertiaryColor = rs.getString("tertiaryColor");
				String brandLogo = rs.getString("brandLogo");
				jsonObj.put("idCompany", cid);
				jsonObj.put("primaryColor", primaryColor);
				jsonObj.put("secondaryColor", secondaryColor);
				jsonObj.put("tertiaryColor", tertiaryColor);
				rsu.next();
				jsonObj.put("companyurl", rsu.getString("companyurl"));
				String Name = Utilities.getBrandingDir(cid, brandLogo);
				String url = AppProperties.fileurl + "?action=downloadFile&companyid=" + cid + "&token=" + randomkey
						+ "&fileName=" + brandLogo;
				jsonObj.put("brandLogo", url);
			} else {
				jsonObj.put("idCompany", cid);
				jsonObj.put("primaryColor", AppProperties.primaryColor);
				jsonObj.put("secondaryColor", AppProperties.secondaryColor);
				jsonObj.put("tertiaryColor", AppProperties.tertiaryColor);
				String Name = Utilities.getBrandingDir(cid, AppProperties.brandLogo);
				String url = AppProperties.fileurl + "?action=downloadFile&companyid=" + cid + "&token=" + randomkey
						+ "&fileName=" + AppProperties.brandLogo;
				jsonObj.put("brandLogo", url);
				jsonObj.put("companyurl", "");
			}
		} catch (Exception e) {
			System.err.println(Utilities.getNow());
			e.printStackTrace();
		}
		return jsonObj;
	}

	public JSONObject suiteScheduler(int sid, String dateStr, String tim, String frequency, String subFreq) {
		String sql = "INSERT INTO cta.scheduler (suiteid, frequency, status, subfrequency, companyid,"
				+ " scheduledate, scheduletime, scheduleby) VALUES " + "(?,?,?,?,?,?,?,?)";
		String sql_update = "UPDATE cta.scheduler SET scheduledate=?, scheduletime=?, frequency=?, "
				+ "subfrequency=?, scheduleby=?, status = 0 WHERE suiteid=?";
		String sqlCheck = "SELECT suiteid FROM cta.scheduler WHERE suiteid= " + sid;
		JSONObject jsonObj = new JSONObject();

		Date date = null;
		Time time = null;
		String timestr = "";

		if (frequency != null) {
			if (frequency.equalsIgnoreCase("other")) {
				timestr = dateStr + " " + tim;
				Calendar cal = Utilities.getCalender(timestr, "yyyy-MM-dd HH:mm:ss.S");
				date = Utilities.getDate(cal);
				time = Utilities.getTime(cal);
			} else if (frequency.equalsIgnoreCase("daily")) {
				Calendar cal = Utilities.getCalender(tim, "HH:mm:ss.S");
				time = Utilities.getTime(cal);
			} else if (frequency.equalsIgnoreCase("weekly")) {
				Calendar cal = Utilities.getCalender(tim, "HH:mm:ss.S");
				time = Utilities.getTime(cal);
			}
		}

		if (frequency != null && !frequency.equals("")) {
			if (frequency.equals("daily")) {
				subFreq = null;
				date = null;
			} else if (frequency.equals("weekly")) {
				date = null;
			} else if (frequency.equals("undefined")) {
				subFreq = null;
				frequency = null;
			}
		} else {
			subFreq = null;
			frequency = null;
		}
		try {
			Statement stmt_check = testCon.createStatement();
			ResultSet rs = stmt_check.executeQuery(sqlCheck);
			if (rs.next()) {
				PreparedStatement stmt = testCon.prepareStatement(sql_update);
				stmt.setDate(1, date);
				stmt.setTime(2, time);
				stmt.setString(3, frequency);
				stmt.setString(4, subFreq);
				stmt.setString(5, (String) userDetails.get("uname"));
				stmt.setInt(6, sid);

				stmt.executeUpdate();
			} else {
				PreparedStatement stmt = testCon.prepareStatement(sql);
				stmt.setInt(1, sid);
				stmt.setString(2, frequency);
				stmt.setInt(3, 0);
				stmt.setString(4, subFreq);
				stmt.setInt(5, (int) userDetails.get("companyid"));
				stmt.setDate(6, date);
				stmt.setTime(7, time);
				stmt.setString(8, (String) userDetails.get("uname"));
				stmt.executeUpdate();
			}
			jsonObj = getScheduler(sid);
		} catch (Exception e) {
			System.err.println(Utilities.getNow());
			e.printStackTrace();
		}
		return jsonObj;
	}

	public JSONObject getScheduler(int sid) {
		String sql = "SELECT * FROM cta.scheduler WHERE status = 0 AND suiteid = " + sid;
		JSONObject jsonObj = new JSONObject();
		try {
			Statement stmt = testCon.createStatement();
			ResultSet rs = stmt.executeQuery(sql);
			if (rs.next()) {
				jsonObj = getSchedulerAsJSON(rs);
			}
		} catch (Exception e) {
			System.err.println(Utilities.getNow());
			e.printStackTrace();
		}
		return jsonObj;
	}

	public static JSONObject getSchedulerAsJSON(ResultSet rs) {
		JSONObject jsonObj = new JSONObject();
		try {
			int suiteid = rs.getInt("suiteid");
			String frequency = rs.getString("frequency");
			int status = rs.getInt("status");
			String subfrequency = rs.getString("subfrequency");
			jsonObj.put("idscheduler", rs.getInt("idscheduler"));
			jsonObj.put("suiteid", suiteid);
			jsonObj.put("frequency", frequency);
			jsonObj.put("status", status);
			jsonObj.put("subfreq", subfrequency);
			jsonObj.put("companyid", rs.getInt("companyid"));
			if (rs.getDate("scheduledate") != null) {
				jsonObj.put("scheduledate", rs.getDate("scheduledate").toString());
			} else {
				jsonObj.put("scheduledate", rs.getDate("scheduledate"));
			}

			if (rs.getTime("scheduletime") != null) {
				jsonObj.put("scheduletime", rs.getTime("scheduletime").toString());
			} else {
				jsonObj.put("scheduletime", rs.getTime("scheduletime"));
			}
			jsonObj.put("scheduleby", rs.getString("scheduleby"));
		} catch (Exception e) {
			System.err.println(Utilities.getNow());
			e.printStackTrace();
		}

		return jsonObj;
	}

	public void deleteScheduler(int sid) {
		String sql = "UPDATE cta.scheduler SET status = 1 WHERE suiteid = " + sid;
		JSONObject jsonObj = new JSONObject();
		try {
			Statement stmt = testCon.createStatement();
			stmt.executeUpdate(sql);
		} catch (Exception e) {
			System.err.println(Utilities.getNow());
			e.printStackTrace();
		}
	}

	public JSONArray getUploadfileByCompany(int cid) {
		JSONArray json = new JSONArray();
		String sql = "SELECT t4.Test_Case AS TestcaseName, t4.idtest_case As idtest_case, t5.idtest_step As id_test_step, t5.Step_Number As IdTeststep, t5.Page_Name AS TeststepName, "
				+ "t5.Page_Description AS TeststepDesc, t5.filename AS FileName, t5.fileField AS FileField "
				+ "From cta.test_step t5 " + "JOIN cta.test_case t4 on t5.Test_Case_Id = t4.idtest_case "
				+ "JOIN cta.modules t3 ON t4.Module = t3.idmodules "
				+ "JOIN cta.products t2 ON t3.product = t2.idproducts "
				+ "JOIN cta.company t1 ON t2.company = t1.idcompany " + "WHERE t1.idcompany = " + cid
				+ " AND t5.Keyword != 'Validation' AND t5.filename != ''" + " AND t5.status= 0";
		try {
			Statement stmt = testCon.createStatement();
			ResultSet rs = stmt.executeQuery(sql);
			while (rs.next()) {
				JSONObject jsonStr = new JSONObject();
				String TestcaseName = rs.getString("TestcaseName");
				int IdTeststep = rs.getInt("IdTeststep");
				String TeststepName = rs.getString("TeststepName");
				String TeststepDesc = rs.getString("TeststepDesc");
				String FileName = rs.getString("FileName");
				String FileField = rs.getString("FileField");
				int idTestCase = rs.getInt("idtest_case");
				int id_test_step = rs.getInt("id_test_step");
				jsonStr.put("id_test_step", id_test_step);
				jsonStr.put("idTestCase", idTestCase);
				jsonStr.put("TestcaseName", TestcaseName);
				jsonStr.put("IdTeststep", IdTeststep);
				jsonStr.put("TeststepName", TeststepName);
				jsonStr.put("TeststepDesc", TeststepDesc);
				jsonStr.put("FileName", FileName);
				jsonStr.put("FileField", FileField);
				jsonStr.put("status", "None");
				json.add(jsonStr);
			}
		} catch (Exception e) {
			System.err.println(Utilities.getNow());
			e.printStackTrace();
		}
		return json;
	}

	public JSONArray getUploadfileByProduct(int cid, int prodid) {
		JSONArray json = new JSONArray();
		String sql = "SELECT t4.Test_Case AS TestcaseName, t4.idtest_case As idtest_case, t5.idtest_step As id_test_step, t5.Step_Number As IdTeststep, t5.Page_Name AS TeststepName, "
				+ "t5.Page_Description AS TeststepDesc, t5.filename AS FileName, t5.fileField AS FileField "
				+ "From cta.test_step t5 " + "JOIN cta.test_case t4 on t5.Test_Case_Id = t4.idtest_case "
				+ "JOIN cta.modules t3 ON t4.Module = t3.idmodules "
				+ "JOIN cta.products t2 ON t3.product = t2.idproducts "
				+ "JOIN cta.company t1 ON t2.company = t1.idcompany "
				+ "WHERE t1.idcompany = ? AND t2.idproducts=? AND t5.Keyword != 'Validation' AND t5.filename != ''"
				+ " AND t5.status= 0";
		try {
			PreparedStatement stmt = testCon.prepareStatement(sql);
			stmt.setInt(1, cid);
			stmt.setInt(2, prodid);
			ResultSet rs = stmt.executeQuery();
			while (rs.next()) {
				JSONObject jsonStr = new JSONObject();
				String TestcaseName = rs.getString("TestcaseName");
				int IdTeststep = rs.getInt("IdTeststep");
				String TeststepName = rs.getString("TeststepName");
				String TeststepDesc = rs.getString("TeststepDesc");
				String FileName = rs.getString("FileName");
				String FileField = rs.getString("FileField");
				int idTestCase = rs.getInt("idtest_case");
				int id_test_step = rs.getInt("id_test_step");
				jsonStr.put("id_test_step", id_test_step);
				jsonStr.put("idTestCase", idTestCase);
				jsonStr.put("TestcaseName", TestcaseName);
				jsonStr.put("IdTeststep", IdTeststep);
				jsonStr.put("TeststepName", TeststepName);
				jsonStr.put("TeststepDesc", TeststepDesc);
				jsonStr.put("FileName", FileName);
				jsonStr.put("FileField", FileField);
				jsonStr.put("status", "None");
				json.add(jsonStr);
			}
		} catch (Exception e) {
			System.err.println(Utilities.getNow());
			e.printStackTrace();
		}
		return json;
	}

	public JSONArray getUploadfileByModule(int cid, int prodid, int modid) {
		JSONArray json = new JSONArray();
		String sql = "SELECT t4.Test_Case AS TestcaseName, t4.idtest_case As idtest_case, t5.idtest_step As id_test_step, t5.Step_Number As IdTeststep, t5.Page_Name AS TeststepName, "
				+ "t5.Page_Description AS TeststepDesc, t5.filename AS FileName, t5.fileField AS FileField "
				+ "From cta.test_step t5 " + "JOIN cta.test_case t4 on t5.Test_Case_Id = t4.idtest_case "
				+ "JOIN cta.modules t3 ON t4.Module = t3.idmodules "
				+ "JOIN cta.products t2 ON t3.product = t2.idproducts "
				+ "JOIN cta.company t1 ON t2.company = t1.idcompany "
				+ "WHERE t1.idcompany = ? AND t2.idproducts=? AND t3.idmodules=? AND t5.Keyword != 'Validation' AND t5.filename != ''"
				+ " AND t5.status= 0";
		try {
			PreparedStatement stmt = testCon.prepareStatement(sql);
			stmt.setInt(1, cid);
			stmt.setInt(2, prodid);
			stmt.setInt(3, modid);
			ResultSet rs = stmt.executeQuery();
			while (rs.next()) {
				JSONObject jsonStr = new JSONObject();
				String TestcaseName = rs.getString("TestcaseName");
				int IdTeststep = rs.getInt("IdTeststep");
				String TeststepName = rs.getString("TeststepName");
				String TeststepDesc = rs.getString("TeststepDesc");
				String FileName = rs.getString("FileName");
				String FileField = rs.getString("FileField");
				int idTestCase = rs.getInt("idtest_case");
				int id_test_step = rs.getInt("id_test_step");
				jsonStr.put("id_test_step", id_test_step);
				jsonStr.put("idTestCase", idTestCase);
				jsonStr.put("TestcaseName", TestcaseName);
				jsonStr.put("IdTeststep", IdTeststep);
				jsonStr.put("TeststepName", TeststepName);
				jsonStr.put("TeststepDesc", TeststepDesc);
				jsonStr.put("FileName", FileName);
				jsonStr.put("FileField", FileField);
				jsonStr.put("status", "None");
				json.add(jsonStr);
			}
		} catch (Exception e) {
			System.err.println(Utilities.getNow());
			e.printStackTrace();
		}
		return json;
	}

	public JSONArray getUploadfileByTestcase(int cid, int prodid, int modid, int tcid) {
		JSONArray json = new JSONArray();
		String sql = "SELECT t4.Test_Case AS TestcaseName, t4.idtest_case As idtest_case, t5.idtest_step As id_test_step, t5.Step_Number As IdTeststep, t5.Page_Name AS TeststepName, "
				+ "t5.Page_Description AS TeststepDesc, t5.filename AS FileName, t5.fileField AS FileField "
				+ "From cta.test_step t5 " + "JOIN cta.test_case t4 on t5.Test_Case_Id = t4.idtest_case "
				+ "JOIN cta.modules t3 ON t4.Module = t3.idmodules "
				+ "JOIN cta.products t2 ON t3.product = t2.idproducts "
				+ "JOIN cta.company t1 ON t2.company = t1.idcompany "
				+ "WHERE t1.idcompany = ? AND t2.idproducts=? AND t3.idmodules=? AND t4.idtest_case=? AND t5.Keyword != 'Validation' AND t5.filename != '' "
				+ "AND t5.status= 0";
		try {
			PreparedStatement stmt = testCon.prepareStatement(sql);
			stmt.setInt(1, cid);
			stmt.setInt(2, prodid);
			stmt.setInt(3, modid);
			stmt.setInt(4, tcid);
			ResultSet rs = stmt.executeQuery();
			while (rs.next()) {
				JSONObject jsonStr = new JSONObject();
				String TestcaseName = rs.getString("TestcaseName");
				int IdTeststep = rs.getInt("IdTeststep");
				String TeststepName = rs.getString("TeststepName");
				String TeststepDesc = rs.getString("TeststepDesc");
				String FileName = rs.getString("FileName");
				String FileField = rs.getString("FileField");
				int idTestCase = rs.getInt("idtest_case");
				int id_test_step = rs.getInt("id_test_step");
				jsonStr.put("id_test_step", id_test_step);
				jsonStr.put("idTestCase", idTestCase);
				jsonStr.put("TestcaseName", TestcaseName);
				jsonStr.put("IdTeststep", IdTeststep);
				jsonStr.put("TeststepName", TeststepName);
				jsonStr.put("TeststepDesc", TeststepDesc);
				jsonStr.put("FileName", FileName);
				jsonStr.put("FileField", FileField);
				jsonStr.put("status", "None");
				json.add(jsonStr);
			}
		} catch (Exception e) {
			System.err.println(Utilities.getNow());
			e.printStackTrace();
		}
		return json;
	}

	public JSONArray getUploadfileByTestsuite(int cid, int prodid, int tsid) {
		JSONArray json = new JSONArray();
		String sql = "SELECT t5.Test_Case AS TestcaseName, t5.idtest_case As idtest_case, t6.idtest_step As id_test_step, t6.Step_Number As IdTeststep, t6.Page_Name AS TeststepName, "
				+ "t6.Page_Description AS TeststepDesc, t6.filename AS FileName, t6.fileField AS FileField "
				+ "From cta.test_step t6 " + "JOIN cta.test_case t5 on t6.Test_Case_Id = t5.idtest_case "
				+ "JOIN cta.test_suite_case_map t4 on t5.idtest_case = t4.Object_Id "
				+ "JOIN cta.test_suite t3 ON t4.Test_Suite_Id = t3.idtest_suite JOIN cta.products t2 ON t3.productid = t2.idproducts "
				+ "JOIN cta.company t1 ON t2.company = t1.idcompany WHERE t1.idcompany = ? AND t2.idproducts=? AND t3.idtest_suite=? AND t4.Object_Type='case' AND t6.Keyword != 'Validation' AND t6.filename != '' "
				+ "AND t6.status= 0";
		try {
			PreparedStatement stmt = testCon.prepareStatement(sql);
			stmt.setInt(1, cid);
			stmt.setInt(2, prodid);
			stmt.setInt(3, tsid);
			ResultSet rs = stmt.executeQuery();
			while (rs.next()) {
				JSONObject jsonStr = new JSONObject();
				String TestcaseName = rs.getString("TestcaseName");
				int IdTeststep = rs.getInt("IdTeststep");
				String TeststepName = rs.getString("TeststepName");
				String TeststepDesc = rs.getString("TeststepDesc");
				String FileName = rs.getString("FileName");
				String FileField = rs.getString("FileField");
				int idTestCase = rs.getInt("idtest_case");
				int id_test_step = rs.getInt("id_test_step");
				jsonStr.put("id_test_step", id_test_step);
				jsonStr.put("idTestCase", idTestCase);
				jsonStr.put("TestcaseName", TestcaseName);
				jsonStr.put("IdTeststep", IdTeststep);
				jsonStr.put("TeststepName", TeststepName);
				jsonStr.put("TeststepDesc", TeststepDesc);
				jsonStr.put("FileName", FileName);
				jsonStr.put("FileField", FileField);
				jsonStr.put("status", "None");
				json.add(jsonStr);
			}
		} catch (Exception e) {
			System.err.println(Utilities.getNow());
			e.printStackTrace();
		}
		return json;
	}

	public void uploadFile(int cid, String fName) {
		String uname = (String) userDetails.get("uname");
		JSONArray jsonArray = getUploadfileByCompany(cid);
		Map<String, List<JSONObject>> resultMap = new HashMap<>();

		try {
			for (Object obj : jsonArray) {
				JSONObject jsonObject = (JSONObject) obj;
				String fileName = (String) jsonObject.get("FileName");
				if (!resultMap.containsKey(fileName)) {
					resultMap.put(fileName, new ArrayList<>());
				}
				resultMap.get(fileName).add(jsonObject);
			}

			Unzip uz = new Unzip();
			uz.unzip(resultMap, cid, fName, uname, this);
		} catch (Exception e) {
			System.err.println(Utilities.getNow());
			e.printStackTrace();
		}
	}

	public void uploadTestDataFile(int cid, int prodid, String fName) {
		String zipFilePath = Utilities.getTestDataZipPath(cid, prodid, fName);
		String extractDirectory = Utilities.getTestDataPath(cid, prodid);
		Utilities.unZipFile(zipFilePath, extractDirectory);
	}

	public JSONArray getTestDataFiles(int cid) {
		String directoryPath = Utilities.getScriptFolder(cid);
		File directory = new File(directoryPath);
		JSONArray json = new JSONArray();
		if (directory.exists() && directory.isDirectory()) {
			File[] files = directory.listFiles();

			if (files != null) {
				for (File file : files) {
					if (file.isFile()) {
						JSONObject jsonObj = new JSONObject();
						jsonObj.put("FileName", file.getName());
						jsonObj.put("FileSize", file.length());
						SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yy HH:mm:ss");
						String formattedDate = sdf.format(new Date(file.lastModified()));
						jsonObj.put("LastModified", formattedDate);
						json.add(jsonObj);
					}
				}
			}
		} else {
			System.err.println("The specified directory does not exist or is not a directory.");
		}
		return json;
	}

	public void deleteFileforProduct(JSONArray json, int cid) {
		try {
			for (Object obj : json) {
				JSONObject jsonObj = (JSONObject) obj;
				String filename = (String) jsonObj.get("FileName");
				String directoryPath = Utilities.getScriptFolder(cid);

				String path = directoryPath + "\\" + filename;
				File fileToDelete = new File(path);

				if (fileToDelete.exists() && fileToDelete.isFile()) {
					boolean isDeleted = fileToDelete.delete();
					if (isDeleted) {
						System.err.println("File deleted successfully.");
					} else {
						System.err.println("Failed to delete the file.");
					}
				}
			}
		} catch (Exception e) {
			System.err.println(Utilities.getNow());
			e.printStackTrace();
		}
	}

	public JSONObject getTestDataFileNames(int cid, int prodid) {
		String directoryPath = Utilities.getTestDataPath(cid, prodid);
		File directory = new File(directoryPath);
		JSONObject json = new JSONObject();
		if (directory.exists() && directory.isDirectory()) {
			File[] files = directory.listFiles();

			if (files != null) {
				JSONArray jsonArr = new JSONArray();
				for (File file : files) {
					if (file.isFile()) {
						jsonArr.add(file.getName());
					}
				}
				json.put("list", jsonArr);
			}
		} else {
			System.err.println("The specified directory does not exist or is not a directory.");
		}
		return json;
	}

	public JSONObject updateSyncToggle(int toggle, int suiteid) {
		String str = "UPDATE cta.test_suite SET synchedscenarios=? where idtest_suite=?";
		JSONObject json = new JSONObject();
		try {
			PreparedStatement stmt = testCon.prepareStatement(str);
			stmt.setInt(1, toggle);
			stmt.setInt(2, suiteid);
			stmt.executeUpdate();
			json.put("synctoggle", toggle);
		} catch (Exception e) {
			System.err.println(Utilities.getNow());
			e.printStackTrace();
		}
		return json;
	}

	public JSONObject updateHeadless(int toggle, int suiteid) {
		String str = "UPDATE cta.test_suite SET headless=? where idtest_suite=?";
		JSONObject json = new JSONObject();
		try {
			PreparedStatement stmt = testCon.prepareStatement(str);
			stmt.setInt(1, toggle);
			stmt.setInt(2, suiteid);
			stmt.executeUpdate();
			json.put("headless", toggle);
		} catch (Exception e) {
			System.err.println(Utilities.getNow());
			e.printStackTrace();
		}
		return json;
	}

	public int getPageid(int pageno, int testcase, int prodid) {
		String pageid_db = "SELECT idpage FROM cta.testcase_page WHERE testcaseid=? AND pageno=? AND prodid=?";
		int pgid = -1;
		try {
			PreparedStatement stmt = testCon.prepareStatement(pageid_db);
			stmt.setInt(1, testcase);
			stmt.setInt(2, pageno);
			stmt.setInt(3, prodid);
			ResultSet rs = stmt.executeQuery();
			if (rs.next()) {
				pgid = rs.getInt(1);
			}
		} catch (Exception e) {
			System.err.println(Utilities.getNow());
			e.printStackTrace();
		}
		return pgid;
	}

	public JSONArray getPageElement(int pgid, int testcase, int prodid) {
//		String page_ele = "SELECT pe.elementid, pe.elementname, pe.recordedxpath FROM cta.page_element_testcase pet "
//				+ "JOIN cta.page_elements pe ON pe.elementid = pet.elementid WHERE pet.idpage=? AND pet.recorded=1 ORDER BY pet.elementid";
		String page_ele = "SELECT distinct(pe.elementid), pe.*, tp.prodid FROM cta.page_element_testcase pet "
				+ "JOIN cta.page_elements pe ON pe.elementid = pet.elementid "
				+ "JOIN cta.testcase_page tp ON tp.testcaseid = pet.testcaseid WHERE pet.idpage=? AND pe.coveragecount>0 AND tp.prodid=? ORDER BY pe.elementid";
		JSONArray json = new JSONArray();
		try {
			PreparedStatement stmt = testCon.prepareStatement(page_ele);
			stmt.setInt(1, pgid);
			stmt.setInt(2, prodid);
			ResultSet rs = stmt.executeQuery();
			while (rs.next()) {
				JSONObject jsonObj = new JSONObject();
				jsonObj.put("pageId", pgid);
				jsonObj.put("productId", prodid);
				jsonObj.put("testcaseId", testcase);
				jsonObj.put("elementId", (int) rs.getInt("elementid"));
				jsonObj.put("elementName", rs.getString("elementname"));
				jsonObj.put("xpath", rs.getString("recordedxpath"));
				json.add(jsonObj);
			}

		} catch (Exception e) {
			System.err.println(Utilities.getNow());
			e.printStackTrace();
		}
		return json;
	}

	public JSONArray getPageElementOnProd(int pgid, int testcase, int prodid) {
		String page_ele = "SELECT distinct(pe.elementid), pe.*, tp.prodid FROM cta.page_element_testcase pet JOIN cta.page_elements pe ON pe.elementid = pet.elementid "
				+ "JOIN cta.testcase_page tp ON tp.testcaseid = pet.testcaseid WHERE pe.coveragecount>0 AND tp.prodid=? ORDER BY pe.elementid;";
		JSONArray json = new JSONArray();
		try {
			PreparedStatement stmt = testCon.prepareStatement(page_ele);
			stmt.setInt(1, prodid);
			ResultSet rs = stmt.executeQuery();
			while (rs.next()) {
				JSONObject jsonObj = new JSONObject();
				jsonObj.put("pageId", pgid);
				jsonObj.put("productId", prodid);
				jsonObj.put("testcaseId", testcase);
				jsonObj.put("elementId", (int) rs.getInt("elementid"));
				jsonObj.put("elementName", rs.getString("elementname"));
				jsonObj.put("xpath", rs.getString("recordedxpath"));
				json.add(jsonObj);
			}
			rs.close();
			stmt.close();
		} catch (Exception e) {
			System.err.println(Utilities.getNow());
			e.printStackTrace();
		}
		return json;
	}

	public void updateCoverageSSPath(String filename, int pageid) {
		String update = "UPDATE cta.pages SET screenshotpath=? WHERE pageid=?";
		try {
			PreparedStatement stmt = testCon.prepareStatement(update);
			stmt.setString(1, filename);
			stmt.setInt(2, pageid);
			stmt.executeUpdate();

			stmt.close();
		} catch (Exception e) {
			System.err.println(Utilities.getNow());
			e.printStackTrace();
		}
	}

	public JSONArray getOverdueSSJobs() {
		JSONArray jsonArray = new JSONArray();
		String sql = "SELECT * FROM cta.test_case WHERE (laststeprecordedtime < DATE_SUB(NOW(), INTERVAL 5 MINUTE) AND ss_status IS NULL "
				+ "AND laststeprecordedtime IS NOT NULL AND Status=0)";

		try {
			Statement stmt = testCon.createStatement();
			ResultSet rs = stmt.executeQuery(sql);
			while (rs.next()) {
				JSONObject testCase = new JSONObject();
				testCase.put("idtest_case", rs.getInt("idtest_case"));
				testCase.put("Test_Case", rs.getString("Test_Case"));
				int module = rs.getInt("Module");
				int companyid = getCompanyFromModule(module);
				String token = getLicenceToken(companyid);
				testCase.put("laststep_recorded_time", rs.getTimestamp("laststeprecordedtime"));
				testCase.put("randomkey", token);
				testCase.put("compid", companyid);
				jsonArray.add(testCase);
			}
			rs.close();
			stmt.close();
		} catch (SQLException e) {
			System.err.println(Utilities.getNow());
			e.printStackTrace();
		}

		return jsonArray;
	}

	public void updateTestCaseSSStatus(int tcid, String Status) {
		String sql = "UPDATE cta.test_case set ss_status = ? where idtest_case = ?";
		try {
			if (Status.equals("NULL")) {
				sql = "UPDATE cta.test_case set ss_status = NULL where idtest_case = ?";
				PreparedStatement stmt = testCon.prepareStatement(sql);
				stmt.setInt(1, tcid);
				stmt.executeUpdate();
				stmt.close();
			} else {
				PreparedStatement stmt = testCon.prepareStatement(sql);
				stmt.setString(1, Status);
				stmt.setInt(2, tcid);
				stmt.executeUpdate();
				stmt.close();
			}
		} catch (Exception e) {
			System.err.println(Utilities.getNow());
			e.printStackTrace();
		}
	}

	public void updateTestCaseAnalyseStatus(int tcid, int as) {
		String sql = "UPDATE cta.test_case set beenanalysed = ?, analysisdate = ? " + "where idtest_case = ?";
		try {
			PreparedStatement stmt = testCon.prepareStatement(sql);
			stmt.setInt(1, as);
			stmt.setTimestamp(2, Utilities.getCurrentTimestamp());
			stmt.setInt(3, tcid);
			stmt.executeUpdate();
			stmt.close();
		} catch (Exception e) {
			System.err.println(Utilities.getNow());
			e.printStackTrace();
		}
	}

	public void updateTestCaseBaselineStatus(int tcid, int bl) {
		String sql = "UPDATE cta.test_case set imagesbaselined = ? , baselinedate = ? " + "where idtest_case = ?";
		try {
			PreparedStatement stmt = testCon.prepareStatement(sql);
			stmt.setInt(1, bl);
			stmt.setTimestamp(2, Utilities.getCurrentTimestamp());
			stmt.setInt(3, tcid);
			stmt.executeUpdate();
			stmt.close();
		} catch (Exception e) {
			System.err.println(Utilities.getNow());
			e.printStackTrace();
		}
	}

	public void uploadScriptZipFile(int cid, String filename) {
		String zipFilePath = Utilities.getScriptZipFilePath(cid, filename); // Replace with your zip file path
		String destinationPath = Utilities.getScriptFolder(cid); // Replace with your destination folder path
		String backupFolderPath = Utilities.createScriptBackupPath(cid); // Replace with your backup folder path

		try {
			Files.createDirectories(Paths.get(destinationPath));
			Files.createDirectories(Paths.get(backupFolderPath));

			try (ZipInputStream zipInputStream = new ZipInputStream(new FileInputStream(zipFilePath))) {
				ZipEntry entry;

				while ((entry = zipInputStream.getNextEntry()) != null) {
					String entryName = entry.getName();
					Path filePath = Paths.get(destinationPath, entryName);

					if (Files.exists(filePath)) {
						Path backupFilePath = Paths.get(backupFolderPath, entryName);
						Files.move(filePath, backupFilePath, StandardCopyOption.REPLACE_EXISTING);
					}

					try (FileOutputStream outputStream = new FileOutputStream(filePath.toFile())) {
						byte[] buffer = new byte[1024];
						int length;
						while ((length = zipInputStream.read(buffer)) > 0) {
							outputStream.write(buffer, 0, length);
						}
					}
				}
			}
		} catch (IOException e) {
			System.err.println(Utilities.getNow());
			e.printStackTrace();
		}
	}

	public JSONArray getScriptFunctionNames(int cid, String directoryPath) {
		File directory = new File(directoryPath);
		JSONArray json = new JSONArray();
		if (directory.exists() && directory.isDirectory()) {
			File[] files = directory.listFiles();

			if (!directoryPath.contains("Scripts")) {
				// Sort files by last modified time in reverse chronological order
				Arrays.sort(files, new Comparator<File>() {
					@Override
					public int compare(File file1, File file2) {
						long diff = file2.lastModified() - file1.lastModified();
						return Long.signum(diff);
					}
				});
			}

			if (files != null) {
				for (File file : files) {
					if (file.isFile()) {
						JSONObject jsonObj = new JSONObject();
						jsonObj.put("FileName", file.getName());
						jsonObj.put("FileSize", file.length());
						SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yy HH:mm:ss");
						String formattedDate = sdf.format(new Date(file.lastModified()));
						jsonObj.put("LastModified", formattedDate);
						if (directoryPath.contains("Scripts")) {
							String fileurl = AppProperties.fileurl + "?action=scriptFiles&token=" + randomkey
									+ "&companyid=" + cid + "&fname=" + file.getName();
							jsonObj.put("link", fileurl);
						} else {
							String fileurl = AppProperties.fileurl + "?action=codeFiles&token=" + randomkey
									+ "&companyid=" + cid + "&fname=" + file.getName();
							jsonObj.put("link", fileurl);
						}
						json.add(jsonObj);
					}
				}
			}
		} else {
			System.err.println("The specified directory does not exist or is not a directory.");
		}
		return json;
	}

	public JSONArray getTestCaseFromSuiteid(int suiteid) {
		String sql = "SELECT tc.* FROM cta.test_suite_case_map tscm JOIN cta.test_case tc on tc.idtest_case = tscm.Object_Id Where tscm.Object_Type='case' AND tscm.Test_Suite_Id="
				+ suiteid;
		JSONArray json = new JSONArray();
		try {
			Statement stmt = testCon.createStatement();
			ResultSet rs = stmt.executeQuery(sql);
			while (rs.next()) {
				JSONObject jsonObj = getTestCaseAsJSON(rs, false);
				json.add(jsonObj);
			}
			rs.close();
			stmt.close();
		} catch (Exception e) {
			System.err.println(Utilities.getNow());
			e.printStackTrace();
		}
		return json;
	}

	public boolean getBulkTestStatus(int cid) {
		boolean result = false;
		try {
			String sql = "SELECT bulktest FROM cta.company WHERE idcompany = ?";
			PreparedStatement stmt = testCon.prepareStatement(sql);
			stmt.setInt(1, cid);
			ResultSet rs = stmt.executeQuery();
			if (rs.next()) {
				String status = rs.getString(1);
				if (status.equals("false") || status.equals("")) {
					result = false;
				} else
					result = true;
			}
			rs.close();
			stmt.close();
		} catch (Exception e) {
			System.err.println(Utilities.getNow());
			e.printStackTrace();
		}
		return result;
	}

	public JSONArray getALLTestCaseFromCompanyid(int cid) {
		String sql = "SELECT tc.* FROM cta.test_case tc JOIN cta.modules m on tc.Module = m.idmodules "
				+ "JOIN cta.products p on m.product = p.idproducts JOIN cta.company c on p.company = c.idcompany "
				+ "Where tc.Status=0 AND c.idcompany=" + cid;
		JSONArray json = new JSONArray();
		try {
			Statement stmt = testCon.createStatement();
			ResultSet rs = stmt.executeQuery(sql);
			while (rs.next()) {
				JSONObject jsonObj = getTestCaseAsJSON(rs, false);
				json.add(jsonObj);
			}
			rs.close();
			stmt.close();
		} catch (Exception e) {
			System.err.println(Utilities.getNow());
			e.printStackTrace();
		}
		return json;
	}

	private JSONObject getTestCaseExecutionLogAsJSON(ResultSet rs) {
		JSONObject jsonRow = new JSONObject();
		try {
			jsonRow.put("message", rs.getString("message"));
		} catch (Exception e) {
			System.err.println(Utilities.getNow());
			e.printStackTrace();
		}
		return jsonRow;
	}

	public JSONArray getFromTestCaseExecutionLog(int tcrid) {
		String sql = "SELECT * FROM cta.testcaseexecutionlog WHERE Test_Case_Results_Id=" + tcrid;
		JSONArray json = new JSONArray();
		try {
			Statement stmt = testCon.createStatement();
			ResultSet rs = stmt.executeQuery(sql);
			while (rs.next()) {
				JSONObject jsonObj = getTestCaseExecutionLogAsJSON(rs);
				json.add(jsonObj);
			}
			rs.close();
			stmt.close();
		} catch (Exception e) {
			System.err.println(Utilities.getNow());
			e.printStackTrace();
		}
		return json;
	}

	public void updateTestStepIssues(int step_number, String issues, int tcrid) {

		String sql = "UPDATE cta.test_step_result SET issues = CONCAT(issues, ?) WHERE Step_Number = ? "
				+ "AND Test_Case_Results_Id = ?";

		try {
			PreparedStatement stmt = testCon.prepareStatement(sql);
			stmt.setString(1, issues);
			stmt.setInt(2, step_number);
			stmt.setInt(3, tcrid);
			stmt.executeUpdate();
			stmt.close();
		} catch (Exception e) {
			System.err.println(Utilities.getNow());
			e.printStackTrace();
		}

	}

	public List<Integer> getLatestTestCaseResultsId(int testCaseId) {

		String sql = "SELECT DISTINCT Test_Case_Results_Id FROM cta.testcaseexecutionlog WHERE testcaseid = "
				+ testCaseId + " ORDER BY Test_Case_Results_Id DESC LIMIT 2";

		List<Integer> result = new ArrayList<>();
		try {
			Statement stmt = testCon.createStatement();
			ResultSet rs = stmt.executeQuery(sql);
			while (rs.next()) {
				int tcrid = rs.getInt("Test_Case_Results_Id");
				result.add(tcrid);
			}
			rs.close();
			stmt.close();
		} catch (Exception e) {
			System.err.println(Utilities.getNow());
			e.printStackTrace();
		}
		return result;
	}

	public int getLatestPassedTestCaseResultsId(int tcid) {

		String sql = "SELECT * FROM cta.test_case_results WHERE Test_Case_Id = ? AND Status = ? "
				+ "ORDER BY idtest_case_results DESC LIMIT 1";
		int result = -1;
		try {
			PreparedStatement stmt = testCon.prepareStatement(sql);
			stmt.setInt(1, tcid);
			stmt.setString(2, "PASS");
			ResultSet rs = stmt.executeQuery();
			if (rs.next()) {
				result = (int) rs.getInt("idtest_case_results");
			}
			rs.close();
			stmt.close();
		} catch (Exception e) {
			System.err.println(Utilities.getNow());
			e.printStackTrace();
		}
		return result;
	}

	public int getSecondLatestPassedTestCaseResultsId(int tcid) {

		String sql = "SELECT * FROM cta.test_case_results WHERE Test_Case_Id = ? AND Status = ? "
				+ "ORDER BY idtest_case_results DESC LIMIT 1 OFFSET 1";
		int result = -1;
		try {
			PreparedStatement stmt = testCon.prepareStatement(sql);
			stmt.setInt(1, tcid);
			stmt.setString(2, "PASS");
			ResultSet rs = stmt.executeQuery();
			if (rs.next()) {
				result = (int) rs.getInt("idtest_case_results");
			}
			rs.close();
			stmt.close();
		} catch (Exception e) {
			System.err.println(Utilities.getNow());
			e.printStackTrace();
		}
		return result;
	}

	public int getLatestPassedTestStepResultsId(int tsid) {

		String sql = "SELECT * FROM cta.test_step_result WHERE Test_Step = ? AND Status = ? "
				+ "ORDER BY Test_Case_Results_Id DESC LIMIT 1";
		int result = -1;
		try {
			PreparedStatement stmt = testCon.prepareStatement(sql);
			stmt.setInt(1, tsid);
			stmt.setString(2, "PASS");
			ResultSet rs = stmt.executeQuery();
			if (rs.next()) {
				result = (int) rs.getInt("Test_Case_Results_Id");
			}
			rs.close();
			stmt.close();
		} catch (Exception e) {
			System.err.println(Utilities.getNow());
			e.printStackTrace();
		}
		return result;
	}

	public String getPassedOuterhtml(int ptcrid, int tsid) {

		String sql = "SELECT * FROM cta.test_step_result WHERE Test_Case_Results_Id = ? AND Test_Step = ? AND Status = ? "
				+ "order by idtest_step_result desc";
		String result = "";
		try {
			PreparedStatement stmt = testCon.prepareStatement(sql);
			stmt.setInt(1, ptcrid);
			stmt.setInt(2, tsid);
			stmt.setString(3, "PASS");
			ResultSet rs = stmt.executeQuery();
			if (rs.next()) {
				result = (String) rs.getString("outerhtml");
			}
			rs.close();
			stmt.close();
		} catch (Exception e) {
			System.err.println(Utilities.getNow());
			e.printStackTrace();
		}
		return result;
	}

	public void updateTestStepAttr(int tcrid, int tsid, String nonMatchingAttr) {

		String sql = "UPDATE cta.test_step_attr SET nonmatchingattributes = ?, tcrid = ?, arenonmatchesapproved = ?, "
				+ "approvedby = ?, approveddate = ? WHERE test_step = ?";
		try {
			PreparedStatement stmt = testCon.prepareStatement(sql);
			stmt.setString(1, nonMatchingAttr);
			stmt.setInt(2, tcrid);
			stmt.setInt(3, -1);
			stmt.setString(4, null);
			stmt.setDate(5, null);
			stmt.setInt(6, tsid);
			stmt.executeUpdate();
			stmt.close();
		} catch (Exception e) {
			System.err.println(Utilities.getNow());
			e.printStackTrace();
		}
	}

	public String getApprovedAttr(int tsid) {

		String sql = "SELECT * FROM cta.test_step_attr WHERE test_step = ? AND arenonmatchesapproved = ?";
		String result = "";
		try {
			PreparedStatement stmt = testCon.prepareStatement(sql);
			stmt.setInt(1, tsid);
			stmt.setInt(2, 1);
			ResultSet rs = stmt.executeQuery();
			if (rs.next()) {
				result = (String) rs.getString("nonmatchingattributes");
			}
			rs.close();
			stmt.close();
		} catch (Exception e) {
			System.err.println(Utilities.getNow());
			e.printStackTrace();
		}
		return result;
	}
	

public void updateXpath(int tsid, String xpath) {
	
	String sql = "UPDATE cta.test_step SET Object_Xpath = ? WHERE idtest_step = ?";
	try {
		PreparedStatement stmt = testCon.prepareStatement(sql); 
		stmt.setString(1, xpath);
		stmt.setInt(2, tsid);
		stmt.executeUpdate();	
		stmt.close();
	} catch (Exception e) {
		System.err.println(Utilities.getNow());
		e.printStackTrace();
	}
}

public void writexpathToDB(int tsid, String xpath) {
	
	String sql = "INSERT INTO cta.xpath (Test_Step_ID, Created_Date, Xpath) VALUES (?, ?, ?)";
	
	try {
		PreparedStatement stmt = testCon.prepareStatement(sql);
		stmt.setInt(1, tsid);
		stmt.setTimestamp(2, Utilities.getCurrentTimestamp());
		stmt.setString(3, xpath);
		stmt.executeUpdate();
		stmt.close();
	} catch (Exception e) {
		System.err.println(Utilities.getNow());
		e.printStackTrace();
	}
}

public String getNonMatchingAttr(int tsid) {
	String sql = "SELECT * FROM cta.test_step_attr WHERE test_step = ?";
	String result = "";
	try {
		PreparedStatement stmt = testCon.prepareStatement(sql);
		stmt.setInt(1, tsid);
		ResultSet rs = stmt.executeQuery();
		if(rs.next()) {
			result = rs.getString("nonmatchingattributes");
		}
		rs.close();
		stmt.close();
	} catch(Exception e) {
		System.err.println(Utilities.getNow());
		e.printStackTrace();
	}
	
	return result;
}

public String getOuterHTML(int tsrid) {
	String sql = "SELECT * FROM cta.test_step_result WHERE idtest_step_result = ?";
	String result = "";
	try {
		PreparedStatement stmt = testCon.prepareStatement(sql);
		stmt.setInt(1, tsrid);
		ResultSet rs = stmt.executeQuery();
		if(rs.next()) {
			result = rs.getString("outerhtml");
		}
		rs.close();
		stmt.close();
	} catch(Exception e) {
		System.err.println(Utilities.getNow());
		e.printStackTrace();
	}
	return result;
}

public String getRecordedOuterHTML(int tsid) {
	String sql = "SELECT * FROM cta.test_step WHERE idtest_step = ?";
	String result = "";
	try {
		PreparedStatement stmt = testCon.prepareStatement(sql);
		stmt.setInt(1, tsid);
		ResultSet rs = stmt.executeQuery();
		if(rs.next()) {
			result = rs.getString("outerhtml");
		}
		rs.close();
		stmt.close();
	} catch(Exception e) {
		System.err.println(Utilities.getNow());
		e.printStackTrace();
	}
	return result;
}

public String getLastPassedOuterHTML(int tsid, int ptcrid) {
	String sql = "SELECT * FROM cta.test_step_result WHERE Test_Step = ? AND Test_Case_Results_Id = ?";
	String result = "";
	try {
		PreparedStatement stmt = testCon.prepareStatement(sql);
		stmt.setInt(1, tsid);
		stmt.setInt(2, ptcrid);
		ResultSet rs = stmt.executeQuery();
		if(rs.next()) {
			result = rs.getString("outerhtml");
		}
		rs.close();
		stmt.close();
	} catch(Exception e) {
		System.err.println(Utilities.getNow());
		e.printStackTrace();
	}
	return result;
}

public void updateDuration(int testCaseResultsId, double duration) {
	String sql = "UPDATE cta.test_case_results SET Duration = ? WHERE idtest_case_results = ?";
	
	try {
		PreparedStatement stmt = testCon.prepareStatement(sql);
		stmt.setDouble(1, duration);
		stmt.setInt(2, testCaseResultsId);
		stmt.executeUpdate();	
		stmt.close();
	} catch (Exception e) {
		System.err.println(Utilities.getNow());
		e.printStackTrace();
	}
}

	public void updateFindAndAssignData(int testStepId, String action, String newAndroidUIAutomator, String newXpath, String newIdStrategy) throws SQLException {
	    int targetTestStepId = testStepId;

	    if ("click".equalsIgnoreCase(action) || "sendKeys".equalsIgnoreCase(action)) {
	        targetTestStepId = testStepId - 1;
	    }

	    String updateQuery = "UPDATE cta.mobile_automation " +
	                         "SET android_uiautomator = ?, xpath = ?, id_strategy = ? " +
	                         "WHERE idtest_step = ?";

	    try (PreparedStatement updateStmt = testCon.prepareStatement(updateQuery)) {
	        updateStmt.setString(1, newAndroidUIAutomator);
	        updateStmt.setString(2, newXpath);
	        updateStmt.setString(3, newIdStrategy);
	        updateStmt.setInt(4, targetTestStepId);
	        updateStmt.executeUpdate();
	    } catch (SQLException e) {
	        System.err.println(Utilities.getNow());
	        e.printStackTrace();
	    }
	}
	
	public void updateMobileStepAction(int testStepId, String action) {
		String updateQuery = "UPDATE cta.mobile_automation "
				+ "SET action" + "WHERE idtest_step = ?";
		try (PreparedStatement updateStmt = testCon.prepareStatement(updateQuery)) {
			updateStmt.setString(1, action);
			updateStmt.executeUpdate();
		} catch (SQLException e) {
			System.err.println(Utilities.getNow());
			e.printStackTrace();
		}
	}

public void updateDurationForTestStep(int testStepId, double ts_duration) {
	String sql = "UPDATE cta.test_step_result SET Duration = ? WHERE Test_Step = ? ORDER BY idtest_step_result DESC LIMIT 1";
	
	try {
		PreparedStatement stmt = testCon.prepareStatement(sql);
		stmt.setDouble(1, ts_duration);
		stmt.setInt(2, testStepId);
		stmt.executeUpdate();	
		stmt.close();
	} catch (Exception e) {
		System.err.println(Utilities.getNow());
		e.printStackTrace();
	}
}

public void removeOuterHTML(int tsid) {
	
	String sql = "UPDATE cta.test_step_result SET outerhtml = ? WHERE Test_Step = ?";
	
	try {
		PreparedStatement stmt = testCon.prepareStatement(sql);
		stmt.setString(1, null);
		stmt.setInt(2, tsid);
		stmt.executeUpdate();	
		stmt.close();
	} catch (Exception e) {
		System.err.println(Utilities.getNow());
		e.printStackTrace();
	}
	
}

public void removeRecordedOuterHTML(int tsid) {
	
	String sql = "UPDATE cta.test_step SET outerhtml = ? WHERE idtest_step = ?";
	
	try {
		PreparedStatement stmt = testCon.prepareStatement(sql);
		stmt.setString(1, null);
		stmt.setInt(2, tsid);
		stmt.executeUpdate();	
		stmt.close();
	} catch (Exception e) {
		System.err.println(Utilities.getNow());
		e.printStackTrace();
	}
	
}

public void approveAttr(int tsid, String uname) {
	
	String sql = "UPDATE cta.test_step_attr SET arenonmatchesapproved = ?, approvedby = ?, approveddate = ? WHERE test_step = ?";
	
	try {
		PreparedStatement stmt = testCon.prepareStatement(sql);
		stmt.setInt(1, 1);
		stmt.setString(2, uname);
		stmt.setTimestamp(3, Utilities.getCurrentTimestamp());
		stmt.setInt(4, tsid);
		stmt.executeUpdate();	
		stmt.close();
	} catch (Exception e) {
		System.err.println(Utilities.getNow());
		e.printStackTrace();
	}
	
}

public String getxpath(int tsid) {
	
	String sql = "SELECT * FROM cta.test_step where idtest_step = ?";
	String result = "";
	try {
		PreparedStatement stmt = testCon.prepareStatement(sql);
		stmt.setInt(1, tsid);
		ResultSet rs = stmt.executeQuery();
		if(rs.next()) {
			result = rs.getString("Object_Xpath");
		}
		rs.close();
		stmt.close();
	} catch(Exception e) {
		System.err.println(Utilities.getNow());
		e.printStackTrace();
	}
	return result;
}

public String getPassedFoundBy(int ptcrid, int tsid) {
	
	String sql = "SELECT * FROM cta.test_step_result WHERE Test_Case_Results_Id = ? AND Test_Step = ? AND Status = ? "
			+ "order by idtest_step_result desc";
	String result = "";
	try {
		PreparedStatement stmt = testCon.prepareStatement(sql);
		stmt.setInt(1, ptcrid);
		stmt.setInt(2, tsid);
		stmt.setString(3, "PASS");
		ResultSet rs = stmt.executeQuery();
		if (rs.next()) {
			result = (String) rs.getString("foundby");
		}
		rs.close();
		stmt.close();
	} catch (Exception e) {
		System.err.println(Utilities.getNow());
		e.printStackTrace();
	}
	return result;
}

public void updateTestSuiteCountAfterRerunFailedTests(int tsrid, int failedCount) {
	String sql = "SELECT * FROM cta.test_suite_results WHERE idtest_suite_results = ?";
	String sql_update = "UPDATE cta.test_suite_results SET passCount = ?, failCount = ? WHERE idtest_suite_results = ?";
	int totalCount = -1;
	try {
		PreparedStatement stmt = testCon.prepareStatement(sql);
		stmt.setInt(1, tsrid);
		ResultSet rs = stmt.executeQuery();
		if (rs.next()) {
			totalCount = (int) rs.getInt("totalCount");
			int passedCount = totalCount - failedCount;
			stmt = testCon.prepareStatement(sql_update);
			stmt.setInt(1, passedCount);
			stmt.setInt(2, failedCount);
			stmt.setInt(3, tsrid);
			stmt.executeUpdate();
		}
		rs.close();
		stmt.close();
	} catch(Exception e) {
		System.err.println(Utilities.getNow());
		e.printStackTrace();
	}
	
}

public void removePassedOuterHTML(int tsrid) {
	
	String sql = "UPDATE cta.test_step_result SET outerhtml = ? WHERE idtest_step_result = ?";
	
	try {
		PreparedStatement stmt = testCon.prepareStatement(sql);
		stmt.setString(1, null);
		stmt.setInt(2, tsrid);
		stmt.executeUpdate();	
		stmt.close();
	} catch (Exception e) {
		System.err.println(Utilities.getNow());
		e.printStackTrace();
	}
}

public void updateRerunInTestCaseResultsToDB(int testCaseResultsId) {
	
	String sql = "UPDATE cta.test_case_results SET firstrun = ? WHERE idtest_case_results = ?";
	
	try {
		PreparedStatement stmt = testCon.prepareStatement(sql);
		stmt.setString(1, "false");
		stmt.setInt(2, testCaseResultsId);
		stmt.executeUpdate();	
		stmt.close();
	} catch (Exception e) {
		System.err.println(Utilities.getNow());
		e.printStackTrace();
	}
	
}

public int getParallelThreadCount(int testSuiteId) {
	
	String sql = "SELECT threadcount FROM cta.test_suite where idtest_suite = ?";
	int result = 0;
	try {
		PreparedStatement stmt = testCon.prepareStatement(sql);
		stmt.setInt(1, testSuiteId);
		ResultSet rs = stmt.executeQuery();
		if (rs.next()) {
			result = rs.getInt(1);
		}
		rs.close();
		stmt.close();
	} catch (Exception e) {
		System.err.println(Utilities.getNow());
		e.printStackTrace();
	}

	return result;
}

public void updateFirstRunCountToDB(int tsrid) {
	String sql = "SELECT * FROM cta.test_suite_results WHERE idtest_suite_results = ?";
	String sql_update = "UPDATE cta.test_suite_results SET firstRun_passCount = ?, firstRun_failCount = ? WHERE idtest_suite_results = ?";
	int totalCount = -1;
	try {
		PreparedStatement stmt = testCon.prepareStatement(sql);
		stmt.setInt(1, tsrid);
		ResultSet rs = stmt.executeQuery();
		if (rs.next()) {
			int passedCount = (int) rs.getInt("passCount");
			int failedCount = (int) rs.getInt("failCount");
			stmt = testCon.prepareStatement(sql_update);
			stmt.setInt(1, passedCount);
			stmt.setInt(2, failedCount);
			stmt.setInt(3, tsrid);
			stmt.executeUpdate();
		}
		rs.close();
		stmt.close();
	} catch(Exception e) {
		System.err.println(Utilities.getNow());
		e.printStackTrace();
	}
	
}

public void update2ndRunPassCount(int tsrid) {

	String sql = "UPDATE cta.test_suite_results SET secondRun_passCount = secondRun_passCount + 1 WHERE idtest_suite_results = ?";

	try {
		PreparedStatement stmt = testCon.prepareStatement(sql);
		stmt.setInt(1, tsrid);
		stmt.executeUpdate();
		stmt.close();
	} catch (Exception e) {
		System.err.println(Utilities.getNow());
		e.printStackTrace();
	}
	
}

public void update2ndRunFailCount(int tsrid) {

	String sql = "UPDATE cta.test_suite_results SET secondRun_failCount = secondRun_failCount + 1 WHERE idtest_suite_results = ?";

	try {
		PreparedStatement stmt = testCon.prepareStatement(sql);
		stmt.setInt(1, tsrid);
		stmt.executeUpdate();
		stmt.close();
	} catch (Exception e) {
		System.err.println(Utilities.getNow());
		e.printStackTrace();
	}
	
}

public void set2ndRunCount(int tsrid) {

	String sql = "UPDATE cta.test_suite_results SET secondRun_passCount = ?, secondRun_failCount = ? WHERE idtest_suite_results = ?";

	try {
		PreparedStatement stmt = testCon.prepareStatement(sql);
		stmt.setInt(1, 0);
		stmt.setInt(2, 0);
		stmt.setInt(3, tsrid);
		stmt.executeUpdate();
		stmt.close();
	} catch (Exception e) {
		System.err.println(Utilities.getNow());
		e.printStackTrace();
	}
	
}

public void updateNotExecutedCount(int tsrid) {
	String sql = "UPDATE cta.test_suite_results SET not_executed = not_executed - 1 WHERE idtest_suite_results = ?";

	try {
		PreparedStatement stmt = testCon.prepareStatement(sql);
		stmt.setInt(1, tsrid);
		stmt.executeUpdate();
		stmt.close();
	} catch (Exception e) {
		System.err.println(Utilities.getNow());
		e.printStackTrace();
	}
	
}

public void updatePassCount(int per, int tsrid, int total, int pcount) {
	
	String sql_pass = "SELECT * FROM cta.test_suite_results WHERE idtest_suite_results = ?";
	String sql = null;

	if (per == 5) {
		sql = "UPDATE cta.test_suite_results SET 5Percent = ? WHERE idtest_suite_results = ?";
	} else if (per == 10) {
		sql = "UPDATE cta.test_suite_results SET 10Percent = ? WHERE idtest_suite_results = ?";
	} else if (per == 15) {
		sql = "UPDATE cta.test_suite_results SET 15Percent = ? WHERE idtest_suite_results = ?";
	} else if (per == 20) {
		sql = "UPDATE cta.test_suite_results SET 20Percent = ? WHERE idtest_suite_results = ?";
	} else if (per == 25) {
		sql = "UPDATE cta.test_suite_results SET 25Percent = ? WHERE idtest_suite_results = ?";
	} else if (per == 30) {
		sql = "UPDATE cta.test_suite_results SET 30Percent = ? WHERE idtest_suite_results = ?";
	} else if (per == 35) {
		sql = "UPDATE cta.test_suite_results SET 35Percent = ? WHERE idtest_suite_results = ?";
	} else if (per == 40) {
		sql = "UPDATE cta.test_suite_results SET 40Percent = ? WHERE idtest_suite_results = ?";
	} else if (per == 45) {
		sql = "UPDATE cta.test_suite_results SET 45Percent = ? WHERE idtest_suite_results = ?";
	} else if (per == 50) {
		sql = "UPDATE cta.test_suite_results SET 50Percent = ? WHERE idtest_suite_results = ?";
	} else if (per == 55) {
		sql = "UPDATE cta.test_suite_results SET 55Percent = ? WHERE idtest_suite_results = ?";
	} else if (per == 60) {
		sql = "UPDATE cta.test_suite_results SET 60Percent = ? WHERE idtest_suite_results = ?";
	} else if (per == 65) {
		sql = "UPDATE cta.test_suite_results SET 65Percent = ? WHERE idtest_suite_results = ?";
	} else if (per == 70) {
		sql = "UPDATE cta.test_suite_results SET 70Percent = ? WHERE idtest_suite_results = ?";
	} else if (per == 75) {
		sql = "UPDATE cta.test_suite_results SET 75Percent = ? WHERE idtest_suite_results = ?";
	} else if (per == 80) {
		sql = "UPDATE cta.test_suite_results SET 80Percent = ? WHERE idtest_suite_results = ?";
	} else if (per == 85) {
		sql = "UPDATE cta.test_suite_results SET 85Percent = ? WHERE idtest_suite_results = ?";
	} else if (per == 90) {
		sql = "UPDATE cta.test_suite_results SET 90Percent = ? WHERE idtest_suite_results = ?";
	} else if (per == 95) {
		sql = "UPDATE cta.test_suite_results SET 95Percent = ? WHERE idtest_suite_results = ?";
	} else {
		sql = "UPDATE cta.test_suite_results SET 100Percent = ? WHERE idtest_suite_results = ?";
	} 

	try {
		PreparedStatement stmt = testCon.prepareStatement(sql_pass);
		stmt.setInt(1, tsrid);
		ResultSet rs = stmt.executeQuery();
		if(rs.next()) {
			int passCount = -1;
			if(pcount == -1) {
				passCount = rs.getInt("passCount");
			} else {
				passCount = pcount;
			}
			double percent = (double) passCount/total;
			double percentage = Math.round(percent * 100.0) / 100.0;
			stmt = testCon.prepareStatement(sql);
			stmt.setDouble(1, percentage);
			stmt.setInt(2, tsrid);
			stmt.executeUpdate();
		}
		rs.close();
		stmt.close();
	} catch (Exception e) {
		System.err.println(Utilities.getNow());
		e.printStackTrace();
	}
	
}

public void updatePassCountForPreviousRun(int tsrid) {
	JSONArray json = getTestCaseResultFromTSRID(tsrid);
	int n = json.size();
	int passCount = 0;
	for(int i=0;i<n;i++) {
		JSONObject tcr = (JSONObject) json.get(i);
		String status = (String) tcr.get("Status");
		if(status.equalsIgnoreCase("PASS")) {
			passCount++;
		}
		double per = (double) i/n;
    	double next_per = (double) (i+1)/n;
    	if(per <= 0.05 && next_per>=0.05) {
    		updatePassCount(5, tsrid, i, passCount);
    	}
    	if(per <= 0.1 && next_per>=0.1) {
    		updatePassCount(10, tsrid, i, passCount);
    	}
    	if(per <= 0.15 && next_per>=0.15) {
    		updatePassCount(15, tsrid, i, passCount);
    	}
    	if(per <= 0.2 && next_per>=0.2) {
    		updatePassCount(20, tsrid, i, passCount);
    	}
    	if(per <= 0.25 && next_per>=0.25) {
    		updatePassCount(25, tsrid, i, passCount);
    	}
    	if(per <= 0.3 && next_per>=0.3) {
    		updatePassCount(30, tsrid, i, passCount);
    	}
    	if(per <= 0.35 && next_per>=0.35) {
    		updatePassCount(35, tsrid, i, passCount);
    	}
    	if(per <= 0.4 && next_per>=0.4) {
    		updatePassCount(40, tsrid, i, passCount);
    	}
    	if(per <= 0.45 && next_per>=0.45) {
    		updatePassCount(45, tsrid, i, passCount);
    	}
    	if(per <= 0.5 && next_per>=0.5) {
    		updatePassCount(50, tsrid, i, passCount);
    	}
    	if(per <= 0.55 && next_per>=0.55) {
    		updatePassCount(55, tsrid, i, passCount);
    	}
    	if(per <= 0.60 && next_per>=0.60) {
    		updatePassCount(60, tsrid, i, passCount);
    	}
    	if(per <= 0.65 && next_per>=0.65) {
    		updatePassCount(65, tsrid, i, passCount);
    	}
    	if(per <= 0.70 && next_per>=0.70) {
    		updatePassCount(70, tsrid, i, passCount);
    	}
    	if(per <= 0.75 && next_per>=0.75) {
    		updatePassCount(75, tsrid, i, passCount);
    	}
    	if(per <= 0.80 && next_per>=0.80) {
    		updatePassCount(80, tsrid, i, passCount);
    	}
    	if(per <= 0.85 && next_per>=0.85) {
    		updatePassCount(85, tsrid, i, passCount);
    	}
    	if(per <= 0.90 && next_per>=0.90) {
    		updatePassCount(90, tsrid, i, passCount);
    	}
    	if(per <= 0.95 && next_per>=0.95) {
    		updatePassCount(95, tsrid, i, passCount);
    	}
    	if(per <= 1.00 && next_per>=1.00) {
    		updatePassCount(100, tsrid, i, passCount);
    	}
	}
}

public JSONArray getTestCaseResultFromTSRID(int tsrid) {
	String all_sql = "SELECT * FROM cta.test_case_results tcr JOIN cta.test_suite_case_results tscr ON "
			+ "tscr.test_case_results_id = tcr.idtest_case_results WHERE tscr.test_suite_results_id = ?"
			+ " and tcr.firstrun = ? order by tcr.Executed_Date Desc";

	JSONArray tcresult = new JSONArray();
	ResultSet rs = null;
	try {
		PreparedStatement stmt = testCon.prepareStatement(all_sql);
		stmt.setInt(1, tsrid);
		stmt.setString(2, "true");
		rs = stmt.executeQuery();

		while (rs.next()) {
			JSONObject tcr = getTestCaseStatusAsJSON(rs);
			tcresult.add(tcr);
		}
		rs.close();
		stmt.close();
	} catch (Exception e) {
		System.err.println(Utilities.getNow());
		e.printStackTrace();
	}
	return tcresult;
}

private JSONObject getTestCaseStatusAsJSON(ResultSet rs) {
	JSONObject jsonRow = new JSONObject();
	try {
		jsonRow.put("idtest_case_results", rs.getInt("idtest_case_results"));
		jsonRow.put("Test_Case_Id", rs.getInt("Test_Case_Id"));
		jsonRow.put("Status", rs.getString("Status"));
		jsonRow.put("Executed_By", rs.getString("Executed_By"));
		jsonRow.put("Executed_Date", (rs.getTimestamp("Executed_Date")).toString());
		jsonRow.put("testcase", rs.getString("test_case_name"));
		jsonRow.put("firstrun", rs.getString("firstrun"));
	} catch (Exception e) {
		System.err.println(Utilities.getNow());
		e.printStackTrace();
	}
	return jsonRow;
}

public JSONObject getPercentageDataForRuns(int tsrid1, int tsrid2) {
	JSONObject json = new JSONObject();
	JSONArray data = new JSONArray();
	JSONObject json1 = getPercentageData(tsrid1);
	JSONObject json2 = getPercentageData(tsrid2);
	
	for(int i=0;i<20;i++) {
		JSONObject percent = new JSONObject();
		percent.put("current", json1.get(i + ""));
		percent.put("previous", json2.get(i + ""));
		data.add(percent);
	}
	json.put("progressData", data);
	return json;
}

public boolean isPercentageUpdated(int tsrid) {
	String sql = "SELECT * FROM cta.test_suite_results WHERE idtest_suite_results = ?";
	ResultSet rs = null;
	boolean res = true;
	try {
		PreparedStatement stmt = testCon.prepareStatement(sql);
		stmt.setInt(1, tsrid);
		rs = stmt.executeQuery();
		if(rs.next()) {
			String status = rs.getString("Status");
			if(status.equalsIgnoreCase("RUNNING") || status.equalsIgnoreCase("STARTED")) {
				return true;
			}
			Double temp = rs.getDouble("5Percent");
			if(temp == 0.0) {
				res = false;
			} else {
				res = true;
			}
		}
	} catch (Exception e) {
		System.err.println(Utilities.getNow());
		e.printStackTrace();
	}
	return res;
}

public JSONObject getPercentageData(int tsrid) {
	String sql = "SELECT * FROM cta.test_suite_results WHERE idtest_suite_results = ?";
	if(!isPercentageUpdated(tsrid)) {
		updatePassCountForPreviousRun(tsrid);
	}
	JSONObject json = new JSONObject();
	ResultSet rs = null;
	try {
		PreparedStatement stmt = testCon.prepareStatement(sql);
		stmt.setInt(1, tsrid);
		rs = stmt.executeQuery();
		if(rs.next()) {
			json.put("0", rs.getDouble("5Percent"));
			json.put("1", rs.getDouble("10Percent"));
			json.put("2", rs.getDouble("15Percent"));
			json.put("3", rs.getDouble("20Percent"));
			json.put("4", rs.getDouble("25Percent"));
			json.put("5", rs.getDouble("30Percent"));
			json.put("6", rs.getDouble("35Percent"));
			json.put("7", rs.getDouble("40Percent"));
			json.put("8", rs.getDouble("45Percent"));
			json.put("9", rs.getDouble("50Percent"));
			json.put("10", rs.getDouble("55Percent"));
			json.put("11", rs.getDouble("60Percent"));
			json.put("12", rs.getDouble("65Percent"));
			json.put("13", rs.getDouble("70Percent"));
			json.put("14", rs.getDouble("75Percent"));
			json.put("15", rs.getDouble("80Percent"));
			json.put("16", rs.getDouble("85Percent"));
			json.put("17", rs.getDouble("90Percent"));
			json.put("18", rs.getDouble("95Percent"));
			json.put("19", rs.getDouble("100Percent"));
		}
	} catch (Exception e) {
		System.err.println(Utilities.getNow());
		e.printStackTrace();
	}
	return json;
}

public void updateInitialVariablesToDB(int tsid, JSONObject varJSON) {
	String sql = "UPDATE cta.test_suite SET initialvariables = ? WHERE idtest_suite = ?";
	try {
		PreparedStatement stmt = testCon.prepareStatement(sql);
		stmt = testCon.prepareStatement(sql);
		stmt.setString(1, varJSON.toString());
		stmt.setInt(2, tsid);
		stmt.executeUpdate();
		stmt.close();
	} catch (Exception e) {
		System.err.println(Utilities.getNow());
		e.printStackTrace();
	}
	
}

public int getLastTestSuiteResultsId(int tsid) {
	String sql = "SELECT * FROM cta.test_suite_results WHERE Test_Suite_Id = ? AND (isIgnored = 0 OR isIgnored IS NULL)"
			+ " ORDER BY idtest_suite_results DESC LIMIT 1 offset 1";
	int result = -1;
	try {
		PreparedStatement stmt = testCon.prepareStatement(sql);
		stmt.setInt(1, tsid);;
		ResultSet rs = stmt.executeQuery();
		if(rs.next()) {
			result = rs.getInt("idtest_suite_results");
		}
		rs.close();
		stmt.close();
	} catch (Exception e) {
		System.err.println(Utilities.getNow());
		e.printStackTrace();
	}
	return result;
}

public void insertFoundElementToDB(int tsrid, String outerhtml) {
	String sql = "INSERT INTO cta.foundelements (Test_Step_Results_Id, outerhtml) VALUES (?, ?)";
	try {
		PreparedStatement stmt = testCon.prepareStatement(sql);
		stmt.setInt(1, tsrid);
		if(outerhtml.length() > 1000) {
			outerhtml = outerhtml.substring(0, 1000);
		}
		stmt.setString(2, outerhtml);
		stmt.executeUpdate();
		stmt.close();
	} catch (Exception e) {
		System.err.println(Utilities.getNow());
		e.printStackTrace();
	}
}

public void updateApprovedElementToDB(int tsid, String outerhtml) {
	String sql = "UPDATE cta.test_step_attr SET approved_element = ? WHERE test_step = ?";
	try {
		PreparedStatement stmt = testCon.prepareStatement(sql);
		stmt = testCon.prepareStatement(sql);
		stmt.setString(1, outerhtml);
		stmt.setInt(2, tsid);
		stmt.executeUpdate();
		stmt.close();
	} catch (Exception e) {
		System.err.println(Utilities.getNow());
		e.printStackTrace();
	}
}

public JSONArray getAllFoundElementsFromDB(int tsrid) {
	String sql = "SELECT * FROM cta.foundelements WHERE Test_Step_Results_Id = ?";
	JSONArray result = new JSONArray();
	try {
		PreparedStatement stmt = testCon.prepareStatement(sql);
		stmt.setInt(1, tsrid);
		ResultSet rs = stmt.executeQuery();
		while(rs.next()) {
			JSONObject json = new JSONObject();
			json.put(rs.getInt("idfoundelements"), rs.getString("outerhtml"));
			result.add(json);
		}
		rs.close();
		stmt.close();
	} catch(Exception e) {
		System.err.println(Utilities.getNow());
		e.printStackTrace();
	}
	return result;
}

public boolean getWaitConditionFromDB(int tcid) {
	String sql="SELECT * FROM cta.test_case WHERE idtest_case = ?";
	boolean result = false;
	try {
		PreparedStatement stmt = testCon.prepareStatement(sql);
		stmt.setInt(1, tcid);
		ResultSet rs = stmt.executeQuery();
		if(rs.next()) {
			String res = rs.getString("isWait");
			if(res != null && res.equalsIgnoreCase("true")) {
				result = true;
			} else {
				result = false;
			}
		}
		rs.close();
		stmt.close();
	} catch(Exception e) {
		System.err.println(Utilities.getNow());
		e.printStackTrace();
	}
	return result;
}

public String getOuterHTMLWithId(int elementid) {
	String sql = "SELECT * FROM cta.foundelements where idfoundelements = ?";
	String result = "";
	try {
		PreparedStatement stmt = testCon.prepareStatement(sql);
		stmt.setInt(1, elementid);
		ResultSet rs = stmt.executeQuery();
		if(rs.next()) {
			result = rs.getString("outerhtml");
		}
		rs.close();
		stmt.close();
	} catch(Exception e) {
		System.err.println(Utilities.getNow());
		e.printStackTrace();
	}
	return result;
}

public int insertRequestToDB(String url, int tcid, int tcrid, int tsid, 
		int tsrid, int stepnum, int attempt) {
	String sql = "INSERT INTO cta.apicall_data (url, Test_Case_Id, "
			+ "Test_Case_Results_Id, Test_Step_Id,Test_Step_Result_Id, "
			+ "starttime, stepnum, attempt) "
			+ "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
	int pk = -1;
	try {
		PreparedStatement stmt = testCon.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
		stmt.setString(1, url);
		stmt.setInt(2, tcid);
		stmt.setInt(3, tcrid);
		stmt.setInt(4, tsid);
		stmt.setInt(5, tsrid);
		stmt.setLong(6, System.currentTimeMillis());
		stmt.setInt(7, stepnum);
		stmt.setInt(8, attempt);
		stmt.executeUpdate();
		ResultSet generatedKeys = stmt.getGeneratedKeys();
		if (generatedKeys.next()) {
			pk = (generatedKeys.getBigDecimal(1)).intValue();
		}
		generatedKeys.close();
		stmt.close();
	} catch (Exception e) {
		System.err.println(Utilities.getNow());
		e.printStackTrace();
	}
	return pk;
}

public boolean getPendingAPICalls( int tcrid, int tsid) {
	boolean pendingapis = true;
	String sql = "SELECT * FROM apicall_data where statuscode is null and "
			+ "Test_Case_Results_Id = ? and Test_Step_Id = ?";
	int pk = -1;
	try {
		PreparedStatement stmt = testCon.prepareStatement(sql);
		stmt.setInt(1, tcrid);
		stmt.setInt(2, tsid);
		ResultSet rs = stmt.executeQuery();
		if(!rs.next()) {
			pendingapis = false;
		}
		stmt.close();
	} catch (Exception e) {
		System.err.println(Utilities.getNow());
		e.printStackTrace();
	}
	return pendingapis;
}

public void updateApiDataToDB(long responsetime, long statuscode, long endtime, int id) {
	String sql = "UPDATE cta.apicall_data SET responsetime = ?, statuscode = ?, endtime = ? WHERE idapicall_data = ?";
	try {
		PreparedStatement stmt = testCon.prepareStatement(sql);
		stmt.setLong(1, responsetime);
		stmt.setLong(2, statuscode);
		stmt.setLong(3, endtime);
		stmt.setInt(4, id);
		stmt.executeUpdate();
		stmt.close();
	} catch (Exception e) {
		System.err.println(Utilities.getNow());
		e.printStackTrace();
	}
}

public void updateTestStepResultIdToApiData(int tsid, int tsrid) {
	String sql = "UPDATE cta.apicall_data SET Test_Step_Result_Id = ? WHERE Test_Step_Id = ? AND Test_Step_Result_Id = ?";
	try {
		PreparedStatement stmt = testCon.prepareStatement(sql);
		stmt.setInt(1, tsrid);
		stmt.setInt(2, tsid);
		stmt.setInt(3, -1);
		stmt.executeUpdate();
		stmt.close();
	} catch (Exception e) {
		System.err.println(Utilities.getNow());
		e.printStackTrace();
	}
}

public void updateApiCallCount(int tcrid) {
	String sql = "UPDATE cta.test_case_results SET apicalls_count = COALESCE(apicalls_count, 0) + 1 WHERE idtest_case_results = ?";
	try {
		PreparedStatement stmt = testCon.prepareStatement(sql);
		stmt.setInt(1, tcrid);
		stmt.executeUpdate();
		stmt.close();
	} catch (Exception e) {
		System.err.println(Utilities.getNow());
		e.printStackTrace();
	}
}

public void summarizeStatusCodeCountToDB(int tcrid) {
	String sql = "SELECT DISTINCT(statuscode) FROM cta.apicall_data WHERE Test_Case_Results_Id = ?";
	try {
		PreparedStatement stmt = testCon.prepareStatement(sql);
		stmt.setInt(1, tcrid);
		ResultSet rs = stmt.executeQuery();
		while(rs.next()) {
			long statusCode = rs.getLong("statuscode");
			updateStatusCodeCountToDB(statusCode, tcrid);
		}
		rs.close();
		stmt.close();
	} catch (Exception e) {
		System.err.println(Utilities.getNow());
		e.printStackTrace();
	}
}

private void updateStatusCodeCountToDB(long statusCode, int tcrid) {
	String sql = "SELECT * FROM cta.apicall_data WHERE statuscode = ? AND Test_Case_Results_Id = ?";
	String sql1 = "INSERT INTO cta.summarized_apidata (statuscode, count, average_response_time, Test_Case_Results_Id) VALUES (?, ?, ?, ?)";
	
	try {
		PreparedStatement stmt = testCon.prepareStatement(sql);
		stmt.setLong(1, statusCode);
		stmt.setInt(2, tcrid);
		ResultSet rs = stmt.executeQuery();
		int totalcount = 0;
		long totalresponsetime = 0;
		while(rs.next()) {
			totalcount++;
			totalresponsetime += rs.getLong("responsetime");
		}
		stmt = testCon.prepareStatement(sql1);
		double average = Math.round(((double) totalresponsetime / totalcount) * 100.0) / 100.0;
		stmt.setLong(1, statusCode);
		stmt.setInt(2, totalcount);
		stmt.setDouble(3, average);
		stmt.setInt(4, tcrid);
		stmt.executeUpdate();
		rs.close();
		stmt.close();
	} catch (Exception e) {
		System.err.println(Utilities.getNow());
		e.printStackTrace();
	}
	
}

public JSONObject getAPIDataFromDB(int tcrid) {
	String sql = "SELECT * FROM cta.apicall_data WHERE Test_Case_Results_Id = ?";
	String sql2 = "SELECT * FROM cta.summarized_apidata WHERE Test_Case_Results_Id = ?";
	JSONObject json = new JSONObject();
	JSONArray json1 = new JSONArray();
	JSONArray json2 = new JSONArray();
	try {
		PreparedStatement stmt = testCon.prepareStatement(sql);
		stmt.setInt(1, tcrid);
		ResultSet rs = stmt.executeQuery();
		while(rs.next()) {
			JSONObject jsonRow = getAPIDataAsJSONFROMDB(rs);
			json1.add(jsonRow);
		}
		json.put("apidata", json1);
		stmt = testCon.prepareStatement(sql2);
		stmt.setInt(1, tcrid);
		ResultSet rs2 = stmt.executeQuery();
		while(rs2.next()) {
			JSONObject jsonRow = getSummarizedAPIDataAsJSONFROMDB(rs2);
			json2.add(jsonRow);
		}
		json.put("summarizedData", json2);
		rs.close();
		rs2.close();
		stmt.close();
	} catch (Exception e) {
		System.err.println(Utilities.getNow());
		e.printStackTrace();
	}
	return json;
}

private JSONObject getSummarizedAPIDataAsJSONFROMDB(ResultSet rs) {
	JSONObject jsonRow = new JSONObject();
	try {
		jsonRow.put("statuscode", rs.getLong("statuscode"));
		jsonRow.put("count", rs.getInt("count"));
		jsonRow.put("average_response_time", rs.getDouble("average_response_time"));
	} catch (Exception e) {
		System.err.println(Utilities.getNow());
		e.printStackTrace();
	}
	return jsonRow;
}

private JSONObject getAPIDataAsJSONFROMDB(ResultSet rs) {
	JSONObject jsonRow = new JSONObject();
	try {
		jsonRow.put("url", rs.getString("url"));
		jsonRow.put("Test_Step_Id", rs.getLong("Test_Step_Id"));
		jsonRow.put("starttime", rs.getLong("starttime"));
		jsonRow.put("endtime", rs.getLong("endtime"));
		jsonRow.put("responsetime", rs.getLong("responsetime"));
		jsonRow.put("statuscode", rs.getLong("statuscode"));
		jsonRow.put("stepnum", rs.getLong("stepnum"));
	} catch (Exception e) {
		System.err.println(Utilities.getNow());
		e.printStackTrace();
	}
	return jsonRow;
}

public void saveApiCallToDB(JSONObject json, int tsid) {
	String sql = "UPDATE cta.test_step SET apiData = ? WHERE idtest_step = ?";
	try {
		PreparedStatement stmt = testCon.prepareStatement(sql);
		stmt = testCon.prepareStatement(sql);
		stmt.setString(1, json.toString());
		stmt.setInt(2, tsid);
		stmt.executeUpdate();
		stmt.close();
	} catch (Exception e) {
		System.err.println(Utilities.getNow());
		e.printStackTrace();
	}
}

public List<JSONObject> getApiDataForRecentStep(int tcrid, int test_step_id, 
		int attempt) {
	String sql = "SELECT * FROM cta.apicall_data WHERE Test_Step_Id = ?"
			+ " AND Test_Case_Results_Id = ? AND attempt = ?";
	List<JSONObject> result = new ArrayList<JSONObject>();
	try {
		PreparedStatement stmt = testCon.prepareStatement(sql);
		stmt.setInt(1, test_step_id);
		stmt.setInt(2, tcrid);
		stmt.setInt(3, attempt);
		ResultSet rs = stmt.executeQuery();
		while(rs.next()) {
			JSONObject json = new JSONObject();
			String url = rs.getString("url");
			long statuscode = rs.getLong("statuscode");
			json.put("url", url);
			json.put("statuscode", statuscode);
			result.add(json);
		}
		rs.close();
		stmt.close();
	} catch (Exception e) {
		System.err.println(Utilities.getNow());
		e.printStackTrace();
	}
	return result;
}

public JSONObject getDataForExecutionReports(int tcrid, int companyId) {
	String sql = "SELECT * FROM cta.test_case_results WHERE idtest_case_results = ?";
	JSONObject result = new JSONObject();
	try {
		PreparedStatement stmt = testCon.prepareStatement(sql);
		stmt.setInt(1, tcrid);;
		ResultSet rs = stmt.executeQuery();
		if(rs.next()) {
			int tcid = rs.getInt("Test_Case_Id");
			result.put("test_case_name", rs.getString("test_case_name"));
			result.put("Executed_Date", rs.getDate("Executed_Date").toString());
			result.put("Duration", rs.getDouble("Duration"));
			result.put("Status", rs.getString("status"));
			result.put("Browser", rs.getString("Browser"));
			String fileurl = AppProperties.fileurl + "?action=executionlog&token=" + randomkey + "&companyid="
					+ companyId + "&tcid=" + tcid + "&tcrid=" + tcrid;
			result.put("logFile", fileurl);
		}
		rs.close();
		stmt.close();
	} catch (Exception e) {
		System.err.println(Utilities.getNow());
		e.printStackTrace();
	}
	return result;
}

public String getTestStepStatus(int tcrid, int stepNumber) {
	String sql = "SELECT * FROM cta.test_step_result tsr JOIN cta.test_step ts ON tsr.Test_Step = ts.idtest_step WHERE "
			+ "ts.tsSequence = ? AND tsr.Test_Case_Results_Id = ? ORDER BY idtest_step_result DESC";
	String result = "";
	try {
		PreparedStatement stmt = testCon.prepareStatement(sql);
		stmt.setInt(1, stepNumber);
		stmt.setInt(2, tcrid);
		ResultSet rs = stmt.executeQuery();
		if(rs.next()) {
			result = rs.getString("Status");
		}
		rs.close();
		stmt.close();
	} catch (Exception e) {
		System.err.println(Utilities.getNow());
		e.printStackTrace();
	}
	return result;
}

public JSONArray getExecutionFlow(int tcid) {
	JSONArray result = new JSONArray();
	JSONArray jsonArr = new JSONArray();
	JSONArray dependantTCArr = getTestStepsForTCIDAndAncestors(tcid, jsonArr);
	for(int i=0;i<dependantTCArr.size();i++) {
		int testcaseid = (int)dependantTCArr.get(i);
		if(dependantTCArr.size() > 1) {
			jsonArr.add("Starting the Dependant Test Case " + testcaseid);
		}
		getExecutionFlowForTestCase(testcaseid, result);
	}
	return result;
}

public void getExecutionFlowForTestCase(int tcid, JSONArray jsonArr) {
	String sql = "SELECT * FROM cta.test_step AS ts JOIN cta.test_step_attr AS tsa ON ts.idtest_step = tsa.test_step "
			+ "WHERE ts.status = 0 AND ts.Test_Case_Id = ? ORDER BY tsSequence";
	try {
		PreparedStatement stmt = testCon.prepareStatement(sql);
		stmt.setInt(1, tcid);
		ResultSet rs = stmt.executeQuery();
		while(rs.next()) {
			String flow = rs.getString("Flow");
			if(flow != null && !flow.equalsIgnoreCase("Disable")) {
				String keyword = rs.getString("Keyword");
				if(keyword != null && keyword.equalsIgnoreCase("Inline")) {
					String testData = rs.getString("testdata");
					int testdata = Integer.valueOf(testData);
					jsonArr.add("Starting the Inline Test Case " + testData);
					getExecutionFlowForTestCase(testdata, jsonArr);
				} else {
					int stepNum = rs.getInt("tsSequence");
					String wait = rs.getString("wait");
					int waitTime = rs.getInt("waittime");
					jsonArr.add("Step Number " + stepNum);
					if(wait != null) {
						jsonArr.add("Waiting " + wait + " the Step for " + waitTime + " seconds");
					}
				}
			}
		}
		rs.close();
		stmt.close();
	} catch (Exception e) {
		System.err.println(Utilities.getNow());
		e.printStackTrace();
	}
}

}
