package de.kjgstbarbara;

import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.server.StreamResource;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

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
        return Path.of(System.getenv("HOME")).resolve(".kjgtermine").resolve("profile-image").resolve(username + ".png");
    }

    public static Optional<StreamResource> getProfileImage(String username) {
        try (FileInputStream fileInputStream = new FileInputStream(getProfileImagePath(username).toFile())) {
            return Optional.of(new StreamResource("profile-picture.png", () -> fileInputStream));
        } catch (IOException e) {
            return Optional.empty();
        }

    }
}
