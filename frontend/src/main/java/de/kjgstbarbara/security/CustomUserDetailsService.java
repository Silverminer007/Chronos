package de.kjgstbarbara.security;

import de.kjgstbarbara.service.PersonsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;

@Service
public class CustomUserDetailsService implements UserDetailsService {
    @Autowired
    private PersonsRepository personsRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return personsRepository.findByUsername(username).map(person ->
                        new User(person.getUsername(), person.getPassword(),
                                new ArrayList<>()))
                .orElseThrow(() -> new UsernameNotFoundException("Username not found"));
    }
}