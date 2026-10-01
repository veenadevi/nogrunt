package nogrunt.codegenNew;

import org.json.simple.JSONObject;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.Select;

import nogrunt.*;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Collectors;
import java.nio.charset.StandardCharsets;
import java.nio.file.StandardCopyOption;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.HashSet;
import java.util.Set;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

public class CodeGen {
	
	int stepNum = 1;
	List<String> lines = null;
	boolean replace = false;
	boolean ignore = false;
	MySQlConn msc = null;
	MySqlConn2 msc2 = null;
	final String importStep = "\n//import";
	final String pageConstants = "\n\t\t//pageconstants";
	final String stepNumberStr = "//Step Number -";
	int addHereLineIndex = 10;
	String actionsStr = "Actions actions = new Actions(driver);";
	final String packageDef = "//package";
	final String methodDef = "public void runLogin() {";	
	final String threadWait = "\n			page.waitForTimeout(waitime);";
	final String takeSS = "await page.screenshot({ path: 'ssfilename' });";
	String prevRowiframexpath = "";
	HashMap tabs = new HashMap();
	int prevTabId = -1;
	int prevPageId=-1;
    Set<String> pomclassSet = new HashSet<>();
    Path latestFilePath = null;
	
//	public static void main (String[] args) {
//		PlaywrightTypeScript sj = new PlaywrightTypeScript();
//		sj.genCode(null, -1, null, null, null);
//	}
	
	
    CodeGenHelper codeGenHelper;
    
    public CodeGenHelper getCodeGenHelper() {
		return codeGenHelper;
	}


	public void setCodeGenHelper(CodeGenHelper codeGenHelper) {
		this.codeGenHelper = codeGenHelper;
	}


	public void setMsc(MySQlConn m) {
		msc = m;
		msc2 = new MySqlConn2(msc);
	}
		
    
	public boolean genCode(JSONObject step, int companyId, String randomkey, 
			Utilities utilities, String requestedBy) {
		try {
		
		boolean isLicenseValid = checkLicense(companyId);
		String justFileName = codeGenHelper.getJustFileName((String)step.get("title"), randomkey);
		String filename = justFileName;
		if(justFileName != null && !justFileName.equals("")) {
			
			if(companyId > 0) {
				Utilities.createCodePath(companyId);
			}
			
			msc = new MySQlConn(null);
			msc2 = new MySqlConn2(msc);
			boolean validLicense = checkLicense(companyId);
			validLicense = true;
			
			if(!validLicense) {
				System.err.println("License is no longer valid");
				return false;
			}
			
			justFileName = justFileName + codeGenHelper.getFileExtension();
			String fullFilePath = Utilities.getCodeFilePath(companyId, justFileName);
			
			String templateFileName = codeGenHelper.getTemplateFileName();
	        copyResourceFile(templateFileName, fullFilePath);
	        
	        File latestFile = new File(fullFilePath);
	        Path latestFilePath = latestFile.toPath();
	        
	        try {
	        	replaceContent(latestFilePath, codeGenHelper.getClassSig(), filename);
	        	JSONObject userCache = utilities.getUserFromCache(randomkey);
				msc2.decrementLicense((int)userCache.get("companyid"), requestedBy);
	        }catch(Exception e) {
	        	e.printStackTrace();
	        }
	        return true;
		} else {
			String codeDir = Utilities.getCodePath(companyId);
			File latestFile = Utilities.getlatestFileFromDir(codeDir, randomkey, codeGenHelper.getFileExtension());
			latestFilePath = latestFile.toPath();
			try {					
				getLinesAsList(latestFilePath);			
				getStepNum();
				String replacementText = getReplacementText(step);
			
				if(!ignore) {
					String latestStep = codeGenHelper.getLatestStepFormat();
					String LastStep = codeGenHelper.getLastStepFormat();
					replacementText =  latestStep + replacementText + LastStep;
					replaceContent(latestFilePath, latestStep, stepNumberStr + " " + stepNum);
					replaceContent(latestFilePath, LastStep, replacementText);
					if(stepNum % 50 == 0) {
						JSONObject userCache = utilities.getUserFromCache(randomkey);
						msc2.decrementLicense((int)userCache.get("companyid"), requestedBy);
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		}catch (Exception ex) {
			System.out.println(ex.getMessage());
			ex.printStackTrace();
		} finally {
		}
		
		return true;
	}
	
	public boolean checkLicense(int companyId) {
		JSONObject license = PasswordUtils.getLicenseDetails(companyId, msc, msc2);
		
		if(license == null || license.size() == 0) {
			return false;
		}
		
		int tcCount = (int)license.get("tcCount");
		if(tcCount > 0) {
			return true;
		}
		return false;
	}
	
	public void getStepNum() {
		
		for(int i=addHereLineIndex + 1; i< lines.size();i++) {
			String line = lines.get(lines.size() - i);
			
			if(line.trim().startsWith(stepNumberStr.trim())) {
				String snstr = line.trim().substring(stepNumberStr.trim().length(),line.trim().length());
				stepNum = Integer.valueOf(snstr.trim());
				stepNum++;
				break;
			} else if(line.trim().startsWith("try {")) {
				stepNum = 1;
				break;
			}
		}
	}
	
	public void copyResourceFile(String resourcefile, String destinationPath) {
		System.out.println("Looking for Template in folder  " + System.getProperty("user.dir"));
		// Get the URL of the resource
        URL resourceURL = CodeGen.class.getResource("/" + resourcefile);
        System.out.println("Resource File Path: " + resourceURL.getPath());
        
        try (InputStream inputStream = getClass().getResourceAsStream("/" + resourcefile);
             OutputStream outputStream = new FileOutputStream(destinationPath)) {

            if (inputStream == null) {
                throw new IllegalArgumentException("Resource not found: " + resourcefile);
            }

            byte[] buffer = new byte[1024];
            int length;
            while ((length = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, length);
            }

            System.out.println("File copied successfully to " + destinationPath);
        } catch (IOException e) {
            System.out.println("An error occurred while copying the file: " + e.getMessage());
        }
    }
	
	public void replaceContent(Path filePath, String originalText, String replacementText) throws IOException{
		
		
		String content = Files.lines(filePath, StandardCharsets.UTF_8)
                .collect(Collectors.joining("\n"));

		// Replace text
		content = content.replace(originalText.trim(), replacementText);

		// Write the new content back to the file
		Files.write(filePath, content.getBytes(StandardCharsets.UTF_8));
	}
	
	public void getLinesAsList(Path filePath) {
		try {
			lines = Files.lines(filePath, StandardCharsets.UTF_8)
	                .collect(Collectors.toList());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public String getUniqueLabel(JSONObject step) {
		JSONObject uniquetoele = (JSONObject)step.get("uninametoele");
		String name = null;
		if(uniquetoele != null) {
			name = (String)uniquetoele.get("text");
			
			if(name != null && !name.equals("")) {

		        // Replace all characters that are not letters or numbers with an empty string
		        name = name.replaceAll("(?i)please", "");
		        name = name.replaceAll("(?i)enter", "");
		        
		        name = name.replaceAll("[^a-zA-Z0-9]", "");
		        
		        name = name.replaceFirst("^\\d+", "");
		        
		        
		        if (name == null || name.equals("")) {
		        	name = "field";
		        } else if(name.length() > 10) {
		        	name = name.substring(0,10);
		        }
			}
		}
		return name;
	}
	
	public String escapeDoubleQuotes(String input) {
        StringBuilder stringBuilder = new StringBuilder();

        for (char c : input.toCharArray()) {
            if (c == '\"') {
                stringBuilder.append("\\\"");
            } else {
                stringBuilder.append(c);
            }
        }

        return stringBuilder.toString();
    }
	
	private String getReplacementText(JSONObject step) {
		
		String replacementText = "";
		String stepType = (String)step.get("action");
		String testData = (String)step.get("testdata"); 
		boolean closingBraceNeeded = false;
		
		if(stepType.equalsIgnoreCase("apidata")) {
			ignore = true;
			return null;
		}
		
		if(stepType.equalsIgnoreCase("url")) {
			String url = (String)step.get("url");
			replacementText = replacementText + "\n";
			replacementText = replacementText + codeGenHelper.getNavigateCode(url);
		}  else {
			String xpath = (String)step.get("click");
			boolean isShadow = false;
			if(step.get("shadow") != null) {
				  isShadow = (boolean)step.get("shadow"); 
			  }
			xpath = escapeDoubleQuotes(xpath);
			String fieldname = "field" + stepNum;
			
			String uniqueLabel = getUniqueLabel(step);
			
			if(uniqueLabel != null && !uniqueLabel.equals("")) {
				fieldname = uniqueLabel + stepNum;
			}
			
			replacementText = replacementText + "\n";
			replacementText = replacementText + replaceElementSpecificCode(xpath, fieldname, step, isShadow);
			replacementText = replacementText + "\n";
			
			 String iframexpath = (String) step.get("iframexpath");
			 prevRowiframexpath = iframexpath;
		}
        return replacementText;
	}
	
	private String replaceElementSpecificCode(String xpath, String fieldname, JSONObject step, boolean isShadow) {
	    
		String iframexpath = (String) step.get("iframexpath");
		int tabid = (int)step.get("tabid");
		int tcid = (int) step.get("Test_Case_Id");
		int elementid = (int) step.get("elementId");
		int tsid = (int) step.get("idtest_step");
		String action = (String)step.get("action");
		String keyword = (String)step.get("Keyword");
		String type = (String)step.get("type");
		String nearestname = (String)step.get("nearestname");
		String src = (String)step.get("imgpath");
		
        String uniquename = "null";
        String elementtype = getElementTypeFromXpath(xpath);
        String pagename = "";
        String pageid = "";
        String objectName = "";
        
        int pid = -1;
		String uniquenameDetail = msc2.getPomUniqueName(elementid);
        String[] parts = uniquenameDetail.split("~");
        if (parts.length == 4) {
        	uniquename = parts[0];
            elementtype = parts[1];
            pagename = parts[2];
            pageid = parts[3];
            if(uniquename == null || uniquename.equals("null") || uniquename.equals("")) {
            	uniquename = "nonamele";
            }
        } else {
            System.out.println("The input string does not contain any delimiter on "+pid+" So element added " + uniquename + " with id " + elementid+ " in step "+ tsid);
        }
        pagename = pagename+pageid;
        if(!pagename.equals("") && pagename.length() > 3) {
        	objectName = pagename.substring(0, 5) + pageid;
        	objectName = objectName.toLowerCase();
        }
        
        pid = (pageid != null && !pageid.equals("")) ? Integer.parseInt(pageid) : -1;
        if (tabs.get(tabid) == null) {
        	tabs.put(tabid, tabs.size());
        }
        
		String replaceStr = "";
		if(isShadow) {
			replaceStr = codeGenHelper.generateShadowCode(step, fieldname, xpath);
		} else {
			if(prevTabId!=-1 && prevTabId != tabid) {
				replaceStr += codeGenHelper.generateTabChnageSelScript(tabid, tabs, fieldname);
			}
			if(!prevRowiframexpath.equals(iframexpath)) {
				replaceStr += codeGenHelper.generateIframeSelScript(step, iframexpath, fieldname, xpath);
			}
	        // Add a line whenever there is a page id change
	        if (prevPageId != pid && !pagename.equals("") &&!pomclassSet.contains(pagename)) {
	        	
	            codeGenHelper.addActionClassPageConstantText(pagename, objectName, latestFilePath);
	            pomclassSet.add(pagename);
	        }
	        String testData = (String)step.get("testdata"); 
			if (testData != null && testData.contains("\n")) {
			    testData = "";
			}
			String altpath = (String)step.get("altpath");
			if (altpath != null && altpath.contains("\n")) {
				altpath = "";
			}
			if(elementid != 0 && !uniquename.equals("null") && !uniquename.equals("nonamele")) {
				replaceStr += codeGenHelper.getActionClassPomCodeForElement(elementtype, action, fieldname, objectName, uniquename, testData);
			} else {
				replaceStr += codeGenHelper.getActionClassCodeForElement(elementtype, action, fieldname, step, testData);
			}
			
		}
		prevTabId = tabid;
		prevPageId = pid;
		return replaceStr;
	}
	
	
	
	private String getElementTypeFromXpath(String xpath) {
        String[] xPaths = xpath.split("\\|");
        String lastXPath = xPaths[xPaths.length - 1].trim();
        String[] elements = lastXPath.split("/");
        String lastElement = elements[elements.length - 1];
        lastElement = lastElement.split("\\[")[0];
        return lastElement;
	}


}
