package nogrunt.scripts;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.io.IOException;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;

public class JSHandler {
	
	public String readJavaScriptFromFile(String filePath) {
	    try {
	        return new String(Files.readAllBytes(Paths.get(filePath)));
	    } catch (IOException e) {
	        e.printStackTrace();
	        return null; // or handle the exception as per your requirement
	    }
	}
	
	public static Object executeJavaScriptFunction(WebDriver driver, String functionScript, Object... args) {
	    JavascriptExecutor jsExecutor = (JavascriptExecutor) driver;
	    String script = functionScript + " return " + functionScript.split("\\s")[1].split("\\(")[0] + ".apply(null, arguments);";
	    return jsExecutor.executeScript(script, args);
	}

}
