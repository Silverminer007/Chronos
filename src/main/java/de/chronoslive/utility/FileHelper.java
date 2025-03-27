package de.chronoslive.utility;

import de.chronoslive.entitys.Person;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class FileHelper {
    public static void saveProfileImage(BufferedImage bufferedImage, Person person) throws IOException {
        Path profileImagePath = Path.of("profile-image", person.getId() + ".png");
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        ImageIO.write(bufferedImage, "png", byteArrayOutputStream);
        Files.write(profileImagePath, byteArrayOutputStream.toByteArray());
    }
}