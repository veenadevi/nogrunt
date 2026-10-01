package nogrunt;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class Unzip {
//	MySQlConn msc = new MySQlConn(null);
	Utilities uti = new Utilities();
    public void unzip(Map<String, List<JSONObject>> hashMap, int cid, String fName, 
    		String uname, MySQlConn msc) {
    	
        String zipFilePath = uti.getZipFilePath(cid, fName);
        try (ZipInputStream zipInputStream = new ZipInputStream(new FileInputStream(zipFilePath))) {
            ZipEntry entry;
            JSONObject latestUpdate = new JSONObject();
			JSONArray json = msc.getUploadfileByCompany(cid);
			latestUpdate.put("detail", json);
			latestUpdate.put("refresh", "running");
			Utilities.cacheZipUpload(uname, latestUpdate);
            
            while ((entry = zipInputStream.getNextEntry()) != null) {
                String entryName = entry.getName();
                if (hashMap.containsKey(entryName)) {                	
                    List<JSONObject> jsonList = hashMap.get(entryName);
                    if (jsonList != null && !jsonList.isEmpty()) {
                    	int fsttcid=-1;
                    	boolean isFirstObject = true;
                        for (JSONObject jsonObject : jsonList) {
		    				int tcid = (int) jsonObject.get("idTestCase");
		                    if(isFirstObject) {
		                    	fsttcid = tcid;
			                    String extractDirectory = uti.getTestCasePath(cid, tcid);
				                backupFile(Paths.get(extractDirectory + "\\" + entryName));
			                    File destinationFile = new File(extractDirectory, entryName);
			                    if (!destinationFile.getParentFile().exists()) {
			                        destinationFile.getParentFile().mkdirs();
			                    }
			                    try (FileOutputStream fos = new FileOutputStream(destinationFile)) {
			                        byte[] buffer = new byte[1024];
			                        int bytesRead;
			
			                        while ((bytesRead = zipInputStream.read(buffer)) != -1) {
			                            fos.write(buffer, 0, bytesRead);
			                        }
			                        
				            		latestUpdate = uti.getZipUploadFromCache(uname, cid, msc);
				            		cachingFileData(tcid, entryName, latestUpdate, "true", uname);
			                    } catch (IOException e) {
		                            e.printStackTrace();
		                            System.err.println("Error writing file: " + entryName);
		                            
				            		latestUpdate = uti.getZipUploadFromCache(uname, cid, msc);
				            		cachingFileData(tcid, entryName, latestUpdate, "false", uname);
				            	}
			                    isFirstObject = false;
		                    } else {
		                    	String extractDirectory = uti.getTestCasePath(cid, tcid);
		                    	backupFile(Paths.get(extractDirectory + "\\" + entryName));
		                    	String getDirectory = uti.getTestCasePath(cid, fsttcid);
		                    	
		                        Path sourcePath = Paths.get(getDirectory, entryName);
		                        Path destinationPath = Paths.get(extractDirectory, entryName);
		                        
		                        try {
		                            Files.copy(sourcePath, destinationPath, StandardCopyOption.REPLACE_EXISTING);		                            
		                            System.out.println("File copied successfully.");
		                            
				            		latestUpdate = uti.getZipUploadFromCache(uname, cid, msc);
				            		cachingFileData(tcid, entryName, latestUpdate, "true", uname);
		                        } catch (IOException e) {
		                            e.printStackTrace();
		                            System.err.println("Error copying file.");
		                            
				            		latestUpdate = uti.getZipUploadFromCache(uname, cid, msc);
				            		cachingFileData(tcid, entryName, latestUpdate, "false", uname);
		                        }
		                    }
		                }
	                   zipInputStream.closeEntry(); 
                    } else {
                    	System.err.println("No data associated with file: " +entryName);
                    }
                } else {
                	System.err.println("File " +entryName+ " is not upload as no file with this name found in database");
                }
            }
            latestUpdate.put("refresh", "Completed");
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
    
    public void backupFile(Path sourcePath) {
        String fileNameWithExtension = sourcePath.getFileName().toString();
        String timestamp = new SimpleDateFormat("yyyyMMddHHmmss").format(new java.util.Date());
        int lastDotIndex = fileNameWithExtension.lastIndexOf('.');
        String fileNameWithoutExtension = lastDotIndex > 0 ? fileNameWithExtension.substring(0, lastDotIndex) : fileNameWithExtension;
        String newFileName = fileNameWithoutExtension + "_" +timestamp + (lastDotIndex > 0 ? fileNameWithExtension.substring(lastDotIndex) : "");
        Path targetPath = sourcePath.resolveSibling(newFileName);
        try {
            Files.move(sourcePath, targetPath);
            System.out.println("File renamed successfully.");
        } catch (IOException e) {
            System.err.println("File renaming failed: " + e.getMessage());
        }
    }
    
    public void cachingFileData(int tcid, String entryName, JSONObject latestUpdate, String status, String uname) {
    	JSONArray array = (JSONArray) latestUpdate.get("detail");
        for (Object json : array) {
            JSONObject jsonObj = (JSONObject) json;
            int idTestCase = (int)jsonObj.get("idTestCase"); 
            String fname = (String)jsonObj.get("FileName"); 
            if (idTestCase == tcid && fname.equals(entryName) ) {
            	jsonObj.put("status", status);
            }
        }
		Utilities.cacheZipUpload(uname,  latestUpdate);
    }
    
    public void cachingFileData( String entryName, JSONObject latestUpdate, String status, String uname) {
    	JSONArray array = (JSONArray) latestUpdate.get("detail");
        for (Object json : array) {
            JSONObject jsonObj = (JSONObject) json;
            String fname = (String)jsonObj.get("FileName"); 
            if (fname.equals(entryName) ) {
            	jsonObj.put("status", status);
            }
        }
		Utilities.cacheZipUpload(uname,  latestUpdate);
    }
}
