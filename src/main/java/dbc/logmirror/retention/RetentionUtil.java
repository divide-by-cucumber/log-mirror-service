package dbc.logmirror.retention;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class RetentionUtil {

    private static final Logger logger = LoggerFactory.getLogger(RetentionUtil.class);

    public static long applyRetentionPolicy(String logPath, Long maxAgeDays, Integer maxFiles, Double maxTotalSizeGB) throws Exception {
        File file = new File(logPath);
        Path dirPath = file.getParentFile().toPath();

        if (!Files.exists(dirPath)) {
            return 0;
        }

        String baseName = file.getName();
        List<File> rotatedFiles = new ArrayList<>();

        // Find all rotated files matching the pattern (e.g., access.log.*)
        try (Stream<Path> paths = Files.list(dirPath)) {
            paths.filter(p -> p.getFileName().toString().startsWith(baseName + "."))
                    .forEach(p -> rotatedFiles.add(p.toFile()));
        }

        long deletedBytes = 0;

        // Apply age policy
        if (maxAgeDays != null && maxAgeDays > 0) {
            Instant cutoffTime = Instant.now().minus(maxAgeDays, ChronoUnit.DAYS);
            for (File f : rotatedFiles) {
                try {
                    FileTime lastModified = Files.getLastModifiedTime(f.toPath());
                    if (lastModified.toInstant().isBefore(cutoffTime)) {
                        deletedBytes += f.length();
                        if (f.delete()) {
                            logger.info("Deleted old log file: {}", f.getAbsolutePath());
                        }
                    }
                } catch (Exception e) {
                    logger.error("Error checking file age: {}", f.getAbsolutePath(), e);
                }
            }
        }

        // Refresh list after age cleanup
        rotatedFiles.clear();
        try (Stream<Path> paths = Files.list(dirPath)) {
            paths.filter(p -> p.getFileName().toString().startsWith(baseName + "."))
                    .forEach(p -> rotatedFiles.add(p.toFile()));
        }

        // Apply count policy
        if (maxFiles != null && maxFiles > 0) {
            while (rotatedFiles.size() > maxFiles) {
                // Sort by modification time and delete oldest
                File oldest = rotatedFiles.stream()
                        .min((f1, f2) -> {
                            try {
                                return Files.getLastModifiedTime(f1.toPath())
                                        .compareTo(Files.getLastModifiedTime(f2.toPath()));
                            } catch (Exception e) {
                                return 0;
                            }
                        })
                        .orElse(null);

                if (oldest != null) {
                    deletedBytes += oldest.length();
                    if (oldest.delete()) {
                        logger.info("Deleted excess log file (count policy): {}", oldest.getAbsolutePath());
                        rotatedFiles.remove(oldest);
                    }
                }
            }
        }

        // Apply size policy
        if (maxTotalSizeGB != null && maxTotalSizeGB > 0) {
            long maxTotalSizeBytes = (long) (maxTotalSizeGB * 1024 * 1024 * 1024);
            long totalSize = rotatedFiles.stream().mapToLong(File::length).sum();

            while (totalSize > maxTotalSizeBytes && !rotatedFiles.isEmpty()) {
                // Sort by modification time and delete oldest
                File oldest = rotatedFiles.stream()
                        .min((f1, f2) -> {
                            try {
                                return Files.getLastModifiedTime(f1.toPath())
                                        .compareTo(Files.getLastModifiedTime(f2.toPath()));
                            } catch (Exception e) {
                                return 0;
                            }
                        })
                        .orElse(null);

                if (oldest != null) {
                    long fileSize = oldest.length();
                    deletedBytes += fileSize;
                    totalSize -= fileSize;

                    if (oldest.delete()) {
                        logger.info("Deleted log file (size policy): {}", oldest.getAbsolutePath());
                        rotatedFiles.remove(oldest);
                    }
                }
            }
        }

        return deletedBytes;
    }
}
