package nogrunt;

import java.net.HttpURLConnection;
import javax.net.ssl.HttpsURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.util.Base64;
import java.io.OutputStream;

import org.json.simple.JSONObject;
import org.json.simple.JSONArray;
import org.json.simple.parser.JSONParser;



public class OpenAIClient {

    public static void main(String[] args) {
    	SelGrid sg = new SelGrid();
    	AppProperties.getProperties();
    	String result = sg.defaultTestData("~RANDOMALPHA_1-8~", true,"","","","");
        System.out.println(result);
    }
    
    public static String genData(String prompt, int howMany, int maxTokens) {
    	String result = "";
    	try {
 //           prompt = "Return 10 random Indian first name suggestions. The names should be unique.";
            String model = "text-davinci-002";
            model = "gpt-3.5-turbo";
            String apiKey = "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";
            URL url = new URL("https://api.openai.com/v1/completions");
            
            HttpsURLConnection  connection = (HttpsURLConnection ) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Authorization", "Bearer " + apiKey);
//            connection.setDoInput(true);
            connection.setDoOutput(true);

            String body = "{\"model\": \"" + model+ "\", \"prompt\": \"" + prompt + "\", \"max_tokens\": " + maxTokens + "}";
            // Create JSON request body
//            JSONObject jsonBody = new JSONObject();
//            		jsonBody.put("model", "text-davinci-003");
//            		jsonBody.put("prompt", prompt);
//            		jsonBody.put("max_tokens", maxTokens);
            byte[] input = body.getBytes(StandardCharsets.UTF_8);
            		OutputStream outputStream = connection.getOutputStream();
                    outputStream.write(input);
                    outputStream.flush();
                    outputStream.close();
                    
            int responseCode = connection.getResponseCode();        
            
            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8));
            StringBuilder responseBuilder = new StringBuilder();
            String responseLine;
            while ((responseLine = reader.readLine()) != null) {
                responseBuilder.append(responseLine.trim());
            }
            String response = responseBuilder.toString();
            JSONParser parser = new JSONParser();
            JSONObject jsonObj = null;
            try {
            	jsonObj = (JSONObject) parser.parse(response);
            } catch (Exception e) {
            	e.printStackTrace();
            }
            
            JSONArray choices = (JSONArray)jsonObj.get("choices");
            JSONObject results = (JSONObject)choices.get(0);
            result = (String)results.get("text");
            if(howMany > 1) {
            	result = result.substring(4,result.length());
            }
//            result = response.substring(response.indexOf("\"text\": \"") + 9, response.indexOf("\", \""));
            
            while(result.startsWith("\n")) {
            	result = result.substring(1,result.length());
            }
            
            int endIndex = result.length();
            if(howMany > 1) {
            	endIndex = result.indexOf("\n");
            }
            
            result = result.substring(0,endIndex);
            System.out.println("Random first name suggestion: " + result);
            reader.close();
            connection.disconnect();
        } catch (IOException e) {
            e.printStackTrace();
        }
    	
    	return result;
    }
    

}


