package nogrunt.codegenNew;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import org.apache.commons.lang3.StringUtils;

import java.sql.ResultSet;
import java.sql.PreparedStatement;
import java.util.List;
import java.util.Random;
import org.json.simple.JSONObject;

import nogrunt.codegen.SelJava;
import nogrunt.*;

public class PomClassCodeGen {
	
	int waittime = 4000;
	SelJava sj = new SelJava();
	MySQlConn msc = null;
	MySqlConn2 msc2 = null;
	CodeGenHelper codegenHelper;

	public CodeGenHelper getCodegenHelper() {
		return codegenHelper;
	}

	public void setCodegenHelper(CodeGenHelper codegenHelper) {
		this.codegenHelper = codegenHelper;
	}

	public PomClassCodeGen(MySQlConn msc) {
		this.msc = msc;
		msc2 = new MySqlConn2(msc);
	}
	
	
	public JSONObject getTeststepFromelement(int elementid) {
		try {
			String gettsid = "SELECT test_step from test_step_attr where elementId = ?";
			PreparedStatement stmt = msc.testCon.prepareStatement(gettsid);
			stmt.setInt(1, elementid);
			ResultSet rs = stmt.executeQuery();
			if(rs.next()) {
				int tsid = rs.getInt(1);
				rs.close();
				stmt.close();
				
				String getTS = "SELECT * from test_step where idtest_step = ?";
				stmt = msc.testCon.prepareStatement(getTS);
				stmt.setInt(1, tsid);
				rs = stmt.executeQuery();
				rs.next();
				return msc.getTestStepsFromDb(rs, false, true);
			}
		} catch (Exception e) {
			System.err.println(Utilities.getCurrentTimestamp());
			e.printStackTrace();
		}
		
		return null;
	}

	public void genPOMClassCode(int companyId, ResultSet rs, String folderName, int prodid, boolean isMultipleLocatorPom) {
    	String currentPage = null;
        Path latestFilePath = null;
        
        try {
        	while (rs.next()) {
                String elementtype = rs.getString("elementtype");
                String clickable = rs.getString("clickable");
                String elementname = rs.getString("elementname");
                int elementid = rs.getInt("elementid");
                String xpath = rs.getString("recordedxpath");
                String nearestname = rs.getString("nearestname");
                String uniquename = rs.getString("uniquename");
                
                JSONObject step = getTeststepFromelement(elementid);
                
                if(step == null || step.size() == 0) {
                	continue;
                }
                
                if(step != null && step.size() > 0) {
	        		String elementplaceholder = (String)step.get("elementplaceholder");
	        		int elementplaceholderindex = (int)step.get("elementplaceholderindex");
	        		String keyword = (String)step.get("Keyword");
	        		String action = (String)step.get("Action");
	        		if(keyword != null && action != null &&
	        				keyword.equalsIgnoreCase("url") &&
	        				keyword.equalsIgnoreCase("get")) {
	        			continue;
	        		}
                }
                
                String pattern = ".*/BUTTON(\\[.*\\])?/SPAN$";
                if (xpath.matches(pattern)) {
                	elementtype = "BUTTON";
                	elementname = nearestname;
                }
                
                String pagename = rs.getString("pagename");
                int pageId = rs.getInt("pageid");
                String fname = pagename.replaceAll("[^a-zA-Z0-9]", "")+pageId;
                
                if(uniquename == null || uniquename.equals("") || uniquename.matches("\\d+")) {
                    Random rand = new Random();
                    int randomNumber = 100 + rand.nextInt(900);
                    if(nearestname != null && !nearestname.equals("")) {
	                    uniquename = nearestname.replaceAll("[^a-zA-Z0-9]", "");
	            	    // This is to check if the name contains only digits as it was causing issue in the java file.
	            	    if (uniquename.matches("\\d+") || uniquename.equals("")) {
	            	    	uniquename = "removableString" + uniquename;
	            	    }
                	} else {
                		uniquename = "noname";
                	}
            	    uniquename += randomNumber;
            	    if(uniquename.length() > 45) {
            	    	uniquename = uniquename.substring(uniquename.length() - 45,uniquename.length());
    				}
            	    
            	    while (msc2.doesUniqueNameExist(uniquename, pageId)) { //checks DB existence of uniquename
            	        int extraRandom = 10 + rand.nextInt(90);
            	        uniquename += extraRandom;

            	        // Trim if length exceeds 45 characters
            	        if (uniquename.length() > 45) {
            	            uniquename = uniquename.substring(uniquename.length() - 45);
            	        }
            	    }
            	    
                    msc2.updatePomUniqueElement(elementid, uniquename);
                }
                
                //Condition for page change
                if(currentPage == null || !currentPage.equals(fname)) {
                	String tempPath = codegenHelper.getPomTemplateFileName();
                	String fileName = fname + codegenHelper.getPomFileExtension();
                	String targetPath = Utilities.getGenPomFilePath(companyId, prodid, folderName, fileName);
                	sj.copyResourceFile(tempPath, targetPath);
        	        File latestFile = new File(targetPath);
        	        latestFilePath = latestFile.toPath();
        	        codegenHelper.replacePomClassInitializations(latestFilePath, fname, isMultipleLocatorPom);
        	        currentPage = fname;
                }
       
                codegenHelper.addTextForPomClassElementLocator(latestFilePath, uniquename, elementtype, step, isMultipleLocatorPom);
                
                codegenHelper.addTextForPomClassElementMethod(latestFilePath, uniquename, elementtype, step, isMultipleLocatorPom);
                
            }
        } catch(Exception e) {
        	e.printStackTrace();
        } 
    }
	
	
	
	
}
