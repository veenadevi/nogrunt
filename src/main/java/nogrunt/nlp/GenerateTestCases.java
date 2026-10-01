package nogrunt.nlp;

import nogrunt.MySQlConn;
import nogrunt.Utilities;
import nogrunt.integrations.*;
import nogrunt.integrations.llm.openaiclient.Main;
import nogrunt.integrations.llm.openaiclient.dto.MessageResponseDTO;
import nogrunt.integrations.llm.openaiclient.dto.MessagesListResponseDTO;

import java.util.List;

import org.json.simple.JSONObject;
import org.json.simple.JSONArray;
import org.json.simple.JSONValue;
import org.json.simple.parser.JSONParser;

public class GenerateTestCases {
	
	public JSONArray generateTC(int companyid, String req, MySQlConn msc) {
		LLMIntegration llmi = new LLMIntegration();
		req = prompt.replace("&req", req);
		req = prompt360.replace("&req", req);
//		MessagesListResponseDTO elmessages = Main.callOpenAI(req);
//        String str = getTCs(elmessages);
//        str = fixJsonStructure(str);
        
//        JSONParser parser = new JSONParser();
        JSONArray jsonArr = new JSONArray();
//        jsonArr = convertStrToArray(str, jsonArr);

       	if(jsonArr.size() < 50) {
       		prompt2 = prompt2.replace("&req", req);
       		prompt2 = prompt2.replace("&tcs", "");
       		prompt2 = prompt2.replace("&size", String.valueOf(jsonArr.size()));
       		MessagesListResponseDTO elmessages2 = Main.callOpenAIv2(prompt2);
       		String str2 = getTCs(elmessages2);
       		str2 = fixJsonStructure(str2);
       		jsonArr = convertStrToArray(str2, jsonArr);
       		System.out.println(str2);
       	}

        return jsonArr;
	}
	
	public static String getTCs(MessagesListResponseDTO elmessages) {
		MessageResponseDTO elmessage = elmessages.data().get(0);
		List<MessageResponseDTO.Content> elcontents = elmessage.content();
		String str = elcontents.get(0).text().value();
		return str;
	}
	
	private String fixJsonStructure(String str) {
		if(str.contains("[")) {
			str = str.substring(str.indexOf("["), str.length());
			if(str.contains("]")) {
				str = str.substring(0,str.indexOf("]")+1);
			}
		}
		
        return str;
	}
	
	private JSONArray convertStrToArray(String str, JSONArray ja) {
		JSONParser parser = new JSONParser();
        JSONArray jsonArr = new JSONArray();
        try {
        	jsonArr = (JSONArray)parser.parse(str);
        	ja.addAll(jsonArr);
        } catch (Exception e) {
        	System.err.println(Utilities.getNow());
        	System.err.println(e.getMessage());
        	System.err.println(e.getStackTrace().toString());
        }
        
        return ja;
	}
	
	public static String prompt2 = "This was my previous request &req and this was the list of test scenarios"
									+ " that you returned &tcs. As you can see I asked for 50 test scenarios and"
									+ " you generated only &size. Can you generate the remaining test scenarios so we have at least 50 test scenarios";
	
	public static String prompt = "Generate test cases from the below description of QR Code Generation, then filter the output as follows:" 
				+ " Personas - " 
				+ "1. company admin - has ability to generate QR codes and associate them to products"
				+ "2. Buyer - has ability to scan the qr code at the time of buying the product physically and reporting any fake products they encounter"
				+ "3. Company sales/marketing - will be provided with a report of all fake products that have been scanned along with users contact numbers and GPS location where they were scanned." 
				+ " &req " 
				+ "1. Generate:"
				+ "   - All possible test scenarios" 
				+ "   - Positive, negative and edge scenarios for each test case" 
				+ "   - List the following attributes for each of the test cases - user persona, mobile or web or api test"

				+ "2. Filter output:"
				+ "  - Remove any explanatory text or descriptions"
				+ "  - Keep only the essential test case information:"
				+ "   * Each test case should start with the persona logging into the system"
				+ "   * Input parameter"
				+ "   * Expected result (that can be validated on the UI)"
				+ "   * Specific test conditions (if critical)"
				+ "   * provide the scenario in Gherkin syntax	" 

				+ "3. Formatting:"
				+ "   - Use JSON format [{\"tcid\":running number, \"description\":a description of the test scenarion, \"persona\":the persona that this scenario applies to, \"device\":web or mobile or api, \"gherkin\":the gherkin syntax}]"

				+ "4. Prioritization:"
				+ "  - List the top 50 tests cases from critical path and high-impact tests first to the trivial ones last."
				+ "  - Provide a comprehensive list of test cases."

				+ "5. Exclude:"
//				+ "  - Redundant or obvious test cases"
				+ "   - Lengthy explanations or justifications"
//				+ "   - Non-essential edge cases"
				
				+ "6. Verifications:"
				+ "  - if you provide less than 50 test scenarios - please rerun my request so that we can get a minimum of 50 test scenarios "


				+ "Aim for actionable set of test cases.";
	public static String prompt360 = "Generate test cases from the below description of a document management application, then filter the output as follows: "
			+ "Personas - "
			+ "1. Document360 user - has the ability create article and publish them either one ata  time or in bulk " 
			+ " &req " 
			+ "1. Generate:"
			+ "   - All possible test scenarios" 
			+ "   - Positive, negative and edge scenarios for each test case" 
			+ "   - List the following attributes for each of the test cases - user persona, mobile or web or api test"

			+ "2. Filter output:"
			+ "  - Remove any explanatory text or descriptions"
			+ "  - Keep only the essential test case information:"
			+ "   * Each test case should start with the persona logging into the system"
			+ "   * Input parameter"
			+ "   * Expected result (that can be validated on the UI)"
			+ "   * Specific test conditions (if critical)"
			+ "   * provide the scenario in Gherkin syntax	" 

			+ "3. Formatting:"
			+ "   - Use JSON format [{\"tcid\":running number, \"description\":a description of the test scenarion, \"persona\":the persona that this scenario applies to, \"device\":web or mobile or api, \"gherkin\":the gherkin syntax}]"

			+ "4. Prioritization:"
			+ "  - List the top 50 tests cases from critical path and high-impact tests first to the trivial ones last."
			+ "  - Provide a comprehensive list of test cases."

			+ "5. Exclude:"
//			+ "  - Redundant or obvious test cases"
			+ "   - Lengthy explanations or justifications"
//			+ "   - Non-essential edge cases"
			
			+ "6. Verifications:"
			+ "  - if you provide less than 50 test scenarios - please rerun my request so that we can get a minimum of 50 test scenarios "


			+ "Aim for actionable set of test cases.";	
	
	public static String prompt3 = "Generate test cases based on the description of QR Code Generation below. Filter the output according to the following specifications:\r\n"
			+ "\r\n"
			+ "Personas:\r\n"
			+ "Company Admin: Can generate QR codes and associate them with products.\r\n"
			+ "Buyer: Can scan QR codes at the point of purchase to report any fake products encountered.\r\n"
			+ "Sales/Marketing: Receives reports of all scanned fake products, including user contact numbers and GPS locations.\r\n"
			+ "Description:\r\n"
			+ "The application must generate QR codes that are printed on product labels. Buyers can scan these QR codes using a mobile app to verify product authenticity. If a product is fake, users can report the location and store details. The company can track and remove fake products using this information.\r\n"
			+ "\r\n"
			+ "Key features include:\r\n"
			+ "\r\n"
			+ "Admins can determine QR code length and pattern.\r\n"
			+ "QR codes can be linked to promotions, events, and loyalty programs.\r\n"
			+ "Each batch of QR codes can be managed for size and patterns.\r\n"
			+ "Validation criteria may include hologram text, color, and serial numbers.\r\n"
			+ "Activities performed on QR codes must be logged for auditing purposes.\r\n"
			+ "Codes can be created, deleted, activated, or deactivated, with export capabilities for offline reporting.\r\n"
			+ "Requirements:\r\n"
			+ "Generate:\r\n"
			+ "\r\n"
			+ "All possible test scenarios, including positive, negative, and edge cases.\r\n"
			+ "Each test case must include the following attributes: user persona, device type (mobile/web/API), input parameters, expected results, specific test conditions, and Gherkin syntax.\r\n"
			+ "Output Format:\r\n"
			+ "\r\n"
			+ "Provide the results in JSON format:\r\n"
			+ "[{\"tcid\": running_number, \"description\": \"test case description\", \"persona\": \"relevant persona\", \"device\": \"web/mobile/API\", \"gherkin\": \"gherkin syntax\"}]\r\n"
			+ "\r\n"
			+ "Prioritization:\r\n"
			+ "\r\n"
			+ "List the top 50 test cases, starting with critical and high-impact scenarios.\r\n"
			+ "Exclusions:\r\n"
			+ "\r\n"
			+ "Avoid lengthy explanations or justifications.\r\n"
			+ "Verification:\r\n"
			+ "\r\n"
			+ "If fewer than 50 test scenarios are generated, please rerun the request to ensure at least 50 actionable test cases.";

}
