package de.kjgstbarbara.service;

import lombok.Getter;
import org.springframework.stereotype.Service;

@Service
@Getter
public class GroupService {
    private final GroupRepository groupRepository;

    public GroupService(GroupRepository groupRepository) {
        this.groupRepository = groupRepository;
    }
}
