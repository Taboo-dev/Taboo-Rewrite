package io.github.chalkyjeans.taboobot.events;

import io.github.chalkyjeans.taboobot.TabooBotApplication;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.events.GenericEvent;
import net.dv8tion.jda.api.hooks.EventListener;
import net.dv8tion.jda.api.hooks.IEventManager;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Slf4j
@Component
public class EventManager implements IEventManager {

    private final ArrayList<EventListener> listeners = new ArrayList<>();

    public void start() {
        registerEvents();
    }

    private void registerEvents() {
        TabooBotApplication.getProvider().getApplicationContext().getBeansOfType(ListenerAdapter.class).values().forEach(this::register);
    }

    @Override
    public void register(@NotNull Object listener) {
        if (listener instanceof ListenerAdapter listenerAdapter) {
            listeners.add(listenerAdapter);
            log.info("Registering event listener: {}", listenerAdapter.getClass().getSimpleName());
        }
    }

    @Override
    public void unregister(@NotNull Object listener) {
        if (listener instanceof ListenerAdapter listenerAdapter) {
            listeners.remove(listenerAdapter);
            log.info("Unregistering event listener: {}", listenerAdapter.getClass().getSimpleName());
        }
    }

    @Override
    public void handle(@NotNull GenericEvent event) {
        listeners.forEach(listener -> {
            try {
                listener.onEvent(event);
            } catch (Exception e) {
                log.error("Error handling event {}", event.getClass().getSimpleName(), e);
            }
        });
    }

    @NotNull
    @Override
    public List<Object> getRegisteredListeners() {
        return Collections.singletonList(listeners);
    }

}
