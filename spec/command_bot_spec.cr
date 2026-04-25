require "spec"
require "../src/medusae"

private class MacroBotExample
  include Medusae::Client::CommandBot

  @events = [] of String

  getter events : Array(String)

  slash_command "ping" do |interaction|
    @events << "slash:#{interaction.data.try(&.name) || "unknown"}"
    respond_with_message(interaction, "pong")
  end

  component "confirm" do |_interaction|
    @events << "component:confirm"
  end

  global_component do
    @events << "component:global"
  end

  autocomplete "ping" do |_interaction|
    @events << "autocomplete:ping"
  end
end

describe Medusae::Client::CommandBot do
  it "registers macro-defined slash command handlers" do
    responses = [] of Medusae::Client::InteractionResponse
    bot = MacroBotExample.new(->(payload : Medusae::Client::InteractionResponse) {
      responses << payload
    })

    bot.handle_interaction(Medusae::Client::Interaction.new(
      id: "1",
      token: "abc",
      type: Medusae::Client::InteractionType::ApplicationCommand,
      data: Medusae::Client::InteractionData.new(name: "ping"),
    ))

    bot.events.should eq(["slash:ping"])
    responses.size.should eq(1)
    responses.first.type.should eq(Medusae::Client::ResponseType::ChannelMessage)
    responses.first.data.should_not be_nil
    responses.first.data.not_nil!.content.should eq("pong")
  end

  it "prefers exact component handlers over global handlers" do
    bot = MacroBotExample.new(->(_payload : Medusae::Client::InteractionResponse) { })

    bot.handle_interaction(Medusae::Client::Interaction.new(
      id: "1",
      token: "abc",
      type: Medusae::Client::InteractionType::MessageComponent,
      data: Medusae::Client::InteractionData.new(custom_id: "confirm"),
    ))

    bot.events.should eq(["component:confirm"])
  end

  it "falls back to global component handlers" do
    bot = MacroBotExample.new(->(_payload : Medusae::Client::InteractionResponse) { })

    bot.handle_interaction(Medusae::Client::Interaction.new(
      id: "1",
      token: "abc",
      type: Medusae::Client::InteractionType::MessageComponent,
      data: Medusae::Client::InteractionData.new(custom_id: "missing"),
    ))

    bot.events.should eq(["component:global"])
  end

  it "supports autocomplete handlers defined with macros" do
    bot = MacroBotExample.new(->(_payload : Medusae::Client::InteractionResponse) { })

    bot.handle_interaction(Medusae::Client::Interaction.new(
      id: "1",
      token: "abc",
      type: Medusae::Client::InteractionType::ApplicationCommandAutocomplete,
      data: Medusae::Client::InteractionData.new(name: "ping"),
    ))

    bot.events.should eq(["autocomplete:ping"])
  end

  it "rejects duplicate macro command registration" do
    expect_raises(ArgumentError, /already registered/) do
      DuplicateMacroBot.new(->(_payload : Medusae::Client::InteractionResponse) { })
    end
  end
end

private class DuplicateMacroBot
  include Medusae::Client::CommandBot

  slash_command "ping" do
  end

  slash_command "ping" do
  end
end
