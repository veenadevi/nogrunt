package nogrunt.codegen;

import java.io.File;
import java.nio.file.Path;
import java.util.List;

import org.json.simple.JSONObject;

import nogrunt.AppProperties;
import nogrunt.MySQlConn;
import nogrunt.MySqlConn2;
import nogrunt.Utilities;

public class GenCypress extends SelJava {
	
//	int stepNum = 1;
//	List<String> lines = null;
//	boolean replace = false;
//	boolean ignore = false;
	MySQlConn msc = null;
	MySqlConn2 msc2 = null;
	final String latestStep = "\n			//Latest Step";
	final String LastStep = "\n			//Add from Here";
	final String stepNumberStr = "	//Step Number -";
//	int addHereLineIndex = 13;
//	final String classSig = "CypressTemplate";
	final String methodDef = "function runfunc() {";
	final String methodCall = "runfunc();";
	final String threadWait = "\n			cy.wait(waitime);";	
	final String takeSS = "\n			//takeSS(ssfilename);";
	
	public void setMsc(MySQlConn m) {
		msc = m;
		msc2 = new MySqlConn2(msc);
	}
	
	public boolean genCode(JSONObject step, int companyId, String randomkey, 
			Utilities utilities, String requestedBy) {
		try {
		
		boolean isLicenseValid = super.checkLicense(companyId);
		
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
			
			justFileName = justFileName + "_" + randomkey + ".cy.js";
			String fullFilePath = Utilities.getCodeFilePath(companyId, justFileName);
			
			String templateFileName = "CypressTemplate.cy.js";
//	        Path templateFilePath = Paths.get(templateFileName); // Adjust this path as needed
	        copyResourceFile(templateFileName, fullFilePath);
	        
	        File latestFile = new File(fullFilePath);
	        Path latestFilePath = latestFile.toPath();

	        String methodStr =  methodDef.replace("runfunc", (String)step.get("method"));
	        String methodCallStr = methodCall.replace("runfunc", (String)step.get("method"));
	        
	        try {
	        	replaceContent(latestFilePath, methodDef, methodStr);
	        	replaceContent(latestFilePath, methodCall, methodCallStr);
	        	JSONObject userCache = utilities.getUserFromCache(randomkey);
				super.msc2 = msc2;
	        	msc2.decrementLicense((int)userCache.get("companyid"),requestedBy);
	        }catch(Exception e) {
	        	e.printStackTrace();
	        }
	        return true;
		} else {
			String codeDir = Utilities.getCodePath(companyId);
			File latestFile = Utilities.getlatestFileFromDir(codeDir, randomkey, ".cy.js");
			Path latestFilePath = latestFile.toPath();
			try {
			
				
				getLinesAsList(latestFilePath);
			
				getStepNum();
				String replacementText = getReplacementText(step, latestFilePath);
			
				if(!ignore) {
					if(replace) {
						replaceContent(latestFilePath, lines.get(lines.size()- (addHereLineIndex + 2)), replacementText);
					} else {
						replacementText =  latestStep + threadWait + replacementText + LastStep;
						replaceContent(latestFilePath, latestStep, stepNumberStr + " " + stepNum);
						replaceContent(latestFilePath, LastStep, replacementText);
						if(stepNum % 50 == 0) {
							JSONObject userCache = utilities.getUserFromCache(randomkey);
							msc2.decrementLicense((int)userCache.get("companyid"),requestedBy);
						}
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		}catch (Exception ex) {
			ex.printStackTrace();
			System.err.println(ex.getMessage());
		} finally {
			//msc is persistent msc2 is only needed for codegen
			msc2 = null;
		}
		
		return true;
	}
	
public String getReplacementText(JSONObject step, Path filePath) {
		
		String replacementText = "";
		String stepType = (String)step.get("action");
		String testData = (String)step.get("testdata");
		addHereLineIndex = 13;
		
		if(stepType.equalsIgnoreCase("apidata")) {
			ignore = true;
			return null;
		}
		
		if(stepType.equalsIgnoreCase("url")) {
			replacementText = replacementText + "\n";
			String url = (String)step.get("url");
			replacementText = replacementText + "			cy.visit('"+ url + "');";
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
				replacementText = replacementText.replace(";", ".click();");
//				replacementText = replacementText + "\n";
//				replacementText = replacementText + "			" +fieldname + ".click();";
			} else if(stepType.equalsIgnoreCase("submit")) {
				replacementText = replacementText + "\n";
				replacementText = replacementText + findElement(xpath, fieldname, step, isShadow);
				replacementText = replacementText + "\n";
				replacementText = replacementText + "			" + fieldname + ".submit();";
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
				replacementText = replacementText + findElement(xpath, fieldname, step, isShadow);
				replacementText = replacementText + "\n";
				replacementText = replacementText + "			" + fieldname + ".submit();";
			} else if(stepType.equalsIgnoreCase("Enter") || stepType.equalsIgnoreCase("change")) {
				String inputType = (String)step.get("type");
				replacementText = replacementText + "\n";
				String elementLocator = findElement(xpath, fieldname, step, isShadow);
				String prevElement = findElement(xpath, prevFieldname, step, isShadow);
				String peStmt = prevElement;
				prevElement = prevElement.trim();
				prevElement = prevElement.substring(0, prevElement.indexOf("\",\""));
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
					if(addhere.trim().equals(LastStep.trim())) {
						String send = lines.get(lines.size() - (addHereLineIndex + 2));
						if(send.trim().contains(".type(")) {
							String eleLoc = lines.get(lines.size() - (addHereLineIndex + 2));
							eleLoc = eleLoc.trim();
							eleLoc = eleLoc.substring(0, eleLoc.indexOf("\",\""));
							if(eleLoc.equals(prevElement)) {
								replace = true;
							} else if(isShadow &&
									prevElement.trim().endsWith(eleLoc.trim())) {
								replace = true;
							}
						}						
					}
					
					if(replace) {
						replacementText = peStmt.replace(";", ".type(\"" + testData + "\");");
						return replacementText;
					} else {
//						replacementText = replacementText + "\n";
						replacementText = replacementText.replace(";", ".type('" + testData + "');");
//						replacementText = replacementText + fieldname + ".sendKeys(\"" + testData + "\");";
					}
				}
			} else if(stepType.equalsIgnoreCase("dblClick")) {
				replacementText = replacementText + "\n";
				replacementText = replacementText + findElement(xpath, fieldname, step, isShadow);
				replacementText = replacementText + "\n";
				replacementText = replacementText + "			Actions actions = new Actions(driver);";
				replacementText = replacementText + "\n";
				replacementText = replacementText + "			actions.doubleClick(" + fieldname + ").perform();";
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
					replacementText = replacementText + "			" + fieldname + ".sendKeys(Keys.ARROW_UP);";
				} else if (stepType.equalsIgnoreCase("ArrowDown")) {
					replacementText = replacementText + "\n";
					replacementText = replacementText + "			" + fieldname + ".sendKeys(Keys.ARROW_DOWN);";
				}
			} else if(stepType.equalsIgnoreCase("rightclick")) {
				replacementText = replacementText + "\n";
				replacementText = replacementText + findElement(xpath, fieldname, step, isShadow);
				replacementText = replacementText + "\n";
				replacementText = replacementText + "			Actions actions = new Actions(driver);";
				replacementText = replacementText + "\n";
				replacementText = replacementText + "			actions.contextClick(" + fieldname + ").build().perform();";
			} else {
				System.out.println("Action not supported" + stepType);
			}
			
			 String takessreplace = takeSS.replace("ssfilename", "\"" + fieldname + ".jpg" + "\"");
			 replacementText = replacementText + takessreplace ;
		}
        return replacementText;
	}

private String findElement(String xpath, String fieldname, JSONObject step, boolean isShadow) {
	if(isShadow) {
		String replaceStr = "";
		JSONObject shadowElements = (JSONObject)step.get("shadowelements");
		replaceStr = "\n			lookingFor = \"" + (String)shadowElements.get("type")+ "\";";
		replaceStr = replaceStr + "\n			lookingForIndex = " + (long)shadowElements.get("index") + ";";
		replaceStr = replaceStr + "\n			WebElement " + fieldname + "= getShadowElement(\"" + xpath +"\", lookingFor, lookingForIndex );";		
		return replaceStr;
	} else {
		String testData = (String)step.get("testdata"); 
		String altpath = (String)step.get("altpath");
		return "			findElement(\"" + xpath + "\",\"" + 
				testData + "\",\""+ altpath +"\");";
	}
}

/*
 * Hi Deva, below is a custom findElement function with the behavior you described:

javascript
Copy code
// Custom findElement function with error handling
function findElement(xpath, searchString, altpath) {
  let we = null;
  let xpath1 = null;
  let xpath2 = null;

  try {
    if (xpath != null && xpath !== '' && xpath.includes('|')) {
      xpath1 = xpath.substring(0, xpath.indexOf('|') - 1);
      xpath2 = xpath.substring(xpath.indexOf('|') + 2, xpath.length);
    } else {
      throw new Error('Xpath1 not valid');
    }

    we = cy.xpath(xpath1);
    return we;
  } catch (expath1) {
    try {
      if (xpath2 === null) {
        throw new Error('Xpath2 not valid');
      }
      we = cy.xpath(xpath2);
      return we;
    } catch (expath2) {
      try {
        if (searchString != null && searchString !== '') {
          const xsearch = //*[text()='${searchString}'];
          we = cy.xpath(xsearch);
          return we;
        } else {
          throw new Error('No search path');
        }
      } catch (ealtpath) {
        try {
          if (altpath != null && altpath !== '') {
            const xalt = //*[text()='${altpath}'];
            we = cy.xpath(xalt);
            return we;
          }
        } catch (e) {
          // Log the error and return null
          cy.log(e.message);
          return null;
        }
      }
    }
  }
}
This custom findElement function attempts to locate an element on the page using the provided XPath expression. If an error occurs during the execution of any of the XPath expressions, instead of breaking the script, it logs the error message using cy.log and returns null. This allows the script to continue executing without interruption.

You can integrate this findElement function into your Cypress test script and use it to handle errors gracefully during element location. Let me know if you need further assistance!

Let me if it solves your problem ?
 * 
 * 
 */

}
