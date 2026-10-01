package nogrunt;

import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

@SuppressWarnings("unused")
public class ExcelWriter {
	
	static String fileName = null;
	static XSSFWorkbook workbook = null;
	static int rowNumber = 0;

//  private static String[] columns = { "First Name", "Last Name", "Email",
//    "Date Of Birth" };

private static String[] columns = { "Page Name","Description of Action performed","Object Property","Keyword",
		  "Action","Flow","TestData","API -  Data","API - Expected Result","API  - Test Data ","API -  Data output",
		  "Data Validation","Result","TestData","Data Validation","Result",
		  "TestData","Data Validation","Result" };

  public static void main(String[] args) throws IOException, 
	InvalidFormatException {

	  JSONParser parser = new JSONParser();
	  try {
		  Sheet sheet = createxls();
    
		  Object obj = parser.parse(new FileReader("C:/Users/Deva/eclipse-workspace/KeyLogging/src/keylogger/all elements.json"));
		  JSONObject root = (JSONObject)obj;
		  fileName = (String)root.get("title");
		  fileName = "C:/Users/Deva/eclipse-workspace/KeyLogging/src/keylogger/Test Cases/" + fileName + ".xlsx";
		  JSONArray steps = (JSONArray)root.get("steps");
		  Iterator<?> iterator = steps.iterator();
		    while (iterator.hasNext()) {
			   	JSONObject step = (JSONObject)iterator.next();
			   	String stepType = (String)step.get("type");
			   	if(stepType.equals("navigate")) {
			   		sheet = createNavigateRow(sheet, step);
			   	} else if(stepType.equals("click")) {
			   		sheet = createClickRow(sheet, step);
			   	} else if(stepType.equals("change")) {
			   		sheet = createChangeRow(sheet, step);
			   	}
		       System.out.println(stepType);
		    }
	  } catch(Exception e) {
	    e.printStackTrace();
	  }
	  closexls();
  }
  
  private static Sheet createChangeRow(Sheet sheet, JSONObject step) {
	  String action = "Enter";
	  String keyword = "EditBox";
	  String value = (String)step.get("value");
	   
	   JSONArray selectors = (JSONArray)step.get("selectors");
	   
	   String xpath = "No xpath found";
	   
	   Iterator<?> selectorsIter = selectors.iterator();
	    while (selectorsIter.hasNext()) {
		   	JSONArray selected = (JSONArray)selectorsIter.next();
		   	
		   	Iterator<?> selectedIter = selected.iterator();
		   	boolean exit = false;
		   	while (selectedIter.hasNext()) {
		   		xpath = (String)selectedIter.next();
		   		
		   		if(action.equals("Click") && xpath.contains("role=\"button\"")) {
		   			action = "Button";
		   		}
		   		
		   		if(!xpath.startsWith("aria/")) {
		   			xpath = getFixedXPath(xpath);
		   			
		   			exit = true;
		   			break;
		   		}
		   	}
		   	
		   	if(exit) {
		   		break;
		   	}

	    }
	   
	   createRowValues(sheet, "","",xpath,keyword,
			   action,"",value,"","","","","",
			 "","","");
	  
	  return sheet;
  }
  
  private static String getFixedXPath(String xpath) {
	  
 			if(xpath.startsWith("#")) {
 				xpath = xpath.substring(1,xpath.length());
 				String id = "";
 				if(xpath.contains(">")) {
 					id = xpath.substring(0,xpath.indexOf(">"));
 					xpath = xpath.substring(xpath.indexOf(">"), xpath.length());
 				} else {
 					id = xpath;
 				}
 				
 				String formattedId = "//*[@id=\"" + id.trim() + "\"]";
 				if(id.equals(xpath)) {
 					xpath = formattedId;
 				} else {
 					xpath = formattedId + xpath;
 					
 				}
 			} else if(xpath.startsWith("[data-testid=")) {
 				String part1 = xpath.substring(0,xpath.indexOf("[data-testid="));
 				String part3 = xpath.substring(xpath.indexOf("[data-testid=") + 13,xpath.length());
 				String id = part3.substring(0,part3.indexOf("]"));
 				part3 = part3.substring(id.length(),part3.length());
 				id = id.trim();
 				xpath = "//*[@data-testid='" + id + "'" + part3;
 			}
 			
 			List<String> dotAsClass = Arrays.asList("div", "span", "button");
 			for(int i=0; i < dotAsClass.size(); i++) {
 				String qual = dotAsClass.get(i);
	 			while(xpath.contains(qual+ ".")) {
	 				String part1 = xpath.substring(0,xpath.indexOf(qual + "."));
	 				String part3 = xpath.substring(xpath.indexOf(qual + ".") + qual.length() + 1,xpath.length());
	 				String id = "";
	 				if(part3.contains(">")) {
	 					id = part3.substring(0,part3.indexOf(">"));	
	 					part3 = part3.substring(id.length(),part3.length());
	 				} else {
	 					id = part3;
	 					part3 = "";
	 				}
	 						   						
	 				id = id.trim();
	 				if(id.contains(".")) {
	 					id = id.replace(".", " ");
	 				}
	 				xpath = part1 + qual + "[@class='" + id + "']" + part3;
	 			}
 			}
 			
 			while(xpath.contains(":nth-child(")) {
 				String part1 = xpath.substring(0,xpath.indexOf(":nth-child("));
 				String part3 = xpath.substring(xpath.indexOf(":nth-child(") + 11,xpath.length());
 				String id = part3.substring(0,part3.indexOf(")"));
 				part3 = part3.substring(id.length()+1,part3.length());
 				id = id.trim();
 				xpath = part1 + "[" + id + "]" + part3;
 			}
 			
 			xpath = xpath.replace(">", "/");
	  
	  return xpath;
  }
  
  private static boolean isXPathDirectlyAvailable(Sheet sheet, JSONArray selectors, String keyword,
		  String action, String value) {
	  	
	  	String tempPath = "";
	  	String xpath = "";
	  	boolean xpathFound = false;
	  	
	  	Iterator<?> selectorsIter = selectors.iterator();
	    while (selectorsIter.hasNext()) {
		   	JSONArray selected = (JSONArray)selectorsIter.next();
		   	
		   	Iterator<?> selectedIter = selected.iterator();
		   	boolean exit = false;
		   	while (selectedIter.hasNext()) {
		   		tempPath = (String)selectedIter.next();
		   		
		   		if(tempPath.startsWith("xpath/")) {
		   			xpath = tempPath.substring(6,tempPath.length());
		   			xpathFound = true;
		   		} else if(tempPath.startsWith("text/")) {
//		   			value = tempPath.substring(5,tempPath.length());
		   		}
		   	}
	    }
	    
	    if(xpathFound) {
	    	createRowValues(sheet, "","",xpath,keyword,
	 			   action,"",value,"","","","","",
	 			 "","","");
	    }
	    
	    return xpathFound;
  }
  
  private static void xPathDirectlyNotAvailable(Sheet sheet, JSONArray selectors,
		  String xpath, String keyword, String action, String value) {
	  
	  Iterator<?> selectorsIter = selectors.iterator();
	    while (selectorsIter.hasNext()) {
		   	JSONArray selected = (JSONArray)selectorsIter.next();
		   	
		   	Iterator<?> selectedIter = selected.iterator();
		   	boolean exit = false;
		   	while (selectedIter.hasNext()) {
		   		xpath = (String)selectedIter.next();
		   		
		   		if(keyword.equals("Link") && xpath.contains("role=\"button\"")) {
		   			keyword = "Button";
		   		}
		   		
		   		if(!xpath.startsWith("aria/")) {
		   			xpath = getFixedXPath(xpath);
		   			exit = true;
		   			break;
		   		}
		   	}
		   	
		   	if(exit) {
		   		break;
		   	}

	    }
	    
	   createRowValues(sheet, "","",xpath,keyword,
			   action,"",value,"","","","","",
			 "","","");
  }
  
  private static Sheet createClickRow(Sheet sheet, JSONObject step) {
	   
	  String keyword = "Link";
	  String value = ""; 
	  String action = "Click";
	   
	   JSONArray asserts = (JSONArray)step.get("assertedEvents");
	   
	   if(asserts != null) {
		   JSONObject assertData = (JSONObject)asserts.get(0);
		   String title = ((String)assertData.get("title")).toLowerCase();
		   if(title.equals("val".toLowerCase())) {
			   value = (String)assertData.get("url");
			   keyword = "Validation";
			   action = "Validate";
		   }
	   }
	   
	   JSONArray selectors = (JSONArray)step.get("selectors");
	   
	   String xpath = "No xpath found";
	   
	   boolean xpathFound = isXPathDirectlyAvailable(sheet, selectors,keyword,
				  action, value);
	   
	   if(!xpathFound) {
		   xPathDirectlyNotAvailable(sheet, selectors,xpath,keyword,action,value);		   
	   }
	   
	   return sheet;
  }
  
  private static Sheet createNavigateRow(Sheet sheet, JSONObject step) {

	   String url = (String)step.get("url");
	   if(url.equals("chrome://new-tab-page/") && rowNumber == 1) {
	   } else {
		   createRowValues(sheet, "","","","URL",
				  "Get","",(String)step.get("url"),"","","","","",
				 "","","");
	   }
	   return sheet;
  }
  
  private static Sheet createxls() {
	   
	   try {
	    	  workbook = new XSSFWorkbook(); 
	    	  Sheet sheet = workbook.createSheet("TestScript");
	    	    
	    	  createHeaderRowValues(sheet, "Page Name","Description of Action performed","Object Property","Keyword",
	    			  "Action","Flow","TestData","API -  Data","","","API - Expected Result","API  - Test Data ","",
	    			  "API -  Data output","Data Validation","","Result","TestData","Data Validation","","Result",
	    			  "TestData","Data Validation","","Result",true);
	    	  
	    	  createHeaderRowValues(sheet, "","","","","","","Yes","URL","WebService","JSON - Request",
	    			  "JSON - Response","File Name","variableName","Jason Response Parameter = Automation Variable Name",
	    			  "Expected Value","Actual Value","","Yes","Expected Value","Actual Value","","No",
	    			  "Expected Value","Actual Value","",false);
   	
	    	return sheet;

	   } catch(Exception e) {
	         e.printStackTrace();
	   }
	   
	   return null;	   
  }
  
  private static Row createHeaderRowValues(Sheet sheet, String val1,String val2,String val3,String val4,String val5,
		   String val6,String val7,String val8,String merge1, String merge2,String val9,String val10,String merge3,
		   String val11,String val12,String merge4,String val13,String val14,String val15,String merge5,String val16,
		   String val17,String val18,String merge6,String val19,boolean mergeRows) {
	  
	  Row rowhead = sheet.createRow(rowNumber);
	  
    	rowhead.createCell(0).setCellValue(val1);  
	   	rowhead.createCell(1).setCellValue(val2);  
	   	rowhead.createCell(2).setCellValue(val3);  
	   	rowhead.createCell(3).setCellValue(val4);  
	   	rowhead.createCell(4).setCellValue(val5); 
	   	rowhead.createCell(5).setCellValue(val6);  
	   	rowhead.createCell(6).setCellValue(val7);  
	   	rowhead.createCell(7).setCellValue(val8); 
	   	rowhead.createCell(8).setCellValue(merge1);
	   	rowhead.createCell(9).setCellValue(merge2);
	   	rowhead.createCell(10).setCellValue(val9);  
	   	rowhead.createCell(11).setCellValue(val10); 
	   	rowhead.createCell(12).setCellValue(merge3);
	   	rowhead.createCell(13).setCellValue(val11);
	   	rowhead.createCell(14).setCellValue(val12); 
	   	rowhead.createCell(15).setCellValue(merge4);
	   	rowhead.createCell(16).setCellValue(val13);  
	   	rowhead.createCell(17).setCellValue(val14); 
	   	rowhead.createCell(18).setCellValue(val15);
	   	rowhead.createCell(19).setCellValue(merge5);
	   	rowhead.createCell(20).setCellValue(val16); 
	   	rowhead.createCell(21).setCellValue(val17);
	   	rowhead.createCell(22).setCellValue(val18); 
	   	rowhead.createCell(23).setCellValue(merge6);
	   	rowhead.createCell(24).setCellValue(val19);
	   	 
	   	
	   	if(mergeRows) {
	   		sheet.addMergedRegion(CellRangeAddress.valueOf("W1:X1"));
	   		sheet.addMergedRegion(CellRangeAddress.valueOf("S1:T1"));
	   		sheet.addMergedRegion(CellRangeAddress.valueOf("O1:P1")); 
	   		sheet.addMergedRegion(CellRangeAddress.valueOf("L1:M1")); 
	   		sheet.addMergedRegion(CellRangeAddress.valueOf("H1:J1")); 
	   	}
	   	
	   	rowNumber = ++rowNumber;
	   	return rowhead;
 }
  
  private static Row createRowValues(Sheet sheet, String val1,String val2,String val3,String val4,String val5,
		   String val6,String val7,String val8,String val9,String val10,String val11,String val12,String val13,
		   String val14, String val15) {
	  
	  Row rowhead = sheet.createRow(rowNumber);
	  
     	rowhead.createCell(0).setCellValue("Page Name");  
	   	rowhead.createCell(1).setCellValue("Page Action");  
	   	rowhead.createCell(2).setCellValue(val3);  
	   	rowhead.createCell(3).setCellValue(val4);  
	   	rowhead.createCell(4).setCellValue(val5); 
	   	rowhead.createCell(5).setCellValue(val6);  
	   	rowhead.createCell(6).setCellValue(val7);  
	   	rowhead.createCell(7).setCellValue(val8);  
	   	rowhead.createCell(8).setCellValue(val9);  
	   	rowhead.createCell(9).setCellValue(val10);  
	   	rowhead.createCell(10).setCellValue(val11); 
	   	rowhead.createCell(11).setCellValue(val12);  
	   	rowhead.createCell(12).setCellValue(val13);  
	   	rowhead.createCell(13).setCellValue(val14);  
	   	rowhead.createCell(14).setCellValue(val15); 
	   	
	   	rowNumber = ++rowNumber;
	   	return rowhead;
  }
  
  private static void closexls() {
	   try {
		   FileOutputStream fileOut = new FileOutputStream(fileName);
		    workbook.write(fileOut);
		    fileOut.close();
	   } catch (Exception e) {
		   e.printStackTrace();
	   }
  }

}
