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

    public double getScoreOf(Participant participant) {// TODO
        double points = 0;
        for (Game game : games) {
            points += game.getMaxPoints();
        }
        return getPointsOf(participant) / points;
    }

    public List<Participant> getScoreBoard() {
        return participants.stream().sorted(Comparator.comparing(this::getPointsOf).reversed()).toList();
    }
}
