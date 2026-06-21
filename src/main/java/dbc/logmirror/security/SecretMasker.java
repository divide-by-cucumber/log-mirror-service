package dbc.logmirror.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SecretMasker {

    private static final Logger logger = LoggerFactory.getLogger(SecretMasker.class);
    private static final String MASK = "***";

    public static String maskPassword(String password) {
        if (password == null) {
            return null;
        }
        return password.length() > 0 ? MASK : "";
    }

    public static String maskValue(String value) {
        if (value == null) {
            return null;
        }
        return MASK;
    }

    public static String redactLogLine(String line) {
        if (line == null) {
            return line;
        }
        // Redact common secrets: password=..., token=..., apiKey=..., etc.
        String redacted = line.replaceAll("(?i)(password|passwd|pwd)\\s*[:=]\\s*[^\\s,}]+", "$1=***");
        redacted = redacted.replaceAll("(?i)(token|apikey|api_key|secret)\\s*[:=]\\s*[^\\s,}]+", "$1=***");
        redacted = redacted.replaceAll("(?i)(authorization|auth)\\s*[:=]\\s*[^\\s,}]+", "$1=***");
        return redacted;
    }
}
