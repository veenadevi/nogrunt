package nogrunt.codegen;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;
import java.util.Random;

import org.json.simple.JSONObject;

import nogrunt.codegen.SelJava;
import nogrunt.*;

public class GenJavaPomClass {
	
	final String classDef = "runLogic";
	final String packageDef = "//package";
	final List<String> RANKED_LOCATORS = List.of(
	        "id", "data-value", "classname", "id-xpath", "dataset-id", "xpath", "placeholder", "text"
	    );
	int waittime = 4000;
	SelJava sj = new SelJava();
	MySQlConn msc = null;
	MySqlConn2 msc2 = null;

	public GenJavaPomClass(MySQlConn msc) {
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

	public void genJavaPOM(int companyId, ResultSet rs, String folderName, int prodid) {
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
                
            	String pattern = ".*/BUTTON\\[.*\\]/SPAN$";
                if (xpath.matches(pattern)) {
                	elementtype = "BUTTON";
                	elementname = nearestname;
                }

//                if((elementtype != null) && (elementtype.equalsIgnoreCase("INPUT") || elementtype.equalsIgnoreCase("BUTTON") || 
//                		elementtype.equalsIgnoreCase("LABEL") || elementtype.equalsIgnoreCase("SELECT")
//                		|| elementtype.equalsIgnoreCase("TEXTAREA"))) {
//                	elementtype = elementtype;
//                } else if ((elementtype != null) && (elementtype.equalsIgnoreCase("SPAN") && elementname.equals(""))) {
//                	continue;
//                } else if((elementtype != null) && (elementtype.equalsIgnoreCase("A"))) {
//                	elementtype = "LINK";
//                } else {
//                	if(clickable.equals("1")) {
//                		elementtype = "LINK";
//                	} else elementtype = "LABEL";
//                }
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
                	String tempPath = "PomTemplate.java";
                	String fileName = fname +".java";
                	String targetPath = Utilities.getGenPomFilePath(companyId, prodid, folderName, fileName);
                	sj.copyResourceFile(tempPath, targetPath);
        	        File latestFile = new File(targetPath);
        	        latestFilePath = latestFile.toPath();
        	        try {
        	        	sj.replaceContent(latestFilePath, classDef, fname);
        	        }catch(Exception e) {
        	        	e.printStackTrace();
        	        }
        	        currentPage = fname;
                }
       
                String pathDeclaration = textForLocatorDecleration(step, uniquename);
                String fileContent = new String(Files.readAllBytes(latestFilePath), StandardCharsets.UTF_8);
                fileContent = fileContent.replace("//Web Elements", pathDeclaration + "\n\t//Web Elements");
                Files.write(latestFilePath, fileContent.getBytes(StandardCharsets.UTF_8), StandardOpenOption.WRITE);

                
                String elementMethod = textForElementMethod(uniquename, elementtype, step);
                fileContent = new String(Files.readAllBytes(latestFilePath), StandardCharsets.UTF_8);
                fileContent = fileContent.replace("//POM Methods", elementMethod + "\n\t\t//POM Methods");
                Files.write(latestFilePath, fileContent.getBytes(StandardCharsets.UTF_8), StandardOpenOption.WRITE);
                
            }
        } catch(Exception e) {
        	e.printStackTrace();
        } 
    }
	
	public String textForLocatorDecleration(JSONObject step, String name) {
		String repString = "private By "+name+" = ";
		repString += findElementByPreferredLocatorInSelJava(step);
		repString += ";\n";
		return repString;
	}
	
	public String textForElementMethod(String name, String type, JSONObject step) {
		String action = (String) step.getOrDefault("Action", "");
		String repString = "\n";
		if(type.equalsIgnoreCase("LABEL")) {
			repString += "\tpublic void scrollTo" +name+ "() {\n";
			repString += "\t\twaitForElement("+name+").click();";
			repString += "\t\treturn "+name+";\n\t}";
		} else if(type.equalsIgnoreCase("LINK") || type.equalsIgnoreCase("BUTTON")){
			repString += "\tpublic void click" +name+ "(){\n";
			repString += "\t\twaitForElement("+name+").click();\n\t}";
		} else if(type.equalsIgnoreCase("INPUT") || type.equalsIgnoreCase("TEXTAREA")){
			if(action.equalsIgnoreCase("upload")) {
				repString += "\tpublic WebElement Upload" +name+ "(){\n";
				repString += "\t\treturn "+name+";\n\t}";
			}
			else {
				repString += "\tpublic void click" +name+ "(){\n";
				repString += "\t\twaitForElement("+name+").click();";
				repString += "\n\t}";
				
				repString += "\n";
				
				repString += "\tpublic void Enter" +name+ "(String testData){\n";
				repString += "\t\twaitForElement("+name+").sendKeys(testData);\n\t}";
			}
		} else {
			repString += "\tpublic void click" +name+ "(){\n";
			repString += "\t\twaitForElement("+name+").click();\n\t}";
		}

		return repString;
	}
	
	String findElementByPreferredLocatorInSelJava(JSONObject step) {

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
	                            	returnString = "By.id(\"" + idValue + "\")"; //only id based locator
	                            }
	                            return returnString;
	                        }
	                    }
	                }
	            }

	        } else if (locator.equalsIgnoreCase("id-xpath")) {
	        	
	        	if(relPath.contains("@id=")) {
	        		relPath = relPath.replace("\"", "\\\"");
	            	returnString = "By.xpath(\"" + relPath + "\")"; //by id relative xpath
	                return returnString;
	        	}
	        	
	        } else if (locator.equalsIgnoreCase("placeholder")) {

	            String elePlaceholder = (String) step.get("elementplaceholder");
	            if (elePlaceholder != null && !elePlaceholder.isBlank()) {
	                returnString = "page.getByPlaceholder(\""+ elePlaceholder +"\");"; //by placeholder
	                return returnString;
	            }

	        } else if (locator.equalsIgnoreCase("classsname")) {

	            String className = (String) step.get("classname");
	            Integer classIndex = (Integer) step.getOrDefault("classnameindex", 0);

	            if (className != null && !className.equals("") && classIndex > 0) {
	                returnString = "By.xpath(\"(//*[@class='" + className + "'])["+classIndex+"]\")";
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
	            	returnString = "By.xpath(\"" + relPath + "\")"; //by xpath
	                return returnString;
	            } 
	            else {
	            	absPath = absPath.replace("\"", "\\\"");
	            	returnString = "By.xpath(\"" + absPath + "\")"; //by xpath
	                return returnString;
	            }
	        }
	    }

	    returnString = "null; // Unable to find the element with the preferences\n";
	    return returnString;
	}
	
}
