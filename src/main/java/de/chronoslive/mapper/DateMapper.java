package de.chronoslive.mapper;

import de.chronoslive.dtos.DateDto;
import de.chronoslive.entitys.Date;
import de.chronoslive.entitys.Group;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Mapper(componentModel = "spring")
public interface DateMapper {
    @Mapping(source = "notes", target = "description")
    @Mapping(source = "group.id", target = "group_id")
    @Mapping(source = "linkedTo", target = "linked_to")
    @Mapping(source = "dateCancelled", target = "status")
    @Mapping(source = "title", target = "name")
    DateDto toDto(Date date);

    @Mapping(target = "notes", source = "description")
    @Mapping(target = "group", source = "group_id")
    @Mapping(target = "linkedTo", source = "linked_to")
    @Mapping(target = "dateCancelled", source = "status")
    Date toEntity(DateDto dto);

    default LocalDateTime map(String isoDateTime) {
        return LocalDateTime.parse(isoDateTime);
    }

    default String map(LocalDateTime date) {
        return date.toString();
    }

    default String map(LocalDate date) {
        return date != null ? "CANCELED" : "PLANNED";
    }

    default LocalDate mapLocalDate(String status) {
        return "CANCELED".equalsIgnoreCase(status) ? null : LocalDate.now();
    }

    default Group map(long id) {
        return new Group(id);
    }
}