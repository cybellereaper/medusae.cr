package com.github.cybellereaper.medusae.commands.core.response;

import com.github.cybellereaper.medusae.commands.core.response.component.ActionRowSpec;
import com.github.cybellereaper.medusae.commands.core.response.embed.EmbedSpec;

import java.util.ArrayList;
import java.util.List;

public final class InteractionReply implements CommandResponse {
    private final ResponseMode mode;
    private final String content;
    private final boolean ephemeral;
    private final List<EmbedSpec> embeds;
    private final List<ActionRowSpec> components;
    private final boolean disableTriggeredComponent;
    private final boolean clearComponents;

    private InteractionReply(ResponseMode mode, String content, boolean ephemeral, List<EmbedSpec> embeds, List<ActionRowSpec> components,
                             boolean disableTriggeredComponent, boolean clearComponents) {
        this.mode = mode;
        this.content = content;
        this.ephemeral = ephemeral;
        this.embeds = List.copyOf(embeds);
        this.components = List.copyOf(components);
        this.disableTriggeredComponent = disableTriggeredComponent;
        this.clearComponents = clearComponents;
    }

    public static Builder create() {
        return new Builder(ResponseMode.REPLY);
    }

    public static Builder ephemeral() {
        return new Builder(ResponseMode.REPLY).ephemeral(true);
    }

    public static Builder updateMessage() {
        return new Builder(ResponseMode.UPDATE_MESSAGE);
    }

    public static Builder deferReply() {
        return new Builder(ResponseMode.DEFER_REPLY);
    }

    public static Builder deferUpdate() {
        return new Builder(ResponseMode.DEFER_UPDATE);
    }

    public static Builder followup() {
        return new Builder(ResponseMode.FOLLOWUP);
    }

    public static Builder editOriginal() {
        return new Builder(ResponseMode.EDIT_ORIGINAL);
    }

    public ResponseMode mode() {
        return mode;
    }

    public String content() {
        return content;
    }

    public boolean isEphemeral() {
        return ephemeral;
    }

    public List<EmbedSpec> embeds() {
        return embeds;
    }

    public List<ActionRowSpec> components() {
        return components;
    }

    public boolean disableTriggeredComponent() {
        return disableTriggeredComponent;
    }

    public boolean clearComponents() {
        return clearComponents;
    }

    public static final class Builder {
        private final ResponseMode mode;
        private final List<EmbedSpec> embeds = new ArrayList<>();
        private final List<ActionRowSpec> components = new ArrayList<>();
        private String content;
        private boolean ephemeral;
        private boolean disableTriggeredComponent;
        private boolean clearComponents;

        private Builder(ResponseMode mode) {
            this.mode = mode;
        }

        public Builder content(String value) {
            this.content = value;
            return this;
        }

        public Builder ephemeral(boolean value) {
            this.ephemeral = value;
            return this;
        }

        public Builder embed(EmbedSpec value) {
            this.embeds.add(value);
            return this;
        }

        public Builder embeds(List<EmbedSpec> value) {
            this.embeds.addAll(value);
            return this;
        }

        public Builder components(ActionRowSpec... rows) {
            this.components.addAll(List.of(rows));
            return this;
        }

        public Builder components(List<ActionRowSpec> rows) {
            this.components.addAll(rows);
            return this;
        }

        public Builder disableTriggeredComponent() {
            this.disableTriggeredComponent = true;
            return this;
        }

        public Builder clearComponents() {
            this.clearComponents = true;
            return this;
        }

        public InteractionReply build() {
            return new InteractionReply(mode, content, ephemeral, embeds, components, disableTriggeredComponent, clearComponents);
        }
    }
}
