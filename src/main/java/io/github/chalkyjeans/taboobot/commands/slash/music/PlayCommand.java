package io.github.chalkyjeans.taboobot.commands.slash.music;

import io.github.chalkyjeans.taboobot.backend.TabooAPI;
import io.github.chalkyjeans.taboobot.bot.TabooBot;
import io.github.chalkyjeans.taboobot.core.CommandFlag;
import io.github.chalkyjeans.taboobot.core.SlashCommand;
import io.github.chalkyjeans.taboobot.music.AudioResultHandler;
import io.github.chalkyjeans.taboobot.music.AudioScheduler;
import io.github.chalkyjeans.taboobot.music.GuildAudioPlayer;
import io.github.chalkyjeans.taboobot.music.MusicUtil;
import lavalink.client.io.jda.JdaLink;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.entities.GuildVoiceState;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.AutoCompleteQuery;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.managers.AudioManager;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
@Slf4j
public class PlayCommand extends SlashCommand {

    public PlayCommand() {
        this.setCommandData(Commands.slash("play", "Plays a song.").addOptions(
                new OptionData(OptionType.STRING, "song", "The song to play.", true, true),
                new OptionData(OptionType.STRING, "provider", "The provider to use. (Ignore if link)", false)
                        .addChoices(
                                new Command.Choice("Spotify (Default)", "scsearch"),
                                new Command.Choice("YouTube", "ytsearch"),
                                new Command.Choice("SoundCloud", "scsearch")
                        )
        ));
        this.addCommandFlags(CommandFlag.MUSIC);
        this.setEphemeral(false);
    }

    @Override
    public void executeCommand(SlashCommandInteractionEvent event) {
        GuildAudioPlayer player = TabooBot.getInstance().getAudioManager().getAudioPlayer(event.getGuild().getIdLong());
        AudioScheduler scheduler = player.getScheduler();
        JdaLink link = scheduler.getLink();
        String input = event.getOption("song").getAsString();
        OptionMapping providerOption = event.getOption("provider");
        Member member = event.getMember();
        GuildVoiceState voiceState = member.getVoiceState();
        AudioManager audioManager = event.getGuild().getAudioManager();
        String query;
        String provider;
        if (providerOption == null) {
            provider = "spsearch";
        } else {
            provider = providerOption.getAsString();
        }
        if (MusicUtil.isUrl(input)) {
            query = input;
        } else {
            query = String.format("%s:%s", provider, input);
        }
        if (audioManager.getConnectedChannel() == null) {
            scheduler.setChannelId(event.getChannel().getIdLong());
            link.connect(voiceState.getChannel().asVoiceChannel());
            link.getRestClient().loadItem(query, new AudioResultHandler(event, member, player));
        }
    }

    private final String bookmarkEmoji = "\uD83D\uDD16";
    private final String searchEmoji = "\uD83D\uDD0D";

    @Override
    public void handleAutoComplete(CommandAutoCompleteInteractionEvent event) {
        AutoCompleteQuery focusedOption = event.getFocusedOption();
        String value = focusedOption.getValue();
        Long userId = event.getUser().getIdLong();
        if (focusedOption.getName().equals("song")) {
            Set<Command.Choice> bookmarks;
            Set<Command.Choice> searchHistory;
            Set<Command.Choice> choices;
            if (value.isEmpty()) {
                bookmarks = TabooAPI.Bookmarks.getBookmarks(userId)
                                .stream()
                                .limit(25)
                                .map(bookmark -> new Command.Choice(String.format("%s %s", bookmarkEmoji, bookmark.name()), bookmark.url()))
                                .collect(Collectors.toSet());
                searchHistory = TabooAPI.SearchHistories.getSearchHistory(userId)
                                .stream()
                                .limit(25)
                                .map(history -> new Command.Choice(String.format("%s %s", searchEmoji, history.name()), history.url()))
                                .collect(Collectors.toSet());
                bookmarks.forEach(bookmark -> searchHistory.removeIf(history -> history.getAsString().equals(bookmark.getAsString())));
            } else {
                bookmarks = TabooAPI.Bookmarks.getBookmarks(userId)
                        .stream()
                        .filter(bookmark -> bookmark.name().toLowerCase().contains(value.toLowerCase()))
                        .limit(25)
                        .map(bookmark -> new Command.Choice(String.format("%s %s", bookmarkEmoji, bookmark.name()), bookmark.url()))
                        .collect(Collectors.toSet());
                searchHistory = TabooAPI.SearchHistories.getSearchHistory(userId)
                        .stream()
                        .filter(history -> history.name().toLowerCase().contains(value.toLowerCase()))
                        .limit(25)
                        .map(history -> new Command.Choice(String.format("%s %s", searchEmoji, history.name()), history.url()))
                        .collect(Collectors.toSet());
                bookmarks.forEach(bookmark -> searchHistory.removeIf(history -> history.getAsString().equals(bookmark.getAsString())));
            }
            choices = Stream.concat(bookmarks.stream(), searchHistory.stream()).collect(Collectors.toSet());
            event.replyChoices(choices).queue();
        }
    }

}
