package com.jettra.rest.client;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

public class GenericType<T> {
    private final Type type;

    protected GenericType() {
        Type superClass = getClass().getGenericSuperclass();
        if (superClass instanceof Class) {
            throw new RuntimeException("Missing type parameter.");
        }
        this.type = ((ParameterizedType) superClass).getActualTypeArguments()[0];
    }

    public Type getType() {
        return type;
    }
}
