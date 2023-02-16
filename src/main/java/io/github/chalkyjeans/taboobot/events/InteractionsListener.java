package io.github.chalkyjeans.taboobot.events;

import io.github.chalkyjeans.taboobot.bot.TabooBot;
import io.github.chalkyjeans.taboobot.config.TabooConfig;
import io.github.chalkyjeans.taboobot.util.ResponseHelper;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.MessageContextInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.UserContextInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import java.awt.*;

@Component
public class InteractionsListener extends ListenerAdapter {

    private final TabooConfig config;

    public InteractionsListener(TabooConfig config) {
        this.config = config;
    }

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        if (!event.isFromGuild()) return;
        if (!config.isProduction() && !(config.getOwnerId() == event.getUser().getIdLong())) {
            event.replyEmbeds(ResponseHelper.createEmbed(null, "I am in debug mode! Only my owner can use commands!",
                    Color.RED, event.getUser()).build()).setEphemeral(true).queue();
        } else {
            TabooBot.getInstance().getInteractionCommandHandler().handleSlashCommand(event, event.getMember());
        }
    }

    @Override
    public void onMessageContextInteraction(@NotNull MessageContextInteractionEvent event) {
        if (!event.isFromGuild()) return;
        if (!config.isProduction() && !(config.getOwnerId() == (event.getUser().getIdLong()))) {
            event.replyEmbeds(ResponseHelper.createEmbed(null, "I am in debug mode! Only my owner can use commands!",
                    Color.RED, event.getUser()).build()).setEphemeral(true).queue();
        } else {
            TabooBot.getInstance().getInteractionCommandHandler().handleMessageContextCommand(event);
        }
    }

    @Override
    public void onUserContextInteraction(@NotNull UserContextInteractionEvent event) {
        if (!event.isFromGuild()) return;
        if (!config.isProduction() && !(config.getOwnerId() == (event.getUser().getIdLong()))) {
            event.replyEmbeds(ResponseHelper.createEmbed(null, "I am in debug mode! Only my owner can use commands!",
                    Color.RED, event.getUser()).build()).setEphemeral(true).queue();
        } else {
            TabooBot.getInstance().getInteractionCommandHandler().handleUserContextCommand(event);
        }
    }

    @Override
    public void onCommandAutoCompleteInteraction(@NotNull CommandAutoCompleteInteractionEvent event) {
        if (!event.isFromGuild()) return;
        TabooBot.getInstance().getInteractionCommandHandler().handleAutoComplete(event);
    }

}
