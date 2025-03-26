package de.chronoslive.entitys;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Entity
@NoArgsConstructor
public class PollReminder {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long id;
    @ManyToOne
    private Date date;
    private LocalDateTime pollIntervalStart;
    private LocalDateTime pollIntervalEnd;
    @ManyToOne
    private Person pollStarter;
    private int amountOfTimesSend;
}
