package de.kjgstbarbara.chronos.service;

import lombok.Getter;
import org.springframework.stereotype.Service;

@Getter
@Service
public class FeedbackService {
    private final FeedbackRepository feedbackRepository;

    public FeedbackService(FeedbackRepository feedbackRepository) {
        this.feedbackRepository = feedbackRepository;
    }
}
