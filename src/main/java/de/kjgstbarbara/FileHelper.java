package de.kjgstbarbara;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class FileHelper {
    public static void saveProfileImage(BufferedImage bufferedImage, String username) throws IOException {
        Path baseDirectory = Path.of("");
        Path personalDirectory = baseDirectory.resolve(username);
        Path profileImagePath = personalDirectory.resolve("profile-image.jpeg");
        if (!Files.exists(personalDirectory))
            Files.createDirectories(personalDirectory);
        if(!Files.exists(profileImagePath))
            Files.write(profileImagePath, new byte[]{});
        ImageIO.write(bufferedImage, "jpeg", profileImagePath.toFile());
    }

    public static String getProfileImagePath(String username) {
        return username + "/profile-image.jpeg";
    }
}
