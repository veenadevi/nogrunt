package nogrunt.nlp;

import nogrunt.AppProperties;
import nogrunt.MySQlConn;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.JSONArray;

import java.util.Iterator;;

public class CreateNLPSteps {
	
	public void saveNLPStep(int tcid, String stepsInfo, MySQlConn msc, long randomKey) {
		
		 String[] steps = stepsInfo.split("\n");
		 
		 for(int i=0; i<steps.length;i++) {
			 String step = steps[i];
			 step = step.trim();
			 
			 boolean startsWithEnter = step.trim().toLowerCase().startsWith("enter".toLowerCase());
			 boolean startsWithURL = step.trim().toLowerCase().startsWith("url".toLowerCase());
			 boolean startsWithClick = step.trim().toLowerCase().startsWith("click".toLowerCase());
			 boolean startsWithSelect = step.trim().toLowerCase().startsWith("select".toLowerCase());
			 boolean startsWithHover = step.trim().toLowerCase().startsWith("hover".toLowerCase());
			 
			 String xpath= "";
			 String testData = "";
			 String keyword = "";
			 String action = "";
			 String testdatasource = "";
			 String type = null;
			 String altpath = "";
			 
			 String nlpTestData = extractBetween(step, "~~", "~~");
			 String tempStep = step.replaceAll("~~.*?~~", "");
			 String nlpElement = extractBetween(tempStep, "~", "~");
			 String nlpCloseBy = "";
		     
		     xpath = nlpElement;
			 
//			 if(step.contains(AppProperties.delimiter)) {
//				 testData = step.substring(step.indexOf(AppProperties.delimiter) + 1, step.length());
//				 testData = testData.substring(0,testData.indexOf(AppProperties.delimiter));
//			 }
			 
		     if (xpath != null && !xpath.isEmpty()) {
	    	    if (xpath.contains("'")) {
	    	        xpath = "//*[translate(text(), 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz') = \"" + xpath.toLowerCase() + "\"]";
	    	    } else {
	    	        xpath = "//*[translate(text(), 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz') = '" + xpath.toLowerCase() + "']";
	    	    }
		    }
		     
			 if(startsWithURL) {
				 keyword = "URL";
				 action = "Get";
				 xpath= "";
				 if (nlpTestData != null) {
		            testData = nlpTestData;
			     }
			 } else if(startsWithEnter) {	 
//				 xpath = step.substring(step.indexOf("into ") + 5, step.length());
//				 xpath = xpath = "//*[text()='" + xpath + "']";
				 
				 keyword = "EditBox";
				 action = "Enter";
				 type = "text";
				 if (nlpTestData != null) {
		            testData = nlpTestData;
			     }
			 } else if (startsWithClick) {
//				 xpath = step.substring(step.indexOf("Click ") + 6, step.length());
//				 xpath = xpath = "//*[text()='" + xpath + "']";
				 
				 keyword = "Link";
				 action = "Click";	
				 if (nlpTestData != null) {
		            testData = nlpTestData;
		            testdatasource = "SameRow";
		            nlpCloseBy = nlpTestData;
		            nlpTestData = "";
			     }
				 else {
					 testData = nlpElement;
					 altpath = "//input[translate(@placeholder, 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz') = '" + nlpElement.toLowerCase() + "']";
				 }	 
			 } else if (startsWithSelect) {
//				 xpath = step.substring(step.indexOf("for ") + 4, step.length());
//				 xpath = xpath = "//*[text()='" + xpath + "']";
				 
				 keyword = "DropDown";
				 action = "Select";
				 if (nlpTestData != null) {
					 nlpCloseBy = nlpTestData;
					 nlpTestData = "";
			     }
				 
			 } else if (startsWithHover) {
//				 xpath = xpath = "//*[text()='" + xpath + "']";
				 keyword = "Mouse";
				 action = "hover";
				 if (nlpTestData != null) {
					 nlpCloseBy = nlpTestData;
					 nlpTestData = "";
			     }
			 }
			 
			 int test_step = msc.insertIntoTestSteps(tcid, i, "", step,
					  xpath, keyword, action, "Positive", testData, "", "", randomKey,0.0, "", "", 
						"", -1, "", "", "", 1, "", 0, "","",altpath, "","nlp",1,type, i,1);
			 
			 if (testdatasource.equals("SameRow")) {
				 String elementType = AppProperties.delimiter + nlpElement + AppProperties.delimiter;
				 msc.updateTestStepFindElementByData(test_step, testdatasource, elementType,
	                     1, testdatasource);
			 }
			
			 
			 msc.writeNlpStepAttributesToDB(test_step, action, nlpElement, nlpTestData, nlpCloseBy);
		 }
		
	}
	
	private String extractBetween(String text, String start, String end) {
	    int startIdx = text.indexOf(start);
	    int endIdx = text.indexOf(end, startIdx + start.length());

	    if (startIdx != -1 && endIdx != -1 && endIdx > startIdx) {
	        return text.substring(startIdx + start.length(), endIdx).trim();
	    }
	    return null;
	}
	
	public JSONObject createNLPTestCase(String testCase, String xlsLoc, 
			int version, String createdBy, String saveType, 
			long randomKey, int moduleId, int width, int height, 
			String steps, MySQlConn msc, int companyId) {
		
		int tcid = -1111;
		JSONObject json = new JSONObject();
		
		if(steps != null && steps.toLowerCase().startsWith("given")) {
			CreateGherkinTestCases cgtc = new CreateGherkinTestCases();
			json =  cgtc.saveNLPStep(companyId, tcid, steps, msc);
			json.put("testcasename", testCase);
		} else {
			tcid = msc.checkAndWriteTestCaseToDB(testCase, xlsLoc, 
					version, createdBy, saveType, randomKey, moduleId, 
					width, height);
			
			tcid = msc.createNLPTestCase(tcid, steps);
			CreateNLPSteps cnlps = new CreateNLPSteps();
			cnlps.saveNLPStep(tcid, steps, msc, randomKey);
		}
		
		json.put("id", tcid);
		return json;
	}
	
	public int writeTctoDB(String testCase, String xlsLoc, 
			int version, String createdBy, String saveType, 
			long randomKey, int moduleId, int width, int height, 
			String steps, MySQlConn msc) {
		
		int tcid = msc.checkAndWriteTestCaseToDB(testCase, xlsLoc, 
				version, createdBy, saveType, randomKey, moduleId, 
				width, height);
		
		tcid = msc.createNLPTestCase(tcid, steps);
		CreateNLPSteps cnlps = new CreateNLPSteps();
		if(steps instanceof String) {
			JSONParser  parser = new JSONParser();
			JSONObject stepsjson = null;
			try {
				stepsjson = (JSONObject)parser.parse(steps);
				if(stepsjson.get("nlp") instanceof String) {
					steps = (String)stepsjson.get("nlp");
					stepsjson = (JSONObject)parser.parse(steps);
				} else if(stepsjson.get("nlp") instanceof JSONObject){
					stepsjson = (JSONObject)stepsjson.get("nlp");
				}
			} catch (Exception e) {
				e.printStackTrace();
			}			
			
			JSONArray jsonArray =  new JSONArray();
			Iterator iter = stepsjson.keySet().iterator();
			String result = "";
			int index = 1;
//			while (iter.hasNext()) {
			while (index <= stepsjson.size()) {
//				String key = (String)iter.next();
				String key = String.valueOf(index);
	            String value = (String)stepsjson.get(key);
	            value = value.trim();
	            if(value.contains("\"")) {
	            	if(value.startsWith("\"")) {
	            		value = value.substring(value.indexOf("\"") + 1, value.length());
	            	}
	            }
	            if(value.contains("\"")) {
	            	if(value.endsWith("\"")) {
	            		value = value.substring(0,value.indexOf("\""));
	            	}
	            }
	            jsonArray.add(value); // Add each value to the JSONArray
	            if(result.equals("")) {
	            	result = value;
	            } else {
	            	result = result + "\n" + value;
	            }
	            
	            index = index + 1;
	        }
			steps = result;
		}
		cnlps.saveNLPStep(tcid, steps, msc, randomKey);
		return tcid;
	}
	
	public JSONArray writeTctoDB(JSONArray ja, MySQlConn msc, 
			String randomKey, int moduleId, String uname) {
		for(int i=0;i<ja.size(); i++) {
			JSONObject json = (JSONObject)ja.get(i);
			String testCase = (String)json.get("device")+"-"+(String)json.get("persona")+"-tcid"+(Long)json.get("tcid");
			String steps = (String)json.get("automationScripts");
			long rkL = Long.valueOf(randomKey);
			int tcid = writeTctoDB(testCase, "",1, uname, "recording", 
					rkL, moduleId, -1,-1,steps,msc);
			json.put("status", "Success - TestCase Id - " + String.valueOf(tcid) + "- Test Case Name :" + testCase);
		}
		return ja;
	}
	
	public JSONArray getNLPTestCase(JSONArray jsonArr, MySQlConn msc,int companyId) {
		
		for (int i=0; i< jsonArr.size(); i++) {
			JSONObject tc = (JSONObject)jsonArr.get(i);
			String gherkin = (String)tc.get("gherkin");
//			gherkin = "given I am successfully logged in at the url \"https://test.acviss.co/dashboard/login/\" with username as \"Demo\" and pwd as \"Demo\" \r\n"
//					+ "when I delete product \"none\"\r\n"
//					+ "then on searching for \"none\" on the product details page - the product should no longer be found";
			Long tcidL = (Long)tc.get("tcid");
			int tcid = tcidL.intValue();
			CreateGherkinTestCases cgtc = new CreateGherkinTestCases();
			JSONObject json =  cgtc.saveNLPStep(companyId, tcid, gherkin, msc);
			String str = json.toJSONString();
			tc.put("automationScripts",str);
		}
		return jsonArr;
	}
	
	 public static void main(String[] args) {
		CreateNLPSteps cnlps = new CreateNLPSteps();
//		MySQlConn msc= new MySQlConn(null);
//		cnlps.writeTctoDB("web-company admin-tcid1","",1,"Deva","recording",
//				4748335,54,-1,-1,"{\"nlp\":{\"11\":\"Enter Number of Codes\",\"12\":\"Select Verify Hologram Color\",\"13\":\"Click Submit\",\"1\":\"Enter ~company admin username~ into Username\",\"2\":\"Enter ~company admin password~ into Password\",\"3\":\"Click Login\",\"4\":\"Click Dashboard\",\"5\":\"Click Code Management\",\"6\":\"Click Generate Codes\",\"7\":\"Select Type of Code\",\"8\":\"Select MI10 Set Name\",\"9\":\"Enter Batch Name\",\"10\":\"Enter Batch Code\"}}",
//				null);
		
//		String gherkin = "given I am successfully logged in at the url \"https://test.acviss.co/dashboard/login/\" with username as \"Demo\" and pwd as \"Demo\" \r\n"
//				+ "when I delete product \"none\"\r\n"
//				+ "then on searching for \"none\" on the product details page - the product should no longer be found";
	}

}
