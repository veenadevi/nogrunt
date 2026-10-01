package nogrunt;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.openqa.selenium.WebDriver;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.nio.file.*;
import org.openqa.selenium.JavascriptExecutor;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

public class PageInfo {
	
	public static void savePageSource(WebDriver driver, String fileName) {
		
		// Get the page source
        String pageSource = driver.getPageSource();
        
     // Remove scripts and styles using JavaScript
//        ((JavascriptExecutor) driver).executeScript("var elements = document.querySelectorAll('script, style');" +
//                "for (var i = 0; i < elements.length; i++) { elements[i].remove(); }");

        // Get the raw text content without scripts, styles, and other elements
       // String rawTextContent = (String) ((JavascriptExecutor) driver).executeScript("return document.body.innerText");

//        String rawTextWithElementType = (String) ((JavascriptExecutor) driver).executeScript("var elements = document.body.querySelectorAll('*');" +
//                "var result = '';" +
//                "for (var i = 0; i < elements.length; i++) {" +
//                "  var element = elements[i];" +
//                "  var tagName = element.tagName.toLowerCase();" +
//                "  var textContent = element.innerText.trim();" +
//                "  if (textContent.length > 0) {" +
//                "    result += tagName + ': ' + textContent + '\\n';" +
//                "  }" +
//                "}" +
//                "return result;");
        
	    ExecutorService executor = Executors.newSingleThreadExecutor();

	    // Submit a new task to the executor
	    executor.submit(() -> {	        

	        // Write the page source to a file
	        try {
	            Files.writeString(Path.of(fileName), pageSource, StandardOpenOption.CREATE);
	        } catch (IOException e) {
	            e.printStackTrace();
	        }
	    });

	    // Shutdown the executor to release resources
	    executor.shutdown();
	}
	
	

	public static void saveElementInfoToFile(WebDriver driver, String fileName) {
		 try (BufferedWriter writer = new BufferedWriter(new FileWriter(fileName))) {
		        Set<String> ancestorTexts = new HashSet<>();

		        for (WebElement element : driver.findElements(By.cssSelector("*"))) {
		            if (element.findElements(By.cssSelector("*")).isEmpty()) {
		                String tagName = element.getTagName();
		                String textContent = element.getText().trim();

		                if (!textContent.isEmpty() && !ancestorTexts.contains(textContent)) {
		                    writer.write(tagName + ": " + textContent);
		                    writer.newLine();
		                    ancestorTexts.add(textContent);
		                }
		            }
		        }
		    } catch (IOException e) {
		        e.printStackTrace();
		    }
	}

}
