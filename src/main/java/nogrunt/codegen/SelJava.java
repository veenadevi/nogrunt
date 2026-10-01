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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.HashSet;
import java.util.Set;

public class SelJava {
	
	int stepNum = 1;
	List<String> lines = null;
	boolean replace = false;
	boolean ignore = false;
	MySQlConn msc = null;
	MySqlConn2 msc2 = null;
	final String latestStep = "\n			//Latest Step";
	final String LastStep = "\n			//Add from Here";
	final String pageConstants = "\n\t\t//pageconstants";
	final String stepNumberStr = "//Step Number -";
	int addHereLineIndex = 9;
	String actionsStr = "Actions actions = new Actions(driver);";
	final String classSig = "JavaTemplate";
	final String packageDef = "//package";
	final String methodDef = "public void runLogin() {";
	final String threadWait = "\n			Thread.sleep(waitime);";	
	final String takeSS = "\n			takeSS(ssfilename);";
	String prevRowiframexpath = "";
	HashMap tabs = new HashMap();
	int prevTabId = -1;
	int prevPageId=-1;
    Set<String> pomclassSet = new HashSet<>();
    Path latestFilePath = null;
    final List<String> RANKED_LOCATORS = List.of(
	        "id", "data-value", "classname", "id-xpath", "dataset-id", "xpath", "placeholder", "text"
	    );

	
	public static void main (String[] args) {
		SelJava sj = new SelJava();
		sj.genCode(null, -1, null, null,null);
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
		String filename = justFileName + "_" + randomkey;
		if(justFileName != null && !justFileName.equals("")) {
			
			if(companyId > 0) {
				Utilities.createCodePath(companyId);
			}
			
			msc = new MySQlConn(null);
			msc2 = new MySqlConn2(msc);
			boolean validLicense = checkLicense(companyId);
			
			if(!validLicense) {
				System.err.println("License is no longer valid");
				return false;
			}
			
			justFileName = justFileName + "_" + randomkey + ".java";
			justFileName = justFileName.replace("/", "");
			String fullFilePath = Utilities.getCodeFilePath(companyId, justFileName);
			
			String templateFileName = "JavaTemplate.java";
//	        Path templateFilePath = Paths.get(templateFileName); // Adjust this path as needed
	        copyResourceFile(templateFileName, fullFilePath);
	        
	        File latestFile = new File(fullFilePath);
	        Path latestFilePath = latestFile.toPath();
	        
	        String packageStr = "package " + (String)step.get("path") + ";";
	        String methodStr =  methodDef.replace("runLogin", (String)step.get("method"));
	        
	        try {
	        	replaceContent(latestFilePath, packageDef, packageStr);
	        	replaceContent(latestFilePath, methodDef, methodStr);
	        	replaceContent(latestFilePath, classSig, filename);
	        	JSONObject userCache = utilities.getUserFromCache(randomkey);
				msc2.decrementLicense((int)userCache.get("companyid"),requestedBy);
	        }catch(Exception e) {
	        	e.printStackTrace();
	        }
	        return true;
		} else {
			String codeDir = Utilities.getCodePath(companyId);
			File latestFile = Utilities.getlatestFileFromDir(codeDir, randomkey, ".java");
			latestFilePath = latestFile.toPath();
			try {
			
				
				getLinesAsList(latestFilePath);
			
				getStepNum();
				String replacementText = getReplacementText(step);
			
				if(!ignore) {
//					if(replace) {
//						replaceContent(latestFilePath, lines.get(lines.size()- (addHereLineIndex + 2)), replacementText);
//					} else {
						replacementText =  latestStep + threadWait + replacementText + LastStep;
						replaceContent(latestFilePath, latestStep, stepNumberStr + " " + stepNum);
						replaceContent(latestFilePath, LastStep, replacementText);
						if(stepNum % 50 == 0) {
							JSONObject userCache = utilities.getUserFromCache(randomkey);
							msc2.decrementLicense((int)userCache.get("companyid"),requestedBy);
//						}
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		}catch (Exception ex) {
			ex.printStackTrace();
		} finally {
			//msc is persistent msc2 is only needed for codegen
//			msc2 = null;
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
	
//	public void decrementLicense(int companyId) {
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
//			
//			msc2.updateLicenseCount(licReq, companyId);
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
			} else if(line.trim().startsWith(actionsStr)) {
				stepNum = 1;
				break;
			}
		}
	}
	
	public void copyResourceFile(String resourcefile, String destinationPath) {
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
		
		if(stepType.equalsIgnoreCase("apidata")) {
			ignore = true;
			return null;
		}
		
		if(stepType.equalsIgnoreCase("url")) {
			replacementText = replacementText + "\n";
			String url = (String)step.get("url");
			replacementText = replacementText + "\t\t\tdriver.get(\""+ url + "\");";
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
				// Find the username field and enter the username
				replacementText = replacementText + "\n";
//				replacementText = replacementText + "			WebElement " + fieldname +" = driver.findElement(By.xpath(\"" + xpath + "\"));";
				replacementText = replacementText + findElement(xpath, fieldname, step, isShadow, false);
				replacementText = replacementText + "\n";
//				replacementText = replacementText + "			" +fieldname + ".click();";
			} else if(stepType.equalsIgnoreCase("submit")) {
				// Find the username field and enter the username
				replacementText = replacementText + "\n";
//				replacementText = replacementText + "			WebElement " + fieldname +" = driver.findElement(By.xpath(\"" + xpath + "\"));";
				replacementText = replacementText + findElement(xpath, fieldname, step, isShadow, false);
				replacementText = replacementText + "\n";
//				replacementText = replacementText + "			" + fieldname + ".submit();";
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
				// Find the username field and enter the username
				replacementText = replacementText + "\n";
//				replacementText = replacementText + "			WebElement " + fieldname +" = driver.findElement(By.xpath(\"" + xpath + "\"));";
				replacementText = replacementText + findElement(xpath, fieldname, step, isShadow, false);
				replacementText += "\n\t\t\t"+fieldname+".click();";
				replacementText = replacementText + "\n";
				replacementText = replacementText + "			Select " + select + " = new Select(" + fieldname + ");";
				replacementText = replacementText + "\n";
				replacementText = replacementText + "			" + select + ".selectByVisibleText(\"" + text + "\");";
				replacementText = replacementText + "\n";
				replacementText = replacementText + "			//" + select + ".selectByValue(" + value + ");";
				replacementText = replacementText + "\n";
				replacementText = replacementText + "			//int index = Integer.valueOf(" + id + ");;";
				replacementText = replacementText + "\n";
				replacementText = replacementText + "			//" + select + ".selectByIndex(index);";
			} else if(stepType.equalsIgnoreCase("submit")) {
				replacementText = replacementText + "\n";
//				replacementText = replacementText + "			WebElement " + fieldname +" = driver.findElement(By.xpath(\"" + xpath + "\"));";
				replacementText = replacementText + findElement(xpath, fieldname, step, isShadow, false);
				replacementText += "\n\t\t\t"+fieldname+".click();";
				replacementText = replacementText + "\n";
//				replacementText = replacementText + "			" + fieldname + ".submit();";
			} else if(stepType.equalsIgnoreCase("Enter") || stepType.equalsIgnoreCase("change")) {
				String inputType = (String)step.get("type");
				replacementText = replacementText + "\n";
				String elementLocator = findElement(xpath, fieldname, step, isShadow, true);
				String prevElement = findElement(xpath, prevFieldname, step, isShadow, true);
				prevElement = prevElement.trim();
//				prevElement = prevElement.substring(0, prevElement.indexOf("\",\""));
				replacementText = replacementText + elementLocator;
				if(inputType != null && (inputType.equals("date") ||
						inputType.equals("datetime-local"))) {
					replacementText = replacementText + "\n";
					replacementText = replacementText + "			" + fieldname + ".clear();";
					replacementText = replacementText + "\n";
					replacementText = replacementText + "			JavascriptExecutor js = (JavascriptExecutor)driver;";
					replacementText = replacementText + "\n";
					replacementText = replacementText + "			js.executeScript(\"arguments[0].value='\" + \"" + testData + "\" + \"';\", " + fieldname + ");";				
				} else if(stepType.equalsIgnoreCase("upload")){
					replacementText = replacementText + "\n";
					replacementText = replacementText + "			" + fieldname + "sendKeys(please input location to file);";
				} else {					
					String addhere = lines.get(lines.size() - addHereLineIndex);					
//					if(addhere.trim().equals(LastStep.trim())) {
//						String send = lines.get(lines.size() - (addHereLineIndex + 2));
//						if(send.trim().contains(".sendKeys(")) {
//							String eleLoc = lines.get(lines.size() - (addHereLineIndex + 3));
//							eleLoc = eleLoc.trim();
//							eleLoc = eleLoc.substring(0, eleLoc.indexOf("\",\""));
//							if(eleLoc.equals(prevElement)) {
//								replace = true;
//							} else if(isShadow &&
//									prevElement.trim().endsWith(eleLoc.trim())) {
//								replace = true;
//							}
//						}						
//					}
					
//					if(replace) {
//						replacementText = "	" + prevFieldname + ".sendKeys(\"" + testData + "\");";
//						return replacementText;
//					} else {
						replacementText = replacementText + "\n";
//						replacementText = replacementText + fieldname + ".sendKeys(\"" + testData + "\");";//writing inside pom class only
//					}
				}
			} else if(stepType.equalsIgnoreCase("dblClick")) {
				replacementText = replacementText + "\n";
				replacementText = replacementText + findElement(xpath, fieldname, step, isShadow, false);
				replacementText = replacementText + "\n";
				replacementText = replacementText + "			actions.doubleClick(" + fieldname + ").perform();";
			} else if(stepType.equalsIgnoreCase("dnddrag")) {
				replacementText = replacementText + "\n";
				replacementText = replacementText + findElement(xpath, fieldname, step, isShadow, false);
				replacementText = replacementText + "\n";
				replacementText = replacementText + "			dragStart = " + fieldname + ";";
			} else if(stepType.equalsIgnoreCase("dnddrop")) {
				replacementText = replacementText + "\n";
				replacementText = replacementText + findElement(xpath, fieldname, step, isShadow, false);
				replacementText = replacementText + "\n";
				replacementText = replacementText + "			put your implementation of drag and drop here dragStart and " + fieldname;	
			} else if (stepType.startsWith("Arrow")) {
				replacementText = replacementText + "\n";
				replacementText = replacementText + findElement(xpath, fieldname, step, isShadow, false);
				if(stepType.equalsIgnoreCase("ArrowUp")) {
					replacementText = replacementText + "\n";
					replacementText = replacementText + "			" + fieldname + ".sendKeys(Keys.ARROW_UP);";
				} else if (stepType.equalsIgnoreCase("ArrowDown")) {
					replacementText = replacementText + "\n";
					replacementText = replacementText + "			" + fieldname + ".sendKeys(Keys.ARROW_DOWN);";
				}
			} else if(stepType.equalsIgnoreCase("rightclick")) {
				replacementText = replacementText + "\n";
				replacementText = replacementText + findElement(xpath, fieldname, step, isShadow, false);
				replacementText = replacementText + "\n";
				replacementText = replacementText + "			actions.contextClick(" + fieldname + ").build().perform();";
			} else {
				System.out.println("Action not supported " + stepType);
			}
			
//			 String takessreplace = takeSS.replace("ssfilename", "\"" + fieldname + ".jpg" + "\"");
//			 replacementText = replacementText + takessreplace ;
			 String iframexpath = (String) step.get("iframexpath");
			 prevRowiframexpath = iframexpath;

			 
		}
        return replacementText;
	}
	
	private String findElement(String xpath, String fieldname, JSONObject step, boolean isShadow, Boolean isTextBox) {
	    
		String iframexpath = (String) step.get("iframexpath");
		int tabid = (int)step.get("tabid");
		int tcid = (int) step.get("Test_Case_Id");
		int elementid = (int) step.get("elementId");
		int tsid = (int) step.get("idtest_step");
		String action = (String)step.get("action");
        String nearestname = "null";
        String elementtype = getElementTypeFromXpath(xpath);
        String pagename = "";
        String pageid = "";
        String objectName = "";
        int pid = -1;
		String uniquenameDetail = msc2.getPomUniqueName(elementid);
        String[] parts = uniquenameDetail.split("~");
        if (parts.length == 4) {
            nearestname = parts[0];
            // To check if the name starts with "removableString" so that we can remove it as we added while generating pomclass.
//            if (nearestname.startsWith("removableString")) {
//            	nearestname = nearestname.substring("removableString".length());
//            }
            elementtype = parts[1];
            pagename = parts[2];
            pageid = parts[3];
            if(nearestname == null ||nearestname.equals("null") || nearestname.equals("")) {
            	nearestname = "nonamele";
            }
        } else {
            System.out.println(Utilities.getNow() + " : The input string does not contain any delimiter on "+pid+" So element added " + nearestname + " with id " + elementid+ " in step "+ tsid);
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
			replaceStr = replaceStr + "\n			WebElement " + fieldname + "= getShadowElement(\"" + xpath +"\", lookingFor, lookingForIndex );";
		} else {
			if(prevTabId!=-1 && prevTabId != tabid) {
				replaceStr += generateTabChnageSelScript(tabid, tabs, fieldname);
			}
			if(!prevRowiframexpath.equals(iframexpath)) {
				replaceStr += generateIframeSelScript(step, iframexpath, fieldname, xpath);
			}
	        // Add a line whenever there is a page id change
			if (prevPageId != pid && !pagename.equals("") &&!pomclassSet.contains(pagename)) {
	            String pcText = pagename + " " +objectName + " = new " + pagename+"(driver);";
	            pcText = pcText + pageConstants;
	            
	            try {
	            	replaceContent(latestFilePath, pageConstants, pcText);
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
			
			if(elementid != 0 && !nearestname.equals("null") && !nearestname.equals("nonamele")) {
				if(elementtype.equalsIgnoreCase("LABEL")) {
					replaceStr +=  "\t\t\tWebElement " + fieldname +" = "+ objectName +".scrollTo" +nearestname+ "();";
				} else if(elementtype.equalsIgnoreCase("LINK") || elementtype.equalsIgnoreCase("BUTTON")){
					replaceStr +=  "\t\t\t"+objectName +".click" +nearestname+ "();";
				} else if(elementtype.equalsIgnoreCase("INPUT") || elementtype.equalsIgnoreCase("TEXTAREA")){
					if(action.equalsIgnoreCase("upload")) {
						replaceStr +=  "\t\t\tWebElement " + fieldname +" = "+ objectName +".Upload" +nearestname+ "();";
					}
					else if (action.equalsIgnoreCase("ENTER") || action.equalsIgnoreCase("CHANGE")) {
						replaceStr +=  "\t\t\t" + objectName +".Enter" +nearestname+ "(\""+testData+"\");"; 
					}
					else {
						replaceStr +=  "\t\t\t" + objectName +".click" +nearestname+ "();";
					}
				} else {
					replaceStr +=  "\t\t\t"+objectName +".click" +nearestname+ "();";
				}
			} else {
				
				replaceStr += "\t\t\tWebElement "+fieldname+" = ";
				replaceStr += findElementByPreferredLocator(step);
				if (action.equalsIgnoreCase("Enter") || action.equalsIgnoreCase("change")) {
					replaceStr += "\n\t\t\t"+fieldname+".sendKeys("+testData+");";
				} else if (action.equalsIgnoreCase("upload") || action.equalsIgnoreCase("dblClick") 
						|| action.equalsIgnoreCase("rightclick")) {
					//do nothing
				}
				else {
					replaceStr += "\n\t\t\t"+fieldname+".click();";
				}
			}
			
		}
		prevTabId = tabid;
		prevPageId = pid;
		return replaceStr;
	}
	
	String findElementByPreferredLocator(JSONObject step) {
		
		String xpath1 = null;
		String xpath2 = null;
		String xpath = (String) step.get("Object_Xpath");
		if(xpath != null && !xpath.equals("") && xpath.contains("|")) {
			xpath1 = xpath.substring(0, xpath.indexOf("|") - 1);
			xpath2 = xpath.substring(xpath.indexOf("|") + 2, xpath.length());
		}
		String action = (String)step.get("Action");
	    String testData = (String)step.get("testdata");
	    if (testData != null && testData.contains("\n")) {
		    testData = "";
		}
		
		String returnString;
		
		for (String locator : RANKED_LOCATORS) {
			
			if (locator.equalsIgnoreCase("id")) {
				
				if (xpath1 != null && !xpath1.startsWith("HTML")) {
					String idValue = null;
					String singleQuote = "@id='";
					String doubleQuote = "@id=\"";
					String endQuote = "']";
					int startIdx = xpath1.indexOf(singleQuote);
					if (startIdx == -1) {
						startIdx = xpath1.indexOf(doubleQuote);
						endQuote = "\"]";
					}
			        if (startIdx != -1) {
			            startIdx += 5; // Move past "@id='"
			            int endIndex = xpath1.indexOf(endQuote, startIdx);// Find the position of the closing quote
			            if (endIndex != -1) {
			            	// Ensure the XPath does not have additional paths after the `id`
			                String remainingPath = xpath1.substring(endIndex + endQuote.length()).trim();
			                if (remainingPath.isEmpty() || remainingPath.equals("/")) {
			                    // Valid XPath with only the id attribute
			                	idValue = xpath1.substring(startIdx, endIndex); //retrived idValue, we can return
				                returnString = "driver.findElement(By.id(\""+ idValue +"\"));";
				                return returnString;
			                }
			                else {
			                	xpath1 = escapeDoubleQuotes(xpath1);
								returnString = "driver.findElement(By.xpath(\""+xpath1+"\"));";
								return returnString;
			                }
			            } 
			        } 
		        }
				
			}
			else if (locator.equalsIgnoreCase("placeholder")) {
				
				String elePlaceholder = (String) step.get("elementplaceholder");
				if (elePlaceholder != null && !elePlaceholder.isBlank()) {
					returnString = "driver.findElement(By.xpath(\"//*[@placeholder='"+ elePlaceholder +"']\"));";
	                return returnString;
				}
				
			}
			else if (locator.equalsIgnoreCase("classsname")) {
				
				String className = (String) step.get("classname");
				Integer classIndex = (Integer) step.getOrDefault("classnameindex", 0);
			   
			}
			else if (locator.equalsIgnoreCase("text")) {
				
				if (testData != null && !testData.equals("")) {
					returnString = "driver.findElement(By.xpath(\"//*[text()='" + testData + "']\"));";
					return returnString;
				}
				
			}
			else if (locator.equalsIgnoreCase("xpath")) {
				if (xpath1 != null) {
					xpath1 = escapeDoubleQuotes(xpath1);
					returnString = "driver.findElement(By.xpath(\""+xpath1+"\"));";
					return returnString;
				}
			}
		}
		
		returnString = "null; //Unable to find the element with the preferences\n";
		return returnString;
	}
	
	
	private String generateIframeSelScript(JSONObject step, String iframexpath, String fieldname, String xpath) {
		
		String replaceStr = "			WebElement " + fieldname + "X = null;\n";
		int maxRetries = 5;
		if(iframexpath == null || iframexpath.equals("")) {
		    replaceStr += "\t\t\tdriver.switchTo().defaultContent();\n";
	    	}
		else {
			iframexpath = escapeDoubleQuotes(iframexpath);
			
			replaceStr += "\t\t\tint retries" + fieldname + " = 0;\n";
			replaceStr += "\t\t\tboolean switched" + fieldname + " = false;\n";
			replaceStr += "\t\t\twhile (!switched" + fieldname + ") {\n";
			replaceStr += "\t\t\t\ttry {\n";

			
			if (iframexpath.startsWith("iframeindex=")) {
				int iframeIndex = Integer.parseInt(iframexpath.substring(12));
				replaceStr += "\t\t\t\t\tdriver.switchTo().frame(" + iframeIndex + ");\n";
			} 
			else {
			replaceStr += "\t\t\t\t\t" + fieldname + "X = driver.findElement(By.xpath(\"" + iframexpath + "\"));\n";
			replaceStr += "\t\t\t\t\tdriver.switchTo().frame(" + fieldname + "X);\n";
			}
			replaceStr += "\t\t\t\t\tswitched" + fieldname + " = true;\n";
			
			replaceStr += "\t\t\t\t} catch (org.openqa.selenium.NoSuchElementException e) {\n";

			if (!iframexpath.equals("") && !prevRowiframexpath.equals("")) {
			replaceStr += "\t\t\t\t\tdriver.switchTo().defaultContent();\n";
			replaceStr += "\t\t\t\t\t" + fieldname + "X = driver.findElement(By.xpath(\"" + iframexpath + "\"));\n";
			replaceStr += "\t\t\t\t\tdriver.switchTo().frame(" + fieldname + "X);\n";
			replaceStr += "\t\t\t\t\tswitched" + fieldname + " = true;\n";
			} 
			replaceStr += "\t\t\t\t\tretries" + fieldname + "++;\n";
			replaceStr += "\t\t\t\t\tif (retries" + fieldname + " >= " + maxRetries + ") {\n";
			replaceStr += "\t\t\t\t\t\tthrow e;\n";
			replaceStr += "\t\t\t\t\t}\n";
			replaceStr += "\t\t\t\t\tThread.sleep(waitime);\n";
			replaceStr += "\t\t\t\t}\n";
			replaceStr += "\t\t\t}\n";
			
		}
		
	    String selScriptString = replaceStr;
	    return selScriptString;
	}
	
	private String generateTabChnageSelScript(int tabid, HashMap tabs, String fieldname) {
		int tabIndex = tabs.size();
		if(tabs.get(tabid) == null) {
			tabs.put(tabid, tabIndex);
		} else {
			tabIndex = (int)tabs.get(tabid);
		}
		String replaceStr = "\n";
		replaceStr += "\t\t\tString[] " + fieldname + "windowHandles = driver.getWindowHandles().toArray(new String[0]);\n";
		replaceStr += "\t\t\tString " + fieldname + "tab2Handle = " + fieldname + "windowHandles["+tabIndex+"];\n";
		replaceStr += "\t\t\tdriver.switchTo().window(" + fieldname + "tab2Handle);\n";
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
