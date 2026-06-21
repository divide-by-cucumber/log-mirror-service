package dbc.logmirror.pipeline;

import dbc.logmirror.pipeline.PipelineExecutor;
import dbc.logmirror.model.LogEvent;
import dbc.logmirror.processor.LogStreamProcessor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class PipelineExecutorTest {

    private PipelineExecutor executor;

    @BeforeEach
    void setUp() {
        executor = new PipelineExecutor();
    }

    @Test
    void testExecutorEmptyPipeline() throws Exception {
        LogEvent event = new LogEvent("log1", "server1", Instant.now(), "test line");
        Optional<LogEvent> result = executor.execute(event);

        assertTrue(result.isPresent());
        assertEquals("test line", result.get().getLine());
    }

    @Test
    void testExecutorWithProcessors() throws Exception {
        // Add processor that appends "_processed"
        executor.addProcessor(new LogStreamProcessor() {
            @Override
            public void start() throws Exception {}

            @Override
            public Optional<LogEvent> process(LogEvent event) throws Exception {
                event.setLine(event.getLine() + "_processed");
                return Optional.of(event);
            }

            @Override
            public void stop() {}
        });

        LogEvent event = new LogEvent("log1", "server1", Instant.now(), "test");
        try {
            executor.startAll();
            Optional<LogEvent> result = executor.execute(event);
            executor.stopAll();

            assertTrue(result.isPresent());
            assertEquals("test_processed", result.get().getLine());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void testExecutorDropEvent() throws Exception {
        // Add processor that drops events
        executor.addProcessor(new LogStreamProcessor() {
            @Override
            public void start() throws Exception {}

            @Override
            public Optional<LogEvent> process(LogEvent event) throws Exception {
                return Optional.empty();
            }

            @Override
            public void stop() {}
        });

        LogEvent event = new LogEvent("log1", "server1", Instant.now(), "test");
        try {
            executor.startAll();
            Optional<LogEvent> result = executor.execute(event);
            executor.stopAll();

            assertFalse(result.isPresent());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void testExecutorProcessorCount() {
        assertEquals(0, executor.getProcessorCount());
        
        executor.addProcessor(new LogStreamProcessor() {
            @Override
            public void start() throws Exception {}

            @Override
            public Optional<LogEvent> process(LogEvent event) throws Exception {
                return Optional.of(event);
            }

            @Override
            public void stop() {}
        });

        assertEquals(1, executor.getProcessorCount());
    }
}
