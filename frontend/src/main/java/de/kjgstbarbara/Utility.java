package de.kjgstbarbara;

import java.net.URL;

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
}
