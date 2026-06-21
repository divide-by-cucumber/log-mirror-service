package dbc.logmirror.rotation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.zip.GZIPOutputStream;

public class LogRotationUtil {

    private static final Logger logger = LoggerFactory.getLogger(LogRotationUtil.class);
    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    public static String rotateLog(String logPath, boolean gzipCompress) throws IOException {
        Path path = Paths.get(logPath);
        File logFile = path.toFile();

        if (!logFile.exists()) {
            logger.warn("Log file does not exist: {}", logPath);
            return null;
        }

        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);
        String rotatedName = logPath + "." + timestamp;

        if (gzipCompress) {
            rotatedName += ".gz";
            compressAndRotate(logFile, rotatedName);
            logger.info("Rotated and compressed log: {} -> {}", logPath, rotatedName);
        } else {
            File rotatedFile = new File(rotatedName);
            if (!logFile.renameTo(rotatedFile)) {
                throw new IOException("Failed to rename log file to: " + rotatedName);
            }
            logger.info("Rotated log: {} -> {}", logPath, rotatedName);
        }

        // Create new empty log file
        logFile.createNewFile();
        return rotatedName;
    }

    private static void compressAndRotate(File source, String destinationPath) throws IOException {
        try (FileInputStream fis = new FileInputStream(source);
             FileOutputStream fos = new FileOutputStream(destinationPath);
             GZIPOutputStream gzipOutputStream = new GZIPOutputStream(fos)) {

            byte[] buffer = new byte[8192];
            int len;
            while ((len = fis.read(buffer)) != -1) {
                gzipOutputStream.write(buffer, 0, len);
            }
        }

        // Delete original file after successful compression
        if (!source.delete()) {
            logger.warn("Failed to delete original log file after compression: {}", source.getAbsolutePath());
        }
    }

    public static long getFileSize(String filePath) {
        File file = new File(filePath);
        return file.exists() ? file.length() : 0;
    }

    public static boolean shouldRotateBySize(String logPath, long maxSizeBytes) {
        return getFileSize(logPath) >= maxSizeBytes;
    }
}
