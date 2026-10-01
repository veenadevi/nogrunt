package nogrunt.springboot.Controllers.repository;

import nogrunt.springboot.Controllers.model.MobileTestCase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface MobileTestCaseRepo extends JpaRepository<MobileTestCase, Integer> {

    @Query(value = "SELECT idtest_case FROM test_case WHERE extKey = :extKey ORDER BY idtest_case DESC LIMIT 1", nativeQuery = true)
    Integer findByExtKey(@Param("extKey") Integer extKey);

    @Query(value = "SELECT COUNT(*) > 0 FROM cta.test_case WHERE idtest_case = :idtest_case",nativeQuery = true)
    Integer existsByTestCaseId(@Param("idtest_case") Integer idtest_case);

    @Query(value = "SELECT case_Type FROM cta.test_case WHERE idtest_case = :idtest_case", nativeQuery = true)
    String getCaseType(@Param("idtest_case") Integer idtest_case);

    @Modifying
    @Query(value = "UPDATE cta.test_case SET case_Type = :caseType WHERE idtest_case = :idtestCase", nativeQuery = true)
    void updateCaseType(@Param("idtestCase") Integer idtestCase, @Param("caseType") String caseType);

    @Query(value = "select Test_Case from cta.test_case where idtest_case = :Test_Case_Id",nativeQuery = true)
    String getTestCaseName(@Param("Test_Case_Id")Integer Test_Case_Id);

    @Query(value = "SELECT extKey FROM cta.test_case WHERE idtest_case = :idtest_case", nativeQuery = true)
    Integer getExtKey(@Param("idtest_case") Integer idtest_case);

}
