package de.kjgstbarbara.chronos.whatsapp;

import it.auties.whatsapp.api.Whatsapp;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class WhatsAppInstances {
    private static final Map<Long, Whatsapp> WHATSAPPS = Collections.synchronizedMap(new HashMap<>());

    public static Whatsapp getWhatsApp(long organisationID) {
        if (!WHATSAPPS.containsKey(organisationID)) {
            Whatsapp whatsapp = Whatsapp.webBuilder().newConnection(organisationID).unregistered(qrcode -> {});
            WHATSAPPS.put(organisationID, whatsapp);
        }
        return WHATSAPPS.get(organisationID);
    }
}
