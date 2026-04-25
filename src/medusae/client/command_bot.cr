module Medusae
  module Client
    module CommandBot
      macro included
        @router : Medusae::Client::SlashCommandRouter

        def initialize(responder : Medusae::Client::SlashCommandRouter::InteractionResponder)
          @router = Medusae::Client::SlashCommandRouter.new(responder)
          register_macro_handlers
          after_initialize
        end

        protected def after_initialize : Nil
        end

        def handle_interaction(interaction : Medusae::Client::Interaction?) : Nil
          @router.handle_interaction(interaction)
        end

        protected def respond_with_message(interaction : Medusae::Client::Interaction, content : String) : Nil
          @router.respond_with_message(interaction, content)
        end

        protected def defer_message(interaction : Medusae::Client::Interaction) : Nil
          @router.defer_message(interaction)
        end

        private def register_macro_handlers : Nil
          __register_generated_handlers
        end

        private def __register_generated_handlers : Nil
        end
      end

      private macro __define_keyed_handler(prefix, register_method, key, &block)
        {% method_name = "__medusae_#{prefix.id}_#{block.body.line_number}".id %}

        private def {{method_name}}(interaction : Medusae::Client::Interaction) : Nil
          {% if block.args.size > 0 %}
            ->({{block.args[0]}} : Medusae::Client::Interaction) { {{block.body}} }.call(interaction)
          {% else %}
            {{block.body}}
          {% end %}
        end

        private def __register_generated_handlers : Nil
          previous_def
          @router.{{register_method.id}}({{key}}) do |interaction|
            {{method_name}}(interaction)
          end
        end
      end

      private macro __define_global_handler(prefix, register_method, &block)
        {% method_name = "__medusae_#{prefix.id}_#{block.body.line_number}".id %}

        private def {{method_name}}(interaction : Medusae::Client::Interaction) : Nil
          {% if block.args.size > 0 %}
            ->({{block.args[0]}} : Medusae::Client::Interaction) { {{block.body}} }.call(interaction)
          {% else %}
            {{block.body}}
          {% end %}
        end

        private def __register_generated_handlers : Nil
          previous_def
          @router.{{register_method.id}} do |interaction|
            {{method_name}}(interaction)
          end
        end
      end

      macro slash_command(name, &block)
        __define_keyed_handler("slash", register_slash_handler, {{name}}) do {% if block.args.size > 0 %}|{{block.args.splat}}|{% end %}
          {{block.body}}
        end
      end

      macro user_context_command(name, &block)
        __define_keyed_handler("user_context", register_user_context_menu_handler, {{name}}) do {% if block.args.size > 0 %}|{{block.args.splat}}|{% end %}
          {{block.body}}
        end
      end

      macro message_context_command(name, &block)
        __define_keyed_handler("message_context", register_message_context_menu_handler, {{name}}) do {% if block.args.size > 0 %}|{{block.args.splat}}|{% end %}
          {{block.body}}
        end
      end

      macro component(custom_id, &block)
        __define_keyed_handler("component", register_component_handler, {{custom_id}}) do {% if block.args.size > 0 %}|{{block.args.splat}}|{% end %}
          {{block.body}}
        end
      end

      macro modal(custom_id, &block)
        __define_keyed_handler("modal", register_modal_handler, {{custom_id}}) do {% if block.args.size > 0 %}|{{block.args.splat}}|{% end %}
          {{block.body}}
        end
      end

      macro autocomplete(name, &block)
        __define_keyed_handler("autocomplete", register_autocomplete_handler, {{name}}) do {% if block.args.size > 0 %}|{{block.args.splat}}|{% end %}
          {{block.body}}
        end
      end

      macro global_component(&block)
        __define_global_handler("global_component", register_global_component_handler) do {% if block.args.size > 0 %}|{{block.args.splat}}|{% end %}
          {{block.body}}
        end
      end

      macro global_modal(&block)
        __define_global_handler("global_modal", register_global_modal_handler) do {% if block.args.size > 0 %}|{{block.args.splat}}|{% end %}
          {{block.body}}
        end
      end
    end
  end
end
