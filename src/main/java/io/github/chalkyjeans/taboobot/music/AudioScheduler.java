package io.github.chalkyjeans.taboobot.music;

import io.github.chalkyjeans.taboobot.bot.TabooBot;
import lavalink.client.io.jda.JdaLink;
import lavalink.client.player.IPlayer;
import lavalink.client.player.LavalinkPlayer;
import lavalink.client.player.event.PlayerEventListenerAdapter;
import lavalink.client.player.track.AudioTrack;
import lavalink.client.player.track.AudioTrackEndReason;
import lavalink.client.player.track.AudioTrackInfo;
import lombok.Getter;
import lombok.Setter;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import net.dv8tion.jda.api.interactions.components.buttons.Button;

import java.awt.*;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

@Getter
public class AudioScheduler extends PlayerEventListenerAdapter {

    private final LavalinkPlayer player;
    private final JdaLink link;
    private final BlockingQueue<AudioTrack> queue;
    private final long guildId;
    @Setter private LoopState loopState;
    @Setter private long channelId;

    public AudioScheduler(LavalinkPlayer player, long guildId) {
        this.guildId = guildId;
        this.queue = new LinkedBlockingQueue<>();
        this.link = TabooBot.getInstance().getLavalink().getLink(guildId);
        this.player = link.getPlayer();
        this.loopState = LoopState.OFF;
        player.addListener(this);
    }

    public void queue(AudioTrack track) {
        if (player.getPlayingTrack() != null) {
            queue.offer(track);
        } else {
            player.playTrack(track);
        }
    }

    public void nextTrack() {
        AudioTrack track = queue.poll();
        if (track != null) {
            player.playTrack(track);
        } else {
            if (queue.size() == 0) {
                VoiceChannel channel = TabooBot.getInstance().getShardManager().getVoiceChannelById(channelId);
                MessageEmbed embed = new EmbedBuilder()
                        .setDescription("There are no more tracks in the queue.")
                        .setColor(Color.RED)
                        .setTimestamp(Instant.now())
                        .build();
                channel.sendMessageEmbeds(embed).queue();
            }
        }
    }

    public void skipTo(int index) {
        AudioTrack[] tracks = queue.toArray(new AudioTrack[0]);
        AudioTrack track = tracks[index];
        for (int i = 0; i < (index + 1); i++) {
            queue.poll();
        }
        if (track != null) {
            player.playTrack(track);
        }
    }

    public void shuffle() {
        AudioTrack[] tracks = queue.toArray(new AudioTrack[0]);
        for (int i = tracks.length - 1; i > 0; i--) {
            int index = (int) (Math.random() * (i + 1));
            AudioTrack tmp = tracks[index];
            tracks[index] = tracks[i];
            tracks[i] = tmp;
        }
        queue.clear();
        queue.addAll(List.of(tracks));
    }

    @Override
    public void onTrackStart(IPlayer player, AudioTrack track) {
        VoiceChannel channel = TabooBot.getInstance().getShardManager().getVoiceChannelById(channelId);
        AudioTrackInfo info = track.getInfo();
        long length = info.getLength();
        String duration = MusicUtil.toMinutesAndSeconds(length);
        boolean repeat = loopState == LoopState.TRACK || loopState == LoopState.QUEUE;
        if (!repeat) {
            EmbedBuilder embed = new EmbedBuilder()
                    .setTitle("Now Playing:")
                    .setDescription(String.format("[%s](%s) by %s", info.getTitle(), info.getUri(), info.getAuthor()))
                    .addField("Duration:", duration, true)
                    .setThumbnail(MusicUtil.getArtworkUrl(track))
                    .setColor(0x9F90CF)
                    .setTimestamp(Instant.now());
            String identifier = info.getIdentifier();
            String id = String.format("music:[]:%s:%s", channelId, identifier);
            channel.sendMessageEmbeds(embed.build()).addActionRow(
                    Button.secondary(id.replace("[]", "pause"), "Play/Pause"),
                    Button.secondary(id.replace("[]", "skip"), "Skip"),
                    Button.secondary(id.replace("[]", "loop"), "Loop"),
                    Button.secondary(id.replace("[]", "shuffle"), "Shuffle"),
                    Button.danger(id.replace("[]", "stop"), "Stop")
            ).addActionRow(
                    Button.primary(id.replace("[]", "bookmark"), "Bookmark")
            ).queue();
        }
    }

    @Override
    public void onTrackEnd(IPlayer player, AudioTrack track, AudioTrackEndReason endReason) {
        VoiceChannel channel = TabooBot.getInstance().getShardManager().getVoiceChannelById(channelId);
        if (endReason.mayStartNext) {
            AudioTrackInfo info = track.getInfo();
            String description = String.format("[%s](%s) by %s", info.getTitle(), info.getUri(), info.getAuthor());
            switch (loopState) {
                case OFF -> {
                    MessageEmbed embed = new EmbedBuilder()
                            .setTitle("Track Ended:")
                            .setDescription(description)
                            .setColor(0x9F90CF)
                            .setTimestamp(Instant.now())
                            .build();
                    channel.sendMessageEmbeds(embed).queue();
                    nextTrack();
                }
                case TRACK -> {
                    player.playTrack(track);
                    MessageEmbed embed = new EmbedBuilder()
                            .setTitle("Looping:")
                            .setDescription(description)
                            .setColor(0x9F90CF)
                            .setTimestamp(Instant.now())
                            .build();
                    channel.sendMessageEmbeds(embed).queue();
                }
                case QUEUE -> {
                    BlockingQueue<AudioTrack> clone = new LinkedBlockingQueue<>(queue);
                    queue.clear();
                    queue.offer(track);
                    queue.addAll(clone);

                }
            }
        }
    }

    @Override
    public void onTrackException(IPlayer player, AudioTrack track, Exception exception) {
        VoiceChannel channel = TabooBot.getInstance().getShardManager().getVoiceChannelById(channelId);
        AudioTrackInfo info = track.getInfo();
        MessageEmbed embed = new EmbedBuilder()
                .setTitle("An error occurred while playing the track:")
                .setDescription(info.getTitle())
                .setColor(Color.RED)
                .setTimestamp(Instant.now())
                .build();
        channel.sendMessageEmbeds(embed).queue();
    }

    public void destroy() {
        player.stopTrack();
        queue.clear();
    }

}
