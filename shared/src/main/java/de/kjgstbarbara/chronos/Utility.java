package de.kjgstbarbara.chronos;

import java.net.URL;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

public class Utility {

    public static String baseURL(URL url) {
        StringBuilder urlBuilder = new StringBuilder();
        urlBuilder.append(url.getProtocol()).append("://");
        urlBuilder.append(url.getHost());
        if (url.getPort() != -1) {
            urlBuilder.append(":").append(url.getPort());
        }
        return urlBuilder.toString();
    }

    public static String generatePassword(int length) throws NoSuchAlgorithmException {
        final String chrs = "0123456789abcdefghijklmnopqrstuvwxyz-_ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        final SecureRandom secureRandom = SecureRandom.getInstanceStrong();
        return secureRandom
                .ints(length, 0, chrs.length())
                .mapToObj(chrs::charAt)
                .collect(StringBuilder::new, StringBuilder::append, StringBuilder::append)
                .toString();
    }
}
