package io.github.chalkyjeans.taboobot.commands.slash.misc;

import io.github.chalkyjeans.taboobot.core.SlashCommand;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class PingCommand extends SlashCommand {

    public PingCommand() {
        this.setCommandData(Commands.slash("ping", "Pong!"));
        this.setEphemeral(true);
    }

    @Override
    public void executeCommand(SlashCommandInteractionEvent event) {
        JDA jda = event.getJDA();
        User user = event.getUser();
        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("Ping!")
                .setDescription(String.format(
                        """
                        **Gateway:** %s ms
                        **Rest:** %s ms
                        """, jda.getGatewayPing(), jda.getRestPing().complete()))
                .setColor(0x9F90CF)
                .setFooter(String.format("Requested by %s", user.getAsTag()), user.getEffectiveAvatarUrl())
                .setTimestamp(Instant.now());
        event.getHook().sendMessageEmbeds(embed.build()).queue();
    }

}
