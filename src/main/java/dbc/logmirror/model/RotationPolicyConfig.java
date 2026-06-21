package dbc.logmirror.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class RotationPolicyConfig {

    @JsonProperty("type")
    private RotationType type;

    @JsonProperty("maxSizeMB")
    private Integer maxSizeMB;

    @JsonProperty("timeUnit")
    private TimeUnit timeUnit;

    @JsonProperty("inactivityMinutes")
    private Integer inactivityMinutes;

    @JsonProperty("gzipCompressed")
    private boolean gzipCompressed = false;

    public RotationPolicyConfig() {
    }

    public RotationType getType() {
        return type;
    }

    public void setType(RotationType type) {
        this.type = type;
    }

    public Integer getMaxSizeMB() {
        return maxSizeMB;
    }

    public void setMaxSizeMB(Integer maxSizeMB) {
        this.maxSizeMB = maxSizeMB;
    }

    public TimeUnit getTimeUnit() {
        return timeUnit;
    }

    public void setTimeUnit(TimeUnit timeUnit) {
        this.timeUnit = timeUnit;
    }

    public Integer getInactivityMinutes() {
        return inactivityMinutes;
    }

    public void setInactivityMinutes(Integer inactivityMinutes) {
        this.inactivityMinutes = inactivityMinutes;
    }

    public boolean isGzipCompressed() {
        return gzipCompressed;
    }

    public void setGzipCompressed(boolean gzipCompressed) {
        this.gzipCompressed = gzipCompressed;
    }

    public enum RotationType {
        SIZE,
        TIME,
        INACTIVITY,
        SOURCE_ROTATION
    }

    public enum TimeUnit {
        HOURLY,
        DAILY,
        WEEKLY
    }
}
