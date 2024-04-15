package de.kjgstbarbara;

import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.server.StreamResource;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;

public class FileHelper {
    public static void saveProfileImage(BufferedImage bufferedImage, String username) throws IOException {
        Path profileImagePath = getProfileImagePath(username);
        if (!Files.exists(profileImagePath.getParent())) {
            Files.createDirectories(profileImagePath.getParent());
        }
        if (Files.exists(profileImagePath)) {
            Files.delete(profileImagePath);
        }
        Files.write(profileImagePath, new byte[]{});
        ImageIO.write(bufferedImage, "png", profileImagePath.toFile());
    }

    public static Path getProfileImagePath(String username) {
        return Path.of(System.getenv("HOME")).resolve(".kjgtermine").resolve(username).resolve("profile-image.png");
    }

    public static StreamResource getProfileImage(String username) {
        return new StreamResource("profile-picture.png", () -> {
            try {
                return new FileInputStream(getProfileImagePath(username).toFile());
            } catch (FileNotFoundException e) {
                Notification.show(e.getLocalizedMessage()).addThemeVariants(NotificationVariant.LUMO_ERROR);// TODO
                return null;
            }
        });
    }
}
