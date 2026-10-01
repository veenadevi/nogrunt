package nogrunt.dbaccess;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Set;
import java.util.HashMap;
import java.util.Iterator;

import nogrunt.*;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import jakarta.servlet.http.HttpServletRequest;

public class DashBoardData {
	
	public Connection testCon;
	
	public DashBoardData(Connection tc) {
		testCon = tc;
	}
	
	public DashBoardData(MySQlConn msc) {
		testCon = msc.testCon;
	}
	
	public JSONArray getTop5(HttpServletRequest request) {
		String action = request.getParameter("action");
		String companyIdStr = request.getParameter("companyid");
		int companyId = -1;
		if(companyIdStr != null && !companyIdStr.equals("undefined")) {
			companyId = Integer.valueOf(companyIdStr);
		}
		String fromDate = request.getParameter("fromDate");
		String toDate = request.getParameter("toDate");
		
		if(toDate == null || fromDate == null) {
			LocalDate today = LocalDate.now();
	        toDate = getTodate(today);
	        fromDate = getFromdate(today);
		}
		return getTop5(companyId, fromDate, toDate);
	}
	
	public JSONArray getCountByAuthor(HttpServletRequest request) {
		String action = request.getParameter("action");
		String companyIdStr = request.getParameter("companyid");
		int companyId = -1;
		if(companyIdStr != null && !companyIdStr.equals("undefined")) {
			companyId = Integer.valueOf(companyIdStr);
		}
		String fromDate = request.getParameter("fromDate");
		String toDate = request.getParameter("toDate");
		String author = request.getParameter("author");
		
		if(toDate == null || fromDate == null) {
			LocalDate today = LocalDate.now();
	        toDate = getTodate(today);
	        fromDate = getFromdate(today);
		}
		return getCountByAuthor(companyId, author, fromDate, toDate);
	}
	
	public JSONArray getTopCards(HttpServletRequest request) {
		String action = request.getParameter("action");
		String companyIdStr = request.getParameter("companyid");
		int companyId = -1;
		if(companyIdStr != null && !companyIdStr.equals("undefined")) {
			companyId = Integer.valueOf(companyIdStr);
		}
		String fromDate = request.getParameter("fromDate");
		String toDate = request.getParameter("toDate");
		
		if(toDate == null || fromDate == null) {
			LocalDate today = LocalDate.now();
	        toDate = getTodate(today);
	        fromDate = getFromdate(today);
		}
		return getTopCards(companyId, fromDate, toDate);
	}
	
	public JSONArray getExecutionThreadChart(HttpServletRequest request) {
		String action = request.getParameter("action");
		String companyIdStr = request.getParameter("companyid");
		int companyId = -1;
		if(companyIdStr != null && !companyIdStr.equals("undefined")) {
			companyId = Integer.valueOf(companyIdStr);
		}
		String fromDate = request.getParameter("fromDateTime");
		String toDate = request.getParameter("toDateTime");
		
		if(toDate == null || fromDate == null) {
			LocalDate today = LocalDate.now();
	        toDate = getTodateTime(today);
	        fromDate = getFromdateTime(today);
		}
		return getExecutionThreadChart(companyId, fromDate, toDate);
	}
	
	public JSONArray getLast5Suites(HttpServletRequest request) {
		String action = request.getParameter("action");
		String companyIdStr = request.getParameter("companyid");
		int companyId = -1;
		if(companyIdStr != null && !companyIdStr.equals("undefined")) {
			companyId = Integer.valueOf(companyIdStr);
		}
		String fromDate = request.getParameter("fromDateTime");
		String toDate = request.getParameter("toDateTime");
		
		if(toDate == null || fromDate == null) {
			LocalDate today = LocalDate.now();
	        toDate = getTodateTime(today);
	        fromDate = getFromdateTime(today);
		}
		return getLast5Suites(companyId, fromDate, toDate);
	}
	
	public JSONObject getFlakiness(HttpServletRequest request) {
		String tsIdStr = request.getParameter("tsid");
		int tsid = -1;
		if(tsIdStr != null && !tsIdStr.equals("undefined")) {
			tsid = Integer.valueOf(tsIdStr);
		}
		
		return getFlakiness(tsid);
	}
	
	public String getTodate(LocalDate today) {		 
	     DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
	     String toDate = today.format(formatter);
	     return toDate;
	}
	
	public String getFromdate(LocalDate today) {
		LocalDate sevenDaysPrior = today.minusDays(7);
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
       String fromDate = sevenDaysPrior.format(formatter);
       return fromDate;
	}
	
	public String getFromdateTime(LocalDate today) {
		LocalDate sevenDaysPrior = today.minusDays(7);
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        String fromDate = sevenDaysPrior.format(formatter);
        return fromDate;
	}
	
	public String getTodateTime(LocalDate today) {		 
	     DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
	     String toDate = today.format(formatter);
	     return toDate;
	}
	
	
	
	public JSONArray getTop5(int companyId, String from, String to) {
		
		JSONArray result = new JSONArray();
		String sql = "SELECT Created_By, count(*) as count FROM cta.test_case as tc "
				+ " join modules as mods on tc.Module = mods.idmodules "
				+ " join products as prods on prods.idproducts = mods.product "
				+ " join company as comp on comp.idcompany = prods.company "
				+ " where comp.idcompany = ? and "
				+ "Created_Date  BETWEEN  ? AND ? "
				+ "group by Created_By order by count desc limit 5";		
		try {
			PreparedStatement stmt = testCon.prepareStatement(sql);
			stmt.setInt(1, companyId);
			stmt.setString(2, from );
			stmt.setString(3, to);
			ResultSet rs = stmt.executeQuery();
			while(rs.next()) {
//				JSONObject res = new JSONObject();
//				res.put("author", rs.getString("Created_By"));
//				res.put("value", rs.getInt("count"));
//				result.add(res);
				JSONObject res = new JSONObject();
				if(rs.getString("Created_By") == null) {
					res.put("author", "");
				} else {
					res.put("author", rs.getString("Created_By"));
				}
				res.put("value", rs.getInt("count"));
				result.add(res);
			}
			rs.close();
			stmt.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return result;
	}
	
	public JSONArray getLast5Suites(int companyId, String from, String to) {
		
		JSONArray result = new JSONArray();
		String sql = "SELECT idtest_suite_results, Test_Suite_Id, Executed_Date,"
				+ "    ts.Test_Suite as Test_Suite, ts.productid as prodid "
				+ "    FROM cta.test_suite_results "
				+ " JOIN cta.test_suite as ts on ts.idtest_suite = Test_Suite_Id "
				+ " JOIN cta.products as prods on prods.idproducts = ts.productid "
				+ " JOIN cta.company as comp on comp.idcompany = prods.company "
				+ "WHERE comp.idcompany = ? AND "
				+ "(Test_Suite_Id, Executed_Date) IN ( "
				+ "   SELECT Test_Suite_Id,MAX(Executed_Date) "
				+ "   FROM cta.test_suite_results GROUP BY Test_Suite_Id) "
				+ "ORDER BY Executed_Date DESC LIMIT 5";		
		try {
			PreparedStatement stmt = testCon.prepareStatement(sql);
			stmt.setInt(1, companyId);
			ResultSet rs = stmt.executeQuery();
			
			while(rs.next()) {
				int tsid = rs.getInt("Test_Suite_Id");
				String sqlDetails = "select * from cta.test_suite_results "
						+ "where Test_Suite_Id = ? order by "
						+ "idtest_suite_results desc Limit 5";
				stmt = testCon.prepareStatement(sqlDetails);
				stmt.setInt(1, tsid);
				ResultSet rs2 = stmt.executeQuery();
				String tsname = rs.getString("Test_Suite");
				JSONObject res = new JSONObject();					
				res.put("testSuiteName", tsname);
				JSONArray runResults = new JSONArray();
				while(rs2.next()) {
					JSONObject runObj = new JSONObject();
					runObj.put("type", rs2.getString("Status"));
					runObj.put("total", rs2.getInt("totalCount"));
					runObj.put("data", rs2.getInt("passCount"));
					runObj.put("tsid", rs2.getInt("Test_Suite_Id"));
					runObj.put("prodid", rs.getInt("prodid"));
					runResults.add(runObj);
				}
				res.put("last5Run", runResults);
				result.add(res);
				rs2.close();
			}
			rs.close();
			stmt.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return result;
	}
	
	public JSONArray getAuthors(int companyId) {
		
		JSONArray result = new JSONArray();
		String sql = " SELECT username FROM cta.userprofile "
				+ " where company = ? order by username asc";
		try {
			PreparedStatement stmt = testCon.prepareStatement(sql);
			stmt.setInt(1, companyId);
			ResultSet rs = stmt.executeQuery();
			while(rs.next()) {
				JSONObject res = new JSONObject();
				String un = rs.getString("username");
				res.put("label", un);
				res.put("value", un);
				result.add(res);
			}
			rs.close();
			stmt.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return result;		
	}
	
	public JSONArray getCountByAuthor(int companyId, String author, String fromDate, 
			String toDate) {
		
		JSONArray result = new JSONArray();
		String sql = " SELECT DATE(created_date) AS date, COUNT(*) AS count "
				+ "FROM cta.test_case "
				+ "WHERE Created_By = ? and Created_Date  BETWEEN  ? AND ? "
				+ "GROUP BY DATE(created_date);";
		try {
			PreparedStatement stmt = testCon.prepareStatement(sql);
			stmt.setString(1, author);
			stmt.setString(2, fromDate);
			stmt.setString(3, toDate);
			ResultSet rs = stmt.executeQuery();
			while(rs.next()) {
				JSONObject res = new JSONObject();
				res.put("day", rs.getString("date"));
				res.put("pass", rs.getInt("count"));
				res.put("fail", 0);
				result.add(res);
			}
			rs.close();
			stmt.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return result;		
	}
	
	public void updateDashBoardForStatus(int companyid, String Status) {
		
		String sql = "";
		if(Status.equalsIgnoreCase("RUNNING")) {
			return;
		}
		
		if(Status.equalsIgnoreCase("STARTED")) {
			sql = "UPDATE cta.dashboard set "
				+ "testCasesExecution = COALESCE(testCasesExecution, 0) + 1 "
				+ " where companyid = ?" ;
		} else if (Status.equalsIgnoreCase("PASS")) {
			sql = "UPDATE cta.dashboard set "
				+ "testCasesPassed = COALESCE(testCasesPassed, 0) + 1 "
				+ " where companyid = ?" ;
		} else if (Status.equalsIgnoreCase("FAIL") || Status.equalsIgnoreCase("FAILED")) {
			sql = "UPDATE cta.dashboard set "
				+ "testCasesFailed = COALESCE(testCasesFailed, 0) + 1 "
				+ " where companyid = ?" ;
		} else if (Status.equalsIgnoreCase("ABEND") || Status.equalsIgnoreCase("ABORTED")) {
			sql = "UPDATE cta.dashboard set "
				+ "testCasesAborted = COALESCE(testCasesAborted, 0) + 1 "
				+ " where companyid = ?" ;
		} else if (Status.equalsIgnoreCase("STOPP") || Status.equalsIgnoreCase("STOPPED")) {
			sql = "UPDATE cta.dashboard set "
				+ "testCasesStopped = COALESCE(testCasesStopped, 0) + 1 "
				+ " where companyid = ?" ;
		}
		
		try {
			PreparedStatement stmt = testCon.prepareStatement(sql);
			stmt.setInt(1, companyid);
			stmt.execute();
			stmt.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}
	
	public void updateDashBoardForTSStatus(int tsrid, String Status) {
		String sql_findcompany = "select idcompany from cta.company as comp "
				+ "join cta.products as prods on prods.company = comp.idcompany "
				+ "join cta.test_suite as ts on ts.productid = prods.idproducts "
				+ "join cta.test_suite_results as tsr on tsr.Test_Suite_Id = ts.idtest_suite "
				+ "where tsr.idtest_suite_results = ?";
		String sql = "";
		if(Status.equalsIgnoreCase("RUNNING")) {
			return;
		}
		
		if(Status.equalsIgnoreCase("STARTED") ||
				Status.equalsIgnoreCase("PASS")) {
		
			if(Status.equalsIgnoreCase("STARTED")) {
				sql = "UPDATE cta.dashboard set "
					+ "totalSuiteRuns = COALESCE(totalSuiteRuns, 0) + 1 "
					+ " where companyid = ?" ;
			} else if (Status.equalsIgnoreCase("PASS")) {
				sql = "UPDATE cta.dashboard set "
					+ "suitesPassCount = COALESCE(suitesPassCount, 0) + 1 "
					+ " where companyid = ?" ;
			}
			
			try {
				PreparedStatement stmt = testCon.prepareStatement(sql_findcompany);
				stmt.setInt(1, tsrid);
				ResultSet rs = stmt.executeQuery();
				if(rs.next()) {
					int companyid = rs.getInt("idcompany");
					stmt = testCon.prepareStatement(sql);
					stmt.setInt(1, companyid);
					stmt.execute();
				}
				rs.close();
				stmt.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
	}
	
	public void updateDashBoardForTestStepStatus(int tsid, String Status) {
		String sql_findcompany = "select idcompany from cta.company as comp "
				+ "join cta.products as prods on prods.company = comp.idcompany "
				+ "join cta.modules as mods on mods.product = prods.idproducts "
				+ "join cta.test_case as tc on tc.Module = mods.idmodules "
				+ "join cta.test_step as ts on ts.Test_Case_Id = tc.idtest_case "
				+ "where ts.idtest_step = ?";
		String sql = "";
		
		if (Status.equalsIgnoreCase("PASS")) {
			sql = "UPDATE cta.dashboard set "
				+ "stepsPassCount = COALESCE(stepsPassCount, 0) + 1 "
				+ " where companyid = ?" ;
		}
		
		try {
			PreparedStatement stmt = testCon.prepareStatement(sql_findcompany);
			stmt.setInt(1, tsid);
			ResultSet rs = stmt.executeQuery();
			if(rs.next()) {
				int companyid = rs.getInt("idcompany");
				stmt = testCon.prepareStatement(sql);
				stmt.setInt(1, companyid);
				stmt.execute();
			}
			rs.close();
			stmt.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}
	
	public void updateDashBoardForTCCount(int tcid) {
		
		String sql_findCompany = "select idcompany from cta.company as comp "
				+ "join cta.products as prods on prods.company = comp.idcompany "
				+ "join cta.modules as mods on mods.product = prods.idproducts "
				+ "join cta.test_case as tc on tc.Module = mods.idmodules "
				+ "where tc.idtest_case = ?";
		
		try {
			PreparedStatement stmt = testCon.prepareStatement(sql_findCompany);
			stmt.setInt(1, tcid);
			ResultSet rs = stmt.executeQuery();
			if(rs.next()) {
				int companyid = rs.getInt("idcompany");
				
				String updateTCCount = "update cta.dashboard set testCaseCount = COALESCE(testCaseCount, 0) + 1 "
						+ "where companyid = ?";
				stmt = testCon.prepareStatement(updateTCCount);
				stmt.setInt(1, companyid);
				stmt.execute();
			}
			rs.close();
			stmt.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}
	
	public void updateDashBoardForTestStepCount(int tsid) {
		
		String sql_findCompany = "select idcompany from cta.company as comp "
				+ "join cta.products as prods on prods.company = comp.idcompany "
				+ "join cta.modules as mods on mods.product = prods.idproducts "
				+ "join cta.test_case as tc on tc.Module = mods.idmodules "
				+ "join cta.test_step as ts on ts.Test_Case_Id = tc.idtest_case "
				+ "where ts.idtest_step = ?";
		
		try {
			PreparedStatement stmt = testCon.prepareStatement(sql_findCompany);
			stmt.setInt(1, tsid);
			ResultSet rs = stmt.executeQuery();
			if(rs.next()) {
				int companyid = rs.getInt("idcompany");
				
				String updateTestStepCount = "update cta.dashboard set testStepsCount = COALESCE(testStepsCount, 0) + 1 "
						+ "where companyid = ?";
				stmt = testCon.prepareStatement(updateTestStepCount);
				stmt.setInt(1, companyid);
				stmt.execute();
			}
			rs.close();
			stmt.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}
	
	public void updateDashBoardForTSCount(int cid) {
		
		String sql = "UPDATE cta.dashboard SET testSuiteCount = COALESCE(testSuiteCount, 0) + 1 WHERE companyid = ?";
		
		try {
			PreparedStatement stmt = testCon.prepareStatement(sql);
			stmt.setInt(1, cid);
			stmt.execute();
			stmt.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}
	
	public JSONArray getTopCards(int companyId, String fromDate, String toDate) {
		JSONArray result = new JSONArray();
		String sql = "SELECT * FROM cta.dashboard where companyid = ?";
		try {
			PreparedStatement stmt = testCon.prepareStatement(sql);
			stmt.setInt(1, companyId);
			ResultSet rs = stmt.executeQuery();
			while(rs.next()) {
				JSONObject res = new JSONObject();
				res.put("authored", rs.getInt("testCaseCount"));
				res.put("executed", rs.getInt("testCasesExecution"));
				res.put("pass", rs.getInt("testCasesPassed"));
				res.put("fail", rs.getInt("testCasesFailed"));
				res.put("aborted", rs.getInt("testCasesAborted"));
				res.put("stopped", rs.getInt("testCasesStopped"));
				result.add(res);
			}
			rs.close();
			stmt.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return result;
	}
	
	public JSONArray getExecutionThreadChart(int companyId, String fromDateTime, String toDateTime) {
		JSONArray result = new JSONArray();
		String sql = "SELECT test_case_name, Executed_Date, Duration, tcr.Status FROM cta.test_case_results as tcr "
				+ "join cta.test_case as tc on tc.idtest_case = tcr.Test_Case_Id "
				+ "join cta.modules as mods on mods.idmodules = tc.Module "
				+ "join cta.products as prods on prods.idproducts = mods.product "
				+ "join cta.company as comp on comp.idcompany = prods.company "
				+ "where comp.idcompany = ? and  Executed_Date  BETWEEN  ? AND ? "
				+ "order by Executed_Date asc";
		try {
			PreparedStatement stmt = testCon.prepareStatement(sql);
			stmt.setInt(1, companyId);
			stmt.setString(2, fromDateTime);
			stmt.setString(3, toDateTime);
			ResultSet rs = stmt.executeQuery();
			JSONArray threadArray = new JSONArray();
			while(rs.next()) {
				Timestamp executedDate = rs.getTimestamp("Executed_Date");
				double duration = rs.getDouble("Duration");
				Timestamp executedDateEnd = getExecutionEnd(executedDate, duration);
				boolean slotFound = false;
				
				for(int i=0;i<threadArray.size(); i++) {
					JSONArray threada = (JSONArray)threadArray.get(i);
					JSONObject job = (JSONObject)threada.get(threada.size() - 1);
					JSONArray startEnd = (JSONArray)job.get("y");
					long endInMilli = (long)startEnd.get(1);
					if(endInMilli <= executedDate.getTime()) {
						slotFound = true;
						String threadName = (String)job.get("x");
						JSONObject jobNew = createJob(threadName, executedDate, executedDateEnd, rs);
						threada.add(jobNew);
						result.add(jobNew);
						break;
					}
				}
				
				if(!slotFound) {
					JSONArray threada = new JSONArray();
					JSONObject job = createJob("Thread" + (threadArray.size() + 1), executedDate, executedDateEnd, rs);
					threada.add(job);
					result.add(job);
					threadArray.add(threada);
				}
			}
			
			
			rs.close();
			stmt.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return result;
	}
	
	public JSONObject createJob(String threadName, Timestamp executedDate, Timestamp executedDateEnd,
			ResultSet rs) throws Exception{
		JSONObject job = new JSONObject();
		job.put("x", threadName);
		JSONArray startEnd = new JSONArray();
		startEnd.add(executedDate.getTime());
		startEnd.add(executedDateEnd.getTime());
		job.put("y", startEnd);
		job.put("label", rs.getString("test_case_name"));
		return job;
	}
	
	public Timestamp getExecutionEnd(Timestamp executedDate, double duration) {
		// Convert double to seconds and milliseconds
        long durationSeconds = (long) duration;
        long durationMilliseconds = (long) ((duration - durationSeconds) * 1000);
        
     // Use Calendar to add duration
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(executedDate);
        calendar.add(Calendar.SECOND, (int) durationSeconds); // Add seconds
        calendar.add(Calendar.MILLISECOND, (int) durationMilliseconds); // Add milliseconds

        // Get the new Timestamp
        Timestamp newTimestamp = new Timestamp(calendar.getTimeInMillis());
        
        return newTimestamp;
	}
	
	public void updateFlakiness(int tsid) {
		
		String sql = "SELECT * FROM cta.test_suite_results "
				+ "WHERE Test_Suite_Id = ? AND runtype = 'undefined' AND "
				+ "(Status = 'FAIL' or Status = 'PASS') order by idtest_suite_results "
				+ "desc LIMIT 2";
		
		try {
			PreparedStatement stmt = testCon.prepareStatement(sql);
			stmt.setInt(1, tsid);
			ResultSet rs = stmt.executeQuery();
			HashMap failMap = new HashMap();
			int tssize = 0;
			boolean calSize = true;
			int latesttsrid = -1;
			Set <Integer> latest = null;
			Set <Integer> previous = null;
			while(rs.next()) {
				int tsrid = rs.getInt("idtest_suite_results");
				String sql2 = "SELECT idtest_suite_case_results, test_suite_results_id, "
						+ "test_case_results_id, tcr.Status, tcr.Test_Case_Id "
						+ "FROM cta.test_suite_case_results "  
						+ "JOIN cta.test_case_results as tcr on "
						+ "tcr.idtest_case_results = test_case_results_id "
						+ "WHERE test_suite_results_id = ? "
						+ "ORDER BY idtest_suite_case_results ASC";
				stmt = testCon.prepareStatement(sql2);
				stmt.setInt(1, tsrid);
				ResultSet rs2 = stmt.executeQuery();
				Set <Integer> failedCase = new HashSet<Integer>();
				while(rs2.next()) {
					if(calSize) {
						tssize++;
						latesttsrid = tsrid;
					}
					int tcid = rs2.getInt("Test_Case_Id");
					String status = rs2.getString("Status");
					if(status != null && status.equals("FAIL")) {
						failedCase.add(tcid);
					} else if(status != null && status.equals("PASS")) {
						failedCase.remove(tcid);
					}
				}
				calSize = false; 
				failMap.put(tsrid, failedCase);
				if(latest == null) {
					latest = failedCase;
				} else {
					previous = failedCase;
				}
				rs2.close();
			}
			
			int flakinessCounter = 0;
			
			Iterator latestIter = latest.iterator();
			
			while(latestIter.hasNext()) {
				int failedTest = (int)latestIter.next();
				if(!previous.contains(failedTest)) {
					flakinessCounter = flakinessCounter + 1;
				}
			}
			
			double flakiness = ((double)flakinessCounter / tssize) * 100.0;
			
//			Iterator iter = failMap.keySet().iterator();
//			Set<Integer> commonElements = null;
//			Set<Integer> allUniqueElements = null;
//			HashMap stability = new HashMap();
//			boolean first = true;
//			while(iter.hasNext()) {
//				Set failedCase = (Set)failMap.get(iter.next());
//				int pass_age = failedCase.size() / tssize;
////				stability.put(iter.next(), pass_age);
//				if(first) {
//					commonElements = new HashSet<>(failedCase);
//					allUniqueElements = new HashSet<>(failedCase);
//					first = false;
//				} else {
//					commonElements.retainAll(failedCase);
//					allUniqueElements.addAll(failedCase);
//				}				
//			}
//			Set<Integer> nonCommonElements = new HashSet<>(allUniqueElements);
//	        nonCommonElements.removeAll(commonElements);
//	        double flakiness = nonCommonElements.size() / (nonCommonElements.size() + commonElements.size());
	        String sql3 = "UPDATE cta.test_suite set flakiness = ?";
        	stmt = testCon.prepareStatement(sql3);
        	stmt.setDouble(1, flakiness);
        	stmt.execute();
	        stmt.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public JSONObject getFlakiness(int tsid) {
		JSONObject result = new JSONObject();
		String sql = "SELECT flakiness FROm cta.test_suite "
				+ "WHERE idtest_suite = ?";
		
		try {
			PreparedStatement stmt = testCon.prepareStatement(sql);
			stmt.setInt(1, tsid);
			ResultSet rs = stmt.executeQuery();
			double flakiness = 0.0;
			if(rs.next()) {
				flakiness = rs.getDouble("flakiness");
			}
			
			result.put("flakiness", flakiness);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return result;
	}
	
	public void summarize(int cid) {
		String sql = "SELECT * cta.dashboard WHERE companyid = ?";
		try {
			PreparedStatement stmt = testCon.prepareStatement(sql);
			stmt.setInt(1, cid);
			ResultSet rs = stmt.executeQuery();
			String isSummarized = "";
			if(rs.next()) {
				isSummarized = rs.getString("isSummarized");
				if(isSummarized.equalsIgnoreCase("false")) {
					summarizeTCCount(cid);
					summarizeTSCount(cid);
					summarizeTCStatus(cid);
					summarizeTSStatus(cid);
					updateIsSummarized(cid);
				}
			}
			rs.close();
			stmt.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void updateIsSummarized(int cid) {
		String sql = "UPDATE cta.dashboard SET isSummarized = ? WHERE companyid = ?";
		try {
			PreparedStatement stmt = testCon.prepareStatement(sql);
			stmt.setString(1, "true");
			stmt.setInt(2, cid);
			stmt.executeUpdate();
			stmt.close();
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	public void summarizeTCCount(int cid) {
		String sql = "SELECT COUNT(*) FROM cta.test_case AS tc "
				+ "join cta.modules as mods on mods.idmodules = tc.Module "
				+ "join cta.products as prods on prods.idproducts = mods.product "
				+ "join cta.company as comp on comp.idcompany = prods.company "
				+ "where comp.idcompany = ?";
		String sql_update = "UPDATE cta.dashboard SET testCaseCount = ? WHERE companyid = ?";
		int count = -1;
		try {
			PreparedStatement stmt = testCon.prepareStatement(sql);
			stmt.setInt(1, cid);
			ResultSet rs = stmt.executeQuery();
			if(rs.next()) {
				count = rs.getInt(1);
				stmt = testCon.prepareStatement(sql_update);
				stmt.setInt(1, count);
				stmt.setInt(2, cid);
				stmt.executeUpdate();
			}
			rs.close();
			stmt.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void summarizeTSCount(int cid) {
		String sql = "SELECT COUNT(*) FROM cta.test_suite AS ts "
				+ "join cta.products as prods on prods.idproducts = ts.productid "
				+ "join cta.company as comp on comp.idcompany = prods.company "
				+ "where comp.idcompany = ?";
		String sql_update = "UPDATE cta.dashboard SET testSuiteCount = ? WHERE companyid = ?";
		int count = -1;
		try {
			PreparedStatement stmt = testCon.prepareStatement(sql);
			stmt.setInt(1, cid);
			ResultSet rs = stmt.executeQuery();
			if(rs.next()) {
				count = rs.getInt(1);
				stmt = testCon.prepareStatement(sql_update);
				stmt.setInt(1, count);
				stmt.setInt(2, cid);
				stmt.executeUpdate();
			}
			rs.close();
			stmt.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void summarizeTSStatus(int cid) {
		String sql_pass = "SELECT COUNT(*) FROM cta.test_suite_results AS tsr "
				+ "join cta.test_suite as ts on ts.idtest_suite = tsr.Test_Suite_Id "
				+ "join cta.products as prods on prods.idproducts = ts.productid "
				+ "join cta.company as comp on comp.idcompany = prods.company "
				+ "where comp.idcompany = ? and tsr.Status = ?";
		String sql = "SELECT COUNT(*) FROM cta.test_suite_results AS tsr "
				+ "join cta.test_suite as ts on ts.idtest_suite = tsr.Test_Suite_Id "
				+ "join cta.products as prods on prods.idproducts = ts.productid "
				+ "join cta.company as comp on comp.idcompany = prods.company "
				+ "where comp.idcompany = ?";
		String sql_updatepass = "UPDATE cta.dashboard SET suitesPassCount = ? WHERE companyid = ?";
		String sql_update = "UPDATE cta.dashboard SET totalSuiteRuns = ? WHERE companyid = ?";
		int passcount = -1;
		int count = -1;
		try {
			PreparedStatement stmt = testCon.prepareStatement(sql);
			stmt.setInt(1, cid);
			ResultSet rs = stmt.executeQuery();
			if(rs.next()) {
				count = rs.getInt(1);
				stmt = testCon.prepareStatement(sql_update);
				stmt.setInt(1, count);
				stmt.setInt(2, cid);
				stmt.executeUpdate();
			}
			stmt = testCon.prepareStatement(sql_pass);
			stmt.setInt(1, cid);
			stmt.setString(2, "PASS");
			ResultSet rs1 = stmt.executeQuery();
			if(rs1.next()) {
				passcount = rs1.getInt(1);
				stmt = testCon.prepareStatement(sql_updatepass);
				stmt.setInt(1, passcount);
				stmt.setInt(2, cid);
				stmt.executeUpdate();
			}
			rs.close();
			rs1.close();
			stmt.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void summarizeTCStatus(int cid) {
		String sql_pass = "SELECT COUNT(*) FROM cta.test_case_results AS tcr "
				+ "join cta.test_case as tc on tc.idtest_case = tcr.Test_Case_Id "
				+ "join cta.modules as mods on mods.idmodules = tc.Module "
				+ "join cta.products as prods on prods.idproducts = mods.product "
				+ "join cta.company as comp on comp.idcompany = prods.company "
				+ "where comp.idcompany = ? and tsr.Status = ?";
		String sql_fail = "SELECT COUNT(*) FROM cta.test_case_results AS tcr "
				+ "join cta.test_case as tc on tc.idtest_case = tcr.Test_Case_Id "
				+ "join cta.modules as mods on mods.idmodules = tc.Module "
				+ "join cta.products as prods on prods.idproducts = mods.product "
				+ "join cta.company as comp on comp.idcompany = prods.company "
				+ "where comp.idcompany = ? and tsr.Status IN (?, ?)";
		String sql_abort = "SELECT COUNT(*) FROM cta.test_case_results AS tcr "
				+ "join cta.test_case as tc on tc.idtest_case = tcr.Test_Case_Id "
				+ "join cta.modules as mods on mods.idmodules = tc.Module "
				+ "join cta.products as prods on prods.idproducts = mods.product "
				+ "join cta.company as comp on comp.idcompany = prods.company "
				+ "where comp.idcompany = ? and tsr.Status IN (?, ?)";
		String sql_stop = "SELECT COUNT(*) FROM cta.test_case_results AS tcr "
				+ "join cta.test_case as tc on tc.idtest_case = tcr.Test_Case_Id "
				+ "join cta.modules as mods on mods.idmodules = tc.Module "
				+ "join cta.products as prods on prods.idproducts = mods.product "
				+ "join cta.company as comp on comp.idcompany = prods.company "
				+ "where comp.idcompany = ? and tsr.Status IN (?, ?)";
		String sql = "SELECT COUNT(*) FROM cta.test_case_results AS tcr "
				+ "join cta.test_case as tc on tc.idtest_case = tcr.Test_Case_Id "
				+ "join cta.modules as mods on mods.idmodules = tc.Module "
				+ "join cta.products as prods on prods.idproducts = mods.product "
				+ "join cta.company as comp on comp.idcompany = prods.company "
				+ "where comp.idcompany = ?";
		String sql_updatepass = "UPDATE cta.dashboard SET testCasesPassed = ? WHERE companyid = ?";
		String sql_updatefail = "UPDATE cta.dashboard SET testCasesFailed = ? WHERE companyid = ?";
		String sql_updateabort = "UPDATE cta.dashboard SET testCasesAborted = ? WHERE companyid = ?";
		String sql_updatestop = "UPDATE cta.dashboard SET testCasesStopped = ? WHERE companyid = ?";
		String sql_update = "UPDATE cta.dashboard SET testCasesExecution = ? WHERE companyid = ?";
		int passcount = -1;
		int failcount = -1;
		int abortcount = -1;
		int stopcount = -1;
		int count = -1;
		try {
			PreparedStatement stmt = testCon.prepareStatement(sql);
			stmt.setInt(1, cid);
			ResultSet rs = stmt.executeQuery();
			if(rs.next()) {
				count = rs.getInt(1);
				stmt = testCon.prepareStatement(sql_update);
				stmt.setInt(1, count);
				stmt.setInt(2, cid);
				stmt.executeUpdate();
			}
			stmt = testCon.prepareStatement(sql_pass);
			stmt.setInt(1, cid);
			stmt.setString(2, "PASS");
			ResultSet rs1 = stmt.executeQuery();
			if(rs1.next()) {
				passcount = rs1.getInt(1);
				stmt = testCon.prepareStatement(sql_updatepass);
				stmt.setInt(1, passcount);
				stmt.setInt(2, cid);
				stmt.executeUpdate();
			}
			stmt = testCon.prepareStatement(sql_fail);
			stmt.setInt(1, cid);
			stmt.setString(2, "FAIL");
			stmt.setString(3, "FAILED");
			ResultSet rs2 = stmt.executeQuery();
			if(rs2.next()) {
				failcount = rs2.getInt(1);
				stmt = testCon.prepareStatement(sql_updatefail);
				stmt.setInt(1, failcount);
				stmt.setInt(2, cid);
				stmt.executeUpdate();
			}
			stmt = testCon.prepareStatement(sql_abort);
			stmt.setInt(1, cid);
			stmt.setString(2, "ABORTED");
			stmt.setString(3, "ABEND");
			ResultSet rs3 = stmt.executeQuery();
			if(rs3.next()) {
				abortcount = rs3.getInt(1);
				stmt = testCon.prepareStatement(sql_updateabort);
				stmt.setInt(1, abortcount);
				stmt.setInt(2, cid);
				stmt.executeUpdate();
			}
			stmt = testCon.prepareStatement(sql_stop);
			stmt.setInt(1, cid);
			stmt.setString(2, "STOPP");
			stmt.setString(3, "STOPPED");
			ResultSet rs4 = stmt.executeQuery();
			if(rs4.next()) {
				stopcount = rs4.getInt(1);
				stmt = testCon.prepareStatement(sql_updatestop);
				stmt.setInt(1, stopcount);
				stmt.setInt(2, cid);
				stmt.executeUpdate();
			}
			rs.close();
			rs1.close();
			rs2.close();
			rs3.close();
			rs4.close();
			stmt.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
