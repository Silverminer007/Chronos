package de.kjgstbarbara;

import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.server.PWA;
import com.vaadin.flow.theme.Theme;
import it.auties.whatsapp.api.PairingCodeHandler;
import it.auties.whatsapp.api.Whatsapp;
import it.auties.whatsapp.model.message.standard.PollUpdateMessage;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * The entry point of the Spring Boot application.
 *
 * Use the @PWA annotation make the application installable on phones, tablets
 * and some desktop browsers.
 *
 */
@SpringBootApplication
@Theme(value = "gruppentool")
public class Application implements AppShellConfigurator {

    public static void main(String[] args) {
        WhatsAppUtils.init();
        SpringApplication.run(Application.class, args);
    }

}
