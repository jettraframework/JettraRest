package io.jettra.rest.util;

import io.jettra.jwt.JettraJson;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;

public class RestJson {

    public static <T> T fromJson(String json, Class<T> clazz) {
        if (json == null || json.trim().isEmpty()) return null;
        try {
            Map<String, Object> map = JettraJson.parse(json);
            T instance = clazz.getDeclaredConstructor().newInstance();
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                try {
                    Field field = clazz.getDeclaredField(entry.getKey());
                    field.setAccessible(true);
                    Object value = entry.getValue();
                    if (value != null && !field.getType().isAssignableFrom(value.getClass())) {
                        if (field.getType() == String.class) {
                            value = value.toString();
                        } else if (field.getType() == int.class || field.getType() == Integer.class) {
                            value = Integer.parseInt(value.toString());
                        } else if (field.getType() == long.class || field.getType() == Long.class) {
                            value = Long.parseLong(value.toString());
                        } else if (field.getType() == boolean.class || field.getType() == Boolean.class) {
                            value = Boolean.parseBoolean(value.toString());
                        }
                    }
                    field.set(instance, value);
                } catch (NoSuchFieldException e) {
                    // ignore unknown fields
                }
            }
            return instance;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static String toJson(Object obj) {
        if (obj == null) return "null";
        if (obj instanceof String) return "\"" + obj + "\"";
        if (obj instanceof Number || obj instanceof Boolean) return obj.toString();
        if (obj instanceof Iterable) {
            java.util.StringJoiner sj = new java.util.StringJoiner(",", "[", "]");
            for (Object item : (Iterable<?>) obj) {
                sj.add(toJson(item));
            }
            return sj.toString();
        }
        try {
            Map<String, Object> map = new HashMap<>();
            if (obj.getClass().isRecord()) {
                for (java.lang.reflect.RecordComponent rc : obj.getClass().getRecordComponents()) {
                    rc.getAccessor().setAccessible(true);
                    map.put(rc.getName(), rc.getAccessor().invoke(obj));
                }
            } else {
                for (Field field : obj.getClass().getDeclaredFields()) {
                    if (Modifier.isStatic(field.getModifiers()) || Modifier.isTransient(field.getModifiers())) {
                        continue;
                    }
                    field.setAccessible(true);
                    map.put(field.getName(), field.get(obj));
                }
            }
            return JettraJson.toJson(map);
        } catch (Exception e) {
            e.printStackTrace();
            return "{}";
        }
    }
}
