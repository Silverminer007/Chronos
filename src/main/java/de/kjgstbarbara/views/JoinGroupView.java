package de.kjgstbarbara.views;


import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.router.*;
import com.vaadin.flow.spring.security.AuthenticationContext;
import de.kjgstbarbara.data.Group;
import de.kjgstbarbara.data.Person;
import de.kjgstbarbara.service.GroupRepository;
import de.kjgstbarbara.service.GroupService;
import de.kjgstbarbara.service.PersonsRepository;
import de.kjgstbarbara.service.PersonsService;
import de.kjgstbarbara.views.nav.MainNavigationView;
import jakarta.annotation.security.PermitAll;
import org.springframework.security.core.userdetails.UserDetails;

@Route(value = "group/join/:groupID", layout = MainNavigationView.class)
@PermitAll
public class JoinGroupView extends Div implements BeforeEnterObserver {
    private final PersonsRepository personsRepository;
    private final GroupRepository groupRepository;

    private final Person person;

    public JoinGroupView(PersonsService personsService, GroupService groupService, AuthenticationContext authenticationContext) {
        this.personsRepository = personsService.getPersonsRepository();
        this.groupRepository = groupService.getGroupRepository();
        this.person = authenticationContext.getAuthenticatedUser(UserDetails.class)
                .flatMap(userDetails -> personsRepository.findByUsername(userDetails.getUsername()))
                .orElse(null);
        if (person == null) {
            authenticationContext.logout();
        }
    }

    @Override
    public void beforeEnter(BeforeEnterEvent beforeEnterEvent) {
        Group group = beforeEnterEvent.getRouteParameters().get("groupID").map(Long::valueOf).flatMap(groupRepository::findById).orElse(null);
        if (group != null) {
            if(!group.getRequests().contains(person)) {
                if (!group.getMembers().contains(person)) {
                    group.getRequests().add(person);
                    groupRepository.save(group);
                    Notification.show("Deine Beitrittsanfrage für " + group.getTitle() + " wurde verschickt").addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                } else {
                    Notification.show("Du gehörst schon zu dieser Gruppe: " + group.getTitle()).addThemeVariants(NotificationVariant.LUMO_WARNING);
                }
            } else {
                Notification.show("Deine Beitrittsanfrage zu dieser Gruppe läuft noch: " + group.getTitle());
            }
        }
        beforeEnterEvent.rerouteTo(GroupView.class);
    }
}