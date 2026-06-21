package dbc.logmirror.processor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Text-based shell processor for plain Unix command line tools.
 *
 * This processor runs a shell command and passes raw log lines to stdin,
 * collecting output lines from stdout. Ideal for grep, sed, awk, tr, etc.
 *
 * No JSON serialization is involved - raw text lines are processed directly.
 */
public class TextShellProcessor implements LogTextProcessor {

    private static final Logger logger = LoggerFactory.getLogger(TextShellProcessor.class);
    private static final long PROCESS_TIMEOUT_SECONDS = 5;
    private static final int RESTART_DELAY_MS = 1000;
    private static final int MAX_RESTART_ATTEMPTS = 3;

    private final String processorId;
    private final String command;

    private Process process;
    private BufferedWriter stdin;
    private BufferedReader stdout;
    private int restartAttempts = 0;

    public TextShellProcessor(String processorId, String command) {
        this.processorId = processorId;
        this.command = command;
    }

    @Override
    public void start() throws Exception {
        logger.info("Starting text shell processor: {} with command: {}", processorId, command);
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

        logger.info("Text shell processor started: {}, error stream redirected", processorId);
        restartAttempts = 0;
    }

    @Override
    public List<String> process(String line) throws Exception {
        List<String> results = new ArrayList<>();

        try {
            if (process == null || !process.isAlive()) {
                logger.warn("Text shell process died, attempting restart");
                attemptRestart();
                if (process == null || !process.isAlive()) {
                    logger.error("Failed to restart text shell processor");
                    return results;  // Return empty list on failure
                }
            }

            // Send line to stdin with newline
            stdin.write(line);
            stdin.write("\n");
            stdin.flush();

            // Read output lines until we get back to prompt or timeout
            // For simple tools like grep, sed, we expect immediate output
            long deadline = System.currentTimeMillis() + 1000;  // 1 second timeout per line
            while (System.currentTimeMillis() < deadline) {
                if (stdout.ready()) {
                    String response = stdout.readLine();
                    if (response != null) {
                        results.add(response);
                    } else {
                        logger.warn("Text shell processor returned null, process may have died");
                        attemptRestart();
                        break;
                    }
                } else {
                    // Small sleep to avoid busy waiting
                    Thread.sleep(10);
                }
            }

        } catch (Exception e) {
            logger.error("Text shell processor error", e);
            attemptRestart();
            throw e;
        }

        return results;
    }

    private void attemptRestart() {
        try {
            if (restartAttempts >= MAX_RESTART_ATTEMPTS) {
                logger.error("Max restart attempts exceeded for processor: {}", processorId);
                return;
            }

            restartAttempts++;
            logger.info("Attempting to restart text shell processor (attempt {}/{})",
                    restartAttempts, MAX_RESTART_ATTEMPTS);

            stop();
            Thread.sleep(RESTART_DELAY_MS);
            startProcess();

        } catch (Exception e) {
            logger.error("Failed to restart text shell processor", e);
        }
    }

    @Override
    public void stop() {
        logger.info("Stopping text shell processor: {}", processorId);

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
                    logger.warn("Text shell process did not terminate gracefully, destroying");
                    process.destroyForcibly();
                }
            } catch (InterruptedException e) {
                logger.warn("Interrupted while waiting for process termination", e);
                process.destroyForcibly();
            }
        }

        logger.info("Text shell processor stopped: {}", processorId);
    }
}
