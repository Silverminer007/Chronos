package de.kjgstbarbara.chronos.service;

import de.kjgstbarbara.chronos.data.Feedback;
import de.kjgstbarbara.chronos.data.Person;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FeedbackRepository extends JpaRepository<Feedback, Long> {
    void deleteByPerson(Person person);
}
