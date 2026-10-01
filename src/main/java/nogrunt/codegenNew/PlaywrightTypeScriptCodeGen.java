package nogrunt.codegenNew;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import org.json.simple.JSONObject;

import nogrunt.Utilities;

public class PlaywrightTypeScriptCodeGen extends CodeGenHelper {
	
	final String latestStep = "\n\t//Latest Step";
	final String LastStep = "\n\t//Add from Here";
	final String pageConstants = "\n\t//page constants";
	final String importStep = "\n//import";
	final String fileExtension = ".spec.ts";
	final String pomFileExtension = ".ts";
	final String classSig = "//title";
	final String templateFileName = "playwrightTypeScriptTemplate.ts";
	
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
		String pcText = "const "+objectName+" = new "+pagename+"(page);";
        pcText = pcText + pageConstants;
        String importText = "import { "+pagename+" } from '../pages/"+pagename+"';" + importStep;
        try {
        	Utilities.replaceContent(filePath, pageConstants, pcText);
        	Utilities.replaceContent(filePath, importStep, importText);
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
		return pomFileExtension;
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
		return "\tawait page.goto(\"" + url + "\");";
	}
	
	@Override
	public String getActionClassPomCodeForElement(String elementtype, String action, String fieldname, String objectName, String uniquename, String testData) {
		String replaceStr = "";
		if(elementtype.equalsIgnoreCase("LABEL")) {
			replaceStr +=  "\tawait " + objectName +".scrollTo" +uniquename+ "();";
		} else if(elementtype.equalsIgnoreCase("LINK") || elementtype.equalsIgnoreCase("BUTTON")){
			replaceStr +=  "\tawait "+objectName +".click" +uniquename+ "();";
		} else if(elementtype.equalsIgnoreCase("INPUT") || elementtype.equalsIgnoreCase("TEXTAREA")){
			if(action.equalsIgnoreCase("upload")) {
				replaceStr +=  "\t\t\tLocator " + fieldname +" = "+ objectName +".Upload" +uniquename+ "();";
			}
			else if (action.equalsIgnoreCase("ENTER") || action.equalsIgnoreCase("CHANGE")) {
				replaceStr +=  "\tawait " + objectName +".Enter" +uniquename+ "(\""+testData+"\");"; 
			}
			else {
				replaceStr +=  "\tawait " + objectName +".click" +uniquename+ "();";
			}
		} else {
			replaceStr +=  "\tawait "+ objectName +".click" +uniquename+ "();";
		}
		
		return replaceStr;
	}
	
	@Override
	public String getPomTemplateFileName() {
		return "playwrightTypeScriptPomTemplate.ts";
	}
	
	@Override
	public void replacePomClassInitializations(Path filePath, String fname, boolean isMultipleLocatorPom) {
		try {
			if (isMultipleLocatorPom) {
				Utilities.replaceContent(filePath, "private page: Page;","private page: Page;\n\tprivate elementHelper: ElementHelper;");
				Utilities.replaceContent(filePath, "//import","import { ElementHelper } from './ElementHelper';");
				Utilities.replaceContent(filePath, "//locator paths", "this.elementHelper = new ElementHelper(page);");
			}
			else {
				
			}
	        Utilities.replaceContent(filePath, "runLogic", fname);
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
			fileContent = fileContent.replace("//POM Methods", elementMethod + "\n\t//POM Methods");
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
			repString += "\tasync scrollTo"+name+"(){\n";
			repString += "\t\tawait this."+name+".click();\n\t}";
		} else if(type.equalsIgnoreCase("LINK") || type.equalsIgnoreCase("BUTTON")){
			repString += "\tasync click" +name+ "(){\n";
			repString += "\t\tawait this."+name+".click();\n\t}";
		} else if(type.equalsIgnoreCase("INPUT") || type.equalsIgnoreCase("TEXTAREA")){
			if(action.equalsIgnoreCase("upload")) {
				repString += "\t\tpublic Locator Upload" +name+ "(){\n";
				repString += "\t\t\treturn "+name+";\n\t\t}";
			}
			else {
				repString += "\tasync click"+name+"(){\n";
				repString += "\t\tawait this."+name+".click();\n\t}";
				
				repString += "\n\n";
				
				repString += "\tasync Enter"+name+"(testData: string){\n";
				repString += "\t\tawait this."+name+".fill(testData);\n\t}";
			}
		} else {
			repString += "\tasync click" +name+ "(){\n";
			repString += "\t\tawait this."+name+".click();\n\t}";
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
				fileContent = fileContent.replace("//locator names",pathDeclaration+"\n\n\t//locator names");
			}
			else {
				pathDeclaration = textForPomClassNormalLocator(step, uniquename, elementType);
				fileContent = fileContent.replace("//locator names","readonly "+uniquename+": Locator;\n\t//locator names");
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
		return "page.locator(\"#" + idValue + "\")";
	}
	
	@Override
	public String getLocatorByXpath(String xpath) {
		return "page.locator(\"" + xpath + "\")";
	}
	
	@Override
	public String getLocatorByPlaceholder(String placeholder, String elementType) {
		return "page.getByPlaceholder(\""+ placeholder +"\")";
	}
	
	@Override
	public String getLocatorByClassNameAndIndex(String className, Integer classIndex) {
		return "page.locator(\"(//*[@class='" + className + "'])["+classIndex+"]\")";
	}
	
	@Override
	public String getLocatorByClassNameAndText(String className, String testData, String elementType) {
		return "page.locator(\"//"+ elementType +"[@class='" + className + "' and text()='"+testData+"']\")";
	}
	
	@Override
	public String getLocatorByText(String testData) {
		return "page.locator(\"//*[text()='"+testData+"']\")";
	}
	
	//multiple locators:-
	
	@Override
	public String textForPomClassMultipleLocators(JSONObject step, String name, String elementType) {
		StringBuilder repString = new StringBuilder();
	    repString.append("private ").append(name).append("Data() {\n");
	    repString.append("\t\treturn {\n");
	    repString.append(getElementDataForPomClassMultipleLocators(step, elementType));
	    repString.append("\n\t\t};\n");
	    repString.append("\t}");
	    return repString.toString();
	}
	
	@Override
	public void appendData(StringBuilder sb, String key, String value) {
	    if (value != null && !value.isEmpty()) {
	    	sb.append("\t\t\"").append(key).append("\": ").append("\"").append(value).append("\",\n");
	    }
	}
	
	@Override
	public String textForPomClassMultipleLocatorsElementMethod(String name, String elementType, JSONObject step) {
		String action = (String) step.getOrDefault("Action", "");
		String repString = "\n";
		
		if (elementType.equalsIgnoreCase("LABEL")) {
	        repString += "\t\tasync scrollTo" + name + "() {\n";
	        repString += "\t\t\tawait this.elementHelper.clickElement(this." + name + "Data(), '" + elementType + "');\n";
	        repString += "\t\t}\n";
	    } else if (elementType.equalsIgnoreCase("LINK") || elementType.equalsIgnoreCase("BUTTON")) {
	        repString += "\t\tasync click" + name + "() {\n";
	        repString += "\t\t\tawait this.elementHelper.clickElement(this." + name + "Data(), '" + elementType + "');\n";
	        repString += "\t\t}\n";
	    } else if (elementType.equalsIgnoreCase("INPUT") || elementType.equalsIgnoreCase("TEXTAREA")) {
	        if (action.equalsIgnoreCase("upload")) {
	            repString += "\t\tasync Upload" + name + "() {\n";
	            repString += "\t\t\treturn this." + name + "Data();\n";
	            repString += "\t\t}\n";
	        } else {
	            repString += "\t\tasync click" + name + "() {\n";
	            repString += "\t\t\tawait this.elementHelper.clickElement(this." + name + "Data(), '" + elementType + "');\n";
	            repString += "\t\t}\n\n";

	            repString += "\t\tasync Enter" + name + "(testData: string) {\n";
	            repString += "\t\t\tawait this.elementHelper.enterText(this." + name + "Data(), '" + elementType + "', testData);\n";
	            repString += "\t\t}\n";
	        }
	    } else {
	        repString += "\t\tasync click" + name + "() {\n";
	        repString += "\t\t\tawait this.elementHelper.clickElement(this." + name + "Data(), '" + elementType + "');\n";
	        repString += "\t\t}\n";
	    }


		return repString;
	}
	
	@Override
	public String getActionClassCodeForElement(String elementType, String action, String fieldname, JSONObject step, String testData) {
		
		String replaceStr = "\tconst "+fieldname+" = ";
		replaceStr += this.findElementByPreferredLocator(step, elementType) + ";";
		if (action.equalsIgnoreCase("Enter") || action.equalsIgnoreCase("change") || action.equalsIgnoreCase("upload")) {
			replaceStr += "\n\tawait "+fieldname+".fill(\""+testData+"\");";
		} else {
			replaceStr += "\n\tawait "+fieldname+".click();";
		}
		return replaceStr;
	}
	
}