package dbc.logmirror.config;

import dbc.logmirror.model.ApplicationConfig;
import dbc.logmirror.model.LogDefinition;
import dbc.logmirror.model.Server;
import dbc.logmirror.persistence.ConfigurationPersistence;
import dbc.logmirror.security.SecretMasker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
public class ConfigurationManager {

    private static final Logger logger = LoggerFactory.getLogger(ConfigurationManager.class);

    private final ConfigurationPersistence persistence;
    private final EnvironmentVariableResolver resolver;
    private final ConfigValidator validator;
    private ApplicationConfig currentConfig;

    public ConfigurationManager(ConfigurationPersistence persistence, 
                                 EnvironmentVariableResolver resolver) {
        this.persistence = persistence;
        this.resolver = resolver;
        this.validator = new ConfigValidator(resolver);
    }

    public void initialize() throws IOException {
        this.currentConfig = persistence.load();
        validator.validate(currentConfig);
        logger.info("Configuration initialized successfully");
    }

    public ApplicationConfig getCurrentConfig() {
        return currentConfig;
    }

    public void reload() throws IOException {
        ApplicationConfig newConfig = persistence.load();
        validator.validate(newConfig);
        this.currentConfig = newConfig;
        logger.info("Configuration reloaded successfully");
    }

    public void save() throws IOException {
        validator.validate(currentConfig);
        persistence.save(currentConfig);
    }

    public Optional<Server> getServer(String serverId) {
        return currentConfig.getServers().stream()
                .filter(s -> s.getId().equals(serverId))
                .findFirst();
    }

    public Optional<LogDefinition> getLog(String logId) {
        return currentConfig.getLogs().stream()
                .filter(l -> l.getId().equals(logId))
                .findFirst();
    }

    public Server addServer(Server server) throws IOException {
        validator.validateServer(server);
        
        // Check for duplicate id
        if (currentConfig.getServers().stream().anyMatch(s -> s.getId().equals(server.getId()))) {
            throw new IllegalArgumentException("Server with id already exists: " + server.getId());
        }

        currentConfig.getServers().add(server);
        save();
        logger.info("Server added: {}", server.getId());
        return server;
    }

    public Server updateServer(String serverId, Server updatedServer) throws IOException {
        Server existing = getServer(serverId)
                .orElseThrow(() -> new IllegalArgumentException("Server not found: " + serverId));

        updatedServer.setId(serverId);
        validator.validateServer(updatedServer);

        currentConfig.getServers().remove(existing);
        currentConfig.getServers().add(updatedServer);
        save();
        logger.info("Server updated: {}", serverId);
        return updatedServer;
    }

    public void deleteServer(String serverId) throws IOException {
        Server existing = getServer(serverId)
                .orElseThrow(() -> new IllegalArgumentException("Server not found: " + serverId));

        // Check if any logs reference this server
        boolean hasLogs = currentConfig.getLogs().stream()
                .anyMatch(l -> l.getServerId().equals(serverId));
        if (hasLogs) {
            throw new IllegalArgumentException("Cannot delete server with associated logs: " + serverId);
        }

        currentConfig.getServers().remove(existing);
        save();
        logger.info("Server deleted: {}", serverId);
    }

    public LogDefinition addLog(LogDefinition log) throws IOException {
        validator.validateLog(log, currentConfig.getServers());
        
        // Check for duplicate id
        if (currentConfig.getLogs().stream().anyMatch(l -> l.getId().equals(log.getId()))) {
            throw new IllegalArgumentException("Log with id already exists: " + log.getId());
        }

        currentConfig.getLogs().add(log);
        save();
        logger.info("Log added: {}", log.getId());
        return log;
    }

    public LogDefinition updateLog(String logId, LogDefinition updatedLog) throws IOException {
        LogDefinition existing = getLog(logId)
                .orElseThrow(() -> new IllegalArgumentException("Log not found: " + logId));

        updatedLog.setId(logId);
        validator.validateLog(updatedLog, currentConfig.getServers());

        currentConfig.getLogs().remove(existing);
        currentConfig.getLogs().add(updatedLog);
        save();
        logger.info("Log updated: {}", logId);
        return updatedLog;
    }

    public void deleteLog(String logId) throws IOException {
        LogDefinition existing = getLog(logId)
                .orElseThrow(() -> new IllegalArgumentException("Log not found: " + logId));

        currentConfig.getLogs().remove(existing);
        save();
        logger.info("Log deleted: {}", logId);
    }

    public Server getServerWithResolvedCredentials(String serverId) {
        Server server = getServer(serverId)
                .orElseThrow(() -> new IllegalArgumentException("Server not found: " + serverId));

        Server resolved = new Server();
        resolved.setId(server.getId());
        resolved.setName(server.getName());
        resolved.setHost(server.getHost());
        resolved.setPort(server.getPort());
        resolved.setUsername(server.getUsername());
        resolved.setAuthenticationType(server.getAuthenticationType());

        // Resolve credentials
        if (server.getPassword() != null) {
            resolved.setPassword(resolver.resolveValue(server.getPassword()));
        }
        if (server.getPrivateKeyFile() != null) {
            resolved.setPrivateKeyFile(resolver.resolveValue(server.getPrivateKeyFile()));
        }
        if (server.getPassphrase() != null) {
            resolved.setPassphrase(resolver.resolveValue(server.getPassphrase()));
        }

        return resolved;
    }

    public Server getMaskedServer(String serverId) {
        Server server = getServer(serverId)
                .orElseThrow(() -> new IllegalArgumentException("Server not found: " + serverId));

        Server masked = new Server();
        masked.setId(server.getId());
        masked.setName(server.getName());
        masked.setHost(server.getHost());
        masked.setPort(server.getPort());
        masked.setUsername(server.getUsername());
        masked.setAuthenticationType(server.getAuthenticationType());

        // Mask secrets
        if (server.getPassword() != null) {
            masked.setPassword(SecretMasker.maskPassword(server.getPassword()));
        }
        if (server.getPrivateKeyFile() != null) {
            masked.setPrivateKeyFile(SecretMasker.maskValue(server.getPrivateKeyFile()));
        }
        if (server.getPassphrase() != null) {
            masked.setPassphrase(SecretMasker.maskValue(server.getPassphrase()));
        }

        return masked;
    }
}
