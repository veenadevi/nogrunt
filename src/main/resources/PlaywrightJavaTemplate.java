package nogrunt.codegen;

import com.microsoft.playwright.*;

import java.nio.file.Paths;

import nogrunt.pages.*;

public class PlaywrightJavaTemplate {

    Playwright playwright;
    Browser browser;
    Page page;
    int waitime = 1000;

    public void runLogic() {
    	
        playwright = Playwright.create();
        browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(false));
        page = browser.newPage();
        
        
        //pageconstants
        
    	try {
    		//Latest Step
    	    //Add from Here
    	} catch (Exception e) {
            e.printStackTrace();
        } finally {
            // Close the browser
            browser.close();
            playwright.close();
        }
    }
}