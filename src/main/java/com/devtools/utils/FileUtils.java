package com.devtools.utils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Stream;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Utility class for file and directory operations.
 * Uses modern Java NIO APIs for better performance and error handling.
 */
public final class FileUtils {

    private static final Log LOG = LogFactory.getLog(FileUtils.class);

    private FileUtils() {
        // Utility class - prevent instantiation
    }

    /**
     * Writes content to a file using Java NIO.
     * Creates parent directories if they don't exist.
     * 
     * @param filename the target file path
     * @param content the content to write
     * @throws IOException if an I/O error occurs
     */
    public static void writeFile(final String filename, final String content) throws IOException {
        final Path path = Paths.get(filename);
        
        // Create parent directories if they don't exist
        final Path parentDir = path.getParent();
        if (parentDir != null) {
            Files.createDirectories(parentDir);
        }
        
        Files.writeString(path, content, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        LOG.info("Successfully generated " + filename);
    }

    /**
     * Creates a directory and all necessary parent directories.
     * Uses Java NIO for better error handling.
     * 
     * @param directoryPath the directory path to create
     * @return false if directory creation failed, true if successful or already exists
     */
    public static boolean createDirectories(final String directoryPath) {
        try {
            final Path path = Paths.get(directoryPath);
            
            if (Files.exists(path)) {
                if (!Files.isDirectory(path)) {
                    LOG.error(directoryPath + " exists but is not a directory.");
                    return false; // Failure - exists but not a directory
                }
                return true; // Success - already exists
            }
            
            Files.createDirectories(path);
            LOG.info("Directory created successfully: " + directoryPath);
            return true; // Success
            
        } catch (final IOException e) {
            LOG.error("Failed to create directory: " + directoryPath, e);
            return false; // Failure
        }
    }

    /**
     * Extracts the filename without any extensions and capitalizes the first letter.
     * Uses Apache Commons IO FilenameUtils for better cross-platform compatibility.
     * 
     * @param absoluteFilename the absolute file path
     * @return the filename without extensions, with first letter capitalized
     */
    public static String getFileNameNoExtensions(final String absoluteFilename) {
        // Use Apache Commons IO to extract filename without extension
        String fileName = FilenameUtils.getBaseName(absoluteFilename);

        // Remove multiple extensions manually (everything after the first dot)
        final int firstDotIndex = fileName.indexOf('.');
        if (firstDotIndex != -1) {
            fileName = fileName.substring(0, firstDotIndex);
        }

        // Capitalize the first letter using Apache Commons Lang3
        return StringUtils.capitalize(fileName);
    }

    /**
     * Recursively searches for a file matching the specified class name inside the folder.
     * It looks for "ClassName.java"
     * Uses Apache Commons IO for more efficient file searching.
     *
     * @param folder    the starting directory to search in
     * @param className the name of the class (without extension)
     * @return the full absolute path of the file if found; otherwise, null.
     */
    public static String findClassPath(final File folder, final String packageName, final String className) {
        if (folder == null || !folder.isDirectory()) {
            return null;
        }

        final Path rootPath = folder.toPath();
        final String targetFile = className + ".java";
        final String packagePath = packageName.replace(".", File.separator);

        try (final Stream<Path> pathStream = Files.find(rootPath, Integer.MAX_VALUE,
                (path, attr) -> attr.isRegularFile() && path.getFileName().toString().equals(targetFile)).parallel()) {
            return pathStream
                    .filter(path -> path.toAbsolutePath().toString().contains(packagePath))
                    .findFirst()
                    .map(Path::toAbsolutePath)
                    .map(Path::toString)
                    .orElse(null);
        } catch (final Exception ignored) {
            return null;
        }
    }

    public static Map<String, Set<String>> readPropertiesFile(final String resourceName) {
        final Properties properties = new Properties();
        final Map<String, Set<String>> parsedProperties = new HashMap<>();

        try (final InputStream inputStream = FileUtils.class.getClassLoader().getResourceAsStream(resourceName)) {
            if (inputStream == null) {
                return parsedProperties; // Ignore missing file silently
            }
            properties.load(inputStream);

            // First pass: Read properties as sets
            properties.forEach((key, value) -> {
                final String keyStr = key.toString();
                final Set<String> values = Set.of(value.toString().split(","));
                parsedProperties.put(keyStr, values);
            });

            // Second pass: Resolve placeholders
            parsedProperties.forEach((key, valueSet) -> {
                final Set<String> resolvedValues = new HashSet<>();
                for (final String value : valueSet) {
                    if (value.startsWith("${") && value.endsWith("}")) { // Placeholder detected
                        final String referencedKey = value.substring(2, value.length() - 1); // Extract key
                        resolvedValues.addAll(parsedProperties.getOrDefault(referencedKey, Collections.emptySet()));
                    } else {
                        resolvedValues.add(value);
                    }
                }
                parsedProperties.put(key, resolvedValues); // Update with resolved values
            });

        } catch (final IOException e) {
            System.err.println("Error reading properties file: " + e.getMessage());
        }

        return parsedProperties;
    }
}