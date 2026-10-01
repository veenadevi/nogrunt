package keylogger.codegen;

import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.*;
import org.openqa.selenium.chrome.*;
import org.openqa.selenium.interactions.*;

import keylogger.*;

public class JavaTemplate {
	WebDriver driver;
	WebElement dragStart = null;
	
	public void runLogin() {
        // Set the path of the ChromeDriver
		System.setProperty("webdriver.chrome.driver", AppProperties.webdriverchromedriver);

        // Initialize a ChromeDriver instance
        WebDriver driver = new ChromeDriver();
        Actions actions = new Actions(driver);

        try {
        	//Add from Here

        } finally {
            // Close the browser
            driver.quit();
        }
    }
	
	


}
