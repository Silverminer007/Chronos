package de.kjgstbarbara.chronos.services;

import de.kjgstbarbara.chronos.data.GameGroup;
import de.kjgstbarbara.chronos.data.Participant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface GameGroupRepository extends JpaRepository<GameGroup, Long>, JpaSpecificationExecutor<GameGroup> {
    List<GameGroup> findByParticipantsIn(List<Participant> participantList);
}