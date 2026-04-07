package com.github.cybellereaper.medusae.commands.core.parser;

import com.github.cybellereaper.medusae.commands.core.annotation.*;
import com.github.cybellereaper.medusae.commands.core.exception.RegistrationException;
import com.github.cybellereaper.medusae.commands.core.interaction.context.ComponentContext;
import com.github.cybellereaper.medusae.commands.core.interaction.context.InteractionContext;
import com.github.cybellereaper.medusae.commands.core.interaction.context.ModalContext;
import com.github.cybellereaper.medusae.commands.core.interaction.context.SelectContext;
import com.github.cybellereaper.medusae.commands.core.model.*;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;

public final class InteractionModuleParser {
    private static HandlerMarker detect(Method method) {
        if (method.isAnnotationPresent(ButtonHandler.class))
            return new HandlerMarker(InteractionHandlerType.BUTTON, method.getAnnotation(ButtonHandler.class).value());
        if (method.isAnnotationPresent(StringSelectHandler.class))
            return new HandlerMarker(InteractionHandlerType.STRING_SELECT, method.getAnnotation(StringSelectHandler.class).value());
        if (method.isAnnotationPresent(UserSelectHandler.class))
            return new HandlerMarker(InteractionHandlerType.USER_SELECT, method.getAnnotation(UserSelectHandler.class).value());
        if (method.isAnnotationPresent(RoleSelectHandler.class))
            return new HandlerMarker(InteractionHandlerType.ROLE_SELECT, method.getAnnotation(RoleSelectHandler.class).value());
        if (method.isAnnotationPresent(MentionableSelectHandler.class))
            return new HandlerMarker(InteractionHandlerType.MENTIONABLE_SELECT, method.getAnnotation(MentionableSelectHandler.class).value());
        if (method.isAnnotationPresent(ChannelSelectHandler.class))
            return new HandlerMarker(InteractionHandlerType.CHANNEL_SELECT, method.getAnnotation(ChannelSelectHandler.class).value());
        if (method.isAnnotationPresent(ModalHandler.class))
            return new HandlerMarker(InteractionHandlerType.MODAL, method.getAnnotation(ModalHandler.class).value());
        return null;
    }

    private static InteractionParameter parseParameter(int index, Parameter parameter, InteractionHandlerType type) {
        boolean wrappedOptional = parameter.getType() == java.util.Optional.class;
        Class<?> resolvedType = wrappedOptional ? extractOptionalType(parameter) : parameter.getType();

        if (InteractionContext.class.isAssignableFrom(parameter.getType())
                || ComponentContext.class.isAssignableFrom(parameter.getType())
                || SelectContext.class.isAssignableFrom(parameter.getType())
                || ModalContext.class.isAssignableFrom(parameter.getType())) {
            return new InteractionParameter(index, parameter, parameter.getType(), InteractionParameterKind.CONTEXT, null, true, false);
        }

        PathParam pathParam = parameter.getAnnotation(PathParam.class);
        if (pathParam != null) {
            return new InteractionParameter(index, parameter, resolvedType, InteractionParameterKind.PATH_PARAM, normalize(pathParam.value()), !wrappedOptional, wrappedOptional);
        }

        Field field = parameter.getAnnotation(Field.class);
        if (field != null) {
            if (type != InteractionHandlerType.MODAL) {
                throw new RegistrationException("@Field is only valid on @ModalHandler methods: " + parameter);
            }
            return new InteractionParameter(index, parameter, resolvedType, InteractionParameterKind.FIELD, normalize(field.value()), !wrappedOptional, wrappedOptional);
        }

        if (parameter.isAnnotationPresent(ComponentState.class)) {
            return new InteractionParameter(index, parameter, resolvedType, InteractionParameterKind.COMPONENT_STATE, "state", !wrappedOptional, wrappedOptional);
        }

        throw new RegistrationException("Unsupported interaction parameter signature: " + parameter);
    }

    private static Class<?> extractOptionalType(Parameter parameter) {
        Type parameterizedType = parameter.getParameterizedType();
        if (!(parameterizedType instanceof ParameterizedType typed) || !(typed.getActualTypeArguments()[0] instanceof Class<?> argumentType)) {
            throw new RegistrationException("Optional parameter must use a concrete generic type: " + parameter);
        }
        return argumentType;
    }

    private static Set<InteractionSource> allowedSources(Class<?> ownerType, Method method) {
        AllowedInteractionSource methodSources = method.getAnnotation(AllowedInteractionSource.class);
        if (methodSources != null) {
            return Set.copyOf(Arrays.asList(methodSources.value()));
        }
        AllowedInteractionSource typeSources = ownerType.getAnnotation(AllowedInteractionSource.class);
        if (typeSources != null) {
            return Set.copyOf(Arrays.asList(typeSources.value()));
        }
        return Set.of(InteractionSource.GUILD, InteractionSource.DM);
    }

    private static List<String> combine(Check classAnnotation, Check methodAnnotation) {
        List<String> values = new ArrayList<>();
        if (classAnnotation != null) values.addAll(Arrays.asList(classAnnotation.value()));
        if (methodAnnotation != null) values.addAll(Arrays.asList(methodAnnotation.value()));
        return values.stream().map(InteractionModuleParser::normalize).toList();
    }

    private static List<String> combine(RequireUserPermissions classAnnotation, RequireUserPermissions methodAnnotation) {
        List<String> values = new ArrayList<>();
        if (classAnnotation != null) values.addAll(Arrays.asList(classAnnotation.value()));
        if (methodAnnotation != null) values.addAll(Arrays.asList(methodAnnotation.value()));
        return values.stream().map(InteractionModuleParser::normalize).toList();
    }

    private static List<String> combine(RequireBotPermissions classAnnotation, RequireBotPermissions methodAnnotation) {
        List<String> values = new ArrayList<>();
        if (classAnnotation != null) values.addAll(Arrays.asList(classAnnotation.value()));
        if (methodAnnotation != null) values.addAll(Arrays.asList(methodAnnotation.value()));
        return values.stream().map(InteractionModuleParser::normalize).toList();
    }

    private static String normalize(String value) {
        String normalized = value == null ? null : value.trim().toLowerCase(Locale.ROOT);
        if (normalized == null || normalized.isBlank()) {
            throw new RegistrationException("Annotation value must not be blank");
        }
        return normalized;
    }

    public List<InteractionHandler> parse(Object instance) {
        Objects.requireNonNull(instance, "instance");
        Class<?> type = instance.getClass();
        List<InteractionHandler> handlers = new ArrayList<>();
        for (Method method : type.getDeclaredMethods()) {
            HandlerMarker marker = detect(method);
            if (marker == null) {
                continue;
            }
            method.setAccessible(true);
            handlers.add(parseHandler(instance, type, method, marker));
        }
        return List.copyOf(handlers);
    }

    private InteractionHandler parseHandler(Object instance, Class<?> ownerType, Method method, HandlerMarker marker) {
        List<InteractionParameter> parameters = new ArrayList<>();
        Parameter[] reflected = method.getParameters();
        for (int i = 0; i < reflected.length; i++) {
            parameters.add(parseParameter(i, reflected[i], marker.type));
        }

        return new InteractionHandler(
                instance,
                method,
                marker.type,
                normalize(marker.route),
                allowedSources(ownerType, method),
                ownerType.isAnnotationPresent(EphemeralDefault.class) || method.isAnnotationPresent(EphemeralDefault.class),
                ownerType.isAnnotationPresent(DeferReply.class) || method.isAnnotationPresent(DeferReply.class),
                ownerType.isAnnotationPresent(DeferUpdate.class) || method.isAnnotationPresent(DeferUpdate.class),
                combine(ownerType.getAnnotation(Check.class), method.getAnnotation(Check.class)),
                combine(ownerType.getAnnotation(RequireUserPermissions.class), method.getAnnotation(RequireUserPermissions.class)),
                combine(ownerType.getAnnotation(RequireBotPermissions.class), method.getAnnotation(RequireBotPermissions.class)),
                method.isAnnotationPresent(Cooldown.class) ? new CooldownSpec(method.getAnnotation(Cooldown.class).amount(), method.getAnnotation(Cooldown.class).seconds(), method.getAnnotation(Cooldown.class).bucket())
                        : (ownerType.isAnnotationPresent(Cooldown.class) ? new CooldownSpec(ownerType.getAnnotation(Cooldown.class).amount(), ownerType.getAnnotation(Cooldown.class).seconds(), ownerType.getAnnotation(Cooldown.class).bucket()) : null),
                List.copyOf(parameters)
        );
    }

    private record HandlerMarker(InteractionHandlerType type, String route) {
    }
}
