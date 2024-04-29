package de.kjgstbarbara.service;

import de.kjgstbarbara.data.Feedback;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FeedbackRepository extends JpaRepository<Feedback, Long> {
}
