package nogrunt.springboot.Controllers.testCaseDtos;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class LlmTestCaseDto {
	
	@JsonProperty("testCaseTitle")
    private String testCaseTitle;
	
    private String section;
    
    private String type;
    
    @JsonProperty("elementIDs")
    private Map<String, Integer> elementIDsMap;
    
    @JsonProperty("executionSteps")
    private List<String> executionSteps;
    
    @JsonProperty("backend_execution_steps")
    private List<String> backendExecutionSteps;

    // Getters and Setters
    public List<String> getBackendExecutionSteps() {
		return backendExecutionSteps;
	}

	public void setBackendExecutionSteps(List<String> backendExecutionSteps) {
		this.backendExecutionSteps = backendExecutionSteps;
	}

    public String getTestCaseTitle() {
        return testCaseTitle;
    }

    public void setTestCaseTitle(String testCaseTitle) {
        this.testCaseTitle = testCaseTitle;
    }

    public String getSection() {
        return section;
    }

    public void setSection(String section) {
        this.section = section;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Map<String, Integer> getElementIDsMap() {
        return elementIDsMap;
    }
    
    public void setElementIDsMap(Map<String, Integer> elementIDsMap) {
		this.elementIDsMap = elementIDsMap;
	}

	public List<String> getExecutionSteps() {
		return executionSteps;
	}

	public void setExecutionSteps(List<String> executionSteps) {
		this.executionSteps = executionSteps;
	}
    
}
