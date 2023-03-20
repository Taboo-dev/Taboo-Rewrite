package io.github.chalkyjeans.taboobot.commands.slash.misc;

import io.github.chalkyjeans.taboobot.bot.TabooBot;
import io.github.chalkyjeans.taboobot.core.SlashCommand;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import org.springframework.stereotype.Component;

@Component
public class HelpCommand extends SlashCommand {

    public HelpCommand() {
        this.setCommandData(Commands.slash("help", "Shows a list of commands"));
        this.setEphemeral(true);
    }

    @Override
    public void executeCommand(SlashCommandInteractionEvent event) {
        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("Commands List")
                .setDescription("**All commands:**\n")
                .setColor(0x9F90CF)
                .setTimestamp(event.getTimeCreated());
        TabooBot.getInstance().getShardManager().getShards().get(0).retrieveCommands()
                .queue(commands -> commands.forEach(command -> {
                    String format = String.format("%s - %s", command.getAsMention(), command.getDescription());
                    embed.appendDescription(format);
                }));
        event.getHook().sendMessageEmbeds(embed.build()).queue();
    }

}
