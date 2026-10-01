package nogrunt;

import java.util.Base64;
import java.util.Iterator;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.sql.SQLException;
import nogrunt.exceptions.*;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import java.nio.charset.*;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLDecoder;

import nogrunt.codegen.*;

public class TransformTS {
	
	public static final int DEFAULT_BUFFER_SIZE = 8192;
	
	//String fullFileName = null;
	String fileName = null;
	XSSFWorkbook  workbook = null;
	Row lastEditBox = null;
	//String testCasesFolder = AppProperties.testcasesfolder;
	ExcelWriter2 ew = new ExcelWriter2();
	int stepId = 0;
//	MySQlConn msc = new MySQlConn();
	MySQlConn msc;
	int testcaseId = -1;
	long randomKey = -1;
	double eventTime = 0.0;
	String saveType = "";
	String mode = null;
	int analysisTC = -1;
	String dirName = "test";
	String uname = "";
	boolean merging = false;
	JSONObject mergeJsonObject = null;
	
	public void setMSC(MySQlConn m) {
		msc = m;
	}
	  
	  public void processIncStepsJson(JSONObject step,String sType, 
			  String name, Utilities userCache) {
		  
		  saveType = sType;
		  String gentype = "recording";
		  String randomkey = "";
		  int companyId = -1;
		  
		  JSONObject result = null;
		  mode = (String)step.get("mode");
		  try {
			  
			  if(step.get("key") != null) {
				  if(!(step.get("key") instanceof String)) {
					  randomKey = (long)step.get("key");
					  randomkey = new Long((long)step.get("key")).toString(); 
				  } else if (step.get("key") instanceof String) {
					  randomkey = (String)step.get("key");
					  randomKey = Long.valueOf(randomkey); 
				  }
				  
				  if((mode == null) || (mode != null && !mode.equals("analysis"))) {
					  JSONObject userDetails = userCache.getUserFromCache(randomkey);
					  companyId =  (int)userDetails.get("companyid");
					  msc.setUserDetails(userDetails);
					  gentype = msc.getGetTypeForCompany(companyId);
				  }
			  }
			  if(step.get("time") != null) {
				  if(step.get("time") instanceof Long) {
					  eventTime = (long)step.get("time") / 1.0;
				  } else if (step.get("time") instanceof Double){
					  eventTime = (double)step.get("time");
				  } else {
					  String timestr = (String)step.get("time");
					  if(timestr != null && !timestr.equals("")) {
						  eventTime = Double.valueOf(timestr);
					  }
				  }
			  }			  
			  
			  if(mode != null && mode.equals("analysis")) {
				  analysisTC = Integer.valueOf((String)step.get("analysisTC"));
				  if(analysisTC == -1) {
					  return ;
				  }
				  JSONObject userDetails = userCache.getUserFromCache(randomkey);
				  String uname =  (String)userDetails.get("uname");
				  int cid = msc.getCompanyFromUser(uname);
				  userDetails.put("companyid", cid);
			  }		  
			  
//			  if(gentype.equals("code")) {
//				  SelJava sj = new SelJava();
//				  sj.setMsc(msc);
//				  sj.genCode(step, companyId, randomkey, userCache);
//				  GenCypress gc = new GenCypress();
//				  gc.setMsc(msc);
//				  gc.genCode(step, companyId, randomkey, userCache);
//			  } else {
				  processSteps(null, step,  sType,userCache, randomkey);
//			  }
			  result = createIncResponse(testcaseId);
			}catch(TestStepBeforeTitleException e) {
				throw e;
		  } catch(Exception e) {
		    e.printStackTrace();
		  }	
		  
		  return ;
	  }
	  
	  private XSSFSheet processSteps(XSSFSheet sheet, JSONObject step, String sType,
			  Utilities userCache, String randomkey) {
		  
		  try {
			  String stepType = (String)step.get("action");
			  JSONObject userDetails = userCache.getUserFromCache(randomkey);
			  msc.setUserDetails(userDetails);			  
		   		
			  if(userDetails != null) {
				  uname = (String)userDetails.get("uname");
				  mergeJsonObject = Utilities.getMergeStepRequest(uname);
				  if(mode == null) {	 			
			   			if(mergeJsonObject != null) {
			   				String mergeRandomKey = (String)mergeJsonObject.get("randomkey");
			   				if(mergeRandomKey == null ) {
			   					mergeRandomKey = String.valueOf((long)mergeJsonObject.get("key"));
			   				}
			   				if(mergeRandomKey.equals(randomkey)) {
			   					merging = true;		
			   				} else {
			   					Utilities.flushMergeStepRequest(uname);
			   				}
			   			}
				  } else if(mode.equalsIgnoreCase("analysis")) {
		   				int analysisTC = Integer.valueOf((String)step.get("analysisTC"));
		   				testcaseId = analysisTC;
				  }
			  } else {
		   			uname = "SYS";
		   		}
		   		
			   	if(stepType.equals("makeVar")) {
			   		stepType = (String)step.get("prevAction");
			   	}
			   	if(stepType.equals("title")) {
			   		String title = (String)step.get("title");
			   		fileName = title;
			   		dirName = fileName;
			   		String moduleIdStr = (String)step.get("moduleid");
			   		int moduleId = new Integer(moduleIdStr).intValue();
			   		msc.setUserDetails(userDetails);
			   		
			   		if(userDetails != null && userDetails.get("uname") != null) {
			   			uname = (String)userDetails.get("uname");
			   		}
			   		testcaseId = msc.checkAndWriteTestCaseToDB(fileName, 
			   				"", 1, uname,sType, randomKey, moduleId,
			   				-1, -1);
			   		int companyId = msc.getCompanyFromModule(moduleId);
			   		Utilities.createTestCaseFolder(companyId, testcaseId);
			   	} else if(stepType.equals("merge")) {
			   		userCache.cacheMergeStepRequest(uname, step);
			   	} else if(stepType.equals("url")) {
			   		step.put("tsSequence", 0);
			   		if(!merging) {
			   			long width = (long)step.get("width");
			   			long height = (long)step.get("height");
			   			msc.updateViewPortForTestCase(width, height, randomKey);
			   			createNavigateRow(sheet, step);
			   		} else {
			   			long mergetc = Long.parseLong((String)mergeJsonObject.get("mergetc"));
						  int test_case = (int)mergetc;
						  msc.updateTestCaseKey(test_case, randomKey);
			   		}
			   	} else if(stepType.equals("click")) {
			   		createClickRow(sheet, step);
			   	} else if(stepType.equals("button")) {
			   		createButtonRow(sheet, step);
			   	} else if(stepType.equals("submit") ) {
			   		createSubmitRow(sheet, step);			   		
			   	} else if(stepType.equals("select")) {
			   		createSelectRow(sheet, step);
			   	} else if(stepType.equals("change")) {
			   		createChangeRow(sheet, step);
			   	} else if(stepType.equals("validatetext")) {
			   		createValidateRow(sheet, step);
			   	} else if(stepType.equals("radio")) {
			   		createRadioRow(sheet, step);
			   	} else if(stepType.equals("checkbox")) {
			   		createCheckBoxRow(sheet, step);
			   	} else if(stepType.equals("fileUpload")) {
			   		createFileUploadRow(sheet, step);
			   	} else if(stepType.equals("makeVar")) {
			   		createMakeVarRow(sheet, step);
			   	} else if(stepType.equals("validateVar")) {
			   		createValidateVarRow(sheet, step);
			   	} else if(stepType.equals("clear")) {
			   		createClearRow(sheet, step);
			   	} else if(stepType.equals("pdfTextPartial")) {
			   		createPdfTextPartialRow(sheet, step);
			   	} else if(stepType.equals("getText")) {
			   		createMakeVarRow(sheet, step);
			   	} else if(stepType.equals("getProperty")) {
			   		createGetPropertyRow(sheet, step);
			   	} else if(stepType.equals("saveImage")) {
			   		storeImage(step);
			   	} else if(stepType.equals("dblClick")) {
			   		createDblClickRow(sheet, step);
			   	} else if(stepType.equals("fileDownload")) {
			   		createFileDownloadRow(sheet, step);
			   	} else if(stepType.equals("dnddrop")) {
			   		createDndDrop(sheet, step);
			   	} else if(stepType.equals("dnddrag")) {
			   		createDndDrag(sheet, step);	
			   	} else if(stepType.startsWith("Arrow")) {
			   		createArrow(sheet, step);	
			   	} else if(stepType.equalsIgnoreCase("rightclick")) {
			   		createRightClick(sheet, step);	
			   	} else if(stepType.equalsIgnoreCase("apiData")) {
			   		processApiData(step);	
			   	} else if(stepType.equalsIgnoreCase("error")) {
			   		createErrorRow(sheet, step);
			   	} else if(stepType.equalsIgnoreCase("createwindow")) {
			   		createNewWindow(step);
			   	} else if(stepType.equalsIgnoreCase("ByText")) {
			   		createByTextRow(step);
			   	}
		       
		       return sheet;
		  }catch(TestStepBeforeTitleException e) {
				throw e;
		  } catch (Exception e) {
			  e.printStackTrace();
		  } finally {
//			  msc.releaseConn();
			  msc = null;
		  }
		  
		  return null;
	  }
	    
	  private JSONObject createIncResponse(int testcaseId) {
		  JSONObject jsonRow = new JSONObject();			
		  jsonRow.put("idtest_case", testcaseId);
		  return jsonRow;
	  }
	  
	  private void storeImage(JSONObject step) {
		  try {
			  String testCase = (String)step.get("testcase");
			  String filename = (String)step.get("ssdir");
			  
			  msc.updateImageLocation(testCase, randomKey, eventTime,
						filename);
			} catch (Exception e) {
				e.printStackTrace();
			}
	  }
	  
	  private static void copyInputStreamToFile(InputStream inputStream, File file)
	            throws IOException {

	        // append = false
	        try (FileOutputStream outputStream = new FileOutputStream(file, false)) {
	            int read;
	            byte[] bytes = new byte[DEFAULT_BUFFER_SIZE];
	            while ((read = inputStream.read(bytes)) != -1) {
	                outputStream.write(bytes, 0, read);
	            }
	        }

	    }
	  
	  private XSSFSheet createChangeRow(XSSFSheet sheet, JSONObject step) {
		  String action = "Clear & Enter";
		  String keyword = "EditBox";
		  String value = getValue(sheet, step) ;		  
		  String xpath = (String)step.get("click");
		  String varName = getVarName(step);
		  String pageDesc = (String)step.get("pageName");
		  String valDevice = (String)step.get("valDevice");
		  String flow = (String)step.get("Flow");
		  String filename = (String)step.get("filename");
		  String fileField = (String)step.get("fileField");
		  String DBUrl = (String)step.get("DBUrl");
//		  int apiid = (int)step.get("apiid");
		  String apiparam = (String)step.get("apiparam");
		  String DBQuery = (String)step.get("DBQuery");
		  String DBType = (String)step.get("DBType");
		  
		  sheet = createRowValues(sheet, step, "",pageDesc,xpath,keyword,
				   action,flow,value,"","","","","",
				 varName,"","",valDevice,filename, fileField, 
					DBUrl, -1, apiparam, DBQuery, DBType);
		  
		  return sheet;
	  }
	  
	  private XSSFSheet createValidateRow(XSSFSheet sheet,JSONObject step) {
		  String action = "validatePartial";
		  String keyword = "Assertion";
		  String value = getValue(sheet, step) ;		  
		  String xpath = (String)step.get("click");
		  String varName = getVarName(step);
		  String pageDesc = (String)step.get("pageName");
		  String valDevice = (String)step.get("valDevice");
		  String flow = (String)step.get("Flow");
		  String filename = (String)step.get("filename");
		  String fileField = (String)step.get("fileField");
		  String DBUrl = (String)step.get("DBUrl");
//		  int apiid = (int)step.get("apiid");
		  String apiparam = (String)step.get("apiparam");
		  String DBQuery = (String)step.get("DBQuery");
		  String DBType = (String)step.get("DBType");
		   
		  sheet = createRowValues(sheet, step, "",pageDesc,xpath,keyword,
				   action,flow,value,"","","","","",
				 varName,"","",valDevice, filename, fileField, 
					DBUrl, -1, apiparam, DBQuery, DBType);
		  
		  return sheet;
	  }
	  
	  private XSSFSheet createRadioRow(XSSFSheet sheet,JSONObject step) {
		  String action = "click";
		  String keyword = "RadioButton";
		  String value = getValue(sheet, step) ;	  
		  String xpath = (String)step.get("click");
		  String varName = getVarName(step);
		  String pageDesc = (String)step.get("pageName");
		  String valDevice = (String)step.get("valDevice");
		  String flow = (String)step.get("Flow");
		   
		  sheet = createRowValues(sheet, step, "",pageDesc,xpath,keyword,
				   action,flow,value,"","","","","",
				 varName,"","",valDevice,"","","",-1,"","","");
		  
		  return sheet;
	  }
	  
	  private XSSFSheet createCheckBoxRow(XSSFSheet sheet,JSONObject step) {
		  String action = "click";
		  String keyword = "Checkbox";
		  String value = getValue(sheet, step) ;	  
		  String xpath = (String)step.get("click");
		  String varName = getVarName(step);
		  String pageDesc = (String)step.get("pageName");
		  String valDevice = (String)step.get("valDevice");
		  String flow = (String)step.get("Flow");
		   
		  sheet = createRowValues(sheet, step, "",pageDesc,xpath,keyword,
				   action,flow,value,"","","","","",
				 varName,"","",valDevice,"","","",-1,"","","");
		  
		  return sheet;
	  }
	  
	  private XSSFSheet createFileUploadRow(XSSFSheet sheet,JSONObject step) {
		  String action = "Upload";
		  String keyword = "FileUpload";
		  String value = getValue(sheet, step) ;		  
		  String xpath = (String)step.get("click");
		  String varName = getVarName(step);
		  String pageDesc = (String)step.get("pageName");
		  String valDevice = (String)step.get("valDevice");
		  String flow = (String)step.get("Flow");
		  String filename = (String)step.get("filename");
		  String fileField = (String)step.get("fileField");
		  String DBUrl = (String)step.get("DBUrl");
		  String apiparam = (String)step.get("apiparam");
		  String DBQuery = (String)step.get("DBQuery");
		  String DBType = (String)step.get("DBType");
		  
		  sheet = createRowValues(sheet, step, "",pageDesc,xpath,keyword,
					   action,flow,value,"","","","","",
					 varName,"","",valDevice, filename, fileField, 
						DBUrl, -1, apiparam, DBQuery, DBType);
		  return sheet;
	  }
	  
	  private XSSFSheet createGetPropertyRow(XSSFSheet sheet,JSONObject step) {
		  String action = "GetProperty";
		  String keyword = "Element";
		  String value = getValue(sheet, step) ;	
		  String xpath = (String)step.get("click");
		  String varName = getVarName(step);
		  String pageDesc = (String)step.get("pageName");
		  String valDevice = (String)step.get("valDevice");
		  String flow = (String)step.get("Flow");
		  String filename = (String)step.get("filename");
		  String fileField = (String)step.get("fileField");
		  String DBUrl = (String)step.get("DBUrl");
//		  int apiid = (int)step.get("apiid");
		  String apiparam = (String)step.get("apiparam");
		  String DBQuery = (String)step.get("DBQuery");
		  String DBType = (String)step.get("DBType");
		   
		  sheet = createRowValues(sheet, step, "",pageDesc,xpath,keyword,
				   action,flow,value,"","","","","",
				 varName,"","",valDevice,filename, fileField, 
					DBUrl, -1, apiparam, DBQuery, DBType);
		  
		  return sheet;
	  }
	  
	  private XSSFSheet createMakeVarRow(XSSFSheet sheet,JSONObject step) {
		  String action = "GetText";
		  String keyword = "Element";
		  String value = getValue(sheet, step) ;		  
		  String xpath = (String)step.get("click");
		  String varName = getVarName(step);
		  String pageDesc = (String)step.get("pageName");
		  String valDevice = (String)step.get("valDevice");
		  String flow = (String)step.get("Flow");
		  String filename = (String)step.get("filename");
		  String fileField = (String)step.get("fileField");
		  String DBUrl = (String)step.get("DBUrl");
//		  int apiid = (int)step.get("apiid");
		  String apiparam = (String)step.get("apiparam");
		  String DBQuery = (String)step.get("DBQuery");
		  String DBType = (String)step.get("DBType");
		   
		  sheet = createRowValues(sheet, step, "",pageDesc,xpath,keyword,
				   action,flow,value,"","","","","",
				 varName,"","",valDevice,filename, fileField, 
					DBUrl, -1, apiparam, DBQuery, DBType);
		  
		  return sheet;
	  }
	  
	  private XSSFSheet createValidateVarRow(XSSFSheet sheet,JSONObject step) {
		  String action = "SearchText";
		  String keyword = "Validation";
		  String value = getValue(sheet, step) ;		  
		  String xpath = "";
		  String varName = getVarName(step);
		  String pageDesc = (String)step.get("pageName");
		  String valDevice = (String)step.get("valDevice");
		  String flow = (String)step.get("Flow");
		  String filename = (String)step.get("filename");
		  String fileField = (String)step.get("fileField");
		  String DBUrl = (String)step.get("DBUrl");
//		  int apiid = (int)step.get("apiid");
		  String apiparam = (String)step.get("apiparam");
		  String DBQuery = (String)step.get("DBQuery");
		  String DBType = (String)step.get("DBType");
		   
		  sheet = createRowValues(sheet, step, "",pageDesc,xpath,keyword,
				   action,flow,value,"","","","","",
				 varName,"","",valDevice,filename, fileField, 
					DBUrl, -1, apiparam, DBQuery, DBType);
		  
		  return sheet;
	  }
	  
	  private XSSFSheet createClearRow(XSSFSheet sheet,JSONObject step) {
		  String action = "clear";
		  String keyword = "EditBox";
		  String value = "";		  
		  String xpath = (String)step.get("click");
		  String varName = getVarName(step);
		  String pageDesc = (String)step.get("pageName");
		  String valDevice = (String)step.get("valDevice");
		  
		   
		  sheet = createRowValues(sheet, step, "",pageDesc,xpath,keyword,
				   action,"Optional",value,"","","","","",
				 varName,"","",valDevice,"","","",-1,"","","");
		  
		  return sheet;
	  }
	  
	  private XSSFSheet createPdfTextPartialRow(XSSFSheet sheet,JSONObject step) {
		  String action = "pdfTextPartial";
		  String keyword = "Validation";
		  String codedUrl = (String)step.get("PDFFile");
		  String fileName = "";
		  try {
			  String decodeURl = java.net.URLDecoder.decode(codedUrl, StandardCharsets.UTF_8.name());
			  java.io.File file = new java.io.File(decodeURl);
			  fileName = file.getName();
			  String ext = fileName.substring(fileName.indexOf("."),fileName.length());
			  if(fileName.contains("(")) {
				  fileName = fileName.substring(0,fileName.lastIndexOf("(") -1);				  
				  fileName = fileName+ ext;
			  }
			  
		  } catch(Exception e) {
			  e.printStackTrace();
		  }
		  String value = "File=" + fileName + "," + (String)step.get("testdata");
		  if(value.endsWith("=value")) {
			  value = value.substring(0,value.indexOf("=value"));
		  }
		  String xpath = (String)step.get("click");
		  String varName = getVarName(step);
		  String pageDesc = (String)step.get("pageName");
		  String valDevice = (String)step.get("valDevice");
		  String flow = (String)step.get("Flow");
		  String filename = (String)step.get("filename");
		  String fileField = (String)step.get("fileField");
		  String DBUrl = (String)step.get("DBUrl");
//		  int apiid = (int)step.get("apiid");
		  String apiparam = (String)step.get("apiparam");
		  String DBQuery = (String)step.get("DBQuery");
		  String DBType = (String)step.get("DBType");
		   
		  sheet = createRowValues(sheet, step, "",pageDesc,xpath,keyword,
				   action,"Optional",value,"","","","","",
				 varName,"","",valDevice,filename, fileField, 
					DBUrl, -1, apiparam, DBQuery, DBType);
		  
		  return sheet;
	  }
	  
	  private String getVarName(JSONObject step) {
		   String stepType = (String)step.get("action");
		   String varName = "";
		   	if(stepType.equals("makeVar")) {
		   		varName = (String)step.get("varName");
		   		varName = AppProperties.delimiter + varName + AppProperties.delimiter;
		   	}
		   	return varName;
	  }
	  
	  private String getValue(XSSFSheet sheet,JSONObject step) {
		  String value = (String)step.get("testdata"); 
		  return value;
	  }
	  
	   private XSSFSheet createClickRow(XSSFSheet sheet,JSONObject step) {
		   
		   String keyword = "Link";
		   String action = "Click";		  
		   String xpath = (String)step.get("click");
		   String value = getValue(sheet, step) ;
		   String varName = getVarName(step);
		   String pageDesc = (String)step.get("pageName");
		   String valDevice = (String)step.get("valDevice");
		   String flow = (String)step.get("Flow");
		   
		   if(step.get("keyword") != null) {
			   keyword = (String)step.get("keyword");
		   }
		   	
		   	if(step.get("validateWith") != null) {
		   		int target = (new Integer((String)step.get("validateWith"))).intValue();
		   		Row prevRow = sheet.getRow(target + 1);
		   		value = ((Cell)prevRow.getCell(12)).getStringCellValue();
		   	}
		   
		   sheet = createRowValues(sheet, step, "",pageDesc,xpath,keyword,
				   action,flow,value,"","","","","",
				varName,"","",valDevice,"","","",-1,"","","");
		
		   
		   return sheet;
	  }
	   
	   private void createByTextRow(JSONObject step) {
		   
		   String keyword = "DropDown";
		   String action = "ByText";		  
		   String xpath = (String)step.get("click");
		   String value = (String)step.get("testdata");
		   String varName = getVarName(step);
		   String pageDesc = (String)step.get("pageName");
		   String valDevice = (String)step.get("valDevice");
		   String flow = (String)step.get("Flow");
		   
		   if(step.get("keyword") != null) {
			   keyword = (String)step.get("keyword");
		   }
		   	
		   
		    createRowValues(null, step, "",pageDesc,xpath,keyword,
				   action,flow,value,"","","","","",
				varName,"","",valDevice,"","","",-1,"","","");
		   
	  }
	   
	   private XSSFSheet createErrorRow(XSSFSheet sheet,JSONObject step) {
		   
		   String keyword = "Error";
		   String action = "Error";		  
		   String xpath = (String)step.get("click");
		   String value = getValue(sheet, step) ;
		   String varName = getVarName(step);
		   String pageDesc = (String)step.get("pageName");
		   String valDevice = (String)step.get("valDevice");
		   String flow = (String)step.get("Flow");
		   
		   if(step.get("keyword") != null) {
			   keyword = (String)step.get("keyword");
		   }
		   	
		   	if(step.get("validateWith") != null) {
		   		int target = (new Integer((String)step.get("validateWith"))).intValue();
		   		Row prevRow = sheet.getRow(target + 1);
		   		value = ((Cell)prevRow.getCell(12)).getStringCellValue();
		   	}
		   
		   sheet = createRowValues(sheet, step, "",pageDesc,xpath,keyword,
				   action,flow,value,"","","","","",
				varName,"","",valDevice,"","","",-1,"","","");
		
		   
		   return sheet;
	  }
	   
	   private void createNewWindow(JSONObject step) {
		   
		   String keyword = "Window";
		   String action = "Create";		  
		   String xpath = (String)step.get("click");
		   String value = (String)step.get("url"); ;
		   String varName = getVarName(step);
		   String pageDesc = (String)step.get("pageName");
		   String valDevice = (String)step.get("valDevice");
		   String flow = (String)step.get("Flow");
		   
		   createRowValues(null, step, "",pageDesc,xpath,keyword,
				   action,flow,value,"","","","","",
				varName,"","",valDevice,"","","",-1,"","","");		

	  }
	   
	   private XSSFSheet createDblClickRow(XSSFSheet sheet,JSONObject step) {
		   
		   String keyword = "Link";
		   String action = (String)step.get("action");	  
		   String xpath = (String)step.get("click");
		   String value = getValue(sheet, step) ;
		   String varName = getVarName(step);
		   String pageDesc = (String)step.get("pageName");
		   String valDevice = (String)step.get("valDevice");
		   String flow = (String)step.get("Flow");
		   
		   if(step.get("keyword") != null) {
			   keyword = (String)step.get("keyword");
		   }
		   	
		   	if(step.get("validateWith") != null) {
		   		int target = (new Integer((String)step.get("validateWith"))).intValue();
		   		Row prevRow = sheet.getRow(target + 1);
		   		value = ((Cell)prevRow.getCell(12)).getStringCellValue();
		   	}
		   
		   sheet = createRowValues(sheet, step, "",pageDesc,xpath,keyword,
				   action,flow,value,"","","","","",
				varName,"","",valDevice,"","","",-1,"","","");
		
		   
		   return sheet;
	  }
	   
	   private XSSFSheet createDndDrop(XSSFSheet sheet,JSONObject step) {
		   
		   String keyword = "Link";
		   String action = (String)step.get("action");	  
		   String xpath = (String)step.get("click");
		   String value = getValue(sheet, step) ;
		   String varName = getVarName(step);
		   String pageDesc = (String)step.get("pageName");
		   String valDevice = (String)step.get("valDevice");
		   String flow = (String)step.get("Flow");
		   
		   if(step.get("keyword") != null) {
			   keyword = (String)step.get("keyword");
		   }
		   	
		   
		   sheet = createRowValues(sheet, step, "",pageDesc,xpath,keyword,
				   action,flow,value,"","","","","",
				varName,"","",valDevice,"","","",-1,"","","");
		
		   
		   return sheet;
	  }

	private XSSFSheet createDndDrag(XSSFSheet sheet,JSONObject step) {
	   
	   String keyword = "Link";
	   String action = (String)step.get("action");	  
	   String xpath = (String)step.get("click");
	   String value = getValue(sheet, step) ;
	   String varName = getVarName(step);
	   String pageDesc = (String)step.get("pageName");
	   String valDevice = (String)step.get("valDevice");
	   String flow = (String)step.get("Flow");
	   
	   if(step.get("keyword") != null) {
		   keyword = (String)step.get("keyword");
	   }
	   	
	   sheet = createRowValues(sheet, step, "",pageDesc,xpath,keyword,
			   action,flow,value,"","","","","",
			varName,"","",valDevice,"","","",-1,"","","");
	
	   
	   return sheet;
	}
	
	private XSSFSheet createArrow(XSSFSheet sheet,JSONObject step) {
		   
		   String keyword = "Link";
		   String action = (String)step.get("action");	  
		   String xpath = (String)step.get("click");
		   String value = getValue(sheet, step) ;
		   String varName = getVarName(step);
		   String pageDesc = (String)step.get("pageName");
		   String valDevice = (String)step.get("valDevice");
		   String flow = (String)step.get("Flow");
		   
		   if(step.get("keyword") != null) {
			   keyword = (String)step.get("keyword");
		   }
		   	
		   sheet = createRowValues(sheet, step, "",pageDesc,xpath,keyword,
				   action,flow,value,"","","","","",
				varName,"","",valDevice,"","","",-1,"","","");
		
		   
		   return sheet;
		}
	
	private void processApiData(JSONObject step) {
		   
		   String action = (String)step.get("action");	  
		   String request = (String)step.get("request");
		   String responseBody = (String)step.get("responseBody");
		   long pageNumber = (long) step.get("coveragepageno");
		   int pageNo = (int)pageNumber;
		   long actualTime = 0;		  	
		  	if(step.get("actualTime") != null) {
		  		actualTime = (long)step.get("actualTime");
		  	} 
		   
		  	 msc.writeApiDataToDB(request, responseBody, randomKey, actualTime, pageNo);
		}
	
	private XSSFSheet createRightClick(XSSFSheet sheet,JSONObject step) {
		   
		   String keyword = "Element";
		   String action = (String)step.get("action");	  
		   String xpath = (String)step.get("click");
		   String value = getValue(sheet, step) ;
		   String varName = getVarName(step);
		   String pageDesc = (String)step.get("pageName");
		   String valDevice = (String)step.get("valDevice");
		   String flow = (String)step.get("Flow");
		   
		   if(step.get("keyword") != null) {
			   keyword = (String)step.get("keyword");
		   }
		   	
		   sheet = createRowValues(sheet, step, "",pageDesc,xpath,keyword,
				   action,flow,value,"","","","","",
				varName,"","",valDevice,"","","",-1,"","","");
		
		   
		   return sheet;
		}
	   
	private XSSFSheet createFileDownloadRow(XSSFSheet sheet,JSONObject step) {
		   
		   
		   String fullFilename = (String)step.get("File");
		   String filename = fullFilename.substring(fullFilename.lastIndexOf("\\") + 1, 
				   fullFilename.length());
		   
		   String keyword = "File";
		   String action = "Download";
		   String value = filename ;
		   String flow = (String)step.get("Flow");
		   	   
		   sheet = createRowValues(sheet, step, "","Download File","",keyword,
				   action,flow,value,"","","","","",
				"","","","","","","",-1,"","","");
		
		   
		   return sheet;
	  }
	   
	  public XSSFSheet createRowValues(XSSFSheet sheet, JSONObject step, String val1,String val2,
			  String val3, String val4,String val5, String val6,String val7,String val8,
			  String val9, String val10,String val11,String val12,String val13, 
			  String val14, String val15, String valDevice, String filename, 
			  String fileField, String DBUrl, int apiid, String apiparam,
			  String DBQuery, String DBType) {
		  
		  	if(val6 == null || val6.equals("")) {
		  		val6 = "Positive";
		  	}
		  	
		  	int clickable = 0;
		  	if(step.get("clickable") != null) {
		  		clickable = ((Long)step.get("clickable")).intValue();
		  	}
		  	
		  	String issues = (String)step.get("issues");
		  	
		  	if(step.get("keyword") != null && (val4 == null || !val4.equalsIgnoreCase("Assertion"))) {
		  		if(!((String)step.get("keyword")).equals("")) {
		  			val4 = (String)step.get("keyword");
		  		}
			   }
		  	
		  	long actualTime = 0;
		  	
		  	if(step.get("actualTime") != null && !(step.get("actualTime") instanceof String)) {
		  		actualTime = (long)step.get("actualTime");
		  	}
		  	
		  	String eventName = (String)step.get("eventname");
		  	
		  	String iframexpath = (String)step.get("iframepath");
		  	if(iframexpath == null) {
		  		iframexpath = "";
		  	}
		  	String altpath = (String)step.get("altpath");
		  	String outerhtml = (String)step.get("outerhtml");
		  	long tabid = -1;
		  	if(step.get("tabid") != null) {
		  		tabid = (long)step.get("tabid");
		  	}
		  	long windowid = -1;
		  	if(step.get("windowid") != null) {
		  		windowid = (long)step.get("windowid");
		  	}
		  	String type = (String)step.get("type");
		  	
		  	String imgpath = (String)step.get("imgpath");
		  	String bgcolor = (String)step.get("backgroundColor");
		  	String color = (String)step.get("color");
		  	String url = (String)step.get("url");
		  	String pagename = (String)step.get("Currentpagename");
		  	int pageno = 0;
		  	if(step.get("pageno") != null) {
		  		pageno = ((Long)step.get("pageno")).intValue();
		  	}
		  	
		  	boolean uniqueisBackup = false; 
		  	JSONObject uniqueDiscover = (JSONObject)step.get("uniqueobjectforimg");
		  	String uniqueText = null;
	  		String parentToTarget = null;
	  		String uniqueToParent = null;
	  		
	  		if((uniqueDiscover == null || (uniqueDiscover != null && uniqueDiscover.size() == 0)) &&
	  				!(step.get("uninametoele") instanceof String)) {
	  			uniqueDiscover = (JSONObject)step.get("uninametoele");
	  			if(uniqueDiscover != null && uniqueDiscover.size() >= 0) {
	  				uniqueisBackup = true;
	  			}
	  		}
	  		
		  	if(uniqueDiscover != null && uniqueDiscover.size() > 0) {
		  		uniqueText = (String)uniqueDiscover.get("text");
		  		parentToTarget = (String)uniqueDiscover.get("parenttotarget");
		  		uniqueToParent = (String)uniqueDiscover.get("uniquetoparent");
		  	}
		  	
		  	String nearestname = null;
	  		String parenttotarget = null;
	  		String uniquetoparent = null;
	  		long index=-1;
		  	
		  	if(step.get("samerowdata") instanceof JSONObject) {
		  	
		  		JSONObject samerowdata = (JSONObject)step.get("samerowdata");		  		
		  		
			  	if(samerowdata != null && samerowdata.size() > 0) {
			  		nearestname = (String)samerowdata.get("text");
			  		parenttotarget = (String)samerowdata.get("parenttotarget");
			  		uniquetoparent = (String)samerowdata.get("uniquetoparent");
			  		index = (long)samerowdata.get("index");
			  	}
		  	}
		  
		  	if(saveType.equals("Recording")) {
		  		recordingLogic(step, val1,val2,val3, val4, val5, val6, val7, val8, val9, 
		  			  val10, val11, val12, val13, val14, val15, valDevice, 
		  			  filename, fileField, DBUrl, apiid, apiparam, DBQuery,
		  			  DBType, clickable, issues, actualTime,eventName,
		  			  iframexpath, altpath, outerhtml,tabid, type, imgpath, 
		  			  bgcolor, color, url, uniqueText, parentToTarget, uniqueToParent, 
		  			  pagename, pageno, uniqueisBackup, nearestname, parenttotarget, 
		  			  uniquetoparent, index, windowid);
		  	} 
			  
			return sheet;
			  
		  }
	  
	  private void recordingLogic(JSONObject step, String val1,String val2,String val3, String val4,
			  String val5, String val6,String testData,String val8, String val9, 
			  String val10,String val11,String val12,String val13, String val14, 
			  String val15, String valDevice, String filename, String fileField, 
				String DBUrl, int apiid, String apiparam, String DBQuery,
				String DBType, int clickable, String issues, long actualTime,
				String eventName, String iframexpath, String altpath, String outerhtml, 
				long tabid, String type, String imgpath, String bgcolor, String color,
				String url, String uniqueText, String parentToTarget, String uniqueToParent, 
				String pagename, int pageno, boolean uniqueisBackup,  String nearestname, 
				String parenttotarget, String uniquetoparent, long index, long windowid) {
		  
		  boolean mandatoryStep = false;
		  String n_step = null;
		  if(merging) {
			  n_step = "merged";
		  }
		  if(step.containsKey("Mandatorystep")) {
			  mandatoryStep = (boolean) step.get("Mandatorystep");
			  if(mandatoryStep) {
				  n_step = "hover";
			  }
		  }
		  
		  int tsSequence = (int) step.get("tsSequence");
		  if(!merging) {
			  
			  boolean isShadow = false;
			  int shadowIndex = -1;
			  String shadowEle = "";
			  String shadowPath = "";
			  
			  if(step.get("shadow") != null) {
				  isShadow = (boolean)step.get("shadow"); 
			  }				  
			  
			  if(isShadow) {
				  JSONObject shadow = (JSONObject)step.get("shadowelements");
				  if(shadow != null) {
					  shadowIndex = ((Long)shadow.get("index")).intValue();
					  shadowEle = (String)shadow.get("type");
					  shadowPath = (String)shadow.get("path");
				  } else {
					  isShadow = false;
				  }
			  }
			  
			  if(altpath != null && altpath.length() > 500) {
				  altpath = altpath.substring(0,500);
			  }
			  
			  if(val2 != null && val2.length() > 250) {
				  val2 = val2.substring(0,225);
			  }
			  
			  if(nearestname != null && nearestname.length() > 100) {
				  nearestname = nearestname.substring(0,90);
			  }
			  
			  int xpos = 0;
			  int ypos = 0;
			  
			  if(step.get("x") != null && step.get("y") != null) {
				  long xposl = (long) step.get("x");
				  xpos = (int)xposl;
				  long yposl = (long) step.get("y");
				  ypos = (int)yposl;
			  }
			  
			  
			  
			  int testStep = msc.writeTestStepToDB(false,testcaseId, stepId, val1, val2, val3, val4, 
					  val5, val6, testData, val13, valDevice, randomKey, 
					  eventTime, filename, fileField, DBUrl, apiid, 
					  apiparam, DBQuery, DBType, clickable, issues, actualTime, 
					  eventName, iframexpath,altpath, outerhtml,n_step, tabid, type,
					  isShadow,shadowPath,tsSequence, windowid);	
			  
			  if(testStep > -1) {
				  
				  if(uniqueText != null && uniqueText.length() > 100) {
					  uniqueText = uniqueText.substring(0,100);
				  }
				  
				  if(pagename != null && pagename.length() > 45) {
					  pagename = pagename.substring(0,45);
				  }
				  
				  String elementplaceholder = (String)step.get("elementplaceholder");
				  int elementplaceholderindex = -1;
				  
				  
				  if(step.get("elementplaceholderindex") != null && 
						  !(step.get("elementplaceholderindex") instanceof String)) {
					  Long ephi = (Long)step.get("elementplaceholderindex");
					  elementplaceholderindex = ephi.intValue();
				  }
				  
				  
				  JSONObject attribute = (JSONObject)step.get("attribute");
				  String classes = null;
				  if(attribute != null && attribute.get("class") != null) {
					  classes = (String)attribute.get("class");
				  }
				  int classIndex = -1;
				  if(attribute.get("classIndex") != null && 
						  !(attribute.get("classIndex") instanceof String)) {
					  Long classi = (Long)attribute.get("classIndex");
					  classIndex = classi.intValue();
				  }
				  
				  int typeIndex = -1;
				  if(attribute.get("typeIndex") != null && 
						  !(attribute.get("typeIndex") instanceof String)) {
					  Long typei = (Long)attribute.get("typeIndex");
					  typeIndex = typei.intValue();
				  }
				  
				  String href = (String)attribute.get("href");
				  int hrefIndex = -1;
				  if(attribute.get("hrefIndex") != null && 
						  !(attribute.get("hrefIndex") instanceof String)) {
					  Long hrefi = (Long)attribute.get("hrefIndex");
					  hrefIndex = hrefi.intValue();
				  }
				  
				  String dataValue = (String)attribute.get("data-value");
				  int dataValueIndex = -1;
				  if(attribute.get("data-valueIndex") != null && 
						  !(attribute.get("data-valueIndex") instanceof String)) {
					  Long dataValueInd = (Long)attribute.get("data-valueIndex");
					  dataValueIndex = dataValueInd.intValue();
				  }
				 
				  msc.writeTestStepAttrToDB(testStep, randomKey,imgpath,bgcolor, color, url,
						  uniqueText, uniqueToParent, parentToTarget, pagename, pageno,isShadow,
						  shadowEle,shadowIndex,shadowPath,uniqueisBackup, nearestname, parenttotarget, 
						  uniquetoparent, index, xpos, ypos, elementplaceholder, elementplaceholderindex,
						  classes,classIndex,dataValue,dataValueIndex,typeIndex,href,hrefIndex);
			  }
			  
			 
		  } else {
			  
			  long mergetc =  Long.parseLong((String)mergeJsonObject.get("mergetc"));
			  int test_case = (int)mergetc;
			  String mergetcname = (String)mergeJsonObject.get("mergetcname");
			  long mergenextnum = Long.parseLong((String)mergeJsonObject.get("mergestepnum"));
			  int nextStepNum = (int)mergenextnum;

			  msc.addTestStepIncStepnum(test_case, nextStepNum);
			  msc.writeTestStepToDB(true,test_case, nextStepNum, val1, 
					  val2, val3, val4, val5, val6, 
					  testData, val13, valDevice, randomKey,
							eventTime, filename, fileField, 
							DBUrl, apiid, apiparam, DBQuery, DBType,
							clickable, issues, actualTime, eventName,
							iframexpath,altpath, outerhtml,n_step,tabid, 
							type,false,"", nextStepNum,windowid);
			  nextStepNum = nextStepNum + 1;
			  mergenextnum = (long)nextStepNum;
			  mergeJsonObject.put("mergestepnum", String.valueOf(mergenextnum));
		  }
	  }
	  
	  private XSSFSheet createButtonRow(XSSFSheet sheet,JSONObject step) {
		   
		  String keyword = "Button";
		  String action = "Click";		  
		  String xpath = (String)step.get("click");
		  String value = getValue(sheet, step) ; 
		  String varName = getVarName(step);
		  String pageDesc = (String)step.get("pageName");
		  String valDevice = (String)step.get("valDevice");
		  String flow = (String)step.get("Flow");
		   
		   sheet = createRowValues(sheet, step, "",pageDesc,xpath,keyword,
				   action,flow,value,"","","","","",
				 varName,"","",valDevice,"","","",-1,"","","");
		   
		   return sheet;
	  }
	  
	  private XSSFSheet createSubmitRow(XSSFSheet sheet,JSONObject step) {
		   
		  String keyword = "Form";
		  String action = "Submit";		  
		  String xpath = (String)step.get("click");
		  String value = getValue(sheet, step) ; 
		  String varName = getVarName(step);
		  String pageDesc = (String)step.get("pageName");
		  String valDevice = (String)step.get("valDevice");
		  String flow = (String)step.get("Flow");
		   
		   sheet = createRowValues(sheet, step, "",pageDesc,xpath,keyword,
				   action,flow,value,"","","","","",
				 varName,"","",valDevice,"","","",-1,"","","");
		   
		   return sheet;
	  }
	  
	  private XSSFSheet createSelectRow(XSSFSheet sheet,JSONObject step) {
		   
		  String keyword = "DropDown";
		  String action = "Select";		   
		  String xpath = (String)step.get("click");
		  String value = getValue(sheet, step) ;
		  String varName = getVarName(step);
		  String pageDesc = (String)step.get("pageName");
		  String valDevice = (String)step.get("valDevice");
		  String flow = (String)step.get("Flow");
		   
		   sheet = createRowValues(sheet, step, "",pageDesc,xpath,keyword,
				   action,flow,value,"","","","","",
				 varName,"","",valDevice,"", "", 
					"", -1, "", "","");
		   
		   return sheet;
	  }
	    
	  private XSSFSheet createNavigateRow(XSSFSheet sheet,JSONObject step) {

		   String url = (String)step.get("url");
		   String pageDesc = (String)step.get("pageName");
		   String valDevice = (String)step.get("valDevice");
		   sheet = createRowValues(sheet, step, "",pageDesc,"","URL",
					  "Get","",(String)step.get("url"),"","","","","",
					 "","","",valDevice,"","","",-1,"","","");
		   return sheet;
	  }
}
