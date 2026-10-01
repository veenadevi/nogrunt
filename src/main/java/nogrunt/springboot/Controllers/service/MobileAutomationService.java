package nogrunt.springboot.Controllers.service;

import nogrunt.AppProperties;
import nogrunt.Utilities;
import nogrunt.springboot.Controllers.dtoModel.MobileAutomationDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import nogrunt.springboot.Controllers.model.MobileAutomation;
import nogrunt.springboot.Controllers.repository.MobileAutomationRepository;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Map;

@Service
public class MobileAutomationService {

    @Autowired
    private final MobileAutomationRepository repository;

    @Autowired
    private final TokenVerificationService tokenVerificationService;

    private final MobileTestService mobileTestService;


    public MobileAutomationService(MobileAutomationRepository repository, TokenVerificationService tokenVerificationService, MobileTestService mobileTestService) {
        this.repository = repository;
        this.tokenVerificationService = tokenVerificationService;
        this.mobileTestService = mobileTestService;
    }

    public MobileAutomation saveActions(MobileAutomationDTO dto) {

        MobileAutomation entity = new MobileAutomation();
        entity.setAction(dto.getAction());
        entity.setDevice("mobile");

        if (dto.getTestCaseId() != null) {
            entity.setTest_case_id(dto.getTestCaseId());
        } else {
            Integer testCaseId = mobileTestService.getIdTestCase(dto.getExtKey());
            entity.setTest_case_id(testCaseId);
        }

        Integer testStepId = mobileTestService.saveEntity(dto);
        entity.setIdtest_step(testStepId);

        entity.setElementId(dto.getElementId());
        entity.setTimestamp(LocalDateTime.now());
        entity.setAppiumTimestamp(dto.getTimestamp());
        entity.setSequenceNumber(dto.getSequenceNumber());
        entity.setStrategyMap(dto.getStrategyMap());
        entity.setAndroidUiautomator(dto.getAndroidUiautomator());
        entity.setXpath(dto.getXpath());
        entity.setIdStrategy(dto.getIdStrategy());
        MobileAutomation savedEntity = repository.save(entity);

        String token = String.valueOf(dto.getExtKey());
        String uname = tokenVerificationService.getUsernameByToken(token);
        int companyid = tokenVerificationService.getCompanyIdByToken(token);
        Utilities.callApi(AppProperties.javareactinternalurl, "updateRecordingCache", uname, null, companyid);

        return savedEntity;
    }

    public static MobileAutomationDTO mapToDTO(Map<String, Object> dataMap) {
        MobileAutomationDTO dto = new MobileAutomationDTO();
        dto.setExtKey((Integer) dataMap.getOrDefault("extKey", null));
        dto.setTestCaseId((Integer) dataMap.getOrDefault("testCaseId", null));
        dto.setAction((String) dataMap.getOrDefault("action", null));
        dto.setElementId((String) dataMap.getOrDefault("elementId", null));
        Object timestampValue = dataMap.getOrDefault("timestamp", null);
        if (timestampValue instanceof String) {
            String timestampString = (String) timestampValue;
            try {
                LocalDateTime timestamp = LocalDateTime.parse(timestampString, DateTimeFormatter.ISO_DATE_TIME);
                dto.setTimestamp(timestamp);
            } catch (DateTimeParseException e) {
                throw new IllegalArgumentException("Invalid date-time format in 'timestamp': " + timestampString, e);
            }
        } else if (timestampValue instanceof LocalDateTime) {
            dto.setTimestamp((LocalDateTime) timestampValue);
        } else if (timestampValue instanceof Timestamp) {
            dto.setTimestamp(((Timestamp) timestampValue).toLocalDateTime());
        } else {
            dto.setTimestamp(null);
        }

        dto.setSequenceNumber((Integer) dataMap.getOrDefault("sequenceNumber", null));
        dto.setStrategyMap((String) dataMap.getOrDefault("strategyMap", null));
        dto.setAndroidUiautomator((String) dataMap.getOrDefault("android_uiautomator", null));
        dto.setXpath((String) dataMap.getOrDefault("xpath", null));
        dto.setElement((String) dataMap.getOrDefault("element",null));
        dto.setTestData((String) dataMap.getOrDefault("testData",null));
        dto.setIdStrategy((String) dataMap.getOrDefault("id_strategy", null));


        dto.setIdStrategy((String) dataMap.getOrDefault("id_strategy", null));
        return dto;
    }

}



