package de.kjgstbarbara.gameevaluation.data;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToMany;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
@Entity
public class Game extends AbstractEntity {
    @OneToMany(fetch = FetchType.EAGER)
    private List<GameGroup> gameGroups;
    private String name;
    private int maxPoints;

    public int getPointsOf(Participant participant) {
        for (GameGroup gameGroup : gameGroups) {
            if (gameGroup.getParticipants().contains(participant)) {
                return gameGroup.getPoints();
            }
        }
        return 0;
    }
}
