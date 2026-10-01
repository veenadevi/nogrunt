package nogrunt.integrations;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.apache.http.client.methods.HttpPatch;
import org.apache.http.entity.StringEntity;
import org.json.simple.JSONObject;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class ADOIntegration {
	String organizationUrl = "https://dev.azure.com/nogrunt";
	String pat = "tvrfakssamtlkejqhjj3lgm55ofak3uh3fzssfojx5htcui56dha";
	String MSteamURL = "https://nogruntcollab.webhook.office.com/webhookb2/57fd17e9-7290-4cd5-82cc-210acf0c0a97@a9fd8410-c65f-44b3-bb7e-4e5b07255947/IncomingWebhook/8dbac5db45e949ef9deb516bed0d95d9/b56a6a77-98af-421e-ad7c-0eaeefac809c";
	
	public static void main(String[] args) {
		ADOIntegration adoi = new ADOIntegration();
//		adoi.retrieveWorkItem();
//		adoi.updateTSResult(adoi.organizationUrl, adoi.pat, "A message", "1");
//		adoi.retrieveTestCases(adoi.organizationUrl, adoi.pat, "A message", 6, "Inttest");
		adoi.retrieveWorkItem();
	}
	
	public void updateTSResult(String url, String token, String msg, String workItemId) {
        // Replace with your Azure DevOps organization URL
		organizationUrl = url;

        // Replace with your PAT (Personal Access Token)
        String pat = token;

        try {
            // Create an HTTP client
            try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
                // Construct the URL to update a work item
                String workItemUrl = organizationUrl + "/_apis/wit/workitems/" + workItemId + "?api-version=6.0";

               
                // Create an HTTP PATCH request
                HttpPatch httpPatch = new HttpPatch(workItemUrl);

                // Set up authentication using the PAT
                httpPatch.addHeader(BasicScheme.authenticate(
                        new UsernamePasswordCredentials("", pat), "UTF-8", false));

                // Set the request content type to JSON
                httpPatch.setHeader("Content-Type", "application/json-patch+json");

             // Construct a JSON object for the update operations using org.json.simple
                JSONObject updateJsonObject = new JSONObject();
                updateJsonObject.put("op", "add");
                updateJsonObject.put("path", "/fields/System.History");
                updateJsonObject.put("value", msg);

                // Convert the JSON object to a JSON string
               String jsonPayload = updateJsonObject.toJSONString();
               jsonPayload ="[" + jsonPayload + "]";
                
                // Set the request body
                httpPatch.setEntity(new StringEntity(jsonPayload));

                // Execute the request
                HttpResponse response = httpClient.execute(httpPatch);

                // Check the response status
                if (response.getStatusLine().getStatusCode() == 200) {
                    // Get the response entity
                    HttpEntity entity = response.getEntity();

                    // Convert the entity to a string (JSON response)
                    String responseBody = EntityUtils.toString(entity);

                    // Print the JSON response
                    System.out.println("Updated Work Item:\n" + responseBody);
                } else {
                    System.err.println("Failed to update work item. Status code: " + response.getStatusLine().getStatusCode());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
	
	public void retrieveTestCases(String url, String token, String msg, int planId, 
			String project) {
		organizationUrl = url;
		project = "Inttest";
        // Replace with your PAT (Personal Access Token)
        String pat = token;

        try {
            // Create an HTTP client
            try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
                // Construct the URL to update a work item
            	String apiUrl =  String.format("%s/%s/_apis/testplan/plans/%d?api-version=5.0-preview.1",
            			organizationUrl, project, planId);

            	// Create HTTP connection
                URL tpurl = new URL(apiUrl);
                HttpURLConnection connection = (HttpURLConnection) tpurl.openConnection();
                connection.setRequestMethod("GET");
                connection.setRequestProperty("Authorization", "Basic " + java.util.Base64.getEncoder().encodeToString((":" + pat).getBytes()));

             // Read response
                int responseCode = connection.getResponseCode();
                BufferedReader reader;
                if (responseCode == HttpURLConnection.HTTP_OK) {
                	System.out.println("Success");
                    reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                } else {
                    reader = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
                }

                // Output response
                String line;
                StringBuilder response = new StringBuilder();
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();

                // Print test information
                System.out.println("Test Information:");
                System.out.println(response.toString());

                // Close connection
                connection.disconnect();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
	}
	
	public void retrieveWorkItem() {
        // Replace with your TFS/Azure DevOps organization URL
//        String organizationUrl = "https://dev.azure.com/nogrunt";
//
//        // Replace with your PAT (Personal Access Token)
//        String pat = "tvrfakssamtlkejqhjj3lgm55ofak3uh3fzssfojx5htcui56dha";

        // Replace with the ID of the work item you want to retrieve
        int workItemId = 4;

        try {
            // Create an HTTP client
            try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
                // Construct the URL to retrieve a work item
                String workItemUrl = organizationUrl + "/_apis/wit/workitems/" + workItemId + "?api-version=6.0";

                // Create an HTTP GET request
                HttpGet httpGet = new HttpGet(workItemUrl);

                // Set up authentication using the PAT
                httpGet.addHeader(BasicScheme.authenticate(
                        new UsernamePasswordCredentials("", pat), "UTF-8", false));

                // Execute the request
                HttpResponse response = httpClient.execute(httpGet);

                // Check the response status
                if (response.getStatusLine().getStatusCode() == 200) {
                    // Get the response entity
                    HttpEntity entity = response.getEntity();

                    // Convert the entity to a string (JSON response)
                    String responseBody = EntityUtils.toString(entity);

                    // Print the JSON response
                    System.out.println("Work Item Details:\n" + responseBody);
                } else {
                    System.err.println("Failed to retrieve work item. Status code: " + response.getStatusLine().getStatusCode());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
