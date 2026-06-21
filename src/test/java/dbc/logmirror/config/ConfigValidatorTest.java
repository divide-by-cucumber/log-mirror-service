package dbc.logmirror.config;

import dbc.logmirror.config.ConfigValidator;
import dbc.logmirror.config.EnvironmentVariableResolver;
import dbc.logmirror.model.ApplicationConfig;
import dbc.logmirror.model.AuthenticationType;
import dbc.logmirror.model.LogDefinition;
import dbc.logmirror.model.Server;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ConfigValidatorTest {

    private ConfigValidator validator;
    private EnvironmentVariableResolver resolver;

    @BeforeEach
    void setUp() {
        resolver = new EnvironmentVariableResolver();
        validator = new ConfigValidator(resolver);
    }

    @Test
    void testValidateValidServer() {
        Server server = new Server("srv1", "Test Server", "localhost", "user", AuthenticationType.PASSWORD);
        server.setPassword("password123");

        assertDoesNotThrow(() -> validator.validateServer(server));
    }

    @Test
    void testValidateServerMissingId() {
        Server server = new Server(null, "Test", "localhost", "user", AuthenticationType.PASSWORD);
        server.setPassword("pass");

        assertThrows(IllegalArgumentException.class, () -> validator.validateServer(server));
    }

    @Test
    void testValidateServerMissingHost() {
        Server server = new Server("srv1", "Test", null, "user", AuthenticationType.PASSWORD);
        server.setPassword("pass");

        assertThrows(IllegalArgumentException.class, () -> validator.validateServer(server));
    }

    @Test
    void testValidateServerPrivateKeyMissing() {
        Server server = new Server("srv1", "Test", "localhost", "user", AuthenticationType.PRIVATE_KEY);
        server.setPrivateKeyFile(null);

        assertThrows(IllegalArgumentException.class, () -> validator.validateServer(server));
    }

    @Test
    void testValidateValidLog() {
        Server server = new Server("srv1", "Test", "localhost", "user", AuthenticationType.PASSWORD);
        server.setPassword("pass");

        LogDefinition log = new LogDefinition("log1", "srv1", "/var/log/app.log", "/data/app.log");

        assertDoesNotThrow(() -> validator.validateLog(log, java.util.List.of(server)));
    }

    @Test
    void testValidateLogMissingId() {
        Server server = new Server("srv1", "Test", "localhost", "user", AuthenticationType.PASSWORD);
        server.setPassword("pass");

        LogDefinition log = new LogDefinition(null, "srv1", "/var/log/app.log", "/data/app.log");

        assertThrows(IllegalArgumentException.class, () -> validator.validateLog(log, java.util.List.of(server)));
    }

    @Test
    void testValidateLogNonexistentServer() {
        Server server = new Server("srv1", "Test", "localhost", "user", AuthenticationType.PASSWORD);
        server.setPassword("pass");

        LogDefinition log = new LogDefinition("log1", "srv999", "/var/log/app.log", "/data/app.log");

        assertThrows(IllegalArgumentException.class, () -> validator.validateLog(log, java.util.List.of(server)));
    }
}
