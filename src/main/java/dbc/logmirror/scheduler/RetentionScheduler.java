package dbc.logmirror.scheduler;

import dbc.logmirror.config.ConfigurationManager;
import dbc.logmirror.mirror.MirrorManager;
import dbc.logmirror.model.LogDefinition;
import dbc.logmirror.retention.RetentionUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class RetentionScheduler {

    private static final Logger logger = LoggerFactory.getLogger(RetentionScheduler.class);

    private final ConfigurationManager configManager;

    public RetentionScheduler(ConfigurationManager configManager) {
        this.configManager = configManager;
    }

    @Scheduled(cron = "0 0 1 * * *") // 1 AM daily
    public void executeRetentionPolicies() {
        try {
            logger.debug("Running retention policies");
            List<LogDefinition> logs = configManager.getCurrentConfig().getLogs();

            for (LogDefinition log : logs) {
                if (log.getRetentionPolicy() == null) {
                    continue;
                }

                try {
                    Long maxAgeDays = log.getRetentionPolicy().getMaxAgeDays() != null ? 
                            log.getRetentionPolicy().getMaxAgeDays().longValue() : null;
                    
                    long deletedBytes = RetentionUtil.applyRetentionPolicy(
                            log.getLocalMirrorPath(),
                            maxAgeDays,
                            log.getRetentionPolicy().getMaxFiles(),
                            log.getRetentionPolicy().getMaxTotalSizeGB()
                    );

                    if (deletedBytes > 0) {
                        logger.info("Retention cleanup for {}: {} bytes deleted", 
                                log.getId(), deletedBytes);
                    }
                } catch (Exception e) {
                    logger.error("Error applying retention policy for log: {}", log.getId(), e);
                }
            }
        } catch (Exception e) {
            logger.error("Error executing retention policies", e);
        }
    }
}
