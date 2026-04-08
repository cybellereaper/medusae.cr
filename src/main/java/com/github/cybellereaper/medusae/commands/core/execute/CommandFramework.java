package com.github.cybellereaper.medusae.commands.core.execute;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.cybellereaper.medusae.commands.core.autocomplete.AutocompleteProvider;
import com.github.cybellereaper.medusae.commands.core.autocomplete.AutocompleteRegistry;
import com.github.cybellereaper.medusae.commands.core.check.CheckRegistry;
import com.github.cybellereaper.medusae.commands.core.check.CommandCheck;
import com.github.cybellereaper.medusae.commands.core.exception.CheckFailedException;
import com.github.cybellereaper.medusae.commands.core.exception.CommandNotFoundException;
import com.github.cybellereaper.medusae.commands.core.exception.RegistrationException;
import com.github.cybellereaper.medusae.commands.core.exception.ResolutionException;
import com.github.cybellereaper.medusae.commands.core.interaction.context.InteractionContext;
import com.github.cybellereaper.medusae.commands.core.model.*;
import com.github.cybellereaper.medusae.commands.core.parser.CommandParser;
import com.github.cybellereaper.medusae.commands.core.parser.InteractionModuleParser;
import com.github.cybellereaper.medusae.commands.core.registry.CommandRegistry;
import com.github.cybellereaper.medusae.commands.core.registry.InteractionHandlerRegistry;
import com.github.cybellereaper.medusae.commands.core.resolve.ParameterResolver;
import com.github.cybellereaper.medusae.commands.core.resolve.ConversionSupport;
import com.github.cybellereaper.medusae.commands.core.resolve.ResolverRegistry;
import com.github.cybellereaper.medusae.commands.core.response.CommandResponse;
import com.github.cybellereaper.medusae.commands.core.response.ImmediateResponse;
import com.github.cybellereaper.medusae.commands.core.response.InteractionReply;

import java.lang.reflect.InvocationTargetException;
import java.util.*;

public final class CommandFramework {
    private final CommandRegistry commandRegistry = new CommandRegistry();
    private final InteractionHandlerRegistry interactionRegistry = new InteractionHandlerRegistry();
    private final ResolverRegistry resolverRegistry = new ResolverRegistry();
    private final CheckRegistry checkRegistry = new CheckRegistry();
    private final AutocompleteRegistry autocompleteRegistry = new AutocompleteRegistry();
    private final CooldownManager cooldownManager = new CooldownManager();
    private final CommandParser commandParser = new CommandParser();
    private final InteractionModuleParser interactionParser = new InteractionModuleParser();
    private CommandExceptionHandler exceptionHandler = CommandExceptionHandler.rethrowing();

    private static void applyResult(CommandResponder responder, Object result) {
        if (result instanceof CommandResponse response) {
            responder.accept(response);
        } else if (result instanceof String content) {
            responder.accept(ImmediateResponse.publicMessage(content));
        }
    }

    private static Throwable unwrap(Throwable throwable) {
        if (throwable instanceof InvocationTargetException invocationTargetException && invocationTargetException.getCause() != null) {
            return invocationTargetException.getCause();
        }
        return throwable;
    }

    private static Object wrapOptional(CommandParameter parameter, Object value) {
        return parameter.wrappedOptional() ? Optional.of(value) : value;
    }

    private static Object convert(Class<?> type, String raw, String field, boolean required, boolean wrappedOptional) {
        if (raw == null) {
            if (!required) {
                if (wrappedOptional) return Optional.empty();
                return type.isPrimitive() ? primitiveDefault(type) : null;
            }
            throw new ResolutionException("Missing required value: " + field);
        }
        Object converted = convertString(raw, type, field);
        return wrappedOptional ? Optional.of(converted) : converted;
    }

    private static <T> T resolveEntityParameter(T contextTarget, Map<String, T> optionTargets, String optionName) {
        if (contextTarget != null) return contextTarget;
        if (optionName == null) return null;
        return optionTargets.get(optionName);
    }

    private static Object convertString(String value, Class<?> type, String fieldName) {
        return ConversionSupport.convertScalar(value, type, fieldName);
    }

    private static Object primitiveDefault(Class<?> primitiveType) {
        if (primitiveType == boolean.class) return false;
        if (primitiveType == int.class) return 0;
        if (primitiveType == long.class) return 0L;
        if (primitiveType == double.class) return 0D;
        return null;
    }

    private static CommandInteraction buildSyntheticInteraction(InteractionExecution interaction) {
        return new CommandInteraction("interaction", CommandType.CHAT_INPUT, null, null, Map.of(), null,
                interaction.rawInteraction(), interaction.dm(), interaction.guildId(), interaction.userId(),
                interaction.userPermissions(), interaction.botPermissions(), null, null, null, null, null, null);
    }

    private static String optionValue(CommandInteraction interaction, String option) {
        CommandOptionValue value = interaction.options().get(option);
        return value == null || value.value() == null ? "" : String.valueOf(value.value());
    }

    public void registerCommands(Object... handlers) {
        for (Object handler : handlers) {
            CommandDefinition parsed = commandParser.parse(handler);
            validateChecks(parsed);
            commandRegistry.register(parsed);
        }
    }

    public void registerModules(Object... handlers) {
        for (Object handler : handlers) {
            registerModule(handler);
        }
    }

    private void registerModule(Object handler) {
        try {
            registerCommands(handler);
        } catch (RegistrationException ignored) {
            // module may be component/modal only.
        }
        List<InteractionHandler> parsedHandlers = interactionParser.parse(handler);
        parsedHandlers.forEach(this::validateChecks);
        parsedHandlers.forEach(interactionRegistry::register);
    }

    public <T> void registerResolver(Class<T> type, ParameterResolver<? extends T> resolver) {
        resolverRegistry.register(type, resolver);
    }

    public void registerCheck(String id, CommandCheck check) {
        checkRegistry.register(id, check);
    }

    public void registerAutocomplete(String id, AutocompleteProvider provider) {
        autocompleteRegistry.register(id, provider);
    }

    public void setExceptionHandler(CommandExceptionHandler exceptionHandler) {
        this.exceptionHandler = Objects.requireNonNull(exceptionHandler, "exceptionHandler");
    }

    public List<String> executeAutocomplete(CommandInteraction interaction, CommandResponder responder) {
        Objects.requireNonNull(interaction, "interaction");
        Objects.requireNonNull(responder, "responder");
        CommandDefinition definition = commandRegistry.find(interaction.commandName())
                .orElseThrow(() -> new CommandNotFoundException("Unknown command: " + interaction.commandName()));

        String option = interaction.focusedOption();
        if (option == null) return List.of();

        CommandContext context = new CommandContext(interaction, responder);
        for (AutocompleteHandler handler : definition.autocompleteHandlers()) {
            if (handler.optionName().equalsIgnoreCase(option)) {
                try {
                    @SuppressWarnings("unchecked")
                    List<String> values = (List<String>) handler.method().invoke(handler.instance(), context);
                    return values == null ? List.of() : values;
                } catch (IllegalAccessException | InvocationTargetException exception) {
                    exceptionHandler.onException(context, unwrap(exception));
                    return List.of();
                }
            }
        }

        return definition.handlers().stream()
                .flatMap(h -> h.parameters().stream())
                .filter(p -> p.autocompleteId() != null && p.optionName().equalsIgnoreCase(option))
                .findFirst()
                .flatMap(p -> autocompleteRegistry.find(p.autocompleteId()))
                .map(provider -> provider.complete(context, optionValue(interaction, option)))
                .orElse(List.of());
    }

    public void execute(CommandInteraction interaction, CommandResponder responder) {
        Objects.requireNonNull(interaction, "interaction");
        Objects.requireNonNull(responder, "responder");
        CommandContext context = new CommandContext(interaction, responder);
        try {
            CommandDefinition definition = commandRegistry.find(interaction.commandName())
                    .orElseThrow(() -> new CommandNotFoundException("Unknown command: " + interaction.commandName()));
            CommandHandler handler = commandRegistry.findHandler(interaction.commandName(), interaction.routeKey())
                    .orElseThrow(() -> new CommandNotFoundException("Unknown subcommand route: " + interaction.routeKey()));

            enforceGuards(context, definition, handler);
            applyCooldown(definition, handler, interaction);

            Object[] args = resolveParameters(context, handler);
            invokeHandler(handler.instance(), handler.method(), args, false, false, false, responder);
        } catch (Throwable throwable) {
            exceptionHandler.onException(context, unwrap(throwable));
        }
    }

    public void executeInteraction(InteractionExecution interaction, CommandResponder responder) {
        Objects.requireNonNull(interaction, "interaction");
        Objects.requireNonNull(responder, "responder");

        try {
            InteractionHandlerRegistry.ResolvedInteractionHandler resolved = interactionRegistry.find(interaction.type(), interaction.customId())
                    .orElseThrow(() -> new CommandNotFoundException("Unknown interaction route: " + interaction.type() + " " + interaction.customId()));
            InteractionHandler handler = resolved.handler();

            InteractionContext context = InteractionContext.from(interaction, responder, resolved.routeMatch().pathParams());

            enforceGuards(context, handler);
            applyCooldown(handler, interaction);

            Object[] args = resolveInteractionParameters(context, handler);
            invokeHandler(handler.instance(), handler.method(), args, handler.deferReply(), handler.deferUpdate(), handler.ephemeralDefault(), responder);
        } catch (Throwable throwable) {
            exceptionHandler.onException(new CommandContext(buildSyntheticInteraction(interaction), responder), unwrap(throwable));
        }
    }

    public CommandRegistry registry() {
        return commandRegistry;
    }

    public InteractionHandlerRegistry interactionRegistry() {
        return interactionRegistry;
    }

    private void enforceGuards(CommandContext context, CommandDefinition definition, CommandHandler handler) {
        if (definition.guildOnly() && context.interaction().dm())
            throw new CheckFailedException("Command is guild-only");
        if (definition.dmOnly() && !context.interaction().dm()) throw new CheckFailedException("Command is DM-only");

        List<String> userPermissions = new ArrayList<>(definition.requiredUserPermissions());
        userPermissions.addAll(handler.requiredUserPermissions());
        if (!context.interaction().userPermissions().containsAll(userPermissions)) {
            throw new CheckFailedException("Missing user permissions: " + userPermissions);
        }

        List<String> botPermissions = new ArrayList<>(definition.requiredBotPermissions());
        botPermissions.addAll(handler.requiredBotPermissions());
        if (!context.interaction().botPermissions().containsAll(botPermissions)) {
            throw new CheckFailedException("Missing bot permissions: " + botPermissions);
        }

        List<String> checks = new ArrayList<>(definition.checks());
        checks.addAll(handler.checks());
        runChecks(context, checks);
    }

    private void enforceGuards(InteractionContext context, InteractionHandler handler) {
        InteractionExecution interaction = context.interaction();
        InteractionSource source = interaction.dm() ? InteractionSource.DM : InteractionSource.GUILD;
        if (!handler.allowedSources().contains(source)) {
            throw new CheckFailedException("Interaction source not allowed: " + source);
        }
        if (!interaction.userPermissions().containsAll(handler.requiredUserPermissions())) {
            throw new CheckFailedException("Missing user permissions: " + handler.requiredUserPermissions());
        }
        if (!interaction.botPermissions().containsAll(handler.requiredBotPermissions())) {
            throw new CheckFailedException("Missing bot permissions: " + handler.requiredBotPermissions());
        }
        runChecks(new CommandContext(buildSyntheticInteraction(interaction), response -> {
        }), handler.checks());
    }

    private void applyCooldown(CommandDefinition definition, CommandHandler handler, CommandInteraction interaction) {
        applyCooldown(definition.name() + ":" + (handler.routeKey() == null ? "root" : handler.routeKey()),
                handler.cooldown() == null ? definition.cooldown() : handler.cooldown(), interaction);
    }

    private void applyCooldown(InteractionHandler handler, InteractionExecution interaction) {
        applyCooldown("interaction:" + handler.type() + ":" + handler.route(), handler.cooldown(), buildSyntheticInteraction(interaction));
    }

    private void applyCooldown(String routeKey, CooldownSpec cooldown, CommandInteraction interaction) {
        cooldownManager.enforce(routeKey, cooldown, interaction);
    }

    private void invokeHandler(Object instance,
                               java.lang.reflect.Method method,
                               Object[] args,
                               boolean deferReply,
                               boolean deferUpdate,
                               boolean ephemeralDefault,
                               CommandResponder responder) throws InvocationTargetException, IllegalAccessException {
        Object result = method.invoke(instance, args);
        Object normalized = normalizeResult(result, deferReply, deferUpdate, ephemeralDefault);
        applyResult(responder, normalized);
    }

    private static Object normalizeResult(Object result, boolean deferReply, boolean deferUpdate, boolean ephemeralDefault) {
        if (result != null) {
            return result;
        }
        if (deferReply) {
            return InteractionReply.deferReply().ephemeral(ephemeralDefault).build();
        }
        if (deferUpdate) {
            return InteractionReply.deferUpdate().build();
        }
        return null;
    }

    private void runChecks(CommandContext context, List<String> checks) {
        for (String checkId : checks) {
            boolean result = checkRegistry.find(checkId)
                    .orElseThrow(() -> new RegistrationException("Unknown check id '" + checkId + "'"))
                    .test(context);
            if (!result) throw new CheckFailedException("Check failed: " + checkId);
        }
    }

    private Object[] resolveParameters(CommandContext context, CommandHandler handler) {
        Object[] args = new Object[handler.parameters().size()];
        for (CommandParameter parameter : handler.parameters()) {
            args[parameter.index()] = resolveParameter(context, parameter);
        }
        return args;
    }

    private Object[] resolveInteractionParameters(InteractionContext context, InteractionHandler handler) {
        Object[] args = new Object[handler.parameters().size()];
        for (InteractionParameter parameter : handler.parameters()) {
            args[parameter.index()] = resolveInteractionParameter(context, parameter);
        }
        return args;
    }

    private Object resolveInteractionParameter(InteractionContext context, InteractionParameter parameter) {
        return switch (parameter.kind()) {
            case CONTEXT -> context;
            case PATH_PARAM ->
                    convert(parameter.type(), context.pathParam(parameter.key()), parameter.key(), parameter.required(), parameter.wrappedOptional());
            case FIELD ->
                    convert(parameter.type(), context.interaction().modalFields().get(parameter.key()), parameter.key(), parameter.required(), parameter.wrappedOptional());
            case COMPONENT_STATE ->
                    convert(parameter.type(), context.interaction().statePayload(), "state", parameter.required(), parameter.wrappedOptional());
        };
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private Object resolveParameter(CommandContext context, CommandParameter parameter) {
        Class<?> type = parameter.optionType();
        CommandInteraction interaction = context.interaction();

        return switch (parameter.kind()) {
            case CONTEXT -> context;
            case RAW_INTERACTION -> interaction.rawInteraction();
            case TARGET_USER ->
                    resolveEntityParameter(interaction.targetUser(), interaction.optionUsers(), parameter.optionName());
            case TARGET_MEMBER ->
                    resolveEntityParameter(interaction.targetMember(), interaction.optionMembers(), parameter.optionName());
            case TARGET_CHANNEL ->
                    resolveEntityParameter(interaction.targetChannel(), interaction.optionChannels(), parameter.optionName());
            case TARGET_ROLE ->
                    resolveEntityParameter(interaction.targetRole(), interaction.optionRoles(), parameter.optionName());
            case TARGET_ATTACHMENT ->
                    resolveEntityParameter(interaction.targetAttachment(), interaction.optionAttachments(), parameter.optionName());
            case TARGET_MESSAGE -> interaction.targetMessage();
            case OPTION -> resolveOption(type, interaction, parameter);
            case CUSTOM -> resolverRegistry.find(type)
                    .map(resolver -> ((ParameterResolver) resolver).resolve(context, parameter))
                    .orElseThrow(() -> new ResolutionException("No resolver registered for custom type: " + type.getName()));
        };
    }

    private Object resolveOption(Class<?> type, CommandInteraction interaction, CommandParameter parameter) {
        CommandOptionValue option = interaction.options().get(parameter.optionName());
        if (option == null || option.value() == null) {
            if (parameter.defaultValue() != null) {
                Object fallbackValue = convertString(parameter.defaultValue(), type, parameter.optionName());
                return parameter.wrappedOptional() ? Optional.of(fallbackValue) : fallbackValue;
            }
            if (!parameter.required()) {
                if (parameter.wrappedOptional()) return Optional.empty();
                if (type.isPrimitive()) return primitiveDefault(type);
                return null;
            }
            throw new ResolutionException("Missing required option: " + parameter.optionName());
        }

        Object rawValue = option.value();
        if (type.isInstance(rawValue)) return parameter.wrappedOptional() ? Optional.of(rawValue) : rawValue;
        if (rawValue instanceof String textValue) {
            Object resolvedValue = convertString(textValue, type, parameter.optionName());
            return parameter.wrappedOptional() ? Optional.of(resolvedValue) : resolvedValue;
        }
        if (rawValue instanceof JsonNode node) {
            if (type == String.class) return wrapOptional(parameter, ConversionSupport.parseString(node));
            if (type == int.class || type == Integer.class) return wrapOptional(parameter, requireNonNullConverted(ConversionSupport.parseInt(node), parameter.optionName(), type));
            if (type == long.class || type == Long.class) return wrapOptional(parameter, requireNonNullConverted(ConversionSupport.parseLong(node), parameter.optionName(), type));
            if (type == double.class || type == Double.class) return wrapOptional(parameter, requireNonNullConverted(ConversionSupport.parseDouble(node), parameter.optionName(), type));
            if (type == boolean.class || type == Boolean.class) return wrapOptional(parameter, requireNonNullConverted(ConversionSupport.parseBooleanStrict(node), parameter.optionName(), type));
        }
        if (rawValue instanceof Number number) {
            if (type == int.class || type == Integer.class) return wrapOptional(parameter, number.intValue());
            if (type == long.class || type == Long.class) return wrapOptional(parameter, number.longValue());
            if (type == double.class || type == Double.class) return wrapOptional(parameter, number.doubleValue());
        }
        if (rawValue instanceof Boolean booleanValue && (type == boolean.class || type == Boolean.class))
            return wrapOptional(parameter, booleanValue);
        throw new ResolutionException("Invalid option type for '" + parameter.optionName() + "'. Expected " + type.getSimpleName());
    }

    private Object requireNonNullConverted(Object converted, String optionName, Class<?> type) {
        if (converted != null) {
            return converted;
        }
        throw new ResolutionException("Invalid option type for '" + optionName + "'. Expected " + type.getSimpleName());
    }

    private void validateChecks(CommandDefinition definition) {
        List<String> checks = new ArrayList<>(definition.checks());
        definition.handlers().forEach(handler -> checks.addAll(handler.checks()));
        for (String check : checks) {
            if (checkRegistry.find(check).isEmpty()) {
                throw new RegistrationException("Check '" + check + "' was referenced but is not registered");
            }
        }
    }

    private void validateChecks(InteractionHandler handler) {
        for (String check : handler.checks()) {
            if (checkRegistry.find(check).isEmpty()) {
                throw new RegistrationException("Check '" + check + "' was referenced but is not registered");
            }
        }
    }
}
