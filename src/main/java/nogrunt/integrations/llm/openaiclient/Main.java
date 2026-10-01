package nogrunt.integrations.llm.openaiclient;

import nogrunt.integrations.llm.openaiclient.dto.*;
import nogrunt.Utilities;
import nogrunt.integrations.*;
import nogrunt.nlp.*;

import java.io.InputStream;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.HashMap;
import java.util.TreeMap;
import java.util.Map;
import java.sql.Timestamp;

import org.json.simple.JSONObject;
import org.json.simple.JSONArray;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import static java.lang.Thread.sleep;

public class Main {
	public static AssistantAIClient client;
	public static ThreadResponseDTO thread;
	public static String prevPage = null;
	
    public static void main(String[] args) {
//        doGen();
//        eachScenario(tc);
    	String ghDesc = "given I am successfully logged in at the url \"https://test.acviss.co/dashboard/login/\" with username as \"Demo\" and pwd as \"Demo\" \r\n"
        		+ " when I delete product \"none\"\r\n"
        		+ " then on searching for \"none\" on the product details page displays the message \"no product found \" on the product details page \r\n";
        
    	
    	
    	ghDesc = "Given the company admin is logged in, When generating a QR code with a specific length and pattern, Then the QR code is successfully created"; 
    	String temp = req2.replace("&ghdesc", ghDesc);
    	
    	eachScenarioInteraction( ghDesc);
    	
//    	String prompt = newPrompt.replace("&gherkin", ghDesc);
//    	prompt = prompt.replace("&pages", LLMIntegration.applicationPages);
//    	prompt = prompt.replace("&elements", LLMIntegration.applicationPageElements);
//    	prompt = prompt.replace("&transistions", LLMIntegration.applicationPageTransitions);
//    	MessagesListResponseDTO elmessages2 = Main.callOpenAIv2(prompt);
//		String str2 = GenerateTestCases.getTCs(elmessages2);
//		str2 = str2.substring(str2.indexOf("json")+4,str2.length());
//		str2 = str2.substring(0,str2.indexOf("`"));
//		JSONArray json = new JSONArray();
//		try {
//			JSONParser parser = new JSONParser();
//			json = (JSONArray)parser.parse(str2);
//		}catch (Exception e) {
//			e.printStackTrace();
//		}
		
//    	JSONObject breakdown = eachScenarioInteraction("All the Activity" , temp);
//    	JSONObject entities = new JSONObject();
//        steps(breakdown, entities, ghDesc);
    }
    public static JSONObject eachScenarioInteraction(String ghSyntax) {
    	JSONObject json = new JSONObject();
//    	String temp = req2.replace("&ghdesc", ghSyntax);
//    	JSONObject breakdown = eachScenarioInteraction("All the Activity" , temp);
//    	JSONObject entities = new JSONObject();
////    	return breakdown;
//    	JSONObject steps1 =  steps(breakdown, entities, ghSyntax);
//    	json =  getAutomationSteps(steps1);
//    	
//    	String v1 = verify.replace("&pages", LLMIntegration.applicationPages);
//    	v1 = v1.replace("&elements", LLMIntegration.applicationPageElements);
//    	v1 = v1.replace("&gherkin", ghSyntax);
//    	v1 = v1.replace("&steps", json.toJSONString());
//    	MessagesListResponseDTO elmessages2 = Main.callOpenAI(v1);
//   		String str2 = GenerateTestCases.getTCs(elmessages2);
    	
    	String prompt = newPrompt.replace("&gherkin", ghSyntax);
    	prompt = prompt.replace("&pages", LLMIntegration.applicationPages);
    	prompt = prompt.replace("&elements", LLMIntegration.applicationPageElements);
    	prompt = prompt.replace("&transistions", LLMIntegration.applicationPageTransitions);
    	
//    	prompt = newPrompt361.replace("&gherkin", ghSyntax);
//    	prompt = prompt.replace("&pages360", LLMIntegration.applicationPages360);
//    	prompt = prompt.replace("&elements360", LLMIntegration.applicationPageElements360);
//    	prompt = prompt.replace("&transistions360", LLMIntegration.applicationPageTransitions360);
    	
    	
    	MessagesListResponseDTO elmessages2 = Main.callOpenAI(prompt);
		String str2 = GenerateTestCases.getTCs(elmessages2);
		if(str2.contains("json")) {
			str2 = str2.substring(str2.indexOf("json")+4,str2.length());
		}
		if(str2.contains("`")) {
			str2 = str2.substring(0,str2.indexOf("`"));
		}
//		JSONArray jsona = new JSONArray();
		try {
			JSONParser parser = new JSONParser();
			json = (JSONObject)parser.parse(str2);
		}catch (Exception e) {
			e.printStackTrace();
		}
    	
    	return json;
  }
    
    public static JSONObject validateTestSteps(String gherkin, JSONObject testSteps) {
    	String prompt = validationPrompt.replace("&gherkin", gherkin);
    	prompt = prompt.replace("&teststeps", testSteps.toJSONString());
    	prompt = prompt.replace("&transistions", LLMIntegration.applicationPageTransitions360);
    	
    	MessagesListResponseDTO elmessages2 = Main.callOpenAI(prompt);
    	String str2 = GenerateTestCases.getTCs(elmessages2);
    	JSONObject json = new JSONObject();
//    	try {
//			JSONParser parser = new JSONParser();
//			json = (JSONObject)parser.parse(str2);
//		}catch (Exception e) {
//			e.printStackTrace();
//		}
    	
    	return json;
    }
    
    public static JSONObject getAutomationSteps(JSONObject steps1) {
    	JSONObject result = new JSONObject();
    	int index = 1;    	
    	int success = 0;
    	int size = steps1.size();
    	
    	do {
    		String indexStr = String.valueOf(index);
    		Object actions = steps1.get(index);
    		
    		if(actions == null) {
    			index = index + 1;
    			continue;
    		}
    		
    		if(actions instanceof JSONArray) {
    			JSONArray ja = (JSONArray)actions;
    			JSONObject jo = (JSONObject)ja.get(0);
    			String url = (String)jo.get("URL");
    			result.put(result.size(), " URL ~" + url + "~");
    		} else if(actions instanceof JSONObject) {
    			JSONObject jo = (JSONObject)actions;
    			JSONArray actionList = (JSONArray)jo.get("actions");
    			if(actionList == null) {
    				String url = (String)jo.get("action");
    				result.put(result.size(), " URL ~" + url + "~");
    			}
    			if(actionList != null) {    			
	    			for(int i=0;i<actionList.size();i++) {
	    				JSONObject steps = (JSONObject)actionList.get(i);
	    				String stepAction = (String)steps.get("action");
	    				String label = (String)steps.get("label");
	    				String value = (String)steps.get("value");
	    				if(stepAction != null) {
	    					if(stepAction.equalsIgnoreCase("enter_text") ||
	    							(((stepAction.toLowerCase()).contains("enter")) &&
	    							 (stepAction.toLowerCase()).contains("text"))) {
	    						if(value == null) {
	    							result.put(result.size(), "Enter value into " + label);
	    						} else {
	    							result.put(result.size(), "Enter ~" + value + "~ into " + label);
	    						}
	    					} else if (stepAction.equalsIgnoreCase("click")) {
	    						result.put(result.size(), "Click " + label);
	    					}
	    				}
	    			}
    			}
    			
    			JSONArray transistions = (JSONArray)jo.get("transistions");
    			if(transistions != null) {
	    			for(int i=0; i<transistions.size();i++) {
	    				JSONObject transit = (JSONObject)transistions.get(i);
	    				result.put(result.size(), "Click " + (String)transit.get("destpage"));
	    			}
    			}
    			
    		} else {
    			System.out.println("Oh! what have we done");
    		}   		
    		
    		index = index + 1;
    		success = success + 1;
    	} while (success < size);
    	
    	return result;
    }
    
    public static String postProcessOp(String respstr) {
        // Sometimes the response from OpenAI has ```json <content> ``` in the response.
        // This causes issue with the json parsing of the data. So we sanitize the response 
        // by removing them if present so that we get a proper json string that can be parsed.
        String target = "```json";
        String replacement = "";
        String processed = respstr.replace(target, replacement);
        target = "```";
        replacement = "";
        String cleaned = processed.replace(target, replacement);
        return cleaned;
    }

    public static void doGen() {

        HashMap<String, String> scenarios = new HashMap<String, String>();

        scenarios.put("ExportCodeScenario", LLMIntegration.gherkinExportCodeScenario);
        scenarios.put("SubCustomerScenario", LLMIntegration.gherkinSubCustomerScenario); 
        scenarios.put("ProdDeletionScenario", LLMIntegration.gherkinProdDeletionScenario);
        scenarios.put("ViewDataScenario", LLMIntegration.gherkinViewDataScenario);
		
        for (String scenario : scenarios.keySet()) {
        	System.out.println("\nScenario: " + scenario);
            // First get the page transitions involved for the test scenarion.
            // For each of the page transitions get the interactions that need to be performed.

            String testcase = scenarios.get(scenario);
            System.out.println(testcase);
            
            eachScenario(testcase);  
            
            int secondsToSleep = 5;
            try {
                Thread.sleep(secondsToSleep * 1000);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
            }
        }        
    }
    
    public static JSONObject eachScenario(String testcase) {
    	String elementActionPrompt = eachScenarioTransistions(testcase);
    	return eachScenarioInteraction("Page Element Action Response:" , elementActionPrompt);
    }
    
    public static String eachScenarioTransistions(String testcase) {
    	String transitionPrompt = LLMIntegration.getPromptScenarioTransition(testcase);
        MessagesListResponseDTO messages = callOpenAI(transitionPrompt);
        // System.out.println("Response messages data size: " + messages.data().size());
        // Get the first response
        MessageResponseDTO message = messages.data().get(0);
        List<MessageResponseDTO.Content> contents = message.content();
        // Access the content of first response and sanitize it
        String transitionInfo = postProcessOp(contents.get(0).text().value());
        System.out.println("Page Transitions Response:\n" + transitionInfo);

        String appPageElements = LLMIntegration.applicationPageElements;
        String elementActionPrompt = LLMIntegration.
       		 getPromptPageElementAction(testcase, transitionInfo, appPageElements);
        return elementActionPrompt;
    }
    
    public static JSONObject eachScenarioInteraction(String comment, String elementActionPrompt) {
//    	elementActionPrompt = req2;
        MessagesListResponseDTO elmessages = callOpenAI(elementActionPrompt);
        // System.out.println("Response messages data size: " + messages.data().size());
        // Get the first response
        MessageResponseDTO elmessage = elmessages.data().get(0);
        List<MessageResponseDTO.Content> elcontents = elmessage.content();
        // Access the content of first response and sanitize it
        String pageElementAction = postProcessOp(elcontents.get(0).text().value());
        if(pageElementAction.startsWith("I'm sorry")) {
        	return null;
        }
//        System.out.println(comment + ":\n" + pageElementAction); 
        JSONObject entities =  convertToJSON(pageElementAction);
//        entities = (JSONObject)entities.get("nlp");
//        entities(entities);
        return entities;
    }
    
    public static JSONObject steps(JSONObject breakdown, JSONObject entities1, String gherkin) {
    	JSONArray actions = (JSONArray)breakdown.get("breakdown");
    	TreeMap<Integer,JSONObject> bdSeq = getSeq(actions);
    	JSONObject entities = null;
    	
    	for (Map.Entry<Integer, JSONObject> bd : bdSeq.entrySet()) {
    		JSONObject action = bd.getValue();
    		String activity = (String)action.get("action");
    		if(activity == null && action.get("URL") != null) {
    			JSONArray ja = new JSONArray();
    			ja.add(bd.getValue());
    			entities1.put(bd.getKey(), ja);
    			prevPage = "Launching URL";
    			continue;
    		}
    		if(activity.contains("Navigate to the URL") ||
    				activity.contains("Open the URL")) {
    			entities1.put(bd.getKey(), bd.getValue());
    			prevPage = "Launching URL";
    			continue;
    		}
    		
    		if(activity.contains("Navigate to ") && !activity.contains("URL")) {
    			pageTransistion(entities, activity);
    		} else {
	    		String temp = req3_1;
	    		temp = temp.replace("&ghdesc", gherkin);
	    		temp = temp.replace("&action", activity);
	    		
	    		entities = eachScenarioInteraction("Breakdown :" , temp);
	    		if(entities == null) {
	    			continue;
	    		}
	    		
	    		actionToPage(entities,activity);
//	    		JSONArray eArr = (JSONArray)entities.get("actionToPageElement");
	    		entities1.put(bd.getKey(), entities);
    		}
        }
    	
    	return entities1;
    }
    
    public static TreeMap getSeq(JSONArray jsonArray) {
    	TreeMap<Integer,JSONObject> actionSeq = new TreeMap();
    	
    	for(int i=0;i<jsonArray.size();i++) {
    		JSONObject action = (JSONObject)jsonArray.get(i); 
    		int seq = -1;
    		if(action.get("seq") instanceof String) {
    			String st = (String)action.get("seq");
        		seq = Integer.valueOf(st);
    		} else if(action.get("seq") instanceof Long) {
    			long lseq = (Long)action.get("seq");
    			seq = (int)lseq;
    		}
    		
    		actionSeq.put(seq, action);
    	}
    	
    	return actionSeq;
    }
    
    public static void actionToPage(JSONObject entities, String activity) {
    	JSONArray actions = (JSONArray)entities.get("actions");
    	
    	TreeMap<Integer,JSONObject> actionSeq = getSeq(actions);
    	
    	for (Map.Entry<Integer, JSONObject> entry : actionSeq.entrySet()) {
    		JSONObject actionObj = entry.getValue();
    		String action = (String)actionObj.get("action");
    		String entity = (String)actionObj.get("entity");
    		String label = (String)actionObj.get("label");
    		String temp = req4;
    		temp = temp.replace("&action", action);
    		temp = temp.replace("&entity", entity);
    		temp = temp.replace("&activity", activity);
    		if(label != null && !label.equals("")) {
    			temp = temp.replace("&label", label);
    		} else {
    			temp = temp.replace("(with label as &label)", " ");
    		}
    		JSONObject actionToPage = eachScenarioInteraction("Action to page :" , temp );
    		JSONArray atpe = (JSONArray)actionToPage.get("actionToPageElement");
    		entities.put("actionToPageElement", atpe);
    		
    		JSONObject pageObj = (JSONObject)atpe.get(0);
    		String pageName = (String)pageObj.get("pagename");
    		
    		if(prevPage != null) {
	    		if(!pageName.toLowerCase().equals(prevPage.toLowerCase())) {
	    			pageTransistion(entities);
	    		}
    		} else {
    			prevPage = pageName;
    			entities.put("transistions", "none");
    		}
    		
//    		if(action != null && !action.equals("") &&
//    				!(action.toLowerCase()).contains("click") && !(action.toLowerCase()).contains("submit") &&
//    				!(action.toLowerCase()).contains("login") && !(action.toLowerCase()).contains("delete") &&
//    				!(action.toLowerCase()).contains("search") && !(action.toLowerCase()).contains("enter")) {
//    			pageTransistion(entities);
//    		}
    	}    	
    }
    
    public static void pageTransistion(JSONObject entities) {
    	JSONArray actions = (JSONArray)entities.get("actionToPageElement");
    	
    	TreeMap<Integer,JSONObject> actionSeq = getSeq(actions);
    	
    	for (Map.Entry<Integer, JSONObject> entry : actionSeq.entrySet()) {
    		JSONObject action = entry.getValue();
    		String curPage = (String)action.get("pagename");
    		if (prevPage == null) {
    			prevPage = curPage;
    			entities.put("transistions", "none");
//    			continue;
    		}
//    		String temp = req5;
//    		temp = temp.replace("&prevpage", prevPage);
//    		temp = temp.replace("&curPage", curPage);
//    		JSONObject actionToPage = eachScenarioInteraction("Action to page :" , temp );
//    		prevPage = curPage;
//    		JSONArray trans = (JSONArray)actionToPage.get("transistions");
//    		entities.put("transistions", trans);
////    		entities.remove("")
//    		System.out.println("Entity/Action/Pagelement" + ":\n" + entities.toJSONString()); 
    		pageTransistion(entities, curPage);
    	}
    }
    
    public static void pageTransistion(JSONObject entities, String curPage) {
    	String temp = req5;
		temp = temp.replace("&prevpage", prevPage);
		temp = temp.replace("&curPage", curPage);
		JSONObject actionToPage = eachScenarioInteraction("Action to page :" , temp );
		prevPage = curPage;
		JSONArray trans = (JSONArray)actionToPage.get("transistions");
		entities.put("transistions", trans);
//		entities.remove("")
		System.out.println("Entity/Action/Pagelement" + ":\n" + entities.toJSONString()); 
    }
    
    public static JSONObject convertToJSON(String jsonString) {
    	JSONObject jsonObject= null;
    	try {
            // Parse the JSON string
            JSONParser parser = new JSONParser();
            Object obj = parser.parse(jsonString);

            // Convert parsed object to JSONObject
            jsonObject = (JSONObject) obj;

        } catch (ParseException e) {
        	System.out.println(jsonString);
            e.printStackTrace();
        }
    	
//    	jsonObject = new JSONObject();
//    	jsonObject.put("nlp", jsonString);
    	return jsonObject;
    }
    
    public static MessagesListResponseDTO callOpenAI(String prompt) {

        //LOAD YOUR API KEY
        Properties properties = new Properties();
        long DELAY = 3; // wait for a bit between status checks
        try (InputStream input = Main.class.getClassLoader().getResourceAsStream("application.properties")) {
            if (input == null) {
                System.out.println("Sorry, unable to find application.properties");
                return null;
            }
            properties.load(input);

            client = new AssistantAIClient(properties);
            AssistantResponseDTO assistant = client.createAssistant(properties.getProperty("openai.assistant.instructions"));
            thread = client.createThread();
            client.sendMessage(thread.id(), "user", prompt);
            RunResponseDTO run = client.runMessage(thread.id(), assistant.id());

            waitUntilRunIsFinished(client, thread, run, DELAY);

            MessagesListResponseDTO allResponses = client.getMessages(thread.id());
            // log(allResponses);
            return allResponses;
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        
        return null;
    }
    
    public static MessagesListResponseDTO callOpenAIv2(String prompt) {

        //LOAD YOUR API KEY
        Properties properties = new Properties();
        long DELAY = 3; // wait for a bit between status checks
        try (InputStream input = Main.class.getClassLoader().getResourceAsStream("application.properties")) {
            if (input == null) {
                System.out.println("Sorry, unable to find application.properties");
                return null;
            }
            properties.load(input);

            client = new AssistantAIClient(properties);
            AssistantResponseDTOv2 assistant = client.createAssistantv2(properties.getProperty("openai.assistant.instructions"));
            thread = client.createThread();
            client.sendMessage(thread.id(), "user", prompt);
            RunResponseDTOv2 run = client.runMessagev2(thread.id(), assistant.id());

            waitUntilRunIsFinishedv2(client, thread, run, DELAY);

            MessagesListResponseDTO allResponses = client.getMessages(thread.id());
            // log(allResponses);
            return allResponses;
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        
        return null;
    }

    private static void waitUntilRunIsFinished(AssistantAIClient client, ThreadResponseDTO thread, RunResponseDTO run, long DELAY) throws InterruptedException {
    	long startTime = System.currentTimeMillis();
    	while (!isRunDone(client, thread.id(), run.id())) {
            superviseWorkInProgress(client, thread);
            long curTime = System.currentTimeMillis();
            if((curTime - startTime) >= 120000) {
            	break;
            }
            sleep(DELAY * 1000);
        }
    }
    
    private static void waitUntilRunIsFinishedv2(AssistantAIClient client, ThreadResponseDTO thread, RunResponseDTOv2 run, long DELAY) throws InterruptedException {
        while (!isRunDonev2(client, thread.id(), run.id())) {
            superviseWorkInProgress(client, thread);
            sleep(DELAY * 1000);
        }
    }

    private static void superviseWorkInProgress(AssistantAIClient client, ThreadResponseDTO thread) {
        try {
            // System.out.println("Checking messages to supervise assistant's work");
            // MessagesListResponseDTO messages = client.getMessages(thread.id());
            // log(messages);
            client.getMessages(thread.id());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static boolean isRunDone(AssistantAIClient client, String threadId, String runId) {
//        RunResponseDTO status;
        
        try {
            String status = client.getRunStatus(threadId, runId);
            return isRunStateFinal(status);
        } catch (Exception e) {
            System.err.println("Failed to get run state, will retry..." + e);
            e.printStackTrace();
            return false;
        }
    }
    
    private static boolean isRunDonev2(AssistantAIClient client, String threadId, String runId) {
//        RunResponseDTOv2 status;
        try {
            String status = client.getRunStatusv2(threadId, runId);
            return isRunStateFinalv2(status);
        } catch (Exception e) {
            System.err.println("Failed to get run state, will retry..." + e);
            e.printStackTrace();
            return true;
        }
    }

//    private static boolean isRunStateFinal(RunResponseDTO runResponseDTO) {
    private static boolean isRunStateFinal(String status) {
        List<String> finalStates = List.of("cancelled", "failed", "completed", "expired"); //I consider these states as final
//        String runStatus = Optional.of(runResponseDTO).map(RunResponseDTO::status).orElse("unknown").toLowerCase();
//        return finalStates.contains(runStatus);
        return finalStates.contains(status);
    }
    
//    private static boolean isRunStateFinalv2(RunResponseDTOv2 runResponseDTO) {
    private static boolean isRunStateFinalv2(String status) {
        List<String> finalStates = List.of("cancelled", "failed", "completed", "expired"); //I consider these states as final
//        String runStatus = Optional.of(runResponseDTO).map(RunResponseDTOv2::status).orElse("unknown").toLowerCase();
        return finalStates.contains(status);
    }
    
    public static String req = "for this gherkin syntax - "
    		+ "the previous time i ran this you identified 3 actions and tis time you idnetified only 1 action - what do i need to provide so you will be more consistent"
    		+ "can you identify all the entities , all the attributes for those entities, "
    		+ "the values that have been defined for these attributes "
    		+ "and the action to be performed on those entities. "
    		+ "Actions will be spread across the Given and when and then statements in the gherkin syntax "
    		+ "Can you give me the response in the following json format\r\n"
    		+ "{\"entities\":[{\"entity\": \"entity name\", \"attributes\":[{\"attribute name\":\"attribute value\"},{\"attribute name\":\"attribute value\"}]},\r\n"
    		+ "             {\"entity\": \"entity name\", \"attributes\":[{\"attribute name\":\"attribute value\"},{\"attribute name\":\"attribute value\"}]}],\r\n"
    		+ "\"actions\":[{\"entity\":\"entity name\", \"action\":\"action name\", \"seq\":\"sequence number\"}]}\r\n"
    		+ "where entity name is the name of the entity that you have identified\r\n"
    		+ "attribute name is also the name of the attribute that you have identified\r\n"
    		+ "attribute value is the value that has been provided the gherkin syntax for that attribute\r\n"
    		+ "seq is the order in which the action has to be performed for the given gherkin syntax, it is an integer\r\n"
    		+ "action name is the action that you have identified"
    		+ "given I am successfully logged in at the url \"https://test.acviss.co/dashboard/login/\" with username as \"Demo\" and pwd as \"Demo\" \r\n"
    		+ "when I delete product \"none\"\r\n"
    		+ "then search for \"none\" on the product details page - "
    		+ "the product should no longer be found";
    
    public static String req2 = "for this gherkin syntax - \r\n"
    		+ "    		\r\n"
    		+ " &ghdesc "
    		+ "			\r\n"
    		+ "			can you break this down into a series of steps that would need to be performed in order to achieve the goal as described by the gherkin syntax.\r\n"
    		+ "			messages that are displayed on the screen are not actions but are a result of an already performed action "
    		+ "			Can you give me the response as a json object like the format "
    		+ "{ \"breakdown\":[{\"seq\":\"1\", \"URL\":\"URL\"},{\"seq\":\"2\", \"action\":\"action to be performed\"},{\"seq\":\"3\", \"action\":\"action to be performed\"}]}.";
    
//    public static String incAction = "These are the only pages available on the application " + LLMIntegration.applicationPageElements
//    		+ "- can you identify the pages where these actions can be performed on the entity "
//    		+ "that is identified for each action";
    
//    public static String req3 = "			Based on this gherkin syntax  " 
//    		+ " &ghdesc "
//    		+ "			\r\n"
    		public static String req3 =   "can you identify all the entities and the attributes for those entities that are explictly stated in order to accomplish the action"
    		+ " &action \r\n"
    		+ "In some cases the values for the attributes are also mentioned, in such cases do capture the value in the attribute value of the response and the "
    		+ "html element type of these attributes. Can you also identify the action to be performed in order to accomplish the action \r\n"
    		
//    		+ "    		the values that have been defined for these attributes and the html element type of the attributes\r\n"
//    		+ "    		and the action to be performed on those entities. "
//    		+ "Please do not make up any name and values for attributes if they are not provided in the original statement\r\n"
    		+ "seq is the order in which the action has to be performed for the given gherkin syntax, it is an integer\\r\\n"
    		+ "			\r\n"
    		+ "			Can you give me the response in the following json format \r\n"
    		+ "    		{\"entities\":[{\"entity\": \"entity name\", \"attributes\":[{\"name\":\"attribute name\",\"value\":\"attribute value\",\"type\":\"html element type\"},{\"attribute name\":\"attribute value\",\"type\":\"html element type\"}]}, \r\n"
    		+ "    		             {\"entity\": \"entity name\", \"attributes\":[{\"attribute name\":\"attribute value\"},{\"attribute name\":\"attribute value\"}]}], \r\n"
    		+ "    		\"actions\":[{\"entity\":\"entity name\", \"action\":\"action name\", \"label\":\"HTML Label in the Page\", \"seq\":\"sequence number\"}]}";
    
    		public static String req3_1 =   "can you identify all the actions that need to be performed on the html page to accomplish  "
    	    		+ " &action \r\n"
    	    		+ "seq is the order in which the action has to be performed for the given gherkin syntax, it is an integer\\r\\n"
    	    		+ "			\r\n"
    	    		+ "			Can you give me the response in the following json format \r\n"
//    	    		+ "    		{\"entities\":[{\"entity\": \"entity name\", \"attributes\":[{\"name\":\"attribute name\",\"value\":\"attribute value\",\"type\":\"html element type\"},{\"attribute name\":\"attribute value\",\"type\":\"html element type\"}]}, \r\n"
//    	    		+ "    		             {\"entity\": \"entity name\", \"attributes\":[{\"attribute name\":\"attribute value\"},{\"attribute name\":\"attribute value\"}]}], \r\n"
    	    		+ "    		{\"actions\":[{\"entity\":\"entity name\", \"action\":\"action name\", \"label\":\"HTML Label in the Page\", \"value\":\"value, if any,  defined in the Gherkin syntax explictly\", \"seq\":\"sequence number\"}]}";
    		
    		public static String req4 = "For the web application, following are the pages that currently exist on the web application and the elements on each of the pages \r\n"
    		+ LLMIntegration.applicationPageElements
    		+ "			\r\n"
    		+ "			which page element would be able to &action (with label as &label) on the &entity in order to perform the activity - &activity. "
    		+ "The pagelement and pagename that you identify should be able to act on the entity that you have identified."
    		+ "the page element is the element that can achieve the functionality and page name is thepage that element was found in as part of the data I provided "
    		+ "Can you provide the response in the following json format {\"actionToPageElement\":[{\"pagename\":\"page name\",\"pageelement\":\"element label\"}]}"
    		+ "";
    
    public static String req5 ="These are the only allowed transistions between pages on a web application - \r\n"
    		+ LLMIntegration.applicationPageTransitions
    		+ "    		+ \"			r\n"
    		+ "    		+ \"			what page transistions are required to go from &prevpage to &curPage \r\n"
    		+ "    		+ \"Your recommendation should strictly stick to the page transistions as provided \r\n"
    		+ "    		+ \"Can you provide the response in the following json format {\"transistions\":[{\"frompage\":\"page name\",\"destpage\":\"page name\"}]}";
    
    public static String tc = "given I am successfully logged in at the url \"https://test.acviss.co/dashboard/login/\" with username as \"Demo\" and pwd as \"Demo\" \r\n"
    		+ "when I delete product \"none\"\r\n"
    		+ "then on searching for \"none\" on the product details page - the product should no longer be found";
    
    public static String verify = "All the pages in the application are &pages , the UI elements"
    		+ " on each page are &elements. The Gherkin intent was &gherkin. You returned these steps &steps"
    		+ " as the steps needed to be performed on the application page and page elements in order"
    		+ " to achieve the intent described in gherkin syntax. Can you fix the steps to only include "
    		+ "only the steps that are absolutely necessary to achieve the intent ";
    
    public static String newPrompt = "You are an expert software tester who understands the "
    		+ "product intimately. This product has been built and you are required to identify"
    		+ " accurately the steps required to achieve this scenario so that the identifed "
    		+ "scenarios can be automated using open source framework like Selenium.  I need "
    		+ "you to convert the intent of what the test scenario needs to achieve which is "
    		+ "written in Gherkin syntax into a sequence of steps that can actually be "
    		+ "performed on the application. "
    		
    		+ "Step 1 - The starting sequence "
    		+ "  --  Always start by going to the base url to launch the application "
    		+ "  -- If the test scenario requires to identify the type of user - then add steps for the user to sign into the application"
    		+ "  -- Anonymous user scenarios will not require the user to sign in."
    		
    		+ ""
    		+ "Step 2 - Identify the required page to fulfill the intent"
    		+ "   -- The Application Pages and their description data provided as part of this "
    		+ "prompt - provides a list of all the pages in the application with a dscription "
    		+ "of what the intent of each page is"
    		+ "   -- These are the only pages available for this application and so all "
    		+ "intents need to be fulfullied using these pages only."
    		+ "   -- Identify the page that can fulfill the intent as defined in the gherkin "
    		+ "syntax. "


    		+ "Step 3 - Identify the required page transistions "
    		+ "  -- The possible page transistions in the application are defined in the Page "
    		+ "Transistions data provided as part of this prompt"
    		+ "  -- identify the shortest path from the Url Launch to the page identified in "
    		+ "step2 above"
    		+ "  -- The transistion that are defined in this prompt are the only transistions "
    		+ "that are possible in this application"

    		+ "Step 4 - Identify actions to be perfomed on each page to enable the transistion"
    		+ " to the next page"
    		+ "  -- The Application Pages and their elements data provided with this prompt "
    		+ "list all the elements on a page and what each element does "
    		+ "  -- use this info to identify what actions need to be performed on a page to "
    		+ "enable a succesful transistion to the next page as identified in step 3."
    		+ "  -- All the relevant actions on the page need to identified accurately using "
    		+ "only the data that has been provided."

			+ "Step 5. Exclude:"
//			+ "  - Redundant steps - if and when steps repeat and are not needed to repeat please exclude"
			+ "   - Lengthy explanations or justifications"
//			+ "   - Non-essential edge cases"
    		
    		+ "Step 6 - Output"
    		+ "  -- Provide output in the following json format - "
    		+ "{\"1\":\"Enter ~company admin username~ into Username\",\"2\":\"Enter ~company admin password~ into Password\",\"3\":\"Click Login\"}"
    		
    		
    		+"Intent in gherkin syntax - &gherkin"    		
    		+"/r/n"    		
    		+ "&pages"
    		+"/r/n"    		
    		+ "&elements"
    		+"/r/n"    		
    		+ "&transistions"
    		;
    
    public static String validationPrompt = "You are an expert software tester who understands the "
    		+ "product intimately. you are given the Gherkin syntax for 1 of many test scenarions "
    		+ "as the intent of this task and the test steps are the result of converting the given "
    		+ "gherkin text to actionable steps."
    		+ " You are required to identify all the valid page transistions in sequential order "
    		+ "that are required in order to execute this specific test scenario, "
    		+ "base your analysis only on the provuded possible page transistions "
    		+ "A page transistion is considered valid only if the destination page of the "
    		+ "previous step is the source step of the next step."
    		+ ""
    		+ "The Gherkin syntax is &gherkin "
    		+ "The test steps that have been generated are &teststeps "
    		+ " The only possible page transistions are &transistions. "
    		+ " Please share a detailed analysis of how you determined the transistion paths."
    		+ " Please indicate which of the transistion paths are not present in the possible "
    		+ "page transistion list that I have provided";
    
    public static String newPrompt360 = "You are an expert software tester who understands the "
    		+ "product intimately. This product has been built and you MUST identify"
    		+ " accurately the steps required to achieve this scenario so that the identified "
    		+ "scenarios can be automated using open source framework like Selenium.  Your task is "
    		+ "to convert the intent of the test scenario as expressed in gherkin syntax  "
    		+ "into a sequence of steps that can actually be automated using Selenium.  "
    		
    		+ "###Instruction 1### - The starting sequence "
    		+ "  --  Every scenario MUST start by launching the base url for the application "
    		+ "  --  the next steps MUST be steps for the user to sign into the application "
    		+ "  --  You will be penalized if every scenarios doesnt start with base url and login steps"

    		+ "###Instruction 2### - Your task is to identify the Application page that can "
    		+ "fulfill the intent of the test scenario as expressed in gherkin syntax"
    		+ "   -- The Application Pages and their description data is provided as part of this "
    		+ "prompt - You MUST use - only the Applications pages and their explanations that have"
    		+ " been provided as part of this prompt"
    		+ "   -- These are the only pages available for this application and so all "
    		+ "intents MUST be fulfullied using these Application pages only."
    		+ "   -- Identify the page that can fulfill the intent as defined in the gherkin "
    		+ "syntax. "


    		+ "###Instruction 3### - Your task is to identify the required page transistions "
    		+ "  -- The possible page transistions in the application are defined in the Page "
    		+ "Transistions data provided as part of this prompt"
    		+ "  -- identify the shortest path from the Url Launch to the page identified in "
    		+ "step2 above"
    		+ "  -- The page transistion that are defined in this prompt are the only page "
    		+ "transistions that are possible in this application"

    		+ "###Instruction 4### - You MUST identify all the elements that need to be filled "
    		+ "and or selected on each page to enable the transistion to the next page - "
    		+ "at every step of the test scenario"
    		+ "  -- The Application Pages and their elements data provided with this prompt "
    		+ "list all the elements on a page and what each element does "
    		+ "  -- use this info to identify what actions need to be performed on an application "
    		+ "page to enable a succesful transistion to the next page as identified in step 3."
    		+ "  -- All the relevant actions on the page MUST be identified accurately using "
    		+ "only the data that has been provided."
    		+ "  -- The then part of te gherkin scenario MUST be converted to a step that can be "
    		+ "validated based on the data provided  ."

			+ "###Instruction 5###. Exclude:"
//			+ "  - Redundant steps - if and when steps repeat and are not needed to repeat please exclude"
			+ "   - Lengthy explanations or justifications"
//			+ "   - Non-essential edge cases"
    		
    		+ "###Instruction 6### - Output"
    		+ "  -- Provide output in the following json format - "
    		+ "{\"1\":\"Enter Base URL\",\"2\":\"Enter ~company admin username~ into Username\",\"3\":\"Enter ~company admin password~ into Password\",\"4\":\"Click Login\"}"
    		
    		
    		+"Intent in gherkin syntax - &gherkin"    		
    		+"/r/n"    		
    		+ "&pages360"
    		+"/r/n"    		
    		+ "&elements360"
    		+"/r/n"    		
    		+ "&transistions360"
    		;
    
    public static String newPrompt361 = "Question --> When given the following gherkin syntax for a test scenario "
    		+ " Given the Document360 user is logged into the system "
    		+ " And an article is in draft status "
    		+ " When the user clicks on the publish button "
    		+ " And adds comments "
    		+ " And configures the article settings "
    		+ " Then the article status should change to published - You Must convert it to a set of test steps "
    		+ " using the page transistions and page elements listed below "
    		+ " Answer --> This step is achieved by the below 5 steps \"\r\n"
    		+ "	01. Launch Base URL. \"\r\n"
    		+ "	02. Enter Username. \"\r\n"
    		+ "	03. Enter Password. \"\r\n"
    		+ "	04. Click on Login Button. \"\r\n"
    		+ "	05. Click on project Name on the project Dashboard\"\r\n"
    		+ "	And an article is in draft status - this can be addressed by \"\r\n"
    		+ "	06. Search for article in draft status \"\r\n"
    		+ "	When the user clicks on the publish button  - this is addressed by \"\r\n"
    		+ "	07. Click the check box for article in draft status\"\r\n"
    		+ "	08. Click the publish button. \"	\r\n"
    		+ "	And adds comments - this can be addressed by \"\r\n"
    		+ "	09. Enter comments into the comments text area \"\r\n"
    		+ "	10. Click on the Yes button \"\r\n"
    		+ "	Then the article status should change to published --> this can be addressed by \"\r\n"
    		+ "	11. Search for the article \"\r\n"
    		+ "	12. Confirm that the status is published \"\r\n"
    		+ "	Similaly can you convert this test scenario described in gherkin syntax into test steps \"\r\n"
    		
  			+ "###Instruction ### - Output"
  			+ "  -- Provide output in the following json format - "
  			+ "{\"1\":\"Enter Base URL\",\"2\":\"Enter ~company admin username~ into Username\",\"3\":\"Enter ~company admin password~ into Password\",\"4\":\"Click Login\"}"
    		
    		+ "	 &gherkin \"	\r\n"
    		+ " /r/n\"    		\r\n"
    		+ " &pages360\"\r\n"
    		+ " /r/n\"    		\r\n"
    		+ " &elements360\"\r\n"
    		+ " /r/n\"    		\r\n"
    		+ " &transistions360"
    		;
}
