package de.kjgstbarbara.service;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Getter
public class PasswordResetService {
    @Autowired
    private PasswordResetRepository passwordResetRepository;
}
