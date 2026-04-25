require "spec"
require "../src/medusae"

describe Medusae::Client::SlashCommandRouter do
  it "responds pong to ping interactions" do
    response = [] of Medusae::Client::InteractionResponse
    router = Medusae::Client::SlashCommandRouter.new(->(payload : Medusae::Client::InteractionResponse) {
      response << payload
    })

    interaction = Medusae::Client::Interaction.ping(id: "1", token: "abc")
    router.handle_interaction(interaction)

    response.size.should eq(1)
    response.first.type.should eq(Medusae::Client::ResponseType::Pong)
  end

  it "dispatches slash handlers by command name" do
    called = false
    router = Medusae::Client::SlashCommandRouter.new(->(_payload : Medusae::Client::InteractionResponse) { })
    router.register_slash_handler("ping") { called = true }

    interaction = Medusae::Client::Interaction.slash_command("ping", id: "1", token: "abc")
    router.handle_interaction(interaction)

    called.should be_true
  end

  it "dispatches user context handlers by command name" do
    called = false
    router = Medusae::Client::SlashCommandRouter.new(->(_payload : Medusae::Client::InteractionResponse) { })
    router.register_user_context_menu_handler("profile") { called = true }

    interaction = Medusae::Client::Interaction.user_context("profile", id: "1", token: "abc")
    router.handle_interaction(interaction)

    called.should be_true
  end

  it "dispatches message context handlers by command name" do
    called = false
    router = Medusae::Client::SlashCommandRouter.new(->(_payload : Medusae::Client::InteractionResponse) { })
    router.register_message_context_menu_handler("quote") { called = true }

    interaction = Medusae::Client::Interaction.message_context("quote", id: "1", token: "abc")
    router.handle_interaction(interaction)

    called.should be_true
  end

  it "dispatches handlers for keys with leading or trailing whitespace" do
    called = false
    router = Medusae::Client::SlashCommandRouter.new(->(_payload : Medusae::Client::InteractionResponse) { })
    router.register_component_handler("confirm") { called = true }

    interaction = Medusae::Client::Interaction.component("  confirm  ", id: "1", token: "abc")
    router.handle_interaction(interaction)

    called.should be_true
  end

  it "runs global component handler when key-specific handler is absent" do
    called = false
    router = Medusae::Client::SlashCommandRouter.new(->(_payload : Medusae::Client::InteractionResponse) { })
    router.register_global_component_handler { called = true }

    interaction = Medusae::Client::Interaction.component("unknown", id: "1", token: "abc")
    router.handle_interaction(interaction)

    called.should be_true
  end

  it "rejects duplicate handlers" do
    router = Medusae::Client::SlashCommandRouter.new(->(_payload : Medusae::Client::InteractionResponse) { })
    router.register_component_handler("my-button") { }

    expect_raises(ArgumentError, /already registered/) do
      router.register_component_handler("my-button") { }
    end
  end

  it "requires interaction id and token when responding" do
    router = Medusae::Client::SlashCommandRouter.new(->(_payload : Medusae::Client::InteractionResponse) { })
    interaction = Medusae::Client::Interaction.ping

    expect_raises(ArgumentError, /id and token/) do
      router.handle_interaction(interaction)
    end
  end
end
