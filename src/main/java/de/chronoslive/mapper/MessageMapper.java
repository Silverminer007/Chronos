package de.chronoslive.mapper;

import de.chronoslive.dtos.MessageDto;
import de.chronoslive.entitys.Date;
import de.chronoslive.entitys.Person;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.time.LocalDateTime;

@Mapper(componentModel = "spring")
public interface MessageMapper {
    @Mapping(source = "informationSender.id", target = "sender_id")
    @Mapping(source = "informationText", target = "value")
    @Mapping(source = "informationTime", target = "timeStamp")
    MessageDto toDto(Date.Information information);

    @Mapping(target = "informationSender", source = "sender_id")
    @Mapping(target = "informationText", source = "value")
    @Mapping(target = "informationTime", source = "timeStamp")
    Date.Information toEntity(MessageDto messageDto);

    default LocalDateTime map(String isoDateTime) {
        return LocalDateTime.parse(isoDateTime);
    }

    default String map(LocalDateTime date) {
        return date.toString();
    }

    default Person map(long id) {
        return new Person(id);
    }
}