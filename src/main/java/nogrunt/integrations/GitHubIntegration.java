package nogrunt.integrations;

import nogrunt.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.*;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class GitHubIntegration {
	
	public static void main(String[] args) {
		String owner = "boothana"; // Replace with the GitHub repository owner's username or organization name
        String repo = "keyloggingv2";   // Replace with the GitHub repository name
        String token =  "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";
        // Define the date from which you want to retrieve commits
        String dateStr = "2023-11-01 00:00"; // Desired date in yyyy-MM-dd HH:mm format
        
        GitHubIntegration gi = new GitHubIntegration();
        List<String> ja = gi.getTags(owner, repo, token, dateStr,"https://api.github.com/repos/");
        System.out.println(ja.toString());
	}
	
	public List<String> getTags(int prodid, String repo, String fromStr, String toStr, MySqlConn2 msc2) {
		
		JSONObject prodIntDets = msc2.getIntegrationdetail(prodid, "Github");
		String  conn = (String)prodIntDets.get("adminprop");
		if(conn == null) {
			return new JSONArray();
		}
		String[] connDetails = conn.split(","); 
		List<String> descs = new ArrayList<>();
		List<String> tempDesc = getTags(connDetails[0], repo, connDetails[1], fromStr,connDetails[2]);
		descs = msc2.mergeAndEliminateDuplicates(descs, tempDesc);
		return descs;
	}
	
	public List<String> getTags(String owner, String repo, String token, String dateStr, 
			String gitUrl) {

		List<String> ja = new ArrayList<>();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        if(!dateStr.contains("T")) {
        	dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        }
       
        try {
        	Date fromDate = dateFormat.parse(dateStr); // Replace with your desired date
        	String fromDateStr = dateFormat.format(fromDate);
        	fromDateStr = URLEncoder.encode(fromDateStr, StandardCharsets.UTF_8.toString());
//            URL url = new URL("https://api.github.com/repos/" + owner + "/" + repo + "/commits?since=" + fromDateStr);
            URL apiUrl  = new URL(gitUrl + owner + "/" + repo + "/commits?since=" + fromDateStr);
            
//            List<String> allCommits = new ArrayList<>();
            int pageNumber = 1;
            
            while(true) {
            	String urlWithPage = apiUrl  + "&page=" + pageNumber;
            	URL url = new URL(urlWithPage);
	            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
	            conn.setRequestMethod("GET");
	            conn.setRequestProperty("Authorization", "token " + token);
	
	            int responseCode = conn.getResponseCode();
	            if (responseCode == HttpURLConnection.HTTP_OK) {
	                BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
	                String inputLine;
	                StringBuffer response = new StringBuffer();
	
	                while ((inputLine = in.readLine()) != null) {
	                    response.append(inputLine);
	                }
	                in.close();
	
	             // Parse the JSON response into a JSON array
	                JSONParser parser = new JSONParser();
	                JSONArray jsonArray = (JSONArray) parser.parse(response.toString());
	
	                // Process the JSON array (jsonArray) as needed
	                // For example, you can iterate through commits
	                
	                for (Object commitObj : jsonArray) {
	                    JSONObject commit = (JSONObject) commitObj;
	//                    String commitSha = (String) commit.get("sha");
	                    JSONObject commitInfo = (JSONObject) commit.get("commit");
	                    String commitMessage = (String) commitInfo.get("message");
	//                    System.out.println("Commit SHA: " + commitSha);
	//                    System.out.println("Commit Message: " + commitMessage);
	                    ja.add(commitMessage);
	                }
	                
	             // Check if there's a "Link" header for pagination
                    Map<String, List<String>> headers = conn.getHeaderFields();
                    List<String> linkHeader = headers.get("Link");

                    if (linkHeader == null || !linkHeader.toString().contains("rel=\"next\"")) {
                        break; // No more pages to fetch
                    }

                    pageNumber++;
	            } else {
	                // Print the response body in case of an error
	                BufferedReader errorIn = new BufferedReader(new InputStreamReader(conn.getErrorStream()));
	                String errorInputLine;
	                StringBuilder errorResponse = new StringBuilder();
	
	                while ((errorInputLine = errorIn.readLine()) != null) {
	                    errorResponse.append(errorInputLine);
	                }
	                errorIn.close();
	
	                System.out.println("Error Response Body:\n" + errorResponse.toString());
	                break;
	            }
	            conn.disconnect();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        return ja;
    }

}
