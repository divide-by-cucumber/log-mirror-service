package dbc.logmirror.mirror;

import dbc.logmirror.config.ConfigurationManager;
import dbc.logmirror.model.LogDefinition;
import dbc.logmirror.model.LogEvent;
import dbc.logmirror.model.MirrorStatistics;
import dbc.logmirror.model.ProcessorConfig;
import dbc.logmirror.model.ProcessorType;
import dbc.logmirror.model.Server;
import dbc.logmirror.pipeline.PipelineExecutor;
import dbc.logmirror.processor.ProcessorFactory;
import dbc.logmirror.ssh.SSHConnection;
import dbc.logmirror.ssh.SSHConnectionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

public class LogMirrorTask implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(LogMirrorTask.class);
    private static final int RECONNECT_DELAY_MS = 5000;
    private static final int MAX_RECONNECT_ATTEMPTS = Integer.MAX_VALUE;

    private final LogDefinition logDef;
    private final Server server;
    private final MirrorStatistics stats;
    private final SSHConnectionManager sshManager;
    private final ProcessorFactory processorFactory;
    private final ConfigurationManager configManager;

    private volatile boolean running = false;
    private volatile boolean stopped = false;
    private PipelineExecutor pipeline;

    public LogMirrorTask(LogDefinition logDef, Server server, SSHConnectionManager sshManager,
                         ProcessorFactory processorFactory, ConfigurationManager configManager) {
        this.logDef = logDef;
        this.server = server;
        this.stats = new MirrorStatistics(logDef.getId());
        this.sshManager = sshManager;
        this.processorFactory = processorFactory;
        this.configManager = configManager;
    }

    @Override
    public void run() {
        running = true;
        stats.setActive(true);

        try {
            initializePipeline();
            pipeline.startAll();

            int reconnectAttempts = 0;
            while (running && !stopped && reconnectAttempts < MAX_RECONNECT_ATTEMPTS) {
                try {
                    tailLog();
                    reconnectAttempts = 0; // Reset on successful connection
                } catch (Exception e) {
                    reconnectAttempts++;
                    if (reconnectAttempts < MAX_RECONNECT_ATTEMPTS) {
                        logger.warn("Error tailing log, reconnecting... (attempt {})", reconnectAttempts, e);
                        stats.incrementReconnectCount();
                        Thread.sleep(RECONNECT_DELAY_MS);
                    }
                }
            }

        } catch (Exception e) {
            logger.error("Fatal error in mirror task", e);
        } finally {
            if (pipeline != null) {
                pipeline.stopAll();
            }
            running = false;
            stats.setActive(false);
            logger.info("Mirror task stopped for log: {}", logDef.getId());
        }
    }

    private void initializePipeline() throws Exception {
        pipeline = new PipelineExecutor();

        for (ProcessorConfig processorConfig : logDef.getProcessors()) {
            switch (processorConfig.getType()) {
                case TEXT_SHELL:
                    // Text processor for plain text filtering/transformation
                    try {
                        pipeline.addTextProcessor(processorFactory.createTextProcessor(processorConfig));
                        logger.debug("Added text processor: {}", processorConfig.getId());
                    } catch (Exception e) {
                        logger.warn("Failed to create text processor: {}", processorConfig.getId(), e);
                        throw e;
                    }
                    break;

                case JAVA_CLASS:
                case EVENT_SHELL:
                case SHELL_COMMAND:  // Backward compatibility
                    // Event processor for structured event processing
                    try {
                        pipeline.addProcessor(processorFactory.createProcessor(processorConfig));
                        logger.debug("Added event processor: {}", processorConfig.getId());
                    } catch (Exception e) {
                        logger.warn("Failed to create event processor: {}", processorConfig.getId(), e);
                        throw e;
                    }
                    break;

                default:
                    throw new IllegalArgumentException("Unknown processor type: " + processorConfig.getType());
            }
        }

        logger.info("Pipeline initialized with {} processors", pipeline.getProcessorCount());
    }

    private void tailLog() throws Exception {
        Server resolvedServer = configManager.getServerWithResolvedCredentials(server.getId());
        SSHConnection conn = sshManager.getConnection(
                resolvedServer.getHost(),
                resolvedServer.getPort(),
                resolvedServer.getUsername(),
                resolvedServer.getPassword(),
                resolvedServer.getPrivateKeyFile(),
                resolvedServer.getPassphrase()
        );

        // Execute tail -n 0 -F <logfile>
        String command = String.format("tail -n 0 -F %s", logDef.getSourcePath());
        logger.info("Executing command: {}", command);

        try (var channel = conn.openExecChannel(command)) {
            channel.open().verify(30, TimeUnit.SECONDS);
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(channel.getInvertedOut(), StandardCharsets.UTF_8));

            // Prepare local mirror file
            Files.createDirectories(Paths.get(logDef.getLocalMirrorPath()).getParent());

            try (BufferedWriter writer = new BufferedWriter(new FileWriter(logDef.getLocalMirrorPath(), true))) {
                String line;
                while (running && !stopped && (line = reader.readLine()) != null) {
                    try {
                        processLogLine(line, writer);
                    } catch (Exception e) {
                        logger.error("Error processing log line", e);
                    }
                }
            }
        }
    }

    private void processLogLine(String line, BufferedWriter writer) throws Exception {
        stats.incrementLinesReceived();

        try {
            // Execute the full pipeline: text processors -> event processors
            java.util.List<LogEvent> results = pipeline.executeFromText(
                    logDef.getId(),
                    server.getId(),
                    line
            );

            // Write all processed events to mirror file
            for (LogEvent processed : results) {
                writer.write(processed.getLine());
                writer.newLine();
                writer.flush();

                stats.incrementBytesWritten(processed.getLine().getBytes(StandardCharsets.UTF_8).length + 1); // +1 for newline
                stats.setLastEventTimestamp(Instant.now());
            }

            if (results.isEmpty()) {
                logger.debug("Line dropped by pipeline: {}", line);
            }
        } catch (Exception e) {
            logger.error("Pipeline execution failed", e);
            throw e;
        }
    }

    public void stop() {
        stopped = true;
        running = false;
    }

    public MirrorStatistics getStatistics() {
        return stats;
    }

    public boolean isRunning() {
        return running && !stopped;
    }
}
