package nogrunt.codegenNew;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import org.json.simple.JSONObject;

import nogrunt.Utilities;

public class SelJavaCodeGen extends CodeGenHelper {
	
	final String latestStep = "\n\t\t\t//Latest Step";
	final String LastStep = "\n\t\t\t//Add from Here";
	final String pageConstants = "\n\t\t//pageconstants";
	final String fileExtension = ".java";
	final String classSig = "JavaTemplate";
	final String templateFileName = "JavaTemplate.java";
	
	@Override
	public String getLatestStepFormat() {
		return latestStep;
	}
	
	@Override
	public String getLastStepFormat() {
		return LastStep;
	}
	
	@Override
	public void addActionClassPageConstantText(String pagename, String objectName, Path filePath) {
		String pcText = pagename + " " +objectName + " = new " + pagename+"(page);";
        pcText = pcText + pageConstants;
        
        try {
        	Utilities.replaceContent(filePath, pageConstants, pcText);
        }catch (Exception e) {
        	e.printStackTrace();
        }
	}
	
	@Override
	public String getFileExtension() {
		return fileExtension;
	}
	
	@Override
	public String getPomFileExtension() {
		return fileExtension;
	}
	
	@Override
	public String getClassSig() {
		return classSig;
	}
	
	@Override
	public String getTemplateFileName() {
		return templateFileName;
	}
	
	@Override
	public String getNavigateCode(String url) {
	    return "\t\t\tdriver.get(\"" + url + "\");";
	}
	
	@Override
	public String getActionClassCodeForElement(String elementType, String action, String fieldname, JSONObject step, String testData) {
		//to implement
		String replaceStr = "\t\t\tWebElement "+fieldname+" = ";
		replaceStr += this.findElementByPreferredLocator(step, elementType) + ";";
		if (action.equalsIgnoreCase("Enter") || action.equalsIgnoreCase("change") || action.equalsIgnoreCase("upload")) {
			replaceStr += "\n\t\t\t"+fieldname+".sendKeys(\""+testData+"\");";
		} else {
			replaceStr += "\n\t\t\t"+fieldname+".click();";
		}
		return replaceStr;
	}
	
	@Override
	public String getActionClassPomCodeForElement(String elementtype, String action, String fieldname, String objectName, String uniquename, String testData) {
		String replaceStr = "";
		if(elementtype.equalsIgnoreCase("LABEL")) {
			replaceStr +=  "\t\t\tLocator " + fieldname +" = "+ objectName +".scrollTo" +uniquename+ "();";
		} else if(elementtype.equalsIgnoreCase("LINK") || elementtype.equalsIgnoreCase("BUTTON")){
			replaceStr +=  "\t\t\t"+objectName +".click" +uniquename+ "();";
		} else if(elementtype.equalsIgnoreCase("INPUT") || elementtype.equalsIgnoreCase("TEXTAREA")){
			if(action.equalsIgnoreCase("upload")) {
				replaceStr +=  "\t\t\tLocator " + fieldname +" = "+ objectName +".Upload" +uniquename+ "();";
			}
			else if (action.equalsIgnoreCase("ENTER") || action.equalsIgnoreCase("CHANGE")) {
				replaceStr +=  "\t\t\t" + objectName +".Enter" +uniquename+ "(\""+testData+"\");"; 
			}
			else {
				replaceStr +=  "\t\t\t" + objectName +".click" +uniquename+ "();";
			}
		} else {
			replaceStr +=  "\t\t\t"+objectName +".click" +uniquename+ "();";
		}
		
		return replaceStr;
	}
	
	@Override
	public String getPomTemplateFileName() {
		return "PWJavaPomTemplate.java";
	}
	
	
	@Override
	public void replacePomClassInitializations(Path filePath, String fname, boolean isMultipleLocatorPom) {
		try {
			if (isMultipleLocatorPom) {
				Utilities.replaceContent(filePath, "private Page page;","private Page page;\n\tprivate ElementHelper elementHelper;");
				Utilities.replaceContent(filePath, "this.page = page;", "this.page = page;\n\t\tthis.elementHelper = new ElementHelper(page);");
			}
			else {
				
			}
			String packageStr = "package nogrunt.pages;";
	        packageStr = packageStr + "\r\n";
	        packageStr = packageStr + "import com.microsoft.playwright.*;";
	        Utilities.replaceContent(filePath, "//package", packageStr);
	        Utilities.replaceContent(filePath, "runLogic", fname);
	        Utilities.replaceContent(filePath, "LoginPage", fname);
		} catch (IOException e) {
			System.out.println("error in intializing pom class code template");
			e.printStackTrace();
		}
	}
	
	@Override
	public void addTextForPomClassElementMethod(Path filePath, String uniquename, String elementType, JSONObject step, boolean isMultipleLocatorPom) {
		
		try {
			String elementMethod;
			if (isMultipleLocatorPom) {
				elementMethod = textForPomClassMultipleLocatorsElementMethod(uniquename, elementType, step);
			}
			else {
				elementMethod = textForPomClassNormalElementMethod(uniquename, elementType, step);
			}
			String fileContent = new String(Files.readAllBytes(filePath), StandardCharsets.UTF_8);
			fileContent = fileContent.replace("//POM Methods", elementMethod + "\n\t\t//POM Methods");
			Files.write(filePath, fileContent.getBytes(StandardCharsets.UTF_8), StandardOpenOption.WRITE);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public String textForPomClassNormalElementMethod(String name, String type, JSONObject step) {
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
	
	@Override
	public void addTextForPomClassElementLocator(Path filePath, String uniquename, String elementType, JSONObject step, boolean isMultipleLocatorPom) {
		
		try {
			String pathDeclaration;
			String fileContent = new String(Files.readAllBytes(filePath), StandardCharsets.UTF_8);
			if (isMultipleLocatorPom) {
				pathDeclaration = textForPomClassMultipleLocators(step, uniquename, elementType);
				fileContent = fileContent.replace("//locator names",pathDeclaration+";\n\t//locator names");
			}
			else {
				pathDeclaration = textForPomClassNormalLocator(step, uniquename, elementType);
				fileContent = fileContent.replace("//locator names","private final Locator "+uniquename+";\n\t//locator names");
				fileContent = fileContent.replace("//locator paths", pathDeclaration + "\n\t\t//locator paths");
			}
			Files.write(filePath, fileContent.getBytes(StandardCharsets.UTF_8), StandardOpenOption.WRITE);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	@Override
	public String textForPomClassNormalLocator(JSONObject step, String name, String elementType) {
		String repString = "this."+name+" = ";
		repString += findElementByPreferredLocator(step, elementType) + ";";
		return repString;
	}
	
	@Override
	public String getLocatorById(String idValue) {
	    return "driver.findElement(By.id(\"" + idValue + "\"))";
	}

	@Override
	public String getLocatorByXpath(String xpath) {
	    return "driver.findElement(By.xpath(\"" + xpath + "\"))";
	}

	@Override
	public String getLocatorByPlaceholder(String placeholder, String elementType) {
	    return "driver.findElement(By.xpath(\"//" + elementType + "[@placeholder='" + placeholder + "']\"))";
	}

	@Override
	public String getLocatorByClassNameAndIndex(String className, Integer classIndex) {
	    return "driver.findElement(By.xpath(\"(//*[@class='" + className + "'])[" + classIndex + "]\"))";
	}

	@Override
	public String getLocatorByClassNameAndText(String className, String testData, String elementType) {
	    return "driver.findElement(By.xpath(\"//" + elementType + "[@class='" + className + "' and text()='" + testData + "']\"))";
	}

	@Override
	public String getLocatorByText(String testData) {
	    return "driver.findElement(By.xpath(\"//*[text()='" + testData + "']\"))";
	}
	
	
	@Override
	public String textForPomClassMultipleLocators(JSONObject step, String name, String elementType) {
		StringBuilder repString = new StringBuilder();
		repString.append("private Map<String, String> ").append(name).append("Data() {\n");
        repString.append("\t\treturn Map.of(\n");
        repString.append(getElementDataForPomClassMultipleLocators(step, elementType));
        repString.append("\n\t\t);\n");
        repString.append("\t}\n");
        return repString.toString();
	}
	
	@Override
	public void appendData(StringBuilder sb, String key, String value) {
	    if (value != null && !value.isEmpty()) {
	    	sb.append("\t\t\t\"").append(key).append("\", \"").append(value).append("\",\n");
	    }
	}
	
	@Override
	public String textForPomClassMultipleLocatorsElementMethod(String name, String elementType, JSONObject step) {
		String action = (String) step.getOrDefault("Action", "");
		String repString = "\n";
		
		if (elementType.equalsIgnoreCase("LABEL")) {
	        repString += "\t\tpublic void scrollTo" + name + "() {\n";
	        repString += "\t\t\telementHelper.clickElement(" + name + "Data(), \"" + elementType + "\");\n";
	        repString += "\t\t}\n";
	    } else if (elementType.equalsIgnoreCase("LINK") || elementType.equalsIgnoreCase("BUTTON")) {
	        repString += "\t\tpublic void click" + name + "() {\n";
	        repString += "\t\t\telementHelper.clickElement(" + name + "Data(), \"" + elementType + "\");\n";
	        repString += "\t\t}\n";
	    } else if (elementType.equalsIgnoreCase("INPUT") || elementType.equalsIgnoreCase("TEXTAREA")) {
	        if (action.equalsIgnoreCase("upload")) {
	            repString += "\t\tpublic Locator Upload" + name + "() {\n";
	            repString += "\t\t\treturn " + name + "Data();\n";
	            repString += "\t\t}\n";
	        } else {
	            repString += "\t\tpublic void click" + name + "() {\n";
	            repString += "\t\t\telementHelper.clickElement(" + name + "Data(), \"" + elementType + "\");\n";
	            repString += "\t\t}\n\n";
	            
	            repString += "\t\tpublic void Enter" + name + "(String testData) {\n";
	            repString += "\t\t\telementHelper.enterText(" + name + "Data(), \"" + elementType + "\", testData);\n";
	            repString += "\t\t}\n";
	        }
	    } else {
	        repString += "\t\tpublic void click" + name + "() {\n";
	        repString += "\t\t\telementHelper.clickElement(" + name + "Data(), \"" + elementType + "\");\n";
	        repString += "\t\t}\n";
	    }

		return repString;
	}

	
}