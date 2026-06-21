package dbc.logmirror.api;

import dbc.logmirror.config.ConfigurationManager;
import dbc.logmirror.model.Server;
import dbc.logmirror.security.SecretMasker;
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
import java.util.stream.Collectors;

@Tag(name = "Servers", description = "Manage SSH servers for log mirroring")
@RestController
@RequestMapping("/api/servers")
public class ServerController {

    private static final Logger logger = LoggerFactory.getLogger(ServerController.class);

    private final ConfigurationManager configManager;

    public ServerController(ConfigurationManager configManager) {
        this.configManager = configManager;
    }

    @Operation(summary = "Get all SSH servers", description = "Retrieve list of all configured SSH servers with masked credentials")
    @ApiResponse(responseCode = "200", description = "List of servers retrieved successfully",
            content = @Content(mediaType = "application/json", schema = @Schema(type = "array")))
    @GetMapping
    public ResponseEntity<List<Server>> getAllServers() {
        try {
            List<Server> servers = configManager.getCurrentConfig().getServers().stream()
                    .map(s -> configManager.getMaskedServer(s.getId()))
                    .collect(Collectors.toList());
            return ResponseEntity.ok(servers);
        } catch (Exception e) {
            logger.error("Error retrieving servers", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Operation(summary = "Get SSH server by ID", description = "Retrieve a specific SSH server configuration with masked credentials")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Server found"),
        @ApiResponse(responseCode = "404", description = "Server not found"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/{id}")
    public ResponseEntity<Server> getServer(
            @Parameter(description = "Server ID") @PathVariable String id) {
        try {
            Server server = configManager.getMaskedServer(id);
            return ResponseEntity.ok(server);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (Exception e) {
            logger.error("Error retrieving server", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Operation(summary = "Create new SSH server", description = "Create a new SSH server configuration")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Server created successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid server configuration"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping
    public ResponseEntity<Server> createServer(@RequestBody Server server) {
        try {
            Server created = configManager.addServer(server);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(configManager.getMaskedServer(created.getId()));
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid server configuration: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        } catch (IOException e) {
            logger.error("Error creating server", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Operation(summary = "Update SSH server", description = "Update an existing SSH server configuration")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Server updated successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid server configuration"),
        @ApiResponse(responseCode = "404", description = "Server not found"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PutMapping("/{id}")
    public ResponseEntity<Server> updateServer(
            @Parameter(description = "Server ID") @PathVariable String id,
            @RequestBody Server server) {
        try {
            Server updated = configManager.updateServer(id, server);
            return ResponseEntity.ok(configManager.getMaskedServer(updated.getId()));
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid server configuration: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        } catch (IOException e) {
            logger.error("Error updating server", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Operation(summary = "Delete SSH server", description = "Delete an SSH server configuration")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Server deleted successfully"),
        @ApiResponse(responseCode = "400", description = "Cannot delete server"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteServer(
            @Parameter(description = "Server ID") @PathVariable String id) {
        try {
            configManager.deleteServer(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            logger.warn("Cannot delete server: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        } catch (IOException e) {
            logger.error("Error deleting server", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
