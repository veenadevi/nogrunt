package nogrunt.springboot.Controllers.repository;

import nogrunt.springboot.Controllers.model.MobileTestStepAttr;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MobileTestStepAttrRepo extends JpaRepository<MobileTestStepAttr, Integer> {
}
