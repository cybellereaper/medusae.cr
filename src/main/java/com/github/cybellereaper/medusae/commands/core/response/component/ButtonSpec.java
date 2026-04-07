package com.github.cybellereaper.medusae.commands.core.response.component;

public record ButtonSpec(ButtonStyle style, String customId, String label, String url,
                         boolean disabled) implements ComponentSpec {
    public static ButtonSpec primary(String customId, String label) {
        return new ButtonSpec(ButtonStyle.PRIMARY, customId, label, null, false);
    }

    public static ButtonSpec secondary(String customId, String label) {
        return new ButtonSpec(ButtonStyle.SECONDARY, customId, label, null, false);
    }

    public static ButtonSpec success(String customId, String label) {
        return new ButtonSpec(ButtonStyle.SUCCESS, customId, label, null, false);
    }

    public static ButtonSpec danger(String customId, String label) {
        return new ButtonSpec(ButtonStyle.DANGER, customId, label, null, false);
    }

    public static ButtonSpec link(String url, String label) {
        return new ButtonSpec(ButtonStyle.LINK, null, label, url, false);
    }

    public ButtonSpec disable() {
        return disabled ? this : new ButtonSpec(style, customId, label, url, true);
    }

    public enum ButtonStyle {PRIMARY, SECONDARY, SUCCESS, DANGER, LINK}
}
