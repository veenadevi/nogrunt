package nogrunt;


import java.io.*;
import java.util.*;
import org.json.simple.*; 
import org.json.simple.parser.*;  
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Workbook;
import  org.apache.poi.hssf.usermodel.HSSFSheet;  
import  org.apache.poi.hssf.usermodel.HSSFWorkbook;  
import  org.apache.poi.hssf.usermodel.HSSFRow; 


public class GenerateTestCase {
	
	static OutputStream fileOut = null;
	static HSSFWorkbook  workbook = null;
	static short rowNumber = 0;
	
   public static void main(String[] args) {
 
      JSONParser parser = new JSONParser();
      try {    	
    	  
    	  HSSFSheet sheet = createxls();
   
         Object obj = parser.parse(new FileReader("C:/Users/Deva/eclipse-workspace/KeyLogging/src/keylogger/timer test.json"));
         JSONObject root = (JSONObject)obj;

         JSONArray steps = (JSONArray)root.get("steps");
         Iterator iterator = steps.iterator();
         while (iterator.hasNext()) {
        	JSONObject step = (JSONObject)iterator.next();
        	String stepType = (String)step.get("type");
        	if(stepType.equals("navigate")) {
        		sheet = createNavigateRow(sheet, step);
        	} else if(stepType.equals("click")) {
        		sheet = createClickRow(sheet, step);
        	}
            System.out.println(stepType);
         }
      } catch(Exception e) {
         e.printStackTrace();
      }
      
      closexls();
   }
   
   private static HSSFSheet createClickRow(HSSFSheet sheet, JSONObject step) {
	   HSSFRow rowhead = sheet.createRow(rowNumber);
	   
	   String sel1 = "";
	   String sel2 = "";
	   
	   JSONArray selectors = (JSONArray)step.get("selectors");
	    
	   JSONArray selected = (JSONArray)selectors.get(0);
	   sel1 = (String)selected.get(0);
	   if(selectors.size() > 1) {
		   JSONArray selected2 = (JSONArray)selectors.get(1);
		   sel2 = (String)selected2.get(0);
	   }
	   
	   rowhead = createRowValues(rowhead, "","",sel1,"Link",
 			  "Click",sel2,(String)step.get("url"),"","","","","",
 			 "","","");
	   
	   rowNumber = ++rowNumber;
	   return sheet;
   }
   
   private static HSSFSheet createNavigateRow(HSSFSheet sheet, JSONObject step) {
	   HSSFRow rowhead = sheet.createRow(rowNumber);
	   rowhead = createRowValues(rowhead, "","","","URL",
 			  "Get","",(String)step.get("url"),"","","","","",
 			 "","","");
	   
	   rowNumber = ++rowNumber;
	   return sheet;
   }
   
   private static HSSFSheet createxls() {
	   
	   try {
		   
		   fileOut = new FileOutputStream("C:/Users/Deva/eclipse-workspace/KeyLogging/src/keylogger/timer test.xlsx");   
		   
		 //creating an instance of Workbook class   
	    	  workbook = new HSSFWorkbook(); 
	    	  HSSFSheet sheet = workbook.createSheet("TestScript"); 
//	    	  workbook.getSheet("TestScript");
	    	  
	    	  HSSFRow rowhead = sheet.createRow(rowNumber);  
	    	  rowhead = createRowValues(rowhead, "Page Name","Description of Action performed","Object Property","Keyword",
	    			  "Action","Flow","TestData","Data Validation","Result","TestData","Data Validation","Result",
	    			  "TestData","Data Validation","Result");
	    	  rowNumber = ++rowNumber;
    	
	    	workbook.write(fileOut); 
	    	
	    	return sheet;
	    	  //creates an excel file at the specified location  
//	    	  OutputStream fileOut = new FileOutputStream("C:/Users/Deva/eclipse-workspace/KeyLogging/src/keylogger/timer test.xlsx");   
//	    	  System.out.println("Excel File has been created successfully.");   
//	    	  wb.write(fileOut);
	   } catch(Exception e) {
	         e.printStackTrace();
	   }
	   
	   return null;	   
   }
   
   private static HSSFRow createRowValues(HSSFRow rowhead, String val1,String val2,String val3,String val4,String val5,
		   String val6,String val7,String val8,String val9,String val10,String val11,String val12,String val13,
		   String val14, String val15) {
	   	rowhead.createCell(0).setCellValue(val1);  
	   	rowhead.createCell(1).setCellValue(val2);  
	   	rowhead.createCell(2).setCellValue(val3);  
	   	rowhead.createCell(3).setCellValue(val4);  
	   	rowhead.createCell(4).setCellValue(val5); 
	   	rowhead.createCell(0).setCellValue(val6);  
	   	rowhead.createCell(1).setCellValue(val7);  
	   	rowhead.createCell(2).setCellValue(val8);  
	   	rowhead.createCell(3).setCellValue(val9);  
	   	rowhead.createCell(3).setCellValue(val10);  
	   	rowhead.createCell(4).setCellValue(val11); 
	   	rowhead.createCell(0).setCellValue(val12);  
	   	rowhead.createCell(1).setCellValue(val13);  
	   	rowhead.createCell(2).setCellValue(val14);  
	   	rowhead.createCell(3).setCellValue(val15); 
	   	
	   	return rowhead;
   }
   
   private static void closexls() {
	   try {
		   workbook.write(fileOut);
	   fileOut.close();  
	 //closing the workbook  
	 workbook.close(); 
	   } catch (Exception e) {
		   e.printStackTrace();
	   }
   }
   
   
}
