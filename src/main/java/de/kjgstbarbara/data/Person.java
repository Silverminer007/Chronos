package de.kjgstbarbara.data;

import de.kjgstbarbara.service.RoleRepository;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.security.core.GrantedAuthority;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Entity
@Data
@NoArgsConstructor
public class Person {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Setter(AccessLevel.PACKAGE)
    private long id;
    private String firstName;
    private String lastName;
    private String username = "";
    private LocalDate birthDate;
    private long phoneNumber = 0L;
    private String password;
    @ManyToMany(fetch = FetchType.EAGER)
    @Getter(value = AccessLevel.PRIVATE)
    private List<Role> roles = new ArrayList<>();

    public String toString() {
        return firstName + " " + lastName;
    }


    public int hashCode() {
        return Objects.hashCode(this.getId());
    }

    public boolean equals(Object o) {
        if (o instanceof Person p) {
            return Objects.equals(p.getId(), this.getId());
        }
        return false;
    }

    public boolean hasAuthority(Role.Type type, Role.Scope scope, Object id) {
        for (Role role : roles) {
            if (type.matches(role.getType()) &&
                    scope.matches(role.getScope()) &&
                    (role.idMatches(id) || role.getScopeID() == null)) {
                return true;
            }
        }
        return false;
    }

    public boolean hasRole(Role.Type type, Role.Scope scope, Object id) {
        for (Role role : roles) {
            if (role.getType().equals(type) &&
                    role.getScope().equals(scope) &&
                    (role.idMatches(id) || role.getScopeID() == null)) {
                return true;
            }
        }
        return false;
    }

    public void removeRole(Role.Type type, Role.Scope scope, Object id) {
        roles.removeIf(role ->
                role.getType().equals(type) &&
                role.getScope().equals(scope) &&
                (role.idMatches(id) || role.getScopeID() == null));
    }

    public void grantRole(Role.Type type, Role.Scope scope, Object id) {
        if(!hasRole(type, scope, id)) {
            this.getRoles().add(Role.createRole(scope, type, id));
        }
    }

    public void saveRoles(RoleRepository roleRepository) {
        for (Role r : this.getRoles()) {
            roleRepository.save(r);
        }
    }

    public List<GrantedAuthority> rolesAsAuthorities() {
        return this.getRoles().stream().map(Role::asGrantedAuthority).toList();
    }
}