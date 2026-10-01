package nogrunt.springboot.Controllers.dtoModel;

public class MobileTestCaseDto {

	private Integer token;
	private String module;
	private String testScenario;

	public Integer getToken() {
		return token;
	}

	public void setToken(Integer token) {
		this.token = token;
	}

	public String getModule() {
		return module;
	}

	public void setModule(String module) {
		this.module = module;
	}

	public String getTestScenario() {
		return testScenario;
	}

	public void setTestScenario(String testScenario) {
		this.testScenario = testScenario;
	}

}
