package de.kjgstbarbara.data;

import com.google.common.base.Charsets;
import org.springframework.util.ResourceUtils;

import java.io.IOException;
import java.nio.file.Files;

public class NoProfileImage {
    private static String BASE64 = null;

    public static String load() {
        if (BASE64 == null) {
            try {
                BASE64 = Files.readString(ResourceUtils.getFile("classpath:noprofileimage.txt").toPath(), Charsets.UTF_8);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return BASE64;
    }
}
