package dbc.logmirror.monitoring;

import dbc.logmirror.mirror.MirrorManager;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.stereotype.Component;

@Component
public class MetricsPublisher {

    private final MeterRegistry meterRegistry;
    private final MirrorManager mirrorManager;
    private final AtomicInteger activeMirrors;

    public MetricsPublisher(MeterRegistry meterRegistry, MirrorManager mirrorManager) {
        this.meterRegistry = meterRegistry;
        this.mirrorManager = mirrorManager;
        this.activeMirrors = new AtomicInteger(0);

        // Register gauges
        meterRegistry.gauge("mirrors.active", activeMirrors);
    }

    public void recordProcessedEvent(String logId) {
        meterRegistry.counter("events.processed", Tags.of("logId", logId)).increment();
    }

    public void recordDroppedEvent(String logId) {
        meterRegistry.counter("events.dropped", Tags.of("logId", logId)).increment();
    }

    public void recordReconnect(String logId) {
        meterRegistry.counter("reconnects", Tags.of("logId", logId)).increment();
    }

    public void recordProcessorFailure(String logId, String processorId) {
        meterRegistry.counter("processor.failures", 
                Tags.of("logId", logId, "processorId", processorId)).increment();
    }

    public void recordRotation(String logId) {
        meterRegistry.counter("rotations", Tags.of("logId", logId)).increment();
    }

    public void recordRetentionDeletion(String logId, long bytes) {
        meterRegistry.counter("retention.deletions", Tags.of("logId", logId)).increment();
        meterRegistry.counter("retention.bytes.deleted", Tags.of("logId", logId)).increment(bytes);
    }
}
