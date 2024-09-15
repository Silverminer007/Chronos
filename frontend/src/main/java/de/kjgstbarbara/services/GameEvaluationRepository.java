package de.kjgstbarbara.services;

import de.kjgstbarbara.data.Person;
import de.kjgstbarbara.data.GameEvaluation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface GameEvaluationRepository extends JpaRepository<GameEvaluation, Long>, JpaSpecificationExecutor<GameEvaluation> {
    Page<GameEvaluation> findByGroupMembersIn(Pageable pageable, List<Person> personList);
}