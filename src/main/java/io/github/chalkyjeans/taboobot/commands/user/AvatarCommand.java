package io.github.chalkyjeans.taboobot.commands.user;

import io.github.chalkyjeans.taboobot.core.UserContextCommand;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.UserContextInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import org.springframework.stereotype.Component;

@Component
public class AvatarCommand extends UserContextCommand {

    public AvatarCommand() {
        this.setCommandData(Commands.user("Avatar"));
        this.setEphemeral(true);
    }

    @Override
    public void executeCommand(UserContextInteractionEvent event) {
        User target = event.getTarget();
        target.retrieveProfile().useCache(false).queue(profile -> {
            MessageEmbed embed = new EmbedBuilder()
                    .setTitle(String.format("%s's avatar", target.getName()))
                    .setImage(target.getEffectiveAvatarUrl())
                    .setColor(profile.getAccentColorRaw())
                    .setFooter(target.getAsTag() + " || " + target.getId())
                    .build();
            event.getHook().sendMessageEmbeds(embed).queue();
        });
    }

}
