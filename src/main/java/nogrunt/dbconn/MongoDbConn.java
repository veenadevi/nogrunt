package nogrunt.dbconn;

import nogrunt.*;

import com.mongodb.client.*;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.json.simple.JSONObject;
import org.json.simple.JSONArray;
import org.json.simple.parser.JSONParser;  
import java.util.List;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;

public class MongoDbConn {
	
	public static void main(String[] args) {
		String uri = "mongodb://localhost:27017/local";
		String custUsername = "name";
		String custPwd = "pwd";
		 String query = "{ \"collectionName\": \"otp\", \"query\": { \"userID\": \"user123\", \"isUsed\": false }, \"sort\": { \"expirationTime\": -1 }, \"limit\": 1, \"projection\": { \"otp\": 1 } }";
		MongoDbConn mdc = new MongoDbConn();
		mdc.readFromMongodb(uri, custUsername, custPwd, query, new ExecutionLogger("test","test"));
	}

    public String readFromMongodb(String uri, String custUsername, 
    		String custPwd, String intent, ExecutionLogger el) {
        // MongoDB connection URI with the database name already included
//        String uri = "mongodb://localhost:27017/local";  // "tvs" is included as part of the URI

        uri = uri.replace("uname", custUsername);
        uri = uri.replace("pwd", custPwd);
    	// Example UI input (this would be dynamically provided from the UI)
       // intent = "{ \"collectionName\": \"otp\", \"query\": { \"userID\": \"user123\", \"isUsed\": false }, \"sort\": { \"expirationTime\": -1 }, \"limit\": 1, \"projection\": { \"otp\": 1 } }";

     // Extract the database name from the URI using URI parsing
        String databaseName = "";
        try {
	        URI dbUri = new URI(uri);
	        String path = dbUri.getPath();
	        databaseName = path.startsWith("/") ? path.substring(1) : path;
	        uri = uri.replace(databaseName, "admin");
        } catch (Exception e) {
        	
        }
        
        // Parse the UI input as a JSONObject using org.simple.json
        JSONParser parser = new JSONParser();
        JSONObject jsonInput = null;
        try {
        	jsonInput = (JSONObject) parser.parse(intent);
        } catch(Exception e) {
        	
        }

        // Extract the necessary fields from the UI input
        String collectionName = (String)jsonInput.get("collectionName");
        String queryJson = (String)jsonInput.get("query").toString();
        String sortJson = null;
        if(jsonInput.get("sort") != null) {
        	sortJson = (String)jsonInput.get("sort").toString();
        }
        
        Integer limit = null;
        if(jsonInput.get("limit") != null) {
        	long limitl = (Long)jsonInput.get("limit");
        	limit = (int)limitl;
        }
        
        String projectionJson = null;
        if(jsonInput.get("projection") != null) {
        	projectionJson = jsonInput.get("projection").toString();
        }

        // Error handling: Ensure collectionName and query are provided
        if (collectionName == null || collectionName.isEmpty()) {
            System.out.println("Error: Collection name is missing or invalid.");
            return null;
        }

        if (queryJson == null || queryJson.isEmpty()) {
            System.out.println("Error: Query is missing or invalid.");
            return null;
        }

        try (MongoClient mongoClient = MongoClients.create(uri)) {

            // Parse the query JSON into a Bson object
            Bson query = Document.parse(queryJson);

            // Handle default for limit if it is null
            if (limit == null) {
                limit = 0;  // Return all results if no limit is specified
                System.out.println("Limit not provided, returning all results.");
            }

            // Handle default for sort if it is null
            Bson sort = null;
            if (sortJson == null) {
                sort = new Document();  // No sorting, maintain the order they were received
                System.out.println("Sort not provided, no sorting applied (documents returned in their current order).");
            } else {
                sort = Document.parse(sortJson);
            }

            // Handle default for projection if it is null
            Bson projection = null;
            if (projectionJson == null) {
                projection = new Document();  // Return all fields if projection is missing
                System.out.println("Projection not provided, returning all fields.");
            } else {
                projection = Document.parse(projectionJson);
            }
            
            MongoDatabase database = mongoClient.getDatabase(databaseName);

            // Get the collection dynamically based on the collection name from the UI
            MongoCollection<Document> collection = database.getCollection(collectionName);

            // Build the query dynamically
            FindIterable<Document> result = collection.find(query)
                                              .sort((Document) sort)  // Apply sorting from UI input (or default)
                                              .limit(limit)           // Apply limit from UI input (0 = no limit)
                                              .projection(projection);
            
            Document firstDoc = result.first();
            String res = null;
            
            if(firstDoc != null) {
            	 Map.Entry<String, Object> entry = firstDoc.entrySet().iterator().next();
                 String fieldName = entry.getKey();  // Field name (e.g., "otp")
                 res = entry.getValue().toString();
            }

            // Check if any documents are returned
            if (res != null) {
                // Extract and print the result
                return res;
            } 

        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Error occurred while processing the query: " + e.getMessage());
        }
        
        return null;
    }
}
