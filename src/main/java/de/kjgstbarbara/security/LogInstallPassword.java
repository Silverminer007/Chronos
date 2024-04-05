package de.kjgstbarbara.security;

import de.kjgstbarbara.data.Person;
import de.kjgstbarbara.data.Role;
import de.kjgstbarbara.service.PersonsRepository;
import de.kjgstbarbara.service.RoleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

import static de.kjgstbarbara.security.SecurityUtils.generatePassword;

@Component
public class LogInstallPassword implements CommandLineRunner {
    @Autowired
    private PersonsRepository personsRepository;
    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        if (personsRepository.findByUsername("admin").isEmpty()) {
            System.out.println("###### Generating default ADMIN User: START");
            Person admin = new Person();
            admin.setUsername("admin");
            admin.setFirstName("Admin");
            admin.setLastName("");
            admin.setBirthDate(LocalDate.now());
            String password = generatePassword(15);
            admin.setPassword(passwordEncoder.encode(password));
            System.out.println("###### Generating default ADMIN Users Username: admin");
            System.out.println("###### Generating default ADMIN Users Password: " + password);
            admin.grantRole(Role.Type.ADMIN, Role.Scope.ALL, null);
            admin.saveRoles(roleRepository);
            personsRepository.save(admin);
            System.out.println("###### Generating default ADMIN User: END");
            if (personsRepository.findByUsername("admin").isEmpty()) {
                System.out.println("###### Generating default ADMIN User: ERROR");
            }
        }
    }
}