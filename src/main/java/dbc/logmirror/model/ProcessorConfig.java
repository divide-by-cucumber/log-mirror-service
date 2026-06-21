package dbc.logmirror.model;

import java.util.HashMap;
import java.util.Map;

public class ProcessorConfig {

    private String id;
    private ProcessorType type;
    private String className;
    private String command;
    private Map<String, String> parameters = new HashMap<>();

    public ProcessorConfig() {
    }

    public ProcessorConfig(String id, ProcessorType type) {
        this.id = id;
        this.type = type;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public ProcessorType getType() {
        return type;
    }

    public void setType(ProcessorType type) {
        this.type = type;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public String getCommand() {
        return command;
    }

    public void setCommand(String command) {
        this.command = command;
    }

    public Map<String, String> getParameters() {
        return parameters;
    }

    public void setParameters(Map<String, String> parameters) {
        this.parameters = new HashMap<>(parameters);
    }
}
