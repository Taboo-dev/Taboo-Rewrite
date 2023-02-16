package io.github.chalkyjeans.taboobot.bot;

import club.minnced.discord.webhook.WebhookClientBuilder;
import club.minnced.discord.webhook.external.JDAWebhookClient;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.jagrosh.jdautilities.commons.waiter.EventWaiter;
import io.github.chalkyjeans.taboobot.config.TabooConfig;
import io.github.chalkyjeans.taboobot.core.InteractionCommandHandler;
import io.github.chalkyjeans.taboobot.events.EventManager;
import io.github.chalkyjeans.taboobot.music.AudioManager;
import lavalink.client.io.jda.JdaLavalink;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import net.dv8tion.jda.api.exceptions.InvalidTokenException;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.sharding.DefaultShardManagerBuilder;
import net.dv8tion.jda.api.sharding.ShardManager;
import net.dv8tion.jda.api.utils.ChunkingFilter;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

@Component
@Getter
@Slf4j
public class TabooBot implements CommandLineRunner {

    @Getter
    private static TabooBot instance;
    private final TabooConfig config;
    private ShardManager shardManager;
    private JdaLavalink lavalink;
    private JDAWebhookClient webhookClient;
    private InteractionCommandHandler interactionCommandHandler;
    private EventManager eventManager;
    private EventWaiter eventWaiter;
    private AudioManager audioManager;

    private final ExecutorService commandExecutor =
            Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors(),
                    new ThreadFactoryBuilder()
                            .setNameFormat("Taboo Command Thread %d")
                            .setUncaughtExceptionHandler((thread, throwable) -> {
                                log.error("An uncaught error occurred on the command thread-pool! (Thread {})", thread.getName(), throwable);
                            }).build());

    private final ScheduledExecutorService scheduledExecutor =
            Executors.newSingleThreadScheduledExecutor(new ThreadFactoryBuilder()
                    .setNameFormat("Taboo Scheduled Executor Thread")
                    .setUncaughtExceptionHandler((thread, throwable) -> {
                        log.error("An uncaught error occurred on the scheduled executor thread-pool! (Thread {})", thread.getName(), throwable);
                    }).build());

    private final ExecutorService executor =
            Executors.newSingleThreadExecutor(new ThreadFactoryBuilder()
                    .setNameFormat("Taboo Executor Thread")
                    .setUncaughtExceptionHandler((thread, throwable) -> {
                        log.error("An uncaught error occurred on the executor thread-pool! (Thread {})", thread.getName(), throwable);
                    }).build());

    public TabooBot(TabooConfig config) {
        instance = this;
        this.config = config;
    }

    @Override
    public void run(String... args) {
        try {
            TabooBot bot = new TabooBot(config);
            bot.start();
        } catch (Exception e) {
            if (e instanceof InvalidTokenException) {
                log.error("Invalid token provided! Please check your config file.");
            } else if (e instanceof ErrorResponseException) {
                log.error("An error occurred while starting the bot! Please check your config file.");
            } else {
                log.error("An error occurred while starting the bot!", e);
            }
        }
    }

    private void start() {
        this.interactionCommandHandler = new InteractionCommandHandler(config);
        this.eventManager = new EventManager();
        this.eventWaiter = new EventWaiter();
        this.lavalink = new JdaLavalink(null, 1, null);
        this.audioManager = new AudioManager();
        this.shardManager = DefaultShardManagerBuilder.createDefault(config.getToken())
                .enableIntents(GatewayIntent.GUILD_VOICE_STATES, GatewayIntent.GUILD_EMOJIS_AND_STICKERS, GatewayIntent.GUILD_MEMBERS)
                .enableCache(CacheFlag.VOICE_STATE)
                .setMemberCachePolicy(MemberCachePolicy.ALL)
                .setChunkingFilter(ChunkingFilter.ALL)
                .setShardsTotal(-1)
                .setStatus(OnlineStatus.ONLINE)
                .setEventManagerProvider(i -> eventManager)
                .setVoiceDispatchInterceptor(lavalink.getVoiceInterceptor())
                .build();
        this.webhookClient = new WebhookClientBuilder(config.getWebhookUrl())
                .setThreadFactory(r -> {
                    Thread thread = new Thread(r);
                    thread.setName("Taboo Webhook Thread");
                    thread.setUncaughtExceptionHandler((t, e) -> {
                        log.error("An uncaught error occurred on the webhook thread-pool! (Thread {})", t.getName(), e);
                    });
                    return thread;
                }).setWait(true)
                .buildJDA();
        this.eventManager.start();
    }


}
