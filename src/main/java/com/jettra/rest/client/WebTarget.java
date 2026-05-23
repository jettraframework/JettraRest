package com.jettra.rest.client;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class WebTarget {

    private final Client client;
    private String uri;
    private final Map<String, String> queryParams = new HashMap<>();

    public WebTarget(Client client, String uri) {
        this.client = client;
        this.uri = uri;
    }

    public Client getClient() {
        return client;
    }

    public String getUri() {
        return uri;
    }

    public WebTarget path(String path) {
        if (path != null && !path.isEmpty()) {
            if (!this.uri.endsWith("/") && !path.startsWith("/")) {
                this.uri += "/";
            }
            this.uri += path;
        }
        return this;
    }

    public WebTarget resolveTemplate(String name, Object value) {
        if (value != null) {
            String encoded = URLEncoder.encode(value.toString(), StandardCharsets.UTF_8);
            this.uri = this.uri.replace("{" + name + "}", encoded);
        }
        return this;
    }

    public WebTarget queryParam(String name, Object value) {
        if (value != null) {
            this.queryParams.put(name, value.toString());
        }
        return this;
    }

    public InvocationBuilder request(String... acceptHeaders) {
        String finalUri = this.uri;
        if (!queryParams.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            sb.append("?");
            for (Map.Entry<String, String> entry : queryParams.entrySet()) {
                if (sb.length() > 1) sb.append("&");
                sb.append(URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8))
                  .append("=")
                  .append(URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8));
            }
            finalUri += sb.toString();
        }
        InvocationBuilder builder = new InvocationBuilder(client, finalUri);
        if (acceptHeaders.length > 0) {
            builder.header("Accept", String.join(", ", acceptHeaders));
        }
        return builder;
    }
}
