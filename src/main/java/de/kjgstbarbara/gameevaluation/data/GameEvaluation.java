package de.kjgstbarbara.gameevaluation.data;

import de.kjgstbarbara.data.Group;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Entity
@Data
public class GameEvaluation extends AbstractEntity {
    @OneToMany(fetch = FetchType.EAGER)
    private List<Participant> participants = new ArrayList<>();
    @OneToMany(fetch = FetchType.EAGER)
    private List<Game> games = new ArrayList<>();
    private String name;
    @ManyToOne
    private Group group;

    public int getPointsOf(Participant participant) {
        int points = 0;
        for (Game game : games) {
            points += game.getPointsOf(participant);
        }
        return points;
    }

    public double getScoreOf(Participant participant) {
        double maxPoints = getMaxPointsFor(participant);
        return maxPoints == 0 ? 1.0D : getPointsOf(participant) / maxPoints;
    }

    public double getMaxPointsFor(Participant participant) {
        double maxPoints = 0;
        for (Game game : games) {
            if (game.didParticipate(participant)) {
                maxPoints += game.getMaxPoints();
            }
        }
        return maxPoints;
    }

    public List<Participant> getScoreBoard() {
        return participants.stream().sorted(Comparator.comparing(this::getScoreOf).reversed()).toList();
    }
}
