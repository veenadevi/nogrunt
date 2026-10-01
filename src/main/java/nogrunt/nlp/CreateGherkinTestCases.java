package nogrunt.nlp;

import nogrunt.AppProperties;
import nogrunt.MySQlConn;
import nogrunt.integrations.*;
import nogrunt.integrations.llm.openaiclient.Main;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

public class CreateGherkinTestCases {
	
	public JSONObject saveNLPStep(int companyid, int tcid1, String stepsInfo, MySQlConn msc) {
		LLMIntegration llmi = new LLMIntegration();
		llmi.gherkinGenericScenario = stepsInfo;
//		return Main.eachScenario(stepsInfo);
		String temp = Main.req2.replace("&ghdesc", stepsInfo);
		JSONObject result = Main.eachScenarioInteraction(stepsInfo);
		JSONObject validatedResult = Main.validateTestSteps(stepsInfo, result);
		
		JSONObject ret = new JSONObject();
//		
		String jsonString = JSONValue.toJSONString(result); // Indentation factor of 4
        System.out.println(jsonString);
		
		ret.put("nlp", result);
		return ret;
		
//		llmi.getTcSteps(companyid, tcid, stepsInfo);
	}
	
	public static void main(String[] args) {
		CreateGherkinTestCases cgtc = new CreateGherkinTestCases();
		String gherkin = "given I am successfully logged in at the url \"https://test.acviss.co/dashboard/login/\" with username as \"Demo\" and pwd as \"Demo\" \r\n"
		+ "when I delete product \"none\"\r\n"
		+ "then on searching for \"none\" on the product details page - the product should no longer be found";
		AppProperties.getProperties();
		MySQlConn msc = new MySQlConn(null);
		cgtc.saveNLPStep(1, 11, gherkin,  msc);
	}

}
