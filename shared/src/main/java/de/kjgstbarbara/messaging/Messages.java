package de.kjgstbarbara.messaging;

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
                    #DATE_TITLE (#GROUP_NAME)
                    Von #DATE_START_TIME am #DATE_START_DATE
                    Bis #DATE_END_TIME am #DATE_END_DATE
                                       \s
                    Deine Rückmeldung zu diesem Termin: #FEEDBACK_STATUS
                    Weitere Informationen zu diesem Termin findest du unter: #DATE_LINK""";

    public static final String ORGANISATION_JOIN_REQUEST_ACCEPTED =
            """
                    Hi #PERSON_NAME,
                    deine Beitrittsanfrage zu #ORGANISATION_NAME wurde akzeptiert. Du kannst jetzt Mitglied von Gruppen dieser Organisation werden um Termine zu sehen und neue Gruppen erstellen
                    #BASE_URL/groups
                    """;

    public static final String ORGANISATION_JOIN_REQUEST_DECLINED =
            """
                    Hi #PERSON_NAME,
                    deine Beitrittsanfrage zu #ORGANISATION_NAME wurde abgelehnt
                    """;

    public static final String ORGANISATION_JOIN_REQUEST_NEW =
            """
                    Hi #ORGANISATION_ADMIN_NAME,
                    #PERSON_NAME möchte gerne deiner Organisation #ORGANISATION_NAME beitreten. Wenn du diese Anfrage bearbeiten möchtest, klicke bitte auf diesen Link
                    
                    Anfrage annehmen: #BASE_URL/organisation/manage/#ORGANISATION_ID/#PERSON_ID/yes
                    
                    Anfrage ablehnen: #BASE_URL/organisation/manage/#ORGANISATION_ID/#PERSON_ID/no
                    """;

    public static final String PERSON_RESET_PASSWORD =
            """
                    Hey #PERSON_NAME,
                    du hast angefragt dein Passwort zurückzusetzen.
                    Wenn du das nicht getan hast, kannst du diese Nachricht einfach ignorieren.
                    
                    Dein Einmalpasswort lautet: #PERSON_OTP
                    Dein Benutzername lautet: #PERSON_USERNAME
                    """;
}
