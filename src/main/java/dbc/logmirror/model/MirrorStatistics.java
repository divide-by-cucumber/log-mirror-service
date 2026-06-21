package dbc.logmirror.model;

import java.time.Instant;

public class MirrorStatistics {

    private String logId;
    private long bytesWritten = 0;
    private long linesReceived = 0;
    private int reconnectCount = 0;
    private Instant lastEventTimestamp;
    private int rotationCount = 0;
    private boolean active = false;

    public MirrorStatistics(String logId) {
        this.logId = logId;
    }

    public String getLogId() {
        return logId;
    }

    public long getBytesWritten() {
        return bytesWritten;
    }

    public void incrementBytesWritten(long count) {
        this.bytesWritten += count;
    }

    public long getLinesReceived() {
        return linesReceived;
    }

    public void incrementLinesReceived() {
        this.linesReceived++;
    }

    public int getReconnectCount() {
        return reconnectCount;
    }

    public void incrementReconnectCount() {
        this.reconnectCount++;
    }

    public Instant getLastEventTimestamp() {
        return lastEventTimestamp;
    }

    public void setLastEventTimestamp(Instant timestamp) {
        this.lastEventTimestamp = timestamp;
    }

    public int getRotationCount() {
        return rotationCount;
    }

    public void incrementRotationCount() {
        this.rotationCount++;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
