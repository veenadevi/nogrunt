package nogrunt;

import java.util.Properties;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.*;
import javax.mail.search.*;
import javax.mail.internet.MimeMultipart;
import javax.mail.internet.MimeBodyPart;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.mail.internet.MimeMessage;

import org.json.simple.JSONObject;

import javax.mail.internet.InternetAddress;
import java.util.*;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

public class MailAccess {
//	MySQlConn msc = new MySQlConn();
	
	public String getFromEmail(int tsId, String username, String password, String select, 
			String filter, ExecutionLogger el, MySQlConn msc) {
		int prodId = msc.getProdIdFromTestStepId(tsId);
		JSONObject json =  msc.getEmailConnectionDetails(prodId);
		String protocol = (String) json.get("protocol");
		int port = (int)json.get("port");
		String host = (String) json.get("host");
		
		// create properties field
        Properties properties = new Properties();
        properties.put("mail.store.protocol", protocol);
        properties.put("mail.imap.host", host);
        properties.put("mail.imap.port", port);
        properties.put("mail.imap.starttls.enable", "true");
        properties.put("mail.imap.ssl.trust", "*");
        properties.put("mail.imap.ssl.enable", "true");
        
        String result = null;
        
        try {
        	
            // create session object
            Session session = Session.getDefaultInstance(properties);
            
            // create the IMAP store object and connect with the server
            el.logExecution("using host, port, protocol, username and pwd for email access: " + 
            		host + "--" + port + "--" + protocol + "--" + username + "--" + password);
            Store store = session.getStore(protocol);
             store.connect(host, port, username, password);

            // create the folder object and open it
            Folder folder = store.getFolder("inbox");
            folder.open(Folder.READ_ONLY);
            
           
            String[] selectors = select.split(AppProperties.testdatadelimiter);
            List<SearchTerm> searchTerms = new ArrayList<>();
            String timeHorizon = null;
            
            for(int i=0; i < selectors.length; i++) {
            	String[] keyValue = selectors[i].split(":");
            	
            	if(keyValue[0].equalsIgnoreCase("from")) {
            		searchTerms.add(new FromTerm(new InternetAddress(keyValue[1])));
            		el.logExecution("Select criteria from address: " + keyValue[1]);
            	} else if(keyValue[0].equalsIgnoreCase("subject")) {
            		searchTerms.add(new SubjectTerm(keyValue[1]));
            		el.logExecution("Select criteria subject: " + keyValue[1]);
            	} else if(keyValue[0].equalsIgnoreCase("time") ) {
            		if(!keyValue[1].equalsIgnoreCase("latest")) {
            			// Calculate the date-time 5 minutes before the current time
            	        Calendar calendar = Calendar.getInstance();
            	        calendar.add(Calendar.MINUTE, -5);
            	        Date afterDate = calendar.getTime();
            			searchTerms.add(new ReceivedDateTerm(ComparisonTerm.GT, afterDate));
            			el.logExecution("Select criteria time: " + keyValue[1]);
            		} else {
            			timeHorizon = keyValue[1];
            			Calendar calendar = Calendar.getInstance();
            	        calendar.add(Calendar.MINUTE, -5);
            	        Date afterDate = calendar.getTime();
            			searchTerms.add(new ReceivedDateTerm(ComparisonTerm.EQ, afterDate));
            			el.logExecution("Select criteria time: " + ComparisonTerm.EQ + " " + afterDate);
            		}
            	}
            }
            
            SearchTerm finalSearchTerm = new AndTerm(searchTerms.toArray(new SearchTerm[0]));
            
            //SearchTerm unseen = new FlagTerm(new Flags(Flags.Flag.SEEN), false);
            
            Message[] messages = folder.search(finalSearchTerm);
            if(timeHorizon != null) {
            	if(timeHorizon.equalsIgnoreCase("latest")) {
            		Address[] senders = messages[messages.length - 1].getFrom();
            		for(int i=0;i<messages.length;i++) {
            			el.logExecution("Message number " + i + " " + messages[i].getReceivedDate().toString());
            		}
//            		Date dater = messages[messages.length - 1].getReceivedDate();
//            		String senderEmail = ((InternetAddress) senders[0]).getAddress();
            		// Get the HTML content of the email message
                    String htmlContent = getTextFromMessage(messages[messages.length - 1]);

                    // Parse the HTML content using Jsoup to extract text
                    String textContent = extractTextFromHtml(htmlContent);
                    el.logExecution("Found Text Content: " + textContent);
            		
            		// Search for a 6-digit number within the text content
//                    String regex = "\\\\" + filter;
//                    String regex = "\\d{6}";
                    String regex = filter;
                    Pattern pattern = Pattern.compile(regex);
                    Matcher matcher = pattern.matcher(textContent);
                    
                    if (matcher.find()) {
                        result = matcher.group();
                        el.logExecution("Found result: " + result);
                    }
            	}
            } else {
	            for (int i = 0; i < messages.length; i++) {
	                Address[] senders = messages[i].getFrom();
	                if (senders != null && senders.length > 0) {
	                    String senderEmail = ((InternetAddress) senders[0]).getAddress();
	                    System.out.println("Sender email: " + senderEmail);
	                }
	            }
            }

            // close the folder and store objects
            folder.close(false);
            store.close();
    	} catch(Exception e) {
    		e.printStackTrace();
    	}
        
        return result;
    }
	
	private static String getTextFromMessage(Message message) throws Exception {
        Object content = message.getContent();
        if (content instanceof String) {
            return (String) content;
        } else if (content instanceof MimeMultipart) {
            MimeMultipart multipart = (MimeMultipart) content;
            StringBuilder textContent = new StringBuilder();
            for (int i = 0; i < multipart.getCount(); i++) {
                MimeBodyPart bodyPart = (MimeBodyPart) multipart.getBodyPart(i);
                if (bodyPart.isMimeType("text/plain")) {
                    textContent.append(bodyPart.getContent());
                } else if (bodyPart.isMimeType("text/html")) {
                	textContent.append(bodyPart.getContent());
                }
            }
            return textContent.toString();
        } else {
            return ""; // No text content found
        }
    }
	
	private static String extractTextFromHtml(String htmlContent) {
        Document doc = Jsoup.parse(htmlContent);
        return doc.text();
    }

	
	public void sendEmail(int tsId, String fromAddress, String password, String toAddress,
			String subject, String content, ExecutionLogger el, MySQlConn msc) {
		
		int prodId = msc.getProdIdFromTestStepId(tsId);
		JSONObject json =  msc.getSendEmailConnectionDetails(prodId);
		int port = (int)json.get("port");
		String host = (String) json.get("host");

        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", host);
        props.put("mail.smtp.port", port);
        
        el.logExecution("Email being sent to follwoing details -  host-port-from-pwd-to-subject");
        el.logExecution("Email being sent to values are " + host +"--" + port 
        		+ "--" + fromAddress + "--" + password + "--" + toAddress + "--" + subject);

        Session session = Session.getInstance(props,
            new javax.mail.Authenticator() {
                protected javax.mail.PasswordAuthentication getPasswordAuthentication() {
                    return new javax.mail.PasswordAuthentication(fromAddress, password);
                }
            });

        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(fromAddress));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toAddress));
            message.setSubject(subject);
            message.setContent(content, "text/html");

            Transport.send(message);

            el.logExecution("Email sent successfully.");
        } catch (MessagingException e) {
        	el.logExecution(e);
            e.printStackTrace();
        }
	}
	
    public static void main(String[] args) throws Exception {
    	
    	
    	final String username = "prahalad@nogrunt.com";
        final String password = "!2Prahalad1";

        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", "smtp.zoho.in");
        props.put("mail.smtp.port", "587");

        Session session = Session.getInstance(props,
            new javax.mail.Authenticator() {
                protected javax.mail.PasswordAuthentication getPasswordAuthentication() {
                    return new javax.mail.PasswordAuthentication(username, password);
                }
            });

        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(username));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse("deva@nogrunt.com"));
            message.setSubject("Subject Line");
            message.setText("Hello,\n\nThis is a test email from Java.");

            Transport.send(message);

            System.out.println("Email sent successfully.");
        } catch (MessagingException e) {
            e.printStackTrace();
        }
    }
}
