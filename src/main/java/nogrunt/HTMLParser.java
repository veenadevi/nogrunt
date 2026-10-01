package nogrunt;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.RemoteWebDriver;

import java.net.URL;
import java.util.*;

public class HTMLParser {
	static WebDriver driver;
	
    public static void main(String[] args) {
        // Set the path to the ChromeDriver executable
        System.setProperty("webdriver.chrome.driver", "C:\\Nogrunt\\libs\\chromedriver.exe");

        // Initialize a new WebDriver instance
 //       WebDriver driver = new ChromeDriver();
        DesiredCapabilities capabilities = new DesiredCapabilities();
		capabilities.setBrowserName("chrome");
        try {
			driver = new RemoteWebDriver(new URL("http://localhost:4444/wd/hub"), capabilities);
		} catch (Exception e) {
			e.printStackTrace();
		}
        
        driver.manage().window().maximize();
        // Define the starting URL
        String startUrl = "http://riskcentralops.gieom.com/Account/Login";

        // Recursively extract elements from linked pages
        List<WebElement> visitedLinks = new ArrayList<>();
        List<String> visitedUrl = new ArrayList<>();
        visitedUrl.add("http://riskcentralops.gieom.com/Account/LogOff");
        extractElements(driver, startUrl, visitedLinks,visitedUrl, new HashMap());

        // Close the WebDriver instance
        driver.quit();
    }

    private static void extractElements(WebDriver driver, String url, 
    		List<WebElement> visitedLinks, List<String> visitedUrl, HashMap elementsMap) {
        
    	if(url != null && !url.equals("SUBMIT")) {
	    	if(visitedUrl.contains(url) ) {
	    		return;
	    	} else {
	    		visitedUrl.add(url);
	    	}
    	}
    	
    	// Navigate to the target URL
        driver.get(url);

        // Find all clickable elements using Selenium WebDriver
        List<WebElement> clickableElements = driver.findElements(By.cssSelector("a, button, input[type=submit]"));

        // Find all input elements that accept user input using Selenium WebDriver
        List<WebElement> inputElements = driver.findElements(By.cssSelector("input[type=text], input[type=password], input[type=email], input[type=number], input[type=date], input[type=checkbox], select"));

        // Print the extracted elements
        System.out.println("Clickable elements on page " + driver.getCurrentUrl() + ":");
        HashMap<WebElement,HashMap> clickableAttributesMap = new HashMap<>();
        for (WebElement element : clickableElements) {       	   
        	// Use JavascriptExecutor to execute a JavaScript function that returns all attributes and their values for the element
            clickableAttributesMap.put(element, getAttrs(element));
        }

        System.out.println("Input elements on page " + driver.getCurrentUrl() + ":");
        HashMap<WebElement,HashMap> inputAttributesMap = new HashMap<>();
        for (WebElement element : inputElements) {
        	inputAttributesMap.put(element, getAttrs(element));
        }
        
        HashMap eleMap = new HashMap();
        eleMap.put("clickable", clickableAttributesMap);
        eleMap.put("input", inputAttributesMap);
        elementsMap.put(url, eleMap);

        // Fill out input fields with random data
        Random random = new Random();
        for (WebElement input : inputElements) {
            String type = input.getAttribute("type");
            try {
	            if (type.equals("text") || type.equals("password")) {
	            	if(input.getAttribute("placeholder").equals("Username")) {
	            		input.sendKeys("Derek");
	            	} else if(input.getAttribute("placeholder").equals("Password")) {
	            		input.sendKeys("Password@123");
	            	} else {
	            		input.sendKeys(Utilities.randomGen("CHAR",8,false));
	            	}
	            } else if (type.equals("email")) {
	                input.sendKeys("test@example.com");
	            } else if (type.equals("number")) {
	                input.sendKeys(String.valueOf(random.nextInt(100)));
	            } else if (type.equals("date")) {
	                input.sendKeys("2022-01-01");
	            } else if (type.equals("checkbox")) {	
	            		input.click();	            	
	            } else if (input.getTagName().equals("select")) {
	                input.findElement(By.xpath(".//option[1]")).click();
	            }
            } catch (Exception e) {
        		//e.printStackTrace();
        	}
        }

        Iterator<WebElement> iter = clickableAttributesMap.keySet().iterator();
        while(iter.hasNext()) {
        	WebElement we = iter.next();
        	HashMap<String, HashMap<String, String>> tagMap = clickableAttributesMap.get(we);
        	Iterator<String> xpathIter = tagMap.keySet().iterator();

			WebElement element = null;
			String xpath = xpathIter.next();
			try {
				element = driver.findElement(By.xpath(xpath));
			} catch (Exception e) {
				e.printStackTrace();
				throw e;
			}
			
			HashMap<String, String> attrMap = tagMap.get(xpath);
			String tagName = attrMap.get("TAGNAME");
			String type = attrMap.get("type");			

            if (!visitedLinks.contains(we)) {
                visitedLinks.add(we);
                if(tagName.equals("input") && type.equals("submit")) {
                	try {
    				element.click();
                	} catch (Exception e) {
                		((JavascriptExecutor) driver).executeScript("arguments[0].click();", we);
                	}
    				extractElements(driver, "SUBMIT", visitedLinks,visitedUrl,elementsMap);
    			}
            }
        }

        // Recursively extract elements from pages resulting from button clicks
        List<WebElement> newPages = driver.findElements(By.cssSelector("button, input[type=submit]"));
        for (WebElement page : newPages) {
        	try {
        		page.click();
        	} catch (Exception e) {
				try {
					((JavascriptExecutor) driver).executeScript("arguments[0].click();", page);
					
				} catch(Exception e22) {
						throw e22;
				
				}
            extractElements(driver, driver.getCurrentUrl(), visitedLinks,visitedUrl,elementsMap);
        }
    }
}
    
    public static HashMap getAttrs(WebElement element) {
    	HashMap<String,String> attributesMap = new HashMap<>();
    	JavascriptExecutor js = (JavascriptExecutor) driver;
        java.util.Map<String, String> attributes = (java.util.Map<String, String>) js.executeScript(
                "var items = {}; " +
                "for (index = 0; index < arguments[0].attributes.length; ++index) { " +
                "    items[arguments[0].attributes[index].name] = arguments[0].attributes[index].value " +
                "}; " +
                "return items;", element);
        String tagName = element.getTagName();
        attributesMap.put("TAGNAME", tagName);
        
        String xpath = "//" + tagName + "[";
        // Print out the attribute names and values for the element
        for (java.util.Map.Entry<String, String> entry : attributes.entrySet()) {
           // System.out.println(entry.getKey() + " : " + entry.getValue());
            attributesMap.put(entry.getKey(), entry.getValue());
            xpath += "@" + entry.getKey() + "='" + entry.getValue() + "' and ";
        }
        xpath = xpath.substring(0, xpath.length() - 5) + "]";  
        
        HashMap<String, HashMap<String,String>> xpathMap = new HashMap<>();
        xpathMap.put(xpath, attributesMap);
        return xpathMap;
    }
}





