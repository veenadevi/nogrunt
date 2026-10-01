package nogrunt.springboot.Controllers.repository;

import nogrunt.springboot.Controllers.model.MobileAutomation;
import nogrunt.springboot.Controllers.model.TestStep;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MobileAutomationRepository extends JpaRepository<MobileAutomation, Integer> {

    @Query(value = "SELECT * FROM mobile_automation WHERE test_case_id = :idtest_case", nativeQuery = true)
    List<MobileAutomation> findByIdTest_case(@Param("idtest_case") Integer idtest_case);

    @Query(value = "SELECT * FROM cta.mobile_automation WHERE idtest_step = :testStepId", nativeQuery = true)
	Optional<MobileAutomation> findByIdtest_step(@Param("testStepId") int testStepId);
}