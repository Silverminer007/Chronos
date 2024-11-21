package de.kjgstbarbara.components;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.datetimepicker.DateTimePicker;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.ValidationException;
import com.vaadin.flow.data.binder.ValidationResult;
import com.vaadin.flow.router.RouteParam;
import com.vaadin.flow.router.RouteParameters;
import de.kjgstbarbara.data.Date;
import de.kjgstbarbara.data.Group;
import de.kjgstbarbara.data.Organisation;
import de.kjgstbarbara.data.Person;
import de.kjgstbarbara.views.date.calendar.CalendarView;

import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

public class CreateDateDialog {
    private Consumer<Date> closeListener = d -> {};
    private Person person;
    private List<Organisation> organisations = new ArrayList<>();
    private List<Group> groups = new ArrayList<>();
    private LocalDate defaultDate = LocalDate.now();
    private Function<Organisation, Organisation> organisationSaver = o -> o;
    private Function<Group, Group> groupSaver = g -> g;
    private Function<Date, Date> dateSaver = d -> d;

    public CreateDateDialog() {
    }

    public CreateDateDialog(Consumer<Date> closeListener, Person person, List<Organisation> organisations, List<Group> groups, LocalDate defaultDate, Function<Organisation, Organisation> organisationSaver, Function<Group, Group> groupSaver, Function<Date, Date> dateSaver) {
        this.closeListener = closeListener;
        this.person = person;
        this.organisations = organisations;
        this.groups = groups;
        this.defaultDate = defaultDate;
        this.organisationSaver = organisationSaver;
        this.groupSaver = groupSaver;
        this.dateSaver = dateSaver;
    }

    public CreateDateDialog setCloseListener(Consumer<Date> closeListener) {
        this.closeListener = closeListener;
        return this;
    }

    public CreateDateDialog setPerson(Person person) {
        this.person = person;
        return this;
    }

    public CreateDateDialog setOrganisations(List<Organisation> organisations) {
        this.organisations = organisations;
        return this;
    }

    public CreateDateDialog setGroups(List<Group> groups) {
        this.groups = groups;
        return this;
    }

    public CreateDateDialog setDefaultDate(LocalDate defaultDate) {
        this.defaultDate = defaultDate;
        return this;
    }

    public CreateDateDialog setOrganisationSaver(Function<Organisation, Organisation> organisationSaver) {
        this.organisationSaver = organisationSaver;
        return this;
    }

    public CreateDateDialog setGroupSaver(Function<Group, Group> groupSaver) {
        this.groupSaver = groupSaver;
        return this;
    }

    public CreateDateDialog setDateSaver(Function<Date, Date> dateSaver) {
        this.dateSaver = dateSaver;
        return this;
    }

    public void create() {
        Objects.requireNonNull(closeListener, "closeListener must not be null");
        Objects.requireNonNull(person, "person must not be null");
        Objects.requireNonNull(organisations, "organisations must not be null");
        Objects.requireNonNull(groups, "groups must not be null");
        Objects.requireNonNull(dateSaver, "dateSaver must not be null");
        Objects.requireNonNull(organisationSaver, "organisationSaver must not be null");
        Objects.requireNonNull(groupSaver, "groupSaver must not be null");
        Objects.requireNonNull(dateSaver, "dateSaver must not be null");
        Objects.requireNonNull(organisations, "organisations must not be null");

        ClosableDialog dialog = new ClosableDialog();
        dialog.setTitle(new H3("Termin erstellen"));
        dialog.setMaxWidth("800px");

        Binder<Date> binder = new Binder<>();

        FormLayout content = new FormLayout();
        content.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("500px", 2)
        );

        TextField boardTitle = new TextField("Titel des Termins");
        boardTitle.setRequired(true);
        binder.forField(boardTitle).withValidator(new NonNullValidator<>()).bind(Date::getTitle, Date::setTitle);
        content.add(boardTitle);

        ComboBox<Group> selectGroup = new ComboBox<>();
        selectGroup.setLabel("Gruppe");
        selectGroup.setRequired(true);
        selectGroup.setAllowCustomValue(true);
        selectGroup.addCustomValueSetListener(event -> {
            Group group = new Group();
            group.setName(event.getDetail());
            group.getAdmins().add(person);
            group.getMembers().add(person);
            selectGroup.setValue(group);
        });
        binder.forField(selectGroup).withValidator(new NonNullValidator<>()).bind(Date::getGroup, Date::setGroup);
        selectGroup.setItems(groups);
        content.add(selectGroup);

        ComboBox<Organisation> selectOrganisation = new ComboBox<>();
        selectOrganisation.setLabel("Organisation");
        selectOrganisation.setVisible(false);
        selectOrganisation.setRequired(true);
        selectOrganisation.setAllowCustomValue(true);
        selectOrganisation.setItems(organisations);
        selectOrganisation.addCustomValueSetListener(event -> {
            Organisation organisation = new Organisation();
            organisation.setName(event.getDetail());
            organisation.setAdmin(person);
            organisation.getMembers().add(person);
            selectOrganisation.setValue(organisation);
        });
        content.add(selectOrganisation);
        content.setColspan(selectOrganisation, 2);

        selectGroup.addValueChangeListener(event -> selectOrganisation.setVisible(event.getValue().getOrganisation() == null));

        DateTimePicker startPicker = new DateTimePicker("Von");
        startPicker.setStep(Duration.of(30, ChronoUnit.MINUTES));
        startPicker.setRequiredIndicatorVisible(true);
        binder.forField(startPicker).withValidator(new NonNullValidator<>()).withConverter(new TimeZoneConverter(person.getTimezone())).bind(Date::getStart, Date::setStart);
        content.add(startPicker);

        DateTimePicker endPicker = new DateTimePicker("Bis");
        endPicker.setStep(Duration.of(30, ChronoUnit.MINUTES));
        endPicker.setRequiredIndicatorVisible(true);
        binder.forField(endPicker)
                .withValidator((s, valueContext) ->
                        s.isAfter(startPicker.getValue()) ?
                                ValidationResult.ok() :
                                ValidationResult.error("Das Ende einer Veranstaltung kann nicht vor dessen Beginn liegen"))
                .withConverter(new TimeZoneConverter(person.getTimezone()))
                .bind(Date::getEnd, Date::setEnd);
        startPicker.addValueChangeListener(event -> {
            if (event.getValue() != null && (endPicker.getValue() == null || endPicker.getValue().isBefore(event.getValue()))) {
                endPicker.setValue(event.getValue().plusHours(1));
            }
        });
        content.add(endPicker);

        TextField venue = new TextField("Veranstaltungsort");
        binder.forField(venue).bind(Date::getVenue, Date::setVenue);
        content.add(venue);

        TextArea notes = new TextArea("Notizen");
        binder.forField(notes).bind(Date::getNotes, Date::setNotes);
        content.add(notes);
        content.setColspan(notes, 2);

        ComboBox<Integer> selectRepetitionInterval = createRepetitionIntervalSelector();
        content.add(selectRepetitionInterval);

        DatePicker endOfSeries = new DatePicker("Ende der Terminserie");
        endOfSeries.setValue(LocalDate.now().plusYears(1));
        endOfSeries.setEnabled(false);
        content.add(endOfSeries);

        selectRepetitionInterval.addValueChangeListener(event -> endOfSeries.setEnabled(event.getValue() != 0));

        Checkbox publish = new Checkbox("Veröffentlichen");
        publish.setVisible(false);// TODO
        binder.forField(publish).bind(Date::isPublish, Date::setPublish);
        content.add(publish);

        dialog.add(content);

        Date date = new Date();
        date.setStart(defaultDate.atStartOfDay().withHour(19));
        date.setEnd(date.getStartAtTimezone(ZoneOffset.UTC).plusHours(1));

        binder.readBean(date);

        dialog.getFooter().add(new de.kjgstbarbara.components.DialogFooter(dialog::close, () -> {
            try {
                binder.writeBean(date);
                if (selectOrganisation.isVisible()) {
                    if (selectOrganisation.getValue() != null) {
                        if (date.getGroup().getOrganisation() == null) {
                            organisationSaver.apply(selectOrganisation.getValue());
                            date.getGroup().setOrganisation(selectOrganisation.getValue());
                        }
                    } else {
                        selectOrganisation.setInvalid(true);
                        selectOrganisation.setErrorMessage("Bitte wähle eine Organisation aus, zu der die neue Gruppe gehören soll");
                        return;
                    }
                }
                groupSaver.apply(date.getGroup());
                Date d = dateSaver.apply(date);
                if (selectRepetitionInterval.getValue() != 0) {
                    d.setRepetitionRule(selectRepetitionInterval.getValue());
                    d.setLinkedTo(d.getId());
                    d.setEndOfRepetition(endOfSeries.getValue());
                }
                dateSaver.apply(d);
                int countDates = 1;
                if (selectRepetitionInterval.getValue() != 0) {
                    Date dateCopy = new Date(d);
                    LocalDate localDate = d.getStartAtTimezone(ZoneOffset.UTC).toLocalDate().plusDays(selectRepetitionInterval.getValue());
                    while (localDate.isBefore(endOfSeries.getValue())) {
                        countDates++;
                        dateCopy.setStart(dateCopy.getStartAtTimezone(ZoneOffset.UTC).plusDays(selectRepetitionInterval.getValue()));
                        dateCopy.setEnd(dateCopy.getEndAtTimezone(ZoneOffset.UTC).plusDays(selectRepetitionInterval.getValue()));
                        dateCopy = new Date(dateCopy);
                        dateSaver.apply(dateCopy);
                        localDate = localDate.plusDays(selectRepetitionInterval.getValue());
                    }
                }
                UI.getCurrent().navigate(CalendarView.class, new RouteParameters(new RouteParam("page", 0)));
                if (selectRepetitionInterval.getValue() != null) {
                    Notification.show("Es wurden " + countDates + " Termine erstellt");
                } else {
                    Notification.show("\"" + date.getTitle() + "\" wurde erstellt");
                }
                closeListener.accept(date);
                dialog.close();
            } catch (ValidationException ignored) {
            }
        }, "Erstellen"));
        dialog.open();
    }

    private static ComboBox<Integer> createRepetitionIntervalSelector() {
        ComboBox<Integer> selectRepetitionInterval = new ComboBox<>();
        selectRepetitionInterval.setAllowCustomValue(true);
        selectRepetitionInterval.setLabel("Terminwiederholung");
        selectRepetitionInterval.setItems(0, 1, 2, 7, 14, 21);
        selectRepetitionInterval.setValue(0);
        selectRepetitionInterval.setItemLabelGenerator(value -> {
            if (value == 0) {
                return "Keine Wiederholung";
            } else if (value == 1) {
                return "jeden Tag";
            } else if (value == 7) {
                return "jede Woche";
            } else if (value == 14) {
                return "alle zwei Wochen";
            } else {
                return "alle " + value + " Tage";
            }
        });
        return selectRepetitionInterval;
    }
}
