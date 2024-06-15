package de.kjgstbarbara.gameevaluation.data;

import de.kjgstbarbara.data.Person;
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
    @Deprecated
    private int maxPoints;

    public int getPointsOf(Participant participant) {
        for (GameGroup gameGroup : gameGroups) {
            if (gameGroup.getParticipants().contains(participant)) {
                return gameGroup.getPoints();
            }
        }
        return 0;
    }

    public int getMaxPoints() {
        int highestPoints = 0;
        for (GameGroup g : gameGroups) {
            highestPoints = Math.max(highestPoints, g.getPoints());
        }
        return highestPoints;
    }

    public boolean didParticipate(Participant participant) {
        for (GameGroup g : gameGroups) {
            if (g.getParticipants().contains(participant)) {
                return true;
            }
        }
        return false;
    }
}
