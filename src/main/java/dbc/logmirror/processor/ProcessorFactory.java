package dbc.logmirror.processor;

import dbc.logmirror.model.LogEvent;
import dbc.logmirror.model.ProcessorConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

/**
 * Factory for creating processors.
 *
 * Supports both event-based processors (operating on LogEvent) and text-based
 * processors (operating on raw log lines).
 */
@Component
public class ProcessorFactory {

    private static final Logger logger = LoggerFactory.getLogger(ProcessorFactory.class);

    private final ApplicationContext applicationContext;
    private final ObjectMapper objectMapper;

    public ProcessorFactory(ApplicationContext applicationContext, ObjectMapper objectMapper) {
        this.applicationContext = applicationContext;
        this.objectMapper = objectMapper;
    }

    /**
     * Create an event processor from configuration.
     */
    public LogEventProcessor createEventProcessor(ProcessorConfig config) throws Exception {
        logger.debug("Creating event processor: {} of type {}", config.getId(), config.getType());

        switch (config.getType()) {
            case JAVA_CLASS:
                return createJavaEventProcessor(config);
            case EVENT_SHELL:
            case SHELL_COMMAND:  // Backward compatibility
                return createEventShellProcessor(config);
            default:
                throw new IllegalArgumentException(
                        "Cannot create event processor from type: " + config.getType());
        }
    }

    /**
     * Create a text processor from configuration.
     */
    public LogTextProcessor createTextProcessor(ProcessorConfig config) throws Exception {
        logger.debug("Creating text processor: {} of type {}", config.getId(), config.getType());

        switch (config.getType()) {
            case TEXT_SHELL:
                return createTextShellProcessor(config);
            default:
                throw new IllegalArgumentException(
                        "Cannot create text processor from type: " + config.getType());
        }
    }

    private LogEventProcessor createJavaEventProcessor(ProcessorConfig config) throws Exception {
        if (config.getClassName() == null || config.getClassName().isEmpty()) {
            throw new IllegalArgumentException("Java processor requires className");
        }

        try {
            Class<?> clazz = Class.forName(config.getClassName());

            if (!LogEventProcessor.class.isAssignableFrom(clazz)) {
                throw new IllegalArgumentException(
                        "Class does not implement LogEventProcessor: " + config.getClassName());
            }

            // Try to get from Spring context first
            try {
                Object bean = applicationContext.getBean(clazz);
                return (LogEventProcessor) bean;
            } catch (Exception e) {
                // Not a Spring bean, instantiate directly
                LogEventProcessor processor = (LogEventProcessor) clazz.getDeclaredConstructor().newInstance();

                // Autowire if possible
                AutowireCapableBeanFactory beanFactory = applicationContext.getAutowireCapableBeanFactory();
                beanFactory.autowireBean(processor);

                logger.info("Created Java event processor: {}", config.getClassName());
                return processor;
            }
        } catch (ClassNotFoundException e) {
            logger.error("Processor class not found: {}", config.getClassName(), e);
            throw new Exception("Processor class not found: " + config.getClassName(), e);
        }
    }

    private LogEventProcessor createEventShellProcessor(ProcessorConfig config) throws Exception {
        if (config.getCommand() == null || config.getCommand().isEmpty()) {
            throw new IllegalArgumentException("Event shell processor requires command");
        }

        logger.info("Created event shell processor: {} with command: {}", config.getId(), config.getCommand());
        return new EventShellProcessor(config.getId(), config.getCommand(), objectMapper);
    }

    private LogTextProcessor createTextShellProcessor(ProcessorConfig config) throws Exception {
        if (config.getCommand() == null || config.getCommand().isEmpty()) {
            throw new IllegalArgumentException("Text shell processor requires command");
        }

        logger.info("Created text shell processor: {} with command: {}", config.getId(), config.getCommand());
        return new TextShellProcessor(config.getId(), config.getCommand());
    }

    // Backward compatibility method - creates event processor
    public LogStreamProcessor createProcessor(ProcessorConfig config) throws Exception {
        logger.debug("Creating processor (legacy): {} of type {}", config.getId(), config.getType());

        // For backward compatibility, treat all as event processors
        if (config.getType() == null) {
            throw new IllegalArgumentException("Processor type is required");
        }

        switch (config.getType()) {
            case JAVA_CLASS:
                return new EventProcessorAdapter(createJavaEventProcessor(config));
            case SHELL_COMMAND:
            case EVENT_SHELL:
                return new EventProcessorAdapter(createEventShellProcessor(config));
            default:
                throw new IllegalArgumentException("Unknown processor type: " + config.getType());
        }
    }

    /**
     * Adapter to wrap LogEventProcessor as LogStreamProcessor for backward compatibility.
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
        public java.util.Optional<LogEvent> process(LogEvent event) throws Exception {
            var results = delegate.process(event);
            return results.isEmpty() ? java.util.Optional.empty() : java.util.Optional.of(results.get(0));
        }

        @Override
        public void stop() {
            delegate.stop();
        }
    }
}

