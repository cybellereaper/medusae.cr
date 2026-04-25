module Medusae
  module Client
    enum InteractionType
      Ping = 1
      ApplicationCommand = 2
      MessageComponent = 3
      ApplicationCommandAutocomplete = 4
      ModalSubmit = 5
    end

    enum CommandType
      ChatInput = 1
      UserContext = 2
      MessageContext = 3
    end

    enum ResponseType
      Pong = 1
      ChannelMessage = 4
      DeferredChannelMessage = 5
      DeferredMessageUpdate = 6
      UpdateMessage = 7
      Autocomplete = 8
      Modal = 9
    end

    record InteractionData,
      name : String? = nil,
      custom_id : String? = nil,
      type : CommandType? = nil

    record Interaction,
      id : String? = nil,
      token : String? = nil,
      type : InteractionType = InteractionType::Ping,
      data : InteractionData? = nil

    record InteractionResponseData,
      content : String? = nil do
      def self.message(content : String) : self
        new(content: content)
      end
    end

    record InteractionResponse,
      id : String,
      token : String,
      type : ResponseType,
      data : InteractionResponseData? = nil
  end
end
