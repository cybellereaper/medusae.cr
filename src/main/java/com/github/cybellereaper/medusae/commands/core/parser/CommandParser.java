package com.github.cybellereaper.medusae.commands.core.parser;

import com.github.cybellereaper.medusae.commands.core.annotation.*;
import com.github.cybellereaper.medusae.commands.core.exception.RegistrationException;
import com.github.cybellereaper.medusae.commands.core.execute.CommandContext;
import com.github.cybellereaper.medusae.commands.core.model.*;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;

public final class CommandParser {
    private static boolean isCommandHandlerMethod(CommandType commandType, Method method) {
        return method.isAnnotationPresent(Execute.class)
                || method.isAnnotationPresent(Subcommand.class)
                || (commandType == CommandType.CHAT_INPUT && method.getName().equals("root"));
    }

    private static Class<?> extractOptionalType(Parameter parameter) {
        Type parameterizedType = parameter.getParameterizedType();
        if (!(parameterizedType instanceof ParameterizedType typed)) {
            throw new RegistrationException("Optional parameter must declare a generic type: " + parameter);
        }
        Type[] arguments = typed.getActualTypeArguments();
        if (arguments.length != 1 || !(arguments[0] instanceof Class<?> argumentType)) {
            throw new RegistrationException("Optional parameter must use a concrete generic type: " + parameter);
        }
        return argumentType;
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

    private static List<CommandOptionChoice> extractChoices(Parameter parameter) {
        OptionChoice[] choices = parameter.getAnnotationsByType(OptionChoice.class);
        if (choices.length == 0) {
            return List.of();
        }
        return Arrays.stream(choices)
                .map(choice -> new CommandOptionChoice(choice.name(), choice.value()))
                .toList();
    }

    private static Map<String, String> extractNameLocalizations(Parameter parameter) {
        NameLocalization[] localizations = parameter.getAnnotationsByType(NameLocalization.class);
        if (localizations.length == 0) {
            return Map.of();
        }
        Map<String, String> result = new LinkedHashMap<>();
        for (NameLocalization localization : localizations) {
            result.put(normalizeName(localization.locale()), localization.value());
        }
        return Map.copyOf(result);
    }

    private static Map<String, String> extractDescriptionLocalizations(Parameter parameter) {
        DescriptionLocalization[] localizations = parameter.getAnnotationsByType(DescriptionLocalization.class);
        if (localizations.length == 0) {
            return Map.of();
        }
        Map<String, String> result = new LinkedHashMap<>();
        for (DescriptionLocalization localization : localizations) {
            result.put(normalizeName(localization.locale()), localization.value());
        }
        return Map.copyOf(result);
    }

    private static List<Integer> extractChannelTypes(Parameter parameter) {
        ChannelTypes channelTypes = parameter.getAnnotation(ChannelTypes.class);
        if (channelTypes == null || channelTypes.value().length == 0) {
            return List.of();
        }
        return Arrays.stream(channelTypes.value()).boxed().toList();
    }

    private static void validateOptionSchema(Parameter reflected,
                                             ParameterKind kind,
                                             Class<?> optionType,
                                             String autocompleteId,
                                             List<CommandOptionChoice> choices,
                                             Double minValue,
                                             Double maxValue,
                                             Integer minLength,
                                             Integer maxLength,
                                             List<Integer> channelTypes) {
        if (autocompleteId != null && !choices.isEmpty()) {
            throw new RegistrationException("@Autocomplete cannot be used together with @OptionChoice: " + reflected);
        }
        if ((minValue != null || maxValue != null) && !(optionType == int.class || optionType == Integer.class
                || optionType == long.class || optionType == Long.class || optionType == double.class || optionType == Double.class)) {
            throw new RegistrationException("@MinValue/@MaxValue are only valid for numeric option parameters: " + reflected);
        }
        if ((minLength != null || maxLength != null) && optionType != String.class) {
            throw new RegistrationException("@MinLength/@MaxLength are only valid for string option parameters: " + reflected);
        }
        if (!channelTypes.isEmpty() && kind != ParameterKind.TARGET_CHANNEL) {
            throw new RegistrationException("@ChannelTypes is only valid for channel target parameters: " + reflected);
        }
    }

    public CommandDefinition parse(Object instance) {
        Objects.requireNonNull(instance, "instance");
        Class<?> type = instance.getClass();
        Command command = type.getAnnotation(Command.class);
        if (command == null) {
            throw new RegistrationException("Missing @Command on " + type.getName());
        }

        List<CommandHandler> handlers = new ArrayList<>();
        List<AutocompleteHandler> autocompleteHandlers = new ArrayList<>();

        Method[] declaredMethods = type.getDeclaredMethods();
        java.util.Arrays.sort(declaredMethods, java.util.Comparator.comparing(Method::getName));
        for (Method method : declaredMethods) {
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
        boolean wrappedOptional = parameter.getType() == java.util.Optional.class;
        Class<?> optionType = wrappedOptional ? extractOptionalType(parameter) : parameter.getType();
        ParameterKind kind = determineKind(optionType);
        String optionName = extractOptionName(parameter);
        boolean required = !(wrappedOptional
                || parameter.isAnnotationPresent(com.github.cybellereaper.medusae.commands.core.annotation.Optional.class)
                || parameter.isAnnotationPresent(Default.class));
        String defaultValue = parameter.isAnnotationPresent(Default.class) ? parameter.getAnnotation(Default.class).value() : null;
        String autocompleteId = parameter.isAnnotationPresent(Autocomplete.class) ? parameter.getAnnotation(Autocomplete.class).value() : null;
        List<CommandOptionChoice> choices = extractChoices(parameter);
        Double minValue = parameter.isAnnotationPresent(MinValue.class) ? parameter.getAnnotation(MinValue.class).value() : null;
        Double maxValue = parameter.isAnnotationPresent(MaxValue.class) ? parameter.getAnnotation(MaxValue.class).value() : null;
        Integer minLength = parameter.isAnnotationPresent(MinLength.class) ? parameter.getAnnotation(MinLength.class).value() : null;
        Integer maxLength = parameter.isAnnotationPresent(MaxLength.class) ? parameter.getAnnotation(MaxLength.class).value() : null;
        List<Integer> channelTypes = extractChannelTypes(parameter);
        Map<String, String> nameLocalizations = extractNameLocalizations(parameter);
        Map<String, String> descriptionLocalizations = extractDescriptionLocalizations(parameter);

        if ((kind == ParameterKind.CONTEXT || kind == ParameterKind.RAW_INTERACTION) && !required) {
            throw new RegistrationException("@Optional/@Default are only valid for option parameters: " + parameter);
        }

        validateOptionSchema(parameter, kind, optionType, autocompleteId, choices, minValue, maxValue, minLength, maxLength, channelTypes);

        return new CommandParameter(index, parameter, optionType, optionName, extractDescription(parameter), kind, required, defaultValue,
                autocompleteId, wrappedOptional, choices, minValue, maxValue, minLength, maxLength, channelTypes,
                nameLocalizations, descriptionLocalizations);
    }
}
