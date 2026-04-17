package com.github.cybellereaper.medusae.commands.core.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
@Repeatable(DescriptionLocalizations.class)
public @interface DescriptionLocalization {
    String locale();

    String value();
}
