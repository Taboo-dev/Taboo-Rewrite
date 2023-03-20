package io.github.chalkyjeans.taboobot.commands.slash.moderation;

import io.github.chalkyjeans.taboobot.core.SlashCommand;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
public class BanCommand extends SlashCommand {

    public BanCommand() {
        this.setCommandData(Commands.slash("ban", "Bans a user")
                .addOption(OptionType.USER, "user", "The user to ban", true)
                .addOption(OptionType.STRING, "reason", "The reason for the ban", true)
                .addOption(OptionType.INTEGER, "days", "The amount of days worth of messages to delete", false));
        this.setEphemeral(true);
    }

    @Override
    public void executeCommand(SlashCommandInteractionEvent event) {
        Member member = event.getMember();
        Member target = event.getOption("user").getAsMember();
        String reason = event.getOption("reason").getAsString();
        int days = event.getOption("days") != null ? event.getOption("days").getAsInt() : 0;
        target.ban(days, TimeUnit.DAYS).queue(unused -> {
            event.getHook().sendMessage(String.format("Banned %s for %s", target.getAsMention(), reason)).queue();
            target.getUser().openPrivateChannel()
                    .flatMap(channel -> channel.sendMessage(String.format("You have been banned from %s for %s", member.getGuild().getName(), reason)))
                    .queue();
        }, throwable -> event.getHook().sendMessage("Failed to ban user").queue());
    }

}
