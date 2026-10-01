package nogrunt;

import nogrunt.integrations.*;
import nogrunt.exceptions.*;
import nogrunt.codegen.*;

import java.sql.Connection;
import java.sql.Date;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.text.ParseException;
import java.util.Calendar;
import java.sql.Time;
//import java.time.format.TextStyle;
import java.util.Locale;
import java.sql.ResultSet;
import java.net.URLEncoder;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.net.HttpURLConnection;
import java.net.URL;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Map;
import java.util.HashMap;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.LocalTime;

import org.apache.poi.common.usermodel.HyperlinkType;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import jakarta.servlet.http.HttpServletResponse;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.sql.DataSource;
import javax.naming.NamingException;

import java.util.zip.ZipOutputStream;
import java.util.zip.ZipEntry;

import java.io.*;

public class MySqlConn2 {
	
	public Connection testCon2;
	MySQlConn msc ;
//	MySQlConn msc = new MySQlConn();
	public int totalPages=-1;
	private int initialClickCount = 0;
	
	public MySqlConn2(MySQlConn m) {
		
		if(m == null || m.testCon == null) {
			getDbConn();
			msc = new MySQlConn(testCon2);
		} else {
			testCon2 = m.testCon;
			msc = m;
		}
		
		try {
		    // Attempt to use the connection for some operation
		    testCon2.prepareStatement("SELECT 1").executeQuery();
		} catch (SQLException e) {
	        System.out.println("Connection in MSC2 is closed - create new");
	        getDbConn();
		}
	}
	
	public void getDbConn() {
		try {

			if(AppProperties.poolingenabled != null && AppProperties.poolingenabled.equals("true")) {
				Context initContext = new InitialContext();
		        Context envContext  = (Context) initContext.lookup("java:/comp/env");
		        DataSource ds = (DataSource) envContext.lookup("jdbc/cta");
		        testCon2 = ds.getConnection();
//		        System.out.println("------------------------------------------------------------------------------");
//		        printCallStack(4);
//		        System.out.println("Created Connection MSC2: " + testCon2.hashCode());
		        
		     // Cast to Tomcat's DataSource
//	            org.apache.tomcat.jdbc.pool.DataSource tomcatDataSource = (org.apache.tomcat.jdbc.pool.DataSource) ds;

	            // Now you can get various stats
//	            System.out.println("Active Connections MSC2: " + tomcatDataSource.getActive());
//	            System.out.println("Idle Connections MSC2: " + tomcatDataSource.getIdle());
//	            System.out.println("------------------------------------------------------------------------------");
			
			} else {
	            // Step 1: Load the JDBC driver
	            Class.forName(AppProperties.databaseDriver);
	
	            // Step 2: Establish the connection
	            String url = AppProperties.databaseUrl;
	            String username = AppProperties.databaseUsername;
	            String password = AppProperties.databasePassword;
	            testCon2 = DriverManager.getConnection(url, username, password); 
			}
        } catch (SQLException | ClassNotFoundException  e) {
        	System.err.println(Utilities.getNow());
            e.printStackTrace();
        } catch (NamingException  e) {
        	System.err.println(Utilities.getNow());
            e.printStackTrace();
        }
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
//            System.out.println("Active Connections before close MSC2: " + tomcatDataSource.getActive());
//            System.out.println("Idle Connections before close MSC2 : " + tomcatDataSource.getIdle());
//			// Step 6: Close the connection
//            printCallStack(3);
//            System.out.println("closing Connection MSC2: " + testCon2.hashCode());
			testCon2.close();
			
//			initContext = new InitialContext();
//	        envContext  = (Context) initContext.lookup("java:/comp/env");
//	        ds = (DataSource) envContext.lookup("jdbc/cta");
//			
//			// Cast to Tomcat's DataSource
//            tomcatDataSource = (org.apache.tomcat.jdbc.pool.DataSource) ds;

            // Now you can get various stats
//            System.out.println("Active Connections after close MSC2: " + tomcatDataSource.getActive());
//            System.out.println("Idle Connections after close MSC2: " + tomcatDataSource.getIdle());
//            System.out.println("------------------------------------------------------------------------------");
            
            msc.closeDbConn();
		
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
	    for (int j=0; j < 10 && j < stackTrace.length ; j++) {
	        System.out.println("call stack MSC2: " + stackTrace[j].toString());
	    }
	}
	
	 public void markJobAsExecuted(int scheduleid, JSONObject schedule) {
		 Calendar cal = Calendar.getInstance();
	    	Date date = Utilities.getDate(cal);
	    	Time time = Utilities.getTime(cal);
         // Insert a record into the executed jobs table
		 try {
	         String insertSql = "INSERT INTO cta.scheduler_runs (scheduleid, scheduledFrequency,scheduledsubfrequency,"
	         		+ "scheduleddate, scheduledtime,executeddate,executedtime) VALUES (?,?,?,?,?,?,?)";
	         PreparedStatement insertStatement = testCon2.prepareStatement(insertSql);
	         insertStatement.setInt(1, scheduleid);
	         insertStatement.setString(2, (String)schedule.get("frequency"));
	         insertStatement.setString(3, (String)schedule.get("subfrequency"));
	         if(schedule.get("scheduledate") == null) {
	        	 insertStatement.setDate(4, (Date)schedule.get("scheduledate"));
	         } else {
	        	 String dateString = (String)schedule.get("scheduledate"); // Example date string
	             String pattern = "yyyy-MM-dd"; // The pattern that matches the date format
	             DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern);
	             LocalDate scheduleDate = LocalDate.parse(dateString, formatter);
	             Date sqlDate = Date.valueOf(scheduleDate);
	             insertStatement.setDate(4, sqlDate);
	         }
	         
	         if(schedule.get("scheduletime") == null) {
	        	 insertStatement.setTime(5, (Time)schedule.get("scheduletime"));
	         } else {
	        	 String timeString = (String)schedule.get("scheduletime"); // Example time string
	             String pattern = "HH:mm:ss"; // The pattern that matches the time format
	             DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern);
	             LocalTime scheduleTime = LocalTime.parse(timeString, formatter);
	             Time sqlTime = Time.valueOf(scheduleTime);
	             insertStatement.setTime(5, sqlTime);
	         }
	         insertStatement.setDate(6, date);
	         insertStatement.setTime(7, time);
	         insertStatement.executeUpdate();
	         insertStatement.close();
		 }catch (Exception e) {
			 System.err.println(Utilities.getNow());
			 e.printStackTrace();
		 }
     }
	 
	 public JSONArray getOverdueJobs(ExecutionLogger el) {
		 
	        Calendar cal = Calendar.getInstance();
	    	Date date = Utilities.getDate(cal);
	    	Time time = Utilities.getTime(cal);
	    	
	    	SimpleDateFormat sdfDayOfWeek = new SimpleDateFormat("EEEE");
	    	String currentDayOfWeek = sdfDayOfWeek.format(cal.getTime());

         // Create a LocalDateTime object for the current date and time
         JSONArray jArray = new JSONArray();
         
         try {           
             String sql = "SELECT * FROM cta.scheduler WHERE scheduletime <= ? AND"
             		+ " status = 0 AND (scheduledate = ? OR (frequency = 'daily') OR "
             		+ "(frequency = 'weekly' AND subfrequency like '%" + currentDayOfWeek + "%'))"
             				+ " AND idscheduler NOT IN ("
             				+ "SELECT scheduleid FROM cta.scheduler_runs where executeddate = ? and  executedtime <= ?)";

             // Create a prepared statement
             PreparedStatement preparedStatement = testCon2.prepareStatement(sql);
             preparedStatement.setTime(1, time); 
             preparedStatement.setDate(2, date);
             preparedStatement.setDate(3, date);
             preparedStatement.setTime(4, time);              

             // Execute the query
             ResultSet resultSet = preparedStatement.executeQuery();
             while (resultSet.next()) {
            	 JSONObject jObj = MySQlConn.getSchedulerAsJSON(resultSet);
                 jArray.add(jObj);                 
             }
             
             if(jArray.size() == 0) {
            	 sql = "SELECT scheduleid FROM cta.scheduler_runs where executeddate = ? and  executedtime <= ?";
            	 preparedStatement = testCon2.prepareStatement(sql);
            	 preparedStatement.setDate(1, date);
                 preparedStatement.setTime(2, time); 
                 resultSet = preparedStatement.executeQuery();
                 String found = " ";
                 while(resultSet.next()) {
                	 found = resultSet.getInt(1) + found ;
                 }
//                 el.logExecution("------------------------------------------------------------------------");
//                 el.logExecution("Scheduler runs query being run " + sql);
//                 el.logExecution("executeddate =  " + date + "--" + "ExecutedTime = " + time);
//                 el.logExecution("Already runs schedules " + "--" + found);
                 
                 sql = "SELECT * FROM cta.scheduler WHERE scheduletime <= ? AND"
                  		+ " status = 0 AND (scheduledate = ? OR (frequency = 'daily') OR "
                 		+ "(frequency = 'weekly' AND subfrequency like '%" + currentDayOfWeek + "%'))";
                 preparedStatement = testCon2.prepareStatement(sql);
                 preparedStatement.setTime(1, time); 
                 preparedStatement.setDate(2, date);
                 resultSet = preparedStatement.executeQuery();
                 found = "-";
                 while(resultSet.next()) {
                	 found = resultSet.getInt(1) + found ;
                 }
                 
//                 el.logExecution("Schedules query being run " + sql);
//                 el.logExecution("scheduletime =  " + time + "--" + "scheduledate = " + date);
//                 el.logExecution("All schedules for today " + "--" + found);
//                 el.logExecution("------------------------------------------------------------------------");
             }

             resultSet.close();
             preparedStatement.close();
         } catch (SQLException e) {
        	 System.err.println(Utilities.getNow());
             e.printStackTrace();
         }
         return jArray;
	 }	 
	 
	 public JSONObject getpagedetails(int tcid) {
			String sql = "SELECT p.*, tcp.pageno, tcp.testcaseid FROM cta.testcase_page tcp JOIN cta.pages p "
					+ "ON tcp.idpage=p.pageid WHERE tcp.testcaseid = ? ORDER BY tcp.pageno";
//			String sql_edge = "SELECT * FROM cta.pagerelation WHERE productid=?";
			JSONObject json = new JSONObject();
			JSONArray nodes = new JSONArray();
			JSONArray edges = new JSONArray();
			try {
				PreparedStatement stmt =  testCon2.prepareStatement(sql);
				stmt.setInt(1,tcid);
				ResultSet rs = stmt.executeQuery();
				while(rs.next()) {
					JSONObject jsonStr = new JSONObject();
					JSONObject jsonEdge = new JSONObject();
					int id = rs.getInt("pageid");
					String pagename = rs.getString("pagename");
					int productid = rs.getInt("prodid");
					int totalelementonpage = rs.getInt("totalelementonpage");
					int usedelementsonpage = rs.getInt("usedelementonpage");
					String label = pagename + "\n" + usedelementsonpage + "/" + totalelementonpage + " elements covered";
					String screenshotpath = rs.getString("screenshotpath");
					int pageno = rs.getInt("pageno");
					int testcaseid = rs.getInt("testcaseid");
					jsonStr.put("id", id);
					jsonStr.put("pagename", pagename);
					jsonStr.put("prodid", productid);
					jsonStr.put("label", label);
					jsonStr.put("totalelementonpage", totalelementonpage);
					jsonStr.put("usedelementsonpage", usedelementsonpage);
					jsonStr.put("screenshotpath",screenshotpath);
					jsonStr.put("pageno", pageno);
					jsonStr.put("testcaseid", testcaseid);
					jsonEdge.put("from", pageno);
					jsonEdge.put("to", pageno+1);
					edges.add(jsonEdge);
					nodes.add(jsonStr);
				}
				
				rs.close();
				stmt.close();
			} catch (Exception e) {
				System.err.println(Utilities.getNow());
				e.printStackTrace();
			}
			totalPages = nodes.size();
			json.put("nodes", nodes);
			json.put("edges", edges);
			return json;
		}
	 
	 
	 public JSONObject getpagedetailsOnProd(int prodid, int company, String randomkey) {
			String sql = "SELECT * FROM cta.pages WHERE prodid=?";
			String clickable = "SELECT count(*) FROM cta.page_elements WHERE clickable=1 AND pageid=?";
			String usedElement = "SELECT count(*) FROM cta.page_elements WHERE coveragecount>=1 AND pageid=?";
			String sql_edge = "SELECT * FROM cta.pagerelation WHERE productid=?";
			JSONObject json = new JSONObject();
			JSONArray nodes = new JSONArray();
			JSONArray edges = new JSONArray();
			try {
				PreparedStatement stmt =  testCon2.prepareStatement(sql);
				stmt.setInt(1,prodid);
				ResultSet rs = stmt.executeQuery();
				int index=0;
				while(rs.next()) {
					JSONObject jsonStr = new JSONObject();
					JSONObject jsonEdge = new JSONObject();
					
					int id = rs.getInt("pageid");
					String lastCliclkedName = rs.getString("lastclicked_name");
					int totalClickable = 0;
					stmt = testCon2.prepareStatement(clickable);
					stmt.setInt(1, id);
					ResultSet rs2 = stmt.executeQuery();
					if(rs2.next()) {
						totalClickable = rs2.getInt(1);
					}
					int usedelementsonpage = 0;
					stmt = testCon2.prepareStatement(usedElement);
					stmt.setInt(1, id);
					rs2 = stmt.executeQuery();
					if(rs2.next()) {
						usedelementsonpage = rs2.getInt(1);
					}
					rs2.close();
					String pagename = rs.getString("pagename");
					int productid = rs.getInt("prodid");
					int totalelementonpage = rs.getInt("totalelementonpage");
					
					String label = pagename + "\n Last Clicked: " + lastCliclkedName + "\n" + usedelementsonpage + "/" + totalClickable + " clickable elements covered"
							+ "\n out of total "+ totalelementonpage +" elements on page \n";
					String screenshotpath = Utilities.getCoverageSSPath(company, prodid, id);
					
					File file = new File(screenshotpath);
					String url = null;
					if(file.exists() && file.isFile()) {
						url = AppProperties.fileurl +"?action=downloadCoverageFile&companyid="+company+"&token="+ randomkey;
						try {
						    String encodedPath = URLEncoder.encode(screenshotpath, "UTF-8");
						    url += "&fileName=" + encodedPath;
						} catch (UnsupportedEncodingException e) {
							System.err.println(Utilities.getNow());
						    e.printStackTrace();
						}
					}
					
					jsonStr.put("id", id);
					jsonStr.put("pageid", id);
					jsonStr.put("pagename", pagename);
					jsonStr.put("prodid", productid);
					jsonStr.put("label", label);
					jsonStr.put("totalelementonpage", totalelementonpage);
					jsonStr.put("usedelementsonpage", usedelementsonpage);
					jsonStr.put("screenshotpath",url);
					nodes.add(jsonStr);
				}
				stmt =  testCon2.prepareStatement(sql_edge);
				stmt.setInt(1,prodid);
				rs = stmt.executeQuery();
				while(rs.next()) {
					JSONObject jsonStr = new JSONObject();
					int from = rs.getInt("pagefrom");
					int to = rs.getInt("pageto");
					int pagerel = rs.getInt("pagerelationid");
					
					String apilist = getApiListFromDirectory(prodid, pagerel, company);

					jsonStr.put("from", from);
					jsonStr.put("to", to);
					jsonStr.put("pagerelationid", pagerel);
					jsonStr.put("api", apilist);

					edges.add(jsonStr);
				}
				
				rs.close();
				stmt.close();
			} catch (Exception e) {
				System.err.println(Utilities.getNow());
				e.printStackTrace();
			}
			totalPages = nodes.size();
			json.put("nodes", nodes);
			json.put("edges", edges);
			return json;
		}
	 
	 	private String getApiListFromDirectory(int prodid, int pagerelid, int companyId) {
	 		String sql = "SELECT apiid FROM cta.page_testcase_api WHERE prodid=? AND pagerelationid=?";
	 		String sql_api = "SELECT * FROM cta.apidata WHERE idapidata=?";
	 		String res = "";
	 		try {
	 			PreparedStatement stmt = testCon2.prepareStatement(sql);
	 			stmt.setInt(1, prodid);
	 			stmt.setInt(2, pagerelid);
	 			ResultSet rs = stmt.executeQuery();
	 			String result = "";
	 			while(rs.next()){
	 				int apiid = rs.getInt(1);
	 				stmt = testCon2.prepareStatement(sql_api);
	 				stmt.setInt(1, apiid);
	 				ResultSet rs2 = stmt.executeQuery();
	 				while(rs2.next()) {
	 					int tcid = rs2.getInt("testcase");
	 					String randomString = rs2.getString("randomString");
 						String filePath = Utilities.getApiDataPath(companyId, tcid);
 						String responseFilePath = filePath + "\\request_" + randomString +".txt";
 						result = Utilities.readDataFromFile(responseFilePath);
 						res += removeHostnameFromURL(result);
					}
	 				rs2.close();
	 			}
	 			rs.close();
	 			stmt.close();
			} catch (Exception e) {
				System.err.println(Utilities.getNow());
				e.printStackTrace();
			}
		return res;
	}
	 	
	 	private String removeHostnameFromURL(String url) {
	 	    return url.replaceAll(".*?(?:\\.com/|\\.co/)", "");
	 	}

		public int getPageNo(int idpage, int prodid) {
	 		String sql = "SELECT pageno From cta.testcase_page where idpage = ? and prodid=?";
	 		int pageno = -1;
	 		try {
	 			PreparedStatement stmt = testCon2.prepareStatement(sql);
	 			stmt.setInt(1, idpage);
	 			stmt.setInt(2, prodid);
	 			ResultSet rs = stmt.executeQuery();
	 			if (rs.next()) {
	 				pageno = rs.getInt(1);
	 			} 
	 			} catch(Exception e) {
	 				System.err.println(Utilities.getNow());
	 				e.printStackTrace();
	 			}
	 		return pageno;
	 	}
		
		public JSONArray getTeststepsOnPageid(int tcid, int pageid) {
			String pn = "SELECT pageno FROM cta.testcase_page WHERE idtestcase=? AND idpage=?";
			String sql = "SELECT * FROM cta.test_step ts JOIN cta.test_step_attr tsa ON ts.idtest_step = tsa.test_step "
					+ "WHERE ts.Test_Case_Id = ? AND tsa.pagenumber = ? ORDER BY tsa.test_step";
			JSONArray json = new JSONArray();
			try {
				PreparedStatement stmt =  testCon2.prepareStatement(pn);
				stmt.setInt(1, tcid);
				stmt.setInt(2, pageid);
				ResultSet rs = stmt.executeQuery();
				int pageno=-1;
				if(rs.next()) {
					pageno = rs.getInt(1);
					stmt =  testCon2.prepareStatement(sql);
					stmt.setInt(1, tcid);
					stmt.setInt(2, pageno);
					rs = stmt.executeQuery();
					while(rs.next()) {
						JSONObject jsonObj = new JSONObject();
						jsonObj = msc.getTestStepsFromDb(rs, false, false);
						json.add(jsonObj);
					}
				}
				rs.close();
				stmt.close();
			} catch (Exception e) {
				System.err.println(Utilities.getNow());
				e.printStackTrace();
			}
			return json;
		}
		
		public JSONArray getElementsOfPageUsingTsid(int tcid, int pageno) {
			String sql = "SELECT * FROM cta.page_elements WHERE pageid=?";
			String pn = "SELECT idpage FROM cta.testcase_page WHERE testcaseid = ? AND pageno=?";
			JSONArray json = new JSONArray();
			try {
				PreparedStatement stmt =  testCon2.prepareStatement(pn);
				stmt.setInt(1, tcid);
				stmt.setInt(2, pageno);
				ResultSet rs = stmt.executeQuery();
				int pageid=-1;
				while(rs.next()) {
					pageid = rs.getInt(1);
					stmt =  testCon2.prepareStatement(sql);
					stmt.setInt(1, pageid);
					ResultSet rs2 = stmt.executeQuery();
					while(rs2.next()) {
						JSONObject jsonStr = new JSONObject();
						int testcaseid = rs2.getInt("testcaseid");
						int idpage = rs2.getInt("idpage");
						int pagenumber = rs2.getInt("apgeno");
						jsonStr.put("testcaseid", testcaseid);
						jsonStr.put("pageid", idpage);
						jsonStr.put("pagenumber", pagenumber);
						json.add(jsonStr);
					}
					rs2.close();
				}
				
				rs.close();
				stmt.close();
			} catch (Exception e) {
				System.err.println(Utilities.getNow());
				e.printStackTrace();
			}
			return json;
		}
		
		public JSONArray getElementsOfPage(int pageid) {
			String sql = "SELECT * FROM cta.page_elements WHERE pageid=?";
			JSONArray json = new JSONArray();
			try {
				PreparedStatement stmt =  testCon2.prepareStatement(sql);
				stmt.setInt(1, pageid);
				ResultSet rs = stmt.executeQuery();
				while(rs.next()) {
					JSONObject jsonStr = new JSONObject();
					jsonStr.put("elementid", rs.getInt("elementid"));
					jsonStr.put("elementname", rs.getString("elementname"));
					jsonStr.put("elementtype", rs.getString("elementtype"));
					jsonStr.put("recordedxpath", rs.getString("recordedxpath"));
					jsonStr.put("coveragecount", rs.getInt("coveragecount"));
					jsonStr.put("nearestname", rs.getString("nearestname"));
					jsonStr.put("uniquename", rs.getString("uniquename"));
					json.add(jsonStr);
				}				
				rs.close();
				stmt.close();
			} catch (Exception e) {
				System.err.println(Utilities.getNow());
				e.printStackTrace();
			}
			return json;
		}
		
		public int countTotalElementOnPage(int pageid) {
			int totalPageCount=0;
			String str = "SELECT Count(*) FROM cta.page_elements WHERE pageid=?";
			try {
				PreparedStatement stmt = testCon2.prepareStatement(str);
				stmt.setInt(1, pageid);
				ResultSet rs = stmt.executeQuery();
				if(rs.next()) {
					totalPageCount=rs.getInt(1);
				}
				rs.close();
				stmt.close();
			} catch (Exception e) {
				System.err.println(Utilities.getNow());
				e.printStackTrace();
			}
			return totalPageCount;
		}
		
		public int countTotalRecordedElementOnPage(int pageid) {
			int totalPageCount=0;
			String str = "SELECT Count(*) FROM cta.page_elements WHERE pageid=? AND recorded = 'true'";
			try {
				PreparedStatement stmt = testCon2.prepareStatement(str);
				stmt.setInt(1, pageid);
				ResultSet rs = stmt.executeQuery();
				if(rs.next()) {
					totalPageCount=rs.getInt(1);
				}
				
				rs.close();
				stmt.close();
			} catch (Exception e) {
				System.err.println(Utilities.getNow());
				e.printStackTrace();
			}
			return totalPageCount;
		}
	 
		public JSONObject getproductelementsdetail(int prodid) {
			String sql = "SELECT * FROM cta.pages Where prodid=?";
			JSONObject jsonStr = new JSONObject();
			try {
				PreparedStatement stmt =  testCon2.prepareStatement(sql);
				stmt.setInt(1,prodid);
				ResultSet rs = stmt.executeQuery();
				int totalpages = 0;
				int totalelement = 0;
				int usedelement = 0;
				while(rs.next()) {
					totalpages += 1;
					totalelement += rs.getInt("totalelementonpage");
					usedelement += rs.getInt("usedelementonpage");
				}
				jsonStr.put("prodid", prodid);
				jsonStr.put("totalpages", totalpages);
				jsonStr.put("totalelement", totalelement);
				jsonStr.put("usedelement", usedelement);
				
				rs.close();
				stmt.close();
			} catch (Exception e) {
				System.err.println(Utilities.getNow());
				e.printStackTrace();
			}
			return jsonStr;
		}
		
		 public JSONObject getpagescreenshot(int company, int prodid, int id, String randomkey) {
				String sql = "SELECT screenshotpath FROM cta.pages WHERE pageid=?";
				JSONObject json = new JSONObject();
				try {
					PreparedStatement stmt =  testCon2.prepareStatement(sql);
					stmt.setInt(1,id);
					ResultSet rs = stmt.executeQuery();
					if(rs.next()) {
						String screenshotpath = Utilities.getCoverageSSPath(company, prodid, id);
						String url = AppProperties.fileurl +"?action=downloadCoverageFile&companyid="+company+"&token="+ randomkey;
						try {
						    String encodedPath = URLEncoder.encode(screenshotpath, "UTF-8");
						    url += "&fileName=" + encodedPath;
						} catch (UnsupportedEncodingException e) {
							System.err.println(Utilities.getNow());
						    e.printStackTrace();
						}
						json.put("screenshotpath",url);
					}
					
					rs.close();
					stmt.close();
				} catch (Exception e) {
					System.err.println(Utilities.getNow());
					e.printStackTrace();
				}
				return json;
			}
		
		public void updatePageRelationTable(int from, int to, int prodid, int tcid) {
			String insert = "INSERT INTO cta.pagerelation (pagefrom, pageto, productid) VALUE (?,?,?)";
			String checkDup = "SELECT * FROM cta.pagerelation WHERE pagefrom=? AND pageto=? AND productid=?";
			String getPageId = "SELECT pageno FROM cta.testcase_page WHERE testcaseid=? AND idpage=? AND prodid=?";
			String update = "UPDATE cta.page_testcase_api SET pagerelationid=? WHERE testcase=? AND pagenumber=? AND prodid=?";
			try {
				PreparedStatement stmt = testCon2.prepareStatement(checkDup);
				stmt.setInt(1, from);
				stmt.setInt(2, to);
				stmt.setInt(3, prodid);
				ResultSet rs = stmt.executeQuery();
				if(!rs.next()) {
					if(to != from) {
						stmt = testCon2.prepareStatement(insert, Statement.RETURN_GENERATED_KEYS);
						stmt.setInt(1, from);
						stmt.setInt(2, to);
						stmt.setInt(3, prodid);
						stmt.executeUpdate();
						int pagerelid = -1;
						ResultSet generated = stmt.getGeneratedKeys();	
						if (generated.next()) {
							pagerelid = (generated.getBigDecimal(1)).intValue();
						}
						generated.close();
						
						stmt = testCon2.prepareStatement(getPageId);
						stmt.setInt(1, tcid);
						stmt.setInt(2, from);
						stmt.setInt(3, prodid);
						ResultSet rs2 = stmt.executeQuery();
						int pageno = -1;
						if(rs2.next()) {
							pageno = rs2.getInt(1);
						}
						rs2.close();
	        					
						PreparedStatement stmt_up = testCon2.prepareStatement(update);
						stmt_up.setInt(1, pagerelid);
						stmt_up.setInt(2, tcid);
						stmt_up.setInt(3, pageno);
						stmt_up.setInt(4, prodid);
						stmt_up.executeUpdate();
				        
				        stmt_up.close();
					}
				} else {
					stmt = testCon2.prepareStatement(getPageId);
					stmt.setInt(1, tcid);
					stmt.setInt(2, from);
					stmt.setInt(3, prodid);
					ResultSet rs2 = stmt.executeQuery();
					int pageno = -1;
					if(rs2.next()) {
						pageno = rs2.getInt(1);
					}
					rs2.close();
					int pagerelid = rs.getInt("pagerelationid");
					PreparedStatement stmt_up = testCon2.prepareStatement(update);
					stmt_up.setInt(1, pagerelid);
					stmt_up.setInt(2, tcid);
					stmt_up.setInt(3, pageno);
					stmt_up.setInt(4, prodid);
					stmt_up.executeUpdate();
			        
			        stmt_up.close();
				}
				rs.close();
		        stmt.close();
			} catch (Exception e) {
				System.err.println(Utilities.getNow());
				e.printStackTrace();
			}
		}
		

		public void updateDbForElementDetails(JSONObject json) {
			
			Timestamp startts= Utilities.getCurrentTimestamp();
			String page_check = "SELECT pageid FROM cta.pages WHERE pagename=? AND prodid=? AND totalelementonpage=? AND lastclicked_name=?";
			String sql_check = "SELECT idpagegroup FROM cta.pagegroup WHERE pagename=? AND prodid=?";
			try {
				String pagename = (String) json.get("pageName");
				if(pagename != null && pagename.length() > 150) {
					pagename = pagename.substring(0,150);
				} else if(pagename == null || pagename.equals("")) {
					pagename = "noname";
				}
				long pno = (long)json.get("pageNo");
				int pagenumber = (int)pno;
				long key = (long)json.get("token");
				int token = (int)key;
				long lastCount = (long) json.get("clickcount");
				int lastClickCount = (int) lastCount;
				if(lastClickCount < initialClickCount) {
					initialClickCount = 0;
				}
				int tcid = msc.getTestCaseIdFromKey(token);
				int modid = msc.getModuleFromTestcase(tcid);
				int prodid = msc.getProductsFromModule(modid);
				int cid = msc.getCompanyFromModule(modid);
				
				JSONArray allElements = (JSONArray) json.get("allelementsofpage");
				int totalElement = allElements.size();
				JSONArray testedList = (JSONArray) json.get("testellist");				
				int testedElement = testedList.size();
				JSONObject lastclick = (JSONObject) json.get("lastclick");
				String lastclicked_xpath = (String) lastclick.get("xpath");
				String lastclicked_name = (String) lastclick.get("name");
				JSONArray updatedElement = compareForRecordedStatus(allElements, testedList);
				
				int lastgroupid = -1;
				String lastEntered = "SELECT idpagegroup FROM cta.pagegroup WHERE prodid=? ORDER BY idpagegroup DESC LIMIT 1";
				PreparedStatement stmt_l = testCon2.prepareStatement(lastEntered);
				stmt_l.setInt(1, prodid);
				ResultSet lastRs = stmt_l.executeQuery();
				if(lastRs.next()) lastgroupid= lastRs.getInt(1);
				
				lastRs.close();	
				// PAGE_GROUPING FOR DOUBLE CLICK
				PreparedStatement stmt_db = testCon2.prepareStatement(sql_check);
				stmt_db.setString(1, pagename);
				stmt_db.setInt(2, prodid);
				ResultSet rs = stmt_db.executeQuery();
				int pagegroupid=-1;
				if(!rs.next()) {
					String db = "INSERT INTO cta.pagegroup (pagename, tcid, prodid) VALUES (?,?,?)";
					PreparedStatement stmt_pg = testCon2.prepareStatement(db, Statement.RETURN_GENERATED_KEYS);
					stmt_pg.setString(1, pagename);
					stmt_pg.setInt(2, tcid);
					stmt_pg.setInt(3, prodid);
					stmt_pg.executeUpdate();
					ResultSet generated = stmt_pg.getGeneratedKeys();	
					if (generated.next()) {
						pagegroupid = (generated.getBigDecimal(1)).intValue();
					}
					generated.close();
					stmt_pg.close();
//					updatePageGroupRelationTable(lastgroupid, pagegroupid, prodid);
				} else {
					pagegroupid = (int)rs.getInt("idpagegroup");
//					updatePageGroupRelationTable(lastgroupid, pagegroupid, prodid);
				}
				
				int lastid = -1;
				lastEntered = "SELECT idpage FROM cta.testcase_page WHERE prodid=? AND testcaseid=? ORDER BY idpage DESC LIMIT 1";
				stmt_l = testCon2.prepareStatement(lastEntered);
				stmt_l.setInt(1, prodid);
				stmt_l.setInt(2, tcid);
				ResultSet lastRs2 = stmt_l.executeQuery();
				if(lastRs2.next()) lastid= lastRs2.getInt(1);
				
				stmt_l.close();
				lastRs2.close();
				
				JSONObject pageinfo = checkDuplicatePage(token, pagename, updatedElement);
				int page_id = (int)pageinfo.get("pageid");
				int matchPercent = (int)pageinfo.get("matchPercent");
				int matchElement = (int)pageinfo.get("matchElement");
				if(page_id !=0 && matchPercent> Integer.valueOf(AppProperties.upperLimitofPageVersioning)) {
					updateTestCasePageTable(tcid, page_id, pagenumber, prodid, totalElement, updatedElement, lastid, lastClickCount);
				} else {
					//CREATE PAGE IF NO DUPLICATE PRESENT
					stmt_db = testCon2.prepareStatement(page_check);
					stmt_db.setString(1, pagename);
					stmt_db.setInt(2, prodid);
					stmt_db.setInt(3, totalElement);
					stmt_db.setString(4, lastclicked_name);
					rs = stmt_db.executeQuery();
					int pageid=-1;
					if(!rs.next()) {
						String page_sql = "INSERT INTO cta.pages (pagename, prodid, totalelementonpage, usedelementonpage, screenshotpath, "
								+ "lastclicked_xpath, lastclicked_name, pagegroupid) VALUE (?,?,?,?,?,?,?,?)";
						PreparedStatement stmt_p = testCon2.prepareStatement(page_sql, Statement.RETURN_GENERATED_KEYS);
						stmt_p.setString(1, pagename);
						stmt_p.setInt(2, prodid);
						stmt_p.setInt(3, totalElement);
						stmt_p.setInt(4, testedElement);
						stmt_p.setString(5, null);
						stmt_p.setString(6, lastclicked_xpath);
						stmt_p.setString(7, lastclicked_name);
						stmt_p.setInt(8, pagegroupid);
						stmt_p.executeUpdate();
						ResultSet generatedKeys = stmt_p.getGeneratedKeys();
						
						if (generatedKeys.next()) {
							pageid = (generatedKeys.getBigDecimal(1)).intValue();
						}
						generatedKeys.close();
						stmt_p.close();

					} else {
						pageid = (int)rs.getInt(1);
					}
						
					if(matchPercent>Integer.valueOf(AppProperties.lowerLimitofPageVersioning) && 
							matchPercent<Integer.valueOf(AppProperties.upperLimitofPageVersioning) && 
								totalElement - matchElement < Integer.valueOf(AppProperties.elementDiffforPageVersioning)) {
						updatePageVersion(pageid, page_id);
					}					
					updateTestCasePageTable(tcid, pageid, pagenumber, prodid, totalElement, updatedElement, lastid, lastClickCount);
					page_id = pageid; // For generating the pom class for testcase.
				}
				stmt_db.close();
				rs.close();
				fixMissingElements(tcid, startts, page_id, pagenumber, lastClickCount);
				initialClickCount = lastClickCount;
				GenPomClassForTestcase(page_id, prodid, cid);
			} catch (Exception e) {
				System.err.println(Utilities.getNow());
				e.printStackTrace();
			}
		}
		
		public String extractType(String xpath) {
			String[] parts = xpath.split(" \\| ");
	        String lastPart = parts[parts.length - 1];

	        // Split the last part by "/" and get the last segment
	        String[] lastSegments = lastPart.split("/");
	        String lastWord = lastSegments[lastSegments.length - 1];

	        // Check for indexing of any array elements and print the last word
	        if (lastWord.contains("svg")) {
	        	lastWord = "svg";
	        } else if (lastWord.contains("[")) {
	            lastWord = lastWord.substring(0, lastWord.indexOf("["));
	        }
	        
	        return lastWord;
		}
		
		public void fixMissingElements(int tcid, Timestamp beforeTime, int pageid, int pagenumber, int lastClickCount) {
			try {
				String sql = "SELECT * from cta.test_step as ts "
						+ "join cta.test_step_attr as tsa on tsa.test_step = ts.idtest_step "
						+ "where  ts.Test_Case_Id = ? and tsa.elementId IS NULL and ts.createddate <= ?";
				String element_sql = "INSERT INTO cta.page_elements "
                		+ "(elementname, pageid, recordedxpath, clickable, recorded, nearestname, "
                		+ "elementtype, coveragecount) VALUE (?,?,?,?,?,?,?,?)";
				PreparedStatement stmt = testCon2.prepareStatement(sql);
				stmt.setInt(1, tcid);
				stmt.setTimestamp(2, beforeTime);
				ResultSet rs = stmt.executeQuery();
				
				while(rs.next()) {
					JSONObject testStep = msc.getTestStepsFromDb(rs, false, true);
					
					String xpath = (String)testStep.get("Object_Xpath");
					if(xpath == null || xpath.equals("")) {
						continue;
					}
					String processedXpath = xpath.replaceAll("'", "\\\\'");
					
					String nearestName = (String)testStep.get("nearestname");
                    if(nearestName == null || nearestName.equals("")) {
                    	nearestName = (String)testStep.get("Page_Description");
                    }
	                String pet_sql = "select elementid from cta.page_elements where recordedxpath = ? and pageid = ? and nearestname = ? and status = 0";
	                PreparedStatement stmt_l2 = testCon2.prepareStatement(pet_sql);
	                stmt_l2.setString(1, processedXpath);
	                stmt_l2.setInt(2, pageid);
	                stmt_l2.setString(3, nearestName);
	                ResultSet lastRs3 = stmt_l2.executeQuery();
	                
	                int elementid = -1;
	                String type = extractType(xpath);
	                
					if(!lastRs3.next()) {
	                    PreparedStatement stmt_el = testCon2.prepareStatement(element_sql, Statement.RETURN_GENERATED_KEYS);
	                    stmt_el.setString(1, (String)testStep.get("Page_Description"));
	                    stmt_el.setInt(2, pageid);
	                    stmt_el.setString(3, xpath);
	                    stmt_el.setBoolean(4, true);
	                    stmt_el.setBoolean(5, true);
	                    stmt_el.setString(6, nearestName);
	                    stmt_el.setString(7, type);
	                    stmt_el.setInt(8, 1);
	                    stmt_el.executeUpdate();
	                    ResultSet generatedKeys = stmt_el.getGeneratedKeys();
	                    
	                    
	                    if (generatedKeys.next()) {
	                        elementid = (generatedKeys.getBigDecimal(1)).intValue();
	                    }
	                    generatedKeys.close();
	                    stmt_el.close();
	                    
	                 // POPULATING PAGE_ELEMENT_TESTCASE TABLE
		                pet_sql = "INSERT INTO cta.page_element_testcase "
		                		+ "(testcaseid, idpage, elementid, recorded) VALUE (?,?,?,?)";
		                PreparedStatement stmt_ec = testCon2.prepareStatement(pet_sql);
		                stmt_ec.setInt(1, tcid);
		                stmt_ec.setInt(2, pageid);
		                stmt_ec.setInt(3, elementid);
		                stmt_ec.setBoolean(4, true);
		                stmt_ec.executeUpdate();
		                stmt_ec.close();
					} else {
						
						elementid = lastRs3.getInt(1);
					}
					lastRs3.close();
					stmt_l2.close();
	                
	                String sql_update = "UPDATE cta.test_step_attr SET elementId=?, pagenumber=? "
                    		+ "WHERE test_step=? AND elementId IS NULL";
                    PreparedStatement stmt_u = testCon2.prepareStatement(sql_update);
                    stmt_u.setInt(1, elementid);
                    stmt_u.setInt(2, pagenumber);
                    stmt_u.setInt(3, (int)testStep.get("idtest_step"));
                    int rowsUpdated = stmt_u.executeUpdate();
                    stmt_u.close();
				}
				rs.close();
				stmt.close();
			}catch (Exception e) {
				System.err.println(Utilities.getCurrentTimestamp());
				e.printStackTrace();
			}
			
		}
			
		public void updateTestCasePageTable(int tcid, int pageid, int pagenumber, int prodid, int totalElement, 
				JSONArray updatedElement, int lastid, int lastClickCount) {
			String tcpage_check = "SELECT idtestcase_page FROM testcase_page WHERE testcaseid=? AND idpage = ? AND pageno=? AND prodid=?";
		    String element_check = "SELECT elementid, recordedxpath FROM cta.page_elements WHERE elementname=? AND recordedxpath = ? AND pageid=? AND status =0";
		    String sql = "INSERT INTO cta.testcase_page (testcaseid, idpage, pageno, prodid, createddate) VALUE (?,?,?,?,?)";
		    List<JSONObject> failedUpdates = new ArrayList<>();  // List to track failed updates

		    try {
		        // TESTCASE_PAGE TABLE DATA POPULATION
		    	PreparedStatement stmt = testCon2.prepareStatement(tcpage_check);
		    	stmt.setInt(1, tcid);
		        stmt.setInt(2, pageid);
		        stmt.setInt(3, pagenumber);
		        stmt.setInt(4, prodid);
		        ResultSet rs1 = stmt.executeQuery();
		        
		        if(!rs1.next()) {		        
			        stmt = testCon2.prepareStatement(sql);
			        stmt.setInt(1, tcid);
			        stmt.setInt(2, pageid);
			        stmt.setInt(3, pagenumber);
			        stmt.setInt(4, prodid);
			        stmt.setTimestamp(5, Utilities.getCurrentTimestamp());
			        stmt.executeUpdate();
		        }
		        rs1.close();
		        stmt.close();

		        if (totalElement > 0) {
		            for (int i = 0; i < totalElement; i++) {
		                JSONObject element = (JSONObject) updatedElement.get(i);
		                String absxpath = (String) element.get("elpath");
		                String relxpath = (String) element.get("elrelativepath");
		                String xpath = "";
		                if(relxpath != null && !relxpath.equals("")) {
			                xpath = relxpath + " | " + absxpath;
		                } else {
			                xpath = absxpath;
		                }
		                String name = (String) element.get("elname");
		                boolean isclickable = false;
		                if(element.get("isClickable") != null) {
		                	isclickable = (boolean) element.get("isClickable");
		                }
		                String nearestname = (String) element.get("nearestname");
		                String elementtype = (String) element.get("elementtype");
		                boolean recorded = (boolean) element.get("recorded");
		                int elementid = -1;

		                // CHECKING DUPLICATE ELEMENT AND THEN CREATING ELEMENT
		                String newxpath = xpath;
		                if(!xpath.contains("|")) {
		                	newxpath = "%" + xpath;
		                	element_check = "SELECT elementid, recordedxpath FROM cta.page_elements WHERE elementname=? AND recordedxpath like ? AND pageid=?";
		                }
		                
		                PreparedStatement stmt_ec = testCon2.prepareStatement(element_check);
		                stmt_ec.setString(1, name);
		                
		                stmt_ec.setString(2, newxpath);
		                stmt_ec.setInt(3, pageid);
		                ResultSet rs = stmt_ec.executeQuery();
		                if (!rs.next()) {
		                    String element_sql = "INSERT INTO cta.page_elements "
		                    		+ "(elementname, pageid, recordedxpath, clickable, recorded, nearestname, "
		                    		+ "elementtype) VALUE (?,?,?,?,?,?,?)";
		                    PreparedStatement stmt_el = testCon2.prepareStatement(element_sql, Statement.RETURN_GENERATED_KEYS);
		                    stmt_el.setString(1, name);
		                    stmt_el.setInt(2, pageid);
		                    stmt_el.setString(3, xpath);
		                    stmt_el.setBoolean(4, isclickable);
		                    stmt_el.setBoolean(5, recorded);
		                    stmt_el.setString(6, nearestname);
		                    stmt_el.setString(7, elementtype);
		                    stmt_el.executeUpdate();
		                    ResultSet generatedKeys = stmt_el.getGeneratedKeys();

		                    if (generatedKeys.next()) {
		                        elementid = (generatedKeys.getBigDecimal(1)).intValue();
		                    }
		                    if (recorded) {
		                        updateCoverageCount(elementid);
		                    }
		                    generatedKeys.close();
		                    stmt_el.close();
		                } else {
		                    elementid = (int) rs.getInt(1);
		                    xpath = rs.getString(2); //using duplicate elements xpath for further computation
		                    if (recorded) {
		                        updateCoverageCount(elementid);
		                    }
		                }

		                // POPULATING PAGE_ELEMENT_TESTCASE TABLE
		                String pet_sql = "INSERT INTO cta.page_element_testcase "
		                		+ "(testcaseid, idpage, elementid, recorded) VALUE (?,?,?,?)";
		                stmt_ec = testCon2.prepareStatement(pet_sql);
		                stmt_ec.setInt(1, tcid);
		                stmt_ec.setInt(2, pageid);
		                stmt_ec.setInt(3, elementid);
		                stmt_ec.setBoolean(4, recorded);
		                stmt_ec.executeUpdate();
		                stmt_ec.close();
		                rs.close();

		                int teststepid = -1;
		                String processedXpath = xpath.replaceAll("'", "\\\\'");
//		                processedXpath = processedXpath.replaceAll("\\[\\d+\\]$", "");
//		                pet_sql = "SELECT idtest_step FROM cta.test_step WHERE Test_Case_Id=? AND REGEXP_REPLACE(Object_Xpath, '\\\\[[0-9]+\\\\]$', '') "
		                pet_sql = "SELECT idtest_step FROM cta.test_step WHERE Test_Case_Id=? AND Object_Xpath "
		                		+ "LIKE '%" + processedXpath + "' AND tsSequence >= ? AND tsSequence <= ?";
		                PreparedStatement stmt_l2 = testCon2.prepareStatement(pet_sql);
		                stmt_l2.setInt(1, tcid);
		                stmt_l2.setInt(2, initialClickCount);
		                stmt_l2.setInt(3, lastClickCount);
		                ResultSet lastRs3 = stmt_l2.executeQuery();
		                boolean updateSuccess = false;
		                while (lastRs3.next()) {
		                    teststepid = lastRs3.getInt(1);
		                    String sql_update = "UPDATE cta.test_step_attr SET elementId=?, pagenumber=? "
		                    		+ "WHERE test_step=? AND elementId IS NULL";
		                    PreparedStatement stmt_u = testCon2.prepareStatement(sql_update);
		                    stmt_u.setInt(1, elementid);
		                    stmt_u.setInt(2, pagenumber);
		                    stmt_u.setInt(3, teststepid);
		                    int rowsUpdated = stmt_u.executeUpdate();
		                    stmt_u.close();
		                    if (rowsUpdated > 0) {
		                        updateSuccess = true;
		                    }
		                    updateCvrgAbsXpathWithRelativeXpath(teststepid, elementid);
		                    if(!recorded) {
		                    	updateTCElementAsRecorded(tcid, elementid);		                    	
		                    }
		                }
		                stmt_l2.close();
		                lastRs3.close();

		                // If update was not successful, add element to the failed list
		                if (!updateSuccess) {
		                    failedUpdates.add(element);
		                }
		            }
		        }

		        if (lastid != -1) updatePageRelationTable(lastid, pageid, prodid, tcid);

		        // Log the failed updates
		        if (!failedUpdates.isEmpty()) {
		            System.out.println("Failed to update the following elements:" + failedUpdates.size());
		            for (JSONObject failedElement : failedUpdates) {
		                System.out.println(failedElement);
		            }
		        }

		    } catch (Exception e) {
		    	System.err.println(Utilities.getNow());
		        e.printStackTrace();
		    }
		}
		
		public JSONArray compareForRecordedStatus(JSONArray allEll, JSONArray tested) {
	        for (Object obj : allEll) {
	            JSONObject jsonObject1 = (JSONObject) obj;
	            String xpath = (String) jsonObject1.get("elpath");
	            boolean presentInSecondArray = false;

	            for (Object obj2 : tested) {
	                JSONObject jsonObject2 = (JSONObject) obj2;
	                String xpath2 = (String) jsonObject2.get("elpath");

	                if (xpath.equals(xpath2)) {
	                    presentInSecondArray = true;
	                    break;
	                }
	            }

	            jsonObject1.put("recorded", presentInSecondArray);
	        }
	        
	        for (Object obj : allEll) {
	            JSONObject jsonObject1 = (JSONObject) obj;
	            if (!jsonObject1.containsKey("recorded")) {
	                jsonObject1.put("recorded", false);
	            }
	        }
	        return allEll;	
		}
		
		public JSONObject checkDuplicatePage(int token, String pagename, JSONArray totalele) {
			int tcid = msc.getTestCaseIdFromKey(token);
			int totalElement = totalele.size();
			int pageid = 0, matchElement = 0;
			int matchPercent = 0;
			JSONObject result = new JSONObject();
			try {
		        for (Object obj : totalele) {
		            JSONObject jsonObject1 = (JSONObject) obj;
		            String xpath = (String) jsonObject1.get("elpath");
		            String processedXpath = xpath.replaceAll("'", "\\\\'");
		            
					String sql = "SELECT p.pageid FROM cta.testcase_page tcp JOIN cta.pages p ON tcp.idpage=p.pageid "
							+ "JOIN cta.page_elements pe ON p.pageid = pe.pageid WHERE tcp.testcaseid = ? "
							+ "AND p.pagename = ? AND pe.recordedxpath LIKE '%"+ processedXpath +"%'";
		            
					PreparedStatement stmt = null;
					ResultSet rs = null;
					try {
					    stmt = testCon2.prepareStatement(sql);
					    stmt.setInt(1, tcid);
					    stmt.setString(2, pagename);
					    rs = stmt.executeQuery();
					    while(rs.next()) {
					        matchElement += 1;
					        pageid = rs.getInt(1);
					    }
					} finally {
					    if (rs != null) {
					        rs.close();
					    }
					    if (stmt != null) {
					        stmt.close();
					    }
					}
		        }
		        
		        matchPercent = (int) (((double) matchElement/totalElement) * 100);
		        if(matchPercent > Integer.valueOf(AppProperties.upperLimitofPageVersioning) || 
		        		totalElement - matchElement > Integer.valueOf(AppProperties.elementDiffforPageVersioning)) {
		        	result.put("pageid",pageid);
		        } else result.put("pageid", 0);
		        
		        result.put("matchPercent", matchPercent);
		        result.put("matchElement", matchElement);    
			} catch (Exception e) {
				System.err.println(Utilities.getNow());
				e.printStackTrace();
			}
			return result;
		}
		
		public void updatePageVersion(int pageid, int parentPageId) {
			String sql = "UPDATE cta.pages SET version = version+1, versionedfrompage = ? WHERE pageid = ?";
			try {
				PreparedStatement stmt =  testCon2.prepareStatement(sql);
				stmt.setInt(1, parentPageId);
				stmt.setInt(2, pageid);
				stmt.executeUpdate();
				
		        stmt.close();
			} catch (Exception e) {
				System.err.println(Utilities.getNow());
				e.printStackTrace();
			}
			
		}
		
		public void updateCoverageCount(int elementid) {
			String updateCount = "UPDATE cta.page_elements SET coveragecount = coveragecount+1, "
					+ "clickable=1, recorded = 1 WHERE elementid=?";
			try {				
				PreparedStatement stmt_update = testCon2.prepareStatement(updateCount);
				stmt_update.setInt(1, elementid);
				stmt_update.executeUpdate();
				
				stmt_update.close();
			} catch (Exception e) {
				System.err.println(Utilities.getNow());
				e.printStackTrace();
			}
		}

		public JSONObject getTcidFromVFName(int key, String fileName) {
			String sql = "SELECT idtest_case, Created_By FROM cta.test_case WHERE extKey = ? AND Test_Case = ? "
					+ "order by Created_Date Desc Limit 1";
			JSONObject testcase = new JSONObject();
			try {
				PreparedStatement stmt =  testCon2.prepareStatement(sql);
				stmt.setInt(1, key);
				stmt.setString(2, fileName);
				ResultSet rs = stmt.executeQuery();
				if(rs.next()) {
					int tcid = rs.getInt(1);
					String uname = rs.getString(2);
					testcase.put("username", uname);
					testcase.put("tcid", tcid);
				}
				
				rs.close();
				stmt.close();
			} catch (Exception e) {
				System.err.println(Utilities.getNow());
				e.printStackTrace();
			}
			return testcase;
		}

		public void updateTestCaseFileName(int tcid, String fileName) {
			String sql = "UPDATE cta.test_case SET videofilename = ? WHERE idtest_case = ? ";
			try {
				PreparedStatement stmt =  testCon2.prepareStatement(sql);
				stmt.setString(1, fileName);
				stmt.setInt(2, tcid);
				stmt.executeUpdate();

				stmt.close();
			} catch (Exception e) {
				System.err.println(Utilities.getNow());
				e.printStackTrace();
			}
			
		}
		
		
		public void insertIntegrationDetails(int prodId, String intname, String adminprop) {
			String sql = "INSERT INTO cta.integration (productid, intname, adminprop) "
					+ "VALUES (?, ?, ?)";
			String check = "SELECT * FROM cta.integration WHERE productid=? AND intname=? AND status=0";
			try {
		        PreparedStatement stmt = testCon2.prepareStatement(check);
		        stmt.setInt(1, prodId);
		        stmt.setString(2, intname);
		        ResultSet rs = stmt.executeQuery();
		        if(rs.next()) {
		        	updateIntegrationDetails(prodId, intname, adminprop);
		        } else {
		            stmt = testCon2.prepareStatement(sql);
		            stmt.setInt(1, prodId);
		            stmt.setString(2, intname);
		            stmt.setString(3, adminprop);
		    		stmt.executeUpdate();
		        }
		        rs.close();
		        stmt.close();
			} catch(Exception e) {
				System.err.println(Utilities.getNow());
				e.printStackTrace();
			}
		}

		public void updateIntegrationDetails(int prodId, String intname, String adminprop) {
			String sql = "Update cta.integration SET adminprop = ?"
					+ " WHERE productid = ? AND intname = ?";
			try {
		        PreparedStatement stmt = testCon2.prepareStatement(sql);
		        stmt.setString(1, adminprop);
		        stmt.setInt(2, prodId);
		        stmt.setString(3, intname);
				stmt.executeUpdate();
				stmt.close();
			} catch(Exception e) {
				System.err.println(Utilities.getNow());
				e.printStackTrace();
			}
		}
		
		public void insertUserIntegrationDetails(int prodId, String intname, String userprop) {
			String sql = "INSERT INTO cta.integration (productid, intname, userprop) "
					+ "VALUES (?, ?, ?)";
			String check = "SELECT * FROM cta.integration WHERE productid=? AND intname=?";
			try {
		        PreparedStatement stmt = testCon2.prepareStatement(check);
		        stmt.setInt(1, prodId);
		        stmt.setString(2, intname);
		        ResultSet rs = stmt.executeQuery();
		        if(rs.next()) {
		        	updateUserIntegrationDetails(prodId, intname, userprop);
		        } else {
		            stmt = testCon2.prepareStatement(sql);
		            stmt.setInt(1, prodId);
		            stmt.setString(2, intname);
		            stmt.setString(3, userprop);
		    		stmt.executeUpdate();
		        }
		        rs.close();
		        stmt.close();
			} catch(Exception e) {
				System.err.println(Utilities.getNow());
				e.printStackTrace();
			}
		}

		public void updateUserIntegrationDetails(int prodId, String intname, String userprop) {
			String sql = "Update cta.integration SET userprop = ?"
					+ " WHERE productid = ? AND intname = ?";
			try {
		        PreparedStatement stmt = testCon2.prepareStatement(sql);
		        stmt.setString(1, userprop);
		        stmt.setInt(2, prodId);
		        stmt.setString(3, intname);
				stmt.executeUpdate();
				stmt.close();
			} catch(Exception e) {
				System.err.println(Utilities.getNow());
				e.printStackTrace();
			}
		}
		
		public void deleteIntegration(int intid) {
			String sql = "Update cta.integration SET status = 1 WHERE integrationid = ?";
			try {
		        PreparedStatement stmt = testCon2.prepareStatement(sql);
		        stmt.setInt(1, intid);
				stmt.executeUpdate();
				stmt.close();
			} catch(Exception e) {
				System.err.println(Utilities.getNow());
				e.printStackTrace();
			}
		}
		
		public JSONObject getIntegrationdetail(int prodid, String name) {
			String sql = "SELECT * FROM cta.integration WHERE productid=? AND status=0 AND intname=?";
			JSONObject jsonStr = new JSONObject();
			try {
				PreparedStatement stmt =  testCon2.prepareStatement(sql);
				stmt.setInt(1,prodid);
				stmt.setString(2, name);
				ResultSet rs = stmt.executeQuery();
				if(rs.next()) {
					int productid = rs.getInt("productid");
					int integrationid = rs.getInt("integrationid");
					String intname = rs.getString("intname");
					String adminprop = rs.getString("adminprop");
					String userprop = rs.getString("userprop");
					jsonStr.put("integrationid", integrationid);
					jsonStr.put("productid", productid);
					jsonStr.put("intname", intname);
					jsonStr.put("adminprop", adminprop);
					jsonStr.put("userprop", userprop);
				}
				
				rs.close();
				stmt.close();
			} catch (Exception e) {
				System.err.println(Utilities.getNow());
				e.printStackTrace();
			}
			return jsonStr;
		}
		
		public void insertStacticIntegrationDetails(String intname, String adminattribute, String userattribute) {
			String sql = "INSERT INTO cta.integration_static (intname, adminattribute, userattribute) "
					+ "VALUES (?, ?, ?)";
			String check = "SELECT * FROM cta.integration_static WHERE intname=?";
			try {
		        PreparedStatement stmt = testCon2.prepareStatement(check);
		        stmt.setString(1, intname);
		        ResultSet rs = stmt.executeQuery();
		        if(rs.next()) {
		        	updateStaticIntegrationDetails(intname, adminattribute, userattribute);
		        } else {
		            stmt = testCon2.prepareStatement(sql);
		            stmt.setString(1, intname);
		            stmt.setString(2, adminattribute);
		            stmt.setString(3, userattribute);
		    		stmt.executeUpdate();
		        }
		        rs.close();
		        stmt.close();
			} catch(Exception e) {
				System.err.println(Utilities.getNow());
				e.printStackTrace();
			}
		}

		public void updateStaticIntegrationDetails(String intname, String adminattribute, String userattribute) {
			String sql = "Update cta.integration_static SET adminattribute = ?, userattribute=?"
					+ " WHERE intname = ?";
			try {
		        PreparedStatement stmt = testCon2.prepareStatement(sql);
		        stmt.setString(1, adminattribute);
		        stmt.setString(2, userattribute);
		        stmt.setString(3, intname);
				stmt.executeUpdate();
				stmt.close();
			} catch(Exception e) {
				System.err.println(Utilities.getNow());
				e.printStackTrace();
			}
		}
		
		public JSONArray getStaticIntegrationdetail() {
			String sql = "SELECT * FROM cta.integration_static";
			String sql_get = "SELECT integrationid FROM cta.integration WHERE intname=?";
			JSONArray jsonArr = new JSONArray();
			try {
				PreparedStatement stmt =  testCon2.prepareStatement(sql);
				ResultSet rs = stmt.executeQuery();
				int integrationid = -1; 
				while(rs.next()) {
					JSONObject jsonStr = new JSONObject();
					int id_integration_static = rs.getInt("id_integration_static");
					String intname = rs.getString("intname");
					stmt =  testCon2.prepareStatement(sql_get);
					stmt.setString(1,intname);
					ResultSet rs2 = stmt.executeQuery();
					if(rs2.next()) {
						integrationid = rs2.getInt(1);
					}
					String adminprop = rs.getString("adminattribute");
					String userprop = rs.getString("userattribute");
					jsonStr.put("staticintid", id_integration_static);
					jsonStr.put("intname", intname);
					jsonStr.put("integrationid", integrationid);
					jsonStr.put("admin_attributes", adminprop);
					jsonStr.put("user_attributes", userprop);
					jsonArr.add(jsonStr);
					rs2.close();
				}
				rs.close();
				stmt.close();
			} catch (Exception e) {
				System.err.println(Utilities.getNow());
				e.printStackTrace();
			}
			return jsonArr;
		}
		
		public JSONArray getDownloadedFilesList(int tcid) {
			String sql = "SELECT TestData FROM cta.test_step WHERE Action='Download' and Test_Case_Id=?";
			JSONArray jsonArr = new JSONArray();
			try {
				PreparedStatement stmt =  testCon2.prepareStatement(sql);
				stmt.setInt(1, tcid);
				ResultSet rs = stmt.executeQuery();
				while(rs.next()) {
					String filename = rs.getString(1);
					//Handle multiple files being downloaded or a zip file is downloaded
					if(filename != null && filename.contains(AppProperties.delimiter)) {
						String[] filenames = filename.split(AppProperties.delimiter);
						for (String name : filenames) {
							jsonArr.add(name);
				        }
					} else {
						jsonArr.add(filename);
					}
				}

				rs.close();
				stmt.close();
			} catch (Exception e) {
				System.err.println(Utilities.getNow());
				e.printStackTrace();
			}
			return jsonArr;
		}

		public void getTcDescription(int tcid, String desc) {
			String sql = "Update cta.test_case SET testcasedescription = ?"
					+ " WHERE idtest_case = ?";
			try {
		        PreparedStatement stmt = testCon2.prepareStatement(sql);
		        stmt.setString(1, desc);
		        stmt.setInt(2, tcid);
				stmt.executeUpdate();

				stmt.close();
			} catch(Exception e) {
				System.err.println(Utilities.getNow());
				e.printStackTrace();
			}
		}
		
		public JSONArray getAllTCforTagFromGitHub(int prodid, String repo, MySQlConn msc,
				String fromDate, String toDate) {
			
			
			SimpleDateFormat inputFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.ENGLISH);
	        SimpleDateFormat outputFormat = new SimpleDateFormat("yyyy-MM-dd");
	        JSONArray jsonArray = new JSONArray();
	        try {
	            Date date = new java.sql.Date(inputFormat.parse(fromDate).getTime()); 
	            String fromDateFmt = outputFormat.format(date);
	            String toDateFmt = "";
	            
	            if(toDate != null ) {
	            	date = new java.sql.Date(inputFormat.parse(fromDate).getTime());
	            	toDateFmt = outputFormat.format(date);
	            } else {
	            	LocalDate currentDate = LocalDate.now();
	            	DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
	            	toDateFmt = currentDate.format(formatter);
	            }
	            
	            GitHubIntegration ghi = new GitHubIntegration();
	            List<String> tempDesc = ghi.getTags(prodid, repo, fromDateFmt, toDateFmt, this);
	            jsonArray.addAll(tempDesc);

	        } catch (ParseException e) {
	        	System.err.println(Utilities.getNow());
	            e.printStackTrace();
	        }
	        
	        return jsonArray;
		}
		
		public JSONArray getAllTCforTagFromGitHub(int prodid, String desc, MySQlConn msc) {
			GitHubIntegration ghi = new GitHubIntegration();
			
			JSONObject prodIntDets = getIntegrationdetail(prodid, "Github");
			String  conn = (String)prodIntDets.get("adminprop");
			if(conn == null) {
				return new JSONArray();
			}
			String[] connDetails = conn.split(","); 
			
			String[] searchStrings = desc.split(" ");
			String reposList = "";
			String after = "";
			for(int i=0;i<searchStrings.length;i++) {
				String param = searchStrings[i];
				String[] paramSplit = param.split(":");
				String param1 = paramSplit[0];
				if(param1.equalsIgnoreCase("repo")) {
					reposList = paramSplit[1];
				} else if(param1.equalsIgnoreCase("after")) {
					after = paramSplit[1];
				}
			}
			
			if(after.equals("")) {
				LocalDate currentDate = LocalDate.now();
		        
		        // Subtract 2 weeks from the current date
		        LocalDate dateMinusTwoWeeks = currentDate.minusWeeks(2);
		        
		        // Format the date as a string
		        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
		        after = dateMinusTwoWeeks.format(formatter);
			}
			
			String[] repos = reposList.split(",");
			List<String> descs = new ArrayList<>();
			
			for(int i=0;i<repos.length;i++) {
				String repo = repos[i];
				
				List<String> tempDesc = ghi.getTags(connDetails[0], repo, connDetails[1], after,
						connDetails[2]);
				descs = mergeAndEliminateDuplicates(descs, tempDesc);
			}
			
			List<String> tags = new ArrayList<>();			
			for(int i=0;i<descs.size();i++) {
				String tempDesc = descs.get(i);
				List<String> tempTags = Utilities.getTagsFromDesc(tempDesc);
				tags = mergeAndEliminateDuplicates(tags, tempTags);
			}
			
			return getAllTCforTag(prodid, tags, msc);
			
		}
		
		public List<String> mergeAndEliminateDuplicates(List<String> list1, List<String> list2) {
	        // Create a Set to store unique values
	        Set<String> uniqueValues = new HashSet<>();

	        // Add all elements from list1 to the Set
	        uniqueValues.addAll(list1);

	        // Add all elements from list2 to the Set
	        uniqueValues.addAll(list2);

	        // Create a new List to hold the merged and deduplicated values
	        List<String> mergedList = new ArrayList<>(uniqueValues);

	        return mergedList;
	    }
		
		public JSONArray getAllTCforTag(int prodid, List<String> tags, MySQlConn msc) {
			
			String tagStr = tags.get(0);
			
			for(int i=1; i<tags.size();i++) {
				tagStr =tagStr + "|";
				tagStr =tagStr + tags.get(i);
			}
			
			String sql = "select * from cta.test_case tc "
					+ "join cta.modules mod1 on tc.Module = mod1.idmodules "
					+ "join cta.products prod on mod1.product = prod.idproducts "
					+ "where idproducts = " + prodid + " and tc.testcasedescription "
							+ "REGEXP '#(" + tagStr + ")'";
			
			String countSql = "select * from cta.test_case tc "
					+ "join cta.modules mod1 on tc.Module = mod1.idmodules "
					+ "join cta.products prod on mod1.product = prod.idproducts "
					+ "where idproducts = " + prodid + " and tc.testcasedescription "
					+ "REGEXP '#(" + tagStr + ")'";
			
			return msc.tcsearch(sql, countSql, AppProperties.listlimitInt);			
		}
		
		public void updateTestStepWithScript(int teststepattrid, String fileName, String script) {
			String sql = null;
			try {
				sql = "UPDATE cta.test_step_attr SET  "
						+ "scriptfile = ?, script=? WHERE test_step = ?";
				PreparedStatement stmt = testCon2.prepareStatement(sql);
				stmt.setString(1, fileName);
				stmt.setString(2, script);
				stmt.setInt(3, teststepattrid);
				stmt.executeUpdate();
				stmt.close();
			}catch(Exception e) {
				System.err.println(Utilities.getNow());
				e.printStackTrace();
			}
		}
		
		public void updateTestStepWithCmd(int teststepattrid, String fileName) {
			String sql = null;
			try {
				sql = "UPDATE cta.test_step_attr SET  "
						+ "cmdfile = ? WHERE test_step = ?";
				PreparedStatement stmt = testCon2.prepareStatement(sql);
				stmt.setString(1, fileName);
				stmt.setInt(2, teststepattrid);
				stmt.executeUpdate();
				stmt.close();
			}catch(Exception e) {
				System.err.println(Utilities.getNow());
				e.printStackTrace();
			}
		}
		
		public String generateLicense(String forCompany, String requestedBy,
				String toDate, String tcCount, MySQlConn msc) {
			int companyid = Integer.valueOf(forCompany);
			return generateLicense(companyid, requestedBy,
					toDate, tcCount, msc);
		}
		
		public String generateLicense(int companyid, String requestedBy,
				String toDate, String tcCount, MySQlConn msc) {
			String codeDownloadPrmsn = "off";
			String performanceTestingPrmsn = "off";
			String coveragePrmsn = "off";
			String apiDataPrmsn = "off";
			String parallelThreadCnt = "1";
			String vRecPrmsn = "off";
			String NLPCreationPrmsn = "off";
			String chatBotPrmsn = "off";
			
			JSONObject requestedUserType = msc.getUserDetails(requestedBy, 59);
			
			if(requestedUserType == null || requestedUserType.size() == 0) {
				return "License Key Generation Error 4286";
			}
			
			int userType = (int)requestedUserType.get("usertype");
			
			if(userType != 1) {
				return "License Key Generation Error 9825";
			}
			
			String licReq = toDate + AppProperties.licenselimiter + tcCount + AppProperties.licenselimiter + codeDownloadPrmsn
					+ AppProperties.licenselimiter + performanceTestingPrmsn + AppProperties.licenselimiter + coveragePrmsn
					+ AppProperties.licenselimiter + apiDataPrmsn + AppProperties.licenselimiter + parallelThreadCnt
					+ AppProperties.licenselimiter + vRecPrmsn + AppProperties.licenselimiter + NLPCreationPrmsn
					+ AppProperties.licenselimiter + chatBotPrmsn;
			
			try {
				licReq = PasswordUtils.encryptLicenseKey(licReq);
			} catch (Exception e) {
				return "License Key Generation Error 2781";
			}
			
//			int companyid = Integer.valueOf(forCompany);
			String sql = "UPDATE cta.licenses SET  "
					+ "licusage = ? WHERE companyid = ?";
			
			try {			
				PreparedStatement stmt = testCon2.prepareStatement(sql);
				stmt.setString(1, licReq);
				stmt.setInt(2, companyid);
				stmt.executeUpdate();
				stmt.close();
				
//				sql = "INSERT INTO cta.licenseusage (companyid, datecreated,createdBy, "
//						+ "licusage) VALUES (?,?,?,?)";
//				stmt = testCon2.prepareStatement(sql);
//				stmt.setInt(1, companyid);
//				stmt.setTimestamp(2, Utilities.getCurrentTimestamp());
//				stmt.setString(3, requestedBy);
//				stmt.setString(4, licReq);
//				stmt.executeUpdate();
//				stmt.close();
				insertLicenseCount( companyid,  requestedBy,  licReq,Integer.valueOf(tcCount));
			} catch (Exception e) {
				return "License Key Generation Error 6092";
			}
			
			JSONObject license = PasswordUtils.getLicenseDetails(companyid, msc, this);
			Utilities.cacheLicense(companyid, license);
			
			return licReq;
		}
		
		public void insertLicenseCount(int companyid, String requestedBy, String licReq, 
				int tccount) throws Exception{
		
			String sql = "INSERT INTO cta.licenseusage (companyid, datecreated,createdBy, "
					+ "licusage, tccount) VALUES (?,?,?,?,?)";
			PreparedStatement stmt = testCon2.prepareStatement(sql);				
			stmt = testCon2.prepareStatement(sql);
			stmt.setInt(1, companyid);
			stmt.setTimestamp(2, Utilities.getCurrentTimestamp());
			stmt.setString(3, requestedBy);
			stmt.setString(4, licReq);
			stmt.setInt(5, tccount);
			stmt.executeUpdate();
			stmt.close();
		}
		
		public void decrementLicense(int companyId, String requestedBy) {
			JSONObject license = PasswordUtils.getLicenseDetails(companyId, msc, this);
			
			if(license == null || license.size() == 0) {
				System.err.println("License count could not be decremented");
				return ;
			}
			
			int tcCount = (int)license.get("tcCount");
			if(tcCount > 0) {
				tcCount = tcCount - 1;
				String licReq = (String)license.get("toDate") + AppProperties.licenselimiter +  String.valueOf(tcCount) + AppProperties.licenselimiter + (String)license.get("codeDownloadPrmsn") 
						+ AppProperties.licenselimiter + (String)license.get("performanceTestingPrmsn")  + AppProperties.licenselimiter + (String)license.get("coveragePrmsn") 
						+ AppProperties.licenselimiter + (String)license.get("apiDataPrmsn")  + AppProperties.licenselimiter + String.valueOf(license.get("parallelThreadCnt")) 
						+ AppProperties.licenselimiter + (String)license.get("vRecPrmsn")  + AppProperties.licenselimiter + (String)license.get("NLPCreationPrmsn") 
						+ AppProperties.licenselimiter + (String)license.get("chatBotPrmsn");
				
				try {
					licReq = PasswordUtils.encryptLicenseKey(licReq);
				} catch (Exception e) {
					System.err.println("License count could not be updated to db");
				}	
				try {
//					msc2.updateLicenseCount(licReq, companyId);
					insertLicenseCount( companyId,  requestedBy,  licReq,Integer.valueOf(tcCount));
				} catch (Exception e) {
					System.err.println(Utilities.getNow());
					e.printStackTrace();
				}
			} else {
				System.err.println("License count could not be decremented as it is already 0");
			}
		}
		
		public void updateLicenseCount(String licusage, int companyId) {
			PreparedStatement stmt = null;
			
			try {
				String sql = "UPDATE cta.licenseusage SET "
						+ "licusage = ? WHERE companyid = ?";
				stmt = testCon2.prepareStatement(sql);
				stmt.setString(1, licusage);
				stmt.setInt(2, companyId);
				stmt.executeUpdate();
			} catch (Exception e) {
				System.err.println(Utilities.getNow());
				e.printStackTrace();
			} finally {
				try {
					stmt.close();
				} catch (Exception e1) {
					
				}
			}
		}
		
		public String updateLicenseUsage(String forCompany, String toDate, String tcCount, String codeDownloadPrmsn, 
				String performanceTestingPrmsn, String coveragePrmsn, String apiDataPrmsn, String parallelThreadCnt, 
				String vRecPrmsn, String NLPCreationPrmsn, String chatBotPrmsn) {
			
			PreparedStatement stmt = null;
			int companyid = Integer.valueOf(forCompany);
			String result="";
			
			JSONObject license = Utilities.getLicenseFromCache(companyid, msc, this);
			if(tcCount==null || tcCount.equals("")) {
				tcCount = (String)license.get("tcCount"); 
			}
			if(toDate==null || toDate.equals("")) {
				toDate = (String)license.get("toDate");
			}
			try {
				String licusage = toDate + AppProperties.licenselimiter + tcCount + AppProperties.licenselimiter + codeDownloadPrmsn
						+ AppProperties.licenselimiter + performanceTestingPrmsn + AppProperties.licenselimiter + coveragePrmsn
						+ AppProperties.licenselimiter + apiDataPrmsn + AppProperties.licenselimiter + parallelThreadCnt
						+ AppProperties.licenselimiter + vRecPrmsn + AppProperties.licenselimiter + NLPCreationPrmsn
						+ AppProperties.licenselimiter + chatBotPrmsn;
				
				try {
					licusage = PasswordUtils.encryptLicenseKey(licusage);
				} catch (Exception e) {
					return "License Key Generation Error 2781";
				}
				
				String sql = "UPDATE cta.licenseusage SET licusage = ? WHERE companyid = ?";
				stmt = testCon2.prepareStatement(sql);
				stmt.setString(1, licusage);
				stmt.setInt(2, companyid);
				stmt.executeUpdate();
				
				sql = "UPDATE cta.licenses SET licusage = ? WHERE companyid = ?";
				stmt = testCon2.prepareStatement(sql);
				stmt.setString(1, licusage);
				stmt.setInt(2, companyid);
				stmt.executeUpdate();
				
				license = PasswordUtils.getLicenseDetails(companyid, msc, this);
				Utilities.cacheLicense(companyid, license);
				
			} catch (Exception e) {
				System.err.println(Utilities.getNow());
				e.printStackTrace();
				result = "An exception occured called "+ e;
			} finally {
				try {
					stmt.close();
				} catch (Exception e1) {
					
				}
			}
			result = "The License usage update is successful";
			return result;
		}
		
		public JSONObject getLicense(int companyId) {
			JSONObject result = new JSONObject();
			PreparedStatement stmt = null;
			ResultSet rs = null;
			try {
				String sql = "SELECT * FROM cta.licenseusage where companyid = ? "
						+ "order by idlicenseusage desc limit 1";
				stmt = testCon2.prepareStatement(sql);
				stmt.setInt(1, companyId);
				rs = stmt.executeQuery();
				
				if(rs.next()) {
					result.put("idlicenseusage", rs.getInt("idlicenseusage"));
					result.put("companyid", rs.getInt("companyid"));
					result.put("testcaseid", rs.getInt("testcaseid"));
					result.put("testcasename", rs.getString("testcasename"));
					result.put("key", rs.getInt("key"));
					result.put("datecreated", rs.getTimestamp("datecreated"));
					result.put("createdBy", rs.getString("createdBy"));
					result.put("licusage", rs.getString("licusage"));
				}
				
			} catch (Exception e) {
				System.err.println(Utilities.getNow());
				e.printStackTrace();
			} finally {
				try {
					stmt.close();
					rs.close();
				} catch (Exception e1) {
					System.err.println(Utilities.getNow());
					e1.printStackTrace();
				}
			}
			
			return result;
		}
		
		public JSONObject updateTCSettings(String browser,String envUrl, 
				String proxy, String res, int multirun, int threshold, 
				int tcid, String envName, boolean cont, boolean audio, 
				boolean video, boolean force, String tcDescription, 
				String isWait, boolean stealthMode) {
			JSONObject result = new JSONObject();
			String sql = "UPDATE cta.test_case SET browser = ?, envurl = ?, proxyurl = ?, "
					+ "multirun = ?, testcasethreshold = ?, envname = ?, continuetest = ?, "
					+ "enableaudio = ?, enablevideo = ?, forcenewsession = ?, testcasedescription = ?, "
					+ "isWait = ?, stealthMode = ? where idtest_case = ?";
			try {
				PreparedStatement stmt = testCon2.prepareStatement(sql);
				stmt.setString(1, browser);
				stmt.setString(2, envUrl);
				stmt.setString(3, proxy);
				stmt.setInt(4, multirun);
				stmt.setInt(5, threshold);
				stmt.setString(6, envName);
				stmt.setBoolean(7, cont);
				stmt.setBoolean(8, audio);
				stmt.setBoolean(9, video);
				stmt.setBoolean(10, force);
				stmt.setString(11, tcDescription);
				stmt.setString(12, isWait);
				stmt.setBoolean(13, stealthMode);
				stmt.setInt(14, tcid);
				stmt.executeUpdate();
				stmt.close();
				result.put("message", "success");
				if(res != null && !res.equalsIgnoreCase("")) {
					updateResolutionForTC(res, tcid);
				}
			} catch (Exception e) {
				System.err.println(Utilities.getNow());
				e.printStackTrace();
				result.put("message", "failed to update");
			}
			
			return result;
		}
		
		public void updateResolutionForTC(String res, int tcid) {
			String sql = "UPDATE cta.test_case SET width = ?, height = ? where idtest_case = ?";
			try {
				
				String[] resdet = res.split("x");
				int width = Integer.valueOf(resdet[0]);
				int height = Integer.valueOf(resdet[1]);
				
				PreparedStatement stmt = testCon2.prepareStatement(sql);
				stmt.setInt(1, width);
				stmt.setInt(2, height);
				stmt.setInt(3, tcid);
				stmt.executeUpdate();
				stmt.close();
			} catch (Exception e) {
				
			}
		}
		
		public JSONObject getNLPForTC(int tcid) {
			String sql = "SELECT * from cta.test_case_nlp where test_case = ?";
			JSONObject result = new JSONObject();
			try {
				PreparedStatement stmt = testCon2.prepareStatement(sql);
				stmt.setInt(1, tcid);
				ResultSet rs = stmt.executeQuery();
				
				if(rs.next()) {
					result.put("test_case", rs.getInt("test_case"));
					result.put("nlpsteps", rs.getString("nlpsteps"));
				}
				rs.close();
				stmt.close();
			} catch (Exception e) {
				System.err.println(Utilities.getNow());
				e.printStackTrace();
			}
			
			return result;
		}
		
		public JSONArray getApiOnTestCase(int testcaseid) {
			
			String sql = "SELECT * FROM cta.apidata WHERE testcase=? ORDER BY createddate ASC";
			JSONArray json = new JSONArray();
			try {
				PreparedStatement stmt = testCon2.prepareStatement(sql);
				stmt.setInt(1, testcaseid);
				ResultSet rs = stmt.executeQuery();
				
				Set<String> uniqueUrls = new HashSet<>();
				while(rs.next()) {
					JSONObject jsonObj = new JSONObject();
					jsonObj.put("apiid", rs.getInt("idapidata"));
					jsonObj.put("testcase", rs.getInt("testcase"));
					String url = rs.getString("url");
					jsonObj.put("url", url);
					jsonObj.put("createddate", rs.getString("createddate"));
					jsonObj.put("type", null);
					jsonObj.put("body", null);

					if (uniqueUrls.add(url)) {
					    json.add(jsonObj);
					}
				}
				rs.close();
				stmt.close();
			} catch (Exception e) {
				System.err.println(Utilities.getNow());
				e.printStackTrace();
			}
			return json;
		}
		
		public JSONObject getApiDetailOnTestCase(int apiid) {
			
			String sql = "SELECT * FROM cta.apidata WHERE idapidata=?";
			JSONObject jsonObj = new JSONObject();			try {
				PreparedStatement stmt = testCon2.prepareStatement(sql);
				stmt.setInt(1, apiid);
				ResultSet rs = stmt.executeQuery();
				if(rs.next()) {
					jsonObj.put("testcase", rs.getInt("testcase"));
					String url = rs.getString("url");
					jsonObj.put("url", url);
					jsonObj.put("createddate", rs.getString("createddate"));
					jsonObj.put("type", null);
					jsonObj.put("body", null);
				}
				rs.close();
				stmt.close();
			} catch (Exception e) {
				System.err.println(Utilities.getNow());
				e.printStackTrace();
			}
			return jsonObj;
		}
		
		public void getAIGenNameofStepsofSuite(int suiteid) {
			JSONArray json = msc.getTestCaseFromSuiteid(suiteid);
			for(Object obj : json) {
				JSONObject jsonObj = (JSONObject) obj;
				int tcid =(int) jsonObj.get("idtest_case");
//				String sql = "SELECT ts.idtest_step, ts.Step_Number, ts.Page_Description, ts.Action, ts.Keyword, ts.TestData, ts.altpath, ts.outerhtml, tsa.nearestname  from cta.test_step ts \r\n"
//						+ "JOIN cta.test_step_attr tsa on ts.idtest_step = tsa.test_step Where ts.Test_Case_Id=" + tcid;
				 String sql = "SELECT * FROM cta.test_step WHERE Test_Case_Id = ? AND (status = 0 OR status = NULL)"
						 			+ "ORDER BY Step_Number, eventTime ASC";
				try {
					PreparedStatement stmt = testCon2.prepareStatement(sql);
					stmt.setInt(1, tcid);
					ResultSet rs = stmt.executeQuery();
					JSONObject TestStep = msc.getExecutableTestStepsAsJSON(rs, false, false, false, false);
					JSONObject stepData = (JSONObject) TestStep.get("data");
					rs.close();
					stmt.close();
					for (Object key : stepData.keySet()) {
		                JSONObject dataObject = (JSONObject) stepData.get(key);

		                // Extracting the values
		                int idtest_step = (int) dataObject.get("idtest_step");
		                int Step_Number = (int) dataObject.get("Step_Number");
		                String Page_Description = (String) dataObject.get("Page_Description");
		                String Action = (String) dataObject.get("Action");
		                String Keyword = (String) dataObject.get("Keyword");
		                String TestData = (String) dataObject.get("TestData");
		                String altpath = (String) dataObject.get("altpath");
		                String outerhtml = (String) dataObject.get("outerhtml");
		                String nearestname = (String) dataObject.get("nearestname");

						if(!(outerhtml == null || outerhtml.equals("")) && (Keyword != null && !Keyword.equals("EditBox"))){
							nearestname = outerHtmlAttribute(outerhtml);
						} else if(!(altpath == null || altpath.equals(""))) {
							nearestname = nearestname;
						} else nearestname = Page_Description;

						ExcelWriterForGenAi(tcid, Step_Number, Page_Description, Action, Keyword, TestData, nearestname);
					}
				} catch(Exception e) {
					System.err.println(Utilities.getNow());
					e.printStackTrace();
				}
			}
		}
		
	    public String outerHtmlAttribute(String html) {
	        //html = "<input aria-describedby=\"username\" autofocus=\"\" class=\"form-control\" id=\"username\" name=\"username\" placeholder=\"Enter Username\" required=\"\" type=\"text\" value=\"\">";

	        String ariaDescribedBy = getValueIfPresentForOuterHTML(html, "aria-describedby");
	        String placeholder = getValueIfPresentForOuterHTML(html, "placeholder");
	        String name = getValueIfPresentForOuterHTML(html, "name");
	        String id = getValueIfPresentForOuterHTML(html, "id");
	        String result = null;

	        if (ariaDescribedBy != null && !ariaDescribedBy.isEmpty()) {
	            result = ariaDescribedBy;
		        return result;
	        } else if (placeholder != null && !placeholder.isEmpty()) {
	        	result = placeholder;
		        return result;
	        } else if(name != null && !name.isEmpty()){
	        	result = name;
		        return result;
	        } else {
	        	result = id;
		        return result;
	        }
	    }

	    private static String getValueIfPresentForOuterHTML(String html, String attribute) {
	        int startIndex = html.indexOf(attribute + "=\"");
	        if (startIndex != -1) {
	            startIndex += attribute.length() + 2; // Add 2 to move past the attribute name and the equals sign
	            int endIndex = html.indexOf("\"", startIndex);
	            if (endIndex != -1) {
	                return html.substring(startIndex, endIndex);
	            }
	        }
	        return null;
	    }


	    public void ExcelWriterForGenAi(int idtest_case, int stepNumber, String pageDescription, String action, 
	    		String keyword, String testData, String nearestname) {
		    try {
		        // Open Excel file
		        String excelFilePath = "C:\\Nogrunt\\output.xlsx"; // Provide your desired file path
		                Workbook workbook;
		                Sheet sheet;
		                if (new File(excelFilePath).exists()) {
		                    FileInputStream inputStream = new FileInputStream(new File(excelFilePath));
		                    workbook = new XSSFWorkbook(inputStream);
		                    sheet = workbook.getSheetAt(0); // Assuming you want to write to the first sheet
		                } else {
		                    workbook = new XSSFWorkbook();
		                    sheet = workbook.createSheet("Sheet1");
		                    // Write headers
		                    Row headerRow = sheet.createRow(0);
		                    headerRow.createCell(0).setCellValue("ID");
		                    headerRow.createCell(1).setCellValue("Step Number");
		                    headerRow.createCell(2).setCellValue("Page Description");
		                    headerRow.createCell(3).setCellValue("Action");
		                    headerRow.createCell(4).setCellValue("Keyword");
		                    headerRow.createCell(5).setCellValue("Test Data");
		                    headerRow.createCell(6).setCellValue("Nearest Name");
		                }

		                // Write data to Excel
		                int rowCount = sheet.getLastRowNum();
		                Row row = sheet.createRow(++rowCount);

		                row.createCell(0).setCellValue(idtest_case);
		                row.createCell(1).setCellValue(stepNumber);
		                row.createCell(2).setCellValue(pageDescription);
		                row.createCell(3).setCellValue(action);
		                row.createCell(4).setCellValue(keyword);
		                row.createCell(5).setCellValue(testData);
		                row.createCell(6).setCellValue(nearestname);

		                // Write to file
		                FileOutputStream outputStream = new FileOutputStream(excelFilePath);
		                workbook.write(outputStream);
		                workbook.close();
		                outputStream.close();
		            } catch (Exception e) {
		            	System.err.println(Utilities.getNow());
		                e.printStackTrace();
		            }
		        }
	    
	    public void writeErrorForTestStep(int test_step, String errorType, String key, String error,
	    		String oldValue, String newValue, int analysistc) {
	    	
	    	String sql1 = "INSERT into cta.test_Step_error (test_step, errortype,analysistcr)"
	    			+ "Values (?,?,?)";
	    	
	    	String sql2 = "INSERT into cta.test_Step_errorkey (test_step_error, errorkey, errorseverity, "
	    			+ "oldValue, latestvalue) Values (?,?,?, ?, ?)";
	    	
	    	try {
	    		PreparedStatement stmt = testCon2.prepareStatement(sql1, Statement.RETURN_GENERATED_KEYS);
	    		stmt.setInt(1, test_step);
				stmt.setString(2, errorType);
				stmt.setInt(3, analysistc);
				stmt.executeUpdate();
				int errorKey = -1;
				ResultSet generated = stmt.getGeneratedKeys();	
				if (generated.next()) {
					errorKey = (generated.getBigDecimal(1)).intValue();
					
					stmt = testCon2.prepareStatement(sql2);
					stmt.setInt(1, errorKey);
					stmt.setString(2, key);
					stmt.setString(3, error);
					stmt.setString(4, oldValue);
					stmt.setString(5, newValue);
					stmt.execute();
				}
				generated.close();
				stmt.close();
	    		
	    	} catch (Exception e) {
	    		System.err.println(Utilities.getNow());
	    		e.printStackTrace();
	    	}
	    	
	    }
	    
	    public JSONObject getErrorForTestStep(int tsid, int stepNumber, int companyId,
	    		int tcid, String randomkey) {
	    	
	    	JSONObject result = new JSONObject();
	    	
	    	String sql = "SELECT * FROM cta.test_step_errorkey AS tsek " + 
	                  "JOIN cta.test_step_error AS tse ON tse.idtest_step_error = tsek.test_step_error " +
	                  "JOIN cta.test_step AS ts ON ts.idtest_step = tse.test_step " +
	                  "WHERE ts.idtest_step = ? "+
	                  "AND tse.analysistcr = (SELECT MAX(analysistcr) FROM cta.test_step_errorkey AS tsek2 " +
	                                          "JOIN cta.test_step_error AS tse2 ON tse2.idtest_step_error = tsek2.test_step_error " +
	                                          "JOIN cta.test_step AS ts2 ON ts2.idtest_step = tse2.test_step " +
	                                          "WHERE ts2.idtest_step = ?) " +
	                  "ORDER BY tse.analysistcr DESC";
	    	JSONArray jsonA = new JSONArray();
	    	try {
	    		PreparedStatement stmt = testCon2.prepareStatement(sql);
	    		stmt.setInt(1, tsid);
	    		stmt.setInt(2, tsid);
	    		ResultSet rs = stmt.executeQuery();	
	    		String analysisSSDir = Utilities.getAnalysisSSDir(companyId, tcid);
	    		
	    		while(rs.next()) {	    			
	    			JSONObject attr = new JSONObject();
					attr.put("errorkey", rs.getString("errorkey"));
					attr.put("errorseverity", rs.getString("errorseverity"));
					attr.put("oldValue", rs.getString("oldValue"));
					attr.put("latestvalue", rs.getString("latestvalue"));
					jsonA.add(attr);
	    		}
	    		
	    		result.put("data", jsonA);
	    		
	    		String origFileStr = Utilities.getAnalysisSSFileName(tcid, stepNumber , 
	    				companyId, "orig");
	    		String latestFileStr = Utilities.getAnalysisSSFileName(tcid, stepNumber , 
	    				companyId, "latest");
	    		
	    		String origAbsLoc = analysisSSDir + origFileStr;
	    		String lateAbsLoc = analysisSSDir + latestFileStr;
	    		
	    		File origFile = new File(origAbsLoc);
	    		File lateFile = new File(lateAbsLoc);
	    		
	    		String origurl = null;
	    		String latesturl = null;
	    		
	    		if(origFile.exists()) {
	    			origurl = AppProperties.fileurl + "?"
	    				+ "action=downloadFiless&token="+randomkey+"&fileName="+origFileStr;
	    		}
	    		
	    		if(lateFile.exists()) {
	    			latesturl = AppProperties.fileurl + "?"
	    				+ "action=downloadFiless&token="+randomkey+"&fileName="+latestFileStr;
	    		}
	    		result.put("image1", origurl);
	    		result.put("image2", latesturl);
	    		rs.close();
	    		stmt.close();
	    	} catch (Exception e) {
	    		System.err.println(Utilities.getNow());
	    		e.printStackTrace();
	    	}
	    	
	    	return result;
	    }
	    
	    public JSONObject getImageDiffForTestStep(int tsid, int stepNumber, int companyId,
	    		int tcid, String randomkey, int tcrid) {
	    	
	    	JSONObject result = new JSONObject();
	    	JSONArray jsonA = new JSONArray();
	    	try {	    		
	    		result.put("data", jsonA);
	    		int lastestPass = msc.getLatestPassResult(tcid);
	    		String origFileStr = Utilities.getSSFileName(tcid, tsid, lastestPass, stepNumber, 
	    				companyId, "afterAction");
	    		String latestFileStr = Utilities.getSSFileName(tcid, tsid, tcrid, stepNumber, 
	    				companyId, "afterAction");
	    		
	    		String origAbsLoc = Utilities.getScreenShotsDir(companyId, tcid, lastestPass) 
	    				+ origFileStr;
	    		String lateAbsLoc = Utilities.getScreenShotsDir(companyId, tcid, tcrid) 
	    				+ latestFileStr;
	    		
	    		File origFile = new File(origAbsLoc);
	    		File lateFile = new File(lateAbsLoc);
	    		
	    		String origurl = null;
	    		String latesturl = null;
	    		
	    		if(origFile.exists()) {
	    			origurl = AppProperties.fileurl + "?"
	    				+ "action=downloadFile&token="+randomkey+"&fileName="+origFileStr;
	    		}
	    		
	    		if(lateFile.exists()) {
	    			latesturl = AppProperties.fileurl + "?"
	    				+ "action=downloadFile&token="+randomkey+"&fileName="+latestFileStr;
	    		}
	    		result.put("image1", origurl);
	    		result.put("image2", latesturl);

	    	} catch (Exception e) {
	    		System.err.println(Utilities.getNow());
	    		e.printStackTrace();
	    	}
	    	
	    	return result;
	    }
	    
	    public void moveTestCaseResultsForAnalysis(int analysistc, int tcr) {
	    	String sql = " UPDATE cta.test_case_Results set Test_Case_Id = ? where idtest_case_results = ?";
	    	
	    	try {
	    		PreparedStatement stmt = testCon2.prepareStatement(sql);
	    		stmt.setInt(1, analysistc);
	    		stmt.setInt(2, tcr);
	    		stmt.execute();
	    		stmt.close();
	    	} catch (Exception e) {
	    		System.err.println(Utilities.getNow());
	    		e.printStackTrace();
	    		System.err.println(e.getMessage());
	    	}
	    }
	    
		public String getPageNamefromPageid(int pid) {
			String sql = "SELECT pagename FROM cta.pages WHERE pageid=?";
			String pname = "";
			try {
				PreparedStatement stmt = testCon2.prepareStatement(sql);
				stmt.setInt(1, pid);
				ResultSet rs = stmt.executeQuery();

				if(rs.next()) {
					pname = rs.getString(1);
				}
				rs.close();
				stmt.close();
			} catch (Exception e) {
				System.err.println(Utilities.getNow());
				e.printStackTrace();
			}
			return pname;
		}
	    
		public void getNearestNameOnPage(int prodid) {
		    String sql = "SELECT DISTINCT pe.pageid, pe.elementtype, pe.nearestname, p.pagename, pe.clickable FROM cta.page_elements pe "
		    		+ "JOIN cta.testcase_page tp ON tp.idpage = pe.pageid "
		    		+ "JOIN cta.pages p ON p.pageid=tp.idpage WHERE tp.testcaseid=? and pe.status=0";
		    
		    try (PreparedStatement stmt = testCon2.prepareStatement(sql)) {
		        stmt.setInt(1, prodid);
		        try (ResultSet rs = stmt.executeQuery()) {
		            // Prepare Excel file
		            String excelFilePath = "C:\\Nogrunt\\output_" + prodid + "_.xlsx";
		            Workbook workbook;
		            Sheet sheet;
		            if (new File(excelFilePath).exists()) {
		                workbook = new XSSFWorkbook(new FileInputStream(new File(excelFilePath)));
		                sheet = workbook.getSheetAt(0);
		            } else {
		                workbook = new XSSFWorkbook();
		                sheet = workbook.createSheet("Sheet1");
		                // Write headers
		                Row headerRow = sheet.createRow(0);
		                headerRow.createCell(0).setCellValue("ID");
		                headerRow.createCell(1).setCellValue("Element Type");
		                headerRow.createCell(2).setCellValue("Element Name");
		            }

		            Map<String, Integer> pageRowIndices = new HashMap<>();

		            while (rs.next()) {
		                String elementtype = rs.getString("elementtype");
		                String clickable = rs.getString("clickable");
		                if(elementtype.equalsIgnoreCase("INPUT") || elementtype.equalsIgnoreCase("BUTTON") || 
		                		elementtype.equalsIgnoreCase("LABEL") || elementtype.equalsIgnoreCase("SELECT")
		                		|| elementtype.equalsIgnoreCase("TEXTAREA")) {
		                	elementtype = elementtype;
		                } else if (elementtype.equalsIgnoreCase("A")) {
		                	elementtype = "LINK";
		                } else {
		                	if(clickable.equals("1")) {
		                		elementtype = "LINK";
		                	} else elementtype = "LABEL";
		                }
		                
		                String nearestname = rs.getString("nearestname");
		                String pagename = rs.getString("pagename");
		                int pageId = rs.getInt("pageid");

		                if (!pageRowIndices.containsKey(pagename)) {
		                    int newRowNum = sheet.getLastRowNum() + 1;
		                    Row pageRow = sheet.createRow(newRowNum);
		                    pageRow.createCell(1).setCellValue(pagename);
		                    sheet.addMergedRegion(new CellRangeAddress(newRowNum, newRowNum, 1, 2));
		                    pageRow.getCell(1).getCellStyle().setAlignment(HorizontalAlignment.CENTER);
		                    pageRowIndices.put(pagename, newRowNum);
		                }

		                int currentRowIndex = pageRowIndices.get(pagename);
		                Row row = sheet.createRow(sheet.getLastRowNum() + 1);
		                row.createCell(0).setCellValue(pageId);
		                row.createCell(1).setCellValue(elementtype);
		                row.createCell(2).setCellValue(nearestname);
		            }

		            try (FileOutputStream outputStream = new FileOutputStream(excelFilePath)) {
		                workbook.write(outputStream);
		            }
			        rs.close();
		        }
		        stmt.close();
		    } catch (SQLException | IOException e) {
		    	System.err.println(Utilities.getNow());
		        e.printStackTrace();
		    }
		}
		
		public void getPageRelationForAi(int prodid) {
		    String sql = "SELECT * FROM cta.pagerelation where productid=?";
		    
		    try (PreparedStatement stmt = testCon2.prepareStatement(sql)) {
		        stmt.setInt(1, prodid);
		        try (ResultSet rs = stmt.executeQuery()) {
		            // Prepare Excel file
		            String excelFilePath = "C:\\Nogrunt\\Relation_output_" + prodid + ".xlsx";
		            Workbook workbook;
		            Sheet sheet;
		            if (new File(excelFilePath).exists()) {
		                workbook = new XSSFWorkbook(new FileInputStream(new File(excelFilePath)));
		                sheet = workbook.getSheetAt(0);
		            } else {
		                workbook = new XSSFWorkbook();
		                sheet = workbook.createSheet("Sheet1");
		                // Write headers
		                Row headerRow = sheet.createRow(0);
		                headerRow.createCell(0).setCellValue("ID");
		                headerRow.createCell(1).setCellValue("From Page");
		                headerRow.createCell(2).setCellValue("To Page");
		            }

		            // Write data to Excel
		            int rowCount = sheet.getLastRowNum();
		            while (rs.next()) {
		                String frompage = getPageNamefromPageid(rs.getInt("pagefrom"));
		                String topage = getPageNamefromPageid(rs.getInt("pageto"));
		                

		                Row row = sheet.createRow(++rowCount);
		                row.createCell(0).setCellValue(prodid);
		                row.createCell(1).setCellValue(frompage);
		                row.createCell(2).setCellValue(topage);
		            }

		            // Write to file
		            try (FileOutputStream outputStream = new FileOutputStream(excelFilePath)) {
		                workbook.write(outputStream);
		            }
			        rs.close();
		        }
		        stmt.close();
		    } catch (SQLException | IOException e) {
		    	System.err.println(Utilities.getNow());
		        e.printStackTrace();
		    }
		}
		
		public void downloadTestSuiteResultFile(int tsrid, int companyId, 
				HttpServletResponse response) {
			
			Workbook workbook = downloadTestSuiteResultFile(tsrid, companyId);
			try {
			ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            workbook.close();

            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setHeader("Content-Disposition", "attachment; filename=testSuite_result_"+tsrid+".xlsx");
            response.setContentLength(out.size());

            OutputStream outputStream = response.getOutputStream();
            out.writeTo(outputStream);
            out.close();
            outputStream.flush();
			} catch (Exception e) {
				System.err.println(Utilities.getNow());
				e.printStackTrace();
				System.err.println(e.getMessage());
			}
			
		}
		

		public Workbook downloadTestSuiteResultFile(int tsrid, int companyId) {
			String sql = "SELECT * FROM cta.test_suite_case_results tscr JOIN cta.test_case_results tcr on tcr.idtest_case_results = tscr.test_case_results_id WHERE tscr.test_suite_results_id=?";
			Workbook workbook = new XSSFWorkbook();
			
			try {    
	            Sheet sheet = workbook.createSheet("Test Results");
	            
	            Row headerRow = sheet.createRow(0);
	            headerRow.createCell(0).setCellValue("Executed Date");
	            headerRow.createCell(1).setCellValue("Test Case Name");
	            headerRow.createCell(2).setCellValue("Result");
	            headerRow.createCell(3).setCellValue("Log File");
	            
				PreparedStatement stmt = testCon2.prepareStatement(sql);
				stmt.setInt(1, tsrid);
				ResultSet rs = stmt.executeQuery();
				
				int rowCount = 1;
				while(rs.next()) {
					String executedDate = rs.getString("Executed_Date");
					String testcaseName = rs.getString("test_case_name");
					String result = rs.getString("Status");
					int testcaseid = rs.getInt("Test_Case_Id");
					int tcrid = rs.getInt("idtest_case_results");
					String token = msc.getLicenceToken(companyId);
					String fileurl = AppProperties.fileurl + "?action=executionlog&token="+token+"&companyid="+companyId+"&tcid="+testcaseid+"&tcrid="+tcrid;
					
	                Row row = sheet.createRow(rowCount++);
	                row.createCell(0).setCellValue(executedDate);
	                row.createCell(1).setCellValue(testcaseName);
	                row.createCell(2).setCellValue(result);
//	                row.createCell(3).setCellValue("LogFile");
	                
	                CreationHelper createHelper = workbook.getCreationHelper();
	                Hyperlink hyperlink = createHelper.createHyperlink(HyperlinkType.URL);
	                hyperlink.setAddress(fileurl);
	                Cell logFileCell = row.createCell(3);
	                logFileCell.setCellValue("LogFile");
	                logFileCell.setHyperlink(hyperlink);
		
				}
				rs.close();
				stmt.close();
				
		    } catch (Exception e) {
		    	System.err.println(Utilities.getNow());
		        e.printStackTrace();
		    }
			return workbook;
		}
		
		public void downloadTestCaseResultFile(int tcrid, int companyId, 
				HttpServletResponse response) {
			
			Workbook workbook = downloadTestCaseResultFile(tcrid, companyId);
			try {
			ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            workbook.close();

            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setHeader("Content-Disposition", "attachment; filename=testCase_result_"+tcrid+".xlsx");
            response.setContentLength(out.size());

            OutputStream outputStream = response.getOutputStream();
            out.writeTo(outputStream);
            out.close();
            outputStream.flush();
			} catch (Exception e) {
				System.err.println(Utilities.getNow());
				e.printStackTrace();
				System.err.println(e.getMessage());
			}			
		}
		

		public Workbook downloadTestCaseResultFile(int tcrid, int companyId) {
			String sql = "SELECT Executed_Date, Step_Number, Action, Keyword, Status as teststep_status, Object_Xpath FROM "
					+ "cta.test_step_result WHERE Test_Case_Results_Id=?";
			Workbook workbook = new XSSFWorkbook();
			
			try {    
	            Sheet sheet = workbook.createSheet("Test Case Results");
	            
	            Row headerRow = sheet.createRow(0);
	            headerRow.createCell(0).setCellValue("Executed Date");
	            headerRow.createCell(1).setCellValue("Step Number");
	            headerRow.createCell(2).setCellValue("Action");
	            headerRow.createCell(3).setCellValue("Keyword");
	            headerRow.createCell(4).setCellValue("TestStep Result");
	            headerRow.createCell(5).setCellValue("XPath");
	            
				PreparedStatement stmt = testCon2.prepareStatement(sql);
				stmt.setInt(1, tcrid);
				ResultSet rs = stmt.executeQuery();
				
				int rowCount = 1;
				while(rs.next()) {
					String executedDate = rs.getString("Executed_Date");
					int stepNumber = rs.getInt("Step_Number");
					String action = rs.getString("Action");
					String keyword = rs.getString("Keyword");
					String ts_result = rs.getString("teststep_status");
					String Xpath = rs.getString("Object_Xpath");
					
	                Row row = sheet.createRow(rowCount++);
	                row.createCell(0).setCellValue(executedDate);
	                row.createCell(1).setCellValue(stepNumber);
	                row.createCell(2).setCellValue(action);
	                row.createCell(3).setCellValue(keyword);
	                row.createCell(4).setCellValue(ts_result);
	                row.createCell(5).setCellValue(Xpath);
	                
				}
				rs.close();
				stmt.close();
				
		    } catch (Exception e) {
		    	System.err.println(Utilities.getNow());
		        e.printStackTrace();
		    }
			return workbook;
		}
		
		public void triggerBulkApi(String token, String randomKey, int companyId) {
			String str = "SELECT * FROM cta.api Where randomkey = ? AND status = 2 Order By createddate asc";
			try {
				PreparedStatement stmt = testCon2.prepareStatement(str);
				stmt.setString(1, token);
				ResultSet rs = stmt.executeQuery();
				randomKey = Utilities.randomGen(AppProperties.NUMBER, 7, false);
				while(rs.next()) {
	                String url = rs.getString("url") + "&performanceTest=1";
	                String type = rs.getString("type");
	                String body = rs.getString("body");
	                
	                JSONParser parser = new JSONParser();
	                JSONObject jsonObject = (JSONObject) parser.parse(body);
	                jsonObject.put("key", Long.parseLong(randomKey));	                    
	                String scenario = (String) jsonObject.get("Scenario");
	                jsonObject.put("Scenario", scenario + "_ForPerformanceTest");
	                String modifiedBody = jsonObject.toJSONString();
	                executeApiRequest(url, type, modifiedBody);
				}
				rs.close();
				stmt.close();
		    } catch (Exception e) {
		    	System.err.println(Utilities.getNow());
		        e.printStackTrace();
		    }
		}
		
		public String updateTestSteptoTestSequence(int companyId, int testCase, int tcLimit) {
			String str = "UPDATE cta.test_step set tsSequence=Step_Number WHERE Test_Case_Id=? AND tsSequence = -1";
			String response = "";
			try {
				PreparedStatement stmt = null;
				if(tcLimit == -2) {
					stmt = testCon2.prepareStatement(str);
					stmt.setInt(1, testCase);
					stmt.executeUpdate();
//					el.logExecution("Scheduler runs query being run " + sql);
				} else if(tcLimit == -1) {
					JSONArray testcases = msc.getALLTestCaseFromCompanyid(companyId);
			        for (Object obj : testcases) {
			            JSONObject testcase = (JSONObject) obj;
			            int tcid = (int) testcase.get("idtest_case");
						stmt = testCon2.prepareStatement(str);
						stmt.setInt(1, tcid);
						stmt.executeUpdate();
			        }
				} else {
					int x = getTestcaseUpdatedTill(companyId);
					int y = x+tcLimit;
					JSONArray testcases = msc.getALLTestCaseFromCompanyid(companyId);
			        for (int i = x; i < testcases.size() && tcLimit > 0; i++) {
			            JSONObject testcase = (JSONObject) testcases.get(i);
			            int tcid = (int) testcase.get("idtest_case");
						stmt = testCon2.prepareStatement(str);
						stmt.setInt(1, tcid);
						stmt.executeUpdate();
			            tcLimit--;
			        }
			        updateTestcaseUpdatedTill(companyId, y);
			        
				}
				stmt.close();
				response = "Update Successful";
		    } catch (Exception e) {
		    	System.err.println(Utilities.getNow());
		        e.printStackTrace();
		        response = "Exception "+e;
		    }
			return response;
		}
		
		public int getTestcaseUpdatedTill(int cid) {
			String str = "SELECT testcaseupdatedtill FROM cta.company where idcompany=?"; 
			int value = 0;
			try {
				PreparedStatement stmt =  testCon2.prepareStatement(str);
				stmt.setInt(1, cid);
				ResultSet rs = stmt.executeQuery();
				if(rs.next()) {
					value = rs.getInt(1);
				}
				rs.close();
				stmt.close();
			} catch (Exception e) {
				System.err.println(Utilities.getNow());
				e.printStackTrace();
			}
			return value;
		}
		
		public void updateTestcaseUpdatedTill(int cid, int value) {
			String str = "UPDATE cta.company SET testcaseupdatedtill=? where idcompany=?"; 
			try {
				PreparedStatement stmt =  testCon2.prepareStatement(str);
				stmt.setInt(1,value);
				stmt.setInt(2, cid);
				stmt.executeUpdate();
				stmt.close();
			} catch (Exception e) {
				System.err.println(Utilities.getNow());
				e.printStackTrace();
			}
		}
		
		private static void executeApiRequest(String url, String type, String body) {
		    HttpURLConnection connection = null;
			 DataOutputStream outputStream = null;
			 BufferedReader in = null;
		    try {
		        // Create URL object
		        URL apiUrl = new URL(url);

		        // Open connection
		        connection = (HttpURLConnection) apiUrl.openConnection();

		        // Set request method
		        connection.setRequestMethod(type);
		        connection.setRequestProperty("Content-Type", "application/json");
		        
		        // If request type is POST, set the request body
		        if ("POST".equalsIgnoreCase(type) && body != null && !body.isEmpty()) {
		            connection.setDoOutput(true);
			        outputStream = new DataOutputStream(connection.getOutputStream());
			        outputStream.writeBytes(body);
			        outputStream.flush();
		        }

		        // Get the response code
		        int responseCode = connection.getResponseCode();
		        System.out.println("Executing API request:");
		        System.out.println("URL: " + url);
		        System.out.println("Type: " + type);
		        System.out.println("Body: " + body);
		        System.out.println("Response Code: " + responseCode);

		        // Read the API response
		        in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
		        String inputLine;
		        StringBuffer response = new StringBuffer();
		        while ((inputLine = in.readLine()) != null) {
		            response.append(inputLine);
		        }
		        
		    } catch (Exception e) {
		    	System.err.println(Utilities.getNow());
		        e.printStackTrace();
		    } finally {
		        // Disconnect from the API endpoint
		        if (connection != null) {
		            connection.disconnect();
		        }
		        try {
		            if (outputStream != null) {
		                outputStream.close();
		            }
		            if (in != null) {
		                in.close();
		            }
		        } catch (Exception e) {
		        	System.err.println(Utilities.getNow());
		            e.printStackTrace();
		        }
		     }
		}
		
		public String downloadTestSteps(int tcid, String randomKey, HttpServletResponse response) {
			JSONObject jsonObj = msc.getTestStepsByTestCaseID(tcid, false);
			JSONObject jsnoData = (JSONObject) jsonObj.get("data");
			StringBuilder allSteps = new StringBuilder();
			
			for (int i = 2; i<jsnoData.size(); i++) {
                JSONObject step = (JSONObject) jsnoData.get(i);
                String action = (String) step.get("Action");
                String testData = (String) step.get("TestData");
                String keyword = (String) step.get("Keyword");
                String pageDescription = (String) step.get("Page_Description");
                String nearestName = (String) step.get("nearestname");
                
                String stepDescription = formatStepDetails(action, testData, keyword, pageDescription, nearestName);
                allSteps.append(stepDescription).append(System.lineSeparator());
			}
			
	        String fileName = tcid + randomKey + ".txt";
	        String fileContents = allSteps.toString();

	        try {
	            response.setContentType("text/plain");
	            response.setHeader("Content-Disposition", "attachment; filename=\"" + fileName + "\"");

	            response.getOutputStream().write(fileContents.getBytes());
	            response.getOutputStream().flush();
	        } catch (IOException e) {
	        	System.err.println(Utilities.getNow());
	            e.printStackTrace();
	            return "File download unsuccessful: " + e.getMessage();
	        }

	        return "File downloaded successfully: " + fileName;
	    }

		private String formatStepDetails(String action, String testData, String keyword, String pageDescription, String nearestName) {
	        StringBuilder description = new StringBuilder();
	        
	        if((testData.equals("\u200B") ||testData.equals("")) && keyword.equals("") 
	        		&& (pageDescription.equals("\u200B") || pageDescription.equals(""))) {
	        	 description.append(action).append(" ").append(nearestName);
	        } else {
		        if ("URL".equalsIgnoreCase(keyword)) {
		            description.append(keyword).append(" ").append(testData);
		        } else if ("Enter".equalsIgnoreCase(action)) {
		            description.append(action).append(" ").append(testData).append(" into ").append(pageDescription);
		        } else if ("Click".equalsIgnoreCase(action)) {
		            description.append(action).append(" ").append(keyword).append(" ").append(pageDescription);
		        } else {
		        	description.append(action).append(" ").append(keyword);
		        }
	        }

	        return description.toString();
		}
		
		public void DownloadPomClassForTestcase(int pid, int companyId, String randomkey, HttpServletResponse response) {
			
//			GenAllPomClassesForProduct(pid, companyId); //For testing purpose - updating all pom class files before download
			
	        String folderName = "GenPoms_"+pid;
		    String zipFileName = folderName + ".zip";
		    File folderToZip = new File(Utilities.getPomZipFile(companyId, pid, folderName));
		    File zipFile = new File(Utilities.getPomZipFile(companyId, pid, zipFileName));
		    
		    try (FileOutputStream fos = new FileOutputStream(zipFile);
		         ZipOutputStream zos = new ZipOutputStream(fos)) {
		        
		        zipFolder(folderToZip, folderToZip.getName(), zos);
		    } catch (IOException e) {
		    	System.err.println(Utilities.getNow());
		        e.printStackTrace();
		        return;
		    }
		    
		    // Write the zip file to the response
		    try (FileInputStream fis = new FileInputStream(zipFile)) {
		        response.setContentType("application/zip");
		        response.setHeader("Content-Disposition", "attachment; filename=\"" + zipFileName + "\"");
		        response.setContentLength((int) zipFile.length());
		        
		        OutputStream os = response.getOutputStream();
		        byte[] buffer = new byte[1024];
		        int bytesRead;
		        while ((bytesRead = fis.read(buffer)) != -1) {
		            os.write(buffer, 0, bytesRead);
		        }
		        os.flush();
		    } catch (IOException e) {
		    	System.err.println(Utilities.getNow());
		        e.printStackTrace();
		    }
		}
		
		public void GenAllPomClassesForProduct(int prodid,int companyId) {
			String sql = "SELECT * FROM pages where prodid = ? and status =0";
			try {
				PreparedStatement stmt = testCon2.prepareStatement(sql);
				stmt.setInt(1, prodid);
				ResultSet rs = stmt.executeQuery();
				
				while(rs.next()) {
					int pageid = rs.getInt("pageid");
					GenPomClassForTestcase(pageid,prodid, companyId);
				}
				rs.close();
				stmt.close();
			} catch (Exception e){
				System.err.println(Utilities.getNow());
				e.printStackTrace();
			}
		}
		
		public void GenPomClassForTestcase(int pageid,int prodid, int companyId) {
			String sql = "SELECT DISTINCT pe.pageid, pe.elementtype, pe.nearestname, p.pagename, pe.clickable, pe.recordedxpath, pe.elementname, pe.elementid, "
					+ "pe.uniquename FROM cta.page_elements pe JOIN cta.pages p ON p.pageid=pe.pageid WHERE p.pageid=? and pe.status=0 "
					+ "and pe.clickable = 1 and recorded = true order by pe.pageid;";
	        
	        String folderName = "GenPoms_"+prodid;
	        Utilities.createGenPomFolder(companyId, prodid ,folderName);
		    
		    try (PreparedStatement stmt = testCon2.prepareStatement(sql)) {
		        stmt.setInt(1, pageid);
		        ResultSet rs = stmt.executeQuery();
				GenPomClass gpc = new GenPomClass(msc);
				gpc.genCode(companyId, rs, folderName, prodid);
		        rs.close();
		        stmt.close();
		    } catch (Exception e) {
		    	System.err.println(Utilities.getNow());
		    	e.printStackTrace();
		    }
		}
		
		private void zipFolder(File folderToZip, String parentFolder, ZipOutputStream zos) throws IOException {
		    File[] files = folderToZip.listFiles();
		    for (File file : files) {
		        if (file.isDirectory()) {
		            zipFolder(file, parentFolder + "/" + file.getName(), zos);
		            continue;
		        }
		        try (FileInputStream fis = new FileInputStream(file)) {
		            ZipEntry zipEntry = new ZipEntry(parentFolder + "/" + file.getName());
		            zos.putNextEntry(zipEntry);
		            
		            byte[] buffer = new byte[1024];
		            int bytesRead;
		            while ((bytesRead = fis.read(buffer)) != -1) {
		                zos.write(buffer, 0, bytesRead);
		            }
		            zos.closeEntry();
		        }
		    }
		}
		
		public void updatePomUniqueElement(int eid, String uname) {
			String sql = "UPDATE cta.page_elements SET uniquename = ? WHERE elementid = ?";
			try {
				if(uname.length() > 45) {
					uname = uname.substring(uname.length() - 45,uname.length());
				}
				
				PreparedStatement stmt =  testCon2.prepareStatement(sql);
				stmt.setString(1, uname);
				stmt.setInt(2, eid);
				stmt.executeUpdate();
				
		        stmt.close();
			} catch (Exception e) {
				System.err.println(Utilities.getNow());
				e.printStackTrace();
			}
		}
		
		public boolean doesUniqueNameExist(String uname, int pageId) {
		    String sql = "SELECT COUNT(*) FROM cta.page_elements WHERE uniquename = ? AND pageid = ?";
		    try (PreparedStatement stmt = testCon2.prepareStatement(sql)) {
		        stmt.setString(1, uname);
		        stmt.setInt(2, pageId);
		        try (ResultSet rs = stmt.executeQuery()) {
		            if (rs.next()) {
		                return rs.getInt(1) > 0; // If count > 0, the name exists for this pageId
		            }
		        }
		    } catch (Exception e) {
		        System.err.println(Utilities.getNow() + " - Error checking unique name existence.");
		        e.printStackTrace();
		    }
		    return false; // Default to false if an error occurs
		}
		
		public String getPomUniqueName(int eid) {
			String result = "";
			String str = "SELECT DISTINCT pe.uniquename, pe.elementtype, pe.pageid, p.pagename FROM cta.page_elements pe "
					+ "JOIN cta.pages p ON p.pageid=pe.pageid WHERE pe.elementid=?";
			try {
				PreparedStatement stmt =  testCon2.prepareStatement(str);
				stmt.setInt(1, eid);
				ResultSet rs = stmt.executeQuery();
				if(rs.next()) {
					String uniquename = rs.getString("uniquename");
					String elementtype = rs.getString("elementtype");
					String pagename = rs.getString("pagename");
					int pageid = rs.getInt("pageid");
					pagename = pagename.replaceAll("[^a-zA-Z0-9]", "");
					result = uniquename +"~"+ elementtype+"~"+pagename+"~"+pageid;
				}
				rs.close();
		        stmt.close();
			} catch (Exception e) {
				System.err.println(Utilities.getNow());
				e.printStackTrace();
			}
			return result;
		}
		
		public int updatePageElementForMissingElement(String name, String type, int pageid, String xpath, String nearestname, String uname, int tsid) {
			String sql = "INSERT INTO cta.page_elements (elementname, elementtype, pageid, recordedxpath, clickable, recorded, "
					+ "coveragecount, nearestname, status, uniquename) VALUE (?,?,?,?,?,?,?,?,?,?)";
			String sql_update = "UPDATE cta.test_step_attr SET elementId=? WHERE test_step=?";
			int elementid = -1;
			try {
				PreparedStatement stmt = testCon2.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
				stmt.setString(1, name);
				stmt.setString(2, type);
				stmt.setInt(3,pageid);
				stmt.setString(4, xpath);
				stmt.setInt(5, 1);
				stmt.setInt(6, 0);
				stmt.setInt(7, 0);
				stmt.setString(8, nearestname);
				stmt.setInt(9, 0);
				stmt.setString(10, uname);
				stmt.executeUpdate();
				ResultSet generated = stmt.getGeneratedKeys();	
				if (generated.next()) {
					elementid = (generated.getBigDecimal(1)).intValue();
				}
				generated.close();
				
				stmt = testCon2.prepareStatement(sql_update);
				stmt.setInt(1, elementid);
				stmt.setInt(2, tsid);
				stmt.executeUpdate();
				stmt.close();
			} catch (Exception e) {
				System.err.println(Utilities.getNow());
				e.printStackTrace();
			}
			return elementid;
		}
		
		public void updateCvrgAbsXpathWithRelativeXpath(int tsid, int elementid) {
			String sql = "SELECT Object_Xpath FROM cta.test_step WHERE idtest_step = ?";
			String updatesql = "UPDATE cta.page_elements SET recordedxpath = ? WHERE elementid=?";
			try{
				PreparedStatement stmt =  testCon2.prepareStatement(sql);
				stmt.setInt(1, tsid);
				ResultSet rs = stmt.executeQuery();
				if(rs.next()) {
					String objXpath = rs.getString(1);
					stmt = testCon2.prepareStatement(updatesql);
					stmt.setString(1, objXpath);
					stmt.setInt(2, elementid);
					stmt.executeUpdate();
				}
				rs.close();
				stmt.close();
			} catch (Exception e) {
				System.err.println(Utilities.getNow());
				e.printStackTrace();
			}
		}
		
		public void updateTCElementAsRecorded(int tcid, int elementid) {
			String updatesql = "UPDATE cta.page_element_testcase SET recorded = true "
					+ "WHERE elementid=? AND testcaseid = ? AND recorded = false";
			try{
				PreparedStatement stmt =  testCon2.prepareStatement(updatesql);
				stmt.setInt(1, elementid);
				stmt.setInt(2, tcid);
				int rowsAffected = stmt.executeUpdate();
				
				if(rowsAffected > 0) {
					updateCoverageCount(elementid);
					updatePageElementAsRecorded(elementid);
				}
				stmt.close();
			} catch (Exception e) {
				System.err.println(Utilities.getNow());
				e.printStackTrace();
			}
		}
		
		public void updatePageElementAsRecorded(int elementid) {
			String updatesql = "UPDATE cta.page_elements SET recorded = true "
					+ "WHERE elementid=? AND recorded = false";
			try{
				PreparedStatement stmt =  testCon2.prepareStatement(updatesql);
				stmt.setInt(1, elementid);
				stmt.executeUpdate();
				stmt.close();
			} catch (Exception e) {
				System.err.println(Utilities.getNow());
				e.printStackTrace();
			}
		} 
		
		public int createApiResults(int apiid, String name, String executedby, String envurl,
				String type, String body, String url, String headers, String tests) {
			
			String sql = "insert into cta.apiresults (idapi, name, executedby, executeddate, envurl, "
					+ "type, body, url, headers, tests) values (?,?,?,?,?,?,?,?,?,?)";
			int generatedkey = -1;
			try {
				PreparedStatement stmt =  testCon2.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
				stmt.setInt(1, apiid);
				stmt.setString(2, name);
				stmt.setString(3, executedby);
				stmt.setTimestamp(4, Utilities.getCurrentTimestamp());
				stmt.setString(5, envurl);
				stmt.setString(6, type);
				stmt.setString(7, body);
				stmt.setString(8, url);
				stmt.setString(9, headers);
				stmt.setString(10, tests);
				stmt.executeUpdate();
				ResultSet generated = stmt.getGeneratedKeys();	
				if (generated.next()) {
					generatedkey = (generated.getBigDecimal(1)).intValue();
				}
				generated.close();
				stmt.close();
			} catch (Exception e) {
				System.err.println(Utilities.getNow());
				System.err.println(e.getMessage());
			}
			
			return generatedkey;
		}
		
		public void deleteApi(int apiid, int modid) {
			String sql = "update cta.api set status = 1 where idapi = ? and moduleid = ?";
			try {
				PreparedStatement stmt =  testCon2.prepareStatement(sql);
				stmt.setInt(1, apiid);
				stmt.setInt(2, modid);
				stmt.executeUpdate();
				stmt.close();
			} catch (Exception e) {
				System.err.println(Utilities.getNow());
				System.err.println(e.getMessage());
			}
		}
		
		
		public JSONObject enrichElementsWithIds(JSONObject inputJson, int productId) {
		    JSONObject resultJson = new JSONObject();
		    JSONObject wrapper = (JSONObject) inputJson.get("0");
		    JSONArray pagesArray = (JSONArray) wrapper.get("pages");

		    JSONArray enrichedPages = new JSONArray();

		    for (int i = 0; i < pagesArray.size(); i++) {
		        JSONObject page = (JSONObject) pagesArray.get(i);
		        String pageName = (String) page.get("page_name");

		        int pageId = getPageIdByPageUrlAndProduct(pageName, productId);
		        if (pageId == -1) {
		            System.err.println("Page ID not found for: " + pageName);
		        }

		        JSONArray elements = (JSONArray) page.get("elements");
		        for (int j = 0; j < elements.size(); j++) {
		            JSONObject element = (JSONObject) elements.get(j);
		            String elementName = (String) element.get("element_name");
		            
		            int elementId = -1;
		            if (pageId == -1) {
		            	elementId = -2;
			        }
		            else  {
		            	elementId = getElementIdByNameAndPageId(elementName, pageId);
		            }
		            element.put("element id", elementId);
		        }

		        enrichedPages.add(page);
		    }

		    JSONObject wrapped = new JSONObject();
		    wrapped.put("pages", enrichedPages);
		    resultJson.put("0", wrapped);
		    return resultJson;
		}

		
		
		private int getPageIdByPageUrlAndProduct(String pageName, int productId) {
		    int pageId = -1;

		    String sql = "SELECT pe.pageid " +
		                 "FROM modules m " +
		                 "JOIN cta.test_case tc ON tc.Module = m.idmodules " +
		                 "JOIN cta.test_step ts ON ts.Test_Case_Id = tc.idtest_case " +
		                 "JOIN cta.test_step_attr tsa ON tsa.test_step = ts.idtest_step " +
		                 "JOIN cta.page_elements pe ON pe.elementid = tsa.elementid " +
		                 "WHERE m.product = ? AND tsa.pageurl = ? LIMIT 1";

		    try {
		        PreparedStatement ps = testCon2.prepareStatement(sql);
		        ps.setInt(1, productId);
		        ps.setString(2, pageName);

		        ResultSet rs = ps.executeQuery();
		        if (rs.next()) {
		            pageId = rs.getInt("pageid");
		        }

		        rs.close();
		        ps.close();
		    } catch (SQLException e) {
		        System.err.println("Error fetching pageId for productId=" + productId + ", pageName=" + pageName);
		        e.printStackTrace();
		    }

		    return pageId;
		}


		private int getElementIdByNameAndPageId(String uiElementName, int pageId) {
		    int elementId = -1;

		    // Normalizing both DB column and input string using chained REPLACE()
		    String sql = "SELECT elementid FROM cta.page_elements " +
	                 "WHERE pageid = ? AND " +
	                 "LOWER(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(" +
	                 "elementname, ' ', ''), '-', ''), '(', ''), ')', ''), '.', ''), ',', ''), '\\'', ''), '\"', ''), '/', ''), '*', ''), '’', '')) = " +
	                 "LOWER(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(" +
	                 "?, ' ', ''), '-', ''), '(', ''), ')', ''), '.', ''), ',', ''), '\\'', ''), '\"', ''), '/', ''), '*', ''), '’', '')) " +
	                 "LIMIT 1";

		    // Fallback query on nearestsname if first fails
		    String fallbackSql = sql.replace("elementname", "nearestname");

		    try {
		        // First attempt: match against elementname
		        PreparedStatement ps = testCon2.prepareStatement(sql);
		        ps.setInt(1, pageId);
		        ps.setString(2, uiElementName);
		        ResultSet rs = ps.executeQuery();

		        if (rs.next()) {
		            elementId = rs.getInt("elementid");
		        }

		        rs.close();
		        ps.close();

		        // Fallback: try matching against nearestsname
		        if (elementId == -1) {
		            PreparedStatement ps2 = testCon2.prepareStatement(fallbackSql);
		            ps2.setInt(1, pageId);
		            ps2.setString(2, uiElementName);
		            ResultSet rs2 = ps2.executeQuery();

		            if (rs2.next()) {
		                elementId = rs2.getInt("elementid");
		            }

		            rs2.close();
		            ps2.close();
		        }

		    } catch (SQLException e) {
		        System.err.println("Error fetching element ID for name: '" + uiElementName + "', page ID: " + pageId);
		        e.printStackTrace();
		    }

		    return elementId;
		}



		
}
