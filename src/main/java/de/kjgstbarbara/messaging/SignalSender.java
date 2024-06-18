package de.kjgstbarbara.messaging;

import com.vaadin.flow.component.UI;
import de.kjgstbarbara.data.Person;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SignalSender {
    private static final Logger LOGGER = LogManager.getLogger(SignalSender.class);
    private long phoneNumber;

    public void sendMessage(String message, Person sendTo) {
        try {
            ProcessBuilder processBuilder = new ProcessBuilder();
            processBuilder.command("/bin/signal-cli", "-a", "+" + phoneNumber, "send", "-m", message, "+" + sendTo.getPhoneNumber().number());
            processBuilder.directory(new File(System.getProperty("user.home")));
            Process process = processBuilder.start();
            StreamGobbler streamGobbler =
                    new StreamGobbler(process.getInputStream(), LOGGER::info);
            streamGobbler.run();

            int exitCode = process.waitFor();
            if(exitCode != 0) {
                LOGGER.info("Die Nachricht konnte nicht via Signal an {} verschickt werden. Nachricht \n {}", sendTo.getName(), message);
            }
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public void register(UI ui, BiConsumer<UI, String> linkConsumer) {
        try {
            ProcessBuilder processBuilder = new ProcessBuilder();
            processBuilder.command("/bin/signal-cli", "link", "-n", "Chronos");
            processBuilder.directory(new File(System.getProperty("user.home")));
            Process process = processBuilder.start();
            StreamGobbler streamGobbler =
                    new StreamGobbler(process.getInputStream(), s -> linkConsumer.accept(ui, s));
            new Thread(streamGobbler).start();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void unregister() {
        try {
            ProcessBuilder processBuilder = new ProcessBuilder();
            processBuilder.command("/bin/signal-cli", "-a", "+" + phoneNumber, "unregister");
            processBuilder.directory(new File(System.getProperty("user.home")));
            Process process = processBuilder.start();
            StreamGobbler streamGobbler =
                    new StreamGobbler(process.getInputStream(), LOGGER::info);
            streamGobbler.run();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private record StreamGobbler(InputStream inputStream, Consumer<String> consumer) implements Runnable {

        @Override
            public void run() {
                new BufferedReader(new InputStreamReader(inputStream)).lines()
                        .forEach(consumer);
            }
        }
}