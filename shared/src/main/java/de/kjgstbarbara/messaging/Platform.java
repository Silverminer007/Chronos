package de.kjgstbarbara.messaging;

import de.kjgstbarbara.Result;
import de.kjgstbarbara.data.Person;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.function.BiFunction;

public enum Platform {
    EMAIL((message, sendTo) -> {
        RestClient restClient = RestClient.create("http://email:8080/send");
        try {
            restClient.put()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new Email(sendTo.getEMailAddress(), "Chronos Message", message))
                    .header("Content-Type", "application/json")
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve().toBodilessEntity();
            return Result.success();
        } catch (RestClientException e) {
            return Result.error("Die Nachricht konnte nicht an " + sendTo.getEMailAddress() + " verschickt werden");
        }
    });

    private static final Logger LOGGER = LogManager.getLogger(Platform.class);

    private final BiFunction<String, Person, Result> send;

    Platform(BiFunction<String, Person, Result> send) {
        this.send = send;
    }

    public Result send(Person person, String message) {
        return this.send.apply(message, person);
    }

    private record Email(String to, String subject, String message) {
    }
}
