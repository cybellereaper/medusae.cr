package com.github.cybellereaper.medusae.commands.discord.response;

import com.github.cybellereaper.medusae.client.*;
import com.github.cybellereaper.medusae.commands.core.execute.CommandResponder;
import com.github.cybellereaper.medusae.commands.core.response.*;
import com.github.cybellereaper.medusae.commands.core.response.component.ActionRowSpec;
import com.github.cybellereaper.medusae.commands.core.response.component.ButtonSpec;
import com.github.cybellereaper.medusae.commands.core.response.component.ComponentSpec;
import com.github.cybellereaper.medusae.commands.core.response.component.StringSelectSpec;
import com.github.cybellereaper.medusae.commands.core.response.embed.EmbedSpec;

import java.util.List;

public final class DiscordResponseApplier implements CommandResponder {
    private final InteractionContext context;

    public DiscordResponseApplier(InteractionContext context) {
        this.context = context;
    }

    private static List<DiscordEmbed> toEmbeds(List<EmbedSpec> specs) {
        return specs.stream().map(spec -> new DiscordEmbed(spec.title(), spec.description(), spec.color())
                        .withUrl(spec.url())
                        .withImage(spec.imageUrl())
                        .withThumbnail(spec.thumbnailUrl()))
                .toList();
    }

    private static List<DiscordActionRow> toRows(List<ActionRowSpec> rows) {
        return rows.stream()
                .map(row -> DiscordActionRow.of(row.components().stream().map(DiscordResponseApplier::toComponent).toList()))
                .toList();
    }

    private static DiscordComponent toComponent(ComponentSpec spec) {
        return switch (spec) {
            case ButtonSpec button -> switch (button.style()) {
                case PRIMARY -> toButton(DiscordButton.PRIMARY, button);
                case SECONDARY -> toButton(DiscordButton.SECONDARY, button);
                case SUCCESS -> toButton(DiscordButton.SUCCESS, button);
                case DANGER -> toButton(DiscordButton.DANGER, button);
                case LINK -> new DiscordButton(DiscordButton.LINK, button.label(), null, button.url(), null, button.disabled());
            };
            case StringSelectSpec select -> new DiscordStringSelectMenu(select.customId(), select.options().stream()
                    .map(option -> new DiscordSelectOption(option.label(), option.value(), option.description(), option.defaultSelected()))
                    .toList(), select.placeholder(), select.minValues(), select.maxValues(), select.disabled());
        };
    }

    private static DiscordButton toButton(int style, ButtonSpec button) {
        return new DiscordButton(style, button.label(), button.customId(), button.url(), null, button.disabled());
    }

    private static DiscordModal toModal(ModalReply modalReply) {
        List<DiscordActionRow> rows = modalReply.fields().stream().map(field -> {
            DiscordTextInput input = switch (field.style()) {
                case SHORT -> DiscordTextInput.shortInput(field.id(), field.label());
                case PARAGRAPH -> DiscordTextInput.paragraph(field.id(), field.label());
            };
            if (!field.required()) {
                input = input.optional();
            }
            return DiscordActionRow.of(List.of(input));
        }).toList();
        return DiscordModal.of(modalReply.customId(), modalReply.title(), rows);
    }

    @Override
    public void accept(CommandResponse response) {
        switch (response) {
            case ImmediateResponse immediateResponse -> {
                DiscordMessage message = DiscordMessage.ofContent(immediateResponse.content());
                context.respondWithMessage(immediateResponse.ephemeral() ? message.asEphemeral() : message);
            }
            case DeferredResponse ignored -> context.deferMessage();
            case FollowupResponse ignored -> throw new UnsupportedOperationException(
                    "Follow-up responses require webhook helpers not yet exposed by DiscordClient");
            case ModalReply modalReply -> context.respondWithModal(toModal(modalReply));
            case InteractionReply interactionReply -> applyInteractionReply(interactionReply);
        }
    }

    private void applyInteractionReply(InteractionReply reply) {
        switch (reply.mode()) {
            case DEFER_REPLY -> context.deferMessage();
            case DEFER_UPDATE -> context.deferUpdate();
            case UPDATE_MESSAGE -> context.respondWithUpdatedMessage(toDiscordMessage(reply));
            default -> context.respondWithMessage(toDiscordMessage(reply));
        }
    }

    private static DiscordMessage toDiscordMessage(InteractionReply reply) {
        return new DiscordMessage(
                reply.content(),
                toEmbeds(reply.embeds()),
                toRows(reply.components()),
                reply.isEphemeral()
        );
    }
}
