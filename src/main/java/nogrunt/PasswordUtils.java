package nogrunt;

import org.bouncycastle.crypto.generators.SCrypt;
import org.bouncycastle.util.encoders.Hex;
import org.json.simple.JSONObject;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;
import javax.crypto.KeyGenerator;

public class PasswordUtils {
	
	    private static final int N = 16384; 
	    private static final int r = 8;     
	    private static final int p = 1;  
	    
	    private static final String SECRET_KEY = "e9c5cc14272b9dc33714e8e58126da1d"; // Replace with your secret key
	    private static final String ALGORITHM = "AES";
	    private static final String TRANSFORMATION = "AES/ECB/PKCS5Padding";

	    public static String hashPassword(String password, String salt) throws NoSuchAlgorithmException {

	        // Combine the password and salt
	        String passwordWithSalt = password + salt;
	        
	        // Compute the salted hash of the password using SHA-512
	        MessageDigest digest = MessageDigest.getInstance("SHA-512");
	        byte[] hashBytes = digest.digest(passwordWithSalt.getBytes());
	        
	        // Perform key stretching using scrypt
	        byte[] stretchedBytes = SCrypt.generate(hashBytes, salt.getBytes(), N, r, p, 64);
	        
	        // Convert the stretched hash to a hexadecimal string
	        String stretchedHash = Hex.toHexString(stretchedBytes);
	        
	        // Return the salted and stretched hash
	        return stretchedHash;
	    }
	    
	    public static String usage(String password, String salt) throws NoSuchAlgorithmException {

	        // Combine the password and salt
	        String passwordWithSalt = password + salt;
	        
	        // Compute the salted hash of the password using SHA-512
	        MessageDigest digest = MessageDigest.getInstance("SHA-512");
	        byte[] hashBytes = digest.digest(passwordWithSalt.getBytes());
	        
	        // Perform key stretching using scrypt
	        byte[] stretchedBytes = SCrypt.generate(hashBytes, salt.getBytes(), N, r, p, 64);
	        
	        // Convert the stretched hash to a hexadecimal string
	        String stretchedHash = Hex.toHexString(stretchedBytes);
	        
	        // Return the salted and stretched hash
	        return stretchedHash;
	    }
	    
	    public static String encryptLicenseKey(String licenseKey) throws Exception {
	        SecretKey secretKey = new SecretKeySpec(SECRET_KEY.getBytes(), ALGORITHM);
	        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
	        cipher.init(Cipher.ENCRYPT_MODE, secretKey);

	        byte[] encryptedBytes = cipher.doFinal(licenseKey.getBytes());
	        return Base64.getEncoder().encodeToString(encryptedBytes);
	    }

	    public static String decryptLicenseKey(String encryptedLicenseKey) throws Exception {
	        SecretKey secretKey = new SecretKeySpec(SECRET_KEY.getBytes(), ALGORITHM);
	        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
	        cipher.init(Cipher.DECRYPT_MODE, secretKey);

	        byte[] encryptedBytes = Base64.getDecoder().decode(encryptedLicenseKey);
	        byte[] decryptedBytes = cipher.doFinal(encryptedBytes);
	        return new String(decryptedBytes);
	    }

//	    public static void main(String[] args) {
//	        try {
//	            String originalLicenseKey = "31/12/2024-2000";
//	            
//	            // Encrypt the license key
//	            String encryptedLicenseKey = encryptLicenseKey(originalLicenseKey);
//	            System.out.println("Encrypted License Key: " + encryptedLicenseKey);
//	            
//	            // Decrypt the license key
//	            String decryptedLicenseKey = decryptLicenseKey(encryptedLicenseKey);
//	            System.out.println("Decrypted License Key: " + decryptedLicenseKey);
//	        } catch (Exception e) {
//	            e.printStackTrace();
//	        }
//	    }
	    
	    public static SecretKey generateAESKey(int keyLength) throws NoSuchAlgorithmException {
	        KeyGenerator keyGen = KeyGenerator.getInstance("AES");
	        keyGen.init(keyLength); // Valid key lengths: 128, 192, or 256
	        return keyGen.generateKey();
	    }

	    public static void main(String[] args) {
	        try {
	            // Generate a 128-bit AES key
	            SecretKey aes128Key = generateAESKey(128);
	            byte[] keyBytes = aes128Key.getEncoded(); // Get the key bytes

	            // Convert the key bytes to a hexadecimal string
	            StringBuilder keyHex = new StringBuilder();
	            for (byte b : keyBytes) {
	                keyHex.append(String.format("%02x", b));
	            }

	            System.out.println("Generated 128-bit AES Key (Hexadecimal): " + keyHex.toString());
	        } catch (NoSuchAlgorithmException e) {
	            e.printStackTrace();
	        }
	    }
	    
	    public static JSONObject getLicenseDetails(int companyId, MySQlConn msc, MySqlConn2 msc2) {
			if(msc == null) {
				msc = new MySQlConn(null);
				if(msc2 == null) {
					msc2 = new MySqlConn2(msc);
				}
			}
			
			if(msc2 == null) {
				msc2 = new MySqlConn2(msc);
			}
			
			JSONObject compLic = msc2.getLicense(companyId);
			
			String licusage = (String)compLic.get("licusage");
			
			if(licusage == null) {
				System.err.println(Utilities.getNow());
				System.err.println("License Usage value is null");
				return null;
			}
			
			try {
				licusage = PasswordUtils.decryptLicenseKey(licusage);
			} catch (Exception e) {
				e.printStackTrace();
				return null;
			}
			
			String[] licDetails = licusage.split(AppProperties.licenselimiter);
			String[] propertyNames = {"toDate", "tcCount", "codeDownloadPrmsn", "performanceTestingPrmsn", "coveragePrmsn", 
					"apiDataPrmsn", "parallelThreadCnt", "vRecPrmsn", "NLPCreationPrmsn", "chatBotPrmsn"};
			
			JSONObject license = new JSONObject();
			int numProperties = Math.min(licDetails.length,propertyNames.length);
			for (int i = 0; i < numProperties; i++) {
			    String propertyName = propertyNames[i];
			    String propertyValue = licDetails[i];
			    
			    if (propertyName.equals("tcCount") || propertyName.equals("parallelThreadCnt")) {
			    	if(propertyName.equals("parallelThreadCnt") && (propertyValue.equals("null") ||
			    			propertyValue.equals("0"))) {
			    		license.put(propertyName, 1);
			    	} else {
				        try {
				            int intValue = Integer.parseInt(propertyValue);
				            license.put(propertyName, intValue);
				        } catch (NumberFormatException e) {
				        	e.printStackTrace();
				        }
			    	}
			    } else {
			        license.put(propertyName, propertyValue);
			    }
			}
			
			return license;
		}
}
