package de.kjgstbarbara.messaging;

import com.vaadin.flow.server.StreamResource;
import de.kjgstbarbara.data.*;
import de.kjgstbarbara.service.ConfigService;
import de.kjgstbarbara.service.DatesService;
import de.kjgstbarbara.service.FeedbackService;
import de.kjgstbarbara.service.PersonsService;
import it.auties.whatsapp.api.PairingCodeHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.function.Consumer;

@Component
public class SenderUtils {
    private static final Logger LOGGER = LoggerFactory.getLogger(SenderUtils.class);

    @Autowired
    private WhatsAppMessageSender whatsAppMessageSender;
    @Autowired
    private EMailMessageSender eMailMessageSender;
    @Autowired
    private ConfigService configService;
    @Autowired
    private PersonsService personsService;
    @Autowired
    private DatesService datesService;
    @Autowired
    private FeedbackService feedbackService;

    public void reSetupWhatsApp(boolean qrCode) {
        whatsAppMessageSender.setup(qrCode);
    }

    public void setWhatsAppPairingCodeHandler(PairingCodeHandler pairingCodeHandler) {
        whatsAppMessageSender.setPairingCodeHandler(pairingCodeHandler);
    }

    public void setWhatsAppQrCodeHandler(Consumer<StreamResource> qrCodeHandler) {
        whatsAppMessageSender.setQrCodeHandler(qrCodeHandler);
    }

    public boolean sendMessage(String message, Person sendTo, Organisation organisation) {
        try {
            switch (sendTo.getReminder()) {
                case EMAIL -> eMailMessageSender.sendMessage(message, sendTo);
                case WHATSAPP -> whatsAppMessageSender.sendMessage(message, sendTo);
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    // Protokoll hochladen

    // Protokoll abfragen
}
