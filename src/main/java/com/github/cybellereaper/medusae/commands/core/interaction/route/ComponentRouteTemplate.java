package com.github.cybellereaper.medusae.commands.core.interaction.route;

import com.github.cybellereaper.medusae.commands.core.exception.RegistrationException;
import com.github.cybellereaper.medusae.commands.core.model.InteractionRouteMatch;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ComponentRouteTemplate {
    private static final Pattern PARAM = Pattern.compile("\\{([a-zA-Z][a-zA-Z0-9_]*)}");

    private final String template;
    private final Pattern pattern;
    private final List<String> paramNames;

    private ComponentRouteTemplate(String template, Pattern pattern, List<String> paramNames) {
        this.template = template;
        this.pattern = pattern;
        this.paramNames = paramNames;
    }

    public static ComponentRouteTemplate compile(String template) {
        String normalized = normalize(template);
        Matcher matcher = PARAM.matcher(normalized);
        StringBuilder regex = new StringBuilder("^");
        int cursor = 0;
        List<String> params = new ArrayList<>();

        while (matcher.find()) {
            String name = matcher.group(1);
            if (params.contains(name)) {
                throw new RegistrationException("Duplicate path param '" + name + "' in route " + template);
            }
            params.add(name);
            regex.append(Pattern.quote(normalized.substring(cursor, matcher.start())));
            regex.append("([^:|]+)");
            cursor = matcher.end();
        }

        regex.append(Pattern.quote(normalized.substring(cursor)));
        regex.append("(?:\\|(.+))?$");
        return new ComponentRouteTemplate(normalized, Pattern.compile(regex.toString()), List.copyOf(params));
    }

    private static String normalize(String value) {
        if (value == null) {
            throw new RegistrationException("Route template must not be null");
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        if (normalized.isEmpty()) {
            throw new RegistrationException("Route template must not be blank");
        }
        if (normalized.length() > 100) {
            throw new RegistrationException("Route template exceeds Discord custom-id limit (100): " + value);
        }
        return normalized;
    }

    public Optional<InteractionRouteMatch> match(String customId) {
        if (customId == null || customId.isBlank()) {
            return Optional.empty();
        }
        Matcher matcher = pattern.matcher(customId.trim().toLowerCase(Locale.ROOT));
        if (!matcher.matches()) {
            return Optional.empty();
        }
        Map<String, String> path = new LinkedHashMap<>();
        for (int i = 0; i < paramNames.size(); i++) {
            path.put(paramNames.get(i), matcher.group(i + 1));
        }
        String statePayload = matcher.group(paramNames.size() + 1);
        return Optional.of(new InteractionRouteMatch(template, Map.copyOf(path), statePayload));
    }

    public String template() {
        return template;
    }

    public List<String> paramNames() {
        return paramNames;
    }

    public boolean conflictsWith(ComponentRouteTemplate other) {
        if (this.template.equals(other.template)) {
            return true;
        }
        if (paramNames.isEmpty() && other.paramNames.isEmpty()) {
            return false;
        }
        String probe = this.template.replaceAll("\\{[a-zA-Z][a-zA-Z0-9_]*}", "x");
        return other.match(probe).isPresent();
    }
}
