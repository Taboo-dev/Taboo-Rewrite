package io.github.chalkyjeans.taboobot.core;

import lombok.Data;
import lombok.NoArgsConstructor;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Data
@NoArgsConstructor
public abstract class SlashCommand implements ICommand {

    private SlashCommandData commandData;
    private List<Permission> requiredUserPermissions = new ArrayList<>();
    private List<Permission> requiredBotPermissions = new ArrayList<>();
    private boolean isGlobal = false;
    private boolean isEphemeral = false;
    private List<Long> enabledGuilds = new ArrayList<>();
    private final Set<CommandFlag> commandFlags = new HashSet<>();

    public void addCommandFlags(CommandFlag... flags) {
        commandFlags.addAll(List.of(flags));
    }

    public abstract void executeCommand(SlashCommandInteractionEvent event);

    public void handleAutoComplete(CommandAutoCompleteInteractionEvent event) {}

}