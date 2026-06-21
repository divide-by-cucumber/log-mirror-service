package dbc.logmirror.ssh;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Component
public class SSHConnectionManager {

    private static final Logger logger = LoggerFactory.getLogger(SSHConnectionManager.class);

    private final ConcurrentMap<String, SSHConnection> connections = new ConcurrentHashMap<>();

    public SSHConnection getConnection(String host, int port, String username, 
                                        String password, String privateKeyFile, String passphrase) throws IOException {
        String key = host + ":" + port;

        SSHConnection conn = connections.get(key);
        if (conn != null && conn.isConnected()) {
            return conn;
        }

        // Create new connection
        conn = new SSHConnection(host, port, username, password, privateKeyFile, passphrase);
        conn.connect();

        SSHConnection existing = connections.putIfAbsent(key, conn);
        if (existing != null) {
            // Another thread already created the connection
            conn.disconnect();
            return existing;
        }

        return conn;
    }

    public void closeConnection(String host, int port) {
        String key = host + ":" + port;
        SSHConnection conn = connections.remove(key);
        if (conn != null) {
            conn.disconnect();
            logger.info("Closed SSH connection to {}:{}", host, port);
        }
    }

    public void closeAllConnections() {
        for (SSHConnection conn : connections.values()) {
            try {
                conn.disconnect();
            } catch (Exception e) {
                logger.debug("Error closing connection", e);
            }
        }
        connections.clear();
        logger.info("Closed all SSH connections");
    }
}
