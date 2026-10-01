package nogrunt.springboot.Controllers.service;

import jakarta.transaction.Transactional;
import nogrunt.springboot.Controllers.model.TestCaseResult;
import nogrunt.springboot.Controllers.model.TestStepResult;
import nogrunt.springboot.Controllers.repository.MobileCaseResultRepo;
import nogrunt.springboot.Controllers.repository.MobileStepsResultRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class MobileResultService {

	@Autowired
	private MobileCaseResultRepo mobileCaseResultRepo;

	@Autowired
	private TokenVerificationService tokenVerificationService;

	@Autowired
	private MobileTestService mobileTestService;

	@Autowired
	private MobileStepsResultRepo mobileStepsResultRepo;

	@Transactional
	public Integer saveCaseResults(String token, Integer Test_Case_Id, String status, Double duration) {

		TestCaseResult testCaseResult = new TestCaseResult();
		testCaseResult.setTest_Case_Id(Test_Case_Id);
		testCaseResult.setExecuted_Date(LocalDateTime.now());
		testCaseResult.setStatus(status);
		testCaseResult.setDuration(duration);

		String uname = tokenVerificationService.getUsernameByToken(String.valueOf(token));
		testCaseResult.setExecuted_By(uname);

		String testCaseName = mobileTestService.getTestCaseName(Test_Case_Id);
		testCaseResult.setTest_case_name(testCaseName);

		TestCaseResult savedResult = mobileCaseResultRepo.save(testCaseResult);

		return savedResult.getIdtest_case_results();
	}

	@Transactional
	public String updateCaseResultStatus(Integer Test_Case_Id, String Status, Double Duration) {
		try {
			mobileCaseResultRepo.updateByTestCaseId(Test_Case_Id, Status, Duration);
			return "Test case result updated successfully";
		} catch (Exception e) {
			return "Error updating test case result: " + e.getMessage();
		}
	}

	public Integer getIdCaseResult(Integer Test_Case_Id) {
		return mobileCaseResultRepo.getIdCaseResult(Test_Case_Id);
	}

	// FOR MOBILE TEST STEP RESULTS METHOD
	public Integer saveMobileStepsResults(Integer Step_Number, String Object_Xpath, String Keyword, String Action,
			String Flow, String testDataResult, String Status, Integer Test_Step, Double Duration,
			Integer Test_Case_Results_Id, LocalDateTime Executed_Date, String ThreshholdStatus, String ssfilename,
			String elementDescription, String findAssignScreenshot, String stepErrorCode, String outerhtml,
			String stepErrorMessage) {

		TestStepResult testStepResult = new TestStepResult();

		testStepResult.setStep_Number(Step_Number);
		testStepResult.setObject_Xpath(Object_Xpath);
		testStepResult.setKeyword(Keyword);
		testStepResult.setAction(Action);
		testStepResult.setFlow(Flow);
		testStepResult.setTestData(testDataResult);
		testStepResult.setStatus(Status);
		testStepResult.setTest_Step(Test_Step);
		testStepResult.setDuration(Duration);
		testStepResult.setTest_Case_Results_Id(Test_Case_Results_Id);
		testStepResult.setExecuted_Date(Executed_Date);
		testStepResult.setThreshholdStatus(ThreshholdStatus);
		testStepResult.setPage_Description(elementDescription);
		testStepResult.setFailure_Screenshot_Location(ssfilename);
		testStepResult.setScreenshot1(findAssignScreenshot);
		testStepResult.setIssuetype(stepErrorCode);
		testStepResult.setOuterhtml(outerhtml);
		testStepResult.setIssues(stepErrorMessage);

		TestStepResult savedStepsResult = mobileStepsResultRepo.save(testStepResult);
		return savedStepsResult.getIdtest_step_result();
	}
}
