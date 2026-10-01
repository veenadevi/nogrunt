package nogrunt;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.EC2ContainerCredentialsProviderWrapper;
import com.amazonaws.auth.InstanceProfileCredentialsProvider;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.simpleemail.AmazonSimpleEmailService;
import com.amazonaws.services.simpleemail.AmazonSimpleEmailServiceClientBuilder;
import com.amazonaws.services.simpleemail.model.*;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.s3.model.AmazonS3Exception;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;
import java.util.Base64;

import org.json.simple.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.Scanner;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class EmailService {

	public static void main(String[] args) {
		AppProperties.getProperties();
		EmailService es = new EmailService();
		//es.sendMail("Deva", "boothana@yahoo.com", "Welcome to Nogrunt - the no code automation platform");
		JSONObject emailData = new JSONObject();
		emailData.put("name", "Deva");
		emailData.put("recipient", "boothana@yahoo.com");
		emailData.put("subject", AppProperties.welcomesubject);
		es.sendMail(emailData);
		
	}
    	
	
	public void sendMail(JSONObject emailData) {
		
		String name = (String)emailData.get("name");
		String recipient = (String)emailData.get("recipient");
		String subject = (String)emailData.get("subject");
		String template = (String)emailData.get("template");	
    	

        // Set up the AWS credentials and configuration based on the environment
        String environment = "development"; // or "production"
        String accessKey = "";
        String secretKey = "";
        String region = Regions.AP_SOUTH_1.getName(); // or the appropriate region for your use case
//        String region = AppProperties.awsregion;
        AmazonSimpleEmailService sesClient;
        
        String bodyText = "";
        Destination destination = null;

        if (AppProperties.environment.equals("development")) {
            // Use AWS Basic Credentials for development
            accessKey = AppProperties.awssesaccesskey;
            secretKey = AppProperties.awssessecretaccesskey;
            BasicAWSCredentials basicCreds = new BasicAWSCredentials(accessKey, secretKey);
            sesClient = AmazonSimpleEmailServiceClientBuilder.standard()
                    .withCredentials(new AWSStaticCredentialsProvider(basicCreds))
                    .withRegion(region).build();
            bodyText = processTemplate(template);
        } else {
            // Use EC2 Instance Role for production
            InstanceProfileCredentialsProvider instanceCreds = new InstanceProfileCredentialsProvider(false);
            sesClient = AmazonSimpleEmailServiceClientBuilder.standard()
                    .withCredentials(instanceCreds)
                    .withRegion(region).build();
            bodyText = processTemplateS3Client(template);
        }
        
        if(template.equals("welcome.html")) {
        	bodyText = bodyText.toString().replace("$name", name);
        	destination = new Destination().withToAddresses(recipient);
        } else if(template.equals("TestCaseExecution.html")) {
        	bodyText = bodyText.toString().replace("$TC", (String)emailData.get("testcase"));
        	bodyText = bodyText.toString().replace("$status", (String)emailData.get("status"));
        	double dur = (double)emailData.get("duration");
        	String durStr = String.valueOf(dur);
        	bodyText = bodyText.toString().replace("$dur", durStr);
        	bodyText = bodyText.toString().replace("$executeddate", (String)emailData.get("excuteddate"));
        	bodyText = bodyText.toString().replace("$browser", (String)emailData.get("browser"));
        	bodyText = bodyText.toString().replace("$envurl", (String)emailData.get("envurl"));
        	String width = String.valueOf((int)emailData.get("width"));
        	String height = String.valueOf((int)emailData.get("height"));
        	bodyText = bodyText.toString().replace("$resolution", width + " by " +
        			height);
        	bodyText = bodyText.toString().replace("$results", (String)emailData.get("resultsURL"));
        	bodyText = bodyText.toString().replace("$log", (String)emailData.get("logfile"));
        	
        	subject = subject.toString().replace("&testcasename", (String)emailData.get("testcase"));
        	subject = subject.toString().replace("&status", (String)emailData.get("status"));
        	destination = new Destination().withToAddresses(recipient);
        } else if(template.equals("ForgotPwd.html")) {
        	int valkey = (int)emailData.get("valkey");
        	bodyText = bodyText.toString().replace("$otp", String.valueOf(valkey));
        	destination = new Destination().withToAddresses(recipient);
        } else if(template.equals("TestSuiteResult.html")) {
        	bodyText = bodyText.toString().replace("$TC", (String)emailData.get("testSuiteName"));
        	bodyText = bodyText.toString().replace("$status", (String)emailData.get("status"));
        	double dur = (double)emailData.get("duration");
        	String durStr = String.valueOf(dur);
        	bodyText = bodyText.toString().replace("$dur", durStr);
        	bodyText = bodyText.toString().replace("$executeddate", (String)emailData.get("excuteddate"));
        	bodyText = bodyText.toString().replace("$passcount", (String)emailData.get("pass/fail"));
        	String envurl = "";
        	if((String)emailData.get("envurl") != null) {
        		envurl = (String)emailData.get("envurl");
        	}
        	bodyText = bodyText.toString().replace("$envurl", envurl);
        	bodyText = bodyText.toString().replace("$results", (String)emailData.get("resultsurl"));
        	
        	subject = subject.toString().replace("&testsuite", (String)emailData.get("testSuiteName"));
        	subject = subject.toString().replace("&status", (String)emailData.get("status"));
        	String[] recipients = (String[])emailData.get("recipients");
        	if(recipients != null) {
        		recipients[recipients.length - 1] = recipient;
        		destination = new Destination().withToAddresses(recipients);
        	}
        	
        }

        // Set up the message
        String sender = AppProperties.fromaddress;

        Content subjectContent = new Content(subject);
        Content bodyContent = new Content(bodyText);
        Body body = new Body().withHtml(bodyContent);

        Message message = new Message().withSubject(subjectContent).withBody(body);
//        Destination destination = new Destination().withToAddresses(recipients);
        SendEmailRequest request = new SendEmailRequest().withSource(sender)
                .withDestination(destination).withMessage(message);

        // Send the email
        SendEmailResult result = sesClient.sendEmail(request);
        System.out.println("Email sent with message ID: " + result.getMessageId());
    }
	
	public String processTemplate(String template) {
		String replacedContent = "";
		try {
	        
	        StringBuilder templateContent = new StringBuilder();
	        
	        try (InputStream inputStream = EmailService.class.getClassLoader().getResourceAsStream(template);
	                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {

	               String line;
	               while ((line = reader.readLine()) != null) {
	            	   templateContent.append(line).append(System.lineSeparator());
	               }
	        } catch (IOException e) {
	            System.err.println("Error loading template file: " + e.getMessage());
	            return null;
	        }
	        
	        // Replace the variable with the desired value
	        //replacedContent = templateContent.toString().replace("$name", name);
	        replacedContent = templateContent.toString();

		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return replacedContent;
    }
    	
//    	public String processTemplate(String template) {
//    		String replacedContent = "";
//    		try {
//    	        
//    	        // Load the template file
//    	        String templatePath = "s3:\\\\" + AppProperties.s3bucket + "/" +template;
//    	        File templateFile = new File(templatePath);
//    	        StringBuilder templateContent = new StringBuilder();
//    	        
//    	        try (Scanner scanner = new Scanner(templateFile)) {
//    	            while (scanner.hasNextLine()) {
//    	                String line = scanner.nextLine();
//    	                templateContent.append(line).append("\n");
//    	            }
//    	        } catch (IOException e) {
//    	            System.err.println("Error loading template file: " + e.getMessage());
//    	            return null;
//    	        }
//    	        
//    	        // Replace the variable with the desired value
//    	        //replacedContent = templateContent.toString().replace("$name", name);
//    	        replacedContent = templateContent.toString();
//
//    		} catch (Exception e) {
//    			e.printStackTrace();
//    		}
//    		
//    		return replacedContent;
//        }
    	
    	public String processTemplateS3Client(String template) {
    		AmazonS3 s3Client = AmazonS3ClientBuilder.standard()
                    .withRegion(Regions.US_EAST_1)
                    .build();
    		String bucketName = AppProperties.s3bucket;
    		String objectKey = template;
    		String replacedContent = "";
    		
    		try {
    		    S3Object s3Object = s3Client.getObject(bucketName, objectKey);
    		    InputStream objectData = s3Object.getObjectContent();

    		    // Read the object data into a string
    		    StringBuilder templateContent = new StringBuilder();

    		    	Scanner scanner = new Scanner(objectData);
    		    
    		        while (scanner.hasNextLine()) {
    		            String line = scanner.nextLine();
    		            templateContent.append(line).append("\n");
    		        }
    		    
    		    // Replace the variable with the desired value
    		    //replacedContent = templateContent.toString().replace("$name", name);
    		        replacedContent = templateContent.toString();

    		} catch (AmazonS3Exception e) {
    		    System.err.println("Error loading S3 object: " + e.getMessage());
    		    return null;
    		}
    		return replacedContent;
    	}
}
