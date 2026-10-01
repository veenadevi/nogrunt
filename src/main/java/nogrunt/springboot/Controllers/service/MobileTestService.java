package nogrunt.springboot.Controllers.service;

import jakarta.transaction.Transactional;
import nogrunt.Utilities;
import nogrunt.springboot.Controllers.dtoModel.MobileAutomationDTO;
import nogrunt.springboot.Controllers.dtoModel.MobileTestCaseDto;
import nogrunt.springboot.Controllers.model.MobileTestCase;
import nogrunt.springboot.Controllers.model.MobileTestStep;
import nogrunt.springboot.Controllers.model.MobileTestStepAttr;
import nogrunt.springboot.Controllers.repository.MobileTestCaseRepo;
import nogrunt.springboot.Controllers.repository.MobileTestStepAttrRepo;
import nogrunt.springboot.Controllers.repository.MobileTestStepRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;

@Service
public class MobileTestService {

    @Autowired
    private MobileTestStepRepo mobileTestStepRepo;

    @Autowired
    private MobileTestCaseRepo mobileTestCaseRepo;

    @Autowired
    private TokenVerificationService tokenVerificationService;

    @Autowired
    private MobileTestStepAttrRepo mobileTestStepAttrRepo;
    
    @Autowired
    private MobileCapabilitiesService mobileCapabilitiesService;

    //Save into TestStep Table
    public Integer saveEntity(MobileAutomationDTO automationDTO) {

        MobileTestStep entity = new MobileTestStep();

        if (automationDTO.getTestCaseId() != null){
            entity.setTestCaseId(automationDTO.getTestCaseId());
        }else {
            Integer testCaseId = getIdTestCase(automationDTO.getExtKey());
            entity.setTestCaseId(testCaseId);
        }
        entity.setPageDescription(automationDTO.getElement());
        entity.setTestData(automationDTO.getElement());
        entity.setStatus(0);
        entity.setTestData(automationDTO.getTestData());
        entity.setTsSequence(automationDTO.getSequenceNumber());
        MobileTestStep savedEntity = mobileTestStepRepo.save(entity);
        Integer testStepId = savedEntity.getIdTestStep();

        saveMobileTestStepAttr(testStepId);

        return testStepId;
    }

    //check whether testcase Exists ot Not
    public boolean testCaseExists(Integer idtest_case) {
        return idtest_case != null && mobileTestCaseRepo.existsByTestCaseId(idtest_case) > 0;
    }


    //Save into TestCase Table
    public Integer saveTestCase(MobileTestCaseDto mobileTestCaseDto, Integer capabilities_id) {

        MobileTestCase mobileTestCase = new MobileTestCase();

        mobileTestCase.setTestCase(mobileTestCaseDto.getTestScenario());
        mobileTestCase.setExtKey(mobileTestCaseDto.getToken());
        mobileTestCase.setModule(mobileTestCaseDto.getModule());
        mobileTestCase.setSaveType("Recording");
        mobileTestCase.setStatus("0");
        mobileTestCase.setCaseType("MOBILE");
        mobileTestCase.setCreatedDate(LocalDateTime.now());

        String token = String.valueOf(mobileTestCaseDto.getToken());
        String uname = tokenVerificationService.getUsernameByToken(token);
        mobileTestCase.setCreatedBy(uname);

        Integer companyId = tokenVerificationService.getCompanyIdByToken(token);

        MobileTestCase savedEntity = mobileTestCaseRepo.save(mobileTestCase);
        Integer testcaseId = savedEntity.getIdtest_case();
        
        mobileCapabilitiesService.savetestCaseCapMapId(testcaseId, capabilities_id);

        Utilities.createTestCaseFolder(companyId, testcaseId);
        return testcaseId;
    }


    //To Get TestCaseId by using extKey
    public Integer getIdTestCase(Integer extKey) {
        return mobileTestCaseRepo.findByExtKey(extKey);
    }

    //TO GET TEST CASE NAME USING Test_Case_Id
    public String getTestCaseName(Integer Test_Case_Id){
        return mobileTestCaseRepo.getTestCaseName(Test_Case_Id);
    }

    //TO GET EXT KEY USING TEST_CASE_ID
    public Integer getExtKeyByTestCaseId(Integer idtest_case){
        return mobileTestCaseRepo.getExtKey(idtest_case);
    }

    //To update caseType in Test case table
    @Transactional
    public String updateCaseType(Integer idtest_case) {
        String currentCaseType = mobileTestCaseRepo.getCaseType(idtest_case);

        if (currentCaseType == null) {
            mobileTestCaseRepo.updateCaseType(idtest_case, "WEB and MOBILE");
            return "CaseType updated to WEB and MOBILE.";
        }
        switch (currentCaseType.toUpperCase()) {
            case "WEB":
                mobileTestCaseRepo.updateCaseType(idtest_case, "WEB and MOBILE");
                return "CaseType updated to WEB and MOBILE.";
            case "WEB AND MOBILE":
            case "MOBILE":
                return "No changes required.";
            default:
                return "Test case doesn't exist.";
        }
    }

    //TestStepAttr Row Creation
    public Integer saveMobileTestStepAttr(Integer testStepId){
        MobileTestStepAttr stepAttr = new MobileTestStepAttr();
        stepAttr.setTestStep(testStepId);

        MobileTestStepAttr saveAttr = mobileTestStepAttrRepo.save(stepAttr);

        return saveAttr.getId();
    }
}
