package de.kjgstbarbara.service;

import de.kjgstbarbara.data.Date;
import de.kjgstbarbara.data.Feedback;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FeedbackRepository extends JpaRepository<Feedback, Feedback.Key> {
    default List<Feedback> findByDate(Date date) {
        return findAll().stream().filter(feedback -> feedback.getKey().getDate().getId() == date.getId()).toList();
    }
}
