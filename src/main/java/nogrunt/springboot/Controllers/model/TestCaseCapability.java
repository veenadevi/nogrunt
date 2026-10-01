package nogrunt.springboot.Controllers.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "testcase_capability_map")
public class TestCaseCapability {
	
	@Id
	@Column(name = "mapping_Id")
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private int mappingId;

	@Column(name = "testcase_Id")
	private int testCaseId;

	@Column(name = "capabilities_id")
	private int capabilitiesId;

	public int getMappingId() {
		return mappingId;
	}

	public void setMappingId(int mappingId) {
		this.mappingId = mappingId;
	}

	public int getTestCaseId() {
		return testCaseId;
	}

	public void setTestCaseId(int testCaseId) {
		this.testCaseId = testCaseId;
	}

	public int getCapabilitiesId() {
		return capabilitiesId;
	}

	public void setCapabilitiesId(int capabilitiesId) {
		this.capabilitiesId = capabilitiesId;
	}
}
