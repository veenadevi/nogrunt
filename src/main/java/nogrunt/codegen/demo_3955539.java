package nogrunt.codegen;

import java.io.File;
import java.net.URL;

import org.apache.commons.io.FileUtils;
import org.json.simple.JSONObject;
import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.*;

import nogrunt.Utilities;

import org.openqa.selenium.chrome.*;
import org.openqa.selenium.interactions.*;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.RemoteWebDriver;

public class demo_3955539 {
	
	WebDriver driver = null;
	int waitime = 1000;
	String screenshotDir = "C:\\screenshots\\";
	boolean takeSS = true;
	String lookingFor = "";
	long lookingForIndex = -1;
	
	public static void main(String[] args) {
		demo_3955539 jt = new demo_3955539();
		System.setProperty("webdriver.chrome.driver", "C:\\Nogrunt\\libs\\chromedriver.exe");
		
		DesiredCapabilities capabilities = new DesiredCapabilities();
		capabilities.setBrowserName("chrome");
		
		try {
			jt.driver = new RemoteWebDriver(new URL("http://localhost:4444/wd/hub"), capabilities);
		} catch (Exception e) {
			jt.driver.quit();
			e.printStackTrace();
		}
		
		jt.runLogin();
	}
	
	public void setDriver(WebDriver drvr) {
		driver = drvr;
	}
	
	public void setWaittime(int wt) {
		waitime = wt;
	}
	
	public void setTakeSS(boolean t) {
		takeSS = t;
	}
	
	public void setScreenshotDir(String ssd) {
		screenshotDir = ssd;
	}
	
	public void takeSS(String ssfilename) {
		
		if(!takeSS) {
			return;
		}
		
		File screenshot = null;
		try {
			screenshot = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
		} catch(Exception e) {
			
		}
		String fsl = screenshotDir + ssfilename;
		try {
			FileUtils.copyFile(screenshot, new File(fsl));
		}catch (Exception e) {
			e.printStackTrace();
		}
		return;
	}
	
	private WebElement getShadowElement(String xpath, String lookingFor, long lookingForIndex) {
		WebElement we = driver.findElement(By.xpath(xpath));
		String script = "return arguments[0].shadowRoot.querySelectorAll('*');";
        JavascriptExecutor jsExecutor = (JavascriptExecutor) driver;
        Object result = jsExecutor.executeScript(script, we);

        if (result instanceof java.util.List<?>) {
            java.util.List<?> descendants = (java.util.List<?>) result;
            
            int howManyFound = 0;
//            String lookingFor = (String)step.get("shadowelement");
//            int lookingForIndex = (int)step.get("shadowindex");
            boolean shadowFound = false;
            for (Object descendant : descendants) {
                if (descendant instanceof WebElement) {
                    WebElement shadowDomElement = (WebElement) descendant;
                    // Process each descendant element as needed
                    String elementType = shadowDomElement.getTagName();
                    if(elementType.equalsIgnoreCase(lookingFor)) {
                    	if(lookingForIndex == howManyFound + 1) {
                    		we = shadowDomElement;
                    		shadowFound = true;
                    		break;
                    	} else {
                    		howManyFound = howManyFound + 1;
                    		if(howManyFound >= lookingForIndex) {
                    			shadowFound = false;
	                    		break;
                    		}
                    	}
                    }
                }
            }
        }
        
        return we;
	}
	
	private WebElement findElement(String xpath, String searchString, String altpath) {
		WebElement we = null;
		String xpath1 = null;
		String xpath2 = null;
		try {
			if(xpath != null && !xpath.equals("") && xpath.contains("|")) {
				xpath1 = xpath.substring(0, xpath.indexOf("|") - 1);
				xpath2 = xpath.substring(xpath.indexOf("|") + 2, xpath.length());
			} else {
				throw new Exception("Xpath1 not valid");
			}
			we = driver.findElement(By.xpath(xpath1));
			return we;
		} catch (Exception expath1) {
			try {
				if(xpath2 == null) {
					throw new Exception("Xpath2 not valid");
				}
				we = driver.findElement(By.xpath(xpath2));
				return we;
			} catch (Exception expath2) {
				try {															

					if(searchString != null && !searchString.equals("")) {
						String xsearch = "//*[text()='" + searchString + "']";
						we = driver.findElement(By.xpath(xsearch));
						return we;
					} else {
						throw new Exception("No search path");
					}
				} catch (Exception ealtpath) {
					try {
						if(altpath != null && !altpath.equals("")) {
							String xalt = "//*[text()='" + altpath + "']";
							we = driver.findElement(By.xpath(xalt));
							return we;
						}
					} catch (Exception altex) {
						return null;
					}
				}
			}
		}									
		return null;
	}
	
	public void runLogin() {

        Actions actions = new Actions(driver);

        try {
        		//Step Number - 1
        	
				//Step Number - 2
			Thread.sleep(waitime);
			driver.get("https://pmsdevuser.gapblue.com:7050/#/");
			
				//Step Number - 3
			Thread.sleep(waitime);
			WebElement Email2 = findElement("//*[@id=\"input-group-1\"] | HTML/BODY/APP-ROOT/DIV/APP-LOGIN/DIV/DIV[3]/FORM/DIV/INPUT","","");
			Email2.click();
			takeSS("Email2.jpg");
			
				//Step Number - 4
			Thread.sleep(waitime);
			WebElement Email3 = findElement("//*[@id=\"input-group-1\"] | HTML/BODY/APP-ROOT/DIV/APP-LOGIN/DIV/DIV[3]/FORM/DIV/INPUT","d","");
															Email3.sendKeys("deva@nogrunt.com");
			takeSS("Email3.jpg");
			
				//Step Number - 5
			Thread.sleep(waitime);
			WebElement Password4 = findElement("//*[@id=\"input-group-2\"] | HTML/BODY/APP-ROOT/DIV/APP-LOGIN/DIV/DIV[3]/FORM/DIV[2]/INPUT","","");
			Password4.click();
			takeSS("Password4.jpg");
			
				//Step Number - 6
			Thread.sleep(waitime);
			WebElement Password5 = findElement("//*[@id=\"input-group-2\"] | HTML/BODY/APP-ROOT/DIV/APP-LOGIN/DIV/DIV[3]/FORM/DIV[2]/INPUT","Ma*e4*#XXPzT%","");
Password5.sendKeys("Ma*e4*#XXPzT%");
			takeSS("Password5.jpg");
			
				//Step Number - 7
			Thread.sleep(waitime);
			WebElement LOGIN6 = findElement("HTML/BODY/APP-ROOT/DIV/APP-LOGIN/DIV/DIV[3]/FORM/DIV[3]/BUTTON | HTML/BODY/APP-ROOT/DIV/APP-LOGIN/DIV/DIV[3]/FORM/DIV[3]/BUTTON","","");
			LOGIN6.click();
			takeSS("LOGIN6.jpg");
			
				//Step Number - 8
			Thread.sleep(waitime);
			WebElement Projects7 = findElement("//A[@href=\"#/pms\"] /*[name() = \"svg\" and @class=\"bi bi-kanban\"] | HTML/BODY/APP-ROOT/DIV/DIV/DIV[3]/APP-NAVBAR/DIV/A[2]/*[name() = \"svg\"]","","");
			Projects7.click();
			takeSS("Projects7.jpg");
			
				//Step Number - 9
			Thread.sleep(waitime);
			WebElement xZnBcmpD8 = findElement("(//*[@id=\"tbody-1\"]/SPAN/TR/TD[2]/DIV/DIV[2])[1] | HTML/BODY/APP-ROOT/DIV/DIV/DIV[3]/DIV/APP-PROJECT-MANAGEMENT/MAIN/MAT-TAB-GROUP/DIV/MAT-TAB-BODY[2]/DIV/APP-PROJECTS/TABLE/TBODY/SPAN/TR/TD[2]/DIV/DIV[2]","xZnBcmpD","//DIV[2 and text()='xZnBcmpD']");
			xZnBcmpD8.click();
			takeSS("xZnBcmpD8.jpg");
			
				//Step Number - 10
			Thread.sleep(waitime);
			WebElement Download9 = findElement("HTML/BODY/APP-ROOT/DIV/DIV/DIV[3]/DIV/APP-PROJECT-DETAILS/DIV/DIV/DIV/DIV[2]/DIV/DIV/DIV/DIV/DIV/SPAN/BUTTON/*[name() = \"svg\" and @class=\"bi bi-plus-square-fill text-theme-color\"] | HTML/BODY/APP-ROOT/DIV/DIV/DIV[3]/DIV/APP-PROJECT-DETAILS/DIV/DIV/DIV/DIV[2]/DIV/DIV/DIV/DIV/DIV/SPAN/BUTTON/*[name() = \"svg\"]","","");
			Download9.click();
			takeSS("Download9.jpg");
			
				//Step Number - 11
			Thread.sleep(waitime);
			WebElement TaskPriori10 = findElement("//*[@id=\"actionsModal\"]/DIV/DIV/DIV[3]/DIV/DIV/FORM/DIV/INPUT | HTML/BODY/APP-ROOT/DIV/DIV/DIV[3]/DIV/APP-PROJECT-DETAILS/DIV[2]/DIV/DIV/DIV[3]/DIV/DIV/FORM/DIV/INPUT","","//*[text()= 'Task name']");
			TaskPriori10.click();
			takeSS("TaskPriori10.jpg");
			
				//Step Number - 12
			Thread.sleep(waitime);
			WebElement TaskPriori11 = findElement("//*[@id=\"actionsModal\"]/DIV/DIV/DIV[3]/DIV/DIV/FORM/DIV/INPUT | HTML/BODY/APP-ROOT/DIV/DIV/DIV[3]/DIV/APP-PROJECT-DETAILS/DIV[2]/DIV/DIV/DIV[3]/DIV/DIV/FORM/DIV/INPUT","t","//*[text()= 'Task name']");
			TaskPriori11.sendKeys("tsk1");
			takeSS("TaskPriori11.jpg");
			
				//Step Number - 13
			Thread.sleep(waitime);
			WebElement TaskPriori12 = findElement("((//*[@id=\"message\"])[2])[7] | HTML/BODY/APP-ROOT/DIV/DIV/DIV[3]/DIV/APP-PROJECT-DETAILS/DIV[2]/DIV/DIV/DIV[3]/DIV/DIV/FORM/DIV[2]/TEXTAREA","","");
				TaskPriori12.sendKeys("dsc1");
			takeSS("TaskPriori12.jpg");
			
				//Step Number - 14
			Thread.sleep(waitime);
			WebElement ProposedSt13 = findElement("//*[@id=\"actionsModal\"]/DIV/DIV/DIV[3]/DIV/DIV/FORM/DIV[6]/DIV/DIV[4]/INPUT | HTML/BODY/APP-ROOT/DIV/DIV/DIV[3]/DIV/APP-PROJECT-DETAILS/DIV[2]/DIV/DIV/DIV[3]/DIV/DIV/FORM/DIV[6]/DIV/DIV[4]/INPUT","","");
			ProposedSt13.click();
			takeSS("ProposedSt13.jpg");
			
				//Step Number - 15
			Thread.sleep(waitime);
			WebElement TaskPriori14 = findElement("((//*[@id=\"message\"])[2])[7] | HTML/BODY/APP-ROOT/DIV/DIV/DIV[3]/DIV/APP-PROJECT-DETAILS/DIV[2]/DIV/DIV/DIV[3]/DIV/DIV/FORM/DIV[2]/TEXTAREA","dsc1","");
TaskPriori14.sendKeys("dsc1");
			takeSS("TaskPriori14.jpg");
			
				//Step Number - 16
			Thread.sleep(waitime);
			WebElement ProposedSt15 = findElement("//*[@id=\"actionsModal\"]/DIV/DIV/DIV[3]/DIV/DIV/FORM/DIV[6]/DIV/DIV[4]/INPUT | HTML/BODY/APP-ROOT/DIV/DIV/DIV[3]/DIV/APP-PROJECT-DETAILS/DIV[2]/DIV/DIV/DIV[3]/DIV/DIV/FORM/DIV[6]/DIV/DIV[4]/INPUT","2024-02-10","");
			ProposedSt15.clear();
			JavascriptExecutor js = (JavascriptExecutor)driver;
			js.executeScript("arguments[0].value='" + "2024-02-10" + "';", ProposedSt15);
			takeSS("ProposedSt15.jpg");
			
				//Step Number - 17
			Thread.sleep(waitime);
			WebElement ProposedSt16 = findElement("//*[@id=\"actionsModal\"]/DIV/DIV/DIV[3]/DIV/DIV/FORM/DIV[6]/DIV/DIV[4]/INPUT | HTML/BODY/APP-ROOT/DIV/DIV/DIV[3]/DIV/APP-PROJECT-DETAILS/DIV[2]/DIV/DIV/DIV[3]/DIV/DIV/FORM/DIV[6]/DIV/DIV[4]/INPUT","2024-02-10","");
			ProposedSt16.click();
			takeSS("ProposedSt16.jpg");
			
				//Step Number - 18
			Thread.sleep(waitime);
			WebElement EffortInda17 = findElement("(//*[@id=\"estimatedEffort\"])[1] | HTML/BODY/APP-ROOT/DIV/DIV/DIV[3]/DIV/APP-PROJECT-DETAILS/DIV[2]/DIV/DIV/DIV[3]/DIV/DIV/FORM/DIV[6]/DIV/DIV[3]/INPUT","","");
			EffortInda17.click();
			takeSS("EffortInda17.jpg");
			
				//Step Number - 19
			Thread.sleep(waitime);
			WebElement EffortInda18 = findElement("(//*[@id=\"estimatedEffort\"])[1] | HTML/BODY/APP-ROOT/DIV/DIV/DIV[3]/DIV/APP-PROJECT-DETAILS/DIV[2]/DIV/DIV/DIV[3]/DIV/DIV/FORM/DIV[6]/DIV/DIV[3]/INPUT","","");
				EffortInda18.sendKeys("3.5");
			takeSS("EffortInda18.jpg");
			
				//Step Number - 20
			Thread.sleep(waitime);
			WebElement EffortInda19 = findElement("(//*[@id=\"estimatedEffort\"])[1] | HTML/BODY/APP-ROOT/DIV/DIV/DIV[3]/DIV/APP-PROJECT-DETAILS/DIV[2]/DIV/DIV/DIV[3]/DIV/DIV/FORM/DIV[6]/DIV/DIV[3]/INPUT","3.5","");
			EffortInda19.click();
			takeSS("EffortInda19.jpg");
			
				//Step Number - 21
			Thread.sleep(waitime);
			WebElement EffortInda20 = findElement("((//*[@id=\"disabledSelect\"])[2])[5] | HTML/BODY/APP-ROOT/DIV/DIV/DIV[3]/DIV/APP-PROJECT-DETAILS/DIV[2]/DIV/DIV/DIV[3]/DIV/DIV/FORM/DIV[6]/DIV/DIV/SELECT","0~8: null~-Assign Employee-","");
			EffortInda20.click();
			takeSS("EffortInda20.jpg");
			
				//Step Number - 22
			Thread.sleep(waitime);
			WebElement EffortInda21 = findElement("((//*[@id=\"disabledSelect\"])[2])[5] | HTML/BODY/APP-ROOT/DIV/DIV/DIV[3]/DIV/APP-PROJECT-DETAILS/DIV[2]/DIV/DIV/DIV[3]/DIV/DIV/FORM/DIV[6]/DIV/DIV/SELECT","2~10: 1068~Deva Gowda H S","");
			Select select21 = new Select(EffortInda21);
			select21.selectByVisibleText("Deva Gowda H S");
			//select21.selectByValue(10: 1068);
			//int index = Integer.valueOf(2);;
			//select21.selectByIndex(index);
			takeSS("EffortInda21.jpg");
			
				//Step Number - 23
			Thread.sleep(waitime);
			takeSS("EffortInda22.jpg");
			
				//Step Number - 24
			Thread.sleep(waitime);
			WebElement Task23 = findElement("//*[@id=\"closeAddActionsModal\"]/*[name() = \"svg\" and @class=\"w-5 h-5\"] | HTML/BODY/APP-ROOT/DIV/DIV/DIV[3]/DIV/APP-PROJECT-DETAILS/DIV[2]/DIV/DIV/DIV/DIV[2]/BUTTON[2]/*[name() = \"svg\"]","","");
			Task23.click();
			takeSS("Task23.jpg");
			
				//Step Number - 25
			Thread.sleep(waitime);
			WebElement DevaGowdaH24 = findElement("//*[@id=\"dropdownLoggedUserButton\"]/DIV/SPAN | HTML/BODY/APP-ROOT/DIV/DIV/DIV/DIV[2]/BUTTON/DIV/SPAN","Deva Gowda H S","//SPAN[1 and text()='Deva Gowda H S']");
			DevaGowdaH24.click();
			takeSS("DevaGowdaH24.jpg");
			
				//Step Number - 26
			Thread.sleep(waitime);
			WebElement LogOut25 = findElement("//*[@id=\"dropdown-logged-user\"]/DIV[3]/A | HTML/BODY/APP-ROOT/DIV/DIV/DIV[2]/DIV[3]/A"," Log Out","//A[1 and text()=' Log Out']");
			LogOut25.click();
			takeSS("LogOut25.jpg");
			
			//Latest Step
			Thread.sleep(waitime);
			WebElement Areyousure26 = findElement("(//*[@id=\"popup-modal\"]/DIV/DIV/DIV/BUTTON)[1] | HTML/BODY/APP-ROOT/DIV/DIV[5]/DIV/DIV/DIV/BUTTON","","");
			Areyousure26.click();
			takeSS("Areyousure26.jpg");
			//Add from Here
        }catch (Exception e) {
        	e.printStackTrace();
        } finally {
            // Close the browser
            driver.quit();
        }
    }
}