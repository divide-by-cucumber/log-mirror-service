package dbc.logmirror.mirror;

import dbc.logmirror.config.ConfigurationManager;
import dbc.logmirror.model.LogDefinition;
import dbc.logmirror.model.MirrorStatistics;
import dbc.logmirror.model.Server;
import dbc.logmirror.processor.ProcessorFactory;
import dbc.logmirror.ssh.SSHConnectionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
public class MirrorManager {

    private static final Logger logger = LoggerFactory.getLogger(MirrorManager.class);

    private final ConfigurationManager configManager;
    private final SSHConnectionManager sshManager;
    private final ProcessorFactory processorFactory;

    private final Map<String, LogMirrorTask> activeTasks = new ConcurrentHashMap<>();
    private final ExecutorService executorService = Executors.newVirtualThreadPerTaskExecutor();

    public MirrorManager(ConfigurationManager configManager, SSHConnectionManager sshManager,
                         ProcessorFactory processorFactory) {
        this.configManager = configManager;
        this.sshManager = sshManager;
        this.processorFactory = processorFactory;
    }

    public void initializeFromConfiguration() throws Exception {
        logger.info("Initializing mirrors from configuration");

        List<LogDefinition> logs = configManager.getCurrentConfig().getLogs();
        for (LogDefinition log : logs) {
            if (log.isEnabled()) {
                startMirror(log.getId());
            }
        }

        logger.info("Mirrors initialized. Active: {}", activeTasks.size());
    }

    public void startMirror(String logId) throws Exception {
        if (activeTasks.containsKey(logId)) {
            logger.warn("Mirror already running for log: {}", logId);
            return;
        }

        LogDefinition logDef = configManager.getLog(logId)
                .orElseThrow(() -> new IllegalArgumentException("Log not found: " + logId));

        Server server = configManager.getServer(logDef.getServerId())
                .orElseThrow(() -> new IllegalArgumentException("Server not found: " + logDef.getServerId()));

        LogMirrorTask task = new LogMirrorTask(logDef, server, sshManager, processorFactory, configManager);
        activeTasks.put(logId, task);

        logger.info("Starting mirror for log: {} on server: {}", logId, logDef.getServerId());
        executorService.submit(task);
    }

    public void stopMirror(String logId) {
        LogMirrorTask task = activeTasks.get(logId);
        if (task == null) {
            logger.warn("Mirror not found for log: {}", logId);
            return;
        }

        logger.info("Stopping mirror for log: {}", logId);
        task.stop();
        activeTasks.remove(logId);
    }

    public void stopAllMirrors() {
        logger.info("Stopping all mirrors");
        List<String> logIds = new ArrayList<>(activeTasks.keySet());
        for (String logId : logIds) {
            stopMirror(logId);
        }
    }

    public Optional<MirrorStatistics> getMirrorStatistics(String logId) {
        LogMirrorTask task = activeTasks.get(logId);
        if (task != null) {
            return Optional.of(task.getStatistics());
        }
        return Optional.empty();
    }

    public Map<String, MirrorStatistics> getAllStatistics() {
        Map<String, MirrorStatistics> stats = new HashMap<>();
        for (Map.Entry<String, LogMirrorTask> entry : activeTasks.entrySet()) {
            stats.put(entry.getKey(), entry.getValue().getStatistics());
        }
        return stats;
    }

    public int getActiveMirrorCount() {
        return (int) activeTasks.values().stream().filter(LogMirrorTask::isRunning).count();
    }

    public void shutdown() {
        logger.info("Shutting down mirror manager");
        stopAllMirrors();
        sshManager.closeAllConnections();
        executorService.shutdown();
    }
}
