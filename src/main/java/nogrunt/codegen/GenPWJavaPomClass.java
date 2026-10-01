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

public class GenPWJavaPomClass {
	
	final String classDef = "runLogic";
	final String constructorDef = "LoginPage";
	final String packageDef = "//package";
	// Constant for ranked locators
	final List<String> RANKED_LOCATORS = List.of(
	        "id", "data-value", "id-xpath", "rel-xpath", "classname", "dataset-id", "xpath", "placeholder", "text"
	    );
	int waittime = 4000;
	SelJava sj = new SelJava();
	MySQlConn msc = null;
	MySqlConn2 msc2 = null;

	public GenPWJavaPomClass(MySQlConn msc) {
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

	public void genPwJavaPOM(int companyId, ResultSet rs, String folderName, int prodid) {
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
	        		//        		String id = getID(xpath);
                }
                
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
                	String tempPath = "PWJavaPomTemplate.java";
                	String fileName = fname +".java";
                	String targetPath = Utilities.getGenPomFilePath(companyId, prodid, folderName, fileName);
                	sj.copyResourceFile(tempPath, targetPath);
        	        File latestFile = new File(targetPath);
        	        latestFilePath = latestFile.toPath();
        	        String packageStr = "package nogrunt.pages;";
        	        packageStr = packageStr + "\r\n";
        	        packageStr = packageStr + "import com.microsoft.playwright.*;";
        	        try {
        	        	sj.replaceContent(latestFilePath, packageDef, packageStr);
        	        	sj.replaceContent(latestFilePath, classDef, fname);
        	        	sj.replaceContent(latestFilePath, constructorDef, fname);
        	        }catch(Exception e) {
        	        	e.printStackTrace();
        	        }
        	        currentPage = fname;
                }
       
                String pathDeclaration = textForLocatorDecleration(step,elementtype, uniquename);
                String fileContent = new String(Files.readAllBytes(latestFilePath), StandardCharsets.UTF_8);
                fileContent = fileContent.replace("//locator names","private final Locator "+uniquename+";\n\t//locator names");
                fileContent = fileContent.replace("//locator paths", pathDeclaration + "\n\t\t//locator paths");
                Files.write(latestFilePath, fileContent.getBytes(StandardCharsets.UTF_8), StandardOpenOption.WRITE);

//                String elementAction = textForElementActionName(uniquename, elementtype, step);
                String elementMethod = textForElementMethod(uniquename, elementtype, step);
                fileContent = new String(Files.readAllBytes(latestFilePath), StandardCharsets.UTF_8);
                fileContent = fileContent.replace("//POM Methods", elementMethod + "\n//POM Methods");
                Files.write(latestFilePath, fileContent.getBytes(StandardCharsets.UTF_8), StandardOpenOption.WRITE);
                
            }
        } catch(Exception e) {
        	e.printStackTrace();
        } 
    }
	
	public String textForLocatorDecleration(JSONObject step, String elementtype, String name) {
		String repString = "this."+name+" = ";
		repString += findElementByPreferredLocatorInPWJava(step, elementtype);
		return repString;
	}
	
	public String textForElementMethod(String name, String type, JSONObject step) {
		String action = (String) step.getOrDefault("Action", "");
		String repString = "\n";
		if(type.equalsIgnoreCase("LABEL")) {
			repString += "\t\tpublic void scrollTo" +name+ "() {\n";
			repString += "\t\t\t"+name+".click();\n\t\t}";
			repString += "\t\t\treturn "+name+";\n\t\t}";
		} else if(type.equalsIgnoreCase("LINK") || type.equalsIgnoreCase("BUTTON")){
			repString += "\t\tpublic void click" +name+ "(){\n";
			repString += "\t\t\t"+name+".click();\n\t\t}";
		} else if(type.equalsIgnoreCase("INPUT") || type.equalsIgnoreCase("TEXTAREA")){
			if(action.equalsIgnoreCase("upload")) {
				repString += "\t\tpublic Locator Upload" +name+ "(){\n";
				repString += "\t\t\treturn "+name+";\n\t\t}";
			}
			else {
				repString += "\t\tpublic void click" +name+ "(){\n";
				repString += "\t\t\t"+name+".click();\n\t\t}";
				
				repString += "\n\n";
				
				repString += "\t\tpublic void Enter" +name+ "(String testData){\n";
				repString += "\t\t\t"+name+".fill(testData);\n\t\t}";
			}
		} else {
			repString += "\t\tpublic void click" +name+ "(){\n";
			repString += "\t\t\t"+name+".click();\n\t\t}";
		}

		return repString;
	}
	
	public String textForElementActionName(String name, String type, JSONObject step) {
		String src = null;
		String nearestname = null;
		String elementplaceholder = null;
		String testdata = null;
		String xpath = null;
		String relPath = null;
		String idpath = null;
		
		if(step != null && step.size() > 0) {
			src = (String)step.get("imgpath");
			nearestname = (String)step.get("nearestname");
			elementplaceholder = (String)step.get("elementplaceholder");
			testdata = (String)step.get("testdata");
			xpath = (String)step.get("Object_Xpath");
			
			if(xpath != null && xpath.contains("@id=")) {
				if(xpath.contains("|")) {
					relPath = xpath.substring(0,xpath.indexOf("|")-1);
					relPath = relPath.replace("\"", "'");
				}				
				
				if(!relPath.contains("(") && (StringUtils.countMatches(relPath, "[") == 1)) {
					idpath = relPath.substring(relPath.indexOf("=")+1,relPath.length()-1);
					idpath = "#" + idpath.substring(1,idpath.length() -1);
				}
			}
			
			if(type == null || type.equals("")) {
				type = (String)step.get("Keyword");
			} else if(type != null && (type.equalsIgnoreCase("span") || type.equalsIgnoreCase("div"))) {
				type = (String)step.get("Keyword");
			}
		}
		String repString = "\n";
		if(type.equalsIgnoreCase("LABEL")) {
			repString += "\t\tpublic void click" +name+ "(){\n";
			repString += "\t\t\tclick(\r\n"
					+ "    \t\t\tpage.click(\"text=" + nearestname + "\");\r\n\t\t}";
		} else if(type.equalsIgnoreCase("LINK")){
			repString += "\t\tpublic void click" +name+ "(){\n";
			repString +=  "\t\t\tpage.click(\"text=" + nearestname + "\");\r\n\t\t}";
		} else if(type.equalsIgnoreCase("IMG")){
			repString += "\t\tpublic void click" +name+ "(){\n";
			repString += "\t\t\tpage.locator(\"img[src='" + src + "']\").click();\r\n\t\t}";	
		} else if(type.equalsIgnoreCase("BUTTON")){
			repString += "\t\tpublic void click" +name+ "(){\n";
			repString += "\t\t\tpage.locator(\"button:has-text('" + nearestname +"')\").click();\r\n\t\t}";	
		} else if(type.equalsIgnoreCase("INPUT") || type.equalsIgnoreCase("TEXTAREA")){
			if(elementplaceholder != null && !elementplaceholder.equals("")) {
				repString += "\t\tpublic void click" +name+ "(){\n";
				repString +=  "\t\t\tpage.getByPlaceholder(\"" + elementplaceholder + "\").click();\r\n\t\t}\r\n";
				
				repString += "\t\tpublic void enter" +name+ "(String testdata){\n";
				repString += "\t\t\tpage.getByPlaceholder(\"" + elementplaceholder + "\").fill(testdata);\r\n\t\t}";
			} else {
				repString += "\t\tpublic void click" +name+ "(){\n";
				repString = fixLSforLocator(repString, idpath, relPath, nearestname) + "\r\n";
						
				repString += "\t\tpublic void enter" +name+ "(String testdata){\n";
				repString = fixLSforLocator(repString, idpath, relPath, nearestname) + "\r\n";
			}
		} else if(type.equalsIgnoreCase("LI")){
			repString += "\t\tpublic void click" +name+ "(String testdata){\n";
			repString +=  "\t\t\tpage.click(\"text=\" + testdata );\r\n\t\t}";
		} else {
			repString += "\t\tpublic void click" +name+ "(){\n";
			repString = fixLSforClick(repString, idpath, relPath, nearestname);
		}

		return repString;
	}
	
	private String fixLSforClick(String repString, String idpath, String relPath, String nearestname) {
		if(idpath != null) {
			repString +=  "\t\t\tpage.click(\"" + idpath + "\");\r\n\t\t}";
		} else if(relPath != null) {
			repString +=  "\t\t\tpage.click(\"" + relPath + "\");\r\n\t\t}";
		} else {
			repString +=  "\t\t\tpage.click(\"text=" + nearestname + "\");\r\n\t\t}";
		}
		
		return repString;
	}
	
	private String fixLSforLocator(String repString, String idpath, String relPath, String nearestname) {
		if(idpath != null) {
			repString +=  "\t\t\tpage.locator(\"" + idpath + "\");\r\n\t\t}";
		} else if(relPath != null) {
			repString +=  "\t\t\tpage.locator(\"" + relPath + "\");\r\n\t\t}";
		} else {
			repString +=  "\t\t\tpage.locator(\"text=" + nearestname + "\");\r\n\t\t}";
		}
		
		return repString;
	}
	
	String findElementByPreferredLocatorInPWJava(JSONObject step, String elementType) {

	    String relPath = null;
	    String absPath = null;
	    String xpath = (String) step.get("Object_Xpath");
	    if (xpath != null && !xpath.equals("") && xpath.contains("|")) {
	        relPath = xpath.substring(0, xpath.indexOf("|") - 1);
	        absPath = xpath.substring(xpath.indexOf("|") + 2, xpath.length());
	    }
	    String action = (String)step.get("Action");
	    String testData = (String)step.get("TestData");
	    if (testData != null && testData.contains("\n")) {
		    testData = "";
		}
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
	                        if (remainingPath.isEmpty() || remainingPath.equals("/")) {
	                            idValue = relPath.substring(startIdx, endIndex);
	                            idValue = relPath.substring(startIdx, endIndex);
	                            if (idValue.contains(" ") || idValue.matches(".*\\d.*") || idValue.contains(":")) {
	                            	isValidId = false;
	                            	continue;
	                            }
	                            else {
	                            	returnString = "page.locator(\"#" + idValue + "\");"; //only id based locator
	                            }
	                            return returnString;
	                        }
	                    }
	                }
	            }

	        } else if (locator.equalsIgnoreCase("id-xpath")) {
	        	
	        	if(relPath.contains("@id=") && isValidId) {
	        		relPath = relPath.replace("\"", "\\\"");
	            	returnString = "page.locator(\"" + relPath + "\");"; //by id relative xpath
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

	            if (className != null && !className.equals("")) {
	            	String xpathLocator = "(//*[@class='" + className + "'])";

	                // Append index (XPath is 1-based)
	                xpathLocator += "[" + (classIndex + 1) + "]";

	                returnString = "page.locator(\"" + xpathLocator + "\");";
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
	        		if (testData != null && !testData.equals("") && elementType.equalsIgnoreCase("A")) {
		                String locatorString = "//"+elementType+"[.//*[text()='"+testData+"']]";
		                returnString = "page.locator(\"" + locatorString + "\");";//by text
		                return returnString;
		            }
	        	}

	        } else if (locator.equalsIgnoreCase("rel-xpath")) {
	        	if (relPath != null && (!relPath.contains("@id=") || isValidId) && !relPath.contains("HTML")) {
	            	relPath = relPath.replace("\"", "\\\"");
	            	returnString = "page.locator(\"xpath=" + relPath + "\");"; //by xpath
	                return returnString;
	            }
	        } else if (locator.equalsIgnoreCase("xpath")) {
	        	
	            if (relPath != null && (!relPath.contains("@id=") || isValidId)) {
	            	relPath = relPath.replace("\"", "\\\"");
	            	returnString = "page.locator(\"xpath=" + relPath + "\");"; //by xpath
	                return returnString;
	            } 
	            else {
	            	absPath = absPath.replace("\"", "\\\"");
	                returnString = "page.locator(\"xpath=" + absPath + "\");"; //by xpath
	                return returnString;
	            }
	        }
	    }

	    returnString = "null; // Unable to find the element with the preferences\n";
	    return returnString;
	}
	
}
