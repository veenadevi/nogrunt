package nogrunt.springboot.Controllers.service;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import nogrunt.springboot.Controllers.model.*;
import nogrunt.springboot.Controllers.repository.MobileAutomationRepository;
import nogrunt.springboot.Controllers.repository.PageElementRepository;
import nogrunt.springboot.Controllers.repository.TestCaseRepository;
import nogrunt.springboot.Controllers.repository.TestStepAttrRepository;
import nogrunt.springboot.Controllers.repository.TestStepRepository;
import nogrunt.springboot.Controllers.testCaseDtos.LlmTestCaseDto;

@Service
public class TestCaseService {
	
	@Autowired
    private TestCaseRepository testCaseRepository;
	@Autowired
    private TestStepRepository testStepRepository;
	@Autowired
    private TestStepAttrRepository testStepAttrRepository;
	@Autowired
    private PageElementRepository pageElementRepository;
	
	@Autowired
    private MobileAutomationRepository mobileAutomationRepository;
	
	 Boolean isMobile = false;
	 Boolean isManual = false;
	 
	 // set the above two boolean values and moduleId and oldTestCaseId
	 
	 private final RestTemplate restTemplate = new RestTemplate();

	    public String createNLPTestCase(LlmTestCaseDto dto,  String token) {
	        
	        String externalId = createTestCaseViaKeyLoggingAPI(
	                "59",
	                token,
	                "710",
	                "697",
	                dto.getTestCaseTitle(),
	                dto.getBackendExecutionSteps()
	        );

	        return externalId;
	    }

	    private String createTestCaseViaKeyLoggingAPI(
	            String companyId,
	            String token,
	            String productId,
	            String moduleId,
	            String newTestCaseName,
	            List<String> backendSteps
	    ) {
	        try {
	            String joinedSteps = String.join("\n", backendSteps);

	            URI uri = UriComponentsBuilder.newInstance()
	                    .scheme("http")
	                    .host("localhost")
	                    .port(8080)
	                    .path("/KeyLogging/ReactApp")
	                    .queryParam("action", "createTestCase")
	                    .queryParam("companyid", companyId)
	                    .queryParam("token", token)
	                    .queryParam("product", productId)
	                    .queryParam("module", moduleId)
	                    .queryParam("newTestCase", newTestCaseName)
	                    .build()
	                    .encode()
	                    .toUri();
	            
	            HttpHeaders headers = new HttpHeaders();
	            headers.setContentType(MediaType.TEXT_PLAIN);

	            HttpEntity<String> requestEntity = new HttpEntity<>(joinedSteps, headers);

	            ResponseEntity<String> response = restTemplate.postForEntity(uri, requestEntity, String.class);
	            return response.getBody();  // assuming this returns the ID

	        } catch (Exception e) {
	            e.printStackTrace();
	            return "error";
	        }
	    }
    
	@Transactional
    public TestCase createTestCaseAndAddSteps(LlmTestCaseDto testCaseDto) {
		
		// Step 1: Create a new test case
        TestCase testCase = new TestCase();
        testCase.setTestCaseTitle(testCaseDto.getTestCaseTitle());
        testCase.setModuleId(736);
        testCase.setStatus("0");
        testCase.setCreatedBy("Nogrunt LLM");
        testCase.setCreatedDate(LocalDateTime.now());
        testCase = testCaseRepository.save(testCase);
       
        AtomicInteger mobileSeq = new AtomicInteger(1);
        
        List<Integer> elementIDs = new ArrayList<>(testCaseDto.getElementIDsMap().values());
         
        if (isManual) {
        	Integer oldTestCaseId = 11277;
        	List<TestStep> testSteps;
        	if (isMobile) {
        	    testSteps = testStepRepository.findTestStepsByTestCaseIdForMobile(oldTestCaseId);
        	} else {
        	    testSteps = testStepRepository.findTestStepsByTestCaseId(oldTestCaseId);
        	}
        	
        	if (testSteps == null) {
        	    testSteps = new ArrayList<>();
        	}

    	    // Loop through testSteps and add idTestStep to elementIDs
        	elementIDs.clear();
    	    for (TestStep testStep : testSteps) {
    	        elementIDs.add(testStep.getId()); // Assuming 'getId()' returns 'idTestStep'
    	    }
        }
        
        
        for (int i = 0; i < elementIDs.size(); i++) {
        	
        	int idx = i+1;
        	String elementIDKey = "elementid_"+idx;
        	Integer elementId = testCaseDto.getElementIDsMap().get(elementIDKey);
        	if (isManual) elementId = elementIDs.get(i);
        	Integer testStepId = (isMobile || isManual) ? elementId : null;
        	
        	if (isMobile) {
                processMobileTestStep(testStepId, testCase, mobileSeq);
            } else if (isManual) { 
            	processStandardTestStepByTestStepId(testStepId, testCase, i);
            } else {
                processStandardTestStep(elementId, testCase, i, testCaseDto);
            }
        	
        
        }
        
        return testCase;
    }
	
	private void processStandardTestStepByTestStepId(Integer testStepId, TestCase testCase, int i) {
		// Fetch three rows for the given testStepId
	    TestStep testStep = testStepRepository.findById(testStepId)
	        .orElseThrow(() -> new RuntimeException("No row found in test_step for testStepId: " + testStepId));
	    Boolean isUrlStep  = false;
	    if (testStep.getKeyword().equalsIgnoreCase("URL") && testStep.getAction().equalsIgnoreCase("Get")) {
	    	isUrlStep = true;
	    }
	    TestStepAttr testStepAttr = null;
	    if (!isUrlStep) {
	    	testStepAttr = testStepAttrRepository.findByTestStep(testStepId)
	    	        .orElseThrow(() -> new RuntimeException("No row found in test_step_attr for testStepId: " + testStepId));
	    }

	    // Duplicate the original three rows
	    TestStep newTestStep = createTestStep(testStep, testCase, i);
	    if (!isUrlStep) {
	    	createTestStepAttr(testStepAttr, newTestStep, testCase);
	    }
	    
	}
	
	
	private void processMobileTestStep(Integer testStepId, TestCase testCase, AtomicInteger mobileSeq) {
	    // Implement mobile-specific logic here
		// Fetch three rows for the given testStepId
	    MobileAutomation mobileAutomation = mobileAutomationRepository.findByIdtest_step(testStepId)
	        .orElseThrow(() -> new RuntimeException("No row found in mobile_automation for testStepId: " + testStepId));
	    TestStep testStep = testStepRepository.findById(testStepId)
	        .orElseThrow(() -> new RuntimeException("No row found in test_step for testStepId: " + testStepId));
	    TestStepAttr testStepAttr = testStepAttrRepository.findByTestStep(testStepId)
	        .orElseThrow(() -> new RuntimeException("No row found in test_step_attr for testStepId: " + testStepId));

	    // Check if the action is "click" or "sendkeys"
	    if (!isManual && 
	    	    ("click".equalsIgnoreCase(mobileAutomation.getAction()) || 
	    	     "sendkeys".equalsIgnoreCase(mobileAutomation.getAction()))) {
	    	
	        int previousTestStepId = testStepId - 1;

	        // Fetch three rows for testStepId - 1
	        MobileAutomation prevMobileAutomation = mobileAutomationRepository.findByIdtest_step(previousTestStepId)
	            .orElseThrow(() -> new RuntimeException("No row found in mobile_automation for testStepId: " + previousTestStepId));
	        TestStep prevTestStep = testStepRepository.findById(previousTestStepId)
	            .orElseThrow(() -> new RuntimeException("No row found in test_step for testStepId: " + previousTestStepId));
	        TestStepAttr prevTestStepAttr = testStepAttrRepository.findByTestStep(previousTestStepId)
	            .orElseThrow(() -> new RuntimeException("No row found in test_step_attr for testStepId: " + previousTestStepId));

	        // Duplicate previous three rows first
	        TestStep newTestStep = createTestStep(prevTestStep, testCase, mobileSeq.get());
	        createTestStepAttr(prevTestStepAttr, newTestStep, testCase);
	        createMobileAutomation(prevMobileAutomation, newTestStep, testCase, mobileSeq.get());
	        mobileSeq.incrementAndGet();
	    }

	    // Duplicate the original three rows
	    TestStep newTestStep = createTestStep(testStep, testCase, mobileSeq.get());
	    createTestStepAttr(testStepAttr, newTestStep, testCase);
	    createMobileAutomation(mobileAutomation, newTestStep, testCase, mobileSeq.get());
	    mobileSeq.incrementAndGet();
	}
	
	// Helper method to duplicate TestStep
	private TestStep createTestStep(TestStep oldTestStep, TestCase testCase, int sequence) {
	    TestStep newTestStep = new TestStep();
	    copyTestStepFields(oldTestStep, newTestStep);
	    newTestStep.setTestCaseId(testCase.getId());
	    newTestStep.setActualTime(System.currentTimeMillis());
	    newTestStep.setCreatedDate(LocalDateTime.now());
	    if (!isManual) {
	    	if (isMobile) {
	    		newTestStep.setTsSequence(-1);
			    newTestStep.setTs_sequence(sequence);
	    	} else {
	    		newTestStep.setTsSequence(sequence+1);
	    	}
		    newTestStep.setManuallyAdded("true");
	    }
	    return testStepRepository.save(newTestStep);
	}

	// Helper method to duplicate TestStepAttr
	private void createTestStepAttr(TestStepAttr oldTestStepAttr, TestStep testStep, TestCase testCase) {
	    TestStepAttr newTestStepAttr = new TestStepAttr();
	    copyTestStepAttrFields(oldTestStepAttr, newTestStepAttr);
	    newTestStepAttr.setTestStepId(testStep.getId());
	    testStepAttrRepository.save(newTestStepAttr);
	}

	// Helper method to duplicate MobileAutomation
	private void createMobileAutomation(MobileAutomation oldMobileAutomation, TestStep testStep, TestCase testCase, int sequence) {
	    MobileAutomation newMobileAutomation = new MobileAutomation();
	    copyMobileAutomationFields(oldMobileAutomation, newMobileAutomation);
	    newMobileAutomation.setIdtest_step(testStep.getId());
	    newMobileAutomation.setTest_case_id(testCase.getId());
	    newMobileAutomation.setTimestamp(LocalDateTime.now());
	    newMobileAutomation.setAppiumTimestamp(LocalDateTime.now());
	    if (isManual) sequence = oldMobileAutomation.getSequenceNumber();
	    newMobileAutomation.setSequenceNumber(sequence);
	    
	    mobileAutomationRepository.save(newMobileAutomation);
	}
	
	
	private void processStandardTestStep(Integer elementId, TestCase testCase, int i, LlmTestCaseDto testCaseDto) {
		// Step 2: Fetch test_step_attr row for given elementId in order
    	TestStepAttr oldTestStepAttr = null;
    	TestStep oldTestStep = null;
    	try {
    		
    		String step = null;
    		if (i < testCaseDto.getBackendExecutionSteps().size()) {
    			step = testCaseDto.getBackendExecutionSteps().get(i);
    		}
    		else {
    			step = testCaseDto.getBackendExecutionSteps().get(testCaseDto.getBackendExecutionSteps().size() - 1);
    		}
    		
    		if (elementId == 0 && step.trim().toLowerCase().startsWith("url")) {
    		    String urlValue = extractBetween(step, "~~", "~~");
    		    if (urlValue == null || urlValue.isEmpty()) {
    		        urlValue = step;
    		    }

    		    oldTestStep = new TestStep();
    		    oldTestStep.setKeyword("URL");
    		    oldTestStep.setAction("Get");
    		    oldTestStep.setTestData(urlValue);
    		    oldTestStep.setTestDataSource("LLM");
    		    oldTestStep.setObjectXpath("");

    		    oldTestStepAttr = new TestStepAttr();
    		}
    		
    		final int curElementId = elementId;
    		if (oldTestStepAttr == null) {
    			oldTestStepAttr = testStepAttrRepository.findLatestByElementId(elementId)
                        .orElseThrow(() -> new RuntimeException("No row found for elementId: " + curElementId));
    		}   		
    		
    		// Step 3: Fetch test_step row based on extracted test_step_id
    		if (oldTestStep == null) {
    			final int idTestStep =  oldTestStepAttr.getTestStepId();
    			oldTestStep = testStepRepository.findById(oldTestStepAttr.getTestStepId())
    	        		.orElseThrow(() -> new RuntimeException("No row found for idtest_step " + idTestStep));
    		}
    		
    	} catch (RuntimeException e) {
    		System.err.println("Error occurred: " + e.getMessage());
    	    
    		if (e.getMessage().contains("No row found for elementId")) {
    	        // Only create a new TestStepAttr if the error happened in the first query
    			
    			PageElement existingPageElement = pageElementRepository.findById(elementId).orElse(null);
    	        if (existingPageElement == null) {
    	            PageElement newPageElement = new PageElement();
    	            newPageElement = pageElementRepository.save(newPageElement);
    	            elementId = newPageElement.getElementid();
    	        }
    	        oldTestStepAttr = new TestStepAttr();
    	        oldTestStepAttr.setElementId(elementId);
    	    }

    	    // Always create a new TestStep if the second query fails
    		String testData = null;
    		if (i < testCaseDto.getExecutionSteps().size()) {
    			testData = testCaseDto.getExecutionSteps().get(i);
    		}
    		else {
    			testData = testCaseDto.getExecutionSteps().get(testCaseDto.getExecutionSteps().size() - 1);
    		}
    	    oldTestStep = new TestStep();
    	    oldTestStep.setTestData(testData);
    	    oldTestStep.setTestDataSource("LLM");
    	}
    	

    
    	
//    	String executionStep = testCaseDto.getExecutionSteps().get(i);
    	
//    	boolean isVerifyStep = executionStep.trim().toLowerCase().startsWith("verify");

    // Step 4: Create new test step with the new test case ID      	
    	TestStep newTestStep = new TestStep();
        copyTestStepFields(oldTestStep, newTestStep);
        newTestStep.setTsSequence(i + 1); // Set new sequence
        newTestStep.setTestCaseId(testCase.getId()); //set test case id we created
        newTestStep.setManuallyAdded("true");
        newTestStep.setActualTime(System.currentTimeMillis());
	    newTestStep.setCreatedDate(LocalDateTime.now());
        
//        if (isVerifyStep) {
//        	newTestStep.setKeyword("Assertion");
//        	newTestStep.setAction("validatePartial");
//        	newTestStep.setSubAction("validatePartial");
//        }
        newTestStep = testStepRepository.save(newTestStep);
    	
    // create new test step attr
        TestStepAttr newTestStepAttr = new TestStepAttr();
        copyTestStepAttrFields(oldTestStepAttr, newTestStepAttr);
        newTestStepAttr.setTestStepId(newTestStep.getId()); // Assign new test step ID
        newTestStepAttr = testStepAttrRepository.save(newTestStepAttr);
	}
	
	
	private void copyTestStepFields(TestStep oldTestStep, TestStep newTestStep) {
	    newTestStep.setStepNumber(oldTestStep.getStepNumber());
	    newTestStep.setPageName(oldTestStep.getPageName());
	    newTestStep.setPageDescription(oldTestStep.getPageDescription());
	    newTestStep.setObjectXpath(oldTestStep.getObjectXpath());
	    newTestStep.setKeyword(oldTestStep.getKeyword());
	    newTestStep.setAction(oldTestStep.getAction());
	    newTestStep.setSubAction(oldTestStep.getSubAction());
	    newTestStep.setFlow(oldTestStep.getFlow());
	    newTestStep.setTestData(oldTestStep.getTestData());
	    newTestStep.setVarName(oldTestStep.getVarName());
	    newTestStep.setValDevice(oldTestStep.getValDevice());
	    newTestStep.setExtKey(oldTestStep.getExtKey());
	    newTestStep.setEventTime(oldTestStep.getEventTime());
	    newTestStep.setRecordedData(oldTestStep.getRecordedData());
	    newTestStep.setFilename(oldTestStep.getFilename());
	    newTestStep.setFileField(oldTestStep.getFileField());
	    newTestStep.setDbUrl(oldTestStep.getDbUrl());
	    newTestStep.setApiId(oldTestStep.getApiId());
	    newTestStep.setApiParam(oldTestStep.getApiParam());
	    newTestStep.setDbQuery(oldTestStep.getDbQuery());
	    newTestStep.setStatus(oldTestStep.getStatus());
	    newTestStep.setDbType(oldTestStep.getDbType());
	    newTestStep.setTestDataSource(oldTestStep.getTestDataSource());
	    newTestStep.setGenDataType(oldTestStep.getGenDataType());
	    newTestStep.setGenDataLen(oldTestStep.getGenDataLen());
	    newTestStep.setGenDateFormat(oldTestStep.getGenDateFormat());
	    newTestStep.setGenDateOffset(oldTestStep.getGenDateOffset());
	    newTestStep.setDbVersion(oldTestStep.getDbVersion());
	    newTestStep.setClickable(oldTestStep.getClickable());
	    newTestStep.setIssues(oldTestStep.getIssues());
	    newTestStep.setScreenshot(oldTestStep.getScreenshot());
	    newTestStep.setEventName(oldTestStep.getEventName());
	    newTestStep.setRegex(oldTestStep.getRegex());
	    newTestStep.setIframeXpath(oldTestStep.getIframeXpath());
	    newTestStep.setAltPath(oldTestStep.getAltPath());
	    newTestStep.setOuterHtml(oldTestStep.getOuterHtml());
	    newTestStep.setWait(oldTestStep.getWait());
	    newTestStep.setWaitTime(oldTestStep.getWaitTime());
	    newTestStep.setDependantTC(oldTestStep.getDependantTC());
	    newTestStep.setManuallyAdded(oldTestStep.getManuallyAdded());
	    newTestStep.setRecordedXpath(oldTestStep.getRecordedXpath());
	    newTestStep.setSameRowType(oldTestStep.getSameRowType());
	    newTestStep.setSameRowIndex(oldTestStep.getSameRowIndex());
	    newTestStep.setTabId(oldTestStep.getTabId());
	    newTestStep.setWindowId(oldTestStep.getWindowId());
	    newTestStep.setTestStepThreshold(oldTestStep.getTestStepThreshold());
	    newTestStep.setCustomerEmail(oldTestStep.getCustomerEmail());
	    newTestStep.setCustomerPassword(oldTestStep.getCustomerPassword());
	    newTestStep.setEmailSelectionCriteria(oldTestStep.getEmailSelectionCriteria());
	    newTestStep.setEmailFilter(oldTestStep.getEmailFilter());
	    newTestStep.setType(oldTestStep.getType());
	    newTestStep.setCreatedDate(oldTestStep.getCreatedDate());
	    newTestStep.setTsSequence(oldTestStep.getTsSequence());
	    newTestStep.setTs_sequence(oldTestStep.getTs_sequence());
	    newTestStep.setActualTime(oldTestStep.getActualTime());
	}
	
	
	public static void copyTestStepAttrFields(TestStepAttr oldTestStepAttr, TestStepAttr newTestStepAttr) {
	    newTestStepAttr.setImgPath(oldTestStepAttr.getImgPath());
	    newTestStepAttr.setBgColor(oldTestStepAttr.getBgColor());
	    newTestStepAttr.setColor(oldTestStepAttr.getColor());
	    newTestStepAttr.setPageUrl(oldTestStepAttr.getPageUrl());
	    newTestStepAttr.setUniqueText(oldTestStepAttr.getUniqueText());
	    newTestStepAttr.setUniqueToParent(oldTestStepAttr.getUniqueToParent());
	    newTestStepAttr.setParentToTarget(oldTestStepAttr.getParentToTarget());
	    newTestStepAttr.setIsDynamic(oldTestStepAttr.getIsDynamic());
	    newTestStepAttr.setCreatesAlert(oldTestStepAttr.getCreatesAlert());
	    newTestStepAttr.setDynamicProcessed(oldTestStepAttr.getDynamicProcessed());
	    newTestStepAttr.setEndRange(oldTestStepAttr.getEndRange());
	    newTestStepAttr.setScope(oldTestStepAttr.getScope());
	    newTestStepAttr.setPageName(oldTestStepAttr.getPageName());
	    newTestStepAttr.setPageNumber(oldTestStepAttr.getPageNumber());
	    newTestStepAttr.setLocationStrategy(oldTestStepAttr.getLocationStrategy());
	    newTestStepAttr.setSearchPageXpath(oldTestStepAttr.getSearchPageXpath());
	    newTestStepAttr.setShadowDom(oldTestStepAttr.getShadowDom());
	    newTestStepAttr.setShadowIndex(oldTestStepAttr.getShadowIndex());
	    newTestStepAttr.setShadowElement(oldTestStepAttr.getShadowElement());
	    newTestStepAttr.setShadowPath(oldTestStepAttr.getShadowPath());
	    newTestStepAttr.setToAddress(oldTestStepAttr.getToAddress());
	    newTestStepAttr.setEmailSubject(oldTestStepAttr.getEmailSubject());
	    newTestStepAttr.setEmailContent(oldTestStepAttr.getEmailContent());
	    newTestStepAttr.setUniqueIsBackup(oldTestStepAttr.getUniqueIsBackup());
	    newTestStepAttr.setElementId(oldTestStepAttr.getElementId());
	    newTestStepAttr.setScriptFile(oldTestStepAttr.getScriptFile());
	    newTestStepAttr.setScript(oldTestStepAttr.getScript());
	    newTestStepAttr.setCmdFile(oldTestStepAttr.getCmdFile());
	    newTestStepAttr.setNearestName(oldTestStepAttr.getNearestName());
	    newTestStepAttr.setParentToTargetCvrg(oldTestStepAttr.getParentToTargetCvrg());
	    newTestStepAttr.setUniqueToParentCvrg(oldTestStepAttr.getUniqueToParentCvrg());
	    newTestStepAttr.setIndexCvrg(oldTestStepAttr.getIndexCvrg());
	    newTestStepAttr.setGetAttribute(oldTestStepAttr.getGetAttribute());
	    newTestStepAttr.setxPos(oldTestStepAttr.getxPos());
	    newTestStepAttr.setyPos(oldTestStepAttr.getyPos());
	    newTestStepAttr.setElementPlaceholder(oldTestStepAttr.getElementPlaceholder());
	    newTestStepAttr.setElementPlaceholderIndex(oldTestStepAttr.getElementPlaceholderIndex());
	    newTestStepAttr.setClassName(oldTestStepAttr.getClassName());
	    newTestStepAttr.setClassNameIndex(oldTestStepAttr.getClassNameIndex());
	    newTestStepAttr.setHref(oldTestStepAttr.getHref());
	    newTestStepAttr.setHrefIndex(oldTestStepAttr.getHrefIndex());
	    newTestStepAttr.setTypeIndex(oldTestStepAttr.getTypeIndex());
	}
	
	private void copyMobileAutomationFields(MobileAutomation oldMobileAutomation, MobileAutomation newMobileAutomation) {
	    newMobileAutomation.setAction(oldMobileAutomation.getAction());
	    newMobileAutomation.setElementId(oldMobileAutomation.getElementId());
	    newMobileAutomation.setDevice(oldMobileAutomation.getDevice());
	    newMobileAutomation.setSequenceNumber(oldMobileAutomation.getSequenceNumber());
	    newMobileAutomation.setStrategyMap(oldMobileAutomation.getStrategyMap());
	    newMobileAutomation.setAndroidUiautomator(oldMobileAutomation.getAndroidUiautomator());
	    newMobileAutomation.setXpath(oldMobileAutomation.getXpath());
	    newMobileAutomation.setIdStrategy(oldMobileAutomation.getIdStrategy());
	}
	
	private String extractBetween(String text, String start, String end) {
	    int startIdx = text.indexOf(start);
	    int endIdx = text.indexOf(end, startIdx + start.length());

	    if (startIdx != -1 && endIdx != -1 && endIdx > startIdx) {
	        return text.substring(startIdx + start.length(), endIdx).trim();
	    }
	    return null;
	}

    
}
