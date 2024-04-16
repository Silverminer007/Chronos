package de.kjgstbarbara.data;

import jakarta.annotation.Nullable;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Entity
public class Date implements Comparable<Date> {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Setter(AccessLevel.PRIVATE)
    private long id;
    private String title;
    private LocalDateTime start;
    @Nullable
    private LocalDateTime end;
    private String attachment;
    private boolean internal = true;
    private boolean pollRunning = true;
    @ManyToOne
    private Board board;

    @Override
    public int compareTo(Date o) {
        return start.compareTo(o.getStart());
    }
}