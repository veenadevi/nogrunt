package nogrunt.integrations;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import java.util.Base64;
import org.apache.http.entity.StringEntity;
import org.apache.http.client.methods.HttpPost;

public class JiraIntegration {
	
	public static void main(String[] args) {
		
		String apiToken = "ATATT3xFfGF0fNAbwZFK0ZmNsk237BzaiAZmrn2CojtXDQi3PgAmaQxzsS_CQv0sPZtplJ7fv0tlaAMpOevI8Q0UZ5LNyrXMZy-VrrFUbgxXyOuZWY5WskDmGzescPh4pAsoKGOfUleixAOyXjJN1ln4gEumXZdW7bcm6abJnV8SPTt0uilhdi4=85BEDA61"; // Replace with your Jira API token
        String username = "deva@nogrunt.com"; // Replace with your Jira username
        String jiraBaseUrl = "https://nogrunt.atlassian.net"; // Replace with your Jira instance URL
        String issueKey = "SCRUM-2"; // Replace with the Jira issue key you want to retrieve
        String commentText = "This is a new comment added via the API.";
        
        JiraIntegration ji = new JiraIntegration();
        
        ji.updateTSResult(jiraBaseUrl, apiToken, commentText, 
    			issueKey, username);
     
    }
	
	public void updateTSResult(String jiraBaseUrl, String apiToken, String commentText, 
			String issueKey, String username) {
		
//		String apiToken = "ATATT3xFfGF0fNAbwZFK0ZmNsk237BzaiAZmrn2CojtXDQi3PgAmaQxzsS_CQv0sPZtplJ7fv0tlaAMpOevI8Q0UZ5LNyrXMZy-VrrFUbgxXyOuZWY5WskDmGzescPh4pAsoKGOfUleixAOyXjJN1ln4gEumXZdW7bcm6abJnV8SPTt0uilhdi4=85BEDA61"; // Replace with your Jira API token
//        String username = "deva@nogrunt.com"; // Replace with your Jira username
//        String jiraBaseUrl = "https://nogrunt.atlassian.net"; // Replace with your Jira instance URL
//        String issueKey = "SCRUM-2"; // Replace with the Jira issue key you want to retrieve
		
		// Construct the authorization header
        String authorizationHeader = "Basic " + java.util.Base64.getEncoder().encodeToString((username + ":" + apiToken).getBytes());
        
     // Create an HTTP client
        HttpClient httpClient = HttpClients.createDefault();
        
     // Construct the GET request to retrieve the Jira issue
        String apiUrl = jiraBaseUrl + "/rest/api/2/issue/" + issueKey + "/comment";
        
     // Define the comment you want to add
//        String commentText = "This is a new comment added via the API.";
        
        
     // Construct the JSON payload for the new comment
        String jsonPayload = "{\"body\":\"" + commentText + "\"}";
        
        
     // Create a POST request to add the comment
        HttpPost request = new HttpPost(apiUrl);
        request.setHeader("Authorization", authorizationHeader);
        request.setHeader("Content-Type", "application/json");
        request.setEntity(new StringEntity(jsonPayload, "UTF-8"));
        
//        HttpGet request = new HttpGet(apiUrl);
//        request.setHeader("Authorization", authorizationHeader);
        
        try {
            // Execute the request
            HttpResponse response = httpClient.execute(request);

            // Check the response status code
            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode == 201) {
                System.out.println("Comment added to the Jira issue successfully.");
            } else {
                System.err.println("Failed to add comment to Jira issue. Status code: " + statusCode);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
	}

}
