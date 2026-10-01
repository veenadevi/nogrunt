package nogrunt.springboot.Controllers.repository;

import nogrunt.springboot.Controllers.model.TestStepResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MobileStepsResultRepo extends JpaRepository<TestStepResult, Integer> {
}
