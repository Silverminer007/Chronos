package de.kjgstbarbara.components;

import com.vaadin.flow.component.ClientCallable;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinService;
import elemental.json.Json;
import elemental.json.JsonObject;
import elemental.json.JsonValue;
import lombok.Getter;

import javax.net.ssl.HttpsURLConnection;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Getter
@Tag("my-recaptcha")
public class ReCaptcha extends Component {

    private static final String SECRET_KEY = "6LdKLrMpAAAAAFvrOmgsRmtpeHAyAlnKv3McxonK";

    private static final String WEBSITE_KEY = "6LdKLrMpAAAAALdULe46Iy9dZ9LD0kXDftTr__Eg";

    private boolean valid;

    public ReCaptcha() {
        Element div = new Element("div");
        div.setAttribute("class", "g-recaptcha");
        div.setAttribute("data-sitekey", WEBSITE_KEY);
        div.setAttribute("data-callback", "myCallback"); // Note that myCallback must be declared in the global scope.
        getElement().appendChild(div);

        Element script = new Element("script");
        script.setAttribute("type", "text/javascript");
        script.setAttribute("src", "https://www.google.com/recaptcha/api.js?hl=en");
        getElement().appendChild(script);

        UI.getCurrent().getPage().executeJs("$0.init = function () {\n" +
                "    function myCallback(token) {\n" +
                "        $0.$server.callback(token);\n" +
                "    }\n" +
                "    window.myCallback = myCallback;" + // See myCallback comment above.
                "};\n" +
                "$0.init();\n", this);
    }

    @ClientCallable
    public void callback(String response) {
        try {
            valid = checkResponse(response);
        } catch (IOException | URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    private boolean checkResponse(String response) throws IOException, URISyntaxException {
        String remoteAddr = getRemoteAddr(VaadinService.getCurrentRequest());

        String url = "https://www.google.com/recaptcha/api/siteverify";

        String postData = "secret=" + URLEncoder.encode(SECRET_KEY, StandardCharsets.UTF_8) +
                "&remoteip=" + URLEncoder.encode(remoteAddr, StandardCharsets.UTF_8) +
                "&response=" + URLEncoder.encode(response, StandardCharsets.UTF_8);


        String result = doHttpPost(url, postData);

        System.out.println("Verify result:\n" + result);

        JsonObject parse = Json.parse(result);
        JsonValue jsonValue = parse.get("success");
        return jsonValue != null && jsonValue.asBoolean();
    }

    private static String getRemoteAddr(VaadinRequest request) {
        String ret = request.getHeader("x-forwarded-for");
        if (ret == null || ret.isEmpty()) {
            ret = request.getRemoteAddr();
        }
        return ret;
    }

    private static String doHttpPost(String urlStr, String postData) throws IOException, URISyntaxException {
        URL url = new URI(urlStr).toURL();
        HttpsURLConnection con = (HttpsURLConnection) url.openConnection();
        try {

            con.setRequestMethod("POST");
            con.setDoOutput(true);
            con.setDoInput(true);
            con.setReadTimeout(10_000);
            con.setConnectTimeout(10_000);
            con.setUseCaches(false);

            try (OutputStreamWriter writer = new OutputStreamWriter(con.getOutputStream(), StandardCharsets.UTF_8)) {
                writer.write(postData);
            }

            StringBuilder response = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(con.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line).append("\n");
                }
            }

            return response.toString();
        } finally {
            con.disconnect();
        }
    }
}