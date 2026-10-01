package nogrunt;

import org.apache.commons.lang3.StringUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.openqa.selenium.WebDriver;

import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ExecutionUtility {
	
	boolean isMatched;
	
	public int comparexpath(String xpath1, String xpath2) {
		int count = 0;
		String temp1 = "";
		String temp2 = "";
		while(xpath1.contains("/")) {
			if(!xpath2.contains("/")) {
				count += StringUtils.countMatches(xpath1, "/");
				break;
			}
			temp1 = xpath1.substring(0,xpath1.indexOf("/"));
			temp2 = xpath2.substring(0,xpath2.indexOf("/"));
			String index1 = "1";
			String index2 = "1";
			if(temp1.contains("[")) {
				index1 = temp1.substring(temp1.indexOf("[")+1,temp1.indexOf("]"));
				temp1 = temp1.substring(0,temp1.indexOf("["));
			}
			if(temp2.contains("[")) {
				index2 = temp2.substring(temp2.indexOf("[")+1,temp2.indexOf("]"));
				temp2 = temp2.substring(0,temp2.indexOf("["));
			}
			if(!temp1.equalsIgnoreCase(temp2)) {
				count++;
			}
			if(!index1.equals(index2)) {
				count++;
			}
			xpath1 = xpath1.substring(xpath1.indexOf("/")+1);
			xpath2 = xpath2.substring(xpath2.indexOf("/")+1);
		}
		if(xpath2.contains("/")) {
			count += StringUtils.countMatches(xpath2, "/");
		}
		return count;
	}
	
	public int getPassedTcrid(MySQlConn msc, int tsid, int tcid) {
		int passedTcrid = msc.getLatestPassedTestCaseResultsId(tcid);
		if(passedTcrid == -1) {
			passedTcrid = msc.getLatestPassedTestStepResultsId(tsid);
		}
		return passedTcrid;
	}
	
	public void getErrorCode(MySQlConn msc, int tsid, int tcid, String recordedOuterhtml, String pageSource, SelGrid sg) {
		sg.el.logExecution("tcid : " + tcid + " and tsid : " + tsid);
		int ptcrid = getPassedTcrid(msc, tsid, tcid);
		sg.el.logExecution("ptcrid : " + ptcrid + " and tsid : " + tsid);
		String outerhtml = msc.getPassedOuterhtml(ptcrid, tsid);
		if(outerhtml != null && !outerhtml.equals("")) {
			int outerhtmlMatches = StringUtils.countMatches(pageSource, outerhtml);
			if(outerhtmlMatches == 0) {
				sg.errorCode = "E0006";
				sg.runIssues = "Element Not Found - The Element that was found when the test case last passed is also not present in the page.";
			}
			if(outerhtmlMatches > 1) {
	        	sg.errorCode = "E0007";
	        	sg.runIssues = "Element Not Found - The Element that was found when the test case last passed is present multiple times on the page.";
	        }
			if(outerhtmlMatches == 1) {
	        	sg.errorCode = "E0008";
	        	sg.runIssues = "Element Not Found - The Element that was found when the test case last passed is present only once on the page.";
	        }
		}
		if(outerhtml == null || outerhtml.equals("")) {
			outerhtml = recordedOuterhtml;
			sg.el.logExecution("Recorded Outer html found is :" + outerhtml);
			int outerhtmlMatches = StringUtils.countMatches(pageSource, outerhtml);
			if(outerhtmlMatches == 0) {
				sg.errorCode = "E0009";
				sg.runIssues = "Element Not Found - Neither are the Element when the test case last passed nor the Element that was recorded are found in the page.";
			}
			if(outerhtmlMatches > 1) {
	        	sg.errorCode = "E0010";
	        	sg.runIssues = "Element Not Found - The Element that was Recorded was found multiple times in the page.";
	        }
			if(outerhtmlMatches == 1) {
	        	sg.errorCode = "E0011";
	        	sg.runIssues = "Element Not Found - The Element that was Recorded was found only once on the page.";
	        }
		}
	}
	
	public boolean comparePassedOuterHTML(MySQlConn msc, int tsid, int tcid, String foundOuterhtml, String recordedOuterhtml, 
			String pageSource, SelGrid sg, boolean[] flag, String foundby) {
		
		sg.el.logExecution("tcid : " + tcid + " and tsid : " + tsid);
		int ptcrid = getPassedTcrid(msc, tsid, tcid);
		sg.el.logExecution("ptcrid : " + ptcrid + " and tsid : " + tsid);
		String outerhtml = msc.getPassedOuterhtml(ptcrid, tsid);
		String passedFoundby = msc.getPassedFoundBy(ptcrid, tsid);
		sg.el.logExecution("Passed Location Strategy : " + passedFoundby);
		sg.el.logExecution("Current Location Strategy : " + foundby);
		sg.el.logExecution("Last Passed Outer html found is :" + outerhtml);
		if(outerhtml != null && !outerhtml.equals("")) {
			int outerhtmlMatches = StringUtils.countMatches(pageSource, outerhtml);
			if(outerhtmlMatches == 0) {
				sg.errorCode = "E0001";
				sg.runIssues = "Element Not Found - The element had been found, when the test case had passed recently";
			}
			if(outerhtmlMatches > 1) {
	        	sg.errorCode = "E0002";
	        	sg.runIssues = "The Element Occurs Multiple times on the page - potential for Incorrect Element being found";
	        }
		}
		if(outerhtml == null || outerhtml.equals("")) {
			outerhtml = recordedOuterhtml;
			sg.el.logExecution("Recorded Outer html found is :" + outerhtml);
			int outerhtmlMatches = StringUtils.countMatches(pageSource, outerhtml);
			if(outerhtmlMatches == 0) {
				sg.errorCode = "E0003";
				sg.runIssues = "Element Not Found - The Recorded element had been found, when the test case had passed recently";
			}
			if(outerhtmlMatches > 1) {
	        	sg.errorCode = "E0002";
	        	sg.runIssues = "The Element Occurs Multiple times on the page - potential for Incorrect Element being found";
	        }
		}
		if(outerhtml != null && !outerhtml.equals("") && !outerhtml.equals(foundOuterhtml)) {
			int outerhtmlMatches = StringUtils.countMatches(pageSource, outerhtml);
			if(outerhtmlMatches == 1) {
				sg.errorCode = "E0005";
				sg.runIssues = "Element Occurs once on Page - But the Locators failed to locate the Element";
				
				String tag1 = null;
				Pattern pattern = Pattern.compile("<\\s*(\\w+)");
		        Matcher matcher1 = pattern.matcher(outerhtml);

		        if (matcher1.find()) {
		            tag1 = matcher1.group(1);
		        }
		        
		        String tag2 = null;
		        Matcher matcher2 = pattern.matcher(foundOuterhtml);

		        if (matcher2.find()) {
		            tag2 = matcher2.group(1);
		        }
		        
		        if(tag1 != null && tag2 != null && !tag1.equalsIgnoreCase(tag2)) {
		        	flag[1] = !flag[1];
		        }
				flag[0]=!flag[0];
			}
			return true;
		}
		return false;
	}
	
	public void getAllAttributes(HashMap attr, String outerhtml) {
		HtmlAttributes attributes = new HtmlAttributes();
		attributes.getAttr(outerhtml, attr);
	}
	
	public boolean attrWiseCompare(MySQlConn msc, int stepnum, int tsid, int tcid, String outerhtml1, String recordedOuterhtml, SelGrid sg, ExecutionLogger el) {
		int ptcrid = getPassedTcrid(msc, tsid, tcid);
		String outerhtml2 = msc.getPassedOuterhtml(ptcrid, tsid);
		if(outerhtml2 == null || outerhtml2.equals("")) {
			outerhtml2 = recordedOuterhtml;
		}
		el.logExecution("Step " + stepnum);
		el.logExecution("Previous Outer HTML: " + outerhtml2);
		el.logExecution("--------------------------------------------------------------------------------------------");
		el.logExecution("Current Outer HTML: " + outerhtml1);
		el.logExecution("xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx");
		HashMap attr1 = new HashMap();
		HashMap attr2 = new HashMap();
	    getAllAttributes(attr1, outerhtml1);
	    getAllAttributes(attr2, outerhtml2);
	    isMatched = true;
	    String approvedAttr = msc.getApprovedAttr(tsid);
	    compareOuterhtml(attr1, attr2, approvedAttr, sg, el);
	    return isMatched;
	}
	
	public void compareOuterhtml(HashMap attributes1, HashMap attributes2, String approvedAttr, SelGrid sg, ExecutionLogger el) {
		if(attributes1.containsKey("text")) {
			String text1 = (String) attributes1.get("text");
			el.logExecution("Current text: " + text1);
			if(attributes2.containsKey("text")) {
				String text2 = (String) attributes2.get("text");
				el.logExecution("Previous text: " + text2);
				if(!text1.equals(text2) && !approvedAttr.contains("text")) {
//					el.logExecution("Previous element " +  "text : " + text2);
//                    el.logExecution("Current element " +  "text : " + text1);
					sg.nonMatchingAttr += "text, ";
					isMatched = false;
				} else {
					el.logExecution("Text matched");
				}
			} else {
				if(!approvedAttr.contains("text")) {
					sg.nonMatchingAttr += "text, ";
					isMatched = false;
				}
			}
		}
		for (Object obj : attributes1.entrySet()) {
            Map.Entry entry = (Map.Entry) obj;
            if(entry.getKey().equals("text") || entry.getKey().equals("nested")) {
            	continue;
            }
            String value1 = (String) entry.getValue();
            el.logExecution("Current " + entry.getKey() + ": " + value1);
            if(attributes2.containsKey(entry.getKey())) {
            	String value2 = (String) attributes2.get(entry.getKey());
            	el.logExecution("Previous " + entry.getKey() + ": " + value2);
            	if(!value1.equals(value2) && !approvedAttr.contains((String) entry.getKey())) {
//                    el.logExecution("Previous element " + entry.getKey() + " : " + value2);
//                    el.logExecution("Current element " + entry.getKey() + " : " + value1);
            		sg.nonMatchingAttr += entry.getKey() + ", ";
            		isMatched = false;
            	} else {
            		el.logExecution(entry.getKey() + " matched");
            	}
            } else {
            	if(!approvedAttr.contains((String) entry.getKey())) {
            		sg.nonMatchingAttr += entry.getKey() + ", ";
            		isMatched = false;
            	}
            }
            System.out.println(entry.getKey() + " : " + entry.getValue());
        }
		if(attributes1.containsKey("nested")) {
			HashMap nested1 = (HashMap) attributes1.get("nested");
			if(attributes2.containsKey("nested")) {
				HashMap nested2 = (HashMap) attributes2.get("nested");
				compareOuterhtml(nested1, nested2, approvedAttr, sg, el);
			} else {
				if(!approvedAttr.contains("nested")) {
					sg.nonMatchingAttr += "nested, ";
					isMatched = false;
					//return false;
				}
			}
		}
		//return true;
	}
	
	public boolean compareExecutionLogs(MySQlConn msc, int tcrid, int stepnum, int tcid, int tsid, 
			boolean canContinue, String pageSource, SelGrid sg, String outerhtml, ExecutionLogger el) {
		
		int passedTcrid = msc.getLatestPassedTestCaseResultsId(tcid);
		if(passedTcrid == -1) {
			passedTcrid = msc.getLatestPassedTestStepResultsId(tsid);
		}
		//List<Integer> tcrid = msc.getLatestTestCaseResultsId(stepnum);
		
		JSONArray json1 = msc.getFromTestCaseExecutionLog(tcrid);
		JSONArray json2 = msc.getFromTestCaseExecutionLog(passedTcrid);
		
		List<String> stepwise1 = new ArrayList<>();
		String temp = "";
		for(Object obj : json1) {
			JSONObject json = (JSONObject) obj;
			String message = (String) json.get("message");
			if(message.equals("Starting Steps")) {
				temp = "";
			}
			temp += message + "\n";
			if(message.equals("----------------------------------------------------------------------------------------------------")) {
				stepwise1.add(temp);
				temp = "";
			}
		}
		stepwise1.add(temp);
		
		List<String> stepwise2 = new ArrayList<>();
		temp = "";
		for(Object obj : json2) {
			JSONObject json = (JSONObject) obj;
			String message = (String) json.get("message");
			temp += message + "\n";
			if(message.equals("Starting Steps")) {
				temp = "";
			}
			if(message.equals("----------------------------------------------------------------------------------------------------")) {
				stepwise2.add(temp);
				temp = "";
			}
		}
		stepwise2.add(temp);
		
		int n = stepwise1.size();
		int m = stepwise2.size();
		int i = 0;
		
		while(i<n && i<m) {
			
			if(!stepwise1.get(i).equals(stepwise2.get(i))) {
				
				String stepLogs1 = stepwise1.get(i);
				String stepLogs2 = stepwise2.get(i);
				
				int stepNumber = -1;
				Pattern patternStep = Pattern.compile("\"Step_Number\":(\\d+)");
		        Matcher matcherStep = patternStep.matcher(stepLogs1);
		        if (matcherStep.find()) {
		            stepNumber = Integer.parseInt(matcherStep.group(1));
		        }
				
				Pattern patternOuterHTML = Pattern.compile("Outer HTML of element found: ([^\\n\\r]+)");
		        Matcher matcherOuterHTML1 = patternOuterHTML.matcher(stepLogs1);
		        Matcher matcherOuterHTML2 = patternOuterHTML.matcher(stepLogs2);
		        
		        String outerHTML1 = "";
		        String outerHTML2 = "";
		        
		        if(matcherOuterHTML1.find()) {
		        	outerHTML1 = matcherOuterHTML1.group(1).trim();
		        	el.logExecution("Step " + stepNumber + "Current Outer HTML: " + outerHTML1);
		        }
		        el.logExecution("---------------------------------------------------------------------------------------------");
		        if(matcherOuterHTML2.find()) {
		        	outerHTML2 = matcherOuterHTML2.group(1).trim();
		        	el.logExecution("Step " + stepNumber + "Previous Outer HTML: " + outerHTML2);
		        }
		        el.logExecution("---------------------------------------------------------------------------------------------");
		        if(outerHTML2.equals("")) {
		        	outerHTML2 = outerhtml;
		        }
		        int outerHTMLMatches = StringUtils.countMatches(pageSource, outerHTML2);
		        
		        if(outerHTMLMatches == 0 && (stepNumber == stepnum)) {
		        	sg.errorCode = "E0001";
		        	sg.runIssues = "Element Not Found - The element had been found, when the test case had passed recently";
		        }
		        if(outerHTMLMatches > 1 && (stepNumber == stepnum)) {
		        	sg.errorCode = "E0002";
		        	sg.runIssues = "The Element Occurs Multiple times on the page - potential for Incorrect Element being found";
		        }
		        
		        if(!outerHTML1.equals(outerHTML2)) {
		        	canContinue = true;
		        }
			}
			i++;
		}
		return canContinue;
	}
	
	
	public void getPageSource(WebDriver driver, String filename) {
		try {
			String pageSource =  driver.getPageSource();
			try{
				Utilities.writeDataToFile(filename, pageSource);
			} catch(Exception e) {
				e.printStackTrace();
			}
		} catch(Exception e) {
			
		}
	}
	
	
	
    public boolean compareOuterHTML(String actual, String recorded, ExecutionLogger el) {
    	
    	el.logExecution("Actual outer HTML: " + actual);
    	
    	el.logExecution("Recorded outer HTML: " + recorded);
    	
    	Pattern tagPattern = Pattern.compile("<(\\w+)");
        Matcher actualTagMatcher = tagPattern.matcher(actual);
        Matcher recordedTagMatcher = tagPattern.matcher(recorded);
    	
        Set<String>actualTags = new HashSet<>();
        while(actualTagMatcher.find()) {
        	actualTags.add(actualTagMatcher.group(1));
        }
        
        el.logExecution("Actual Tagnames: " + actualTags);
        
        Set<String>recordedTags = new HashSet<>();
        while(recordedTagMatcher.find()) {
        	recordedTags.add(recordedTagMatcher.group(1));
        }
        el.logExecution("Recorded Tagnames: " + recordedTags);
        
        if(!(actualTags.containsAll(recordedTags) || recordedTags.containsAll(actualTags))) {
        	return false;
        }
        
    	if(actual.contains("type")) {
    		Pattern pattern = Pattern.compile("type=\"(.*?)\"");
	        Matcher matcher = pattern.matcher(actual);
	        
	        if (matcher.find()) {
	        	el.logExecution("Actual Type Value: " + matcher.group(1));
		        Matcher recordedMatcher = pattern.matcher(recorded);
		        
		        if(recordedMatcher.find()) {
		        	el.logExecution("Recorded Type Value: " + recordedMatcher.group(1));
		        	if(!matcher.group(1).equals(recordedMatcher.group(1))) {
//		        		return false;
		        	}
		        } else {
//		        	return false;
		        }
	        }
    	}
    	
        if(actual.contains("alt")) {
        	Pattern pattern = Pattern.compile("alt=\"(.*?)\"");
	        Matcher matcher = pattern.matcher(actual);
	        
	        if (matcher.find()) {
	        	el.logExecution("Actual Alt Value: " + matcher.group(1));
		        Matcher recordedMatcher = pattern.matcher(recorded);
		        
		        if(recordedMatcher.find()) {
		        	el.logExecution("Recorded Alt Value: " + recordedMatcher.group(1));
		        	if(!matcher.group(1).equals(recordedMatcher.group(1))) {
//		        		return false;
		        	}
		        } else {
//		        	return false;
		        }
	        }
        }
        
        if(actual.contains("src")) {
        	Pattern pattern = Pattern.compile("alt=\"(.*?)\"");
	        Matcher matcher = pattern.matcher(actual);
	        
	        if (matcher.find()) {
	        	el.logExecution("Actual src Value: " + matcher.group(1));
		        Matcher recordedMatcher = pattern.matcher(recorded);
		        
		        if(recordedMatcher.find()) {
		        	el.logExecution("Recorded Src Value: " + recordedMatcher.group(1));
		        	if(!matcher.group(1).equals(recordedMatcher.group(1))) {
//		        		return false;
		        	}
		        } else {
//		        	return false;
		        }
	        }
        }
        
        if(actual.contains("href")) {
        	Pattern pattern = Pattern.compile("class=\"(.*?)\"");
	        Matcher matcher = pattern.matcher(actual);
	        
	        if (matcher.find()) {
	        	el.logExecution("Actual href Value: " + matcher.group(1));
		        Matcher recordedMatcher = pattern.matcher(recorded);
		        
		        if(recordedMatcher.find()) {
		        	el.logExecution("Recorded href Value: " + recordedMatcher.group(1));
		        	if(!matcher.group(1).equals(recordedMatcher.group(1))) {
//		        		return false;
		        	}
		        } else {
//		        	return false;
		        }
	        }
        }
        
        if(actual.contains("class")) {
        	Pattern pattern = Pattern.compile("class=\"(.*?)\"");
	        Matcher matcher = pattern.matcher(actual);
	        
	        if (matcher.find()) {
	        	el.logExecution("Actual Class Value: " + matcher.group(1));
	        	recorded = recorded.replaceAll("\\s?ant-tooltip-open", "");
		        Matcher recordedMatcher = pattern.matcher(recorded);
		        
		        if(recordedMatcher.find()) {
		        	el.logExecution("Recorded Class Value: " + recordedMatcher.group(1));
		        	if(!matcher.group(1).equals(recordedMatcher.group(1))) {
//		        		return false;
		        	}
		        } else {
//		        	return false;
		        }
	        }
        }
        
        
    	return true;
    }
    
}
