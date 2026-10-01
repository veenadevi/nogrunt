package nogrunt.springboot.Controllers.repository;


import org.springframework.data.jpa.repository.JpaRepository;

import nogrunt.springboot.Controllers.model.PageElement;

public interface PageElementRepository extends JpaRepository<PageElement, Integer> {
}