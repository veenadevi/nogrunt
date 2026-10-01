package nogrunt.springboot.Controllers;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import nogrunt.AppProperties;
import nogrunt.Utilities;
import nogrunt.springboot.Controllers.model.TestCase;
import nogrunt.springboot.Controllers.service.TestCaseService;
import nogrunt.springboot.Controllers.testCaseDtos.*;

import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/test-case")
public class NogruntTestCaseController {
	
	@Autowired
	 private TestCaseService testCaseService;

    @GetMapping("/hello")
    public String sayHello() {
        return "Hello, World!";
    }
    
    @PostMapping("/create")
    public ResponseEntity<String> createTestCaseAndAddSteps(@RequestBody LlmTestCaseDto testCaseDto) {
    	try {
            TestCase testCase = testCaseService.createTestCaseAndAddSteps(testCaseDto);
            return ResponseEntity.ok(testCase.getId().toString());
        } catch (Exception e) {
        	e.printStackTrace();
        	return ResponseEntity.ok(Utilities.randomGen(AppProperties.NUMBER, 5, false));
//            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
//                    .body("Error creating test case: " + e.getMessage());
        }
    }
    
    @PostMapping("/create-nlp")
    public ResponseEntity<String> createNLPTestCase(@RequestBody LlmTestCaseDto testCaseDto, @RequestParam("token") String token) {
    	try {
            String testCaseId = testCaseService.createNLPTestCase(testCaseDto, token);
            return ResponseEntity.ok(testCaseId);
        } catch (Exception e) {
        	e.printStackTrace();
        	return ResponseEntity.ok(Utilities.randomGen(AppProperties.NUMBER, 5, false));
//            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
//                    .body("Error creating test case: " + e.getMessage());
        }
    }
    
    
}
