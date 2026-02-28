package com.farmer.location.config;

import com.farmer.location.service.GovernmentBodyDataLoader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializationListener {
    
    private final GovernmentBodyDataLoader governmentBodyDataLoader;
    
    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        log.info("Application started, initializing government bodies data...");
        governmentBodyDataLoader.loadGovernmentBodiesFromCsv();
    }
}
