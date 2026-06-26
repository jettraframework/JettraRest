package io.jettra.rest.client;

import io.jettra.rest.annotations.Context;
import io.jettra.rest.annotations.QueryParam;
import io.jettra.rest.annotations.PathParam;
import io.jettra.rest.annotations.DELETE;
import io.jettra.rest.annotations.PUT;
import io.jettra.rest.annotations.HeaderParam;
import io.jettra.rest.annotations.Path;
import io.jettra.rest.annotations.POST;
import com.google.gson.Gson;
import io.jettra.rest.core.Response;

import java.lang.reflect.*;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RestClientProxy implements InvocationHandler {

    private final HttpClient httpClient;
    private final String baseUri;
    private final int timeoutMillis;
    private final Gson gson = new Gson();

    public RestClientProxy(HttpClient httpClient, String baseUri, int timeoutMillis) {
        this.httpClient = httpClient;
        this.baseUri = baseUri.endsWith("/") ? baseUri.substring(0, baseUri.length() - 1) : baseUri;
        this.timeoutMillis = timeoutMillis;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        // Resolve method path
        String fullPath = baseUri;
        if (method.isAnnotationPresent(Path.class)) {
            String pathVal = method.getAnnotation(Path.class).value();
            if (!pathVal.startsWith("/")) {
                pathVal = "/" + pathVal;
            }
            fullPath += pathVal;
        } else if (method.getDeclaringClass().isAnnotationPresent(Path.class)) {
            String pathVal = method.getDeclaringClass().getAnnotation(Path.class).value();
            if (!pathVal.startsWith("/")) {
                pathVal = "/" + pathVal;
            }
            fullPath += pathVal;
        }

        // Determine HTTP method
        String httpMethod = "GET";
        if (method.isAnnotationPresent(POST.class)) {
            httpMethod = "POST";
        } else if (method.isAnnotationPresent(PUT.class)) {
            httpMethod = "PUT";
        } else if (method.isAnnotationPresent(DELETE.class)) {
            httpMethod = "DELETE";
        }

        // Extract parameters
        Parameter[] parameters = method.getParameters();
        Map<String, String> queryParams = new HashMap<>();
        Map<String, String> headerParams = new HashMap<>();
        Object bodyEntity = null;

        for (int i = 0; i < parameters.length; i++) {
            Parameter param = parameters[i];
            Object val = args[i];
            if (val == null) continue;

            if (param.isAnnotationPresent(PathParam.class)) {
                String name = param.getAnnotation(PathParam.class).value();
                fullPath = fullPath.replace("{" + name + "}", URLEncoder.encode(val.toString(), StandardCharsets.UTF_8));
            } else if (param.isAnnotationPresent(QueryParam.class)) {
                String name = param.getAnnotation(QueryParam.class).value();
                queryParams.put(name, val.toString());
            } else if (param.isAnnotationPresent(HeaderParam.class)) {
                String name = param.getAnnotation(HeaderParam.class).value();
                headerParams.put(name, val.toString());
            } else if (!param.isAnnotationPresent(Context.class)) {
                bodyEntity = val;
            }
        }

        // Build query string
        if (!queryParams.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            sb.append("?");
            for (Map.Entry<String, String> entry : queryParams.entrySet()) {
                if (sb.length() > 1) {
                    sb.append("&");
                }
                sb.append(URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8))
                  .append("=")
                  .append(URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8));
            }
            fullPath += sb.toString();
        }

        // Prepare HTTP Request Builder
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(fullPath))
                .timeout(Duration.ofMillis(timeoutMillis));

        // Add headers
        boolean hasAccept = false;
        boolean hasContentType = false;

        for (Map.Entry<String, String> header : headerParams.entrySet()) {
            builder.header(header.getKey(), header.getValue());
            if (header.getKey().equalsIgnoreCase("Accept")) hasAccept = true;
            if (header.getKey().equalsIgnoreCase("Content-Type")) hasContentType = true;
        }

        if (!hasAccept) {
            builder.header("Accept", "application/json");
        }

        // Set HTTP method & body publisher
        HttpRequest.BodyPublisher bodyPublisher;
        if (bodyEntity != null) {
            String json = gson.toJson(bodyEntity);
            bodyPublisher = HttpRequest.BodyPublishers.ofString(json);
            if (!hasContentType) {
                builder.header("Content-Type", "application/json");
            }
        } else {
            bodyPublisher = HttpRequest.BodyPublishers.noBody();
        }

        builder.method(httpMethod, bodyPublisher);

        // Send HTTP request
        HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());

        // Parse response
        Class<?> returnType = method.getReturnType();
        if (returnType == Response.class) {
            Response.Status status = Response.Status.INTERNAL_SERVER_ERROR;
            for (Response.Status s : Response.Status.values()) {
                if (s.getStatusCode() == response.statusCode()) {
                    status = s;
                    break;
                }
            }
            Response.Builder responseBuilder = Response.status(status).entity(response.body());
            // copy headers
            response.headers().map().forEach((k, v) -> {
                if (!v.isEmpty()) {
                    responseBuilder.header(k, v.get(0));
                }
            });
            return responseBuilder.build();
        }

        if (response.statusCode() >= 400) {
            throw new RuntimeException("HTTP Error: " + response.statusCode() + " - " + response.body());
        }

        if (returnType == void.class || returnType == Void.class) {
            return null;
        }

        // Deserialize to target return type
        Type genericReturnType = method.getGenericReturnType();
        return gson.fromJson(response.body(), genericReturnType);
    }
}
