package de.chronoslive.mapper;

import de.chronoslive.dtos.OrganisationDto;
import de.chronoslive.entitys.Organisation;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface OrganisationMapper {

    OrganisationDto toDto(Organisation organisation);

    Organisation toEntity(OrganisationDto organisationDto);
}
