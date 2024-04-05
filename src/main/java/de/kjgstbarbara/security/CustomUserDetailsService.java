package de.kjgstbarbara.security;

import de.kjgstbarbara.data.Role;
import de.kjgstbarbara.service.PersonsRepository;
import de.kjgstbarbara.service.PersonsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsService implements UserDetailsService {
    @Autowired
    private PersonsRepository personsRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return personsRepository.findByUsername(username).map(person ->
                        new User(person.getUsername(), person.getPassword(),
                                person.rolesAsAuthorities()))
                .orElseThrow(() -> new UsernameNotFoundException("Username not found"));
    }
}