package io.github.chalkyjeans.taboobot.music;

import io.github.chalkyjeans.taboobot.bot.TabooBot;
import lavalink.client.io.jda.JdaLink;
import lavalink.client.player.LavalinkPlayer;
import lombok.Getter;
import net.dv8tion.jda.api.entities.Guild;

@Getter
public class GuildAudioPlayer {

    private final long guildId;
    private final LavalinkPlayer player;
    private final AudioScheduler scheduler;
    private final JdaLink link;

    public GuildAudioPlayer(long guildId) {
        Guild guild = TabooBot.getInstance().getShardManager().getGuildById(guildId);
        this.guildId = guild.getIdLong();
        this.link = TabooBot.getInstance().getLavalink().getLink(guild);
        this.player = link.getPlayer();
        this.scheduler = new AudioScheduler(player, guildId);
    }

    public void destroy() {
        link.destroy();
        scheduler.destroy();
    }

}
