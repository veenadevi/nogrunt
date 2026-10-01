package nogrunt.api;

import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.tags.Tag;
import io.swagger.v3.oas.models.info.*;
import io.swagger.v3.oas.models.servers.*;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.responses.ApiResponse;

import java.io.File;
import java.util.Map;
import java.util.List;
import java.util.Iterator;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import nogrunt.*;

public class SwaggerJSONIngestor {
	
	public static void main(String[] args) {
//		SwaggerJSONIngestor sji = new SwaggerJSONIngestor();
//		sji.parseSwagger("C:\\keyloggingv2\\swaggeringestion.json");
	}
	
	public void parseSwagger(String swaggerPath, MySQlConn msc, int moduleId, 
			String randomKey) {
		// Path to your Swagger JSON or YAML file
//		swaggerPath = "C:\\keyloggingv2\\swaggeringestion.json";
    
		// Parse the Swagger document
		OpenAPI openAPI = new OpenAPIV3Parser().read(swaggerPath);
    
		if (openAPI != null) {
	        // Process each path and operation
	        Paths paths = openAPI.getPaths();
	        List<Tag> tagsList = openAPI.getTags();
	        JSONObject tags = processTags(tagsList, msc, moduleId);
	        List<Server> servers = openAPI.getServers();
	        Iterator serverIter = servers.iterator();
	        Components components = openAPI.getComponents();
	        JSONObject comps = saveComponents(components);
	        
	        Server server = (Server)serverIter.next();
	        String url = server.getUrl();
	        
	        Info info = openAPI.getInfo();
	        String version = info.getVersion();
	        for (Map.Entry<String, PathItem> entry : paths.entrySet()) {
	            String path = entry.getKey();
	            PathItem pathItem = entry.getValue();
	            String fullURL = url + path;
	            
	            // Iterate over HTTP methods (GET, POST, etc.) defined for this path
	            for (Map.Entry<PathItem.HttpMethod, Operation> methodEntry : pathItem.readOperationsMap().entrySet()) {
	                PathItem.HttpMethod method = methodEntry.getKey();
	                Operation operation = methodEntry.getValue();
	                int itemId = -1;
	                // Example: Print request parameters
	                JSONArray jArray = null;
	                if(operation.getParameters() != null) {
	                	jArray = new JSONArray();
		                for (Parameter parameter : operation.getParameters()) {
		                	Schema paramSchema = parameter.getSchema();
		                	if(paramSchema != null ) {
		                		Schema itemsSchema = paramSchema.getItems();
		                		if(itemsSchema != null && itemsSchema.get$ref() == null) {
		                			itemId = saveItems(msc, itemsSchema, moduleId);
		                		}
		                	}
		                	JSONObject jo = new JSONObject();
		                	jo.put("key", parameter.getName());
		                	jo.put("value", "");
		                	jArray.add(jo);
		                	String valueType = parameter.getSchema().getType();
		                	jo.put("valueType", valueType);
		                	jo.put("itemId", itemId);
		                    itemId = -1;
		                }
	                }
	                
	             // Retrieve request body schema and $ref value
	                JSONObject requestBodySchema = new JSONObject();
	                io.swagger.v3.oas.models.parameters.RequestBody requestBody = operation.getRequestBody();
	                if (requestBody != null && requestBody.getContent() != null) {
	                    Map<String, io.swagger.v3.oas.models.media.MediaType> mediaTypeMap = requestBody.getContent();
	                    for (Map.Entry<String, io.swagger.v3.oas.models.media.MediaType> mediaTypeEntry : mediaTypeMap.entrySet()) {
	                        io.swagger.v3.oas.models.media.MediaType mediaType = mediaTypeEntry.getValue();
	                        Schema schema = mediaType.getSchema();
	                        if (schema != null) {
	                            String ref = schema.get$ref(); // Retrieve $ref value
	                            requestBodySchema = convertSchemaToJSON(ref, openAPI,  requestBodySchema);
	                            break;
	                        }
	                    }
	                }
	                
	                ApiResponses responses = operation.getResponses();
	                JSONObject responsesJSON = new JSONObject();
	                if (responses != null) {
	                    for (String responseCode : responses.keySet()) {
	                        ApiResponse response = responses.get(responseCode);
	                        JSONObject responseDetails = new JSONObject();
	                        responseDetails.put("description", response.getDescription());	                        
	                        responsesJSON.put(responseCode, responseDetails);
	                    }
	                }
	                
	                List<String> apiTags = operation.getTags();
	                int apiTag = -1;
	                if(apiTags != null && apiTags.size() > 0) {
	                	String apiTagStr = apiTags.get(0);
	                	apiTag = (int)tags.get(apiTagStr);
	                }
	                
	                msc.addApi(operation.getSummary(), 1, 0, moduleId, "", method.toString(), 
	                		requestBodySchema.toJSONString(), fullURL, jArray, randomKey, "{}",
	                		version, responsesJSON,apiTag,null);
	                
	            }
        	}
		} else {
        	System.err.println("Failed to parse Swagger file.");
    	}
	}
	
	public int saveItems(MySQlConn msc, Schema itemsSchema, int modid) {
		
		int apiDefId = -1;
		if (itemsSchema != null) {
             // Process items
             String enumStr = "";              
             if(itemsSchema.getEnum() != null) {
           	  enumStr = itemsSchema.getEnum().toString();
             }
             apiDefId = msc.addApiDef("", modid);
              
             msc.addApiDefParams(apiDefId, "", "", itemsSchema.getType(), enumStr, 
            		  (String)itemsSchema.getDefault());
        }
		return apiDefId; 
	}
	
	public JSONObject saveComponents(Components components) {
		
		if (components != null && components.getSchemas() != null) {
            Map<String, Schema> definitions = components.getSchemas();

            // Iterate through all definitions
            for (Map.Entry<String, Schema> entry : definitions.entrySet()) {
                String definitionName = entry.getKey();
                Schema schema = entry.getValue();

                // Process each schema (definition)
                System.out.println("Definition Name: " + definitionName);
                System.out.println("Schema Type: " + schema.getType());
                // You can access more properties of the schema as needed
            }
        } else {
            System.out.println("No components or schemas found in the Swagger file.");
        }
		
		return null;
		
	}
		
		public JSONObject convertSchemaToJSON(String ref, OpenAPI openAPI, JSONObject result) {
			if (ref != null) {
                String[] segments = ref.split("/");
                String schemaKey = segments[segments.length - 1];
                Schema refSchema = openAPI.getComponents().getSchemas().get(schemaKey);
               
                if (refSchema != null && refSchema.getProperties() != null) {
                	Map props = refSchema.getProperties();
                	Iterator iter = props.keySet().iterator();
                	while(iter.hasNext()) {
                		String key = (String)iter.next();
                		Schema propSchema = (Schema)props.get(key);
                		String propValue = propSchema.getType();
                		
                		if(propValue == null ) {
                			propValue = propSchema.get$ref();
                			JSONObject res = new JSONObject();
                			res = convertSchemaToJSON(propValue, openAPI, res);
                			propValue = res.toJSONString();
                		} else if (propValue.equalsIgnoreCase("array")) {
                				Schema itemSchema = propSchema.getItems();
                				propValue = itemSchema.getType();
                				if(propValue == null) {
	                				propValue = itemSchema.get$ref();
	                    			JSONObject res = new JSONObject();
	                    			res = convertSchemaToJSON(propValue, openAPI, res);
	                    			propValue = res.toJSONString();
                				}
                				propValue = "[" + propValue + "]"; 
                		}
                		
                		result.put(key, propValue);
                	}
                }
//                break; // Assuming only one media type is considered here (e.g., application/json)
            }
			
			return result;
		}
		
		public JSONObject processTags(List<Tag> tagsList, MySQlConn msc, int moduleid) {
			
			JSONObject result = new JSONObject();
			
			if(tagsList == null || tagsList.size() == 0) {
				return result;
			}
			
			for(int i=0; i<tagsList.size(); i++) {
				Tag tag = tagsList.get(i);
				String name = tag.getName();
				String desc = tag.getDescription();
				int key = msc.addDefinition(name, desc, moduleid);
				result.put(name, key);
			}
			return result;
		}

}
