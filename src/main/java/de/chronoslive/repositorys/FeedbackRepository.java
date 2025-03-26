package de.chronoslive.repositorys;

import de.chronoslive.entitys.Feedback;
import de.chronoslive.entitys.Person;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FeedbackRepository extends JpaRepository<Feedback, Long> {
    void deleteByPerson(Person person);
}
