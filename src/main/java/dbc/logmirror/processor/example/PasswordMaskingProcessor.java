package dbc.logmirror.processor.example;

import dbc.logmirror.model.LogEvent;
import dbc.logmirror.processor.LogStreamProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Example processor that masks passwords and sensitive data in log lines.
 */
public class PasswordMaskingProcessor implements LogStreamProcessor {

    private static final Logger logger = LoggerFactory.getLogger(PasswordMaskingProcessor.class);

    private static final Pattern PASSWORD_PATTERN = Pattern.compile(
            "(?i)(password|passwd|pwd|secret|token)\\s*[:=]\\s*([^\\s,}]+)"
    );

    @Override
    public void start() throws Exception {
        logger.info("PasswordMaskingProcessor started");
    }

    @Override
    public Optional<LogEvent> process(LogEvent event) throws Exception {
        String maskedLine = PASSWORD_PATTERN.matcher(event.getLine())
                .replaceAll("$1=***");

        event.setLine(maskedLine);
        return Optional.of(event);
    }

    @Override
    public void stop() {
        logger.info("PasswordMaskingProcessor stopped");
    }
}
