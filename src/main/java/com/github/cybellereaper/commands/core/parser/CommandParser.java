package com.github.cybellereaper.commands.core.parser;

import com.github.cybellereaper.commands.core.annotation.*;
import com.github.cybellereaper.commands.core.exception.RegistrationException;
import com.github.cybellereaper.commands.core.execute.CommandContext;
import com.github.cybellereaper.commands.core.model.*;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public final class CommandParser {
    public CommandDefinition parse(Object instance) {
        Objects.requireNonNull(instance, "instance");
        Class<?> type = instance.getClass();
        Command command = type.getAnnotation(Command.class);
        if (command == null) {
            throw new RegistrationException("Missing @Command on " + type.getName());
        }

        List<CommandHandler> handlers = new ArrayList<>();
        List<AutocompleteHandler> autocompleteHandlers = new ArrayList<>();

        for (Method method : type.getDeclaredMethods()) {
            if (method.isAnnotationPresent(Autocomplete.class)) {
                autocompleteHandlers.add(parseAutocomplete(instance, command, method));
                continue;
            }
            if (isCommandHandlerMethod(command.type(), method)) {
                handlers.add(parseHandler(instance, command, method));
            }
        }

        if (handlers.isEmpty() && autocompleteHandlers.isEmpty()) {
            throw new RegistrationException("No command handlers were found for " + type.getName());
        }

        validateHandlers(command, handlers);

        return new CommandDefinition(
                normalizeName(command.value()),
                command.type(),
                extractDescription(type),
                type.isAnnotationPresent(Hidden.class),
                type.isAnnotationPresent(GuildOnly.class),
                type.isAnnotationPresent(DmOnly.class),
                annotationValues(type.getAnnotation(Check.class)),
                annotationValues(type.getAnnotation(RequireUserPermissions.class)),
                annotationValues(type.getAnnotation(RequireBotPermissions.class)),
                cooldown(type.getAnnotation(Cooldown.class)),
                List.copyOf(handlers),
                List.copyOf(autocompleteHandlers)
        );
    }

    private static boolean isCommandHandlerMethod(CommandType commandType, Method method) {
        return method.isAnnotationPresent(Execute.class)
                || method.isAnnotationPresent(Subcommand.class)
                || (commandType == CommandType.CHAT_INPUT && method.getName().equals("root"));
    }

    private CommandHandler parseHandler(Object instance, Command command, Method method) {
        method.setAccessible(true);
        String subcommand = annotationValue(method.getAnnotation(Subcommand.class));
        String group = annotationValue(method.getAnnotation(SubcommandGroup.class));
        if (command.type() != CommandType.CHAT_INPUT && (subcommand != null || group != null)) {
            throw new RegistrationException("Context menu command methods cannot declare @Subcommand: " + method);
        }

        List<CommandParameter> parameters = new ArrayList<>();
        Parameter[] reflectedParameters = method.getParameters();
        for (int i = 0; i < reflectedParameters.length; i++) {
            parameters.add(parseParameter(i, reflectedParameters[i]));
        }

        return new CommandHandler(
                instance,
                method,
                group,
                subcommand,
                extractDescription(method),
                method.isAnnotationPresent(Hidden.class),
                annotationValues(method.getAnnotation(Check.class)),
                annotationValues(method.getAnnotation(RequireUserPermissions.class)),
                annotationValues(method.getAnnotation(RequireBotPermissions.class)),
                cooldown(method.getAnnotation(Cooldown.class)),
                List.copyOf(parameters),
                routeKey(group, subcommand)
        );
    }

    private AutocompleteHandler parseAutocomplete(Object instance, Command command, Method method) {
        method.setAccessible(true);
        if (command.type() != CommandType.CHAT_INPUT) {
            throw new RegistrationException("Autocomplete methods are only valid for chat input commands: " + method);
        }
        Autocomplete annotation = method.getAnnotation(Autocomplete.class);
        return new AutocompleteHandler(instance, method, null, normalizeName(annotation.value()), normalizeName(annotation.value()));
    }

    private CommandParameter parseParameter(int index, Parameter parameter) {
        ParameterKind kind = determineKind(parameter.getType());
        String optionName = extractOptionName(parameter);
        boolean required = !(parameter.isAnnotationPresent(Optional.class) || parameter.isAnnotationPresent(Default.class));
        String defaultValue = parameter.isAnnotationPresent(Default.class) ? parameter.getAnnotation(Default.class).value() : null;
        String autocompleteId = parameter.isAnnotationPresent(Autocomplete.class) ? parameter.getAnnotation(Autocomplete.class).value() : null;

        if ((kind == ParameterKind.CONTEXT || kind == ParameterKind.RAW_INTERACTION) && !required) {
            throw new RegistrationException("@Optional/@Default are only valid for option parameters: " + parameter);
        }

        return new CommandParameter(index, parameter, optionName, extractDescription(parameter), kind, required, defaultValue, autocompleteId);
    }

    private static ParameterKind determineKind(Class<?> type) {
        if (type == CommandContext.class) {
            return ParameterKind.CONTEXT;
        }
        if (type == Object.class) {
            return ParameterKind.RAW_INTERACTION;
        }
        String name = type.getSimpleName();
        return switch (name) {
            case "ResolvedUser" -> ParameterKind.TARGET_USER;
            case "ResolvedMember" -> ParameterKind.TARGET_MEMBER;
            case "ResolvedChannel" -> ParameterKind.TARGET_CHANNEL;
            case "ResolvedRole" -> ParameterKind.TARGET_ROLE;
            case "ResolvedAttachment" -> ParameterKind.TARGET_ATTACHMENT;
            case "ResolvedMessage" -> ParameterKind.TARGET_MESSAGE;
            default -> isBuiltinOptionType(type) ? ParameterKind.OPTION : ParameterKind.CUSTOM;
        };
    }

    private static boolean isBuiltinOptionType(Class<?> type) {
        return type == String.class
                || type == long.class || type == Long.class
                || type == int.class || type == Integer.class
                || type == boolean.class || type == Boolean.class
                || type == double.class || type == Double.class
                || type.isEnum();
    }

    private static String extractOptionName(Parameter parameter) {
        if (parameter.isAnnotationPresent(Name.class)) {
            return normalizeName(parameter.getAnnotation(Name.class).value());
        }
        return normalizeName(parameter.getName());
    }

    private static String routeKey(String group, String subcommand) {
        if (group == null && subcommand == null) {
            return null;
        }
        if (group != null && subcommand == null) {
            throw new RegistrationException("@SubcommandGroup requires @Subcommand");
        }
        return group == null ? subcommand : group + "/" + subcommand;
    }

    private static void validateHandlers(Command command, List<CommandHandler> handlers) {
        if (command.type() != CommandType.CHAT_INPUT && handlers.size() > 1) {
            throw new RegistrationException("Context menu commands support exactly one handler");
        }

        long rootHandlers = handlers.stream().filter(h -> h.routeKey() == null).count();
        if (command.type() == CommandType.CHAT_INPUT && rootHandlers > 1) {
            throw new RegistrationException("Only one root handler is allowed per command");
        }

        long distinctRoutes = handlers.stream().map(CommandHandler::routeKey).distinct().count();
        if (distinctRoutes != handlers.size()) {
            throw new RegistrationException("Duplicate subcommand routes for command '" + command.value() + "'");
        }
    }

    private static String extractDescription(Class<?> type) {
        Description description = type.getAnnotation(Description.class);
        return description == null ? "No description provided" : description.value().trim();
    }

    private static String extractDescription(Method method) {
        Description description = method.getAnnotation(Description.class);
        return description == null ? "No description provided" : description.value().trim();
    }

    private static String extractDescription(Parameter parameter) {
        Description description = parameter.getAnnotation(Description.class);
        return description == null ? "No description provided" : description.value().trim();
    }

    private static String annotationValue(Subcommand annotation) {
        return annotation == null ? null : normalizeName(annotation.value());
    }

    private static String annotationValue(SubcommandGroup annotation) {
        return annotation == null ? null : normalizeName(annotation.value());
    }

    private static List<String> annotationValues(Check annotation) {
        return annotation == null ? List.of() : normalize(annotation.value());
    }

    private static List<String> annotationValues(RequireUserPermissions annotation) {
        return annotation == null ? List.of() : normalize(annotation.value());
    }

    private static List<String> annotationValues(RequireBotPermissions annotation) {
        return annotation == null ? List.of() : normalize(annotation.value());
    }

    private static List<String> normalize(String[] values) {
        return Arrays.stream(values).map(CommandParser::normalizeName).toList();
    }

    private static CooldownSpec cooldown(Cooldown annotation) {
        return annotation == null ? null : new CooldownSpec(annotation.amount(), annotation.seconds(), annotation.bucket().trim().toLowerCase(Locale.ROOT));
    }

    private static String normalizeName(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        if (normalized.isEmpty()) {
            throw new RegistrationException("Annotation value must not be blank");
        }
        return normalized;
    }
}
