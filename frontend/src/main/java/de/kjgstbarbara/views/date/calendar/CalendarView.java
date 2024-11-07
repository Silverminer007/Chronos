package de.kjgstbarbara.views.date.calendar;

import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.*;
import com.vaadin.flow.theme.lumo.LumoIcon;
import com.vaadin.flow.theme.lumo.LumoUtility;
import de.kjgstbarbara.Utility;
import de.kjgstbarbara.components.*;
import de.kjgstbarbara.components.Header;
import de.kjgstbarbara.data.*;
import de.kjgstbarbara.service.*;
import de.kjgstbarbara.views.MainNavigationView;
import jakarta.annotation.security.PermitAll;

@Route(value = "calendar/:page?/:search?", layout = MainNavigationView.class)
@RouteAlias(value = ":page?/:search?", layout = MainNavigationView.class)
@PageTitle("Meine Termine")
@PermitAll
public class CalendarView extends VerticalLayout implements BeforeEnterObserver {

    private final OrganisationRepository organisationRepository;
    private final DateRepository dateRepository;
    private final GroupRepository groupRepository;

    private final Calendar calendarView;

    private final Person person;

    private int page = 0;
    private String search = "";

    public CalendarView(PersonsService personsService, OrganisationService organisationService, DatesService datesService, GroupService groupService, FeedbackService feedbackService) {
        this.organisationRepository = organisationService.getOrganisationRepository();
        this.dateRepository = datesService.getDateRepository();
        this.groupRepository = groupService.getGroupRepository();
        FeedbackRepository feedbackRepository = feedbackService.getFeedbackRepository();
        this.person = Utility.getAuthenticatedUser(personsService.getPersonsRepository()).orElse(null);
        this.calendarView = this.person == null ? null :
                switch (this.person.getCalendarLayout()) {
                    case MONTH ->
                            new CalendarPageView.Month(dateRepository, groupRepository, organisationRepository, this.person);
                    case YEAR ->
                            new CalendarPageView.Year(dateRepository, groupRepository, organisationRepository, this.person);
                    case LIST_NEXT -> new CalendarListView.Amount(dateRepository, feedbackRepository, this.person);
                    default -> new CalendarListView.Month(dateRepository, feedbackRepository, this.person);
                };
        this.setPadding(false);
        this.setSpacing(false);
        this.setSizeFull();
    }

    private HorizontalLayout createHeaderBar() {
        HorizontalLayout header = new Header();

        H4 title = new H4(this.calendarView.getTitle(this.page, this.person.getUserLocale()));
        header.add(title);

        header.add(new Search(searchString -> this.search = searchString, this.search));
        return header;
    }

    private HorizontalLayout createFooter() {
        HorizontalLayout footer = new HorizontalLayout();
        footer.setSpacing(true);
        footer.setPadding(true);
        footer.addClassNames(LumoUtility.Width.FULL,
                LumoUtility.JustifyContent.BETWEEN,
                LumoUtility.AlignSelf.STRETCH);
        footer.setAlignItems(Alignment.CENTER);

        Button previousButton = new Button(LumoIcon.ARROW_LEFT.create());
        previousButton.addThemeVariants(ButtonVariant.LUMO_LARGE, ButtonVariant.LUMO_CONTRAST);
        previousButton.addClickListener(event -> this.previous());
        previousButton.addClickShortcut(Key.ARROW_LEFT);
        footer.add(previousButton);

        Button createButton = new Button("Erstellen", VaadinIcon.PLUS.create());
        createButton.setTooltipText("Einen neuen Termin erstellen");
        createButton.setAriaLabel("Neuer Termin");
        createButton.addThemeVariants(ButtonVariant.LUMO_CONTRAST, ButtonVariant.LUMO_LARGE);
        createButton.addClickListener(event ->
                new CreateDateDialog()
                        .setPerson(person)
                        .setOrganisations(this.organisationRepository.findByMembersIn(this.person))
                        .setGroups(this.groupRepository.findByAdminsIn(this.person))
                        .setGroupSaver(this.groupRepository::save)
                        .setDateSaver(this.dateRepository::save)
                        .setOrganisationSaver(this.organisationRepository::save)
                        .create());
        footer.add(createButton);

        Button nextButton = new Button(LumoIcon.ARROW_RIGHT.create());
        nextButton.addThemeVariants(ButtonVariant.LUMO_LARGE, ButtonVariant.LUMO_CONTRAST);
        nextButton.addClickShortcut(Key.ARROW_RIGHT);
        nextButton.addClickListener(event -> this.next());
        footer.add(nextButton);
        return footer;
    }

    private void next() {
        UI.getCurrent().navigate(CalendarView.class,
                new RouteParameters(new RouteParam("page", this.page + 1)));
    }

    private void previous() {
        UI.getCurrent().navigate(CalendarView.class,
                new RouteParameters(new RouteParam("page", this.page - 1)));
    }

    @Override
    public void beforeEnter(BeforeEnterEvent beforeEnterEvent) {
        if (person == null) {
            return;
        }
        this.page = beforeEnterEvent.getRouteParameters().get("page").map(Integer::parseInt).orElse(0);
        this.search = beforeEnterEvent.getRouteParameters().get("search").orElse("");

        this.removeAll();

        this.add(createHeaderBar());
        this.add(this.calendarView.getCalendarPage(this.page, this.search));
        this.add(createFooter());
    }
}