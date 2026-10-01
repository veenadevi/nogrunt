package nogrunt.springboot.Controllers.repository;

import nogrunt.springboot.Controllers.model.TestCaseResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MobileCaseResultRepo extends JpaRepository<TestCaseResult, Integer> {

    @Modifying
    @Query(value = "UPDATE cta.test_case_results SET Status = :Status, Duration = :Duration WHERE Test_Case_Id = :Test_Case_Id order by idtest_case_results desc limit 1", nativeQuery = true)
    void updateByTestCaseId(@Param("Test_Case_Id") Integer Test_Case_Id, @Param("Status") String Status, @Param("Duration") Double Duration);

    @Query(value = "SELECT idtest_case_results FROM cta.test_case_results WHERE Test_Case_Id = :Test_Case_Id order by idtest_case_results desc limit 1", nativeQuery = true)
    Integer getIdCaseResult(@Param("Test_Case_Id") Integer Test_Case_Id);
}
