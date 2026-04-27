package com.github.cybellereaper.medusae.http;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonCreator;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class DiscordRestResponse {
    private final Object body;

    @JsonCreator
    public DiscordRestResponse(List<Map<String, Object>> items) {
        this.body = items == null ? new ArrayList<>() : items;
    }

    public DiscordRestResponse() {
        this.body = new LinkedHashMap<String, Object>();
    }

    public Object body() {
        return body;
    }

    @JsonAnySetter
    void put(String key, Object value) {
        objectBody().put(key, value);
    }

    public Map<String, Object> objectBody() {
        return body instanceof Map<?, ?> map ? castMap(map) : Map.of();
    }

    public List<Map<String, Object>> objectListBody() {
        if (!(body instanceof List<?> list)) {
            return List.of();
        }
        return list.stream()
                .filter(Map.class::isInstance)
                .map(Map.class::cast)
                .map(DiscordRestResponse::castMap)
                .toList();
    }

    public String string(String field) {
        Object value = objectBody().get(field);
        return value == null ? null : value.toString();
    }

    public int integer(String field) {
        Object value = objectBody().get(field);
        return value instanceof Number number ? number.intValue() : 0;
    }

    public long longValue(String field) {
        Object value = objectBody().get(field);
        return value instanceof Number number ? number.longValue() : 0L;
    }

    public double doubleValue(String field, double defaultValue) {
        Object value = objectBody().get(field);
        return value instanceof Number number ? number.doubleValue() : defaultValue;
    }

    public boolean bool(String field, boolean defaultValue) {
        Object value = objectBody().get(field);
        return value instanceof Boolean bool ? bool : defaultValue;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> castMap(Map<?, ?> map) {
        return (Map<String, Object>) map;
    }
}
