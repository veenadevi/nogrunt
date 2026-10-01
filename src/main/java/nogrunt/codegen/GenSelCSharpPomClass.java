package nogrunt.codegen;

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

public class GenSelCSharpPomClass {
	
	final String classDef = "SelCSharpPomTemplate";
	final String constructorDef = "Constructor";
	final String packageDef = "//package";
	// Constant for ranked locators
	final List<String> RANKED_LOCATORS = List.of(
	        "id", "data-value", "id-xpath", "placeholder", "text", "classname", "dataset-id", "xpath"
	    );
	int waittime = 4000;
	SelJava sj = new SelJava();
	MySQlConn msc = null;
	MySqlConn2 msc2 = null;

	public GenSelCSharpPomClass(MySQlConn msc) {
		this.msc = msc;
		msc2 = new MySqlConn2(msc);
	}
	
	private String getID(String xpath) {
		if(!xpath.contains("|")) {
			return null;
		}
		
		String idpart = xpath.substring(0,xpath.indexOf("|")-1);
		if(idpart.contains("@id=")) {
			idpart = idpart.substring(idpart.indexOf("@id") + 5, idpart.indexOf("\"]"));
			return idpart;
		} else {
			return null;
		}
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

	public void genSelCSharpPOM(int companyId, ResultSet rs, String folderName, int prodid, boolean isMultipleLocatorPom) {
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
                    msc2.updatePomUniqueElement(elementid, uniquename);
                }
                
                //Condition for page change
                if(currentPage == null || !currentPage.equals(fname)) {
                	String tempPath = "SelCSharpPomTemplate.cs";
                	String fileName = fname +".cs";
                	String targetPath = Utilities.getGenPomFilePath(companyId, prodid, folderName, fileName);
                	sj.copyResourceFile(tempPath, targetPath);
        	        File latestFile = new File(targetPath);
        	        latestFilePath = latestFile.toPath();
//        	        String packageStr = "package nogrunt.pages;";
//        	        packageStr = packageStr + "\r\n";
//        	        packageStr = packageStr + "import com.microsoft.playwright.*;";
        	        try {
//        	        	sj.replaceContent(latestFilePath, packageDef, packageStr);
        	        	if (isMultipleLocatorPom) {
        	        		sj.replaceContent(latestFilePath, "//declarations","private ElementHelper elementHelper;");
        	        		sj.replaceContent(latestFilePath, "//helpers", "this.elementHelper = new ElementHelper(driver);");
        	        	}
        	        	else {
        	        		sj.replaceContent(latestFilePath, "//declarations","private WebDriverWait wait;");
        	        		sj.replaceContent(latestFilePath, "//helpers", "this.wait = new WebDriverWait(driver, TimeSpan.FromSeconds(10));");
        	        		sj.replaceContent(latestFilePath, "//IWebElements", "private IWebElement WaitForElementToBeClickable(By locator)\r\n"
        	        				+ "\t\t{\r\n"
        	        				+ "\t\t\treturn wait.Until(ExpectedConditions.ElementToBeClickable(locator));\r\n"
        	        				+ "\t\t}\n\n"
        	        				+ "\t\t//IWebElements");
        	        	}
        	        	sj.replaceContent(latestFilePath, classDef, fname);
        	        	sj.replaceContent(latestFilePath, constructorDef, fname);
        	        }catch(Exception e) {
        	        	e.printStackTrace();
        	        }
        	        currentPage = fname;
                }
       
                String pathDeclaration;
                if (isMultipleLocatorPom) {
                	pathDeclaration = textForMultipleLocators(step, uniquename, elementtype);
                }
                else {
                	pathDeclaration = textForLocatorDecleration(step, uniquename, elementtype);
                }
                String fileContent = new String(Files.readAllBytes(latestFilePath), StandardCharsets.UTF_8);
                fileContent = fileContent.replace("//IWebElements", pathDeclaration+"\n\t\t//IWebElements");
                Files.write(latestFilePath, fileContent.getBytes(StandardCharsets.UTF_8), StandardOpenOption.WRITE);

                String elementMethod;
                if (isMultipleLocatorPom) {
                	elementMethod = textForMultipleLocatorsElementMethod(uniquename, elementtype, step);
                }
                else {
                	elementMethod = textForElementMethod(uniquename, elementtype, step);
                }
                fileContent = new String(Files.readAllBytes(latestFilePath), StandardCharsets.UTF_8);
                fileContent = fileContent.replace("//POM Methods", elementMethod + "\n\t\t//POM Methods");
                Files.write(latestFilePath, fileContent.getBytes(StandardCharsets.UTF_8), StandardOpenOption.WRITE);
                
            }
        } catch(Exception e) {
        	e.printStackTrace();
        } 
    }
	
	public String textForMultipleLocators(JSONObject step, String name, String elementType) {
		String repString = "private Dictionary<string, string> "+name+"Data() => new Dictionary<string, string>\r\n\t\t{\n";
		repString += getElementData(step, elementType);
		repString += "\n\t\t};\n";
		return repString;
	}
	
	public String getElementData(JSONObject step, String elementType) {
		
		StringBuilder sb = new StringBuilder();
        
	    String xpath = (String) step.get("Object_Xpath");
	    String relPath = null;
	    String absPath = null;
	    if (xpath != null && !xpath.equals("") && xpath.contains("|")) {
	        relPath = xpath.substring(0, xpath.indexOf("|") - 1);
	        absPath = xpath.substring(xpath.indexOf("|") + 2, xpath.length());
	    }
	    String action = (String)step.get("Action");
	    String testData = (String)step.get("TestData");
	    String pageDescription = (String)step.get("Page_Description");
	    if (testData != null && testData.contains("\n")) {
		    testData = "";
		}
	    if (elementType.equalsIgnoreCase("button") && (testData == null || testData.equals(""))) testData = pageDescription;
	    String className = (String) step.get("classname");
	    String dataValue = (String) step.get("dataValue");
	    String placeholder = (String) step.get("elementplaceholder");
	    
	    // Extract id from XPath
	    String id = "";
        if (!relPath.isEmpty()) {
            String singleQuote = "@id='";
            String doubleQuote = "@id=\"";
            String endQuote = "']";
            int startIdx = relPath.indexOf(singleQuote);
            if (startIdx == -1) {
                startIdx = relPath.indexOf(doubleQuote);
                endQuote = "\"]";
            }
            if (startIdx != -1) {
                startIdx += 5;
                int endIndex = relPath.indexOf(endQuote, startIdx);
                if (endIndex != -1) {
                    String remainingPath = relPath.substring(endIndex + endQuote.length()).trim();
                    if (remainingPath.isEmpty() || remainingPath.equals("/")) {
                    	id = relPath.substring(startIdx, endIndex);
                    }
                }
            }
        }
        relPath = relPath.replace("\"", "\\\"");
        absPath = absPath.replace("\"", "\\\"");
        
        // Apply logic before appending
        if (id != null && !id.isEmpty()) {
            sb.append("\t\t\t{ \"id\", \"").append(id).append("\" },\n");
        }
        
        if (className != null && !className.isEmpty()) {
            sb.append("\t\t\t{ \"classname\", \"").append(className).append("\" },\n");
        }
        
        if (testData != null && !testData.isEmpty()) {
        	if (!action.equalsIgnoreCase("Enter") && !action.equalsIgnoreCase("change")) {
        		if (elementType.equalsIgnoreCase("button") || elementType.equalsIgnoreCase("span") ||
        				elementType.equalsIgnoreCase("div") || elementType.equalsIgnoreCase("A")) {
        			sb.append("\t\t\t{ \"text\", \"").append(testData).append("\" },\n");
        		}
        	}
        }
        
        if (placeholder != null && !placeholder.isEmpty()) {
            sb.append("\t\t\t{ \"placeholder\", \"").append(placeholder).append("\" },\n");
        }
        
        if (relPath != null && !relPath.isEmpty() && !relPath.startsWith("HTML")) {
            sb.append("\t\t\t{ \"relative-xpath\", \"").append(relPath).append("\" },\n");
        }
        
        if (absPath != null && !absPath.isEmpty()) {
            sb.append("\t\t\t{ \"absolute-xpath\", \"").append(absPath).append("\" },\n");
        }
        
        // Remove the last comma and newline if present
        if (sb.length() > 0) {
            sb.setLength(sb.length() - 2);
        }
        
        return sb.toString();
	}
	
	
	public String textForMultipleLocatorsElementMethod(String name, String elementType, JSONObject step) {
		String action = (String) step.getOrDefault("Action", "");
		String repString = "\n";
		
		if (elementType.equalsIgnoreCase("LABEL")) {
	        repString += "\t\tpublic void scrollTo" + name + "() {\n";
	        repString += "\t\t\telementHelper.ClickElement(" + name + "Data(), \"" + elementType + "\");\n";
	        repString += "\t\t}\n";
	    } else if (elementType.equalsIgnoreCase("LINK") || elementType.equalsIgnoreCase("BUTTON")) {
	        repString += "\t\tpublic void click" + name + "() {\n";
	        repString += "\t\t\telementHelper.ClickElement(" + name + "Data(), \"" + elementType + "\");\n";
	        repString += "\t\t}\n";
	    } else if (elementType.equalsIgnoreCase("INPUT") || elementType.equalsIgnoreCase("TEXTAREA")) {
	        if (action.equalsIgnoreCase("upload")) {
	            repString += "\t\tpublic IWebElement Upload" + name + "() {\n";
	            repString += "\t\t\treturn " + name + "Data();\n";
	            repString += "\t\t}\n";
	        } else {
	            repString += "\t\tpublic void click" + name + "() {\n";
	            repString += "\t\t\telementHelper.ClickElement(" + name + "Data(), \"" + elementType + "\");\n";
	            repString += "\t\t}\n\n";
	            
	            repString += "\t\tpublic void Enter" + name + "(String testData) {\n";
	            repString += "\t\t\telementHelper.EnterText(" + name + "Data(), \"" + elementType + "\", testData);\n";
	            repString += "\t\t}\n";
	        }
	    } else {
	        repString += "\t\tpublic void click" + name + "() {\n";
	        repString += "\t\t\telementHelper.ClickElement(" + name + "Data(), \"" + elementType + "\");\n";
	        repString += "\t\t}\n";
	    }

		return repString;
	}
	
	
	public String textForLocatorDecleration(JSONObject step, String name, String elementType) {
		String repString = "private IWebElement "+name+" => WaitForElementToBeClickable(";
		repString += findElementByPreferredLocatorInSelCSharp(step, elementType);
		repString += ");";
		return repString;
	}
	
	public String textForElementMethod(String name, String type, JSONObject step) {
		String action = (String) step.getOrDefault("Action", "");
		String repString = "\n";
		if(type.equalsIgnoreCase("LABEL")) {
			repString += "\t\tpublic void scrollTo" +name+ "() {\n";
			repString += "\t\t\t"+name+".Click();";
			repString += "\t\t\treturn "+name+";\n\t\t}";
		} else if(type.equalsIgnoreCase("LINK") || type.equalsIgnoreCase("BUTTON")){
			repString += "\t\tpublic void click" +name+ "(){\n";
			repString += "\t\t\t"+name+".Click();\n\t\t}";
		} else if(type.equalsIgnoreCase("INPUT") || type.equalsIgnoreCase("TEXTAREA")){
			if(action.equalsIgnoreCase("upload")) {
				repString += "\t\tpublic IWebElement Upload" +name+ "(){\n";
				repString += "\t\t\treturn "+name+";\n\t\t}";
			}
			else {
				repString += "\t\tpublic void click" +name+ "(){\n";
				repString += "\t\t\t"+name+".Click();";
				repString += "\n\t\t}";
				
				repString += "\n\n";
				
				repString += "\t\tpublic void Enter" +name+ "(String testData){\n";
				repString += "\t\t\t"+name+".SendKeys(testData);\n\t\t}";
			}
		} else {
			repString += "\t\tpublic void click" +name+ "(){\n";
			repString += "\t\t\t"+name+".Click();\n\t\t}";
		}

		return repString;
	}
	
	String findElementByPreferredLocatorInSelCSharp(JSONObject step, String elementType) {

		String relPath = null;
	    String absPath = null;
	    String xpath = (String) step.get("Object_Xpath");
	    if (xpath != null && !xpath.equals("") && xpath.contains("|")) {
	        relPath = xpath.substring(0, xpath.indexOf("|") - 1);
	        absPath = xpath.substring(xpath.indexOf("|") + 2, xpath.length());
	    }
	    String action = (String)step.get("Action");
	    String testData = (String)step.get("TestData");
	    String pageDescription = (String)step.get("Page_Description");
	    if (testData != null && testData.contains("\n")) {
		    testData = "";
		}
	    String className = (String) step.get("classname");
	    boolean isValidId = true;

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
	                        idValue = relPath.substring(startIdx, endIndex);
	                        boolean hasNumber = false;
	                        if (idValue.length() >= 5) hasNumber = idValue.substring(0, 5).matches(".*\\d.*");
	                        if (hasNumber) {
	                        	isValidId = false;
	                        	continue;
	                        }
	                        if (remainingPath.isEmpty() || remainingPath.equals("/")) {
	                            if (idValue.contains(" ") || idValue.matches("^\\d.*")) {
	                            	continue;
	                            }
	                            else {  
	                            	returnString = "By.Id(\"" + idValue + "\")"; //only id based locator
	                            }
	                            return returnString;
	                        }
	                    }
	                }
	            }

	        } else if (locator.equalsIgnoreCase("id-xpath")) {
	        	
	        	if(relPath.contains("@id=") && isValidId) {
	        		relPath = relPath.replace("\"", "\\\"");
	            	returnString = "By.XPath(\"" + relPath + "\")"; //by id relative xpath
	                return returnString;
	        	}
	        	
	        } else if (locator.equalsIgnoreCase("placeholder")) {

	            String elePlaceholder = (String) step.get("elementplaceholder");
	            if (elePlaceholder != null && !elePlaceholder.isBlank()) {
	            	//input[@placeholder='Enter your name']
	            	returnString = "By.XPath(\"//"+ elementType +"[@placeholder='" + elePlaceholder + "']\")"; //by placeholder
	                return returnString;
	            }

	        } else if (locator.equalsIgnoreCase("classname")) {

	            Integer classIndex = (Integer) step.getOrDefault("classnameindex", 0);

	            if (className != null && !className.equals("") && classIndex >= 0) {
	            	classIndex++;
	                returnString = "By.XPath(\"(//*[@class='" + className + "'])["+classIndex+"]\")";
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
	        		if (elementType.equalsIgnoreCase("button") || elementType.equalsIgnoreCase("span") ||
	        				elementType.equalsIgnoreCase("div") || elementType.equalsIgnoreCase("A")) {
	        			if (elementType.equalsIgnoreCase("button") && (testData == null || testData.equals(""))) testData = pageDescription;
	        			if (testData != null && !testData.equals("")) {
	        				if (className != null && !className.equals("") && !elementType.equalsIgnoreCase("div")) {
	        					returnString = "By.XPath(\"//"+ elementType +"[@class='" + className + "' and text()='"+testData+"']\")"; //by classname plus text
	        	                return returnString;
		        			}
		        			else {
		        				returnString = "By.XPath(\"//*[text()='"+testData+"']\")"; //by classname plus text
	        	                return returnString;
		        			}
	        			}
	        		}
	        	}

	        } else if (locator.equalsIgnoreCase("xpath")) {
	        	
	            if (relPath != null) {
	            	relPath = relPath.replace("\"", "\\\"");
	            	returnString = "By.XPath(\"" + relPath + "\")"; //by xpath
	                return returnString;
	            } 
	            else {
	            	absPath = absPath.replace("\"", "\\\"");
	            	returnString = "By.XPath(\"" + absPath + "\")"; //by xpath
	                return returnString;
	            }
	        }
	    }

	    returnString = "null; // Unable to find the element with the preferences\n";
	    return returnString;
	}
	
}
