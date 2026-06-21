package dbc.logmirror;

import dbc.logmirror.config.ConfigurationManager;
import dbc.logmirror.mirror.MirrorManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class ApplicationStartupListener {

    private static final Logger logger = LoggerFactory.getLogger(ApplicationStartupListener.class);

    private final ConfigurationManager configManager;
    private final MirrorManager mirrorManager;

    public ApplicationStartupListener(ConfigurationManager configManager, MirrorManager mirrorManager) {
        this.configManager = configManager;
        this.mirrorManager = mirrorManager;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        try {
            logger.info("Application started, initializing configuration and mirrors");
            configManager.initialize();
            mirrorManager.initializeFromConfiguration();
            logger.info("Initialization complete");
        } catch (Exception e) {
            logger.error("Failed to initialize application", e);
        }
    }
}
