require "spec"
require "../src/medusae"

describe Medusae::Client::SlashCommandRouter do
  it "responds pong to ping interactions" do
    response = [] of Medusae::Client::InteractionResponse
    router = Medusae::Client::SlashCommandRouter.new(->(payload : Medusae::Client::InteractionResponse) {
      response << payload
    })

    interaction = Medusae::Client::Interaction.new(id: "1", token: "abc", type: Medusae::Client::InteractionType::Ping)
    router.handle_interaction(interaction)

    response.size.should eq(1)
    response.first.type.should eq(Medusae::Client::ResponseType::Pong)
  end

  it "dispatches slash handlers by command name" do
    called = false
    router = Medusae::Client::SlashCommandRouter.new(->(_payload : Medusae::Client::InteractionResponse) { })
    router.register_slash_handler("ping") { called = true }

    interaction = Medusae::Client::Interaction.new(
      id: "1",
      token: "abc",
      type: Medusae::Client::InteractionType::ApplicationCommand,
      data: Medusae::Client::InteractionData.new(name: "ping"),
    )
    router.handle_interaction(interaction)

    called.should be_true
  end

  it "dispatches user context handlers by command name" do
    called = false
    router = Medusae::Client::SlashCommandRouter.new(->(_payload : Medusae::Client::InteractionResponse) { })
    router.register_user_context_menu_handler("profile") { called = true }

    interaction = Medusae::Client::Interaction.new(
      id: "1",
      token: "abc",
      type: Medusae::Client::InteractionType::ApplicationCommand,
      data: Medusae::Client::InteractionData.new(type: Medusae::Client::CommandType::UserContext, name: "profile"),
    )
    router.handle_interaction(interaction)

    called.should be_true
  end

  it "dispatches message context handlers by command name" do
    called = false
    router = Medusae::Client::SlashCommandRouter.new(->(_payload : Medusae::Client::InteractionResponse) { })
    router.register_message_context_menu_handler("quote") { called = true }

    interaction = Medusae::Client::Interaction.new(
      id: "1",
      token: "abc",
      type: Medusae::Client::InteractionType::ApplicationCommand,
      data: Medusae::Client::InteractionData.new(type: Medusae::Client::CommandType::MessageContext, name: "quote"),
    )
    router.handle_interaction(interaction)

    called.should be_true
  end

  it "dispatches handlers for keys with leading or trailing whitespace" do
    called = false
    router = Medusae::Client::SlashCommandRouter.new(->(_payload : Medusae::Client::InteractionResponse) { })
    router.register_component_handler("confirm") { called = true }

    interaction = Medusae::Client::Interaction.new(
      id: "1",
      token: "abc",
      type: Medusae::Client::InteractionType::MessageComponent,
      data: Medusae::Client::InteractionData.new(custom_id: "  confirm  "),
    )
    router.handle_interaction(interaction)

    called.should be_true
  end

  it "runs global component handler when key-specific handler is absent" do
    called = false
    router = Medusae::Client::SlashCommandRouter.new(->(_payload : Medusae::Client::InteractionResponse) { })
    router.register_global_component_handler { called = true }

    interaction = Medusae::Client::Interaction.new(
      id: "1",
      token: "abc",
      type: Medusae::Client::InteractionType::MessageComponent,
      data: Medusae::Client::InteractionData.new(custom_id: "unknown"),
    )
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
    interaction = Medusae::Client::Interaction.new(type: Medusae::Client::InteractionType::Ping)

    expect_raises(ArgumentError, /id and token/) do
      router.handle_interaction(interaction)
    end
  end
end
