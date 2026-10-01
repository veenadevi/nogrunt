package nogrunt.api;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import org.apache.pdfbox.pdmodel.PDDocument;

import nogrunt.*;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.zip.GZIPInputStream;

public class ApiHandler {
    public static void main(String[] args) throws Exception {
        String apiUrl = "http://your-api-endpoint.com/resource";

        // Example JSON array of query parameters
        String jsonParams = "[{\"idapiparams\":108,\"value\":\"getAllApi\",\"key\":\"action\"}," +
                            "{\"idapiparams\":109,\"value\":\"59\",\"key\":\"companyid\"}," +
                            "{\"idapiparams\":110,\"value\":\"43869815\",\"key\":\"token\"}," +
                            "{\"idapiparams\":111,\"value\":\"54\",\"key\":\"ModuleId\"}]";

        // Example for GET request
        String responseGet = sendRequest(apiUrl, "GET", "{\"optionalKey\":\"optionalValue\"}", jsonParams,
        		"Custom-Header:HeaderValue", null, null,-1,-1,null);
        System.out.println("GET Response: " + responseGet);

        // Example for POST request
        String jsonInputString = "{\"key\":\"value\"}";
        String responsePost = sendRequest(apiUrl, "POST", jsonInputString, null, "Another-Header:HeaderValue", 
        		null,null,-1,-1,null);
        System.out.println("POST Response: " + responsePost);
    }

    public static String sendRequest(String apiUrl, String method, String body, String jsonParams, 
    		String headers, String callType, String tests, int companyId, int apiid, ExecutionLogger el) 
    				throws Exception {
    	
    	JSONParser parser = new JSONParser();
        // Add query parameters to the URL if provided
        if (jsonParams != null && !jsonParams.isEmpty()) {
            StringBuilder queryBuilder = new StringBuilder();
            
            JSONArray jsonArray = (JSONArray) parser.parse(jsonParams);

            for (Object obj : jsonArray) {
                JSONObject jsonObject = (JSONObject) obj;
                String key = (String) jsonObject.get("key");
                String value = (String) jsonObject.get("value");
                if (queryBuilder.length() > 0) {
                    queryBuilder.append("&");
                }
                queryBuilder.append(key).append("=").append(value);
            }
            
            if((!apiUrl.contains("?"))) {
            	apiUrl += "?" + queryBuilder.toString();
            } else {
            	apiUrl += queryBuilder.toString();
            }
        }

        HttpURLConnection connection = (HttpURLConnection) new URL(apiUrl).openConnection();
        connection.setRequestMethod(method);
        connection.setRequestProperty("Content-Type", "application/json");

        // Parse and add headers if provided
        
        if(headers != null && !headers.equals("")) {
	        JSONObject headersjson = (JSONObject)parser.parse(headers);
	        for (Object key : headersjson.keySet()) {
                connection.setRequestProperty((String) key, (String) headersjson.get(key));
            }
        }
        
        // Set doOutput to true for POST, PUT, PATCH, DELETE (if body is included), and GET (if body is included)
        if ("POST".equalsIgnoreCase(method) || "PUT".equalsIgnoreCase(method) || 
            "PATCH".equalsIgnoreCase(method) || 
            ("DELETE".equalsIgnoreCase(method) && body != null && body.contains(":") 
            && !body.equals("{ }")) || 
            ("GET".equalsIgnoreCase(method) && body != null && body.contains(":") 
            && !body.equals("{ }"))) {
            
            connection.setDoOutput(true);
            if (body != null) {
                try (OutputStream os = connection.getOutputStream()) {
                    byte[] input = body.getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);
                }
            }
        }

        // Read the response
        StringBuilder response = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
            String responseLine;
            while ((responseLine = br.readLine()) != null) {
                response.append(responseLine.trim());
            }
        } catch (IOException e) {
            // Handle error response if necessary
        	System.err.println(Utilities.getNow());
            System.err.println("Error reading response: " + e.getMessage());
            
            BufferedReader  reader = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
            response = new StringBuilder();
            String responseLine;
            while ((responseLine = reader.readLine()) != null) {
                response.append(responseLine.trim());
            }            
            
            if(callType != null && callType.equalsIgnoreCase("test")) {
            	return "FAIL"+ AppProperties.delimiter + "No Tests Passed"+ AppProperties.delimiter + response.toString();
            }
        }
        
        System.out.println(Utilities.getNow());
        System.out.println("Response Body: " + response.toString());
        
        int responseCode = connection.getResponseCode();
        connection.disconnect();
        
        if(callType != null && callType.equalsIgnoreCase("test")) {
        	String res = runTests(tests, responseCode, el, response);
        	return res;
        } else {
        	return response.toString();
        }
    }
    
    public static String runTests(String tests, int responseCode, ExecutionLogger el,
    		StringBuilder response) {
    	
    	String result = "PASS";
    	int failcount = 0;
		 String failcountstr = "";
		 
		 try {
	    	JSONParser parser = new JSONParser();
	    	JSONObject testsjson = (JSONObject)parser.parse(tests);
	        if(testsjson == null || testsjson.size() == 0) {
	        	return "TRUE";
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
				el.logExecution("Status code assertion not expected");
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
	            		value = ApiAccess.getDataFromJSON(jsonResponse, key, el);
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
	            	
	            if(expectedValue == null && value != null) {
	            	el.logExecution("The assertion failed as the return value is not null while expected value is null");
            		result = "FAIL";
            		failcount = failcount + 1;
	            } else if(expectedValue != null && value == null) {
	            	el.logExecution("The assertion failed as the return value is null and expected value is not null");
            		result = "FAIL";
            		failcount = failcount + 1; 
	            } else if(expectedValue.equalsIgnoreCase("non-empty")) {
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
		 }catch (Exception e) {
			 e.printStackTrace();
			 el.logExecution(e);
			 el.logExecution("The assertion failed as there was an error" );
			 result = "FAIL";
		 }
		 
		 return result+ AppProperties.delimiter + failcountstr+AppProperties.delimiter + response.toString();
    }
    
    
    public static String streamValidatePDF(String pdfUrl, String searchText, ExecutionLogger el) {
    	
    	String status = "PASS";
        try {
            // Connect to the URL
            URL url = new URL(pdfUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.connect();
            el.logExecution("The Original Text is: " + searchText);
            String[] searchStrings = searchText.split(AppProperties.delimiter);

            // Check if the response is successful
            if (connection.getResponseCode() == 200) {
                try (InputStream inputStream = connection.getInputStream()) {
                    // Load the PDF document from the InputStream
                    PDDocument document = PDDocument.load(inputStream);
                    String pdfText = PDFTextExtractor.getpdfText(document);
                    el.logExecution("The PDf file contained the text: " + pdfText);
                    
                    for (int i=0; i< searchStrings.length; i++) {
                    	String searchString = searchStrings[i];
                    // Check if the text contains the target string
	                    if (pdfText.contains(searchString)) {
	                        el.logExecution("Text found in the PDF: " + searchString);
	                    } else {
	                    	el.logExecution("Text not found in the PDF." + searchString);
	                        status = "FAIL";
	                    }
                    }

                    // Close the document
                    document.close();
                }
            } else {
                System.out.println("Failed to fetch the PDF. HTTP Response Code: " + connection.getResponseCode());
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        
        return status;
    }
    
    public static String streamValidateXML(String xmlUrl, String searchText, ExecutionLogger el) {
    	String status = "PASS";
    	try {
    		// Connect to the URL
            URL url = new URL(xmlUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.connect();
            
            String contentType = connection.getContentType();
            el.logExecution("Content-Type: " + contentType);
            
            String encoding = connection.getContentEncoding();
            el.logExecution("Content-Encoding: " + encoding);
        
            // Check if the response is successful
            if (connection.getResponseCode() == 200) {
                try {
                	InputStream inputStream = connection.getInputStream();
                	if ("gzip".equalsIgnoreCase(encoding)) {
                        inputStream = new GZIPInputStream(inputStream);
                    }
                    BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));
                    String content = "";
                    String line;
                    while ((line = reader.readLine()) != null) {
                    	content += line;
                    }
                	if (content.contains(searchText)) {
                        el.logExecution("Text found in the XML: " + searchText);
                    } else {
                    	el.logExecution("Text not found in the XML." + searchText);
                        status = "FAIL";
                    }
                    inputStream.close();
                } catch(Exception e) {
                	e.printStackTrace();
                }
            } else {
                System.out.println("Failed to fetch the XML. HTTP Response Code: " + connection.getResponseCode());
            }
    	} catch (Exception e) {
    		e.printStackTrace();
    	}
    	
    	return status;
    }
}