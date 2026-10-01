package nogrunt.codegen;

import org.json.simple.JSONObject;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.Select;

import jakarta.servlet.http.HttpServletResponse;
import nogrunt.*;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Collectors;
import java.nio.charset.StandardCharsets;
import java.nio.file.StandardCopyOption;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.HashSet;
import java.util.Set;

public class CodeGenFacade {
	
	public static boolean codeGen (MySQlConn msc, int tcid, int companyId, String randomkey,
			Utilities userCache, String tcname, HttpServletResponse response, JSONObject userDetails) {
		
		boolean isLicenseValid = false;
		String genType = msc.getGetTypeForCompany(companyId);
		JSONObject json = msc.getTestStepsByTestCaseID(tcid, true);	
		String requestedBy = (String)userDetails.get("uname");
		
		if(genType != null && genType.equals("pwts")) {
			PlaywrightTypeScriptFull pwtsf = new PlaywrightTypeScriptFull();
			pwtsf.setMsc(msc);
			isLicenseValid = pwtsf.genCode(json, companyId, randomkey, userCache, 
					tcname, response, requestedBy);
		} else if(genType != null && genType.equals("pwjava")) {
			PlaywrightJavaFull pwjf = new PlaywrightJavaFull();
			pwjf.setMsc(msc);
			isLicenseValid = pwjf.genCode(json, companyId, randomkey, userCache, 
					tcname, response,requestedBy);
		} else if (genType != null && genType.equals("selc#")) {
			SeleniumCSharpFull scsf = new SeleniumCSharpFull();
			scsf.setMsc(msc);
			isLicenseValid = scsf.genCode(json, companyId, randomkey, userCache, 
					tcname, response,requestedBy);
		} else {
						
			SelJavaFull sjf = new SelJavaFull();
			sjf.setMsc(msc);
			isLicenseValid = sjf.genCode(json, companyId, randomkey, userCache, 
					tcname, response,requestedBy);
		}
		return isLicenseValid;
	}
	
}
