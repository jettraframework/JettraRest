package com.jettra.rest.client;

import com.google.gson.Gson;
import com.jettra.rest.core.Response;

import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

public class InvocationBuilder {

    private final Client client;
    private final String uri;
    private final Map<String, String> headers = new HashMap<>();
    private final Gson gson = new Gson();

    public InvocationBuilder(Client client, String uri) {
        this.client = client;
        this.uri = uri;
    }

    public InvocationBuilder header(String name, String value) {
        if (value != null) {
            this.headers.put(name, value);
        }
        return this;
    }

    public Response get() {
        return execute("GET", null);
    }

    public <T> T get(Class<T> responseType) {
        Response response = get();
        return deserialize(response, responseType);
    }

    public <T> T get(GenericType<T> responseType) {
        Response response = get();
        return deserialize(response, responseType);
    }

    public Response post(Entity<?> entity) {
        return execute("POST", entity);
    }

    public <T> T post(Entity<?> entity, Class<T> responseType) {
        Response response = post(entity);
        return deserialize(response, responseType);
    }

    public Response put(Entity<?> entity) {
        return execute("PUT", entity);
    }

    public Response delete() {
        return execute("DELETE", null);
    }

    private Response execute(String method, Entity<?> entity) {
        try {
            HttpRequest.Builder reqBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(uri))
                    .timeout(Duration.ofMillis(client.getReadTimeoutMillis()));

            // Set Headers
            headers.forEach(reqBuilder::header);
            if (!headers.containsKey("Accept")) {
                reqBuilder.header("Accept", "application/json");
            }

            HttpRequest.BodyPublisher bodyPublisher;
            if (entity != null) {
                String json = gson.toJson(entity.getEntity());
                bodyPublisher = HttpRequest.BodyPublishers.ofString(json);
                if (!headers.containsKey("Content-Type")) {
                    reqBuilder.header("Content-Type", entity.getMediaType());
                }
            } else {
                bodyPublisher = HttpRequest.BodyPublishers.noBody();
            }

            reqBuilder.method(method, bodyPublisher);

            HttpResponse<String> httpResponse = client.getHttpClient().send(
                    reqBuilder.build(),
                    HttpResponse.BodyHandlers.ofString()
            );

            Response.Status status = Response.Status.INTERNAL_SERVER_ERROR;
            for (Response.Status s : Response.Status.values()) {
                if (s.getStatusCode() == httpResponse.statusCode()) {
                    status = s;
                    break;
                }
            }

            Response.Builder respBuilder = Response.status(status).entity(httpResponse.body());
            httpResponse.headers().map().forEach((k, v) -> {
                if (!v.isEmpty()) {
                    respBuilder.header(k, v.get(0));
                }
            });

            return respBuilder.build();

        } catch (Exception e) {
            throw new RuntimeException("Error executing REST client request on URI: " + uri, e);
        }
    }

    private <T> T deserialize(Response response, Class<T> responseType) {
        if (response.getStatus() >= 400) {
            throw new RuntimeException("HTTP Error " + response.getStatus() + ": " + response.getEntity());
        }
        String body = (String) response.getEntity();
        return gson.fromJson(body, responseType);
    }

    private <T> T deserialize(Response response, GenericType<T> responseType) {
        if (response.getStatus() >= 400) {
            throw new RuntimeException("HTTP Error " + response.getStatus() + ": " + response.getEntity());
        }
        String body = (String) response.getEntity();
        return gson.fromJson(body, responseType.getType());
    }
}
