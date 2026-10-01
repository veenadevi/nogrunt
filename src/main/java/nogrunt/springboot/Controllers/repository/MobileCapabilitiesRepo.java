package nogrunt.springboot.Controllers.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import nogrunt.springboot.Controllers.model.MobileCapabilities;

@Repository
public interface MobileCapabilitiesRepo extends JpaRepository<MobileCapabilities, Integer> {

	MobileCapabilities findByCapabilitiesId(Integer capabilitiesId);

	List<MobileCapabilities> findByCompanyId(Integer companyId);
}
