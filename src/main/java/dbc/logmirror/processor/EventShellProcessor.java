package dbc.logmirror.processor;

import dbc.logmirror.model.LogEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Event-based shell processor using JSON protocol.
 *
 * This processor runs a shell command and communicates with it using JSON-serialized
 * LogEvent objects. The shell script receives events as JSON on stdin and outputs
 * transformed events as JSON on stdout.
 *
 * This is useful for sophisticated event processing logic that needs access to
 * metadata, timestamps, server information, etc.
 *
 * Example shell script (Python):
 * ```python
 * import json
 * import sys
 *
 * for line in sys.stdin:
 *     try:
 *         event = json.loads(line)
 *         # Process event
 *         event['line'] = event['line'].upper()
 *         print(json.dumps(event))
 *     except:
 *         pass  # Drop invalid events
 * ```
 */
public class EventShellProcessor implements LogEventProcessor {

    private static final Logger logger = LoggerFactory.getLogger(EventShellProcessor.class);
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

    public EventShellProcessor(String processorId, String command, ObjectMapper objectMapper) {
        this.processorId = processorId;
        this.command = command;
        this.objectMapper = objectMapper;
    }

    @Override
    public void start() throws Exception {
        logger.info("Starting event shell processor: {} with command: {}", processorId, command);
        startProcess();
    }

    private void startProcess() throws Exception {
        ProcessBuilder pb = new ProcessBuilder("/bin/bash", "-c", command);
        pb.redirectErrorStream(true);

        process = pb.start();
        stdin = new BufferedWriter(
                new OutputStreamWriter(process.getOutputStream(), StandardCharsets.UTF_8));
        stdout = new BufferedReader(
                new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8));

        logger.info("Event shell processor started: {}", processorId);
        restartAttempts = 0;
    }

    @Override
    public List<LogEvent> process(LogEvent event) throws Exception {
        List<LogEvent> results = new ArrayList<>();

        try {
            if (process == null || !process.isAlive()) {
                logger.warn("Event shell process died, attempting restart");
                attemptRestart();
                if (process == null || !process.isAlive()) {
                    logger.error("Failed to restart event shell processor");
                    return results;  // Return empty list on failure
                }
            }

            // Send event as JSON line
            String eventJson = objectMapper.writeValueAsString(event);
            stdin.write(eventJson);
            stdin.write("\n");
            stdin.flush();

            // Read response - expect one line of JSON per input event
            String response = stdout.ready() ? stdout.readLine() : "";
            if (response == null) {
                logger.warn("Event shell processor returned null, process may have died");
                attemptRestart();
                return results;
            }

            // Empty line means event was dropped
            if (response.isEmpty()) {
                logger.debug("Event dropped by shell processor");
                return results;
            }

            // Parse response back into LogEvent
            try {
                LogEvent result = objectMapper.readValue(response, LogEvent.class);
                results.add(result);
            } catch (Exception e) {
                logger.warn("Failed to parse processor output: {}", response, e);
                // Drop invalid output
            }

        } catch (Exception e) {
            logger.error("Event shell processor error", e);
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
            logger.info("Attempting to restart event shell processor (attempt {}/{})",
                    restartAttempts, MAX_RESTART_ATTEMPTS);

            stop();
            Thread.sleep(RESTART_DELAY_MS);
            startProcess();

        } catch (Exception e) {
            logger.error("Failed to restart event shell processor", e);
        }
    }

    @Override
    public void stop() {
        logger.info("Stopping event shell processor: {}", processorId);

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
                    logger.warn("Event shell process did not terminate gracefully, destroying");
                    process.destroyForcibly();
                }
            } catch (InterruptedException e) {
                logger.warn("Interrupted while waiting for process termination", e);
                process.destroyForcibly();
            }
        }

        logger.info("Event shell processor stopped: {}", processorId);
    }
}
