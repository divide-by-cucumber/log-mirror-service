package dbc.logmirror.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.HashSet;
import java.util.Set;

@Component
public class EnvironmentVariableResolver {

    private static final Logger logger = LoggerFactory.getLogger(EnvironmentVariableResolver.class);
    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\$\\{([^}]+)\\}");

    public String resolveValue(String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }

        StringBuffer result = new StringBuffer();
        Matcher matcher = PLACEHOLDER_PATTERN.matcher(value);

        while (matcher.find()) {
            String varName = matcher.group(1);
            String varValue = System.getenv(varName);

            if (varValue == null) {
                logger.error("Environment variable not found: {}", varName);
                throw new IllegalArgumentException("Environment variable not found: " + varName);
            }

            matcher.appendReplacement(result, Matcher.quoteReplacement(varValue));
        }

        matcher.appendTail(result);
        return result.toString();
    }

    public boolean containsPlaceholder(String value) {
        if (value == null) {
            return false;
        }
        return PLACEHOLDER_PATTERN.matcher(value).find();
    }

    public Set<String> extractPlaceholders(String value) {
        Set<String> placeholders = new HashSet<>();
        if (value == null) {
            return placeholders;
        }

        Matcher matcher = PLACEHOLDER_PATTERN.matcher(value);
        while (matcher.find()) {
            placeholders.add(matcher.group(1));
        }
        return placeholders;
    }

    public void validatePlaceholders(String value) throws IllegalArgumentException {
        Set<String> placeholders = extractPlaceholders(value);
        for (String placeholder : placeholders) {
            if (System.getenv(placeholder) == null) {
                throw new IllegalArgumentException("Environment variable not found: " + placeholder);
            }
        }
    }
}
