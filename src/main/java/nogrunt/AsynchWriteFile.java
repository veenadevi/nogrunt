package nogrunt;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AsynchWriteFile {
	
	public static void writeToFileAsync(String fileName, String pageSource) {
        CompletableFuture.runAsync(() -> {
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(fileName))) {
                writer.write(pageSource);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

}
