package dbc.logmirror.api;

import dbc.logmirror.config.ConfigurationManager;
import dbc.logmirror.model.LogDefinition;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;

@Tag(name = "Logs", description = "Manage log mirror definitions")
@RestController
@RequestMapping("/api/logs")
public class LogController {

    private static final Logger logger = LoggerFactory.getLogger(LogController.class);

    private final ConfigurationManager configManager;

    public LogController(ConfigurationManager configManager) {
        this.configManager = configManager;
    }

    @Operation(summary = "Get all log definitions", description = "Retrieve list of all configured log mirror definitions")
    @ApiResponse(responseCode = "200", description = "List of logs retrieved successfully",
            content = @Content(mediaType = "application/json", schema = @Schema(type = "array")))
    @GetMapping
    public ResponseEntity<List<LogDefinition>> getAllLogs() {
        try {
            List<LogDefinition> logs = configManager.getCurrentConfig().getLogs();
            return ResponseEntity.ok(logs);
        } catch (Exception e) {
            logger.error("Error retrieving logs", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Operation(summary = "Get log definition by ID", description = "Retrieve a specific log mirror definition")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Log found"),
        @ApiResponse(responseCode = "404", description = "Log not found"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/{id}")
    public ResponseEntity<LogDefinition> getLog(
            @Parameter(description = "Log ID") @PathVariable String id) {
        try {
            LogDefinition log = configManager.getLog(id)
                    .orElseThrow(() -> new IllegalArgumentException("Log not found: " + id));
            return ResponseEntity.ok(log);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (Exception e) {
            logger.error("Error retrieving log", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Operation(summary = "Create new log definition", description = "Create a new log mirror definition")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Log created successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid log configuration"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping
    public ResponseEntity<LogDefinition> createLog(@RequestBody LogDefinition log) {
        try {
            LogDefinition created = configManager.addLog(log);
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid log configuration: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        } catch (IOException e) {
            logger.error("Error creating log", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Operation(summary = "Update log definition", description = "Update an existing log mirror definition")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Log updated successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid log configuration"),
        @ApiResponse(responseCode = "404", description = "Log not found"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PutMapping("/{id}")
    public ResponseEntity<LogDefinition> updateLog(
            @Parameter(description = "Log ID") @PathVariable String id,
            @RequestBody LogDefinition log) {
        try {
            LogDefinition updated = configManager.updateLog(id, log);
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid log configuration: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        } catch (IOException e) {
            logger.error("Error updating log", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Operation(summary = "Delete log definition", description = "Delete a log mirror definition")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Log deleted successfully"),
        @ApiResponse(responseCode = "404", description = "Log not found"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteLog(
            @Parameter(description = "Log ID") @PathVariable String id) {
        try {
            configManager.deleteLog(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            logger.warn("Log not found: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (IOException e) {
            logger.error("Error deleting log", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
