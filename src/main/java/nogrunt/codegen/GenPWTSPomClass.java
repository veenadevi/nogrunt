package nogrunt.codegen;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import java.sql.ResultSet;

import java.util.Random;


import nogrunt.codegen.SelJava;
import nogrunt.*;

public class GenPWTSPomClass {
	
	final String classDef = "//classname";
	final String packageDef = "//package";
	int waittime = 4000;
	SelJava sj = new SelJava();
	MySQlConn msc = null;
	MySqlConn2 msc2 = null;

	public GenPWTSPomClass(MySQlConn msc) {
		this.msc = msc;
		msc2 = new MySqlConn2(msc);
	}

	public void genPwtsPOM(int companyId, ResultSet rs, String folderName, int prodid) {
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
                
            	String pattern = ".*/BUTTON\\[.*\\]/SPAN$";
                if (xpath.matches(pattern)) {
                	elementtype = "BUTTON";
                	elementname = nearestname;
                }

                if((elementtype != null) && (elementtype.equalsIgnoreCase("INPUT") || elementtype.equalsIgnoreCase("BUTTON") || 
                		elementtype.equalsIgnoreCase("LABEL") || elementtype.equalsIgnoreCase("SELECT")
                		|| elementtype.equalsIgnoreCase("TEXTAREA"))) {
                	elementtype = elementtype;
                } else if ((elementtype != null) && (elementtype.equalsIgnoreCase("SPAN") && elementname.equals(""))) {
                	continue;
                } else if((elementtype != null) && (elementtype.equalsIgnoreCase("A"))) {
                	elementtype = "LINK";
                } else {
                	if(clickable.equals("1")) {
                		elementtype = "LINK";
                	} else elementtype = "LABEL";
                }
                String pagename = rs.getString("pagename");
                int pageId = rs.getInt("pageid");
                String fname = pagename.replaceAll("[^a-zA-Z0-9]", "")+pageId;
                
                if(uniquename == null || uniquename.equals("")) {
                    Random rand = new Random();
                    int randomNumber = 100 + rand.nextInt(900);
                    uniquename = nearestname.replaceAll("[^a-zA-Z0-9]", "") + randomNumber;
            	    // This is to check if the name contains only digits as it was causing issue in the java file.
            	    if (uniquename.matches("\\d+")) {
            	    	uniquename = "removableString" + uniquename;
            	    }
            	    
                    msc2.updatePomUniqueElement(elementid, uniquename);
                }
                
                //Condition for page change
                if(currentPage == null || !currentPage.equals(fname)) {
                	String tempPath = "PlaywrightPomTemplate.ts";
                	String fileName = fname +".ts";
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

                String elementAction = textForElementActionName(uniquename, elementtype, xpath);
                String fileContent = new String(Files.readAllBytes(latestFilePath), StandardCharsets.UTF_8);
                fileContent = fileContent.replace("//Declaration", elementAction + "\n//Declaration");
                Files.write(latestFilePath, fileContent.getBytes(StandardCharsets.UTF_8), StandardOpenOption.WRITE);
                
            }
        } catch(Exception e) {
        	e.printStackTrace();
        } 
    }
	
	public String textForXpathDecleration(String xpath, String name) {
		xpath = xpath.replace("\"", "\\\"");
		String repString = "\n";
		repString += "\t\t@FindBy(xpath =\""+xpath+"\")\n";
		repString += "\t\tprivate WebElement " +name+ ";\n";
		return repString;
	}
	
	public String textForElementActionName(String name, String type, String xpath) {
		String repString = "\n";
		if(type.equalsIgnoreCase("LABEL")) {
			repString = getElementMethod(repString, "scrollTo" +name,  xpath);
			repString = repString + "}";
		} else if(type.equalsIgnoreCase("LINK") || type.equalsIgnoreCase("BUTTON")){
			repString = getElementMethod(repString, "click"+name,  xpath);
			repString = repString + "\t\t\tif (locator) {\n\t\t";
			repString = repString + "\t\tawait locator.click();\n\t\t";
			repString = repString + "\t} else {\n\t\t";
			repString = repString + "\t\tconsole.error('click"+name +"locator is null.');\n\t\t";
			repString = repString + "\t}\n\t\t";
			repString = repString + "}";
		} else if(type.equalsIgnoreCase("INPUT") || type.equalsIgnoreCase("TEXTAREA")){
			repString = getElementMethod(repString, "clickAndEnter" +name,  xpath);
			repString += "\t\t\treturn locator;\n\t\t}";
		} else {
			repString = getElementMethod(repString, name,  xpath);
			repString = repString + "}";
		}

		return repString;
	}
	
	public String getElementMethod(String repString, String name, String xpath) {
		
		if(xpath != null && xpath.contains("|")) {
			String xpath1 = xpath.substring(0,xpath.indexOf("|") - 1);
			String xpath2 = xpath.substring(xpath.indexOf("|") + 1,xpath.length());
			xpath1 = xpath1.trim();
			xpath2 = xpath2.trim();
			
			repString += "\t\tasync " +name+ "(){\n";
			repString +=  "			const locator = await this.findElement('" + xpath + "');\n";
			
		} else {
			repString += "\t\tasync " +name+ "(){\n";
			repString += "\t\t\tconst locator = this.page.locator(`xpath=" + xpath +"`);\n";
			repString += "\t\t\tawait locator.waitFor({ state: 'visible' });\n\t\t}";
			repString += "\t\t\tawait locator.click();\n\t\t";			
		}
		return repString;
	}
	
}
