//package

import java.io.File;
import java.net.URL;
import java.util.List;

import org.apache.commons.io.FileUtils;

import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.*;



import org.openqa.selenium.chrome.*;
import org.openqa.selenium.interactions.*;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.RemoteWebDriver;

public class JavaTemplate {
	
	WebDriver driver = null;
	int waitime = 1000;
	String screenshotDir = "C:\\screenshots\\";
	boolean takeSS = true;
	String lookingFor = "";
	long lookingForIndex = -1;
	
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
        	//Latest Step
        	//Add from Here
        }catch (Exception e) {
        	e.printStackTrace();
        } finally {
            // Close the browser
            driver.quit();
        }
    }
}