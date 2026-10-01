package nogrunt.integrations;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;

import org.json.simple.JSONObject;
import org.json.simple.JSONArray;
import org.json.simple.parser.JSONParser;
import java.util.Iterator;

public class OpenAITest {

   public static String chatGPT(String prompt) {
       String chatUrl = "https://api.openai.com/v1/chat/completions";
       String assistantUrl = "https://api.openai.com/v1/assistants";
       String apiKey = "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";
//       String chatModel = "gpt-3.5-turbo";
       String chatModel = "gpt-4o-mini";
       String assistantModel = "gpt-4-turbo-preview";
       // Set the seed for random number generator
       int seed = 42; // You can use any integer value as the seed

       // Set the temperature parameter
       double temperature = 0; // Set temperature to 0 for deterministic output
//       String url = assistantUrl;
//       String model = assistantModel;
       String url = chatUrl;
       String model = chatModel;

       try {
           URL obj = new URL(url);
           HttpURLConnection connection = (HttpURLConnection) obj.openConnection();
           connection.setRequestMethod("POST");
           connection.setRequestProperty("Authorization", "Bearer " + apiKey);
           connection.setRequestProperty("Content-Type", "application/json");

           // The request body
//           String body = "{\"model\": \"" + model + "\", \"messages\": [{\"role\": \"user\", \"content\": \"" + prompt + "\"}]"
//           		+ ", \"temperature\": " + temperature + ", \"seed\": " + seed + "}";
           String body = "{\"model\": \"" + model + "\", \"messages\": [{\"role\": \"user\", \"content\": \"" + prompt + "\"}]"
              		+ "}";
//           String body = "{\"prompt\": \"" + prompt + "\", \"max_tokens\": 100}";
           connection.setDoOutput(true);
//           OutputStreamWriter writer = new OutputStreamWriter(connection.getOutputStream());
//           writer.write(body);
//           writer.flush();
//           writer.close();
           
        // Write the request body
           try (OutputStream os = connection.getOutputStream()) {
               byte[] input = body.getBytes("utf-8");
               os.write(input, 0, input.length);
           }

           // Response from ChatGPT
           BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream()));
           String line;

           StringBuffer response = new StringBuffer();

           while ((line = br.readLine()) != null) {
               response.append(line);
           }
           br.close();

           // calls the method to extract the message.
           return extractMessageFromJSONResponse(response.toString());

       } catch (IOException e) {
    	   e.printStackTrace();
           throw new RuntimeException(e);
       }
   }

   public static String extractMessageFromJSONResponse(String response) {
	   String content = null;
	   try {
		   JSONParser parser = new JSONParser();
	       JSONObject jsonObject = (JSONObject) parser.parse(response);
	       JSONArray choices = (JSONArray)jsonObject.get("choices");
	       JSONObject choiceVal = (JSONObject)choices.get(0);
	       JSONObject message = (JSONObject)choiceVal.get("message");
	       content = (String)message.get("content");
    	   System.out.println(content);
	   } catch (Exception e) {
		   e.printStackTrace();
	   }
	   
	   return content;

   }

   public static void main(String[] args) {

       System.out.println(chatGPT2("hello, how are you? Can you tell me what's a Fibonacci Number?"));

   }
   
   
   public static String chatGPT2(String prompt) {
       String apiKey = "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"; // Replace with your OpenAI API key
       String model = "gpt-4o-mini"; // Replace with the correct model if necessary
       String apiUrl = "https://api.openai.com/v1/chat/completions";

       try {
           // Create the URL object
           URL url = new URL(apiUrl);
           HttpURLConnection conn = (HttpURLConnection) url.openConnection();
           conn.setRequestMethod("POST");
           conn.setRequestProperty("Authorization", "Bearer " + apiKey);
           conn.setRequestProperty("Content-Type", "application/json");
           conn.setDoOutput(true);

           // Create the JSON request body
           String jsonInputString = "{"
               + "\"model\": \"" + model + "\","
               + "\"messages\": [{"
               + "\"role\": \"user\","
               + "\"content\": \"" + prompt + "\""
               + "}],"
               + "\"temperature\": 0.0"
               + "}";

           // Write the request body
           try (OutputStream os = conn.getOutputStream()) {
               byte[] input = jsonInputString.getBytes("utf-8");
               os.write(input, 0, input.length);
           }

           // Read the response
           try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), "utf-8"))) {
               StringBuilder response = new StringBuilder();
               String responseLine;
               while ((responseLine = br.readLine()) != null) {
                   response.append(responseLine.trim());
               }
               System.out.println("Response: " + response.toString());
               return response.toString();
           }

       } catch (Exception e) {
           e.printStackTrace();
       }
       return null;
   }
}



