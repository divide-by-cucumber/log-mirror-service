package dbc.logmirror.config;

import dbc.logmirror.model.ApplicationConfig;
import dbc.logmirror.model.LogDefinition;
import dbc.logmirror.model.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class ConfigValidator {

    private static final Logger logger = LoggerFactory.getLogger(ConfigValidator.class);
    private final EnvironmentVariableResolver resolver;

    public ConfigValidator(EnvironmentVariableResolver resolver) {
        this.resolver = resolver;
    }

    public void validate(ApplicationConfig config) throws IllegalArgumentException {
        if (config == null) {
            throw new IllegalArgumentException("Configuration cannot be null");
        }

        validateServers(config.getServers());
        validateLogs(config.getLogs(), config.getServers());
    }

    public void validateServers(List<Server> servers) {
        if (servers == null) {
            return;
        }

        for (Server server : servers) {
            validateServerInternal(server);
        }
    }

    public void validateServer(Server server) {
        validateServerInternal(server);
    }

    private void validateServerInternal(Server server) {
        if (server.getId() == null || server.getId().isEmpty()) {
            throw new IllegalArgumentException("Server id cannot be empty");
        }
        if (server.getHost() == null || server.getHost().isEmpty()) {
            throw new IllegalArgumentException("Server host cannot be empty");
        }
        if (server.getUsername() == null || server.getUsername().isEmpty()) {
            throw new IllegalArgumentException("Server username cannot be empty");
        }
        if (server.getAuthenticationType() == null) {
            throw new IllegalArgumentException("Server authentication type must be specified");
        }

        switch (server.getAuthenticationType()) {
            case PASSWORD:
                if (server.getPassword() == null || server.getPassword().isEmpty()) {
                    throw new IllegalArgumentException("Password authentication requires password");
                }
                // Validate placeholders if present
                if (resolver.containsPlaceholder(server.getPassword())) {
                    resolver.validatePlaceholders(server.getPassword());
                }
                break;
            case PRIVATE_KEY:
                if (server.getPrivateKeyFile() == null || server.getPrivateKeyFile().isEmpty()) {
                    throw new IllegalArgumentException("Private key authentication requires privateKeyFile");
                }
                // Validate placeholders
                if (resolver.containsPlaceholder(server.getPrivateKeyFile())) {
                    resolver.validatePlaceholders(server.getPrivateKeyFile());
                }
                if (server.getPassphrase() != null && resolver.containsPlaceholder(server.getPassphrase())) {
                    resolver.validatePlaceholders(server.getPassphrase());
                }
                break;
        }
    }

    private void validateLogs(List<LogDefinition> logs, List<Server> servers) {
        if (logs == null) {
            return;
        }

        for (LogDefinition log : logs) {
            validateLogInternal(log, servers);
        }
    }

    public void validateLog(LogDefinition log, List<Server> servers) {
        validateLogInternal(log, servers);
    }

    private void validateLogInternal(LogDefinition log, List<Server> servers) {
        if (log.getId() == null || log.getId().isEmpty()) {
            throw new IllegalArgumentException("Log id cannot be empty");
        }
        if (log.getServerId() == null || log.getServerId().isEmpty()) {
            throw new IllegalArgumentException("Log serverId cannot be empty");
        }

        // Check that referenced server exists
        boolean serverExists = servers.stream().anyMatch(s -> s.getId().equals(log.getServerId()));
        if (!serverExists) {
            throw new IllegalArgumentException("Referenced server not found: " + log.getServerId());
        }

        if (log.getSourcePath() == null || log.getSourcePath().isEmpty()) {
            throw new IllegalArgumentException("Log sourcePath cannot be empty");
        }
        if (log.getLocalMirrorPath() == null || log.getLocalMirrorPath().isEmpty()) {
            throw new IllegalArgumentException("Log localMirrorPath cannot be empty");
        }
    }
}
