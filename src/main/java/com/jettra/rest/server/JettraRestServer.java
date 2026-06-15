package com.jettra.rest.server;

import com.google.gson.Gson;
import com.jettra.jwt.JettraJWT;
import com.jettra.server.JettraServer;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.jettra.rest.annotations.*;
import com.jettra.rest.core.Response;
import com.jettra.rest.security.SecurityContext;
import com.jettra.rest.security.UserPrincipal;

import java.io.InputStream;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JettraRestServer {

    private static final Gson gson = new Gson();
    private static JettraJWT jwtEngine;
    private static String basePath = "/api";
    private static String jwtSecret = "default_secret_key_jettra_rest_2026";
    private static long jwtExpiration = 3600000;
    private static String authHeaderName = "Authorization";
    private static String tokenPrefix = "Bearer ";

    static {
        // Load configurations
        try (InputStream is = JettraRestServer.class.getClassLoader().getResourceAsStream("jettra-rest.properties")) {
            Properties props = new Properties();
            if (is != null) {
                props.load(is);
                basePath = props.getProperty("jettra.rest.base-path", "/api");
                jwtSecret = props.getProperty("jettra.rest.security.jwt.secret", jwtSecret);
                jwtExpiration = Long.parseLong(props.getProperty("jettra.rest.security.jwt.expiration-millis", String.valueOf(jwtExpiration)));
                authHeaderName = props.getProperty("jettra.rest.security.header-name", "Authorization");
                tokenPrefix = props.getProperty("jettra.rest.security.token-prefix", "Bearer ");
            }
        } catch (Exception e) {
            System.err.println("[JettraRest] Could not load jettra-rest.properties. Using default settings.");
        }
        jwtEngine = new JettraJWT(jwtSecret, jwtExpiration);
    }

    private static class RestRoute {
        String httpMethod;
        Pattern pathPattern;
        List<String> pathParamNames;
        Method method;
        Object resourceInstance;
        boolean secured;
        List<String> rolesAllowed;
        boolean permitAll;

        public RestRoute(String httpMethod, Pattern pathPattern, List<String> pathParamNames, Method method, Object resourceInstance, boolean secured, List<String> rolesAllowed, boolean permitAll) {
            this.httpMethod = httpMethod;
            this.pathPattern = pathPattern;
            this.pathParamNames = pathParamNames;
            this.method = method;
            this.resourceInstance = resourceInstance;
            this.secured = secured;
            this.rolesAllowed = rolesAllowed;
            this.permitAll = permitAll;
        }
    }

    public static void register(JettraServer server, Class<?> resourceClass) {
        try {
            Object instance = resourceClass.getDeclaredConstructor().newInstance();
            register(server, instance);
        } catch (Exception e) {
            throw new RuntimeException("Could not register REST resource class: " + resourceClass.getName(), e);
        }
    }

    public static void register(JettraServer server, Object resource) {
        Class<?> clazz = resource.getClass();
        if (!clazz.isAnnotationPresent(Path.class)) {
            throw new IllegalArgumentException("Resource class must be annotated with @Path");
        }

        String classPath = clazz.getAnnotation(Path.class).value();
        if (!classPath.startsWith("/")) {
            classPath = "/" + classPath;
        }

        String fullBase = basePath + classPath;
        List<RestRoute> routes = new ArrayList<>();

        boolean classSecured = clazz.isAnnotationPresent(Secured.class);
        List<String> classRoles = clazz.isAnnotationPresent(RolesAllowed.class) ?
                Arrays.asList(clazz.getAnnotation(RolesAllowed.class).value()) : List.of();

        for (Method method : clazz.getDeclaredMethods()) {
            String httpMethod = null;
            if (method.isAnnotationPresent(GET.class)) httpMethod = "GET";
            else if (method.isAnnotationPresent(POST.class)) httpMethod = "POST";
            else if (method.isAnnotationPresent(PUT.class)) httpMethod = "PUT";
            else if (method.isAnnotationPresent(DELETE.class)) httpMethod = "DELETE";

            if (httpMethod == null) continue;

            String methodPath = method.isAnnotationPresent(Path.class) ? method.getAnnotation(Path.class).value() : "";
            if (!methodPath.startsWith("/") && !methodPath.isEmpty()) {
                methodPath = "/" + methodPath;
            }

            String fullMethodPath = fullBase + methodPath;
            String resolvedMethodPath = JettraServer.resolvePath(fullMethodPath);
            
            // Parse path parameters (e.g. /api/productos/{id} -> regex /api/productos/([^/]+))
            List<String> paramNames = new ArrayList<>();
            Matcher matcher = Pattern.compile("\\{([^}]+)\\}").matcher(resolvedMethodPath);
            StringBuilder regex = new StringBuilder();
            int lastEnd = 0;
            while (matcher.find()) {
                regex.append(Pattern.quote(resolvedMethodPath.substring(lastEnd, matcher.start())));
                regex.append("([^/]+)");
                paramNames.add(matcher.group(1));
                lastEnd = matcher.end();
            }
            regex.append(Pattern.quote(resolvedMethodPath.substring(lastEnd)));
            Pattern pattern = Pattern.compile(regex.toString() + "/?");

            boolean methodSecured = classSecured || method.isAnnotationPresent(Secured.class);
            List<String> methodRoles = method.isAnnotationPresent(RolesAllowed.class) ?
                    Arrays.asList(method.getAnnotation(RolesAllowed.class).value()) : classRoles;
            boolean permitAll = method.isAnnotationPresent(PermitAll.class);

            routes.add(new RestRoute(httpMethod, pattern, paramNames, method, resource, methodSecured, methodRoles, permitAll));
        }

        // Register HttpHandler under base path in JettraServer
        server.addHandler(fullBase, new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) {
                try {
                    String requestPath = exchange.getRequestURI().getPath();
                    String requestMethod = exchange.getRequestMethod();
                    System.out.println("[JettraRestServer] Processing request: " + requestMethod + " " + requestPath);

                    RestRoute matchedRoute = null;
                    Matcher routeMatcher = null;

                    for (RestRoute route : routes) {
                        if (route.httpMethod.equalsIgnoreCase(requestMethod)) {
                            Matcher m = route.pathPattern.matcher(requestPath);
                            if (m.matches()) {
                                matchedRoute = route;
                                routeMatcher = m;
                                break;
                            }
                        }
                    }

                    if (matchedRoute == null) {
                        sendError(exchange, 404, "Endpoint not found");
                        return;
                    }

                    // Security check
                    SecurityContext securityContext = null;
                    if (matchedRoute.secured && !matchedRoute.permitAll) {
                        String authHeader = exchange.getRequestHeaders().getFirst(authHeaderName);
                        if (authHeader == null || !authHeader.startsWith(tokenPrefix)) {
                            sendError(exchange, 401, "Missing or invalid authorization header");
                            return;
                        }

                        String token = authHeader.substring(tokenPrefix.length()).trim();
                        String username = null;
                        try {
                            username = jwtEngine.extractUsername(token);
                        } catch (Exception e) {
                            sendError(exchange, 401, "Malformed token");
                            return;
                        }

                        if (username == null || !jwtEngine.isTokenValid(token, username)) {
                            sendError(exchange, 401, "Expired or invalid token");
                            return;
                        }

                        // Try extracting roles or claims from JWT if supported by JWT payload
                        // For simplicity, we can mock/fetch roles based on username
                        Set<String> roles = new HashSet<>();
                        if ("admin".equalsIgnoreCase(username)) {
                            roles.add("ADMIN");
                        } else {
                            roles.add("USER");
                        }

                        securityContext = new SecurityContext(new UserPrincipal(username), roles, true, "JWT");

                        // Role authorization
                        if (!matchedRoute.rolesAllowed.isEmpty()) {
                            boolean hasRole = false;
                            for (String role : matchedRoute.rolesAllowed) {
                                if (securityContext.isUserInRole(role)) {
                                    hasRole = true;
                                    break;
                                }
                            }
                            if (!hasRole) {
                                sendError(exchange, 403, "Access forbidden for this role");
                                return;
                            }
                        }
                    }

                    // Invoke method and inject parameters
                    Parameter[] methodParameters = matchedRoute.method.getParameters();
                    Object[] invokeArgs = new Object[methodParameters.length];

                    // Extract query parameters
                    Map<String, String> queryParams = parseQuery(exchange.getRequestURI().getQuery());

                    for (int i = 0; i < methodParameters.length; i++) {
                        Parameter param = methodParameters[i];
                        if (param.isAnnotationPresent(PathParam.class)) {
                            String name = param.getAnnotation(PathParam.class).value();
                            int idx = matchedRoute.pathParamNames.indexOf(name);
                            if (idx != -1) {
                                String val = routeMatcher.group(idx + 1);
                                invokeArgs[i] = convertValue(val, param.getType());
                            }
                        } else if (param.isAnnotationPresent(QueryParam.class)) {
                            String name = param.getAnnotation(QueryParam.class).value();
                            String val = queryParams.get(name);
                            invokeArgs[i] = convertValue(val, param.getType());
                        } else if (param.isAnnotationPresent(Context.class) && param.getType() == SecurityContext.class) {
                            invokeArgs[i] = securityContext;
                        } else {
                            // Extract request body
                            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                            if (!body.isBlank()) {
                                invokeArgs[i] = gson.fromJson(body, param.getType());
                            } else {
                                invokeArgs[i] = null;
                            }
                        }
                    }

                    // Execute method (using Virtual Thread if JettraServer is using Virtual Thread Executor)
                    Object result = matchedRoute.method.invoke(matchedRoute.resourceInstance, invokeArgs);

                    // Send response
                    if (result instanceof Response) {
                        Response resp = (Response) result;
                        byte[] bodyBytes = new byte[0];
                        if (resp.getEntity() != null) {
                            String json = resp.getEntity() instanceof String ? (String) resp.getEntity() : gson.toJson(resp.getEntity());
                            bodyBytes = json.getBytes(StandardCharsets.UTF_8);
                        }
                        
                        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
                        resp.getHeaders().forEach((k, v) -> exchange.getResponseHeaders().set(k, v));
                        exchange.sendResponseHeaders(resp.getStatus(), bodyBytes.length > 0 ? bodyBytes.length : -1);
                        if (bodyBytes.length > 0) {
                            try (OutputStream os = exchange.getResponseBody()) {
                                os.write(bodyBytes);
                            }
                        }
                    } else {
                        byte[] bodyBytes = new byte[0];
                        if (result != null) {
                            String json = gson.toJson(result);
                            bodyBytes = json.getBytes(StandardCharsets.UTF_8);
                        }
                        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
                        exchange.sendResponseHeaders(200, bodyBytes.length > 0 ? bodyBytes.length : -1);
                        if (bodyBytes.length > 0) {
                            try (OutputStream os = exchange.getResponseBody()) {
                                os.write(bodyBytes);
                            }
                        }
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                    try {
                        sendError(exchange, 500, "Internal Server Error: " + e.getMessage());
                    } catch (Exception ex) {
                        // ignore
                    }
                }
            }
        });
    }

    private static Map<String, String> parseQuery(String query) {
        Map<String, String> params = new HashMap<>();
        if (query == null || query.isBlank()) return params;
        for (String param : query.split("&")) {
            String[] pair = param.split("=");
            if (pair.length > 1) {
                params.put(pair[0], pair[1]);
            } else if (pair.length > 0) {
                params.put(pair[0], "");
            }
        }
        return params;
    }

    private static Object convertValue(String val, Class<?> type) {
        if (val == null) return null;
        if (type == String.class) return val;
        if (type == Integer.class || type == int.class) return Integer.parseInt(val);
        if (type == Long.class || type == long.class) return Long.parseLong(val);
        if (type == Boolean.class || type == boolean.class) return Boolean.parseBoolean(val);
        return val;
    }

    private static void sendError(HttpExchange exchange, int status, String message) throws java.io.IOException {
        String json = "{\"error\": \"" + message + "\"}";
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }
}
