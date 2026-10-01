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

import nogrunt.pages.*;

public class JavaTemplate {
	
	WebDriver driver = null;
	int waitime = 1000;
	
	public void setDriver(WebDriver driver) {
		this.driver = driver;
	}
	
	public void setWaittime(int wt) {
		waitime = wt;
	}
	
	
	public void runLogin() {

        Actions actions = new Actions(driver);
        
        //pageconstants

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