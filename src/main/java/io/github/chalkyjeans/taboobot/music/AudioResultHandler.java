package io.github.chalkyjeans.taboobot.music;

import io.github.chalkyjeans.taboobot.backend.TabooAPI;
import io.github.chalkyjeans.taboobot.util.objects.SearchHistory;
import lavalink.client.io.FriendlyException;
import lavalink.client.io.LoadResultHandler;
import lavalink.client.player.track.AudioPlaylist;
import lavalink.client.player.track.AudioTrack;
import lavalink.client.player.track.AudioTrackInfo;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

import java.awt.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class AudioResultHandler implements LoadResultHandler {

    private final SlashCommandInteractionEvent event;
    private final Member member;
    private final AudioScheduler scheduler;

    public AudioResultHandler(SlashCommandInteractionEvent event, Member member, GuildAudioPlayer player) {
        this.event = event;
        this.member = member;
        this.scheduler = player.getScheduler();
    }

    @Override
    public void trackLoaded(AudioTrack track) {
        scheduler.queue(track);
        handle(track);
        log.info("Track loaded: {}", track.getInfo().getTitle());
    }

    @Override
    public void playlistLoaded(AudioPlaylist playlist) {
        playlist.getTracks().forEach(scheduler::queue);
        handlePlaylist(playlist);
        log.info("Playlist loaded: {}", playlist.getName());
    }

    @Override
    public void searchResultLoaded(List<AudioTrack> tracks) {
        AudioTrack track = tracks.get(0);
        scheduler.queue(track);
        handle(track);
        log.info("Search result loaded: {}", track.getInfo().getTitle());
    }

    @Override
    public void noMatches() {
        MessageEmbed embed = new EmbedBuilder()
                .setTitle("No results found")
                .setColor(Color.RED)
                .setTimestamp(Instant.now())
                .build();
        event.getHook().sendMessageEmbeds(embed).queue();
        log.info("No matches found");
    }

    @Override
    public void loadFailed(FriendlyException exception) {
        MessageEmbed embed = new EmbedBuilder()
                .setTitle("An error occurred while loading the track")
                .setColor(Color.RED)
                .setTimestamp(Instant.now())
                .build();
        event.getHook().sendMessageEmbeds(embed).queue();
        log.error("An error occurred while loading the track: {}", exception.getMessage());
        exception.printStackTrace();
    }

    private void handle(AudioTrack track) {
        AudioTrackInfo info = track.getInfo();
        MessageEmbed embed = new EmbedBuilder()
                .setTitle("Added to queue:")
                .setDescription(String.format("[%s](%s) by %s", info.getTitle(), info.getUri(), info.getAuthor()))
                .setColor(0x9F90CF)
                .setTimestamp(Instant.now())
                .build();
        event.getHook().sendMessageEmbeds(embed).queue();
        TabooAPI.SearchHistories.saveSearchHistory(new SearchHistory(
                member.getIdLong(),
                info.getTitle(),
                info.getUri(),
                info.getIdentifier()
        ));
    }

    private void handlePlaylist(AudioPlaylist playlist) {
        StringBuilder description = new StringBuilder("Tracks:\n");
        int trackList = playlist.getTracks().size();
        int trackCount = Math.min(trackList, 10);
        for (int i = 0; i < trackCount; i++) {
            AudioTrack track = playlist.getTracks().get(i);
            AudioTrackInfo info = track.getInfo();
            description.append("`#")
                    .append(i + 1)
                    .append("` [")
                    .append(info.getTitle())
                    .append("](")
                    .append(info.getUri())
                    .append(") by ")
                    .append(info.getAuthor())
                    .append("\n");
        }
        if (trackList > trackCount) {
            description.append("And ")
                    .append("`")
                    .append(trackList - trackCount)
                    .append("`")
                    .append(" more tracks...");
        }
        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("Added to queue:")
                .setDescription(description)
                .setColor(0x9F90CF)
                .setTimestamp(Instant.now());
        if (trackList == 0) {
            embed.setDescription("The queue is empty.");
        } else {
            embed.setDescription(description);
        }
        event.getHook().sendMessageEmbeds(embed.build()).queue();
        List<SearchHistory> histories = new ArrayList<>();
        playlist.getTracks().forEach(audioTrack -> {
            AudioTrackInfo info = audioTrack.getInfo();
            histories.add(new SearchHistory(
                    member.getIdLong(),
                    info.getTitle(),
                    info.getUri(),
                    info.getIdentifier()
            ));
        });
        TabooAPI.SearchHistories.saveSearchHistory(histories);
    }

}
