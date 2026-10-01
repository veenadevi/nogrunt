package nogrunt.codegenNew;

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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Collectors;
import java.nio.charset.StandardCharsets;
import java.nio.file.StandardCopyOption;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

public class CodeGenFull {
	
	MySQlConn msc = null;
	MySqlConn2 msc2 = null;
	
//	public static void main (String[] args) {
//		PlaywrightJava sj = new PlaywrightJava();
//		sj.genCode(null, -1, null, null, null);
//	}
	
	public void setMsc(MySQlConn m) {
		msc = m;
		msc2 = new MySqlConn2(msc);
	}
	
	public boolean genCode(JSONObject json, int companyId, String randomkey, 
			Utilities utilities, String tcname, HttpServletResponse response,
			String requestedBy, String genType) {
		CodeGen pwjava = new CodeGen();
		pwjava.setMsc(msc);
		CodeGenHelper codeGenHelper = CodeGenHelper.getClassByGenType(genType);
		pwjava.setCodeGenHelper(codeGenHelper);
		JSONObject names = new JSONObject();
		names.put("title", tcname);
		names.put("path", "nogrunt.codegen");
		names.put("method", "runLogin");
		boolean licenseIsValid = pwjava.genCode(names, companyId, randomkey, 
				utilities, requestedBy);
		if(!licenseIsValid) {
			return false;
		}
		
		JSONObject steps = (JSONObject)json.get("data"); 
		JSONObject urlstep = (JSONObject) steps.get(2);
		urlstep = fixURL(urlstep);
		licenseIsValid = pwjava.genCode(urlstep, companyId, randomkey, 
				utilities, requestedBy);
		
		if(!licenseIsValid) {
			return false;
		}
		
		int i =1;
		while (i < steps.size()) {
			JSONObject step = (JSONObject) steps.get(i + 2);
			step = fixStep(step);
			licenseIsValid = pwjava.genCode(step, companyId, randomkey, 
					utilities, requestedBy);
			if(!licenseIsValid) {
				return false;
			}
			i = i + 1;
		}
		String justFileName = codeGenHelper.getJustFileName(tcname, randomkey) + codeGenHelper.getFileExtension();
		String fullFilePath = Utilities.getCodeFilePath(companyId, justFileName);
        byte[] buffer = new byte[1024];
        int length;
        Path path = Paths.get(fullFilePath);
        String filename = path.getFileName().toString();
		
        try (InputStream fileInputStream = new FileInputStream(fullFilePath);
                OutputStream responseOutputStream = response.getOutputStream()) {
               response.setContentType("text/plain");
               response.setHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"");

               while ((length = fileInputStream.read(buffer)) != -1) {
                   responseOutputStream.write(buffer, 0, length);
               }
        }catch(Exception e) {
        	e.printStackTrace();
        } finally {
        	msc = null;
        	msc2 = null;
        }
        
        return true;
	}
	
	public JSONObject fixURL(JSONObject step) {
		step.put("action", "url");
		step.put("url", (String)step.get("TestData"));
		return step;
	}
	
	public JSONObject fixStep(JSONObject step) {
		step.put("action", (String)step.get("Action"));
		String keyword = (String)step.get("Keyword");
		step.put("testdata", (String)step.get("TestData"));
		step.put("click", (String)step.get("Object_Xpath"));
		
		JSONObject uninametoele = new JSONObject();
		uninametoele.put("text", (String)step.get("uniquetext"));
		step.put("uninametoele", uninametoele);
		
		return step;
	}
	
	
}
