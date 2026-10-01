package nogrunt.springboot.Controllers.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "test_case", schema = "cta") // Assuming schema is "cta"
public class TestCase {

	@Id
	@Column(name = "idtest_case")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "Test_Case", nullable = false)
    private String testCaseTitle;

    @Column(name = "Module", nullable = false)
    private Integer moduleId;
    
    @Column(name = "Created_By")
    private String createdBy;

	@Column(name = "Created_Date")
    private LocalDateTime createdDate;
    
    @Column(name = "status")
    private String status;
    
    
    //getters and setters
	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getTestCaseTitle() {
		return testCaseTitle;
	}

	public void setTestCaseTitle(String testCaseTitle) {
		this.testCaseTitle = testCaseTitle;
	}

	public Integer getModuleId() {
		return moduleId;
	}

	public void setModuleId(Integer moduleId) {
		this.moduleId = moduleId;
	}

	public LocalDateTime getCreatedDate() {
		return createdDate;
	}

	public void setCreatedDate(LocalDateTime createdDate) {
		this.createdDate = createdDate;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}
	
	public String getCreatedBy() {
		return createdBy;
	}

	public void setCreatedBy(String createdBy) {
		this.createdBy = createdBy;
	}
    
}
