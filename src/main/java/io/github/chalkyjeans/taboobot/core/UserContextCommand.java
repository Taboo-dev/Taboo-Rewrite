package io.github.chalkyjeans.taboobot.core;

import lombok.Data;
import lombok.NoArgsConstructor;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.interaction.command.UserContextInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;

import java.util.List;
import java.util.Set;

@Data
@NoArgsConstructor
public abstract class UserContextCommand implements ICommand {

    private CommandData commandData;
    private List<Permission> requiredUserPermissions;
    private List<Permission> requiredBotPermissions;
    private boolean isGlobal;
    private boolean isEphemeral;
    private List<Long> enabledGuilds;
    private Set<CommandFlag> commandFlags;

    /**
     * Executes requested context menu command.
     *
     * @param event The UserContextInteractionEvent.
     */
    public abstract void executeCommand(UserContextInteractionEvent event);

}
