package de.kjgstbarbara.data;

import jakarta.annotation.Nullable;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Data;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Objects;

@Entity
@Data
public class Role {
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Id
    private long id;
    private Scope scope;
    private Type type;
    @Nullable
    private String scopeID;

    public static Role createRole(Scope scope, Type type, Object id) {
        Role role = new Role();
        role.setScope(scope);
        role.setType(type);
        role.setScopeID(id == null ? null : String.valueOf(id));
        return role;
    }

    public boolean idMatches(Object id) {
        if(id == null) {
            return this.scopeID == null;
        } else {
            return Objects.equals(scopeID, String.valueOf(id));
        }
    }

    public GrantedAuthority asGrantedAuthority() {
        return new SimpleGrantedAuthority(this.scope + "_" + this.type + "_" + (this.scopeID == null ? "*" : this.scopeID));
    }

    @Getter
    public enum Scope {
        ALL(null), BOARD(ALL), ORGANISATION(ALL);
        private final Scope parent;
        Scope(Scope parent) {
            this.parent = parent;
        }

        public boolean matches(Scope scope) {
            return scope.equals(this) || (this.getParent() != null && this.getParent().matches(scope));
        }
    }

    @Getter
    public enum Type {
        ADMIN(null), MEMBER(ADMIN);
        private final Type parent;
        Type(@Nullable Type type) {
            parent = type;
        }

        public boolean matches(Type type) {
            return type.equals(this) || (this.getParent() != null && this.getParent().matches(type));
        }
    }
}