package io.github.chalkyjeans.taboobot.core;

import lombok.Data;
import lombok.NoArgsConstructor;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.interaction.command.MessageContextInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Data
@NoArgsConstructor
public abstract class MessageContextCommand implements ICommand {

    private CommandData commandData;
    private List<Permission> requiredUserPermissions = new ArrayList<>();
    private List<Permission> requiredBotPermissions = new ArrayList<>();
    private boolean isGlobal = false;
    private boolean isEphemeral = false;
    private List<Long> enabledGuilds = new ArrayList<>();
    private Set<CommandFlag> commandFlags = new HashSet<>();

    /**
     * Executes the requested context menu command
     * @param event The MessageContextInteractionEvent
     */
    public abstract void executeCommand(MessageContextInteractionEvent event);

}
