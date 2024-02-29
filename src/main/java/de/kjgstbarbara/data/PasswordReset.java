package de.kjgstbarbara.data;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;

import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
public class PasswordReset {
    @Id
    @NonNull
    private String token;
    @NonNull
    private LocalDateTime requested = LocalDateTime.now();
    @NonNull
    @ManyToOne
    private Person requester;

    public boolean expired() {
        return requested.plusHours(24L).isBefore(LocalDateTime.now());
    }
}