package de.kjgstbarbara.service;

import de.kjgstbarbara.data.Feedback;
import de.kjgstbarbara.data.Person;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FeedbackRepository extends JpaRepository<Feedback, Long> {
    void deleteByPerson(Person person);
}
