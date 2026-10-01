package nogrunt.servlets;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import jakarta.servlet.ServletContext;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import nogrunt.AppProperties;
import nogrunt.AuditQueueReader;
import nogrunt.AuditQueuingHelper;
import nogrunt.MySQlConn;
import nogrunt.MySqlConn2;
import nogrunt.Utilities;

/**
 * Servlet implementation class StepsServlet
 */
@WebServlet("/videohost")
@MultipartConfig(
		  fileSizeThreshold = 1024 * 1024 * 1, // 1 MB
		  maxFileSize = 1024 * 1024 * 10,      // 10 MB
		  maxRequestSize = 1024 * 1024 * 100   // 100 MB
		)
public class videohost extends HttpServlet {
	
	public static final int DEFAULT_BUFFER_SIZE = 8192;
	private static HashMap results = new HashMap();
	private static final long serialVersionUID = 1L;
	private Thread queueThread;
	private VideoQueuingHelper vqh;
	private Thread auditQueueThread;
	private AuditQueuingHelper auditQueue;

    public videohost() {
        // TODO Auto-generated constructor stub
    }
    
    @Override
    public void init() throws ServletException {
     	
    	vqh = new VideoQueuingHelper();
    	ServletContext servletContext = getServletContext();
    	servletContext.setAttribute("videoQueueWriterThread",vqh);
        
        auditQueue = new AuditQueuingHelper();
		Utilities.aqh = auditQueue;
		auditQueueThread = new AuditQueueReader(auditQueue);
		auditQueueThread.start();
		
		Utilities util = new Utilities();
		servletContext.setAttribute("userCache",util);
        AppProperties.getProperties();
    }

	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		doPost(request,response);
	}

	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

		MySqlConn2 MSC = new MySqlConn2(null);
		MySQlConn msc = new MySQlConn(null);
		
		String action = request.getParameter("action");
		String randomkey = request.getParameter("randomid");
		String companyIdStr = request.getParameter("compid");
		int companyId = -1;
		if(companyIdStr != null && !companyIdStr.equals("undefined")) {
			companyId = new Integer(companyIdStr).intValue();
		}
		
		JSONObject audit = new JSONObject();
		audit.put("action", action);
		
		ServletContext servletContext = getServletContext();
		Utilities userCache = (Utilities)servletContext.getAttribute("userCache");
		
		if(action.equals("videouploade")) {
			try {
				if(randomkey == null) {
					JSONObject res = new JSONObject();
					res.put("message", "fail");
					response.getWriter().print(res);
					return;
				} else {
					if(userCache.getUserFromCache(randomkey) == null) {
						JSONObject res = new JSONObject();
						res.put("message", "fail");
						response.getWriter().print(res);
						return;
					}
				}
				int token = new Integer(randomkey).intValue();
				String fileName = request.getParameter("data");
				JSONObject testcase = MSC.getTcidFromVFName(token, fileName);
				
				if(testcase!=null) {
					int tcid = (int) testcase.get("tcid");
					String uname = (String) testcase.get("username");
					fileName = fileName + ".mp4";
					Utilities.createVideoFolderPath(companyId, tcid);
					String Name = Utilities.getVideoPath(companyId, tcid,  fileName);
					File file = new File(Name);
					InputStream is = request.getInputStream();
				    
					copyInputStreamUsingFiles(is, Name);
					MSC.updateTestCaseFileName(tcid, fileName);
					
					Utilities.callApi(AppProperties.javareactinternalurl,"updateRecordingCache",uname,null,companyId);
				}
				audit.put("userid", userCache.getUserFromCache(randomkey).get("uname"));
				audit.put("randomkey", randomkey);
				
				audit.put("result", "success");
				AuditQueuingHelper aqh = userCache.aqh;
				aqh.addToQueue(audit);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	
	private static void copyInputStreamUsingFiles(InputStream inputStream, String outputFilePath) throws IOException {
	    Path outputPath = Paths.get(outputFilePath);
	    Files.copy(inputStream, outputPath, StandardCopyOption.REPLACE_EXISTING);
	    inputStream.close();
	}

}

