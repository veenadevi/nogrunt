package nogrunt.springboot.Controllers.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import nogrunt.springboot.Controllers.model.TestCaseCapability;

@Repository
public interface TestCaseCapRepo extends JpaRepository<TestCaseCapability, Integer> {
	
	TestCaseCapability findByTestCaseId(Integer TestCaseId);

}
