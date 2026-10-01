package nogrunt.codegenNew;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import org.json.simple.JSONObject;

import nogrunt.Utilities;

public class SelCSharpCodeGen extends CodeGenHelper {
	
	final String latestStep = "\n\t\t\t\t//Latest Step";
	final String LastStep = "\n\t\t\t\t//Add from Here";
	final String fileExtension = ".cs";
	final String classSig = "SeleniumCSharpTemplate";
	final String templateFileName = "SeleniumCSharpTemplate.cs";
	
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
		String pdText = pagename + " " +objectName + ";";
    	pdText = pdText + "\n\t\t//page declarations";
        String piText = objectName + " = new " + pagename+"(driver);";
        piText = piText + "\n\t\t\t//page initializations";
        
        try {
        	Utilities.replaceContent(filePath, "\n\t\t//page declarations", pdText);
        	Utilities.replaceContent(filePath, "\n\t\t\t//page initializations", piText);
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
		return "\t\t\t\tdriver.Navigate().GoToUrl(\"" + url + "\");";
	}
	
	@Override
	public String getActionClassPomCodeForElement(String elementtype, String action, String fieldname, String objectName, String uniquename, String testData) {
		String replaceStr = "";
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
		
		return replaceStr;
	}
	
	@Override
	public String getPomTemplateFileName() {
		return "SelCSharpPomTemplate.cs";
	}
	
	@Override
	public void replacePomClassInitializations(Path filePath, String fname, boolean isMultipleLocatorPom) {
		try {
			if (isMultipleLocatorPom) {
				Utilities.replaceContent(filePath, "//declarations","private ElementHelper elementHelper;");
				Utilities.replaceContent(filePath, "//helpers", "this.elementHelper = new ElementHelper(driver);");
			}
			else {
				Utilities.replaceContent(filePath, "//declarations","private WebDriverWait wait;");
				Utilities.replaceContent(filePath, "//helpers", "this.wait = new WebDriverWait(driver, TimeSpan.FromSeconds(10));");
				Utilities.replaceContent(filePath, "//IWebElements", "private IWebElement WaitForElementToBeClickable(By locator)\r\n"
						+ "\t\t{\r\n"
						+ "\t\t\treturn wait.Until(ExpectedConditions.ElementToBeClickable(locator));\r\n"
						+ "\t\t}\n\n"
						+ "\t\t//IWebElements");
			}
			Utilities.replaceContent(filePath, "SelCSharpPomTemplate", fname);
			Utilities.replaceContent(filePath, "Constructor", fname);
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
	
	@Override
	public void addTextForPomClassElementLocator(Path filePath, String uniquename, String elementType, JSONObject step, boolean isMultipleLocatorPom) {
		
		try {
			String pathDeclaration;
			if (isMultipleLocatorPom) {
				pathDeclaration = textForPomClassMultipleLocators(step, uniquename, elementType);
			}
			else {
				pathDeclaration = textForPomClassNormalLocator(step, uniquename, elementType);
			}
			String fileContent = new String(Files.readAllBytes(filePath), StandardCharsets.UTF_8);
			fileContent = fileContent.replace("//IWebElements", pathDeclaration+"\n\t\t//IWebElements");
			Files.write(filePath, fileContent.getBytes(StandardCharsets.UTF_8), StandardOpenOption.WRITE);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	@Override
	public String textForPomClassNormalLocator(JSONObject step, String name, String elementType) {
		String repString = "private IWebElement "+name+" => WaitForElementToBeClickable(";
		repString += findElementByPreferredLocator(step, elementType);
		repString += ");";
		return repString;
	}
	
	@Override
	public String getLocatorById(String idValue) {
		return "By.Id(\"" + idValue + "\")";
	}
	
	@Override
	public String getLocatorByXpath(String xpath) {
		return "By.XPath(\"" + xpath + "\")";
	}
	
	@Override
	public String getLocatorByPlaceholder(String placeholder, String elementType) {
		return "By.XPath(\"//"+ elementType +"[@placeholder='" + placeholder + "']\")";
	}
	
	@Override
	public String getLocatorByClassNameAndIndex(String className, Integer classIndex) {
		return "By.XPath(\"(//*[@class='" + className + "'])["+classIndex+"]\")";
	}
	
	@Override
	public String getLocatorByClassNameAndText(String className, String testData, String elementType) {
		return "By.XPath(\"//"+ elementType +"[@class='" + className + "' and text()='"+testData+"']\")";
	}
	
	@Override
	public String getLocatorByText(String testData) {
		return "By.XPath(\"//*[text()='"+testData+"']\")";
	}
	
	@Override
	public String textForPomClassMultipleLocators(JSONObject step, String name, String elementType) {
		String repString = "private Dictionary<string, string> "+name+"Data() => new Dictionary<string, string>\r\n\t\t{\n";
		repString += getElementDataForPomClassMultipleLocators(step, elementType);
		repString += "\n\t\t};\n";
		return repString;
	}
	
	@Override
	public void appendData(StringBuilder sb, String key, String value) {
	    if (value != null && !value.isEmpty()) {
	    	sb.append("\t\t\t{ \"").append(key).append("\", \"").append(value).append("\" },\n");
	    }
	}
	
	@Override
	public String textForPomClassMultipleLocatorsElementMethod(String name, String elementType, JSONObject step) {
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
	
}