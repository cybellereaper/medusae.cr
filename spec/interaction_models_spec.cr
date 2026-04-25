require "spec"
require "../src/medusae"

describe Medusae::Client::Interaction do
  it "builds typed slash command interactions with factory helpers" do
    interaction = Medusae::Client::Interaction.slash_command("ping", id: "1", token: "abc")

    interaction.type.should eq(Medusae::Client::InteractionType::ApplicationCommand)
    interaction.data.should_not be_nil
    interaction.data.not_nil!.type.should eq(Medusae::Client::CommandType::ChatInput)
    interaction.data.not_nil!.name.should eq("ping")
  end

  it "builds typed component and modal interactions with factory helpers" do
    component = Medusae::Client::Interaction.component("confirm", id: "1", token: "abc")
    modal = Medusae::Client::Interaction.modal_submit("profile-modal", id: "2", token: "def")

    component.type.should eq(Medusae::Client::InteractionType::MessageComponent)
    component.data.not_nil!.custom_id.should eq("confirm")

    modal.type.should eq(Medusae::Client::InteractionType::ModalSubmit)
    modal.data.not_nil!.custom_id.should eq("profile-modal")
  end
end

describe Medusae::Client::InteractionResponse do
  it "serializes strongly typed response payloads as json" do
    response = Medusae::Client::InteractionResponse.new(
      id: "1",
      token: "abc",
      type: Medusae::Client::ResponseType::ChannelMessage,
      data: Medusae::Client::InteractionResponseData.message("hello"),
    )

    json = response.to_json
    json.should contain("\"content\":\"hello\"")
  end
end
