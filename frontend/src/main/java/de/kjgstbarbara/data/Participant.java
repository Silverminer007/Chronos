package de.kjgstbarbara.data;

import jakarta.persistence.Entity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Entity
@Data
public class Participant extends AbstractEntity {
    private String name;
}
