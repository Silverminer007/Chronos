package de.kjgstbarbara.chronos.data;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.ArrayList;
import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Entity
@Data
@Table(name = "gruppe")
public class GameGroup extends AbstractEntity {
    @ManyToMany(fetch = FetchType.EAGER)
    private List<Participant> participants = new ArrayList<>();
    private int points;
    private String name;
}
