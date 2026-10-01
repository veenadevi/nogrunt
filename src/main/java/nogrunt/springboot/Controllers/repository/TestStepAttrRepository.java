package nogrunt.springboot.Controllers.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import nogrunt.springboot.Controllers.model.TestStep;
import nogrunt.springboot.Controllers.model.TestStepAttr;

import java.util.List;
import java.util.Optional;

@Repository
public interface TestStepAttrRepository extends JpaRepository<TestStepAttr, Integer> {

	// Fetch the latest row for a given elementId
	@Query(value = """
	        SELECT tsa.* 
	        FROM test_step_attr tsa
	        JOIN test_step ts ON tsa.test_step = ts.idtest_step
	        WHERE tsa.elementId = :elementId 
	        AND ts.manuallyAdded IS NULL
	        ORDER BY tsa.test_step DESC
	        LIMIT 1
	        """, nativeQuery = true)
    Optional<TestStepAttr> findLatestByElementId(@Param("elementId") Integer elementId);
	
	@Query(value = "SELECT * FROM cta.test_step_attr where test_step = :testStepId", nativeQuery = true)
	Optional<TestStepAttr> findByTestStep(@Param("testStepId") int testStepId);
}