package io.github.chalkyjeans.taboobot.core;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;

import java.util.List;
import java.util.Set;

public interface ICommand {

    CommandData getCommandData();
    boolean isGlobal();
    List<Long> getEnabledGuilds();
    List<Permission> getRequiredUserPermissions();
    List<Permission> getRequiredBotPermissions();
    Set<CommandFlag> getCommandFlags();

}
