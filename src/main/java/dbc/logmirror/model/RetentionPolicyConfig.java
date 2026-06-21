package dbc.logmirror.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class RetentionPolicyConfig {

    @JsonProperty("type")
    private RetentionType type;

    @JsonProperty("maxAgeDays")
    private Integer maxAgeDays;

    @JsonProperty("maxFiles")
    private Integer maxFiles;

    @JsonProperty("maxTotalSizeGB")
    private Double maxTotalSizeGB;

    public RetentionPolicyConfig() {
    }

    public RetentionType getType() {
        return type;
    }

    public void setType(RetentionType type) {
        this.type = type;
    }

    public Integer getMaxAgeDays() {
        return maxAgeDays;
    }

    public void setMaxAgeDays(Integer maxAgeDays) {
        this.maxAgeDays = maxAgeDays;
    }

    public Integer getMaxFiles() {
        return maxFiles;
    }

    public void setMaxFiles(Integer maxFiles) {
        this.maxFiles = maxFiles;
    }

    public Double getMaxTotalSizeGB() {
        return maxTotalSizeGB;
    }

    public void setMaxTotalSizeGB(Double maxTotalSizeGB) {
        this.maxTotalSizeGB = maxTotalSizeGB;
    }

    public enum RetentionType {
        AGE,
        COUNT,
        SIZE
    }
}
