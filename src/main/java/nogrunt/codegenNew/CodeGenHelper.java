package nogrunt.codegenNew;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import org.json.simple.JSONObject;

import nogrunt.Utilities;

public abstract class CodeGenHelper {
	
	final List<String> RANKED_LOCATORS = List.of(
	        "id", "data-value", "id-xpath", "placeholder", "text", "classname", "dataset-id", "xpath"
	    );
	
	public static CodeGenHelper getClassByGenType(String genType) {
		if (genType != null && genType.equals("pwjava")) {
			return new PlaywrightJavaCodeGen();
		}
		else if (genType != null && genType.equals("selc#")) {
			return new SelCSharpCodeGen();
		}
		else if (genType != null && genType.equals("pwts")) {
			return new PlaywrightTypeScriptCodeGen();
		}
		else if (genType != null && genType.equals("seljava")) {
			return new SelJavaCodeGen();
		}
		else {
			return new PlaywrightJavaCodeGen();
		}
	}
	
	public abstract String getLatestStepFormat();
	
	public abstract String getLastStepFormat();
	
	public abstract String getFileExtension();
	
	public abstract String getPomFileExtension();
	
	public abstract String getClassSig();
	
	public abstract String getTemplateFileName();
	
	public abstract String getNavigateCode(String url);
	
	public abstract void addActionClassPageConstantText(String pagename, String objectName, Path filePath);
	
	public abstract String getActionClassPomCodeForElement(String elementtype, String action, String fieldname, String objectName, String uniquename, String testData);
	
	public abstract String getPomTemplateFileName();
	
	public abstract void replacePomClassInitializations(Path filePath, String fname, boolean isMultipleLocatorPom);
	
	public abstract void addTextForPomClassElementMethod(Path filePath, String uniquename, String elementType, JSONObject step, boolean isMultipleLocatorPom);
	
	public abstract String textForPomClassNormalElementMethod(String name, String type, JSONObject step);
	
	public abstract void addTextForPomClassElementLocator(Path filePath, String uniquename, String elementType, JSONObject step, boolean isMultipleLocatorPom);
	
	public abstract String textForPomClassNormalLocator(JSONObject step, String name, String elementType);
	
	public abstract String getLocatorById(String idValue);
	
	public abstract String getLocatorByXpath(String xpath);
	
	public abstract String getLocatorByPlaceholder(String placeholder, String elementType);
	
	public abstract String getLocatorByClassNameAndIndex(String className, Integer classIndex);
	
	public abstract String getLocatorByClassNameAndText(String className, String testData, String elementType);
	
	public abstract String getLocatorByText(String testData);
	
	public abstract String textForPomClassMultipleLocators(JSONObject step, String name, String elementType);
	
	public abstract void appendData(StringBuilder sb, String key, String value);
	
	public abstract String textForPomClassMultipleLocatorsElementMethod(String name, String elementType, JSONObject step);
	
	
	
	String findElementByPreferredLocator(JSONObject step, String elementType) {

		String relPath = null;
	    String absPath = null;
	    String xpath = (String) step.get("Object_Xpath");
	    if (xpath != null && !xpath.equals("") && xpath.contains("|")) {
	        relPath = xpath.substring(0, xpath.indexOf("|") - 1);
	        absPath = xpath.substring(xpath.indexOf("|") + 2, xpath.length());
	    }
	    if (absPath == null) absPath = xpath;
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
	                            	returnString = getLocatorById(idValue); //only id based locator
	                            }
	                            return returnString;
	                        }
	                    }
	                }
	            }

	        } else if (locator.equalsIgnoreCase("id-xpath")) {
	        	
	        	if(relPath != null && relPath.contains("@id=") && isValidId) {
	        		relPath = relPath.replace("\"", "\\\"");
	            	returnString = getLocatorByXpath(relPath); //by id relative xpath
	                return returnString;
	        	}
	        	
	        } else if (locator.equalsIgnoreCase("placeholder")) {

	            String elePlaceholder = (String) step.get("elementplaceholder");
	            if (elePlaceholder != null && !elePlaceholder.isBlank()) {
	            	//input[@placeholder='Enter your name']
	            	returnString = getLocatorByPlaceholder(elePlaceholder, elementType); //by placeholder
	                return returnString;
	            }

	        } else if (locator.equalsIgnoreCase("classname")) {

	            Integer classIndex = (Integer) step.getOrDefault("classnameindex", 0);

	            if (className != null && !className.equals("") && classIndex >= 0) {
	            	classIndex++;
	                returnString = getLocatorByClassNameAndIndex(className, classIndex);
	                return returnString; 
	            }

	        } else if (locator.equalsIgnoreCase("data-value")) {
	        	
//	        	String dataValue = (String) step.get("dataValue");
//	            Integer dataValueIndex = (Integer) step.getOrDefault("dataValueIndex", 0);
//	        	if (dataValue != null && dataValueIndex >= 0) {
//        			returnString = "page.locator(\"[data-value='"+dataValue+"']\")"; //by data value
//        			returnString += ".nth("+dataValueIndex+");";
//	                return returnString;
//	        	}

	        } else if (locator.equalsIgnoreCase("text")) {
	        	
	        	if (!action.equalsIgnoreCase("Enter") && !action.equalsIgnoreCase("change")) {
	        		if (elementType.equalsIgnoreCase("button") || elementType.equalsIgnoreCase("span") ||
	        				elementType.equalsIgnoreCase("div") || elementType.equalsIgnoreCase("A")) {
	        			if (elementType.equalsIgnoreCase("button") && (testData == null || testData.equals(""))) testData = pageDescription;
	        			if (testData != null && !testData.equals("")) {
	        				if (className != null && !className.equals("") && !elementType.equalsIgnoreCase("div")) {
	        					returnString = getLocatorByClassNameAndText(className, testData, elementType); //by classname plus text
	        	                return returnString;
		        			}
		        			else {
		        				returnString = getLocatorByText(testData); //by text
	        	                return returnString;
		        			}
	        			}
	        		}
	        	}

	        } else if (locator.equalsIgnoreCase("xpath")) {
	        	
	            if (relPath != null) {
	            	relPath = relPath.replace("\"", "\\\"");
	            	returnString = getLocatorByXpath(relPath); //by xpath
	                return returnString;
	            } 
	            else {
	            	absPath = absPath.replace("\"", "\\\"");
	            	returnString = getLocatorByXpath(absPath); //by xpath
	                return returnString;
	            }
	        }
	    }

	    returnString = "null; // Unable to find the element with the preferences\n";
	    return returnString;
	}
	
	
	public String getElementDataForPomClassMultipleLocators(JSONObject step, String elementType) {
		
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
            appendData(sb, "id", id);
        }
        
        if (className != null && !className.isEmpty()) {
            appendData(sb, "classname", className);
        }
        
        if (testData != null && !testData.isEmpty()) {
        	if (!action.equalsIgnoreCase("Enter") && !action.equalsIgnoreCase("change")) {
        		if (elementType.equalsIgnoreCase("button") || elementType.equalsIgnoreCase("span") ||
        				elementType.equalsIgnoreCase("div") || elementType.equalsIgnoreCase("A")) {
        			appendData(sb, "text", testData);
        		}
        	}
        }
        
        if (placeholder != null && !placeholder.isEmpty()) {
            appendData(sb, "placeholder", placeholder);
        }
        
        if (relPath != null && !relPath.isEmpty() && !relPath.startsWith("HTML")) {
            appendData(sb, "relative-xpath", relPath);
        }
        
        if (absPath != null && !absPath.isEmpty()) {
            appendData(sb, "absolute-xpath", absPath);
        }
        
        // Remove the last comma and newline if present
        if (sb.length() > 0) {
            sb.setLength(sb.length() - 2);
        }
        
        return sb.toString();
	}
	
	
	public String escapeDoubleQuotes(String input) {
		return input.replace("\"", "\\\"");
    }
	
	public String getJustFileName(String filename, String randomKey) {
		if (filename == null || filename.equals("")) return null;
		String justFileName = filename + "_" + randomKey;
		if(justFileName != null) {
			justFileName = justFileName.trim().replaceAll(" ", "_");
			justFileName = justFileName.replaceAll("[\\\\/:*?\"<>|]", "_");  // Replace illegal characters
		}
		return justFileName;
	}
	
	
	public String getActionClassCodeForElement(String elementType, String action, String fieldname, JSONObject step, String testData) {
		//to implement
		String replaceStr = "\t\t\tLocator "+fieldname+" = ";
		replaceStr += this.findElementByPreferredLocator(step, elementType) + ";";
		if (action.equalsIgnoreCase("Enter") || action.equalsIgnoreCase("change") || action.equalsIgnoreCase("upload")) {
			replaceStr += "\n\t\t\t"+fieldname+".fill(\""+testData+"\");";
		} else {
			replaceStr += "\n\t\t\t"+fieldname+".click();";
		}
		return replaceStr;
	}
	
	public String generateIframeSelScript(JSONObject step, String iframexpath, String fieldname, String xpath) {
		//to implement
		System.out.println("Not supported: this is an iframe ");
		String replaceStr = "";
	    if(iframexpath != null && iframexpath.equals("")) {
//		    replaceStr += "\t\tdriver.switchTo().defaultContent();\n";
	    }
	    else {
	    	replaceStr = "			Frame " + fieldname + "X = null;\n";
		    iframexpath = escapeDoubleQuotes(iframexpath); 
		    replaceStr += "\t\t\ttry {\n";
		    replaceStr += "\t\t\t\t " + fieldname + "X = page.frameLocator(\"xpath=" +iframexpath+"\").frame(); \n";
		    replaceStr += "\t\t\t} catch (Exception e) {\n";
		    replaceStr += "\t\t\t\t\tthrow e;\n";
		    replaceStr += "\t\t\t}\n";
	    }
	    String selScriptString = replaceStr;
	    return selScriptString;
	}
	
	public String generateTabChnageSelScript(int tabid, HashMap tabs,  String fieldname) {
		//to implement
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
	
	public String generateShadowCode(JSONObject step, String fieldname, String xpath) {
		//to implement
		JSONObject shadowElements = (JSONObject)step.get("shadowelements");
		String replaceStr = "\n			lookingFor = \"" + (String)shadowElements.get("type")+ "\";";
		replaceStr = replaceStr + "\n			lookingForIndex = " + (long)shadowElements.get("index") + ";";
		replaceStr = replaceStr + "\n			const " + fieldname + "= getShadowElement(\"" + xpath +"\", lookingFor, lookingForIndex );";
		return replaceStr;
	}
	
}