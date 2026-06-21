package dbc.logmirror.api;

import dbc.logmirror.mirror.MirrorManager;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@Tag(name = "Status", description = "Query service status and statistics")
@RestController
@RequestMapping("/api/status")
public class StatusController {

    private final MirrorManager mirrorManager;

    public StatusController(MirrorManager mirrorManager) {
        this.mirrorManager = mirrorManager;
    }

    @Operation(summary = "Get service status", description = "Retrieve current service status including active mirror count and detailed statistics")
    @ApiResponse(responseCode = "200", description = "Status retrieved successfully")
    @GetMapping
    public ResponseEntity<Map<String, Object>> getStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("activeMirrors", mirrorManager.getActiveMirrorCount());
        status.put("statistics", mirrorManager.getAllStatistics());
        status.put("timestamp", System.currentTimeMillis());
        return ResponseEntity.ok(status);
    }
}
