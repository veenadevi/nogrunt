package nogrunt;

import nogrunt.integrations.AdbSmsRetriever;
import nogrunt.integrations.GoogleSheets;
import org.apache.commons.lang3.StringUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.FileReader;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ProcessTestData {

    HashMap<String, String> randomMap = new HashMap<>();

    ExecutionLogger el;

    ConcurrentHashMap<String, String> varMap = null;

    int xlsColCount = -1;

    int xlsRowCount = -1;

    int mrFieldIndex = 0;

    int mrIndex = -1;

    boolean indexUsed = false;

    public String processTestData(String testData, JSONObject step, String varName,
                                  int testCaseId, int companyId, int runonceIndex, MySQlConn msc, String randomKey,
                                  String gendateoffset, SelGrid sg, ExecutionLogger el)
            throws Exception {
    	
        String source = (String) step.get("testdata_source");
        String action = (String) step.get("Action");
        String keyword = (String) step.get("Keyword");
        String ValDevice = (String) step.get("ValDevice");

        if (step.get("Keyword") != null && ((String) step.get("Keyword")).equals("DropDown")) {
            return testData;
        } else if (source.equalsIgnoreCase("AsRecorded") || source.equalsIgnoreCase("IsAVar")
                || source.equalsIgnoreCase("GenData") || source.equalsIgnoreCase("UseVariableFromTheScenario")
                || source.equalsIgnoreCase("DependentonOtherTestScenario") || source.equalsIgnoreCase("Search")
                || source.equalsIgnoreCase("SameRow")) {
            testData = sg.defaultTestData(testData, AppProperties.chatgpton, varName,
                    (String) step.get("Keyword"), action, gendateoffset);

            if ((StringUtils.countMatches(testData, AppProperties.delimiter) >= 2)) {
                Iterator iter = randomMap.keySet().iterator();
                while (iter.hasNext()) {
                    String key = (String) iter.next();
                    if (testData.contains(key)) {
                        testData = testData.replace(key, (String) randomMap.get(key));
                    }

                    if (StringUtils.countMatches(testData, AppProperties.delimiter) < 2) {
                        break;
                    }
                }
            }

            return testData;
        } else if ((source.equalsIgnoreCase("FromFile") && !action.equals(AppProperties.UPLOAD))) {
            String filename = (String) step.get("filename");
            String fieldname = (String) step.get("fileField");
            filename = Utilities.getInputFilePath(companyId, testCaseId, filename);
            String scope = (String) step.get("scope");
            return processFileTypes(filename, fieldname, testCaseId, companyId,
                    runonceIndex, el, scope, sg);
        } else if (source.equalsIgnoreCase("FromDB")) {
            String query = (String) step.get("DBQuery");
            GetUserDbConn udb = new GetUserDbConn();
            int tsid = (int) step.get("idtest_step");
            return udb.getCustomerDbConn(msc, query, tsid, el);
        } else if (source.equalsIgnoreCase("GoogleSheet") && !action.equals(AppProperties.UPLOAD)) {
            String spreadsheetId = (String) step.get("filename");
            String range = (String) step.get("fileField");
            range = range.replace(AppProperties.testdatadelimiter, AppProperties.gsheetdelimiter);
            try {
                return GoogleSheets.getCellValue(spreadsheetId, range, companyId);
            } catch (Exception e) {
                el.logExecution(e);
            }
        } else if (source.equalsIgnoreCase("FromApi") &&
                (!((keyword.equalsIgnoreCase("Api") && action.equalsIgnoreCase("test"))))) {
//            SelGrid sg = new SelGrid();
            String res = sg.callAPI(step, randomKey, source);
            return res;
        } else if (source.equalsIgnoreCase("FromEmail")) {
            String email = (String) step.get("customerEmail");
            String pwd = (String) step.get("customerPassword");
            String select = (String) step.get("EmailSelectionCriteria");
            String filter = (String) step.get("EmailFilter");
            int tsid = (int) step.get("idtest_step");
            MailAccess ma = new MailAccess();
            return ma.getFromEmail(tsid, email, pwd, select, filter, el, msc);
        } else if (source.equalsIgnoreCase("FromOutlook")) {
            String select = (String) step.get("EmailSelectionCriteria");
            String filter = (String) step.get("EmailFilter");
            return ApiAccess.callApiForDesktop(AppProperties.outlookaccessurl, select,
                    filter, el, varMap);
        } else if (source.equalsIgnoreCase("FromReferenceFile")) {
            String filename = (String) step.get("filename");
            String fieldname = (String) step.get("fileField");
            int testStepId = (int) step.get("idtest_step");
            int prodid = msc.getProdIdFromTestStepId(testStepId);
            filename = Utilities.getProductInputFile(companyId, prodid, testStepId, filename);
            String scope = (String) step.get("scope");
            return processFileTypes(filename, fieldname, testCaseId, companyId,
                    runonceIndex, el, scope, sg);
        } else if (source.equalsIgnoreCase("fromSMS")) {
            AdbSmsRetriever smsi = new AdbSmsRetriever();
            List<String> smss = smsi.retrieveSms();
            String sms = smss.get(0);
            Pattern pattern = Pattern.compile("body=(.+?), [a-z_]+=");
//            Pattern pattern = Pattern.compile("body=([^,]+)");
            Matcher matcher = pattern.matcher(sms);

            if (matcher.find()) {
                String body = matcher.group(1).trim();

//                Pattern otpPattern = Pattern.compile((String) step.get("regex"));
             String regexString = (String) step.get("regex");
             String actualRegex = regexString.replace("\\\\", "\\");
             Pattern otpPattern = Pattern.compile(actualRegex);
                Matcher otpmatcher = otpPattern.matcher(body);

                if (otpmatcher.find()) {
                    testData = otpmatcher.group();
                    System.out.println("Extracted OTP: " + testData);
                } else {
                    System.out.println("No OTP found.");
                }
                System.out.println("Extracted SMS Body: " + body);
            } else {
                System.out.println("No SMS body found.");
            }
        }
        return testData;
    }

    public String processFileTypes(String filename, String fieldname, int testCaseId,
                                   int companyId, int runonceIndex, ExecutionLogger el, String scope, SelGrid sg) {

        if (filename.endsWith(".json")) {
            return getFromJSONFile(filename, fieldname, testCaseId, companyId);
        } else if (filename.endsWith(".xlsx")) {
            if (scope != null && scope.equals("Multi Run")) {
                indexUsed = true;
            } else {
                indexUsed = false;
            }
//            SelGrid sg = new SelGrid();
            return sg.getFromXLSXFile(filename, fieldname, testCaseId, companyId, runonceIndex,
                    indexUsed, el);
        } else if (filename.endsWith(".xls")) {
            if (scope != null && scope.equals("Multi Run")) {
                indexUsed = true;
            } else {
                indexUsed = false;
            }
//            SelGrid sg = new SelGrid();
            return sg.getFromXLSFile(filename, fieldname, testCaseId, companyId, runonceIndex,
                    indexUsed, el);
        }

        return null;
    }

    public String getFromJSONFile(String filename, String fieldname, int testCaseId, int companyId) {
//        filename = Utilities.getInputFilePath(companyId, testCaseId,  filename);

        // To create JSON Parser Object
        JSONParser jsp = new JSONParser();
        try {
            FileReader reader = new FileReader(filename);
            JSONObject rootObj = (JSONObject) jsp.parse(reader);
            return processJSON(rootObj, fieldname, true);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public String processJSON(JSONObject jsonO, String fieldname, boolean firstTime) {
        String tempfieldName = fieldname;
        String name = "";
        int index = -1;
        String fullName = name;

        if (tempfieldName.contains(".")) {
            name = tempfieldName.substring(0, tempfieldName.indexOf("."));
            fullName = name;
            if (name.contains("[") && name.contains("]")) {
                String indexStr = name.substring(name.indexOf("[") + 1, name.indexOf("]"));
                index = new Integer(indexStr).intValue();
                name = name.substring(0, name.indexOf("["));
            }
        } else {
            name = tempfieldName;
            fullName = name;
        }

        int count = StringUtils.countMatches(tempfieldName, ".");

        JSONArray ja = null;
        JSONObject jo = null;

        Object rootObj = jsonO.get(name);

        if (count > 0) {
            ja = (JSONArray) rootObj;
            jo = (JSONObject) ja.get(index);
        }

        if (tempfieldName.contains(".")) {
            tempfieldName = tempfieldName.substring(fullName.length() + 1, tempfieldName.length());
            return processJSON(jo, tempfieldName, false);
        }

        return rootObj.toString();
    }
}
