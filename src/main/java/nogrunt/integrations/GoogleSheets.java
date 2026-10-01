package nogrunt.integrations;

import nogrunt.*;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.SheetsScopes;
import com.google.api.services.sheets.v4.model.ValueRange;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeRequestUrl;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeTokenRequest;
import com.google.api.client.auth.oauth2.TokenResponse;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import javax.print.attribute.AttributeSetUtilities;
import java.io.File;
import java.io.FileInputStream;

public class GoogleSheets {
	private static final String APPLICATION_NAME = "Google Sheets API Java Quickstart";
	  private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
	  private static final String TOKENS_DIRECTORY_PATH = "tokens\\";
	  

	  /**
	   * Global instance of the scopes required by this quickstart.
	   * If modifying these scopes, delete your previously saved tokens/ folder.
	   */
	  private static final List<String> SCOPES =
	      Collections.singletonList(SheetsScopes.SPREADSHEETS_READONLY);
	  
	  private static GoogleClientSecrets getClientSecrets(int companyId) throws IOException{
		  String credsfile =  Utilities.getGoogleOAuthCredPath(companyId) + "\\credentials.json";
			File file = new File(credsfile);
			if (!file.exists()) {
			    throw new FileNotFoundException("File not found: " + credsfile);
			}
		    
			InputStream in = new FileInputStream(file);

		    GoogleClientSecrets clientSecrets =
		        GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));
		    
		    return clientSecrets;
	  }
	  
	  
	  /**
	   * Creates an authorized Credential object.
	   *
	   * @param HTTP_TRANSPORT The network HTTP Transport.
	   * @return An authorized Credential object.
	   * @throws IOException If the credentials.json file cannot be found.
	   */
	  private static Credential getCredentials(final NetHttpTransport HTTP_TRANSPORT, int companyId)
	      throws IOException {
		  
		  GoogleClientSecrets clientSecrets = getClientSecrets(companyId);
	    
	    // Build flow and trigger user authorization request.
	    GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
	        HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, SCOPES)
	        .setDataStoreFactory(new FileDataStoreFactory(new java.io.File(TOKENS_DIRECTORY_PATH+companyId)))
	        .setAccessType("offline")
	        .build();
	    LocalServerReceiver receiver = new LocalServerReceiver.Builder()
	    		.setPort(8887)
	    		.setCallbackPath("/Callback")
	    		.build();
	    Credential credential = new AuthorizationCodeInstalledApp(flow, receiver).authorize("user");
	    String refreshToken = credential.getRefreshToken();
	    System.out.println(refreshToken);
	    return credential;
	  }
	  
	  public static Sheets getSheets(int companyId)
		      throws IOException {
		  Sheets sheet= null;
		  try {
			    final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
			    sheet =
			        new Sheets.Builder(HTTP_TRANSPORT, JSON_FACTORY, 
			        		getCredentials(HTTP_TRANSPORT,companyId))
			            .setApplicationName(APPLICATION_NAME)
			            .build();
		  }catch (Exception e) {
			  e.printStackTrace();
		  }
		  return sheet;
	  }

	  /**
	   * Prints the names and majors of students in a sample spreadsheet:
	   * https://docs.google.com/spreadsheets/d/1BxiMVs0XRA5nFMdKvBdBZjgmUUqptlbs74OgvE2upms/edit
	   */
	  public static void main(String... args)  {
		  try {
			  getCellValue("1GqKEVJ1wjCTLWIOMOmj0p9BYChHH3P1CCyk9MRO7MSM", "Sheet1!A1:A3", 1);
		  } catch (Exception e) {
			  e.printStackTrace();
		  }
	  }
	  
	  public static String getCellValue(String spreadsheetId, String range, int companyId) throws Exception{
		  String result = "";
		  try {
		    Sheets service = getSheets(companyId);
		    ValueRange response = service.spreadsheets().values()
		        .get(spreadsheetId, range)
		        .execute();
		    List<List<Object>> values = response.getValues();
		    
		    if (values == null || values.isEmpty()) {
		      System.out.println("No data found.");
		    } else {
		      for (List row : values) {
		        System.out.printf("%s\n", row.get(0));
		        result = (String)row.get(0);
		      }
		    }
		  }catch (Exception e) {
			  e.printStackTrace();
			  System.err.println(e.getMessage());
			  throw e;
		  }
		    
		    return result;
	  }
	  
		    public static String gSheetsAuthorization(int companyId) throws IOException {
		    	GoogleClientSecrets clientSecrets = getClientSecrets(companyId);
		    	
		    	String clientId = clientSecrets.getDetails().getClientId();
		        String clientSecret = clientSecrets.getDetails().getClientSecret();
		        String redirectUri = clientSecrets.getDetails().getRedirectUris().get(0);
		        
//		        String redirectUri = "http://localhost:8887/Callback";
//		        redirectUri = "http://localhost:9000/keyloggingv2/ReactApp?action=authResponse&companyid=1";
		        
		        NetHttpTransport httpTransport = new NetHttpTransport();
		        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
		                httpTransport, JSON_FACTORY, clientId, clientSecret, SCOPES)
		                .setAccessType("offline")
		                .setApprovalPrompt("force")
		                .build();

		        // Construct the authorization URL
		        GoogleAuthorizationCodeRequestUrl url = flow.newAuthorizationUrl().setRedirectUri(redirectUri);

		        // Display the authorization URL to the user
		        System.out.println("Open the following URL in your browser:");
		        System.out.println(url);
		        return url.toString();
		    }
		    
		    public static void handleCallback(String authorizationCode, int companyId) throws Exception{
		    	
		    	try {
			    	GoogleClientSecrets clientSecrets = getClientSecrets(companyId);
			    	
			    	String redirectUri = clientSecrets.getDetails().getRedirectUris().get(0);
			    	
			    	final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
			        
			        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
			        		HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, SCOPES)
			            .setDataStoreFactory(new FileDataStoreFactory(new java.io.File(TOKENS_DIRECTORY_PATH+companyId)))
			            .build();
	
			        // Exchange authorization code for access token
			        GoogleAuthorizationCodeTokenRequest tokenRequest = flow.newTokenRequest(authorizationCode);
			        tokenRequest.setRedirectUri(redirectUri);
			        TokenResponse tokenResponse = tokenRequest.execute();
			        flow.createAndStoreCredential(tokenResponse, "user");
		    	} catch (Exception e) {
		    		e.printStackTrace();
		    		System.err.println(e.getMessage());
		    		throw e;
		    	}

		        // Store the credential
		        
		    }
}
