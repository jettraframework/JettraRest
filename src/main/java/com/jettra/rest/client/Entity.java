package com.jettra.rest.client;

public class Entity<T> {
    private final T entity;
    private final String mediaType;

    private Entity(T entity, String mediaType) {
        this.entity = entity;
        this.mediaType = mediaType;
    }

    public T getEntity() {
        return entity;
    }

    public String getMediaType() {
        return mediaType;
    }

    public static <T> Entity<T> json(T entity) {
        return new Entity<>(entity, "application/json");
    }
}
