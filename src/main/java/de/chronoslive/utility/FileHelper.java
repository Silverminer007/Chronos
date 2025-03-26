package de.chronoslive.utility;

import com.vaadin.flow.server.StreamResource;
import de.chronoslive.entitys.Person;
import org.springframework.util.ResourceUtils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Base64;

public class FileHelper {
    public static void saveProfileImage(BufferedImage bufferedImage, Person person) throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        ImageIO.write(bufferedImage, "png", byteArrayOutputStream);
        person.setProfileImage(Base64.getEncoder().encodeToString(byteArrayOutputStream.toByteArray()));
    }

    public static StreamResource getProfileImage(Person person) {
        return new StreamResource("profile-picture.png", () ->
                new ByteArrayInputStream(Base64.getDecoder().decode(person.getProfileImage() == null ? FileHelper.loadDefaultProfileImage() : person.getProfileImage())));

    }

    private static String BASE64 = null;

    private static String loadDefaultProfileImage() {
        if (BASE64 == null) {
            try {
                BASE64 = Files.readString(ResourceUtils.getFile("classpath:noprofileimage.txt").toPath(), StandardCharsets.UTF_8);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return BASE64;
    }
}
