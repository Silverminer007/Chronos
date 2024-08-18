package de.kjgstbarbara.chronos.messaging;

public class Messages {
    public static final String DATE_CANCELLED = """
            Hey #PERSON_FIRSTNAME,
            #DATE_TITLE am #DATE_START_DATE wurde gerade abgesagt.
            Weitere Infos findest du hier:
            #DATE_LINK
            """;

    public static final String DATE_POLL =
            """
                    Hey #PERSON_FIRSTNAME,
                    am #DATE_START_DATE um #DATE_START_TIME ist #DATE_TITLE. Bist du dabei?
                    Wenn ja klicke bitte hier:
                    #DATE_LINK/vote/1/#PERSON_ID
                    Wenn nicht klicke bitte hier:
                    #DATE_LINK/vote/2/#PERSON_ID
                    """;

    public static final String DATE_REMINDER =
            """
                    Hey #PERSON_FIRSTNAME,
                    du hast #DATE_TIME_UNTIL_START einen Termin bei der #ORGANISATION_NAME :)
                    #DATE_TITLE (#BOARD_TITLE)
                    Von #DATE_START_TIME am #DATE_START_DATE
                    Bis #DATE_END_TIME am #DATE_END_DATE
                                       \s
                    Deine Rückmeldung zu diesem Termin: #FEEDBACK_STATUS
                    Weitere Informationen zu diesem Termin findest du unter: #DATE_LINK""";
}
