package dbc.logmirror.processor;

import dbc.logmirror.model.LogEvent;

import java.util.Optional;

public interface LogStreamProcessor {

    void start() throws Exception;

    Optional<LogEvent> process(LogEvent event) throws Exception;

    void stop();
}
