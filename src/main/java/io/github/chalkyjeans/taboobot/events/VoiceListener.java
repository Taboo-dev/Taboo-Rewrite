package io.github.chalkyjeans.taboobot.events;

import io.github.chalkyjeans.taboobot.bot.TabooBot;
import io.github.chalkyjeans.taboobot.music.AudioScheduler;
import io.github.chalkyjeans.taboobot.music.GuildAudioPlayer;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import net.dv8tion.jda.api.entities.channel.middleman.AudioChannel;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceUpdateEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Component
public class VoiceListener extends ListenerAdapter {

    private final ScheduledExecutorService ses = Executors.newScheduledThreadPool(1);

    @Override
    public void onGuildVoiceUpdate(@NotNull GuildVoiceUpdateEvent event) {
        Guild guild = event.getGuild();
        if (!guild.getSelfMember().getVoiceState().isGuildDeafened()) {
            try {
                guild.deafen(guild.getSelfMember(), true).queue();
            } catch (IllegalStateException ignored) {}
        }
        JDA jda = event.getJDA();
        if (event.getMember().getIdLong() == guild.getSelfMember().getIdLong()) {
            AudioChannel voiceChannel = event.getChannelLeft();
            if (voiceChannel == null) {
                voiceChannel = event.getChannelJoined();
            }
            List<Member> members = voiceChannel.getMembers();
            GuildAudioPlayer guildAudioPlayer = TabooBot.getInstance().getAudioManager().getAudioPlayer(guild.getIdLong());
            AudioScheduler scheduler = guildAudioPlayer.getScheduler();
            MessageEmbed embed = new EmbedBuilder()
                    .setTitle("Voice Channel Left")
                    .setDescription(String.format("I have left the voice channel %s because I was alone.", voiceChannel.getAsMention()))
                    .setColor(0x9F90CF)
                    .setTimestamp(Instant.now())
                    .build();
            if ((members.size() == 1) && (members.get(0).getIdLong() == jda.getSelfUser().getIdLong())) {
                AudioChannel finalVoiceChannel = voiceChannel;
                ses.schedule(() -> {
                    if (finalVoiceChannel.getMembers().size() > 1) { // Check if there are still people in the voice channel
                        return; // Abort the SES
                    }
                    long channelId = scheduler.getChannelId();
                    VoiceChannel channel = guild.getVoiceChannelById(channelId);
                    scheduler.destroy();
                    jda.getDirectAudioController().disconnect(guild);
                    channel.sendMessageEmbeds(embed).queue();
                }, 2, TimeUnit.MINUTES);
            }
        }
    }

}
