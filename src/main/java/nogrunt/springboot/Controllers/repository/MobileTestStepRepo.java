package nogrunt.springboot.Controllers.repository;

import nogrunt.springboot.Controllers.model.MobileTestStep;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;


@Repository
public interface MobileTestStepRepo extends JpaRepository<MobileTestStep, Integer> {

    @Query(value = "SELECT * FROM cta.test_step ORDER BY idtest_step DESC LIMIT 5", nativeQuery = true)
    List<MobileTestStep> getAllUsersDataAsJson();
}
