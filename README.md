# Log Mirror Service

A simple service for configuration-driven log mirroring and processing from remote Linux servers via SSH.

## Overview

The Log Mirror Service continuously follows log files on remote Linux servers using SSH and `tail -F`, applies configurable processing pipelines to each log event, and writes the processed logs to local mirror files. Multiple servers and multiple log files per server are fully supported.

## Typical Use Cases

The Log Mirror Service is designed for situations where logs from multiple systems need to be collected, filtered, enriched, and retained locally without deploying heavyweight log aggregation infrastructure.

### Centralized Log Collection

Mirror application, web server, database, and system logs from multiple Linux servers into a central location for troubleshooting, auditing, and analysis.

Examples:
* Nginx access and error logs
* Apache HTTPD logs
* Java application logs
* Systemd and syslog files
* Database logs

### Lightweight Alternative to ELK / Splunk

For small and medium-sized environments, the service provides centralized log collection and processing without the operational overhead of Elasticsearch, Logstash, Fluentd, Splunk, or similar platforms.

### Security and Compliance Archiving

Create immutable local copies of critical logs before they are rotated or deleted on the source systems.

Examples:
* Authentication logs
* Security audit logs
* Application audit trails
* Regulatory compliance records

### Log Filtering and Noise Reduction

Use standard Unix tools such as `grep`, `awk`, and `sed` to remove unimportant log entries before they are stored.

Examples:
* Remove DEBUG messages
* Keep only ERROR and WARN entries
* Extract specific fields from access logs
* Mask passwords or sensitive information

### Log Enrichment and Transformation

Add metadata or perform structured processing using Java or script-based processors.

Examples:
* Add environment information
* Tag log entries with application metadata
* Parse custom log formats
* Enrich events with external lookup information

### Migration and Integration Projects

Mirror logs from legacy systems and transform them into formats required by downstream tools.

Examples:
* Convert legacy logs into JSON
* Forward selected events to external systems
* Prepare data for analytics pipelines
* Feed monitoring or alerting solutions

### Operations and Troubleshooting

Maintain local mirrors of remote logs for operational support teams.

Benefits:
* Access logs without SSHing into production servers
* Retain historical data beyond source retention limits
* Search and analyze logs locally
* Reduce load on production systems

### Multi-Stage Processing Pipelines

Combine fast text-based filtering with structured event enrichment.

Example pipeline:
```text
Nginx Access Log
    ↓
grep -v DEBUG
    ↓
grep ERROR
    ↓
GeoIP Enrichment
    ↓
Compliance Masking
    ↓
Local Mirror File
```

This architecture allows high-volume filtering to be performed efficiently with standard Unix tools while more complex business logic is implemented in Java or script-based processors.

## Key Features

- **Remote Log Tailing**: SSH-based log streaming using Apache SSHD library
- **Dual-Tier Processing Pipeline**: Chain multiple text processors (plain Unix tools) and event processors (structured enrichment) in a single pipeline
- **Plain Text Processing**: Direct support for grep, sed, awk, tr, cut and other standard Unix tools without JSON overhead
- **Structured Event Processing**: Enrichment, validation, and transformation of LogEvent objects with full metadata access
- **REST API Management**: Full REST API for managing servers, logs, and system operations
- **JSON Configuration**: Configuration stored in JSON, reloadable at runtime without restart
- **Environment Variable Resolution**: Support for credential placeholders with environment variable resolution
- **Rotation Policies**: SIZE, TIME, INACTIVITY, and SOURCE_ROTATION policies with optional gzip compression
- **Retention Policies**: Cleanup based on age, file count, or total size
- **Virtual Threads**: Uses Java 21 virtual threads for efficient concurrency
- **Monitoring**: Spring Boot Actuator and Micrometer integration for metrics
- **Security**: Credential masking in API responses, shell processor validation, never logs secrets

## Architecture

```
┌─────────────────────────────────────────────────────────┐
│                   REST API Controllers                  │
│  (ServerController, LogController, AdminController)     │
└─────────────────┬───────────────────────────────────────┘
                  │
┌─────────────────▼───────────────────────────────────────┐
│            Configuration Manager                        │
│  (Persistence, Validation, Environment Resolution)      │
└─────────────────┬───────────────────────────────────────┘
                  │
┌─────────────────▼───────────────────────────────────────┐
│              Mirror Manager                             │
│  (Manages multiple LogMirrorTask instances)             │
└────┬────────────┬────────────┬──────────────┬───────────┘
     │            │            │              │
┌────▼──┐ ┌─────▼──┐ ┌──────▼─┐ ┌──────────▼─┐
│Mirror1│ │Mirror2 │ │Mirror3 │ │Mirror...   │
└────┬──┘ └────┬───┘ └───┬────┘ └──────┬─────┘
     │         │         │             │
     └─────────┼─────────┼─────────────┘
               │
     ┌─────────▼──────────────────────────────┐
     │  SSH Connections (Apache SSHD)         │
     │  Text Processors (grep, sed, awk)      │
     │  Event Processors (enrichment, etc)    │
     │  Local Mirror File Writer              │
     └────────────────────────────────────────┘
```

### Processing Pipeline Flow

```
Raw log line from SSH tail
     ↓
[Text Processor 1] ──→ Filter/transform as plain text
     ↓
[Text Processor N] ──→ More text processing
     ↓
Create LogEvent (with metadata)
     ↓
[Event Processor 1] ──→ Enrichment, validation, etc.
     ↓
[Event Processor N] ──→ More structured processing
     ↓
Processed LogEvent
     ↓
Write to mirror file
```

## Technical Stack

- **Java 21** with Virtual Threads
- **Spring Boot 3.2.11**
- **Spring Web** for REST API
- **Apache SSHD 2.10.0** for SSH connectivity
- **Jackson** for JSON serialization with JavaTimeModule
- **Micrometer** for metrics
- **Spring Boot Actuator** for monitoring
- **SpringDoc OpenAPI 2.1.0** for REST API documentation

## Package Structure

```
dbc.logmirror/
├── api/                      # REST Controllers
│   ├── ServerController.java
│   ├── LogController.java
│   ├── AdminController.java
│   └── StatusController.java
├── config/                   # Configuration
│   ├── ApplicationBeanConfiguration.java
│   ├── ConfigurationManager.java
│   ├── ConfigurationValidator.java
│   └── EnvironmentVariableResolver.java
├── mirror/                   # Mirror Engine
│   ├── LogMirrorTask.java
│   └── MirrorManager.java
├── model/                    # Domain Models
│   ├── ApplicationConfig.java
│   ├── LogDefinition.java
│   ├── Server.java
│   ├── ProcessorConfig.java
│   ├── RotationPolicyConfig.java
│   ├── RetentionPolicyConfig.java
│   ├── MirrorStatistics.java
│   ├── LogEvent.java
│   ├── AuthenticationType.java
│   └── ProcessorType.java
├── pipeline/                 # Processing Pipeline
│   └── PipelineExecutor.java
├── processor/                # Processor Implementations
│   ├── LogEventProcessor.java       # Interface for structured event processing
│   ├── LogTextProcessor.java        # Interface for plain text processing
│   ├── LogStreamProcessor.java      # Legacy interface (deprecated)
│   ├── TextShellProcessor.java      # Shell commands on raw text (grep, sed, awk)
│   ├── EventShellProcessor.java     # Shell commands on JSON events
│   ├── ShellProcessor.java          # Legacy shell processor (deprecated)
│   ├── ProcessorFactory.java        # Factory for creating processors
│   └── example/
│       └── PasswordMaskingProcessor.java
├── ssh/                      # SSH Layer
│   ├── SSHConnection.java
│   └── SSHConnectionManager.java
├── rotation/                 # Rotation Policies
│   └── LogRotationUtil.java
├── retention/                # Retention Policies
│   └── RetentionUtil.java
├── scheduler/                # Scheduled Tasks
│   └── RetentionScheduler.java
├── monitoring/               # Metrics & Monitoring
│   └── MetricsPublisher.java
├── security/                 # Security & Masking
│   └── SecretMasker.java
├── persistence/              # Configuration Persistence
│   └── ConfigurationPersistence.java
├── Application.java          # Spring Boot Entrypoint
└── ApplicationStartupListener.java
```

## Processor Types

The Log Mirror Service supports four processor types organized into two tiers:

### Text Processors (Plain Text Processing)

These processors operate on raw log lines as strings with no JSON serialization overhead.

**Processor Type: `TEXT_SHELL`**
- Operates directly on raw log lines
- No JSON serialization/deserialization
- Ideal for standard Unix tools: `grep`, `sed`, `awk`, `tr`, `cut`, `sort`, `uniq`
- Returns list of strings (can filter, duplicate, or transform lines)
- Highly efficient for simple text filtering and transformation

**Example:** Filter out debug lines
```json
{
  "id": "filter-debug",
  "type": "TEXT_SHELL",
  "command": "grep -v 'DEBUG'"
}
```

### Event Processors (Structured Event Processing)

These processors operate on `LogEvent` objects with full metadata access.

**Processor Type: `JAVA_CLASS`**
- Custom Java implementation of `LogEventProcessor` interface
- Full access to LogEvent metadata (timestamp, server, attributes)
- Use for: complex business logic, enrichment, validation
- Compiled into the application

**Processor Type: `EVENT_SHELL`**
- Shell commands receive/send JSON-serialized LogEvent objects
- Ideal for: sophisticated processing in Python, Node.js, etc.
- Access to full LogEvent metadata via JSON
- Returns list of events (can filter, enrich, or duplicate)

**Example:** Python-based event enrichment
```json
{
  "id": "enrich-processor",
  "type": "EVENT_SHELL",
  "command": "python3 /opt/processors/enrich.py"
}
```

**Processor Type: `SHELL_COMMAND`** (Legacy)
- Original JSON-based text processor
- Maintained for backward compatibility
- Use `TEXT_SHELL` or `EVENT_SHELL` for new configurations

### Pipeline Execution Flow

The pipeline executes in two phases:

1. **Text Processing Phase**: All `TEXT_SHELL` processors execute on the raw log line
   - Can filter lines (return empty list to drop)
   - Can transform lines (modify text)
   - Can duplicate lines (return multiple strings)

2. **Event Processing Phase**: Raw text is converted to `LogEvent`, then `JAVA_CLASS` and `EVENT_SHELL` processors execute
   - Can enrich events with metadata
   - Can validate and filter events
   - Can duplicate events (return multiple LogEvent objects)

This dual-tier approach provides both efficiency (text processors) and power (event processors).

## Configuration

Configuration is stored in `config/log-mirror.json` and consists of:

### Server Configuration

```json
{
  "servers": [
    {
      "id": "web-server-01",
      "name": "Web Server 01",
      "host": "web01.example.com",
      "port": 22,
      "username": "sysadmin",
      "authenticationType": "PASSWORD",
      "password": "${SSH_PASSWORD_WEB01}",
      "privateKeyFile": null,
      "passphrase": null
    }
  ]
}
```

**Authentication Methods:**
- `PASSWORD`: SSH password authentication with environment variable support
- `PRIVATE_KEY`: Private key authentication with optional passphrase

**Environment Variable Resolution:**
Use `${VAR_NAME}` placeholders in credential fields. These are resolved from system environment variables at runtime.

### Log Definition Configuration

```json
{
  "logs": [
    {
      "id": "web-access-logs",
      "serverId": "web-server-01",
      "sourcePath": "/var/log/nginx/access.log",
      "localMirrorPath": "/data/mirrors/web-access.log",
      "enabled": true,
      "rotationPolicy": {
        "type": "SIZE",
        "maxSizeMB": 500,
        "gzipCompressed": true
      },
      "retentionPolicy": {
        "type": "AGE",
        "maxAgeDays": 30
      },
      "processors": [
        {
          "id": "errorFilter",
          "type": "TEXT_SHELL",
          "command": "grep -v 'HTTP/1.1 2'",
          "parameters": {}
        },
        {
          "id": "enrichment",
          "type": "EVENT_SHELL",
          "command": "python3 /opt/processors/enrich.py",
          "parameters": {}
        }
      ]
    }
  ]
}
```

### Rotation Policies

**SIZE**: Rotate when file exceeds `maxSizeMB`
```json
{
  "type": "SIZE",
  "maxSizeMB": 500,
  "gzipCompressed": true
}
```

**TIME**: Rotate on schedule
```json
{
  "type": "TIME",
  "timeUnit": "DAILY"
}
```

**INACTIVITY**: Rotate if no events for specified duration
```json
{
  "type": "INACTIVITY",
  "inactivityMinutes": 60
}
```

**SOURCE_ROTATION**: Rotate when remote log rotates

### Retention Policies

**AGE**: Delete files older than specified days
```json
{
  "type": "AGE",
  "maxAgeDays": 30
}
```

**COUNT**: Keep only most recent N files
```json
{
  "type": "COUNT",
  "maxFiles": 100
}
```

**SIZE**: Keep total size under threshold
```json
{
  "type": "SIZE",
  "maxTotalSizeGB": 50
}
```

### Security Configuration

```json
{
  "security": {
    "allowShellProcessors": true,
    "allowedCommandPrefixes": [
      "/bin/",
      "/usr/bin/",
      "/opt/log-processors/"
    ]
  }
}
```

## REST API

### Server Management

```http
GET    /api/servers              # List all servers (credentials masked)
POST   /api/servers              # Create new server
GET    /api/servers/{id}         # Get specific server (credentials masked)
PUT    /api/servers/{id}         # Update server
DELETE /api/servers/{id}         # Delete server
```

### Log Management

```http
GET    /api/logs                 # List all log definitions
POST   /api/logs                 # Create new log definition
GET    /api/logs/{id}            # Get specific log definition
PUT    /api/logs/{id}            # Update log definition
DELETE /api/logs/{id}            # Delete log definition
```

### Administration

```http
POST   /api/admin/reload         # Reload configuration from disk
POST   /api/admin/save           # Save current configuration to disk
POST   /api/admin/start/{logId}  # Start mirroring for specific log
POST   /api/admin/stop/{logId}   # Stop mirroring for specific log
```

### Monitoring

```http
GET    /api/status               # Get system status and statistics
GET    /actuator/metrics         # All metrics
GET    /actuator/health          # Health check
GET    /actuator/prometheus      # Prometheus metrics
```

### REST API Documentation and Swagger UI

The Log Mirror Service provides complete OpenAPI 3.0 documentation with an interactive Swagger UI for API exploration and testing.

#### Accessing the API Documentation

Once the application is running on `http://localhost:8080`, access the following endpoints:

- **Swagger UI (Interactive)**: http://localhost:8080/swagger-ui.html
  - Beautiful, interactive web interface for exploring and testing all API endpoints
  - Try-it-out feature to execute requests directly from the browser
  - Parameter validation and schema visualization
  - Request/response examples

- **OpenAPI JSON Specification**: http://localhost:8080/v3/api-docs
  - Complete OpenAPI 3.0 specification in JSON format
  - Can be imported into Postman, Insomnia, or other REST clients
  - Useful for API documentation generation and integration

- **OpenAPI YAML Specification**: http://localhost:8080/v3/api-docs.yaml
  - Same specification in YAML format

#### API Documentation Features

The API documentation includes:

1. **Tagged Endpoints**: Organized by functionality
   - **Servers**: SSH server configuration management
   - **Logs**: Log mirror definitions management
   - **Admin**: Administrative operations (reload, save, start, stop mirrors)
   - **Status**: Service status and statistics queries

2. **Detailed Operation Documentation**: Each endpoint includes
   - Clear description of what the endpoint does
   - Parameter documentation (path, query, body)
   - Request/response examples
   - HTTP status codes and error descriptions

3. **Schema Documentation**: Complete information about data models
   - Server configuration schema with all fields
   - LogDefinition schema with rotation/retention policies
   - Response object schemas

4. **Security Information**: Notes on credential masking in API responses

#### Example API Calls Using the Swagger UI

1. **Create a new server**:
   - Navigate to Servers > POST /api/servers
   - Click "Try it out"
   - Enter server configuration JSON
   - Click "Execute"

2. **Get all logs**:
   - Navigate to Logs > GET /api/logs
   - Click "Try it out"
   - Click "Execute"
   - View the response

3. **Start a mirror**:
   - Navigate to Admin > POST /api/admin/start/{logId}
   - Click "Try it out"
   - Enter the log ID
   - Click "Execute"

#### OpenAPI Configuration

The OpenAPI configuration is customizable via `OpenApiConfiguration.java`:

```java
@Configuration
public class OpenApiConfiguration {
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Log Mirror Service API")
                        .version("0.1.0")
                        .description("..."))
                .addServersItem(new Server()
                        .url("http://localhost:8080")
                        .description("Local development server"));
    }
}
```

To customize:
- Update the title, description, version, and contact information
- Add additional server configurations (dev, staging, production)
- Modify response examples and schema documentation

#### Integration with API Tools

The OpenAPI specification can be used with various tools:

- **Postman**: Import the OpenAPI JSON specification for automated collection generation
- **Insomnia**: Use the OpenAPI URL to auto-generate requests
- **API Documentation Generators**: Use the spec to generate static HTML documentation
- **OpenAPI Code Generators**: Generate client SDKs in any language



### Creating Processors

#### Text Processor (Plain Unix Tool)

Text processors operate on raw log lines using standard Unix tools:

```bash
#!/bin/bash
# /opt/processors/filter-important.sh
# Only pass lines with ERROR or CRITICAL

while IFS= read -r line; do
  if [[ "$line" =~ ERROR|CRITICAL ]]; then
    echo "$line"
  fi
done
```

Reference in configuration:

```json
{
  "id": "importantOnly",
  "type": "TEXT_SHELL",
  "command": "/opt/processors/filter-important.sh"
}
```

**Common Unix Commands for Text Processors:**
- `grep` - Filter lines by pattern
- `sed` - Stream editing (substitution, transformation)
- `awk` - Field extraction and processing
- `tr` - Character translation
- `cut` - Field/column extraction
- `sort` - Sort lines
- `uniq` - Remove duplicate lines

#### Event Processor (Java Class)

Implement `LogEventProcessor` for custom Java logic with full metadata access:

```java
package dbc.processor;

import dbc.logmirror.model.LogEvent;
import dbc.logmirror.processor.LogEventProcessor;
import java.util.ArrayList;
import java.util.List;

public class CustomProcessor implements LogEventProcessor {

    @Override
    public void start() throws Exception {
        // Initialize resources
    }

    @Override
    public List<LogEvent> process(LogEvent event) throws Exception {
        // Transform or filter the event
        String modified = event.getLine().toUpperCase();
        event.setLine(modified);
        
        List<LogEvent> results = new ArrayList<>();
        
        // Return empty list to drop the event
        // Return single event to pass through (possibly modified)
        // Return multiple events for duplication/splitting
        results.add(event);
        
        return results;
    }

    @Override
    public void stop() {
        // Cleanup resources
    }
}
```

Reference in configuration:

```json
{
  "id": "uppercase",
  "type": "JAVA_CLASS",
  "className": "dbc.processor.CustomProcessor",
  "parameters": {}
}
```

#### Event Processor (JSON-based Shell Command)

Event shell processors receive JSON-serialized `LogEvent` objects on stdin and write transformed events as JSON to stdout:

```python
#!/usr/bin/env python3
# /opt/processors/enrich.py
# Enriches log events with metadata

import sys
import json
from datetime import datetime

for line in sys.stdin:
    try:
        event = json.loads(line.strip())
        
        # Add enrichment data
        event['attributes']['enriched'] = True
        event['attributes']['enrichedAt'] = datetime.now().isoformat()
        event['attributes']['severity'] = 'HIGH' if 'ERROR' in event['line'] else 'LOW'
        
        # Output the enriched event
        print(json.dumps(event))
    except json.JSONDecodeError:
        # Pass through unparseable events
        print(line.strip())
```

Reference in configuration:

```json
{
  "id": "enrichment",
  "type": "EVENT_SHELL",
  "command": "/opt/processors/enrich.py",
  "parameters": {}
}
```

#### Mixed Pipeline Example

Combine text and event processors for efficient processing:

```json
{
  "logs": [
    {
      "id": "web-logs",
      "processors": [
        {
          "id": "filter1",
          "type": "TEXT_SHELL",
          "command": "grep -v 'DEBUG'"
        },
        {
          "id": "filter2",
          "type": "TEXT_SHELL",
          "command": "grep 'ERROR|WARN'"
        },
        {
          "id": "enrich",
          "type": "EVENT_SHELL",
          "command": "python3 /opt/enrich.py"
        },
        {
          "id": "validate",
          "type": "JAVA_CLASS",
          "className": "dbc.EventValidator"
        }
      ]
    }
  ]
}
```

**Processing Flow:**
1. Raw text → `grep -v 'DEBUG'` → filters DEBUG lines
2. Text → `grep 'ERROR|WARN'` → keeps only ERROR/WARN
3. Text converted to LogEvent
4. Event → `enrich.py` → adds metadata enrichment
5. Event → `EventValidator` → validates and ensures quality
6. Final event written to mirror file

## Processor Architecture

For detailed information about the processor architecture, including design patterns, performance considerations, and advanced use cases, see [PROCESSOR_ARCHITECTURE.md](PROCESSOR_ARCHITECTURE.md).

**Key Highlights:**
- Two-tier architecture: Text processors → Event processors
- Text processors for efficient plain text filtering with Unix tools
- Event processors for structured enrichment and transformation
- Pipeline execution with metadata access and event manipulation
- Support for dropping, transforming, and duplicating events

## Building and Running

### Prerequisites

- Java 21
- Maven 3.8+
- Linux/Unix environment for SSH connectivity

### Build

```bash
mvn clean package
```

### Run

```bash
# Set environment variables for credentials
export SSH_PASSWORD_WEB01="password123"
export PRIVATE_KEY_PATH="/home/user/.ssh/id_rsa"
export KEY_PASSPHRASE="passphrase"

# Start the service
java -jar target/log-mirror-service-0.1.0.jar
```

### Configuration

Set the config file path via environment variable:

```bash
export LOG_MIRROR_CONFIG_PATH=/etc/log-mirror/config.json
java -jar target/log-mirror-service-0.1.0.jar
```

Default location: `config/log-mirror.json`

## Deployment

### Docker Deployment

Create a `Dockerfile`:

```dockerfile
FROM eclipse-temurin:21-jdk

WORKDIR /app

COPY target/log-mirror-service-0.1.0.jar .
COPY config/log-mirror.json config/

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "log-mirror-service-0.1.0.jar"]
```

Build and run:

```bash
docker build -t log-mirror-service .
docker run -e SSH_PASSWORD_WEB01=password -p 8080:8080 log-mirror-service
```

### Systemd Service

Create `/etc/systemd/system/log-mirror-service.service`:

```ini
[Unit]
Description=Log Mirror Service
After=network.target

[Service]
Type=simple
User=logmirror
WorkingDirectory=/opt/log-mirror-service
EnvironmentFile=/opt/log-mirror-service/.env
ExecStart=/usr/bin/java -jar /opt/log-mirror-service/log-mirror-service.jar
Restart=on-failure
RestartSec=10

[Install]
WantedBy=multi-user.target
```

Enable and start:

```bash
sudo systemctl daemon-reload
sudo systemctl enable log-mirror-service
sudo systemctl start log-mirror-service
```

## Monitoring

### Metrics

Available metrics via `/actuator/metrics`:

- `mirrors.active` - Number of active mirror tasks
- `events.processed` - Total events processed
- `events.dropped` - Events filtered by processors
- `reconnects` - SSH reconnection attempts
- `processor.failures` - Processor errors
- `rotations` - Log rotations performed
- `retention.deletions` - Retention cleanup deletions

### Health Checks

```bash
curl http://localhost:8080/actuator/health
```

### Prometheus Integration

Metrics available at `/actuator/prometheus` for Prometheus scraping:

```yaml
# prometheus.yml
scrape_configs:
  - job_name: 'log-mirror-service'
    static_configs:
      - targets: ['localhost:8080']
    metrics_path: '/actuator/prometheus'
```

## Troubleshooting

### SSH Connection Issues

1. Verify SSH credentials and environment variables are set
2. Check SSH key permissions: `chmod 600 ~/.ssh/id_rsa`
3. Enable debug logging: Set `dbc.logmirror.ssh` to DEBUG level

### Processor Failures

1. Check processor logs for errors
2. Verify Java class is on classpath
3. Ensure shell commands have proper permissions

### Mirror Not Starting

1. Check configuration file syntax
2. Verify referenced servers exist
3. Check local mirror path is writable

### Performance Issues

1. Monitor active mirrors: `/api/status`
2. Check system resources and log file sizes
3. Optimize processor pipeline (remove unnecessary processing)

## Security Considerations

1. **Credentials**: Never commit credentials to version control. Use environment variables.
2. **SSH Keys**: Protect private keys with appropriate file permissions
3. **Network**: Run in secure network with SSH only from trusted hosts
4. **API Access**: Restrict REST API access via firewall or authentication layer
5. **Logs**: Credentials are never logged; check with `SecretMasker` utilities

## License

See `LICENSE`

## Support

For issues and feature requests, please open an issue in the repository.
