package io.github.chalkyjeans.taboobot;

import io.github.chalkyjeans.taboobot.bot.TabooBot;
import io.github.chalkyjeans.taboobot.util.ApplicationContextProvider;
import io.github.chalkyjeans.taboobot.util.ResponseHelper;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextClosedEvent;

@SpringBootApplication
@EnableConfigurationProperties
@Slf4j
public class TabooBotApplication {

    @Getter
    private static ApplicationContextProvider provider;

    public TabooBotApplication(ApplicationContextProvider provider) {
        TabooBotApplication.provider = provider;
    }

    public static void main(String[] args) {
        SpringApplication application = new SpringApplication(TabooBotApplication.class);
        application.setBanner((environment, sourceClass, out) -> out.println(
                """
                ___________     ___.                 \s
                \\__    ___/____ \\_ |__   ____   ____ \s
                  |    |  \\__  \\ | __ \\ /  _ \\ /  _ \\\s
                  |    |   / __ \\| \\_\\ (  <_> |  <_> )
                  |____|  (____  /___  /\\____/ \\____/\s
                                \\/    \\/              \s
                """
        ));
        application.addListeners((ApplicationListener<ContextClosedEvent>) event -> {
            log.info("Shutting down Taboo...");
            TabooBot.getInstance().getWebhookClient().send(ResponseHelper.getShutdownMessage());
        });
        application.run(args);
    }

}
