package nogrunt.springboot.Controllers.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import nogrunt.springboot.Controllers.model.TestStep;

import java.util.List;
import java.util.Optional;

@Repository
public interface TestStepRepository extends JpaRepository<TestStep, Integer> {

	Optional<TestStep> findById(Integer id);
	
	 @Query(value = "SELECT * FROM test_step WHERE Test_Case_Id = :testCaseId ORDER BY ts_sequence ASC", nativeQuery = true)
	 List<TestStep> findTestStepsByTestCaseIdForMobile(@Param("testCaseId") Integer testCaseId);
	 
	 @Query(value = "SELECT * FROM test_step WHERE Test_Case_Id = :testCaseId ORDER BY tsSequence ASC", nativeQuery = true)
	 List<TestStep> findTestStepsByTestCaseId(@Param("testCaseId") Integer testCaseId);
}
