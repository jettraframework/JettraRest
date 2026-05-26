package com.jettra.rest.client;

import java.lang.reflect.Proxy;
import java.net.http.HttpClient;
import java.time.Duration;

public class RestClientBuilder {

    private int connectTimeout = 5000;
    private int readTimeout = 5000;

    private RestClientBuilder() {
    }

    public static RestClientBuilder newBuilder() {
        return new RestClientBuilder();
    }

    public RestClientBuilder connectTimeout(int millis) {
        this.connectTimeout = millis;
        return this;
    }

    public RestClientBuilder readTimeout(int millis) {
        this.readTimeout = millis;
        return this;
    }

    public Client build() {
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(connectTimeout))
                .build();
        return new Client(client, readTimeout);
    }

    @SuppressWarnings("unchecked")
    public static <T> T create(Class<T> clientInterface) {
        RestClient annotation = clientInterface.getAnnotation(RestClient.class);
        if (annotation == null) {
            throw new IllegalArgumentException("Interface must be annotated with @RestClient");
        }
        
        String baseUri = annotation.baseUri();
        String configBaseUri = com.jettra.server.config.JettraConfig.getProperty("baseUri");
        if (configBaseUri != null && !configBaseUri.isEmpty()) {
            // Si el baseUri en el config es completo y no hay override en la anotacion, se usa.
            // O si la anotacion es parcial (ej. "/authors"), se concatena.
            if (baseUri.isEmpty() || baseUri.startsWith("/")) {
                baseUri = configBaseUri + baseUri;
            } else {
                baseUri = configBaseUri;
            }
        } else if (baseUri.isEmpty()) {
            baseUri = "http://localhost:8080/api";
        }

        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();

        return (T) Proxy.newProxyInstance(
                clientInterface.getClassLoader(),
                new Class<?>[]{clientInterface},
                new RestClientProxy(httpClient, baseUri, 5000)
        );
    }
}
