package nogrunt;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.json.simple.JSONObject;

import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.hssf.usermodel.HSSFRow;

public class ExcelWriter2 {
	


//  private static String[] columns = { "First Name", "Last Name", "Email",
//    "Date Of Birth" };
  private static String[] columns = { "Page Name","Description of Action performed","Object Property","Keyword",
		  "Action","Flow","TestData","API -  Data","API - Expected Result","API  - Test Data ","API -  Data output",
		  "Data Validation","Result","TestData","Data Validation","Result",
		  "TestData","Data Validation","Result" };

//  public static void main(String[] args) throws IOException, 
//	InvalidFormatException {
//	  
//  }
  
  public XSSFSheet createSheet(String sheetName) {
	   
	   try {
		   XSSFWorkbook workbook = new XSSFWorkbook();   
		   XSSFSheet sheet = workbook.createSheet(sheetName);    
		   return sheet;
	   } catch(Exception e) {
	         e.printStackTrace();
	   }
	   
	   return null;	   
  }
  
  public XSSFSheet retrieveSheet(String fileName, String sheetName) {
	   try {			
		   XSSFWorkbook workbook = new XSSFWorkbook(new FileInputStream(fileName));   
		   XSSFSheet sheet = workbook.getSheet(sheetName);    
		   return sheet;
	   } catch(Exception e) {
	         e.printStackTrace();
	   }
	   
	   return null;	   
 }
  
  public HSSFSheet retrieveXLSSheet(String fileName, String sheetName) {
	   try {			
		   HSSFWorkbook  workbook = new HSSFWorkbook (new FileInputStream(fileName));   
		   HSSFSheet sheet = workbook.getSheet(sheetName);    
		   return sheet;
	   } catch(Exception e) {
	         e.printStackTrace();
	   }
	   
	   return null;	   
}
  
  public XSSFSheet createTSuiteHeaderRows(XSSFSheet sheet) {
	   
	   try {	    
		   createSuiteHeader(sheet,0, "Project","Module","TestCaseName","Run ?",
	    			  "Test Type","DefectID","BROWSER");
	    	return sheet;

	   } catch(Exception e) {
	         e.printStackTrace();
	   }
	   
	   return null;	   
 }
  
  public XSSFSheet createTScenarioHeaderRows(XSSFSheet sheet) {
	   
	   try {	    
		   createScenarioHeader(sheet,0, "Page Name","Description of Action performed","Object Property","Keyword",
	    			  "Action","Flow","TestData","API -  Data","","","API - Expected Result","API  - Test Data ","",
	    			  "API -  Data output","Data Validation","","Result","TestData","Data Validation","","Result",
	    			  "TestData","Data Validation","","Result",true);
	    	  
		   createScenarioHeader(sheet, 1,"","","","","","","Yes","URL","WebService","JSON - Request",
	    			  "JSON - Response","File Name","variableName","Jason Response Parameter = Automation Variable Name",
	    			  "Expected Value","Actual Value","","No","Expected Value","Actual Value","","No",
	    			  "Expected Value","Actual Value","",false);
 	
	    	return sheet;

	   } catch(Exception e) {
	         e.printStackTrace();
	   }
	   
	   return null;	   
}
  
  public XSSFSheet createSuiteHeader(XSSFSheet sheet, int rowNumber, String val1,String val2,String val3,String val4,String val5,
		   String val6,String val7) {
	  
	  Row rowhead = sheet.createRow(rowNumber);
	  
   	rowhead.createCell(0).setCellValue(val1);  
	   	rowhead.createCell(1).setCellValue(val2);  
	   	rowhead.createCell(2).setCellValue(val3);  
	   	rowhead.createCell(3).setCellValue(val4);  
	   	rowhead.createCell(4).setCellValue(val5); 
	   	rowhead.createCell(5).setCellValue(val6);  
	   	rowhead.createCell(6).setCellValue(val7);  
   	
	   	return sheet;
}
  
  public XSSFSheet createSuiteRowValues(XSSFSheet sheet, int rowNumber, String val1,String val2,String val3,String val4,String val5,
		   String val6,String val7) {
	  
	  Row rowHead = sheet.createRow(rowNumber);
	  
	  	rowHead.createCell(0).setCellValue(val1);  
	  	rowHead.createCell(1).setCellValue(val2);  
	  	rowHead.createCell(2).setCellValue(val3);  
	  	rowHead.createCell(3).setCellValue(val4);  
	  	rowHead.createCell(4).setCellValue(val5); 
	  	rowHead.createCell(5).setCellValue(val6);  
	  	rowHead.createCell(6).setCellValue(val7);  
	   	return sheet;
 }
  
  public XSSFSheet createScenarioHeader(XSSFSheet sheet, int rowNumber, String val1,String val2,String val3,String val4,String val5,
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
	   	
	   	return sheet;
 }
  
  public JSONObject getSheetAsJSON(ExcelWriter2 ew2, String fileName) {
	  XSSFSheet sheet = ew2.retrieveSheet(fileName,"TestScript");
	  JSONObject json = ew2.getSheetAsJSON(sheet);
	  JSONObject steps = (JSONObject)json.get("data");
	  
	  String testcasename = fileName.substring(fileName.lastIndexOf("\\")+1,
			  fileName.indexOf("."));
	  int stepId = 1;
	  int randomKey = new Integer(Utilities.randomGen("NUMBER", 8, AppProperties.chatgpton)).intValue();
//	  Iterator iter = steps
	  MySQlConn msc = new MySQlConn(null);
	  int testcaseId = msc.checkAndWriteTestCaseToDB(testcasename, 
			  fileName, 1, "Deva","Recording", randomKey,0,-1,-1);

	  for(int i= 0;i<steps.size();i++) {

		  JSONObject step = (JSONObject)steps.get(i + 2);
		  msc.writeTestStepToDB(false,testcaseId, stepId, 
			  (String)step.get("PageName"), (String)step.get("DescriptionofActionperformed"), 
			  (String)step.get("ObjectProperty"), (String)step.get("Keyword"),
			  (String)step.get("Action"), (String)step.get("Flow"), 
			  (String)step.get("TestData"), (String)step.get("APITestData"), 
			  (String)step.get("APIData"), randomKey, 
			  0.0, "", "", "", -1, "", 
			  "", "",(int)step.get("clickable"),(String)step.get("issues"), Long.valueOf("0").longValue(),
				"","","","","",-1,null, false,"", (int)step.get("tsSequence"),-1);
		  
	  }

	  return json;
  }
  
  public int getColCount(ExcelWriter2 ew2, String fileName, String fieldName) {
	  String sheetName = fieldName.substring(0, fieldName.indexOf(AppProperties.testdatadelimiter));
	  XSSFSheet sheet = ew2.retrieveSheet(fileName,sheetName);
	  return sheet.getRow(0).getLastCellNum();
  }
  
  public int getRowCount(ExcelWriter2 ew2, String fileName, String fieldName) {
	    // Extract the sheet name from the fieldName
	    String sheetName = fieldName.substring(0, fieldName.indexOf(AppProperties.testdatadelimiter));
	    // Retrieve the sheet using the provided method
	    XSSFSheet sheet = ew2.retrieveSheet(fileName, sheetName);
	    // Get the total number of rows
	    return sheet.getLastRowNum() + 1; // Adding 1 because getLastRowNum() is zero-based
	}
  
  public String getCell(ExcelWriter2 ew2, String fileName, String fieldName, 
		  int runonceIndex, boolean useIndex, SelGrid sg, int mrIndex, int mrFieldIndex,
		  ExecutionLogger el) {
	  String sheetName = fieldName.substring(0, fieldName.indexOf(AppProperties.testdatadelimiter));
	  String cellReference = fieldName.substring(fieldName.indexOf(AppProperties.testdatadelimiter) + AppProperties.testdatadelimiter.length(), fieldName.length());
	  
	  String key = null;
	  if(cellReference.contains(AppProperties.testdatadelimiter)) {
		  return getvalueForKey(ew2, fileName, fieldName, sheetName, 
				  cellReference, sg);
	  }
	  String columnReference = cellReference.replaceAll("\\d+", "");
	  XSSFSheet sheet = ew2.retrieveSheet(fileName,sheetName);
	  int rowReference = -1;
	  int columnIndex = -1;
	  int rowreftouse = -1;
	  int colreftouse = -1;
	  if(useIndex) {
		  rowReference = mrFieldIndex ;
		  columnIndex = mrIndex;
		  try {
			  rowReference = Integer.parseInt(cellReference.replaceAll("[^\\d]+", ""));
			  Row firstNonEmptyRow = findFirstNonEmptyRow(sheet);		  
			  if(runonceIndex >= firstNonEmptyRow.getLastCellNum()) {
				  sg.eof = true;
				  return null;
			  }
		  } catch (Exception noRow) {
			  rowReference = mrIndex ;
			  columnIndex = (int) (columnReference.toUpperCase().charAt(0) - 'A') + 1;
			  columnIndex = columnIndex - 1;
		  }
		  rowreftouse = rowReference;
		  colreftouse = columnIndex;		  
  	  } else {
  		rowReference = Integer.parseInt(cellReference.replaceAll("[^\\d]+", ""));
  		columnIndex = (int) (columnReference.toUpperCase().charAt(0) - 'A') + 1;
  		rowreftouse = rowReference - 1;
		colreftouse = columnIndex - 1;
  	  }  
	  
	  
	  XSSFRow row = sheet.getRow(rowreftouse); // Get the 13th row (since row index starts from 0)
	  XSSFCell cell = row.getCell(colreftouse); // Get the 7th cell in the row (since cell index starts from 0)
	  
	  DataFormatter dataFormatter = new DataFormatter();
//	  String cellValue = cell.getRawValue().toString();
	  String cellValue = dataFormatter.formatCellValue(cell);
	  el.logExecution("Retrieving value for cell Row=" + (rowreftouse) + " Col=" + (colreftouse));
	  return cellValue;
  }
  
  public String getXLSCell(ExcelWriter2 ew2, String fileName, String fieldName, 
		  int runonceIndex, boolean useIndex, SelGrid sg, int mrIndex, int mrFieldIndex,
		  ExecutionLogger el) {
	  String sheetName = fieldName.substring(0, fieldName.indexOf(AppProperties.testdatadelimiter));
	  String cellReference = fieldName.substring(fieldName.indexOf(AppProperties.testdatadelimiter) + AppProperties.testdatadelimiter.length(), fieldName.length());
	  
	  String key = null;
	  if(cellReference.contains(AppProperties.testdatadelimiter)) {
		  return getvalueForKey(ew2, fileName, fieldName, sheetName, 
				  cellReference, sg);
	  }
	  String columnReference = cellReference.replaceAll("\\d+", "");
	  int rowReference = -1;
	  int columnIndex = -1;
	  if(useIndex) {
		  rowReference = mrFieldIndex ;
		  columnIndex = mrIndex;
  	  } else {
  		rowReference = Integer.parseInt(cellReference.replaceAll("[^\\d]+", ""));
  		columnIndex = (int) (columnReference.toUpperCase().charAt(0) - 'A') + 1;
  	  }
	  
	  HSSFSheet sheet = ew2.retrieveXLSSheet(fileName,sheetName);
	  Row firstNonEmptyRow = findFirstNonEmptyRowXLS(sheet);
	  
	  if(runonceIndex >= firstNonEmptyRow.getLastCellNum()) {
		  sg.eof = true;
		  return null;
	  }
	  
	  HSSFRow row = sheet.getRow(rowReference -1); // Get the 13th row (since row index starts from 0)
	  HSSFCell cell = row.getCell(columnIndex -1); // Get the 7th cell in the row (since cell index starts from 0)
	  
	  DataFormatter dataFormatter = new DataFormatter();
//	  String cellValue = cell.getRawValue().toString();
	  String cellValue = dataFormatter.formatCellValue(cell);
	  el.logExecution("Retrieving value for cell Row=" + (rowReference -1) + " Col=" + (columnIndex -1));
	  return cellValue;
  }
  
  private static Row findFirstNonEmptyRow(XSSFSheet sheet) {
      // Iterate over rows in the sheet
      for (Row row : sheet) {
          if (row != null && rowIteratorHasData(row)) {
              return row; // Return the first non-empty row
          }
      }
      return null; // No non-empty row found
  }
  
  private static Row findFirstNonEmptyRowXLS(HSSFSheet sheet) {
      // Iterate over rows in the sheet
      for (Row row : sheet) {
          if (row != null && rowIteratorHasData(row)) {
              return row; // Return the first non-empty row
          }
      }
      return null; // No non-empty row found
  }
  
  private static boolean rowIteratorHasData(Row row) {
      // Check if the row has any non-empty cells
      for (int i = row.getFirstCellNum(); i < row.getLastCellNum(); i++) {
          if (row.getCell(i) != null && !row.getCell(i).toString().trim().isEmpty()) {
              return true; // Found a non-empty cell
          }
      }
      return false; // No non-empty cells in the row
  }
  
  
  public boolean checkCell(ExcelWriter2 ew2, String fileName, String fieldName, 
		  String stringToBeFound, boolean exactMatch, int runonceIndex,
		  boolean useIndex, SelGrid sg, int mrIndex, int mrFieldIndex, ExecutionLogger el) {
	  String cellValue = getCell(ew2, fileName, fieldName, runonceIndex,
			  useIndex, sg, mrIndex, mrFieldIndex, el);
	  
	  el.logExecution("Value in the file is : " + cellValue);
	  el.logExecution("Value on the screen is : " + stringToBeFound);
	  
	  if(cellValue != null) {
		  if(exactMatch) {
			  el.logExecution("Checking whether the values are an exact match");
			  if(cellValue.equals(stringToBeFound)) {
				  el.logExecution("The values are an exact match - PASS");
				  return true;
			  }	else {
				  el.logExecution("The values are not an exact match - FAIL");
			  }
		  } else {
			  el.logExecution("Checking whether one of the values contains the other");
			  if(cellValue.contains(stringToBeFound) || stringToBeFound.contains(cellValue)) {
				  el.logExecution("One of the values contains the other - PASS");
				  return true;
			  } else {
				  el.logExecution("Neither of the values contains the other - FAIL");
			  }
		  }
      }
	  
	  return false;
	  
  }
  
  public boolean checkAllRows(ExcelWriter2 ew2, String fileName, String fieldName, String stringToBeFound) {
	  String sheetName = fieldName.substring(0, fieldName.indexOf(AppProperties.testdatadelimiter));
	  String cellReference = fieldName.substring(fieldName.indexOf(AppProperties.testdatadelimiter) + 1, fieldName.length());
	  
	  int rowReference = Integer.parseInt(cellReference.replaceAll("[^\\d]+", ""));
	  
	  XSSFSheet sheet = ew2.retrieveSheet(fileName,sheetName);
	  XSSFRow row = sheet.getRow(rowReference -1);
	  
	  if(row != null) {
		  for (int colIndex = 0; colIndex < row.getLastCellNum(); colIndex++) {
	          XSSFCell cell = row.getCell(colIndex);
	          if (cell != null) {
	              String cellValue = cell.getStringCellValue();
	              if(cellValue.equals(stringToBeFound)) {
	            	  return true;
	              }
	          }
	      }	  
	  }
	  return false;
  }
  
  public boolean checkAllCols(ExcelWriter2 ew2, String fileName, String fieldName, String stringToBeFound) {
	  String sheetName = fieldName.substring(0, fieldName.indexOf(AppProperties.testdatadelimiter));
	  String cellReference = fieldName.substring(fieldName.indexOf(AppProperties.testdatadelimiter) + 1, fieldName.length());
	  String columnReference = cellReference.replaceAll("\\d+", ""); 
	  
	  int columnIndex = (int) (columnReference.toUpperCase().charAt(0) - 'A') + 1;
	  
	  XSSFSheet sheet = ew2.retrieveSheet(fileName,sheetName);
	  
	  // loop through all rows in the sheet
      for (int rowIndex = 0; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
          XSSFRow row = sheet.getRow(rowIndex);
          
          if (row != null) {
              // loop through all columns in the row
        	  XSSFCell cell = row.getCell(columnIndex -1);
        	  if(cell != null) {
        		  String cellValue = cell.getStringCellValue();
        		  if(cellValue.equals(stringToBeFound)) {
                	  return true;
                  }
        	  }
          }
      }
	  
	  return false;
  }
  
  public String getvalueForKey(ExcelWriter2 ew2, String fileName, String fieldName,
		  String sheetName, String cellReference, SelGrid sg) {
	  
	  String resultColumn = cellReference.substring(0,cellReference.indexOf("["));
	  int resultColumnIndex = (int) (resultColumn.toUpperCase().charAt(0) - 'A') + 1;
	  cellReference = cellReference.substring(cellReference.indexOf("[") + 2 ,cellReference.length());
	  String searchColumn = cellReference.substring(0 ,cellReference.indexOf("]"));
	  int searchColumnIndex = (int) (searchColumn.toUpperCase().charAt(0) - 'A') + 1;
	  cellReference = cellReference.substring(cellReference.indexOf("]") + 2 ,cellReference.length());
	  String key = cellReference.substring(0 ,cellReference.indexOf("]"));
	  key = sg.getVarValue(key);
	  
	  XSSFSheet sheet = ew2.retrieveSheet(fileName,sheetName);
	  
	  // loop through all rows in the sheet
      for (int rowIndex = 0; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
          XSSFRow searchRow = sheet.getRow(rowIndex);
          
          if (searchRow != null) {
              // loop through all columns in the row
        	  XSSFCell searchCell = searchRow.getCell(searchColumnIndex -1);
        	  if(searchCell != null) {
        		  String searchCellValue = searchCell.getStringCellValue();
        		  if(searchCellValue.equals(key)) {
        			  XSSFCell resultCell = searchRow.getCell(resultColumnIndex -1);
        			  String resultCellValue = resultCell.getStringCellValue();
                	  return resultCellValue;
                  }
        	  }
          }
      }
	  
	  return null;
  }
  
  public boolean checkFile(ExcelWriter2 ew2, String fileName, String sheetName, String stringToBeFound) {
	  
	  XSSFSheet sheet = ew2.retrieveSheet(fileName,sheetName);
	  
	  // loop through all rows in the sheet
      for (int rowIndex = 0; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
          XSSFRow row = sheet.getRow(rowIndex);
          
          if (row != null) {
        	  for (int colIndex = 0; colIndex < row.getLastCellNum(); colIndex++) {
    	          XSSFCell cell = row.getCell(colIndex);
    	          if (cell != null) {
    	              String cellValue = cell.getStringCellValue();
    	              if(cellValue != null && cellValue.equals(stringToBeFound)) {
    	            	  return true;
    	              }
    	          }
    	      }	
          }
      }
	  
	  return false;
  }
  
  public JSONObject getSheetAsJSON(XSSFSheet sheet) {
	  
	  Row headerRow = sheet.getRow(0);
	  JSONObject JSONarr = new JSONObject();
	  
	  for(int i=2; i<sheet.getPhysicalNumberOfRows(); i++) {
		  Row row = sheet.getRow(i);
		  JSONObject jsonRow = new JSONObject();
		  for(int j=0; j<headerRow.getPhysicalNumberOfCells(); j++) {
			  if(row != null && row.getCell(j) != null) {
				  String key = headerRow.getCell(j).toString().replaceAll(" ", "");
				  key = key.replaceAll("-", "");
				  jsonRow.put(key, row.getCell(j).toString());
			  }
		  }
		  JSONarr.put(i,jsonRow);
	  }
  
	  JSONObject JSONData = new JSONObject();
	  JSONData.put("data", JSONarr);
	  
	  return JSONData;
  }
  
  public XSSFSheet createRowValues(XSSFSheet sheet, String val1,String val2,String val3,String val4,String val5,
		   String val6,String val7,String val8,String val9,String val10,String val11,String val12,String val13,
		   String val14, String val15) {
	  
	  Row previousRow = sheet.getRow(sheet.getLastRowNum());
	  boolean createRow = true;
	  
	  if(val6.equals("")) {
		  val6 = "Positive";
	  }
	  
	  if(val3.equals(previousRow.getCell(2).toString()) &&
			  val4.equals(previousRow.getCell(3).toString()) &&
			  val5.equals(previousRow.getCell(4).toString())) {
		  if(val3.endsWith("INPUT") || val5.equals("pdfTextPartial")) {
		  
			  if(val7 != null && 
					  previousRow.getCell(6).toString() != null &&
					  !val7.equals(previousRow.getCell(6).toString())) {
				  if(previousRow.getCell(6).toString().equals("") &&
						  !val7.equals("")) {
					  previousRow.createCell(6).setCellValue(val7);
					  createRow = false;
				  } else if(!previousRow.getCell(6).toString().equals("") &&
						  val7.equals("")) {
					  createRow = false;
				  } else {
					  createRow = true;
				  }
			  } else if(val7 != null && 
					  previousRow.getCell(6).toString() != null &&
					  val7.equals(previousRow.getCell(6).toString())) {
				  createRow = false;
			  }
		  } else {
			  createRow = false;
		  }
		  
	  } 
	  
	  if (createRow){
	  
	  Row rowHead = sheet.createRow(sheet.getLastRowNum()+1);
	  	if(val1 == null || !val1.equals("")) {
	  		rowHead.createCell(0).setCellValue(val1);
	  	} else {
	  		rowHead.createCell(0).setCellValue("Page Name");
	  	}
	  	
	  	if(val2 == null || !val2.equals("")) {
	  		rowHead.createCell(1).setCellValue(val2);
	  	} else {
	  		rowHead.createCell(1).setCellValue("");
	  	}
	  	  
//	  	rowHead.createCell(1).setCellValue("Page Action");  
	  	rowHead.createCell(2).setCellValue(val3);  
	  	rowHead.createCell(3).setCellValue(val4);  
	  	rowHead.createCell(4).setCellValue(val5); 
	  	rowHead.createCell(5).setCellValue(val6);  
	  	rowHead.createCell(6).setCellValue(val7);  
	  	rowHead.createCell(7).setCellValue(val8);  
	  	rowHead.createCell(8).setCellValue(val9);  
	  	rowHead.createCell(9).setCellValue(val10);  
	  	rowHead.createCell(10).setCellValue(val11); 
	  	rowHead.createCell(11).setCellValue(val12);  
	  	rowHead.createCell(12).setCellValue(val13);  
	  	rowHead.createCell(13).setCellValue(val14);  
	  	rowHead.createCell(14).setCellValue(val15);
	  }
	   	return sheet;
  }
  
  public XSSFSheet updateRowValues(XSSFSheet sheet, String val1,String val2,String val3,String val4,String val5,
		   String val6,String val7,String val8,String val9,String val10,String val11,String val12,String val13,
		   String val14, String val15) {
	  
	  Row rowHead = sheet.getRow(sheet.getLastRowNum());
	  
	  	rowHead.createCell(3).setCellValue(val4);  
	  	rowHead.createCell(4).setCellValue(val5); 
	  	rowHead.createCell(6).setCellValue(val7); 

	   	return sheet;
 }
  
  public  void closexls(String fullFileName, XSSFWorkbook  workbook) throws Exception{
		   FileOutputStream fileOut = new FileOutputStream(fullFileName);
		    workbook.write(fileOut);
		    fileOut.close();
  }
  
  public  void transferXLS(InputStream is , String fullFileName) throws Exception{
	// create an XSSFWorkbook object from the input stream
	  XSSFWorkbook workbook = new XSSFWorkbook(is);

	  // get the sheet and process it
	  XSSFSheet sheet = workbook.getSheetAt(0);

	  // iterate through the rows and process the data
	  for (Row row : sheet) {
	      // process each row
	  }

	  // close the workbook and input stream
	  workbook.close();
	  is.close();
  }
  
//  public String readFromCSV(String filename, String filefield) {
//      String filePath = "path/to/your/file.csv";
//      String[] fields = filefield.split(AppProperties.delimiter);
//      String cellReference = fields[1];
//      int targetRow = getRowIndex(cellReference);
//      int targetColumn = getColumnIndex(cellReference);
//
//      try (CSVReader reader = new CSVReader(new FileReader(filePath))) {
//          String[] line;
//          int currentRow = 0;
//
//          while ((line = reader.readNext()) != null) {
//              if (currentRow == targetRow) {
//                  System.out.println("Cell G2 Value: " + line[targetColumn]);
//                  break;
//              }
//              currentRow++;
//          }
//      } catch (IOException e) {
//          e.printStackTrace();
//      }
//  }
  
  public int getRowIndex(String cellReference) {
      String rowPart = cellReference.replaceAll("[A-Z]", ""); // Remove letters to get the row number
      return Integer.parseInt(rowPart) - 1; // Convert to 0-based index
  }
  
  public static int getColumnIndex(String cellReference) {
      String columnPart = cellReference.replaceAll("[0-9]", ""); // Remove numbers to get the column letters
      int columnIndex = 0;

      // Convert letters to a number (e.g., A = 0, B = 1, ..., G = 6)
      for (int i = 0; i < columnPart.length(); i++) {
          columnIndex = columnIndex * 26 + (columnPart.charAt(i) - 'A' + 1);
      }
      return columnIndex - 1; // Convert to 0-based index
  }
}
