package de.kjgstbarbara;

import com.vaadin.flow.server.StreamResource;
import de.kjgstbarbara.data.NoProfileImage;
import de.kjgstbarbara.data.Person;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;

public class FileHelper {
    public static void saveProfileImage(BufferedImage bufferedImage, Person person) throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        ImageIO.write(bufferedImage, "png", byteArrayOutputStream);
        person.setProfileImage(Base64.getEncoder().encodeToString(byteArrayOutputStream.toByteArray()));
    }

    public static StreamResource getProfileImage(Person person) {
        return new StreamResource("profile-picture.png", () ->
                new ByteArrayInputStream(Base64.getDecoder().decode(person.getProfileImage() == null ? NoProfileImage.load() : person.getProfileImage())));

    }
}
