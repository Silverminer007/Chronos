package de.kjgstbarbara.data;

import jakarta.persistence.*;
import lombok.*;

@Data
@NoArgsConstructor
@Entity
public class Board {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Setter(AccessLevel.PRIVATE)
    private long id;
    @ManyToOne
    private Organisation organisation;
    @NonNull
    private String title;
    private String memberTitle = "Mitglieder";
    private String dateTitle = "Termine";
    private boolean memberEnabled = true;
    private boolean dateEnabled = true;

}