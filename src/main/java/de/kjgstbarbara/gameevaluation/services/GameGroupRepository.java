package de.kjgstbarbara.gameevaluation.services;

import de.kjgstbarbara.gameevaluation.data.GameGroup;
import de.kjgstbarbara.gameevaluation.data.Participant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface GameGroupRepository extends JpaRepository<GameGroup, Long>, JpaSpecificationExecutor<GameGroup> {
    List<GameGroup> findByParticipantsIn(List<Participant> participantList);
}