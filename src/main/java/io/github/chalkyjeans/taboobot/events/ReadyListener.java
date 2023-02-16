package io.github.chalkyjeans.taboobot.events;

import io.github.chalkyjeans.taboobot.bot.TabooBot;
import io.github.chalkyjeans.taboobot.config.TabooConfig;
import io.github.chalkyjeans.taboobot.util.ResponseHelper;
import lavalink.client.io.jda.JdaLavalink;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.sharding.ShardManager;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import java.net.URI;

@Slf4j
@Component
public class ReadyListener extends ListenerAdapter {

    private final TabooConfig config;

    public ReadyListener(TabooConfig config) {
        this.config = config;
    }

    public void onReady(@NotNull ReadyEvent event) {
        ShardManager shardManager = TabooBot.getInstance().getShardManager();
        log.info("Successfully started {} shards.", shardManager.getShards().size());
        JdaLavalink lavalink = TabooBot.getInstance().getLavalink();
        lavalink.setJdaProvider(shardManager::getShardById);
        lavalink.setUserId(shardManager.getShards().get(0).getSelfUser().getId());
        TabooConfig.LavalinkConfig lavalinkConfig = config.getLavalink();
        lavalink.addNode("node-1", URI.create(lavalinkConfig.getHostUrl()), lavalinkConfig.getPassword());
        TabooBot.getInstance().getInteractionCommandHandler().start();
        TabooBot.getInstance().getInteractionCommandHandler().startCommandCheck();
        TabooBot.getInstance().getWebhookClient().send(ResponseHelper.getStartupMessage());
    }

}
