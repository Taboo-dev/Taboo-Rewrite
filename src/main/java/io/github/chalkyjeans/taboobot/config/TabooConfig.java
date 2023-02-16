package io.github.chalkyjeans.taboobot.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties("taboo")
@Data
public class TabooConfig {

    private String token;
    private boolean production;
    private long guildId;
    private long ownerId;
    private String webhookUrl;
    private LavalinkConfig lavalink;
    private BackendConfig backend;

    @Data
    public static class LavalinkConfig {
        private String hostUrl;
        private String password;
    }

    @Data
    public static class BackendConfig {
        private String username;
        private String password;
    }

}
