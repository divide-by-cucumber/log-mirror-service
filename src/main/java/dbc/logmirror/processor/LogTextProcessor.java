package dbc.logmirror.processor;

import java.util.List;

/**
 * Processes plain text log lines.
 *
 * Text processors operate on raw log lines as strings. They are ideal for
 * common Unix tools and text transformations that don't need structured
 * event information.
 *
 * Examples: grep, sed, awk, tr, cut, sort, uniq, etc.
 *
 * Text processors are more efficient than event processors for simple
 * line-based filtering and transformation as they avoid JSON serialization
 * overhead.
 */
public interface LogTextProcessor {

    /**
     * Initialize the processor. Called once before processing begins.
     * Use for resource allocation, subprocess startup, etc.
     */
    void start() throws Exception;

    /**
     * Process a single log line as text.
     *
     * @param line The raw log line to process
     * @return List of transformed lines. May be empty (drop line), single item (pass through),
     *         or multiple items (split or duplicate)
     * @throws Exception If processing fails
     */
    List<String> process(String line) throws Exception;

    /**
     * Shutdown the processor. Called once when processing ends.
     * Use for resource cleanup, subprocess termination, etc.
     */
    void stop();
}
