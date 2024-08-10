package de.kjgstbarbara.chronos.messaging;

import de.kjgstbarbara.chronos.data.Person;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
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
            processBuilder.command("/usr/local/bin/signal-cli", "-a", "+" + phoneNumber, "send", "-m", message, "+" + sendTo.getPhoneNumber().number());
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

    public void register(Consumer<String> linkConsumer) {
        try {
            ProcessBuilder processBuilder = new ProcessBuilder();
            processBuilder.command("/usr/local/bin/signal-cli", "link", "-n", "Chronos");
            processBuilder.directory(new File(System.getProperty("user.home")));
            Process process = processBuilder.start();
            StreamGobbler streamGobbler =
                    new StreamGobbler(process.getInputStream(), linkConsumer);
            new Thread(streamGobbler).start();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void unregister() {
        try {
            ProcessBuilder processBuilder = new ProcessBuilder();
            processBuilder.command("/usr/local/bin/signal-cli", "-a", "+" + phoneNumber, "unregister");
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