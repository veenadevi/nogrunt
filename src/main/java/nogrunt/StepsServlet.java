package nogrunt;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.Base64;
import java.util.HashMap;
import java.io.FileWriter;
import java.io.BufferedWriter;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import jakarta.servlet.ServletContext;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Servlet implementation class StepsServlet
 */
@WebServlet("/StepsServlet")
@MultipartConfig(
		  fileSizeThreshold = 1024 * 1024 * 1, // 1 MB
		  maxFileSize = 1024 * 1024 * 10,      // 10 MB
		  maxRequestSize = 1024 * 1024 * 100   // 100 MB
		)
public class StepsServlet extends HttpServlet {
	
	public static final int DEFAULT_BUFFER_SIZE = 8192;
//	private static HashMap results = new HashMap();
	private static final long serialVersionUID = 1L;
	private Thread queueThread;
	private QueuingHelper qh;
	private Thread coverageThread;
	private CoverageHelper ch;
	private Thread auditQueueThread;
	private AuditQueuingHelper auditQueue;
	
	private static int index = 0;

    /**
     * Default constructor. 
     */
    public StepsServlet() {
        // TODO Auto-generated constructor stub
    }
    
    @Override
    public void init() throws ServletException {
     	
    	qh = new QueuingHelper();
    	ServletContext servletContext = getServletContext();
    	servletContext.setAttribute("queueWriterThread",qh);
    	int consumerCount = Integer.valueOf(AppProperties.consumerCount);
        queueThread = new QueueReader(qh);
        queueThread.start();
        
        Thread[] threads = new Thread[consumerCount - 1];
        for(int i=0;i<consumerCount-1;i++) {
        	threads[i] = new QueueReader(qh);
        	threads[i].setName("queueThread" + i+2);
        	threads[i].start();
        }
        
    	ch = new CoverageHelper();
    	ServletContext servletContextC = getServletContext();
    	servletContextC.setAttribute("coverageWriterThread",ch);
    	coverageThread = new CoverageReader(ch);
    	coverageThread.start();
        
        auditQueue = new AuditQueuingHelper();
		Utilities.aqh = auditQueue;
		auditQueueThread = new AuditQueueReader(auditQueue);
		auditQueueThread.start();
		
		Utilities util = new Utilities();
		servletContext.setAttribute("userCache",util);
        AppProperties.getProperties();
    }
    
    private void configResponse(HttpServletResponse response)
    {
       response.setContentType("image/jpeg");
       response.addHeader("Access-Control-Allow-Origin", AppProperties.reactappurl);
       response.addHeader("Access-Control-Allow-Methods", "POST, GET, OPTIONS, PUT, DELETE, HEAD");
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
		response.getWriter().append("Served at: ").append(request.getContextPath());
		System.out.println("Test1");
		doPost(request,response);
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

		String action = request.getParameter("action");
		String performanceTest = request.getParameter("performanceTest");
		
		JSONObject audit = new JSONObject();
		audit.put("action", action);
	
		String title = request.getParameter("name");
		String stepNumber = request.getParameter("step");
		
		ServletContext servletContext = getServletContext();
		Utilities userCache = (Utilities)servletContext.getAttribute("userCache");
		
		if(action.equalsIgnoreCase("queuesave") || action.equalsIgnoreCase("apidata")
				|| action.equalsIgnoreCase("analysis") || action.equalsIgnoreCase("openwindow")) {
			try {
				BufferedReader reader = request.getReader();

		    	qh = (QueuingHelper)servletContext.getAttribute("queueWriterThread");

				JSONParser parser = new JSONParser();
				JSONObject step = new JSONObject();
				String decodeStr = "";
				if(performanceTest != null && performanceTest.equals("1")) {
					step = (JSONObject)parser.parse(reader);
				} else {
					String jsonstr = (String) parser.parse(reader);
					decodeStr = URLDecoder.decode(jsonstr, "UTF-8");
					step = (JSONObject)parser.parse(decodeStr);
				}
				
				String tsSeq = request.getParameter("clickcount");
				int tsSequence=-999;

				if (tsSeq != null && !tsSeq.isEmpty()) {
				    try {
				        tsSequence = Integer.parseInt(tsSeq);
				    } catch (NumberFormatException e) {
				    	System.err.println(Utilities.getNow());
				    	e.printStackTrace();
				    }
				} else {
					tsSequence = -1;
				}

				step.put("userCache", userCache);
				String randomkey = "";
				step.put("tsSequence", tsSequence);
				step.put("addBackCounter", 0);              //To control the race condition in the queue.
				if(action.equalsIgnoreCase("analysis")) {
					String analysisTC = request.getParameter("analysisTC");
					String analysiskey = request.getParameter("analysiskey");
					String analysisname = request.getParameter("analysisname");
					step.put("analysisTC", analysisTC);
					step.put("key", analysiskey);
					step.put("mode", "analysis");
					step.put("uname", analysisname);
					audit.put("randomkey", analysiskey);
					audit.put("userid", analysisname);
					randomkey = analysiskey;
					
					JSONObject user = new JSONObject();
					user.put("uname", analysisname);
					user.put("key", analysiskey);
					userCache.cacheUser(analysiskey, user);
					
				} else {
					randomkey = String.valueOf((long) step.get("key"));
					
					if(performanceTest == null) {
						
						if(randomkey == null) {
							JSONObject res = new JSONObject();
							res.put("message", "fail");
							response.getWriter().print(res);
							return;
						} else {
							if(!userCache.isUserInCache(randomkey)) {
								JSONObject res = new JSONObject();
								res.put("message", "fail");
								response.getWriter().print(res);
								return;
							}
						}
						
						MySQlConn msc = null;
						try {
							msc = new MySQlConn(null);
							JSONObject user = userCache.getUserFromCache(randomkey);
							int cid = (int) user.get("companyid");
							msc.setUserDetails(user);
							if(msc.getBulkTestStatus(cid)) {
								String tcName = (String) step.get("Scenario");
								int moduleId = -1;
								if(step.containsKey("moduleid")) {
							   		String moduleIdStr = (String)step.get("moduleid");
							   		moduleId = new Integer(moduleIdStr).intValue();
								}
								String token = msc.getLicenceToken(cid);
								String requestURL = request.getRequestURL().toString() + "?" + request.getQueryString();
								
						        String modifiedUrl = requestURL.replaceAll("randomid=\\d+", "randomid=" + token);
						        
								String type = request.getMethod(); 
								msc.addApi(tcName+"_"+index, 1, 2, moduleId, null, type, 
										decodeStr, modifiedUrl, null, randomkey,null,"",null, 
										-1, null);
								index++;
							}
						} catch (Exception e) {
							System.err.println(Utilities.getNow());
							e.printStackTrace();
						} finally {
							msc.closeDbConn();
						}
					} else {
						JSONObject json = new JSONObject();
						json.put("companyid", 1);
						json.put("uname", "PerformanceTest");
						json.put("usertype", 1);
						
						Utilities.cacheUser(randomkey, json);
					}
				}
					audit.put("userid", userCache.getUserFromCache(randomkey).get("uname"));
					audit.put("randomkey", randomkey);
				
					qh.addToQueue(step);				
					
					JSONObject success = new JSONObject();
					success.put("message", "success");
					audit.put("result", "success");
					AuditQueuingHelper aqh = userCache.aqh;
					aqh.addToQueue(audit);
					response.getWriter().print(success);
//				} 
			}catch (Exception e) {
				System.err.println(Utilities.getNow());
				e.printStackTrace();
			}
		} else if(action.equals("auth")) {
			try {
				BufferedReader reader = request.getReader();
				JSONParser parser = new JSONParser();
				String jsonstr = (String) parser.parse(reader);
				String decodeStr = URLDecoder.decode(jsonstr, "UTF-8");
				JSONObject step = (JSONObject)parser.parse(decodeStr);
				UserActivities ua = new UserActivities();
				JSONObject auditdet = new JSONObject();
				auditdet.put("uname",step.get("uname"));
				auditdet.put("from","extension");
				audit.put("object", auditdet);
				MySQlConn msc = null;
				try {
					msc = new MySQlConn(null);
					JSONObject success = ua.authenticate(jsonstr,msc);
					JSONObject mergeReq = userCache.getMergeRequest((String)step.get("uname"));
					if(mergeReq != null) {
						success.put("mergetc", (long)mergeReq.get("testcaseid"));
						success.put("mergestepnum", (long)mergeReq.get("stepnum"));
						success.put("mergenextnum", (long)mergeReq.get("nextnum"));
						success.put("mergetcname", (String)mergeReq.get("testcasename"));
						Utilities.cacheMergeStepRequest((String)step.get("uname"), success);
						Utilities.flushMergeRequest((String)step.get("uname"));
					}
					audit.put("result", success.get("message"));
					audit.put("randomkey", success.get("randomkey"));
					userCache.aqh.addToQueue(audit);
					response.getWriter().print(success);
				} catch (Exception e) {
					throw e;
				} finally {
					msc.closeDbConn();
				}
				
			} catch (Exception e) {
				System.err.println(Utilities.getNow());
				e.printStackTrace();
			}
		} else if(action.equals("reauth")) {
			
			try {			
				BufferedReader reader = request.getReader();
				JSONParser parser = new JSONParser();
				String jsonstr = (String) parser.parse(reader);
				String decodeStr = URLDecoder.decode(jsonstr, "UTF-8");
				JSONObject step = (JSONObject)parser.parse(decodeStr);
				
				String randomkey = String.valueOf((long) step.get("key"));
				
				JSONObject auditdet = new JSONObject();
				auditdet.put("uname",step.get("uname"));
				auditdet.put("from","extension");
				audit.put("object", auditdet);
				
				JSONObject res = new JSONObject();
				if(!userCache.isUserInCache(randomkey)) {					
					res.put("message", "fail");	
					audit.put("result", "fail");
				} else {
					res.put("message", "pass");	
					audit.put("result", "pass");
				}
				audit.put("randomkey", randomkey);
				userCache.aqh.addToQueue(audit);
				
				response.getWriter().print(res);
			} catch (Exception e) {
				System.err.println(Utilities.getNow());
				e.printStackTrace();
				JSONObject res = new JSONObject();
				res.put("message", "fail");
				response.getWriter().print(res);
			}

		} else if(action.equals("imgsave")) {
			
			try {
				
//				String stepId = request.getParameter("stepId");
				String stepId = (Utilities.randomGen("NUMBER", 8, AppProperties.chatgpton)).toString();
				String name = request.getParameter("testcase");
				double eventTime = 0.0;
				 if(request.getParameter("eventTime") != null) {
					 String eTime = request.getParameter("eventTime");
					 eventTime = new Double(eTime).doubleValue();
				  }
				 long randomKey = -1;
				 if(request.getParameter("randomkey") != null) {
					  randomKey = new Long(request.getParameter("randomkey")).longValue();
				  } 
				
				String directory = AppProperties.testcasesfolder + name ;
				File dir = new File(directory);
				if (!dir.exists()) {
				    dir.mkdirs();
				}
				BufferedReader reader = request.getReader();
				JSONParser parser = new JSONParser();
				String base64Image = (String) parser.parse(reader);					
				String imageDataBytes = base64Image.substring(base64Image.indexOf(",")+1);
				InputStream inputStream = new ByteArrayInputStream(Base64.getDecoder().decode(imageDataBytes.getBytes()));				
				File file = new File(dir + "\\Step" + stepId + ".png");
				copyInputStreamToFile(inputStream, file);
				
		    	qh = (QueuingHelper)servletContext.getAttribute("queueWriterThread");
		    	
		    	JSONObject json = new JSONObject();
		    	json.put("action", "saveImage");
		    	json.put("time", eventTime);
		    	json.put("testcase",  name );
		    	json.put("key", randomKey);
		    	json.put("ssdir", stepId + ".png");
		    	
				String jsonstr = json.toString();
				String encodedString = URLEncoder.encode(jsonstr, "UTF-8");
				
				qh.addToQueue(encodedString);
		    	
		
			} catch (Exception e) {
				System.err.println(Utilities.getNow());
				e.printStackTrace();
			}
		} 	else if(action.equals("mergeTestCase")) {
			try {
				String uname = request.getParameter("uname");
				String json = request.getParameter("json");
				JSONParser parser = new JSONParser();
			    JSONObject jsonObject = (JSONObject) parser.parse(json);

				userCache.cacheMergeRequest(uname,jsonObject);
				response.getWriter().print("success");
			} catch(Exception e) {
				System.err.println(Utilities.getNow());
				e.printStackTrace();
			}
		} 	else if(action.equals("analysis")) {
			try {
				String uname = request.getParameter("uname");
				String json = request.getParameter("json");
				JSONParser parser = new JSONParser();
			    JSONObject jsonObject = (JSONObject) parser.parse(json);

				userCache.cacheMergeRequest(uname,jsonObject);
				response.getWriter().print("success");
			} catch(Exception e) {
				System.err.println(Utilities.getNow());
				e.printStackTrace();
			}	
		} 	else if(action.equals("refresh")) {
			MySQlConn msc = null;
			try {
				String uname = request.getParameter("uname");
				String json = request.getParameter("jsonstr");
				JSONParser parser = new JSONParser();
			    JSONObject jsonObject = (JSONObject) parser.parse(json);
			    UserActivities ua = new UserActivities();
		    	msc = new MySQlConn(null);
			    JSONObject success = ua.refresh(json, msc);
			    JSONObject auditdet = new JSONObject();
				auditdet.put("uname",uname);
				audit.put("object", auditdet);
			    audit.put("result", success.get("message"));
				audit.put("randomkey", success.get("randomkey"));
				userCache.aqh.addToQueue(audit);
				response.getWriter().print(success);
			} catch(Exception e) {
				e.printStackTrace();
			} finally {
				msc.closeDbConn();
			}
		} else if(action.equals("coverage")) {
			MySQlConn msc = null;
			MySqlConn2 MSC = null;
			try {
				 long randomKey = -1;
				 if(request.getParameter("randomid") != null) {
					  randomKey = new Long(request.getParameter("randomid")).longValue();
				  } 
				msc = new MySQlConn(null);
				MSC = new MySqlConn2(msc);
				
				BufferedReader reader = request.getReader();
				JSONParser parser = new JSONParser();
				JSONObject data = (JSONObject) parser.parse(reader);
				
				int tcid = msc.getTestCaseIdFromKey(randomKey);
				
				//for codegen tcid will be -1, so return
				if(tcid == -1) {
					return;
				}
				
				int modid = msc.getModuleFromTestcase(tcid);
				int companyid = msc.getCompanyFromModule(modid);
				
				JSONObject companyData = msc.getCompany(companyid, String.valueOf(randomKey));
				
				String bec = (String)companyData.get("backendcoverage");
				String genType = (String)companyData.get("gentype");
				
				boolean Cflag = false;
				
				if(bec != null && bec.equals("true")) {
					Cflag = true;
				}
				
				if(genType != null && genType.equals("code")) {
					Cflag = false;
				}
				if(Cflag) {
					audit.put("userid", userCache.getUserFromCache(String.valueOf(randomKey)).get("uname"));
					audit.put("randomkey", randomKey);
					audit.put("result", "success");
					AuditQueuingHelper aqh = userCache.aqh;
					aqh.addToQueue(audit);
					
					ch.addToQueue(data);
					msc.updateTCCoverageStatus(tcid);
				} 
				
			} catch (Exception e) {
				e.printStackTrace();
			}	finally {
				msc.closeDbConn();
				MSC.closeDbConn();
			}
		}
	}
	
	private static void copyInputStreamToFile(InputStream inputStream, File file)
            throws IOException {

        // append = false
        try (FileOutputStream outputStream = new FileOutputStream(file, false)) {
            int read;
            byte[] bytes = new byte[DEFAULT_BUFFER_SIZE];
            while ((read = inputStream.read(bytes)) != -1) {
                outputStream.write(bytes, 0, read);
            }
        }

    }
	

	
	public void doOptions(HttpServletRequest req, HttpServletResponse resp)
	        throws IOException {
	    //The following are CORS headers. Max age informs the 
	    //browser to keep the results of this call for 1 day.
	    resp.setHeader("Access-Control-Allow-Origin", "*");
	    resp.setHeader("Access-Control-Allow-Methods", "GET, POST");
	    resp.setHeader("Access-Control-Allow-Headers", "Content-Type");
	    resp.setHeader("Access-Control-Max-Age", "86400");
	    //Tell the browser what requests we allow.
	    resp.setHeader("Allow", "GET, HEAD, POST, TRACE, OPTIONS");
	}

}
