package dbc.logmirror.pipeline;

import dbc.logmirror.model.LogEvent;
import dbc.logmirror.processor.LogEventProcessor;
import dbc.logmirror.processor.LogStreamProcessor;
import dbc.logmirror.processor.LogTextProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Pipeline executor for processing log events and text through multiple processors.
 *
 * The pipeline supports two types of processors:
 * 1. Text processors - operate on raw log lines (grep, sed, awk, etc.)
 * 2. Event processors - operate on structured LogEvent objects
 *
 * Processing flow:
 * Raw text line -> Text processors -> LogEvent creation -> Event processors -> Result
 */
public class PipelineExecutor {

    private static final Logger logger = LoggerFactory.getLogger(PipelineExecutor.class);

    private final List<LogStreamProcessor> eventProcessors = new ArrayList<>();
    private final List<LogTextProcessor> textProcessors = new ArrayList<>();
    private final List<Object> allProcessors = new ArrayList<>();

    /**
     * Add an event processor (legacy interface for backward compatibility)
     */
    public void addProcessor(LogStreamProcessor processor) {
        eventProcessors.add(processor);
        allProcessors.add(processor);
    }

    /**
     * Add a text processor
     */
    public void addTextProcessor(LogTextProcessor processor) {
        textProcessors.add(processor);
        allProcessors.add(processor);
    }

    /**
     * Add an event processor
     */
    public void addEventProcessor(LogEventProcessor processor) {
        eventProcessors.add(new EventProcessorAdapter(processor));
        allProcessors.add(processor);
    }

    /**
     * Start all processors in the pipeline
     */
    public void startAll() throws Exception {
        for (int i = 0; i < allProcessors.size(); i++) {
            try {
                Object processor = allProcessors.get(i);
                if (processor instanceof LogTextProcessor) {
                    ((LogTextProcessor) processor).start();
                } else if (processor instanceof LogEventProcessor) {
                    ((LogEventProcessor) processor).start();
                } else if (processor instanceof LogStreamProcessor) {
                    ((LogStreamProcessor) processor).start();
                }
                logger.debug("Started processor {}: {}", i, processor.getClass().getSimpleName());
            } catch (Exception e) {
                logger.error("Failed to start processor {}", i, e);
                // Rollback by stopping previously started processors
                for (int j = i - 1; j >= 0; j--) {
                    try {
                        Object processor = allProcessors.get(j);
                        if (processor instanceof LogTextProcessor) {
                            ((LogTextProcessor) processor).stop();
                        } else if (processor instanceof LogEventProcessor) {
                            ((LogEventProcessor) processor).stop();
                        } else if (processor instanceof LogStreamProcessor) {
                            ((LogStreamProcessor) processor).stop();
                        }
                    } catch (Exception ex) {
                        logger.error("Failed to stop processor {} during rollback", j, ex);
                    }
                }
                throw e;
            }
        }
    }

    /**
     * Stop all processors in reverse order
     */
    public void stopAll() {
        for (int i = allProcessors.size() - 1; i >= 0; i--) {
            try {
                Object processor = allProcessors.get(i);
                if (processor instanceof LogTextProcessor) {
                    ((LogTextProcessor) processor).stop();
                } else if (processor instanceof LogEventProcessor) {
                    ((LogEventProcessor) processor).stop();
                } else if (processor instanceof LogStreamProcessor) {
                    ((LogStreamProcessor) processor).stop();
                }
                logger.debug("Stopped processor {}: {}", i, processor.getClass().getSimpleName());
            } catch (Exception e) {
                logger.error("Failed to stop processor {}", i, e);
            }
        }
    }

    /**
     * Execute the full pipeline starting from a raw text line.
     *
     * @param logId The log definition ID
     * @param serverId The server ID
     * @param line The raw log line
     * @return List of processed log events (may be empty if all dropped, or multiple if processors split)
     * @throws Exception If processing fails
     */
    public List<LogEvent> executeFromText(String logId, String serverId, String line) throws Exception {
        List<LogEvent> results = new ArrayList<>();

        try {
            // Phase 1: Pass through text processors
            List<String> processedLines = new ArrayList<>();
            processedLines.add(line);

            for (LogTextProcessor textProc : textProcessors) {
                List<String> nextLines = new ArrayList<>();
                for (String currentLine : processedLines) {
                    try {
                        List<String> output = textProc.process(currentLine);
                        nextLines.addAll(output);
                    } catch (Exception e) {
                        logger.error("Text processor failed", e);
                        // Drop line on error
                    }
                }
                processedLines = nextLines;

                // If all lines were dropped, return empty
                if (processedLines.isEmpty()) {
                    logger.debug("All lines dropped by text processor");
                    return results;
                }
            }

            // Phase 2: Convert to LogEvent and pass through event processors
            for (String processedLine : processedLines) {
                LogEvent event = new LogEvent(logId, serverId, java.time.Instant.now(), processedLine);

                for (LogStreamProcessor eventProc : eventProcessors) {
                    try {
                        Optional<LogEvent> result = eventProc.process(event);
                        if (result.isPresent()) {
                            event = result.get();
                        } else {
                            logger.debug("Event dropped by event processor");
                            event = null;
                            break;
                        }
                    } catch (Exception e) {
                        logger.error("Event processor failed", e);
                        throw e;
                    }
                }

                if (event != null) {
                    results.add(event);
                }
            }

        } catch (Exception e) {
            logger.error("Pipeline execution failed", e);
            throw e;
        }

        return results;
    }

    /**
     * Execute the pipeline starting from a LogEvent (legacy method for backward compatibility)
     */
    public Optional<LogEvent> execute(LogEvent event) throws Exception {
        try {
            Optional<LogEvent> current = Optional.of(event);

            for (int i = 0; i < eventProcessors.size(); i++) {
                if (!current.isPresent()) {
                    logger.debug("Event dropped by processor {}", i - 1);
                    return Optional.empty();
                }

                try {
                    current = eventProcessors.get(i).process(current.get());
                } catch (Exception e) {
                    logger.error("Processor {} failed to process event", i, e);
                    throw e;
                }
            }

            return current;
        } catch (Exception e) {
            logger.error("Pipeline execution failed", e);
            throw e;
        }
    }

    public int getProcessorCount() {
        return allProcessors.size();
    }

    /**
     * Adapter to wrap LogEventProcessor as LogStreamProcessor for pipeline compatibility
     */
    private static class EventProcessorAdapter implements LogStreamProcessor {
        private final LogEventProcessor delegate;

        EventProcessorAdapter(LogEventProcessor delegate) {
            this.delegate = delegate;
        }

        @Override
        public void start() throws Exception {
            delegate.start();
        }

        @Override
        public Optional<LogEvent> process(LogEvent event) throws Exception {
            var results = delegate.process(event);
            return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
        }

        @Override
        public void stop() {
            delegate.stop();
        }
    }
}

