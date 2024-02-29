package de.kjgstbarbara.service;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
@Getter
public class RoleService {
    private final RoleRepository roleRepository;
}
