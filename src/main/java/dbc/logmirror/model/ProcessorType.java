package dbc.logmirror.model;

/**
 * Processor type determines how the processor operates.
 *
 * JAVA_CLASS: Direct Java class implementing LogEventProcessor (structured event processing)
 * EVENT_SHELL: Shell command using JSON protocol for LogEvent serialization (structured)
 * TEXT_SHELL: Shell command operating on plain text lines (simple text filtering/transformation)
 */
public enum ProcessorType {
    /**
     * Java class implementing LogEventProcessor interface.
     * Provides full access to LogEvent metadata.
     */
    JAVA_CLASS,

    /**
     * Shell command communicating via JSON protocol.
     * Receives and sends LogEvent objects serialized as JSON.
     * Examples: Python/Node scripts with full event processing logic.
     * @deprecated Use EVENT_SHELL for clarity
     */
    SHELL_COMMAND,

    /**
     * Shell command for text-based processing.
     * Operates on raw log lines without JSON serialization.
     * Ideal for grep, sed, awk, tr, cut, sort, uniq, etc.
     */
    TEXT_SHELL,

    /**
     * Shell command with JSON protocol for event processing.
     * Receives and sends LogEvent objects serialized as JSON.
     * Examples: Python/Node scripts needing event metadata.
     */
    EVENT_SHELL
}
