package com.jettra.rest.core;

import java.util.HashMap;
import java.util.Map;

public class Response {

    private final int status;
    private final Object entity;
    private final Map<String, String> headers;

    private Response(int status, Object entity, Map<String, String> headers) {
        this.status = status;
        this.entity = entity;
        this.headers = headers;
    }

    public int getStatus() {
        return status;
    }

    public Object getEntity() {
        return entity;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public static Builder status(int status) {
        return new Builder(status);
    }

    public static Builder status(Status status) {
        return new Builder(status.getStatusCode());
    }

    public static Builder ok() {
        return status(Status.OK);
    }

    public static Builder ok(Object entity) {
        return ok().entity(entity);
    }

    public static Builder created(Object entity) {
        return status(Status.CREATED).entity(entity);
    }

    public static Builder noContent() {
        return status(Status.NO_CONTENT);
    }

    public static class Builder {
        private final int status;
        private Object entity;
        private final Map<String, String> headers = new HashMap<>();

        public Builder(int status) {
            this.status = status;
        }

        public Builder entity(Object entity) {
            this.entity = entity;
            return this;
        }

        public Builder header(String name, String value) {
            this.headers.put(name, value);
            return this;
        }

        public Response build() {
            return new Response(status, entity, headers);
        }
    }

    public enum Status {
        OK(200),
        CREATED(201),
        ACCEPTED(202),
        NO_CONTENT(204),
        MOVED_PERMANENTLY(301),
        FOUND(302),
        BAD_REQUEST(400),
        UNAUTHORIZED(401),
        FORBIDDEN(403),
        NOT_FOUND(404),
        METHOD_NOT_ALLOWED(405),
        INTERNAL_SERVER_ERROR(500);

        private final int code;

        Status(int code) {
            this.code = code;
        }

        public int getStatusCode() {
            return code;
        }
    }
}
