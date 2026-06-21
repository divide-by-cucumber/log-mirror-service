package dbc.logmirror.model;

import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class LogEvent {

    private String logId;
    private String serverId;
    private Instant timestamp;
    private String line;
    private Map<String, Object> attributes = new HashMap<>();

    public LogEvent() {
    }

    public LogEvent(String logId, String serverId, Instant timestamp, String line) {
        this.logId = logId;
        this.serverId = serverId;
        this.timestamp = timestamp;
        this.line = line;
    }

    public String getLogId() {
        return logId;
    }

    public void setLogId(String logId) {
        this.logId = logId;
    }

    public String getServerId() {
        return serverId;
    }

    public void setServerId(String serverId) {
        this.serverId = serverId;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    public String getLine() {
        return line;
    }

    public void setLine(String line) {
        this.line = line;
    }

    public Map<String, Object> getAttributes() {
        return Collections.unmodifiableMap(attributes);
    }

    public void setAttributes(Map<String, Object> attributes) {
        this.attributes = new HashMap<>(attributes);
    }

    public void putAttribute(String key, Object value) {
        this.attributes.put(key, value);
    }
}
