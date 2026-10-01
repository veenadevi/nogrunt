package nogrunt.integrations;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class AdbSmsRetriever {

//	public static String getConnectedDeviceId() {
//        try {
//            ProcessBuilder processBuilder = new ProcessBuilder("adb", "devices");
//            Process process = processBuilder.start();
//            BufferedReader reader = new BufferedReader(
//                new InputStreamReader(process.getInputStream())
//            );
//            
//            String line;
//            reader.readLine();
//            line = reader.readLine();
//            if (line != null && !line.trim().isEmpty()) {
//                return line.split("\\s+")[0];
//            }
//            
//            process.waitFor();
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//        return null;
//    }
//
//
//    public static List<String> retrieveSms() {
//        List<String> smsMessages = new ArrayList<>();
//        String deviceId = getConnectedDeviceId();
//        try {
//            ProcessBuilder processBuilder = new ProcessBuilder(
//                    "adb", "-s", deviceId, "shell",
//                    "content query --uri content://sms/inbox | head -n 1"
//                );
//
//
//            Process process = processBuilder.start();
//            BufferedReader reader = new BufferedReader(
//
//                new InputStreamReader(process.getInputStream())
//            );
//
//            String line;
//            while ((line = reader.readLine()) != null) {
//                if (line.contains("address=") && line.contains("body=")) {
//                    smsMessages.add(line);
//                }
//            }
//
//            process.waitFor();
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//        return smsMessages;
//    }
//
//    public static void main(String[] args) {
//        List<String> messages = retrieveSms();
//        messages.forEach(System.out::println);
//    }

	private static final String ADB_COMMAND = resolveAdbCommand();

	private static String resolveAdbCommand() {
		String macAdbPath = "/opt/homebrew/bin/adb";
		String usrLocalBinAdb = "/usr/local/bin/adb";
		if (new File(macAdbPath).exists()) {
			return macAdbPath;
		} else if (new File(usrLocalBinAdb).exists()) {
			return usrLocalBinAdb;
		} else {
			return "adb";
		}
	}

	public static boolean isAndroidEnvironment() {
		String os = System.getProperty("os.name").toLowerCase();
		return os.contains("mac") || os.contains("linux") || os.contains("windows");
	}

	public static boolean isAdbAvailable() {
		try {
			ProcessBuilder processBuilder = new ProcessBuilder(ADB_COMMAND, "version");
			processBuilder.redirectErrorStream(true);
			Process process = processBuilder.start();

			BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
			String output;
			while ((output = reader.readLine()) != null) {
				System.out.println("[ADB OUTPUT] " + output);
			}

			process.waitFor();
			return process.exitValue() == 0;
		} catch (Exception e) {
			System.out.println("ADB check failed: " + e.getMessage());
			return false;
		}
	}

	public static String getConnectedDeviceId() {
		if (!isAdbAvailable()) {
			System.out.println("ADB not available.");
			return null;
		}

		try {
			ProcessBuilder processBuilder = new ProcessBuilder(ADB_COMMAND, "devices");
			Process process = processBuilder.start();
			BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

			reader.readLine();
			String line = reader.readLine();
			if (line != null && !line.trim().isEmpty() && line.contains("device")) {
				return line.split("\\s+")[0];
			}

			process.waitFor();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public static List<String> retrieveSms() {
		List<String> smsMessages = new ArrayList<>();

		if (!isAndroidEnvironment() || !isAdbAvailable()) {
			System.out.println("Skipping SMS retrieval (not Android or ADB missing)");
			return smsMessages;
		}

		String deviceId = getConnectedDeviceId();
		if (deviceId == null) {
			System.out.println("No Android device connected.");
			return smsMessages;
		}

		try {
			ProcessBuilder processBuilder = new ProcessBuilder(ADB_COMMAND, "-s", deviceId, "shell",
					"content query --uri content://sms/inbox | head -n 1");

			Process process = processBuilder.start();
			BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

			String line;
			while ((line = reader.readLine()) != null) {
				if (line.contains("address=") && line.contains("body=")) {
					smsMessages.add(line);
				}
			}

			process.waitFor();
		} catch (Exception e) {
			e.printStackTrace();
		}

		return smsMessages;
	}

	public static void main(String[] args) {
		List<String> messages = retrieveSms();
		if (messages.isEmpty()) {
			System.out.println("No messages retrieved.");
		} else {
			messages.forEach(System.out::println);
		}
	}
}
