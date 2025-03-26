import de.chronoslive.entitys.*;
import de.chronoslive.messaging.MessageScheduler;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

public class MessageSchedulerTests {
    @Test
    void testPollReminders() {
        LocalDateTime now = LocalDateTime.now();

        Person person = new Person();
        person.setFirstName("John");
        person.setLastName("Smith");
        person.setEMailAddress("john.smith@gmail.com");

        Organisation organisation = new Organisation();
        organisation.setAdmin(person);
        organisation.setName("John Smith");

        Group group = new Group();
        group.setName("John Smith");
        group.setOrganisation(organisation);
        group.getMembers().add(person);
        group.getAdmins().add(person);

        Date date = new Date();
        date.setStart(now);
        date.setEnd(now.plusHours(2));
        date.setTitle("Test Date");
        date.setGroup(group);

        PollReminder pollReminder = new PollReminder();
        pollReminder.setPollIntervalStart(now.minusDays(1));
        pollReminder.setPollIntervalEnd(now);
        pollReminder.setPollStarter(person);
        pollReminder.setDate(date);

        Assertions.assertFalse(MessageScheduler.checkSendReminder(pollReminder, now.minusHours(36)));

        Assertions.assertTrue(MessageScheduler.checkSendReminder(pollReminder, now.minusHours(24)));

        Assertions.assertTrue(MessageScheduler.checkSendReminder(pollReminder, now.minusHours(12)));

        Assertions.assertTrue(MessageScheduler.checkSendReminder(pollReminder, now.minusHours(6)));

        Assertions.assertTrue(MessageScheduler.checkSendReminder(pollReminder, now.minusHours(3)));

        Assertions.assertTrue(MessageScheduler.checkSendReminder(pollReminder, now.minusHours(1)));

        Assertions.assertFalse(MessageScheduler.checkSendReminder(pollReminder, now.minusHours(2)));

        Assertions.assertFalse(MessageScheduler.checkSendReminder(pollReminder, now.minusHours(23)));

        Assertions.assertFalse(MessageScheduler.checkSendReminder(pollReminder, now.plusHours(24)));


        // Testen was passiert, wenn der Erinnerungszeitraum schon vor Beginn des Termins endet
        date.setStart(now.plusDays(2));
        date.setEnd(now.plusDays(4));

        Assertions.assertTrue(MessageScheduler.checkSendReminder(pollReminder, now));

        Assertions.assertTrue(MessageScheduler.checkSendReminder(pollReminder, now.plusHours(1)));

        Assertions.assertTrue(MessageScheduler.checkSendReminder(pollReminder, now.plusHours(2)));

        Assertions.assertTrue(MessageScheduler.checkSendReminder(pollReminder, now.plusHours(3)));

        Assertions.assertTrue(MessageScheduler.checkSendReminder(pollReminder, now.plusHours(4)));

        Assertions.assertTrue(MessageScheduler.checkSendReminder(pollReminder, now.plusDays(1)));

        Assertions.assertFalse(MessageScheduler.checkSendReminder(pollReminder, now.plusDays(3)));
    }
}
