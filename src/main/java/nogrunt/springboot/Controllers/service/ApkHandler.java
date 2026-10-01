package nogrunt.springboot.Controllers.service;

import nogrunt.Utilities;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class ApkHandler {

    public String uploadApk(MultipartFile file, int companyid) throws IOException {
    	String folderPath = Utilities.getCompanyFolder(companyid) + "\\" + "apks"; 
        Utilities.createFolder(folderPath);

        try {
            validateApkFile(file);

            String fileName = file.getOriginalFilename();

            if (Files.exists(Path.of(folderPath))) {
                throw new IOException("File " + fileName + " already exists. Please rename the file or upload a different one.");
            }
            
            Files.copy(file.getInputStream(), Path.of(folderPath));
            return fileName;

        } catch (IOException ex) {
            throw new IOException("Failed to upload APK file: " + ex.getMessage());
        }
    }

    public List<String> getApkList(Integer companyId) throws IOException {
        String UPLOAD_DIR = Utilities.getCompanyFolder(companyId) + "/apks";

        try {
            Path uploadPath = Paths.get(UPLOAD_DIR);
            return Files.list(uploadPath)
                    .filter(path -> path.toString().endsWith(".apk"))
                    .map(path -> path.getFileName().toString())
                    .collect(Collectors.toList());

        } catch (IOException ex) {
            throw new IOException("Failed to retrieve APK list: " + ex.getMessage());
        }
    }

    private void validateApkFile(MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            throw new IOException("Please select a file to upload");
        }

        if (!file.getOriginalFilename().endsWith(".apk")) {
            throw new IOException("Only APK files are allowed");
        }
    }

}
