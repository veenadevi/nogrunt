package nogrunt.springboot.Controllers.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import nogrunt.springboot.Controllers.model.TestCase;

@Repository
public interface TestCaseRepository extends JpaRepository<TestCase, Integer> {

}
