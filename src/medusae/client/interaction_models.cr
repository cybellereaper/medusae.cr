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
      type : CommandType? = nil do
      include JSON::Serializable

      def self.slash(name : String) : self
        new(name: name, type: CommandType::ChatInput)
      end

      def self.user_context(name : String) : self
        new(name: name, type: CommandType::UserContext)
      end

      def self.message_context(name : String) : self
        new(name: name, type: CommandType::MessageContext)
      end

      def self.component(custom_id : String) : self
        new(custom_id: custom_id)
      end
    end

    record Interaction,
      id : String? = nil,
      token : String? = nil,
      type : InteractionType = InteractionType::Ping,
      data : InteractionData? = nil do
      include JSON::Serializable

      def self.ping(id : String? = nil, token : String? = nil) : self
        new(id: id, token: token, type: InteractionType::Ping)
      end

      def self.slash_command(name : String, id : String? = nil, token : String? = nil) : self
        new(id: id, token: token, type: InteractionType::ApplicationCommand, data: InteractionData.slash(name))
      end

      def self.user_context(name : String, id : String? = nil, token : String? = nil) : self
        new(id: id, token: token, type: InteractionType::ApplicationCommand, data: InteractionData.user_context(name))
      end

      def self.message_context(name : String, id : String? = nil, token : String? = nil) : self
        new(id: id, token: token, type: InteractionType::ApplicationCommand, data: InteractionData.message_context(name))
      end

      def self.component(custom_id : String, id : String? = nil, token : String? = nil) : self
        new(id: id, token: token, type: InteractionType::MessageComponent, data: InteractionData.component(custom_id))
      end

      def self.modal_submit(custom_id : String, id : String? = nil, token : String? = nil) : self
        new(id: id, token: token, type: InteractionType::ModalSubmit, data: InteractionData.component(custom_id))
      end

      def self.autocomplete(name : String, id : String? = nil, token : String? = nil) : self
        new(id: id, token: token, type: InteractionType::ApplicationCommandAutocomplete, data: InteractionData.slash(name))
      end
    end

    record InteractionResponseData,
      content : String? = nil do
      include JSON::Serializable

      def self.message(content : String) : self
        new(content: content)
      end
    end

    record InteractionResponse,
      id : String,
      token : String,
      type : ResponseType,
      data : InteractionResponseData? = nil do
      include JSON::Serializable
    end
  end
end
