package dbc.logmirror.ssh;

import org.apache.sshd.client.SshClient;
import org.apache.sshd.client.channel.ChannelExec;
import org.apache.sshd.client.session.ClientSession;
import org.apache.sshd.common.keyprovider.FileKeyPairProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

public class SSHConnection {

    private static final Logger logger = LoggerFactory.getLogger(SSHConnection.class);
    private static final int CONNECTION_TIMEOUT_MS = 30000;

    private final String host;
    private final int port;
    private final String username;
    private final String password;
    private final String privateKeyFile;
    private final String passphrase;

    private SshClient sshClient;
    private ClientSession session;

    public SSHConnection(String host, int port, String username, 
                         String password, String privateKeyFile, String passphrase) {
        this.host = host;
        this.port = port;
        this.username = username;
        this.password = password;
        this.privateKeyFile = privateKeyFile;
        this.passphrase = passphrase;
    }

    public void connect() throws IOException {
        logger.info("Connecting to {}:{}", host, port);
        
        sshClient = SshClient.setUpDefaultClient();
        sshClient.start();

        try {
            session = sshClient.connect(username, host, port).verify(CONNECTION_TIMEOUT_MS).getSession();

            if (password != null && !password.isEmpty()) {
                session.addPasswordIdentity(password);
                logger.info("Using password authentication");
            } else if (privateKeyFile != null && !privateKeyFile.isEmpty()) {
                try {
                    session.addPublicKeyIdentity(new FileKeyPairProvider(Paths.get(privateKeyFile))
                            .loadKeys(null).iterator().next());
                    logger.info("Using private key authentication");
                } catch (Exception e) {
                    throw new IOException("Failed to load private key: " + e.getMessage(), e);
                }
            } else {
                throw new IOException("No authentication method provided");
            }

            session.auth().verify(CONNECTION_TIMEOUT_MS);
            logger.info("Successfully connected to {}:{}", host, port);
        } catch (Exception e) {
            if (session != null) {
                try {
                    session.close();
                } catch (Exception ex) {
                    logger.debug("Error closing session", ex);
                }
            }
            if (sshClient != null) {
                try {
                    sshClient.stop();
                } catch (Exception ex) {
                    logger.debug("Error stopping SSH client", ex);
                }
            }
            logger.error("Connection failed", e);
            throw new IOException("SSH connection failed: " + e.getMessage(), e);
        }
    }

    public ChannelExec openExecChannel(String command) throws IOException {
        if (session == null || !session.isOpen()) {
            throw new IOException("SSH session not connected");
        }

        try {
            ChannelExec channel = session.createExecChannel(command);
            logger.debug("Opened exec channel for command: {}", command);
            return channel;
        } catch (IOException e) {
            logger.error("Failed to open exec channel", e);
            throw e;
        }
    }

    public void disconnect() {
        if (session != null) {
            try {
                session.close();
                logger.debug("Closed SSH session");
            } catch (Exception e) {
                logger.debug("Error closing session", e);
            }
        }

        if (sshClient != null) {
            try {
                sshClient.stop();
                logger.info("Disconnected from {}:{}", host, port);
            } catch (Exception e) {
                logger.debug("Error stopping SSH client", e);
            }
        }
    }

    public boolean isConnected() {
        return session != null && session.isOpen();
    }
}

