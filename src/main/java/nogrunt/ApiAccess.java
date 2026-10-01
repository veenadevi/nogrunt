package nogrunt;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

public class ApiAccess {
	
	 public static Object callApiTD(String url, String params, String query, 
			 ExecutionLogger el, ConcurrentHashMap<String, String> varMap, String headers) {
		 try {	
			 url = replaceParams(url, params, varMap);
			 URL apiURL = new URL(url);			 
			 
			 el.logExecution("API is being called with url - " + apiURL.toString());
			 HttpURLConnection connection = (HttpURLConnection) apiURL.openConnection();
			    connection.setRequestMethod("GET");
		        connection.setRequestProperty("Accept", "application/json");
		        connection.setRequestProperty("Content-Type", "application/json");
		        connection.setDoOutput(true);
		        
		        JSONParser parser = new JSONParser();
		        JSONObject headersjson = (JSONObject)parser.parse(headers);
		        for (Object key : headersjson.keySet()) {
	                connection.setRequestProperty((String) key, (String) headersjson.get(key));
	            }
		        
		        int responseCode = connection.getResponseCode();
		        
		        if (responseCode == HttpURLConnection.HTTP_OK) {
		            BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
		            String inputLine;
		            StringBuffer response = new StringBuffer();
		            while ((inputLine = in.readLine()) != null) {
		                response.append(inputLine);
		            }
		            in.close();
		            el.logExecution("API Response : " + response.toString());
		            
		         // Parse the API response as JSON using org.json.simple
//	                JSONParser parser = new JSONParser();
//	                Object jsonResponse = parser.parse(response.toString());

//	                return getDataFromJSON(jsonResponse, query, el);
//	                return jsonResponse;
		            String responseStr = response.toString();
		            if (responseStr.trim().startsWith("{")) {
		                Object parsed = parser.parse(responseStr);
		                return parsed;
		            } else {
		                return responseStr;
		            }
		        } else {
		            System.out.println("API Request failed with response code: " + responseCode);
		        }
		 }catch(Exception e) {
			 e.printStackTrace();
		 }
		 
		 return null;
	 }
	 
	 public static String callApiForDesktop(String url, String params, String query, 
			 ExecutionLogger el, ConcurrentHashMap<String, String> varMap) {
		 try {	
			 URL apiURL = new URL(url);			 
			 
			 el.logExecution("API is being called with url - " + apiURL.toString());
			 HttpURLConnection connection = (HttpURLConnection) apiURL.openConnection();
		        connection.setRequestMethod("GET");
		        connection.setRequestProperty("Accept", "application/json");
		        
		        int responseCode = connection.getResponseCode();
		        
		        if (responseCode == HttpURLConnection.HTTP_OK) {
		            BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
		            String inputLine;
		            StringBuffer response = new StringBuffer();
		            while ((inputLine = in.readLine()) != null) {
		                response.append(inputLine);
		            }
		            in.close();
		            el.logExecution("API Response : " + response.toString());
		            
		         // Parse the API response as JSON using org.json.simple
	                JSONParser parser = new JSONParser();
	                JSONObject jsonResponse = (JSONObject) parser.parse(response.toString());
	                return jsonResponse.toString();
//	                String otpValue = (String) jsonResponse.get("otp");
//
//	                return otpValue;
	               
		        } else {
		            System.out.println("API Request failed with response code: " + responseCode);
		        }
		 }catch(Exception e) {
			 e.printStackTrace();
		 }
		 
		 return null;
	 }
	 
	 public static HttpURLConnection getPostConnection(String url , String body, 
			 String headers, ExecutionLogger el, String type) {
		 HttpURLConnection connection = null;
		 try {
		 
			 URL apiURL = new URL(url);			 
			 
			 if(el != null) {
				 el.logExecution("API is being called with url - " + apiURL.toString());
			 }
			 	connection = (HttpURLConnection) apiURL.openConnection();
		        connection.setRequestMethod(type);
		        connection.setRequestProperty("Content-Type", "application/json");
		        connection.setDoOutput(true);
		        
	//	        String jsonPayload = "{\"text\":\"" + message + "\"}";  
		        String jsonPayload = body; 
		        
		        JSONParser parser = new JSONParser();
		        if(headers != null && !headers.equals("")) {
			        JSONObject headersjson = (JSONObject)parser.parse(headers);
			        for (Object key : headersjson.keySet()) {
		                connection.setRequestProperty((String) key, (String) headersjson.get(key));
		            }
		        }
		        
		     // Write the message payload to the connection's output stream
	            try (OutputStream os = connection.getOutputStream()) {
	                byte[] input = jsonPayload.getBytes("utf-8");
	                os.write(input, 0, input.length);
	            }
		 } catch(Exception e) {
			 e.printStackTrace();
		 }
		 
		 return connection;
	 }
	 
	 public static Object postToApi(String url, String param, String query, String body, 
			 String headers, ExecutionLogger el, ConcurrentHashMap<String, String> varMap) {
		 Object result = "";

		 try {			 	
			 	HttpURLConnection connection = getPostConnection(url , body, headers, el, "POST");
		        int responseCode = connection.getResponseCode();
		        
		        if (responseCode == HttpURLConnection.HTTP_OK) {
		        	BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
		            String inputLine;
		            StringBuffer response = new StringBuffer();
		            while ((inputLine = in.readLine()) != null) {
		                response.append(inputLine);
		            }
		            in.close();
		            if(el != null) {
		            	el.logExecution("API Response : " + response.toString());
		            }
		            
		         // Parse the API response as JSON using org.json.simple
		            JSONParser parser = new JSONParser();
	                Object jsonResponse = parser.parse(response.toString());
//	                result = response.toString();
	                result = jsonResponse;
		        } else {
		        	result = connection.getResponseMessage();
		        }
		        
		        if(el != null) {
			        if (responseCode == HttpURLConnection.HTTP_OK) {
			            el.logExecution("Posted Successfully" );	               
			        } else {
			        	el.logExecution("Post Failed " + connection.getResponseMessage() );
			        }
		        }
		 }catch(Exception e) {
			 e.printStackTrace();
		 }
		 
		 return result;
	 }
	 
	 public static String testPostApi(String url, String param, String query, String body, 
			 String headers, ExecutionLogger el, ConcurrentHashMap<String, String> varMap,
			 String tests) {
		 String result = "PASS";
		 int failcount = 0;
		 String failcountstr = "";
		 JSONParser parser = new JSONParser();		 
		 
		 try {			 	
			 	HttpURLConnection connection = getPostConnection(url , body, headers, el, "POST");
		        int responseCode = connection.getResponseCode();
		        el.logExecution("Status Code returned for the api call : " + responseCode );
		        
		        if(el != null) {
			        if (responseCode == HttpURLConnection.HTTP_OK) {
			            el.logExecution("Posted Successfully" );	               
			        } else {
			        	el.logExecution("Post Failed " + connection.getResponseMessage() );
			        }
		        }
		        
		        
		        
	        	BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
	            String inputLine;
	            StringBuffer response = new StringBuffer();
	            while ((inputLine = in.readLine()) != null) {
	                response.append(inputLine);
	            }
	            in.close();
	            if(el != null) {
	            	el.logExecution("API Response : " + response.toString());
	            }
		         
	            JSONObject testsjson = null;
	            
	            if(tests != null && !tests.equals("")) {
	            	testsjson = (JSONObject)parser.parse(tests);
	            }
	            if(testsjson == null || testsjson.size() == 0) {
	            	return "PASS";
	            }
		            
	            String testCode = (String)testsjson.get("httpresponsecode");
	            	        
	    		if(testCode != null && !testCode.equals("")) {
	    			el.logExecution("HTTP Status Code to be asserted : " + testCode ); 
	    			if(testCode.equals((Integer.valueOf(responseCode)).toString())) {
	    				el.logExecution("HTTP Status Code assertion passed : " + testCode ); 
	    			} else {
	    				el.logExecution("HTTP Status Code assertion failed : Expected is :" + testCode
	    						+ " Actually returned is : " + responseCode);
	    				result = "FAIL";
	    				failcount = failcount + 1;
	    			}
	    		} else {
	    			el.logExecution("No status code defined ");
	    		}
		            
	            Iterator iter = testsjson.keySet().iterator();
	            Object jsonResponse = parser.parse(response.toString());
	            
	            while(iter.hasNext()) {
	            	String key = (String)iter.next();
	            	String expectedValue = null;
	            	try {
	            		expectedValue = (testsjson.get(key)).toString();
	            	}catch (NullPointerException ne) {
	            		
	            	}
		            el.logExecution("--------------------------------------------------------------------------");
		            el.logExecution("Asserting key :  " + key);
		            el.logExecution("Expected Value is : " + expectedValue );
		            String value = null;
		            try {
		            	if(key != null && !key.equals("httpresponsecode")) {
		            		value = getDataFromJSON(jsonResponse, key, el);
		            	} else {
		            		continue;
		            	}
		            } catch(Exception e) {
		            	if(expectedValue == null ) {
		            		el.logExecution("The assertion passed as the key : " + key + " was null" );
		            	} else {
		            		el.logExecution(e);
		            		el.logExecution("The assertion failed as the key : " + key + " was not found in the response" );
			       	 		result = "FAIL";
			       	 		failcount = failcount + 1;
		            	}
		            	continue;
		            }
		            el.logExecution("Value in response for key : " + key + " -->" + value );		            	
		            	
		            if(expectedValue.equalsIgnoreCase("non-empty")) {
		            	if(value != null && !value.equals("")) {
		            		el.logExecution("The assertion passed as the return value is non-empty");
		            	} else {
		            		el.logExecution("The assertion failed as the return value is empty");
		            		result = "FAIL";
		            		failcount = failcount + 1;
		            	}
		            } else if(expectedValue.equals(value)) {
		            	el.logExecution("The assertion passed as the expected value matches the return value ");
		            } else {
		            	el.logExecution("The assertion failed as the expected value did not match the return value ");
		            	result = "FAIL";
		            	failcount = failcount + 1;
		            }
	            }
	            failcount = testsjson.size() - failcount;
	            failcountstr = failcount + "/" + testsjson.size() + " tests passed";
		 }catch(Exception e) {
			 e.printStackTrace();
			 el.logExecution(e);
			 el.logExecution("The assertion failed as there was an error" );
			 result = "FAIL";
		 }
		 
		 return result+ AppProperties.delimiter + failcountstr;
	 }
	 
	 public static String replaceParams(String url, String params,ConcurrentHashMap<String, String> varMap) {
		 String[] paramArr = params.split(AppProperties.testdatadelimiter);
		 
		 for(int i=0; i < paramArr.length; i++) {
			 String var = AppProperties.testdatadelimiter + paramArr[i] + AppProperties.testdatadelimiter;
			 if(varMap.keySet().contains(var)) {
				 String value = varMap.get(var);
//				 url = url.replace(AppProperties.testdatadelimiter + i , paramArr[i]);
				 url = url.replace(var , value);
			 }
		 }
		 
		 return url;
	 }
	 
	 
	 public static String getDataFromJSON(Object jsonResponse, String query, ExecutionLogger el) {
//         String query = "[10]~id";
//		 query = "products~[11]~images~[2]";
         
         String[] phrases = query.split(AppProperties.testdatadelimiter);
		 Object obj = jsonResponse;
         for (int i=1;i<phrases.length;i++) {
         	String phrase = phrases[i];
         	if(i == phrases.length - 1) {
         		if(obj instanceof JSONObject) {
         			String rv = (((JSONObject)obj).get(phrase)).toString();
         			el.logExecution("returned value from JSONObject(String) : " + phrase + "--" + rv);
         			 return rv;
         		} else if(phrase.contains("[")) {
         			String posStr = phrase.substring(1,phrase.length()-1);
             		int pos = Integer.valueOf(posStr);
             		String rv = (((JSONArray)obj).get(pos)).toString();
         			el.logExecution("returned value from JSONArray[] : " + phrase + "--" + rv);
         			return rv;
         		} else {
         			el.logExecution("returned value String : " + phrase + "--" + (String)obj);
         			return (String)obj;
         		}
         	} else if(phrase.contains("[")) {
         		el.logExecution("Retrieving API Value : is a JSONArray");
         		String posStr = phrase.substring(1,phrase.length()-1);
         		int pos = Integer.valueOf(posStr);
         		obj = ((JSONArray)obj).get(pos);	                    	
             } else {
            	 el.logExecution("Retrieving API Value : is a JSONObject");
             	obj = ((JSONObject)obj).get(phrase);
             }
         	
         	if(obj == null) {
         		el.logExecution("Obj is null - is this expected? ");
         	} else {
         		el.logExecution("Key : " + phrase + " value :  "  + obj.toString());
         	}
         }
         
         return null;
	 }

}
