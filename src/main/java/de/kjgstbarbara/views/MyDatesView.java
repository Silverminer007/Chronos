package de.kjgstbarbara.views;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.renderer.LocalDateTimeRenderer;
import com.vaadin.flow.router.*;
import de.kjgstbarbara.data.Board;
import de.kjgstbarbara.data.Date;
import de.kjgstbarbara.data.Organisation;
import de.kjgstbarbara.data.Person;
import de.kjgstbarbara.service.*;
import de.kjgstbarbara.views.nav.MyDatesNavigationView;
import jakarta.annotation.security.PermitAll;

import java.util.ArrayList;
import java.util.List;

@Route(value = "mydates/:person", layout = MyDatesNavigationView.class)
@PermitAll
public class MyDatesView extends VerticalLayout implements BeforeEnterObserver, AfterNavigationObserver {
    private final PersonsRepository personsRepository;
    private final BoardsRepository boardsRepository;
    private final DateRepository dateRepository;

    private Organisation organisation;
    private Person person;
    private final Grid<Date> grid = new Grid<>(Date.class, false);

    public MyDatesView(PersonsService personsService, BoardsService boardsService, DatesService datesService) {
        this.personsRepository = personsService.getPersonsRepository();
        this.dateRepository = datesService.getDateRepository();
        this.boardsRepository = boardsService.getBoardsRepository();

        grid.addClassName("styling");
        grid.addColumn("title").setHeader("Termin")
                .setAutoWidth(true).setResizable(true)
                .setPartNameGenerator(date->"font-weight-bold");
        grid.addColumn("start")
                .setRenderer(new LocalDateTimeRenderer<>(Date::getStart, "dd.MM.YYYY HH:mm"))
                .setHeader("Datum").setAutoWidth(true).setResizable(true);
        grid.addComponentColumn(person ->
                createStatusIcon(person.isInternal())).setHeader("Intern").setFlexGrow(0);
        grid.addComponentColumn(date -> {
            HorizontalLayout horizontalLayout = new HorizontalLayout();
            horizontalLayout.setSpacing(false);
            Button in = new Button("Zusagen");
            in.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SUCCESS);
            in.setEnabled(!date.getIn().contains(person));
            Button out = new Button("Absagen");
            out.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_ERROR);
            out.setEnabled(!date.getOut().contains(person));
            Button dontknow = new Button("Weiß nicht");
            dontknow.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_CONTRAST);
            dontknow.setEnabled(!date.getDontknow().contains(person));
            in.addClickListener(event -> {
                in.setEnabled(false);
                out.setEnabled(true);
                dontknow.setEnabled(true);
                date.getIn().add(person);
                date.getOut().remove(person);
                date.getDontknow().remove(person);
                dateRepository.save(date);
            });
            out.addClickListener(event -> {
                in.setEnabled(true);
                out.setEnabled(false);
                dontknow.setEnabled(true);
                date.getIn().remove(person);
                date.getOut().add(person);
                date.getDontknow().remove(person);
                dateRepository.save(date);
            });
            dontknow.addClickListener(event -> {
                in.setEnabled(true);
                out.setEnabled(true);
                dontknow.setEnabled(false);
                date.getIn().remove(person);
                date.getOut().remove(person);
                date.getDontknow().add(person);
                dateRepository.save(date);
            });
            horizontalLayout.add(in, out, dontknow);
            return horizontalLayout;
        }).setAutoWidth(true);
        grid.addThemeVariants(GridVariant.LUMO_NO_BORDER, GridVariant.LUMO_ROW_STRIPES);
        grid.setPartNameGenerator(date -> {
            if(date.getIn().contains(person)) {
                return "green";
            }
            if(date.getOut().contains(person)) {
                return "red";
            }
            if(date.getDontknow().contains(person)) {
                return "yellow";
            }
            return null;
        });
        add(grid);
    }

    private List<Date> getDates() {
        List<Date> dates = new ArrayList<>();
        boardsRepository.findByOrg(organisation).stream().filter(board -> board.getMembers().contains(person)).map(Board::getDateList).forEach(dates::addAll);
        return dates.stream().sorted().toList();
    }

    @Override
    public void beforeEnter(BeforeEnterEvent beforeEnterEvent) {
        person = beforeEnterEvent.getRouteParameters().get("person").map(Long::valueOf).flatMap(personsRepository::findById).orElse(null);
        if (person == null) {
            beforeEnterEvent.forwardTo(OrganisationsView.class);
        } else {
            organisation = person.getOrganisation();
        }
    }

    private Icon createStatusIcon(boolean status) {
        Icon icon;
        if (status) {
            icon = VaadinIcon.CHECK.create();
            icon.getElement().getThemeList().add("badge success");
        } else {
            icon = VaadinIcon.CLOSE_SMALL.create();
            icon.getElement().getThemeList().add("badge error");
        }
        icon.getStyle().set("padding", "var(--lumo-space-xs");
        return icon;
    }

    @Override
    public void afterNavigation(AfterNavigationEvent afterNavigationEvent) {
        grid.setItems(getDates());
    }
}