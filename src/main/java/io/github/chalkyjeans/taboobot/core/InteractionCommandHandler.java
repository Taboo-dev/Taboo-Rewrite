package io.github.chalkyjeans.taboobot.core;

import io.github.chalkyjeans.taboobot.TabooBotApplication;
import io.github.chalkyjeans.taboobot.bot.TabooBot;
import io.github.chalkyjeans.taboobot.config.TabooConfig;
import io.github.chalkyjeans.taboobot.util.ResponseHelper;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.GuildVoiceState;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.events.interaction.command.*;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.managers.AudioManager;
import net.dv8tion.jda.api.requests.restaction.CommandListUpdateAction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import static io.github.chalkyjeans.taboobot.util.Constants.SUPPORT_SERVER_URL;

@Slf4j
@Getter
@Component
public class InteractionCommandHandler {

    private final TabooConfig config;
    private final ApplicationContext context;
    private final List<ICommand> commands;
    @Getter
    private final ConcurrentHashMap<Long, List<ICommand>> registeredGuildCommands;
    private CommandListUpdateAction commandListUpdateAction;

    public InteractionCommandHandler(TabooConfig config) {
        this.config = config;
        this.context = TabooBotApplication.getProvider().getApplicationContext();
        this.commands = Collections.synchronizedList(new ArrayList<>());
        this.registeredGuildCommands = new ConcurrentHashMap<>();
    }

    public void start() {
        this.commandListUpdateAction = TabooBot.getInstance().getShardManager().getShards().get(0).updateCommands();
        registerCommands();
    }

    public void startCommandCheck() {
        log.info("Checking for outdated commands...");
        TabooBot.getInstance().getCommandExecutor().submit(() -> {
            if (!config.isProduction()) {
                Guild guild = TabooBot.getInstance().getShardManager().getGuildById(config.getGuildId());
                if (guild == null) {
                    log.error("Debug guild does not exist!");
                    return;
                }
                guild.retrieveCommands().queue(discordCommands -> {
                    List<ICommand> localCommands = this.getRegisteredGuildCommands()
                            .get(guild.getIdLong());
                    handleCommandUpdates(discordCommands, localCommands);
                });
                return;
            }
            TabooBot.getInstance().getShardManager().getShards().get(0).retrieveCommands().queue(discordCommands -> {
                List<ICommand> localCommands = this.getCommands()
                        .stream()
                        .filter(ICommand::isGlobal)
                        .toList();
                handleCommandUpdates(discordCommands, localCommands);
            });
        });
    }

    private void handleCommandUpdates(List<? extends Command> discordCommands, List<? extends ICommand> localCommands) {
        if (localCommands == null || localCommands.isEmpty()) {
            log.warn("No commands have been registered!");
            return;
        }
        boolean commandRemovedOrAdded = localCommands.size() != discordCommands.size();
        if (commandRemovedOrAdded) {
            if (localCommands.size() > discordCommands.size()) {
                log.warn("New command(s) has/have been added! Updating Discord's cache...");
            } else {
                log.warn("Command(s) has/have been removed! Updating Discord's cache...");
            }
            this.updateCommands(commands -> log.info("Updated {} commands!", commands.size()), null);
            return;
        }
        AtomicBoolean outdated = new AtomicBoolean(false);
        localCommands.forEach(localCommand -> {
            Command discordCommand = discordCommands.stream()
                    .filter(command -> command.getName().equalsIgnoreCase(localCommand.getCommandData().getName()))
                    .findFirst()
                    .orElse(null);
            CommandData localCommandData = localCommand.getCommandData();
            CommandData discordCommandData = CommandData.fromCommand(discordCommand);
            if (!localCommandData.equals(discordCommandData)) {
                outdated.set(true);
            }
        });
        if (outdated.get()) {
            this.updateCommands(commands -> log.info("Updated {} commands!", commands.size()), null);
        } else {
            log.info("No outdated commands found!");
        }
    }

    private void registerCommands() {
        log.info("Registering commands...");
        context.getBeansOfType(ICommand.class).values().forEach(this::registerCommand);
    }

    private void registerCommand(ICommand command) {
        if (command == null) {
            return;
        }
        log.info("Registering command: {}", command.getCommandData().getName());
        if (!command.isGlobal() && config.isProduction()) {
            if (command.getEnabledGuilds() == null || command.getEnabledGuilds().isEmpty()) {
                return;
            }
            command.getEnabledGuilds().forEach(guildId -> {
                Guild guild = TabooBot.getInstance().getShardManager().getGuildById(guildId);
                List<ICommand> alreadyRegistered = registeredGuildCommands.containsKey(guildId) ?
                        registeredGuildCommands.get(guildId) : new ArrayList<>();
                alreadyRegistered.add(command);
                registeredGuildCommands.put(guildId, alreadyRegistered);
            });
            return;
        }
        if (!config.isProduction()) {
            Guild guild = TabooBot.getInstance().getShardManager().getGuildById(config.getGuildId());
            if (guild != null) {
                List<ICommand> alreadyRegistered = registeredGuildCommands.containsKey(guild.getIdLong()) ?
                        registeredGuildCommands.get(guild.getIdLong()) : new ArrayList<>();
                alreadyRegistered.add(command);
                registeredGuildCommands.put(guild.getIdLong(), alreadyRegistered);
            }
            return;
        }
        commandListUpdateAction.addCommands(command.getCommandData());
        commands.add(command);
    }

    public void updateCommands(@Nullable Consumer<List<Command>> success, @Nullable Consumer<Throwable> failure) {
        if (config.isProduction()) {
            commandListUpdateAction.queue(success, failure);
            registeredGuildCommands.forEach((guildId, slashCommands) -> {
                Guild guild = TabooBot.getInstance().getShardManager().getGuildById(guildId);
                CommandListUpdateAction updateAction = guild.updateCommands();
                slashCommands.forEach(command -> {
                    updateAction.addCommands(command.getCommandData());
                });
                if (slashCommands.size() > 0) {
                    updateAction.queue();
                }
            });
        } else {
            List<ICommand> commands = registeredGuildCommands.get(config.getGuildId());
            if (commands != null && !commands.isEmpty()) {
                Guild guild = TabooBot.getInstance().getShardManager().getGuildById(config.getGuildId());
                CommandListUpdateAction commandListUpdateAction = guild.updateCommands();
                commands.forEach(command -> {
                    commandListUpdateAction.addCommands(command.getCommandData());
                });
                commandListUpdateAction.queue();
            }
        }
    }

    public void handleAutoComplete(@NotNull CommandAutoCompleteInteractionEvent event) {
        if (event.getGuild() == null) return;
        Runnable r = () -> {
            try {
                SlashCommand command = null;
                long guildId = event.getGuild().getIdLong();
                if (registeredGuildCommands.containsKey(guildId)) {
                    List<SlashCommand> guildCommands = registeredGuildCommands.get(guildId)
                            .stream()
                            .filter(cmd -> cmd instanceof SlashCommand)
                            .map(cmd -> (SlashCommand) cmd)
                            .toList();
                    SlashCommand guildCommand = guildCommands
                            .stream()
                            .filter(cmd -> cmd.getCommandData().getName().equalsIgnoreCase(event.getName()))
                            .findFirst()
                            .orElse(null);
                    if (guildCommand != null)
                        command = guildCommand;
                }
                if (command == null) {
                    SlashCommand globalCommand = commands
                            .stream()
                            .filter(cmd -> cmd instanceof SlashCommand)
                            .map(cmd -> (SlashCommand) cmd)
                            .filter(cmd -> cmd.getCommandData().getName().equalsIgnoreCase(event.getName()))
                            .findFirst()
                            .orElse(null);
                    if (globalCommand != null)
                        command = globalCommand;
                }
                if (command != null)
                    command.handleAutoComplete(event);
            } catch (Exception e) {
                log.warn("Error while handling auto complete", e);
                event.replyChoices(Collections.emptyList()).queue();
            }
        };
        TabooBot.getInstance().getCommandExecutor().execute(r);
    }

    public void handleMessageContextCommand(@NotNull MessageContextInteractionEvent event) {
        if (!event.isFromGuild()) return;
        Guild guild = event.getGuild();
        Member member = event.getMember();
        MessageContextCommand command = null;
        if (registeredGuildCommands.containsKey(guild.getIdLong())) {
            List<MessageContextCommand> guildCommands = registeredGuildCommands.get(guild.getIdLong())
                    .stream()
                    .filter(cmd -> cmd instanceof MessageContextCommand)
                    .map(cmd -> (MessageContextCommand) cmd)
                    .toList();
            MessageContextCommand guildCommand = guildCommands
                    .stream()
                    .filter(cmd -> cmd.getCommandData().getName().equalsIgnoreCase(event.getName()))
                    .findFirst()
                    .orElse(null);
            if (guildCommand != null)
                command = guildCommand;
        }
        if (command == null) {
            MessageContextCommand globalCommand = getRegisteredMessageContextCommands()
                    .stream()
                    .filter(cmd -> cmd.getCommandData().getName().equalsIgnoreCase(event.getName()))
                    .findFirst()
                    .orElse(null);
            if (globalCommand != null)
                command = globalCommand;
        }
        if (command == null) return;
        handleCommandPermissions(command, event, member);
        MessageContextCommand finalCommand = command;
        Runnable r = () -> {
            try {
                event.deferReply(finalCommand.isEphemeral()).queue();
                finalCommand.executeCommand(event);
            } catch (Exception e) {
                log.warn("Error while executing command", e);
                EmbedBuilder embed = ResponseHelper.createEmbed(null, "An error occurred while executing this command. " +
                        "This has automatically been reported to the developer.", Color.RED, null);
                ActionRow row = ActionRow.of(Button.link(SUPPORT_SERVER_URL, "Support Server"));
                if (event.isAcknowledged()) {
                    event.getHook().sendMessageEmbeds(embed.build()).addComponents(row).setEphemeral(true).queue();
                } else {
                    event.replyEmbeds(embed.build()).addComponents(row).setEphemeral(true).queue();
                }
            }
        };
        TabooBot.getInstance().getCommandExecutor().submit(r);
    }

    public void handleUserContextCommand(@NotNull UserContextInteractionEvent event) {
        if (!event.isFromGuild()) return;
        Guild guild = event.getGuild();
        Member member = event.getMember();
        UserContextCommand command = null;
        if (registeredGuildCommands.containsKey(guild.getIdLong())) {
            List<UserContextCommand> guildCommands = registeredGuildCommands.get(guild.getIdLong())
                    .stream()
                    .filter(cmd -> cmd instanceof UserContextCommand)
                    .map(cmd -> (UserContextCommand) cmd)
                    .toList();
            UserContextCommand guildCommand = guildCommands
                    .stream()
                    .filter(cmd -> cmd.getCommandData().getName().equalsIgnoreCase(event.getName()))
                    .findFirst()
                    .orElse(null);
            if (guildCommand != null)
                command = guildCommand;
        }
        if (command == null) {
            UserContextCommand globalCommand = getRegisteredUserContextCommands()
                    .stream()
                    .filter(cmd -> cmd.getCommandData().getName().equalsIgnoreCase(event.getName()))
                    .findFirst()
                    .orElse(null);
            if (globalCommand != null)
                command = globalCommand;
        }
        if (command == null)
            return;
        handleCommandPermissions(command, event, member);
        UserContextCommand finalCommand = command;
        Runnable r = () -> {
            try {
                event.deferReply(finalCommand.isEphemeral()).queue();
                finalCommand.executeCommand(event);
            } catch (Exception e) {
                log.warn("An error occurred while executing a user context command.", e);
                EmbedBuilder embed = ResponseHelper.createEmbed(null, "An error occurred while executing this command. " +
                        "This has automatically been reported to the developer.", Color.RED, null);
                ActionRow row = ActionRow.of(Button.link(SUPPORT_SERVER_URL, "Support Server"));
                if (event.isAcknowledged()) {
                    event.getHook().sendMessageEmbeds(embed.build()).addComponents(row).setEphemeral(true).queue();
                } else {
                    event.replyEmbeds(embed.build()).addComponents(row).setEphemeral(true).queue();
                }
            }
        };
        TabooBot.getInstance().getCommandExecutor().submit(r);
    }

    public void handleSlashCommand(SlashCommandInteractionEvent event, Member member) {
        Runnable r = () -> {
            try {
                if (!event.isFromGuild()) return;
                Guild guild = event.getGuild();
                SlashCommand command = null;
                long guildId = guild.getIdLong();
                if (registeredGuildCommands.containsKey(guildId)) {
                    List<SlashCommand> guildCommands = registeredGuildCommands.get(guildId)
                            .stream()
                            .filter(cmd -> cmd instanceof SlashCommand)
                            .map(cmd -> (SlashCommand) cmd)
                            .toList();
                    SlashCommand guildCommand = guildCommands
                            .stream()
                            .filter(cmd -> cmd.getCommandData().getName().equalsIgnoreCase(event.getName()))
                            .findFirst()
                            .orElse(null);
                    if (guildCommand != null)
                        command = guildCommand;
                }
                if (command == null) {
                    SlashCommand globalCommand = getRegisteredSlashCommands()
                            .stream()
                            .filter(cmd -> cmd.getCommandData().getName().equalsIgnoreCase(event.getName()))
                            .findFirst()
                            .orElse(null);
                    if (globalCommand != null)
                        command = globalCommand;
                }
                if (command != null) {
                    handleCommandPermissions(command, event, member);
                    event.deferReply(command.isEphemeral()).queue();
                    command.executeCommand(event);
                }
            } catch (Exception e) {
                log.warn("Error while executing slash command", e);
                EmbedBuilder embed = ResponseHelper.createEmbed(null, "An error occurred while executing this command. " +
                        "This has automatically been reported to the developer.", Color.RED, null);
                ActionRow row = ActionRow.of(Button.link(SUPPORT_SERVER_URL, "Support Server"));
                if (event.isAcknowledged()) {
                    event.getHook().sendMessageEmbeds(embed.build()).addComponents(row).setEphemeral(true).queue();
                } else {
                    event.replyEmbeds(embed.build()).addComponents(row).setEphemeral(true).queue();
                }
            }
        };
        TabooBot.getInstance().getCommandExecutor().execute(r);
    }

    private void handleCommandPermissions(ICommand command, GenericCommandInteractionEvent event, Member member) {
        List<Permission> requiredUserPermissions = command.getRequiredUserPermissions();
        List<Permission> requiredBotPermissions = command.getRequiredBotPermissions();
        if (requiredUserPermissions != null && !member.hasPermission(requiredUserPermissions)) {
            EmbedBuilder embed = ResponseHelper.createEmbed(null, "You don't have the required permissions to execute this command.",
                    Color.RED, null);
            event.replyEmbeds(embed.build()).setEphemeral(true).queue();
            return;
        }
        if (requiredBotPermissions != null && !event.getGuild().getSelfMember().hasPermission(requiredBotPermissions)) {
            EmbedBuilder embed = ResponseHelper.createEmbed(null, "I don't have the required permissions to execute this command.",
                    Color.RED, null);
            event.replyEmbeds(embed.build()).setEphemeral(true).queue();
            return;
        }
        if (command.getCommandFlags() == null || command.getCommandFlags().isEmpty()) return;
        if (command.getCommandFlags().contains(CommandFlag.MUSIC)) {
            if (event.getChannelType().equals(ChannelType.VOICE)) {
                GuildVoiceState guildVoiceState = member.getVoiceState();
                if (guildVoiceState == null || !guildVoiceState.inAudioChannel()) {
                    EmbedBuilder embed = ResponseHelper.createEmbed(null, "You must be in a voice channel to execute this command.",
                            Color.RED, null);
                    event.replyEmbeds(embed.build()).setEphemeral(true).queue();
                    return;
                }
                AudioManager manager = event.getGuild().getAudioManager();
                if (manager.isConnected()) {
                    if (!manager.getConnectedChannel().equals(guildVoiceState.getChannel())) {
                        EmbedBuilder embed = ResponseHelper.createEmbed(null, "You must be in the same voice channel as me to execute this command.",
                                Color.RED, null);
                        event.replyEmbeds(embed.build()).setEphemeral(true).queue();
                        return;
                    }
                }
            } else {
                EmbedBuilder embed = ResponseHelper.createEmbed(null, "You can only execute this command in voice channels.",
                        Color.RED, null);
                event.replyEmbeds(embed.build()).setEphemeral(true).queue();
                return;
            }
        }
        if (command.getCommandFlags().contains(CommandFlag.DEVELOPER_ONLY)) {
            long idLong = event.getUser().getIdLong();
            if (idLong != config.getOwnerId()) {
                EmbedBuilder embed = ResponseHelper.createEmbed(null, "This command is only available for the developer.",
                        Color.RED, null);
                event.replyEmbeds(embed.build()).setEphemeral(true).queue();
                return;
            }
        }
        if (command.getCommandFlags().contains(CommandFlag.DISABLED)) {
            EmbedBuilder embed = ResponseHelper.createEmbed(null, "This command is currently disabled.",
                    Color.RED, null);
            event.replyEmbeds(embed.build()).setEphemeral(true).queue();
        }
    }

    public List<SlashCommand> getRegisteredSlashCommands() {
        return commands
                .stream()
                .filter(cmd -> cmd instanceof SlashCommand)
                .map(cmd -> (SlashCommand) cmd)
                .toList();
    }

    public List<MessageContextCommand> getRegisteredMessageContextCommands() {
        return commands
                .stream()
                .filter(cmd -> cmd instanceof MessageContextCommand)
                .map(cmd -> (MessageContextCommand) cmd)
                .toList();
    }

    public List<UserContextCommand> getRegisteredUserContextCommands() {
        return commands
                .stream()
                .filter(cmd -> cmd instanceof UserContextCommand)
                .map(cmd -> (UserContextCommand) cmd)
                .toList();
    }

}
