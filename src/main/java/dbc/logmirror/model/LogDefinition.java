package dbc.logmirror.model;

import java.util.ArrayList;
import java.util.List;

public class LogDefinition {

    private String id;
    private String serverId;
    private String sourcePath;
    private String localMirrorPath;
    private boolean enabled = true;
    private RotationPolicyConfig rotationPolicy;
    private RetentionPolicyConfig retentionPolicy;
    private List<ProcessorConfig> processors = new ArrayList<>();

    public LogDefinition() {
    }

    public LogDefinition(String id, String serverId, String sourcePath, String localMirrorPath) {
        this.id = id;
        this.serverId = serverId;
        this.sourcePath = sourcePath;
        this.localMirrorPath = localMirrorPath;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getServerId() {
        return serverId;
    }

    public void setServerId(String serverId) {
        this.serverId = serverId;
    }

    public String getSourcePath() {
        return sourcePath;
    }

    public void setSourcePath(String sourcePath) {
        this.sourcePath = sourcePath;
    }

    public String getLocalMirrorPath() {
        return localMirrorPath;
    }

    public void setLocalMirrorPath(String localMirrorPath) {
        this.localMirrorPath = localMirrorPath;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public RotationPolicyConfig getRotationPolicy() {
        return rotationPolicy;
    }

    public void setRotationPolicy(RotationPolicyConfig rotationPolicy) {
        this.rotationPolicy = rotationPolicy;
    }

    public RetentionPolicyConfig getRetentionPolicy() {
        return retentionPolicy;
    }

    public void setRetentionPolicy(RetentionPolicyConfig retentionPolicy) {
        this.retentionPolicy = retentionPolicy;
    }

    public List<ProcessorConfig> getProcessors() {
        return processors;
    }

    public void setProcessors(List<ProcessorConfig> processors) {
        this.processors = new ArrayList<>(processors);
    }
}
