package com.github.cybellereaper.medusae.commands.core.annotation;

import com.github.cybellereaper.medusae.commands.core.model.InteractionSource;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface AllowedInteractionSource {
    InteractionSource[] value();
}
