package dbc.logmirror.processor;

import dbc.logmirror.model.LogEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

public class ShellProcessor implements LogStreamProcessor {

    private static final Logger logger = LoggerFactory.getLogger(ShellProcessor.class);
    private static final long PROCESS_TIMEOUT_SECONDS = 5;
    private static final int RESTART_DELAY_MS = 1000;
    private static final int MAX_RESTART_ATTEMPTS = 3;

    private final String processorId;
    private final String command;
    private final ObjectMapper objectMapper;

    private Process process;
    private BufferedWriter stdin;
    private BufferedReader stdout;
    private int restartAttempts = 0;

    public ShellProcessor(String processorId, String command, ObjectMapper objectMapper) {
        this.processorId = processorId;
        this.command = command;
        this.objectMapper = objectMapper;
    }

    @Override
    public void start() throws Exception {
        logger.info("Starting shell processor: {} with command: {}", processorId, command);
        startProcess();
    }

    private void startProcess() throws Exception {
        ProcessBuilder pb = new ProcessBuilder("/bin/bash", "-c", command);

        process = pb.start();
        stdin = new BufferedWriter(
                new OutputStreamWriter(process.getOutputStream(), StandardCharsets.UTF_8));
        stdout = new BufferedReader(
                new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8));

        // Redirecting stderr to the logger ..
        Thread.ofVirtual().start(() -> {
            try (BufferedReader err = new BufferedReader(
                    new InputStreamReader(process.getErrorStream(), StandardCharsets.UTF_8))) {

                String line;
                while ((line = err.readLine()) != null) {
                    logger.error("[{}] {}", processorId, line);
                }
            } catch (IOException e) {
                logger.warn("Error reading stderr", e);
            }
        });

        logger.info("Shell processor started: {}, error stream redirected", processorId);
        restartAttempts = 0;
    }

    @Override
    public Optional<LogEvent> process(LogEvent event) throws Exception {
        try {
            if (process == null || !process.isAlive()) {
                logger.warn("Shell process died, attempting restart");
                attemptRestart();
                if (process == null || !process.isAlive()) {
                    logger.error("Failed to restart shell processor");
                    return Optional.empty();
                }
            }

            // Send event as JSON line
            String eventJson = objectMapper.writeValueAsString(event);
            stdin.write(eventJson);
            stdin.write("\n");
            stdin.flush();

            // Read response
            String response = stdout.ready() ? stdout.readLine() : "";
            if (response == null) {
                logger.warn("Shell processor returned null, process may have died");
                return Optional.empty();
            }

            // Empty line means event was dropped
            if (response.isEmpty()) {
                return Optional.empty();
            }

            // Parse response back into LogEvent
            try {
                LogEvent result = objectMapper.readValue(response, LogEvent.class);
                return Optional.of(result);
            } catch (Exception e) {
                logger.warn("Failed to parse processor output: {}", response, e);
                return Optional.empty();
            }

        } catch (Exception e) {
            logger.error("Shell processor error", e);
            attemptRestart();
            throw e;
        }
    }

    private void attemptRestart() {
        try {
            if (restartAttempts >= MAX_RESTART_ATTEMPTS) {
                logger.error("Max restart attempts exceeded for processor: {}", processorId);
                return;
            }

            restartAttempts++;
            logger.info("Attempting to restart shell processor (attempt {}/{})",
                    restartAttempts, MAX_RESTART_ATTEMPTS);

            stop();
            Thread.sleep(RESTART_DELAY_MS);
            startProcess();

        } catch (Exception e) {
            logger.error("Failed to restart shell processor", e);
        }
    }

    @Override
    public void stop() {
        logger.info("Stopping shell processor: {}", processorId);

        try {
            if (stdin != null) {
                stdin.close();
            }
        } catch (Exception e) {
            logger.debug("Error closing stdin", e);
        }

        try {
            if (stdout != null) {
                stdout.close();
            }
        } catch (Exception e) {
            logger.debug("Error closing stdout", e);
        }

        if (process != null) {
            try {
                if (!process.waitFor(PROCESS_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                    logger.warn("Shell process did not terminate gracefully, destroying");
                    process.destroyForcibly();
                }
            } catch (InterruptedException e) {
                logger.warn("Interrupted while waiting for process termination", e);
                process.destroyForcibly();
            }
        }

        logger.info("Shell processor stopped: {}", processorId);
    }
}
