package de.chronoslive.mapper;

import de.chronoslive.dtos.GroupDto;
import de.chronoslive.entitys.Group;
import de.chronoslive.entitys.Organisation;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface GroupMapper {
    @Mapping(source = "organisation.id", target = "organisation_id")
    GroupDto toDto(Group group);

    @Mapping(source = "organisation_id", target = "organisation")
    Group toEntity(GroupDto groupDto);

    default Organisation map(Long id) {
        if (id == null) return null;
        return new Organisation(id);
    }
}