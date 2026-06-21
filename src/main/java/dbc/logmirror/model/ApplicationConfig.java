package dbc.logmirror.model;

import java.util.ArrayList;
import java.util.List;

public class ApplicationConfig {

    private List<Server> servers = new ArrayList<>();
    private List<LogDefinition> logs = new ArrayList<>();
    private SecurityConfig security = new SecurityConfig();

    public ApplicationConfig() {
    }

    public List<Server> getServers() {
        return servers;
    }

    public void setServers(List<Server> servers) {
        this.servers = new ArrayList<>(servers);
    }

    public List<LogDefinition> getLogs() {
        return logs;
    }

    public void setLogs(List<LogDefinition> logs) {
        this.logs = new ArrayList<>(logs);
    }

    public SecurityConfig getSecurity() {
        return security;
    }

    public void setSecurity(SecurityConfig security) {
        this.security = security;
    }

    public static class SecurityConfig {
        private boolean allowShellProcessors = false;
        private List<String> allowedCommandPrefixes = new ArrayList<>();

        public boolean isAllowShellProcessors() {
            return allowShellProcessors;
        }

        public void setAllowShellProcessors(boolean allowShellProcessors) {
            this.allowShellProcessors = allowShellProcessors;
        }

        public List<String> getAllowedCommandPrefixes() {
            return allowedCommandPrefixes;
        }

        public void setAllowedCommandPrefixes(List<String> allowedCommandPrefixes) {
            this.allowedCommandPrefixes = new ArrayList<>(allowedCommandPrefixes);
        }
    }
}
