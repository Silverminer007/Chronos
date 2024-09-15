package de.kjgstbarbara;

import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.server.AppShellSettings;
import com.vaadin.flow.server.PWA;
import com.vaadin.flow.theme.Theme;
import com.vaadin.flow.theme.lumo.Lumo;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * The entry point of the Spring Boot application.
 * Use the @PWA annotation make the application installable on phones, tablets
 * and some desktop browsers.
 *
 */
@PWA(
        name = "Chronos",
        shortName = "Chronos",
        offlinePath="offline.html",
        offlineResources = { "./images/offline.png"}
)
@SpringBootApplication
@Push
@Theme(value = "chronos", variant = Lumo.DARK)
public class Application implements AppShellConfigurator {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    @Override
    public void configurePage(AppShellSettings settings) {
        settings.addFavIcon("icon", "icons/favicon.ico", "16x16");
        settings.addLink("shortcut icon", "icons/favicon.ico");
    }
}
