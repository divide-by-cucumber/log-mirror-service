package dbc.logmirror.persistence;

import dbc.logmirror.model.ApplicationConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Component
public class ConfigurationPersistence {

    private static final Logger logger = LoggerFactory.getLogger(ConfigurationPersistence.class);
    private static final String DEFAULT_CONFIG_PATH = "config/log-mirror.json";

    private final ObjectMapper objectMapper;
    private final String configPath;

    public ConfigurationPersistence(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        String envPath = System.getenv("LOG_MIRROR_CONFIG_PATH");
        this.configPath = envPath != null ? envPath : DEFAULT_CONFIG_PATH;
    }

    public ApplicationConfig load() throws IOException {
        File configFile = new File(configPath);
        
        if (!configFile.exists()) {
            logger.warn("Configuration file not found at {}, returning empty config", configPath);
            return new ApplicationConfig();
        }

        try {
            logger.info("Loading configuration from {}", configPath);
            ApplicationConfig config = objectMapper.readValue(configFile, ApplicationConfig.class);
            logger.info("Configuration loaded successfully. Servers: {}, Logs: {}",
                    config.getServers().size(), config.getLogs().size());
            return config;
        } catch (IOException e) {
            logger.error("Failed to load configuration from {}", configPath, e);
            throw e;
        }
    }

    public void save(ApplicationConfig config) throws IOException {
        Path configDir = Paths.get(configPath).getParent();
        
        if (configDir != null && !Files.exists(configDir)) {
            Files.createDirectories(configDir);
            logger.info("Created configuration directory: {}", configDir);
        }

        try {
            logger.info("Saving configuration to {}", configPath);
            objectMapper.writerWithDefaultPrettyPrinter()
                    .writeValue(new File(configPath), config);
            logger.info("Configuration saved successfully");
        } catch (IOException e) {
            logger.error("Failed to save configuration to {}", configPath, e);
            throw e;
        }
    }

    public String getConfigPath() {
        return configPath;
    }
}
