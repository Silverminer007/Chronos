package de.chronoslive.migration.api;

import de.chronoslive.migration.dto.AddGroupParticipantDto;
import de.chronoslive.migration.dto.AppointmentDto;
import de.chronoslive.migration.dto.ChangeParticipationStatusDto;
import de.chronoslive.migration.dto.MessageDto;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.PostExchange;

public interface AppointmentClient {
    @PostExchange("/api/v2/admin/appointments")
    AppointmentDto createAppointment(@RequestBody AppointmentDto appointmentDto);

    @PostExchange("/api/v2/admin/appointments/{appointmentId}/participants/groups")
    void addGroupParticipant(@PathVariable Long appointmentId, @RequestBody AddGroupParticipantDto addGroupParticipantDto);

    @PostExchange("/api/v2/admin/appointments/participants/status")
    void changeParticipationStatus(@RequestBody ChangeParticipationStatusDto changeParticipationStatusDto);

    @PostExchange("/api/v2/admin/appointments/message")
    void sendMessage(@RequestBody MessageDto messageDto);
}
