package dbc.logmirror.api;

import dbc.logmirror.config.ConfigurationManager;
import dbc.logmirror.mirror.MirrorManager;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Tag(name = "Admin", description = "Administrative operations for configuration and mirror management")
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private static final Logger logger = LoggerFactory.getLogger(AdminController.class);

    private final ConfigurationManager configManager;
    private final MirrorManager mirrorManager;

    public AdminController(ConfigurationManager configManager, MirrorManager mirrorManager) {
        this.configManager = configManager;
        this.mirrorManager = mirrorManager;
    }

    @Operation(summary = "Reload configuration", description = "Reload configuration from disk and restart all mirrors")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Configuration reloaded successfully"),
        @ApiResponse(responseCode = "500", description = "Error reloading configuration")
    })
    @PostMapping("/reload")
    public ResponseEntity<Map<String, String>> reload() {
        try {
            logger.info("Reloading configuration");
            configManager.reload();

            // Restart mirrors based on new configuration
            mirrorManager.stopAllMirrors();
            mirrorManager.initializeFromConfiguration();

            Map<String, String> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Configuration reloaded");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error reloading configuration", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Operation(summary = "Save configuration", description = "Save current configuration to disk")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Configuration saved successfully"),
        @ApiResponse(responseCode = "500", description = "Error saving configuration")
    })
    @PostMapping("/save")
    public ResponseEntity<Map<String, String>> save() {
        try {
            logger.info("Saving configuration");
            configManager.save();

            Map<String, String> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Configuration saved");
            return ResponseEntity.ok(response);
        } catch (IOException e) {
            logger.error("Error saving configuration", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Operation(summary = "Start log mirror", description = "Start mirroring for a specific log")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Mirror started successfully"),
        @ApiResponse(responseCode = "500", description = "Error starting mirror")
    })
    @PostMapping("/start/{logId}")
    public ResponseEntity<Map<String, String>> startMirror(
            @Parameter(description = "Log ID") @PathVariable String logId) {
        try {
            logger.info("Starting mirror for log: {}", logId);
            mirrorManager.startMirror(logId);

            Map<String, String> response = new HashMap<>();
            response.put("status", "success");
            response.put("logId", logId);
            response.put("message", "Mirror started");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error starting mirror", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Operation(summary = "Stop log mirror", description = "Stop mirroring for a specific log")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Mirror stopped successfully"),
        @ApiResponse(responseCode = "500", description = "Error stopping mirror")
    })
    @PostMapping("/stop/{logId}")
    public ResponseEntity<Map<String, String>> stopMirror(
            @Parameter(description = "Log ID") @PathVariable String logId) {
        try {
            logger.info("Stopping mirror for log: {}", logId);
            mirrorManager.stopMirror(logId);

            Map<String, String> response = new HashMap<>();
            response.put("status", "success");
            response.put("logId", logId);
            response.put("message", "Mirror stopped");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error stopping mirror", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
