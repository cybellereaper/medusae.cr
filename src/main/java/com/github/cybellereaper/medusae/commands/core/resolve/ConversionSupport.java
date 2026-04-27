package com.github.cybellereaper.medusae.commands.core.resolve;

import com.github.cybellereaper.medusae.commands.core.exception.ResolutionException;

import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

public final class ConversionSupport {
    private ConversionSupport() {
    }

    public static String parseString(String raw) {
        return raw;
    }

    public static Long parseLong(String raw) {
        if (raw == null) {
            return null;
        }
        try {
            return Long.parseLong(raw.trim());
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    public static Integer parseInt(String raw) {
        Long parsed = parseLong(raw);
        if (parsed == null || parsed < Integer.MIN_VALUE || parsed > Integer.MAX_VALUE) {
            return null;
        }
        return parsed.intValue();
    }

    public static Double parseDouble(String raw) {
        if (raw == null) {
            return null;
        }
        try {
            return Double.parseDouble(raw.trim());
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    public static Boolean parseBooleanStrict(String raw) {
        if (raw == null) {
            return null;
        }
        String text = raw.trim();
        if ("true".equalsIgnoreCase(text)) {
            return true;
        }
        if ("false".equalsIgnoreCase(text)) {
            return false;
        }
        return null;
    }

    public static Boolean parseBooleanLenient(String raw) {
        if (raw == null) {
            return null;
        }
        return Boolean.parseBoolean(raw);
    }

    public static String normalizeEntityId(String raw) {
        if (raw == null) {
            return null;
        }
        String trimmed = raw.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    public static <T> T required(T value, String name, String messagePrefix) {
        if (value == null) {
            throw new ResolutionException(messagePrefix + name);
        }
        return value;
    }

    public static <T> Optional<T> optional(boolean wrappedOptional, T value) {
        return wrappedOptional ? Optional.ofNullable(value) : null;
    }

    public static Object convertScalar(String rawValue, Class<?> targetType, String fieldName) {
        Objects.requireNonNull(targetType, "targetType");
        Function<String, ?> parser = parserFor(targetType);
        if (parser == null) {
            throw new ResolutionException("Unsupported option type: " + targetType.getName());
        }

        Object converted;
        try {
            converted = parser.apply(rawValue);
        } catch (RuntimeException exception) {
            throw new ResolutionException("Failed to convert value '" + fieldName + "': " + rawValue, exception);
        }

        if (converted == null && targetType != String.class) {
            throw new ResolutionException("Failed to convert value '" + fieldName + "': " + rawValue);
        }

        return converted;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static Function<String, ?> parserFor(Class<?> type) {
        if (type == String.class) return ConversionSupport::parseString;
        if (type == int.class || type == Integer.class) return ConversionSupport::parseInt;
        if (type == long.class || type == Long.class) return ConversionSupport::parseLong;
        if (type == double.class || type == Double.class) return ConversionSupport::parseDouble;
        if (type == boolean.class || type == Boolean.class) return ConversionSupport::parseBooleanLenient;
        if (type.isEnum()) {
            return value -> value == null ? null : Enum.valueOf((Class<? extends Enum>) type, value.toUpperCase(Locale.ROOT));
        }
        return null;
    }

}
