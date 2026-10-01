package nogrunt.springboot.Controllers.service;

import java.io.File;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import net.dongliu.apk.parser.ApkFile;
import net.dongliu.apk.parser.bean.ApkMeta;
import nogrunt.springboot.Controllers.model.MobileCapabilities;
import nogrunt.springboot.Controllers.model.TestCaseCapability;
import nogrunt.springboot.Controllers.repository.MobileCapabilitiesRepo;
import nogrunt.springboot.Controllers.repository.TestCaseCapRepo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class MobileCapabilitiesService {

	@Autowired
	private MobileCapabilitiesRepo mobileCapabilitiesRepo;

	@Autowired
	private TestCaseCapRepo testCaseCapRepo;

	private static final Logger logger = LoggerFactory.getLogger(MobileCapabilitiesService.class);

	// Save Mobile Capabilities In DB
	public Integer saveMobileCapabilities(Integer companyId, MobileCapabilities mobileCapabilities) {

		mobileCapabilities.setPlatformName(mobileCapabilities.getPlatformName());
		mobileCapabilities.setPlatformVersion(mobileCapabilities.getPlatformVersion());
		mobileCapabilities.setDeviceName(mobileCapabilities.getDeviceName());
		mobileCapabilities.setApp(mobileCapabilities.getApp());
		mobileCapabilities.setAutomationName(mobileCapabilities.getAutomationName());
		mobileCapabilities.setCompanyId(companyId);
		mobileCapabilities.setName(mobileCapabilities.getName());

		String label = extractLabelNameFromAPP(mobileCapabilities.getApp());
		mobileCapabilities.setLabel(label);

		mobileCapabilities.setCreatedDate(LocalDateTime.now());

		MobileCapabilities savedEntity = mobileCapabilitiesRepo.save(mobileCapabilities);
		return savedEntity.getCapablities_Id();
	}

	// Get Label From APK FILE PATH USING APK
	public static String extractLabelNameFromAPP(String filePath) {
		try {
			if (filePath.endsWith(".apk")) {
				try (ApkFile apkFile = new ApkFile(new File(filePath))) {
					ApkMeta apkMeta = apkFile.getApkMeta();
					return apkMeta.getLabel();
				}
			} else if (filePath.endsWith(".app")) {
				File infoPlist = new File(filePath + "/Info.plist");
				if (infoPlist.exists()) {
					ProcessBuilder pb = new ProcessBuilder("defaults", "read", infoPlist.getAbsolutePath(),
							"CFBundleDisplayName");
					Process process = pb.start();
					try (java.util.Scanner s = new java.util.Scanner(process.getInputStream()).useDelimiter("\\A")) {
						return s.hasNext() ? s.next().trim() : "Unknown iOS App";
					}
				} else {
					System.err.println("Info.plist not found in iOS .app bundle.");
					return "Unknown iOS App";
				}
			} else {
				System.err.println("Unsupported file type: " + filePath);
				return "Unknown Format";
			}
		} catch (Exception e) {
			System.err.println("Failed to extract label name: " + e.getMessage());
			return null;
		}
	}

	// save testcaseId and capabilitiesId in db
	public Integer savetestCaseCapMapId(Integer testcaseId, Integer capabilities_id) {
		TestCaseCapability testCaseCapability = new TestCaseCapability();

		testCaseCapability.setTestCaseId(testcaseId);
		testCaseCapability.setCapabilitiesId(capabilities_id);

		TestCaseCapability savedEntity = testCaseCapRepo.save(testCaseCapability);
		return savedEntity.getMappingId();

	}

	// Fetching Capabilities From DB Based On capabilities_Id
	public Map<String, Object> getDesiredCapabilities(Integer capabilities_Id) {
		logger.debug("Looking up capabilities in database for capabilities_id: {}", capabilities_Id);
		MobileCapabilities capability = mobileCapabilitiesRepo.findByCapabilitiesId(capabilities_Id);

		if (capability == null) {
			logger.warn("No capabilities found for capabilities_id: {}", capabilities_Id);
			throw new RuntimeException("Capabilities not found for capabilities_id: " + capabilities_Id);
		}

		logger.debug(
				"Found capabilities: platformName={}, platformVersion={}, deviceName={}, automationName={}, app={}",
				capability.getPlatformName(), capability.getPlatformVersion(), capability.getDeviceName(),
				capability.getAutomationName(), capability.getApp());

		Map<String, Object> capabilityMap = new LinkedHashMap<>();
		capabilityMap.put("platformName", capability.getPlatformName());
		capabilityMap.put("platformVersion", capability.getPlatformVersion());
		capabilityMap.put("deviceName", capability.getDeviceName());
		capabilityMap.put("automationName", capability.getAutomationName());
		capabilityMap.put("app", capability.getApp());

		Map<String, Object> result = new LinkedHashMap<>();
		result.put("name", capability.getName());
		result.put("CapId", capability.getCapablities_Id());
		result.put("capability", capabilityMap);

		return result;
	}

	// Multiple Capabilities By Company ID
	public List<Map<String, Object>> getAllCapabilitiesByCompanyId(Integer companyId) {
		List<MobileCapabilities> capabilityRecords = mobileCapabilitiesRepo.findByCompanyId(companyId);

		if (capabilityRecords.isEmpty()) {
			throw new RuntimeException("No capabilities found for companyId: " + companyId);
		}

		List<Map<String, Object>> allCapabilities = new ArrayList<>();

		for (MobileCapabilities capability : capabilityRecords) {
			Map<String, Object> capabilityMap = new LinkedHashMap<>();
			capabilityMap.put("platformName", capability.getPlatformName());
			capabilityMap.put("platformVersion", capability.getPlatformVersion());
			capabilityMap.put("deviceName", capability.getDeviceName());
			capabilityMap.put("automationName", capability.getAutomationName());
			capabilityMap.put("app", capability.getApp());

			Map<String, Object> result = new LinkedHashMap<>();
			result.put("name", capability.getName());
			result.put("CapId", capability.getCapablities_Id());
			result.put("capability", capabilityMap);

			allCapabilities.add(result);
		}

		return allCapabilities;
	}

	public MobileCapabilities getCapabilitiesByTestCaseID(Integer Test_Case_Id) {
		Integer capId = findCapIdByTestCaseId(Test_Case_Id);
		MobileCapabilities cap = mobileCapabilitiesRepo.findByCapabilitiesId(capId);
		if (cap == null) {
			throw new RuntimeException("Capabilities not found for capId:" + capId);
		}
		return cap;
	}

	public Integer findCapIdByTestCaseId(Integer testCaseId) {
		TestCaseCapability cap = testCaseCapRepo.findByTestCaseId(testCaseId);
		if (cap == null) {
			throw new RuntimeException("Capabilities not found for testCaseId: " + testCaseId);
		}
		return cap.getCapabilitiesId();
	}

}
