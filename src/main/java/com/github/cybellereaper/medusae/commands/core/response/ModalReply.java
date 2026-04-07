package com.github.cybellereaper.medusae.commands.core.response;

import java.util.ArrayList;
import java.util.List;

public final class ModalReply implements CommandResponse {
    private final String customId;
    private final String title;
    private final List<ModalField> fields;

    private ModalReply(String customId, String title, List<ModalField> fields) {
        this.customId = customId;
        this.title = title;
        this.fields = List.copyOf(fields);
    }

    public static Builder create(String customId) {
        return new Builder(customId);
    }

    public String customId() {
        return customId;
    }

    public String title() {
        return title;
    }

    public List<ModalField> fields() {
        return fields;
    }

    public enum ModalFieldStyle {SHORT, PARAGRAPH}

    public static final class Builder {
        private final String customId;
        private final List<ModalField> fields = new ArrayList<>();
        private String title;

        private Builder(String customId) {
            this.customId = customId;
        }

        public Builder title(String title) {
            this.title = title;
            return this;
        }

        public Builder textInput(String id, String label, boolean required) {
            fields.add(new ModalField(id, label, ModalFieldStyle.SHORT, required));
            return this;
        }

        public Builder paragraphInput(String id, String label, boolean required) {
            fields.add(new ModalField(id, label, ModalFieldStyle.PARAGRAPH, required));
            return this;
        }

        public ModalReply build() {
            return new ModalReply(customId, title, fields);
        }
    }

    public record ModalField(String id, String label, ModalFieldStyle style, boolean required) {
    }
}
