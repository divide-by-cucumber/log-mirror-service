package dbc.logmirror.processor;

import dbc.logmirror.model.LogEvent;

import java.util.List;

/**
 * Processes structured log events.
 *
 * Event processors operate on complete LogEvent objects and have access to
 * metadata, timestamps, server info, etc. They can transform, enrich, duplicate,
 * or drop events.
 *
 * Examples: Java-based enrichment processors, JSON-based shell processors,
 * webhook processors, database processors, etc.
 */
public interface LogEventProcessor {

    /**
     * Initialize the processor. Called once before processing begins.
     * Use for resource allocation, subprocess startup, etc.
     */
    void start() throws Exception;

    /**
     * Process a single log event.
     *
     * @param event The log event to process
     * @return List of events. May be empty (drop event), single item (pass through),
     *         or multiple items (duplicate/enrich with multiple outputs)
     * @throws Exception If processing fails
     */
    List<LogEvent> process(LogEvent event) throws Exception;

    /**
     * Shutdown the processor. Called once when processing ends.
     * Use for resource cleanup, subprocess termination, etc.
     */
    void stop();
}
