package io.jettra.rest.client;

import java.net.http.HttpClient;

public class Client {

    private final HttpClient httpClient;
    private final int readTimeoutMillis;

    public Client(HttpClient httpClient, int readTimeoutMillis) {
        this.httpClient = httpClient;
        this.readTimeoutMillis = readTimeoutMillis;
    }

    public HttpClient getHttpClient() {
        return httpClient;
    }

    public int getReadTimeoutMillis() {
        return readTimeoutMillis;
    }

    public WebTarget target(String uri) {
        return new WebTarget(this, uri);
    }
}
