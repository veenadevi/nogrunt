package nogrunt.codegen;

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

public class SeleniumCSharp {
	
	int stepNum = 1;
	List<String> lines = null;
	boolean replace = false;
	boolean ignore = false;
	MySQlConn msc = null;
	MySqlConn2 msc2 = null;
	final String latestStep = "\n\t\t\t\t//Latest Step";
	final String LastStep = "\n\t\t\t\t//Add from Here";
	final String importStep = "\n//import";
	final String pageDeclarations = "\n\t\t//page declarations";
	final String pageInitializations = "\n\t\t\t//page initializations";
	final String stepNumberStr = "//Step Number -";
	int addHereLineIndex = 10;
	String actionsStr = "Actions actions = new Actions(driver);";
	final String classSig = "SeleniumCSharpTemplate";
	final String packageDef = "//package";
	final String methodDef = "public void runLogin() {";	
	final String threadWait = "\n\t\t\t\tThread.Sleep(waitTime);";
	final String takeSS = "await page.screenshot({ path: 'ssfilename' });";
	String prevRowiframexpath = "";
	HashMap tabs = new HashMap();
	int prevTabId = -1;
	int prevPageId=-1;
    Set<String> pomclassSet = new HashSet<>();
    Path latestFilePath = null;
    // Constant for ranked locators
    final List<String> RANKED_LOCATORS = List.of(
        "id", "data-value", "id-xpath", "dataset-id", "xpath", "placeholder", "classname", "text"
    );

	
	public static void main (String[] args) {
		PlaywrightTypeScript sj = new PlaywrightTypeScript();
		sj.genCode(null, -1, null, null, null);
	}
	
	public void setMsc(MySQlConn m) {
		msc = m;
		msc2 = new MySqlConn2(msc);
	}
		
    
	public boolean genCode(JSONObject step, int companyId, String randomkey, 
			Utilities utilities, String requestedBy) {
		try {
		
		boolean isLicenseValid = checkLicense(companyId);
		String justFileName = (String)step.get("title"); // Replace with your file name
		if(justFileName != null) {
			justFileName = justFileName.trim().replaceAll(" ", "_");
			justFileName = justFileName.replaceAll("[\\\\/:*?\"<>|]", "_");  // Replace illegal characters
		}
		String filename = justFileName + "_" + randomkey;
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
			
			justFileName = justFileName + "_" + randomkey + ".cs";
			String fullFilePath = Utilities.getCodeFilePath(companyId, justFileName);
			
			String templateFileName = "SeleniumCSharpTemplate.cs";
	        copyResourceFile(templateFileName, fullFilePath);
	        
	        File latestFile = new File(fullFilePath);
	        Path latestFilePath = latestFile.toPath();
	        
	        String packageStr = "package " + (String)step.get("path") + ";";
	        String methodStr =  methodDef.replace("runLogin", (String)step.get("method"));
	        
	        try {
//	        	replaceContent(latestFilePath, "//title", justFileName);
//	        	replaceContent(latestFilePath, packageDef, packageStr);
//	        	replaceContent(latestFilePath, methodDef, methodStr);
	        	replaceContent(latestFilePath, classSig, filename);
	        	JSONObject userCache = utilities.getUserFromCache(randomkey);
				msc2.decrementLicense((int)userCache.get("companyid"), requestedBy);
	        }catch(Exception e) {
	        	e.printStackTrace();
	        }
	        return true;
		} else {
			String codeDir = Utilities.getCodePath(companyId);
			File latestFile = Utilities.getlatestFileFromDir(codeDir, randomkey, ".cs");
			latestFilePath = latestFile.toPath();
			try {					
				getLinesAsList(latestFilePath);			
				getStepNum();
				String replacementText = getReplacementText(step);
			
				if(!ignore) {
//					replacementText =  latestStep + threadWait + replacementText + LastStep;
//					replaceContent(latestFilePath, latestStep, stepNumberStr + " " + stepNum);
//					replaceContent(latestFilePath, latestStep, "");
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
	
//	public void decrementLicense(int companyId, String requestedBy) {
//		JSONObject license = PasswordUtils.getLicenseDetails(companyId, msc, msc2);
//		
//		if(license == null || license.size() == 0) {
//			System.err.println("License count could not be decremented");
//			return ;
//		}
//		
//		int tcCount = (int)license.get("tcCount");
//		if(tcCount > 0) {
//			tcCount = tcCount - 1;
//			String licReq = (String)license.get("toDate") + AppProperties.licenselimiter +  String.valueOf(tcCount) + AppProperties.licenselimiter + (String)license.get("codeDownloadPrmsn") 
//					+ AppProperties.licenselimiter + (String)license.get("performanceTestingPrmsn")  + AppProperties.licenselimiter + (String)license.get("coveragePrmsn") 
//					+ AppProperties.licenselimiter + (String)license.get("apiDataPrmsn")  + AppProperties.licenselimiter + String.valueOf(license.get("parallelThreadCnt")) 
//					+ AppProperties.licenselimiter + (String)license.get("vRecPrmsn")  + AppProperties.licenselimiter + (String)license.get("NLPCreationPrmsn") 
//					+ AppProperties.licenselimiter + (String)license.get("chatBotPrmsn");
//			
//			try {
//				licReq = PasswordUtils.encryptLicenseKey(licReq);
//			} catch (Exception e) {
//				System.err.println("License count could not be updated to db");
//			}	
//			try {
////				msc2.updateLicenseCount(licReq, companyId);
//				msc2.insertLicenseCount( companyId,  requestedBy,  licReq);
//			} catch (Exception e) {
//				System.err.println(Utilities.getNow());
//				e.printStackTrace();
//			}
//		} else {
//			System.err.println("License count could not be decremented as it is already 0");
//		}
//	}
	
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
        URL resourceURL = SeleniumCSharp.class.getResource("/" + resourcefile);
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
			replacementText = replacementText + "\t\t\t\tdriver.Navigate().GoToUrl(\"" + url + "\");";  
		}  else {
			String xpath = (String)step.get("click");
			boolean isShadow = false;
			if(step.get("shadow") != null) {
				  isShadow = (boolean)step.get("shadow"); 
			  }
			xpath = escapeDoubleQuotes(xpath);
			String prevFieldname = "field" + (stepNum - 1);
			String fieldname = "field" + stepNum;
			
			String uniqueLabel = getUniqueLabel(step);
			
			if(uniqueLabel != null && !uniqueLabel.equals("")) {
				prevFieldname = uniqueLabel + (stepNum - 1);
				fieldname = uniqueLabel + stepNum;
			}
			
			if(stepType.equalsIgnoreCase("click") || stepType.equalsIgnoreCase("radio") || 
					stepType.equalsIgnoreCase("checkbox") || stepType.equalsIgnoreCase("Button")) {
				replacementText = replacementText + "\n";
				replacementText = replacementText + findElement(xpath, fieldname, step, isShadow);
				replacementText = replacementText + "\n";
			} else if(stepType.equalsIgnoreCase("submit")) {
				replacementText = replacementText + "\n";
				replacementText = replacementText + findElement(xpath, fieldname, step, isShadow);
				replacementText = replacementText + "\n";
			} else if(stepType.equalsIgnoreCase("select")) {
				//do nothing
			} else if(stepType.equalsIgnoreCase("ByText")) {
				String select = "select" + stepNum;
				String id = "";
				String value = "";
				String text = testData;
				
				if(testData.contains("~")) {
					id = testData.substring(0,testData.indexOf(AppProperties.delimiter));
					String wipData = testData.substring(AppProperties.delimiter.length(),testData.length());
					wipData = wipData.substring(wipData.indexOf(AppProperties.delimiter) + AppProperties.delimiter.length(),wipData.length());
					value = wipData.substring(0,wipData.indexOf(AppProperties.delimiter));
					wipData = wipData.substring(wipData.indexOf(AppProperties.delimiter) + AppProperties.delimiter.length(),wipData.length());
					text = wipData.substring(0,wipData.length());
				}
				replacementText = replacementText + "\n";
				replacementText = replacementText + findElement(xpath, fieldname, step, isShadow);
				replacementText = replacementText + "\n";
				replacementText = replacementText + "\t\t\t"+fieldname+".selectOption(new SelectOption().setLabel("+text+"));";
			} else if(stepType.equalsIgnoreCase("Enter") || stepType.equalsIgnoreCase("change")) {
				String inputType = (String)step.get("type");
				String elementLocator = findElement(xpath, fieldname, step, isShadow);
				String prevElement = findElement(xpath, prevFieldname, step, isShadow);
				prevElement = prevElement.trim();
				replacementText = replacementText + "\n";
				replacementText = replacementText + elementLocator;
				if(inputType != null && (inputType.equals("date") ||
						inputType.equals("datetime-local"))) {
					replacementText = replacementText + "\n";
					testData = testData.replace("'", "\\'");
					replacementText = replacementText + "\t\t\t\t" + fieldname + ".fill(\"" + testData + "\");";
				} else if(stepType.equalsIgnoreCase("upload")){
					replacementText = replacementText + "\n";
					replacementText = replacementText + "\t\t\t\t" + fieldname + ".fill(please input location to file);";
				} else {					
					String addhere = lines.get(lines.size() - addHereLineIndex);
				}
			} else if (stepType.equalsIgnoreCase("upload")) {
				replacementText = replacementText + "\n";
				replacementText = replacementText + findElement(xpath, fieldname, step, isShadow);
				replacementText = replacementText + "\n";
				replacementText = replacementText + "\t\t\t"+fieldname+".setInputFiles(Paths.get(please give location to the upload file here));";
			} else if(stepType.equalsIgnoreCase("dblClick")) {
				replacementText = replacementText + "\n";
				replacementText = replacementText + findElement(xpath, fieldname, step, isShadow);
				replacementText = replacementText + "\n";
				replacementText = replacementText + "\t\t\t"+fieldname+".dblclick();";
			} else if(stepType.equalsIgnoreCase("dnddrag")) {
				replacementText = replacementText + "\n";
				replacementText = replacementText + findElement(xpath, fieldname, step, isShadow);
				replacementText = replacementText + "\n";
				replacementText = replacementText + "			dragStart = " + fieldname + ";";
			} else if(stepType.equalsIgnoreCase("dnddrop")) {
				replacementText = replacementText + "\n";
				replacementText = replacementText + findElement(xpath, fieldname, step, isShadow);
				replacementText = replacementText + "\n";
				replacementText = replacementText + "			put your implementation of drag and drop here dragStart and " + fieldname;	
			} else if (stepType.startsWith("Arrow")) {
				replacementText = replacementText + "\n";
				replacementText = replacementText + findElement(xpath, fieldname, step, isShadow);
				if(stepType.equalsIgnoreCase("ArrowUp")) {
					replacementText = replacementText + "\n";
					replacementText = replacementText + "\t\t\t"+fieldname+".press(\"ArrowUp\");";
				} else if (stepType.equalsIgnoreCase("ArrowDown")) {
					replacementText = replacementText + "\n";
					replacementText = replacementText + "\t\t\t"+fieldname+".press(\"ArrowDown\");";
				}
			} else if(stepType.equalsIgnoreCase("rightclick")) {
				replacementText = replacementText + "\n";
				replacementText = replacementText + findElement(xpath, fieldname, step, isShadow);
				replacementText = replacementText + "\n";
				replacementText = replacementText + "\t\t\t"+fieldname+".click(new Locator.ClickOptions().setButton(MouseButton.RIGHT));";
			} 
//			else if(stepType.equalsIgnoreCase("Upload")) {
//				replacementText = replacementText + findElement(xpath, fieldname, step, isShadow);
//
//			} 
			else {
				System.out.println("Action not supported " + stepType);
			}
			 String iframexpath = (String) step.get("iframexpath");
			 prevRowiframexpath = iframexpath;

			 
		}
        return replacementText;
	}
	
	private String getID(String xpath) {
		if(!xpath.contains("|")) {
			return null;
		}
		
		String idpart = xpath.substring(0,xpath.indexOf("|")-1);
		if(idpart.contains("@id=")) {
			idpart = idpart.substring(idpart.indexOf("@id") + 6, idpart.indexOf("\\\"]"));
			return idpart;
		} else {
			return null;
		}
	}
	
	private String findElement(String xpath, String fieldname, JSONObject step, boolean isShadow) {
	    
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
		String elementplaceholder = (String)step.get("elementplaceholder");
		int elementplaceholderindex = (int)step.get("elementplaceholderindex");
		String id = getID(xpath);
		
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
            // To check if the name starts with "removableString" so that we can remove it as we added while generating pomclass.
//            if (uniquename.startsWith("removableString")) {
//            	uniquename = uniquename.substring("removableString".length());
//            }
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
        }
        
        pid = (pageid != null && !pageid.equals("")) ? Integer.parseInt(pageid) : -1;
        if (tabs.get(tabid) == null) {
        	tabs.put(tabid, tabs.size());
        }
        
		String replaceStr = "";
		if(isShadow) {
			JSONObject shadowElements = (JSONObject)step.get("shadowelements");
			replaceStr = "\n			lookingFor = \"" + (String)shadowElements.get("type")+ "\";";
			replaceStr = replaceStr + "\n			lookingForIndex = " + (long)shadowElements.get("index") + ";";
			replaceStr = replaceStr + "\n			const " + fieldname + "= getShadowElement(\"" + xpath +"\", lookingFor, lookingForIndex );";
		} else {
			if(prevTabId!=-1 && prevTabId != tabid) {
				replaceStr += generateTabChnageSelScript(tabid, tabs, fieldname);
			}
			if(!prevRowiframexpath.equals(iframexpath)) {
				replaceStr += generateIframeSelScript(step, iframexpath, fieldname, xpath);
			}
	        // Add a line whenever there is a page id change
	        if (prevPageId != pid && !pagename.equals("") &&!pomclassSet.contains(pagename)) {
	        	String pdText = pagename + " " +objectName + ";";
	        	pdText = pdText + pageDeclarations;
	            String piText = objectName + " = new " + pagename+"(driver);";
	            piText = piText + pageInitializations;
	            
	            String importText = "import {" +pagename + " } from '../pages/"+  pagename +"';";
	            importText = importText + importStep;
	            try {
	            	replaceContent(latestFilePath, pageDeclarations, pdText);
	            	replaceContent(latestFilePath, pageInitializations, piText);
//	            	replaceContent(latestFilePath, importStep, importText);
	            }catch (Exception e) {
	            	e.printStackTrace();
	            }
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
				if(elementtype.equalsIgnoreCase("LABEL")) {
					replaceStr +=  "\t\t\t\tIWebElement " + fieldname +" = "+ objectName +".scrollTo" +uniquename+ "();";
				} else if(elementtype.equalsIgnoreCase("LINK") || elementtype.equalsIgnoreCase("BUTTON")){
					replaceStr +=  "\t\t\t\t"+objectName +".click" +uniquename+ "();";
				} else if(elementtype.equalsIgnoreCase("INPUT") || elementtype.equalsIgnoreCase("TEXTAREA")){
					if(action.equalsIgnoreCase("upload")) {
						replaceStr +=  "\t\t\t\tIWebElement " + fieldname +" = "+ objectName +".Upload" +uniquename+ "();";
					}
					else if (action.equalsIgnoreCase("ENTER") || action.equalsIgnoreCase("CHANGE")) {
						replaceStr +=  "\t\t\t\t" + objectName +".Enter" +uniquename+ "(\""+testData+"\");"; 
					}
					else {
						replaceStr +=  "\t\t\t\t" + objectName +".click" +uniquename+ "();";
					}
				} else {
					replaceStr +=  "\t\t\t\t"+objectName +".click" +uniquename+ "();";
				}
			} else {
				
				replaceStr += "\t\t\t\tIWebElement "+fieldname+" = ";
				replaceStr += findElementByPreferredLocatorInPWJava(step);
				if (action.equalsIgnoreCase("Enter") || action.equalsIgnoreCase("change")) {
					replaceStr += "\n\t\t\t\t"+fieldname+".SendKeys("+testData+");";
				} else if (action.equalsIgnoreCase("upload") || action.equalsIgnoreCase("dblClick") 
						|| action.equalsIgnoreCase("rightclick")) {
					//do nothing
				}
				else {
					replaceStr += "\n\t\t\t\t"+fieldname+".Click();";
				}
			}
			
		}
		prevTabId = tabid;
		prevPageId = pid;
		return replaceStr;
	}
	
	private String generateIframeSelScript(JSONObject step, String iframexpath, String fieldname, String xpath) {
		
		System.out.println("Not supported: this is an iframe ");
		String replaceStr = "			const " + fieldname + "X = null;\n";
	    if(iframexpath != null && iframexpath.equals("")) {
		    replaceStr += "\t\tdriver.switchTo().defaultContent();\n";
	    }
	    else {
		    iframexpath = escapeDoubleQuotes(iframexpath); 
		    replaceStr += "\t\t\ttry {\n";
		    replaceStr += "\t\t\t\t"+ fieldname +"X = driver.findElement(By.xpath(\""+iframexpath+"\"));\n";
		    replaceStr += "\t\t\t\tdriver.switchTo().frame("+ fieldname +"X);\n";
		    replaceStr += "\t\t\t} catch (org.openqa.selenium.NoSuchElementException e) {\n";
		    replaceStr += "\t\t\t\t\tthrow e;\n";
		    replaceStr += "\t\t\t}\n";
	    }
	    String selScriptString = replaceStr;
	    return selScriptString;
	}
	
	private String generateTabChnageSelScript(int tabid, HashMap tabs,  String fieldname) {
		int tabIndex = tabs.size();
		if(tabs.get(tabid) == null) {
			tabs.put(tabid, tabIndex);
		} else {
			tabIndex = (int)tabs.get(tabid);
		}
		String replaceStr = "\n";
		replaceStr += "\t\t\t\tList<Page> "+fieldname+"pages = context.pages();\n";
		replaceStr += "\t\t\t\tpage = pages.get("+tabIndex+");\n";
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
	
	
	String findElementByPreferredLocatorInPWJava(JSONObject step) {

	    String relPath = null;
	    String absPath = null;
	    String xpath = (String) step.get("Object_Xpath");
	    if (xpath != null && !xpath.equals("") && xpath.contains("|")) {
	        relPath = xpath.substring(0, xpath.indexOf("|") - 1);
	        absPath = xpath.substring(xpath.indexOf("|") + 2, xpath.length());
	    }
	    String action = (String)step.get("Action");
	    String testData = (String)step.get("testdata");
	    if (testData != null && testData.contains("\n")) {
		    testData = "";
		}

	    String returnString;

	    for (String locator : RANKED_LOCATORS) {

	        if (locator.equalsIgnoreCase("id")) {

	            if (relPath != null && relPath.contains("@id=")) {
	                String idValue = null;
	                String singleQuote = "@id='";
	                String doubleQuote = "@id=\"";
	                String endQuote = "']";
	                int startIdx = relPath.indexOf(singleQuote);
	                if (startIdx == -1) {
	                    startIdx = relPath.indexOf(doubleQuote);
	                    endQuote = "\"]";
	                }
	                if (startIdx != -1) {
	                    startIdx += 5; // Move past "@id='"
	                    int endIndex = relPath.indexOf(endQuote, startIdx); // Find the position of the closing quote
	                    if (endIndex != -1) {
	                        // Ensure the XPath does not have additional paths after the `id`
	                        String remainingPath = relPath.substring(endIndex + endQuote.length()).trim();
	                        if (remainingPath.isEmpty() || remainingPath.equals("/")) {
	                            idValue = relPath.substring(startIdx, endIndex);
	                            if (idValue.contains(" ") || idValue.matches("^\\d.*")) {
	                            	continue;
	                            }
	                            else {  
	                            	returnString = "driver.FindElement(By.Id(\"" + idValue + "\"));"; //only id based locator
	                            }
	                            return returnString;
	                        }
	                    }
	                }
	            }

	        } else if (locator.equalsIgnoreCase("id-xpath")) {
	        	
	        	if(relPath.contains("@id=")) {
	        		relPath = relPath.replace("\"", "\\\"");
	            	returnString = "driver.FindElement(By.XPath(\"" + relPath + "\"));"; //by id relative xpath
	                return returnString;
	        	}
	        	
	        } else if (locator.equalsIgnoreCase("placeholder")) {

	            String elePlaceholder = (String) step.get("elementplaceholder");
	            if (elePlaceholder != null && !elePlaceholder.isBlank()) {
	                returnString = "page.getByPlaceholder(\""+ elePlaceholder +"\");"; //by placeholder
	                return returnString;
	            }

	        } else if (locator.equalsIgnoreCase("classname")) {

	        	String className = (String) step.get("classname");
	            Integer classIndex = (Integer) step.getOrDefault("classnameindex", 0);

	            if (className != null && !className.equals("") && classIndex > 0) {
	                    returnString = "driver.FindElement(By.XPath(\"(//*[@class='" + className + "'])[1]\"));";
	                    return returnString; 
	            }

	        } else if (locator.equalsIgnoreCase("data-value")) {
	        	
	        	String dataValue = (String) step.get("dataValue");
	            Integer dataValueIndex = (Integer) step.getOrDefault("dataValueIndex", 0);
	        	if (dataValue != null && dataValueIndex >= 0) {
        			returnString = "page.locator(\"[data-value='"+dataValue+"']\")"; //by data value
        			returnString += ".nth("+dataValueIndex+");";
	                return returnString;
	        	}

	        } else if (locator.equalsIgnoreCase("text")) {
	        	
	        	if (!action.equalsIgnoreCase("Enter") && !action.equalsIgnoreCase("change")) {
	        		if (testData != null && !testData.equals("")) {
		                returnString = "page.getByText(\"" + testData + "\");"; //by text
		                return returnString;
		            }
	        	}

	        } else if (locator.equalsIgnoreCase("xpath")) {
	        	
	            if (relPath != null) {
	            	relPath = relPath.replace("\"", "\\\"");
	            	returnString = "driver.FindElement(By.XPath(\"" + relPath + "\"));"; //by xpath
	                return returnString;
	            } 
	            else {
	            	absPath = absPath.replace("\"", "\\\"");
	            	returnString = "driver.FindElement(By.XPath(\"" + absPath + "\"));"; //by xpath
	                return returnString;
	            }
	        }
	    }

	    returnString = "null; // Unable to find the element with the preferences\n";
	    return returnString;
	}


}
